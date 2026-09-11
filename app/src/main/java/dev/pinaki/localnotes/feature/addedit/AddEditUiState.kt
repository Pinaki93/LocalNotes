package dev.pinaki.localnotes.feature.addedit

import androidx.compose.runtime.Immutable

@Immutable
data class AddEditUiState(
    val title: String = "",
    val content: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && (title.isNotBlank() || content.isNotBlank())
}

sealed interface AddEditIntent {
    data class TitleChanged(val title: String) : AddEditIntent
    data class ContentChanged(val content: String) : AddEditIntent
    data object Save : AddEditIntent
}
