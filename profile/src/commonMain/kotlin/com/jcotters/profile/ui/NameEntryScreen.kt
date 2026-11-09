package com.jcotters.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NameEntryScreen(busy: Boolean, error: String?, initialName: String, onSave: (String) -> Unit) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).safeDrawingPadding().imePadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("What should we call you?", style = MaterialTheme.typography.headlineMedium)
            Text("Your name appears beside films you add to shared lists. No account or password needed.", Modifier.padding(vertical = 16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { onSave(name) }, enabled = !busy && name.isNotBlank(), modifier = Modifier.padding(top = 16.dp)) { Text(if (busy) "Saving…" else "Continue") }
        }
    }
}
