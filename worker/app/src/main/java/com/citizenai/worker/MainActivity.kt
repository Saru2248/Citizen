package com.citizenai.worker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.citizenai.worker.data.local.SessionManager
import com.citizenai.worker.presentation.navigation.Screen
import com.citizenai.worker.presentation.navigation.WorkerNavGraph
import com.citizenai.worker.presentation.theme.DarkBackground
import com.citizenai.worker.presentation.theme.PrimaryBlue
import com.citizenai.worker.presentation.theme.WorkerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkerTheme {
                val isLoggedInState = sessionManager.isLoggedIn.collectAsState(initial = null)

                when (val loggedIn = isLoggedInState.value) {
                    null -> {
                        // Loading session state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                    else -> {
                        val navController = rememberNavController()
                        val startDestination = if (loggedIn) Screen.Dashboard.route else Screen.Login.route
                        WorkerNavGraph(navController = navController, startDestination = startDestination)
                    }
                }
            }
        }
    }
}
