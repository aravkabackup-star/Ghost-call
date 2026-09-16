package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.receiver.GhostCallReceiver
import com.example.ui.call.IncomingCallActivity

object NotificationHelper {
    const val CHANNEL_ID = "vido_ghost_call_channel"
    const val CHANNEL_NAME = "Vido Ghost Calls"
    const val NOTIFICATION_ID = 9901
    const val MISSED_CALL_NOTIFICATION_ID = 9902

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Simulated incoming ghost calls with full-screen presentation"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableVibration(true)
                setSound(null, null) // Custom sound handled by RingerService
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildIncomingCallNotification(
        context: Context,
        callId: Long,
        callerName: String,
        callerNumber: String,
        durationSeconds: Int
    ): android.app.Notification {
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, callerName)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(IncomingCallActivity.EXTRA_DURATION_SECONDS, durationSeconds)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            callId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline Action Intent
        val declineIntent = Intent(context, GhostCallReceiver::class.java).apply {
            action = GhostCallReceiver.ACTION_DECLINE
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, callerName)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NUMBER, callerNumber)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            (callId + 100).toInt(),
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer Action Intent
        val answerIntent = Intent(context, IncomingCallActivity::class.java).apply {
            action = GhostCallReceiver.ACTION_ANSWER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, callerName)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(IncomingCallActivity.EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra("EXTRA_AUTO_ANSWER", true)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            (callId + 200).toInt(),
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (callerName.isNotBlank() && callerNumber.isNotBlank()) {
            "$callerName ($callerNumber)"
        } else callerName.ifBlank { callerNumber.ifBlank { "Unknown Caller" } }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Ghost Call")
            .setContentText(displayText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline",
                declinePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                answerPendingIntent
            )

        return builder.build()
    }

    fun showMissedCallNotification(
        context: Context,
        callerName: String,
        callerNumber: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val displayText = if (callerName.isNotBlank()) callerName else callerNumber.ifBlank { "Unknown Caller" }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Missed Ghost Call")
            .setContentText("Missed call from $displayText")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(MISSED_CALL_NOTIFICATION_ID, builder.build())
    }

    fun cancelCallNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
