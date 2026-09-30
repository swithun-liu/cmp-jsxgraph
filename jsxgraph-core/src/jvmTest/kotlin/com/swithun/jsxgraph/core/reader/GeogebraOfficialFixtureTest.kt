/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Text
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeogebraOfficialFixtureTest {
    @Test
    fun readsUpstreamBspncFixtureAndKeepsTextDynamic() {
        val fixture = fixture("geogebra.xml")
        assertTrue(Files.isRegularFile(fixture))

        val board = Board(
            originX = 250.0,
            originY = 250.0,
            unitX = 50.0,
            unitY = 50.0,
            canvasWidth = 500.0,
            canvasHeight = 500.0,
        )
        val result = GeogebraReader(
            Files.readString(fixture),
        ).read(board)
        val drawn = assertIs<GMResult.Ok<DrawnGeogebra>>(
            result,
            result.toString(),
        ).value

        assertIs<Curve>(drawn.objects.getValue("f"))
        val sourcePoint = assertIs<Point>(
            drawn.objects.getValue("T"),
        )
        val tangent = assertIs<Line>(drawn.objects.getValue("t"))
        assertIs<Text>(drawn.objects.getValue("k"))
        val text = assertIs<Text>(drawn.objects.getValue("T1"))
        assertEquals(
            1.0,
            tangent.Slope(),
            1.0e-8,
            "T=(${sourcePoint.X()}, ${sourcePoint.Y()}), " +
                "f(9)=${assertIs<Curve>(
                    drawn.objects.getValue("f"),
                ).Y(9.0)}",
        )
        assertEquals("k = 1", text.plaintext)
        assertEquals(9.275, text.X(), 1.0e-8)
        assertEquals(0.85, text.Y(), 1.0e-8)

        sourcePoint.setPositionDirectly(
            method = 1,
            coordinates = doubleArrayOf(6.0, 2.0),
        )
        board.fullUpdate()

        assertEquals("k = 0", text.plaintext)
        assertEquals(6.275, text.X(), 1.0e-8)
        assertEquals(-0.15, text.Y(), 1.0e-8)
    }

    @Test
    fun readsUpstreamEllipseWorksheet() {
        val result = GeogebraReader(
            Files.readString(fixture("ellip_worksheet.xml")),
        ).read(board())
        val drawn = assertIs<GMResult.Ok<DrawnGeogebra>>(
            result,
            result.toString(),
        ).value

        val ellipse = assertIs<Curve>(drawn.objects.getValue("e"))
        assertEquals(10.0, ellipse.majorAxis(), 1.0e-8)
        assertIs<Point>(drawn.objects.getValue("P"))
    }

    private fun fixture(name: String): Path = assertNotNull(
        generateSequence(
            Path.of("").toAbsolutePath(),
            Path::getParent,
        ).map { directory ->
            directory.resolve(
                Path.of(
                    "third_party",
                    "jsxgraph-src",
                    "examples",
                    "bspnc",
                    name,
                ),
            )
        }.firstOrNull(Files::isRegularFile),
    )

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )
}
