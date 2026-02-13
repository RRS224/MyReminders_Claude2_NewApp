package com.example.myreminders_claude2.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myreminders_claude2.data.RecurrenceType
import com.example.myreminders_claude2.data.Template
import com.example.myreminders_claude2.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTemplateScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    onReminderCreated: () -> Unit
) {
    val templates by viewModel.allTemplates.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var showDateTimePicker by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }
    var selectedDateTime by remember { mutableStateOf(System.currentTimeMillis() + (60 * 60 * 1000)) }

    val dateFormat =
        SimpleDateFormat("EEE, MMM dd yyyy 'at' hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Use Template") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                "Select a Template",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Choose a template to quickly create a reminder with preset details",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (templates.isEmpty()) {
                // Empty state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "📋",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Templates Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create a reminder and save it as a template to use it here!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Templates list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateSelectionCard(
                            template = template,
                            onClick = {
                                selectedTemplate = template
                                showDateTimePicker = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Date/Time Picker Dialog
    if (showDateTimePicker && selectedTemplate != null) {
        LaunchedEffect(Unit) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDateTime

            DatePickerDialog(
                context,
                { _, year, month, day ->
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            calendar.set(year, month, day, hour, minute, 0)
                            selectedDateTime = calendar.timeInMillis

                            // Create reminder from template
                            viewModel.createReminderFromTemplate(
                                template = selectedTemplate!!,
                                dateTime = selectedDateTime
                            )

                            // Navigate back
                            onReminderCreated()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener {
                    showDateTimePicker = false
                    selectedTemplate = null
                }
            }.show()
        }
    }
}

@Composable
private fun TemplateSelectionCard(
    template: Template,
    onClick: () -> Unit
) {
    // Get category emoji
    val categoryEmoji = when (template.mainCategory) {
        "WORK" -> "💼"
        "PERSONAL" -> "🏠"
        "HEALTH" -> "❤️"
        "FINANCE" -> "💰"
        else -> "📌"
    }

    // Get recurrence text
    val recurrenceText = when (template.recurrenceType) {
        RecurrenceType.ONE_TIME -> "One-time"
        RecurrenceType.HOURLY -> if (template.recurrenceInterval == 1) "Hourly" else "Every ${template.recurrenceInterval}h"
        RecurrenceType.DAILY -> if (template.recurrenceInterval == 1) "Daily" else "Every ${template.recurrenceInterval}d"
        RecurrenceType.WEEKLY -> if (template.recurrenceInterval == 1) "Weekly" else "Every ${template.recurrenceInterval}w"
        RecurrenceType.MONTHLY -> if (template.recurrenceInterval == 1) "Monthly" else "Every ${template.recurrenceInterval}m"
        RecurrenceType.ANNUAL -> if (template.recurrenceInterval == 1) "Yearly" else "Every ${template.recurrenceInterval}y"
        else -> "One-time"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Template name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📋",
                    style = MaterialTheme.typography.headlineMedium
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (template.usageCount > 0) {
                        Text(
                            text = "Used ${template.usageCount} time${if (template.usageCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider()

            // Content preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = categoryEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (template.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            // Details badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = template.subCategory ?: template.mainCategory,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Recurrence badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "🔁 $recurrenceText",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Voice badge
                if (template.isVoiceEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "🔊 Voice",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Action hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Tap to set date & time →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}