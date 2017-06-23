package com.fkorotkov.github.apps.signing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.apache.commons.io.FileUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import org.joda.time.DateTime
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

class GithubSecrets(
  val algorithm: Algorithm
) {
  companion object {
    fun initialize(): GithubSecrets {
      Security.addProvider(BouncyCastleProvider())

      val byteArrayInputStream = ByteArrayInputStream(readFileFromEnvVariable("GITHUB_PRIVATE_KEY_FILE"))
      val reader = PemReader(InputStreamReader(byteArrayInputStream))
      val keySpec = PKCS8EncodedKeySpec(reader.readPemObject().content)
      val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey

      return GithubSecrets(Algorithm.RSA256(null, privateKey))
    }

    private fun readFileFromEnvVariable(envName: String): ByteArray {
      val filePath: String = System.getenv()[envName]
        ?: throw IllegalStateException("$envName env variable should be presented in order to run the app!")
      val file = File(filePath)
      if (!file.exists()) {
        throw IllegalStateException("File $filePath doesn't exist!!")
      }
      return FileUtils.readFileToByteArray(file)
    }
  }

  fun sign(issuer: String): String {
    return JWT.create()
      .withIssuedAt(DateTime.now().toDate())
      .withExpiresAt(DateTime.now().plusMinutes(10).toDate())
      .withIssuer(issuer)
      .sign(algorithm)
  }
}
