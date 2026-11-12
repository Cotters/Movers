package com.jcotters.watchlists.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversDropdownMenu
import com.jcotters.designsystem.MoversDropdownMenuItem
import com.jcotters.designsystem.MoversIcon
import com.jcotters.designsystem.MoversIconButton
import com.jcotters.designsystem.MoversSymbol
import com.jcotters.watchlists.preferences.WatchlistSort
import com.jcotters.watchlists.preferences.WatchlistSortField

@Composable
internal fun WatchlistSortMenu(sort: WatchlistSort, enabled: Boolean, onChange: (WatchlistSort) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    MoversDropdownMenu(expanded, { expanded = it }, anchor = {
        MoversIconButton(MoversSymbol.Sort,
            "Sort films: ${sort.field.label}, ${if (sort.ascending) "ascending" else "descending"}",
            onClick = { expanded = !expanded }, enabled = enabled)
    }) {
        WatchlistSortField.entries.forEach { field ->
            SortOption(field.label, sort.field == field, enabled) { onChange(sort.copy(field = field)) }
        }
        HorizontalDivider()
        SortOption("Ascending", sort.ascending, enabled) { onChange(sort.copy(ascending = true)) }
        SortOption("Descending", !sort.ascending, enabled) { onChange(sort.copy(ascending = false)) }
    }
}

@Composable
private fun SortOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    MoversDropdownMenuItem(text = { Text(label) }, onClick = onClick, enabled = enabled,
        modifier = Modifier.semantics { this.selected = selected }, closeOnClick = false,
        leadingIcon = { if (selected) MoversIcon(MoversSymbol.Check) else Box(Modifier.size(24.dp)) })
}
