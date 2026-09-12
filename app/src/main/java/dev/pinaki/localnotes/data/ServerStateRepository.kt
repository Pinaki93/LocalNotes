package dev.pinaki.localnotes.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class ServerStateRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hasPassword(): Boolean = preferences.contains(KEY_PASSWORD_HASH) &&
        preferences.contains(KEY_PASSWORD_SALT)

    fun setPassword(password: String) {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = deriveKey(password, salt)
        preferences.edit()
            .putString(KEY_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val salt = preferences.getString(KEY_PASSWORD_SALT, null)?.decodeBase64() ?: return false
        val expected = preferences.getString(KEY_PASSWORD_HASH, null)?.decodeBase64() ?: return false
        return MessageDigest.isEqual(expected, deriveKey(password, salt))
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun String.decodeBase64(): ByteArray? = runCatching {
        Base64.decode(this, Base64.NO_WRAP)
    }.getOrNull()

    private companion object {
        const val PREFERENCES = "server_state"
        const val KEY_ENABLED = "enabled"
        const val KEY_PASSWORD_SALT = "password_salt"
        const val KEY_PASSWORD_HASH = "password_hash"
        const val SALT_BYTES = 16
        const val HASH_BITS = 256
        const val PBKDF2_ITERATIONS = 210_000
    }
}
