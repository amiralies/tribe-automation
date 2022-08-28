package so.tribe.automation.automation

import domain._
import zio._

trait AutomationRepo {
  def insert(automation: Automation): UIO[Unit]

  def getAllByNetworkIdAndTrigger(
      networkId: String,
      trigger: Trigger
  ): UIO[List[Automation]]

}
