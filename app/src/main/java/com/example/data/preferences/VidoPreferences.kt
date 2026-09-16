package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri

class VidoPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vido_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEFAULT_RINGTONE = "default_ringtone"
        private const val KEY_DEFAULT_VIBRATION = "default_vibration"
        private const val KEY_DEFAULT_DURATION = "default_duration"
        private const val KEY_SHOW_ON_LOCK = "show_on_lock"
        private const val KEY_WAKE_SCREEN = "wake_screen"
    }

    var defaultRingtoneUri: String
        get() = prefs.getString(
            KEY_DEFAULT_RINGTONE,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString() ?: ""
        ) ?: ""
        set(value) = prefs.edit().putString(KEY_DEFAULT_RINGTONE, value).apply()

    var defaultVibration: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_VIBRATION, value).apply()

    var defaultDurationSeconds: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 30)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_DURATION, value).apply()

    var showOnLockScreen: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ON_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ON_LOCK, value).apply()

    var wakeScreen: Boolean
        get() = prefs.getBoolean(KEY_WAKE_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_SCREEN, value).apply()
}
