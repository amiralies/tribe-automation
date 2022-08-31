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
import so.tribe.automation.slate.Block
import so.tribe.automation.slate.Slate
import so.tribe.automation.automation.DomainJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.interop.ErrorResponse
import akka.http.scaladsl.model.HttpResponse

trait WebhookRoutes {
  def routes: Route
}

case class WebhookRoutesImpl(
    configService: ConfigService,
    automationService: AutomationService
) extends WebhookRoutes
    with ZIOSupport
    with WebhookDTOsJsonSupport {

  implicit val domainFailureResponse: ErrorResponse[DomainFailure] = {
    case DomainFailure.NotFound        => HttpResponse(StatusCodes.NotFound)
    case DomainFailure.ValidationError => HttpResponse(StatusCodes.BadRequest)
  }

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

  def handleSubscriptionEvents(payload: WebhookPayload) = {
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

  private def loadBlock(networkId: String) = {
    import DomainJsonSupport._
    import io.circe.syntax._

    automationService
      .getNetworkAutomations(networkId)
      .map(automations => {

        val xml =
          <Card>
          <Card.Content>
            {
            automations.map(automation =>
              <Form callbackId="deleteAutomation" defaultValues={
                JsonObject("id" -> automation.id.asJson).asJson.noSpaces
              }>
                <Markdown text={s"""###${automation.name}"""}/>
                <Markdown text={s"""```\n${automation.asJson.spaces2}\n```"""}/>
                <Button type="submit" variant="danger" className="mt-3" autoDisabled="false">
                  <RawText value={s"""Remove "${automation.name}""""}/>
                </Button>
              </Form>
            )
          }
            <Form callbackId="addAutomation" defaultValues={
            """{"automation": ""}"""
          }>
              <Input
                name="automation"
                label="Automation Json"
                placeholder="Put automation json here"
              />
                <Button type="submit" leadingIcon="PlusIcon" variant="primary" className="mt-3">
                  <RawText value="Add Automation"/>
                </Button>
            </Form>
          </Card.Content>
        </Card>

        val block = Block.fromXml(xml)
        Block.toSlate(block)
      })

  }
  private def handleLoadBlock(networkId: String) = {
    val response = loadBlock(networkId).map(slate => {
      Json.obj(
        "type" -> Json.fromString("LOAD_BLOCK"),
        "status" -> Json.fromString("SUCCEEDED"),
        "data" -> Json.obj(
          "slate" -> Slate.toJson(slate)
        )
      )
    })

    complete(response)
  }

  def handleCallback(networkId: String, data: JsonObject) = {

    val callbackId = data("callbackId").flatMap(_.as[String].toOption)
    callbackId match {
      case Some("addAutomation") =>
        val maybeDto = data("inputs")
          .flatMap(_.asObject)
          .flatMap(_.apply("automation"))
          .flatMap(_.as[String].toOption)
          .flatMap(io.circe.parser.parse(_).toOption)
          .flatMap(_.as[CreateAutomationPayloadDTO].toOption)
        maybeDto match {
          case Some(dto) =>
            val payload = CreateAutomationPayload(
              networkId,
              dto.name,
              dto.trigger,
              dto.actions
            )
            complete(
              automationService
                .createAutomation(payload)
                .flatMap(_ => loadBlock(networkId))
                .map(slate => {
                  Json.obj(
                    "type" -> Json.fromString("Callback"),
                    "status" -> Json.fromString("SUCCEEDED"),
                    "data" -> Json.obj(
                      "slate" -> Slate.toJson(slate),
                      "action" -> Json.fromString("REPLACE")
                    )
                  )
                })
            )
          case None =>
            complete(StatusCodes.BadRequest)
        }

      case Some("deleteAutomation") =>
        val maybeId = data("inputs")
          .flatMap(_.asObject)
          .flatMap(_.apply("id"))
          .flatMap(_.as[String].toOption)
        maybeId match {
          case Some(id) =>
            complete(
              automationService
                .deleteAutomation(id)
                .flatMap(_ => loadBlock(networkId))
                .map(slate => {
                  Json.obj(
                    "type" -> Json.fromString("Callback"),
                    "status" -> Json.fromString("SUCCEEDED"),
                    "data" -> Json.obj(
                      "slate" -> Slate.toJson(slate),
                      "action" -> Json.fromString("REPLACE")
                    )
                  )
                })
            )
          case None =>
            complete(StatusCodes.BadRequest)
        }

      case None | Some(_) => complete(StatusCodes.BadRequest)
    }
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
                handleSubscriptionEvents(payload)
              case WebhookType.LOAD_BLOCK =>
                handleLoadBlock(payload.networkId)
              case WebhookType.Callback =>
                handleCallback(payload.networkId, payload.data)
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
