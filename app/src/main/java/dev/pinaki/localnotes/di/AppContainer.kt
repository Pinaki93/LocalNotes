package dev.pinaki.localnotes.di

import dev.pinaki.localnotes.LocalNotesApplication
import dev.pinaki.localnotes.data.NotesRepository

interface AppContainer {
    fun notesRepository(): NotesRepository

    companion object {
        fun getInstance(): AppContainer =
            AppContainerImpl.initialize(LocalNotesApplication.instance)
    }
}
