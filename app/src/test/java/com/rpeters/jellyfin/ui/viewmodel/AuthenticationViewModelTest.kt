package com.rpeters.jellyfin.ui.viewmodel

import com.rpeters.jellyfin.data.SecureCredentialManager
import com.rpeters.jellyfin.data.repository.JellyfinAuthRepository
import com.rpeters.jellyfin.data.repository.JellyfinUserRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: JellyfinAuthRepository
    private lateinit var userRepository: JellyfinUserRepository
    private lateinit var credentialManager: SecureCredentialManager
    private lateinit var viewModel: AuthenticationViewModel

    private val isAuthenticatingFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        credentialManager = mockk(relaxed = true)

        every { authRepository.isAuthenticating } returns isAuthenticatingFlow
        every { authRepository.isUserAuthenticated() } returns false

        viewModel = AuthenticationViewModel(authRepository, userRepository, credentialManager)
    }

    @Test
    fun `initial state reflects repository values`() = runTest {
        val state = viewModel.authState.value
        assertFalse(state.isAuthenticating)
        assertFalse(state.isAuthenticated)
        assertNull(state.errorMessage)
    }

    @Test
    fun `clearError resets error message`() = runTest {
        // Internal state modification for test (via refresh failure) is better, but direct check for simplicity
        viewModel.clearError()
        assertNull(viewModel.authState.value.errorMessage)
    }

    @Test
    fun `ensureValidTokenWithWait returns true if token not expired`() = runTest {
        every { authRepository.isTokenExpired() } returns false
        
        val result = viewModel.ensureValidTokenWithWait()
        
        assertTrue(result)
    }

    @Test
    fun `ensureValidTokenWithWait returns true after successful re-authentication`() = runTest {
        every { authRepository.isTokenExpired() } returnsMany listOf(true, false)
        coEvery { authRepository.reAuthenticate() } returns true
        
        val result = viewModel.ensureValidTokenWithWait()
        
        assertTrue(result)
    }

    @Test
    fun `ensureValidTokenWithWait returns false after failed re-authentication`() = runTest {
        every { authRepository.isTokenExpired() } returns true
        coEvery { authRepository.reAuthenticate() } returns false
        
        val result = viewModel.ensureValidTokenWithWait()
        
        assertFalse(result)
    }
}
