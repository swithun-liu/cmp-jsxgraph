/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
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
}
