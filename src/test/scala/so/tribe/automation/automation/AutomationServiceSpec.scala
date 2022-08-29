package so.tribe.automation.automation

import zio.test._
import zio._
import domain._
import Action._
import Condition._

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
    test("createAutomation should create an automation with correct if field") {
      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "networkId",
          "First automation",
          Trigger.TrSpaceCreated,
          List(
            AcIf(
              CdEq("spaceName", "General"),
              AcSendNotifToAll("General space has been created"),
              None
            )
          )
        )
        automation <- automationService.createAutomation(payload)
      } yield assertTrue(true)

    },
    test("createAutomation should fail with wrong if field") {
      for {
        automationService <- ZIO.service[AutomationService]
        payload = CreateAutomationPayload(
          "networkId",
          "First automation",
          Trigger.TrSpaceCreated,
          List(
            AcIf(
              CdEq("spacenotName", "General"),
              AcSendNotifToAll("General space has been created"),
              None
            )
          )
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
    },
    test(
      "handleEvent should generate proper events for if actions"
    ) {
      for {
        automationService <- ZIO.service[AutomationService]
        networkId = "networkId3"
        payload = CreateAutomationPayload(
          networkId,
          "First automation",
          Trigger.TrPostCreated,
          List(
            Action.AcIf(
              Condition.CdContains("content", "secretword"),
              AcSendNotifToAll("Wow it has secret word"),
              None
            )
          )
        )
        automation <- automationService.createAutomation(payload)
        r <- automationService.handleEvent(
          Event(
            networkId,
            EventDesc.EvPostCreated("hi", "this is my secretword bro")
          )
        )
      } yield assertTrue(
        r ==
          RunEffectsEvent(
            networkId,
            List(Effect.EffSendNotifToAll("Wow it has secret word"))
          )
      )
    },
    test(
      "handleEvent should generate proper events for if actions no exec"
    ) {
      for {
        automationService <- ZIO.service[AutomationService]
        networkId = "networkId4"
        payload = CreateAutomationPayload(
          networkId,
          "First automation",
          Trigger.TrPostCreated,
          List(
            Action.AcIf(
              Condition.CdContains("content", "secretword"),
              AcSendNotifToAll("Wow it has secret word"),
              None
            )
          )
        )
        automation <- automationService.createAutomation(payload)
        r <- automationService.handleEvent(
          Event(networkId, EventDesc.EvPostCreated("hi", "nop no secret here"))
        )
      } yield assertTrue(
        r ==
          RunEffectsEvent(
            networkId,
            Nil
          )
      )
    }
  ).provideShared(
    AutomationServiceImpl.layer,
    ZLayer.succeed(new MockAutomationEffectRunner()),
    InMemoryAutomationRepo.layer.fresh
  )
}
