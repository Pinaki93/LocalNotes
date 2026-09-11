package dev.pinaki.localnotes.feature.addedit

import androidx.lifecycle.viewModelScope
import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.core.CoreViewModel
import dev.pinaki.localnotes.core.SnackbarState
import dev.pinaki.localnotes.core.ToolbarAction
import dev.pinaki.localnotes.core.ToolbarState
import dev.pinaki.localnotes.data.Note
import dev.pinaki.localnotes.di.AppContainer
import kotlinx.coroutines.launch

class AddEditViewModel(
    private val noteId: Int,
    private val appContainer: AppContainer = AppContainer.getInstance(),
) : CoreViewModel<AddEditUiState, AddEditIntent>(
    initialScreenState = AddEditUiState(
        isEditMode = noteId > 0,
        isLoading = noteId > 0,
    ),
    initialCommonState = CoreUiState(
        toolbarState = ToolbarState(
            title = if (noteId > 0) "Edit note" else "Add note",
        ),
    ),
) {
    private val repository = appContainer.notesRepository()
    private var originalNote: Note? = null

    init {
        if (noteId > 0) loadNote()
    }

    override fun onIntent(intent: AddEditIntent) {
        when (intent) {
            is AddEditIntent.TitleChanged -> updateScreenState { it.copy(title = intent.title) }
            is AddEditIntent.ContentChanged -> updateScreenState { it.copy(content = intent.content) }
            AddEditIntent.Save -> saveNote()
        }
    }

    override fun onBackPress() = appContainer.navigator.navigateUpAsync()

    private fun loadNote() {
        viewModelScope.launch {
            runCatching { repository.getNote(noteId) }
                .onSuccess { note ->
                    originalNote = note
                    if (note == null) {
                        updateScreenState { it.copy(isLoading = false) }
                        showSnackbar(SnackbarState("note-not-found", "Note not found"))
                    } else {
                        updateScreenState {
                            it.copy(title = note.title, content = note.content, isLoading = false)
                        }
                    }
                }
                .onFailure {
                    updateScreenState { it.copy(isLoading = false) }
                    showSnackbar(SnackbarState("load-failed", "Unable to load note"))
                }
        }
    }

    private fun saveNote() {
        val state = currentScreenState
        if (!state.canSave) return

        updateScreenState { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                val title = state.title.trim().ifEmpty { "Untitled" }
                val content = state.content.trim()
                val existing = originalNote
                if (state.isEditMode) {
                    checkNotNull(existing) { "Note is unavailable" }
                    check(repository.updateNote(existing.copy(title = title, content = content))) {
                        "Note no longer exists"
                    }
                } else {
                    repository.createNote(title, content)
                }
            }.onSuccess {
                appContainer.navigator.popBackStackAsync()
            }.onFailure {
                updateScreenState { it.copy(isSaving = false) }
                showSnackbar(SnackbarState("save-failed", "Unable to save note"))
            }
        }
    }
}
