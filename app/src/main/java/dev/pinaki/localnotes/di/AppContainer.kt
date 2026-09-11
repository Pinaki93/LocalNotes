package dev.pinaki.localnotes.di

import android.content.Context
import dev.pinaki.localnotes.data.NotesRepository

interface AppContainer {
    fun notesRepository(): NotesRepository

    companion object {
        fun getInstance(context: Context): AppContainer =
            AppContainerImpl.initialize(context.applicationContext)
    }
}
