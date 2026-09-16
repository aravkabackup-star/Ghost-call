package com.example.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CallerIdentityMode
import com.example.data.local.entity.GhostCallEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CreateGhostCallScreen(
    onNavigateBack: () -> Unit,
    onSchedule: (GhostCallEntity) -> Unit
) {
    val context = LocalContext.current

    var callerName by remember { mutableStateOf("Rahul") }
    var callerNumber by remember { mutableStateOf("+91 9876543210") }
    var identityMode by remember { mutableStateOf(CallerIdentityMode.NAME_AND_NUMBER) }

    var isTempContact by remember { mutableStateOf(false) }
    var tempContactName by remember { mutableStateOf("") }

    // Timing
    val nowCalendar = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 2) } }
    var scheduledEpoch by remember { mutableLongStateOf(nowCalendar.timeInMillis) }
    var quickPresetSelected by remember { mutableStateOf("2m") }

    // Duration: 10s, 30s, 60s, 300s, -1 (until manual end)
    var durationSeconds by remember { mutableIntStateOf(30) }

    // Screen and Audio settings
    var vibrate by remember { mutableStateOf(true) }
    var wakeScreen by remember { mutableStateOf(true) }
    var showOverLock by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Schedule Ghost Call",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section A & B: Caller Identity
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CALLER DETAILS",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = callerName,
                        onValueChange = { callerName = it },
                        label = { Text("Caller Name (Optional)") },
                        placeholder = { Text("e.g. Rahul, Boss, Delivery") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = callerNumber,
                        onValueChange = { callerNumber = it },
                        label = { Text("Caller Number (Optional)") },
                        placeholder = { Text("e.g. +91 9876543210") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_number_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Caller Identity Presentation",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IdentityChip(
                            label = "Name + Number",
                            isSelected = identityMode == CallerIdentityMode.NAME_AND_NUMBER,
                            onClick = { identityMode = CallerIdentityMode.NAME_AND_NUMBER },
                            modifier = Modifier.weight(1f)
                        )
                        IdentityChip(
                            label = "Number Only",
                            isSelected = identityMode == CallerIdentityMode.NUMBER_ONLY,
                            onClick = { identityMode = CallerIdentityMode.NUMBER_ONLY },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IdentityChip(
                            label = "Name Only",
                            isSelected = identityMode == CallerIdentityMode.NAME_ONLY,
                            onClick = { identityMode = CallerIdentityMode.NAME_ONLY },
                            modifier = Modifier.weight(1f)
                        )
                        IdentityChip(
                            label = "Unknown",
                            isSelected = identityMode == CallerIdentityMode.UNKNOWN_CALLER,
                            onClick = { identityMode = CallerIdentityMode.UNKNOWN_CALLER },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Contact Option: Temporary Contact
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTempContact = !isTempContact }
                    ) {
                        Checkbox(
                            checked = isTempContact,
                            onCheckedChange = { isTempContact = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF0284C7),
                                checkmarkColor = Color.White
                            )
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = "Save as temporary contact",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Internal sandbox only; cleared when call finishes",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = isTempContact) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = tempContactName,
                                onValueChange = { tempContactName = it },
                                label = { Text("Temporary Contact Name") },
                                placeholder = { Text("e.g. Rahul (Temporary)") },
                                singleLine = true,
                                colors = outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Section E: Call Timing (Date & Time Picker)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SCHEDULE TIME",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickPresetChip(
                            label = "In 10s",
                            isSelected = quickPresetSelected == "10s",
                            onClick = {
                                quickPresetSelected = "10s"
                                scheduledEpoch = System.currentTimeMillis() + 10_000L
                            }
                        )
                        QuickPresetChip(
                            label = "In 30s",
                            isSelected = quickPresetSelected == "30s",
                            onClick = {
                                quickPresetSelected = "30s"
                                scheduledEpoch = System.currentTimeMillis() + 30_000L
                            }
                        )
                        QuickPresetChip(
                            label = "In 1m",
                            isSelected = quickPresetSelected == "1m",
                            onClick = {
                                quickPresetSelected = "1m"
                                scheduledEpoch = System.currentTimeMillis() + 60_000L
                            }
                        )
                        QuickPresetChip(
                            label = "In 5m",
                            isSelected = quickPresetSelected == "5m",
                            onClick = {
                                quickPresetSelected = "5m"
                                scheduledEpoch = System.currentTimeMillis() + 300_000L
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val timeFormatter = SimpleDateFormat("EEE, MMM d, yyyy  •  h:mm a", Locale.getDefault())
                    val formattedDate = timeFormatter.format(Date(scheduledEpoch))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                quickPresetSelected = "custom"
                                showDateTimePicker(context, scheduledEpoch) { newEpoch ->
                                    scheduledEpoch = newEpoch
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = formattedDate,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to pick custom date & time",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section F: Simulated Call Duration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CALL DURATION (SIMULATED)",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val durations = listOf(
                        10 to "10s",
                        30 to "30s",
                        60 to "1 min",
                        300 to "5 min",
                        -1 to "Manual end"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durations.forEach { (sec, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (durationSeconds == sec) Color(0xFF0284C7) else Color(0xFF1E293B))
                                    .clickable { durationSeconds = sec }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (durationSeconds == sec) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section G, H, I: Audio & Screen Behavior
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RINGTONE, VIBRATION & SCREEN",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Vibrate on call",
                        subtitle = "Device vibration pattern for incoming simulation",
                        icon = Icons.Default.Vibration,
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )

                    SettingToggleRow(
                        title = "Wake screen",
                        subtitle = "Turn screen on when scheduled call arrives",
                        icon = Icons.Default.Visibility,
                        checked = wakeScreen,
                        onCheckedChange = { wakeScreen = it }
                    )

                    SettingToggleRow(
                        title = "Show over lock screen",
                        subtitle = "Present incoming call while device remains locked",
                        icon = Icons.Default.Visibility,
                        checked = showOverLock,
                        onCheckedChange = { showOverLock = it }
                    )
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    val call = GhostCallEntity(
                        callerName = callerName.ifBlank { "Rahul" },
                        callerNumber = callerNumber.ifBlank { "+91 9876543210" },
                        identityMode = identityMode.name,
                        isTemporaryContact = isTempContact,
                        temporaryContactName = if (isTempContact) tempContactName.ifBlank { callerName } else "",
                        scheduledEpochMillis = scheduledEpoch,
                        simulatedDurationSeconds = durationSeconds,
                        vibrate = vibrate,
                        wakeScreen = wakeScreen,
                        showOverLock = showOverLock,
                        isRecurring = false,
                        isEnabled = true
                    )
                    onSchedule(call)
                    Toast.makeText(context, "Ghost Call Scheduled!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_schedule_call_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCHEDULE GHOST CALL", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun IdentityChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuickPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = Color(0xFF64748B), fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0284C7)
            )
        )
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF38BDF8),
    unfocusedBorderColor = Color(0xFF334155),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF38BDF8),
    unfocusedLabelColor = Color(0xFF94A3B8),
    cursorColor = Color(0xFF38BDF8)
)

private fun showDateTimePicker(
    context: Context,
    initialEpoch: Long,
    onEpochSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialEpoch }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onEpochSelected(cal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
