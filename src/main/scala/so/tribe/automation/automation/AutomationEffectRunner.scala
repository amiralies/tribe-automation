package so.tribe.automation.automation

import zio._
import domain._

trait AutomationEffectRunner {
  def runEffects(runEffectsEvent: RunEffectsEvent): UIO[Unit]
}

case class AutomationEffectRunnerImpl() extends AutomationEffectRunner {
  override def runEffects(runEffectsEvent: RunEffectsEvent): UIO[Unit] = {
    ZIO.succeed()
  }
}

object AutomationEffectRunnerImpl {
  val layer = ZLayer.succeed(AutomationEffectRunnerImpl())
}
