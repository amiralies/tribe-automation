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
    val env = AutomationActionInterpreter.makeEnv(event.eventDesc)

    val runEffectsEvent =
      for {
        automations <- automationRepo.getAllByNetworkIdAndTrigger(
          event.networkId,
          EventDesc.toTrigger(event.eventDesc)
        )
        effects = automations
          .flatMap(_.actions)
          .map(AutomationActionInterpreter.interpret(_, env))
          .flatten
        runActionEvent = RunEffectsEvent(event.networkId, effects)
      } yield runActionEvent

    for {
      runEffectsEvent <- runEffectsEvent
      _ <- automationEffectRunner.runEffects(runEffectsEvent).forkDaemon
    } yield runEffectsEvent
  }

}

object AutomationServiceImpl {
  val layer = ZLayer.fromFunction(AutomationServiceImpl(_, _))
}
