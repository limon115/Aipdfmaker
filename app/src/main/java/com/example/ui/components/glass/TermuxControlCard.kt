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
    var isExecuting by remember { mutableStateOf(false) }

    // Whitelisted commands only - prevents command injection
    val allowedCommands = listOf(
        "python server3.py" to "Start LaTeX Server",
        "pkill -f server3.py" to "Stop LaTeX Server",
        "python -m pip install --upgrade requests" to "Update Dependencies"
    )

    val executeCommand: (String) -> Unit = { cmd ->
        // Security: validate command against whitelist
        if (!allowedCommands.any { it.first == cmd }) {
            Toast.makeText(context, "Command not allowed", Toast.LENGTH_SHORT).show()
            return@Unit
        }

        isExecuting = true
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
            Toast.makeText(context, "Command sent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isExecuting = false
        }
    }

    var pendingCommand by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCommand?.let { executeCommand(it) }
            pendingCommand = null
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Termux Commands",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Termux Bridge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            allowedCommands.forEach { (command, label) ->
                GlassButton(
                    onClick = { requestAndExecute(command) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExecuting
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run $label",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
