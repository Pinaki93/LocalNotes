package dev.pinaki.localnotes.feature.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pinaki.localnotes.core.CoreScaffold
import dev.pinaki.localnotes.data.Note
import dev.pinaki.localnotes.ui.components.NeoSurface
import dev.pinaki.localnotes.ui.components.NeoTag
import dev.pinaki.localnotes.ui.theme.AcidYellow
import dev.pinaki.localnotes.ui.theme.LocalNotesTheme
import dev.pinaki.localnotes.ui.theme.Mint

@Composable
fun NoteListDestination(
    viewModel: NoteListViewModel = viewModel(),
) {
    CoreScaffold(viewModel = viewModel) { padding, state, _ ->
        NoteListScreen(
            state = state,
            contentPadding = padding,
        )
    }
}

@Composable
fun NoteListScreen(
    state: NoteListUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    if (state.notes.isEmpty()) {
        EmptyNotesView(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("YOUR NOTES", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            NeoTag(
                text = "${state.notes.size} notes · Stored locally",
                color = AcidYellow,
            )
        }

        items(state.notes, key = Note::id) { note ->
            NoteCard(note)
        }
    }
}

@Composable
private fun EmptyNotesView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NeoTag(text = "Nothing here yet", color = AcidYellow)
        Text(
            text = "YOUR NOTES WILL LIVE HERE.",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Create your first note to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteCard(note: Note) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            NeoTag(text = "Local note", color = Mint)
            Text(note.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NoteListPreview() {
    LocalNotesTheme(darkTheme = false) {
        NoteListScreen(
            state = NoteListUiState(
                notes = listOf(
                    Note(title = "A local note", content = "Everything stays on this device."),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmptyNoteListPreview() {
    LocalNotesTheme(darkTheme = false) {
        NoteListScreen(state = NoteListUiState())
    }
}
