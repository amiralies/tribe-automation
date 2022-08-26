package so.tribe.automation

import akka.http.scaladsl.server.Route
import so.tribe.automation.webhook.WebhookRoutes
import zio._
import akka.http.scaladsl.server.Directives._
import so.tribe.automation.healthcheck.HealthCheckRoutes

trait AppRouter {
  def routes: Route
}

case class AppRouterImpl(
    webhookRoutes: WebhookRoutes,
    healthCheckRoutes: HealthCheckRoutes
) extends AppRouter {
  override def routes = concat(
    healthCheckRoutes.routes,
    webhookRoutes.routes
  )
}

object AppRouterImpl {
  val layer = ZLayer.fromFunction(AppRouterImpl(_, _))
}
