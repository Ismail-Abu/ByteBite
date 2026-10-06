package com.example.guione.meal.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.guione.meal.AppGraph
import com.example.guione.meal.ThemeMode
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.guione.ui.theme.ByteBiteTheme

/** Route names and builders for the app's navigation graph. */
object Routes {
    const val TODAY = "today"
    const val HISTORY = "history"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val ADD = "add"
    const val EDIT = "edit/{mealId}"
    const val DETAIL = "detail/{mealId}"
    const val CAPTURE = "capture"
    const val REVIEW = "review?scan=true"
    fun edit(mealId: String) = "edit/$mealId"
    fun detail(mealId: String) = "detail/$mealId"
}

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDest(Routes.TODAY, "Today", Icons.Outlined.Today),
    TopDest(Routes.HISTORY, "History", Icons.AutoMirrored.Outlined.ListAlt),
    TopDest(Routes.INSIGHTS, "Insights", Icons.AutoMirrored.Outlined.ShowChart),
)

@Composable
fun ByteBiteApp() {
    val context = LocalContext.current
    val settings = remember(context) { AppGraph.settings(context) }
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    ByteBiteTheme(darkTheme = darkTheme) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.TODAY) {
            composable(Routes.TODAY) {
                TodayScreen(
                    onAddMeal = { nav.navigate(Routes.ADD) },
                    onScan = { nav.navigate(Routes.CAPTURE) },
                    onOpenMeal = { nav.navigate(Routes.detail(it)) },
                    onOpenHistory = { nav.navigateTopLevel(Routes.HISTORY) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    bottomBar = { ByteBiteBottomBar(nav) },
                )
            }
            composable(Routes.CAPTURE) {
                CaptureScreen(
                    onBack = { nav.popBackStack() },
                    onReviewReady = { nav.navigate(Routes.REVIEW) },
                )
            }
            composable(
                Routes.REVIEW,
                arguments = listOf(navArgument("scan") { type = NavType.BoolType; defaultValue = true }),
            ) {
                AddEditMealScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack(Routes.TODAY, inclusive = false) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenMeal = { nav.navigate(Routes.detail(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    bottomBar = { ByteBiteBottomBar(nav) },
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    bottomBar = { ByteBiteBottomBar(nav) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.ADD) {
                AddEditMealScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("mealId") { type = NavType.StringType }),
            ) {
                AddEditMealScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("mealId") { type = NavType.StringType }),
            ) { entry ->
                val mealId = entry.arguments?.getString("mealId").orEmpty()
                MealDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(mealId)) },
                    onDeleted = { nav.popBackStack() },
                )
            }
        }
    }
}

/** Navigate to a top-level tab with single-top + state save/restore. */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun ByteBiteBottomBar(nav: NavHostController) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        topDestinations.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = { if (currentRoute != dest.route) nav.navigateTopLevel(dest.route) },
                icon = { Icon(dest.icon, contentDescription = null) },
                label = { Text(dest.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
