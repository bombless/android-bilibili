package com.bilibili.compose.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Popular : Screen("popular")
    data object Dynamic : Screen("dynamic")
    data object History : Screen("history")
    data object Profile : Screen("profile")
    data object Login : Screen("login")
    data object VideoDetail : Screen("video/{bvid}") {
        fun createRoute(bvid: String) = "video/$bvid"
    }
}
