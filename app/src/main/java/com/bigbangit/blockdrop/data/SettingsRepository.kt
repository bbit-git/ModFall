package com.bigbangit.blockdrop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.bigbangit.blockdrop.ui.model.ParticleQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.blockDropPreferences by preferencesDataStore(name = "block_drop_preferences")

class SettingsRepository(
    private val context: Context,
) {
    val isMuted: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        preferences[IsMutedKey] ?: false
    }

    val buttonsEnabled: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        preferences[ButtonsEnabledKey] ?: true
    }

    val gesturesEnabled: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        preferences[GesturesEnabledKey] ?: true
    }

    val musicEnabled: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        preferences[MusicEnabledKey] ?: true
    }

    val particlesEnabled: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        preferences[ParticlesEnabledKey] ?: false
    }

    val particleQuality: Flow<ParticleQuality> = context.blockDropPreferences.data.map { preferences ->
        ParticleQuality.entries.firstOrNull { it.name == preferences[ParticleQualityKey] } ?: ParticleQuality.High
    }

    val mainTrackPathOrUri: Flow<String?> = context.blockDropPreferences.data.map { preferences ->
        preferences[MainTrackPathOrUriKey]
    }

    val shouldShowTutorialOnLaunch: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        !(preferences[TutorialSeenKey] ?: false)
    }

    val musicFolderUri: Flow<String?> = context.blockDropPreferences.data.map { preferences ->
        preferences[MusicFolderUriKey]
    }

    suspend fun setMuted(isMuted: Boolean) {
        context.blockDropPreferences.edit { preferences ->
            preferences[IsMutedKey] = isMuted
        }
    }

    suspend fun setButtonsEnabled(enabled: Boolean) {
        context.blockDropPreferences.edit { preferences ->
            if (!enabled && (preferences[GesturesEnabledKey] ?: true).not()) return@edit
            preferences[ButtonsEnabledKey] = enabled
        }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.blockDropPreferences.edit { preferences ->
            if (!enabled && (preferences[ButtonsEnabledKey] ?: true).not()) return@edit
            preferences[GesturesEnabledKey] = enabled
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.blockDropPreferences.edit { preferences ->
            preferences[MusicEnabledKey] = enabled
        }
    }

    suspend fun setParticlesEnabled(enabled: Boolean) {
        context.blockDropPreferences.edit { preferences ->
            preferences[ParticlesEnabledKey] = enabled
        }
    }

    suspend fun setParticleQuality(quality: ParticleQuality) {
        context.blockDropPreferences.edit { preferences ->
            preferences[ParticleQualityKey] = quality.name
        }
    }

    suspend fun setMainTrackPathOrUri(pathOrUri: String?) {
        context.blockDropPreferences.edit { preferences ->
            if (pathOrUri.isNullOrBlank()) {
                preferences.remove(MainTrackPathOrUriKey)
            } else {
                preferences[MainTrackPathOrUriKey] = pathOrUri
            }
        }
    }

    suspend fun markTutorialSeen() {
        context.blockDropPreferences.edit { preferences ->
            preferences[TutorialSeenKey] = true
        }
    }

    suspend fun setMusicFolderUri(uri: String?) {
        context.blockDropPreferences.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(MusicFolderUriKey)
            } else {
                preferences[MusicFolderUriKey] = uri
            }
        }
    }

    private companion object {
        val IsMutedKey = booleanPreferencesKey("is_muted")
        val ButtonsEnabledKey = booleanPreferencesKey("buttons_enabled")
        val GesturesEnabledKey = booleanPreferencesKey("gestures_enabled")
        val MusicEnabledKey = booleanPreferencesKey("music_enabled")
        val ParticlesEnabledKey = booleanPreferencesKey("particles_enabled")
        val ParticleQualityKey = stringPreferencesKey("particle_quality")
        val TutorialSeenKey = booleanPreferencesKey("tutorial_seen")
        val MusicFolderUriKey = stringPreferencesKey("music_folder_uri")
        val MainTrackPathOrUriKey = stringPreferencesKey("main_track_path_or_uri")
    }
}
