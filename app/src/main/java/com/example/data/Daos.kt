package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isDefault = CASE WHEN id = :activeId THEN 1 ELSE 0 END")
    suspend fun setActiveDefaultProfile(activeId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(profiles: List<UserProfileEntity>)

    @Delete
    suspend fun deleteProfile(profile: UserProfileEntity)
}

@Dao
interface MixerDao {
    @Query("SELECT * FROM saved_mixers ORDER BY lastConnectedTime DESC")
    fun getAllSavedMixers(): Flow<List<MixerDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMixer(mixer: MixerDeviceEntity)

    @Delete
    suspend fun deleteMixer(mixer: MixerDeviceEntity)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM mix_presets WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getPresetsForProfile(profileId: String): Flow<List<MixPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: MixPresetEntity)

    @Delete
    suspend fun deletePreset(preset: MixPresetEntity)
}
