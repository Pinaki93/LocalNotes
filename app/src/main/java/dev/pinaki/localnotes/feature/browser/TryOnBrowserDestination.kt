package dev.pinaki.localnotes.feature.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import dev.pinaki.localnotes.core.CoreScaffold
import dev.pinaki.localnotes.ui.components.NeoSurface
import dev.pinaki.localnotes.ui.components.NeoTag
import dev.pinaki.localnotes.ui.theme.AcidYellow
import dev.pinaki.localnotes.ui.theme.ElectricBlue
import dev.pinaki.localnotes.ui.theme.Ink
import dev.pinaki.localnotes.ui.theme.LocalNotesTheme
import dev.pinaki.localnotes.ui.theme.Mint

@Composable
fun TryOnBrowserDestination(viewModel: TryOnBrowserViewModel = viewModel()) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(
            if (granted) TryOnBrowserIntent.ToggleServer
            else TryOnBrowserIntent.NotificationPermissionDenied,
        )
    }

    CoreScaffold(viewModel = viewModel) { padding, state, onIntent ->
        TryOnBrowser(
            state = state,
            contentPadding = padding,
            onToggle = {
                val needsNotificationPermission =
                    !state.isStarted &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                if (needsNotificationPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onIntent(TryOnBrowserIntent.ToggleServer)
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
    onToggle: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("TRY ON BROWSER", style = MaterialTheme.typography.headlineMedium)
        NeoSurface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            color = if (state.isStarted) Mint else AcidYellow,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
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
