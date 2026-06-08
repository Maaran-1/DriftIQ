package com.driftiq.app.presentation.risk

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.driftiq.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskTrendScreen(
    navController: NavController,
    viewModel: RiskViewModel = hiltViewModel(),
) {
    val risk by viewModel.risk.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Risk Analysis", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            risk?.let { r ->
                // Risk Level Card
                val riskCol = riskColor(r.riskLevel)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, riskCol.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(riskCol.copy(alpha = 0.08f), BackgroundCard))
                            )
                            .padding(24.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            // Level badge
                            Box(
                                modifier = Modifier.size(80.dp).clip(CircleShape)
                                    .background(riskCol.copy(alpha = 0.15f))
                                    .border(2.dp, riskCol.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${r.riskLevel}", color = riskCol, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(r.riskLabel, color = riskCol, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            // Trend indicator
                            val trendIcon = when (r.trend) {
                                "improving" -> Icons.Default.TrendingDown
                                "worsening" -> Icons.Default.TrendingUp
                                else -> Icons.Default.TrendingFlat
                            }
                            val trendColor = when (r.trend) {
                                "improving" -> Success
                                "worsening" -> ErrorColor
                                else -> TextSecondary
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(16.dp))
                                Text(r.trend.replaceFirstChar { it.uppercase() }, color = trendColor, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Explanation
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BackgroundElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Behavioral Analysis", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(r.explanation, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }

                // Risk Level Guide
                RiskLevelGuide(currentLevel = r.riskLevel)
            } ?: run {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Text("No risk data available yet.\nBuild your baseline to see risk analysis.",
                        color = TextTertiary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RiskLevelGuide(currentLevel: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BackgroundElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Risk Level Guide", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                0 to "Healthy — Patterns within your normal range",
                1 to "Observation — Minor shift detected",
                2 to "Mild Concern — Noticeable behavioral change",
                3 to "Moderate Concern — Sustained pattern shift",
                4 to "High Concern — Significant sustained change",
                5 to "Critical — Substantial and sustained drift",
            ).forEach { (level, desc) ->
                val col = riskColor(level)
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (level == currentLevel) col else col.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$level", color = if (level == currentLevel) Color.White else col, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(desc, color = if (level == currentLevel) TextPrimary else TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
