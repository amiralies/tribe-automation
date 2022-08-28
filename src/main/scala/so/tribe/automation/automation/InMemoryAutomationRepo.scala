package so.tribe.automation.automation

import zio._
import domain._

case class InMemoryAutomationRepo(mapRef: Ref[Map[String, Automation]])
    extends AutomationRepo {
  override def insert(automation: domain.Automation): UIO[Unit] =
    mapRef.update(_ + (automation.id -> automation))

}

object InMemoryAutomationRepo {
  val layer = ZLayer {
    for {
      mapRef <- Ref.make[Map[String, Automation]](Map())
    } yield InMemoryAutomationRepo(mapRef)
  }
}
