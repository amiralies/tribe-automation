package so.tribe.automation.automation

import domain._
import zio._
import AutomationDomainValidators._
import so.tribe.automation.Utils
import com.wix.accord._

trait AutomationService {
  def createAutomation(
      payload: CreateAutomationPayload
  ): IO[ValidationError.type, Automation]

  def handleEvent(event: Event): UIO[RunEffectsEvent]
}

case class AutomationServiceImpl(
    automationRepo: AutomationRepo,
    automationEffectRunner: AutomationEffectRunner
) extends AutomationService {

  override def createAutomation(
      payload: CreateAutomationPayload
  ): IO[ValidationError.type, Automation] = for {
    () <- Utils.zioValidate(payload).mapError(_ => ValidationError)
    id = Utils.genUrlSafeUUID()
    automation = Automation(
      id,
      payload.name,
      payload.networkId,
      payload.trigger,
      payload.actions
    )
    () <- automationRepo.insert(automation)
  } yield automation

  override def handleEvent(event: Event): UIO[RunEffectsEvent] = {
    val runEffectsEvent = event.eventDesc match {
      case EventDesc.EvPostCreated(title, content) =>
        for {
          automations <- automationRepo.getAllByNetworkIdAndTrigger(
            event.networkId,
            Trigger.TrPostCreated
          )
          effects = automations
            .flatMap(_.actions)
            .map(actionToEffect)
          runActionEvent = RunEffectsEvent(event.networkId, effects)
        } yield runActionEvent

      case EventDesc.EvSpaceCreated(spaceName) =>
        for {
          automations <- automationRepo.getAllByNetworkIdAndTrigger(
            event.networkId,
            Trigger.TrSpaceCreated
          )
          effects = automations
            .flatMap(_.actions)
            .map(actionToEffect)
          runActionEvent = RunEffectsEvent(event.networkId, effects)
        } yield runActionEvent
    }

    for {
      runEffectsEvent <- runEffectsEvent
      _ <- automationEffectRunner.runEffects(runEffectsEvent).forkDaemon
    } yield runEffectsEvent
  }

  private def actionToEffect(action: Action): Effect =
    action match {
      case Action.HttpPostRequest(url, jsonBody) =>
        Effect.EffHttpPostRequest(url, jsonBody)
      case Action.SendNotifToAll(message) =>
        Effect.EffSendNotifToAll(message)
    }

}

object AutomationServiceImpl {
  val layer = ZLayer.fromFunction(AutomationServiceImpl(_, _))
}
