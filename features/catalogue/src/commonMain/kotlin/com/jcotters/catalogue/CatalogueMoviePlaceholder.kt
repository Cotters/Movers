package com.jcotters.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversDesign
import com.jcotters.designsystem.MoversLoadingPlaceholder
import com.jcotters.designsystem.rememberMoversLoadingOpacity

internal val cataloguePosterWidth = 96.dp * 1.33f

@Composable
internal fun CatalogueMoviePlaceholder(grid: Boolean) {
    val opacity = rememberMoversLoadingOpacity()
    val tokens = MoversDesign.tokens
    val density = LocalDensity.current
    val titleHeight = with(density) { tokens.type.title.lineHeight.toDp() }
    val captionHeight = with(density) { tokens.type.caption.lineHeight.toDp() }
    val modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Loading movie" }

    if (grid) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
            MoversLoadingPlaceholder(opacity, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            MoversLoadingPlaceholder(opacity, Modifier.fillMaxWidth(0.8f).height(titleHeight))
            MoversLoadingPlaceholder(opacity, Modifier.width(40.dp).height(captionHeight))
        }
    } else {
        Column(modifier) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing * 1.5f),
            ) {
                MoversLoadingPlaceholder(opacity, Modifier.width(cataloguePosterWidth).aspectRatio(2f / 3f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing)) {
                    MoversLoadingPlaceholder(opacity, Modifier.fillMaxWidth(0.8f).height(titleHeight))
                    MoversLoadingPlaceholder(opacity, Modifier.width(40.dp).height(captionHeight))
                    repeat(3) {
                        MoversLoadingPlaceholder(opacity, Modifier.fillMaxWidth(0.9f).height(captionHeight))
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
