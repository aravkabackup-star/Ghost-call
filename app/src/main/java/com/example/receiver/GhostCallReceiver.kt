package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.VidoApplication
import com.example.data.local.entity.CallHistoryEntity
import com.example.data.local.entity.CallStatus
import com.example.notification.NotificationHelper
import com.example.scheduler.GhostCallScheduler
import com.example.service.GhostCallRingerService
import com.example.ui.call.ActiveCallActivity
import com.example.ui.call.IncomingCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class GhostCallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_GHOST_CALL = "com.example.vido.TRIGGER_GHOST_CALL"
        const val ACTION_DECLINE = "com.example.vido.ACTION_DECLINE"
        const val ACTION_ANSWER = "com.example.vido.ACTION_ANSWER"
        const val ACTION_CALL_ENDED = "com.example.vido.ACTION_CALL_ENDED"

        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_IDENTITY_MODE = "EXTRA_IDENTITY_MODE"
        const val EXTRA_RINGTONE_URI = "EXTRA_RINGTONE_URI"
        const val EXTRA_VIBRATE = "EXTRA_VIBRATE"
        const val EXTRA_WAKE_SCREEN = "EXTRA_WAKE_SCREEN"
        const val EXTRA_SHOW_OVER_LOCK = "EXTRA_SHOW_OVER_LOCK"
        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
        const val EXTRA_IS_RECURRING = "EXTRA_IS_RECURRING"
        const val EXTRA_RECURRENCE_DAYS = "EXTRA_RECURRENCE_DAYS"
        const val EXTRA_IS_TEMP_CONTACT = "EXTRA_IS_TEMP_CONTACT"
        const val EXTRA_TEMP_CONTACT_NAME = "EXTRA_TEMP_CONTACT_NAME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_TRIGGER_GHOST_CALL -> {
                val callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
                val isTempContact = intent.getBooleanExtra(EXTRA_IS_TEMP_CONTACT, false)
                val tempName = intent.getStringExtra(EXTRA_TEMP_CONTACT_NAME) ?: ""
                val defaultName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Rahul"
                val callerName = if (isTempContact && tempName.isNotBlank()) tempName else defaultName

                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "+91 9876543210"
                val identityMode = intent.getStringExtra(EXTRA_IDENTITY_MODE) ?: "NAME_AND_NUMBER"
                val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                val wakeScreen = intent.getBooleanExtra(EXTRA_WAKE_SCREEN, true)
                val showOverLock = intent.getBooleanExtra(EXTRA_SHOW_OVER_LOCK, true)
                val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 30)
                val isRecurring = intent.getBooleanExtra(EXTRA_IS_RECURRING, false)
                val recurrenceDays = intent.getStringExtra(EXTRA_RECURRENCE_DAYS) ?: ""

                // 1. Start Ringer Service (Plays ringtone, loops vibration, posts notification)
                GhostCallRingerService.startRinging(
                    context,
                    callId,
                    callerName,
                    callerNumber,
                    ringtoneUri,
                    vibrate,
                    durationSeconds
                )

                // 2. Launch Full-screen Incoming Call UI
                val callIntent = Intent(context, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, callerName)
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NUMBER, callerNumber)
                    putExtra(IncomingCallActivity.EXTRA_IDENTITY_MODE, identityMode)
                    putExtra(IncomingCallActivity.EXTRA_DURATION_SECONDS, durationSeconds)
                    putExtra(IncomingCallActivity.EXTRA_WAKE_SCREEN, wakeScreen)
                    putExtra(IncomingCallActivity.EXTRA_SHOW_OVER_LOCK, showOverLock)
                }
                try {
                    context.startActivity(callIntent)
                } catch (e: Exception) {
                    // Handled by Notification FullScreenIntent fallback
                }

                // 3. Reschedule or disable in DB
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = VidoApplication.instance.repository
                        if (isRecurring && recurrenceDays.isNotBlank()) {
                            // Compute next occurrence
                            val call = repo.getCallById(callId)
                            if (call != null) {
                                val cal = Calendar.getInstance().apply { timeInMillis = call.scheduledEpochMillis }
                                val hour = cal.get(Calendar.HOUR_OF_DAY)
                                val minute = cal.get(Calendar.MINUTE)
                                val daysSet = parseDaysSet(recurrenceDays)
                                val nextEpoch = GhostCallScheduler.calculateNextOccurrence(hour, minute, daysSet)
                                val updated = call.copy(scheduledEpochMillis = nextEpoch)
                                repo.updateCall(updated)
                                GhostCallScheduler.scheduleGhostCall(context, updated)
                            }
                        } else {
                            if (callId > 0) {
                                repo.setCallEnabled(callId, false)
                            }
                        }
                    } catch (ignored: Exception) {}
                }
            }

            ACTION_DECLINE -> {
                val callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Rahul"
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "+91 9876543210"

                GhostCallRingerService.stopRinging(context)
                NotificationHelper.cancelCallNotification(context)

                // Notify UI to close
                context.sendBroadcast(Intent(ACTION_CALL_ENDED))
                Toast.makeText(context, "Ghost call declined", Toast.LENGTH_SHORT).show()

                // Record history
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        VidoApplication.instance.repository.recordCallHistory(
                            CallHistoryEntity(
                                callerName = callerName,
                                callerNumber = callerNumber,
                                identityMode = "DEFAULT",
                                status = CallStatus.DECLINED.name,
                                durationSeconds = 0
                            )
                        )
                    } catch (ignored: Exception) {}
                }
            }

            ACTION_ANSWER -> {
                val callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Rahul"
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "+91 9876543210"
                val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 30)

                GhostCallRingerService.stopRinging(context)
                NotificationHelper.cancelCallNotification(context)

                val activeIntent = Intent(context, ActiveCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ActiveCallActivity.EXTRA_CALL_ID, callId)
                    putExtra(ActiveCallActivity.EXTRA_CALLER_NAME, callerName)
                    putExtra(ActiveCallActivity.EXTRA_CALLER_NUMBER, callerNumber)
                    putExtra(ActiveCallActivity.EXTRA_DURATION_SECONDS, durationSeconds)
                }
                try {
                    context.startActivity(activeIntent)
                } catch (ignored: Exception) {}
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule all enabled calls after device reboot
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = VidoApplication.instance.repository
                        val enabledCalls = repo.getAllEnabledCalls()
                        val now = System.currentTimeMillis()
                        enabledCalls.forEach { call ->
                            if (call.scheduledEpochMillis > now) {
                                GhostCallScheduler.scheduleGhostCall(context, call)
                            }
                        }
                    } catch (ignored: Exception) {}
                }
            }
        }
    }

    private fun parseDaysSet(daysStr: String): Set<Int> {
        val set = mutableSetOf<Int>()
        if (daysStr == "DAILY") {
            return (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
        }
        if (daysStr == "WEEKDAYS") {
            return setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
        }
        val parts = daysStr.split(",")
        for (p in parts) {
            when (p.trim().uppercase()) {
                "SUN" -> set.add(Calendar.SUNDAY)
                "MON" -> set.add(Calendar.MONDAY)
                "TUE" -> set.add(Calendar.TUESDAY)
                "WED" -> set.add(Calendar.WEDNESDAY)
                "THU" -> set.add(Calendar.THURSDAY)
                "FRI" -> set.add(Calendar.FRIDAY)
                "SAT" -> set.add(Calendar.SATURDAY)
            }
        }
        return set
    }
}
