package com.jcotters.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversButton
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.arrow_up
import org.jetbrains.compose.resources.painterResource

@Composable
fun ScrollToTopButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = scaleIn(), exit = scaleOut()) {
        MoversButton(onClick = onClick) {
            Icon(
                painter = painterResource(Res.drawable.arrow_up),
                contentDescription = "Scroll to top button",
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
