package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Axes3DTest {
    @Test
    fun axis3DDelegatesToTwoPointLine3D() {
        val board = createBoard()
        val view = createView(board)
        val axis = assertIs<GMResult.Ok<Line3D>>(
            Axis3D.create(
                view = view,
                start = doubleArrayOf(-5.0, 6.0, -3.0),
                end = doubleArrayOf(5.0, 6.0, -3.0),
                id = "axis",
                name = "x",
            ),
        ).value

        board.fullUpdate()

        assertEquals("line3d", axis.elType)
        assertEquals("axis", axis.id)
        assertEquals("x", axis.name)
        assertContentEquals(
            doubleArrayOf(1.0, -5.0, 6.0, -3.0),
            axis.point1.coords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 6.0, -3.0),
            axis.point2.coords,
        )
        assertTrue(axis.isFixed)
    }

    @Test
    fun borderAxesCreateOfficialAxesTicksPlanesAndFaceAxes() {
        val board = createBoard()
        val view = createView(board)
        val axes = assertIs<GMResult.Ok<Axes3D>>(
            Axes3D.create(
                view = view,
                axesPosition = "border",
            ),
        ).value

        assertEquals("axes3d", axes.elType)
        assertEquals(24, axes.objects.size)
        assertEquals(
            setOf(Axes3D.PLANE_SURFACE_GAP),
            axes.unsupportedFeatures,
        )

        val xAxis = assertIs<Line3D>(axes.member("xAxisBorder"))
        assertContentEquals(
            doubleArrayOf(1.0, -5.0, 6.0, -3.0),
            xAxis.point1.coords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 6.0, -3.0),
            xAxis.point2.coords,
        )
        val xTicks = assertIs<Curve>(
            axes.member("xAxisBorderTicks"),
        )
        assertTrue(xTicks.isTicks3D)
        assertEquals(33, xTicks.numberPoints)
        assertEquals(
            listOf(-5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0),
            xTicks.ticks3DDefinition?.labels?.map { it.value },
        )

        val xRear = assertIs<Plane3D>(axes.member("xPlaneRear"))
        assertEquals("axisplane3d", xRear.elType)
        assertContentEquals(
            doubleArrayOf(1.0, -5.0, 0.0, 0.0),
            xRear.point.coords,
        )
        assertContentEquals(doubleArrayOf(-4.0, 6.0), xRear.evaluatedRangeU)
        assertContentEquals(doubleArrayOf(-3.0, 7.0), xRear.evaluatedRangeV)

        val faceAxis = assertIs<Line3D>(
            axes.member("xPlaneRearYAxis"),
        )
        assertContentEquals(
            doubleArrayOf(1.0, -5.0, -4.0, 0.0),
            faceAxis.point1.coords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, -5.0, 6.0, 0.0),
            faceAxis.point2.coords,
        )
        assertTrue(faceAxis.id in xRear.descendants)

        val memberIds = axes.objects.keys.toList()
        board.removeObject(axes)
        for (id in memberIds) {
            assertNull(board.elementById(id))
        }
    }

    @Test
    fun centerCreatesOfficialOriginIntersectionAndNoneOmitsAxes() {
        val centerBoard = createBoard()
        val centerView = createView(centerBoard)
        val center = assertIs<GMResult.Ok<Axes3D>>(
            Axes3D.create(
                view = centerView,
                axesPosition = "center",
            ),
        ).value
        assertEquals(22, center.objects.size)
        assertEquals(
            setOf(Axes3D.PLANE_SURFACE_GAP),
            center.unsupportedFeatures,
        )
        val xAxis = assertIs<Line3D>(center.member("xAxis"))
        val yAxis = assertIs<Line3D>(center.member("yAxis"))
        val origin = assertIs<IntersectionPoint>(center.member("O"))
        assertSame(xAxis, origin.firstElement)
        assertSame(yAxis, origin.secondElement)
        assertEquals(
            listOf(xAxis.id, yAxis.id),
            origin.parents,
        )
        assertFalse(origin.isDraggable)
        assertFalse(origin.isReal)
        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 0.0),
            origin.coords.usrCoords,
        )
        val centerMemberIds = center.objects.keys.toList()
        centerBoard.removeObject(center)
        for (id in centerMemberIds) {
            assertNull(centerBoard.elementById(id))
        }

        val none = assertIs<GMResult.Ok<Axes3D>>(
            Axes3D.create(
                view = createView(createBoard()),
                axesPosition = "none",
            ),
        ).value
        assertEquals(18, none.objects.size)
        assertEquals(
            setOf(Axes3D.PLANE_SURFACE_GAP),
            none.unsupportedFeatures,
        )
        assertNull(none.member("xAxis"))
    }

    @Test
    fun invalidAxesPositionReturnsStructuredError() {
        assertIs<GMResult.Err<Axes3DError.UnsupportedAxesPosition>>(
            Axes3D.create(
                view = createView(createBoard()),
                axesPosition = "diagonal",
            ),
        )
    }

    @Test
    fun viewOwnsAndRemovesDefaultAxesCreatedByFactoryPhase() {
        val board = createBoard()
        val view = createView(board)
        assertNull(view.defaultAxes)

        val axes = assertIs<GMResult.Ok<Axes3D>>(
            view.createDefaultAxes(
                axesPosition = "border",
                ticksAttributes = mapOf(
                    "xAxisBorderTicks" to
                        Axes3DTicksAttributes(drawLabels = false),
                    "yAxisBorderTicks" to
                        Axes3DTicksAttributes(drawLabels = false),
                    "zAxisBorderTicks" to
                        Axes3DTicksAttributes(drawLabels = false),
                ),
            ),
        ).value
        assertSame(axes, view.defaultAxes)
        val memberIds = axes.objects.keys.toList()

        board.removeObject(view)

        assertNull(board.elementById("view"))
        assertNull(view.defaultAxes)
        for (id in memberIds) {
            assertNull(board.elementById(id))
        }
    }

    private fun createBoard(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun createView(board: Board): View3D =
        assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                projection = "parallel",
                azimuth = 1.0,
                elevation = 0.3,
                bank = 0.0,
                id = "view",
                name = "",
            ),
        ).value
}
