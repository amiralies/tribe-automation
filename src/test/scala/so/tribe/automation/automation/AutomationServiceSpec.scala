package so.tribe.automation.automation

import zio.test._
import zio._
import domain._

object AutomationServiceSpec extends ZIOSpecDefault {
  def spec = suite("AutomationServiceSpec")(
    test("createAutomation should create an automation") {
      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "networkId",
          "First automation",
          Trigger.TrPostCreated,
          AutomationAction(Action.SendNotifToAll("message"), Nil)
        )
        automation <- automationService.createAutomation(payload)
      } yield assertTrue(true)

    },
    test("createAutomation should fail when networkId is empty") {
      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "",
          "First automation",
          Trigger.TrPostCreated,
          AutomationAction(Action.SendNotifToAll("message"), Nil)
        )
        error <- automationService.createAutomation(payload).flip
      } yield assertTrue(error == ValidationError)

    }
  ).provideShared(InMemoryAutomationRepo.layer, AutomationServiceImpl.layer)
}
