package com.personal.mangarock.ui.screens.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val UPDATE_INTERVAL_HOURS = longPreferencesKey("update_interval_hours")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val TITLE_LANGUAGE = stringPreferencesKey("title_language")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val updateIntervalHours: Flow<Long> = context.dataStore.data.map { it[Keys.UPDATE_INTERVAL_HOURS] ?: 6L }
    val dataSaver: Flow<Boolean> = context.dataStore.data.map { it[Keys.DATA_SAVER] ?: false }
    val titleLanguage: Flow<String> = context.dataStore.data.map { it[Keys.TITLE_LANGUAGE] ?: "en" }
    val readingDirection: Flow<String> = context.dataStore.data.map { it[Keys.READING_DIRECTION] ?: "VERTICAL" }

    suspend fun setTheme(value: String) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setUpdateInterval(hours: Long) = context.dataStore.edit { it[Keys.UPDATE_INTERVAL_HOURS] = hours }
    suspend fun setDataSaver(enabled: Boolean) = context.dataStore.edit { it[Keys.DATA_SAVER] = enabled }
    suspend fun setTitleLanguage(value: String) = context.dataStore.edit { it[Keys.TITLE_LANGUAGE] = value }
    suspend fun setReadingDirection(value: String) = context.dataStore.edit { it[Keys.READING_DIRECTION] = value }
}
