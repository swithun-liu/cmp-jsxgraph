/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/encoding.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

sealed interface UTF8Error {
    data class InvalidSurrogate(
        val index: Int,
        val codeUnit: Int,
    ) : UTF8Error
}

object UTF8 {
    // JSXGraph 1.13.3: src/utils/encoding.js -> JXG.Util.UTF8.encode.
    fun encode(string: String): GMResult<String, UTF8Error> {
        val normalized = string.replace("\r\n", "\n")
        val output = StringBuilder(normalized.length)
        var index = 0

        while (index < normalized.length) {
            val codeUnit = normalized[index].code
            when {
                codeUnit < 0x80 -> {
                    output.append(codeUnit.toChar())
                }

                codeUnit < 0x800 -> {
                    output.append(((codeUnit shr 6) or 0xC0).toChar())
                    output.append(((codeUnit and 0x3F) or 0x80).toChar())
                }

                codeUnit in HIGH_SURROGATE_RANGE -> {
                    if (index + 1 >= normalized.length) {
                        return GMResult.Err(
                            UTF8Error.InvalidSurrogate(index, codeUnit),
                        )
                    }
                    val lowSurrogate = normalized[index + 1].code
                    if (lowSurrogate !in LOW_SURROGATE_RANGE) {
                        return GMResult.Err(
                            UTF8Error.InvalidSurrogate(index, codeUnit),
                        )
                    }
                    val codePoint =
                        0x10000 +
                            ((codeUnit - HIGH_SURROGATE_START) shl 10) +
                            (lowSurrogate - LOW_SURROGATE_START)
                    output.append(((codePoint shr 18) or 0xF0).toChar())
                    output.append(
                        (((codePoint shr 12) and 0x3F) or 0x80).toChar(),
                    )
                    output.append(
                        (((codePoint shr 6) and 0x3F) or 0x80).toChar(),
                    )
                    output.append(((codePoint and 0x3F) or 0x80).toChar())
                    index += 1
                }

                codeUnit in LOW_SURROGATE_RANGE -> {
                    return GMResult.Err(
                        UTF8Error.InvalidSurrogate(index, codeUnit),
                    )
                }

                else -> {
                    output.append(((codeUnit shr 12) or 0xE0).toChar())
                    output.append(
                        (((codeUnit shr 6) and 0x3F) or 0x80).toChar(),
                    )
                    output.append(((codeUnit and 0x3F) or 0x80).toChar())
                }
            }
            index += 1
        }

        return GMResult.Ok(output.toString())
    }

    /*
     * JSXGraph 1.13.3: src/utils/encoding.js ->
     * JXG.Util.UTF8.decode.
     *
     * The state machine is the MIT-licensed UTF-8 decoder by
     * Bjoern Hoehrmann used by upstream. Like JSXGraph, malformed or
     * incomplete suffixes are omitted instead of being replaced.
     */
    fun decode(utftext: String): String {
        var state = UTF8_ACCEPT
        var codePoint = 0
        val output = StringBuilder(utftext.length)

        for (character in utftext) {
            val charCode = character.code
            if (charCode !in 0..0xFF) {
                break
            }
            val type = UTF8D[charCode]

            codePoint =
                if (state != UTF8_ACCEPT) {
                    (charCode and 0x3F) or (codePoint shl 6)
                } else {
                    (0xFF shr type) and charCode
                }

            state = UTF8D[256 + state + type]
            if (state == UTF8_REJECT) {
                break
            }
            if (state == UTF8_ACCEPT) {
                if (codePoint > 0xFFFF) {
                    output.append((0xD7C0 + (codePoint shr 10)).toChar())
                    output.append(
                        (LOW_SURROGATE_START + (codePoint and 0x3FF)).toChar(),
                    )
                } else {
                    output.append(codePoint.toChar())
                }
            }
        }
        return output.toString()
    }

    // JSXGraph 1.13.3: src/utils/encoding.js ->
    // JXG.Util.UTF8.asciiCharCodeAt.
    fun asciiCharCodeAt(
        string: String,
        index: Int,
    ): Double {
        if (index !in string.indices) {
            return Double.NaN
        }
        val code = string[index].code
        return (WINDOWS_1252_CODES[code] ?: code).toDouble()
    }

    private const val UTF8_ACCEPT = 0
    private const val UTF8_REJECT = 12
    private const val HIGH_SURROGATE_START = 0xD800
    private const val LOW_SURROGATE_START = 0xDC00
    private val HIGH_SURROGATE_RANGE = 0xD800..0xDBFF
    private val LOW_SURROGATE_RANGE = 0xDC00..0xDFFF

    private val WINDOWS_1252_CODES = mapOf(
        8364 to 128,
        8218 to 130,
        402 to 131,
        8222 to 132,
        8230 to 133,
        8224 to 134,
        8225 to 135,
        710 to 136,
        8240 to 137,
        352 to 138,
        8249 to 139,
        338 to 140,
        381 to 142,
        8216 to 145,
        8217 to 146,
        8220 to 147,
        8221 to 148,
        8226 to 149,
        8211 to 150,
        8212 to 151,
        732 to 152,
        8482 to 153,
        353 to 154,
        8250 to 155,
        339 to 156,
        382 to 158,
        376 to 159,
    )

    private val UTF8D = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 8, 8, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 10, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        3, 4, 3, 3, 11, 6, 6, 6, 5, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8,
        0, 12, 24, 36, 60, 96, 84, 12, 12, 12, 48, 72,
        12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
        12, 0, 12, 12, 12, 12, 12, 0, 12, 0, 12, 12,
        12, 24, 12, 12, 12, 12, 12, 24, 12, 24, 12, 12,
        12, 12, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12,
        12, 24, 12, 12, 12, 12, 12, 12, 12, 24, 12, 12,
        12, 12, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12,
        12, 36, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12,
        12, 36, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    )
}
