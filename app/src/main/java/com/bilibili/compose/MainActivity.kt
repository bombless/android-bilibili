package com.bilibili.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bilibili.compose.ui.home.HomeScreen
import com.bilibili.compose.ui.home.HomeViewModel
import com.bilibili.compose.ui.login.LoginScreen
import com.bilibili.compose.ui.login.LoginViewModel
import com.bilibili.compose.ui.navigation.Screen
import com.bilibili.compose.ui.player.PlayerViewModel
import com.bilibili.compose.ui.player.VideoDetailScreen
import com.bilibili.compose.ui.profile.ProfileScreen
import com.bilibili.compose.ui.profile.ProfileViewModel
import com.bilibili.compose.ui.theme.BilibiliTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BilibiliTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiliNavHost()
                }
            }
        }
    }
}

@Composable
fun BiliNavHost() {
    val navController = rememberNavController()
    val app = BiliApp.instance

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel {
                HomeViewModel(app.videoRepository, app.authRepository)
            }
            HomeScreen(
                viewModel = homeViewModel,
                onVideoClick = { bvid ->
                    navController.navigate(Screen.VideoDetail.createRoute(bvid))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        // Video Detail & Playback
        composable(
            route = Screen.VideoDetail.route,
            arguments = listOf(navArgument("bvid") { type = NavType.StringType })
        ) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            val playerViewModel: PlayerViewModel = viewModel {
                PlayerViewModel(app.videoRepository)
            }
            VideoDetailScreen(
                bvid = bvid,
                viewModel = playerViewModel,
                onBackClick = { navController.popBackStack() },
                onRelatedVideoClick = { nextBvid ->
                    navController.navigate(Screen.VideoDetail.createRoute(nextBvid)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Profile
        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel {
                ProfileViewModel(app.authRepository)
            }
            ProfileScreen(
                viewModel = profileViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        // Login
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel {
                LoginViewModel(app.authRepository)
            }
            LoginScreen(
                viewModel = loginViewModel,
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
