package dev.pinaki.localnotes.feature.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import dev.pinaki.localnotes.core.CoreScaffold
import dev.pinaki.localnotes.core.CoreUiState
import dev.pinaki.localnotes.ui.components.NeoButton
import dev.pinaki.localnotes.ui.components.NeoSurface
import dev.pinaki.localnotes.ui.components.NeoTag
import dev.pinaki.localnotes.ui.components.NeoTextField
import dev.pinaki.localnotes.ui.theme.AcidYellow
import dev.pinaki.localnotes.ui.theme.ElectricBlue
import dev.pinaki.localnotes.ui.theme.Ink
import dev.pinaki.localnotes.ui.theme.LocalNotesTheme
import dev.pinaki.localnotes.ui.theme.Mint

@Composable
fun TryOnBrowserDestination(viewModel: TryOnBrowserViewModel = viewModel()) {
    val context = LocalContext.current
    var startPendingPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && startPendingPermission) {
            viewModel.onIntent(TryOnBrowserIntent.StartServer())
        } else if (!granted) {
            viewModel.onIntent(TryOnBrowserIntent.NotificationPermissionDenied)
        }
        startPendingPermission = false
    }

    CoreScaffold(viewModel = viewModel) { padding, state, onIntent ->
        TryOnBrowser(
            state = state,
            contentPadding = padding,
            onStop = { onIntent(TryOnBrowserIntent.StopServer) },
            onSavePassword = { onIntent(TryOnBrowserIntent.SavePassword(it)) },
            onEnable = {
                val needsNotificationPermission =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                if (needsNotificationPermission) {
                    startPendingPermission = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onIntent(TryOnBrowserIntent.StartServer())
                }
            },
        )
    }
}

@Composable
fun TryOnBrowser(
    state: TryOnBrowserUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onEnable: () -> Unit = {},
    onSavePassword: (String) -> Unit = {},
    onStop: () -> Unit = {},
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("TRY ON BROWSER", style = MaterialTheme.typography.headlineMedium)
        NeoButton(
            text = "Set new password",
            onClick = { showPasswordDialog = true },
            modifier = Modifier.fillMaxWidth(),
            color = ElectricBlue,
            enabled = !state.isStarted,
        )
        NeoSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (state.isStarted) {
                        onStop()
                    } else if (state.hasPassword) {
                        onEnable()
                    }
                },
            color = when {
                state.isStarted -> Mint
                state.hasPassword -> AcidYellow
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("start browsing on web", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (state.isStarted) "[started]" else "[stopped]",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (state.isStarted && state.addresses.isNotEmpty()) {
            AccessOnCard(addresses = state.addresses)
        }
    }
    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                onSavePassword(it)
                showPasswordDialog = false
            },
        )
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val mismatch = confirmation.isNotEmpty() && password != confirmation
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        NeoSurface(modifier = Modifier.imePadding(), color = ElectricBlue) {
            Column(
                Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("SET NEW PASSWORD", color = Ink, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "[X]",
                        modifier = Modifier.clickable(onClick = onDismiss),
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    "Use this password to log in from your browser.",
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
                NeoTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                NeoTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = "Confirm password",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (mismatch) {
                    Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NeoButton(
                        "Save",
                        { onConfirm(password) },
                        modifier = Modifier.weight(1f),
                        color = AcidYellow,
                        enabled = password.isNotBlank() && password == confirmation,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessOnCard(addresses: List<String>) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        color = ElectricBlue,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ACCESS ON",
                        color = Ink,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "OPEN IN YOUR BROWSER",
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                NeoTag(text = "Live", color = Mint)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Use a device connected to the same Wi-Fi network.",
                color = Ink,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                addresses.forEachIndexed { index, address ->
                    SelectionContainer {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(3.dp),
                                )
                                .border(2.dp, Ink, RoundedCornerShape(3.dp))
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "[${index + 1}]",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = address,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TryOnBrowserPreview() {
    LocalNotesTheme {
        TryOnBrowser(
            state = TryOnBrowserUiState(
                isStarted = true,
                addresses = listOf("http://192.168.0.4:8080", "http://localhost:8080"),
            ),
        )
    }
}

@Preview
@Composable
private fun PasswordDialogPreview() {
    LocalNotesTheme {
        CoreScaffold(state = CoreUiState()) {
            PasswordDialog(onDismiss = {}) {

            }
        }

    }
}
