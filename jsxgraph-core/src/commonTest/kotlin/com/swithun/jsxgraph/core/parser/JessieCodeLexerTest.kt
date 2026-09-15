package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JessieCodeLexerTest {
    @Test
    fun generatedRuleTableMatchesOfficialTokenStream() {
        val source =
            " \t// line\r\n" +
                "/*multi\ncomment*/\n" +
                "123.5e-2 12. .25 42 \"a\\\"b\" 'c\\'d' " +
                "if else while do for function map use return delete " +
                "true false null Infinity NaN -> => << >> { } ; # ? : " +
                ". [ ] ( ) ! ^ ** * / % + - <= < >= > == ~= != && || " +
                "= , name_1 \$value @"

        val actual = tokens(JessieCodeLexer().tokenize(source))
            .map { it.type to it.lexeme }

        assertEquals(
            listOf(
                JessieCodeTokenType.NUMBER to "123.5e-2",
                JessieCodeTokenType.NUMBER to "12.",
                JessieCodeTokenType.NUMBER to ".25",
                JessieCodeTokenType.NUMBER to "42",
                JessieCodeTokenType.STRING to "\"a\\\"b\"",
                JessieCodeTokenType.STRING to "'c\\'d'",
                JessieCodeTokenType.IF to "if",
                JessieCodeTokenType.ELSE to "else",
                JessieCodeTokenType.WHILE to "while",
                JessieCodeTokenType.DO to "do",
                JessieCodeTokenType.FOR to "for",
                JessieCodeTokenType.FUNCTION to "function",
                JessieCodeTokenType.MAP to "map",
                JessieCodeTokenType.USE to "use",
                JessieCodeTokenType.RETURN to "return",
                JessieCodeTokenType.DELETE to "delete",
                JessieCodeTokenType.TRUE to "true",
                JessieCodeTokenType.FALSE to "false",
                JessieCodeTokenType.NULL to "null",
                JessieCodeTokenType.INFINITY to "Infinity",
                JessieCodeTokenType.NAN to "NaN",
                JessieCodeTokenType.ARROW to "->",
                JessieCodeTokenType.ARROW to "=>",
                JessieCodeTokenType.SHIFT_LEFT to "<<",
                JessieCodeTokenType.SHIFT_RIGHT to ">>",
                JessieCodeTokenType.LEFT_BRACE to "{",
                JessieCodeTokenType.RIGHT_BRACE to "}",
                JessieCodeTokenType.SEMICOLON to ";",
                JessieCodeTokenType.HASH to "#",
                JessieCodeTokenType.QUESTION to "?",
                JessieCodeTokenType.COLON to ":",
                JessieCodeTokenType.DOT to ".",
                JessieCodeTokenType.LEFT_BRACKET to "[",
                JessieCodeTokenType.RIGHT_BRACKET to "]",
                JessieCodeTokenType.LEFT_PARENTHESIS to "(",
                JessieCodeTokenType.RIGHT_PARENTHESIS to ")",
                JessieCodeTokenType.NOT to "!",
                JessieCodeTokenType.EXPONENT to "^",
                JessieCodeTokenType.EXPONENT to "**",
                JessieCodeTokenType.MULTIPLY to "*",
                JessieCodeTokenType.DIVIDE to "/",
                JessieCodeTokenType.MODULO to "%",
                JessieCodeTokenType.PLUS to "+",
                JessieCodeTokenType.MINUS to "-",
                JessieCodeTokenType.LESS_EQUAL to "<=",
                JessieCodeTokenType.LESS to "<",
                JessieCodeTokenType.GREATER_EQUAL to ">=",
                JessieCodeTokenType.GREATER to ">",
                JessieCodeTokenType.EQUAL to "==",
                JessieCodeTokenType.APPROX_EQUAL to "~=",
                JessieCodeTokenType.NOT to "!",
                JessieCodeTokenType.ASSIGN to "=",
                JessieCodeTokenType.AND to "&&",
                JessieCodeTokenType.OR to "||",
                JessieCodeTokenType.ASSIGN to "=",
                JessieCodeTokenType.COMMA to ",",
                JessieCodeTokenType.IDENTIFIER to "name_1",
                JessieCodeTokenType.IDENTIFIER to "\$value",
                JessieCodeTokenType.INVALID to "@",
                JessieCodeTokenType.EOF to "",
            ),
            actual,
        )
    }

    @Test
    fun numberAndUnterminatedStringBoundariesMatchOfficialLexer() {
        val source = ".123foo 1e 123.e2 123.e-2 \"unterminated\nnext"

        val actual = tokens(JessieCodeLexer().tokenize(source))
            .map { it.type to it.lexeme }

        assertEquals(
            listOf(
                JessieCodeTokenType.DOT to ".",
                JessieCodeTokenType.NUMBER to "123",
                JessieCodeTokenType.IDENTIFIER to "foo",
                JessieCodeTokenType.NUMBER to "1",
                JessieCodeTokenType.IDENTIFIER to "e",
                JessieCodeTokenType.NUMBER to "123.",
                JessieCodeTokenType.IDENTIFIER to "e2",
                JessieCodeTokenType.NUMBER to "123.",
                JessieCodeTokenType.IDENTIFIER to "e",
                JessieCodeTokenType.MINUS to "-",
                JessieCodeTokenType.NUMBER to "2",
                JessieCodeTokenType.INVALID to "\"",
                JessieCodeTokenType.IDENTIFIER to "unterminated",
                JessieCodeTokenType.IDENTIFIER to "next",
                JessieCodeTokenType.EOF to "",
            ),
            actual,
        )
    }

    @Test
    fun keywordAndIdentifierBoundariesUseOfficialAsciiRules() {
        val source = "ifx if\$ if\u00E9 a\$b"

        val actual = tokens(JessieCodeLexer().tokenize(source))
            .map { it.type to it.lexeme }

        assertEquals(
            listOf(
                JessieCodeTokenType.IDENTIFIER to "ifx",
                JessieCodeTokenType.IF to "if",
                JessieCodeTokenType.IDENTIFIER to "\$",
                JessieCodeTokenType.IF to "if",
                JessieCodeTokenType.INVALID to "\u00E9",
                JessieCodeTokenType.IDENTIFIER to "a",
                JessieCodeTokenType.IDENTIFIER to "\$b",
                JessieCodeTokenType.EOF to "",
            ),
            actual,
        )
    }

    @Test
    fun sourceLocationsMatchOfficialJisonRanges() {
        val ignoredPrefix = " \t// line\r\n/*multi\ncomment*/\n"
        val tokens = tokens(
            JessieCodeLexer().tokenize(ignoredPrefix + "123.5e-2"),
        )

        assertEquals(
            JessieCodeSourceLocation(
                start = JessieCodeSourcePosition(
                    offset = 29,
                    line = 4,
                    column = 0,
                ),
                end = JessieCodeSourcePosition(
                    offset = 37,
                    line = 4,
                    column = 8,
                ),
            ),
            tokens[0].location,
        )
        assertEquals(
            JessieCodeSourceLocation(
                start = JessieCodeSourcePosition(
                    offset = 37,
                    line = 4,
                    column = 8,
                ),
                end = JessieCodeSourcePosition(
                    offset = 37,
                    line = 4,
                    column = 8,
                ),
            ),
            tokens[1].location,
        )
    }

    @Test
    fun multilineStringLocationMatchesOfficialJisonRange() {
        val tokens = tokens(
            JessieCodeLexer().tokenize("\"line 1\r\nline 2\" next"),
        )

        assertEquals(
            JessieCodeSourceLocation(
                start = JessieCodeSourcePosition(0, 1, 0),
                end = JessieCodeSourcePosition(16, 2, 7),
            ),
            tokens[0].location,
        )
        assertEquals(
            JessieCodeSourceLocation(
                start = JessieCodeSourcePosition(17, 2, 8),
                end = JessieCodeSourcePosition(21, 2, 12),
            ),
            tokens[1].location,
        )
    }

    @Test
    fun unicodeLineSeparatorsMatchOfficialCommentAndLocationBehavior() {
        val tokens = tokens(
            JessieCodeLexer().tokenize(
                "/*a\u2028b*/x //c\u2029y",
            ),
        )

        assertEquals(
            listOf(
                JessieCodeTokenType.DIVIDE to "/",
                JessieCodeTokenType.MULTIPLY to "*",
                JessieCodeTokenType.IDENTIFIER to "a",
                JessieCodeTokenType.IDENTIFIER to "b",
                JessieCodeTokenType.MULTIPLY to "*",
                JessieCodeTokenType.DIVIDE to "/",
                JessieCodeTokenType.IDENTIFIER to "x",
                JessieCodeTokenType.IDENTIFIER to "y",
                JessieCodeTokenType.EOF to "",
            ),
            tokens.map { it.type to it.lexeme },
        )
        assertEquals(
            JessieCodeSourcePosition(
                offset = 13,
                line = 1,
                column = 13,
            ),
            tokens[7].location.start,
        )
    }

    @Test
    fun repeatedUnterminatedBlockCommentPrefixesRemainOrdinaryTokens() {
        val source = buildString {
            repeat(1_000) {
                append("/*a")
            }
        }

        val tokens = tokens(
            JessieCodeLexer(
                limits = JessieCodeLexerLimits(
                    maxSourceLength = source.length,
                    maxTokens = 3_000,
                ),
            ).tokenize(source),
        )

        assertEquals(3_001, tokens.size)
        assertEquals(
            listOf(
                JessieCodeTokenType.DIVIDE,
                JessieCodeTokenType.MULTIPLY,
                JessieCodeTokenType.IDENTIFIER,
                JessieCodeTokenType.DIVIDE,
                JessieCodeTokenType.MULTIPLY,
                JessieCodeTokenType.IDENTIFIER,
            ),
            tokens.take(6).map { it.type },
        )
        assertEquals(JessieCodeTokenType.EOF, tokens.last().type)
    }

    @Test
    fun resourceLimitsReturnStructuredErrors() {
        val invalidLimits = JessieCodeLexer(
            limits = JessieCodeLexerLimits(
                maxSourceLength = -1,
                maxTokens = -1,
            ),
        ).tokenize("")
        assertEquals(
            JessieCodeLexerError.InvalidLimits(
                maxSourceLength = -1,
                maxTokens = -1,
            ),
            assertIs<GMResult.Err<JessieCodeLexerError.InvalidLimits>>(
                invalidLimits,
            ).error,
        )

        val oversizedSource = JessieCodeLexer(
            limits = JessieCodeLexerLimits(
                maxSourceLength = 2,
                maxTokens = 10,
            ),
        ).tokenize("123")
        assertEquals(
            JessieCodeLexerError.SourceLengthExceeded(
                limit = 2,
                actual = 3,
            ),
            assertIs<GMResult.Err<JessieCodeLexerError.SourceLengthExceeded>>(
                oversizedSource,
            ).error,
        )

        val excessiveTokens = JessieCodeLexer(
            limits = JessieCodeLexerLimits(
                maxSourceLength = 100,
                maxTokens = 2,
            ),
        ).tokenize("1 2 3")
        assertEquals(
            JessieCodeLexerError.TokenLimitExceeded(
                limit = 2,
                location = JessieCodeSourcePosition(
                    offset = 4,
                    line = 1,
                    column = 4,
                ),
            ),
            assertIs<GMResult.Err<JessieCodeLexerError.TokenLimitExceeded>>(
                excessiveTokens,
            ).error,
        )
    }

    private fun tokens(
        result: GMResult<List<JessieCodeToken>, JessieCodeLexerError>,
    ): List<JessieCodeToken> =
        assertIs<GMResult.Ok<List<JessieCodeToken>>>(result).value
}
