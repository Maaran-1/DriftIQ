package com.driftiq.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driftiq_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_CONSENT_GIVEN = booleanPreferencesKey("consent_given")
        private val KEY_USAGE_PERMISSION_GRANTED = booleanPreferencesKey("usage_permission")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }
    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }
    val isConsentGiven: Flow<Boolean> = context.dataStore.data.map { it[KEY_CONSENT_GIVEN] ?: false }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[KEY_DEVICE_ID] }

    suspend fun saveTokens(accessToken: String, refreshToken: String, userId: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setConsentGiven(given: Boolean) {
        context.dataStore.edit { it[KEY_CONSENT_GIVEN] = given }
    }

    suspend fun setDeviceId(id: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = id }
    }
}
