package com.driftiq.app.presentation.insight

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.driftiq.app.domain.model.Insight
import com.driftiq.app.domain.usecase.GetDailyInsightUseCase
import com.driftiq.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class InsightDetailViewModel @Inject constructor(
    private val getDailyInsight: GetDailyInsightUseCase,
) : ViewModel() {
    private val _insight = MutableStateFlow<Insight?>(null)
    val insight: StateFlow<Insight?> = _insight
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            getDailyInsight().onSuccess { _insight.value = it }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightDetailScreen(
    navController: NavController,
    viewModel: InsightDetailViewModel = hiltViewModel(),
) {
    val insight by viewModel.insight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Daily Insight", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
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
                val fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d")
                val riskCol = riskColor(i.riskLevel)

                // Header card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(i.periodStart.format(fmt), color = TextTertiary, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${i.wellnessScore}", color = BrandPrimary, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                            Text(" wellness", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = riskCol.copy(alpha = 0.15f),
                        ) {
                            Text("Level ${i.riskLevel}", color = riskCol, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }

                // Insight content
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                    border = BorderStroke(1.dp, BackgroundElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Today's Analysis", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(i.content, color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            } ?: run {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Text("💡\n\nInsight will be available after your baseline is established.", color = TextTertiary, fontSize = 14.sp)
                }
            }
        }
    }
}
