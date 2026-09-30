/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.math.RandomSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GraphReaderTest {
    @Test
    fun undirectedGraphMatchesOfficialFixture() {
        val parsed = parse(
            source = listOf(
                "-5 5 5 -5",
                "graph",
                "3",
                "A 1 2",
                "B",
                "C -3 4",
                "A B",
                "B C",
            ).joinToString("\n"),
            directed = true,
        )

        assertEquals(listOf(-5.0, 5.0, 5.0, -5.0), parsed.boundingBox)
        assertEquals(
            GraphData(
                n = 3,
                nodes = listOf(
                    GraphNode("A", listOf(1.0, 2.0)),
                    GraphNode("B", listOf(null, null)),
                    GraphNode("C", listOf(-3.0, 4.0)),
                ),
                adjMatrix = listOf(
                    listOf(0.0, 1.0, 0.0),
                    listOf(1.0, 0.0, 1.0),
                    listOf(0.0, 1.0, 0.0),
                ),
                nodenumbers = mapOf("A" to 0, "B" to 1, "C" to 2),
                weighted = false,
                directed = false,
            ),
            parsed.graph,
        )
    }

    @Test
    fun directedWeightedGraphMatchesOfficialFixture() {
        val parsed = parse(
            listOf(
                "-10 10 10 -10",
                "digraph",
                "3",
                "A",
                "B 2 3",
                "C",
                "A B 7",
                "B C -2",
                "C A 0",
            ).joinToString("\n"),
        )

        assertEquals(
            listOf(
                listOf(0.0, 7.0, Double.POSITIVE_INFINITY),
                listOf(Double.POSITIVE_INFINITY, 0.0, -2.0),
                listOf(0.0, Double.POSITIVE_INFINITY, 0.0),
            ),
            parsed.graph.adjMatrix,
        )
        assertEquals(true, parsed.graph.weighted)
        assertEquals(true, parsed.graph.directed)
        assertEquals(
            listOf(
                GraphNode("A", listOf(null, null)),
                GraphNode("B", listOf(2.0, 3.0)),
                GraphNode("C", listOf(null, null)),
            ),
            parsed.graph.nodes,
        )
    }

    @Test
    fun decimalIntegerPrefixesMatchJavascriptParseInt() {
        val parsed = parse(
            listOf(
                "-5x 5.9 5 -5",
                "graph",
                "1",
                "A 2x -3.8",
            ).joinToString("\n"),
        )

        assertEquals(listOf(-5.0, 5.0, 5.0, -5.0), parsed.boundingBox)
        assertEquals(
            listOf(2.0, -3.0),
            parsed.graph.nodes.single().coords,
        )
    }

    @Test
    fun malformedEdgesAndTruncatedNodeSectionsAreStructured() {
        val unknownNode = assertIs<
            GMResult.Err<GraphReaderError.UnknownNode>
        >(
            GraphReader(
                listOf(
                    "-5 5 5 -5",
                    "graph",
                    "1",
                    "A",
                    "A missing",
                ).joinToString("\n"),
            ).parseData(),
        ).error
        assertEquals(5, unknownNode.line)
        assertEquals("missing", unknownNode.name)

        val missingNode = assertIs<
            GMResult.Err<GraphReaderError.MissingNodeLine>
        >(
            GraphReader(
                listOf(
                    "-5 5 5 -5",
                    "graph",
                    "2",
                    "A",
                ).joinToString("\n"),
            ).parseData(),
        ).error
        assertEquals(1, missingNode.nodeIndex)
        assertEquals(5, missingNode.line)
    }

    @Test
    fun invalidCountsAndResourceLimitsAreStructured() {
        assertIs<GMResult.Err<GraphReaderError.InvalidNodeCount>>(
            GraphReader("-5 5 5 -5\ngraph\ninvalid").parseData(),
        )
        assertIs<GMResult.Err<GraphReaderError.SourceLimitExceeded>>(
            GraphReader("-5 5 5 -5\ngraph\n0").parseData(
                limits = GraphReaderLimits(maxSourceCharacters = 1),
            ),
        )
        assertIs<GMResult.Err<GraphReaderError.LineLimitExceeded>>(
            GraphReader("-5 5 5 -5\ngraph\n0").parseData(
                limits = GraphReaderLimits(maxLines = 2),
            ),
        )
        assertIs<GMResult.Err<GraphReaderError.NodeLimitExceeded>>(
            GraphReader(
                "-5 5 5 -5\ngraph\n3\nA\nB\nC",
            ).parseData(
                limits = GraphReaderLimits(maxNodes = 2),
            ),
        )
        assertIs<GMResult.Err<GraphReaderError.MatrixLimitExceeded>>(
            GraphReader(
                "-5 5 5 -5\ngraph\n3\nA\nB\nC",
            ).parseData(
                limits = GraphReaderLimits(maxMatrixCells = 8),
            ),
        )
        assertIs<GMResult.Err<GraphReaderError.EdgeLimitExceeded>>(
            GraphReader(
                "-5 5 5 -5\ngraph\n2\nA\nB\nA B",
            ).parseData(
                limits = GraphReaderLimits(maxEdges = 0),
            ),
        )
        assertIs<GMResult.Err<GraphReaderError.InvalidLimits>>(
            GraphReader("-5 5 5 -5\ngraph\n0").parseData(
                limits = GraphReaderLimits(maxNodes = -1),
            ),
        )
    }

    @Test
    fun readCreatesOfficialUndirectedGraphStructureOnBoard() {
        val board = board()
        val randomValues = ArrayDeque(listOf(0.25, 0.75))
        val drawn = assertIs<GMResult.Ok<DrawnGraph>>(
            GraphReader(
                listOf(
                    "-10 10 10 -10",
                    "graph",
                    "3",
                    "A 1 2",
                    "B",
                    "C -3 4",
                    "A B",
                    "B C",
                ).joinToString("\n"),
            ).read(
                board = board,
                randomSource = RandomSource {
                    randomValues.removeFirst()
                },
            ),
        ).value

        assertEquals(
            listOf(-10.0, 10.0, 10.0, -10.0),
            board.getBoundingBox().toList(),
        )
        assertEquals(25.0, board.unitX)
        assertEquals(25.0, board.unitY)
        assertEquals(250.0, board.origin.scrCoords[1])
        assertEquals(250.0, board.origin.scrCoords[2])
        assertEquals(5, board.objects.size)

        val pointA = assertIs<Point>(board.select("A"))
        val pointB = assertIs<Point>(board.select("B"))
        val pointC = assertIs<Point>(board.select("C"))
        assertEquals(1.0, pointA.X())
        assertEquals(2.0, pointA.Y())
        assertEquals(-50.0 / 11.0, pointB.X(), absoluteTolerance = 1.0e-12)
        assertEquals(50.0 / 11.0, pointB.Y(), absoluteTolerance = 1.0e-12)
        assertEquals(-3.0, pointC.X())
        assertEquals(4.0, pointC.Y())
        assertSame(pointB, drawn.nodes[1].reference)

        val edgeAB = assertIs<Line>(drawn.segments[0][1]?.edge)
        val edgeBC = assertIs<Line>(drawn.segments[1][2]?.edge)
        assertSame(drawn.segments[0][1], drawn.segments[1][0])
        assertSame(drawn.segments[1][2], drawn.segments[2][1])
        assertEquals(null, drawn.segments[0][2])
        assertFalse(edgeAB.straightFirst)
        assertFalse(edgeAB.straightLast)
        assertSame(pointA, edgeAB.point1)
        assertSame(pointB, edgeAB.point2)
        assertSame(pointB, edgeBC.point1)
        assertSame(pointC, edgeBC.point2)
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun directedWeightedGraphMatchesOfficialDrawStructure() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGraph>>(
            GraphReader(
                listOf(
                    "-10 10 10 -10",
                    "digraph",
                    "3",
                    "A 0 0",
                    "B 2 0",
                    "C 0 2",
                    "A B 7",
                    "B A 4",
                    "B C -2",
                ).joinToString("\n"),
            ).read(board),
        ).value

        assertEquals(9, board.objects.size)
        val edgeAB = assertIs<DrawnGraphSegment>(drawn.segments[0][1])
        val edgeBA = assertIs<DrawnGraphSegment>(drawn.segments[1][0])
        val edgeBC = assertIs<DrawnGraphSegment>(drawn.segments[1][2])
        assertEquals(null, drawn.segments[0][0])
        assertEquals(null, drawn.segments[0][2])
        assertEquals(null, drawn.segments[2][0])
        assertEquals(null, drawn.segments[2][1])
        assertFalse(edgeAB.edge.firstArrowEnabled)
        assertTrue(edgeAB.edge.lastArrowEnabled)
        assertFalse(edgeBA.edge.firstArrowEnabled)
        assertTrue(edgeBA.edge.lastArrowEnabled)
        assertFalse(edgeBC.edge.firstArrowEnabled)
        assertTrue(edgeBC.edge.lastArrowEnabled)
        assertSame(drawn.nodes[0].reference, edgeAB.edge.point1)
        assertSame(drawn.nodes[1].reference, edgeAB.edge.point2)
        assertSame(drawn.nodes[1].reference, edgeBA.edge.point1)
        assertSame(drawn.nodes[0].reference, edgeBA.edge.point2)
        assertSame(drawn.nodes[1].reference, edgeBC.edge.point1)
        assertSame(drawn.nodes[2].reference, edgeBC.edge.point2)

        assertEquals(7.0, edgeAB.weight)
        assertEquals(4.0, edgeBA.weight)
        assertEquals(-2.0, edgeBC.weight)
        val labelAB = assertIs<com.swithun.jsxgraph.core.base.Text>(
            edgeAB.weightLabel,
        )
        val labelBA = assertIs<com.swithun.jsxgraph.core.base.Text>(
            edgeBA.weightLabel,
        )
        val labelBC = assertIs<com.swithun.jsxgraph.core.base.Text>(
            edgeBC.weightLabel,
        )
        assertEquals("7", labelAB.plaintext)
        assertEquals("4", labelBA.plaintext)
        assertEquals("-2", labelBC.plaintext)
        assertSame(edgeAB.edge, labelAB.anchor)
        assertSame(edgeBA.edge, labelBA.anchor)
        assertSame(edgeBC.edge, labelBC.anchor)
        assertEquals(1.0, labelAB.X())
        assertEquals(0.0, labelAB.Y())
        assertEquals(1.0, labelBA.X())
        assertEquals(0.0, labelBA.Y())
        assertEquals(1.0, labelBC.X())
        assertEquals(1.0, labelBC.Y())

        assertIs<GMResult.Err<GraphReaderError.InvalidBoundingBox>>(
            GraphReader("invalid\ngraph\n0").read(board()),
        )
    }

    @Test
    fun registeredGraphFactoryRunsThroughFileReader() {
        val board = board()
        val registry = ReaderRegistry<Board>().also {
            it.registerGraphReader()
        }
        var callbackBoard: Board? = null

        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source =
                    "-5 5 5 -5\ngraph\n2\nA 0 0\nB 1 1\nA B",
                board = board,
                format = "GRAPH",
                registry = registry,
                callback = { callbackBoard = it },
            ),
        )

        assertSame(board, callbackBoard)
        assertEquals(3, board.objects.size)

        val directedBoard = board()
        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source =
                    "-5 5 5 -5\ndigraph\n2\nA 0 0\nB 1 1\nA B",
                board = directedBoard,
                format = "digraph",
                registry = registry,
            ),
        )
        assertEquals(3, directedBoard.objects.size)
        assertTrue(
            directedBoard.objects.values
                .filterIsInstance<Line>()
                .single()
                .lastArrowEnabled,
        )
    }

    private fun parse(
        source: String,
        directed: Boolean = false,
    ): ParsedGraph =
        assertIs<GMResult.Ok<ParsedGraph>>(
            GraphReader(source).parseData(directed),
        ).value

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )
}
