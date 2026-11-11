package com.oxywatt.diagnostic.persistence

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** UTF-8 byte lengths, explicit null/type tokens and lexicographic field order, codec v1. */
internal class PowerReplayHash {
    private val digest = MessageDigest.getInstance("SHA-256")

    init {
        token("power-replay-hash-v1")
    }

    fun token(value: String?) {
        if (value == null) digest.update("N;".toByteArray(Charsets.UTF_8))
        else {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update("S${bytes.size}:".toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
    }

    fun document(value: Any?) {
        when (value) {
            null,
            JSONObject.NULL -> token(null)
            is JSONObject -> {
                token("object")
                val keys = value.keys().asSequence().toList().sorted()
                token(keys.size.toString())
                keys.forEach {
                    token(it)
                    document(value.get(it))
                }
            }
            is JSONArray -> {
                token("array")
                token(value.length().toString())
                for (i in 0 until value.length()) document(value.get(i))
            }
            is Number -> {
                token("number")
                token(value.toString())
            }
            is Boolean -> {
                token("boolean")
                token(value.toString())
            }
            is String -> {
                token("string")
                token(value)
            }
            else -> error("Unsupported hash field")
        }
    }

    fun finish(): String = digest.digest().joinToString("") { "%02x".format(it) }
}
