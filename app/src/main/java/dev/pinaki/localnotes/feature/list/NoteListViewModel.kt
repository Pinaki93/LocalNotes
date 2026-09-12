package dev.pinaki.localnotes.feature.list

import androidx.lifecycle.viewModelScope
import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.core.CoreViewModel
import dev.pinaki.localnotes.core.ToolbarState
import dev.pinaki.localnotes.core.ToolbarAction
import dev.pinaki.localnotes.di.AppContainer
import dev.pinaki.localnotes.feature.addedit.AddEditDestination
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoteListViewModel(
    private val appContainer: AppContainer = AppContainer.getInstance(),
) : CoreViewModel<NoteListUiState, NoteListIntent>(
    initialScreenState = NoteListUiState(),
    initialCommonState = CoreUiState(
        toolbarState = ToolbarState(
            title = "Local Notes",
            showNavigationIcon = false,
            actions = listOf(
                ToolbarAction(
                    id = TOOLBAR_ACTION_TRY_BROWSER,
                    text = "[B]",
                    contentDescription = "Try on browser",
                ),
                ToolbarAction(
                    id = TOOLBAR_ACTION_ADD_NOTE,
                    text = "[+]",
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

    override fun onIntent(intent: NoteListIntent) {
        when (intent) {
            is NoteListIntent.EditNote -> navigateToEditor(intent.noteId)
        }
    }

    override fun onToolbarActionClicked(actionId: String) {
        when (actionId) {
            TOOLBAR_ACTION_TRY_BROWSER -> appContainer.navigator.navigateAsync("browser")
            TOOLBAR_ACTION_ADD_NOTE -> navigateToEditor()
        }
    }

    private fun navigateToEditor(noteId: Int = 0) {
        appContainer.navigator.navigateAsync(AddEditDestination.route(noteId))
    }

    companion object {
        const val TOOLBAR_ACTION_ADD_NOTE = "toolbar-action-add-note"
        const val TOOLBAR_ACTION_TRY_BROWSER = "toolbar-action-try-browser"
    }
}
