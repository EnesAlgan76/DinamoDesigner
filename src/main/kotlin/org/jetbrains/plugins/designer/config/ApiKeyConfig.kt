package org.jetbrains.plugins.designer.config

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object ApiKeyConfig {

    private const val ENCRYPTED_API_KEY = "AAS7kL5gC2/+WIXOetVZuteNLw0OddHAq4CRwKp9DWM3QbYXjgUsW+ylbMNBhvVwRjVkUDIklQLAx0GEhyXy5Ihw9nz1KtfaeTh+FIKST0TnkU0l6Y1GUDqpBl7YZ2t3oZym6bMJWad5qvYM8SNtV724YxS7twYlkzB0iDBuDpD8="

    private const val ENCRYPTION_KEY = "ensalgn761234567"

    fun getApiKey(): String {
        return try {
            " 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"
         //   decrypt(ENCRYPTED_API_KEY, ENCRYPTION_KEY)
        } catch (e: Exception) {
            throw IllegalStateException("API key configuration error", e)
        }
    }

    private fun decrypt(encryptedData: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
        return String(decryptedBytes)
    }


    const val DEFAULT_MODEL = "gpt-4o-mini"
}
