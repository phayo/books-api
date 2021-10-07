import org.apache.commons.codec.binary.Base64
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.engines.RijndaelEngine
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.params._

import scala.util.Try

class EncryptionUtil(keyBase64: String, ivBase64: String) {
  private val keyBytes = Base64.decodeBase64(keyBase64.getBytes) // "WhateverTheKeyIs".getBytes
  private val ivBytes =  Base64.decodeBase64(ivBase64.getBytes)//"WhateverTheIVVIs".getBytes

  def encrypt(message: String): String = {
    val cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine(128)), new PKCS7Padding())
    val keySize = keyBytes.length
    val ivAndKey = new ParametersWithIV(new KeyParameter(keyBytes, 0, keySize), ivBytes, 0, ivBytes.size)
    cipher.init(true, ivAndKey)
    val messageBytes = message.getBytes("UTF-8")
    val encrypted  = new Array[Byte](cipher.getOutputSize(messageBytes.length))
    val oLen = cipher.processBytes(messageBytes, 0, messageBytes.length, encrypted, 0)
    cipher.doFinal(encrypted, oLen)
    new String(Base64.encodeBase64(encrypted))
  }

  def decrypt(inputBase64: String): Try[String] = Try{
    val cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine(128)), new PKCS7Padding())
    val keySize = keyBytes.length
    val ivAndKey = new ParametersWithIV(new KeyParameter(keyBytes, 0, keySize), ivBytes, 0, ivBytes.size)
    cipher.init(false, ivAndKey)
    val messageBytes = Base64.decodeBase64(inputBase64.getBytes)
    val decrypted  = new Array[Byte](cipher.getOutputSize(messageBytes.length))
    val oLen = cipher.processBytes(messageBytes, 0, messageBytes.length, decrypted, 0)
    cipher.doFinal(decrypted, oLen)

    val zeroTerminationIndex = decrypted.indexOf(0)
    new String(decrypted, 0, zeroTerminationIndex, "UTF-8")
  }
}

object EncryptionUtil {
  def apply(keyBase64: String, ivBase64: String) = new EncryptionUtil(keyBase64, ivBase64)
}
