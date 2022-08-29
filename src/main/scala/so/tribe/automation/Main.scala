package so.tribe.automation

import zio._
import akka.actor.ActorSystem
import akka.http.interop.HttpServer
import so.tribe.automation.config.ConfigService
import so.tribe.automation.config.ConfigServiceImpl
import so.tribe.automation.config.HttpConfig
import so.tribe.automation.healthcheck.HealthCheckRoutesImpl
import so.tribe.automation.webhook.WebhookRoutesImpl
import so.tribe.automation.automation.AutomationServiceImpl
import so.tribe.automation.automation.InMemoryAutomationRepo
import so.tribe.automation.automation.AutomationEffectRunnerImpl
import so.tribe.automation.tribeclient.TribeClientServiceImpl
import sttp.client3.httpclient.zio.HttpClientZioBackend
import so.tribe.automation.persist.mongo.MongoCollectionsHolderImpl
import so.tribe.automation.automation.MongoAutomationRepo

object Main extends ZIOAppDefault {
  val httpServerLayer = {
    val actorSystemLayer = ZLayer.scoped(
      ZIO.acquireRelease(
        ZIO.attempt(ActorSystem("TribeAutomationActorSystem"))
      )(sys =>
        ZIO
          .fromFuture(_ => sys.terminate())
          .either
      )
    )

    val routeLayer = ZLayer.service[AppRouter].project(_.routes)

    val httpConfigLayer = ZLayer
      .service[ConfigService]
      .project(configService =>
        HttpServer.Config(
          configService.access.http.host,
          configService.access.http.port
        )
      )

    (actorSystemLayer ++ routeLayer ++ httpConfigLayer) >>> HttpServer.live
  }

  def startServer = ZIO.scoped {
    for {
      serverBinding <- HttpServer.start
      () <- ZIO.logInfo(s"Server started at $serverBinding")
      _ <- ZIO.never
    } yield ()
  }

  override def run =
    startServer.provide(
      ConfigServiceImpl.layer,
      WebhookRoutesImpl.layer,
      HealthCheckRoutesImpl.layer,
      AppRouterImpl.layer,
      AutomationServiceImpl.layer,
      MongoCollectionsHolderImpl.layer,
      MongoAutomationRepo.layer,
      AutomationEffectRunnerImpl.layer,
      TribeClientServiceImpl.layer,
      HttpClientZioBackend.layer(),
      httpServerLayer
    )
}
