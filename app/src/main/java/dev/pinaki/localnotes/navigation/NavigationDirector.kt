package dev.pinaki.localnotes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import dev.pinaki.localnotes.di.AppContainer

@Composable
fun NavigationDirector(navController: NavController) {
    val navigator = remember { AppContainer.getInstance().navigator }

    LaunchedEffect(navController, navigator) {
        navigator.commands.collect { command ->
            command.completion.complete(
                runCatching { command.action.execute(navController) },
            )
        }
    }
}

private fun NavigationCommand.Action.execute(navController: NavController): Any? = when (this) {
    is NavigationCommand.Action.NavigateToId ->
        navController.navigate(destinationId, args, navOptions, navigatorExtras)
    is NavigationCommand.Action.NavigateToDeepLink ->
        navController.navigate(deepLink, navOptions, navigatorExtras)
    is NavigationCommand.Action.NavigateToRequest ->
        navController.navigate(request, navOptions, navigatorExtras)
    is NavigationCommand.Action.NavigateWithDirections -> when {
        navigatorExtras != null -> navController.navigate(directions, navigatorExtras)
        else -> navController.navigate(directions, navOptions)
    }
    is NavigationCommand.Action.NavigateToRoute -> when (route) {
        is String -> navController.navigate(route, navOptions, navigatorExtras)
        else -> navController.navigate(route, navOptions, navigatorExtras)
    }
    NavigationCommand.Action.Pop -> navController.popBackStack()
    is NavigationCommand.Action.PopToId ->
        navController.popBackStack(destinationId, inclusive, saveState)
    is NavigationCommand.Action.PopToRoute ->
        navController.popBackStack(route, inclusive, saveState)
    is NavigationCommand.Action.PopToRouteClass ->
        navController.popBackStack(route, inclusive, saveState)
    is NavigationCommand.Action.PopToRouteObject ->
        navController.popBackStack(route, inclusive, saveState)
    is NavigationCommand.Action.ClearId -> navController.clearBackStack(destinationId)
    is NavigationCommand.Action.ClearRoute -> navController.clearBackStack(route)
    is NavigationCommand.Action.ClearRouteClass -> navController.clearBackStack(route)
    is NavigationCommand.Action.ClearRouteObject -> navController.clearBackStack(route)
    NavigationCommand.Action.NavigateUp -> navController.navigateUp()
}
