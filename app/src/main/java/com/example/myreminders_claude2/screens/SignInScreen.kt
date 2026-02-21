package com.example.myreminders_claude2.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myreminders_claude2.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    // UI state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordResult by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    // Google Sign-In launcher
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
                } else {
                    localError = "Sign in failed — no token received"
                }
            } catch (e: ApiException) {
                Log.e("SignIn", "Google sign in failed", e)
                localError = "Google sign in failed — please try again"
            }
        }
    }

    // Navigate on success
    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn) onSignInSuccess()
    }

    // Forgot password dialog
    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = {
                showForgotPassword = false
                forgotPasswordResult = null
                forgotPasswordEmail = ""
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = {
                            forgotPasswordEmail = it
                            forgotPasswordResult = null
                        },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (forgotPasswordResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = forgotPasswordResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (forgotPasswordResult?.startsWith("Reset") == true)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (forgotPasswordEmail.isBlank()) {
                        forgotPasswordResult = "Please enter your email address"
                        return@Button
                    }
                    authViewModel.sendPasswordResetEmail(forgotPasswordEmail) { success, message ->
                        forgotPasswordResult = message
                        if (success) forgotPasswordEmail = ""
                    }
                }) {
                    Text("Send Reset Email")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPassword = false
                    forgotPasswordResult = null
                    forgotPasswordEmail = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Text(text = "🔔", style = MaterialTheme.typography.displayMedium)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MyReminders",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to use Family Sharing and sync reminders across devices",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Google Sign-In ─────────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    localError = null
                    try {
                        val gso = GoogleSignInOptions
                            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("661646502531-ebd116h0q4tccehgpjve5n191db9f1e4.apps.googleusercontent.com")
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        launcher.launch(client.signInIntent)
                    } catch (e: Exception) {
                        localError = "Error: ${e.message}"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !authState.isLoading
            ) {
                Text("🇬  Continue with Google", style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Divider ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "  or  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Email field ───────────────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Password field ─────────────────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (email.isNotBlank() && password.isNotBlank()) {
                            if (isCreatingAccount)
                                authViewModel.createAccountWithEmail(email, password)
                            else
                                authViewModel.signInWithEmail(email, password)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Forgot password ────────────────────────────────────────────────
            AnimatedVisibility(visible = !isCreatingAccount) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            forgotPasswordEmail = email
                            showForgotPassword = true
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            "Forgot password?",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Primary action button ──────────────────────────────────────────
            Button(
                onClick = {
                    localError = null
                    when {
                        email.isBlank() -> localError = "Please enter your email address"
                        password.isBlank() -> localError = "Please enter your password"
                        password.length < 6 && isCreatingAccount ->
                            localError = "Password must be at least 6 characters"
                        isCreatingAccount ->
                            authViewModel.createAccountWithEmail(email, password)
                        else ->
                            authViewModel.signInWithEmail(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !authState.isLoading
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (isCreatingAccount) "Create Account" else "Sign In",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Toggle create / sign in ────────────────────────────────────────
            TextButton(onClick = {
                isCreatingAccount = !isCreatingAccount
                localError = null
            }) {
                Text(
                    if (isCreatingAccount)
                        "Already have an account? Sign in"
                    else
                        "No account yet? Create one"
                )
            }

            // ── Error messages ─────────────────────────────────────────────────
            val errorToShow = localError ?: authState.error
            AnimatedVisibility(visible = errorToShow != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorToShow ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Divider ───────────────────────────────────────────────────────
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // ── Skip / guest ───────────────────────────────────────────────────
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Continue without signing in",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Family Sharing won't be available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
