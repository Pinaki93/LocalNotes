package dev.pinaki.localnotes.feature.browser

import androidx.lifecycle.viewModelScope
import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.core.CoreViewModel
import dev.pinaki.localnotes.core.SnackbarState
import dev.pinaki.localnotes.core.ToolbarState
import dev.pinaki.localnotes.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class TryOnBrowserUiState(
    val isStarted: Boolean = false,
    val addresses: List<String> = emptyList(),
    val hasPassword: Boolean = false,
)

sealed interface TryOnBrowserIntent {
    data class StartServer(val password: String? = null) : TryOnBrowserIntent
    data class SavePassword(val password: String) : TryOnBrowserIntent
    data object StopServer : TryOnBrowserIntent
    data object NotificationPermissionDenied : TryOnBrowserIntent
}

class TryOnBrowserViewModel(
    private val appContainer: AppContainer = AppContainer.getInstance(),
) : CoreViewModel<TryOnBrowserUiState, TryOnBrowserIntent>(
    initialScreenState = TryOnBrowserUiState(),
    initialCommonState = CoreUiState(toolbarState = ToolbarState(title = "Try on Browser")),
) {
    private val notesServer = appContainer.notesServer
    private val serverStateRepository = appContainer.serverStateRepository

    init {
        updateScreenState { it.copy(hasPassword = serverStateRepository.hasPassword()) }
        viewModelScope.launch {
            notesServer.isRunning.collectLatest { running ->
                updateScreenState { it.copy(isStarted = running) }
            }
        }
        viewModelScope.launch {
            notesServer.addresses.collectLatest { addresses ->
                updateScreenState { it.copy(addresses = addresses) }
            }
        }
    }

    override fun onIntent(intent: TryOnBrowserIntent) {
        when (intent) {
            is TryOnBrowserIntent.StartServer -> startServer(intent.password)
            is TryOnBrowserIntent.SavePassword -> savePassword(intent.password)
            TryOnBrowserIntent.StopServer -> notesServer.requestStop()
            TryOnBrowserIntent.NotificationPermissionDenied -> showSnackbar(
                SnackbarState(
                    "notification-permission-denied",
                    "Notification permission is required to show the running server",
                ),
            )
        }
    }

    override fun onBackPress() = appContainer.navigator.navigateUpAsync()

    private fun startServer(password: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!serverStateRepository.hasPassword()) {
                if (password.isNullOrBlank()) {
                    showSnackbar(SnackbarState("password-required", "Enter a web password first"))
                    return@launch
                }
                serverStateRepository.setPassword(password)
                updateScreenState { it.copy(hasPassword = true) }
            }
            notesServer.requestStart().onFailure {
                showSnackbar(SnackbarState("server-start-failed", "Unable to start web browsing"))
            }
        }
    }

    private fun savePassword(password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            serverStateRepository.setPassword(password)
            updateScreenState { it.copy(hasPassword = true) }
            showSnackbar(SnackbarState("password-saved", "Web password saved"))
        }
    }
}
