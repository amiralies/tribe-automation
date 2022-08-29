package so.tribe.automation

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import com.wix.accord._
import zio._

object Utils {

  def genHmacSHA256Hex(secret: String, data: String): String = {
    val algorithm = "HmacSHA256"
    val secretKeySpec = new SecretKeySpec(secret.getBytes, algorithm);
    val mac = Mac.getInstance(algorithm)
    mac.init(secretKeySpec)
    bytesToHex(mac.doFinal(data.getBytes))
  }

  private def bytesToHex(bytes: Array[Byte]) =
    bytes.map("%02x".format(_)).mkString

  def timeSafeEq(left: String, right: String) =
    MessageDigest.isEqual(left.getBytes, right.getBytes)

  // Secure random uuid (base64 encoded based on rfc4648) without padding
  def genUrlSafeUUID(): String =
    DatatypeConverter
      .printBase64Binary(
        DatatypeConverter
          .parseHexBinary(java.util.UUID.randomUUID().toString.replace("-", ""))
      )
      .replace("/", "_")
      .replace("=", "")
      .replace("+", "-")

  def stripHtml(htmlString: String) = scala.xml.XML.loadString(htmlString).text

  def zioValidate[T](
      value: T
  )(implicit validator: Validator[T]): IO[Unit, Unit] =
    validate(value) match {
      case Failure(violations) =>
        ZIO.fail()
      case Success => ZIO.succeed()
    }

  def mustachePatternForField(fieldName: String) =
    s"""\\{\\{${fieldName}\\}\\}""".r

}
