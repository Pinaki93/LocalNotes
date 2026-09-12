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
)

sealed interface TryOnBrowserIntent {
    data object ToggleServer : TryOnBrowserIntent
    data object NotificationPermissionDenied : TryOnBrowserIntent
}

class TryOnBrowserViewModel(
    private val appContainer: AppContainer = AppContainer.getInstance(),
) : CoreViewModel<TryOnBrowserUiState, TryOnBrowserIntent>(
    initialScreenState = TryOnBrowserUiState(),
    initialCommonState = CoreUiState(toolbarState = ToolbarState(title = "Try on Browser")),
) {
    private val notesServer = appContainer.notesServer

    init {
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
            TryOnBrowserIntent.ToggleServer -> toggleServer()
            TryOnBrowserIntent.NotificationPermissionDenied -> showSnackbar(
                SnackbarState(
                    "notification-permission-denied",
                    "Notification permission is required to show the running server",
                ),
            )
        }
    }

    override fun onBackPress() = appContainer.navigator.navigateUpAsync()

    private fun toggleServer() {
        viewModelScope.launch(Dispatchers.IO) {
            if (notesServer.isRunning.value) {
                notesServer.requestStop()
            } else {
                notesServer.requestStart().onFailure {
                    showSnackbar(SnackbarState("server-start-failed", "Unable to start web browsing"))
                }
            }
        }
    }
}
