package com.jcotters.designsystem

/** Original icon geometry in a 24-unit viewport; cached once rather than rebuilt during drawing. */
internal data class IconStroke(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    companion object {
        private val pin = listOf(
            IconStroke(8f, 3f, 16f, 3f), IconStroke(9f, 3f, 9f, 9f), IconStroke(15f, 3f, 15f, 9f),
            IconStroke(9f, 9f, 6f, 14f), IconStroke(15f, 9f, 18f, 14f), IconStroke(6f, 14f, 18f, 14f),
            IconStroke(12f, 14f, 12f, 22f),
        )
        val lines = mapOf(
            MoversSymbol.Add to listOf(IconStroke(5f, 12f, 19f, 12f), IconStroke(12f, 5f, 12f, 19f)),
            MoversSymbol.Check to listOf(IconStroke(5f, 12f, 10f, 17f), IconStroke(10f, 17f, 19f, 7f)),
            MoversSymbol.Close to listOf(IconStroke(6f, 6f, 18f, 18f), IconStroke(6f, 18f, 18f, 6f)),
            MoversSymbol.Back to listOf(
                IconStroke(18f, 12f, 5f, 12f), IconStroke(5f, 12f, 11f, 6f), IconStroke(5f, 12f, 11f, 18f),
            ),
            MoversSymbol.Trash to listOf(
                IconStroke(4f, 6f, 20f, 6f), IconStroke(9f, 3f, 15f, 3f),
                IconStroke(6f, 6f, 7f, 21f), IconStroke(7f, 21f, 17f, 21f), IconStroke(17f, 21f, 18f, 6f),
                IconStroke(10f, 10f, 10f, 17f), IconStroke(14f, 10f, 14f, 17f),
            ),
            MoversSymbol.Sort to listOf(
                IconStroke(4f, 6f, 20f, 6f), IconStroke(7f, 12f, 17f, 12f), IconStroke(10f, 18f, 14f, 18f),
            ),
            MoversSymbol.Pin to pin,
            MoversSymbol.PinOff to pin + IconStroke(3f, 3f, 21f, 21f),
        )
    }
}
