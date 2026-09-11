package dev.pinaki.localnotes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.pinaki.localnotes.feature.list.NoteListDestination

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
    }
}
