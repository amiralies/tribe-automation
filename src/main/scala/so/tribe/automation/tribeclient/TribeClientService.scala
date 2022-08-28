package so.tribe.automation.tribeclient

import zio._

trait TribeClientService {
  def sendNotifToAll(networkId: String, message: String): UIO[Unit]
}

case class TribeClientServiceImpl() extends TribeClientService {
  override def sendNotifToAll(networkId: String, message: String): UIO[Unit] = {
    ZIO.logInfo(s"""sending notification "$message" to networkId: $networkId""")
  }
}

object TribeClientServiceImpl {
  val layer = ZLayer.succeed(TribeClientServiceImpl())
}
