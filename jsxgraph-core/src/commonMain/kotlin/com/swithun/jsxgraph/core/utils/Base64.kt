/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/base64.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

sealed interface Base64Error {
    data class Utf8EncodingFailed(
        val cause: UTF8Error,
    ) : Base64Error

    data class InvalidInputLength(
        val sanitizedLength: Int,
        val message: String =
            "JSXGraph/utils/base64: Can't decode string " +
                "(invalid input length).",
    ) : Base64Error
}

object Base64 {
    // JSXGraph 1.13.3: src/utils/base64.js ->
    // JXG.Util.Base64.encode.
    fun encode(input: String): GMResult<String, Base64Error> {
        val encodedInput = when (val result = UTF8.encode(input)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    Base64Error.Utf8EncodingFailed(result.error),
                )
            }
        }
        val length = encodedInput.length
        val paddingLength = length % 3
        val output = StringBuilder(((length + 2) / 3) * 4)
        var index = 0

        while (index < length - paddingLength) {
            val binary =
                (getByte(encodedInput, index) shl 16) or
                    (getByte(encodedInput, index + 1) shl 8) or
                    getByte(encodedInput, index + 2)
            output.append(ALPHABET[binary shr 18])
            output.append(ALPHABET[(binary shr 12) and 63])
            output.append(ALPHABET[(binary shr 6) and 63])
            output.append(ALPHABET[binary and 63])
            index += 3
        }

        when (paddingLength) {
            1 -> {
                val binary = getByte(encodedInput, length - 1)
                output.append(ALPHABET[binary shr 2])
                output.append(ALPHABET[(binary shl 4) and 63])
                output.append(PAD)
                output.append(PAD)
            }

            2 -> {
                val binary =
                    (getByte(encodedInput, length - 2) shl 8) or
                        getByte(encodedInput, length - 1)
                output.append(ALPHABET[binary shr 10])
                output.append(ALPHABET[(binary shr 4) and 63])
                output.append(ALPHABET[(binary shl 2) and 63])
                output.append(PAD)
            }
        }

        return GMResult.Ok(output.toString())
    }

    // JSXGraph 1.13.3: src/utils/base64.js ->
    // JXG.Util.Base64.decode.
    fun decode(
        input: String,
        utf8: Boolean = false,
    ): GMResult<String, Base64Error> {
        val encodedInput = input.filter(::isBase64Character)
        var length = encodedInput.length
        if (length % 4 != 0) {
            return GMResult.Err(
                Base64Error.InvalidInputLength(length),
            )
        }

        var paddingLength = 0
        if (encodedInput.lastOrNull() == PAD) {
            paddingLength = 1
            if (encodedInput.getOrNull(length - 2) == PAD) {
                paddingLength = 2
            }
            length -= 4
        }

        val output = StringBuilder((encodedInput.length / 4) * 3)
        var index = 0
        while (index < length) {
            val binary =
                (getIndex(encodedInput, index) shl 18) or
                    (getIndex(encodedInput, index + 1) shl 12) or
                    (getIndex(encodedInput, index + 2) shl 6) or
                    getIndex(encodedInput, index + 3)
            appendJsCharCode(output, binary shr 16)
            appendJsCharCode(output, (binary shr 8) and 255)
            appendJsCharCode(output, binary and 255)
            index += 4
        }

        when (paddingLength) {
            1 -> {
                val binary =
                    (getIndex(encodedInput, length) shl 12) or
                        (getIndex(encodedInput, length + 1) shl 6) or
                        getIndex(encodedInput, length + 2)
                appendJsCharCode(output, binary shr 10)
                appendJsCharCode(output, (binary shr 2) and 255)
            }

            2 -> {
                val binary =
                    (getIndex(encodedInput, index) shl 6) or
                        getIndex(encodedInput, index + 1)
                appendJsCharCode(output, binary shr 4)
            }
        }

        val decoded = output.toString()
        return GMResult.Ok(if (utf8) UTF8.decode(decoded) else decoded)
    }

    // JSXGraph 1.13.3: src/utils/base64.js ->
    // JXG.Util.Base64.decodeAsArray.
    fun decodeAsArray(
        input: String,
    ): GMResult<List<Int>, Base64Error> =
        when (val result = decode(input)) {
            is GMResult.Ok -> GMResult.Ok(
                result.value.map { character -> character.code },
            )
            is GMResult.Err -> result
        }

    private fun getByte(
        string: String,
        index: Int,
    ): Int = string[index].code and 0xFF

    private fun getIndex(
        string: String,
        index: Int,
    ): Int = ALPHABET.indexOf(string[index])

    private fun appendJsCharCode(
        output: StringBuilder,
        code: Int,
    ) {
        output.append((code and 0xFFFF).toChar())
    }

    private fun isBase64Character(character: Char): Boolean =
        character in 'A'..'Z' ||
            character in 'a'..'z' ||
            character in '0'..'9' ||
            character == '+' ||
            character == '/' ||
            character == PAD

    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private const val PAD = '='
}
