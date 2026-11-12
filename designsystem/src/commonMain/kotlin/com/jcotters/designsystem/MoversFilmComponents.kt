package com.jcotters.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    posterOverlay: @Composable BoxScope.() -> Unit = {},
    metadataAccessory: (@Composable () -> Unit)? = null,
) {
    val tokens = MoversDesign.tokens
    Column(modifier.then(filmAction(title, onOpen)), verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(tokens.imageCornerRadius))) {
            poster(Modifier.matchParentSize())
            posterOverlay()
            onAdd?.let { add ->
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).background(tokens.colors.surface, RoundedCornerShape(tokens.cornerRadius))) {
                    MoversIconButton(MoversSymbol.Add, "Add $title to list", add)
                }
            }
        }
        MoversText(title, style = tokens.type.label, maxLines = 2)
        FilmMetadata(year, metadataAccessory)
    }
}

@Composable
fun MoversFilmRow(
    title: String,
    metadata: String,
    synopsis: String,
    onOpen: (() -> Unit)?,
    poster: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    compact: Boolean = false,
    posterWidth: Dp? = null,
    metadataAccessory: (@Composable () -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MoversDesign.tokens
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .then(if (onOpen == null) Modifier else filmAction(title, onOpen)),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f),
        verticalAlignment = Alignment.Top,
    ) {
        // Explicit widths retain a 2:3 poster; other rows keep their text-height sizing.
        val imageWidth = posterWidth ?: if (compact) 56.dp else 96.dp
        val imageSizing = if (posterWidth != null) Modifier.aspectRatio(2f / 3f) else Modifier.fillMaxHeight()
        Box(Modifier.width(imageWidth).then(imageSizing).align(Alignment.Bottom).clip(RoundedCornerShape(tokens.imageCornerRadius))) {
            poster(Modifier.matchParentSize())
        }
        Column(
            Modifier.weight(1f).heightIn(min = posterWidth?.let { it * 1.5f } ?: if (compact) 96.dp else 144.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing),
        ) {
            MoversText(title, style = tokens.type.label, maxLines = 2)
            FilmMetadata(metadata, metadataAccessory)
            if (synopsis.isNotBlank()) MoversText(synopsis, style = tokens.type.caption, maxLines = if (compact) 2 else 4)
        }
        onAdd?.let { MoversIconButton(MoversSymbol.Add, "Add $title to list", it) }
        trailingContent()
    }
}

@Composable
private fun FilmMetadata(value: String, accessory: (@Composable () -> Unit)?) {
    if (value.isBlank() && accessory == null) return
    val tokens = MoversDesign.tokens
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isNotBlank()) MoversText(value, style = tokens.type.caption, color = tokens.colors.muted)
        }
        accessory?.invoke()
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
        MoversRatingStars(value)
        MoversText(label, style = tokens.type.caption)
        MoversText("$voteCount votes", style = tokens.type.caption, color = tokens.colors.muted)
    }
}

/** Shared fractional stars, including RTL fill direction and invalid-score handling. */
@Composable
fun MoversRatingStars(score: Double, modifier: Modifier = Modifier) {
    val tokens = MoversDesign.tokens
    val value = if (score.isFinite()) score.coerceIn(0.0, 10.0) else 0.0
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(modifier.size(100.dp, 20.dp)) {
        val cell = size.width / 5
        repeat(5) { index ->
            val position = if (rtl) 4 - index else index
            val center = Offset(cell * (position + 0.5f), size.height / 2)
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
            clipRect(
                left = if (rtl) bounds.right - bounds.width * fraction else bounds.left,
                right = if (rtl) bounds.right else bounds.left + bounds.width * fraction,
            ) { drawPath(path, tokens.colors.accent) }
        }
    }
}

@Composable
fun MoversAvatar(
    name: String,
    modifier: Modifier = Modifier,
    background: Color = MoversDesign.tokens.colors.accent,
    contentColor: Color = MoversDesign.tokens.colors.onAccent,
) {
    val tokens = MoversDesign.tokens
    Box(
        modifier.size(32.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)).background(background, CircleShape)
            .clearAndSetSemantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        MoversText(name.trim().take(1).uppercase().ifEmpty { "?" }, style = tokens.type.label,
            color = contentColor)
    }
}

@Composable
fun MoversWatchlistSummary(name: String, filmCount: Int, participants: List<String>, modifier: Modifier = Modifier) {
    val tokens = MoversDesign.tokens
    MoversPanel(modifier.fillMaxWidth()) {
        MoversText(name, style = tokens.type.title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoversText("$filmCount films", style = tokens.type.caption, color = tokens.colors.muted)
            participants.take(3).forEachIndexed { index, person -> MoversAvatar(person,
                background = if (index % 2 == 0) tokens.colors.accent else tokens.colors.raised,
                contentColor = if (index % 2 == 0) tokens.colors.onAccent else tokens.colors.content) }
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
            MoversText("$name $action", style = tokens.type.label, color = if (removed) tokens.colors.danger else tokens.colors.success)
            MoversText(filmTitle, style = tokens.type.body.copy(fontStyle = FontStyle.Italic))
            MoversText(time, style = tokens.type.caption, color = tokens.colors.muted)
        }
    }
}

@Composable
private fun filmAction(title: String, onOpen: () -> Unit): Modifier {
    val tokens = MoversDesign.tokens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    return Modifier.clickable(interactionSource = interaction, indication = null,
        role = Role.Button, onClickLabel = "Open $title", onClick = onOpen)
        .drawWithContent {
            drawContent()
            if (pressed) drawRect(tokens.colors.accent.copy(alpha = 0.12f))
        }
        .border(2.dp, if (focused) tokens.colors.accent else Color.Transparent, RectangleShape)
}
