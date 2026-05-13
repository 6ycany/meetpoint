package com.example.meetpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.meetpoint.ui.AppViewModel
import com.example.meetpoint.ui.MemberViewModel
import com.example.meetpoint.ui.navigation.AppNavGraph
import com.example.meetpoint.ui.theme.MeetPointTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeetPointTheme {
                val appViewModel: AppViewModel = hiltViewModel()
                val memberViewModel: MemberViewModel = hiltViewModel()
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    appViewModel = appViewModel,
                    memberViewModel = memberViewModel
                )
            }
        }
    }
}
