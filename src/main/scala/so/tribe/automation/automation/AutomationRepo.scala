package so.tribe.automation.automation

import domain._
import zio._

trait AutomationRepo {
  def insert(automation: Automation): UIO[Unit]
}
