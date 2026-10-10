/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TextTest {
    @Test
    fun freeTextRegistersIdentityCoordinatesAndContent() {
        val board = board()
        val text = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(2.0, -3.0),
                content = "Hello",
            ),
        )

        assertEquals("boardT0", text.id)
        assertEquals("t_{a}", text.name)
        assertEquals("text", text.elType)
        assertEquals(Const.OBJECT_TYPE_TEXT, text.type)
        assertEquals(Const.OBJECT_CLASS_TEXT, text.elementClass)
        assertSame(text, text.baseElement)
        assertTrue(text.isDraggable)
        assertFalse(text.isConstrained)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -3.0),
            text.coords.usrCoords,
        )
        assertEquals("Hello", text.orgText)
        assertEquals("Hello", text.plaintext)
        assertSame(text, board.select(text.id))
        assertSame(text, board.select(text.name))
    }

    @Test
    fun constrainedTextTracksCoordinateDependencies() {
        val board = board()
        val driver = point(board, 2.0, 3.0, name = "A")
        val text = text(
            Text.create(
                board = board,
                coordinateExpressions = listOf(
                    "A.X() + 1",
                    "A.Y() * 2",
                ),
                content = "Dynamic position",
                name = "",
            ),
        )

        assertFalse(text.isDraggable)
        assertTrue(text.isConstrained)
        assertEquals(Const.OBJECT_TYPE_TEXT, text.type)
        assertEquals("A.X() + 1", text.Xjc)
        assertEquals("A.Y() * 2", text.Yjc)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 6.0),
            text.coords.usrCoords,
        )
        assertSame(text, driver.childElements[text.id])

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-1.0, 4.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 8.0),
            text.coords.usrCoords,
        )
        assertNull(text.coordinateEvaluationError)
    }

    @Test
    fun setTextUpdatesStaticContent() {
        val text = text(
            Text.create(
                board = board(),
                coordinates = doubleArrayOf(0.0, 0.0),
                content = "Before",
                name = "",
            ),
        )

        assertSame(
            text,
            assertIs<GMResult.Ok<Text>>(
                text.setText("After"),
            ).value,
        )
        assertEquals("After", text.orgText)
        assertEquals("After", text.plaintext)
        assertTrue(text.needsUpdate)
    }

    @Test
    fun lineAnchoredTextTracksTheOfficialMidpointAnchor() {
        val board = board()
        val point1 = point(board, 0.0, 0.0, name = "A")
        val point2 = point(board, 4.0, 2.0, name = "B")
        val line = assertIs<GMResult.Ok<Line>>(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                name = "",
            ),
        ).value
        val text = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(1.0, -1.0),
                content = "7",
                name = "",
                anchor = line,
            ),
        )

        assertSame(line, text.anchor)
        assertContentEquals(
            doubleArrayOf(1.0, -1.0),
            text.relativeCoordinates,
        )
        assertEquals(3.0, text.X())
        assertEquals(0.0, text.Y())
        assertSame(text, line.childElements[text.id])

        point2.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(6.0, 4.0),
        )
        board.update()

        assertEquals(4.0, text.X())
        assertEquals(1.0, text.Y())
    }

    @Test
    fun valueTagsTrackDependenciesAndSetTextReplacesThemAtomically() {
        val board = board()
        val driver = point(board, 2.0, 3.0, name = "A")
        val text = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                content = "x=<value>X(A)</value>",
                name = "",
                digits = 2,
            ),
        )

        assertEquals("x=2.00", text.plaintext)
        assertSame(text, driver.childElements[text.id])

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-1.25, 4.0),
        )
        board.update()

        assertEquals("x=-1.25", text.plaintext)
        assertNull(text.contentEvaluationError)

        val failure = assertIs<
            GMResult.Err<TextError.UnsupportedParsedContent>
            >(text.setText("x_{1}"))
        assertEquals("_", failure.error.marker)
        assertEquals("x=<value>X(A)</value>", text.orgText)
        assertEquals("x=-1.25", text.plaintext)
        assertSame(text, driver.childElements[text.id])

        assertIs<GMResult.Ok<Text>>(text.setText("Static"))
        assertEquals("Static", text.plaintext)
        assertFalse(text.id in driver.childElements)
    }

    @Test
    fun creationFailuresRemainExplicitAndAtomic() {
        val board = board()
        val existing = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                content = "Existing",
                id = "text",
            ),
        )

        assertEquals(
            TextError.InvalidCoordinateCount(1),
            assertIs<GMResult.Err<TextError.InvalidCoordinateCount>>(
                Text.create(
                    board = board,
                    coordinates = doubleArrayOf(0.0),
                    content = "Invalid",
                ),
            ).error,
        )
        val compileError = assertIs<
            GMResult.Err<TextError.CoordinateExpressionCompile>
            >(
            Text.create(
                board = board,
                coordinateExpressions = listOf("0", "1 +"),
                content = "Invalid",
            ),
        ).error
        assertEquals(1, compileError.coordinateIndex)
        assertIs<JessieCodeExpressionCompileError.Parser>(
            compileError.error,
        )
        assertIs<TextError.ContentTagSyntax>(
            assertIs<GMResult.Err<TextError.ContentTagSyntax>>(
                Text.create(
                    board = board,
                    coordinates = doubleArrayOf(0.0, 0.0),
                    content = "broken <value>1",
                ),
            ).error,
        )
        assertIs<TextError.ContentExpressionCompile>(
            assertIs<GMResult.Err<TextError.ContentExpressionCompile>>(
                Text.create(
                    board = board,
                    coordinates = doubleArrayOf(0.0, 0.0),
                    content = "<value>1 +</value>",
                ),
            ).error,
        )
        assertEquals(
            TextError.Registration(
                BoardError.DuplicateElementId("text"),
            ),
            assertIs<GMResult.Err<TextError.Registration>>(
                Text.create(
                    board = board,
                    coordinates = doubleArrayOf(1.0, 1.0),
                    content = "Duplicate",
                    id = "text",
                ),
            ).error,
        )
        assertEquals(
            listOf<GeometryElement>(existing),
            board.objectsList,
        )
        assertEquals(1, board.numObjects)
    }

    @Test
    fun sizeHitDecodingAndCoordinateMutationMatchNoRendererBehavior() {
        val board = board()
        val text = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(10.0, -20.0),
                content = "A&#x00B2;",
                parse = false,
                name = "",
            ),
        )
        text.setAttribute(
            mapOf(
                "display" to "internal",
                "fontsize" to 20.0,
                "anchorx" to "left",
                "anchory" to "bottom",
            ),
        )
        text.prepareUpdate().update()

        assertEquals("A\u00B2", text.plaintext)
        assertContentEquals(
            doubleArrayOf(18.0, 18.0),
            text.textSize(),
        )
        assertTrue(text.hasPoint(10.0, 20.0, precision = 1.0))
        assertFalse(text.hasPoint(40.0, 20.0, precision = 1.0))
        assertEquals("3*x", text.expandShortMath("3x"))

        assertSame(text, text.setCoords(2.0, 3.0))
        assertEquals(2.0, text.X())
        assertEquals(3.0, text.Y())
        assertEquals("\u03C0", text.utf8Decode("&#x03C0;"))
    }

    @Test
    fun labelAutoPositionUsesOfficialConflictScoreAndSearchOrder() {
        val board = Board(
            originX = 100.0,
            originY = 100.0,
            unitX = 1.0,
            unitY = 1.0,
            canvasWidth = 200.0,
            canvasHeight = 200.0,
            id = "auto-position",
        )
        val anchor = point(board, 0.0, 0.0, name = "A")
        val blocker = point(board, 25.0, 5.4, name = "B")
        val label = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                content = "label",
                name = "",
                anchor = anchor,
                isLabel = true,
            ),
        )
        anchor.setAttribute(mapOf("visible" to true))
        blocker.setAttribute(mapOf("visible" to true))
        label.setAttribute(
            mapOf(
                "visible" to true,
                "islabel" to true,
                "anchorx" to "left",
                "anchory" to "bottom",
                "offset" to doubleArrayOf(12.0, 0.0),
                "autopositionmindistance" to 12.0,
                "autopositionmaxdistance" to 28.0,
            ),
        )
        label.screenOffset = doubleArrayOf(12.0, 0.0)
        board.update()

        assertEquals(
            -1.0,
            label.calculateAutoPositionScore(
                radius = 12.0,
                angle = 0.0,
            ),
            absoluteTolerance = 1.0e-12,
        )
        label.setAttribute(
            mapOf("autopositionwhitelist" to listOf(blocker.id)),
        )
        assertEquals(
            0.0,
            label.calculateAutoPositionScore(
                radius = 12.0,
                angle = 0.0,
            ),
            absoluteTolerance = 1.0e-12,
        )

        label.setAttribute(mapOf("autopositionwhitelist" to emptyList<String>()))
        assertEquals(
            -1.0,
            label.calculateAutoPositionScore(
                radius = 12.0,
                angle = 0.0,
            ),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -1.0 - kotlin.math.PI / 300.0,
            label.calculateAutoPositionScore(
                radius = 12.0,
                angle = kotlin.math.PI / 30.0,
            ),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -kotlin.math.PI / 60.0,
            label.calculateAutoPositionScore(
                radius = 12.0,
                angle = kotlin.math.PI / 6.0,
            ),
            absoluteTolerance = 1.0e-12,
        )
        val best = buildList {
            for (radiusIndex in 0 until 4) {
                val radius = 12.0 + radiusIndex * (16.0 / 3.0)
                for (angleIndex in 0 until 60) {
                    val angle =
                        angleIndex.toDouble() / 60.0 *
                            2.0 *
                            kotlin.math.PI
                    add(
                        Triple(
                            label.calculateAutoPositionScore(radius, angle),
                            radius,
                            angle,
                        ),
                    )
                }
            }
        }.maxBy { it.first }
        assertEquals(
            59.0 * kotlin.math.PI / 30.0,
            best.third,
            absoluteTolerance = 1.0e-12,
        )
        assertSame(label, label.setAutoPosition())
        assertEquals(
            best.second * kotlin.math.cos(best.third),
            label.screenOffset[0],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            best.second * kotlin.math.sin(best.third),
            label.screenOffset[1],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals("left", label.getAttribute("anchorx"))
        assertEquals("top", label.getAttribute("anchory"))
    }

    @Test
    fun affineTransformsMatchOfficialScreenMatrixAndHitInverse() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            canvasWidth = 640.0,
            canvasHeight = 480.0,
            id = "board",
        )
        val text = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 1.0),
                content = "internal",
                name = "",
            ),
        )
        val transformations = listOf(
            transformation("translate", 1.5, -0.5),
            transformation("shear", 0.2, -0.1),
            transformation("scale", 1.25, 0.75),
            transformation("rotate", PI / 6.0),
        )
        text.addTransform(text, transformations)
        board.update()

        val joined = assertIs<GMResult.Ok<Array<DoubleArray>>>(
            joinScreenTransformations(board, transformations),
        ).value
        assertContentEquals(
            doubleArrayOf(
                -6.076859021798441,
                1.1200317547305485,
                0.15849364905389024,
            ),
            joined[1],
        )
        assertContentEquals(
            doubleArrayOf(
                215.218313001771,
                -0.560048094716167,
                0.774519052838329,
            ),
            joined[2],
        )
        assertEquals(
            3.840864317029974,
            text.actualCoords.usrCoords[1],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            2.347427857925749,
            text.actualCoords.usrCoords[2],
            absoluteTolerance = 1.0e-12,
        )
        val transformedAnchor = Mat.matVecMult(
            joined,
            text.coords.scrCoords,
        )
        assertTrue(
            text.hasPoint(
                transformedAnchor[1],
                transformedAnchor[2],
            ),
        )
        val transformedOutside = Mat.matVecMult(
            joined,
            doubleArrayOf(
                1.0,
                text.coords.scrCoords[1] + 100.0,
                text.coords.scrCoords[2] + 100.0,
            ),
        )
        assertFalse(
            text.hasPoint(
                transformedOutside[1],
                transformedOutside[2],
            ),
        )
    }

    @Test
    fun rotateAttributeBuildsOfficialFiveTransformSequence() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            canvasWidth = 640.0,
            canvasHeight = 480.0,
            id = "board",
        )
        val text = text(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 3.0),
                content = "rotated",
                name = "",
                rotationDegrees = 35.0,
            ),
        )
        board.update()

        assertEquals(
            listOf("translate", "scale", "rotate", "scale", "translate"),
            text.transformations.map {
                it.transformationType.upstreamName
            },
        )
        val joined = assertIs<GMResult.Ok<Array<DoubleArray>>>(
            joinScreenTransformations(board, text.transformations),
        ).value
        assertEquals(
            -10.957826534602873,
            joined[1][0],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            205.24621431765573,
            joined[2][0],
            absoluteTolerance = 1.0e-12,
        )
    }

    private fun board(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "board",
        )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        name: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                name = name,
            ),
        ).value

    private fun text(
        result: GMResult<Text, TextError>,
    ): Text = assertIs<GMResult.Ok<Text>>(result).value

    private fun transformation(
        type: String,
        vararg parameters: Double,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(
            Transformation.create(type, parameters),
        ).value
}
