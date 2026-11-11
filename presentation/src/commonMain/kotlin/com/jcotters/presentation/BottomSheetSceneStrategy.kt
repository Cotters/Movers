package com.jcotters.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.jcotters.designsystem.MoversModalSheet

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull()?.takeIf { it.metadata[MetadataKey] == true } ?: return null
        return BottomSheetScene(entry, entries.dropLast(1), onBack)
    }

    companion object {
        private const val MetadataKey = "movers.bottomSheet"
        val metadata: Map<String, Any> = mapOf(MetadataKey to true)
    }
}

private class BottomSheetScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onDismiss: () -> Unit,
) : OverlayScene<T> {
    override val key: Any = entry.contentKey
    override val entries = listOf(entry)
    override val overlaidEntries = previousEntries

    @OptIn(ExperimentalMaterial3Api::class)
    override val content: @Composable () -> Unit = {
        MoversModalSheet(onDismissRequest = onDismiss) {
            entry.Content()
        }
    }

    // Callback identities must not restart sheet transitions when underlying state changes.
    override fun equals(other: Any?) = other is BottomSheetScene<*> && key == other.key &&
        previousEntries.map { it.contentKey } == other.previousEntries.map { it.contentKey }
    override fun hashCode() = 31 * key.hashCode() + previousEntries.map { it.contentKey }.hashCode()
}
