package eu.delpont.morphee.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository private constructor(private val context: Context) {

    data class AppSettings(
        val timerMinutes: Int = 20,
        val autoTimer: Boolean = true,
        val fadeSeconds: Int = 10,
    )

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            timerMinutes = prefs[TIMER_MINUTES] ?: 20,
            autoTimer = prefs[AUTO_TIMER] ?: true,
            fadeSeconds = prefs[FADE_SECONDS] ?: 10,
        )
    }

    suspend fun setTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[TIMER_MINUTES] = minutes.coerceIn(1, 240) }
    }

    suspend fun setAutoTimer(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_TIMER] = enabled }
    }

    suspend fun setFadeSeconds(seconds: Int) {
        context.dataStore.edit { it[FADE_SECONDS] = seconds.coerceIn(0, 60) }
    }

    companion object {
        private val TIMER_MINUTES = intPreferencesKey("timer_minutes")
        private val AUTO_TIMER = booleanPreferencesKey("auto_timer")
        private val FADE_SECONDS = intPreferencesKey("fade_seconds")

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
