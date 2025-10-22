package com.example.consentgatinglab.ump

data class UmpSnapshot(
    val ready: Boolean,
    val hasTcf: Boolean,
    val gdprApplies: Tri
)
