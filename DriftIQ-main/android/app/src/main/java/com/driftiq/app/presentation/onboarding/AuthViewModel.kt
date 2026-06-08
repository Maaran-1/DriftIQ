package com.driftiq.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun register(email: String, password: String) {
        if (email.isBlank() || password.length < 8) {
            _authState.value = AuthState.Error("Invalid email or password too short")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(email, password)
                .onSuccess {
                    dataStore.setOnboardingComplete(true)
                    _authState.value = AuthState.Success
                }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Registration failed") }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter email and password")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.login(email, password)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Login failed") }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
