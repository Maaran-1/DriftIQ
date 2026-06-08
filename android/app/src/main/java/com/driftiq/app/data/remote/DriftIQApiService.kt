package com.driftiq.app.data.remote

import com.driftiq.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DriftIQApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AccessTokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    // Events
    @POST("events/batch")
    suspend fun postEventsBatch(@Body request: BatchEventsRequest): Response<BatchEventsResponse>

    // Dashboard
    @GET("dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

    // Drift
    @GET("drift/today")
    suspend fun getDriftToday(): Response<DriftTodayResponse>

    @GET("drift/history")
    suspend fun getDriftHistory(@Query("days") days: Int = 30): Response<DriftHistoryResponse>

    // Risk
    @GET("risk/current")
    suspend fun getCurrentRisk(): Response<RiskCurrentResponse>

    @GET("risk/history")
    suspend fun getRiskHistory(@Query("days") days: Int = 30): Response<RiskHistoryResponse>

    // Insights
    @GET("insights/daily")
    suspend fun getDailyInsight(): Response<DailyInsightResponse>

    @GET("insights/weekly")
    suspend fun getWeeklyInsight(): Response<WeeklyInsightResponse>

    @GET("insights/monthly")
    suspend fun getMonthlyInsight(): Response<WeeklyInsightResponse>

    // Twin
    @GET("twin/status")
    suspend fun getTwinStatus(): Response<TwinStatusResponse>

    @POST("twin/reset")
    suspend fun resetTwin(): Response<Unit>

    // Settings
    @PATCH("settings")
    suspend fun updateSettings(@Body request: UpdateSettingsRequest): Response<Unit>

    @POST("settings/export")
    suspend fun exportData(): Response<Map<String, String>>

    @DELETE("settings/account")
    suspend fun deleteAccount(@Body body: Map<String, Any>): Response<Unit>
}
