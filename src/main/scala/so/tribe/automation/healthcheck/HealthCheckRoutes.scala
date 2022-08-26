package so.tribe.automation.healthcheck

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import zio._

trait HealthCheckRoutes {
  val routes: Route
}

case class HealthCheckRoutesImpl() extends HealthCheckRoutes {

  override val routes: Route =
    path("healthcheck") {
      get {
        complete("Ok")
      }
    }

}

object HealthCheckRoutesImpl {
  val layer = ZLayer.succeed(HealthCheckRoutesImpl())
}
