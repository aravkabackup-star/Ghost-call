package com.example.ui.call

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VidoApplication
import com.example.data.local.entity.CallHistoryEntity
import com.example.data.local.entity.CallStatus
import com.example.notification.NotificationHelper
import com.example.receiver.GhostCallReceiver
import com.example.service.GhostCallRingerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_IDENTITY_MODE = "EXTRA_IDENTITY_MODE"
        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
        const val EXTRA_WAKE_SCREEN = "EXTRA_WAKE_SCREEN"
        const val EXTRA_SHOW_OVER_LOCK = "EXTRA_SHOW_OVER_LOCK"
    }

    private var callId: Long = 0L
    private var callerName: String = "Rahul"
    private var callerNumber: String = "+91 9876543210"
    private var identityMode: String = "NAME_AND_NUMBER"
    private var durationSeconds: Int = 30

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GhostCallReceiver.ACTION_CALL_ENDED) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        parseIntent(intent)
        configureLockScreenAndWake()

        val filter = IntentFilter(GhostCallReceiver.ACTION_CALL_ENDED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        if (intent?.getBooleanExtra("EXTRA_AUTO_ANSWER", false) == true) {
            answerCall()
            return
        }

        setContent {
            IncomingCallScreen(
                callerName = callerName,
                callerNumber = callerNumber,
                identityMode = identityMode,
                onDecline = { declineCall() },
                onAnswer = { answerCall() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
        if (intent.getBooleanExtra("EXTRA_AUTO_ANSWER", false)) {
            answerCall()
        }
    }

    private fun parseIntent(intent: Intent?) {
        if (intent == null) return
        callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
        callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Rahul"
        callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "+91 9876543210"
        identityMode = intent.getStringExtra(EXTRA_IDENTITY_MODE) ?: "NAME_AND_NUMBER"
        durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 30)
    }

    @Suppress("DEPRECATION")
    private fun configureLockScreenAndWake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun declineCall() {
        GhostCallRingerService.stopRinging(this)
        NotificationHelper.cancelCallNotification(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                VidoApplication.instance.repository.recordCallHistory(
                    CallHistoryEntity(
                        callerName = callerName,
                        callerNumber = callerNumber,
                        identityMode = identityMode,
                        status = CallStatus.DECLINED.name,
                        durationSeconds = 0
                    )
                )
            } catch (ignored: Exception) {}
        }
        finish()
    }

    private fun answerCall() {
        GhostCallRingerService.stopRinging(this)
        NotificationHelper.cancelCallNotification(this)

        val activeIntent = Intent(this, ActiveCallActivity::class.java).apply {
            putExtra(ActiveCallActivity.EXTRA_CALL_ID, callId)
            putExtra(ActiveCallActivity.EXTRA_CALLER_NAME, callerName)
            putExtra(ActiveCallActivity.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(ActiveCallActivity.EXTRA_IDENTITY_MODE, identityMode)
            putExtra(ActiveCallActivity.EXTRA_DURATION_SECONDS, durationSeconds)
        }
        startActivity(activeIntent)
        finish()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(closeReceiver)
        } catch (ignored: Exception) {}
        super.onDestroy()
    }
}

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    identityMode: String,
    onDecline: () -> Unit,
    onAnswer: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val displayName = when (identityMode) {
        "UNKNOWN_CALLER" -> "Unknown Caller"
        "NUMBER_ONLY" -> callerNumber.ifBlank { "Unknown Caller" }
        else -> callerName.ifBlank { "Rahul" }
    }

    val displaySubtitle = when (identityMode) {
        "UNKNOWN_CALLER", "NAME_ONLY" -> null
        "NUMBER_ONLY" -> "Mobile"
        else -> callerNumber.ifBlank { null }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0F19)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0B0F19),
                            Color(0xFF050811)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Incoming Ghost Call",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Pulsing Avatar Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E293B), Color(0xFF334155))
                                    )
                                )
                                .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Caller Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (displaySubtitle != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displaySubtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vido Simulator",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Bottom Call Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline Action
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onDecline,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .testTag("decline_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Decline",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Decline",
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Answer Action
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onAnswer,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .testTag("answer_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Answer",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Answer",
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
