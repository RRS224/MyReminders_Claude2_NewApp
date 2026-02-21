package com.example.myreminders_claude2.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myreminders_claude2.data.CircleGroup
import com.example.myreminders_claude2.data.CircleGroupManager
import com.example.myreminders_claude2.data.CircleManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCircleScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Remote state
    var sharingCode by remember { mutableStateOf<String?>(null) }
    var connections by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoadingCode by remember { mutableStateOf(true) }
    var isLoadingConnections by remember { mutableStateOf(true) }

    // Local group state
    var groups by remember { mutableStateOf(CircleGroupManager.getGroups(context)) }

    // UI state
    var showAddByCode by remember { mutableStateOf(false) }
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<CircleGroup?>(null) }
    var enteredCode by remember { mutableStateOf("") }
    var addCodeError by remember { mutableStateOf<String?>(null) }
    var addCodeSuccess by remember { mutableStateOf<String?>(null) }
    var isAddingCode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            sharingCode = CircleManager.getOrCreateSharingCode()
            isLoadingCode = false
        }
        scope.launch {
            connections = CircleManager.getConnections()
            isLoadingConnections = false
        }
    }

    fun formatCode(code: String) =
        if (code.length == 6) "${code.take(3)}-${code.takeLast(3)}" else code

    fun refreshGroups() {
        groups = CircleGroupManager.getGroups(context)
    }

    // ── Regenerate confirm ────────────────────────────────────────────────────
    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = { Text("Generate New Code?") },
            text = {
                Text(
                    "Your current code will stop working. Anyone who has your code but hasn't connected yet will need your new code. Existing connections are not affected.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateConfirm = false
                        isLoadingCode = true
                        scope.launch {
                            sharingCode = CircleManager.regenerateSharingCode()
                            isLoadingCode = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Generate New Code") }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── Create / Edit group dialog ────────────────────────────────────────────
    if (showCreateGroup || editingGroup != null) {
        val isEditing = editingGroup != null
        var groupName by remember(editingGroup) {
            mutableStateOf(editingGroup?.name ?: "")
        }
        var selectedMembers by remember(editingGroup) {
            mutableStateOf(editingGroup?.memberUids?.toSet() ?: emptySet())
        }

        AlertDialog(
            onDismissRequest = {
                showCreateGroup = false
                editingGroup = null
            },
            title = { Text(if (isEditing) "Edit Group" else "Create Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group name") },
                        placeholder = { Text("e.g. Office, Book Club") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (connections.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Add people to your circle first before creating groups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Members",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        connections.forEach { connection ->
                            val uid = connection["uid"] ?: return@forEach
                            val name = connection["displayName"] ?: "Unknown"
                            val isSelected = uid in selectedMembers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedMembers = if (isSelected)
                                            selectedMembers - uid
                                        else
                                            selectedMembers + uid
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            val group = (editingGroup ?: CircleGroup()).copy(
                                name = groupName.trim(),
                                memberUids = selectedMembers.toList()
                            )
                            CircleGroupManager.saveGroup(context, group)
                            refreshGroups()
                            showCreateGroup = false
                            editingGroup = null
                        }
                    },
                    enabled = groupName.isNotBlank()
                ) { Text(if (isEditing) "Save Changes" else "Create Group") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateGroup = false
                    editingGroup = null
                }) { Text("Cancel") }
            }
        )
    }

    // ── Main screen ───────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Circle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Sharing code card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Circle Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Share this code with people you want in your circle",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoadingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 32.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = sharingCode?.let { formatCode(it) } ?: "------",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Share my code
                            Button(
                                onClick = {
                                    val code = sharingCode?.let { formatCode(it) } ?: return@Button
                                    val shareText = "Add me to your MyReminders circle!\nMy code is: $code"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share my Circle Code")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share My Code")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Invite someone new
                            OutlinedButton(
                                onClick = {
                                    val shareText = "I use MyReminders to send reminders to people in my circle.\n\n" +
                                            "Download it here:\n" +
                                            "https://play.google.com/store/apps/details?id=com.redboxstudios.myreminders\n\n" +
                                            "Once you're set up, send me your Circle Code and I'll add you."
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Invite someone to MyReminders")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Invite Someone")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            TextButton(
                                onClick = { showRegenerateConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Refresh, null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Generate new code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Add someone by code ───────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Add Someone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showAddByCode = !showAddByCode }) {
                                Icon(
                                    if (showAddByCode) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAddByCode) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Enter the Circle Code from the person you want to add",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = enteredCode,
                                    onValueChange = {
                                        enteredCode = it.uppercase().take(7)
                                        addCodeError = null
                                        addCodeSuccess = null
                                    },
                                    label = { Text("Circle Code (e.g. RAM-447)") },
                                    leadingIcon = { Icon(Icons.Default.Tag, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = addCodeError != null
                                )
                                if (addCodeError != null) {
                                    Text(
                                        addCodeError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (addCodeSuccess != null) {
                                    Text(
                                        addCodeSuccess ?: "",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val cleanCode = enteredCode.replace("-", "").uppercase()
                                        when {
                                            cleanCode.length != 6 ->
                                                addCodeError = "Please enter a valid 6-character code"
                                            cleanCode == sharingCode ->
                                                addCodeError = "That's your own code!"
                                            else -> {
                                                isAddingCode = true
                                                addCodeError = null
                                                scope.launch {
                                                    val user = CircleManager.findUserByCode(cleanCode)
                                                    if (user == null) {
                                                        addCodeError = "Code not found — check and try again"
                                                    } else {
                                                        val added = CircleManager.addConnection(
                                                            connectionUid = user["uid"] ?: "",
                                                            displayName = user["displayName"] ?: "Unknown",
                                                            sharingCode = cleanCode
                                                        )
                                                        if (added) {
                                                            addCodeSuccess = "✓ ${user["displayName"]} added to your circle!"
                                                            enteredCode = ""
                                                            connections = CircleManager.getConnections()
                                                        } else {
                                                            addCodeError = "Something went wrong — please try again"
                                                        }
                                                    }
                                                    isAddingCode = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAddingCode && enteredCode.isNotBlank()
                                ) {
                                    if (isAddingCode) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Add to My Circle")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Groups ────────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showCreateGroup = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Group")
                    }
                }
            }

            if (groups.isEmpty()) {
                item {
                    Text(
                        "No groups yet. Create one to send reminders to multiple people at once.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        connections = connections,
                        onEdit = { editingGroup = it },
                        onDelete = {
                            CircleGroupManager.deleteGroup(context, group.id)
                            refreshGroups()
                        }
                    )
                }
            }

            // ── Connections ───────────────────────────────────────────────────
            item {
                Text(
                    "My Circle ${if (!isLoadingConnections) "(${connections.size})" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoadingConnections) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (connections.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥", style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Your circle is empty",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Share your code or enter someone else's code to get started",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(connections) { connection ->
                    ConnectionCard(
                        displayName = connection["displayName"] ?: "Unknown",
                        sharingCode = connection["sharingCode"] ?: ""
                    )
                }
            }
        }
    }
}

// ── Group card ────────────────────────────────────────────────────────────────
@Composable
fun GroupCard(
    group: CircleGroup,
    connections: List<Map<String, String>>,
    onEdit: (CircleGroup) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val memberNames = group.memberUids.mapNotNull { uid ->
        connections.find { it["uid"] == uid }?.get("displayName")
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Group?") },
            text = { Text("\"${group.name}\" will be deleted. Members are not affected.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (memberNames.isEmpty()) "No members"
                    else memberNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onEdit(group) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete, contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Connection card ───────────────────────────────────────────────────────────
@Composable
fun ConnectionCard(displayName: String, sharingCode: String) {
    val avatarColors = listOf(
        Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00),
        Color(0xFF7B1FA2), Color(0xFFC62828), Color(0xFF00838F)
    )
    val avatarColor = avatarColors[displayName.hashCode().and(0x7FFFFFFF) % avatarColors.size]

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (sharingCode.length == 6) "${sharingCode.take(3)}-${sharingCode.takeLast(3)}"
                    else sharingCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.CheckCircle, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
