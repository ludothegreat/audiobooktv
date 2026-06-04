package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.ludothegreat.audiobooktv.ui.theme.AppTheme
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "audiobooktv-settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.settingsDataStore

    val stopOnAppClose: Flow<Boolean> = store.data.map { it[KEY_STOP_ON_APP_CLOSE] ?: false }
    val diagnosticLogEnabled: Flow<Boolean> = store.data.map { it[KEY_DIAGNOSTIC_LOG] ?: false }
    val selectedTheme: Flow<AppTheme> = store.data.map { prefs ->
        val name = prefs[KEY_SELECTED_THEME]
        AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.Gruvbox
    }

    suspend fun setStopOnAppClose(value: Boolean) {
        store.edit { it[KEY_STOP_ON_APP_CLOSE] = value }
    }

    suspend fun setDiagnosticLogEnabled(value: Boolean) {
        store.edit { it[KEY_DIAGNOSTIC_LOG] = value }
    }

    suspend fun setSelectedTheme(theme: AppTheme) {
        store.edit { it[KEY_SELECTED_THEME] = theme.name }
    }

    suspend fun stopOnAppCloseSnapshot(): Boolean = stopOnAppClose.first()
    suspend fun diagnosticLogEnabledSnapshot(): Boolean = diagnosticLogEnabled.first()

    companion object {
        private val KEY_STOP_ON_APP_CLOSE = booleanPreferencesKey("stop_on_app_close")
        private val KEY_DIAGNOSTIC_LOG = booleanPreferencesKey("diagnostic_log")
        private val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme")
    }
}
