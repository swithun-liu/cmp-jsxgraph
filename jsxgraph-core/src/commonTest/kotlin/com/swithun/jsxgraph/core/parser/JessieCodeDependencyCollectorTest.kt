package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class JessieCodeDependencyCollectorTest {
    @Test
    fun namedElementsMatchOfficialReverseChildTraversal() {
        val fixture = boardFixture()
        val dependencies = collect(
            source = "A + B;",
            board = fixture.board,
        )

        assertEquals(listOf("P2", "P1"), dependencies.keys.toList())
        assertSame(fixture.second, dependencies["P2"])
        assertSame(fixture.first, dependencies["P1"])
    }

    @Test
    fun parametersLocalsAndConstantsAreExcluded() {
        val fixture = boardFixture()
        val dependencies = collect(
            source = "A + B + PI;",
            board = fixture.board,
            parameterNames = setOf("A"),
            localNames = setOf("B"),
        )

        assertEquals(emptyMap(), dependencies)
    }

    @Test
    fun explicitElementCallsMatchOfficialDependencyDiscovery() {
        val fixture = boardFixture()
        val dependencies = collect(
            source = "\$(\"P1\") + \$value(\"P2\");",
            board = fixture.board,
        )

        assertEquals(listOf("P2", "P1"), dependencies.keys.toList())
        assertSame(fixture.second, dependencies["P2"])
        assertSame(fixture.first, dependencies["P1"])
    }

    @Test
    fun duplicateReferencesAreStoredOnce() {
        val fixture = boardFixture()
        val dependencies = collect(
            source = "A + A;",
            board = fixture.board,
        )

        assertEquals(listOf("P1"), dependencies.keys.toList())
    }

    @Test
    fun objectKeysAreIgnoredWhileValuesKeepReverseTraversal() {
        val fixture = boardFixture()
        val dependencies = collect(
            source = "<< A: A, B: B >>;",
            board = fixture.board,
        )

        assertEquals(listOf("P2", "P1"), dependencies.keys.toList())
    }

    @Test
    fun unknownVariablesAreNotDependencies() {
        val fixture = boardFixture()
        val dependencies = collect(
            source = "missing + 1;",
            board = fixture.board,
        )

        assertEquals(emptyMap(), dependencies)
    }

    @Test
    fun missingExplicitIdsAndResourceLimitsAreStructured() {
        val fixture = boardFixture()
        val missing = collectError(
            source = "\$(\"missing\");",
            board = fixture.board,
        )
        assertEquals(
            "missing",
            assertIs<
                JessieCodeDependencyError.MissingExplicitElement
                >(missing).id,
        )

        val ast = parse("A + B;")
        val invalidLimits = assertIs<
            GMResult.Err<JessieCodeDependencyError>
            >(
            JessieCodeDependencyCollector(
                board = fixture.board,
                limits = JessieCodeDependencyLimits(
                    maxVisitedNodes = 0,
                ),
            ).collect(ast),
        ).error
        assertIs<JessieCodeDependencyError.InvalidLimits>(
            invalidLimits,
        )

        val nodeLimit = assertIs<
            GMResult.Err<JessieCodeDependencyError>
            >(
            JessieCodeDependencyCollector(
                board = fixture.board,
                limits = JessieCodeDependencyLimits(
                    maxVisitedNodes = 1,
                ),
            ).collect(ast),
        ).error
        assertIs<JessieCodeDependencyError.NodeLimitExceeded>(
            nodeLimit,
        )

        val depthLimit = assertIs<
            GMResult.Err<JessieCodeDependencyError>
            >(
            JessieCodeDependencyCollector(
                board = fixture.board,
                limits = JessieCodeDependencyLimits(
                    maxTraversalDepth = 1,
                ),
            ).collect(ast),
        ).error
        assertIs<JessieCodeDependencyError.DepthLimitExceeded>(
            depthLimit,
        )
    }

    private fun collect(
        source: String,
        board: Board,
        parameterNames: Set<String> = emptySet(),
        localNames: Set<String> = emptySet(),
    ): Map<String, GeometryElement> =
        assertIs<GMResult.Ok<Map<String, GeometryElement>>>(
            JessieCodeDependencyCollector(board).collect(
                node = parse(source),
                parameterNames = parameterNames,
                localNames = localNames,
            ),
        ).value

    private fun collectError(
        source: String,
        board: Board,
    ): JessieCodeDependencyError =
        assertIs<GMResult.Err<JessieCodeDependencyError>>(
            JessieCodeDependencyCollector(board).collect(
                parse(source),
            ),
        ).error

    private fun parse(source: String): JessieCodeAstNode =
        assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value

    private fun boardFixture(): BoardFixture {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val first = registerElement(
            board = board,
            id = "P1",
            name = "A",
        )
        val second = registerElement(
            board = board,
            id = "P2",
            name = "B",
        )
        return BoardFixture(board, first, second)
    }

    private fun registerElement(
        board: Board,
        id: String,
        name: String,
    ): GeometryElement {
        val element = GeometryElement(
            board = board,
            id = id,
            name = name,
            type = Const.OBJECT_TYPE_POINT,
            elementClass = Const.OBJECT_CLASS_POINT,
        )
        assertIs<GMResult.Ok<String>>(
            board.setId(element, "P"),
        )
        return element
    }

    private data class BoardFixture(
        val board: Board,
        val first: GeometryElement,
        val second: GeometryElement,
    )
}
