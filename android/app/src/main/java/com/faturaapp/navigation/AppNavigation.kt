package com.faturaapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.faturaapp.ui.categorias.CategoriaOverridesScreen
import com.faturaapp.ui.comparacao.ComparacaoScreen
import com.faturaapp.ui.configuracoes.ConfiguracoesScreen
import com.faturaapp.ui.dashboard.DashboardScreen
import com.faturaapp.ui.faturadetalhe.FaturaDetalheScreen
import com.faturaapp.ui.faturasporcartao.FaturasPorCartaoScreen
import com.faturaapp.ui.senhas.SenhasScreen
import com.faturaapp.ui.upload.UploadScreen

private object Rotas {
    const val DASHBOARD = "dashboard"
    const val UPLOAD = "upload"
    const val COMPARACAO = "comparacao"
    const val CONFIGURACOES = "configuracoes"
    const val SENHAS = "senhas"
    const val CATEGORIAS_PERSONALIZADAS = "categorias-personalizadas"
    const val FATURAS_POR_CARTAO = "faturas-por-cartao"
    const val FATURA_DETALHE = "fatura/{faturaId}"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rotas.DASHBOARD) {
        composable(Rotas.DASHBOARD) {
            DashboardScreen(
                onEnviarFatura = { navController.navigate(Rotas.UPLOAD) },
                onComparar = { navController.navigate(Rotas.COMPARACAO) },
                onConfiguracoes = { navController.navigate(Rotas.CONFIGURACOES) },
                onVerPorCartao = { navController.navigate(Rotas.FATURAS_POR_CARTAO) },
            )
        }
        composable(Rotas.FATURAS_POR_CARTAO) {
            FaturasPorCartaoScreen(
                onAbrirFatura = { faturaId -> navController.navigate("fatura/$faturaId") },
            )
        }
        composable(Rotas.UPLOAD) {
            UploadScreen(
                onFaturaEnviada = { navController.popBackStack() }
            )
        }
        composable(Rotas.COMPARACAO) {
            ComparacaoScreen()
        }
        composable(Rotas.CONFIGURACOES) {
            ConfiguracoesScreen(
                onAbrirSenhas = { navController.navigate(Rotas.SENHAS) },
                onAbrirCategoriasPersonalizadas = { navController.navigate(Rotas.CATEGORIAS_PERSONALIZADAS) },
            )
        }
        composable(Rotas.SENHAS) {
            SenhasScreen()
        }
        composable(Rotas.CATEGORIAS_PERSONALIZADAS) {
            CategoriaOverridesScreen()
        }
        composable(
            Rotas.FATURA_DETALHE,
            arguments = listOf(navArgument("faturaId") { type = NavType.LongType }),
        ) {
            FaturaDetalheScreen()
        }
    }
}
