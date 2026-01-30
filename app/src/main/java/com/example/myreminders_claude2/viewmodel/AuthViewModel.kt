package com.example.myreminders_claude2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isSignedIn: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val oneTapClient: SignInClient = Identity.getSignInClient(application)

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    // Callback for when user signs in successfully (to start sync)
    var onSignInSuccess: (() -> Unit)? = null

    // Callback for when user signs out (to stop sync)
    var onSignOut: (() -> Unit)? = null

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        _authState.value = if (currentUser != null) {
            AuthState(
                isSignedIn = true,
                userId = currentUser.uid,
                email = currentUser.email,
                displayName = currentUser.displayName
            )
        } else {
            AuthState(isSignedIn = false)
        }
    }

    fun getSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("661646502531-ebd116h0q4tccehgpive5n191db9f1e4.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(isLoading = true, error = null)

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()

                val user = result.user
                _authState.value = AuthState(
                    isSignedIn = true,
                    userId = user?.uid,
                    email = user?.email,
                    displayName = user?.displayName,
                    isLoading = false
                )

                // Trigger sync start
                onSignInSuccess?.invoke()
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign in failed"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                oneTapClient.signOut().await()
                _authState.value = AuthState(isSignedIn = false)

                // Trigger sync stop
                onSignOut?.invoke()
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    error = e.message ?: "Sign out failed"
                )
            }
        }
    }

    fun getOneTapClient(): SignInClient = oneTapClient
}