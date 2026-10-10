package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

class ElementReflectionsTest {
    @Test
    fun nonPointReflectionAndMirrorBranchesTrackOfficialTransforms() {
        val board = board()
        val axisStart = point(board, 0.0, -5.0, "axisStart")
        val axisEnd = point(board, 0.0, 5.0, "axisEnd")
        val axis = line(board, axisStart, axisEnd, "axis")
        val mirror = point(board, 1.0, -1.0, "mirror")

        val lineStart = point(board, -3.0, -2.0, "lineStart")
        val lineEnd = point(board, -1.0, 1.0, "lineEnd")
        val sourceLine = line(board, lineStart, lineEnd, "sourceLine")
        val sourceCurve = assertIs<GMResult.Ok<Curve>>(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -3.0, -2.0),
                dataY = doubleArrayOf(-1.0, 2.0, 0.0),
                id = "sourceCurve",
                name = "",
            ),
        ).value
        val polygonVertices = listOf(
            point(board, -5.0, -3.0, "polygon1"),
            point(board, -3.0, -4.0, "polygon2"),
            point(board, -2.0, -2.0, "polygon3"),
        )
        val sourcePolygon = assertIs<GMResult.Ok<Polygon>>(
            Polygon.create(
                board = board,
                vertices = polygonVertices,
                id = "sourcePolygon",
                name = "",
            ),
        ).value
        val circleCenter = point(board, -3.0, 2.0, "circleCenter")
        val circleRadius = point(board, -2.0, 2.0, "circleRadius")
        val sourceCircle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = circleCenter,
                point2 = circleRadius,
                id = "sourceCircle",
                name = "",
            ),
        ).value

        val reflectedLine = reflected(
            ElementReflections.createReflection(
                board,
                sourceLine,
                axis,
                attributes("reflectedLine"),
            ),
        ) as Line
        val mirroredLine = reflected(
            ElementReflections.createMirrorElement(
                board,
                sourceLine,
                mirror,
                attributes("mirroredLine"),
            ),
        ) as Line
        val reflectedCurve = reflected(
            ElementReflections.createReflection(
                board,
                sourceCurve,
                axis,
                attributes("reflectedCurve"),
            ),
        ) as Curve
        val mirroredCurve = reflected(
            ElementReflections.createMirrorElement(
                board,
                sourceCurve,
                mirror,
                attributes("mirroredCurve"),
            ),
        ) as Curve
        val reflectedPolygon = reflected(
            ElementReflections.createReflection(
                board,
                sourcePolygon,
                axis,
                attributes("reflectedPolygon"),
            ),
        ) as Polygon
        val mirroredPolygon = reflected(
            ElementReflections.createMirrorElement(
                board,
                sourcePolygon,
                mirror,
                attributes("mirroredPolygon"),
            ),
        ) as Polygon
        val reflectedCircle = reflected(
            ElementReflections.createReflection(
                board,
                sourceCircle,
                axis,
                attributes("reflectedCircle"),
            ),
        ) as Circle
        val mirroredCircle = reflected(
            ElementReflections.createMirrorElement(
                board,
                sourceCircle,
                mirror,
                attributes("mirroredCircle"),
            ),
        ) as Circle
        board.update()

        assertPoint(reflectedLine.point1, 3.0, -2.0)
        assertPoint(reflectedLine.point2, 1.0, 1.0)
        assertPoint(mirroredLine.point1, 5.0, 0.0)
        assertPoint(mirroredLine.point2, 3.0, -3.0)
        assertCurvePoint(reflectedCurve, 0, 4.0, -1.0)
        assertCurvePoint(mirroredCurve, 1, 5.0, -4.0)
        assertPoint(reflectedPolygon.vertices[0], 5.0, -3.0)
        assertPoint(mirroredPolygon.vertices[2], 4.0, 0.0)
        assertPoint(reflectedCircle.center, 3.0, 2.0)
        assertPoint(mirroredCircle.center, 5.0, -4.0)
        assertEquals(1.0, reflectedCircle.Radius())
        assertEquals(1.0, mirroredCircle.Radius())

        assertEquals("reflection", reflectedLine.elType)
        assertEquals("mirrorelement", mirroredCurve.elType)
        assertEquals(
            listOf(
                reflectedLine.point1.id,
                reflectedLine.point2.id,
                "axis",
            ),
            reflectedLine.parents,
        )
        assertEquals(listOf("sourceCurve", "axis"), reflectedCurve.parents)
        assertEquals(listOf("axis"), reflectedPolygon.parents)
        assertEquals(
            listOf(reflectedCircle.center.id, "axis"),
            reflectedCircle.parents,
        )
        assertSame(reflectedLine, axis.childElements["reflectedLine"])
        assertSame(mirroredCircle, mirror.childElements["mirroredCircle"])
        assertFalse("reflectedLine" in sourceLine.childElements)
        assertSame(
            reflectedCurve,
            sourceCurve.childElements["reflectedCurve"],
        )

        lineStart.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -1.0),
        )
        lineEnd.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        sourceCurve.replaceData(
            xData = doubleArrayOf(-3.0, -2.0, -1.0),
            yData = doubleArrayOf(0.0, 3.0, 1.0),
        )
        polygonVertices[0].setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, -2.0),
        )
        circleCenter.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 3.0),
        )
        circleRadius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        axisEnd.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 5.0),
        )
        mirror.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 1.0),
        )
        board.update()

        assertPoint(
            reflectedLine.point1,
            3.3846153846153846,
            -2.0769230769230766,
        )
        assertPoint(mirroredLine.point1, 2.0, 3.0)
        assertCurvePoint(
            reflectedCurve,
            0,
            4.692307692307692,
            -1.5384615384615383,
        )
        assertCurvePoint(mirroredCurve, 2, 1.0, 1.0)
        assertPoint(
            reflectedPolygon.vertices[0],
            4.846153846153846,
            -3.769230769230769,
        )
        assertPoint(mirroredPolygon.vertices[0], 4.0, 4.0)
        assertPoint(
            reflectedCircle.center,
            4.9230769230769225,
            1.6153846153846154,
        )
        assertPoint(mirroredCircle.center, 2.0, -1.0)
        assertEquals(2.0, reflectedCircle.Radius())
        assertEquals(2.0, mirroredCircle.Radius())
    }

    @Test
    fun arcAndSectorReflectionsBecomeDynamicTransformedCurves() {
        val board = board()
        val axisStart = point(board, 0.0, -5.0, "axisStart")
        val axisEnd = point(board, 0.0, 5.0, "axisEnd")
        val axis = line(board, axisStart, axisEnd, "axis")
        val mirror = point(board, 1.0, -1.0, "mirror")
        val arcCenter = point(board, -4.0, 1.0, "arcCenter")
        val arcRadius = point(board, -3.0, 1.0, "arcRadius")
        val arcAngle = point(board, -4.0, 2.0, "arcAngle")
        val arc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = board,
                center = arcCenter,
                radiuspoint = arcRadius,
                anglepoint = arcAngle,
                id = "sourceArc",
                name = "",
            ),
        ).value
        val sectorCenter = point(board, -4.0, -1.0, "sectorCenter")
        val sectorRadius = point(board, -3.0, -1.0, "sectorRadius")
        val sectorAngle = point(board, -4.0, -2.0, "sectorAngle")
        val sector = assertIs<GMResult.Ok<Sector>>(
            Sector.create(
                board = board,
                center = sectorCenter,
                radiuspoint = sectorRadius,
                anglepoint = sectorAngle,
                id = "sourceSector",
                name = "",
            ),
        ).value
        val reflectedArc = assertIs<Curve>(
            reflected(
                ElementReflections.createReflection(
                    board,
                    arc,
                    axis,
                    attributes("reflectedArc"),
                ),
            ),
        )
        val mirroredArc = assertIs<Curve>(
            reflected(
                ElementReflections.createMirrorElement(
                    board,
                    arc,
                    mirror,
                    attributes("mirroredArc"),
                ),
            ),
        )
        val reflectedSector = assertIs<Curve>(
            reflected(
                ElementReflections.createReflection(
                    board,
                    sector,
                    axis,
                    attributes("reflectedSector"),
                ),
            ),
        )
        val mirroredSector = assertIs<Curve>(
            reflected(
                ElementReflections.createMirrorElement(
                    board,
                    sector,
                    mirror,
                    attributes("mirroredSector"),
                ),
            ),
        )
        board.update()

        assertEquals(Const.OBJECT_TYPE_CURVE, reflectedArc.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, reflectedSector.elementClass)
        assertEquals("reflection", reflectedArc.elType)
        assertEquals("mirrorelement", mirroredSector.elType)
        assertEquals(listOf("sourceArc", "axis"), reflectedArc.parents)
        assertEquals(
            listOf("sourceSector", "mirror"),
            mirroredSector.parents,
        )
        assertSame(reflectedArc, arc.childElements["reflectedArc"])
        assertSame(mirroredSector, mirror.childElements["mirroredSector"])
        assertCurveTransform(arc, reflectedArc) {
            reflectAcrossLine(it, axis)
        }
        assertCurveTransform(arc, mirroredArc) {
            mirrorAcrossPoint(it, mirror)
        }
        assertCurveTransform(sector, reflectedSector) {
            reflectAcrossLine(it, axis)
        }
        assertCurveTransform(sector, mirroredSector) {
            mirrorAcrossPoint(it, mirror)
        }

        arcRadius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 1.0),
        )
        sectorAngle.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -3.0),
        )
        axisEnd.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 5.0),
        )
        mirror.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 1.0),
        )
        board.update()

        assertCurveTransform(arc, reflectedArc) {
            reflectAcrossLine(it, axis)
        }
        assertCurveTransform(arc, mirroredArc) {
            mirrorAcrossPoint(it, mirror)
        }
        assertCurveTransform(sector, reflectedSector) {
            reflectAcrossLine(it, axis)
        }
        assertCurveTransform(sector, mirroredSector) {
            mirrorAcrossPoint(it, mirror)
        }
    }

    @Test
    fun projectiveCircleReflectionUsesTransformedConicBranch() {
        val board = board()
        val axisStart = point(board, 0.0, -5.0, "axisStart")
        val axisEnd = point(board, 0.0, 5.0, "axisEnd")
        val axis = line(board, axisStart, axisEnd, "axis")
        val center = point(board, -3.0, 2.0, "circleCenter")
        val radiusPoint = point(board, -2.0, 2.0, "circleRadius")
        val source = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = center,
                point2 = radiusPoint,
                id = "sourceCircle",
                name = "",
            ),
        ).value
        val reflected = assertIs<Curve>(
            reflected(
                ElementReflections.createReflection(
                    board = board,
                    source = source,
                    line = axis,
                    attributes = ElementReflectionAttributes(
                        id = "projectiveReflection",
                        name = "",
                        transformationType =
                            ElementReflectionTransformationType.PROJECTIVE,
                        center = ElementReflectionPointAttributes(
                            id = "projectiveCenter",
                            name = "",
                        ),
                    ),
                ),
            ),
        )
        board.update()

        assertEquals(Const.OBJECT_TYPE_CONIC, reflected.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, reflected.elementClass)
        assertEquals("reflection", reflected.elType)
        assertEquals(axis.id, reflected.parents.last())
        assertCircularCurve(
            curve = reflected,
            center = reflectAcrossLine(center.coords.usrCoords, axis),
            radius = source.Radius(),
        )

        radiusPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 2.0),
        )
        axisEnd.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 5.0),
        )
        board.update()

        assertCircularCurve(
            curve = reflected,
            center = reflectAcrossLine(center.coords.usrCoords, axis),
            radius = source.Radius(),
        )
    }

    private fun board(): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = "board",
    )

    private fun attributes(id: String): ElementReflectionAttributes =
        ElementReflectionAttributes(id = id, name = "")

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
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
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = first,
                point2 = second,
                id = id,
                name = "",
            ),
        ).value

    private fun reflected(
        result: GMResult<GeometryElement, ElementReflectionError>,
    ): GeometryElement =
        assertIs<GMResult.Ok<GeometryElement>>(result).value

    private fun assertPoint(
        point: Point,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(expectedX, point.X(), absoluteTolerance = 1.0e-12)
        assertEquals(expectedY, point.Y(), absoluteTolerance = 1.0e-12)
    }

    private fun assertCurvePoint(
        curve: Curve,
        index: Int,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(
            expectedX,
            curve.points[index].usrCoords[1],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            expectedY,
            curve.points[index].usrCoords[2],
            absoluteTolerance = 1.0e-12,
        )
    }

    private fun assertCurveTransform(
        source: CurveTransformationSource,
        output: Curve,
        transform: (DoubleArray) -> DoubleArray,
    ) {
        val count = minOf(
            source.transformationPointCount,
            source.transformationPoints.size,
        )
        assertEquals(count, output.numberPoints)
        assertEquals(
            source.transformationBezierDegree,
            output.bezierDegree,
        )
        for (index in 0 until count) {
            val expected = transform(
                source.transformationPoints[index].usrCoords,
            )
            assertEquals(
                expected[1],
                output.points[index].usrCoords[1],
                absoluteTolerance = 1.0e-12,
            )
            assertEquals(
                expected[2],
                output.points[index].usrCoords[2],
                absoluteTolerance = 1.0e-12,
            )
        }
    }

    private fun reflectAcrossLine(
        coordinates: DoubleArray,
        line: Line,
    ): DoubleArray {
        val first = line.point1.coords.usrCoords
        val second = line.point2.coords.usrCoords
        val directionX = second[1] - first[1]
        val directionY = second[2] - first[2]
        val offsetX = coordinates[1] - first[1]
        val offsetY = coordinates[2] - first[2]
        val scale = (
            offsetX * directionX +
                offsetY * directionY
            ) / (
            directionX * directionX +
                directionY * directionY
            )
        val projectedX = first[1] + scale * directionX
        val projectedY = first[2] + scale * directionY
        return doubleArrayOf(
            1.0,
            2.0 * projectedX - coordinates[1],
            2.0 * projectedY - coordinates[2],
        )
    }

    private fun mirrorAcrossPoint(
        coordinates: DoubleArray,
        mirror: Point,
    ): DoubleArray =
        doubleArrayOf(
            1.0,
            2.0 * mirror.X() - coordinates[1],
            2.0 * mirror.Y() - coordinates[2],
        )

    private fun assertCircularCurve(
        curve: Curve,
        center: DoubleArray,
        radius: Double,
    ) {
        for (point in curve.points.take(curve.numberPoints)) {
            val deltaX = point.usrCoords[1] - center[1]
            val deltaY = point.usrCoords[2] - center[2]
            assertEquals(
                radius * radius,
                deltaX * deltaX + deltaY * deltaY,
                absoluteTolerance = 1.0e-8,
            )
        }
    }
}
