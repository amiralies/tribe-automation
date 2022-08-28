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

  def handleEvent(event: Event): UIO[Unit]
}

case class AutomationServiceImpl(automationRepo: AutomationRepo)
    extends AutomationService {

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
  } yield automation

  override def handleEvent(event: Event): UIO[Unit] = event.eventDesc match {
    case EventDesc.EvPostCreated(title, content) => ZIO.logInfo(content) // TODO
    case EventDesc.EvSpaceCreated(spaceName) => ZIO.logInfo(spaceName) // TODO
  }

}

object AutomationServiceImpl {
  val layer = ZLayer.fromFunction(AutomationServiceImpl(_))
}
