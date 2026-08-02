package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String, // e.g. "drummer", "bass", "keys", "guitar", "vocals", "choir"
    val profileName: String, // "Drummer", "Bass Player", etc.
    val instrumentIcon: String, // "DRUM", "BASS", "KEYBOARD", "GUITAR", "MIC", "CHOIR"
    val assignedBusId: Int, // 1..16
    val favoriteChannelIdsCsv: String = "1,2,5,8,10", // Comma separated channel IDs
    val customGroupsJson: String = "{}", // Group MCA associations
    val groupsEnabled: Boolean = false, // Enable or disable custom channel groups for this profile (off by default)
    val preferredThemeAccent: String = "CYAN",
    val lastConnectedMixerIp: String = "127.0.0.1",
    val isDefault: Boolean = false
)
