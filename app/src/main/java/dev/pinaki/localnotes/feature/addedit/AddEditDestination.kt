package dev.pinaki.localnotes.feature.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pinaki.localnotes.core.CoreScaffold
import dev.pinaki.localnotes.ui.components.NeoButton
import dev.pinaki.localnotes.ui.components.NeoTag
import dev.pinaki.localnotes.ui.components.NeoTextField
import dev.pinaki.localnotes.ui.theme.AcidYellow
import dev.pinaki.localnotes.ui.theme.ElectricBlue
import dev.pinaki.localnotes.ui.theme.LocalNotesTheme

object AddEditDestination {
    const val NOTE_ID_ARGUMENT = "noteId"
    const val ROUTE = "addedit/{$NOTE_ID_ARGUMENT}"

    fun route(noteId: Int = 0): String = "addedit/$noteId"
}

@Composable
fun AddEditDestination(noteId: Int) {
    val viewModel: AddEditViewModel = viewModel(
        key = "add-edit-$noteId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AddEditViewModel(noteId) as T
        },
    )

    CoreScaffold(viewModel = viewModel) { padding, state, onIntent ->
        AddEditScreen(state = state, contentPadding = padding, onIntent = onIntent)
    }
}

@Composable
fun AddEditScreen(
    state: AddEditUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onIntent: (AddEditIntent) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        NeoTag(
            text = if (state.isEditMode) "Editing local note" else "New local note",
            color = AcidYellow,
        )
        Text(
            text = if (state.isEditMode) "MAKE YOUR CHANGES" else "WRITE SOMETHING DOWN",
            style = MaterialTheme.typography.headlineMedium,
        )
        NeoTextField(
            value = state.title,
            onValueChange = { onIntent(AddEditIntent.TitleChanged(it)) },
            label = "Title",
            placeholder = "Note title",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NeoTextField(
            value = state.content,
            onValueChange = { onIntent(AddEditIntent.ContentChanged(it)) },
            label = "Content",
            placeholder = "Start writing...",
            modifier = Modifier.fillMaxWidth(),
        )
        NeoButton(
            text = if (state.isSaving) "Saving..." else "Save note",
            onClick = { onIntent(AddEditIntent.Save) },
            enabled = state.canSave,
            color = ElectricBlue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddEditPreview() {
    LocalNotesTheme(darkTheme = false) {
        AddEditScreen(
            state = AddEditUiState(title = "A local note", content = "Everything stays here."),
        )
    }
}
