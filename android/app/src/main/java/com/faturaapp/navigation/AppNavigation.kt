package com.faturaapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.faturaapp.ui.dashboard.DashboardScreen
import com.faturaapp.ui.setup.SetupScreen
import com.faturaapp.ui.upload.UploadScreen

private object Rotas {
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
    const val UPLOAD = "upload"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rotas.SETUP) {
        composable(Rotas.SETUP) {
            SetupScreen(
                onSetupConcluido = {
                    navController.navigate(Rotas.DASHBOARD) {
                        popUpTo(Rotas.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Rotas.DASHBOARD) {
            DashboardScreen(
                onEnviarFatura = { navController.navigate(Rotas.UPLOAD) }
            )
        }
        composable(Rotas.UPLOAD) {
            UploadScreen(
                onFaturaEnviada = { navController.popBackStack() }
            )
        }
    }
}
