package com.zenbounce.objects

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.objectPrefsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "object_prefs")

/**
 * Persists the selected [BounceObject] ID to a dedicated DataStore file.
 *
 * Mirrors [com.zenbounce.theme.ThemeManager] in structure.
 * The [currentObject] Flow immediately replays the stored value on collection.
 */
class BounceObjectManager(private val context: Context) {

    private val selectedObjectKey = intPreferencesKey("selected_object_id")

    /** Emits the currently selected [BounceObject] whenever it changes. */
    val currentObject: Flow<BounceObject> = context.objectPrefsDataStore.data
        .map { prefs ->
            val id = prefs[selectedObjectKey] ?: BounceObjectCatalog.DEFAULT.id
            BounceObjectCatalog.byId(id)
        }

    /** Persist the user's choice; [currentObject] will update automatically. */
    suspend fun selectObject(obj: BounceObject) {
        context.objectPrefsDataStore.edit { prefs ->
            prefs[selectedObjectKey] = obj.id
        }
    }
}
