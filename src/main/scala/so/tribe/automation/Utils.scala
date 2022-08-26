package so.tribe.automation

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import java.security.MessageDigest

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

}
