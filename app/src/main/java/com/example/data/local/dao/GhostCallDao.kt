package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.GhostCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GhostCallDao {
    @Query("SELECT * FROM ghost_calls WHERE isRecurring = 0 ORDER BY scheduledEpochMillis ASC")
    fun getOneTimeGhostCalls(): Flow<List<GhostCallEntity>>

    @Query("SELECT * FROM ghost_calls WHERE isRecurring = 1 ORDER BY id DESC")
    fun getRecurringGhostCalls(): Flow<List<GhostCallEntity>>

    @Query("SELECT * FROM ghost_calls WHERE isEnabled = 1 AND scheduledEpochMillis >= :currentTimeMillis ORDER BY scheduledEpochMillis ASC LIMIT 1")
    fun getNextUpcomingCall(currentTimeMillis: Long): Flow<GhostCallEntity?>

    @Query("SELECT * FROM ghost_calls WHERE id = :id")
    suspend fun getGhostCallById(id: Long): GhostCallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: GhostCallEntity): Long

    @Update
    suspend fun update(call: GhostCallEntity)

    @Query("DELETE FROM ghost_calls WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE ghost_calls SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM ghost_calls WHERE isEnabled = 1")
    suspend fun getAllEnabledCalls(): List<GhostCallEntity>
}
