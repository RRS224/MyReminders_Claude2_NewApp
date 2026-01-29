package com.example.myreminders_claude2.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myreminders_claude2.utils.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: () -> Unit,
    onNavigateToManageCategories: () -> Unit = {},
    onNavigateToManageTemplates: () -> Unit = {}
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }

    var selectedTheme by remember { mutableStateOf(themePrefs.themeMode) }
    var useAmoled by remember { mutableStateOf(themePrefs.useAmoledBlack) }
    var useDynamicColors by remember { mutableStateOf(themePrefs.useDynamicColors) }
    var autoDarkEnabled by remember { mutableStateOf(themePrefs.autoDarkEnabled) }
    var autoDarkStartHour by remember { mutableStateOf(themePrefs.autoDarkStartHour) }
    var autoDarkEndHour by remember { mutableStateOf(themePrefs.autoDarkEndHour) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Categories Section
            Text(
                "🏷️ Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = onNavigateToManageCategories
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Manage Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Add, edit, or delete custom reminder categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Manage Categories",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Templates Section
            Text(
                "📋 Templates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = onNavigateToManageTemplates
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Manage Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "View, edit, or delete saved reminder templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Manage Templates",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Theme Section
            Text(
                "🎨 Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Theme Mode Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Light Theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == ThemePreferences.THEME_LIGHT,
                            onClick = {
                                selectedTheme = ThemePreferences.THEME_LIGHT
                                themePrefs.themeMode = ThemePreferences.THEME_LIGHT
                                onThemeChanged()
                            }
                        )
                        Text("☀️ Light", modifier = Modifier.padding(start = 8.dp))
                    }

                    // Dark Theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == ThemePreferences.THEME_DARK,
                            onClick = {
                                selectedTheme = ThemePreferences.THEME_DARK
                                themePrefs.themeMode = ThemePreferences.THEME_DARK
                                onThemeChanged()
                            }
                        )
                        Text("🌙 Dark", modifier = Modifier.padding(start = 8.dp))
                    }

                    // System Theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == ThemePreferences.THEME_SYSTEM,
                            onClick = {
                                selectedTheme = ThemePreferences.THEME_SYSTEM
                                themePrefs.themeMode = ThemePreferences.THEME_SYSTEM
                                onThemeChanged()
                            }
                        )
                        Text("🌓 System Default", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // AMOLED Black Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "⚫ AMOLED Black",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pure black background for OLED screens (saves battery)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useAmoled,
                        onCheckedChange = {
                            useAmoled = it
                            themePrefs.useAmoledBlack = it
                            onThemeChanged()
                        }
                    )
                }
            }

            // Dynamic Colors (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🎨 Dynamic Colors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Match colors to your wallpaper (Material You)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = {
                                useDynamicColors = it
                                themePrefs.useDynamicColors = it
                                onThemeChanged()
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Auto Dark Mode Section
            Text(
                "🕐 Auto Dark Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Time-Based Auto Switch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Automatically switch to dark mode at set times",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoDarkEnabled,
                            onCheckedChange = {
                                autoDarkEnabled = it
                                themePrefs.autoDarkEnabled = it
                                if (it) onThemeChanged()
                            }
                        )
                    }

                    if (autoDarkEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Start Time
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dark Mode Starts: ${formatHour(autoDarkStartHour)}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // End Time
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dark Mode Ends: ${formatHour(autoDarkEndHour)}")
                        }
                    }
                }
            }

            HorizontalDivider()

            // App Info
            Text(
                "ℹ️ About",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "My Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version 1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "My Reminders helps you stay organized with smart notifications, " +
                                "voice input, custom categories, templates, and cloud sync. " +
                                "Built to make managing your daily tasks simple and efficient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Time Pickers (Simple hour selection)
    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time") },
            text = {
                Column {
                    (0..23).forEach { hour ->
                        TextButton(
                            onClick = {
                                autoDarkStartHour = hour
                                themePrefs.autoDarkStartHour = hour
                                showStartTimePicker = false
                                onThemeChanged()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                formatHour(hour),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select End Time") },
            text = {
                Column {
                    (0..23).forEach { hour ->
                        TextButton(
                            onClick = {
                                autoDarkEndHour = hour
                                themePrefs.autoDarkEndHour = hour
                                showEndTimePicker = false
                                onThemeChanged()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                formatHour(hour),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 $period"
}