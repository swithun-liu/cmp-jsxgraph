/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/compressor.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

sealed interface DecompressError {
    data class Base64Decoding(
        val cause: Base64Error,
    ) : DecompressError

    data class ArchiveDecoding(
        val cause: UnzipError,
    ) : DecompressError

    data object MissingArchiveEntry : DecompressError

    data class InvalidUriEncoding(
        val index: Int,
        val reason: String,
    ) : DecompressError
}

object Compressor {
    // JSXGraph 1.13.3: src/compressor.js -> JXG.decompress.
    fun decompress(
        input: String,
        limits: UnzipLimits = UnzipLimits(),
    ): GMResult<String, DecompressError> {
        val bytes = when (val result = Base64.decodeAsArray(input)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    DecompressError.Base64Decoding(result.error),
                )
            }
        }
        val files = when (val result = Unzip(bytes, limits).unzip()) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    DecompressError.ArchiveDecoding(result.error),
                )
            }
        }
        val content = files.firstOrNull()?.content
            ?: return GMResult.Err(DecompressError.MissingArchiveEntry)
        return decodeUriComponent(content)
    }

    /*
     * CommonMain adaptation of JavaScript decodeURIComponent. Literal
     * characters pass through; percent-encoded runs are decoded as strict
     * UTF-8 and malformed input becomes a structured failure.
     */
    private fun decodeUriComponent(
        input: String,
    ): GMResult<String, DecompressError> {
        val output = StringBuilder(input.length)
        var index = 0
        while (index < input.length) {
            if (input[index] != '%') {
                output.append(input[index])
                index += 1
                continue
            }

            val runStart = index
            val bytes = mutableListOf<Int>()
            while (index < input.length && input[index] == '%') {
                if (index + 2 >= input.length) {
                    return GMResult.Err(
                        DecompressError.InvalidUriEncoding(
                            index = index,
                            reason = "Incomplete percent escape.",
                        ),
                    )
                }
                val high = hexValue(input[index + 1])
                val low = hexValue(input[index + 2])
                if (high < 0 || low < 0) {
                    return GMResult.Err(
                        DecompressError.InvalidUriEncoding(
                            index = index,
                            reason = "Percent escape is not hexadecimal.",
                        ),
                    )
                }
                bytes += (high shl 4) or low
                index += 3
            }
            when (
                val result = appendStrictUtf8(
                    bytes = bytes,
                    sourceIndex = runStart,
                    output = output,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(output.toString())
    }

    private fun appendStrictUtf8(
        bytes: List<Int>,
        sourceIndex: Int,
        output: StringBuilder,
    ): GMResult<Unit, DecompressError> {
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index]
            val length = when {
                first <= 0x7F -> 1
                first in 0xC2..0xDF -> 2
                first in 0xE0..0xEF -> 3
                first in 0xF0..0xF4 -> 4
                else -> {
                    return invalidUtf8(sourceIndex, index)
                }
            }
            if (index + length > bytes.size) {
                return invalidUtf8(sourceIndex, index)
            }
            for (continuationIndex in 1 until length) {
                if (
                    bytes[index + continuationIndex] !in
                    0x80..0xBF
                ) {
                    return invalidUtf8(sourceIndex, index)
                }
            }
            if (
                length == 3 &&
                (
                    first == 0xE0 && bytes[index + 1] < 0xA0 ||
                        first == 0xED && bytes[index + 1] >= 0xA0
                    )
            ) {
                return invalidUtf8(sourceIndex, index)
            }
            if (
                length == 4 &&
                (
                    first == 0xF0 && bytes[index + 1] < 0x90 ||
                        first == 0xF4 && bytes[index + 1] >= 0x90
                    )
            ) {
                return invalidUtf8(sourceIndex, index)
            }

            val codePoint = when (length) {
                1 -> first
                2 ->
                    ((first and 0x1F) shl 6) or
                        (bytes[index + 1] and 0x3F)
                3 ->
                    ((first and 0x0F) shl 12) or
                        ((bytes[index + 1] and 0x3F) shl 6) or
                        (bytes[index + 2] and 0x3F)
                else ->
                    ((first and 0x07) shl 18) or
                        ((bytes[index + 1] and 0x3F) shl 12) or
                        ((bytes[index + 2] and 0x3F) shl 6) or
                        (bytes[index + 3] and 0x3F)
            }
            if (codePoint <= 0xFFFF) {
                output.append(codePoint.toChar())
            } else {
                val adjusted = codePoint - 0x10000
                output.append((0xD800 + (adjusted shr 10)).toChar())
                output.append((0xDC00 + (adjusted and 0x3FF)).toChar())
            }
            index += length
        }
        return GMResult.Ok(Unit)
    }

    private fun invalidUtf8(
        sourceIndex: Int,
        byteIndex: Int,
    ): GMResult.Err<DecompressError.InvalidUriEncoding> =
        GMResult.Err(
            DecompressError.InvalidUriEncoding(
                index = sourceIndex + byteIndex * 3,
                reason = "Percent escapes are not valid UTF-8.",
            ),
        )

    private fun hexValue(character: Char): Int = when (character) {
        in '0'..'9' -> character.code - '0'.code
        in 'a'..'f' -> character.code - 'a'.code + 10
        in 'A'..'F' -> character.code - 'A'.code + 10
        else -> -1
    }
}
