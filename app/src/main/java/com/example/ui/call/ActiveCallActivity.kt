package com.example.ui.call

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhonePaused
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VidoApplication
import com.example.data.local.entity.CallHistoryEntity
import com.example.data.local.entity.CallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActiveCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_IDENTITY_MODE = "EXTRA_IDENTITY_MODE"
        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
    }

    private var callId: Long = 0L
    private var callerName: String = "Rahul"
    private var callerNumber: String = "+91 9876543210"
    private var identityMode: String = "NAME_AND_NUMBER"
    private var targetDurationSeconds: Int = -1

    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
        callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Rahul"
        callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "+91 9876543210"
        identityMode = intent.getStringExtra(EXTRA_IDENTITY_MODE) ?: "NAME_AND_NUMBER"
        targetDurationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, -1)

        configureLockScreen()

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
        } catch (ignored: Exception) {}

        setContent {
            ActiveCallScreen(
                callerName = callerName,
                callerNumber = callerNumber,
                identityMode = identityMode,
                targetDurationSeconds = targetDurationSeconds,
                onPlayTone = { digit -> playDialTone(digit) },
                onEndCall = { durationSecs -> endCall(durationSecs) }
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun configureLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun playDialTone(digit: Char) {
        try {
            val tone = when (digit) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_DTMF_0
            }
            toneGenerator?.startTone(tone, 120)
        } catch (ignored: Exception) {}
    }

    private fun endCall(durationSecs: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VidoApplication.instance.repository.recordCallHistory(
                    CallHistoryEntity(
                        callerName = callerName,
                        callerNumber = callerNumber,
                        identityMode = identityMode,
                        status = CallStatus.ANSWERED.name,
                        durationSeconds = durationSecs
                    )
                )
            } catch (ignored: Exception) {}
        }
        Toast.makeText(this, "Simulated call ended", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (ignored: Exception) {}
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCallScreen(
    callerName: String,
    callerNumber: String,
    identityMode: String,
    targetDurationSeconds: Int,
    onPlayTone: (Char) -> Unit,
    onEndCall: (Int) -> Unit
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    // Second simulated caller state
    var secondaryCallerName by remember { mutableStateOf<String?>(null) }
    var showAddCallerDialog by remember { mutableStateOf(false) }
    var newCallerInput by remember { mutableStateOf("") }
    var newNumberInput by remember { mutableStateOf("") }

    // Sheets
    var showKeypadSheet by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showCallDetailsDialog by remember { mutableStateOf(false) }

    // Timer loop
    LaunchedEffect(isOnHold) {
        while (true) {
            delay(1000L)
            if (!isOnHold) {
                elapsedSeconds++
                if (targetDurationSeconds in 1..elapsedSeconds) {
                    onEndCall(elapsedSeconds)
                    break
                }
            }
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val displayName = when (identityMode) {
        "UNKNOWN_CALLER" -> "Unknown Caller"
        "NUMBER_ONLY" -> callerNumber.ifBlank { "Unknown Caller" }
        else -> callerName.ifBlank { "Rahul" }
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
                            Color(0xFF030712)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Info & Timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    // Recording & Hold Banners
                    AnimatedVisibility(visible = isRecording) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Simulated Call Recording",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isOnHold) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhonePaused,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Call on Hold",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (identityMode != "UNKNOWN_CALLER" && identityMode != "NAME_ONLY" && callerNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = callerNumber,
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isOnHold) "Paused ($timeFormatted)" else timeFormatted,
                        color = if (isOnHold) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    // Secondary Simulated Call Status Banner (if active)
                    if (secondaryCallerName != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayName — On Hold",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$secondaryCallerName — Calling (Simulated)",
                                    color = Color(0xFF34D399),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Middle: 3x2 Control Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Row 1: Mute | Keypad | Speaker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControlButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = "Mute",
                            isActive = isMuted,
                            onClick = { isMuted = !isMuted },
                            tag = "mute_button"
                        )
                        CallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = showKeypadSheet,
                            onClick = { showKeypadSheet = true },
                            tag = "keypad_button"
                        )
                        CallControlButton(
                            icon = Icons.Default.VolumeUp,
                            label = "Speaker",
                            isActive = isSpeakerOn,
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            tag = "speaker_button"
                        )
                    }

                    // Row 2: Hold | Add Call | More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControlButton(
                            icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (isOnHold) "Resume" else "Hold",
                            isActive = isOnHold,
                            activeColor = Color(0xFFF59E0B),
                            onClick = { isOnHold = !isOnHold },
                            tag = "hold_button"
                        )
                        CallControlButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Add call",
                            isActive = secondaryCallerName != null,
                            onClick = { showAddCallerDialog = true },
                            tag = "add_call_button"
                        )
                        CallControlButton(
                            icon = Icons.Default.MoreVert,
                            label = "More",
                            isActive = false,
                            onClick = { showMoreMenuSheet = true },
                            tag = "more_button"
                        )
                    }
                }

                // Bottom: Big Red End Call Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onEndCall(elapsedSeconds) },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }

    // Keypad Bottom Sheet
    if (showKeypadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showKeypadSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Keypad",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val keys = listOf(
                    "1", "2", "3",
                    "4", "5", "6",
                    "7", "8", "9",
                    "*", "0", "#"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(keys) { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    onPlayTone(key.first())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // More Options Bottom Sheet
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Call Options",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                MoreMenuItem(
                    icon = Icons.Default.FiberManualRecord,
                    title = if (isRecording) "Stop simulated recording" else "Record call (Simulated)",
                    color = if (isRecording) Color(0xFFEF4444) else Color.White,
                    onClick = {
                        isRecording = !isRecording
                        showMoreMenuSheet = false
                    }
                )

                MoreMenuItem(
                    icon = Icons.Default.PersonAdd,
                    title = "Add simulated caller",
                    onClick = {
                        showMoreMenuSheet = false
                        showAddCallerDialog = true
                    }
                )

                MoreMenuItem(
                    icon = Icons.Default.Dialpad,
                    title = "Dial Keypad",
                    onClick = {
                        showMoreMenuSheet = false
                        showKeypadSheet = true
                    }
                )

                MoreMenuItem(
                    icon = Icons.Default.Info,
                    title = "Call Details & Verification",
                    onClick = {
                        showMoreMenuSheet = false
                        showCallDetailsDialog = true
                    }
                )

                HorizontalDivider(
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                MoreMenuItem(
                    icon = Icons.Default.CallEnd,
                    title = "End Call",
                    color = Color(0xFFEF4444),
                    onClick = {
                        showMoreMenuSheet = false
                        onEndCall(elapsedSeconds)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Second Simulated Caller Dialog
    if (showAddCallerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCallerDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Add Simulated Caller", color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "This places $displayName on hold and initiates a second simulated call.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCallerInput,
                        onValueChange = { newCallerInput = it },
                        label = { Text("Caller Name") },
                        placeholder = { Text("e.g. Aman") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNumberInput,
                        onValueChange = { newNumberInput = it },
                        label = { Text("Caller Number") },
                        placeholder = { Text("+91 9123456789") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        secondaryCallerName = newCallerInput.ifBlank { "Aman" }
                        isOnHold = true
                        showAddCallerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Call (Simulated)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCallerDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Call Details Dialog
    if (showCallDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showCallDetailsDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Simulated Call Details", color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Caller", displayName)
                    DetailRow("Number", callerNumber.ifBlank { "Private" })
                    DetailRow("Duration", timeFormatted)
                    DetailRow("Audio", "Simulated Silent Loop (Safe)")
                    DetailRow("Network", "Local Sandbox Only")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Vido Ghost Call simulator does not connect to carrier networks or record real people.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCallDetailsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = Color(0xFF38BDF8),
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xFF1E293B))
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) activeColor else Color(0xFF334155),
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
