package com.example.model

enum class RoleType {
    MUSICIAN,
    ENGINEER
}

data class UserRole(
    val type: RoleType = RoleType.MUSICIAN,
    val isUnlocked: Boolean = false,
    val engineerPin: String = "1234"
)
