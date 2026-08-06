package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        DocMorphApp()
      }
    }
  }
}

@Composable
fun DocMorphApp() {
  val navController = rememberNavController()
  
  NavHost(navController = navController, startDestination = "onboarding") {
    composable("onboarding") {
      OnboardingScreen(
        onGetStartedClick = {
          navController.navigate("main") {
            popUpTo("onboarding") { inclusive = true }
          }
        },
        onSignInClick = {
          navController.navigate("main") {
            popUpTo("onboarding") { inclusive = true }
          }
        }
      )
    }
    composable("main") {
      MainScreen()
    }
  }
}
