package com.example.meetpoint.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.meetpoint.ui.AppViewModel
import com.example.meetpoint.ui.screen.HomeScreen
import com.example.meetpoint.ui.screen.ResultScreen

object Destinations {
    const val HOME = "home"
    const val RESULT = "result"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: AppViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToResult = { navController.navigate(Destinations.RESULT) }
            )
        }
        composable(Destinations.RESULT) {
            ResultScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
