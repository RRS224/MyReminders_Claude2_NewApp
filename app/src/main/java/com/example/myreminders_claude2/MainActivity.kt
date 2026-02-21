package com.example.myreminders_claude2


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import com.example.myreminders_claude2.data.RecurrenceType
import androidx.compose.foundation.clickable
import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.myreminders_claude2.alarm.AlarmService
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myreminders_claude2.data.Reminder
import com.example.myreminders_claude2.screens.AddReminderScreen
import com.example.myreminders_claude2.screens.CreateReminderTab
import com.example.myreminders_claude2.screens.DeletedRemindersTab
import com.example.myreminders_claude2.screens.MyCircleScreen
import com.example.myreminders_claude2.data.CircleManager
import com.example.myreminders_claude2.screens.StatsScreen
import com.example.myreminders_claude2.screens.EditReminderScreen
import com.example.myreminders_claude2.screens.ManageCategoriesScreen
import com.example.myreminders_claude2.screens.PermissionOnboardingScreen
import com.example.myreminders_claude2.screens.ReminderAlarmScreen
import com.example.myreminders_claude2.screens.VoiceInputScreen
import com.example.myreminders_claude2.screens.SettingsScreen
import com.example.myreminders_claude2.ui.theme.MyRemindersTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import com.example.myreminders_claude2.utils.PermissionChecker
import com.example.myreminders_claude2.utils.PreferencesManager
import com.example.myreminders_claude2.utils.VoiceParser
import com.example.myreminders_claude2.utils.ThemePreferences
import com.example.myreminders_claude2.screens.calculateStreak
import com.example.myreminders_claude2.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.myreminders_claude2.screens.ManageTemplatesScreen
import com.example.myreminders_claude2.screens.SelectTemplateScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myreminders_claude2.screens.SignInScreen
import com.example.myreminders_claude2.viewmodel.AuthViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val viewModel: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Install splash screen BEFORE super.onCreate — required by the API
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // ✅ Enable Crashlytics crash reporting
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        enableEdgeToEdge()

        val reminderId = intent.getLongExtra("REMINDER_ID", -1)

        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            var themeVersion by remember { mutableStateOf(0) }
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when {
                themePrefs.autoDarkEnabled -> themePrefs.shouldUseDarkMode()
                themePrefs.themeMode == ThemePreferences.THEME_DARK -> true
                themePrefs.themeMode == ThemePreferences.THEME_LIGHT -> false
                else -> systemInDarkTheme
            }

            // Force recomposition when themeVersion changes
            LaunchedEffect(themeVersion) {
                // This block re-triggers when themeVersion changes
            }

            MyRemindersTheme(
                darkTheme = darkTheme,
                useAmoledBlack = themePrefs.useAmoledBlack,
                useDynamicColor = themePrefs.useDynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyRemindersApp(
                        viewModel = viewModel,
                        initialReminderId = if (reminderId != -1L) reminderId else null,
                        onThemeChanged = { themeVersion++ }
                    )
                }
            }
        }
    }
}

@Composable
fun MyRemindersApp(
    viewModel: ReminderViewModel,
    initialReminderId: Long? = null,
    onThemeChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    var showOnboarding by remember { mutableStateOf(!prefsManager.hasCompletedOnboarding) }
    var hasSkippedSignIn by remember { mutableStateOf(false) }

    // Connect auth callbacks to sync manager
    LaunchedEffect(Unit) {
        authViewModel.onSignInSuccess = {
            viewModel.startSync()
        }
        authViewModel.onSignOut = {
            viewModel.stopSync()
            hasSkippedSignIn = false  // ✅ Returns to SignIn screen after signing out
        }
    }

    when {
        showOnboarding -> {
            PermissionOnboardingScreen(
                onComplete = {
                    prefsManager.hasCompletedOnboarding = true
                    prefsManager.hasSkippedPermissions = false
                    showOnboarding = false
                },
                onSkip = {
                    prefsManager.hasCompletedOnboarding = true
                    prefsManager.hasSkippedPermissions = true
                    showOnboarding = false
                })

        }
        !authState.isSignedIn && !hasSkippedSignIn -> {
            SignInScreen(
                onSignInSuccess = {
                    hasSkippedSignIn = true  // ✅ Navigate away after successful sign in
                },
                onSkip = {
                    hasSkippedSignIn = true
                }
            )
        }
        else -> {
            MainNavigation(
                viewModel = viewModel,
                authViewModel = authViewModel,
                prefsManager = prefsManager,
                initialReminderId = initialReminderId,
                onNavigateToSignIn = { hasSkippedSignIn = false },
                onThemeChanged = onThemeChanged
            )
        }
    }
}
@Composable
fun MainNavigation(
    viewModel: ReminderViewModel,
    authViewModel: AuthViewModel,
    prefsManager: PreferencesManager,
    initialReminderId: Long?,
    onThemeChanged: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialReminderId) {
        initialReminderId?.let { reminderId ->
            navController.navigate("alarm/$reminderId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home",
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            HomeScreen(
                viewModel = viewModel,
                authViewModel = authViewModel,
                prefsManager = prefsManager,
                onNavigateToAdd = { navController.navigate("add") },
                onNavigateToVoiceInput = { navController.navigate("voiceInput") },
                onNavigateToPermissions = { navController.navigate("permissions") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToEdit = { id -> navController.navigate("edit/$id") },
                onNavigateToReuse = { id -> navController.navigate("reuse/$id") },
                onNavigateToTemplates = { navController.navigate("selectTemplate") },
                onNavigateToStats = { navController.navigate("stats") },
                onNavigateToMyCircle = { navController.navigate("myCircle") },
                onNavigateToSignIn = onNavigateToSignIn
            )
        }
        composable("add",
        enterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            AddReminderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("voiceInput",
        enterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            VoiceInputScreen(
                onNavigateBack = { navController.popBackStack() },
                onVoiceResult = { spokenText ->
                    val parsed = VoiceParser.parseVoiceInput(spokenText)
                    if (parsed != null) {
                        val encodedTitle = java.net.URLEncoder.encode(parsed.title, "UTF-8")
                        val encodedNotes = java.net.URLEncoder.encode(parsed.notes, "UTF-8")
                        val encodedCategory =
                            java.net.URLEncoder.encode(parsed.mainCategory, "UTF-8")
                        val encodedSubCategory =
                            java.net.URLEncoder.encode(parsed.subCategory ?: "", "UTF-8")
                        navController.navigate(
                            "add?title=$encodedTitle&notes=$encodedNotes&dateTime=${parsed.dateTime}" +
                                    "&category=$encodedCategory&subCategory=$encodedSubCategory" +
                                    "&recurrenceType=${parsed.recurrenceType}&recurrenceInterval=${parsed.recurrenceInterval}" +
                                    "&recurrenceDayOfWeek=${parsed.recurrenceDayOfWeek ?: -1}"
                        ) {
                            popUpTo("voiceInput") { inclusive = true }
                        }
                    } else {
                        val encodedText = java.net.URLEncoder.encode(spokenText, "UTF-8")
                        navController.navigate("add?title=$encodedText&notes=&dateTime=0&category=GENERAL&subCategory=&recurrenceType=ONE_TIME&recurrenceInterval=1&recurrenceDayOfWeek=-1") {
                            popUpTo("voiceInput") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "add?title={title}&notes={notes}&dateTime={dateTime}&category={category}&subCategory={subCategory}&recurrenceType={recurrenceType}&recurrenceInterval={recurrenceInterval}&recurrenceDayOfWeek={recurrenceDayOfWeek}",
            arguments = listOf(
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("notes") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("dateTime") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = "GENERAL"
                },
                navArgument("subCategory") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("recurrenceType") {
                    type = NavType.StringType
                    defaultValue = "ONE_TIME"
                },
                navArgument("recurrenceInterval") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("recurrenceDayOfWeek") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ,
        enterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val encodedNotes = backStackEntry.arguments?.getString("notes") ?: ""
            val dateTime = backStackEntry.arguments?.getLong("dateTime") ?: 0L
            val encodedCategory = backStackEntry.arguments?.getString("category") ?: "GENERAL"
            val encodedSubCategory = backStackEntry.arguments?.getString("subCategory") ?: ""
            val recurrenceType =
                backStackEntry.arguments?.getString("recurrenceType") ?: "ONE_TIME"
            val recurrenceInterval = backStackEntry.arguments?.getInt("recurrenceInterval") ?: 1
            val recurrenceDayOfWeek =
                backStackEntry.arguments?.getInt("recurrenceDayOfWeek") ?: -1

            val title = try {
                java.net.URLDecoder.decode(encodedTitle, "UTF-8")
            } catch (e: Exception) {
                encodedTitle
            }

            val notes = try {
                java.net.URLDecoder.decode(encodedNotes, "UTF-8")
            } catch (e: Exception) {
                encodedNotes
            }

            val category = try {
                java.net.URLDecoder.decode(encodedCategory, "UTF-8")
            } catch (e: Exception) {
                encodedCategory
            }

            val subCategory = try {
                java.net.URLDecoder.decode(encodedSubCategory, "UTF-8")
            } catch (e: Exception) {
                encodedSubCategory
            }

            AddReminderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                prefilledTitle = title,
                prefilledNotes = notes,
                prefilledDateTime = if (dateTime > 0) dateTime else System.currentTimeMillis() ,
                prefilledCategory = category,
                prefilledSubCategory = subCategory.ifBlank { null },
                prefilledRecurrenceType = recurrenceType,
                prefilledRecurrenceInterval = recurrenceInterval,
                prefilledRecurrenceDayOfWeek = if (recurrenceDayOfWeek != -1) recurrenceDayOfWeek else null
            )
        }
        composable(
            route = "edit/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
        ,
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
            var reminder by remember { mutableStateOf<Reminder?>(null) }

            LaunchedEffect(reminderId) {
                reminder = viewModel.getReminderById(reminderId)
            }

            reminder?.let { rem ->
                EditReminderScreen(
                    viewModel = viewModel,
                    reminderId = rem.id,
                    initialTitle = rem.title,
                    initialNotes = rem.notes,
                    initialDateTime = rem.dateTime,
                    initialRecurrenceType = rem.recurrenceType,
                    initialRecurrenceInterval = rem.recurrenceInterval,
                    initialMainCategory = rem.mainCategory,
                    initialSubCategory = rem.subCategory,
                    initialIsVoiceEnabled = rem.isVoiceEnabled,
                    onNavigateBack = { navController.popBackStack() },
                    isReuse = false,
                    isRestore = rem.isDeleted  // Auto-open date picker if restoring from Done
                )
            }
        }
        composable(
            route = "reuse/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
        ,
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
            var reminder by remember { mutableStateOf<Reminder?>(null) }

            LaunchedEffect(reminderId) {
                reminder = viewModel.getReminderById(reminderId)
            }

            reminder?.let { rem ->
                EditReminderScreen(
                    viewModel = viewModel,
                    reminderId = 0L, // New reminder (reuse)
                    initialTitle = rem.title,
                    initialNotes = rem.notes,
                    initialDateTime = System.currentTimeMillis(),
                    initialRecurrenceType = rem.recurrenceType,
                    initialRecurrenceInterval = rem.recurrenceInterval,
                    initialMainCategory = rem.mainCategory,
                    initialSubCategory = rem.subCategory,
                    initialIsVoiceEnabled = rem.isVoiceEnabled,
                    onNavigateBack = { navController.popBackStack() },
                    isReuse = true
                )
            }
        }
        composable("stats",
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            val activeReminders by viewModel.allActiveReminders.collectAsState(initial = emptyList())
            val missedReminders by viewModel.completedReminders.collectAsState(initial = emptyList())
            val doneReminders by viewModel.deletedReminders.collectAsState(initial = emptyList())
            StatsScreen(
                activeCount = activeReminders.size,
                missedCount = missedReminders.size,
                doneCount = doneReminders.size,
                allReminders = activeReminders + missedReminders + doneReminders,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("permissions",
        enterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            PermissionOnboardingScreen(
                onComplete = {
                    prefsManager.hasSkippedPermissions = false
                    navController.popBackStack()
                },
                onSkip = { navController.popBackStack() }
            )
        }
        composable("settings",
        enterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onThemeChanged = onThemeChanged,
                onNavigateToManageCategories = { navController.navigate("manageCategories") },
                onNavigateToManageTemplates = { navController.navigate("manageTemplates") },
                onNavigateToPermissions = { navController.navigate("permissions") }
            )
        }
        composable("manageCategories",
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            ManageCategoriesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
// Manage Templates
        composable("manageTemplates",
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            ManageTemplatesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
// Select Template (Use Template)
        composable("selectTemplate",
        enterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
    ) {
            SelectTemplateScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onReminderCreated = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        composable("myCircle",
        enterTransition = { slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(280)) }
        ) {
            MyCircleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "alarm/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
        ,
        enterTransition = { fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(280)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = { fadeOut(tween(280)) }
    ) { backStackEntry ->
            val context = LocalContext.current
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L
            var reminder by remember { mutableStateOf<Reminder?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                val stopIntent = Intent(
                    context,
                    com.example.myreminders_claude2.alarm.AlarmService::class.java
                ).apply {
                    action =
                        com.example.myreminders_claude2.alarm.AlarmService.ACTION_STOP_ALARM
                }
                context.startService(stopIntent)
            }

            LaunchedEffect(reminderId) {
                reminder = viewModel.getReminderById(reminderId)
                isLoading = false
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                reminder != null -> {
                    ReminderAlarmScreen(
                        reminderId = reminder!!.id,
                        title = reminder!!.title,
                        notes = reminder!!.notes,
                        scheduledTime = reminder!!.dateTime,
                        snoozeCount = reminder!!.snoozeCount,
                        onDismiss = {
                            scope.launch {
                                viewModel.softDeleteReminder(reminder!!.id)
                                navController.popBackStack()
                            }
                        },
                        onSnooze = {
                            scope.launch {
                                viewModel.snoozeReminder(reminder!!.id)
                                navController.popBackStack()
                            }
                        }
                    )
                }

                else -> {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
@Composable
fun MyCircleRow(
    onNavigateToMyCircle: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    var sharingCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            scope.launch {
                sharingCode = CircleManager.getOrCreateSharingCode()
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentUser != null) {
                // Signed in — show My Circle button
                TextButton(
                    onClick = onNavigateToMyCircle,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(18.dp)
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))
                    Text(
                        "My Circle",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(14.dp)
                    )
                }

                // Sharing code — tap to navigate to My Circle
                if (sharingCode != null) {
                    val formatted = sharingCode!!.let {
                        if (it.length == 6) "${it.take(3)}-${it.takeLast(3)}" else it
                    }
                    TextButton(
                        onClick = onNavigateToMyCircle,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Not signed in — more prominent prompt
                TextButton(
                    onClick = onNavigateToSignIn,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))
                    Text(
                        "👥 My Circle — Sign in to get started",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionWarningBanner(
    onFixClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permissions Missing",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Reminders may not work reliably",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onFixClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Fix", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyState(isCompleted: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isCompleted) "All caught up!" else "No active reminders",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isCompleted)
                "Reminders you miss will appear here.\nYou're doing great!"
            else
                "Tap Create to set your first reminder.\nWe'll make sure you don't forget.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
fun performHapticFeedback(context: android.content.Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            android.os.VibrationEffect.createOneShot(
                20,
                android.os.VibrationEffect.EFFECT_HEAVY_CLICK
            )
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(20)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveReminderCard(
    reminder: Reminder,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = if (reminder.recurrenceType == RecurrenceType.ANNUAL) {
        SimpleDateFormat("EEE, MMM dd ''yy 'at' hh:mm a", Locale.getDefault())
    } else {
        SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault())
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }

    LaunchedEffect(dismissState.progress) {
        if (dismissState.progress > 0.3f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        15,
                        android.os.VibrationEffect.EFFECT_HEAVY_CLICK
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        }
    }

    // Get category emoji
    val categoryEmoji = when (reminder.mainCategory) {
        "WORK" -> "💼"
        "PERSONAL" -> "🏠"
        "HEALTH" -> "❤️"
        "FINANCE" -> "💰"
        else -> "📌"
    }

    // Get recurrence badge text
    val recurrenceBadge = when (reminder.recurrenceType) {
        "HOURLY" -> if (reminder.recurrenceInterval == 1) "🔁 Hourly" else "🔁 Every ${reminder.recurrenceInterval}h"
        "DAILY" -> if (reminder.recurrenceInterval == 1) "🔁 Daily" else "🔁 Every ${reminder.recurrenceInterval}d"
        "WEEKLY" -> if (reminder.recurrenceInterval == 1) "🔁 Weekly" else "🔁 Every ${reminder.recurrenceInterval}w"
        "MONTHLY" -> if (reminder.recurrenceInterval == 1) "🔁 Monthly" else "🔁 Every ${reminder.recurrenceInterval}m"
        "ANNUAL" -> if (reminder.recurrenceInterval == 1) "🔁 Yearly" else "🔁 Every ${reminder.recurrenceInterval}y"
        else -> null
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Category Emoji + Type + Recurrence Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = categoryEmoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = reminder.subCategory ?: "General",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Recurrence badge (if recurring)
                    recurrenceBadge?.let { badge ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reminder text (main content - BIG)
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Notes (if present - smaller)
                if (reminder.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date/Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(reminder.dateTime)),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(onClick = {
                        performHapticFeedback(context)
                        onDelete()
                    }) {
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
}
@Composable
fun ActiveReminderList(
    reminders: List<Reminder>,
    onDeleteReminder: (Reminder) -> Unit,
    onEditReminder: (Reminder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reminders, key = { it.id }) { reminder ->
            ActiveReminderCard(
                reminder = reminder,
                onDelete = { onDeleteReminder(reminder) },
                onEdit = { onEditReminder(reminder) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedReminderCard(
    reminder: Reminder,
    onDelete: () -> Unit,
    onReuse: () -> Unit,
    onReschedule: () -> Unit
) {
    val scheduledFormat = if (reminder.recurrenceType == RecurrenceType.ANNUAL) {
        SimpleDateFormat("EEE, MMM dd ''yy 'at' hh:mm a", Locale.getDefault())
    } else {
        SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault())
    }
    val completedFormat = if (reminder.recurrenceType == RecurrenceType.ANNUAL) {
        SimpleDateFormat("EEE, MMM dd ''yy 'at' hh:mm a", Locale.getDefault())
    } else {
        SimpleDateFormat("EEE, MMM dd 'at' hh:mm a", Locale.getDefault())
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }

    LaunchedEffect(dismissState.progress) {
        if (dismissState.progress > 0.3f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        20,
                        android.os.VibrationEffect.EFFECT_HEAVY_CLICK
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        }
    }

    // Get category emoji
    val categoryEmoji = when (reminder.mainCategory) {
        "WORK" -> "💼"
        "PERSONAL" -> "🏠"
        "HEALTH" -> "❤️"
        "FINANCE" -> "💰"
        else -> "📌"
    }

    // Get recurrence badge text
    val recurrenceBadge = when (reminder.recurrenceType) {
        "HOURLY" -> if (reminder.recurrenceInterval == 1) "🔁 Hourly" else "🔁 Every ${reminder.recurrenceInterval}h"
        "DAILY" -> if (reminder.recurrenceInterval == 1) "🔁 Daily" else "🔁 Every ${reminder.recurrenceInterval}d"
        "WEEKLY" -> if (reminder.recurrenceInterval == 1) "🔁 Weekly" else "🔁 Every ${reminder.recurrenceInterval}w"
        "MONTHLY" -> if (reminder.recurrenceInterval == 1) "🔁 Monthly" else "🔁 Every ${reminder.recurrenceInterval}m"
        "ANNUAL" -> if (reminder.recurrenceInterval == 1) "🔁 Yearly" else "🔁 Every ${reminder.recurrenceInterval}y"
        else -> null
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReuse() }
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                      .padding(16.dp)
            ) {
                // Header: Checkmark + Category Emoji + Type + Recurrence Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = categoryEmoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = reminder.subCategory ?: "General",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Recurrence badge (if recurring)
                    recurrenceBadge?.let { badge ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.6f
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reminder text (main content)
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Notes (if present)
                if (reminder.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scheduled time
                Text(
                    text = "Scheduled: ${scheduledFormat.format(Date(reminder.dateTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Completed time
                reminder.completedAt?.let { completedTime ->
                    val dismissalText = when (reminder.dismissalReason) {
                        "MANUAL" -> "Dismissed"
                        "AUTO_SNOOZED" -> "Auto-dismissed (3 snoozes)"
                        else -> "Completed"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dismissalText: ${completedFormat.format(Date(completedTime))}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        performHapticFeedback(context)
                        onReschedule()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reschedule")
                }
            }
        }
    }
}

@Composable
fun CompletedReminderList(
    reminders: List<Reminder>,
    onDeleteReminder: (Reminder) -> Unit,
    onReuseReminder: (Reminder) -> Unit,
    onRescheduleReminder: (Reminder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reminders, key = { it.id }) { reminder ->
            CompletedReminderCard(
                reminder = reminder,
                onDelete = { onDeleteReminder(reminder) },
                onReuse = { onReuseReminder(reminder) },
                onReschedule = { onRescheduleReminder(reminder) }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReminderViewModel,
    authViewModel: AuthViewModel,
    prefsManager: PreferencesManager,
    onNavigateToAdd: () -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToReuse: (Long) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToMyCircle: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val activeReminders by viewModel.allActiveReminders.collectAsState(initial = emptyList())
    val completedReminders by viewModel.completedReminders.collectAsState(initial = emptyList())
    val deletedReminders by viewModel.deletedReminders.collectAsState(initial = emptyList())
    val allReminders = activeReminders + completedReminders + deletedReminders
    val permissionStatus =
        remember { mutableStateOf(PermissionChecker.checkAllPermissions(context)) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Search + category filter state (Active tab)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Celebration bubble state
    var celebrationMessage by remember { mutableStateOf<String?>(null) }
    var showCelebration by remember { mutableStateOf(false) }

    val celebrationPrefs = remember {
        context.getSharedPreferences("celebration_prefs", Context.MODE_PRIVATE)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authViewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Log.e("SignIn", "Sign in failed", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionStatus.value = PermissionChecker.checkAllPermissions(context)
    }

    // Celebration bubble logic
    LaunchedEffect(allReminders) {
        if (allReminders.isEmpty()) return@LaunchedEffect

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastShown = celebrationPrefs.getString("last_shown_date", "")
        if (lastShown == today) return@LaunchedEffect

        val totalManuallyDone = allReminders.count {
            it.isDeleted && it.dismissalReason == "MANUAL"
        }
        val totalCompleted = allReminders.count { it.isCompleted && !it.isDeleted }
        val totalDone = totalCompleted + totalManuallyDone
        val streak = calculateStreak(allReminders)
        val totalEverCreated = allReminders.size
        val completionRate = if (totalEverCreated > 0)
            ((totalDone).toFloat() / totalEverCreated * 100).toInt()
        else 0

        val message = when {
            streak == 7 -> "🔥 7-day streak! You're on fire!"
            streak == 14 -> "⚡ 14-day streak! Unstoppable!"
            streak == 30 -> "💎 30-day streak! Legendary!"
            streak == 3 -> "🔥 3-day streak! Keep it up!"
            totalDone == 10 -> "✅ 10 reminders completed! Great start!"
            totalDone == 50 -> "🏆 50 reminders completed! Amazing!"
            totalDone == 100 -> "💯 100 reminders completed! You're a pro!"
            completionRate == 100 && totalEverCreated >= 5 -> "💯 Perfect completion rate!"
            totalEverCreated == 1 -> "🌱 First reminder created! Welcome!"
            else -> null
        }

        if (message != null) {
            celebrationPrefs.edit().putString("last_shown_date", today).apply()
            celebrationMessage = message
            showCelebration = true
            delay(4000)
            showCelebration = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "My Reminders",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        },
                        actions = {
                            // User Profile Menu
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            var showUserMenu by remember { mutableStateOf(false) }

                            Box {
                                if (currentUser != null) {
                                    // Signed In - Show Avatar
                                    IconButton(onClick = { showUserMenu = true }) {
                                        AsyncImage(
                                            model = currentUser.photoUrl,
                                            contentDescription = "Profile",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                ),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(android.R.drawable.ic_menu_myplaces)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showUserMenu,
                                        onDismissRequest = { showUserMenu = false }
                                    ) {
                                        // User Info
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                currentUser.displayName ?: "User",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                currentUser.email ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "✓ Signed in with Google",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        HorizontalDivider()

                                        // Sign Out
                                        DropdownMenuItem(
                                            text = { Text("Sign out") },
                                            onClick = {
                                                showUserMenu = false
                                                authViewModel.signOut()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ExitToApp,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    // Not Signed In - Show Sign In Button
                                    TextButton(onClick = {
                                        authViewModel.signOut()
                                    }) {
                                        Text("Sign In", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // Settings Icon (direct access)
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }

                            // Stats Trophy Icon
                            IconButton(onClick = onNavigateToStats) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Stats",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Clear All button (only on Completed tab)
                            if (selectedTab == 2 && completedReminders.isNotEmpty()) {
                                IconButton(onClick = {
                                    scope.launch {
                                        viewModel.clearAllCompleted()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear All Completed",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.shadow(
                            elevation = 2.dp,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )

                    // ── My Circle row ─────────────────────────────────────
                    MyCircleRow(
                        onNavigateToMyCircle = onNavigateToMyCircle,
                        onNavigateToSignIn = onNavigateToSignIn
                    )

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Create",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Active",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "(${activeReminders.size})",
                                        fontSize = 10.sp,
                                        color = if (selectedTab == 1)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Missed",
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "(${completedReminders.size})",
                                        fontSize = 10.sp,
                                        color = if (selectedTab == 2)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Done",
                                        fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "(${deletedReminders.size})",
                                        fontSize = 10.sp,
                                        color = if (selectedTab == 3)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (permissionStatus.value.hasAnyMissing && selectedTab == 1) {
                    PermissionWarningBanner(
                        onFixClick = onNavigateToPermissions
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                                )
                            )
                        )
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Create Tab
                            CreateReminderTab(
                                onNavigateToManual = onNavigateToAdd,
                                onNavigateToVoice = onNavigateToVoiceInput,
                                onNavigateToTemplates = onNavigateToTemplates
                            )
                        }

                        1 -> {
                            // Active Tab — with search + category filter
                            val filteredReminders = remember(activeReminders, searchQuery, selectedCategory) {
                                activeReminders
                                    .let { list ->
                                        if (selectedCategory == null) list
                                        else list.filter { it.mainCategory == selectedCategory }
                                    }
                                    .let { list ->
                                        if (searchQuery.isBlank()) list
                                        else list.filter {
                                            it.title.contains(searchQuery, ignoreCase = true) ||
                                            (it.notes?.contains(searchQuery, ignoreCase = true) == true)
                                        }
                                    }
                            }
                            val availableCategories = remember(activeReminders) {
                                activeReminders.map { it.mainCategory }.distinct().sorted()
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                // Search bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    placeholder = { Text("Search reminders...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search"
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear search"
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Category filter chips — only shown when >1 category exists
                                if (availableCategories.size > 1) {
                                    val categoryEmojis = mapOf(
                                        "WORK" to "💼",
                                        "PERSONAL" to "🏠",
                                        "HEALTH" to "❤️",
                                        "FINANCE" to "💰",
                                        "GENERAL" to "📋"
                                    )
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        // "All" chip
                                        item {
                                            FilterChip(
                                                selected = selectedCategory == null,
                                                onClick = { selectedCategory = null },
                                                label = { Text("All") }
                                            )
                                        }
                                        items(availableCategories) { category ->
                                            val emoji = categoryEmojis[category] ?: "📌"
                                            val label = category.lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                            FilterChip(
                                                selected = selectedCategory == category,
                                                onClick = {
                                                    selectedCategory =
                                                        if (selectedCategory == category) null
                                                        else category
                                                },
                                                label = { Text("$emoji $label") }
                                            )
                                        }
                                    }
                                }

                                // Results
                                when {
                                    activeReminders.isEmpty() -> EmptyState(isCompleted = false)
                                    filteredReminders.isEmpty() -> {
                                        // No matches — show friendly message
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "🔍 No matches found",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Try a different search or category",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                            if (searchQuery.isNotEmpty() || selectedCategory != null) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                TextButton(onClick = {
                                                    searchQuery = ""
                                                    selectedCategory = null
                                                }) {
                                                    Text("Clear filters")
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        ActiveReminderList(
                                            reminders = filteredReminders,
                                            onDeleteReminder = { viewModel.deleteReminder(it) },
                                            onEditReminder = { onNavigateToEdit(it.id) }
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Missed Tab
                            if (completedReminders.isEmpty()) {
                                EmptyState(isCompleted = true)
                            } else {
                                CompletedReminderList(
                                    reminders = completedReminders,
                                    onDeleteReminder = { viewModel.deleteReminder(it) },
                                    onReuseReminder = { onNavigateToReuse(it.id) },
                                    onRescheduleReminder = { onNavigateToEdit(it.id) }
                                )
                            }
                        }

                        3 -> {
                            // Deleted Tab
                            DeletedRemindersTab(
                                viewModel = viewModel,
                                onNavigateToEdit = onNavigateToEdit
                            )
                        }

                    }
                }
            }

            // Celebration bubble overlay
            AnimatedVisibility(
                visible = showCelebration,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCelebration = false },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = celebrationMessage ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    }












