package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Mesh3DTest {
    @Test
    fun meshUsesOfficialTwoFamilySamplingAndFourDimensionalInputs() {
        val board = createBoard()
        val view = createView(board)
        val point = point(view, doubleArrayOf(1.0, 2.0, 2.0))
        val mesh = mesh(
            Mesh3D.create(
                view = view,
                pointSource = Mesh3DPointSource.Point(point),
                direction1Source = vector(0.0, 1.0, 0.0, 0.0),
                direction2Source = vector(0.0, 0.0, 1.0, 1.0),
                rangeUSource = range(-2.0, 3.0),
                rangeVSource = range(-1.0, 2.0),
                id = "mesh",
                name = "",
            ),
        )

        board.fullUpdate()

        assertTrue(mesh.isMesh3D)
        assertEquals("curve", mesh.elType)
        assertEquals(58, mesh.numberPoints)
        assertEquals(58L, mesh.requestedPointCount())
        assertTrue(mesh.points[4].usrCoords[1].isNaN())
        assertTrue(mesh.points[4].usrCoords[2].isNaN())
        assertTrue(mesh.points[29].usrCoords[1].isNaN())
        assertTrue(mesh.points[36].usrCoords[1].isNaN())
        assertTrue(mesh.points[43].usrCoords[1].isNaN())
        assertTrue(mesh.points[50].usrCoords[1].isNaN())
        assertTrue(mesh.points[57].usrCoords[1].isNaN())

        assertEquals(
            -0.36429513147350645,
            mesh.points.first().usrCoords[1],
            TOLERANCE,
        )
        assertEquals(
            -0.8348490315008023,
            mesh.points.first().usrCoords[2],
            TOLERANCE,
        )
    }

    @Test
    fun meshTracksPointDependencyAcrossBoardUpdates() {
        val board = createBoard()
        val view = createView(board)
        val point = point(view, doubleArrayOf(0.0, 0.0, 0.0))
        val mesh = mesh(
            Mesh3D.create(
                view = view,
                pointSource = Mesh3DPointSource.Point(point),
                direction1Source = vector(1.0, 0.0, 0.0),
                direction2Source = vector(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                id = "mesh",
                name = "",
            ),
        )
        board.fullUpdate()
        val initial = mesh.points.first().usrCoords.copyOf()

        assertIs<GMResult.Ok<Point3D>>(
            point.setPosition(doubleArrayOf(2.0, -3.0, 4.0)),
        )
        board.fullUpdate()

        assertNotEquals(initial[1], mesh.points.first().usrCoords[1])
        assertNotEquals(initial[2], mesh.points.first().usrCoords[2])
        assertTrue(mesh.id in point.descendants)
    }

    @Test
    fun invalidAndOversizedGridsReturnStructuredErrorsBeforeSampling() {
        val view = createView(createBoard())

        assertIs<GMResult.Err<Mesh3DError.InvalidStepWidth>>(
            Mesh3D.create(
                view = view,
                pointSource = pointValues(0.0, 0.0, 0.0),
                direction1Source = vector(1.0, 0.0, 0.0),
                direction2Source = vector(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                stepWidthU = 0.0,
            ),
        )
        val oversized = assertIs<
            GMResult.Err<Mesh3DError.SampleLimitExceeded>
            >(
            Mesh3D.create(
                view = view,
                pointSource = pointValues(0.0, 0.0, 0.0),
                direction1Source = vector(1.0, 0.0, 0.0),
                direction2Source = vector(0.0, 1.0, 0.0),
                rangeUSource = range(-100.0, 100.0),
                rangeVSource = range(-100.0, 100.0),
            ),
        ).error
        assertTrue(oversized.count > Curve.MAX_SAMPLE_COUNT)
        assertEquals(Curve.MAX_SAMPLE_COUNT, oversized.maximum)
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

    private fun point(
        view: View3D,
        coordinates: DoubleArray,
    ): Point3D =
        assertIs<GMResult.Ok<Point3D>>(
            Point3D.create(
                view = view,
                coordinates = coordinates,
                id = "point",
                name = "",
            ),
        ).value

    private fun mesh(
        result: GMResult<Curve, Mesh3DError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun pointValues(
        vararg values: Double,
    ): Mesh3DPointSource =
        Mesh3DPointSource.Values(
            values.map(Line3DCoordinateValue::Numeric),
        )

    private fun vector(
        vararg values: Double,
    ): Mesh3DVectorSource =
        Mesh3DVectorSource.Values(
            values.map(Line3DCoordinateValue::Numeric),
        )

    private fun range(
        start: Double,
        end: Double,
    ): List<Line3DCoordinateValue> =
        listOf(
            Line3DCoordinateValue.Numeric(start),
            Line3DCoordinateValue.Numeric(end),
        )

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
