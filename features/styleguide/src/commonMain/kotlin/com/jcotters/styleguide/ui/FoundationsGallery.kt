package com.jcotters.styleguide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversDesign
import com.jcotters.designsystem.MoversDesignTheme
import com.jcotters.designsystem.MoversIcon
import com.jcotters.designsystem.MoversPanel
import com.jcotters.designsystem.MoversSymbol
import com.jcotters.designsystem.MoversText

@Composable
internal fun FoundationsGallery() {
    val tokens = MoversDesign.tokens
    GalleryHeading("A little cinema, everywhere", "One Movers style · experimental")
    MoversPanel(Modifier.fillMaxWidth()) {
        MoversText("Inter", style = tokens.type.headline)
        MoversText("Good films. Better company.", style = tokens.type.title)
        MoversText("A quiet interface leaves room for the films and the people who chose them.")
        MoversText("Labels and actions · Medium", style = tokens.type.label)
        MoversText("Metadata · 2026 · 114 min", style = tokens.type.caption, color = tokens.colors.muted)
    }
    GalleryHeading("Colour", "Graphite, ivory and a touch of mint")
    val swatches = listOf(
        "Surface" to tokens.colors.surface, "Raised" to tokens.colors.raised,
        "Accent" to tokens.colors.accent, "Danger" to tokens.colors.danger, "Success" to tokens.colors.success,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        swatches.forEach { (label, color) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(48.dp).background(color, RoundedCornerShape(tokens.cornerRadius)))
                MoversText(label, style = tokens.type.caption)
            }
        }
    }
    GalleryHeading("Icons", "Original strokes · 48 dp action targets")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MoversSymbol.entries.forEach { symbol ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoversIcon(symbol)
                MoversText(symbol.name, style = tokens.type.caption)
            }
        }
    }
    GalleryHeading("Spacing", "${tokens.spacing.value.toInt()} dp rhythm · 48 dp minimum touch targets")
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing)) {
        listOf(1, 2, 3, 4).forEach { step ->
            Box(Modifier.size(tokens.spacing * step).background(tokens.colors.accent, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
internal fun GalleryHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoversText(title, Modifier.semantics { heading() }, style = MoversDesign.tokens.type.title)
        subtitle?.let {
            MoversText(
                it,
                style = MoversDesign.tokens.type.caption,
                color = MoversDesign.tokens.colors.muted
            )
        }
    }
}

@Preview
@Composable
private fun FoundationsPreview() {
    MoversDesignTheme { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { FoundationsGallery() } }
}
