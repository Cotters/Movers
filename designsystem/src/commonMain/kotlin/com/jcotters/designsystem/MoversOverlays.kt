package com.jcotters.designsystem

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.composeunstyled.DialogPanel
import com.composeunstyled.DragIndication
import com.composeunstyled.DropdownMenuPanel
import com.composeunstyled.ModalBottomSheetProperties
import com.composeunstyled.Scrim
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.UnstyledDropdownMenuItem
import com.composeunstyled.UnstyledModalBottomSheet
import com.composeunstyled.rememberModalBottomSheetState

data class MoversMenuAction(
    val label: String,
    val onClick: () -> Unit,
    val symbol: MoversSymbol? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

@Composable
fun MoversMenu(actions: List<MoversMenuAction>, modifier: Modifier = Modifier, label: String = "List options") {
    val tokens = MoversDesign.tokens
    val shape = RoundedCornerShape(tokens.cornerRadius + 4.dp)
    var expanded by rememberSaveable { mutableStateOf(false) }
    UnstyledDropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        sideOffset = 6.dp,
        anchor = { MoversIconButton(MoversSymbol.More, label, { expanded = !expanded }) },
        panel = {
            DropdownMenuPanel(
                modifier = Modifier.widthIn(min = 220.dp, max = 320.dp).shadow(12.dp, shape).clip(shape)
                    .background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape).padding(6.dp),
                enter = fadeIn(tween(160)), exit = fadeOut(tween(120)),
            ) {
                actions.forEach { action ->
                    val interaction = remember(action.label) { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    val pressed by interaction.collectIsPressedAsState()
                    val itemShape = RoundedCornerShape(tokens.cornerRadius)
                    UnstyledDropdownMenuItem(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        interactionSource = interaction,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(itemShape)
                            .background(if (focused || pressed) tokens.colors.raised else tokens.colors.surface)
                            .border(1.dp, if (focused) tokens.colors.accent else tokens.colors.surface, itemShape)
                            .padding(horizontal = 10.dp, vertical = tokens.spacing),
                    ) {
                        val foreground = when {
                            !action.enabled -> tokens.colors.muted
                            action.destructive -> tokens.colors.danger
                            else -> tokens.colors.content
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (action.symbol != null) MoversIcon(action.symbol, tint = foreground)
                            else Box(Modifier.size(24.dp))
                            MoversText(action.label, style = tokens.type.label, color = foreground)
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun MoversDialog(title: String, visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val tokens = MoversDesign.tokens
    val shape = RoundedCornerShape(tokens.cornerRadius + 8.dp)
    UnstyledDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        overlay = { Scrim(scrimColor = tokens.colors.scrim, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) },
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), contentAlignment = Alignment.Center) {
            DialogPanel(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().shadow(16.dp, shape).clip(shape)
                    .background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape),
                paneTitle = title,
                enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.97f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.97f),
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(tokens.controlPadding),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f),
                ) {
                    OverlayHeader(title, onDismiss)
                    content()
                }
            }
        }
    }
}

@Composable
fun MoversSheet(title: String, visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val tokens = MoversDesign.tokens
    val shape = RoundedCornerShape(topStart = tokens.cornerRadius + 8.dp, topEnd = tokens.cornerRadius + 8.dp)
    val state = rememberModalBottomSheetState(
        initialDetent = SheetDetent.Hidden,
        animationSpec = tween(240), dismissAnimationSpec = tween(180),
    )
    LaunchedEffect(visible) { state.targetDetent = if (visible) SheetDetent.FullyExpanded else SheetDetent.Hidden }
    UnstyledModalBottomSheet(
        state = state,
        properties = ModalBottomSheetProperties(offsetForIme = true),
        onDismiss = onDismiss,
        overlay = { Scrim(scrimColor = tokens.colors.scrim, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Sheet(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(shape).background(tokens.colors.surface)
                    .border(1.dp, tokens.colors.border, shape),
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)).padding(tokens.controlPadding),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f),
                ) {
                    DragIndication(
                        Modifier.align(Alignment.CenterHorizontally).size(64.dp, 48.dp).drawBehind {
                            val width = 40.dp.toPx()
                            val height = 4.dp.toPx()
                            drawRoundRect(tokens.colors.border,
                                topLeft = Offset((size.width - width) / 2, (size.height - height) / 2),
                                size = Size(width, height), cornerRadius = CornerRadius(2.dp.toPx()))
                        },
                    )
                    OverlayHeader(title, onDismiss)
                    content()
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MoversText(title, Modifier.weight(1f), style = MoversDesign.tokens.type.title)
        MoversIconButton(MoversSymbol.Close, "Close $title", onClose)
    }
}
