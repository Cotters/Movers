package com.jcotters.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MoversFilmCard(
    title: String,
    year: String,
    onOpen: () -> Unit,
    poster: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(tokens.cornerRadius))) {
            poster(Modifier.matchParentSize().then(filmAction(title, onOpen, describe = true)))
            onAdd?.let { add ->
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).background(tokens.colors.surface, RoundedCornerShape(tokens.cornerRadius))) {
                    MoversIconButton(MoversSymbol.Add, "Add $title to a demo list", add)
                }
            }
        }
        MoversText(title, style = tokens.type.label, maxLines = 2)
        MoversText(year, style = tokens.type.caption, color = tokens.colors.muted)
    }
}

@Composable
fun MoversFilmRow(
    title: String,
    metadata: String,
    synopsis: String,
    onOpen: () -> Unit,
    poster: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    Row(modifier.fillMaxWidth().then(filmAction(title, onOpen, describe = false)), horizontalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f), verticalAlignment = Alignment.Top) {
        poster(Modifier.width(80.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(tokens.cornerRadius)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
            MoversText(title, style = tokens.type.label, maxLines = 2)
            MoversText(metadata, style = tokens.type.caption, color = tokens.colors.muted)
            MoversText(synopsis, style = tokens.type.caption, maxLines = 3)
        }
        onAdd?.let { MoversIconButton(MoversSymbol.Add, "Add $title to a demo list", it) }
    }
}

@Composable
fun MoversRating(score: Double, voteCount: String, modifier: Modifier = Modifier) {
    val tokens = MoversDesign.tokens
    val valid = score.isFinite() && score in 0.0..10.0
    val value = if (valid) score else 0.0
    val label = if (valid) "${(value * 10).toInt() / 10.0} / 10" else "Unrated"
    FlowRow(
        modifier.clearAndSetSemantics { contentDescription = "$label · $voteCount votes" },
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(100.dp, 20.dp)) {
            val cell = size.width / 5
            repeat(5) { index ->
                val center = Offset(cell * (index + 0.5f), size.height / 2)
                val path = Path().apply {
                    repeat(10) { point ->
                        val angle = -PI / 2 + point * PI / 5
                        val radius = size.height * if (point % 2 == 0) 0.45f else 0.21f
                        val x = center.x + cos(angle).toFloat() * radius
                        val y = center.y + sin(angle).toFloat() * radius
                        if (point == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, tokens.colors.muted, style = Stroke(1.dp.toPx()))
                val fraction = (value / 2 - index).coerceIn(0.0, 1.0).toFloat()
                val bounds = path.getBounds()
                clipRect(left = bounds.left, right = bounds.left + bounds.width * fraction) { drawPath(path, tokens.colors.accent) }
            }
        }
        MoversText(label, style = tokens.type.caption)
        MoversText("$voteCount votes", style = tokens.type.caption, color = tokens.colors.muted)
    }
}

@Composable
fun MoversAvatar(name: String, modifier: Modifier = Modifier, alternate: Boolean = false) {
    val tokens = MoversDesign.tokens
    Box(
        modifier.size(32.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)).background(if (alternate) tokens.colors.raised else tokens.colors.accent, CircleShape)
            .clearAndSetSemantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        MoversText(name.trim().take(1).uppercase().ifEmpty { "?" }, style = tokens.type.label,
            color = if (alternate) tokens.colors.content else tokens.colors.onAccent)
    }
}

@Composable
fun MoversWatchlistSummary(name: String, filmCount: Int, participants: List<String>, modifier: Modifier = Modifier) {
    val tokens = MoversDesign.tokens
    MoversPanel(modifier.fillMaxWidth()) {
        MoversText(name, style = tokens.type.title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoversText("$filmCount films", style = tokens.type.caption, color = tokens.colors.muted)
            participants.take(3).forEachIndexed { index, person -> MoversAvatar(person, alternate = index % 2 != 0) }
            if (participants.size > 3) MoversText("+${participants.size - 3}", style = tokens.type.caption)
        }
    }
}

@Composable
fun MoversActivity(name: String, filmTitle: String, action: String, time: String, removed: Boolean, modifier: Modifier = Modifier) {
    val tokens = MoversDesign.tokens
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MoversAvatar(name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MoversText("$name $action", style = tokens.type.label, color = if (removed) tokens.colors.success else tokens.colors.danger)
            MoversText(filmTitle, style = tokens.type.body.copy(fontStyle = FontStyle.Italic))
            MoversText(time, style = tokens.type.caption, color = tokens.colors.muted)
        }
    }
}

@Composable
private fun filmAction(title: String, onOpen: () -> Unit, describe: Boolean): Modifier {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    return Modifier.clip(RoundedCornerShape(tokens.cornerRadius)).clickable(interactionSource = interaction, indication = null,
        role = Role.Button, onClickLabel = "Open $title", onClick = onOpen)
        .semantics { if (describe) contentDescription = title }
        .drawWithContent {
            drawContent()
            if (pressed) drawRect(tokens.colors.accent.copy(alpha = 0.12f))
        }
        .border(2.dp, if (focused) tokens.colors.accent else Color.Transparent, RoundedCornerShape(tokens.cornerRadius))
}
