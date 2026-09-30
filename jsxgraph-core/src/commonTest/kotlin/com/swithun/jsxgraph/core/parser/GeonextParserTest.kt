/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class GeonextParserTest {
    @Test
    fun powerAndConditionalReplacementMatchOfficialFixture() {
        val powerCases = mapOf(
            "x ^ 2" to "pow(x,2)",
            "sin(x)^2" to "pow(sin(x),2)",
            "(x+1)^(y+2)" to "pow((x+1),(y+2))",
            "a^b^c" to "pow(pow(a,b),c)",
            "x^" to "x^",
            "((x)^2" to "(pow((x),2)",
        )
        for ((source, expected) in powerCases) {
            assertEquals(expected, ok(GeonextParser.replacePow(source)))
        }
        assertIs<
            GMResult.Err<GeonextParserError.StalledPowerReplacement>
            >(GeonextParser.replacePow("^x"))
        assertIs<
            GMResult.Err<GeonextParserError.MissingClosingParenthesis>
            >(GeonextParser.replacePow("x^(2"))

        val conditionalCases = mapOf(
            "If(a,b,c)" to "((a)?(b):(c))",
            "If(a,If(b,c,d),e)" to
                "((a)?(((b)?(c):(d))):(e))",
            "x+If(a,b,c)+If(d,e,f)" to
                "x+((a)?(b):(c))+((d)?(e):(f))",
            "If(a,b)" to "",
            "If(a,b,c" to "((a)?(b):())",
        )
        for ((source, expected) in conditionalCases) {
            assertEquals(expected, ok(GeonextParser.replaceIf(source)))
        }
    }

    @Test
    fun elementReferencesAndConversionsMatchOfficialFixture() {
        val resolver = GeonextElementResolver { name ->
            mapOf(
                "A" to "idA",
                "B" to "id_B",
                "C[1]" to "idC",
            )[name]
        }

        assertEquals(
            "X(idA)+Y(id_B)+L(idA)+V(id_B)",
            ok(
                GeonextParser.replaceNameById(
                    "X(A)+Y(B)+L(A)+V(B)",
                    resolver,
                ),
            ),
        )
        assertEquals(
            "X(\$('\$('idA'))+Y(\$('\$('id_B'))+" +
                "L(\$('\$('idA'))+V(\$('\$('id_B'))",
            ok(
                GeonextParser.replaceNameById(
                    "X(A)+Y(B)+L(A)+V(B)",
                    resolver,
                    jessieCode = true,
                ),
            ),
        )
        assertEquals(
            "Dist(\$('idA'),\$('id_B'))",
            ok(
                GeonextParser.replaceNameById(
                    "Dist(A,B)",
                    resolver,
                    jessieCode = true,
                ),
            ),
        )
        assertEquals(
            "Deg(idA,id_B,idC)+Rad(idA,id_B,idC)",
            ok(
                GeonextParser.replaceNameById(
                    "Deg(A,B,C[1])+Rad(A,B,C[1])",
                    resolver,
                ),
            ),
        )

        assertEquals(
            "\$('idA').X()+\$('id_B').Y()+" +
                "\$('idA').L()+\$('id_B').Value()",
            ok(
                GeonextParser.replaceIdByObj(
                    "X(idA)+Y(id_B)+L(idA)+V(id_B)",
                ),
            ),
        )
        assertEquals(
            "dist(\$('idA'), \$('id_B'))",
            ok(GeonextParser.replaceIdByObj("Dist(idA,id_B)")),
        )
        assertEquals(
            "deg(\$('idA'),\$('id_B'),\$('idC'))+" +
                "rad(\$('idA'),\$('id_B'),\$('idC'))",
            ok(
                GeonextParser.replaceIdByObj(
                    "Deg(idA,id_B,idC)+Rad(idA,id_B,idC)",
                ),
            ),
        )
        assertEquals(
            "(x+1)",
            ok(GeonextParser.replaceIdByObj("N(x+1)")),
        )

        assertEquals(
            "pow(x,2)",
            ok(GeonextParser.geonext2JS("x^2", resolver)),
        )
        assertEquals(
            "sin(pow(x,2))",
            ok(GeonextParser.geonext2JS("sin(x^2)", resolver)),
        )
        assertEquals(
            "((true)?(abs(-2)):(PI))",
            ok(
                GeonextParser.geonext2JS(
                    "If(True,Abs(-2),Pi)",
                    resolver,
                ),
            ),
        )
        assertEquals(
            "\$('idA').X()+dist(\$('idA'), \$('id_B'))",
            ok(
                GeonextParser.geonext2JS(
                    "X(A)+Dist(A,B)",
                    resolver,
                ),
            ),
        )
        assertEquals(
            "<x>&false",
            ok(
                GeonextParser.geonext2JS(
                    "&lt;x&gt;&amp;False",
                    resolver,
                ),
            ),
        )
        assertEquals(
            "false+ceil(1.2)",
            ok(
                GeonextParser.geonext2JS(
                    "fasle+Trunc(1.2)",
                    resolver,
                ),
            ),
        )
        assertEquals(
            "If(true,Abs(-2),Pi)",
            ok(
                GeonextParser.gxt2jc(
                    "If(True,Abs(-2),Pi)",
                    resolver,
                ),
            ),
        )
    }

    @Test
    fun dependencyDiscoveryMatchesOfficialFiltering() {
        val board = board()
        val first = point(board, "idA", "A")
        val second = point(board, "idB", "B")
        val dependent = point(board, "idD", "dependent")
        val label = assertIs<GMResult.Ok<Text>>(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                content = "label",
                id = "idLabel",
                name = "Label",
                parse = false,
            ),
        ).value

        ok(
            GeonextParser.findDependencies(
                dependent = dependent,
                source = "Dist(A,B)+X(Label)+X(dependent)",
                isLabel = { element -> element === label },
            ),
        )

        assertTrue(dependent.id in first.childElements)
        assertTrue(dependent.id in second.childElements)
        assertFalse(dependent.id in label.childElements)
        assertFalse(dependent.id in dependent.childElements)
    }

    @Test
    fun invalidLimitsAndMissingResolverAreStructured() {
        assertIs<GMResult.Err<GeonextParserError.InvalidLimits>>(
            GeonextParser.replacePow(
                source = "x^2",
                limits = GeonextParserLimits(maxDepth = -1),
            ),
        )
        assertIs<
            GMResult.Err<GeonextParserError.SourceLimitExceeded>
            >(
            GeonextParser.replaceIf(
                source = "If(a,b,c)",
                limits = GeonextParserLimits(maxSourceLength = 2),
            ),
        )
        assertIs<
            GMResult.Err<GeonextParserError.MissingElementResolver>
            >(GeonextParser.geonext2JS("X(A)"))
    }

    private fun point(
        board: Board,
        id: String,
        name: String,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            id = id,
            name = name,
        ),
    ).value

    private fun board(): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = "geonext-parser",
    )

    private fun <T, E> ok(result: GMResult<T, E>): T =
        when (result) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> fail("Expected Ok, got ${result.error}")
        }
}
