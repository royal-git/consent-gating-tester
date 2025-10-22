package com.example.consentgatinglab.diag

import java.util.concurrent.atomic.AtomicLong

class Metrics {
    val starts = AtomicLong(0)
    val stops = AtomicLong(0)
    val failures = AtomicLong(0)

    fun snapshot() = mapOf(
        "starts" to starts.get(),
        "stops" to stops.get(),
        "failures" to failures.get()
    )
}
