package so.tribe.automation.automation

import zio._
import domain._
import so.tribe.automation.tribeclient.TribeClientService
import so.tribe.automation.automation.domain.Effect._
import sttp.client3._
import sttp.model.Header
import sttp.model.MediaType

trait AutomationEffectRunner {
  def runEffects(runEffectsEvent: RunEffectsEvent): UIO[Unit]
}

case class AutomationEffectRunnerImpl(
    tribeClientService: TribeClientService,
    sttpBackend: SttpBackend[Task, Any]
) extends AutomationEffectRunner {

  private def runHttpPostRequestEffect(effect: EffHttpPostRequest): UIO[Unit] =
    basicRequest
      .headers(Header.contentType(MediaType.ApplicationJson))
      .body(effect.jsonBody)
      .post(uri"${effect.url}")
      .send(sttpBackend)
      .orDie
      .map(_ => ())

  private def runSendNotifToAllEffect(
      networkId: String,
      effect: EffSendNotifToAll
  ) = tribeClientService.sendNotifToAll(networkId, effect.message)

  override def runEffects(runEffectsEvent: RunEffectsEvent): UIO[Unit] =
    ZIO.foreachParDiscard(runEffectsEvent.effects) {
      case e @ EffSendNotifToAll(_) =>
        runSendNotifToAllEffect(runEffectsEvent.networkId, e)
      case e @ EffHttpPostRequest(_, _) =>
        runHttpPostRequestEffect(e)
    }

}

object AutomationEffectRunnerImpl {
  val layer = ZLayer.fromFunction(AutomationEffectRunnerImpl(_, _))
}
