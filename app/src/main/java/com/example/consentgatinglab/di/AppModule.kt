package com.example.consentgatinglab.di

import android.app.Application
import com.example.consentgatinglab.analytics.AnalyticsController
import com.example.consentgatinglab.consent.DefaultConsentManager
import com.example.consentgatinglab.ump.UmpStateSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideConsentManager(application: Application): DefaultConsentManager {
        return DefaultConsentManager(application.applicationContext)
    }

    @Provides
    @Singleton
    fun provideUmpStateSource(): UmpStateSource {
        return UmpStateSource()
    }

    @Provides
    @Singleton
    fun provideAnalyticsController(application: Application): AnalyticsController {
        return AnalyticsController(
            app = application,
            devKey = "AF_DEV_KEY_PLACEHOLDER"
        )
    }
}
