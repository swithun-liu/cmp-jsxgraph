/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Slider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TracenpocheReaderTest {
    @Test
    fun officialPointPolygonAndTriangleExamplesCreateGeometry() {
        val drawn = read(
            """
            @options;
            @figure;
              A = point(-4, 0.23);
              B = point(1.57, 0.23);
              sAB = segment(A, B);
              C = point(0.8, 5.77);
              D = point(-5.93, 2.2);
              dCD = droite(C, D);
              polyABCD = polygone(A, B, C, D);
              medsAB = mediatrice(sAB);
              medsBC = mediatrice(B, C);
              O = intersection(medsAB, medsBC);
              ceOA = cercle(O, A);
              bissCAB = bissectrice(C, A, B);
              bissABC = bissectrice(A, B, C);
              J = intersection(bissABC, bissCAB);
              foot = projete(J, sAB);
              inner = cercle(J, foot);
              perpCsAB = perpendiculaire(C, sAB);
              perpBsCA = perpendiculaire(B, dCD);
              H = intersection(perpBsCA, perpCsAB);
            """.trimIndent(),
        )

        val first = assertIs<Point>(drawn.objects.getValue("A"))
        assertEquals(-4.0, first.X(), TOLERANCE)
        assertEquals(0.23, first.Y(), TOLERANCE)
        val segment = assertIs<Line>(drawn.objects.getValue("sAB"))
        assertSame(first, segment.point1)
        assertEquals(false, segment.straightFirst)
        assertEquals(false, segment.straightLast)
        val polygon = assertIs<Polygon>(
            drawn.objects.getValue("polyABCD"),
        )
        assertEquals(5, polygon.vertices.size)
        assertIs<Line>(drawn.objects.getValue("medsAB"))
        assertIs<Line>(drawn.objects.getValue("medsBC"))
        assertIs<Point>(drawn.objects.getValue("O"))
        assertIs<Circle>(drawn.objects.getValue("ceOA"))
        assertIs<Point>(drawn.objects.getValue("foot"))
        assertIs<Circle>(drawn.objects.getValue("inner"))
        assertIs<Point>(drawn.objects.getValue("H"))
        assertEquals(
            listOf(-10.0, 10.0, 10.0, -10.0),
            drawn.objects.getValue("A").board
                .getBoundingBox()
                .toList(),
        )
    }

    @Test
    fun officialDynamicAndTransformationBranchesMatchFixture() {
        val board = board()
        val drawn = read(ADVANCED_SOURCE, board)

        val slider = assertIs<Slider>(drawn.objects.getValue("r"))
        val point = assertIs<Point>(drawn.objects.getValue("P"))
        val tangent = assertIs<Line>(
            drawn.objects.getValue("tanline"),
        )
        val function = assertIs<Curve>(drawn.objects.getValue("f"))

        assertEquals(0.5, slider.Value(), TOLERANCE)
        assertPoint(2.0, 3.0, point)
        assertEquals("tangent", tangent.elType)
        assertEquals(
            2.7963024898437903,
            function.Y(0.5),
            1.0e-10,
        )
        assertPoint(
            0.0,
            0.0,
            assertIs(drawn.objects.getValue("Q")),
        )
        assertPoint(
            5.0,
            6.0,
            assertIs(drawn.objects.getValue("hImage")),
        )
        assertPoint(
            -1.0,
            4.0,
            assertIs(drawn.objects.getValue("rImage")),
        )
        assertPoint(
            -1.0,
            0.0,
            assertIs(drawn.objects.getValue("sImage")),
        )
        assertPoint(
            3.0,
            4.0,
            assertIs(drawn.objects.getValue("tImage")),
        )
        assertPoint(
            3.0,
            -4.0,
            assertIs(drawn.objects.getValue("fImage")),
        )
        assertPoint(
            8.0,
            7.0,
            assertIs(drawn.objects.getValue("N")),
        )
        assertIs<TracenpocheValue.Transform>(
            drawn.values.getValue("homo"),
        )
        val initialTangent = tangent.stdform.copyOf()

        slider.setValue(1.25)
        board.fullUpdate()

        assertEquals(1.25, slider.Value(), TOLERANCE)
        assertPoint(3.5, 4.5, point)
        assertNotEquals(
            initialTangent.toList(),
            tangent.stdform.toList(),
        )
    }

    @Test
    fun indexedLoopMatchesOfficialPolygonPattern() {
        val drawn = read(
            """
            @figure;
              A0=point(0,0);
              B0=point(10,0);
              for i=0 to 2 do;
                p=0.25*([i+1]);
                A[i+1]=pointsur(A0,B0,p){sansnom,i};
              end;
              poly=polygone(A0,A1,A2,A3,B0);
            """.trimIndent(),
        )

        assertPoint(2.5, 0.0, assertIs(drawn.objects.getValue("A1")))
        assertPoint(5.0, 0.0, assertIs(drawn.objects.getValue("A2")))
        assertPoint(7.5, 0.0, assertIs(drawn.objects.getValue("A3")))
        assertEquals(6, assertIs<Polygon>(
            drawn.objects.getValue("poly"),
        ).vertices.size)
        assertEquals(3.0, assertIs<TracenpocheValue.Number>(
            drawn.values.getValue("i"),
        ).value)
    }

    @Test
    fun failuresRollbackBoardAndResourceLimitsAreStructured() {
        val board = board()
        val seed = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(9.0, 8.0),
                id = "seed",
                name = "seed",
            ),
        ).value
        val originalBoundingBox = board.getBoundingBox()

        assertIs<GMResult.Err<TracenpocheReaderError.UnsupportedFunction>>(
            TracenpocheReader(
                "@figure;A=point(1,2);bad=missing(A);",
            ).read(board),
        )
        assertEquals(listOf(seed), board.objects.values.toList())
        assertSame(seed, board.elementByName("seed"))
        assertEquals(
            originalBoundingBox.toList(),
            board.getBoundingBox().toList(),
        )

        assertIs<
            GMResult.Err<TracenpocheReaderError.ObjectLimitExceeded>
        >(
            TracenpocheReader("@figure;A=point(1,2);").read(
                board = board,
                limits = TracenpocheReaderLimits(maxObjects = 0),
            ),
        )
        assertEquals(listOf(seed), board.objects.values.toList())

        assertIs<
            GMResult.Err<TracenpocheReaderError.LoopLimitExceeded>
        >(
            TracenpocheReader(
                "@figure;for i=1 to 2 do;A[i]=point(i,i);end;",
            ).read(
                board = board,
                limits = TracenpocheReaderLimits(
                    maxLoopIterations = 1,
                ),
            ),
        )
        assertEquals(listOf(seed), board.objects.values.toList())
        assertIs<GMResult.Err<TracenpocheReaderError.InvalidLimits>>(
            TracenpocheReader("").read(
                board = board,
                limits = TracenpocheReaderLimits(maxObjects = -1),
            ),
        )
    }

    @Test
    fun registryReadsTracenpocheAndContainsDomainFailures() {
        val registry = ReaderRegistry<Board>().apply {
            registerTracenpocheReader()
        }
        val board = board()

        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source = "@figure;A=point(1,2);",
                board = board,
                format = "TRACENPOCHE",
                registry = registry,
            ),
        )
        assertIs<Point>(board.elementByName("A"))
        assertIs<GMResult.Err<ReaderError.DomainFailure>>(
            FileReader.parseString(
                source = "@figure;bad=missing(1);",
                board = board(),
                format = "tracenpoche",
                registry = registry,
            ),
        )
    }

    private fun read(
        source: String,
        board: Board = board(),
    ): DrawnTracenpoche =
        assertIs<GMResult.Ok<DrawnTracenpoche>>(
            TracenpocheReader(source).read(board),
        ).value

    private fun assertPoint(
        expectedX: Double,
        expectedY: Double,
        actual: Point,
    ) {
        assertEquals(expectedX, actual.X(), TOLERANCE)
        assertEquals(expectedY, actual.Y(), TOLERANCE)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )

    private companion object {
        const val TOLERANCE = 1.0e-8

        val ADVANCED_SOURCE = listOf(
            "@options;",
            "@figure;",
            "A=point(1,2);",
            "B=point(3,4);",
            "C=point(0,3);",
            "X1=point(-2,0);",
            "X2=point(2,0);",
            "Y1=point(2,-2);",
            "Y2=point(2,2);",
            "target=droite(X1,X2);",
            "direction=droite(Y1,Y2);",
            "r=reel(0.5,0,2,0.25){sansnom};",
            "P=pointsur(A,B,r);",
            "midline=mediatrice(A,B);",
            "Q=projete(C,target,direction);",
            "f=fonction(carre(x)+racine(4)+tan(x));",
            "tanline=tangente(f,r);",
            "homo=homothetie(A,2);",
            "hImage=image(homo,B);",
            "turn=rotation(A,90);",
            "rImage=image(turn,B);",
            "mirror=symetrie(A);",
            "sImage=image(mirror,B);",
            "shift=translation(A,B);",
            "tImage=image(shift,A);",
            "flip=reflexion(target);",
            "fImage=image(flip,B);",
            "N=image(7,8);",
        ).joinToString("\n")
    }
}
