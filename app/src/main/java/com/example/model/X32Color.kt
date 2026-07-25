package com.example.model

import androidx.compose.ui.graphics.Color

enum class X32Color(val id: Int, val displayName: String, val composeColor: Color) {
    OFF(0, "Off", Color(0xFF374151)),
    RED(1, "Red", Color(0xFFEF4444)),
    GREEN(2, "Green", Color(0xFF10B981)),
    YELLOW(3, "Yellow", Color(0xFFF59E0B)),
    BLUE(4, "Blue", Color(0xFF3B82F6)),
    MAGENTA(5, "Magenta", Color(0xFFEC4899)),
    CYAN(6, "Cyan", Color(0xFF06B6D4)),
    WHITE(7, "White", Color(0xFFF9FAFB));

    companion object {
        fun fromId(id: Int): X32Color {
            return entries.firstOrNull { it.id == id } ?: CYAN
        }
    }
}
