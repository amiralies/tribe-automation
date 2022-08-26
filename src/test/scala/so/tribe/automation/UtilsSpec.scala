package so.tribe.automation

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

class UtilsSpec extends AnyFlatSpec with Matchers {

  it should "Generate correct hmacsha256 #1" in {
    val hmac = Utils.genHmacSHA256Hex(
      "key",
      "The quick brown fox jumps over the lazy dog"
    )
    hmac shouldBe "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8"
  }

  it should "Generate correct hmacsha256 #2" in {
    val hmac = Utils.genHmacSHA256Hex(
      "supersecretkey",
      "a7842e39796e1e0ed07ded97f2eb51316ee923c2"
    )
    hmac shouldBe "04686ecc1665cdf7f420efa4245597f105687ef068d2981264787182e5a8f785"
  }
}
