package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MoversTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MoversDesign.tokens
    Row(modifier.fillMaxWidth().background(tokens.colors.background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .heightIn(min = 64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        navigationIcon()
        Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 12.dp)) {
            ProvideTextStyle(tokens.type.title, content = title)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Slots let the feature supply its own attribution, metadata and interaction. */
@Composable
fun MoversListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    Row(modifier.fillMaxWidth().padding(horizontal = tokens.controlPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        leadingContent?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProvideTextStyle(tokens.type.label, content = headlineContent)
            if (supportingContent != null) CompositionLocalProvider(LocalContentColor provides tokens.colors.muted) {
                ProvideTextStyle(tokens.type.caption, content = supportingContent)
            }
        }
    }
}

@Composable
fun MoversCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MoversDesign.tokens.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MoversDesign.tokens
    val shape = RoundedCornerShape(tokens.cornerRadius)
    Column(modifier.clip(shape).background(containerColor).border(1.dp, tokens.colors.border, shape), content = content)
}

@Composable
fun MoversFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    MoversButton(onClick, modifier.semantics { this.selected = selected }, variant = if (selected) ButtonVariant.Primary else ButtonVariant.Secondary,
        enabled = enabled) { label() }
}
