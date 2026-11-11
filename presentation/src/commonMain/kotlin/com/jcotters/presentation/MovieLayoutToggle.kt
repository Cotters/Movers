package com.jcotters.presentation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.grid_view
import com.jcotters.presentation.resources.list_view
import org.jetbrains.compose.resources.painterResource

@Composable
fun MovieLayoutToggle(showGrid: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MoversIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painterResource(if (showGrid) Res.drawable.list_view else Res.drawable.grid_view),
            if (showGrid) "Show list" else "Show grid",
        )
    }
}
