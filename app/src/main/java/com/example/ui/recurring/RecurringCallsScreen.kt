package com.example.ui.recurring

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.scheduler.GhostCallScheduler
import com.example.ui.schedule.outlinedTextFieldColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCallsScreen(
    recurringCalls: List<GhostCallEntity>,
    onNavigateBack: () -> Unit,
    onSaveRecurring: (GhostCallEntity) -> Unit,
    onToggleEnabled: (GhostCallEntity, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                    Column {
                        Text(
                            text = "Recurring Calls",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scheduled repeating simulated calls",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (recurringCalls.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Recurring Schedules",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Set up repeating ghost calls for specific weekdays or daily.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(recurringCalls, key = { it.id }) { call ->
                    RecurringCallItem(
                        call = call,
                        onToggle = { enabled -> onToggleEnabled(call, enabled) },
                        onDelete = {
                            onDelete(call.id)
                            Toast.makeText(context, "Recurring schedule deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            containerColor = Color(0xFF0284C7),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_recurring_call_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Recurring")
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0F172A)
        ) {
            AddRecurringCallContent(
                onDismiss = { showAddSheet = false },
                onSave = { newCall ->
                    onSaveRecurring(newCall)
                    showAddSheet = false
                    Toast.makeText(context, "Recurring Call Saved!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun RecurringCallItem(
    call: GhostCallEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = call.scheduledEpochMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val timeFormatted = String.format("%d:%02d %s", displayHour, minute, amPm)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = null,
                        tint = if (call.isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = call.callerName.ifBlank { "Rahul" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeFormatted  •  ${call.recurrenceDays.ifBlank { "Daily" }}",
                        color = if (call.isEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Duration: ${if (call.simulatedDurationSeconds > 0) "${call.simulatedDurationSeconds}s" else "Manual end"}",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = call.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddRecurringCallContent(
    onDismiss: () -> Unit,
    onSave: (GhostCallEntity) -> Unit
) {
    val context = LocalContext.current
    var callerName by remember { mutableStateOf("Rahul") }
    var callerNumber by remember { mutableStateOf("+91 9876543210") }
    var hourOfDay by remember { mutableIntStateOf(19) } // 7:30 PM default
    var minuteOfHour by remember { mutableIntStateOf(30) }
    var durationSeconds by remember { mutableIntStateOf(60) }

    // Repeat mode: "DAILY", "WEEKDAYS", "CUSTOM"
    var repeatMode by remember { mutableStateOf("CUSTOM") }
    var selectedDays by remember { mutableStateOf(setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)) }

    val amPm = if (hourOfDay >= 12) "PM" else "AM"
    val displayHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
    val timeFormatted = String.format("%d:%02d %s", displayHour, minuteOfHour, amPm)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Create Recurring Ghost Call",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = callerName,
            onValueChange = { callerName = it },
            label = { Text("Caller Name") },
            placeholder = { Text("e.g. Rahul") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8)) },
            singleLine = true,
            colors = outlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = callerNumber,
            onValueChange = { callerNumber = it },
            label = { Text("Caller Number") },
            placeholder = { Text("+91 9876543210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF94A3B8)) },
            singleLine = true,
            colors = outlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Time Picker Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            hourOfDay = h
                            minuteOfHour = m
                        },
                        hourOfDay,
                        minuteOfHour,
                        false
                    ).show()
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
                        Text(text = "Trigger Time", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(text = timeFormatted, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(text = "Change", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "Repeat Days", color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        // Day of week chips
        val days = listOf(
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F",
            Calendar.SATURDAY to "S",
            Calendar.SUNDAY to "S"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { (dayInt, label) ->
                val isSelected = selectedDays.contains(dayInt)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
                        .clickable {
                            selectedDays = if (isSelected) {
                                selectedDays - dayInt
                            } else {
                                selectedDays + dayInt
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val nextEpoch = GhostCallScheduler.calculateNextOccurrence(
                    hourOfDay,
                    minuteOfHour,
                    selectedDays
                )
                val daysString = when {
                    selectedDays.size == 7 -> "DAILY"
                    selectedDays == setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY) -> "WEEKDAYS"
                    else -> selectedDays.sorted().joinToString(",") {
                        when (it) {
                            Calendar.SUNDAY -> "SUN"
                            Calendar.MONDAY -> "MON"
                            Calendar.TUESDAY -> "TUE"
                            Calendar.WEDNESDAY -> "WED"
                            Calendar.THURSDAY -> "THU"
                            Calendar.FRIDAY -> "FRI"
                            Calendar.SATURDAY -> "SAT"
                            else -> ""
                        }
                    }
                }

                val newCall = GhostCallEntity(
                    callerName = callerName.ifBlank { "Rahul" },
                    callerNumber = callerNumber.ifBlank { "+91 9876543210" },
                    identityMode = CallerIdentityMode.NAME_AND_NUMBER.name,
                    scheduledEpochMillis = nextEpoch,
                    simulatedDurationSeconds = durationSeconds,
                    isRecurring = true,
                    recurrenceDays = daysString,
                    isEnabled = true
                )
                onSave(newCall)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_recurring_call_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            Text("SAVE RECURRING CALL", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
