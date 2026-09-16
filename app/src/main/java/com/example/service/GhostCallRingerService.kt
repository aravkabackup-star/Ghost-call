package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.VidoApplication
import com.example.data.local.entity.CallHistoryEntity
import com.example.data.local.entity.CallStatus
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GhostCallRingerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var timeoutJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val ACTION_START_RINGER = "com.example.vido.ACTION_START_RINGER"
        const val ACTION_STOP_RINGER = "com.example.vido.ACTION_STOP_RINGER"

        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_RINGTONE_URI = "EXTRA_RINGTONE_URI"
        const val EXTRA_VIBRATE = "EXTRA_VIBRATE"
        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"

        fun startRinging(
            context: Context,
            callId: Long,
            name: String,
            number: String,
            ringtoneUri: String?,
            vibrate: Boolean,
            durationSeconds: Int
        ) {
            val intent = Intent(context, GhostCallRingerService::class.java).apply {
                action = ACTION_START_RINGER
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_CALLER_NAME, name)
                putExtra(EXTRA_CALLER_NUMBER, number)
                putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Fallback for background restrictions
                context.startService(intent)
            }
        }

        fun stopRinging(context: Context) {
            val intent = Intent(context, GhostCallRingerService::class.java).apply {
                action = ACTION_STOP_RINGER
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RINGER -> {
                val callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
                val name = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Rahul"
                val number = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "+91 9876543210"
                val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
                val shouldVibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 30)

                startForegroundNotification(callId, name, number, durationSeconds)
                acquireWakeLock()
                startRingtone(ringtoneUri)
                if (shouldVibrate) {
                    startVibration()
                }

                // 45s Ringing timeout (turns into Missed Call if not answered or declined)
                timeoutJob?.cancel()
                timeoutJob = serviceScope.launch {
                    delay(45000L)
                    handleMissedCall(name, number)
                }
            }
            ACTION_STOP_RINGER -> {
                stopRingingAndCleanUp()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification(
        callId: Long,
        name: String,
        number: String,
        durationSeconds: Int
    ) {
        val notification = NotificationHelper.buildIncomingCallNotification(
            this,
            callId,
            name,
            number,
            durationSeconds
        )
        try {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // In Android 14+, starting foreground service can throw if restrictions apply
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE
            wakeLock = powerManager.newWakeLock(flags, "vido:incoming_call_wake_lock").apply {
                setReferenceCounted(false)
                acquire(60000L) // 60 seconds max
            }
        } catch (e: Exception) {
            // WakeLock permission fallback
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vido:incoming_call_partial").apply {
                    setReferenceCounted(false)
                    acquire(60000L)
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun startRingtone(ringtoneUriStr: String?) {
        try {
            mediaPlayer?.release()
            val uri = if (!ringtoneUriStr.isNullOrBlank()) {
                Uri.parse(ringtoneUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@GhostCallRingerService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to default ringtone
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer.create(this, defaultUri)?.apply {
                    isLooping = true
                    start()
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            // Ignored if vibrator not available
        }
    }

    private fun handleMissedCall(name: String, number: String) {
        stopRingingAndCleanUp()
        NotificationHelper.cancelCallNotification(this)
        NotificationHelper.showMissedCallNotification(this, name, number)

        // Record history
        serviceScope.launch(Dispatchers.IO) {
            try {
                VidoApplication.instance.repository.recordCallHistory(
                    CallHistoryEntity(
                        callerName = name,
                        callerNumber = number,
                        identityMode = "DEFAULT",
                        status = CallStatus.MISSED.name,
                        durationSeconds = 0
                    )
                )
            } catch (ignored: Exception) {}
        }
        stopSelf()
    }

    private fun stopRingingAndCleanUp() {
        timeoutJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (ignored: Exception) {}

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (ignored: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (ignored: Exception) {}

        NotificationHelper.cancelCallNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopRingingAndCleanUp()
        super.onDestroy()
    }
}
