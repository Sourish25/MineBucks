package com.example.newapp

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

import com.example.newapp.ui.DashboardScreen
import com.example.newapp.ui.OnboardingScreen
import com.example.newapp.ui.CurseForgeLoginScreen
import com.example.newapp.ui.SettingsScreen
import com.example.newapp.ui.RevenueViewModel
import com.example.newapp.ui.theme.NewAppTheme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.core.tween

import androidx.compose.animation.animateColorAsState

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: RevenueViewModel

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        com.google.android.gms.ads.MobileAds.initialize(this) {}
        // Preload Ad
        com.example.newapp.ads.AdManager.loadAd(this)
        
        // Initialize manually to access it here and in onNewIntent (or use by viewModels())
        // But we need the Factory.
        viewModel = androidx.lifecycle.ViewModelProvider(this, RevenueViewModel.Factory)[RevenueViewModel::class.java]
        
        handleDeepLink(intent)

        // Request Notification Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             val permission = android.Manifest.permission.POST_NOTIFICATIONS
             if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 requestPermissions(arrayOf(permission), 101)
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

            NewAppTheme(darkTheme = isDarkTheme) {
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
                                        slideInHorizontally { it } + fadeIn() with slideOutHorizontally { -it / 3 } + fadeOut()
                                    } else {
                                        // Exiting Settings
                                        slideInHorizontally { -it } + fadeIn() with slideOutHorizontally { it / 3 } + fadeOut()
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


