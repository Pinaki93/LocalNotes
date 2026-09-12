package dev.pinaki.localnotes.di

import android.content.Context
import dev.pinaki.localnotes.data.NotesDatabase
import dev.pinaki.localnotes.data.NotesRepository
import dev.pinaki.localnotes.data.ServerStateRepository
import dev.pinaki.localnotes.navigation.Navigator
import dev.pinaki.localnotes.navigation.NavigatorImpl
import dev.pinaki.localnotes.server.NotesController
import dev.pinaki.localnotes.server.NotesHtmlController
import dev.pinaki.localnotes.server.NotesServer
import dev.pinaki.localnotes.server.Server
import dev.pinaki.localnotes.server.PasswordAuthenticator
import kotlinx.serialization.json.Json

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

    override val navigator: Navigator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        NavigatorImpl()
    }

    override val json: Lazy<Json> = lazy { Json { ignoreUnknownKeys = true } }

    override val notesServer: NotesServer by lazy {
        NotesServer(
            context = checkNotNull(applicationContext),
            serverFactory = {
                Server(passwordAuthenticator = PasswordAuthenticator(serverStateRepository::verifyPassword))
            },
            restControllers = listOf(NotesController(this)),
            htmlCrudControllers = listOf(NotesHtmlController()),
        )
    }

    override val serverStateRepository: ServerStateRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ServerStateRepository(checkNotNull(applicationContext))
    }
}
