package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
fun MoversNotice(
    message: String,
    modifier: Modifier = Modifier,
    kind: NoticeKind = NoticeKind.Information,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    val background = when (kind) {
        NoticeKind.Information -> tokens.colors.raised
        NoticeKind.Success -> tokens.colors.successSurface
        NoticeKind.Error -> tokens.colors.dangerSurface
    }
    val foreground = when (kind) {
        NoticeKind.Information -> tokens.colors.content
        NoticeKind.Success -> tokens.colors.success
        NoticeKind.Error -> tokens.colors.danger
    }
    FlowRow(
        modifier.fillMaxWidth().background(background, RoundedCornerShape(tokens.cornerRadius))
            .padding(tokens.controlPadding).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MoversText(message, Modifier.weight(1f), style = tokens.type.label, color = foreground)
        if (actionLabel != null && onAction != null) {
            MoversButton(actionLabel, onAction, variant = ButtonVariant.Quiet)
        }
        if (onDismiss != null) MoversIconButton(MoversSymbol.Close, "Dismiss message", onDismiss)
    }
}
