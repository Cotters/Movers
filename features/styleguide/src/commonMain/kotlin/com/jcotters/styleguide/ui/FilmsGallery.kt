package com.jcotters.styleguide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.ButtonVariant
import com.jcotters.designsystem.MoversActivity
import com.jcotters.designsystem.MoversAvatarButton
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversCheckboxRow
import com.jcotters.designsystem.MoversDesign
import com.jcotters.designsystem.MoversDialog
import com.jcotters.designsystem.MoversFilmCard
import com.jcotters.designsystem.MoversFilmRow
import com.jcotters.designsystem.MoversNotice
import com.jcotters.designsystem.MoversPanel
import com.jcotters.designsystem.MoversRating
import com.jcotters.designsystem.MoversSheet
import com.jcotters.designsystem.MoversText
import com.jcotters.designsystem.MoversWatchlistSummary

private val demoTitles = listOf("Midnight Bloom", "The Long Way Home Through Every City We Once Knew")

@Composable
internal fun FilmsGallery() {
    var detailTitle by rememberSaveable { mutableStateOf(demoTitles[0]) }
    var detailVisible by rememberSaveable { mutableStateOf(false) }
    var addingTitle by rememberSaveable { mutableStateOf(demoTitles[0]) }
    var addingVisible by rememberSaveable { mutableStateOf(false) }
    var contributorVisible by rememberSaveable { mutableStateOf(false) }
    var fridayFilms by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var weekendFilms by rememberSaveable { mutableStateOf(listOf(demoTitles[0])) }
    fun openDetail(title: String) {
        detailTitle = title; detailVisible = true
    }

    fun openPicker(title: String) {
        addingTitle = title; addingVisible = true
    }
    GalleryHeading("Posters", "Synthetic films and original placeholder artwork")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        demoTitles.forEachIndexed { index, title ->
            MoversFilmCard(
                title, "2026", { openDetail(title) }, poster = { DemoPoster(index, it) },
                modifier = Modifier.weight(1f), onAdd = { openPicker(title) },
                footerContent = {
                    MoversAvatarButton("Avery", "Added by Avery", { contributorVisible = true })
                })
        }
    }
    GalleryHeading("Watchlist row", "Long title, synopsis and bottom-right contributor")
    MoversFilmRow(
        demoTitles[1],
        "2026 · 114 min",
        "Two friends follow a forgotten route across the places that shaped them.",
        onOpen = { openDetail(demoTitles[1]) },
        poster = { DemoPoster(1, it) },
        posterWidth = 96.dp * 1.2f,
        footerContent = {
            MoversAvatarButton("Avery", "Added by Avery", { contributorVisible = true })
        })
    MoversRating(8.4, "1,286")
    GalleryHeading("Shared list", "Names and initials, with room for the films")
    MoversWatchlistSummary("Friday films", 12, listOf("Avery", "Morgan", "Casey"))
    GalleryHeading("Activity", "Added/imported green; removed red")
    MoversActivity("Avery", demoTitles[0], "added", "Today · 19:42", removed = false)
    MoversActivity("Morgan", demoTitles[1], "removed", "Yesterday · 18:10", removed = true)
    GalleryHeading("Empty list")
    MoversPanel(Modifier.fillMaxWidth()) {
        MoversText("No films yet.", style = MoversDesign.tokens.type.title)
        MoversButton("Find a film", { openPicker(demoTitles[0]) }, variant = ButtonVariant.Secondary)
    }
    MoversSheet("Add to list", addingVisible, onDismiss = { addingVisible = false }) {
        MoversText(addingTitle, style = MoversDesign.tokens.type.label)
        MoversCheckboxRow("Friday films", addingTitle in fridayFilms, { checked ->
            fridayFilms = if (checked) (fridayFilms + addingTitle).distinct() else fridayFilms - addingTitle
        }, description = "Avery, Morgan and Casey")
        MoversCheckboxRow("Weekend picks", addingTitle in weekendFilms, { checked ->
            weekendFilms = if (checked) (weekendFilms + addingTitle).distinct() else weekendFilms - addingTitle
        }, description = "Personal demo list")
        MoversNotice("Selections apply immediately to these demo lists.")
    }
    MoversDialog(detailTitle, detailVisible, onDismiss = { detailVisible = false }) {
        MoversText("2026 · 114 min", color = MoversDesign.tokens.colors.muted)
        MoversRating(8.4, "1,286")
        MoversText("Two friends follow a forgotten route across the places that shaped them.")
        MoversButton("Add to list", { detailVisible = false; openPicker(detailTitle) })
    }
    MoversDialog("Added by Avery", contributorVisible, onDismiss = { contributorVisible = false }) {
        MoversText("Demo contributor · Today, 19:42")
    }
}

@Composable
private fun DemoPoster(index: Int, modifier: Modifier) {
    val palette =
        if (index == 0) listOf(Color(0xFF143E3F), Color(0xFF16252E)) else listOf(Color(0xFF493641), Color(0xFF201E32))
    Box(
        modifier.background(Brush.verticalGradient(palette)).drawBehind {
            drawCircle(
                Color(0xFFB6E4D4).copy(alpha = 0.22f), size.width * 0.55f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.35f)
            )
            drawLine(
                Color.White.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f),
                androidx.compose.ui.geometry.Offset(size.width, size.height * 0.3f), strokeWidth = size.width * 0.03f
            )
        }.padding(12.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        MoversText(
            if (index == 0) "MIDNIGHT\nBLOOM" else "THE LONG\nWAY HOME",
            style = MoversDesign.tokens.type.label,
            color = Color.White,
            maxLines = 2
        )
    }
}
