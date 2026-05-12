package lol.hanyuu.iccardscanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import lol.hanyuu.iccardscanner.ScanState
import lol.hanyuu.iccardscanner.ui.detail.CardDetailScreen
import lol.hanyuu.iccardscanner.ui.history.HistoryScreen
import lol.hanyuu.iccardscanner.ui.home.HomeScreen
import lol.hanyuu.iccardscanner.ui.settings.SettingsScreen
import lol.hanyuu.iccardscanner.ui.summary.MonthlySummaryScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history/{cardIdm}") {
        fun createRoute(cardIdm: String) = "history/$cardIdm"
    }
    data object CardDetail : Screen("detail/{cardIdm}") {
        fun createRoute(cardIdm: String) = "detail/$cardIdm"
    }
    data object Summary : Screen("summary/{cardIdm}") {
        fun createRoute(cardIdm: String) = "summary/$cardIdm"
    }
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    scanState: ScanState,
    onScanStateReset: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                scanState = scanState,
                onScanStateReset = onScanStateReset,
                onNavigateToHistory = { idm -> navController.navigate(Screen.History.createRoute(idm)) },
                onNavigateToDetail = { idm -> navController.navigate(Screen.CardDetail.createRoute(idm)) },
                onNavigateToSummary = { idm -> navController.navigate(Screen.Summary.createRoute(idm)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.History.route,
            arguments = listOf(navArgument("cardIdm") { type = NavType.StringType })
        ) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardIdm") { type = NavType.StringType })
        ) {
            CardDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("cardIdm") { type = NavType.StringType })
        ) {
            MonthlySummaryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
