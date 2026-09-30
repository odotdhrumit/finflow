package com.example.util

import java.security.MessageDigest

object SecurityHelper {

    private const val SALT = "FinFlowSecureSalt#2026"

    fun hashPin(pin: String): String {
        val input = "$pin$SALT"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        if (storedHash.isEmpty()) return true
        val computed = hashPin(enteredPin)
        return computed == storedHash
    }
}
