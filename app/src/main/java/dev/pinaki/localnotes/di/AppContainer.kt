package dev.pinaki.localnotes.di

import dev.pinaki.localnotes.LocalNotesApplication
import dev.pinaki.localnotes.data.NotesRepository
import dev.pinaki.localnotes.navigation.Navigator
import dev.pinaki.localnotes.server.NotesServer
import kotlinx.serialization.json.Json

interface AppContainer {
    fun notesRepository(): NotesRepository
    val navigator: Navigator
    val json: Lazy<Json>
    val notesServer: NotesServer

    companion object {
        fun getInstance(): AppContainer =
            AppContainerImpl.initialize(LocalNotesApplication.instance)
    }
}
