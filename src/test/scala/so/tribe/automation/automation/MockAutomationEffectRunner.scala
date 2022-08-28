package so.tribe.automation.automation

import zio._

class MockAutomationEffectRunner extends AutomationEffectRunner {
  override def runEffects(runEffectsEvent: domain.RunEffectsEvent): UIO[Unit] =
    ZIO.succeed()

}
