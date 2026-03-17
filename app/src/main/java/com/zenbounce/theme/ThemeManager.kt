package com.zenbounce.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zen_prefs")

/**
 * Persists the selected theme ID in DataStore and exposes the resolved [AppTheme]
 * as a cold [Flow]. The Flow immediately replays the stored value on collection.
 */
class ThemeManager(private val context: Context) {

    private val selectedThemeKey = intPreferencesKey("selected_theme_id")

    /** Emits the currently active [AppTheme] whenever it changes. */
    val currentTheme: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            val id = prefs[selectedThemeKey] ?: ThemePresets.default.id
            ThemePresets.byId(id)
        }

    /** Persist the user's choice; the [currentTheme] Flow will update automatically. */
    suspend fun selectTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[selectedThemeKey] = theme.id
        }
    }
}
