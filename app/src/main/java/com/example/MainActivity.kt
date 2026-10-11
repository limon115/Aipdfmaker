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
import com.example.ui.theme.ThemeProvider
import com.example.ui.components.glass.LiquidBackground
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.datastore.AiSettingsDataStore
import com.example.domain.models.ThemeMode


class MainActivity : ComponentActivity() {

  override fun onTrimMemory(level: Int) {
      super.onTrimMemory(level)
      if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
          // System recommends trimming memory; clear caches here if any
          System.gc()
      }
  }

  override fun onNewIntent(intent: android.content.Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val dataStore = AiSettingsDataStore(applicationContext)
    val prefs = getSharedPreferences("docmorph_prefs", android.content.Context.MODE_PRIVATE)
    val hasCompletedOnboarding = prefs.getBoolean("onboarding_completed", false)
    val hasDeepLink = intent?.data?.scheme == "docmorph"
    val initialRoute = if (hasCompletedOnboarding || hasDeepLink) "main" else "onboarding"
    
    setContent {
      val settings by dataStore.aiSettingsFlow.collectAsState(initial = null)
      val themeMode = settings?.themeMode ?: ThemeMode.SYSTEM
      val isDarkTheme = when (themeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      ThemeProvider(
          initialDarkTheme = isDarkTheme,
          customFontPath = settings?.customFontPath ?: ""
      ) {
        LiquidBackground {
        DocMorphApp(
            initialRoute = initialRoute,
            onCompleteOnboarding = {
                prefs.edit().putBoolean("onboarding_completed", true).apply()
            }
        )
      }
      }
    }
  }
}

@Composable
fun DocMorphApp(
    initialRoute: String = "onboarding",
    onCompleteOnboarding: () -> Unit = {}
) {
  val navController = rememberNavController()
  
  NavHost(navController = navController, startDestination = initialRoute) {
    composable("onboarding") {
      OnboardingScreen(
        onGetStartedClick = {
          onCompleteOnboarding()
          navController.navigate("main") {
            popUpTo("onboarding") { inclusive = true }
          }
        },
        onSignInClick = {
          onCompleteOnboarding()
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

