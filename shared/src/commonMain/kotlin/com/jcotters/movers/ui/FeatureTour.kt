package com.jcotters.movers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal interface FeatureTourPreference {
    val seen: Boolean
    fun finish()
}

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
    AlertDialog(
        onDismissRequest = onFinish,
        title = { Text("Welcome to Movers") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TourFeature("Find films", "Browse Home or search for a film.")
                TourFeature("Make watchlists", "Add films with +, or swipe a row for quick actions.")
                TourFeature("Share with friends", "Share a list link to choose films together.")
            }
        },
        confirmButton = { TextButton(onClick = onFinish) { Text("Get started") } },
        dismissButton = { TextButton(onClick = onFinish) { Text("Skip") } },
    )
}

@Composable
private fun TourFeature(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}
