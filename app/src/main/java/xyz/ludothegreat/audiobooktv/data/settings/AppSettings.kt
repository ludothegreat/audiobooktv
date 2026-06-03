package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "audiobooktv-settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.settingsDataStore

    val stopOnAppClose: Flow<Boolean> = store.data.map { it[KEY_STOP_ON_APP_CLOSE] ?: false }

    suspend fun setStopOnAppClose(value: Boolean) {
        store.edit { it[KEY_STOP_ON_APP_CLOSE] = value }
    }

    suspend fun stopOnAppCloseSnapshot(): Boolean = stopOnAppClose.first()

    companion object {
        private val KEY_STOP_ON_APP_CLOSE = booleanPreferencesKey("stop_on_app_close")
    }
}
