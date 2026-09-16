package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.history.CallHistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.recurring.RecurringCallsScreen
import com.example.ui.schedule.CreateGhostCallScreen
import com.example.ui.schedule.ScheduledCallsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.VidoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VidoTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

sealed class VidoScreen {
    data object Home : VidoScreen()
    data object CreateCall : VidoScreen()
    data object ScheduledCalls : VidoScreen()
    data object RecurringCalls : VidoScreen()
    data object CallHistory : VidoScreen()
    data object Settings : VidoScreen()
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<VidoScreen>(VidoScreen.Home) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val scheduledCalls by viewModel.scheduledCalls.collectAsState()
    val recurringCalls by viewModel.recurringCalls.collectAsState()
    val callHistory by viewModel.callHistory.collectAsState()
    val nextUpcomingCall by viewModel.nextUpcomingCall.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    VidoScreen.Home -> {
                        HomeScreen(
                            nextCall = nextUpcomingCall,
                            scheduledCount = scheduledCalls.size,
                            recurringCount = recurringCalls.size,
                            historyCount = callHistory.size,
                            onNavigateToCreate = { currentScreen = VidoScreen.CreateCall },
                            onNavigateToScheduled = { currentScreen = VidoScreen.ScheduledCalls },
                            onNavigateToRecurring = { currentScreen = VidoScreen.RecurringCalls },
                            onNavigateToHistory = { currentScreen = VidoScreen.CallHistory },
                            onNavigateToSettings = { currentScreen = VidoScreen.Settings },
                            onTestNow = { viewModel.triggerTestCallNow(context) },
                            onTestIn10s = { viewModel.triggerTestCallInSeconds(context, 10) }
                        )
                    }

                    VidoScreen.CreateCall -> {
                        CreateGhostCallScreen(
                            onNavigateBack = { currentScreen = VidoScreen.Home },
                            onSchedule = { call ->
                                viewModel.scheduleGhostCall(call, context) {
                                    currentScreen = VidoScreen.ScheduledCalls
                                }
                            }
                        )
                    }

                    VidoScreen.ScheduledCalls -> {
                        ScheduledCallsScreen(
                            calls = scheduledCalls,
                            onNavigateBack = { currentScreen = VidoScreen.Home },
                            onNavigateToCreate = { currentScreen = VidoScreen.CreateCall },
                            onToggleEnabled = { call, enabled ->
                                viewModel.toggleCallEnabled(call, enabled, context)
                            },
                            onDeleteCall = { id ->
                                viewModel.deleteCall(id, context)
                            }
                        )
                    }

                    VidoScreen.RecurringCalls -> {
                        RecurringCallsScreen(
                            recurringCalls = recurringCalls,
                            onNavigateBack = { currentScreen = VidoScreen.Home },
                            onSaveRecurring = { call ->
                                viewModel.scheduleGhostCall(call, context) {}
                            },
                            onToggleEnabled = { call, enabled ->
                                viewModel.toggleCallEnabled(call, enabled, context)
                            },
                            onDelete = { id ->
                                viewModel.deleteCall(id, context)
                            }
                        )
                    }

                    VidoScreen.CallHistory -> {
                        CallHistoryScreen(
                            historyList = callHistory,
                            onNavigateBack = { currentScreen = VidoScreen.Home },
                            onDeleteEntry = { id -> viewModel.deleteHistory(id) },
                            onClearAll = { viewModel.clearAllHistory() }
                        )
                    }

                    VidoScreen.Settings -> {
                        SettingsScreen(
                            settings = settingsState,
                            onNavigateBack = { currentScreen = VidoScreen.Home },
                            onUpdateRingtone = { uri -> viewModel.updateSettings(ringtoneUri = uri) },
                            onUpdateVibrate = { vib -> viewModel.updateSettings(vibrate = vib) },
                            onUpdateDuration = { dur -> viewModel.updateSettings(durationSeconds = dur) },
                            onUpdateShowOnLock = { show -> viewModel.updateSettings(showOnLock = show) },
                            onUpdateWakeScreen = { wake -> viewModel.updateSettings(wakeScreen = wake) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

