import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base64
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SecurityFilterTest extends SecurityFilter with AnyWordSpecLike with MockFactory with Matchers with BeforeAndAfterAll {
  private val keyConfig = ConfigFactory.load("application.conf").getConfig("encryption")
  private val authValid = ConfigFactory.load("application.conf").getInt("authValidInMinutes")
  private val encryptKeyBase64 = keyConfig.getString("key")
  private val encryptIVBase64 = keyConfig.getString("iv")
  private val separator = ConfigFactory.load("application.conf").getString("authSeparator")
  private val encryptUtil = EncryptionUtil(encryptKeyBase64, encryptIVBase64)

  "LoginValid method" should {
    "return true" when {
      "valid auth is provided" in {
        val result = encryptUtil.encrypt("chuk" + separator + System.currentTimeMillis() + (authValid * 60 * 1000))
        loginValid(result) shouldBe true
      }
    }

    "return false" when {
      "time is expired" in {
        val auth = encryptUtil.encrypt("chuk" + separator + System.currentTimeMillis())
        loginValid(auth) shouldBe false
      }

      "wrong encryption key used" in {
        val encryptUtil = EncryptionUtil(new String(Base64.encodeBase64("NewKeyIsHereNoww".getBytes())), encryptIVBase64)
        val wrongAuth = encryptUtil.encrypt("chuk:" + separator + System.currentTimeMillis() + authValid * 60 * 1000)
        loginValid(wrongAuth) shouldBe false
      }
    }
  }
}
