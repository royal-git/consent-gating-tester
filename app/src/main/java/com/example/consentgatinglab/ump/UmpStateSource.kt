package com.example.consentgatinglab.ump

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UmpStateSource {
    private val _flow = MutableStateFlow(
        UmpSnapshot(ready = false, hasTcf = false, gdprApplies = Tri.UNKNOWN)
    )
    val flow: StateFlow<UmpSnapshot> = _flow

    // For prototype we simulate via UI toggles. Expose setter:
    fun update(s: UmpSnapshot) {
        _flow.value = s
    }
}
