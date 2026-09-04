package com.youseif.playerpro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
        val DEFAULT_REFERER = stringPreferencesKey("default_referer")
        val ENABLE_COOKIES = booleanPreferencesKey("enable_cookies")
        val ENABLE_CACHE = booleanPreferencesKey("enable_cache")
        val ENABLE_JAVASCRIPT = booleanPreferencesKey("enable_javascript")
        val AUTOPLAY = booleanPreferencesKey("autoplay")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val THEME = stringPreferencesKey("theme")
        val LAST_QUICK_URL = stringPreferencesKey("last_quick_url")
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val customUserAgent: Flow<String> = context.dataStore.data.map { it[Keys.CUSTOM_USER_AGENT] ?: DEFAULT_UA }
    val defaultReferer: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_REFERER] ?: "" }
    val enableCookies: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENABLE_COOKIES] ?: true }
    val enableCache: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENABLE_CACHE] ?: true }
    val enableJavascript: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENABLE_JAVASCRIPT] ?: true }
    val autoplay: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTOPLAY] ?: false }
    val hardwareAcceleration: Flow<Boolean> = context.dataStore.data.map { it[Keys.HARDWARE_ACCELERATION] ?: true }
    val dataSaver: Flow<Boolean> = context.dataStore.data.map { it[Keys.DATA_SAVER] ?: false }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "dark" }
    val lastQuickUrl: Flow<String> = context.dataStore.data.map { it[Keys.LAST_QUICK_URL] ?: "" }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = value }
    }

    suspend fun setCustomUserAgent(value: String) {
        context.dataStore.edit { it[Keys.CUSTOM_USER_AGENT] = value }
    }

    suspend fun setDefaultReferer(value: String) {
        context.dataStore.edit { it[Keys.DEFAULT_REFERER] = value }
    }

    suspend fun setEnableCookies(value: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_COOKIES] = value }
    }

    suspend fun setEnableCache(value: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_CACHE] = value }
    }

    suspend fun setEnableJavascript(value: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_JAVASCRIPT] = value }
    }

    suspend fun setAutoplay(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY] = value }
    }

    suspend fun setHardwareAcceleration(value: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_ACCELERATION] = value }
    }

    suspend fun setDataSaver(value: Boolean) {
        context.dataStore.edit { it[Keys.DATA_SAVER] = value }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[Keys.THEME] = value }
    }

    suspend fun setLastQuickUrl(value: String) {
        context.dataStore.edit { it[Keys.LAST_QUICK_URL] = value }
    }

    companion object {
        const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
