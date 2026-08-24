package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.model.ConnectionStatus
import com.example.ui.IemViewModel
import com.example.ui.screens.*
import com.example.ui.theme.IemMixerTheme

enum class Screen {
    DASHBOARD,
    DISCOVERY,
    ACCOUNT_SETUP,
    MIXBUS_SELECTION,
    PROFILES,
    ENGINEER,
    DIAGNOSTICS
}

class MainActivity : ComponentActivity() {

    private val viewModel: IemViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsState()

            IemMixerTheme(themeMode = appThemeMode) {
                var isSplashActive by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf(Screen.DISCOVERY) }
                val snackbarHostState = remember { SnackbarHostState() }
                val notificationMessage by viewModel.notificationMessage.collectAsState()
                val connectionInfo by viewModel.connectionState.collectAsState()
                val isConnected = connectionInfo.status == ConnectionStatus.CONNECTED || connectionInfo.status == ConnectionStatus.SIMULATION

                if (isSplashActive) {
                    SplashScreen(
                        onSplashFinished = { isSplashActive = false }
                    )
                } else {
                    val navigateToAccountSetupWithCheck = {
                    if (isConnected) {
                        currentScreen = Screen.ACCOUNT_SETUP
                    } else {
                        viewModel.showNotification("Mixer connection required before account setup!")
                        currentScreen = Screen.DISCOVERY
                    }
                }

                val navigateToProfilesWithCheck = {
                    if (isConnected) {
                        currentScreen = Screen.PROFILES
                    } else {
                        viewModel.showNotification("Mixer connection required before profile setup!")
                        currentScreen = Screen.DISCOVERY
                    }
                }

                LaunchedEffect(notificationMessage) {
                    notificationMessage?.let { msg ->
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.clearNotification()
                    }
                }

                BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
                    currentScreen = Screen.DASHBOARD
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onOpenDiscovery = { currentScreen = Screen.DISCOVERY },
                                onOpenProfiles = navigateToProfilesWithCheck,
                                onOpenEngineer = { currentScreen = Screen.ENGINEER },
                                onOpenDiagnostics = { currentScreen = Screen.DIAGNOSTICS }
                            )
                            Screen.DISCOVERY -> DiscoveryScreen(
                                viewModel = viewModel,
                                onBackToDashboard = { currentScreen = Screen.DASHBOARD },
                                onNavigateToProfiles = navigateToAccountSetupWithCheck
                            )
                            Screen.ACCOUNT_SETUP -> AccountSetupScreen(
                                viewModel = viewModel,
                                onProceedToMixbusSelection = { currentScreen = Screen.MIXBUS_SELECTION },
                                onBackToDiscovery = { currentScreen = Screen.DISCOVERY }
                            )
                            Screen.MIXBUS_SELECTION -> MixbusSelectionScreen(
                                viewModel = viewModel,
                                onProceedToDashboard = { currentScreen = Screen.DASHBOARD },
                                onBackToDiscovery = { currentScreen = Screen.ACCOUNT_SETUP }
                            )
                            Screen.PROFILES -> ProfilesScreen(
                                viewModel = viewModel,
                                onBackToDashboard = { currentScreen = Screen.DASHBOARD },
                                onOpenDiscovery = { currentScreen = Screen.DISCOVERY }
                            )
                            Screen.ENGINEER -> EngineerScreen(
                                viewModel = viewModel,
                                onBackToDashboard = { currentScreen = Screen.DASHBOARD }
                            )
                            Screen.DIAGNOSTICS -> DiagnosticsScreen(
                                viewModel = viewModel,
                                onBackToDashboard = { currentScreen = Screen.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
