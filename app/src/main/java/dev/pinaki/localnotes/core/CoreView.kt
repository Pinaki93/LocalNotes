package dev.pinaki.herdly.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.pinaki.localnotes.ui.components.NeoButton
import dev.pinaki.localnotes.ui.components.NeoSurface
import dev.pinaki.localnotes.ui.theme.AcidYellow
import dev.pinaki.localnotes.ui.theme.ElectricBlue
import dev.pinaki.localnotes.ui.theme.Ink

// 1. Toolbar State
@Immutable
data class ToolbarAction(
    val icon: ImageVector,
    val contentDescription: String?,
    val onClick: () -> Unit
)

@Immutable
data class ToolbarState(
    val title: String = "",
    val showNavigationIcon: Boolean = true,
    val actions: List<ToolbarAction> = emptyList(),
    val onNavigationClick: () -> Unit = {}
)

// 2. Alert Dialog State
@Immutable
data class AlertDialogState(
    val id: String,
    val title: String,
    val message: String,
    val positiveButtonText: String,
    val negativeButtonText: String? = null,
    val isDismissible: Boolean = true
)

// 3. Consolidated UI State
@Immutable
data class CoreUiState(
    val toolbarState: ToolbarState? = null, // null hides the toolbar
    val isLoading: Boolean = false,
    val alertDialogState: AlertDialogState? = null
)

@Composable
fun <S, I> CoreScaffold(
    modifier: Modifier = Modifier,
    viewModel: CoreViewModel<S, I>,
    content: @Composable (PaddingValues, S, (I) -> Unit) -> Unit
) {
    val state = viewModel.uiState.collectAsState()
    CoreScaffold(
        modifier = modifier,
        state = state.value.commonState,
        onAlertPositiveClick = { id ->
            viewModel.onCommonIntent(CommonIntent.OnAlertPositiveClick(id))
        },
        onAlertNegativeClick = { id ->
            viewModel.onCommonIntent(CommonIntent.OnAlertNegativeClick(id))
        },
        onAlertDismiss = { id ->
            viewModel.onCommonIntent(CommonIntent.OnAlertDismiss(id))
        },
        content = { paddingValues ->
            content(
                paddingValues,
                state.value.screenSpecificState,
                viewModel::onIntent
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreScaffold(
    state: CoreUiState,
    modifier: Modifier = Modifier,
    onAlertPositiveClick: (dialogId: String) -> Unit = {},
    onAlertNegativeClick: (dialogId: String) -> Unit = {},
    onAlertDismiss: (dialogId: String) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            state.toolbarState?.let { toolbar ->
                TopAppBar(
                    modifier = Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                    ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AcidYellow,
                        titleContentColor = Ink,
                        navigationIconContentColor = Ink,
                        actionIconContentColor = Ink
                    ),
                    title = {
                        Text(
                            text = toolbar.title.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (toolbar.showNavigationIcon) {
                            NeoIconButton(onClick = toolbar.onNavigationClick) {
                                Text(
                                    text = "[X]",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    },
                    actions = {
                        toolbar.actions.forEach { action ->
                            NeoIconButton(onClick = action.onClick) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.contentDescription
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Main Screen Content
        content(innerPadding)

        // Loading Overlay Dialog
        if (state.isLoading) {
            LoadingDialog()
        }

        // Alert Dialog Handling
        state.alertDialogState?.let { alert ->
            Dialog(
                onDismissRequest = {
                    if (alert.isDismissible) onAlertDismiss(alert.id)
                },
                properties = DialogProperties(
                    dismissOnBackPress = alert.isDismissible,
                    dismissOnClickOutside = alert.isDismissible
                )
            ) {
                NeoSurface(color = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                    ) {
                        Text(
                            text = alert.title.uppercase(),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            alert.negativeButtonText?.let { negativeText ->
                                NeoButton(
                                    text = negativeText,
                                    onClick = { onAlertNegativeClick(alert.id) },
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            NeoButton(
                                text = alert.positiveButtonText,
                                onClick = { onAlertPositiveClick(alert.id) },
                                modifier = Modifier.weight(1f),
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeoIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(3.dp))
            .border(2.dp, Ink, RoundedCornerShape(3.dp))
    ) {
        content()
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        NeoSurface(
            modifier = Modifier.size(104.dp),
            color = AcidYellow
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Ink,
                    strokeWidth = 5.dp
                )
            }
        }
    }
}
