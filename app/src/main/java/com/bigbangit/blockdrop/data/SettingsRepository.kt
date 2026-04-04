package com.bigbangit.blockdrop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.blockDropPreferences by preferencesDataStore(name = "block_drop_preferences")

class SettingsRepository(
    private val context: Context,
) {
    val isMuted: Flow<Boolean> = context.blockDropPreferences.data.map { preferences ->
        preferences[IsMutedKey] ?: false
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
        val TutorialSeenKey = booleanPreferencesKey("tutorial_seen")
        val MusicFolderUriKey = stringPreferencesKey("music_folder_uri")
    }
}
