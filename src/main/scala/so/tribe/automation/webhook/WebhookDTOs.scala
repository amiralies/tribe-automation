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
      data: Option[JsonObject]
  )
  case class ChallengeData(challenge: String)

  case class TestSuccess(
      @JsonKey("type") typ: String,
      status: String,
      data: ChallengeData
  )

}

trait WebhookDTOsJsonSupport {
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec
  import io.circe.Codec
  import WebhookDTOs._

  implicit val jsonConfig: Configuration = Configuration.default

  implicit val challengeDataCodec: Codec[ChallengeData] =
    deriveConfiguredCodec[ChallengeData]

  implicit val testSuccessCodec: Codec[TestSuccess] =
    deriveConfiguredCodec[TestSuccess]

  implicit val webhookPayloadCodec: Codec[WebhookPayload] =
    deriveConfiguredCodec[WebhookPayload]
}
