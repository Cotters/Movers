package com.jcotters.watchlists.documents

internal data class DelimitedTable(val rows: List<List<String>>, val problem: String? = null)

/** Reads quoted fields while retaining complete records before a malformed section. */
internal class DelimitedTableParser(private val text: String, private val separator: Char) {
    private val rows = mutableListOf<List<String>>()
    private val cells = mutableListOf<String>()
    private val field = StringBuilder()
    private var quoted = false
    private var closed = false
    private var index = 0
    private var recordStart = 0

    fun read(): DelimitedTable {
        while (index < text.length) {
            val valid = if (quoted) readQuoted(text[index]) else readUnquoted(text[index])
            if (!valid) break
            index++
        }
        val problem = index < text.length || quoted
        val hasRecord = field.isNotEmpty() || cells.isNotEmpty() || closed
        if (!problem && hasRecord) finishRecord()
        return DelimitedTable(rows, if (problem) text.substring(recordStart) else null)
    }

    private fun readQuoted(character: Char): Boolean {
        if (character != '"') {
            field.append(character)
        } else if (text.getOrNull(index + 1) == '"') {
            field.append('"')
            index++
        } else {
            quoted = false
            closed = true
        }
        return true
    }

    private fun readUnquoted(character: Char): Boolean = when {
        character == separator -> {
            finishField()
            true
        }

        character == '\n' || character == '\r' -> {
            finishRecord()
            if (character == '\r' && text.getOrNull(index + 1) == '\n') index++
            recordStart = index + 1
            true
        }

        character == '"' && field.isEmpty() && !closed -> {
            quoted = true
            true
        }

        closed && !character.isWhitespace() -> false
        character == '"' -> false
        else -> {
            if (!closed) field.append(character)
            true
        }
    }

    private fun finishField() {
        require(cells.size <= MAX_IMPORT_ENTRIES) { "Import up to 10,000 films at a time." }
        cells += field.toString()
        field.clear()
        closed = false
    }

    private fun finishRecord() {
        finishField()
        rows += cells.toList()
        cells.clear()
        require(rows.size <= MAX_IMPORT_ENTRIES + 1) { "Import up to 10,000 films at a time." }
    }
}
