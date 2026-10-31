package com.kipit.app.service

import android.content.Context
import android.content.SharedPreferences

class AiPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kipit_ai_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getModel(): String {
        return prefs.getString(KEY_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
    }

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model.trim()).apply()
    }

    fun isAiEnabled(): Boolean {
        return prefs.getBoolean(KEY_AI_ENABLED, true)
    }

    fun setAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_AI_ENABLED = "ai_enabled"
    }
}
