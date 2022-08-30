package so.tribe.automation.automation

import domain._
import zio._
import AutomationDomainValidators._
import so.tribe.automation.Utils
import com.wix.accord._

trait AutomationService {
  def createAutomation(
      payload: CreateAutomationPayload
  ): IO[DomainFailure, Automation]

  def deleteAutomation(
      automationId: String
  ): IO[DomainFailure, Automation]

  def getNetworkAutomations(networkId: String): UIO[List[Automation]]

  def handleEvent(event: Event): UIO[RunEffectsEvent]

}

case class AutomationServiceImpl(
    automationRepo: AutomationRepo,
    automationEffectRunner: AutomationEffectRunner
) extends AutomationService {

  override def createAutomation(
      payload: CreateAutomationPayload
  ): IO[DomainFailure, Automation] = for {
    () <- Utils
      .zioValidate(payload)
      .mapError(_ => DomainFailure.ValidationError)
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

  override def deleteAutomation(
      automationId: String
  ): IO[DomainFailure, Automation] =
    automationRepo
      .deleteById(automationId)
      .flatMap({
        case None             => ZIO.fail(DomainFailure.NotFound)
        case Some(automation) => ZIO.succeed(automation)
      })

  override def getNetworkAutomations(networkId: String): UIO[List[Automation]] =
    automationRepo.getByNetworkId(networkId)

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
