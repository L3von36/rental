package com.loypos.app.core

import java.security.MessageDigest
import java.security.SecureRandom

object AuthCrypto {
    const val PASSWORD_ROUNDS = 5000

    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = "$salt::$password".toByteArray(Charsets.UTF_8)
        repeat(PASSWORD_ROUNDS) {
            bytes = digest.digest(bytes)
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun safeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun isValidEmail(email: String): Boolean {
        val e = email.trim()
        return e.contains("@") && e.contains(".") && !e.startsWith("@") && e.length >= 5
    }
}
