package com.example.data.repository

import com.example.data.local.dao.CallHistoryDao
import com.example.data.local.dao.GhostCallDao
import com.example.data.local.entity.CallHistoryEntity
import com.example.data.local.entity.GhostCallEntity
import kotlinx.coroutines.flow.Flow

class GhostCallRepository(
    private val ghostCallDao: GhostCallDao,
    private val callHistoryDao: CallHistoryDao
) {
    val scheduledCalls: Flow<List<GhostCallEntity>> = ghostCallDao.getOneTimeGhostCalls()
    val recurringCalls: Flow<List<GhostCallEntity>> = ghostCallDao.getRecurringGhostCalls()
    val callHistory: Flow<List<CallHistoryEntity>> = callHistoryDao.getAllHistory()

    fun getNextUpcomingCall(currentTime: Long): Flow<GhostCallEntity?> =
        ghostCallDao.getNextUpcomingCall(currentTime)

    suspend fun getCallById(id: Long): GhostCallEntity? = ghostCallDao.getGhostCallById(id)

    suspend fun insertCall(call: GhostCallEntity): Long = ghostCallDao.insert(call)

    suspend fun updateCall(call: GhostCallEntity) = ghostCallDao.update(call)

    suspend fun deleteCall(id: Long) = ghostCallDao.deleteById(id)

    suspend fun setCallEnabled(id: Long, enabled: Boolean) = ghostCallDao.setEnabled(id, enabled)

    suspend fun recordCallHistory(history: CallHistoryEntity): Long =
        callHistoryDao.insert(history)

    suspend fun deleteHistory(id: Long) = callHistoryDao.deleteById(id)

    suspend fun clearAllHistory() = callHistoryDao.clearAllHistory()

    suspend fun getAllEnabledCalls(): List<GhostCallEntity> = ghostCallDao.getAllEnabledCalls()
}
