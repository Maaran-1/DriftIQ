package com.driftiq.app.di

import android.content.Context
import androidx.room.Room
import com.driftiq.app.BuildConfig
import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.data.local.db.DriftIQDatabase
import com.driftiq.app.data.local.db.dao.AppUsageEventDao
import com.driftiq.app.data.local.db.dao.CachedDashboardDao
import com.driftiq.app.data.local.db.dao.CachedInsightDao
import com.driftiq.app.data.remote.DriftIQApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(dataStore: UserPreferencesDataStore): OkHttpClient {
        /**
         * Auth interceptor reads the stored access token before each network request.
         *
         * NOTE: OkHttp interceptors run on OkHttp's background thread pool — NOT the main thread.
         * Using runBlocking here is safe in this specific context because:
         * 1. We are already on a background (IO) thread.
         * 2. The DataStore read is fast (single in-memory flow emission, no disk IO).
         * 3. We use firstOrNull() with a short-circuit — it does not suspend long.
         *
         * If token refresh logic is added here in future, move to an Authenticator
         * (okhttp3.Authenticator) to handle 401 retries without deadlocks.
         */
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { dataStore.accessToken.firstOrNull() }
            val request = chain.request().newBuilder().apply {
                token?.let { addHeader("Authorization", "Bearer $it") }
                addHeader("Accept", "application/json")
            }.build()
            chain.proceed(request)
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)  // Extended for LLM insight endpoints
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): DriftIQApiService =
        retrofit.create(DriftIQApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DriftIQDatabase =
        Room.databaseBuilder(context, DriftIQDatabase::class.java, "driftiq.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideEventDao(db: DriftIQDatabase): AppUsageEventDao = db.appUsageEventDao()
    @Provides fun provideDashboardDao(db: DriftIQDatabase): CachedDashboardDao = db.cachedDashboardDao()
    @Provides fun provideInsightDao(db: DriftIQDatabase): CachedInsightDao = db.cachedInsightDao()
}
