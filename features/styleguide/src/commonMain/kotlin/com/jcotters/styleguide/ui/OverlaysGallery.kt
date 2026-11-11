package com.jcotters.styleguide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.jcotters.designsystem.MoversDialog
import com.jcotters.designsystem.MoversMenu
import com.jcotters.designsystem.MoversMenuAction
import com.jcotters.designsystem.MoversNotice
import com.jcotters.designsystem.MoversPanel
import com.jcotters.designsystem.MoversSheet
import com.jcotters.designsystem.MoversSymbol
import com.jcotters.designsystem.MoversText
import com.jcotters.designsystem.MoversTextField
import com.jcotters.designsystem.NoticeKind

@Composable
internal fun OverlaysGallery() {
    var dialog by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf("Try the list menu, dialog or sheet.") }
    GalleryHeading("List menu", "Anchored actions · keyboard navigation · destructive styling")
    MoversPanel(Modifier.fillMaxWidth()) {
        MoversMenu(listOf(
            MoversMenuAction("Change layout", { feedback = "Demo layout changed." }, MoversSymbol.Grid),
            MoversMenuAction("Rename list", { sheet = true }, MoversSymbol.More),
            MoversMenuAction("Unavailable action", {}, enabled = false),
            MoversMenuAction("Delete list", { dialog = true }, MoversSymbol.Trash, destructive = true),
        ))
        MoversText(feedback)
    }
    GalleryHeading("Modals", "Tap outside, Back/Escape or Close to dismiss")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoversButton("Confirmation dialog", { dialog = true }, variant = ButtonVariant.Secondary)
        MoversButton("Rename sheet", { sheet = true }, variant = ButtonVariant.Secondary)
    }
    GalleryHeading("Feedback", "Information, success and failure")
    var removed by rememberSaveable { mutableStateOf(true) }
    MoversNotice(if (removed) "Film removed from the demo list." else "Demo removal undone.",
        actionLabel = if (removed) "Undo" else null, onAction = if (removed) ({ removed = false }) else null)
    MoversNotice("Invitation copied.", kind = NoticeKind.Success)
    var failed by rememberSaveable { mutableStateOf(true) }
    MoversNotice(if (failed) "Couldn't save. Try again." else "Demo retry complete.",
        kind = if (failed) NoticeKind.Error else NoticeKind.Success,
        actionLabel = if (failed) "Retry" else null, onAction = { failed = false })

    MoversDialog("Delete demo list?", dialog, onDismiss = { dialog = false }) {
        MoversText("This is a preview. Your watchlists stay unchanged.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoversButton("Delete list", { dialog = false; feedback = "Demo list deleted." }, variant = ButtonVariant.Destructive)
            MoversButton("Cancel", { dialog = false }, variant = ButtonVariant.Secondary)
        }
    }
    val name = rememberTextFieldState("Friday films")
    MoversSheet("Rename demo list", sheet, onDismiss = { sheet = false }) {
        MoversTextField(name, "List name", placeholder = "Friday films")
        MoversButton("Save", { feedback = "Demo list renamed to ${name.text.toString().trim()}."; sheet = false },
            enabled = name.text.isNotBlank())
    }
}
