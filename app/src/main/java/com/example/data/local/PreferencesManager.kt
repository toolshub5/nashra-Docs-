package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nashra_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            language = prefs.getString("key_language", "ar") ?: "ar",
            themeMode = prefs.getString("key_theme", "system") ?: "system",
            readerFontSize = prefs.getFloat("key_font_size", 16f),
            readerTheme = prefs.getString("key_reader_theme", "light") ?: "light",
            keepLastPage = prefs.getBoolean("key_keep_page", true),
            confirmDelete = prefs.getBoolean("key_confirm_delete", true),
            cloudSyncEnabled = prefs.getBoolean("key_cloud_sync", false),
            lastCloudSyncTimestamp = prefs.getLong("key_last_cloud_sync", 0L)
        )
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("key_cloud_sync", enabled).apply()
        _settings.value = _settings.value.copy(cloudSyncEnabled = enabled)
    }

    fun updateLastCloudSync(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong("key_last_cloud_sync", timestamp).apply()
        _settings.value = _settings.value.copy(lastCloudSyncTimestamp = timestamp)
    }

    fun setLanguage(language: String) {
        prefs.edit().putString("key_language", language).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun setThemeMode(theme: String) {
        prefs.edit().putString("key_theme", theme).apply()
        _settings.value = _settings.value.copy(themeMode = theme)
    }

    fun setReaderFontSize(size: Float) {
        prefs.edit().putFloat("key_font_size", size).apply()
        _settings.value = _settings.value.copy(readerFontSize = size)
    }

    fun setReaderTheme(theme: String) {
        prefs.edit().putString("key_reader_theme", theme).apply()
        _settings.value = _settings.value.copy(readerTheme = theme)
    }

    fun setKeepLastPage(keep: Boolean) {
        prefs.edit().putBoolean("key_keep_page", keep).apply()
        _settings.value = _settings.value.copy(keepLastPage = keep)
    }

    fun setConfirmDelete(confirm: Boolean) {
        prefs.edit().putBoolean("key_confirm_delete", confirm).apply()
        _settings.value = _settings.value.copy(confirmDelete = confirm)
    }
}
