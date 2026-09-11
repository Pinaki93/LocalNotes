package dev.pinaki.localnotes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.pinaki.localnotes.ui.HelloWorldScreen

private const val HELLO_WORLD_ROUTE = "hello_world"

@Composable
fun LocalNotesNavGraph() {
    val navController = rememberNavController()

    NavigationDirector(navController)

    NavHost(
        navController = navController,
        startDestination = HELLO_WORLD_ROUTE,
    ) {
        composable(HELLO_WORLD_ROUTE) {
            HelloWorldScreen()
        }
    }
}
