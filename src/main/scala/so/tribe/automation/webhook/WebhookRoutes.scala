package so.tribe.automation.webhook

import akka.http.scaladsl.server.Directives._
import zio._
import akka.http.scaladsl.server.Route
import so.tribe.automation.config.ConfigService
import io.circe.{Json, JsonObject}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import so.tribe.automation.akkahttp.TribeSignatureDirective
import WebhookDTOs._

trait WebhookRoutes {
  def routes: Route
}

case class WebhookRoutesImpl(configService: ConfigService)
    extends WebhookRoutes
    with WebhookDTOsJsonSupport {

  private def solveChallenge(data: JsonObject) = {
    val challenge = data("challenge")
      .flatMap(_.as[String].toOption)
      .get

    val response = Json.obj(
      "type" -> Json.fromString("TEST"),
      "status" -> Json.fromString("SUCCEEDED"),
      "data" -> Json.obj(
        "challenge" -> Json.fromString(challenge)
      )
    )

    complete(response)
  }

  override def routes =
    path("webhooks") {
      TribeSignatureDirective.verifyWebhookRequest(
        configService.access.tribe.clientSecret
      ) {
        post {
          entity(as[WebhookPayload]) { payload =>
            println(payload)
            payload.typ match {
              case WebhookType.TEST =>
                solveChallenge(payload.data.get)
              case WebhookType.SUBSCRIPTION =>
                complete("ok")
            }

          }
        }
      }
    }

}

object WebhookRoutesImpl {
  val layer: URLayer[ConfigService, WebhookRoutes] = ZLayer.fromFunction {
    (WebhookRoutesImpl(_))
  }
}
