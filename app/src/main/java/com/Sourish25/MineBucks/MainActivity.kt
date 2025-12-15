package com.Sourish25.MineBucks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import com.Sourish25.MineBucks.ui.DashboardScreen
import com.Sourish25.MineBucks.ui.OnboardingScreen
import com.Sourish25.MineBucks.ui.CurseForgeLoginScreen
import com.Sourish25.MineBucks.ui.SettingsScreen
import com.Sourish25.MineBucks.ui.RevenueViewModel
import com.Sourish25.MineBucks.ui.theme.MineBucksTheme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.animation.core.tween

import androidx.compose.animation.animateColorAsState

import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RevenueViewModel

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        com.Sourish25.MineBucks.ads.AdManager.initialize(this)
        // Preload Ad
        com.Sourish25.MineBucks.ads.AdManager.loadAd(this)
        
        // Initialize manually to access it here and in onNewIntent (or use by viewModels())
        // But we need the Factory.
        viewModel = androidx.lifecycle.ViewModelProvider(this, RevenueViewModel.Factory)[RevenueViewModel::class.java]
        
        handleDeepLink(intent)

        // Request Notification Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             val permission = android.Manifest.permission.POST_NOTIFICATIONS
             
             val requestPermissionLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted.
                } else {
                    // Explain to user? State is managed by UI.
                }
            }
            
             if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 // Check if we should show rationale
                 if (shouldShowRequestPermissionRationale(permission)) {
                     // Show UI (handled by PulseUiState usually, but here we can just fire the request for now 
                     // or rely on a Composable to show the rationale. 
                     // Since we are in onCreate, we can't easily show Composable Dialogs instantly without state.
                     // Better approach: Let DashboardScreen handle this check!
                 } else {
                     requestPermissionLauncher.launch(permission)
                 }
             }
        }

        setContent {
            // Re-use the same viewModel instance
            // val viewModel: RevenueViewModel = viewModel(...) // No, use the class property
            
            val uiState by viewModel.uiState.collectAsState()
            
            // Theme Logic
            val isDarkTheme = when(uiState.appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MineBucksTheme(darkTheme = isDarkTheme) {
                // Scoped WebView State
                var showWebView by remember { mutableStateOf(false) }
                // Settings State
                var showSettings by remember { mutableStateOf(false) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                         if (showWebView) {
                            BackHandler { showWebView = false }
                            CurseForgeLoginScreen(
                                onLoginSuccess = { points, cookies ->
                                    viewModel.saveCurseForgePoints(points)
                                    viewModel.saveCurseForgeCookies(cookies)
                                    showWebView = false
                                },
                                onClose = { showWebView = false }
                            )
                        } else {
                            // Screen Transitions
                            AnimatedContent(
                                targetState = showSettings,
                                transitionSpec = {
                                    if (targetState) {
                                        // Entering Settings
                                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                                    } else {
                                        // Exiting Settings
                                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                                    }
                                },
                                label = "ScreenTransition"
                            ) { isSettings ->
                                if (isSettings) {
                                    BackHandler { showSettings = false }
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        uiState = uiState,
                                        onBack = { showSettings = false },
                                        onConnectCurseForge = { 
                                            showSettings = false
                                            showWebView = true 
                                        }
                                    )
                                } else if (uiState.onboarded) {
                                    DashboardScreen(
                                        viewModel = viewModel, 
                                        uiState = uiState,
                                        onOpenSettings = { showSettings = true }
                                    )
                                } else {
                                    OnboardingScreen(
                                        viewModel = viewModel,
                                        onConnectCurseForge = { showWebView = true },
                                        onFinish = { }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "modrevenue" && data.host == "auth") {
            // Format: modrevenue://auth?token=...
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                viewModel.saveModrinthToken(token)
            }
        }
    }
}


