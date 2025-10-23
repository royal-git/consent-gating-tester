package com.example.consentgatinglab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.consentgatinglab.analytics.AnalyticsController
import com.example.consentgatinglab.core.ConsentManager
import com.example.consentgatinglab.core.ConsentSnapshot
import com.example.consentgatinglab.core.ConsentType
import com.example.consentgatinglab.ump.UmpSnapshot
import com.example.consentgatinglab.ump.UmpStateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class DashboardTab(val label: String) {
    Consent("Consent & Signals"),
    AppEvents("App Events"),
    SdkLab("SDK Lab")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboard(
    consent: ConsentManager,
    ump: UmpStateSource,
    controller: AnalyticsController
) {
    val scope = rememberCoroutineScope()
    val consentState by consent.flow.collectAsState(
        initial = ConsentSnapshot(null, emptySet())
    )

    var analytics by remember { mutableStateOf(false) }
    var marketing by remember { mutableStateOf(false) }
    var personalization by remember { mutableStateOf(false) }

    LaunchedEffect(consentState) {
        analytics = consentState.granted.contains(ConsentType.ANALYTICS)
        marketing = consentState.granted.contains(ConsentType.MARKETING)
        personalization = consentState.granted.contains(ConsentType.PERSONALIZATION)
    }

    val umpSnap by ump.flow.collectAsState()
    var gdpr by remember { mutableStateOf(false) }
    val isStarted by controller.isStarted.collectAsState()
    val isBootstrapped by controller.isBootstrapped.collectAsState()

    var selectedTab by remember { mutableStateOf(DashboardTab.Consent) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ConsentGatingLab") })
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                DashboardTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    DashboardTab.Consent -> ConsentTab(
                        analytics = analytics,
                        onAnalyticsChange = { analytics = it },
                        marketing = marketing,
                        onMarketingChange = { marketing = it },
                        personalization = personalization,
                        onPersonalizationChange = { personalization = it },
                        consent = consent,
                        scope = scope,
                        ump = ump,
                        umpSnap = umpSnap,
                        gdpr = gdpr,
                        onGdprChange = { gdpr = it },
                        isBootstrapped = isBootstrapped,
                        isStarted = isStarted,
                        modifier = Modifier.fillMaxSize()
                    )

                    DashboardTab.AppEvents -> AppEventsTab(
                        controller = controller,
                        modifier = Modifier.fillMaxSize()
                    )

                    DashboardTab.SdkLab -> SdkLabTab(
                        controller = controller,
                        scope = scope,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentTab(
    analytics: Boolean,
    onAnalyticsChange: (Boolean) -> Unit,
    marketing: Boolean,
    onMarketingChange: (Boolean) -> Unit,
    personalization: Boolean,
    onPersonalizationChange: (Boolean) -> Unit,
    consent: ConsentManager,
    scope: CoroutineScope,
    ump: UmpStateSource,
    umpSnap: UmpSnapshot,
    gdpr: Boolean,
    onGdprChange: (Boolean) -> Unit,
    isBootstrapped: Boolean,
    isStarted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Consent toggles", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = analytics, onCheckedChange = onAnalyticsChange)
            Spacer(Modifier.width(8.dp))
            Text("analytics")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = marketing, onCheckedChange = onMarketingChange)
            Spacer(Modifier.width(8.dp))
            Text("marketing")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = personalization, onCheckedChange = onPersonalizationChange)
            Spacer(Modifier.width(8.dp))
            Text("personalization")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val granted = buildSet {
                        if (analytics) add(ConsentType.ANALYTICS)
                        if (marketing) add(ConsentType.MARKETING)
                        if (personalization) add(ConsentType.PERSONALIZATION)
                    }
                    consent.update(granted, null)
                }
            }) {
                Text("apply app consent")
            }
            Button(onClick = {
                scope.launch {
                    consent.update(emptySet(), null)
                }
            }) {
                Text("clear all")
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("UMP (simulated)", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = umpSnap.ready,
                onCheckedChange = { ready -> ump.update(umpSnap.copy(ready = ready)) }
            )
            Spacer(Modifier.width(8.dp))
            Text("ump ready")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = umpSnap.hasTcf,
                onCheckedChange = { hasTcf -> ump.update(umpSnap.copy(hasTcf = hasTcf)) }
            )
            Spacer(Modifier.width(8.dp))
            Text("has TCF string")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = gdpr, onCheckedChange = onGdprChange)
            Spacer(Modifier.width(8.dp))
            Text("gdpr subject (app toggle placeholder)")
        }

        Spacer(Modifier.height(4.dp))
        Text("AppsFlyer SDK state", style = MaterialTheme.typography.titleMedium)
        Text(
            "bootstrapped: $isBootstrapped",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "started: $isStarted",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Toggle consent & UMP above to watch AfCoordinator decisions and how they change these states.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AppEventsTab(
    controller: AnalyticsController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App layer (gated)", style = MaterialTheme.typography.titleMedium)
        Text(
            "These calls route through AnalyticsController.sink. When consent is denied, expect 🚫 NoopSink logs and no AppsFlyer traffic.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { controller.logEvent("app_layer_event") },
                modifier = Modifier.weight(1f)
            ) {
                Text("log event (app)")
            }
            Button(
                onClick = { controller.logRevenue("9.99", "USD") },
                modifier = Modifier.weight(1f)
            ) {
                Text("log revenue (app)")
            }
        }

        Button(
            onClick = { controller.setUserId("app_user_${System.currentTimeMillis()}") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("set user ID (app)")
        }
    }
}

@Composable
private fun SdkLabTab(
    controller: AnalyticsController,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AppsFlyer SDK experiments", style = MaterialTheme.typography.titleMedium)
        Text(
            "Direct AppsFlyer SDK calls for vendor-behaviour testing. These bypass the consent gate—watch Logcat closely.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        controller.setConsent(
                            isGdprSubject = true,
                            hasDataUsageConsent = true,
                            hasAdPersonalizationConsent = true
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("AF: grant all")
            }
            Button(
                onClick = {
                    scope.launch {
                        controller.setConsent(
                            isGdprSubject = true,
                            hasDataUsageConsent = false,
                            hasAdPersonalizationConsent = false
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("AF: deny all")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { scope.launch { controller.startIfAllowed(true) } },
                modifier = Modifier.weight(1f)
            ) {
                Text("start()")
            }
            Button(
                onClick = { scope.launch { controller.stop() } },
                modifier = Modifier.weight(1f)
            ) {
                Text("stop()")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Force: individual AF API calls", style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { scope.launch { controller.logTestEvent() } },
                modifier = Modifier.weight(1f)
            ) {
                Text("force log event")
            }
            Button(
                onClick = { scope.launch { controller.logTestRevenue() } },
                modifier = Modifier.weight(1f)
            ) {
                Text("force log revenue")
            }
        }

        Button(
            onClick = { scope.launch { controller.setTestUserId() } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("force set user ID")
        }
    }
}
