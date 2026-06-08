package com.driftiq.app.presentation.drift

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.driftiq.app.domain.model.DriftHistoryEntry
import com.driftiq.app.domain.model.DriftScore
import com.driftiq.app.presentation.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriftDetailScreen(
    navController: NavController,
    viewModel: DriftDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val periods = listOf(7 to "7D", 30 to "30D", 90 to "90D")

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Behavioral Drift", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Today's composite drift
            state.today?.let { today ->
                item { TodayDriftCard(today) }
                if (today.dimensionZScores.isNotEmpty()) {
                    item { DimensionBreakdownCard(today) }
                }
            }

            // Period selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEach { (days, label) ->
                        FilterChip(
                            selected = state.selectedPeriodDays == days,
                            onClick = { viewModel.changePeriod(days) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = BrandPrimary,
                                containerColor = BackgroundCard,
                                labelColor = TextSecondary,
                            ),
                        )
                    }
                }
            }

            // Historical chart
            if (state.history.isNotEmpty()) {
                item { DriftHistoryChart(state.history) }
            }
        }
    }
}

@Composable
private fun TodayDriftCard(today: DriftScore) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BackgroundElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Today's Drift", color = TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${today.compositeDrift.toInt()}",
                    color = if (today.compositeDrift > 60) Warning else BrandPrimary,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(" / 100", color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(4.dp))
            // Drift bar
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BackgroundElevated)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(today.compositeDrift / 100f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(BrandPrimary, if (today.compositeDrift > 60) Warning else BrandSecondary))
                        ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(today.explanation, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            if (today.topContributors.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Top Contributors", color = TextTertiary, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    today.topContributors.take(3).forEach { dim ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandPrimary.copy(alpha = 0.1f),
                        ) {
                            Text(dim.replace("_", " ").replaceFirstChar { it.uppercase() },
                                color = BrandSecondary, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionBreakdownCard(today: DriftScore) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BackgroundElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Dimension Z-Scores", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            today.dimensionZScores
                .entries
                .sortedByDescending { kotlin.math.abs(it.value) }
                .take(6)
                .forEach { (dim, z) ->
                    DimensionRow(dim, z)
                }
        }
    }
}

@Composable
private fun DimensionRow(dimension: String, zScore: Float) {
    val color = when {
        zScore > 2f -> ErrorColor
        zScore > 1f -> Warning
        zScore < -2f -> Info
        zScore < -1f -> BrandSecondary
        else -> Success
    }
    val normalizedWidth = (kotlin.math.abs(zScore) / 3f).coerceIn(0f, 1f)
    val label = dimension.replace("_", " ").replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(120.dp))
        Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(BackgroundElevated)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalizedWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
        Text(
            "${if (zScore > 0) "+" else ""}${String.format("%.2f", zScore)}σ",
            color = color, fontSize = 11.sp, modifier = Modifier.width(52.dp),
        )
    }
}

@Composable
private fun DriftHistoryChart(history: List<DriftHistoryEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(history) {
        modelProducer.runTransaction {
            lineSeries { series(history.map { it.compositeDrift.toDouble() }) }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, BackgroundElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Drift History", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
    }
}

// Extension needed for BoxScope clip
private fun Modifier.clip(shape: RoundedCornerShape) = this.then(Modifier.graphicsLayer {
    clip = true
    this.shape = shape
})

private val Modifier.Companion.graphicsLayer
    get() = Modifier
