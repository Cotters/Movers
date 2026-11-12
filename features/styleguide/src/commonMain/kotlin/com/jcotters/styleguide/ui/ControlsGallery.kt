package com.jcotters.styleguide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.ButtonVariant
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversCheckboxRow
import com.jcotters.designsystem.MoversChoice
import com.jcotters.designsystem.MoversDesign
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversPanel
import com.jcotters.designsystem.MoversSearchField
import com.jcotters.designsystem.MoversSymbol
import com.jcotters.designsystem.MoversText
import com.jcotters.designsystem.MoversTextField

@Composable
internal fun ControlsGallery() {
    var action by rememberSaveable { mutableStateOf("Tap a control to try it.") }
    GalleryHeading("Buttons", "Primary, secondary, quiet and destructive")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoversButton("Create list", { action = "Demo list created." })
        MoversButton("Share", { action = "Demo share selected." }, variant = ButtonVariant.Secondary)
        MoversButton("Cancel", { action = "Demo action cancelled." }, variant = ButtonVariant.Quiet)
        MoversButton("Delete list", { action = "Demo deletion selected. No real data changed." }, variant = ButtonVariant.Destructive)
        MoversButton("Unavailable", {}, enabled = false)
        MoversButton("Saving", {}, loading = true)
    }
    MoversText(action, style = MoversDesign.tokens.type.caption, color = MoversDesign.tokens.colors.muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MoversIconButton(MoversSymbol.Add, "Add a demo film", { action = "Demo film added." })
        MoversIconButton(MoversSymbol.Trash, "Remove a demo film", { action = "Demo film removed." }, destructive = true)
        MoversIconButton(MoversSymbol.Search, "Search demo films", { action = "Demo search selected." })
        MoversIconButton(MoversSymbol.More, "Unavailable options", {}, enabled = false)
    }
    GalleryHeading("Fields", "Labelled forms and inline search · try focus, paste and keyboard actions")
    MoversTextField(rememberTextFieldState(), "List name", placeholder = "Friday films")
    var search by rememberSaveable { mutableStateOf("") }
    MoversSearchField(search, { search = it }, "Search films",
        onSearch = { action = "Demo search submitted." })
    MoversTextField(rememberTextFieldState(""), "Name with an error", placeholder = "Your list", error = "Enter a list name.")
    MoversTextField(rememberTextFieldState("Saved locally"), "Disabled field", enabled = false)
    MoversTextField(rememberTextFieldState(), "Paste titles or CSV", placeholder = "A film title, 2026", multiline = true)
    GalleryHeading("Selections", "Immediate feedback · no confirmation step")
    var selected by rememberSaveable { mutableStateOf("All films") }
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All films", "Unwatched", "Favourites").forEach { label ->
            MoversChoice(label, selected == label, { selected = label })
        }
    }
    var checked by rememberSaveable { mutableStateOf(true) }
    MoversPanel(Modifier.fillMaxWidth()) {
        MoversCheckboxRow("Friday films", checked, { checked = it }, description = "A shared demo list")
        MoversCheckboxRow("A very long watchlist name that should wrap without losing its selection control", false, {}, enabled = false)
    }
}
