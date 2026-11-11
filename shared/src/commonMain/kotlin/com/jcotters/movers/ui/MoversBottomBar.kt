package com.jcotters.movers.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcotters.movers.resources.Res
import com.jcotters.movers.resources.navigation_home
import com.jcotters.movers.resources.navigation_profile
import com.jcotters.movers.resources.navigation_watchlists
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MoversBottomBar(selectedTab: TopLevelDestination, onSelectTab: (TopLevelDestination) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { tab ->
                val selected = selectedTab == tab
                val label = stringResource(tab.labelRes)
                val interactions = remember(tab) { MutableInteractionSource() }
                val pressed by interactions.collectIsPressedAsState()
                val focused by interactions.collectIsFocusedAsState()
                val tileColor by animateColorAsState(
                    targetValue = when {
                        selected -> MaterialTheme.colorScheme.primary
                        pressed || focused -> MaterialTheme.colorScheme.surfaceContainerHighest
                        else -> Color.Transparent
                    },
                    animationSpec = tween(180),
                    label = "Tab tile",
                )
                val iconColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "Tab icon",
                )
                val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(120), label = "Tab press")
                Box(
                    modifier = Modifier.weight(1f).height(56.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = interactions,
                            indication = null,
                            onClick = { onSelectTab(tab) },
                        )
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(com.jcotters.designsystem.MoversDesign.tokens.cornerRadius))
                            .background(tileColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(tab.iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

private val TopLevelDestination.labelRes: StringResource get() = when (this) {
    TopLevelDestination.Home -> Res.string.navigation_home
    TopLevelDestination.Watchlists -> Res.string.navigation_watchlists
    TopLevelDestination.Profile -> Res.string.navigation_profile
}

private val TopLevelDestination.iconRes: DrawableResource get() = when (this) {
    TopLevelDestination.Home -> Res.drawable.navigation_home
    TopLevelDestination.Watchlists -> Res.drawable.navigation_watchlists
    TopLevelDestination.Profile -> Res.drawable.navigation_profile
}

@Preview(widthDp = 360, heightDp = 56)
@Composable
private fun BottomBarPreview() {
    com.jcotters.designsystem.MoversDesignTheme { MoversBottomBar(TopLevelDestination.Watchlists, onSelectTab = {}) }
}
