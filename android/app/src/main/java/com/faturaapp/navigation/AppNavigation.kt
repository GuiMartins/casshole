package com.faturaapp.navigation

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
import com.faturaapp.ui.categorias.CategoriaOverridesScreen
import com.faturaapp.ui.comparacao.ComparacaoScreen
import com.faturaapp.ui.components.BarraNavegacaoFlutuante
import com.faturaapp.ui.components.TelaPrincipal
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

private fun telaPrincipalDaRota(rota: String?): TelaPrincipal = when (rota) {
    Rotas.FATURAS_POR_CARTAO -> TelaPrincipal.FATURAS_POR_CARTAO
    Rotas.COMPARACAO -> TelaPrincipal.COMPARAR
    Rotas.CONFIGURACOES, Rotas.SENHAS, Rotas.CATEGORIAS_PERSONALIZADAS -> TelaPrincipal.CONFIGURACOES
    else -> TelaPrincipal.INICIO
}

private fun NavHostController.navegarParaAba(rota: String) {
    // Sem saveState/restoreState de propósito: tocar numa aba deve sempre levar
    // pra raiz dela, nunca reaparecer numa tela filha (ex: Senhas) em que o
    // usuário tenha ficado antes de trocar de aba.
    navigate(rota) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val telaAtual = telaPrincipalDaRota(backStackEntry?.destination?.route)

    Scaffold(
        floatingActionButton = {
            BarraNavegacaoFlutuante(
                telaAtual = telaAtual,
                onIrParaInicio = { navController.navegarParaAba(Rotas.DASHBOARD) },
                onVerPorCartao = { navController.navegarParaAba(Rotas.FATURAS_POR_CARTAO) },
                onComparar = { navController.navegarParaAba(Rotas.COMPARACAO) },
                onConfiguracoes = { navController.navegarParaAba(Rotas.CONFIGURACOES) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            NavHost(navController = navController, startDestination = Rotas.DASHBOARD) {
                composable(Rotas.DASHBOARD) {
                    DashboardScreen(
                        onEnviarFatura = { navController.navigate(Rotas.UPLOAD) },
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
    }
}
