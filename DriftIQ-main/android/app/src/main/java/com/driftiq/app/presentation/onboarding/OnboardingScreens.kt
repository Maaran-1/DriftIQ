package com.driftiq.app.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.driftiq.app.Screen
import com.driftiq.app.presentation.theme.*

// ─── Splash ───────────────────────────────────────────────────────────
@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(BrandPrimary.copy(alpha = 0.2f), BackgroundPrimary))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DriftIQ", color = TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Text("Behavioral Intelligence", color = BrandSecondary, fontSize = 14.sp)
        }
    }
}

// ─── Welcome ──────────────────────────────────────────────────────────
@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(60.dp))
        Column {
            Text(
                "Understand Your\nBehavioral Patterns",
                color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "DriftIQ passively monitors your device usage to help you understand how your daily behavioral patterns change over time.",
                color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }

        // Feature bullets
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "🧠" to "Builds your personal Behavioral Digital Twin",
                "📊" to "Detects drift from your normal patterns",
                "💡" to "Generates weekly behavioral insights",
                "🔒" to "Private by design — no content access",
            ).forEach { (icon, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(text, color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { navController.navigate(Screen.Consent.route) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { navController.navigate(Screen.SignIn.route) }) {
                Text("Already have an account? Sign In", color = BrandSecondary)
            }
        }
    }
}

// ─── Consent ──────────────────────────────────────────────────────────
@Composable
fun ConsentScreen(navController: NavController, vm: OnboardingViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary)
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text("What DriftIQ Collects", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("DriftIQ only collects behavioral metadata — never your private content.", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        listOf(
            "📱 App names and daily usage duration",
            "⏰ Session start and end times",
            "🔒 App category (social, productivity, etc.)",
            "😴 Estimated sleep windows from device-off periods",
        ).forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = BackgroundCard,
                border = BorderStroke(1.dp, BackgroundElevated),
            ) {
                Text(item, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Never collect section
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1A0A10), border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🚫 What We Never Collect", color = ErrorColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Message content, passwords, financial data, location, keystrokes, photos, or any personal communications.", color = TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                vm.setConsentGiven()
                navController.navigate(Screen.PermissionUsage.route)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("I Agree & Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Permission: Usage Stats ──────────────────────────────────────────
@Composable
fun PermissionUsageStatsScreen(navController: NavController, vm: OnboardingViewModel = hiltViewModel()) {
    val granted by vm.usagePermissionGranted.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("📱", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("App Usage Access", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "DriftIQ needs access to your app usage data to track behavioral patterns. This is the core of your Behavioral Digital Twin. No app content is ever accessed — only which apps you used and for how long.",
                color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (granted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Success.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("✓ Permission granted", color = Success, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { navController.navigate(Screen.PermissionNotifications.route) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { vm.openUsageStatsSettings() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Grant Access", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { navController.navigate(Screen.PermissionNotifications.route) }) {
                    Text("Skip for Now", color = TextTertiary)
                }
            }
        }
    }
}

// ─── Permission: Notifications ────────────────────────────────────────
@Composable
fun PermissionNotificationsScreen(navController: NavController, vm: OnboardingViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("🔔", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Stay Informed", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Enable notifications to receive weekly insights, drift alerts, and baseline updates. You control what types of notifications you receive in Settings.",
                color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    vm.requestNotificationPermission()
                    navController.navigate(Screen.AccountCreate.route)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Allow Notifications", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { navController.navigate(Screen.AccountCreate.route) }) {
                Text("Skip for Now", color = TextTertiary)
            }
        }
    }
}

// ─── Account Create ───────────────────────────────────────────────────
@Composable
fun AccountCreateScreen(navController: NavController, vm: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val authState by vm.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(Screen.Calibration.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Spacer(Modifier.height(32.dp))
            Text("Create Account", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Start building your Behavioral Digital Twin", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary, unfocusedBorderColor = BackgroundElevated,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = BrandPrimary, unfocusedLabelColor = TextTertiary,
                    cursorColor = BrandPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password (min. 8 characters)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary, unfocusedBorderColor = BackgroundElevated,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = BrandPrimary, unfocusedLabelColor = TextTertiary,
                    cursorColor = BrandPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            if (authState is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text((authState as AuthState.Error).message, color = ErrorColor, fontSize = 13.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.register(email, password) },
                enabled = authState !is AuthState.Loading && email.isNotBlank() && password.length >= 8,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (authState is AuthState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { navController.navigate(Screen.SignIn.route) }) {
                Text("Already have an account? Sign In", color = BrandSecondary)
            }
        }
    }
}

// ─── Sign In ──────────────────────────────────────────────────────────
@Composable
fun SignInScreen(navController: NavController, vm: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by vm.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(Screen.Home.route) { popUpTo(Screen.SignIn.route) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Spacer(Modifier.height(60.dp))
            Text("Welcome Back", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Sign in to your account", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary, unfocusedBorderColor = BackgroundElevated,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = BrandPrimary, unfocusedLabelColor = TextTertiary,
                    cursorColor = BrandPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary, unfocusedBorderColor = BackgroundElevated,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedLabelColor = BrandPrimary, unfocusedLabelColor = TextTertiary,
                    cursorColor = BrandPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            if (authState is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text((authState as AuthState.Error).message, color = ErrorColor, fontSize = 13.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.login(email, password) },
                enabled = authState !is AuthState.Loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (authState is AuthState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { navController.navigate(Screen.AccountCreate.route) }) {
                Text("Create Account", color = BrandSecondary)
            }
        }
    }
}

// ─── Calibration ──────────────────────────────────────────────────────
@Composable
fun CalibrationScreen(navController: NavController, vm: OnboardingViewModel = hiltViewModel()) {
    val twin by vm.twinStatus.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundPrimary).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🧠", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("Building Your Baseline", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "DriftIQ is learning your unique behavioral patterns. This takes 7–14 days of normal device usage.",
            color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { twin?.calibrationProgress ?: 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = BrandPrimary,
            trackColor = BackgroundElevated,
        )
        Spacer(Modifier.height(12.dp))
        Text("${twin?.baselineDays ?: 0} of 14 days complete", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = { navController.navigate(Screen.Home.route) }) {
            Text("View Partial Dashboard", color = BrandSecondary)
        }
    }
}
