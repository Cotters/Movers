package com.jcotters.watchlists.documents

private const val BYTE_MASK = 0xFF
private const val BOM_HIGH = 0xFE
private const val UTF16_UNIT_BYTES = 2
private const val BITS_PER_BYTE = 8

internal object ListDocumentEncoding {
    fun decode(bytes: ByteArray): String {
        require(bytes.size <= MAX_IMPORT_BYTES) { "Choose a file up to 5 MB." }
        val first = bytes.getOrNull(0)?.toInt()?.and(BYTE_MASK)
        val second = bytes.getOrNull(1)?.toInt()?.and(BYTE_MASK)
        val utf16 = when (first) {
            BYTE_MASK -> second == BOM_HIGH
            BOM_HIGH -> second == BYTE_MASK
            else -> false
        }
        return if (utf16) decodeUtf16(bytes, littleEndian = first == BYTE_MASK) else decodeUtf8(bytes)
    }

    private fun decodeUtf16(bytes: ByteArray, littleEndian: Boolean): String {
        require(bytes.size % UTF16_UNIT_BYTES == 0) { "Save the file as UTF-8 and retry." }
        return buildString {
            for (index in UTF16_UNIT_BYTES until bytes.size step UTF16_UNIT_BYTES) {
                val first = bytes[index].toInt() and BYTE_MASK
                val second = bytes[index + 1].toInt() and BYTE_MASK
                val code =
                    if (littleEndian) first or (second shl BITS_PER_BYTE) else (first shl BITS_PER_BYTE) or second
                append(code.toChar())
            }
        }
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
    } catch (error: CharacterCodingException) {
        throw IllegalArgumentException("Save the file as UTF-8 and retry.", error)
    }
}
