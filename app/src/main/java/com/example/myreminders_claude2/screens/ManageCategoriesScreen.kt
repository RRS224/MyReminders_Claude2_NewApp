package com.example.myreminders_claude2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myreminders_claude2.utils.CategoryManager
import com.example.myreminders_claude2.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager(context) }
    val customCategories by categoryManager.customCategories.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var remindersUsingCategory by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
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
        },
        floatingActionButton = {
            if (customCategories.size < CategoryManager.MAX_CUSTOM_CATEGORIES) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category"
                    )
                }
            }
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
                "Default Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Default categories (read-only)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DefaultCategoryItem("💼", "WORK")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DefaultCategoryItem("🏠", "PERSONAL")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DefaultCategoryItem("❤️", "HEALTH")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DefaultCategoryItem("💰", "FINANCE")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom categories header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Custom Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${customCategories.size}/${CategoryManager.MAX_CUSTOM_CATEGORIES}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Custom categories list
            if (customCategories.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No custom categories yet.\nTap + to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customCategories, key = { it.id }) { category ->
                        CustomCategoryItem(
                            emoji = category.emoji,
                            name = category.name,
                            onEdit = {
                                selectedCategoryId = category.id
                                selectedCategoryName = category.name
                                showEditDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    val count = viewModel.getRemindersCountByCategory(category.name)
                                    remindersUsingCategory = count
                                    categoryToDelete = category.id to category.name
                                    showDeleteDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        AddCategoryDialog(
            categoryManager = categoryManager,
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Category Dialog
    if (showEditDialog) {
        EditCategoryDialog(
            categoryManager = categoryManager,
            viewModel = viewModel,
            categoryId = selectedCategoryId,
            currentName = selectedCategoryName,
            onDismiss = { showEditDialog = false }
        )
    }

    // Delete Category Dialog
    // ✅ FIX: Use let to safely unpack categoryToDelete — eliminates !! crash risk.
    // If state changes between recompositions, the dialog simply won't show
    // rather than crashing with a NullPointerException.
    if (showDeleteDialog) {
        categoryToDelete?.let { (catId, catName) ->
            DeleteCategoryDialog(
                viewModel = viewModel,
                categoryManager = categoryManager,
                categoryId = catId,
                categoryName = catName,
                remindersCount = remindersUsingCategory,
                onDismiss = {
                    showDeleteDialog = false
                    categoryToDelete = null
                }
            )
        }
    }
}

@Composable
private fun DefaultCategoryItem(emoji: String, name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "Default",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomCategoryItem(
    emoji: String,
    name: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    categoryManager: CategoryManager,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📌") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableEmojis = listOf(
        "📚", "🎓", "✏️", "📝", "🏫", "📖", // School/Education
        "🎨", "🎭", "🎮", "🎵", "🎬", "📷", // Hobbies/Arts
        "👨‍👩‍👧‍👦", "👶", "🏡", "🌳", "🌸", "🐕", // Family/Home
        "🔧", "🛠️", "⚙️", "🔨", "🏗️", "🚗", // DIY/Projects
        "🍳", "🥗", "🛒", "🧺", "🧹", "🧼", // Household
        "✈️", "🏖️", "🗺️", "📍", "🎒", "🏔️"  // Travel
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("Category Name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } }
                )

                Text(
                    "Choose an emoji:",
                    style = MaterialTheme.typography.titleSmall
                )

                // Emoji grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableEmojis.chunked(6).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { emoji ->
                                FilterChip(
                                    selected = selectedEmoji == emoji,
                                    onClick = { selectedEmoji = emoji },
                                    label = {
                                        Text(
                                            emoji,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        categoryName.isBlank() -> {
                            errorMessage = "Name cannot be empty"
                        }
                        categoryName.length < 3 -> {
                            errorMessage = "Name must be at least 3 characters"
                        }
                        categoryManager.categoryExists(categoryName) -> {
                            errorMessage = "Category already exists"
                        }
                        else -> {
                            val success = categoryManager.addCategory(categoryName, selectedEmoji)
                            if (success) {
                                onDismiss()
                            } else {
                                errorMessage = "Maximum categories reached"
                            }
                        }
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryDialog(
    categoryManager: CategoryManager,
    viewModel: ReminderViewModel,
    categoryId: String,
    currentName: String,
    onDismiss: () -> Unit
) {
    val category = categoryManager.customCategories.value.find { it.id == categoryId }
    var categoryName by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(category?.emoji ?: "📌") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remindersCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(categoryId) {
        remindersCount = viewModel.getRemindersCountByCategory(currentName)
    }

    val availableEmojis = listOf(
        "📚", "🎓", "✏️", "📝", "🏫", "📖",
        "🎨", "🎭", "🎮", "🎵", "🎬", "📷",
        "👨‍👩‍👧‍👦", "👶", "🏡", "🌳", "🌸", "🐕",
        "🔧", "🛠️", "⚙️", "🔨", "🏗️", "🚗",
        "🍳", "🥗", "🛒", "🧺", "🧹", "🧼",
        "✈️", "🏖️", "🗺️", "📍", "🎒", "🏔️"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (remindersCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "⚠️ This will affect $remindersCount reminder${if (remindersCount > 1) "s" else ""}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("Category Name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } }
                )

                Text(
                    "Choose an emoji:",
                    style = MaterialTheme.typography.titleSmall
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableEmojis.chunked(6).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { emoji ->
                                FilterChip(
                                    selected = selectedEmoji == emoji,
                                    onClick = { selectedEmoji = emoji },
                                    label = {
                                        Text(
                                            emoji,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        categoryName.isBlank() -> {
                            errorMessage = "Name cannot be empty"
                        }
                        categoryName.length < 3 -> {
                            errorMessage = "Name must be at least 3 characters"
                        }
                        categoryName != currentName && categoryManager.categoryExists(categoryName) -> {
                            errorMessage = "Category already exists"
                        }
                        else -> {
                            scope.launch {
                                // Update category
                                categoryManager.editCategory(categoryId, categoryName, selectedEmoji)

                                // Update all reminders using old category name
                                if (categoryName != currentName) {
                                    viewModel.updateCategoryForAllReminders(currentName, categoryName)
                                }

                                onDismiss()
                            }
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteCategoryDialog(
    viewModel: ReminderViewModel,
    categoryManager: CategoryManager,
    categoryId: String,
    categoryName: String,
    remindersCount: Int,
    onDismiss: () -> Unit
) {
    var selectedTargetCategory by remember { mutableStateOf("GENERAL") }
    val scope = rememberCoroutineScope()

    val availableCategories = categoryManager.getAllCategories()
        .filter { it.first != categoryName } // Exclude the category being deleted

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (remindersCount > 0) {
                    Text(
                        "$remindersCount reminder${if (remindersCount > 1) "s" else ""} use${if (remindersCount == 1) "s" else ""} this category.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "Move them to:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableCategories.forEach { (catName, emoji) ->
                            FilterChip(
                                selected = selectedTargetCategory == catName,
                                onClick = { selectedTargetCategory = catName },
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(emoji)
                                        Text(catName)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        "This category is not being used by any reminders.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        // Move reminders to target category
                        if (remindersCount > 0) {
                            viewModel.updateCategoryForAllReminders(categoryName, selectedTargetCategory)
                        }

                        // Delete the category
                        categoryManager.deleteCategory(categoryId)

                        onDismiss()
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}