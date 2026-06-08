package com.driftiq.app.presentation.reports.weekly

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.driftiq.app.presentation.home.DriftIQBottomNav
import com.driftiq.app.presentation.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDashboardScreen(
    navController: NavController,
    viewModel: WeeklyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val fmt = DateTimeFormatter.ofPattern("MMM d")

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Weekly Report", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
        bottomBar = { DriftIQBottomNav(navController, "weekly") },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Insight card
            state.insight?.let { insight ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(BrandPrimary.copy(alpha = 0.08f), BackgroundCard)))
                            .padding(20.dp),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.BarChart, null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                                Text("Weekly Insight", color = BrandSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${insight.periodStart.format(fmt)} – ${insight.periodEnd.format(fmt)}",
                                color = TextTertiary, fontSize = 11.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            // Wellness score
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${insight.wellnessScore}", color = BrandPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                                Text(" wellness", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(insight.content, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // 7-day drift bar chart
            if (state.history.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BackgroundElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("7-Day Drift Trend", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            state.history.takeLast(7).forEach { entry ->
                                val barHeight = ((entry.compositeDrift / 100f) * 100).dp
                                val riskCol = riskColor(entry.riskLevel)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(barHeight.coerceAtLeast(8.dp))
                                            .background(
                                                Brush.verticalGradient(listOf(riskCol, riskCol.copy(alpha = 0.4f))),
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                            ),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(entry.date.format(DateTimeFormatter.ofPattern("d")), color = TextTertiary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Navigation to other sub-reports
            Text("Detailed Reports", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                "Sleep Trends" to "sleep_trends",
                "Activity & Screen Time" to "activity_trends",
                "Productivity" to "productivity_trends",
                "Learning" to "learning_trends",
            ).forEach { (label, route) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate(route) },
                    shape = RoundedCornerShape(14.dp),
                    color = BackgroundCard,
                    border = BorderStroke(1.dp, BackgroundElevated),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = TextPrimary, fontSize = 14.sp)
                        Text("→", color = BrandSecondary, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
