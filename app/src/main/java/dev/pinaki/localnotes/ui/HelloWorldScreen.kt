package dev.pinaki.localnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pinaki.localnotes.ui.components.NeoButton
import dev.pinaki.localnotes.ui.components.NeoDialog
import dev.pinaki.localnotes.ui.components.NeoSurface
import dev.pinaki.localnotes.ui.components.NeoTag
import dev.pinaki.localnotes.ui.components.NeoTextField
import dev.pinaki.localnotes.ui.theme.AcidYellow
import dev.pinaki.localnotes.ui.theme.ElectricBlue
import dev.pinaki.localnotes.ui.theme.HotPink
import dev.pinaki.localnotes.ui.theme.LocalNotesTheme
import dev.pinaki.localnotes.ui.theme.Mint

@Composable
fun HelloWorldScreen(modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("Design the home screen") }
    var details by remember { mutableStateOf("Use bold cards and keep it delightfully simple.") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        NeoTag(text = "Local only · No noise", color = AcidYellow)
        Spacer(Modifier.height(18.dp))
        Text("BIG IDEAS.\nLOCAL NOTES.", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(10.dp))
        Text(
            "A cheerful, private place for thoughts worth keeping.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(26.dp))

        Text("NEW NOTE", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        NeoSurface(color = HotPink) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NeoTextField(title, { title = it }, "Title", placeholder = "What is this about?", singleLine = true)
                NeoTextField(details, { details = it }, "Note", placeholder = "Write something memorable…")
                NeoButton("Save note  →", {}, Modifier.fillMaxWidth(), ElectricBlue)
            }
        }

        Spacer(Modifier.height(30.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RECENT NOTES", style = MaterialTheme.typography.titleLarge)
            NeoTag("3 notes", Mint)
        }
        Spacer(Modifier.height(14.dp))
        SampleNoteCard(
            title = "Ship the first version",
            body = "Small, useful, and offline-first. Polish can follow momentum.",
            onDelete = { showDeleteDialog = true },
        )
        Spacer(Modifier.height(16.dp))
        SampleNoteCard(
            title = "Weekend reading",
            body = "The Design of Everyday Things — chapter four.",
            onDelete = { showDeleteDialog = true },
            accent = AcidYellow,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        NeoDialog(
            title = "Delete this note?",
            body = "This action cannot be undone. The note will be removed from this device.",
            onConfirm = { showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun SampleNoteCard(title: String, body: String, onDelete: () -> Unit, accent: androidx.compose.ui.graphics.Color = Mint) {
    NeoSurface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            NeoTag("In progress", accent)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                NeoButton("Delete", onDelete, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HelloWorldPreview() {
    LocalNotesTheme(darkTheme = false) { HelloWorldScreen() }
}
