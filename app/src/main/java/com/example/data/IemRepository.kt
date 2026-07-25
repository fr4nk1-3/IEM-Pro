package com.example.data

import android.content.Context
import com.example.model.MixBusState
import com.example.model.X32Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class IemRepository(private val db: AppDatabase) {

    val allProfiles: Flow<List<UserProfileEntity>> = db.profileDao().getAllProfiles()
    val savedMixers: Flow<List<MixerDeviceEntity>> = db.mixerDao().getAllSavedMixers()

    suspend fun ensureDefaultProfiles() {
        val existing = allProfiles.first()
        if (existing.isEmpty()) {
            val defaults = listOf(
                UserProfileEntity(
                    id = "drummer",
                    profileName = "Drummer",
                    instrumentIcon = "DRUM",
                    assignedBusId = 1,
                    favoriteChannelIdsCsv = "1,2,3,4,5,6,7,8,11,27",
                    preferredThemeAccent = "CYAN",
                    isDefault = true
                ),
                UserProfileEntity(
                    id = "bass",
                    profileName = "Bass Player",
                    instrumentIcon = "BASS",
                    assignedBusId = 2,
                    favoriteChannelIdsCsv = "1,11,12,13,22,27",
                    preferredThemeAccent = "BLUE",
                    isDefault = false
                ),
                UserProfileEntity(
                    id = "keys",
                    profileName = "Keyboard Player",
                    instrumentIcon = "KEYBOARD",
                    assignedBusId = 3,
                    favoriteChannelIdsCsv = "16,17,18,19,22,23,27",
                    preferredThemeAccent = "MAGENTA",
                    isDefault = false
                ),
                UserProfileEntity(
                    id = "guitar",
                    profileName = "Guitarist",
                    instrumentIcon = "GUITAR",
                    assignedBusId = 4,
                    favoriteChannelIdsCsv = "13,14,15,22,23,27",
                    preferredThemeAccent = "YELLOW",
                    isDefault = false
                ),
                UserProfileEntity(
                    id = "vocals",
                    profileName = "Vocalist",
                    instrumentIcon = "MIC",
                    assignedBusId = 5,
                    favoriteChannelIdsCsv = "22,23,24,25,13,16,27,31,32",
                    preferredThemeAccent = "WHITE",
                    isDefault = false
                ),
                UserProfileEntity(
                    id = "choir",
                    profileName = "Choir",
                    instrumentIcon = "CHOIR",
                    assignedBusId = 6,
                    favoriteChannelIdsCsv = "23,24,25,26,16,27",
                    preferredThemeAccent = "GREEN",
                    isDefault = false
                )
            )
            db.profileDao().insertAll(defaults)
        }
    }

    suspend fun saveProfile(profile: UserProfileEntity) {
        db.profileDao().insertOrUpdateProfile(profile)
    }

    suspend fun deleteProfile(profile: UserProfileEntity) {
        db.profileDao().deleteProfile(profile)
    }

    suspend fun saveMixerDevice(mixer: MixerDeviceEntity) {
        db.mixerDao().insertMixer(mixer)
    }

    suspend fun deleteMixerDevice(mixer: MixerDeviceEntity) {
        db.mixerDao().deleteMixer(mixer)
    }

    fun getPresetsForProfile(profileId: String): Flow<List<MixPresetEntity>> {
        return db.presetDao().getPresetsForProfile(profileId)
    }

    suspend fun savePreset(preset: MixPresetEntity) {
        db.presetDao().insertPreset(preset)
    }

    suspend fun deletePreset(preset: MixPresetEntity) {
        db.presetDao().deletePreset(preset)
    }
}
