package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallStatus {
    ANSWERED,
    DECLINED,
    MISSED
}

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerName: String,
    val callerNumber: String,
    val identityMode: String,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val status: String, // "ANSWERED", "DECLINED", "MISSED"
    val durationSeconds: Int = 0 // simulated active duration in seconds
)
