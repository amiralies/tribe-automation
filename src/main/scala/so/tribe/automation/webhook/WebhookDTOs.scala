package so.tribe.automation.webhook

import enumeratum.{Enum, EnumEntry, CirceEnum}
import io.circe.JsonObject
import io.circe.generic.extras.JsonKey
import io.circe.Json
import so.tribe.automation.automation.domain
import so.tribe.automation.Constants

object WebhookDTOs {
  sealed trait WebhookType extends EnumEntry
  object WebhookType extends Enum[WebhookType] with CirceEnum[WebhookType] {
    case object TEST extends WebhookType
    case object SUBSCRIPTION extends WebhookType
    case object LOAD_BLOCK extends WebhookType
    case object Callback extends WebhookType

    def values = findValues
  }

  case class WebhookPayload(
      networkId: String,
      @JsonKey("type") typ: WebhookType,
      data: JsonObject,
      currentSettings: Option[Json]
  )

  case class PostPublishedData(
      title: String,
      shortContent: String,
      isReply: Boolean
  )
  case class SpaceCreatedData(name: String)

  case class CreateAutomationPayloadDTO(
      name: String,
      trigger: domain.Trigger,
      actions: List[domain.Action]
  )
}

trait WebhookDTOsJsonSupport {
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec
  import io.circe.Codec
  import WebhookDTOs._
  import so.tribe.automation.automation.DomainJsonSupport._


  implicit val webhookPayloadCodec: Codec[WebhookPayload] =
    deriveConfiguredCodec[WebhookPayload]

  implicit val postPublishedDataCodec: Codec[PostPublishedData] =
    deriveConfiguredCodec[PostPublishedData]

  implicit val spaceCreatedDataCodec: Codec[SpaceCreatedData] =
    deriveConfiguredCodec[SpaceCreatedData]

  implicit val createAutomationPayloadDTOCodec: Codec[CreateAutomationPayloadDTO] =
    deriveConfiguredCodec[CreateAutomationPayloadDTO]
}
