package com.example.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.models.ThemeMode
import com.example.ui.components.glass.GlassBackground
import com.example.ui.components.glass.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToTheme: () -> Unit = {},
    onNavigateToFonts: () -> Unit = {},
    onNavigateToAiConfig: (aiIndex: Int) -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    onNavigateToBackground: () -> Unit = {},
    onNavigateToUpdates: () -> Unit = {},
    onNavigateToApiLab: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToProviderSelection: (aiIndex: Int) -> Unit = {},
    onBottomReached: (Boolean) -> Unit = {}
) {
    val settingsNullable by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val isAtBottom by remember {
        derivedStateOf {
            scrollState.value > 0 &&
                scrollState.maxValue > 0 &&
                (scrollState.value >= scrollState.maxValue - 20 || !scrollState.canScrollForward)
        }
    }

    LaunchedEffect(isAtBottom) {
        onBottomReached(isAtBottom)
    }

    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (settingsNullable == null) {
            com.example.ui.components.WavyLoadingScreen(
                message = "Loading Settings...",
                subMessage = "Reading local configuration",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val settings = settingsNullable!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Header welcome subtitle
                Text(
                    text = "Configure system appearance, multi-agent AI pipelines, generation parameters, and developer diagnostics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category 1: Appearance
                CategorySection(title = "APPEARANCE") {
                    SettingsMenuRow(
                        title = "App Theme",
                        subtitle = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> "System schedule active • Light & dark preview"
                            ThemeMode.LIGHT -> "Light mode active • High contrast"
                            ThemeMode.DARK -> "Dark mode active • Midnight oceanic"
                        },
                        icon = Icons.Default.Palette,
                        iconColor = Color(0xFF8B5CF6),
                        onClick = onNavigateToTheme
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    SettingsMenuRow(
                        title = "Fonts",
                        subtitle = if (settings.customFontPath.isNotBlank()) "Custom font loaded • Global typeface"
                        else "System Default • Sans-serif",
                        icon = Icons.Default.TextFields,
                        iconColor = Color(0xFF3B82F6),
                        onClick = onNavigateToFonts
                    )
                }

                // Category 2: AI Assistants
                CategorySection(title = "AI ASSISTANTS") {
                    SettingsMenuRow(
                        title = "AI #1 – Blueprint Builder",
                        subtitle = "Outline & structure • ${settings.ai1Provider.displayName}",
                        icon = Icons.Default.AutoAwesome,
                        iconColor = Color(0xFFF59E0B),
                        onClick = { onNavigateToAiConfig(1) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    SettingsMenuRow(
                        title = "AI #2 – Note Generator",
                        subtitle = "LaTeX notes & equations • ${settings.ai2Provider.displayName}",
                        icon = Icons.Default.Description,
                        iconColor = Color(0xFF6366F1),
                        onClick = { onNavigateToAiConfig(2) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    SettingsMenuRow(
                        title = "AI #3 – LaTeX Debugger",
                        subtitle = "Compile diagnostics & TeX repair • ${settings.ai3Provider.displayName}",
                        icon = Icons.Default.BugReport,
                        iconColor = Color(0xFF14B8A6),
                        onClick = { onNavigateToAiConfig(3) }
                    )
                }

                // Category 3: Generation & Performance
                CategorySection(title = "GENERATION & PERFORMANCE") {
                    SettingsMenuRow(
                        title = "Advanced Settings",
                        subtitle = "Temperature, output tokens, streaming & caching",
                        icon = Icons.Default.Tune,
                        iconColor = Color(0xFF10B981),
                        onClick = onNavigateToAdvanced
                    )
                }

                // Category 4: Background & Updates
                CategorySection(title = "BACKGROUND & UPDATES") {
                    SettingsMenuRow(
                        title = "Background Processing",
                        subtitle = "Battery optimization & foreground services",
                        icon = Icons.Default.BatteryAlert,
                        iconColor = Color(0xFFF43F5E),
                        onClick = onNavigateToBackground
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    SettingsMenuRow(
                        title = "App Updates",
                        subtitle = "GitHub CI/CD artifact releases & token",
                        icon = Icons.Default.SystemUpdate,
                        iconColor = Color(0xFF0EA5E9),
                        onClick = onNavigateToUpdates
                    )
                }

                // Category 5: Developer
                CategorySection(title = "DEVELOPER") {
                    SettingsMenuRow(
                        title = "API Lab",
                        subtitle = "Test provider credentials & latency",
                        icon = Icons.Default.Science,
                        iconColor = Color(0xFFF97316),
                        onClick = onNavigateToApiLab
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    SettingsMenuRow(
                        title = "App Logs",
                        subtitle = "Real-time execution log stream & errors",
                        icon = Icons.Default.Terminal,
                        iconColor = Color(0xFF64748B),
                        onClick = onNavigateToLogs
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
}

@Composable
private fun CategorySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// -------------------------------------------------------------
// Compatibility Exports for BottomNavItem.Dashboard screen
// -------------------------------------------------------------

@Composable
fun AiUsageDashboardCard() {
    val stats by com.example.domain.services.ai.AiUsageTracker.stats.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Session AI Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Requests:", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.requests}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Est. Tokens Used:", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.estimatedTokens}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cache Hits (Saved Tokens):", style = MaterialTheme.typography.bodyMedium)
                    Text("${stats.cacheHits}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rate Limit (429) Errors:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${stats.rateLimitErrors}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.rateLimitErrors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (stats.tokensByFeature.isNotEmpty()) {
            UsagePieChartCard(stats.tokensByFeature, "Token Distribution")
        }

        if (stats.requestsByFeature.isNotEmpty()) {
            FeatureBarChartCard(stats.requestsByFeature, "Requests by Feature")
        }

        UsageBarChartCard(stats.requests, stats.cacheHits, stats.rateLimitErrors)
    }
}

@Composable
fun UsagePieChartCard(data: Map<String, Int>, title: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val total = data.values.sum().coerceAtLeast(1)
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.error,
                Color(0xFF4CAF50),
                Color(0xFFFF9800)
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp).padding(8.dp)) {
                    var startAngle = -90f
                    data.entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.value.toFloat() / total) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = size
                        )
                        startAngle += sweepAngle
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.entries.forEachIndexed { index, entry ->
                        val percentage = (entry.value.toFloat() / total) * 100
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size], CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(entry.key, style = MaterialTheme.typography.labelMedium)
                                Text("${entry.value} (${String.format(java.util.Locale.US, "%.1f", percentage)}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBarChartCard(data: Map<String, Int>, title: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                Color(0xFF4CAF50)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.entries.forEachIndexed { index, entry ->
                    ChartBarRow(entry.key, entry.value, maxVal, colors[index % colors.size])
                }
            }
        }
    }
}

@Composable
fun UsageBarChartCard(requests: Int, cacheHits: Int, rateLimits: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Usage Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val maxVal = maxOf(requests, cacheHits, rateLimits, 1)

            val primaryColor = MaterialTheme.colorScheme.primary
            val successColor = Color(0xFF4CAF50)
            val errorColor = MaterialTheme.colorScheme.error

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartBarRow("Requests", requests, maxVal, primaryColor)
                ChartBarRow("Cache Hits", cacheHits, maxVal, successColor)
                ChartBarRow("Rate Limits", rateLimits, maxVal, errorColor)
            }
        }
    }
}

@Composable
fun ChartBarRow(label: String, value: Int, maxVal: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(4.dp))
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (value > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp).align(Alignment.CenterStart)
            )
        }
    }
}
