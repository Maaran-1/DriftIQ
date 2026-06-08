package com.driftiq.app.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.driftiq.app.Screen
import com.driftiq.app.domain.model.DashboardSummary
import com.driftiq.app.domain.model.Highlight
import com.driftiq.app.presentation.theme.*
import kotlin.math.min

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        bottomBar = { DriftIQBottomNav(navController, "home") },
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> LoadingState(Modifier.padding(padding))
            is HomeUiState.Error -> ErrorState(state.message, viewModel::refresh, Modifier.padding(padding))
            is HomeUiState.Ready -> HomeContent(state.summary, navController, Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeContent(summary: DashboardSummary, navController: NavController, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { GreetingHeader() }
        item { WellnessGaugeCard(summary.wellnessScore, summary.riskLevel, summary.riskLabel) }
        item { MetricsRow(summary.driftScore, summary.baselineDays, navController) }
        if (summary.highlights.isNotEmpty()) {
            item { HighlightsSection(summary.highlights) }
        }
        item { DimensionQuickAccess(navController) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun GreetingHeader() {
    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(greeting, color = TextSecondary, fontSize = 14.sp)
        Text("Your Dashboard", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WellnessGaugeCard(wellnessScore: Int, riskLevel: Int, riskLabel: String) {
    val riskCol = riskColor(riskLevel)
    val animatedScore by animateIntAsState(targetValue = wellnessScore, animationSpec = tween(1200), label = "score")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BackgroundElevated),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(BackgroundCard, BackgroundPrimary),
                    ),
                )
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Circular Gauge
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    CircularWellnessGauge(animatedScore, riskCol)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            animatedScore.toString(),
                            color = TextPrimary,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text("Wellness", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Risk Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = riskCol.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, riskCol.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(riskCol))
                        Text("Level $riskLevel — $riskLabel", color = riskCol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularWellnessGauge(score: Int, riskColor: Color) {
    val sweepAngle = (score / 100f) * 270f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 14.dp.toPx()
        val radius = (size.minDimension / 2f) - (strokeWidth / 2)
        val center = Offset(size.width / 2f, size.height / 2f)
        // Background track
        drawArc(
            color = Color.White.copy(alpha = 0.08f),
            startAngle = 135f, sweepAngle = 270f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
        // Progress arc
        drawArc(
            brush = Brush.sweepGradient(listOf(BrandPrimary, riskColor)),
            startAngle = 135f, sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
    }
}

@Composable
private fun MetricsRow(driftScore: Float, baselineDays: Int, navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = "Behavioral Drift",
            value = driftScore.toInt().toString(),
            unit = "/ 100",
            color = if (driftScore > 60) Warning else BrandPrimary,
            modifier = Modifier.weight(1f),
        ) { navController.navigate(Screen.DriftDetail.route) }
        MetricCard(
            label = "Baseline Days",
            value = baselineDays.toString(),
            unit = "days",
            color = BrandSecondary,
            modifier = Modifier.weight(1f),
        ) { navController.navigate(Screen.Risk.route) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, unit: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BackgroundElevated),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = TextTertiary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(unit, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
            }
        }
    }
}

@Composable
private fun HighlightsSection(highlights: List<Highlight>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Today's Highlights", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        highlights.forEach { highlight ->
            HighlightCard(highlight)
        }
    }
}

@Composable
private fun HighlightCard(highlight: Highlight) {
    val color = when (highlight.type) {
        "sleep" -> DimSleep
        "social" -> DimSocial
        "productivity" -> DimProductivity
        "entertainment" -> DimEntertainment
        "learning" -> DimLearning
        else -> BrandSecondary
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Text(highlight.message, color = TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DimensionQuickAccess(navController: NavController) {
    Text("Dimensions", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            Triple("Sleep", Icons.Default.NightlightRound, DimSleep) to Screen.Sleep.route,
            Triple("Activity", Icons.Default.PhoneAndroid, BrandSecondary) to Screen.Activity.route,
            Triple("Social", Icons.Default.People, DimSocial) to Screen.Productivity.route,
            Triple("Learning", Icons.Default.School, DimLearning) to Screen.Learning.route,
        ).forEach { (item, route) ->
            DimensionChip(item.first, item.third, modifier = Modifier.weight(1f)) {
                navController.navigate(route)
            }
        }
    }
}

@Composable
private fun DimensionChip(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.height(4.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DriftIQBottomNav(navController: NavController, currentRoute: String) {
    NavigationBar(containerColor = BackgroundCard, tonalElevation = 0.dp) {
        listOf(
            Triple("home", "Home", Icons.Default.Home),
            Triple("timeline", "Timeline", Icons.Default.Timeline),
            Triple("weekly", "Reports", Icons.Default.BarChart),
            Triple("settings", "Settings", Icons.Default.Settings),
        ).forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { if (currentRoute != route) navController.navigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary,
                    indicatorColor = BrandPrimary.copy(alpha = 0.15f),
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                ),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandPrimary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)) {
            Text("Retry")
        }
    }
}
