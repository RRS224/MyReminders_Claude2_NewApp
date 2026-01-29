package com.example.myreminders_claude2.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Reusable confirmation dialog for delete operations throughout the app
 *
 * Usage examples:
 *
 * 1. Clear All Completed:
 *    ConfirmationDialog(
 *        title = "Clear All Completed?",
 *        message = "This will permanently delete all completed reminders. This action cannot be undone.",
 *        onConfirm = { viewModel.clearAllCompleted() },
 *        onDismiss = { showDialog = false }
 *    )
 *
 * 2. Delete Single Reminder:
 *    ConfirmationDialog(
 *        title = "Delete Reminder?",
 *        message = "Are you sure you want to delete \"${reminder.title}\"?",
 *        onConfirm = { viewModel.deleteReminder(reminder) },
 *        onDismiss = { showDialog = false }
 *    )
 *
 * 3. Delete All Templates:
 *    ConfirmationDialog(
 *        title = "Delete All Templates?",
 *        message = "This will permanently delete all saved templates. This action cannot be undone.",
 *        confirmText = "Delete All",
 *        onConfirm = { viewModel.deleteAllTemplates() },
 *        onDismiss = { showDialog = false }
 *    )
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Yes",
    dismissText: String = "No",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Specialized version for delete operations with consistent styling
 */
@Composable
fun DeleteConfirmationDialog(
    title: String = "Delete?",
    itemName: String? = null,
    itemCount: Int? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = when {
        itemName != null -> "Are you sure you want to delete \"$itemName\"? This action cannot be undone."
        itemCount != null && itemCount > 1 -> "This will permanently delete $itemCount items. This action cannot be undone."
        itemCount == 1 -> "This will permanently delete this item. This action cannot be undone."
        else -> "This will permanently delete the selected item(s). This action cannot be undone."
    }

    ConfirmationDialog(
        title = title,
        message = message,
        confirmText = "Delete",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}