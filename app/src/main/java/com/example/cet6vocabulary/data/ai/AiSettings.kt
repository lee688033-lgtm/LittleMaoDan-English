package com.example.cet6vocabulary.data.ai

import android.content.Context

/**
 * Free, OpenAI-compatible endpoints. The presets only prefill the address and model name; the
 * learner can override both, so a provider change never needs an app update.
 */
enum class AiProvider(
    val label: String,
    val shortLabel: String,
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    ZHIPU(
        label = "智谱 GLM-4-Flash（免费）",
        shortLabel = "智谱",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
        defaultModel = "glm-4-flash"
    ),
    SILICONFLOW(
        label = "硅基流动 Qwen2.5-7B（免费）",
        shortLabel = "硅基流动",
        defaultBaseUrl = "https://api.siliconflow.cn/v1",
        defaultModel = "Qwen/Qwen2.5-7B-Instruct"
    ),
    CUSTOM(
        label = "自定义接口",
        shortLabel = "自定义",
        defaultBaseUrl = "",
        defaultModel = ""
    );

    companion object {
        fun fromName(name: String?): AiProvider =
            entries.firstOrNull { it.name == name } ?: ZHIPU
    }
}

/** AI preferences. The API key never leaves the app's private storage and never enters exports. */
class AiSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var provider: AiProvider
        get() = AiProvider.fromName(prefs.getString(KEY_PROVIDER, null))
        set(value) {
            prefs.edit().putString(KEY_PROVIDER, value.name).apply()
        }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
        }

    /** Empty means "use the preset default"; both are stored so a switch keeps its history. */
    var baseUrlOverride: String
        get() = prefs.getString(KEY_BASE_URL, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value.trim()).apply()
        }

    var modelOverride: String
        get() = prefs.getString(KEY_MODEL, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_MODEL, value.trim()).apply()
        }

    val baseUrl: String
        get() = baseUrlOverride.ifBlank { provider.defaultBaseUrl }.trim().trimEnd('/')

    val model: String
        get() = modelOverride.ifBlank { provider.defaultModel }.trim()

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    fun save(provider: AiProvider, apiKey: String, baseUrl: String, model: String) {
        prefs.edit()
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_MODEL, model.trim())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "cet6_ai"
        const val KEY_PROVIDER = "provider"
        const val KEY_API_KEY = "api_key"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
    }
}
