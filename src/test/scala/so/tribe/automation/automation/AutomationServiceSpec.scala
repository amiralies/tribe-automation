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
          List(Action.AcSendNotifToAll("message"))
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
          List(Action.AcSendNotifToAll("message"))
        )
        error <- automationService.createAutomation(payload).flip
      } yield assertTrue(error == ValidationError)
    },
    test("createAutomation should fail when actions is empty") {
      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "networkId",
          "First automation",
          Trigger.TrPostCreated,
          List()
        )
        error <- automationService.createAutomation(payload).flip
      } yield assertTrue(error == ValidationError)
    },
    test("createAutomation should fail when name is empty") {
      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "networkId",
          "",
          Trigger.TrPostCreated,
          List(Action.AcSendNotifToAll("message"))
        )
        error <- automationService.createAutomation(payload).flip
      } yield assertTrue(error == ValidationError)
    },
    test(
      "handlEvent should generate empty runactionevent list when here's no automation"
    ) {
      for {
        automationService <- ZIO.service[AutomationService]
        r <- automationService.handleEvent(
          Event(
            "thisIdDoesenotExist",
            EventDesc.EvPostCreated("hi", "great stuff")
          )
        )
      } yield assertTrue(r.effects.isEmpty)
    },
    test(
      "handlEvent should generate proper runaction events based on given automation"
    ) {

      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "networkId2",
          "First automation",
          Trigger.TrPostCreated,
          List(Action.AcSendNotifToAll("message"))
        )
        automation <- automationService.createAutomation(payload)
        r <- automationService.handleEvent(
          Event("networkId2", EventDesc.EvPostCreated("hi", "great stuff"))
        )
      } yield assertTrue(
        r ==
          RunEffectsEvent(
            "networkId2",
            List(Effect.EffSendNotifToAll("message"))
          )
      )
    }
  ).provideShared(
    AutomationServiceImpl.layer,
    ZLayer.succeed(new MockAutomationEffectRunner()),
    InMemoryAutomationRepo.layer.fresh
  )
}
