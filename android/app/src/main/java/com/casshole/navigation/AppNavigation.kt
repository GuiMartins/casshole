package com.casshole.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.casshole.ui.categories.CategoryOverridesScreen
import com.casshole.ui.comparison.ComparisonScreen
import com.casshole.ui.components.FloatingNavigationBar
import com.casshole.ui.components.MainScreen
import com.casshole.ui.email.EmailSettingsScreen
import com.casshole.ui.settings.SettingsScreen
import com.casshole.ui.dashboard.DashboardScreen
import com.casshole.ui.invoicedetail.InvoiceDetailScreen
import com.casshole.ui.invoicesbycard.InvoicesByCardScreen
import com.casshole.ui.passwords.PasswordsScreen
import com.casshole.ui.upload.UploadScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val UPLOAD = "upload"
    const val COMPARISON = "comparison"
    const val SETTINGS = "settings"
    const val PASSWORDS = "passwords"
    const val CATEGORY_OVERRIDES = "category-overrides"
    const val EMAIL_SETTINGS = "email-settings"
    const val INVOICES_BY_CARD = "invoices-by-card"
    const val INVOICE_DETAIL = "invoice/{invoiceId}"
}

private fun mainScreenForRoute(route: String?): MainScreen = when (route) {
    Routes.INVOICES_BY_CARD -> MainScreen.INVOICES_BY_CARD
    Routes.COMPARISON -> MainScreen.COMPARE
    Routes.SETTINGS, Routes.PASSWORDS, Routes.CATEGORY_OVERRIDES, Routes.EMAIL_SETTINGS -> MainScreen.SETTINGS
    else -> MainScreen.HOME
}

private fun NavHostController.navigateToTab(route: String) {
    // No saveState/restoreState on purpose: tapping a tab should always land
    // on its root, never resurface a child screen (e.g. Passwords) the user
    // happened to leave open before switching tabs.
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = mainScreenForRoute(backStackEntry?.destination?.route)

    Scaffold(
        floatingActionButton = {
            FloatingNavigationBar(
                currentScreen = currentScreen,
                onGoHome = { navController.navigateToTab(Routes.DASHBOARD) },
                onViewByCard = { navController.navigateToTab(Routes.INVOICES_BY_CARD) },
                onCompare = { navController.navigateToTab(Routes.COMPARISON) },
                onSettings = { navController.navigateToTab(Routes.SETTINGS) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onSendInvoice = { navController.navigate(Routes.UPLOAD) },
                    )
                }
                composable(Routes.INVOICES_BY_CARD) {
                    InvoicesByCardScreen(
                        onOpenInvoice = { invoiceId -> navController.navigate("invoice/$invoiceId") },
                    )
                }
                composable(Routes.UPLOAD) {
                    UploadScreen(
                        onInvoiceSent = { navController.popBackStack() }
                    )
                }
                composable(Routes.COMPARISON) {
                    ComparisonScreen()
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onOpenPasswords = { navController.navigate(Routes.PASSWORDS) },
                        onOpenCategoryOverrides = { navController.navigate(Routes.CATEGORY_OVERRIDES) },
                        onOpenEmailSettings = { navController.navigate(Routes.EMAIL_SETTINGS) },
                    )
                }
                composable(Routes.PASSWORDS) {
                    PasswordsScreen()
                }
                composable(Routes.CATEGORY_OVERRIDES) {
                    CategoryOverridesScreen()
                }
                composable(Routes.EMAIL_SETTINGS) {
                    EmailSettingsScreen()
                }
                composable(
                    Routes.INVOICE_DETAIL,
                    arguments = listOf(navArgument("invoiceId") { type = NavType.LongType }),
                ) {
                    InvoiceDetailScreen()
                }
            }
        }
    }
}
