package dev.pinaki.localnotes.navigation

import android.net.Uri
import androidx.annotation.IdRes
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator.Extras
import androidx.savedstate.SavedState
import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

data class NavigationCommand(
    val action: Action,
    internal val completion: CompletableDeferred<Result<Any?>> = CompletableDeferred(),
) {
    sealed interface Action {
        data class NavigateToId(
            @param:IdRes val destinationId: Int,
            val args: SavedState?,
            val navOptions: NavOptions?,
            val navigatorExtras: Extras?,
        ) : Action

        data class NavigateToDeepLink(val deepLink: Uri, val navOptions: NavOptions?, val navigatorExtras: Extras?) : Action
        data class NavigateToRequest(val request: NavDeepLinkRequest, val navOptions: NavOptions?, val navigatorExtras: Extras?) : Action
        data class NavigateWithDirections(val directions: NavDirections, val navOptions: NavOptions?, val navigatorExtras: Extras?) : Action
        data class NavigateToRoute(val route: Any, val navOptions: NavOptions?, val navigatorExtras: Extras?) : Action

        data object Pop : Action
        data class PopToId(@param:IdRes val destinationId: Int, val inclusive: Boolean, val saveState: Boolean) : Action
        data class PopToRoute(val route: String, val inclusive: Boolean, val saveState: Boolean) : Action
        data class PopToRouteClass(val route: KClass<out Any>, val inclusive: Boolean, val saveState: Boolean) : Action
        data class PopToRouteObject(val route: Any, val inclusive: Boolean, val saveState: Boolean) : Action

        data class ClearId(@param:IdRes val destinationId: Int) : Action
        data class ClearRoute(val route: String) : Action
        data class ClearRouteClass(val route: KClass<out Any>) : Action
        data class ClearRouteObject(val route: Any) : Action
        data object NavigateUp : Action
    }
}
