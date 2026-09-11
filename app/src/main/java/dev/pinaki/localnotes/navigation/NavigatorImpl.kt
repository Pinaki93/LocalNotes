package dev.pinaki.localnotes.navigation

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator.Extras
import androidx.savedstate.SavedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class NavigatorImpl(
    private val asyncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : Navigator {
    private val mutableCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = COMMAND_BUFFER_CAPACITY)
    override val commands: Flow<NavigationCommand> = mutableCommands

    private suspend fun execute(action: NavigationCommand.Action): Any? {
        // SharedFlow drops values when it has no subscribers. Waiting here makes startup calls safe.
        mutableCommands.subscriptionCount.first { it > 0 }
        val command = NavigationCommand(action)
        mutableCommands.emit(command)
        return command.completion.await().getOrThrow()
    }

    private fun executeAsync(action: NavigationCommand.Action) {
        // Async calls deliberately have no result channel; failures are contained in this job.
        asyncScope.launch { runCatching { execute(action) } }
    }

    override suspend fun navigate(destinationId: Int, args: SavedState?, navOptions: NavOptions?, navigatorExtras: Extras?) {
        execute(NavigationCommand.Action.NavigateToId(destinationId, args, navOptions, navigatorExtras))
    }

    override suspend fun navigate(deepLink: Uri, navOptions: NavOptions?, navigatorExtras: Extras?) {
        execute(NavigationCommand.Action.NavigateToDeepLink(deepLink, navOptions, navigatorExtras))
    }

    override suspend fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?, navigatorExtras: Extras?) {
        execute(NavigationCommand.Action.NavigateToRequest(request, navOptions, navigatorExtras))
    }

    override suspend fun navigate(directions: NavDirections, navOptions: NavOptions?) {
        execute(NavigationCommand.Action.NavigateWithDirections(directions, navOptions, null))
    }

    override suspend fun navigate(directions: NavDirections, navigatorExtras: Extras) {
        execute(NavigationCommand.Action.NavigateWithDirections(directions, null, navigatorExtras))
    }

    override suspend fun navigate(route: String, navOptions: NavOptions?, navigatorExtras: Extras?) {
        execute(NavigationCommand.Action.NavigateToRoute(route, navOptions, navigatorExtras))
    }

    override suspend fun <T : Any> navigate(route: T, navOptions: NavOptions?, navigatorExtras: Extras?) {
        execute(NavigationCommand.Action.NavigateToRoute(route, navOptions, navigatorExtras))
    }

    override suspend fun popBackStack() = execute(NavigationCommand.Action.Pop) as Boolean
    override suspend fun popBackStack(destinationId: Int, inclusive: Boolean, saveState: Boolean) =
        execute(NavigationCommand.Action.PopToId(destinationId, inclusive, saveState)) as Boolean
    override suspend fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean) =
        execute(NavigationCommand.Action.PopToRoute(route, inclusive, saveState)) as Boolean
    override suspend fun <T : Any> popBackStack(route: KClass<T>, inclusive: Boolean, saveState: Boolean) =
        execute(NavigationCommand.Action.PopToRouteClass(route, inclusive, saveState)) as Boolean
    override suspend fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean) =
        execute(NavigationCommand.Action.PopToRouteObject(route, inclusive, saveState)) as Boolean

    override suspend fun clearBackStack(destinationId: Int) = execute(NavigationCommand.Action.ClearId(destinationId)) as Boolean
    override suspend fun clearBackStack(route: String) = execute(NavigationCommand.Action.ClearRoute(route)) as Boolean
    override suspend fun <T : Any> clearBackStack(route: KClass<T>) = execute(NavigationCommand.Action.ClearRouteClass(route)) as Boolean
    override suspend fun <T : Any> clearBackStack(route: T) = execute(NavigationCommand.Action.ClearRouteObject(route)) as Boolean
    override suspend fun navigateUp() = execute(NavigationCommand.Action.NavigateUp) as Boolean

    override fun navigateAsync(destinationId: Int, args: SavedState?, navOptions: NavOptions?, navigatorExtras: Extras?) = executeAsync(NavigationCommand.Action.NavigateToId(destinationId, args, navOptions, navigatorExtras))
    override fun navigateAsync(deepLink: Uri, navOptions: NavOptions?, navigatorExtras: Extras?) = executeAsync(NavigationCommand.Action.NavigateToDeepLink(deepLink, navOptions, navigatorExtras))
    override fun navigateAsync(request: NavDeepLinkRequest, navOptions: NavOptions?, navigatorExtras: Extras?) = executeAsync(NavigationCommand.Action.NavigateToRequest(request, navOptions, navigatorExtras))
    override fun navigateAsync(directions: NavDirections, navOptions: NavOptions?) = executeAsync(NavigationCommand.Action.NavigateWithDirections(directions, navOptions, null))
    override fun navigateAsync(directions: NavDirections, navigatorExtras: Extras) = executeAsync(NavigationCommand.Action.NavigateWithDirections(directions, null, navigatorExtras))
    override fun navigateAsync(route: String, navOptions: NavOptions?, navigatorExtras: Extras?) = executeAsync(NavigationCommand.Action.NavigateToRoute(route, navOptions, navigatorExtras))
    override fun <T : Any> navigateAsync(route: T, navOptions: NavOptions?, navigatorExtras: Extras?) = executeAsync(NavigationCommand.Action.NavigateToRoute(route, navOptions, navigatorExtras))
    override fun popBackStackAsync() = executeAsync(NavigationCommand.Action.Pop)
    override fun popBackStackAsync(destinationId: Int, inclusive: Boolean, saveState: Boolean) = executeAsync(NavigationCommand.Action.PopToId(destinationId, inclusive, saveState))
    override fun popBackStackAsync(route: String, inclusive: Boolean, saveState: Boolean) = executeAsync(NavigationCommand.Action.PopToRoute(route, inclusive, saveState))
    override fun <T : Any> popBackStackAsync(route: KClass<T>, inclusive: Boolean, saveState: Boolean) = executeAsync(NavigationCommand.Action.PopToRouteClass(route, inclusive, saveState))
    override fun <T : Any> popBackStackAsync(route: T, inclusive: Boolean, saveState: Boolean) = executeAsync(NavigationCommand.Action.PopToRouteObject(route, inclusive, saveState))
    override fun clearBackStackAsync(destinationId: Int) = executeAsync(NavigationCommand.Action.ClearId(destinationId))
    override fun clearBackStackAsync(route: String) = executeAsync(NavigationCommand.Action.ClearRoute(route))
    override fun <T : Any> clearBackStackAsync(route: KClass<T>) = executeAsync(NavigationCommand.Action.ClearRouteClass(route))
    override fun <T : Any> clearBackStackAsync(route: T) = executeAsync(NavigationCommand.Action.ClearRouteObject(route))
    override fun navigateUpAsync() = executeAsync(NavigationCommand.Action.NavigateUp)

    private companion object {
        const val COMMAND_BUFFER_CAPACITY = 3
    }
}
