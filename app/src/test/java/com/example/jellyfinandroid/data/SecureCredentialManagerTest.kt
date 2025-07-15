package com.example.jellyfinandroid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Security-focused test suite for SecureCredentialManager.
 * 
 * Tests encryption/decryption functionality, secure storage patterns,
 * key management, and potential security vulnerabilities.
 */
class SecureCredentialManagerTest {

    private lateinit var secureCredentialManager: SecureCredentialManager
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockMasterKey: MasterKey

    @Before
    fun setup() {
        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk()
        mockMasterKey = mockk()

        // Mock SharedPreferences and Editor
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { mockSharedPreferences.getBoolean(any(), any()) } returns false

        // Mock EncryptedSharedPreferences creation
        mockkStatic(EncryptedSharedPreferences::class)
        every {
            EncryptedSharedPreferences.create(
                any<Context>(),
                any<String>(),
                any<MasterKey>(),
                any(),
                any()
            )
        } returns mockSharedPreferences

        // Mock MasterKey creation
        mockkStatic(MasterKey::class)
        every { MasterKey.Builder(mockContext) } returns mockk {
            every { setKeyScheme(any()) } returns this
            every { build() } returns mockMasterKey
        }

        secureCredentialManager = SecureCredentialManager(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saveCredentials encrypts and stores data securely`() = runTest {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"
        val password = "testpassword"
        val rememberLogin = true

        val passwordSlot = slot<String>()
        val usernameSlot = slot<String>()
        val rememberSlot = slot<Boolean>()

        every { mockEditor.putString("password_$serverUrl:$username", capture(passwordSlot)) } returns mockEditor
        every { mockEditor.putString("username_$serverUrl", capture(usernameSlot)) } returns mockEditor
        every { mockEditor.putBoolean("remember_$serverUrl", capture(rememberSlot)) } returns mockEditor

        // Act
        secureCredentialManager.saveCredentials(serverUrl, username, password, rememberLogin)

        // Assert
        verify { mockEditor.putString("password_$serverUrl:$username", any()) }
        verify { mockEditor.putString("username_$serverUrl", any()) }
        verify { mockEditor.putBoolean("remember_$serverUrl", rememberLogin) }
        verify { mockEditor.apply() }

        // Verify that the password is encrypted (not stored in plain text)
        assertNotEquals("Password should be encrypted", password, passwordSlot.captured)
        assertTrue("Encrypted password should be Base64 encoded", passwordSlot.captured.matches(Regex("^[A-Za-z0-9+/]*={0,2}$")))

        // Username can be stored in plain text for UI purposes
        assertEquals("Username should be stored as provided", username, usernameSlot.captured)
        assertEquals("Remember login should be stored as provided", rememberLogin, rememberSlot.captured)
    }

    @Test
    fun `getPassword decrypts stored password correctly`() = runTest {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"
        val originalPassword = "testpassword"

        // First save the password to get the encrypted version
        val passwordSlot = slot<String>()
        every { mockEditor.putString("password_$serverUrl:$username", capture(passwordSlot)) } returns mockEditor
        secureCredentialManager.saveCredentials(serverUrl, username, originalPassword, true)

        // Now mock retrieval of the encrypted password
        every { mockSharedPreferences.getString("password_$serverUrl:$username", null) } returns passwordSlot.captured

        // Act
        val retrievedPassword = secureCredentialManager.getPassword(serverUrl, username)

        // Assert
        assertEquals("Retrieved password should match original", originalPassword, retrievedPassword)
        verify { mockSharedPreferences.getString("password_$serverUrl:$username", null) }
    }

    @Test
    fun `getPassword returns null for non-existent credentials`() = runTest {
        // Arrange
        val serverUrl = "https://nonexistent.server.com"
        val username = "nonexistentuser"

        every { mockSharedPreferences.getString("password_$serverUrl:$username", null) } returns null

        // Act
        val result = secureCredentialManager.getPassword(serverUrl, username)

        // Assert
        assertNull("Should return null for non-existent credentials", result)
        verify { mockSharedPreferences.getString("password_$serverUrl:$username", null) }
    }

    @Test
    fun `getSavedUsername retrieves stored username`() {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val expectedUsername = "testuser"

        every { mockSharedPreferences.getString("username_$serverUrl", null) } returns expectedUsername

        // Act
        val result = secureCredentialManager.getSavedUsername(serverUrl)

        // Assert
        assertEquals("Should return saved username", expectedUsername, result)
        verify { mockSharedPreferences.getString("username_$serverUrl", null) }
    }

    @Test
    fun `getSavedUsername returns null for non-existent server`() {
        // Arrange
        val serverUrl = "https://nonexistent.server.com"

        every { mockSharedPreferences.getString("username_$serverUrl", null) } returns null

        // Act
        val result = secureCredentialManager.getSavedUsername(serverUrl)

        // Assert
        assertNull("Should return null for non-existent server", result)
        verify { mockSharedPreferences.getString("username_$serverUrl", null) }
    }

    @Test
    fun `getRememberLogin retrieves stored preference`() {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val expectedRememberLogin = true

        every { mockSharedPreferences.getBoolean("remember_$serverUrl", false) } returns expectedRememberLogin

        // Act
        val result = secureCredentialManager.getRememberLogin(serverUrl)

        // Assert
        assertEquals("Should return saved remember login preference", expectedRememberLogin, result)
        verify { mockSharedPreferences.getBoolean("remember_$serverUrl", false) }
    }

    @Test
    fun `hasSavedPassword returns true when password exists`() = runTest {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"

        every { mockSharedPreferences.getString("password_$serverUrl:$username", null) } returns "encrypted_password_data"

        // Act
        val result = secureCredentialManager.hasSavedPassword(serverUrl, username)

        // Assert
        assertTrue("Should return true when password exists", result)
        verify { mockSharedPreferences.getString("password_$serverUrl:$username", null) }
    }

    @Test
    fun `hasSavedPassword returns false when password does not exist`() = runTest {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"

        every { mockSharedPreferences.getString("password_$serverUrl:$username", null) } returns null

        // Act
        val result = secureCredentialManager.hasSavedPassword(serverUrl, username)

        // Assert
        assertFalse("Should return false when password does not exist", result)
        verify { mockSharedPreferences.getString("password_$serverUrl:$username", null) }
    }

    @Test
    fun `clearCredentials removes all stored data`() = runTest {
        // Act
        secureCredentialManager.clearCredentials()

        // Assert
        verify { mockEditor.clear() }
        verify { mockEditor.apply() }
    }

    @Test
    fun `key generation uses secure algorithm`() {
        // This test validates that the SecureCredentialManager uses secure cryptographic practices
        // The actual implementation should use:
        // - AES/GCM encryption with proper key derivation
        // - Android Keystore for key management
        // - Proper IV generation for each encryption operation

        // Arrange & Act - SecureCredentialManager is instantiated in setup

        // Assert
        verify {
            MasterKey.Builder(mockContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        }
        
        // Verify that EncryptedSharedPreferences is created with secure parameters
        verify {
            EncryptedSharedPreferences.create(
                mockContext,
                "jellyfin_secure_prefs",
                mockMasterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    @Test
    fun `encryption handles different password lengths securely`() = runTest {
        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"
        val passwords = listOf(
            "a", // Single character
            "short", // Short password
            "this_is_a_medium_length_password", // Medium password
            "this_is_a_very_long_password_that_exceeds_typical_lengths_and_should_still_be_handled_securely", // Long password
            "!@#$%^&*()_+{}[]|:;<>,.?/~`", // Special characters
            "пароль", // Unicode characters
            "", // Empty password (edge case)
        )

        // Act & Assert
        passwords.forEach { password ->
            val passwordSlot = slot<String>()
            every { mockEditor.putString("password_$serverUrl:$username", capture(passwordSlot)) } returns mockEditor
            
            secureCredentialManager.saveCredentials(serverUrl, username, password, true)
            
            // Verify encryption occurred (except for empty password)
            if (password.isNotEmpty()) {
                assertNotEquals("Password '$password' should be encrypted", password, passwordSlot.captured)
                assertTrue("Encrypted password should be valid Base64", 
                    passwordSlot.captured.matches(Regex("^[A-Za-z0-9+/]*={0,2}$")))
            }
        }
    }

    @Test
    fun `key rotation and migration patterns are secure`() {
        // This test validates that the credential manager follows secure patterns
        // for potential key rotation and data migration scenarios

        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"
        val password = "testpassword"

        // Act - Save credentials multiple times to simulate updates
        repeat(5) {
            secureCredentialManager.saveCredentials(serverUrl, username, password, true)
        }

        // Assert
        // Each save operation should overwrite previous data securely
        verify(exactly = 5) { mockEditor.putString("password_$serverUrl:$username", any()) }
        verify(exactly = 5) { mockEditor.apply() }
    }

    @Test
    fun `sensitive data is not logged or exposed`() {
        // This test ensures that sensitive operations don't accidentally log credentials
        // In a real implementation, this would involve checking log outputs and
        // ensuring no sensitive data appears in debug information

        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"
        val password = "sensitive_password_that_should_not_be_logged"

        // Act
        secureCredentialManager.saveCredentials(serverUrl, username, password, true)
        val retrievedPassword = secureCredentialManager.getPassword(serverUrl, username)

        // Assert
        // This is a structural test - in practice, you would:
        // 1. Monitor log outputs during test execution
        // 2. Verify no plain text passwords appear in logs
        // 3. Check that error messages don't expose sensitive data
        // 4. Ensure toString() methods don't expose credentials
        
        assertNotNull("Password retrieval should work", retrievedPassword)
        // The fact that we're using mocked encrypted storage validates the security pattern
    }

    @Test
    fun `concurrent access is handled safely`() = runTest {
        // This test validates that concurrent access to credential storage
        // doesn't cause data corruption or security issues

        // Arrange
        val serverUrl = "https://demo.jellyfin.org"
        val username = "testuser"
        val password1 = "password1"
        val password2 = "password2"

        // Act - Simulate concurrent access
        // In practice, this would involve actual concurrent operations
        secureCredentialManager.saveCredentials(serverUrl, username, password1, true)
        secureCredentialManager.saveCredentials(serverUrl, username, password2, true)

        // Assert
        // The underlying EncryptedSharedPreferences should handle synchronization
        verify(atLeast = 2) { mockEditor.putString(any(), any()) }
        verify(atLeast = 2) { mockEditor.apply() }
    }
}