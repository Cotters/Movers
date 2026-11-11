package com.jcotters.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTextField
import com.jcotters.presentation.resources.Res
import com.jcotters.presentation.resources.search
import org.jetbrains.compose.resources.painterResource

@Composable
fun MovieSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MoversTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Search films") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) MoversTextButton(onClick = { onValueChange("") }, enabled = enabled) { Text("Clear") }
                MoversIconButton(onClick = onSearch, enabled = enabled && value.isNotBlank()) {
                    Icon(painterResource(Res.drawable.search), "Search films")
                }
            }
        },
    )
}

@Composable
fun MovieSearchResult(
    movie: Movie,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    posterModifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    MovieListRow(
        movie = movie,
        modifier = modifier.fillMaxWidth().then(if (compact) Modifier.heightIn(min = 116.dp) else Modifier.height(180.dp)),
        posterModifier = posterModifier,
        onOpen = onOpen,
        compact = compact,
        trailingContent = trailingContent,
    )
}
