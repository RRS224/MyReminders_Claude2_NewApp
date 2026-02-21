package com.example.myreminders_claude2.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val microphonePermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Check exact alarm permission manually
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    var canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    // Battery optimization state
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isBatteryOptimized by remember {
        mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    // Detect OEM for manufacturer-specific instructions
    val manufacturer = remember { android.os.Build.MANUFACTURER.lowercase() }
    val batteryInstructions = remember(manufacturer) {
        when {
            manufacturer.contains("samsung") ->
                "Settings → Apps → MyReminders → Battery → set to Unrestricted. Also check Settings → Battery → Background usage limits → Sleeping apps and remove MyReminders if listed."
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ->
                "Settings → Apps → MyReminders → Battery Saver → set to No restrictions"
            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                "Settings → Apps → MyReminders → Battery → disable Power-intensive prompt"
            manufacturer.contains("oneplus") ->
                "Settings → Battery → Battery Optimization → MyReminders → Don't optimize"
            manufacturer.contains("oppo") || manufacturer.contains("realme") ->
                "Settings → Battery → Battery Optimization → MyReminders → Don't optimize"
            manufacturer.contains("vivo") ->
                "Settings → Battery → High Background Power Consumption → add MyReminders"
            else ->
                "Settings → Battery → Battery Optimization → MyReminders → Don't optimize"
        }
    }

    // Recheck alarm permission when screen resumes
    LaunchedEffect(Unit) {
        isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    val allGranted = remember(
        notificationPermission?.status,
        canScheduleExactAlarms,
        microphonePermission.status
    ) {
        val notifGranted = notificationPermission?.status?.isGranted ?: true
        val micGranted = microphonePermission.status.isGranted

        notifGranted && canScheduleExactAlarms && micGranted && !isBatteryOptimized
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Permissions",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Enable Permissions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To ensure your reminders work reliably, please grant the following permissions:",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notification Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermission != null) {
                    PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Show alarm notifications when reminders are due",
                        isGranted = notificationPermission.status.isGranted,
                        onRequestPermission = { notificationPermission.launchPermissionRequest() }
                    )
                }

                // Exact Alarm Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PermissionCard(
                        icon = Icons.Default.AlarmOn,
                        title = "Exact Alarms",
                        description = "Trigger reminders at the precise time you set",
                        isGranted = canScheduleExactAlarms,
                        onRequestPermission = { openExactAlarmSettings() }
                    )
                }

                // Microphone Permission
                PermissionCard(
                    icon = Icons.Default.Mic,
                    title = "Microphone",
                    description = "Create reminders using voice input",
                    isGranted = microphonePermission.status.isGranted,
                    onRequestPermission = { microphonePermission.launchPermissionRequest() }
                )

                // Battery Optimization Card
                PermissionCard(
                    icon = Icons.Default.BatteryFull,
                    title = "Battery Optimization",
                    description = if (isBatteryOptimized)
                        "Required for reliable alarms on ${android.os.Build.MANUFACTURER}. Tap to fix:\n$batteryInstructions"
                    else
                        "Alarms will fire reliably in the background",
                    isGranted = !isBatteryOptimized,
                    onRequestPermission = {
                        // Open battery optimization settings directly
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (allGranted) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Continue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                when {
                                    notificationPermission?.status?.isGranted == false -> {
                                        notificationPermission.launchPermissionRequest()
                                    }
                                    !canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                        openExactAlarmSettings()
                                    }
                                    !microphonePermission.status.isGranted -> {
                                        microphonePermission.launchPermissionRequest()
                                    }
                                    isBatteryOptimized -> {
                                        try {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Grant Permissions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Skip for Now",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                IconButton(onClick = onRequestPermission) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Grant",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}