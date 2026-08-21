package dev.quenguyen.ytgrab.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.quenguyen.ytgrab.model.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val DEFAULT_HEIGHT_CAP = intPreferencesKey("default_height_cap")
        val DEFAULT_AUDIO_QUALITY = stringPreferencesKey("default_audio_quality")
        val CONCURRENCY = intPreferencesKey("concurrency")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultFormat = prefs[Keys.DEFAULT_FORMAT]?.let { runCatching { MediaFormat.valueOf(it) }.getOrNull() }
                ?: MediaFormat.MP4,
            defaultHeightCap = prefs[Keys.DEFAULT_HEIGHT_CAP]?.takeIf { it > 0 },
            defaultAudioQualityArg = prefs[Keys.DEFAULT_AUDIO_QUALITY] ?: "0",
            concurrency = prefs[Keys.CONCURRENCY]?.coerceIn(1, 3) ?: 1,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
        )
    }

    suspend fun setDefaultFormat(format: MediaFormat) {
        context.dataStore.edit { it[Keys.DEFAULT_FORMAT] = format.name }
    }

    suspend fun setDefaultHeightCap(heightCap: Int?) {
        context.dataStore.edit {
            if (heightCap == null) it.remove(Keys.DEFAULT_HEIGHT_CAP) else it[Keys.DEFAULT_HEIGHT_CAP] = heightCap
        }
    }

    suspend fun setDefaultAudioQualityArg(value: String) {
        context.dataStore.edit { it[Keys.DEFAULT_AUDIO_QUALITY] = value }
    }

    suspend fun setConcurrency(value: Int) {
        context.dataStore.edit { it[Keys.CONCURRENCY] = value.coerceIn(1, 3) }
    }

    suspend fun setWifiOnly(value: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = value }
    }
}
