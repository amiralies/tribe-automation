package so.tribe.automation.webhook

import enumeratum.{Enum, EnumEntry, CirceEnum}
import io.circe.JsonObject
import io.circe.generic.extras.JsonKey

object WebhookDTOs {
  sealed trait WebhookType extends EnumEntry
  object WebhookType extends Enum[WebhookType] with CirceEnum[WebhookType] {
    case object TEST extends WebhookType
    case object SUBSCRIPTION extends WebhookType

    def values = findValues
  }

  case class WebhookPayload(
      networkId: String,
      @JsonKey("type") typ: WebhookType,
      data: JsonObject
  )

  case class PostPublishedData(
      title: String,
      shortContent: String,
      isReply: Boolean
  )
  case class SpaceCreatedData(name: String)

}

trait WebhookDTOsJsonSupport {
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec
  import io.circe.Codec
  import WebhookDTOs._

  implicit val jsonConfig: Configuration = Configuration.default

  implicit val webhookPayloadCodec: Codec[WebhookPayload] =
    deriveConfiguredCodec[WebhookPayload]

  implicit val postPublishedDataCodec: Codec[PostPublishedData] =
    deriveConfiguredCodec[PostPublishedData]

  implicit val spaceCreatedDataCodec: Codec[SpaceCreatedData] =
    deriveConfiguredCodec[SpaceCreatedData]
}
