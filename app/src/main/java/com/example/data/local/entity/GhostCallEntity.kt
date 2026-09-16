package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallerIdentityMode {
    NAME_AND_NUMBER,
    NUMBER_ONLY,
    NAME_ONLY,
    UNKNOWN_CALLER
}

@Entity(tableName = "ghost_calls")
data class GhostCallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerName: String = "Rahul",
    val callerNumber: String = "+91 9876543210",
    val identityMode: String = CallerIdentityMode.NAME_AND_NUMBER.name,
    val isTemporaryContact: Boolean = false,
    val temporaryContactName: String = "",
    val scheduledEpochMillis: Long,
    val simulatedDurationSeconds: Int = 30, // in seconds, or -1 for manual end
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val wakeScreen: Boolean = true,
    val showOverLock: Boolean = true,
    val isRecurring: Boolean = false,
    val recurrenceDays: String = "", // e.g. "MON,WED,FRI" or "DAILY"
    val isEnabled: Boolean = true,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
