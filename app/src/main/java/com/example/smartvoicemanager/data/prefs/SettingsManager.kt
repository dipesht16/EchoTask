package com.example.smartvoicemanager.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = booleanPreferencesKey("is_dark_theme")
    private val languageKey = stringPreferencesKey("language")
    private val defaultMusicKey = stringPreferencesKey("default_music_uri")

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[themeKey] ?: false
        }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[languageKey] ?: "en"
        }

    val defaultMusicUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[defaultMusicKey]
        }

    suspend fun setTheme(isDark: Boolean) {
        context.dataStore.edit { it[themeKey] = isDark }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[languageKey] = lang }
    }

    suspend fun setDefaultMusicUri(uri: String?) {
        context.dataStore.edit { 
            if (uri != null) it[defaultMusicKey] = uri
            else it.remove(defaultMusicKey)
        }
    }
}
