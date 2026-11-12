package com.jcotters.presentation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jcotters.contract.detail.domain.models.Movie
import com.jcotters.designsystem.MoversSearchField

@Composable
fun MovieSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MoversSearchField(
        value = value,
        onValueChange = onValueChange,
        label = "Search films",
        onSearch = onSearch,
        modifier = modifier,
        enabled = enabled,
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
        modifier = modifier.fillMaxWidth(),
        posterModifier = posterModifier,
        onOpen = onOpen,
        compact = compact,
        trailingContent = trailingContent,
    )
}
