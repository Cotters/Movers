package com.jcotters.movers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jcotters.designsystem.MoversAlertDialog
import com.jcotters.designsystem.MoversTextButton

@Composable
internal expect fun rememberFeatureTourPreference(): FeatureTourPreference

@Composable
internal fun FeatureTour() {
    val preference = rememberFeatureTourPreference()
    var visible by rememberSaveable { mutableStateOf(!preference.seen) }
    if (visible) {
        FeatureTourDialog(onFinish = {
            preference.finish()
            visible = false
        })
    }
}

@Composable
internal fun FeatureTourDialog(onFinish: () -> Unit) {
    MoversAlertDialog(
        onDismissRequest = onFinish,
        title = { Text("Welcome to Movers") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TourFeature("Find films", "Browse Home or search for a film.")
                TourFeature("Make watchlists", "Add films with +, or swipe a row for quick actions.")
                TourFeature("Share with friends", "Share a list link to choose films together.")
            }
        },
        confirmButton = { MoversTextButton(onClick = onFinish) { Text("Get started") } },
        dismissButton = { MoversTextButton(onClick = onFinish) { Text("Skip") } },
    )
}

@Composable
private fun TourFeature(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}
