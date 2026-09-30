/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EncodingBase64Test {
    @Test
    fun utf8EncodingMatchesOfficialFixture() {
        assertEquals(listOf(65, 66, 67), codes(okUtf8(UTF8.encode("ABC"))))
        assertEquals(
            listOf(97, 10, 98),
            codes(okUtf8(UTF8.encode("a\r\nb"))),
        )

        val unicode = "Grüße € 😀"
        val encoded = okUtf8(UTF8.encode(unicode))
        assertEquals(
            listOf(
                71, 114, 195, 188, 195, 159, 101, 32,
                226, 130, 172, 32, 240, 159, 152, 128,
            ),
            codes(encoded),
        )
        assertEquals(unicode, UTF8.decode(encoded))
    }

    @Test
    fun utf8MalformedInputAndAsciiMappingMatchOfficialFixture() {
        val malformed = binaryString(0xE2, 0x28, 0xA1, 0x41)
        assertEquals("", UTF8.decode(malformed))
        assertEquals(
            "A",
            UTF8.decode(binaryString(0x41, 0xE2, 0x28, 0x42)),
        )
        assertIs<GMResult.Err<UTF8Error.InvalidSurrogate>>(
            UTF8.encode("\uD800"),
        )
        assertIs<GMResult.Err<UTF8Error.InvalidSurrogate>>(
            UTF8.encode("\uDC00"),
        )

        val windows1252 = "€‚ƒ„…†‡ˆ‰Š‹ŒŽ"
        assertEquals(128.0, UTF8.asciiCharCodeAt(windows1252, 0))
        assertEquals(130.0, UTF8.asciiCharCodeAt(windows1252, 1))
        assertEquals(142.0, UTF8.asciiCharCodeAt(windows1252, 12))
        assertEquals(65.0, UTF8.asciiCharCodeAt("A", 0))
        assertEquals(256.0, UTF8.asciiCharCodeAt("Ā", 0))
        assertTrue(UTF8.asciiCharCodeAt("", 0).isNaN())
    }

    @Test
    fun base64EncodingAndDecodingMatchOfficialFixture() {
        assertEquals("TWFu", okBase64(Base64.encode("Man")))
        assertEquals("TWE=", okBase64(Base64.encode("Ma")))
        assertEquals("TQ==", okBase64(Base64.encode("M")))

        val unicode = "Grüße € 😀"
        val encoded = okBase64(Base64.encode(unicode))
        assertEquals("R3LDvMOfZSDigqwg8J+YgA==", encoded)
        assertEquals(
            listOf(
                71, 114, 195, 188, 195, 159, 101, 32,
                226, 130, 172, 32, 240, 159, 152, 128,
            ),
            codes(okBase64(Base64.decode(encoded))),
        )
        assertEquals(unicode, okBase64(Base64.decode(encoded, utf8 = true)))
        assertEquals("Man", okBase64(Base64.decode(" T!W\nF\tu ")))
        assertEquals(
            listOf(0, 1, 2, 253, 254, 255),
            okBase64(Base64.decodeAsArray("AAEC/f7/")),
        )
    }

    @Test
    fun base64FailuresAreStructuredAtTheUtilityBoundary() {
        val invalidLength = assertIs<
            GMResult.Err<Base64Error.InvalidInputLength>,
        >(Base64.decode("abc"))
        assertEquals(3, invalidLength.error.sanitizedLength)
        assertEquals(
            "JSXGraph/utils/base64: Can't decode string " +
                "(invalid input length).",
            invalidLength.error.message,
        )

        val invalidSurrogate = assertIs<
            GMResult.Err<Base64Error.Utf8EncodingFailed>,
        >(Base64.encode("\uD800"))
        assertIs<UTF8Error.InvalidSurrogate>(invalidSurrogate.error.cause)
    }

    private fun codes(value: String): List<Int> =
        value.map { character -> character.code }

    private fun binaryString(vararg codes: Int): String =
        buildString {
            for (code in codes) {
                append(code.toChar())
            }
        }

    private fun okUtf8(result: GMResult<String, UTF8Error>): String =
        assertIs<GMResult.Ok<String>>(result).value

    private fun <T> okBase64(result: GMResult<T, Base64Error>): T =
        assertIs<GMResult.Ok<T>>(result).value
}
