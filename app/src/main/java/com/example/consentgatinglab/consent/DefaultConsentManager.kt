package com.example.consentgatinglab.consent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.consentgatinglab.core.ConsentManager
import com.example.consentgatinglab.core.ConsentSnapshot
import com.example.consentgatinglab.core.ConsentType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "consent_store")

class DefaultConsentManager(private val ctx: Context) : ConsentManager {
    private val KEY = stringSetPreferencesKey("granted")

    private val _flow = ctx.dataStore.data
        .map { prefs ->
            val grantedStrings = prefs[KEY] ?: emptySet()
            val granted = grantedStrings.mapNotNull { s ->
                runCatching { ConsentType.valueOf(s) }.getOrNull()
            }.toSet()
            ConsentSnapshot(userId = null, granted = granted)
        }
        .stateIn(
            scope = kotlinx.coroutines.GlobalScope,
            started = SharingStarted.Eagerly,
            initialValue = ConsentSnapshot(null, emptySet())
        )

    override val flow: StateFlow<ConsentSnapshot> = _flow

    override suspend fun current(): ConsentSnapshot = _flow.value

    override suspend fun update(granted: Set<ConsentType>, userId: String?) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY] = granted.map { it.name }.toSet()
        }
    }
}
