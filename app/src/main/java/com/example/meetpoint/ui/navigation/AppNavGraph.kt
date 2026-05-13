package com.example.meetpoint.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.meetpoint.ui.AppViewModel
import com.example.meetpoint.ui.MemberViewModel
import com.example.meetpoint.ui.screen.HomeScreen
import com.example.meetpoint.ui.screen.MemberScreen
import com.example.meetpoint.ui.screen.ResultScreen

object Destinations {
    const val HOME = "home"
    const val RESULT = "result"
    const val MEMBERS = "members"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appViewModel: AppViewModel,
    memberViewModel: MemberViewModel
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // ResultScreen ではボトムナビを非表示
    val showBottomBar = currentRoute != Destinations.RESULT

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Destinations.HOME,
                        onClick = {
                            navController.navigate(Destinations.HOME) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Place, contentDescription = null) },
                        label = { Text("実行") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.MEMBERS,
                        onClick = {
                            navController.navigate(Destinations.MEMBERS) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("メンバー") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = androidx.compose.ui.Modifier.padding(paddingValues = paddingValues)
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    viewModel = appViewModel,
                    onNavigateToResult = { navController.navigate(Destinations.RESULT) }
                )
            }
            composable(Destinations.RESULT) {
                ResultScreen(
                    viewModel = appViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destinations.MEMBERS) {
                MemberScreen(viewModel = memberViewModel)
            }
        }
    }
}
