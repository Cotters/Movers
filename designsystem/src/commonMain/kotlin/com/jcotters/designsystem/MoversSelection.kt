package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MoversChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(tokens.cornerRadius)
    Box(
        modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(shape)
            .background(if (selected) tokens.colors.accent else tokens.colors.surface)
            .border(if (focused) 2.dp else 1.dp, if (focused) tokens.colors.accent else tokens.colors.controlOutline, shape)
            .selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = tokens.controlPadding, vertical = tokens.spacing),
        contentAlignment = Alignment.Center,
    ) {
        MoversText(label, style = tokens.type.label, color = if (selected) tokens.colors.onAccent else tokens.colors.content)
    }
}

@Composable
fun MoversCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(tokens.cornerRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(if (pressed) tokens.colors.raised else Color.Transparent)
            .border(1.dp, if (focused) tokens.colors.accent else Color.Transparent, shape)
            .toggleable(checked, interactionSource = interaction, indication = null, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = tokens.spacing, vertical = tokens.spacing),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val outline = if (enabled) tokens.colors.accent else tokens.colors.muted
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                .background(if (checked) outline else tokens.colors.surface)
                .border(1.5.dp, outline, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) MoversIcon(MoversSymbol.Check, Modifier.size(18.dp), tokens.colors.onAccent)
        }
        Column(Modifier.weight(1f)) {
            MoversText(label, color = if (enabled) tokens.colors.content else tokens.colors.muted)
            description?.let { MoversText(it, style = tokens.type.caption, color = tokens.colors.muted) }
        }
    }
}
