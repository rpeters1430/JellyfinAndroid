package com.example.jellyfinandroid.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinandroid.data.repository.ApiResult
import com.example.jellyfinandroid.data.repository.JellyfinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val serverName: String? = null,
    val savedServerUrl: String = "",
    val savedUsername: String = ""
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login_preferences")

object PreferencesKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val USERNAME = stringPreferencesKey("username")
}

@HiltViewModel
class ServerConnectionViewModel @Inject constructor(
    private val repository: JellyfinRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    init {
        // Load saved credentials
        viewModelScope.launch {
            val preferences = context.dataStore.data.first()
            val savedServerUrl = preferences[PreferencesKeys.SERVER_URL] ?: ""
            val savedUsername = preferences[PreferencesKeys.USERNAME] ?: ""
            
            _connectionState.value = _connectionState.value.copy(
                savedServerUrl = savedServerUrl,
                savedUsername = savedUsername
            )
        }
        
        // Observe repository connection state
        viewModelScope.launch {
            repository.isConnected.collect { isConnected ->
                _connectionState.value = _connectionState.value.copy(
                    isConnected = isConnected,
                    isConnecting = false
                )
            }
        }
    }
    
    fun connectToServer(serverUrl: String, username: String, password: String, rememberLogin: Boolean = false) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _connectionState.value = _connectionState.value.copy(
                errorMessage = "Please fill in all fields"
            )
            return
        }
        
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(
                isConnecting = true,
                errorMessage = null
            )
            
            // First test server connection
            when (val serverResult = repository.testServerConnection(serverUrl)) {
                is ApiResult.Success -> {
                    val serverInfo = serverResult.data
                    _connectionState.value = _connectionState.value.copy(
                        serverName = serverInfo.serverName
                    )
                    
                    // Now authenticate
                    when (val authResult = repository.authenticateUser(serverUrl, username, password)) {
                        is ApiResult.Success -> {
                            // Save credentials if remember login is enabled
                            if (rememberLogin) {
                                saveCredentials(serverUrl, username)
                            } else {
                                clearSavedCredentials()
                            }
                            
                            _connectionState.value = _connectionState.value.copy(
                                isConnecting = false,
                                isConnected = true,
                                errorMessage = null
                            )
                        }
                        is ApiResult.Error -> {
                            // Clear saved credentials on auth failure
                            clearSavedCredentials()
                            _connectionState.value = _connectionState.value.copy(
                                isConnecting = false,
                                errorMessage = authResult.message
                            )
                        }
                        is ApiResult.Loading -> {
                            // This shouldn't happen for this call
                        }
                    }
                }
                is ApiResult.Error -> {
                    _connectionState.value = _connectionState.value.copy(
                        isConnecting = false,
                        errorMessage = "Cannot connect to server: ${serverResult.message}"
                    )
                }
                is ApiResult.Loading -> {
                    // This shouldn't happen for this call
                }
            }
        }
    }
    
    fun clearError() {
        _connectionState.value = _connectionState.value.copy(errorMessage = null)
    }
    
    private suspend fun saveCredentials(serverUrl: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_URL] = serverUrl
            preferences[PreferencesKeys.USERNAME] = username
        }
    }
    
    private suspend fun clearSavedCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SERVER_URL)
            preferences.remove(PreferencesKeys.USERNAME)
        }
        _connectionState.value = _connectionState.value.copy(
            savedServerUrl = "",
            savedUsername = ""
        )
    }
    
    fun startQuickConnect() {
        // TODO: Implement Quick Connect functionality
        _connectionState.value = _connectionState.value.copy(
            errorMessage = "Quick Connect not yet implemented"
        )
    }
}
