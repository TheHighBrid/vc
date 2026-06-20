package com.melato.shop.ui.nav

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Shop : Screen("shop")
    object ProductDetail : Screen("product/{productId}") {
        fun route(id: String) = "product/$id"
    }
    object Cart : Screen("cart")
    object Profile : Screen("profile")
}
