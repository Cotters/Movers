package com.jcotters.styleguide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcotters.designsystem.ButtonVariant
import com.jcotters.designsystem.ComponentDensity
import com.jcotters.designsystem.MoversButton
import com.jcotters.designsystem.MoversChoice
import com.jcotters.designsystem.MoversDesign
import com.jcotters.designsystem.MoversDesignTheme
import com.jcotters.designsystem.MoversPanel
import com.jcotters.designsystem.MoversText
import com.jcotters.designsystem.MoversTextButton
import com.jcotters.designsystem.MoversTopBar
import com.jcotters.designsystem.ThemeMode
import com.jcotters.navigation.Navigator
import com.jcotters.styleguide.GuideSection
import com.jcotters.styleguide.StyleGuideState
import com.jcotters.styleguide.StyleGuideViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StyleGuideRoute(navigator: Navigator) {
    val viewModel: StyleGuideViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StyleGuideScreen(state, viewModel::theme, viewModel::corners, viewModel::density, viewModel::section, viewModel::reset, navigator::goBack)
}

@Composable
internal fun StyleGuideScreen(
    state: StyleGuideState,
    onTheme: (ThemeMode) -> Unit,
    onCorners: (Dp) -> Unit,
    onDensity: (ComponentDensity) -> Unit,
    onSection: (GuideSection) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    // Development chrome retains the real app theme; preview overrides never reach its bars.
    Scaffold(topBar = {
        MoversTopBar(title = { Text("Style guide") }, navigationIcon = {
            MoversTextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        MoversDesignTheme(state.settings) {
            val tokens = MoversDesign.tokens
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).background(tokens.colors.background)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing),
                ) {
                    GuideSection.entries.forEach { section ->
                        MoversChoice(section.name, state.section == section, { onSection(section) })
                    }
                }
                key(state.resetVersion) {
                    val saved = rememberSaveableStateHolder()
                    saved.SaveableStateProvider(state.section.name) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing * 2f),
                        ) {
                            StyleControls(state, onTheme, onCorners, onDensity, onReset)
                            when (state.section) {
                                GuideSection.Basics -> FoundationsGallery()
                                GuideSection.Controls -> ControlsGallery()
                                GuideSection.Overlays -> OverlaysGallery()
                                GuideSection.Films -> FilmsGallery()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleControls(
    state: StyleGuideState,
    onTheme: (ThemeMode) -> Unit,
    onCorners: (Dp) -> Unit,
    onDensity: (ComponentDensity) -> Unit,
    onReset: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    MoversButton(if (expanded) "Hide styling controls" else "Adjust style", { expanded = !expanded }, variant = ButtonVariant.Quiet)
    if (expanded) {
        MoversPanel(Modifier.fillMaxWidth()) {
            MoversText("Preview only · local demo data", style = MoversDesign.tokens.type.caption, color = MoversDesign.tokens.colors.muted)
            MoversText("Theme", style = MoversDesign.tokens.type.label)
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { MoversChoice(it.name, state.settings.theme == it, { onTheme(it) }) }
            }
            MoversText("Corners", style = MoversDesign.tokens.type.label)
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8.dp, 12.dp, 16.dp).forEach { radius ->
                    MoversChoice("${radius.value.toInt()} dp", state.settings.cornerRadius == radius, { onCorners(radius) })
                }
            }
            MoversText("Spacing", style = MoversDesign.tokens.type.label)
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComponentDensity.entries.forEach { MoversChoice(it.name, state.settings.density == it, { onDensity(it) }) }
            }
            MoversButton("Reset examples and style", onReset, variant = ButtonVariant.Secondary)
        }
    }
}
