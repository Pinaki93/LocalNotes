package dev.pinaki.localnotes.di

import dev.pinaki.localnotes.LocalNotesApplication
import dev.pinaki.localnotes.data.NotesRepository
import dev.pinaki.localnotes.navigation.Navigator

interface AppContainer {
    fun notesRepository(): NotesRepository
    val navigator: Navigator

    companion object {
        fun getInstance(): AppContainer =
            AppContainerImpl.initialize(LocalNotesApplication.instance)
    }
}
