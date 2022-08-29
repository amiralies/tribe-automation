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
    val env = EventDesc.toEnv(event.eventDesc)

    val runEffectsEvent =
      for {
        automations <- automationRepo.getAllByNetworkIdAndTrigger(
          event.networkId,
          EventDesc.toTrigger(event.eventDesc)
        )
        effects = automations
          .flatMap(_.actions)
          .map(evalAction(_, env))
        runActionEvent = RunEffectsEvent(event.networkId, effects)
      } yield runActionEvent

    for {
      runEffectsEvent <- runEffectsEvent
      _ <- automationEffectRunner.runEffects(runEffectsEvent).forkDaemon
    } yield runEffectsEvent
  }

  private def evalAction(action: Action, env: Env): Effect = {
    action match {
      case Action.HttpPostRequest(url, jsonBody) =>
        Effect.EffHttpPostRequest(
          supplyVariables(url, env),
          supplyVariables(jsonBody, env)
        )

      case Action.SendNotifToAll(message) =>
        Effect.EffSendNotifToAll(supplyVariables(message, env))
    }
  }

  private def supplyVariables(str: String, env: Env): String =
    env.toList.foldLeft(str)((acc, fieldPair) => {
      val (fieldName, fieldValue) = fieldPair
      val pat = Utils.mustachePatternForField(fieldName)
      pat.replaceAllIn(acc, fieldValue)
    })

}

object AutomationServiceImpl {
  val layer = ZLayer.fromFunction(AutomationServiceImpl(_, _))
}
