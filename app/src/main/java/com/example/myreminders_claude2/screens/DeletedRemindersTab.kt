package com.example.myreminders_claude2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myreminders_claude2.data.Reminder
import com.example.myreminders_claude2.viewmodel.ReminderViewModel
import com.example.myreminders_claude2.performHapticFeedback

@Composable
fun DeletedRemindersTab(
    viewModel: ReminderViewModel,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val deletedReminders by viewModel.deletedReminders.collectAsState(initial = emptyList())
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            "Dismissed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
// Subtitle + Clear All on same row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${deletedReminders.size} dismissed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (deletedReminders.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        performHapticFeedback(context)
                        showClearAllDialog = true
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (deletedReminders.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxSize(),
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
                        "🗑️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Deleted Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Deleted reminders appear here and are kept for 7 days or until 20 are reached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Deleted reminders list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deletedReminders, key = { it.id }) { reminder ->
                    DeletedReminderCard(
                        reminder = reminder,
                        onUndelete = {
                            performHapticFeedback(context)
                            onNavigateToEdit(reminder.id)
                        },
                        onPermanentDelete = {
                            performHapticFeedback(context)
                            selectedReminder = reminder
                            showPermanentDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Deleted?") },
            text = {
                Text("This will permanently delete all ${deletedReminders.size} deleted reminders. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performHapticFeedback(context)
                        viewModel.permanentlyDeleteAll()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permanent Delete Dialog
    // ✅ FIX: Use let to safely unpack selectedReminder — eliminates !! crash risk.
    if (showPermanentDeleteDialog) {
        selectedReminder?.let { reminder ->
            AlertDialog(
                onDismissRequest = {
                    showPermanentDeleteDialog = false
                    selectedReminder = null
                },
                title = { Text("Delete Forever?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This will permanently delete:")
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                reminder.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            "This action cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            performHapticFeedback(context)
                            viewModel.permanentlyDeleteSingle(reminder.id)
                            showPermanentDeleteDialog = false
                            selectedReminder = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Forever")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPermanentDeleteDialog = false
                        selectedReminder = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DeletedReminderCard(
    reminder: Reminder,
    onUndelete: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    // Get category emoji
    val categoryEmoji = when (reminder.mainCategory) {
        "WORK" -> "💼"
        "PERSONAL" -> "🏠"
        "HEALTH" -> "❤️"
        "FINANCE" -> "💰"
        else -> "📌"
    }

    // Calculate how long ago it was deleted
    val deletedAgo = if (reminder.deletedAt != null) {
        val diff = System.currentTimeMillis() - reminder.deletedAt
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = diff / (1000 * 60 * 60)
        val minutes = diff / (1000 * 60)

        when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    } else {
        "Unknown"
    }

    // Original status
    val wasStatus = if (reminder.isCompleted) "Completed" else "Active"

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Category + Title
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = categoryEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (reminder.notes.isNotBlank()) {
                        Text(
                            text = reminder.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Status info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Dismissed $deletedAgo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUndelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }

                OutlinedButton(
                    onClick = onPermanentDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}