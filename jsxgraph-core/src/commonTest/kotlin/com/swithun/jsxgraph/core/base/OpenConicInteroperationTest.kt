/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OpenConicInteroperationTest {
    @Test
    fun hyperbolaInteroperationMatchesOfficialLifecycle() {
        val board = board("hyperbola-interoperation")
        val focus1 = point(board, -3.0, 0.0, "focus1")
        val focus2 = point(board, 3.0, 0.0, "focus2")
        val onPoint = point(board, 5.0, 2.0, "onPoint")
        val hyperbola = assertIs<GMResult.Ok<Curve>>(
            Hyperbola.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                pointOnHyperbola = onPoint,
                id = "hyperbola",
                name = "",
            ),
        ).value
        val generatedPoint = point(
            board,
            hyperbola.X(0.0),
            hyperbola.Y(0.0),
            "generatedPoint",
        )
        val offPoint = point(board, 0.0, 0.0, "offPoint")
        val externalPoint = point(board, 0.0, 6.0, "externalPoint")
        val sourceLine = line(
            board = board,
            first = point(board, -8.0, 2.0, "lineStart"),
            second = point(board, 8.0, 2.0, "lineEnd"),
            id = "sourceLine",
        )
        val elements = createInteroperationElements(
            board = board,
            conic = hyperbola,
            onPoint = onPoint,
            externalPoint = externalPoint,
            sourceLine = sourceLine,
            gliderCoordinates = doubleArrayOf(4.0, 3.0),
        )
        board.update()

        assertFalse(onPoint.isOn(hyperbola, 1.0e-6))
        assertTrue(generatedPoint.isOn(hyperbola, 1.0e-6))
        assertFalse(offPoint.isOn(hyperbola, 1.0e-6))
        assertInteroperation(
            elements = elements,
            tangent = doubleArrayOf(
                0.7230942614110065,
                -0.49269881498359286,
                0.870199906753479,
            ),
            polar = doubleArrayOf(0.2769839649484333, 0.0, 1.0),
            normal = doubleArrayOf(
                3.5380897161734772,
                0.8076358014515694,
                -0.5896816193622462,
            ),
            pole = doubleArrayOf(0.0, -0.8309518948452999),
            firstIntersection = doubleArrayOf(
                5.000001610098847,
                2.0000009116224153,
            ),
            otherIntersection = doubleArrayOf(
                -5.00000041175918,
                2.0000002331340814,
            ),
            tangentToPoint = doubleArrayOf(
                -2.7707133297693725,
                -0.2769840936745115,
            ),
            tangentTo = doubleArrayOf(
                2.422906493644485,
                0.914839370638682,
                -0.4038179366118104,
            ),
            glider = doubleArrayOf(
                4.68769137241946,
                1.8206551487985283,
            ),
            gliderPosition = -2.9090497359101146,
        )

        onPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(6.0, 3.0),
        )
        externalPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 7.0),
        )
        board.update()
        board.update()

        assertInteroperation(
            elements = elements,
            tangent = doubleArrayOf(
                0.6024342476586817,
                -0.5257311121191329,
                0.8506508083520403,
            ),
            polar = doubleArrayOf(
                0.3032206557353392,
                -0.04410233237811922,
                0.9990270187931904,
            ),
            normal = doubleArrayOf(
                4.7929633876184745,
                -0.8236910055276598,
                -0.5670389117272578,
            ),
            pole = doubleArrayOf(0.0, -1.0623058987490528),
            firstIntersection = doubleArrayOf(
                4.45192943407661,
                2.000001203118418,
            ),
            otherIntersection = doubleArrayOf(
                -4.451928194167831,
                2.000000350234066,
            ),
            tangentToPoint = doubleArrayOf(
                -2.730813859995609,
                -0.42406878667548226,
            ),
            tangentTo = doubleArrayOf(
                2.2496242145530725,
                0.8935211807600576,
                -0.4490210457574928,
            ),
            glider = doubleArrayOf(
                3.9574100884900227,
                1.6477052488345352,
            ),
            gliderPosition = -2.9090497359101146,
        )
        assertRemovalLifecycle(board, hyperbola, elements)
    }

    @Test
    fun parabolaInteroperationMatchesOfficialLifecycle() {
        val board = board("parabola-interoperation")
        val focus = point(board, 1.0, 1.0, "focus")
        val directrixStart = point(board, -1.0, 5.0, "directrixStart")
        val directrixEnd = point(board, -1.0, -5.0, "directrixEnd")
        val directrix = line(
            board = board,
            first = directrixStart,
            second = directrixEnd,
            id = "directrix",
        )
        val onPoint = point(board, 1.0, 3.0, "onPoint")
        val parabola = assertIs<GMResult.Ok<Curve>>(
            Parabola.create(
                board = board,
                focus = focus,
                directrix = directrix,
                id = "parabola",
                name = "",
            ),
        ).value
        val generatedPoint = point(
            board,
            parabola.X(0.0),
            parabola.Y(0.0),
            "generatedPoint",
        )
        val offPoint = point(board, 0.0, 0.0, "offPoint")
        val externalPoint = point(board, -5.0, 1.0, "externalPoint")
        val sourceLine = line(
            board = board,
            first = point(board, 3.0, -6.0, "lineStart"),
            second = point(board, 3.0, 6.0, "lineEnd"),
            id = "sourceLine",
        )
        val elements = createInteroperationElements(
            board = board,
            conic = parabola,
            onPoint = onPoint,
            externalPoint = externalPoint,
            sourceLine = sourceLine,
            gliderCoordinates = doubleArrayOf(3.0, 3.0),
        )
        board.update()

        assertFalse(onPoint.isOn(parabola, 1.0e-6))
        assertFalse(generatedPoint.isOn(parabola, 1.0e-6))
        assertFalse(offPoint.isOn(parabola, 1.0e-6))
        assertInteroperation(
            elements = elements,
            tangent = doubleArrayOf(
                -1.414213562373095,
                -0.7071067811865475,
                0.7071067811865475,
            ),
            polar = doubleArrayOf(5.0, -1.0, 0.0),
            normal = doubleArrayOf(
                -1.000000788452624,
                -1.5769052729841697e-7,
                0.9999999999999876,
            ),
            pole = doubleArrayOf(-3.0, 1.0),
            firstIntersection = doubleArrayOf(
                3.000000915271016,
                -2.4641021435696815,
            ),
            otherIntersection = doubleArrayOf(
                2.999994977344084,
                4.464098715304796,
            ),
            tangentToPoint = doubleArrayOf(
                5.0000001323051615,
                -3.472136014168245,
            ),
            tangentTo = doubleArrayOf(
                -1.1283705526388705,
                -0.4082482859627498,
                -0.9128709311882359,
            ),
            glider = doubleArrayOf(
                2.31459381309344,
                4.042757836630079,
            ),
            gliderPosition = 2.7337740076519452,
        )

        focus.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        directrixStart.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 5.0),
        )
        directrixEnd.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -5.0),
        )
        externalPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        board.update()
        board.update()

        assertInteroperation(
            elements = elements,
            tangent = doubleArrayOf(
                -1.4552137502179978,
                -0.970142500145332,
                0.24253562503633214,
            ),
            polar = doubleArrayOf(6.0, -1.0, 0.0),
            normal = doubleArrayOf(
                -2.0000009824141234,
                -1.637356916673394e-7,
                0.9999999999999867,
            ),
            pole = doubleArrayOf(-3.0, 2.0),
            firstIntersection = doubleArrayOf(
                3.0000013002446586,
                -2.8989805472115595,
            ),
            otherIntersection = doubleArrayOf(
                2.9999980256868954,
                6.898977873546191,
            ),
            tangentToPoint = doubleArrayOf(
                6.000001830542032,
                -4.928204287139363,
            ),
            tangentTo = doubleArrayOf(
                -1.2679496984214824,
                -0.49999994279557164,
                -0.8660254368114282,
            ),
            glider = doubleArrayOf(
                4.629184218715164,
                8.08551343353388,
            ),
            gliderPosition = 2.7337740076519452,
        )
        assertRemovalLifecycle(board, parabola, elements)
    }

    private fun createInteroperationElements(
        board: Board,
        conic: Curve,
        onPoint: Point,
        externalPoint: Point,
        sourceLine: Line,
        gliderCoordinates: DoubleArray,
    ): InteroperationElements {
        val tangent = assertIs<GMResult.Ok<Line>>(
            Tangent.create(
                board = board,
                firstParent = onPoint,
                secondParent = conic,
                id = "tangent",
                point1Id = "tangentPoint1",
                point2Id = "tangentPoint2",
            ),
        ).value
        val polar = assertIs<GMResult.Ok<Line>>(
            Tangent.createPolarLine(
                board = board,
                firstParent = conic,
                secondParent = externalPoint,
                id = "polar",
                point1Id = "polarPoint1",
                point2Id = "polarPoint2",
            ),
        ).value
        val normal = assertIs<GMResult.Ok<Line>>(
            Normal.create(
                board = board,
                firstParent = externalPoint,
                secondParent = conic,
                id = "normal",
                point1Id = "normalPoint1",
                point2Id = "normalPoint2",
            ),
        ).value
        val pole = assertIs<GMResult.Ok<Point>>(
            PolePoint.create(
                board = board,
                firstParent = conic,
                secondParent = sourceLine,
                id = "pole",
            ),
        ).value
        val firstIntersection = assertIs<GMResult.Ok<IntersectionPoint>>(
            IntersectionPoint.create(
                board = board,
                first = sourceLine,
                second = conic,
                id = "firstIntersection",
            ),
        ).value
        val otherIntersection =
            assertIs<GMResult.Ok<OtherIntersectionPoint>>(
                OtherIntersectionPoint.create(
                    board = board,
                    first = conic,
                    second = sourceLine,
                    excludedPoints = listOf(firstIntersection),
                    id = "otherIntersection",
                ),
            ).value
        val tangentTo = assertIs<GMResult.Ok<Line>>(
            TangentTo.create(
                board = board,
                conic = conic,
                pointFrom = externalPoint,
                tangentAttributes = TangentToLineAttributes(
                    identity = TangentToIdentity(id = "tangentTo"),
                ),
            ),
        ).value
        val glider = assertIs<GMResult.Ok<Glider>>(
            Glider.create(
                board = board,
                coordinates = gliderCoordinates,
                slideObject = conic,
                id = "glider",
                name = "",
            ),
        ).value
        return InteroperationElements(
            tangent = tangent,
            polar = polar,
            normal = normal,
            pole = pole,
            firstIntersection = firstIntersection,
            otherIntersection = otherIntersection,
            tangentTo = tangentTo,
            tangentToPoint = assertIs<Point>(tangentTo.tangentToPoint),
            glider = glider,
        )
    }

    private fun assertInteroperation(
        elements: InteroperationElements,
        tangent: DoubleArray,
        polar: DoubleArray,
        normal: DoubleArray,
        pole: DoubleArray,
        firstIntersection: DoubleArray,
        otherIntersection: DoubleArray,
        tangentToPoint: DoubleArray,
        tangentTo: DoubleArray,
        glider: DoubleArray,
        gliderPosition: Double,
    ) {
        assertStandardForm(tangent, elements.tangent)
        assertStandardForm(polar, elements.polar)
        assertStandardForm(normal, elements.normal)
        assertPoint(pole, elements.pole)
        assertPoint(firstIntersection, elements.firstIntersection)
        assertPoint(otherIntersection, elements.otherIntersection)
        assertPoint(tangentToPoint, elements.tangentToPoint)
        assertStandardForm(tangentTo, elements.tangentTo)
        assertPoint(glider, elements.glider)
        assertEquals(
            gliderPosition,
            assertIs<Double>(elements.glider.position),
            absoluteTolerance = CONIC_TOLERANCE,
        )
    }

    private fun assertRemovalLifecycle(
        board: Board,
        conic: Curve,
        elements: InteroperationElements,
    ) {
        board.removeObject(conic)
        assertNull(board.elementById(conic.id))
        assertSame(elements.tangent, board.elementById(elements.tangent.id))
        assertSame(elements.polar, board.elementById(elements.polar.id))
        for (
            element in listOf(
                elements.normal,
                elements.pole,
                elements.firstIntersection,
                elements.otherIntersection,
                elements.tangentTo,
                elements.glider,
            )
        ) {
            assertNull(board.elementById(element.id), element.id)
        }
    }

    private fun assertStandardForm(
        expected: DoubleArray,
        actual: Line,
    ) {
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual.stdform[index],
                absoluteTolerance = CONIC_TOLERANCE,
                message = "${actual.id}.stdform[$index]",
            )
        }
    }

    private fun assertPoint(
        expected: DoubleArray,
        actual: Point,
    ) {
        assertEquals(
            expected[0],
            actual.X(),
            absoluteTolerance = CONIC_TOLERANCE,
            message = "${actual.id}.X",
        )
        assertEquals(
            expected[1],
            actual.Y(),
            absoluteTolerance = CONIC_TOLERANCE,
            message = "${actual.id}.Y",
        )
    }

    private fun board(id: String): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        canvasWidth = 640.0,
        canvasHeight = 480.0,
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
            name = "",
        ),
    ).value

    private fun line(
        board: Board,
        first: Point,
        second: Point,
        id: String,
    ): Line = assertIs<GMResult.Ok<Line>>(
        Line.create(
            board = board,
            point1 = first,
            point2 = second,
            id = id,
            name = "",
        ),
    ).value

    private data class InteroperationElements(
        val tangent: Line,
        val polar: Line,
        val normal: Line,
        val pole: Point,
        val firstIntersection: IntersectionPoint,
        val otherIntersection: OtherIntersectionPoint,
        val tangentTo: Line,
        val tangentToPoint: Point,
        val glider: Glider,
    )

    private companion object {
        const val CONIC_TOLERANCE = 2.0e-5
    }
}
