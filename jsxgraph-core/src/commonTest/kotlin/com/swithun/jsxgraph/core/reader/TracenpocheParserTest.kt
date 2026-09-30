/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TracenpocheParserTest {
    @Test
    fun assignmentsCallsAndAttributesMatchOfficialFixture() {
        val program = parse(
            "A=point(-1,0);" +
                "B=point(1,1){rouge,sansnom};" +
                "s=segment(A,B);",
        )

        assertEquals(3, program.statements.size)
        val first = assignment(program, 0)
        assertEquals("A", first.name)
        assertEquals(
            TracenpocheExpression.Call(
                callee = "point",
                arguments = listOf(
                    TracenpocheExpression.Unary(
                        operator = "-",
                        operand =
                            TracenpocheExpression.NumberLiteral(1.0),
                    ),
                    TracenpocheExpression.NumberLiteral(0.0),
                ),
                attributes = emptyList(),
            ),
            first.value,
        )

        val second = assignment(program, 1)
        val point = assertIs<TracenpocheExpression.Call>(second.value)
        assertEquals("B", second.name)
        assertEquals(listOf("rouge", "sansnom"), point.attributes)

        val third = assignment(program, 2)
        val segment = assertIs<TracenpocheExpression.Call>(third.value)
        assertEquals("s", third.name)
        assertEquals("segment", segment.callee)
        assertEquals(
            listOf(
                TracenpocheExpression.Reference("A"),
                TracenpocheExpression.Reference("B"),
            ),
            segment.arguments,
        )
    }

    @Test
    fun distanceFunctionAndOperatorPrecedenceMatchOfficialFixture() {
        val program = parse(
            "d=A#B;f=fonction(x^2+1);v=1+2*3^4;",
        )

        val distance = assignment(program, 0)
        assertEquals(
            TracenpocheExpression.Binary(
                operator = "#",
                left = TracenpocheExpression.Reference("A"),
                right = TracenpocheExpression.Reference("B"),
            ),
            distance.value,
        )

        val function = assertIs<TracenpocheExpression.Function>(
            assignment(program, 1).value,
        )
        assertEquals(
            TracenpocheExpression.Binary(
                operator = "+",
                left = TracenpocheExpression.Binary(
                    operator = "^",
                    left = TracenpocheExpression.Reference("x"),
                    right = TracenpocheExpression.NumberLiteral(2.0),
                ),
                right = TracenpocheExpression.NumberLiteral(1.0),
            ),
            function.expression,
        )

        val arithmetic = assignment(program, 2).value
        assertEquals(
            TracenpocheExpression.Binary(
                operator = "+",
                left = TracenpocheExpression.NumberLiteral(1.0),
                right = TracenpocheExpression.Binary(
                    operator = "*",
                    left = TracenpocheExpression.NumberLiteral(2.0),
                    right = TracenpocheExpression.Binary(
                        operator = "^",
                        left = TracenpocheExpression.NumberLiteral(3.0),
                        right = TracenpocheExpression.NumberLiteral(4.0),
                    ),
                ),
            ),
            arithmetic,
        )
    }

    @Test
    fun variableAndForStatementsMatchOfficialFixture() {
        val program = parse(
            "var i=1;" +
                "for i=1 to 3 do;" +
                "A=point(i,i^2);" +
                "end;",
        )

        assertEquals(2, program.statements.size)
        assertEquals("i", assignment(program, 0).name)
        val loop = assertIs<TracenpocheStatement.For>(
            program.statements[1],
        )
        assertEquals("i", loop.initializer.name)
        assertEquals(
            TracenpocheExpression.NumberLiteral(1.0),
            loop.initializer.value,
        )
        assertEquals(
            TracenpocheExpression.NumberLiteral(3.0),
            loop.endInclusive,
        )
        val body = assertIs<TracenpocheStatement.Expression>(
            loop.body.single(),
        )
        assertEquals(
            "A",
            assertIs<TracenpocheExpression.Assignment>(
                body.expression,
            ).name,
        )
    }

    @Test
    fun indexedLoopAssignmentsMatchOfficialPolygonExample() {
        val program = parse(
            "for i=0 to 18 do;" +
                "p=0.05*([i+1]);" +
                "A[i+1]=pointsur(A0,B0,p){sansnom,i};" +
                "end;",
        )
        val loop = assertIs<TracenpocheStatement.For>(
            program.statements.single(),
        )
        val indexed = assertIs<TracenpocheExpression.Assignment>(
            assertIs<TracenpocheStatement.Expression>(
                loop.body[1],
            ).expression,
        )

        assertEquals("A", indexed.name)
        assertEquals(
            TracenpocheExpression.Binary(
                operator = "+",
                left = TracenpocheExpression.Reference("i"),
                right = TracenpocheExpression.NumberLiteral(1.0),
            ),
            indexed.index,
        )
        val call = assertIs<TracenpocheExpression.Call>(indexed.value)
        assertEquals("pointsur", call.callee)
        assertEquals(listOf("sansnom", "i"), call.attributes)
    }

    @Test
    fun officialConditionalAssignmentBugIsStructured() {
        assertIs<GMResult.Err<TracenpocheParserError.UnexpectedToken>>(
            TracenpocheParser.parse("v=[A<B,A,B];"),
        )
    }

    @Test
    fun syntaxAndResourceFailuresAreStructured() {
        assertIs<GMResult.Err<TracenpocheParserError.UnexpectedEnd>>(
            TracenpocheParser.parse("A=point(1,2)"),
        )
        assertIs<
            GMResult.Err<TracenpocheParserError.StatementLimitExceeded>
        >(
            TracenpocheParser.parse(
                source = "A=1;B=2;",
                limits = TracenpocheParserLimits(maxStatements = 1),
            ),
        )
        assertIs<
            GMResult.Err<TracenpocheParserError.NestingLimitExceeded>
        >(
            TracenpocheParser.parse(
                source = "A=point(1,2);",
                limits = TracenpocheParserLimits(maxNestingDepth = 0),
            ),
        )
        assertIs<GMResult.Err<TracenpocheParserError.InvalidLimits>>(
            TracenpocheParser.parse(
                source = "",
                limits = TracenpocheParserLimits(maxStatements = -1),
            ),
        )
    }

    private fun parse(source: String): ParsedTracenpocheProgram =
        assertIs<GMResult.Ok<ParsedTracenpocheProgram>>(
            TracenpocheParser.parse(source),
        ).value

    private fun assignment(
        program: ParsedTracenpocheProgram,
        index: Int,
    ): TracenpocheExpression.Assignment {
        val statement = assertIs<TracenpocheStatement.Expression>(
            program.statements[index],
        )
        return assertIs(statement.expression)
    }
}
