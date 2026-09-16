package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VidoApplication
import com.example.data.local.entity.CallerIdentityMode
import com.example.data.local.entity.GhostCallEntity
import com.example.receiver.GhostCallReceiver
import com.example.scheduler.GhostCallScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VidoSettingsState(
    val ringtoneUri: String = "",
    val vibrate: Boolean = true,
    val durationSeconds: Int = 30,
    val showOnLock: Boolean = true,
    val wakeScreen: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VidoApplication).repository
    private val preferences = (application as VidoApplication).preferences

    val scheduledCalls: StateFlow<List<GhostCallEntity>> = repository.scheduledCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringCalls: StateFlow<List<GhostCallEntity>> = repository.recurringCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callHistory = repository.callHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextUpcomingCall = repository.getNextUpcomingCall(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _settingsState = MutableStateFlow(
        VidoSettingsState(
            ringtoneUri = preferences.defaultRingtoneUri,
            vibrate = preferences.defaultVibration,
            durationSeconds = preferences.defaultDurationSeconds,
            showOnLock = preferences.showOnLockScreen,
            wakeScreen = preferences.wakeScreen
        )
    )
    val settingsState: StateFlow<VidoSettingsState> = _settingsState.asStateFlow()

    fun scheduleGhostCall(call: GhostCallEntity, context: Context, onScheduled: () -> Unit) {
        viewModelScope.launch {
            val id = repository.insertCall(call)
            val callWithId = call.copy(id = id)
            GhostCallScheduler.scheduleGhostCall(context, callWithId)
            onScheduled()
        }
    }

    fun toggleCallEnabled(call: GhostCallEntity, enabled: Boolean, context: Context) {
        viewModelScope.launch {
            repository.setCallEnabled(call.id, enabled)
            if (enabled) {
                if (call.scheduledEpochMillis > System.currentTimeMillis()) {
                    GhostCallScheduler.scheduleGhostCall(context, call.copy(isEnabled = true))
                }
            } else {
                GhostCallScheduler.cancelGhostCall(context, call.id)
            }
        }
    }

    fun deleteCall(callId: Long, context: Context) {
        viewModelScope.launch {
            GhostCallScheduler.cancelGhostCall(context, callId)
            repository.deleteCall(callId)
        }
    }

    fun deleteHistory(historyId: Long) {
        viewModelScope.launch {
            repository.deleteHistory(historyId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun updateSettings(
        ringtoneUri: String? = null,
        vibrate: Boolean? = null,
        durationSeconds: Int? = null,
        showOnLock: Boolean? = null,
        wakeScreen: Boolean? = null
    ) {
        ringtoneUri?.let {
            preferences.defaultRingtoneUri = it
            _settingsState.value = _settingsState.value.copy(ringtoneUri = it)
        }
        vibrate?.let {
            preferences.defaultVibration = it
            _settingsState.value = _settingsState.value.copy(vibrate = it)
        }
        durationSeconds?.let {
            preferences.defaultDurationSeconds = it
            _settingsState.value = _settingsState.value.copy(durationSeconds = it)
        }
        showOnLock?.let {
            preferences.showOnLockScreen = it
            _settingsState.value = _settingsState.value.copy(showOnLock = it)
        }
        wakeScreen?.let {
            preferences.wakeScreen = it
            _settingsState.value = _settingsState.value.copy(wakeScreen = it)
        }
    }

    fun triggerTestCallNow(context: Context, callerName: String = "Rahul (Test)", callerNumber: String = "+91 9876543210") {
        val triggerIntent = Intent(context, GhostCallReceiver::class.java).apply {
            action = GhostCallReceiver.ACTION_TRIGGER_GHOST_CALL
            putExtra(GhostCallReceiver.EXTRA_CALL_ID, -1L)
            putExtra(GhostCallReceiver.EXTRA_CALLER_NAME, callerName)
            putExtra(GhostCallReceiver.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(GhostCallReceiver.EXTRA_IDENTITY_MODE, CallerIdentityMode.NAME_AND_NUMBER.name)
            putExtra(GhostCallReceiver.EXTRA_RINGTONE_URI, preferences.defaultRingtoneUri)
            putExtra(GhostCallReceiver.EXTRA_VIBRATE, preferences.defaultVibration)
            putExtra(GhostCallReceiver.EXTRA_WAKE_SCREEN, preferences.wakeScreen)
            putExtra(GhostCallReceiver.EXTRA_SHOW_OVER_LOCK, preferences.showOnLockScreen)
            putExtra(GhostCallReceiver.EXTRA_DURATION_SECONDS, preferences.defaultDurationSeconds)
            putExtra(GhostCallReceiver.EXTRA_IS_RECURRING, false)
        }
        context.sendBroadcast(triggerIntent)
    }

    fun triggerTestCallInSeconds(context: Context, seconds: Int, callerName: String = "Rahul (10s Test)", callerNumber: String = "+91 9876543210") {
        val triggerEpoch = System.currentTimeMillis() + (seconds * 1000L)
        val dummyCall = GhostCallEntity(
            id = 9999L,
            callerName = callerName,
            callerNumber = callerNumber,
            identityMode = CallerIdentityMode.NAME_AND_NUMBER.name,
            scheduledEpochMillis = triggerEpoch,
            simulatedDurationSeconds = preferences.defaultDurationSeconds,
            ringtoneUri = preferences.defaultRingtoneUri,
            vibrate = preferences.defaultVibration,
            wakeScreen = preferences.wakeScreen,
            showOverLock = preferences.showOnLockScreen,
            isRecurring = false,
            isEnabled = true
        )
        GhostCallScheduler.scheduleGhostCall(context, dummyCall)
    }
}
