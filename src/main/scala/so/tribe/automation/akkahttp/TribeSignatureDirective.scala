package so.tribe.automation.akkahttp

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Route
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.RejectionHandler
import so.tribe.automation.Utils

private case class TribeReqVerificationData(
    signature: String,
    timestamp: Long,
    body: String
)

object TribeSignatureDirective {
  private val extractVerificationData = for {
    maybeSignature <- optionalHeaderValueByName("x-tribe-signature")
    maybeTimestamp <- optionalHeaderValueByName("x-tribe-request-timestamp")
    body <- entity(as[String])
  } yield parseData(
    maybeTimestamp = maybeTimestamp,
    maybeSignature = maybeSignature,
    body = body
  )

  private def genSignature(secret: String, body: String, timestamp: Long) =
    Utils.genHmacSHA256Hex(secret, s"${timestamp}:${body}")

  private def isValidReq(
      clientSecret: String,
      reqData: TribeReqVerificationData
  ): Boolean = {
    import scala.concurrent.duration._

    val now = System.currentTimeMillis()
    val timeDiff = (now - reqData.timestamp).millis
    val calculatedSig = genSignature(
      secret = clientSecret,
      body = reqData.body,
      timestamp = reqData.timestamp
    )
    val isFreshReq = timeDiff <= 5.minutes
    val hasValidSignature = Utils.timeSafeEq(reqData.signature, calculatedSig)

    isFreshReq && hasValidSignature
  }

  private def parseData(
      maybeSignature: Option[String],
      maybeTimestamp: Option[String],
      body: String
  ) = for {
    signature <- maybeSignature
    timestamp <- maybeTimestamp.flatMap(_.toLongOption)
  } yield TribeReqVerificationData(
    signature = signature,
    timestamp = timestamp,
    body = body
  )

  def verifyWebhookRequest(clientSecret: String) = new Directive0 {
    override def tapply(f: Unit => Route): Route =
      extractVerificationData { maybeData =>
        maybeData match {
          case Some(data) if isValidReq(clientSecret, data) => f()
          case None | Some(_) => complete(StatusCodes.Forbidden)
        }
      }
  }
}
