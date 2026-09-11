package dev.pinaki.localnotes.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Common user actions generated from the CommonScaffold
sealed interface CommonIntent {
    data object OnToolbarBackPressed : CommonIntent
    data class OnAlertPositiveClick(val dialogId: String) : CommonIntent
    data class OnAlertNegativeClick(val dialogId: String) : CommonIntent
    data class OnAlertDismiss(val dialogId: String) : CommonIntent
}

// Wrapper combining Screen-specific State with the Scaffold's Common State
data class ScreenState<S>(
    val commonState: CoreUiState = CoreUiState(),
    val screenSpecificState: S
)

abstract class CoreViewModel<S, I>(
    initialScreenState: S,
    initialCommonState: CoreUiState = CoreUiState()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScreenState(
            commonState = initialCommonState,
            screenSpecificState = initialScreenState
        )
    )
    val uiState: StateFlow<ScreenState<S>> = _uiState.asStateFlow()

    protected val currentScreenState: S
        get() = _uiState.value.screenSpecificState

    protected val currentCommonState: CoreUiState
        get() = _uiState.value.commonState

    abstract fun onIntent(intent: I)

    fun onCommonIntent(intent: CommonIntent) {
        when (intent) {
            is CommonIntent.OnToolbarBackPressed -> onBackPress()
            is CommonIntent.OnAlertPositiveClick -> onPositiveButtonClicked(intent.dialogId)
            is CommonIntent.OnAlertNegativeClick -> onNegativeButtonClicked(intent.dialogId)
            is CommonIntent.OnAlertDismiss -> onAlertDialogDismissed(intent.dialogId)
        }
    }

    // -------------------------------------------------------------
    // Overridable Hooks for Common Intents
    // -------------------------------------------------------------

    open fun onBackPress() {}

    open fun onPositiveButtonClicked(dialogId: String) {
        hideAlertDialog()
    }

    open fun onNegativeButtonClicked(dialogId: String) {
        hideAlertDialog()
    }

    open fun onAlertDialogDismissed(dialogId: String) {
        hideAlertDialog()
    }

    // -------------------------------------------------------------
    // State Reducer Helpers
    // -------------------------------------------------------------

    protected fun updateScreenState(reducer: (S) -> S) {
        _uiState.update { it.copy(screenSpecificState = reducer(it.screenSpecificState)) }
    }

    protected fun updateCommonState(reducer: (CoreUiState) -> CoreUiState) {
        _uiState.update { it.copy(commonState = reducer(it.commonState)) }
    }

    // -------------------------------------------------------------
    // Common Scaffolding Utility Functions
    // -------------------------------------------------------------

    fun showLoader() {
        updateCommonState { it.copy(isLoading = true) }
    }

    fun hideLoader() {
        updateCommonState { it.copy(isLoading = false) }
    }

    fun showAlertDialog(dialogState: AlertDialogState) {
        updateCommonState { it.copy(alertDialogState = dialogState) }
    }

    fun hideAlertDialog() {
        updateCommonState { it.copy(alertDialogState = null) }
    }

    fun updateToolbar(toolbarState: ToolbarState?) {
        updateCommonState { it.copy(toolbarState = toolbarState) }
    }

    fun updateToolbarTitle(title: String) {
        updateCommonState { current ->
            current.copy(
                toolbarState = current.toolbarState?.copy(title = title) ?: ToolbarState(title = title)
            )
        }
    }
}
