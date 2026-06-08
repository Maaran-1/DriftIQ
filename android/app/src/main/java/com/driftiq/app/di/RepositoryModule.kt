package com.driftiq.app.di

import com.driftiq.app.data.repository.*
import com.driftiq.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds @Singleton
    abstract fun bindDriftRepository(impl: DriftRepositoryImpl): DriftRepository

    @Binds @Singleton
    abstract fun bindRiskRepository(impl: RiskRepositoryImpl): RiskRepository

    @Binds @Singleton
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository

    @Binds @Singleton
    abstract fun bindTwinRepository(impl: TwinRepositoryImpl): TwinRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
