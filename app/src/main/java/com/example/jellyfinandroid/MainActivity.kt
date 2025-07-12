package com.example.jellyfinandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.memory.MemoryCache
import com.example.jellyfinandroid.ui.components.BottomNavBar
import com.example.jellyfinandroid.ui.navigation.JellyfinNavGraph
import com.example.jellyfinandroid.ui.navigation.Screen
import com.example.jellyfinandroid.ui.theme.JellyfinAndroidTheme
import com.example.jellyfinandroid.ui.viewmodel.ServerConnectionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // ✅ Optimize image loading performance
        setupImageLoader()
        
        setContent {
            JellyfinAndroidTheme(
                dynamicColor = true
            ) {
                val navController = rememberNavController()
                val connectionViewModel: ServerConnectionViewModel = hiltViewModel()
                val connectionState by connectionViewModel.connectionState.collectAsState()
                
                // Determine the start destination based on connection state
                val startDestination = if (connectionState.isConnected) {
                    Screen.Home.route
                } else {
                    Screen.ServerConnection.route
                }
                
                // Main app scaffold with bottom navigation
                Scaffold(
                    bottomBar = {
                        if (connectionState.isConnected) {
                            BottomNavBar(navController = navController)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    JellyfinNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                        onLogout = {
                            // Handle logout if needed
                        }
                    )
                }
            }
        }
    }
    
    /**
     * ✅ IMPROVEMENT: Configure Coil for better image loading performance
     * - Memory cache: 25% of app memory for faster image loading
     * - Disk cache: Persistent storage for offline viewing
     * - Hardware acceleration: Better performance on supported devices
     */
    private fun setupImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of app memory
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB
                    .build()
            }
            .respectCacheHeaders(false) // Better for media content
            .build()
        
        coil.Coil.setImageLoader(imageLoader)
    }
}
