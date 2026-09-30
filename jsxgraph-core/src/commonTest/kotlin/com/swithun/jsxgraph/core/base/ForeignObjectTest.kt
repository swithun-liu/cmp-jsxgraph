/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeNumericCoordinateFunction
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ForeignObjectTest {
    @Test
    fun explicitAndIntrinsicSizesMatchOfficialStateModel() {
        val board = board()
        val explicit = foreignObject(
            ForeignObject.create(
                board = board,
                content = "<b>full</b>",
                coordinates = doubleArrayOf(-3.0, -2.0),
                sizeTerms = sizeTerms(3.0, 2.0),
                id = "full",
                name = "",
            ),
        )
        val intrinsic = foreignObject(
            ForeignObject.create(
                board = board,
                content = "<b>intrinsic</b>",
                coordinates = doubleArrayOf(1.0, 2.0),
                sizeTerms = emptyList(),
                id = "intrinsic",
                name = "",
            ),
        )

        assertEquals(Const.OBJECT_TYPE_FOREIGNOBJECT, explicit.type)
        assertEquals(Const.OBJECT_CLASS_OTHER, explicit.elementClass)
        assertEquals("foreignobject", explicit.elType)
        assertEquals("<b>full</b>", explicit.content)
        assertEquals(false, explicit.needsRegularUpdate)
        assertEquals(true, explicit.usesUserSize)
        assertContentEquals(doubleArrayOf(3.0, 2.0), explicit.usrSize)
        assertContentEquals(doubleArrayOf(120.0, 80.0), explicit.size)
        assertSame(explicit, board.elementById("full"))

        assertEquals(false, intrinsic.usesUserSize)
        assertNull(intrinsic.usrSize)
        assertNull(intrinsic.size)
        assertNull(intrinsic.W())
        assertNull(intrinsic.H())
    }

    @Test
    fun setSizeSwitchesIntrinsicContentToUserSpaceSizing() {
        val foreignObject = foreignObject(
            ForeignObject.create(
                board = board(),
                content = "<div>content</div>",
                coordinates = doubleArrayOf(0.0, 0.0),
                sizeTerms = emptyList(),
            ),
        )

        assertIs<GMResult.Ok<ForeignObject>>(
            foreignObject.setSize(sizeTerms(5.0, 3.5)),
        )
        foreignObject.fullUpdate()

        assertEquals(5.0, foreignObject.W())
        assertEquals(3.5, foreignObject.H())
        assertContentEquals(
            doubleArrayOf(200.0, 140.0),
            foreignObject.size,
        )
    }

    @Test
    fun invalidSizeAndDuplicateIdReturnStructuredFailures() {
        val board = board()
        assertIs<GMResult.Err<ForeignObjectError.InvalidSizeCount>>(
            ForeignObject.create(
                board = board,
                content = "bad",
                coordinates = doubleArrayOf(0.0, 0.0),
                sizeTerms = sizeTerms(1.0),
            ),
        )
        foreignObject(
            ForeignObject.create(
                board = board,
                content = "first",
                coordinates = doubleArrayOf(0.0, 0.0),
                sizeTerms = emptyList(),
                id = "duplicate",
            ),
        )
        val duplicate = assertIs<GMResult.Err<ForeignObjectError>>(
            ForeignObject.create(
                board = board,
                content = "second",
                coordinates = doubleArrayOf(1.0, 1.0),
                sizeTerms = emptyList(),
                id = "duplicate",
            ),
        )

        assertIs<ForeignObjectError.Registration>(duplicate.error)
        assertEquals(1, board.objects.size)
    }

    private fun sizeTerms(
        vararg values: Double,
    ) = values.map(::JessieCodeNumericCoordinateFunction)

    private fun foreignObject(
        result: GMResult<ForeignObject, ForeignObjectError>,
    ): ForeignObject =
        assertIs<GMResult.Ok<ForeignObject>>(result).value

    private fun board(): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = "board",
    )
}
