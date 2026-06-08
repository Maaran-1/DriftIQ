package com.driftiq.app.data.repository

import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.data.remote.DriftIQApiService
import com.driftiq.app.data.remote.dto.LoginRequest
import com.driftiq.app.data.remote.dto.LogoutRequest
import com.driftiq.app.data.remote.dto.RefreshRequest
import com.driftiq.app.data.remote.dto.RegisterRequest
import com.driftiq.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: DriftIQApiService,
    private val dataStore: UserPreferencesDataStore,
) : AuthRepository {

    override suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        val response = api.register(RegisterRequest(email, password))
        if (!response.isSuccessful) {
            val errorMsg = when (response.code()) {
                409 -> "Email already registered"
                422 -> "Invalid email or password"
                else -> "Registration failed (${response.code()})"
            }
            throw Exception(errorMsg)
        }
        val body = response.body() ?: throw Exception("Empty response")
        dataStore.saveTokens(body.accessToken, body.refreshToken, "", email)
    }

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequest(email, password))
        if (!response.isSuccessful) {
            val errorMsg = when (response.code()) {
                401 -> "Invalid email or password"
                else -> "Login failed (${response.code()})"
            }
            throw Exception(errorMsg)
        }
        val body = response.body() ?: throw Exception("Empty response")
        dataStore.saveTokens(body.accessToken, body.refreshToken, "", email)
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        val refreshToken = dataStore.refreshToken.first() ?: return@runCatching
        api.logout(LogoutRequest(refreshToken))
        dataStore.clearTokens()
    }

    override suspend fun refreshToken(): Result<Unit> = runCatching {
        val rt = dataStore.refreshToken.first() ?: throw Exception("No refresh token")
        val response = api.refresh(RefreshRequest(rt))
        if (!response.isSuccessful) throw Exception("Token refresh failed")
        val body = response.body() ?: throw Exception("Empty response")
        val currentEmail = dataStore.userEmail.first() ?: ""
        dataStore.saveTokens(body.accessToken, rt, "", currentEmail)
    }

    override fun isLoggedIn(): Boolean = false // Use DataStore flow
}
