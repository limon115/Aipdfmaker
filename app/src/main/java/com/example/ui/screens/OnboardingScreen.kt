package com.example.ui.screens


import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    val permissionsToRequest = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    val getStartedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ ->
            onGetStartedClick()
        }
    )

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ ->
            onSignInClick()
        }
    )

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 1000))
        ) {
            com.example.ui.components.glass.GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "DocMorph Notes AI",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Your intelligent companion for crafting, organizing, and morphing notes effortlessly.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(durationMillis = 1000, delayMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 1000, delayMillis = 300))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { getStartedLauncher.launch(permissionsToRequest) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Get Started", style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { signInLauncher.launch(permissionsToRequest) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Sign In", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
