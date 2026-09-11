package dev.pinaki.localnotes.feature.list

import androidx.lifecycle.viewModelScope
import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.core.CoreViewModel
import dev.pinaki.localnotes.core.ToolbarState
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
}
