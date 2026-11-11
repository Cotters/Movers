package com.jcotters.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MoversText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val tokens = MoversDesign.tokens
    BasicText(
        text = text,
        modifier = modifier,
        style = (style ?: tokens.type.body).merge(TextStyle(color = if (color == Color.Unspecified) tokens.colors.content else color)),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun MoversPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tokens = MoversDesign.tokens
    val shape = RoundedCornerShape(tokens.cornerRadius + 4.dp)
    Column(
        modifier = modifier.clip(shape).background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape)
            .padding(tokens.controlPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f),
        content = content,
    )
}
