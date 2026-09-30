/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TracenpocheTokenizerTest {
    @Test
    fun nameSplittingAndImplicitDistanceMatchOfficialFixture() {
        val result = tokenize("AB abC2' var X for Y")

        assertEquals(
            listOf(
                text(TracenpocheTokenType.NAME, "A", 0, 1),
                text(TracenpocheTokenType.OPERATOR, "#", 1, 2),
                text(TracenpocheTokenType.NAME, "B", 1, 2),
                text(TracenpocheTokenType.OPERATOR, "#", 3, 8),
                text(TracenpocheTokenType.NAME, "abC2'", 3, 8),
                text(TracenpocheTokenType.OPERATOR, "#", 9, 12),
                text(TracenpocheTokenType.NAME, "var", 9, 12),
                text(TracenpocheTokenType.NAME, "X", 13, 14),
                text(TracenpocheTokenType.OPERATOR, "#", 15, 18),
                text(TracenpocheTokenType.NAME, "for", 15, 18),
                text(TracenpocheTokenType.NAME, "Y", 19, 20),
            ),
            result.tokens,
        )
        assertEquals(emptyList(), result.diagnostics)
    }

    @Test
    fun numbersAndMalformedNumberRecoveryMatchOfficialFixture() {
        val result = tokenize("12 3.5 6e-2 7e+ 8a .5")

        assertEquals(
            listOf(
                number(12.0, 0, 2),
                number(3.5, 3, 6),
                number(0.06, 7, 11),
                text(TracenpocheTokenType.NAME, "a", 18, 19),
                text(TracenpocheTokenType.OPERATOR, ".", 19, 20),
                number(5.0, 20, 21),
            ),
            result.tokens,
        )
        assertEquals(
            listOf(
                TracenpocheTokenizerDiagnostic(
                    type = "number",
                    value = "7e+",
                    message = "Bad exponent",
                ),
                TracenpocheTokenizerDiagnostic(
                    type = "number",
                    value = "7e+ 8a",
                    message = "Bad number",
                ),
                TracenpocheTokenizerDiagnostic(
                    type = "number",
                    value = "7e+ 8a",
                    message = "Bad number",
                ),
            ),
            result.diagnostics,
        )
    }

    @Test
    fun stringsEscapesAndUnterminatedRecoveryMatchOfficialFixture() {
        val result = tokenize(
            "'a\\n\\u0042' \"x\\t\" 'unterminated",
        )

        assertEquals(
            listOf(
                text(
                    type = TracenpocheTokenType.STRING,
                    value = "a\nB",
                    from = 0,
                    to = 11,
                ),
                text(
                    type = TracenpocheTokenType.STRING,
                    value = "x\t",
                    from = 12,
                    to = 17,
                ),
                text(
                    type = TracenpocheTokenType.STRING,
                    value = "unterminated",
                    from = 18,
                    to = 32,
                ),
            ),
            result.tokens,
        )
        assertEquals(
            listOf(
                TracenpocheTokenizerDiagnostic(
                    type = "string",
                    value = "unterminated",
                    message = "Unterminated string.",
                ),
            ),
            result.diagnostics,
        )
    }

    @Test
    fun commentsAndCustomCombiningOperatorsMatchOfficialFixture() {
        val result = tokenize(
            input = "A// ignored\n<= B && C := D",
            prefix = "=<>!+-*&|/%^#",
            suffix = "=<>&|",
        )

        assertEquals(
            listOf(
                text(TracenpocheTokenType.NAME, "A", 0, 1),
                text(TracenpocheTokenType.OPERATOR, "<=", 12, 14),
                text(TracenpocheTokenType.NAME, "B", 15, 16),
                text(TracenpocheTokenType.OPERATOR, "&&", 17, 19),
                text(TracenpocheTokenType.NAME, "C", 20, 21),
                text(TracenpocheTokenType.OPERATOR, ":", 22, 23),
                text(TracenpocheTokenType.OPERATOR, "=", 23, 24),
                text(TracenpocheTokenType.NAME, "D", 25, 26),
            ),
            result.tokens,
        )
        assertEquals(emptyList(), result.diagnostics)
    }

    @Test
    fun emptyInputAndResourceFailuresAreStructured() {
        assertEquals(
            TracenpocheTokenization(
                tokens = emptyList(),
                diagnostics = emptyList(),
            ),
            tokenize(""),
        )
        assertIs<
            GMResult.Err<TracenpocheTokenizerError.SourceLimitExceeded>
        >(
            TracenpocheTokenizer.tokenize(
                input = "AB",
                limits = TracenpocheTokenizerLimits(
                    maxSourceCharacters = 1,
                ),
            ),
        )
        assertIs<
            GMResult.Err<TracenpocheTokenizerError.TokenLimitExceeded>
        >(
            TracenpocheTokenizer.tokenize(
                input = "AB",
                limits = TracenpocheTokenizerLimits(maxTokens = 1),
            ),
        )
        assertIs<GMResult.Err<TracenpocheTokenizerError.InvalidLimits>>(
            TracenpocheTokenizer.tokenize(
                input = "",
                limits = TracenpocheTokenizerLimits(maxTokens = -1),
            ),
        )
    }

    private fun tokenize(
        input: String,
        prefix: String = "<>+-&",
        suffix: String = "=>&:",
    ): TracenpocheTokenization =
        assertIs<GMResult.Ok<TracenpocheTokenization>>(
            TracenpocheTokenizer.tokenize(
                input = input,
                prefix = prefix,
                suffix = suffix,
            ),
        ).value

    private fun text(
        type: TracenpocheTokenType,
        value: String,
        from: Int,
        to: Int,
    ): TracenpocheToken =
        TracenpocheToken(
            type = type,
            value = TracenpocheTokenValue.Text(value),
            from = from,
            to = to,
        )

    private fun number(
        value: Double,
        from: Int,
        to: Int,
    ): TracenpocheToken =
        TracenpocheToken(
            type = TracenpocheTokenType.NUMBER,
            value = TracenpocheTokenValue.Number(value),
            from = from,
            to = to,
        )
}
