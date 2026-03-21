package com.zenbounce.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * Persists the ball sensitivity (0–100) to a dedicated DataStore file.
 * 50 = default (1× gravity scale), 0 = no movement, 100 = 2× scale.
 */
class SensitivityManager(private val context: Context) {

    private val sensitivityKey = intPreferencesKey("sensitivity")

    val sensitivity: Flow<Int> = context.appPrefsDataStore.data
        .map { prefs -> prefs[sensitivityKey] ?: DEFAULT_SENSITIVITY }

    suspend fun setSensitivity(value: Int) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[sensitivityKey] = value.coerceIn(0, 100)
        }
    }

    companion object {
        const val DEFAULT_SENSITIVITY = 50
    }
}
