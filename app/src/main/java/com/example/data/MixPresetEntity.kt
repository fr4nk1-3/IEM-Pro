package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mix_presets")
data class MixPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: String,
    val busId: Int,
    val presetName: String, // e.g. "Acoustic Set IEM", "High Energy IEM"
    val channelLevelsCsv: String, // CSV of 32 channel float values
    val channelPansCsv: String, // CSV of 32 channel float values
    val createdAt: Long = System.currentTimeMillis()
)
