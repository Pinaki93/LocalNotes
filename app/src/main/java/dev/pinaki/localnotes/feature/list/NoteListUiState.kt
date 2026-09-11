package dev.pinaki.localnotes.feature.list

import androidx.compose.runtime.Immutable
import dev.pinaki.localnotes.data.Note

@Immutable
data class NoteListUiState(
    val notes: List<Note> = emptyList(),
)

sealed interface NoteListIntent {
    data class EditNote(val noteId: Int) : NoteListIntent
}
