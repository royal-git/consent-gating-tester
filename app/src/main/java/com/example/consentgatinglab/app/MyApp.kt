package com.example.consentgatinglab.app

import android.app.Application
import com.example.consentgatinglab.appsflyer.AppsFlyerController
import com.example.consentgatinglab.consent.DefaultConsentManager
import com.example.consentgatinglab.gate.AfCoordinator
import com.example.consentgatinglab.ump.UmpStateSource
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var consent: DefaultConsentManager

    @Inject
    lateinit var ump: UmpStateSource

    @Inject
    lateinit var controller: AppsFlyerController

    override fun onCreate() {
        super.onCreate()

        // Always enable logging - this is a dev/debug tool
        Timber.plant(Timber.DebugTree())

        val gdprRegion = {
            // TODO: simple toggle via UI for demo
            false
        }

        AfCoordinator(
            scope = MainScope(),
            consentFlow = consent.flow,
            umpFlow = ump.flow,
            isGdprSubject = gdprRegion,
            controller = controller
        ).start()

        // Seed: default deny all (already handled by DefaultConsentManager initial state)
    }
}
