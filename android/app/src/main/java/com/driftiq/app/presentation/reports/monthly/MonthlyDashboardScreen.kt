package com.driftiq.app.presentation.reports.monthly

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.driftiq.app.domain.model.Insight
import com.driftiq.app.domain.usecase.GetMonthlyInsightUseCase
import com.driftiq.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter

@HiltViewModel
class MonthlyViewModel @Inject constructor(
    private val getMonthlyInsight: GetMonthlyInsightUseCase,
) : ViewModel() {
    private val _insight = MutableStateFlow<Insight?>(null)
    val insight: StateFlow<Insight?> = _insight
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            getMonthlyInsight().onSuccess { _insight.value = it }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyDashboardScreen(
    navController: NavController,
    viewModel: MonthlyViewModel = hiltViewModel(),
) {
    val insight by viewModel.insight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Monthly Report", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
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
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            insight?.let { i ->
                // Wellness score hero
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "${i.periodStart.format(fmt)} – ${i.periodEnd.format(fmt)}",
                            color = TextTertiary, fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("${i.wellnessScore}", color = BrandPrimary, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Monthly Wellness Score", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { i.wellnessScore / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = BrandPrimary,
                            trackColor = BackgroundElevated,
                        )
                    }
                }

                // Monthly insight text
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BackgroundElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Monthly Behavioral Summary", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(i.content, color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            } ?: run {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Monthly report available after 30 days", color = TextTertiary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
