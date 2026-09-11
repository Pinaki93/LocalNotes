package dev.pinaki.localnotes.navigation

import android.net.Uri
import androidx.annotation.IdRes
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator.Extras
import androidx.navigation.navOptions
import androidx.savedstate.SavedState
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/** App-facing, lifecycle-independent subset of the command API exposed by NavController. */
interface Navigator {
    /** Commands are intentionally exposed as a Flow so only the UI layer knows about NavController. */
    val commands: Flow<NavigationCommand>

    suspend fun navigate(
        @IdRes destinationId: Int,
        args: SavedState? = null,
        navOptions: NavOptions? = null,
        navigatorExtras: Extras? = null,
    )

    suspend fun navigate(
        deepLink: Uri,
        navOptions: NavOptions? = null,
        navigatorExtras: Extras? = null,
    )

    suspend fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions? = null,
        navigatorExtras: Extras? = null,
    )

    suspend fun navigate(directions: NavDirections, navOptions: NavOptions? = null)
    suspend fun navigate(directions: NavDirections, navigatorExtras: Extras)

    suspend fun navigate(
        route: String,
        navOptions: NavOptions? = null,
        navigatorExtras: Extras? = null,
    )

    suspend fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) =
        navigate(route, navOptions(builder))

    suspend fun <T : Any> navigate(
        route: T,
        navOptions: NavOptions? = null,
        navigatorExtras: Extras? = null,
    )

    suspend fun <T : Any> navigate(route: T, builder: NavOptionsBuilder.() -> Unit) =
        navigate(route, navOptions(builder))

    suspend fun popBackStack(): Boolean
    suspend fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean, saveState: Boolean = false): Boolean
    suspend fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean = false): Boolean
    suspend fun <T : Any> popBackStack(route: KClass<T>, inclusive: Boolean, saveState: Boolean = false): Boolean
    suspend fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean = false): Boolean

    suspend fun clearBackStack(@IdRes destinationId: Int): Boolean
    suspend fun clearBackStack(route: String): Boolean
    suspend fun <T : Any> clearBackStack(route: KClass<T>): Boolean
    suspend fun <T : Any> clearBackStack(route: T): Boolean
    suspend fun navigateUp(): Boolean

    fun navigateAsync(@IdRes destinationId: Int, args: SavedState? = null, navOptions: NavOptions? = null, navigatorExtras: Extras? = null)
    fun navigateAsync(deepLink: Uri, navOptions: NavOptions? = null, navigatorExtras: Extras? = null)
    fun navigateAsync(request: NavDeepLinkRequest, navOptions: NavOptions? = null, navigatorExtras: Extras? = null)
    fun navigateAsync(directions: NavDirections, navOptions: NavOptions? = null)
    fun navigateAsync(directions: NavDirections, navigatorExtras: Extras)
    fun navigateAsync(route: String, navOptions: NavOptions? = null, navigatorExtras: Extras? = null)
    fun navigateAsync(route: String, builder: NavOptionsBuilder.() -> Unit) =
        navigateAsync(route, navOptions(builder))
    fun <T : Any> navigateAsync(route: T, navOptions: NavOptions? = null, navigatorExtras: Extras? = null)
    fun <T : Any> navigateAsync(route: T, builder: NavOptionsBuilder.() -> Unit) =
        navigateAsync(route, navOptions(builder))
    fun popBackStackAsync()
    fun popBackStackAsync(@IdRes destinationId: Int, inclusive: Boolean, saveState: Boolean = false)
    fun popBackStackAsync(route: String, inclusive: Boolean, saveState: Boolean = false)
    fun <T : Any> popBackStackAsync(route: KClass<T>, inclusive: Boolean, saveState: Boolean = false)
    fun <T : Any> popBackStackAsync(route: T, inclusive: Boolean, saveState: Boolean = false)
    fun clearBackStackAsync(@IdRes destinationId: Int)
    fun clearBackStackAsync(route: String)
    fun <T : Any> clearBackStackAsync(route: KClass<T>)
    fun <T : Any> clearBackStackAsync(route: T)
    fun navigateUpAsync()
}
