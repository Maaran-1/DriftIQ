package com.driftiq.app.presentation.timeline

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.driftiq.app.domain.model.DriftHistoryEntry
import com.driftiq.app.presentation.home.DriftIQBottomNav
import com.driftiq.app.presentation.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    navController: NavController,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Timeline", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
        bottomBar = { DriftIQBottomNav(navController, "timeline") },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Period selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    listOf(7 to "7D", 30 to "30D", 90 to "90D").forEach { (days, label) ->
                        FilterChip(
                            selected = state.selectedDays == days,
                            onClick = { viewModel.load(days) },
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

            if (state.entries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("No timeline data yet.\nKeep using your device to build history.", color = TextTertiary, fontSize = 14.sp)
                    }
                }
            } else {
                // Timeline entries in reverse chronological order
                items(state.entries.reversed()) { entry ->
                    TimelineEntryRow(entry, isLast = entry == state.entries.first())
                }
            }
        }
    }
}

@Composable
private fun TimelineEntryRow(entry: DriftHistoryEntry, isLast: Boolean) {
    val riskCol = riskColor(entry.riskLevel)
    val formatter = DateTimeFormatter.ofPattern("MMM d")

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline line and dot
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(riskCol))
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(52.dp).background(BackgroundElevated))
            }
        }

        Spacer(Modifier.width(12.dp))

        // Entry content
        Card(
            modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            border = BorderStroke(1.dp, riskCol.copy(alpha = 0.15f)),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(entry.date.format(formatter), color = TextSecondary, fontSize = 12.sp)
                    Text(
                        "Drift: ${entry.compositeDrift.toInt()}",
                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskCol.copy(alpha = 0.15f),
                ) {
                    Text(
                        "L${entry.riskLevel}",
                        color = riskCol, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
