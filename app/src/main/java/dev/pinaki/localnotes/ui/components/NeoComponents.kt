package dev.pinaki.localnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.pinaki.localnotes.ui.theme.Ink

private val NeoShape = RoundedCornerShape(3.dp)

@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    shadowColor: Color = Ink,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowOffset: Dp = 5.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)) {
        Box(Modifier.matchParentSize().offset(shadowOffset, shadowOffset).background(shadowColor, NeoShape))
        Box(
            modifier = Modifier.fillMaxWidth().background(color, NeoShape).border(2.dp, borderColor, NeoShape),
            content = content,
        )
    }
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    NeoSurface(
        modifier = modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactions,
            indication = null,
            onClick = onClick,
        ),
        color = if (enabled) color else MaterialTheme.colorScheme.surfaceVariant,
        shadowOffset = if (enabled) 4.dp else 2.dp,
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            color = if (enabled) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge)
        BasicTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, NeoShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, NeoShape).padding(horizontal = 14.dp, vertical = 13.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = singleLine, visualTransformation = visualTransformation,
            decorationBox = { field ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    field()
                }
            },
        )
    }
}

@Composable
fun NeoTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.background(color, NeoShape).border(2.dp, Ink, NeoShape).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(text.uppercase(), color = Ink, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun NeoDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit, confirmLabel: String = "Delete") {
    Dialog(onDismissRequest = onDismiss) {
        NeoSurface {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    NeoButton("Cancel", onDismiss, color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    NeoButton(confirmLabel, onConfirm, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
