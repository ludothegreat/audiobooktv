package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.speedDataStore by preferencesDataStore(name = "audiobooktv-speeds")

@Singleton
class SpeedStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.speedDataStore

    suspend fun get(itemId: String): Float? = store.data.map { it[key(itemId)] }.first()

    suspend fun set(itemId: String, speed: Float) {
        store.edit { it[key(itemId)] = speed }
    }

    private fun key(itemId: String) = floatPreferencesKey("speed_$itemId")
}
