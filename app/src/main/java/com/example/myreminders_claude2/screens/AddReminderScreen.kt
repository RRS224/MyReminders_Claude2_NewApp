package com.example.myreminders_claude2.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myreminders_claude2.data.CategoryDefaults
import com.example.myreminders_claude2.data.RecurrenceType
import com.example.myreminders_claude2.utils.CategoryManager
import com.example.myreminders_claude2.viewmodel.ReminderViewModel
import com.example.myreminders_claude2.utils.TextFormatter
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Notifications
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import com.example.myreminders_claude2.data.CircleManager
import com.example.myreminders_claude2.data.CircleGroup
import com.example.myreminders_claude2.data.CircleGroupManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    prefilledTitle: String = "",
    prefilledNotes: String = "",
    prefilledDateTime: Long = System.currentTimeMillis() + (60 * 60 * 1000), // 1 hour from now
    prefilledCategory: String = "GENERAL",
    prefilledSubCategory: String? = null,
    prefilledRecurrenceType: String = RecurrenceType.ONE_TIME,
    prefilledRecurrenceInterval: Int = 1,
    prefilledRecurrenceDayOfWeek: Int? = null
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager(context) }
    val allCategories = categoryManager.getAllCategories()
    val scope = rememberCoroutineScope()

    // My Circle send state
    val currentUser = FirebaseAuth.getInstance().currentUser
    var connections by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var showSendSheet by remember { mutableStateOf(false) }
    var sendToSelf by remember { mutableStateOf(true) }
    var selectedConnections by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Local groups
    var groups by remember { mutableStateOf(CircleGroupManager.getGroups(context)) }

    // Load connections if signed in
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            connections = CircleManager.getConnections()
            groups = CircleGroupManager.getGroups(context)
        }
    }


    var reminderText by remember { mutableStateOf(prefilledTitle) }
    var notes by remember { mutableStateOf(prefilledNotes) }
    var selectedDateTime by remember { mutableStateOf(prefilledDateTime) }
    var isVoiceEnabled by remember { mutableStateOf(true) }
    var showSaveAsTemplateDialog by remember { mutableStateOf(false) }

    // Category states - initialize from prefilled values
    var mainCategory by remember {
        mutableStateOf(
            when (prefilledCategory) {
                "WORK" -> CategoryDefaults.WORK
                "PERSONAL" -> CategoryDefaults.PERSONAL
                "HEALTH" -> CategoryDefaults.HEALTH
                "FINANCE" -> CategoryDefaults.FINANCE
                else -> CategoryDefaults.PERSONAL
            }
        )
    }
    var reminderType by remember { mutableStateOf(prefilledSubCategory ?: "General") }
    var customType by remember { mutableStateOf("") }
    var showMainCategoryMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    // Recurrence states - initialize from prefilled values
    var recurrenceType by remember { mutableStateOf(prefilledRecurrenceType) }
    var recurrenceInterval by remember { mutableStateOf(prefilledRecurrenceInterval) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }

    // ✅ FIX: remember{} so the formatter is created once, not on every recomposition
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd yyyy 'at' hh:mm a", Locale.getDefault()) }

    // Get type options based on main category
    val typeOptions = when (mainCategory) {
        CategoryDefaults.WORK -> listOf("General", "Call", "Meeting", "Email", "Deadline", "Report", "Task", "Custom...")
        CategoryDefaults.PERSONAL -> listOf("General", "Call", "Errand", "Appointment", "Event", "Social", "Custom...")
        CategoryDefaults.HEALTH -> listOf("General", "Medication", "Exercise", "Doctor", "Checkup", "Therapy", "Custom...")
        CategoryDefaults.FINANCE -> listOf("General", "Bill", "Payment", "Tax", "Budget", "Investment", "Custom...")
        else -> listOf("General", "Custom...")
    }

    // Reset type to General when category changes
    LaunchedEffect(mainCategory) {
        if (reminderType != "General" && reminderType != "Custom..." && !typeOptions.contains(reminderType)) {
            reminderType = "General"
            customType = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder") },
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
            // Category Section
            Text(
                "Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Main Category Dropdown
            ExposedDropdownMenuBox(
                expanded = showMainCategoryMenu,
                onExpandedChange = { showMainCategoryMenu = !showMainCategoryMenu }
            ) {
                OutlinedTextField(
                    value = mainCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, "Dropdown")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                ExposedDropdownMenu(
                    expanded = showMainCategoryMenu,
                    onDismissRequest = { showMainCategoryMenu = false }
                ) {
                    // Show all categories (default + custom)
                    allCategories.forEach { (category, emoji) ->
                        DropdownMenuItem(
                            text = { Text("$emoji $category") },
                            onClick = {
                                mainCategory = category
                                showMainCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Type Dropdown
            ExposedDropdownMenuBox(
                expanded = showTypeMenu,
                onExpandedChange = { showTypeMenu = !showTypeMenu }
            ) {
                OutlinedTextField(
                    value = reminderType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type (optional)") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, "Dropdown")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                ExposedDropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false }
                ) {
                    typeOptions.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                reminderType = type
                                if (type != "Custom...") {
                                    customType = ""
                                }
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            // Custom Type Input (if Custom... selected)
            if (reminderType == "Custom...") {
                OutlinedTextField(
                    value = customType,
                    onValueChange = { customType = it },
                    label = { Text("Custom Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            HorizontalDivider()

            // Reminder Text (Main content)
            Text(
                "Reminder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = reminderText,
                onValueChange = { reminderText = it },
                label = { Text("What do you need to remember? *") },
                placeholder = { Text("e.g., Call Paul about the leaking tap") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // Notes field (Optional details)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Additional details...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            HorizontalDivider()

            // Date/Time picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedDateTime < System.currentTimeMillis() - (60 * 1000))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    val calendar = Calendar.getInstance()
                    val datePicker = DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(year, month, day, hour, minute, 0)
                                    val newTime = calendar.timeInMillis
                                    // ✅ Guard: reject if today was picked but time is already past
                                    if (newTime < System.currentTimeMillis() - (60 * 1000)) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "⚠️ Cannot select a time in the past",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        selectedDateTime = newTime
                                    }
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    datePicker.datePicker.minDate = System.currentTimeMillis()
                    datePicker.show()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Reminder Date & Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(selectedDateTime)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDateTime < System.currentTimeMillis() - (60 * 1000))
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (selectedDateTime < System.currentTimeMillis() - (60 * 1000)) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "⚠️ Date is in the past",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Recurrence Section
            Text(
                "Recurrence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ExposedDropdownMenuBox(
                expanded = showRecurrenceMenu,
                onExpandedChange = { showRecurrenceMenu = !showRecurrenceMenu }
            ) {
                OutlinedTextField(
                    value = getRecurrenceDisplayText(recurrenceType, recurrenceInterval),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeat") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, "Dropdown")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                ExposedDropdownMenu(
                    expanded = showRecurrenceMenu,
                    onDismissRequest = { showRecurrenceMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("One-time") },
                        onClick = {
                            recurrenceType = RecurrenceType.ONE_TIME
                            recurrenceInterval = 1
                            showRecurrenceMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hourly") },
                        onClick = {
                            recurrenceType = RecurrenceType.HOURLY
                            recurrenceInterval = 1
                            showRecurrenceMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Daily") },
                        onClick = {
                            recurrenceType = RecurrenceType.DAILY
                            recurrenceInterval = 1
                            showRecurrenceMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Weekly") },
                        onClick = {
                            recurrenceType = RecurrenceType.WEEKLY
                            recurrenceInterval = 1
                            showRecurrenceMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Monthly") },
                        onClick = {
                            recurrenceType = RecurrenceType.MONTHLY
                            recurrenceInterval = 1
                            showRecurrenceMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Annually") },
                        onClick = {
                            recurrenceType = RecurrenceType.ANNUAL
                            recurrenceInterval = 1
                            showRecurrenceMenu = false
                        }
                    )
                }
            }

            // Interval selector (if not one-time)
            if (recurrenceType != RecurrenceType.ONE_TIME) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (recurrenceInterval > 1) recurrenceInterval-- }
                        ) {
                            Text("-", style = MaterialTheme.typography.headlineMedium)
                        }

                        Text(
                            text = "Every $recurrenceInterval ${getIntervalUnit(recurrenceType)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        IconButton(
                            onClick = { if (recurrenceInterval < 99) recurrenceInterval++ }
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Voice toggle
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
                            "🔊 Voice Announcement",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Read aloud when alarm rings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = { isVoiceEnabled = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save as Template Button
            OutlinedButton(
                onClick = { showSaveAsTemplateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = reminderText.isNotBlank(),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (reminderText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "📋 Save as Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (reminderText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Reminder Button
            Button(
                onClick = {
                    if (reminderText.isNotBlank()) {
                        if (currentUser != null && connections.isNotEmpty()) {
                            // Show send sheet — let user choose who to send to
                            showSendSheet = true
                        } else {
                            // No circle members or not signed in — save immediately as before
                            val finalType = when (reminderType) {
                                "Custom..." -> TextFormatter.smartCapitalize(customType.ifBlank { "General" })
                                else -> reminderType
                            }
                            viewModel.addReminder(
                                title = TextFormatter.smartCapitalize(reminderText),
                                notes = TextFormatter.smartCapitalize(notes),
                                dateTime = selectedDateTime,
                                recurrenceType = recurrenceType,
                                recurrenceInterval = recurrenceInterval,
                                recurrenceDayOfWeek = null,
                                recurrenceDayOfMonth = null,
                                mainCategory = mainCategory,
                                subCategory = finalType,
                                isVoiceEnabled = isVoiceEnabled
                            )
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = reminderText.isNotBlank() && selectedDateTime > System.currentTimeMillis() - (60 * 1000), // Allow up to 1 minute in past
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Save Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // ── Send to Circle bottom sheet ───────────────────────────────────────────
    if (showSendSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSendSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Save Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Who should receive this reminder?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Just for me toggle
                Card(
                    onClick = { sendToSelf = !sendToSelf },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (sendToSelf)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (sendToSelf) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Save for myself too",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Keep a copy in my own reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sendToSelf) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Groups ───────────────────────────────────────────
                if (groups.isNotEmpty()) {
                    Text(
                        text = "Groups",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    groups.forEach { group ->
                        val groupMemberUids = group.memberUids.toSet()
                        val allSelected = groupMemberUids.isNotEmpty() &&
                                groupMemberUids.all { it in selectedConnections }
                        Card(
                            onClick = {
                                // Tapping a group selects all its members and unticks "just for me"
                                selectedConnections = if (allSelected)
                                    selectedConnections - groupMemberUids
                                else
                                    selectedConnections + groupMemberUids
                                if (!allSelected) sendToSelf = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (allSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.People, null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val memberNames = group.memberUids.mapNotNull { uid ->
                                        connections.find { it["uid"] == uid }?.get("displayName")
                                    }
                                    Text(
                                        if (memberNames.isEmpty()) "No members"
                                        else memberNames.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (allSelected) {
                                    Icon(Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ── Individuals ───────────────────────────────────────────────
                Text(
                    text = "Individuals",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                connections.forEach { connection ->
                    val uid = connection["uid"] ?: return@forEach
                    val name = connection["displayName"] ?: "Unknown"
                    val isSelected = uid in selectedConnections
                    val avatarColors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF1976D2),
                        androidx.compose.ui.graphics.Color(0xFF388E3C),
                        androidx.compose.ui.graphics.Color(0xFFF57C00),
                        androidx.compose.ui.graphics.Color(0xFF7B1FA2),
                        androidx.compose.ui.graphics.Color(0xFFC62828),
                        androidx.compose.ui.graphics.Color(0xFF00838F)
                    )
                    val avatarColor = avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]
                    Card(
                        onClick = {
                            selectedConnections = if (isSelected)
                                selectedConnections - uid
                            else {
                                sendToSelf = false
                                selectedConnections + uid
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name.firstOrNull()?.uppercase() ?: "?",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm save button
                Button(
                    onClick = {
                        showSendSheet = false
                        val finalType = when (reminderType) {
                            "Custom..." -> TextFormatter.smartCapitalize(customType.ifBlank { "General" })
                            else -> reminderType
                        }
                        scope.launch {
                            // Always save for self if sendToSelf is checked
                            if (sendToSelf) {
                                viewModel.addReminder(
                                    title = TextFormatter.smartCapitalize(reminderText),
                                    notes = TextFormatter.smartCapitalize(notes),
                                    dateTime = selectedDateTime,
                                    recurrenceType = recurrenceType,
                                    recurrenceInterval = recurrenceInterval,
                                    recurrenceDayOfWeek = null,
                                    recurrenceDayOfMonth = null,
                                    mainCategory = mainCategory,
                                    subCategory = finalType,
                                    isVoiceEnabled = isVoiceEnabled
                                )
                            }

                            // TODO: send to selected circle members via Firestore + FCM
                            // This will be implemented in the next session
                            // selectedConnections.forEach { uid -> CircleManager.sendReminder(...) }

                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = sendToSelf || selectedConnections.isNotEmpty()
                ) {
                    val label = when {
                        sendToSelf && selectedConnections.isNotEmpty() ->
                            "Save & Send to ${selectedConnections.size} person${if (selectedConnections.size > 1) "s" else ""}"
                        selectedConnections.isNotEmpty() ->
                            "Send to ${selectedConnections.size} person${if (selectedConnections.size > 1) "s" else ""}"
                        sendToSelf -> "Save for Myself"
                        else -> "Save Reminder"
                    }
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showSendSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // Save as Template Dialog
    if (showSaveAsTemplateDialog) {
        SaveAsTemplateDialog(
            reminderText = reminderText,
            onDismiss = { showSaveAsTemplateDialog = false },
            onSave = { templateName ->
                val finalType = when (reminderType) {
                    "Custom..." -> TextFormatter.smartCapitalize(customType.ifBlank { "General" })
                    else -> reminderType
                }

                viewModel.saveAsTemplate(
                    name = templateName,
                    title = TextFormatter.smartCapitalize(reminderText),
                    notes = TextFormatter.smartCapitalize(notes),
                    mainCategory = mainCategory,
                    subCategory = finalType,
                    recurrenceType = recurrenceType,
                    recurrenceInterval = recurrenceInterval,
                    isVoiceEnabled = isVoiceEnabled
                )

                showSaveAsTemplateDialog = false
            }
        )
    }
}

@Composable
private fun SaveAsTemplateDialog(
    reminderText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Give your template a name for easy reuse:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = templateName,
                    onValueChange = {
                        templateName = it
                        errorMessage = null
                    },
                    label = { Text("Template Name") },
                    placeholder = { Text("e.g., Weekly Team Meeting") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Template will save:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "• \"$reminderText\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Category, type, recurrence, and voice settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        templateName.isBlank() -> {
                            errorMessage = "Name cannot be empty"
                        }
                        templateName.length < 3 -> {
                            errorMessage = "Name must be at least 3 characters"
                        }
                        else -> {
                            onSave(templateName.trim())
                        }
                    }
                }
            ) {
                Text("Save Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getRecurrenceDisplayText(type: String, interval: Int): String {
    return when (type) {
        RecurrenceType.ONE_TIME -> "One-time"
        RecurrenceType.HOURLY -> if (interval == 1) "Hourly" else "Every $interval hours"
        RecurrenceType.DAILY -> if (interval == 1) "Daily" else "Every $interval days"
        RecurrenceType.WEEKLY -> if (interval == 1) "Weekly" else "Every $interval weeks"
        RecurrenceType.MONTHLY -> if (interval == 1) "Monthly" else "Every $interval months"
        RecurrenceType.ANNUAL -> if (interval == 1) "Annually" else "Every $interval years"
        else -> "One-time"
    }
}

private fun getIntervalUnit(type: String): String {
    return when (type) {
        RecurrenceType.HOURLY -> "hour(s)"
        RecurrenceType.DAILY -> "day(s)"
        RecurrenceType.WEEKLY -> "week(s)"
        RecurrenceType.MONTHLY -> "month(s)"
        RecurrenceType.ANNUAL -> "year(s)"
        else -> ""
    }
}