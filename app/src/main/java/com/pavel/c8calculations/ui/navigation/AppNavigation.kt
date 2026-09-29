package com.pavel.c8calculations.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pavel.c8calculations.ui.dividend.DividendScreen
import com.pavel.c8calculations.ui.home.HomeScreen
import com.pavel.c8calculations.ui.profit.ProfitScreen
import com.pavel.c8calculations.ui.settings.SettingsScreen
import com.pavel.c8calculations.ui.team.TeamRecognitionScreen

private object Routes {
    const val HOME = "home"
    const val PROFIT = "profit"
    const val TEAM = "team"
    const val DIVIDEND = "dividend"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onProfit = { navController.navigate(Routes.PROFIT) }, onTeam = { navController.navigate(Routes.TEAM) }, onDividend = { navController.navigate(Routes.DIVIDEND) }, onSettings = { navController.navigate(Routes.SETTINGS) }) }
        composable(Routes.PROFIT) { ProfitScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.TEAM) { TeamRecognitionScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DIVIDEND) { DividendScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
