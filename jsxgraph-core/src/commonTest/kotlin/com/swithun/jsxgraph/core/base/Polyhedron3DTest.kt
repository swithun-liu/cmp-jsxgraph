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

class Polyhedron3DTest {
    @Test
    fun facesShareVerticesAndUseOfficialClosureAndNormalRules() {
        val board = createBoard()
        val view = createView(board)
        val point = point(
            view = view,
            coordinates = doubleArrayOf(-2.0, -1.0, 0.0),
            id = "a",
        )
        val polyhedron = polyhedron(
            Polyhedron3D.create(
                view = view,
                vertices = linkedMapOf(
                    "a" to Polyhedron3DVertexSource.Point(point),
                    "b" to values(2.0, -1.0, 0.0),
                    "c" to Polyhedron3DVertexSource.Function(
                        Line3DArrayEvaluator {
                            GMResult.Ok(doubleArrayOf(2.0, 3.0, 0.0))
                        },
                    ),
                    "d" to values(1.0, -2.0, 3.0, 0.0),
                ),
                faceInputs = listOf(
                    Polyhedron3DFaceInput(
                        vertexKeys = listOf("a", "b", "c"),
                        attributes = Face3DAttributes(
                            fillColor = "red",
                        ),
                    ),
                    Polyhedron3DFaceInput(
                        vertexKeys = listOf("a", "c"),
                    ),
                    Polyhedron3DFaceInput(
                        vertexKeys = listOf("d"),
                    ),
                ),
                dependencies = listOf(point),
                id = "polyhedron",
                name = "",
            ),
        )

        board.fullUpdate()

        assertEquals(3, polyhedron.numberFaces)
        assertEquals(listOf(4, 2, 2), polyhedron.faces.map {
            it.curve2D.numberPoints
        })
        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            polyhedron.faces[0].normal,
        )
        assertEquals("red", polyhedron.faces[0].faceAttributes.fillColor)
        assertFalse(polyhedron.faces[0].faceAttributes.dashScale)
        assertTrue(polyhedron.id in point.descendants)
        assertSame(
            polyhedron.faces[0].curve2D,
            polyhedron.faces[0].element2D,
        )
        assertFalse(polyhedron.faces[0].curve2D.dump)
        assertEquals(listOf(polyhedron.id), polyhedron.faces[0].parents)
    }

    @Test
    fun transformedPolyhedronTracksDynamicBaseCoordinates() {
        val board = createBoard()
        val view = createView(board)
        var x = -2.0
        val base = polyhedron(
            Polyhedron3D.create(
                view = view,
                vertices = linkedMapOf(
                    "a" to Polyhedron3DVertexSource.Function(
                        Line3DArrayEvaluator {
                            GMResult.Ok(doubleArrayOf(x, -1.0, 0.0))
                        },
                    ),
                    "b" to values(2.0, -1.0, 0.0),
                    "c" to values(2.0, 3.0, 0.0),
                ),
                faceInputs = listOf(
                    Polyhedron3DFaceInput(listOf("a", "b", "c")),
                ),
                id = "base",
                name = "",
            ),
        )
        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create3D(
                type = "translate",
                parameters = doubleArrayOf(1.0, 2.0, 3.0),
            ),
        ).value
        val transformed = polyhedron(
            Polyhedron3D.create(
                view = view,
                base = base,
                transformations = listOf(translation),
                faceAttributes = listOf(
                    Face3DAttributes(fillColor = "blue"),
                ),
                id = "transformed",
                name = "",
            ),
        )

        board.fullUpdate()
        assertContentEquals(
            doubleArrayOf(1.0, -1.0, 1.0, 3.0),
            transformed.definition.coords.getValue("a"),
        )
        assertEquals(listOf("base"), transformed.parents)
        assertSame(base.faces[0], transformed.faces[0].baseElement)
        assertEquals("blue", transformed.faces[0].faceAttributes.fillColor)

        x = 4.0
        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 1.0, 3.0),
            transformed.definition.coords.getValue("a"),
        )
    }

    @Test
    fun faceFailureRollsBackPreviouslyCreatedFacesAndCurves() {
        val board = createBoard()
        val view = createView(board)
        val before = board.objects.keys.toSet()

        val error = assertIs<
            GMResult.Err<Polyhedron3DError.FaceFactory>
            >(
            Polyhedron3D.create(
                view = view,
                vertices = linkedMapOf(
                    "a" to values(0.0, 0.0, 0.0),
                    "b" to values(1.0, 0.0, 0.0),
                    "c" to values(0.0, 1.0, 0.0),
                ),
                faceInputs = listOf(
                    Polyhedron3DFaceInput(
                        vertexKeys = listOf("a", "b", "c"),
                        attributes = Face3DAttributes(id = "duplicate"),
                    ),
                    Polyhedron3DFaceInput(
                        vertexKeys = listOf("a", "c", "b"),
                        attributes = Face3DAttributes(id = "duplicate"),
                    ),
                ),
            ),
        ).error

        assertEquals(1, error.faceNumber)
        assertEquals(before, board.objects.keys)
        assertNull(board.elementById("duplicate"))
        assertTrue(view.objects.isEmpty())
    }

    @Test
    fun removalCleansFacesCurvesAndViewRegistry() {
        val board = createBoard()
        val view = createView(board)
        val polyhedron = polyhedron(
            Polyhedron3D.create(
                view = view,
                vertices = linkedMapOf(
                    "a" to values(0.0, 0.0, 0.0),
                    "b" to values(1.0, 0.0, 0.0),
                    "c" to values(0.0, 1.0, 0.0),
                ),
                faceInputs = listOf(
                    Polyhedron3DFaceInput(listOf("a", "b", "c")),
                ),
                id = "polyhedron",
                name = "",
            ),
        )
        val faceId = polyhedron.faces.single().id
        val curveId = polyhedron.faces.single().curve2D.id

        board.removeObject(polyhedron)

        assertNull(board.elementById("polyhedron"))
        assertNull(board.elementById(faceId))
        assertNull(board.elementById(curveId))
        assertTrue(view.objects.isEmpty())
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
        id: String,
    ): Point3D =
        assertIs<GMResult.Ok<Point3D>>(
            Point3D.create(
                view = view,
                coordinates = coordinates,
                id = id,
                name = "",
            ),
        ).value

    private fun values(
        vararg coordinates: Double,
    ): Polyhedron3DVertexSource =
        Polyhedron3DVertexSource.Values(
            coordinates.map(Line3DCoordinateValue::Numeric),
        )

    private fun polyhedron(
        result: GMResult<Polyhedron3D, Polyhedron3DError>,
    ): Polyhedron3D =
        assertIs<GMResult.Ok<Polyhedron3D>>(result).value
}
