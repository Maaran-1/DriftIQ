package com.driftiq.app.presentation.reports.sleep

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
import androidx.navigation.NavController
import com.driftiq.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrendsScreen(navController: NavController) {
    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Sleep Trends", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            SleepInsightCard()
        }
    }
}

@Composable
private fun SleepInsightCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        border = BorderStroke(1.dp, DimSleep.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("😴 Sleep Estimates", color = DimSleep, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sleep estimates are derived from device-off periods. DriftIQ tracks when your phone is unused overnight to estimate sleep duration. No microphone or sensor data is used.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text("Methodology Note", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Screen-off windows longer than 3 hours between 8 PM and 10 AM are counted as potential sleep periods.",
                color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }
}
