package com.example.consentgatinglab.diag

import timber.log.Timber

object Tripwire {
    // Debug-only check: warn loudly if someone tries to call the marketing port while AF is off
    // (hook from your port if you add one)
    fun gateMismatch(api: String) {
        Timber.w("TRIPWIRE: blocked call to %s while AppsFlyer is OFF", api)
    }
}
