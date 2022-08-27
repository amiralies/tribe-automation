package so.tribe.automation

import akka.http.scaladsl.server.Route
import so.tribe.automation.webhook.WebhookRoutes
import zio._
import akka.http.scaladsl.server.Directives._
import so.tribe.automation.healthcheck.HealthCheckRoutes
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.model.StatusCodes
import akka.http.interop.ZIOSupport

trait AppRouter {
  def routes: Route
}

case class AppRouterImpl(
    webhookRoutes: WebhookRoutes,
    healthCheckRoutes: HealthCheckRoutes
) extends AppRouter
    with ZIOSupport {
  val rawRoutes = concat(
    healthCheckRoutes.routes,
    webhookRoutes.routes
  )
  override def routes: Route = {

    implicit def exceptionHandler: ExceptionHandler = {
      ExceptionHandler { case e: Throwable =>
        complete(
          ZIO.logError(e.getMessage()).map(_ => StatusCodes.InternalServerError)
        )
      }
    }

    Route.seal(rawRoutes)
  }
}

object AppRouterImpl {
  val layer = ZLayer.fromFunction(AppRouterImpl(_, _))
}
