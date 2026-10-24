package com.example.ui.components.glass

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxControlCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val termuxPermission = "com.termux.permission.RUN_COMMAND"
    var customCommand by remember { mutableStateOf("") }
    
    // Function to physically send the command intent to Termux
    val executeCommand: (String) -> Unit = { cmd ->
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Toast.makeText(context, "Command sent to Termux: $cmd", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Termux Bridge Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Runtime Permission Launcher (Solves the silent permission block)
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCommand?.let { executeCommand(it) }
            pendingCommand = null
        } else {
            Toast.makeText(context, "Termux permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    val requestAndExecute: (String) -> Unit = { cmd ->
        if (ContextCompat.checkSelfPermission(context, termuxPermission) == PackageManager.PERMISSION_GRANTED) {
            executeCommand(cmd)
        } else {
            pendingCommand = cmd
            permissionLauncher.launch(termuxPermission)
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Termux Bridge",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Termux Server Bridge",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            }

            // Dedicated Start Server Button
            GlassButton(
                onClick = { requestAndExecute("python server3.py") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Localhost Server (server3.py)")
            }

            // Custom Command Section
            OutlinedTextField(
                value = customCommand,
                onValueChange = { customCommand = it },
                placeholder = { Text("Enter custom bash command...", color = Color(0xFF94A3B8)) },
                leadingIcon = { Text("$", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0x33FFFFFF),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Color(0xFF2563EB)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { 
                            if (customCommand.isNotBlank()) requestAndExecute(customCommand) 
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color(0xFF2563EB))
                    }
                }
            )
        }
    }
}
