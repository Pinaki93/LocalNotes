package dev.pinaki.localnotes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.pinaki.localnotes.feature.addedit.AddEditDestination
import dev.pinaki.localnotes.feature.list.NoteListDestination
import dev.pinaki.localnotes.feature.browser.TryOnBrowserDestination

private const val NOTE_LIST_ROUTE = "notes"

@Composable
fun LocalNotesNavGraph() {
    val navController = rememberNavController()

    NavigationDirector(navController)

    NavHost(
        navController = navController,
        startDestination = NOTE_LIST_ROUTE,
    ) {
        composable(NOTE_LIST_ROUTE) {
            NoteListDestination()
        }
        composable("browser") {
            TryOnBrowserDestination()
        }
        composable(
            route = AddEditDestination.ROUTE,
            arguments = listOf(
                navArgument(AddEditDestination.NOTE_ID_ARGUMENT) { type = NavType.IntType },
            ),
        ) { entry ->
            AddEditDestination(
                noteId = entry.arguments?.getInt(AddEditDestination.NOTE_ID_ARGUMENT) ?: 0,
            )
        }
    }
}
