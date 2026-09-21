package com.example.data.model

data class UserSettings(
    val language: String = "ar", // "ar" (default) or "en"
    val themeMode: String = "system", // "system", "light", "dark"
    val readerFontSize: Float = 16f,
    val readerTheme: String = "light", // "light", "dark", "sepia"
    val keepLastPage: Boolean = true,
    val confirmDelete: Boolean = true,
    val cloudSyncEnabled: Boolean = false,
    val lastCloudSyncTimestamp: Long = 0L
)
