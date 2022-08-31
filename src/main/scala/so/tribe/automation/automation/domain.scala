package so.tribe.automation.automation

import enumeratum.{Enum, EnumEntry, CirceEnum}
import reactivemongo.api.bson.Macros
import reactivemongo.api.bson.BSONDocumentHandler
import reactivemongo.api.bson.BSONDocument
import scala.util.Try
import so.tribe.automation.persist.mongo.MongoUtils
import io.circe.generic.extras.JsonKey

object domain {
  type Env = Map[String, String]

  case class Event(networkId: String, eventDesc: EventDesc)

  sealed trait EventDesc
  object EventDesc {
    case class EvPostCreated(title: String, content: String) extends EventDesc
    case class EvSpaceCreated(spaceName: String) extends EventDesc

    def toTrigger(desc: EventDesc) = desc match {
      case EvPostCreated(_, _) => Trigger.TrPostCreated
      case EvSpaceCreated(_)   => Trigger.TrSpaceCreated
    }
  }

  case class Automation(
      @JsonKey("_id") id: String,
      name: String,
      networkId: String,
      trigger: Trigger,
      actions: List[Action]
  )

  sealed trait Trigger extends EnumEntry {
    val fieldNames: Set[String]
  }
  object Trigger extends Enum[Trigger] with CirceEnum[Trigger] {
    case object TrPostCreated extends Trigger {
      val fieldNames = Set("title", "content")
    }
    case object TrSpaceCreated extends Trigger {
      val fieldNames = Set("spaceName")
    }

    def values = findValues

  }

  sealed trait Action
  object Action {
    case class AcSendNotifToAll(message: String) extends Action
    case class AcHttpPostRequest(url: String, jsonBody: String) extends Action
    case class AcIf(
        condition: Condition,
        elseBranch: Action,
        thenBranch: Option[Action]
    ) extends Action
  }

  sealed trait Condition
  object Condition {
    case class CdEq(fieldName: String, value: String) extends Condition
    case class CdAnd(left: Condition, right: Condition) extends Condition
    case class CdOr(left: Condition, right: Condition) extends Condition
    case class CdContains(fieldName: String, value: String) extends Condition
  }

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

  sealed trait DomainFailure
  object DomainFailure {
    case object ValidationError extends DomainFailure
    case object NotFound extends DomainFailure
  }

}

object DomainJsonSupport {
  import domain._
  import so.tribe.automation.Constants
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec
  import io.circe.Codec

  implicit val jsonConfig: Configuration =
    Configuration.default.withDiscriminator(Constants.SUMTYPE_TAG_KEY)

  implicit val automationCodec: Codec[Automation] =
    deriveConfiguredCodec[Automation]

  implicit val actionCodec: Codec[Action] =
    deriveConfiguredCodec[Action]

  implicit val condCodec: Codec[Condition] =
    deriveConfiguredCodec[Condition]

  implicit val createAutomationPayloadCodec: Codec[CreateAutomationPayload] =
    deriveConfiguredCodec[CreateAutomationPayload]

}

object DomainBsonSupport {
  import domain._
  import DomainJsonSupport._
  implicit val automationHandler = MongoUtils.genBsonHandler[Automation]
}

object AutomationDomainValidators {
  import com.wix.accord.dsl._
  import domain._
  import Action._
  import Condition._

  def isConditionValid(trigger: Trigger, condition: Condition): Boolean =
    condition match {
      case CdEq(fieldName, _) => trigger.fieldNames.contains(fieldName)
      case CdAnd(left, right) =>
        List(left, right).forall(isConditionValid(trigger, _))
      case CdOr(left, right) =>
        List(left, right).forall(isConditionValid(trigger, _))
      case CdContains(fieldName, _) =>
        trigger.fieldNames.contains(fieldName)
    }

  def isActionValid(trigger: Trigger, action: Action): Boolean = {
    action match {
      case AcIf(condition, elseBranch, thenBranch) =>
        List(
          isActionValid(trigger, elseBranch),
          isConditionValid(trigger, condition),
          thenBranch.forall(isActionValid(trigger, _))
        ).forall(identity)
      case _ => true
    }
  }

  implicit val createAutomationPayloadValidator =
    validator[domain.CreateAutomationPayload] { p =>
      p.name is notEmpty
      p.networkId is notEmpty
      p.actions is notEmpty
      p.actions.forall(isActionValid(p.trigger, _)) is true
    }
}
