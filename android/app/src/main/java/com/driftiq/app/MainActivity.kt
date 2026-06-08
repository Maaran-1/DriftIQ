package com.driftiq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.driftiq.app.data.local.datastore.UserPreferencesDataStore
import com.driftiq.app.presentation.theme.DriftIQTheme
import com.driftiq.app.presentation.onboarding.*
import com.driftiq.app.presentation.home.HomeScreen
import com.driftiq.app.presentation.drift.DriftDetailScreen
import com.driftiq.app.presentation.timeline.TimelineScreen
import com.driftiq.app.presentation.reports.weekly.WeeklyDashboardScreen
import com.driftiq.app.presentation.reports.monthly.MonthlyDashboardScreen
import com.driftiq.app.presentation.risk.RiskTrendScreen
import com.driftiq.app.presentation.settings.SettingsScreen
import com.driftiq.app.presentation.insight.InsightDetailScreen
import com.driftiq.app.service.CollectionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Consent : Screen("consent")
    object PermissionUsage : Screen("permission_usage")
    object PermissionNotifications : Screen("permission_notifications")
    object AccountCreate : Screen("account_create")
    object SignIn : Screen("sign_in")
    object Calibration : Screen("calibration")
    object Home : Screen("home")
    object DriftDetail : Screen("drift_detail")
    object Timeline : Screen("timeline")
    object Weekly : Screen("weekly")
    object Monthly : Screen("monthly")
    object Risk : Screen("risk")
    object Settings : Screen("settings")
    object InsightDetail : Screen("insight_detail/{insightId}") {
        fun route(id: String) = "insight_detail/$id"
    }
    object Sleep : Screen("sleep_trends")
    object Activity : Screen("activity_trends")
    object Productivity : Screen("productivity_trends")
    object Learning : Screen("learning_trends")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dataStore: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start collection service
        CollectionService.start(this)

        val isLoggedIn = runBlocking { dataStore.accessToken.first() != null }
        val isOnboarded = runBlocking { dataStore.isOnboardingComplete.first() }
        val startDest = when {
            isLoggedIn -> Screen.Home.route
            isOnboarded -> Screen.SignIn.route
            else -> Screen.Splash.route
        }

        setContent {
            DriftIQTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = startDest) {
                        composable(Screen.Splash.route) { SplashScreen(navController) }
                        composable(Screen.Welcome.route) { WelcomeScreen(navController) }
                        composable(Screen.Consent.route) { ConsentScreen(navController) }
                        composable(Screen.PermissionUsage.route) { PermissionUsageStatsScreen(navController) }
                        composable(Screen.PermissionNotifications.route) { PermissionNotificationsScreen(navController) }
                        composable(Screen.AccountCreate.route) { AccountCreateScreen(navController) }
                        composable(Screen.SignIn.route) { SignInScreen(navController) }
                        composable(Screen.Calibration.route) { CalibrationScreen(navController) }
                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(Screen.DriftDetail.route) { DriftDetailScreen(navController) }
                        composable(Screen.Timeline.route) { TimelineScreen(navController) }
                        composable(Screen.Weekly.route) { WeeklyDashboardScreen(navController) }
                        composable(Screen.Monthly.route) { MonthlyDashboardScreen(navController) }
                        composable(Screen.Risk.route) { RiskTrendScreen(navController) }
                        composable(Screen.Settings.route) { SettingsScreen(navController) }
                        composable("insight_detail/{insightId}") { InsightDetailScreen(navController) }
                        composable(Screen.Sleep.route) {
                            com.driftiq.app.presentation.reports.sleep.SleepTrendsScreen(navController)
                        }
                        composable(Screen.Activity.route) {
                            com.driftiq.app.presentation.reports.activity.ActivityTrendsScreen(navController)
                        }
                        composable(Screen.Productivity.route) {
                            com.driftiq.app.presentation.reports.productivity.ProductivityScreen(navController)
                        }
                        composable(Screen.Learning.route) {
                            com.driftiq.app.presentation.reports.learning.LearningScreen(navController)
                        }
                    }
                }
            }
        }
    }
}
