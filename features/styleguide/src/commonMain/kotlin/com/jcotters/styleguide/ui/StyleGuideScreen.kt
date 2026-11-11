package com.jcotters.styleguide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcotters.navigation.Navigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StyleGuideRoute(navigator: Navigator) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Style guide") }, navigationIcon = {
            TextButton(onClick = navigator::goBack) { Text("Back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Movers components")
            Text("Experimental · local demo data only")
        }
    }
}
