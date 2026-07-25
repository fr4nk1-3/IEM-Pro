package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_mixers")
data class MixerDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val model: String,
    val ipAddress: String,
    val port: Int = 10023,
    val isFavorite: Boolean = true,
    val firmwareVersion: String = "V4.09",
    val lastConnectedTime: Long = System.currentTimeMillis()
)
