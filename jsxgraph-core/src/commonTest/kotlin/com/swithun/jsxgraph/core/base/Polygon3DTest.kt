package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Polygon3DTest {
    @Test
    fun directPolygonUsesOpen3DVerticesAndClosed2DProxy() {
        val board = createBoard()
        val view = createView(board)
        val first = point(view, doubleArrayOf(-2.0, -1.0, 0.0), "first")
        val second = point(view, doubleArrayOf(2.0, -1.0, 1.0), "second")
        val owned = point(view, doubleArrayOf(1.0, 3.0, 2.0), "owned")
        val polygon = polygon(
            Polygon3D.create(
                view = view,
                vertices = listOf(first, second, owned),
                ownedVertices = setOf(owned),
                id = "polygon",
                name = "",
            ),
        )

        assertEquals(3, polygon.vertices.size)
        assertEquals(4, polygon.polygon2D.vertices.size)
        assertSame(
            polygon.polygon2D.vertices.first(),
            polygon.polygon2D.vertices.last(),
        )
        assertEquals(3, polygon.polygon2D.borders.size)
        assertEquals(
            listOf(owned.point2D),
            polygon.polygon2D.implicitVertices,
        )
        assertTrue(polygon.id in first.descendants)
        assertTrue(owned.id in polygon.descendants)
        assertFalse(polygon.polygon2D.dump)
        assertEquals(listOf(polygon.id), polygon.polygon2D.parents)

        val centroid = doubleArrayOf(
            1.0,
            1.0 / 3.0,
            1.0 / 3.0,
            1.0,
        )
        assertEquals(
            Mat.innerProduct(view.matrix3DRotShift[3], centroid),
            polygon.updateZIndex().zIndex,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun transformedPolygonUsesUpstreamLengthMinusOneLoop() {
        val board = createBoard()
        val view = createView(board)
        val baseVertices = listOf(
            point(
                view,
                doubleArrayOf(-2.0, -1.0, 0.0),
                "a",
                "A",
            ),
            point(
                view,
                doubleArrayOf(2.0, -1.0, 0.0),
                "b",
                "B",
            ),
            point(
                view,
                doubleArrayOf(2.0, 3.0, 0.0),
                "c",
                "C",
            ),
            point(
                view,
                doubleArrayOf(-2.0, 3.0, 0.0),
                "d",
                "D",
            ),
        )
        val base = polygon(
            Polygon3D.create(
                view = view,
                vertices = baseVertices,
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
        val transformed = polygon(
            Polygon3D.create(
                view = view,
                base = base,
                transformations = listOf(translation),
                vertexAttributes = Polygon3DVertexAttributes(
                    withLabel = true,
                ),
                id = "transformed",
                name = "",
            ),
        )

        assertEquals(3, transformed.vertices.size)
        assertEquals(listOf("A'", "B'", "C'"), transformed.vertices.map {
            it.name
        })
        assertContentEquals(
            doubleArrayOf(1.0, -1.0, 1.0, 3.0),
            transformed.vertices[0].coords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 1.0, 3.0),
            transformed.vertices[1].coords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 5.0, 3.0),
            transformed.vertices[2].coords,
        )
        assertEquals(listOf(base.id), transformed.parents)
        assertTrue(transformed.id in base.descendants)
        assertTrue(transformed.vertices.all {
            it in transformed.ownedVertices
        })
    }

    @Test
    fun failedCreationRollsBackOwnedVerticesAndRemovalKeepsExternalOnes() {
        val board = createBoard()
        val view = createView(board)
        val duplicate = point(
            view,
            doubleArrayOf(0.0, 0.0, 0.0),
            "duplicate",
        )
        val generated = point(
            view,
            doubleArrayOf(1.0, 0.0, 0.0),
            "generated",
        )

        assertIs<GMResult.Err<Polygon3DError.Registration>>(
            Polygon3D.create(
                view = view,
                vertices = listOf(generated),
                ownedVertices = setOf(generated),
                id = duplicate.id,
                name = "",
            ),
        )
        assertNull(board.elementById(generated.id))
        assertNull(view.objects[generated.id])

        val owned = point(
            view,
            doubleArrayOf(0.0, 1.0, 0.0),
            "owned",
        )
        val polygon = polygon(
            Polygon3D.create(
                view = view,
                vertices = listOf(duplicate, owned),
                ownedVertices = setOf(owned),
                id = "polygon",
                name = "",
            ),
        )
        val proxyId = polygon.polygon2D.id
        val borderIds = polygon.polygon2D.borders.map(Line::id)
        val ownedProxyId = owned.point2D.id

        board.removeObject(polygon)

        assertNull(board.elementById("polygon"))
        assertNull(board.elementById(proxyId))
        assertTrue(borderIds.all { board.elementById(it) == null })
        assertNull(board.elementById(owned.id))
        assertNull(board.elementById(ownedProxyId))
        assertSame(duplicate, board.elementById(duplicate.id))
        assertSame(duplicate, view.objects[duplicate.id])
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
        name: String = "",
    ): Point3D =
        assertIs<GMResult.Ok<Point3D>>(
            Point3D.create(
                view = view,
                coordinates = coordinates,
                id = id,
                name = name,
            ),
        ).value

    private fun polygon(
        result: GMResult<Polygon3D, Polygon3DError>,
    ): Polygon3D =
        assertIs<GMResult.Ok<Polygon3D>>(result).value
}
