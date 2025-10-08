package com.jcotters.auth.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.sessionDataStore by preferencesDataStore(SessionManager.SESSION_DATA_STORE_NAME)

class SessionManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {

  internal companion object {
    const val SESSION_DATA_STORE_NAME = "session_prefs"
    const val USER_ID_KEY = "user_id"
    const val SESSION_EXPIRES_KEY = "expires_at"
    const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    const val ONE_DAY_MILLIS = 24 * ONE_HOUR_MILLIS
  }

  private val userIdKey = intPreferencesKey(USER_ID_KEY)
  private val expiresAtKey = longPreferencesKey(SESSION_EXPIRES_KEY)

  private val dataStore = context.sessionDataStore

  val sessionState: Flow<SessionState> = dataStore.data.map { prefs ->
    SessionState(
      userId = prefs[userIdKey],
      expiresAt = prefs[expiresAtKey] ?: 0L
    )
  }

  suspend fun createSession(userId: Int, durationMillis: Long = ONE_DAY_MILLIS) {
    val expiresAt = System.currentTimeMillis() + durationMillis
    dataStore.edit { prefs ->
      prefs[userIdKey] = userId
      prefs[expiresAtKey] = expiresAt
    }
  }

  suspend fun clearSession() {
    dataStore.edit(MutablePreferences::clear)
  }
}