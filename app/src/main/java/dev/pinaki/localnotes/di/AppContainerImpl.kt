package dev.pinaki.localnotes.di

import android.content.Context
import dev.pinaki.localnotes.data.NotesDatabase
import dev.pinaki.localnotes.data.NotesRepository

internal object AppContainerImpl : AppContainer {
    @Volatile
    private var applicationContext: Context? = null

    private val database: NotesDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        NotesDatabase.getInstance(
            checkNotNull(applicationContext) { "AppContainer has not been initialized" },
        )
    }

    fun initialize(context: Context): AppContainerImpl {
        if (applicationContext == null) {
            synchronized(this) {
                if (applicationContext == null) {
                    applicationContext = context.applicationContext
                }
            }
        }
        return this
    }

    override fun notesRepository(): NotesRepository = NotesRepository(database)
}
