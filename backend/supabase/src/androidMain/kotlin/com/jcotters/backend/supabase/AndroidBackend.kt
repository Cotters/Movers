package com.jcotters.backend.supabase

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

fun createAndroidBackend(context: Context, url: String, key: String): SupabaseBackend =
    SupabaseBackend(url, key, AndroidCredentials(context.applicationContext))

private class AndroidCredentials(context: Context) : SecureCredentialStore {
    private val preferences = context.getSharedPreferences("movers.sharing", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("movers.sharing.session", null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(
                    KeyGenParameterSpec.Builder(
                        "movers.sharing.session",
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
                )
                generateKey()
            }
    }

    override suspend fun read(): String? {
        val value = preferences.getString("session", null)
        check(value != null || !preferences.getBoolean("claimed", false)) { "Saved sharing identity is missing." }
        if (value == null) return null
        val parts = value.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    override suspend fun write(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        check(
            preferences.edit().putBoolean("claimed", true).putString(
                "session",
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )
            ).commit()
        )
    }

    override suspend fun clear() {
        check(preferences.edit().remove("session").commit())
    }
}
