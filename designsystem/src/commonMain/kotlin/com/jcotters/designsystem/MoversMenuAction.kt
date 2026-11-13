package com.jcotters.designsystem

data class MoversMenuAction(
    val label: String,
    val onClick: () -> Unit,
    val symbol: MoversSymbol? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)
