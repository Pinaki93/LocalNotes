package dev.pinaki.localnotes.feature.list

import androidx.lifecycle.viewModelScope
import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.core.CoreViewModel
import dev.pinaki.localnotes.core.ToolbarState
import dev.pinaki.localnotes.core.ToolbarAction
import dev.pinaki.localnotes.core.SnackbarState
import dev.pinaki.localnotes.di.AppContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoteListViewModel(
    appContainer: AppContainer = AppContainer.getInstance(),
) : CoreViewModel<NoteListUiState, NoteListIntent>(
    initialScreenState = NoteListUiState(),
    initialCommonState = CoreUiState(
        toolbarState = ToolbarState(
            title = "Local Notes",
            showNavigationIcon = false,
            actions = listOf(
                ToolbarAction(
                    id = TOOLBAR_ACTION_ADD_NOTE,
                    text = "+",
                    contentDescription = "Add note",
                )
            ),
        ),
    ),
) {
    private val notesRepository = appContainer.notesRepository()

    init {
        viewModelScope.launch {
            notesRepository.observeAllNotes().collectLatest { notes ->
                updateScreenState { it.copy(notes = notes) }
            }
        }
    }

    override fun onIntent(intent: NoteListIntent) = Unit

    override fun onToolbarActionClicked(actionId: String) {
        when (actionId) {
            TOOLBAR_ACTION_ADD_NOTE -> showSnackbar(
                SnackbarState(
                    id = "add-note",
                    message = "Add note clicked",
                )
            )
        }
    }

    companion object {
        const val TOOLBAR_ACTION_ADD_NOTE = "toolbar-action-add-note"
    }
}
