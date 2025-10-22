package com.example.consentgatinglab.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PolicyFile(
    val version: String,
    val sdks: List<SdkRow>
)

@Serializable
private data class SdkRow(
    val id: String,
    val requiredConsent: List<String>,
    val initOrder: Int = 0,
    val thread: String = "BACKGROUND"
)

class SdkRegistry(private val configs: List<SdkConfig>) {
    fun all(): List<SdkConfig> = configs.sortedBy { it.initOrder }
    fun get(id: SdkId): SdkConfig? = configs.firstOrNull { it.id == id }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromAsset(ctx: Context, assetName: String = "sdk_policy.json"): SdkRegistry {
            val text = ctx.assets.open(assetName).bufferedReader().use { it.readText() }
            val file = json.decodeFromString(PolicyFile.serializer(), text)
            val list = file.sdks.mapNotNull { r ->
                val id = runCatching { SdkId.valueOf(r.id) }.getOrNull() ?: return@mapNotNull null
                val thread = runCatching { ThreadKind.valueOf(r.thread) }.getOrNull() ?: ThreadKind.BACKGROUND
                val req = r.requiredConsent.mapNotNull { s ->
                    runCatching { ConsentType.valueOf(s) }.getOrNull()
                }.toSet()
                SdkConfig(id, req, r.initOrder, thread)
            }
            return SdkRegistry(list)
        }
    }
}
