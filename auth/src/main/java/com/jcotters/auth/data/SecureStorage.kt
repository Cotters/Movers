package com.jcotters.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class AuthStorageCredentials(
  val salt: String?,
  val hash: String?,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SecureStorage.AUTH_PREFERENCES_NAME)

class SecureStorage @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {

  internal companion object {
    const val AUTH_PREFERENCES_NAME = "auth_prefs"
    const val SALT_PREF_NAME = "salt_pref"
    const val HASH_PREF_NAME = "hash_pref"
  }

  suspend fun saveCredentials(username: String, saltHex: String, hashHex: String) {
    val saltPrefKey = createSaltKey(username)
    val hashPrefKey = createHashKey(username)
    context.dataStore.edit { prefs ->
      prefs[saltPrefKey] = saltHex
      prefs[hashPrefKey] = hashHex
    }
  }

  suspend fun loadCredentials(username: String): AuthStorageCredentials {
    val saltPrefKey = createSaltKey(username)
    val hashPrefKey = createHashKey(username)
    val prefs = context.dataStore.data.first()
    return AuthStorageCredentials(salt = prefs[saltPrefKey], hash = prefs[hashPrefKey])
  }

  private fun createSaltKey(username: String): Preferences.Key<String> {
    return stringPreferencesKey("${username}_$SALT_PREF_NAME")
  }

  private fun createHashKey(username: String): Preferences.Key<String> {
    return stringPreferencesKey("${username}_$HASH_PREF_NAME")
  }
}