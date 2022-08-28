package so.tribe.automation.automation

object domain {
  case class Event(networkId: String, eventDesc: EventDesc)

  sealed trait EventDesc
  object EventDesc {
    case class EvPostCreated(title: String, content: String) extends EventDesc
    case class EvSpaceCreated(spaceName: String) extends EventDesc
  }

  sealed trait Trigger
  object Trigger {
    case object TrPostCreated extends Trigger
    case object TrSpaceCreated extends Trigger
  }

  sealed trait Action
  object Action {
    case class SendNotifToAll(message: String) extends Action
    case class HttpPostRequest(url: String, jsonBody: String) extends Action
  }

  case class Automation(
      id: String,
      name: String,
      networkId: String,
      trigger: Trigger,
      actions: List[Action]
  )

  case class CreateAutomationPayload(
      networkId: String,
      name: String,
      trigger: Trigger,
      actions: List[Action]
  )

  case class RunEffectsEvent(
      networkId: String,
      effects: List[Effect]
  )

  sealed trait Effect
  object Effect {
    case class EffSendNotifToAll(message: String) extends Effect
    case class EffHttpPostRequest(url: String, jsonBody: String) extends Effect
  }

  case object ValidationError
}

object AutomationDomainValidators {
  import com.wix.accord.dsl._

  implicit val createAutomationPayloadValidator =
    validator[domain.CreateAutomationPayload] { p =>
      p.name is notEmpty
      p.networkId is notEmpty
      p.actions is notEmpty
    }
}
