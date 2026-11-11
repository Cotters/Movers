@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.jcotters.movers

import androidx.compose.runtime.Composable
import kotlin.native.Platform

@Composable
internal actual fun developmentToolsEnabled(): Boolean = Platform.isDebugBinary
