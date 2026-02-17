package com.example.myreminders_claude2.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myreminders_claude2.data.Reminder
import java.util.Calendar

@Composable
fun StatsScreen(
    activeCount: Int,
    missedCount: Int,
    doneCount: Int,
    allReminders: List<Reminder>,
    onNavigateBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // --- Calculations ---
    val totalCompleted = allReminders.count { it.isCompleted && !it.isDeleted }
    val totalManuallyDone = allReminders.count {
        it.isDeleted && it.dismissalReason == "MANUAL"
    }
    val totalEverCreated = allReminders.size
    val completionRate = if (totalEverCreated > 0)
        ((totalCompleted + totalManuallyDone).toFloat() / totalEverCreated * 100).toInt()
    else 0

    // Streak calculation
    val streak = calculateStreak(allReminders)

    // Rank based on completion rate
    val (rank, rankEmoji) = when {
        completionRate >= 90 -> "Unstoppable" to "🚀"
        completionRate >= 75 -> "On Point" to "⭐"
        completionRate >= 50 -> "Reliable" to "👍"
        completionRate >= 25 -> "Getting There" to "📈"
        else -> "Just Starting" to "🌱"
    }

    // Most used category
    val topCategory = allReminders
        .groupBy { it.mainCategory }
        .maxByOrNull { it.value.size }
        ?.key ?: "None"
    val topCategoryEmoji = when (topCategory) {
        "WORK" -> "💼"
        "PERSONAL" -> "🏠"
        "HEALTH" -> "❤️"
        "FINANCE" -> "💰"
        else -> "📌"
    }

    // Achievements
    val achievements = buildList {
        if (totalEverCreated >= 1) add("🌱" to "First Reminder")
        if (totalEverCreated >= 10) add("📋" to "10 Reminders")
        if (totalEverCreated >= 50) add("🏅" to "50 Reminders")
        if (totalCompleted + totalManuallyDone >= 10) add("✅" to "10 Completed")
        if (totalCompleted + totalManuallyDone >= 50) add("🏆" to "50 Completed")
        if (streak >= 3) add("🔥" to "3-Day Streak")
        if (streak >= 7) add("⚡" to "7-Day Streak")
        if (streak >= 30) add("💎" to "30-Day Streak")
        if (missedCount == 0 && totalEverCreated > 0) add("🎯" to "Zero Missed")
        if (completionRate == 100 && totalEverCreated >= 5) add("💯" to "Perfect Score")
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 50.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // --- Hero Card: Rank + Score ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(rankEmoji, fontSize = 48.sp)
                Text(
                    text = rank,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Completion Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$completionRate%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { completionRate / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // --- Stats Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "🔔",
                value = activeCount.toString(),
                label = "Active"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "⚠️",
                value = missedCount.toString(),
                label = "Missed"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "✅",
                value = doneCount.toString(),
                label = "Done"
            )
        }

        // --- Streak Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 Current Streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$streak days",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Text(
                    text = "Consecutive days with no missed reminders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Top Category Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$topCategoryEmoji ${topCategory.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Text(
                    text = "Your most used reminder category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Achievements ---
        if (achievements.isNotEmpty()) {
            Text(
                text = "🏅 My Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        .compositeOver(MaterialTheme.colorScheme.surface)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    achievements.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (emoji, label) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    .compositeOver(MaterialTheme.colorScheme.surface),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun calculateStreak(reminders: List<Reminder>): Int {
    if (reminders.isEmpty()) return 0

    // Cap streak at age of oldest reminder in days
    val oldestReminder = reminders.minByOrNull { it.createdAt } ?: return 0
    val appAgeInDays = ((System.currentTimeMillis() - oldestReminder.createdAt) /
            (1000 * 60 * 60 * 24)).toInt() + 1

    val calendar = Calendar.getInstance()
    var streak = 0
    var daysBack = 0

    while (daysBack < appAgeInDays) {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        val missedThatDay = reminders.any { reminder ->
            reminder.dismissalReason == "AUTO_SNOOZED" &&
                    reminder.completedAt != null &&
                    reminder.completedAt >= startOfDay &&
                    reminder.completedAt < endOfDay
        }

        if (missedThatDay) break
        streak++
        daysBack++
    }

    return streak
}