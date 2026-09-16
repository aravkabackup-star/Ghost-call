package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.MainActivity
import com.example.data.local.entity.GhostCallEntity
import com.example.receiver.GhostCallReceiver
import java.util.Calendar

object GhostCallScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    fun scheduleGhostCall(context: Context, call: GhostCallEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, GhostCallReceiver::class.java).apply {
            action = GhostCallReceiver.ACTION_TRIGGER_GHOST_CALL
            putExtra(GhostCallReceiver.EXTRA_CALL_ID, call.id)
            putExtra(GhostCallReceiver.EXTRA_CALLER_NAME, call.callerName)
            putExtra(GhostCallReceiver.EXTRA_CALLER_NUMBER, call.callerNumber)
            putExtra(GhostCallReceiver.EXTRA_IDENTITY_MODE, call.identityMode)
            putExtra(GhostCallReceiver.EXTRA_RINGTONE_URI, call.ringtoneUri)
            putExtra(GhostCallReceiver.EXTRA_VIBRATE, call.vibrate)
            putExtra(GhostCallReceiver.EXTRA_WAKE_SCREEN, call.wakeScreen)
            putExtra(GhostCallReceiver.EXTRA_SHOW_OVER_LOCK, call.showOverLock)
            putExtra(GhostCallReceiver.EXTRA_DURATION_SECONDS, call.simulatedDurationSeconds)
            putExtra(GhostCallReceiver.EXTRA_IS_RECURRING, call.isRecurring)
            putExtra(GhostCallReceiver.EXTRA_RECURRENCE_DAYS, call.recurrenceDays)
            putExtra(GhostCallReceiver.EXTRA_IS_TEMP_CONTACT, call.isTemporaryContact)
            putExtra(GhostCallReceiver.EXTRA_TEMP_CONTACT_NAME, call.temporaryContactName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            call.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = call.scheduledEpochMillis

        try {
            // Use setAlarmClock for highest reliability, waking from Doze
            val showIntent = Intent(context, MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                (call.id + 1000).toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            // In case exact alarm permission is missing on Android 12+
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (ignored: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    fun cancelGhostCall(context: Context, callId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GhostCallReceiver::class.java).apply {
            action = GhostCallReceiver.ACTION_TRIGGER_GHOST_CALL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            callId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextOccurrence(
        targetHour: Int,
        targetMinute: Int,
        selectedDays: Set<Int> // Calendar.MONDAY, Calendar.TUESDAY, etc.
    ): Long {
        val now = Calendar.getInstance()
        var candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selectedDays.isEmpty()) {
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        for (dayOffset in 0..7) {
            val checkCal = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (checkCal.timeInMillis > now.timeInMillis && selectedDays.contains(checkCal.get(Calendar.DAY_OF_WEEK))) {
                return checkCal.timeInMillis
            }
        }

        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }
}
