package dev.pinaki.localnotes.feature.list

import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.core.CoreViewModel
import dev.pinaki.localnotes.core.ToolbarState
import dev.pinaki.localnotes.data.Note
import java.util.Date

class NoteListViewModel : CoreViewModel<NoteListUiState, NoteListIntent>(
    initialScreenState = NoteListUiState(
        notes = listOf(
            Note(
                id = 1,
                title = "Ship the first version",
                content = "Small, useful, and offline-first. Polish can follow momentum.",
                dateAdded = Date(1_788_537_600_000),
                dateModified = Date(1_788_537_600_000),
            ),
            Note(
                id = 2,
                title = "Weekend reading",
                content = "The Design of Everyday Things — chapter four.",
                dateAdded = Date(1_788_451_200_000),
                dateModified = Date(1_788_451_200_000),
            ),
            Note(
                id = 3,
                title = "Grocery list",
                content = "Coffee, oat milk, tomatoes, basil, and sourdough.",
                dateAdded = Date(1_788_364_800_000),
                dateModified = Date(1_788_364_800_000),
            ),
        ),
    ),
    initialCommonState = CoreUiState(
        toolbarState = ToolbarState(
            title = "Local Notes",
            showNavigationIcon = false,
        ),
    ),
) {
    override fun onIntent(intent: NoteListIntent) = Unit
}
