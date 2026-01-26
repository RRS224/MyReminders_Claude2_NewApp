package com.example.myreminders_claude2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateReminderTab(
    onNavigateToManual: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToTemplates: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text = "How would you like to\ncreate your reminder?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Manual Entry Card
        CreateMethodCard(
            icon = "✏️",
            title = "Manual Entry",
            description = "Type your reminder details",
            onClick = onNavigateToManual
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Voice Input Card
        CreateMethodCard(
            icon = "🎤",
            title = "Voice Input",
            description = "Speak your reminder naturally",
            onClick = onNavigateToVoice
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Templates Card
        CreateMethodCard(
            icon = "📋",
            title = "Use Template",
            description = "Quick add from saved templates",
            onClick = onNavigateToTemplates,
            badge = "Coming Soon"
        )
    }
}

@Composable
fun CreateMethodCard(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    badge: String? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 48.sp,
                modifier = Modifier.padding(end = 20.dp)
            )

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Badge (if any)
                    badge?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}