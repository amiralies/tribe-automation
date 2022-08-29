package so.tribe.automation.webhook

import akka.http.scaladsl.server.Directives._
import zio._
import akka.http.scaladsl.server.Route
import so.tribe.automation.config.ConfigService
import io.circe.{Json, JsonObject}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import so.tribe.automation.akkahttp.TribeSignatureDirective
import WebhookDTOs._
import so.tribe.automation.automation.AutomationService
import so.tribe.automation.automation.domain._
import akka.http.interop.ZIOSupport
import so.tribe.automation.Utils

trait WebhookRoutes {
  def routes: Route
}

case class WebhookRoutesImpl(
    configService: ConfigService,
    automationService: AutomationService
) extends WebhookRoutes
    with ZIOSupport
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
            payload.typ match {
              case WebhookType.TEST =>
                solveChallenge(payload.data)

              case WebhookType.SUBSCRIPTION =>
                val maybeWebhookName =
                  payload
                    .data("name")
                    .flatMap(_.as[String].toOption)
                maybeWebhookName match {
                  case Some("space.created") =>
                    val data = payload
                      .data("object")
                      .flatMap(_.as[SpaceCreatedData].toOption)
                      .get

                    val event = Event(
                      payload.networkId,
                      EventDesc.EvSpaceCreated(data.name)
                    )

                    val effect = automationService.handleEvent(event)
                    complete(effect.map(_ => "ok"))

                  case Some("post.published") =>
                    val data = payload
                      .data("object")
                      .flatMap(_.as[PostPublishedData].toOption)
                      .get

                    val event = Event(
                      payload.networkId,
                      EventDesc.EvPostCreated(
                        data.title,
                        Utils.stripHtml(data.shortContent)
                      )
                    )

                    val effect =
                      if (!data.isReply) automationService.handleEvent(event)
                      else ZIO.succeed()

                    complete(effect.map(_ => "ok"))
                  case None | Some(_) => complete("ok")
                }

            }

          }
        }
      }
    }

}

object WebhookRoutesImpl {
  val layer: URLayer[ConfigService with AutomationService, WebhookRoutes] =
    ZLayer.fromFunction {
      (WebhookRoutesImpl(_, _))
    }
}
