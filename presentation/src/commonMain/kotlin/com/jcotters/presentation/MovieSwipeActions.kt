package com.jcotters.presentation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jcotters.designsystem.MoversSwipeAction
import com.jcotters.designsystem.MoversSwipeActions
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.add_to_list
import com.jcotters.presentation.resources.trash
import org.jetbrains.compose.resources.painterResource

/** Movie labels/actions adapt to the same reveal gesture used by watchlist overview rows. */
@Composable
fun MovieSwipeActions(
    movieTitle: String,
    onOpen: () -> Unit,
    onAddToList: (() -> Unit)?,
    onRemove: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier, () -> Unit) -> Unit,
) {
    MoversSwipeActions(
        onOpen = onOpen,
        startAction = onAddToList?.let { add ->
            MoversSwipeAction("Add to list", "Add $movieTitle to lists", add,
                icon = { Icon(painterResource(Res.drawable.add_to_list), contentDescription = null, tint = it) })
        },
        endAction = onRemove?.let { remove ->
            MoversSwipeAction("Remove", "Remove $movieTitle from list", remove, destructive = true,
                icon = { Icon(painterResource(Res.drawable.trash), contentDescription = null, tint = it) })
        },
        enabled = enabled,
        modifier = modifier,
        content = content,
    )
}
