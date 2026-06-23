package com.hitlist.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hitlist.detail.presentation.DetailScreen
import com.hitlist.di.HitListDependencyInjector
import com.hitlist.news.presentation.NewsScreen
import com.hitlist.ranking.presentation.RankingScreen

private const val ROUTE_RANKING = "ranking"
private const val ROUTE_DETAIL = "detail/{appId}/{name}"
private const val ROUTE_NEWS = "news?query={query}&appId={appId}"

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_RANKING) {
        composable(ROUTE_RANKING) {
            RankingScreen(
                viewModel = HitListDependencyInjector.getRankingViewModel(),
                onGameClick = { appId, name ->
                    navController.navigate("detail/$appId/${name.encodeForNav()}")
                },
                onNewsClick = {
                    navController.navigate("news?query=gaming&appId=-1")
                }
            )
        }

        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(
                navArgument("appId") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getInt("appId") ?: return@composable
            val name = backStackEntry.arguments?.getString("name") ?: return@composable
            DetailScreen(
                appId = appId,
                name = name.decodeFromNav(),
                viewModel = HitListDependencyInjector.getDetailViewModel(),
                onBack = { navController.popBackStack() },
                onNewsClick = { gameAppId, gameName ->
                    navController.navigate("news?query=${gameName.encodeForNav()}&appId=$gameAppId")
                }
            )
        }

        composable(
            route = ROUTE_NEWS,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = "gaming"
                },
                navArgument("appId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: "gaming"
            val appId = backStackEntry.arguments?.getInt("appId") ?: -1
            NewsScreen(
                query = query.decodeFromNav(),
                appId = appId.takeIf { it != -1 },
                viewModel = HitListDependencyInjector.getNewsViewModel(),
                isApiKeyConfigured = HitListDependencyInjector.isNewsApiConfigured,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun String.encodeForNav() = replace("/", "%2F").replace(" ", "%20")
private fun String.decodeFromNav() = replace("%2F", "/").replace("%20", " ")
