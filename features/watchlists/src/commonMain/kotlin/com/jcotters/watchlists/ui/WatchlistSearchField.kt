package com.jcotters.watchlists.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.close
import com.jcotters.presentation.resources.search
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun WatchlistSearchHeader(
    scrollState: WatchlistHeaderScrollState,
    query: String,
    onQueryChange: (String) -> Unit,
    summary: @Composable () -> Unit,
) {
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(scrollState.visible) {
        if (!scrollState.visible && searchFocused) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    AnimatedVisibility(
        visible = scrollState.visible,
        enter = expandVertically(tween(240, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(200)),
        exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(160)),
    ) {
        Column {
            WatchlistSearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.onFocusChanged { searchFocused = it.hasFocus }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            summary()
        }
    }
}

@Composable
internal fun WatchlistSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Search this list") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        leadingIcon = { Icon(painterResource(Res.drawable.search), contentDescription = null) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(painterResource(Res.drawable.close), contentDescription = "Clear search")
                }
            }
        } else null,
    )
}
