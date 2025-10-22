package com.example.consentgatinglab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.consentgatinglab.appsflyer.AppsFlyerController
import com.example.consentgatinglab.consent.DefaultConsentManager
import com.example.consentgatinglab.ui.DebugDashboard
import com.example.consentgatinglab.ump.UmpStateSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var consent: DefaultConsentManager

    @Inject
    lateinit var ump: UmpStateSource

    @Inject
    lateinit var controller: AppsFlyerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DebugDashboard(consent, ump, controller)
        }
    }
}
