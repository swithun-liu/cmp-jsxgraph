package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.ArcError
import com.swithun.jsxgraph.core.base.AngleBisectorPoint
import com.swithun.jsxgraph.core.base.AxisError
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.BisectorLines
import com.swithun.jsxgraph.core.base.BisectorLinesError
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardError
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.CircumcenterError
import com.swithun.jsxgraph.core.base.CircumcenterPoint
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveBoxPlotSnapshot
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.EllipseError
import com.swithun.jsxgraph.core.base.HyperbolaError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.IncenterPoint
import com.swithun.jsxgraph.core.base.IncircleCircle
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.NormalError
import com.swithun.jsxgraph.core.base.OrthogonalConstructionError
import com.swithun.jsxgraph.core.base.OrthogonalPoint
import com.swithun.jsxgraph.core.base.OtherIntersectionPoint
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelDirectionPoint
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.ParallelPoint
import com.swithun.jsxgraph.core.base.ParallelogramError
import com.swithun.jsxgraph.core.base.ParabolaError
import com.swithun.jsxgraph.core.base.PerpendicularLine
import com.swithun.jsxgraph.core.base.PerpendicularSegmentLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.PolePointError
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.RadicalAxisError
import com.swithun.jsxgraph.core.base.RegularPolygonError
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.TangentError
import com.swithun.jsxgraph.core.base.TangentToError
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.Ticks
import com.swithun.jsxgraph.core.base.TicksCurveLocation
import com.swithun.jsxgraph.core.base.TicksError
import com.swithun.jsxgraph.core.base.TicksSource
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
import com.swithun.jsxgraph.core.math.ClipError
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NativeJessieCodeCreatorsTest {
    @Test
    fun pointCreatorUsesAssignmentNameAndExplicitIdentityAttributes() {
        val implicitBoard = board("implicit")
        val implicit = point(
            evaluate(
                source = "A = point(1, 2); A;",
                board = implicitBoard,
            ),
        )

        assertEquals("A", implicit.name)
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 2.0),
            implicit.coords.usrCoords,
        )
        assertSame(implicit, implicitBoard.select("A"))

        val explicitBoard = board("explicit")
        val explicit = point(
            evaluate(
                source =
                    "A = point(3, 4) << " +
                        "id: \"fixed\", name: \"Named\", " +
                        "needsRegularUpdate: false, fixed: true >>; A;",
                board = explicitBoard,
            ),
        )
        assertEquals("fixed", explicit.id)
        assertEquals("Named", explicit.name)
        assertEquals(false, explicit.needsRegularUpdate)
        assertTrue(explicit.isFixed)
        assertSame(explicit, explicitBoard.select("fixed"))
        assertSame(explicit, explicitBoard.select("Named"))
    }

    @Test
    fun nestedAssignmentUsesTheInnermostCreatorName() {
        val board = board()

        val point = point(
            evaluate(
                source = "A = B = point(1, 2); A;",
                board = board,
            ),
        )

        assertEquals("B", point.name)
        assertSame(point, board.select("B"))
        assertSame(null, board.select("A"))
    }

    @Test
    fun pointCreatorSupportsMixedJessieCodeCoordinateExpressions() {
        val board = board()

        val point = point(
            evaluate(
                source =
                    "A = point(2, 3); " +
                        "B = point(\"A.X() + 1\", 2); B;",
                board = board,
            ),
        )

        assertEquals("B", point.name)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 2.0),
            point.coords.usrCoords,
        )
        val driver = assertIs<Point>(board.select("A"))
        assertSame(point, driver.childElements[point.id])
    }

    @Test
    fun pointCreatorSupportsFunctionAndArrayFunctionCoordinates() {
        val board = board("function-points")

        evaluate(
            source =
                """
                Driver = point(2, 3) << id: "driver" >>;
                arrayPoint = point(
                    function () {
                        return [Driver.X() + 1, Driver.Y() + 2];
                    }
                ) << id: "arrayPoint", name: "" >>;
                mixedPoint = point(
                    function () { return Driver.X() * 2; },
                    "${'$'}(\"driver\").Y() - 1"
                ) << id: "mixedPoint", name: "" >>;
                homogeneousPoint = point(
                    function () {
                        return [
                            2,
                            Driver.X() * 2,
                            Driver.Y() * 2,
                            99
                        ];
                    }
                ) << id: "homogeneousPoint", name: "" >>;
                """.trimIndent(),
            board = board,
        )
        board.fullUpdate()

        val driver = assertIs<Point>(board.select("driver"))
        val arrayPoint = assertIs<Point>(board.select("arrayPoint"))
        val mixedPoint = assertIs<Point>(board.select("mixedPoint"))
        val homogeneousPoint =
            assertIs<Point>(board.select("homogeneousPoint"))
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 5.0),
            arrayPoint.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 2.0),
            mixedPoint.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 3.0),
            homogeneousPoint.coords.usrCoords,
        )
        assertTrue(arrayPoint.isConstrained)
        assertFalse(arrayPoint.isDraggable)
        assertTrue(arrayPoint.parents.isEmpty())
        assertTrue(homogeneousPoint.parents.isEmpty())
        assertSame(mixedPoint, driver.childElements[mixedPoint.id])

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(5.0, -1.0),
        )
        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 1.0),
            arrayPoint.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 10.0, -2.0),
            mixedPoint.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, -1.0),
            homogeneousPoint.coords.usrCoords,
        )
    }

    @Test
    fun pointCreatorReportsStructuredFunctionCoordinateFailures() {
        val scalarBoard = board("scalar-function-point")
        val scalarFailure = creatorError(
            source = "point(function () { return 4; });",
            board = scalarBoard,
        )
        assertEquals("point", scalarFailure.creatorName)
        assertEquals(
            PointError.CoordinateExpressionEvaluation(
                com.swithun.jsxgraph.core.base.CoordinateConstraintError
                    .CoordinateArrayResultExpected("number"),
            ),
            assertIs<JessieCodeCreatorError.PointFactory>(
                scalarFailure.error,
            ).error,
        )
        assertTrue(scalarBoard.objects.isEmpty())

        val nonNumericBoard = board("non-numeric-function-point")
        val nonNumericFailure = creatorError(
            source = "point(function () { return [1, \"y\"]; });",
            board = nonNumericBoard,
        )
        assertEquals(
            PointError.CoordinateExpressionEvaluation(
                com.swithun.jsxgraph.core.base.CoordinateConstraintError
                    .NonNumericResult(
                        coordinateIndex = 1,
                        actualType = "string",
                    ),
            ),
            assertIs<JessieCodeCreatorError.PointFactory>(
                nonNumericFailure.error,
            ).error,
        )
        assertTrue(nonNumericBoard.objects.isEmpty())
    }

    @Test
    fun transformCreatorBuildsTransformedPointsWithoutBoardRegistration() {
        val board = board("transformed-points")

        evaluate(
            source =
                "A = point(1, 2); " +
                    "scale = transform(2, 3) << type: \"scale\" >>; " +
                    "shift = transform(1, -1) << type: \"translate\" >>; " +
                    "B = point(A, [scale, shift]); " +
                    "matrix = transform(" +
                    "[[1, 0, 0], [4, 1, 0], [-1, 0, 1]]) << " +
                    "type: \"matrix\" >>; " +
                    "C = point(A, matrix);",
            board = board,
        )
        board.fullUpdate()

        assertEquals(3, board.numObjects)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 5.0),
            assertIs<Point>(board.select("B")).coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 1.0),
            assertIs<Point>(board.select("C")).coords.usrCoords,
        )
        assertSame(null, board.select("scale"))
        assertSame(null, board.select("shift"))
        assertSame(null, board.select("matrix"))
    }

    @Test
    fun transformCreatorSupportsLiveElementAndFunctionParameters() {
        val board = board("live-transform")

        evaluate(
            source =
                "O = point(0, 0); " +
                    "Y = point(0, 1); " +
                    "axis = line(O, Y); " +
                    "A = point(2, 0); " +
                    "driver = point(1, 0); " +
                    "reflect = transform(axis) << type: \"reflect\" >>; " +
                    "rotate = transform(PI / 2, O) << type: \"rotate\" >>; " +
                    "dynamic = transform(" +
                    "function () { return driver.X(); }, 0" +
                    ") << type: \"translate\" >>; " +
                    "R = point(A, reflect); " +
                    "Q = point(A, rotate); " +
                    "D = point(A, dynamic);",
            board = board,
        )
        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(1.0, -2.0, 0.0),
            assertIs<Point>(board.select("R")).coords.usrCoords,
        )
        val rotated = assertIs<Point>(board.select("Q"))
        assertEquals(0.0, rotated.X(), absoluteTolerance = 1.0e-12)
        assertEquals(2.0, rotated.Y(), absoluteTolerance = 1.0e-12)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 0.0),
            assertIs<Point>(board.select("D")).coords.usrCoords,
        )

        assertIs<Point>(board.select("driver")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 0.0),
        )
        board.fullUpdate()
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 0.0),
            assertIs<Point>(board.select("D")).coords.usrCoords,
        )
    }

    @Test
    fun transformCreatorReportsStructuredParameterFailures() {
        val missingType = creatorError(
            source = "transform(1, 2);",
            board = board("missing-transform-type"),
        )
        assertEquals("transform", missingType.creatorName)
        assertIs<JessieCodeCreatorError.TransformationFactory>(
            missingType.error,
        )

        val invalidMatrix = creatorError(
            source =
                "transform([[1, 0], [0]]) << type: \"affinematrix\" >>;",
            board = board("invalid-transform-matrix"),
        )
        assertEquals("transform", invalidMatrix.creatorName)
        val factory =
            assertIs<JessieCodeCreatorError.TransformationFactory>(
                invalidMatrix.error,
            )
        assertIs<
            com.swithun.jsxgraph.core.base.TransformationError
                .InvalidMatrixShape
            >(factory.error)
    }

    @Test
    fun lineCreatorSupportsPointsCoordinateArraysAndCoefficients() {
        val pointBoard = board("point-line")
        val pointLine = line(
            evaluate(
                source =
                    "A = point(0, 0); B = point(4, 2); " +
                        "l = line(A, B); l;",
                board = pointBoard,
            ),
        )
        assertEquals("l", pointLine.name)
        assertEquals(
            listOf(
                assertIs<Point>(pointBoard.select("A")).id,
                assertIs<Point>(pointBoard.select("B")).id,
            ),
            pointLine.parents,
        )
        assertEquals(0.5, pointLine.Slope())

        val coordinateBoard = board("coordinate-line")
        val coordinateLine = line(
            evaluate(
                source = "line([0, 0], [2, 3]);",
                board = coordinateBoard,
            ),
        )
        assertEquals(3, coordinateBoard.numObjects)
        assertEquals("", coordinateLine.point1.name)
        assertEquals("", coordinateLine.point2.name)
        assertEquals(1.5, coordinateLine.Slope())

        val coefficientBoard = board("coefficient-line")
        val coefficientLine = line(
            evaluate(
                source = "line(1, -2, 3);",
                board = coefficientBoard,
            ),
        )
        assertEquals(3, coefficientBoard.numObjects)
        val normalization = sqrt(13.0)
        assertEquals(-1.0 / normalization, coefficientLine.stdform[0])
        assertEquals(2.0 / normalization, coefficientLine.stdform[1])
        assertEquals(-3.0 / normalization, coefficientLine.stdform[2])
    }

    @Test
    fun radicalAxisCreatorPreservesNestedIdentityAndDynamicGeometry() {
        val board = board("radical-axis")
        evaluate(
            source =
                "center1 = point(-3, -1) << " +
                    "id: \"center1\", name: \"\" >>; " +
                    "radius1 = point(-1, -1) << " +
                    "id: \"radius1\", name: \"\" >>; " +
                    "circle1 = circle(center1, radius1) << " +
                    "id: \"circle1\", name: \"\" >>; " +
                    "center2 = point(2, 2) << " +
                    "id: \"center2\", name: \"\" >>; " +
                    "radius2 = point(5, 2) << " +
                    "id: \"radius2\", name: \"\" >>; " +
                    "circle2 = circle(center2, radius2) << " +
                    "id: \"circle2\", name: \"\" >>; " +
                    "axis = radicalaxis(circle1, \"circle2\") << " +
                    "id: \"axis\", name: \"\", " +
                    "straightFirst: false, straightLast: true, " +
                    "point1: << id: \"axisPoint1\", " +
                    "name: \"first-helper\" >>, " +
                    "point2: << id: \"axisPoint2\", name: \"\", " +
                    "needsRegularUpdate: true >> >>;",
            board = board,
        )
        val axis = assertIs<Line>(board.select("axis"))
        val center1 = assertIs<Point>(board.select("center1"))
        val center2 = assertIs<Point>(board.select("center2"))

        assertTrue("radicalaxis" in NativeJessieCodeCreators.names)
        assertEquals("radicalaxis", axis.elType)
        assertEquals(listOf("circle1", "circle2"), axis.parents)
        assertFalse(axis.straightFirst)
        assertTrue(axis.straightLast)
        assertTrue(axis.constrained)
        assertFalse(axis.isDraggable)
        assertEquals("axisPoint1", axis.point1.id)
        assertEquals("first-helper", axis.point1.name)
        assertEquals("axisPoint2", axis.point2.id)
        assertTrue(axis.point1.parents.isEmpty())
        assertTrue(axis.point2.parents.isEmpty())
        assertPoint(
            1.6029411764705883,
            -3.8382352941176476,
            axis.point1.coords,
        )
        assertPoint(
            0.5441176470588236,
            -2.073529411764706,
            axis.point2.coords,
        )

        center1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, 1.0),
        )
        assertIs<Point>(board.select("radius1")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-1.0, 1.0),
        )
        center2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -2.0),
        )
        assertIs<Point>(board.select("radius2")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -2.0),
        )
        board.update()

        assertPoint(
            -2.6323529411764706,
            -3.2205882352941178,
            axis.point1.coords,
        )
        assertPoint(
            -1.5735294117647058,
            -1.4558823529411766,
            axis.point2.coords,
        )
    }

    @Test
    fun radicalAxisCreatorRejectsInvalidInputsAndRollsBackHelpers() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); B = point(1, 0); " +
                    "C = circle(A, B); radicalaxis(C, A);",
            board = board("radical-wrong-parents"),
        )
        assertEquals("radicalaxis", wrongParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongParents.error,
        )

        val invalidNestedAttribute = creatorError(
            source =
                "A = point(0, 0); B = point(1, 0); " +
                    "C = point(3, 0); D = point(5, 0); " +
                    "c1 = circle(A, B); c2 = circle(C, D); " +
                    "radicalaxis(c1, c2) << point1: 4 >>;",
            board = board("radical-invalid-attribute"),
        )
        assertEquals(
            "point1",
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidNestedAttribute.error,
            ).attribute,
        )

        val duplicateHelperBoard = board("radical-helper-rollback")
        val helperFailure = creatorError(
            source =
                "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                    "B = point(1, 0) << id: \"B\", name: \"\" >>; " +
                    "C = point(3, 0) << id: \"C\", name: \"\" >>; " +
                    "D = point(5, 0) << id: \"D\", name: \"\" >>; " +
                    "c1 = circle(A, B) << id: \"c1\", name: \"\" >>; " +
                    "c2 = circle(C, D) << id: \"c2\", name: \"\" >>; " +
                    "taken = point(8, 8) << " +
                    "id: \"taken\", name: \"\" >>; " +
                    "radicalaxis(c1, c2) << id: \"unused\", " +
                    "point1: << id: \"temporary\" >>, " +
                    "point2: << id: \"taken\" >> >>;",
            board = duplicateHelperBoard,
        )
        val helperFactory = assertIs<
            JessieCodeCreatorError.RadicalAxisFactory,
            >(helperFailure.error)
        assertIs<RadicalAxisError.PointCreation>(helperFactory.error)
        assertEquals(
            setOf("A", "B", "C", "D", "c1", "c2", "taken"),
            duplicateHelperBoard.objects.keys,
        )

        val duplicateLineBoard = board("radical-line-rollback")
        val lineFailure = creatorError(
            source =
                "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                    "B = point(1, 0) << id: \"B\", name: \"\" >>; " +
                    "C = point(3, 0) << id: \"C\", name: \"\" >>; " +
                    "D = point(5, 0) << id: \"D\", name: \"\" >>; " +
                    "c1 = circle(A, B) << id: \"c1\", name: \"\" >>; " +
                    "c2 = circle(C, D) << id: \"c2\", name: \"\" >>; " +
                    "taken = point(8, 8) << " +
                    "id: \"taken\", name: \"\" >>; " +
                    "radicalaxis(c1, c2) << id: \"taken\", " +
                    "point1: << id: \"temporary1\" >>, " +
                    "point2: << id: \"temporary2\" >> >>;",
            board = duplicateLineBoard,
        )
        val lineFactory = assertIs<
            JessieCodeCreatorError.RadicalAxisFactory,
            >(lineFailure.error)
        assertIs<RadicalAxisError.LineCreation>(lineFactory.error)
        assertEquals(
            setOf("A", "B", "C", "D", "c1", "c2", "taken"),
            duplicateLineBoard.objects.keys,
        )
    }

    @Test
    fun tangentPolarCreatorsPreserveAliasesParentOrderAndDynamicGeometry() {
        val board = board("tangent-polar")
        evaluate(
            source =
                "center = point(1, 1) << " +
                    "id: \"center\", name: \"\" >>; " +
                    "radius = point(4, 1) << " +
                    "id: \"radius\", name: \"\" >>; " +
                    "circle = circle(center, radius) << " +
                    "id: \"circle\", name: \"\" >>; " +
                    "P = point(5, 4) << id: \"P\", name: \"\" >>; " +
                    "t = tangent(circle, P) << id: \"t\", name: \"\", " +
                    "straightFirst: false, straightLast: true, " +
                    "point1: << id: \"tPoint1\", name: \"first\" >>, " +
                    "point2: << id: \"tPoint2\", name: \"\" >> >>; " +
                    "p = polar(P, circle) << id: \"p\", name: \"\", " +
                    "point1: << id: \"pPoint1\", name: \"\" >>, " +
                    "point2: << id: \"pPoint2\", name: \"\" >> >>; " +
                    "pl = polarline(P, circle) << " +
                    "id: \"pl\", name: \"\", " +
                    "point1: << id: \"plPoint1\", name: \"\" >>, " +
                    "point2: << id: \"plPoint2\", name: \"\" >> >>;",
            board = board,
        )
        val circle = assertIs<Circle>(board.select("circle"))
        val sourcePoint = assertIs<Point>(board.select("P"))
        val center = assertIs<Point>(board.select("center"))
        val radius = assertIs<Point>(board.select("radius"))
        val tangent = assertIs<Line>(board.select("t"))
        val polar = assertIs<Line>(board.select("p"))
        val polarLine = assertIs<Line>(board.select("pl"))

        assertTrue("tangent" in NativeJessieCodeCreators.names)
        assertTrue("polar" in NativeJessieCodeCreators.names)
        assertTrue("polarline" in NativeJessieCodeCreators.names)
        assertEquals("tangent", tangent.elType)
        assertEquals("tangent", polar.elType)
        assertEquals("polarline", polarLine.elType)
        assertEquals(listOf("circle", "P"), tangent.parents)
        assertEquals(listOf("P", "circle"), polar.parents)
        assertEquals(listOf("circle", "P"), polarLine.parents)
        assertEquals(Const.OBJECT_TYPE_TANGENT, tangent.type)
        assertEquals(Const.OBJECT_TYPE_TANGENT, polar.type)
        assertEquals(Const.OBJECT_TYPE_TANGENT, polarLine.type)
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertSame(sourcePoint, tangent.glider)
        assertEquals("tPoint1", tangent.point1.id)
        assertEquals("first", tangent.point1.name)
        assertEquals("tPoint2", tangent.point2.id)
        assertPoint(2.8, 1.6, tangent.point1.coords)
        assertPoint(2.68, 1.76, tangent.point2.coords)
        assertPoint(2.8, 1.6, polar.point1.coords)
        assertPoint(2.8, 1.6, polarLine.point1.coords)
        assertTrue(circle.childElements.isEmpty())
        assertEquals(
            setOf("t", "p", "pl"),
            sourcePoint.childElements.keys,
        )

        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        radius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        sourcePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -2.0),
        )
        board.update()

        for (line in listOf(tangent, polar, polarLine)) {
            assertPoint(-0.25, -0.25, line.point1.coords)
            assertPoint(-0.125, -0.125, line.point2.coords)
        }
    }

    @Test
    fun tangentPolarLineParentsReuseSourceEndpoints() {
        val board = board("tangent-line")
        evaluate(
            source =
                "A = point(-3, -1) << id: \"A\", name: \"\" >>; " +
                    "B = point(4, 2) << id: \"B\", name: \"\" >>; " +
                    "source = line(A, B) << " +
                    "id: \"source\", name: \"\" >>; " +
                    "P = point(1, 5) << id: \"P\", name: \"\" >>; " +
                    "t = tangent(source, P) << id: \"t\", name: \"\", " +
                    "straightFirst: false, straightLast: true, " +
                    "point1: << id: \"A\", name: \"ignored\" >>, " +
                    "point2: << id: \"B\", name: \"ignored\" >> >>; " +
                    "p = polar(P, source) << id: \"p\", name: \"\" >>;",
            board = board,
        )
        val point1 = assertIs<Point>(board.select("A"))
        val point2 = assertIs<Point>(board.select("B"))
        val source = assertIs<Line>(board.select("source"))
        val parameter = assertIs<Point>(board.select("P"))
        val tangent = assertIs<Line>(board.select("t"))
        val polar = assertIs<Line>(board.select("p"))

        for (output in listOf(tangent, polar)) {
            assertEquals("tangent", output.elType)
            assertEquals(Const.OBJECT_TYPE_TANGENT, output.type)
            assertFalse(output.constrained)
            assertTrue(output.isDraggable)
            assertSame(point1, output.point1)
            assertSame(point2, output.point2)
            assertSame(parameter, output.glider)
            assertEquals(setOf("A", "B"), output.ancestors.keys)
            assertContentEquals(source.stdform, output.stdform)
        }
        assertEquals(listOf("source", "P"), tangent.parents)
        assertEquals(listOf("P", "source"), polar.parents)
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertTrue(source.childElements.isEmpty())
        assertTrue(parameter.childElements.isEmpty())
        assertEquals(
            setOf("source", "t", "p"),
            point1.childElements.keys,
        )
        assertEquals(
            setOf("source", "t", "p"),
            point2.childElements.keys,
        )
        assertEquals(
            setOf("A", "B", "source", "P", "t", "p"),
            board.objects.keys,
        )

        parameter.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(7.0, 1.0),
        )
        board.update()
        assertContentEquals(source.stdform, tangent.stdform)
        assertContentEquals(source.stdform, polar.stdform)

        point1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 3.0),
        )
        point2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -4.0),
        )
        board.update()
        assertContentEquals(source.stdform, tangent.stdform)
        assertContentEquals(source.stdform, polar.stdform)
    }

    @Test
    fun tangentPolarCurveParentsMatchOfficialDynamicGeometry() {
        val board = board("tangent-curve")
        evaluate(
            source =
                "functionCurve = functiongraph(" +
                    "\"x * x - 1\", -4, 4" +
                    ") << id: \"functionCurve\", name: \"\", " +
                    "doAdvancedPlot: false, numberPointsHigh: 64 >>; " +
                    "functionPoint = point(2, 5) << " +
                    "id: \"functionPoint\", name: \"\" >>; " +
                    "functionTangent = tangent(" +
                    "functionCurve, functionPoint" +
                    ") << id: \"functionTangent\", name: \"\", " +
                    "straightFirst: false, straightLast: true, " +
                    "point1: << id: \"functionTangentPoint1\", " +
                    "name: \"\" >>, " +
                    "point2: << id: \"functionTangentPoint2\", " +
                    "name: \"\" >> >>; " +
                    "functionPolar = polar(" +
                    "functionPoint, functionCurve" +
                    ") << id: \"functionPolar\", name: \"\", " +
                    "straightFirst: true, straightLast: false, " +
                    "point1: << id: \"functionPolarPoint1\", " +
                    "name: \"\" >>, " +
                    "point2: << id: \"functionPolarPoint2\", " +
                    "name: \"\" >> >>; " +
                    "parametricCurve = curve(" +
                    "\"2 * cos(x)\", \"sin(x)\", 0, " +
                    "6.283185307179586" +
                    ") << id: \"parametricCurve\", name: \"\", " +
                    "doAdvancedPlot: false, numberPointsHigh: 64 >>; " +
                    "parametricPoint = point(3, 0.75) << " +
                    "id: \"parametricPoint\", name: \"\" >>; " +
                    "parametricTangent = tangent(" +
                    "parametricPoint, parametricCurve" +
                    ") << id: \"parametricTangent\", name: \"\", " +
                    "point1: << id: \"parametricTangentPoint1\", " +
                    "name: \"\" >>, " +
                    "point2: << id: \"parametricTangentPoint2\", " +
                    "name: \"\" >> >>; " +
                    "plotCurve = curve(" +
                    "[-4, -1, 2, 5], [-2, 2, -1, 3]" +
                    ") << id: \"plotCurve\", name: \"\" >>; " +
                    "plotPoint = point(0.25, 2.5) << " +
                    "id: \"plotPoint\", name: \"\" >>; " +
                    "plotTangent = tangent(plotCurve, plotPoint) << " +
                    "id: \"plotTangent\", name: \"\", " +
                    "straightFirst: false, straightLast: false, " +
                    "point1: << id: \"plotTangentPoint1\", " +
                    "name: \"\" >>, " +
                    "point2: << id: \"plotTangentPoint2\", " +
                    "name: \"\" >> >>;",
            board = board,
        )
        val functionCurve = assertIs<Curve>(board.select("functionCurve"))
        val functionPoint = assertIs<Point>(board.select("functionPoint"))
        val functionTangent = assertIs<Line>(
            board.select("functionTangent"),
        )
        val functionPolar = assertIs<Line>(board.select("functionPolar"))
        val parametricCurve = assertIs<Curve>(
            board.select("parametricCurve"),
        )
        val parametricPoint = assertIs<Point>(
            board.select("parametricPoint"),
        )
        val parametricTangent = assertIs<Line>(
            board.select("parametricTangent"),
        )
        val plotCurve = assertIs<Curve>(board.select("plotCurve"))
        val plotPoint = assertIs<Point>(board.select("plotPoint"))
        val plotTangent = assertIs<Line>(board.select("plotTangent"))

        assertFalse(functionTangent.straightFirst)
        assertTrue(functionTangent.straightLast)
        assertTrue(functionPolar.straightFirst)
        assertFalse(functionPolar.straightLast)
        assertFalse(plotTangent.straightFirst)
        assertFalse(plotTangent.straightLast)
        assertEquals(
            listOf("functionCurve", "functionPoint"),
            functionTangent.parents,
        )
        assertEquals(
            listOf("functionPoint", "functionCurve"),
            functionPolar.parents,
        )
        assertSame(functionPoint, functionTangent.glider)
        assertSame(functionPoint, functionPolar.glider)
        assertSame(parametricPoint, parametricTangent.glider)
        assertSame(plotPoint, plotTangent.glider)
        assertTrue(functionCurve.childElements.isEmpty())
        assertTrue(parametricCurve.childElements.isEmpty())
        assertTrue(plotCurve.childElements.isEmpty())
        assertEquals(
            setOf("functionTangent", "functionPolar"),
            functionPoint.childElements.keys,
        )
        assertEquals(
            setOf("parametricTangent"),
            parametricPoint.childElements.keys,
        )
        assertEquals(
            setOf("plotTangent"),
            plotPoint.childElements.keys,
        )
        assertPoint(
            0.5882352941184178,
            -0.6470588235263288,
            functionTangent.point1.coords,
        )
        assertPoint(
            0.6470588235297972,
            -0.4117647058808115,
            functionTangent.point2.coords,
        )
        assertPoint(
            3.1094038429387445,
            1.1337477941451766,
            parametricTangent.point1.coords,
        )
        assertPoint(
            2.843075499191616,
            0.19956749767677068,
            parametricTangent.point2.coords,
        )
        assertPoint(
            0.16666666666666666,
            0.8333333333333334,
            plotTangent.point1.coords,
        )
        assertPoint(
            0.3333333333333333,
            0.6666666666666666,
            plotTangent.point2.coords,
        )

        functionPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-1.0, 4.0),
        )
        parametricPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.5, -1.25),
        )
        plotPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 4.0),
        )
        board.update()

        assertPoint(
            0.40000000000040004,
            1.1999999999992,
            functionTangent.point1.coords,
        )
        assertPoint(
            -2.505232495502133,
            -1.2464977648448397,
            parametricTangent.point1.coords,
        )
        assertPoint(
            1.52,
            -1.64,
            plotTangent.point1.coords,
        )
    }

    @Test
    fun tangentPolarCreatorsRejectInvalidInputsAndRollBackHelpers() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "circle = circle(A, B); tangent(circle, circle);",
            board = board("tangent-wrong-parents"),
        )
        assertEquals("tangent", wrongParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongParents.error,
        )

        val polarLineWithLine = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "P = point(3, 1); source = line(A, B); " +
                    "polarline(source, P);",
            board = board("polarline-line-parents"),
        )
        assertEquals("polarline", polarLineWithLine.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            polarLineWithLine.error,
        )

        val polarLineWithCurve = creatorError(
            source =
                "curve = functiongraph(\"x * x\", -2, 2) << " +
                    "doAdvancedPlot: false, numberPointsHigh: 16 >>; " +
                    "P = point(1, 1); polarline(curve, P);",
            board = board("polarline-curve-parents"),
        )
        assertEquals("polarline", polarLineWithCurve.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            polarLineWithCurve.error,
        )

        val onePointCurve = creatorError(
            source =
                "curve = curve([1], [2]); P = point(1, 2); " +
                    "tangent(curve, P);",
            board = board("tangent-one-point-curve"),
        )
        assertEquals("tangent", onePointCurve.creatorName)
        assertEquals(
            TangentError.InvalidCurvePointCount(1),
            assertIs<JessieCodeCreatorError.TangentFactory>(
                onePointCurve.error,
            ).error,
        )

        val invalidNestedAttribute = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "P = point(3, 1); circle = circle(A, B); " +
                    "polarline(circle, P) << point1: 4 >>;",
            board = board("polarline-invalid-attribute"),
        )
        assertEquals("polarline", invalidNestedAttribute.creatorName)
        assertEquals(
            "point1",
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidNestedAttribute.error,
            ).attribute,
        )

        val duplicateHelperBoard = board("tangent-helper-rollback")
        val helperFailure = creatorError(
            source =
                "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                    "B = point(2, 0) << id: \"B\", name: \"\" >>; " +
                    "P = point(3, 1) << id: \"P\", name: \"\" >>; " +
                    "circle = circle(A, B) << " +
                    "id: \"circle\", name: \"\" >>; " +
                    "taken = point(8, 8) << " +
                    "id: \"taken\", name: \"\" >>; " +
                    "polar(circle, P) << id: \"unused\", " +
                    "point1: << id: \"temporary\" >>, " +
                    "point2: << id: \"taken\" >> >>;",
            board = duplicateHelperBoard,
        )
        val helperFactory = assertIs<
            JessieCodeCreatorError.TangentFactory,
            >(helperFailure.error)
        assertIs<TangentError.PointCreation>(helperFactory.error)
        assertEquals(
            setOf("A", "B", "P", "circle", "taken"),
            duplicateHelperBoard.objects.keys,
        )
        assertTrue(
            assertIs<Circle>(duplicateHelperBoard.select("circle"))
                .childElements
                .isEmpty(),
        )
        assertTrue(
            assertIs<Point>(duplicateHelperBoard.select("P"))
                .childElements
                .isEmpty(),
        )

        val duplicateLineBoard = board("tangent-line-rollback")
        val lineFailure = creatorError(
            source =
                "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                    "B = point(2, 0) << id: \"B\", name: \"\" >>; " +
                    "P = point(3, 1) << id: \"P\", name: \"\" >>; " +
                    "circle = circle(A, B) << " +
                    "id: \"circle\", name: \"\" >>; " +
                    "taken = point(8, 8) << " +
                    "id: \"taken\", name: \"\" >>; " +
                    "polarline(P, circle) << id: \"taken\", " +
                    "point1: << id: \"temporary1\" >>, " +
                    "point2: << id: \"temporary2\" >> >>;",
            board = duplicateLineBoard,
        )
        val lineFactory = assertIs<
            JessieCodeCreatorError.TangentFactory,
            >(lineFailure.error)
        assertIs<TangentError.LineCreation>(lineFactory.error)
        assertEquals(
            setOf("A", "B", "P", "circle", "taken"),
            duplicateLineBoard.objects.keys,
        )
    }

    @Test
    fun tangentToCreatorPreservesNestedAttributesIndicesAndUpdates() {
        val board = board("tangent-to")
        evaluate(
            source =
                """
                center = point(1, 1) << id: "center", name: "" >>;
                radius = point(4, 1) << id: "radius", name: "" >>;
                circle = circle(center, radius) <<
                    id: "circle", name: ""
                >>;
                source = point(5, 4) << id: "source", name: "" >>;
                first = tangentto(circle, source) <<
                    id: "firstTangent",
                    name: "first",
                    straightFirst: false,
                    straightLast: true,
                    point1: <<
                        id: "firstTangentPoint1",
                        name: "firstP1"
                    >>,
                    point2: <<
                        id: "firstTangentPoint2",
                        name: "firstP2"
                    >>,
                    polar: <<
                        id: "firstPolar",
                        name: "polar",
                        straightFirst: true,
                        straightLast: false,
                        point1: <<
                            id: "firstPolarPoint1",
                            name: "polarP1"
                        >>,
                        point2: <<
                            id: "firstPolarPoint2",
                            name: "polarP2"
                        >>
                    >>,
                    point: <<
                        id: "firstIntersection",
                        name: "contact",
                        fixed: true
                    >>
                >>;
                second = tangentto(circle, source, 2) <<
                    id: "secondTangent",
                    name: "",
                    point1: << id: "secondTangentPoint1", name: "" >>,
                    point2: << id: "secondTangentPoint2", name: "" >>,
                    polar: <<
                        id: "secondPolar",
                        name: "",
                        point1: << id: "secondPolarPoint1", name: "" >>,
                        point2: << id: "secondPolarPoint2", name: "" >>
                    >>,
                    point: << id: "secondIntersection", name: "" >>
                >>;
                """.trimIndent(),
            board = board,
        )
        board.update()

        assertTrue("tangentto" in NativeJessieCodeCreators.names)
        val first = assertIs<Line>(board.select("firstTangent"))
        val firstPolar = assertIs<Line>(board.select("firstPolar"))
        val firstPoint = assertIs<IntersectionPoint>(
            board.select("firstIntersection"),
        )
        val second = assertIs<Line>(board.select("secondTangent"))
        val secondPoint = assertIs<IntersectionPoint>(
            board.select("secondIntersection"),
        )

        assertEquals("tangentto", first.elType)
        assertEquals("first", first.name)
        assertFalse(first.straightFirst)
        assertTrue(first.straightLast)
        assertEquals("firstP1", first.point1.name)
        assertEquals("firstP2", first.point2.name)
        assertSame(firstPoint, first.tangentToPoint)
        assertSame(firstPolar, first.tangentToPolar)
        assertSame(firstPoint, first.glider)
        assertEquals("polar", firstPolar.name)
        assertTrue(firstPolar.straightFirst)
        assertFalse(firstPolar.straightLast)
        assertEquals("polarP1", firstPolar.point1.name)
        assertEquals("polarP2", firstPolar.point2.name)
        assertEquals("contact", firstPoint.name)
        assertTrue(firstPoint.isFixed)
        assertEquals(listOf("circle", "firstIntersection"), first.parents)
        assertEquals(listOf("circle", "source"), firstPolar.parents)
        assertPoint(1.0000000000000073, 4.0, firstPoint.coords)
        assertPoint(3.88, 0.16000000000000036, secondPoint.coords)
        assertEquals(
            listOf(
                "center",
                "radius",
                "circle",
                "source",
                "firstPolarPoint1",
                "firstPolarPoint2",
                "firstPolar",
                "firstIntersection",
                "firstTangentPoint1",
                "firstTangentPoint2",
                "firstTangent",
                "secondPolarPoint1",
                "secondPolarPoint2",
                "secondPolar",
                "secondIntersection",
                "secondTangentPoint1",
                "secondTangentPoint2",
                "secondTangent",
            ),
            board.objectsList.map { it.id },
        )

        assertIs<Point>(board.select("center")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        assertIs<Point>(board.select("radius")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        assertIs<Point>(board.select("source")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -3.0),
        )
        board.update()

        assertPoint(1.93238075793812, 2.73238075793812, firstPoint.coords)
        assertPoint(
            -2.73238075793812,
            -1.93238075793812,
            secondPoint.coords,
        )
        assertPoint(-0.6, 0.2, firstPolar.point1.coords)
        assertPoint(-0.5, 0.3, firstPolar.point2.coords)
        assertSame(secondPoint, second.tangentToPoint)
    }

    @Test
    fun tangentToCreatorRejectsInvalidInputsAndIdCollisionsAtomically() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "P = point(3, 1); source = line(A, B); " +
                    "tangentto(source, P);",
            board = board("tangent-to-wrong-parents"),
        )
        assertEquals("tangentto", wrongParents.creatorName)
        assertEquals(
            TangentToError.UnsupportedParents(listOf("line", "point")),
            assertIs<JessieCodeCreatorError.TangentToFactory>(
                wrongParents.error,
            ).error,
        )

        val invalidIndex = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "P = point(3, 1); circle = circle(A, B); " +
                    "tangentto(circle, P, \"1\");",
            board = board("tangent-to-invalid-index"),
        )
        assertEquals("tangentto", invalidIndex.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            invalidIndex.error,
        )

        val invalidPolar = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "P = point(3, 1); circle = circle(A, B); " +
                    "tangentto(circle, P) << polar: 4 >>;",
            board = board("tangent-to-invalid-polar"),
        )
        assertEquals(
            "polar",
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidPolar.error,
            ).attribute,
        )

        val invalidPointFixed = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "P = point(3, 1); circle = circle(A, B); " +
                    "tangentto(circle, P) << " +
                    "point: << fixed: \"yes\" >> >>;",
            board = board("tangent-to-invalid-fixed"),
        )
        assertEquals(
            "fixed",
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidPointFixed.error,
            ).attribute,
        )

        val duplicateBoard = board("tangent-to-duplicate")
        val duplicate = creatorError(
            source =
                """
                A = point(0, 0) << id: "A", name: "" >>;
                B = point(2, 0) << id: "B", name: "" >>;
                P = point(3, 1) << id: "P", name: "" >>;
                circle = circle(A, B) << id: "circle", name: "" >>;
                tangentto(circle, P) <<
                    id: "shared",
                    point1: << id: "tangentPoint1" >>,
                    point2: << id: "tangentPoint2" >>,
                    polar: <<
                        id: "polar",
                        point1: << id: "shared" >>,
                        point2: << id: "polarPoint2" >>
                    >>,
                    point: << id: "intersection" >>
                >>;
                """.trimIndent(),
            board = duplicateBoard,
        )
        assertEquals(
            TangentToError.DuplicateElementId("shared"),
            assertIs<JessieCodeCreatorError.TangentToFactory>(
                duplicate.error,
            ).error,
        )
        assertEquals(
            setOf("A", "B", "P", "circle"),
            duplicateBoard.objects.keys,
        )
        assertEquals(
            setOf("circle"),
            assertIs<Point>(duplicateBoard.select("A")).childElements.keys,
        )
        assertTrue(
            assertIs<Point>(duplicateBoard.select("P"))
                .childElements
                .isEmpty(),
        )
    }

    @Test
    fun normalCreatorCoversLineCircleAndCurveBranches() {
        val board = board("normal")
        evaluate(
            source =
                "A = point(-4, -2) << id: \"A\", name: \"\" >>; " +
                    "B = point(3, 2) << id: \"B\", name: \"\" >>; " +
                    "sourceLine = line(A, B) << " +
                    "id: \"sourceLine\", name: \"\" >>; " +
                    "linePoint = point(-1, 4) << " +
                    "id: \"linePoint\", name: \"\" >>; " +
                    "lineNormal = normal(sourceLine, linePoint) << " +
                    "id: \"lineNormal\", name: \"\", " +
                    "straightFirst: false, straightLast: true, " +
                    "point: << id: \"lineNormalPoint\", " +
                    "name: \"helper\" >> >>; " +
                    "center = point(2, -1) << " +
                    "id: \"center\", name: \"\" >>; " +
                    "radius = point(5, -1) << " +
                    "id: \"radius\", name: \"\" >>; " +
                    "circle = circle(center, radius) << " +
                    "id: \"circle\", name: \"\" >>; " +
                    "circlePoint = point(4, 3) << " +
                    "id: \"circlePoint\", name: \"\" >>; " +
                    "circleNormal = normal(circlePoint, circle) << " +
                    "id: \"circleNormal\", name: \"\" >>; " +
                    "curve = functiongraph(" +
                    "\"0.5 * x * x - 2\", -5, 5" +
                    ") << id: \"curve\", name: \"\", " +
                    "doAdvancedPlot: false, numberPointsHigh: 32 >>; " +
                    "curvePoint = point(-2, 3) << " +
                    "id: \"curvePoint\", name: \"\" >>; " +
                    "curveNormal = normal(curve, curvePoint) << " +
                    "id: \"curveNormal\", name: \"\", " +
                    "point1: << id: \"curveNormalPoint1\", name: \"\" >>, " +
                    "point2: << id: \"curveNormalPoint2\", name: \"\" >> >>;",
            board = board,
        )

        assertTrue("normal" in NativeJessieCodeCreators.names)
        val sourceLine = assertIs<Line>(board.select("sourceLine"))
        val linePoint = assertIs<Point>(board.select("linePoint"))
        val lineNormal = assertIs<Line>(board.select("lineNormal"))
        assertEquals("normal", lineNormal.elType)
        assertEquals(listOf("sourceLine", "linePoint"), lineNormal.parents)
        assertSame(linePoint, lineNormal.point1)
        assertEquals("lineNormalPoint", lineNormal.point2.id)
        assertEquals("helper", lineNormal.point2.name)
        assertSame(lineNormal.point2, lineNormal.normalPoint)
        assertSame(lineNormal.point2, lineNormal.subs["point"])
        assertFalse(lineNormal.straightFirst)
        assertTrue(lineNormal.straightLast)
        assertSame(lineNormal, sourceLine.childElements["lineNormal"])

        val center = assertIs<Point>(board.select("center"))
        val circlePoint = assertIs<Point>(board.select("circlePoint"))
        val circleNormal = assertIs<Line>(board.select("circleNormal"))
        assertSame(center, circleNormal.point1)
        assertSame(circlePoint, circleNormal.point2)
        assertEquals(listOf("circlePoint", "circle"), circleNormal.parents)

        val curveNormal = assertIs<Line>(board.select("curveNormal"))
        assertTrue(curveNormal.constrained)
        assertFalse(curveNormal.isDraggable)
        assertEquals("curveNormalPoint1", curveNormal.point1.id)
        assertEquals("curveNormalPoint2", curveNormal.point2.id)
        assertPoint(-2.399999999994759, 2.8000000000026204, curveNormal.point1.coords)
        assertPoint(-1.9999999999973794, 3.00000000000131, curveNormal.point2.coords)

        assertEquals(
            -0.8682431421244593,
            lineNormal.point2.Y(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun normalCreatorRejectsUnsupportedParentsAndRollsBackHelpers() {
        val coordinateParent = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "source = line(A, B); normal(source, [1, 4]);",
            board = board("normal-coordinate"),
        )
        assertEquals("normal", coordinateParent.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            coordinateParent.error,
        )

        val onePointCurve = creatorError(
            source =
                "curve = curve([1], [2]); P = point(1, 2); " +
                    "normal(curve, P);",
            board = board("normal-one-point"),
        )
        assertEquals(
            NormalError.InvalidCurvePointCount(1),
            assertIs<JessieCodeCreatorError.NormalFactory>(
                onePointCurve.error,
            ).error,
        )

        val duplicateBoard = board("normal-rollback")
        val duplicate = creatorError(
            source =
                "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                    "B = point(2, 0) << id: \"B\", name: \"\" >>; " +
                    "P = point(3, 1) << id: \"P\", name: \"\" >>; " +
                    "curve = functiongraph(\"x * x\", -2, 2) << " +
                    "id: \"curve\", name: \"\", " +
                    "doAdvancedPlot: false, numberPointsHigh: 16 >>; " +
                    "normal(curve, P) << id: \"unused\", " +
                    "point1: << id: \"temporary\" >>, " +
                    "point2: << id: \"A\" >> >>;",
            board = duplicateBoard,
        )
        val factory = assertIs<JessieCodeCreatorError.NormalFactory>(
            duplicate.error,
        )
        assertIs<NormalError.PointCreation>(factory.error)
        assertEquals(
            setOf("A", "B", "P", "curve"),
            duplicateBoard.objects.keys,
        )
    }

    @Test
    fun polePointCreatorCanonicalizesParentsAndTracksDynamicGeometry() {
        val board = board("pole-point")
        evaluate(
            source =
                "center = point(1, 1) << id: \"center\", name: \"\" >>; " +
                    "radius = point(3, 1) << id: \"radius\", name: \"\" >>; " +
                    "circle = circle(center, radius) << " +
                    "id: \"circle\", name: \"\" >>; " +
                    "linePoint1 = point(-1, 4) << " +
                    "id: \"linePoint1\", name: \"\" >>; " +
                    "linePoint2 = point(4, -1) << " +
                    "id: \"linePoint2\", name: \"\" >>; " +
                    "line = line(linePoint1, linePoint2) << " +
                    "id: \"line\", name: \"\" >>; " +
                    "circleFirst = polepoint(circle, \"line\") << " +
                    "id: \"circleFirst\", name: \"\", fixed: true >>; " +
                    "lineFirst = polepoint(line, circle) << " +
                    "id: \"lineFirst\", name: \"\" >>;",
            board = board,
        )
        val circle = assertIs<Circle>(board.select("circle"))
        val line = assertIs<Line>(board.select("line"))
        val center = assertIs<Point>(board.select("center"))
        val radius = assertIs<Point>(board.select("radius"))
        val linePoint1 = assertIs<Point>(board.select("linePoint1"))
        val linePoint2 = assertIs<Point>(board.select("linePoint2"))
        val circleFirst = assertIs<Point>(board.select("circleFirst"))
        val lineFirst = assertIs<Point>(board.select("lineFirst"))

        assertTrue("polepoint" in NativeJessieCodeCreators.names)
        for (point in listOf(circleFirst, lineFirst)) {
            assertEquals("polepoint", point.elType)
            assertEquals(Const.OBJECT_TYPE_CAS, point.type)
            assertEquals(listOf("circle", "line"), point.parents)
            assertTrue(point.isConstrained)
            assertFalse(point.isDraggable)
            assertPoint(5.0, 5.0, point.coords)
        }
        assertTrue(circleFirst.isFixed)
        assertFalse(lineFirst.isFixed)
        assertEquals(
            setOf("circleFirst", "lineFirst"),
            circle.childElements.keys,
        )
        assertEquals(
            setOf("circleFirst", "lineFirst"),
            line.childElements.keys,
        )

        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        radius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 2.0),
        )
        linePoint1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-3.0, -1.0),
        )
        linePoint2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 4.0),
        )
        board.update()

        for (point in listOf(circleFirst, lineFirst)) {
            assertPoint(
                2.499999999999999,
                -2.499999999999999,
                point.coords,
            )
        }
    }

    @Test
    fun polePointCreatorRejectsInvalidInputsAndDuplicateIdsAtomically() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "circle = circle(A, B); polepoint(circle, A);",
            board = board("pole-wrong-parents"),
        )
        assertEquals("polepoint", wrongParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongParents.error,
        )

        val missingParent = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "circle = circle(A, B); " +
                    "polepoint(circle, \"missing\");",
            board = board("pole-missing-parent"),
        )
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            missingParent.error,
        )

        val invalidAttribute = creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); " +
                    "C = point(-1, -1); D = point(1, 1); " +
                    "circle = circle(A, B); line = line(C, D); " +
                    "polepoint(circle, line) << fixed: 1 >>;",
            board = board("pole-invalid-attribute"),
        )
        assertEquals(
            "fixed",
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidAttribute.error,
            ).attribute,
        )

        val duplicateBoard = board("pole-duplicate")
        val duplicate = creatorError(
            source =
                "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                    "B = point(2, 0) << id: \"B\", name: \"\" >>; " +
                    "C = point(-1, -1) << id: \"C\", name: \"\" >>; " +
                    "D = point(1, 1) << id: \"D\", name: \"\" >>; " +
                    "circle = circle(A, B) << " +
                    "id: \"circle\", name: \"\" >>; " +
                    "line = line(C, D) << id: \"line\", name: \"\" >>; " +
                    "taken = point(8, 8) << " +
                    "id: \"taken\", name: \"\" >>; " +
                    "polepoint(line, circle) << " +
                    "id: \"taken\", name: \"\" >>;",
            board = duplicateBoard,
        )
        val factory = assertIs<JessieCodeCreatorError.PolePointFactory>(
            duplicate.error,
        )
        val pointCreation = assertIs<PolePointError.PointCreation>(
            factory.error,
        )
        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            pointCreation.error,
        )
        assertEquals(
            setOf("A", "B", "C", "D", "circle", "line", "taken"),
            duplicateBoard.objects.keys,
        )
        assertTrue(
            assertIs<Circle>(duplicateBoard.select("circle"))
                .childElements
                .isEmpty(),
        )
        assertTrue(
            assertIs<Line>(duplicateBoard.select("line"))
                .childElements
                .isEmpty(),
        )
    }

    @Test
    fun midpointCreatorSupportsPointReferencesCoordinateArraysAndLines() {
        val pointBoard = board("point-midpoint")
        evaluate(
            source =
                "A = point(-4, 2) << id: \"first\", name: \"First\" >>; " +
                    "B = point(2, -2) << id: \"second\", name: \"Second\" >>; " +
                    "M = midpoint(\"first\", \"Second\") << " +
                    "id: \"middle\", name: \"Middle\", fixed: true >>;",
            board = pointBoard,
        )
        val first = assertIs<Point>(pointBoard.select("first"))
        val second = assertIs<Point>(pointBoard.select("second"))
        val middle = assertIs<MidpointPoint>(pointBoard.select("middle"))

        assertContentEquals(
            doubleArrayOf(1.0, -1.0, 0.0),
            middle.coords.usrCoords,
        )
        assertEquals(listOf(first.id, second.id), middle.parents)
        assertTrue(middle.isFixed)
        assertSame(middle, first.childElements[middle.id])
        assertSame(middle, second.childElements[middle.id])

        first.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 4.0),
        )
        pointBoard.update(draggedElement = first)
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 1.0),
            middle.coords.usrCoords,
        )

        val lineBoard = board("line-midpoint")
        evaluate(
            source =
                "A = point(-6, -4); B = point(2, -4); " +
                    "l = segment(A, B); M = midpoint(l);",
            board = lineBoard,
        )
        val line = assertIs<Line>(lineBoard.select("l"))
        val lineMiddle = assertIs<MidpointPoint>(lineBoard.select("M"))
        assertSame(line.point1, lineMiddle.point1)
        assertSame(line.point2, lineMiddle.point2)
        assertEquals(
            listOf(line.point1.id, line.point2.id),
            lineMiddle.parents,
        )
        assertContentEquals(
            doubleArrayOf(1.0, -2.0, -4.0),
            lineMiddle.coords.usrCoords,
        )

        val coordinateBoard = board("coordinate-midpoint")
        val coordinateMiddle = assertIs<MidpointPoint>(
            point(
                evaluate(
                    source =
                        "midpoint([-2, 4], [4, 2]) << " +
                            "id: \"middle\", name: \"\" >>;",
                    board = coordinateBoard,
                ),
            ),
        )
        assertEquals(3, coordinateBoard.numObjects)
        assertEquals(2, coordinateMiddle.ownedPoints.size)
        assertEquals(
            coordinateMiddle.ownedPoints.map(Point::id).toSet(),
            coordinateMiddle.childElements.keys,
        )
        assertTrue(coordinateMiddle.ancestors.isEmpty())
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 3.0),
            coordinateMiddle.coords.usrCoords,
        )

        coordinateBoard.removeObject(coordinateMiddle)
        assertTrue(coordinateBoard.objects.isEmpty())
        assertTrue(coordinateBoard.objectsList.isEmpty())
    }

    @Test
    fun midpointCreatorRejectsUnsupportedParentsAndRollsBackHelpers() {
        val functionBoard = board("function-midpoint")
        val functionFailure = creatorError(
            source =
                "midpoint(" +
                    "function () { return [0, 0]; }, [2, 2]" +
                    ");",
            board = functionBoard,
        )
        assertEquals("midpoint", functionFailure.creatorName)
        assertEquals(
            listOf("function", "array"),
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                functionFailure.error,
            ).parentTypes,
        )
        assertTrue(functionBoard.objects.isEmpty())

        val invalidBoard = board("invalid-midpoint")
        val invalidFailure = creatorError(
            source = "midpoint([0, 0], 2);",
            board = invalidBoard,
        )
        assertEquals(
            listOf("array", "number"),
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                invalidFailure.error,
            ).parentTypes,
        )
        assertTrue(invalidBoard.objects.isEmpty())

        val duplicateBoard = board("duplicate-midpoint")
        evaluate(
            source = "point(0, 0) << id: \"taken\", name: \"\" >>;",
            board = duplicateBoard,
        )
        val duplicateFailure = creatorError(
            source =
                "midpoint([0, 0], [2, 2]) << " +
                    "id: \"taken\", name: \"\" >>;",
            board = duplicateBoard,
        )
        val midpointFailure =
            assertIs<JessieCodeCreatorError.MidpointFactory>(
                duplicateFailure.error,
            )
        assertEquals(
            MidpointError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            midpointFailure.error,
        )
        assertEquals(setOf("taken"), duplicateBoard.objects.keys)
        assertEquals(1, duplicateBoard.objectsList.size)
    }

    @Test
    fun orthogonalCreatorsSupportBothParentOrdersAndOfficialRelationships() {
        val board = board("orthogonal-creators")
        evaluate(
            source =
                "A = point(-4, -1) << id: \"first\", name: \"A\" >>; " +
                    "B = point(2, 3) << id: \"second\", name: \"B\" >>; " +
                    "l = line(A, B) << id: \"baseLine\", name: \"l\" >>; " +
                    "P = point(3, -3) << id: \"source\", name: \"P\" >>; " +
                    "projection = orthogonalprojection(P, l) << " +
                    "id: \"projection\", fixed: true >>; " +
                    "projectionReverse = orthogonalprojection(l, P); " +
                    "foot = perpendicularpoint(P, l); " +
                    "normal = perpendicular(P, l); " +
                    "normalReverse = perpendicular(l, P); " +
                    "drop = perpendicularsegment(l, P);",
            board = board,
        )
        val source = assertIs<Point>(board.select("source"))
        val baseLine = assertIs<Line>(board.select("baseLine"))
        val projection =
            assertIs<OrthogonalPoint>(board.select("projection"))
        val projectionReverse =
            assertIs<OrthogonalPoint>(board.select("projectionReverse"))
        val foot = assertIs<OrthogonalPoint>(board.select("foot"))
        val normal = assertIs<PerpendicularLine>(board.select("normal"))
        val normalReverse =
            assertIs<PerpendicularLine>(board.select("normalReverse"))
        val drop =
            assertIs<PerpendicularSegmentLine>(board.select("drop"))

        assertEquals(
            listOf(source.id, projection.id),
            projection.parents,
        )
        assertTrue(projection.isFixed)
        assertEquals(
            listOf(source.id, projectionReverse.id),
            projectionReverse.parents,
        )
        assertEquals(listOf(source.id, baseLine.id), foot.parents)
        assertEquals(listOf(baseLine.id, source.id), normal.parents)
        assertEquals(
            listOf(baseLine.id, source.id),
            normalReverse.parents,
        )
        assertEquals(listOf(source.id, baseLine.id), drop.parents)
        assertEquals(-0.07692307692307687, projection.X(), 1.0e-12)
        assertEquals(1.6153846153846156, projection.Y(), 1.0e-12)
        assertEquals(projection.X(), projectionReverse.X(), 1.0e-12)
        assertEquals(projection.Y(), projectionReverse.Y(), 1.0e-12)
        assertEquals(projection.X(), foot.X(), 1.0e-12)
        assertEquals(projection.Y(), foot.Y(), 1.0e-12)
        assertSame(drop.point, drop.point1)
        assertSame(source, drop.point2)

        source.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 4.0),
        )
        board.update()

        assertEquals(14.0 / 13.0, projection.X(), 1.0e-12)
        assertEquals(31.0 / 13.0, projection.Y(), 1.0e-12)
        assertEquals(projection.X(), foot.X(), 1.0e-12)
        assertEquals(projection.Y(), foot.Y(), 1.0e-12)
        assertEquals(0.0, drop.point2.X(), 1.0e-12)
        assertEquals(4.0, drop.point2.Y(), 1.0e-12)
    }

    @Test
    fun orthogonalCreatorsMaterializeAndOwnCoordinatePointParents() {
        val projectionBoard = board("coordinate-projection")
        evaluate(
            source =
                "A = point(-4, -1); B = point(2, 3); " +
                    "l = line(A, B); " +
                    "projection = orthogonalprojection(l, [3, -3]);",
            board = projectionBoard,
        )
        val projection =
            assertIs<OrthogonalPoint>(projectionBoard.select("projection"))
        val projectionSource = projection.sourcePoint
        assertTrue(projection.ownsSourcePoint)
        assertSame(
            projectionSource,
            projection.childElements[projectionSource.id],
        )
        assertEquals(5, projectionBoard.objects.size)

        projectionBoard.removeObject(projection)

        assertEquals(null, projectionBoard.elementById(projectionSource.id))
        assertEquals(3, projectionBoard.objects.size)

        val normalBoard = board("coordinate-normal")
        evaluate(
            source =
                "A = point(-4, -1); B = point(2, 3); " +
                    "l = line(A, B); " +
                    "normal = perpendicular([3, -3], l);",
            board = normalBoard,
        )
        val normal =
            assertIs<PerpendicularLine>(normalBoard.select("normal"))
        val normalSource = normal.sourcePoint
        val endpointIds = setOf(normal.point1.id, normal.point2.id)
        assertTrue(normal.ownsSourcePoint)
        assertEquals(7, normalBoard.objects.size)

        normalBoard.removeObject(normal)

        assertEquals(null, normalBoard.elementById(normalSource.id))
        assertTrue(endpointIds.none(normalBoard.objects::containsKey))
        assertEquals(3, normalBoard.objects.size)

        val segmentBoard = board("coordinate-segment")
        evaluate(
            source =
                "A = point(-4, -1); B = point(2, 3); " +
                    "l = line(A, B); " +
                    "drop = perpendicularsegment([3, -3], l);",
            board = segmentBoard,
        )
        val drop =
            assertIs<PerpendicularSegmentLine>(segmentBoard.select("drop"))
        val segmentSource = drop.sourcePoint
        val segmentHelper = drop.point
        assertTrue(segmentHelper.ownsSourcePoint)
        assertEquals(6, segmentBoard.objects.size)

        segmentBoard.removeObject(drop)

        assertSame(
            segmentSource,
            segmentBoard.elementById(segmentSource.id),
        )
        assertSame(
            segmentHelper,
            segmentBoard.elementById(segmentHelper.id),
        )
        assertEquals(5, segmentBoard.objects.size)
    }

    @Test
    fun orthogonalCreatorFailuresAreStructuredAndAtomic() {
        val invalidBoard = board("invalid-orthogonal")
        evaluate(
            source =
                "A = point(0, 0); B = point(2, 2); l = line(A, B);",
            board = invalidBoard,
        )
        val originalIds = invalidBoard.objects.keys.toSet()
        val unsupported = creatorError(
            source = "perpendicular(l, l);",
            board = invalidBoard,
        )
        assertEquals("perpendicular", unsupported.creatorName)
        assertEquals(
            listOf("line", "line"),
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                unsupported.error,
            ).parentTypes,
        )
        assertEquals(originalIds, invalidBoard.objects.keys)

        for (
            creatorName in listOf(
                "orthogonalprojection",
                "perpendicularpoint",
                "perpendicular",
                "perpendicularsegment",
            )
        ) {
            val duplicateBoard = board("duplicate-$creatorName")
            evaluate(
                source =
                    "A = point(0, 0); B = point(2, 2); " +
                        "l = line(A, B); " +
                        "point(4, 4) << id: \"taken\", name: \"\" >>;",
                board = duplicateBoard,
            )
            val before = duplicateBoard.objects.keys.toSet()
            val failure = creatorError(
                source =
                    "$creatorName([3, -3], l) << " +
                        "id: \"taken\", name: \"\" >>;",
                board = duplicateBoard,
            )
            val error =
                assertIs<JessieCodeCreatorError.OrthogonalFactory>(
                    failure.error,
                )
            assertEquals(
                OrthogonalConstructionError.Registration(
                    BoardError.DuplicateElementId("taken"),
                ),
                error.error,
            )
            assertEquals(before, duplicateBoard.objects.keys)
            assertEquals(before.size, duplicateBoard.objectsList.size)
        }
    }

    @Test
    fun parallelCreatorsPreserveOfficialFormsRelationshipsAndUpdates() {
        val board = board("parallel-creators")
        evaluate(
            source =
                "A = point(-4, -1) << id: \"a\", name: \"A\" >>; " +
                    "B = point(2, 3) << id: \"b\", name: \"B\" >>; " +
                    "l = line(A, B) << id: \"baseLine\", name: \"l\" >>; " +
                    "C = point(3, -3) << id: \"c\", name: \"C\" >>; " +
                    "ppThree = parallelpoint(A, B, C) << " +
                    "id: \"ppThree\", fixed: true >>; " +
                    "ppLinePoint = parallelpoint(l, C); " +
                    "ppPointLine = parallelpoint(C, l); " +
                    "parallelThree = parallel(A, B, C) << " +
                    "id: \"parallelThree\" >>; " +
                    "parallelLinePoint = parallel(l, C) << " +
                    "id: \"parallelLinePoint\" >>; " +
                    "parallelPointLine = parallel(C, l) << " +
                    "id: \"parallelPointLine\" >>;",
            board = board,
        )
        val first = assertIs<Point>(board.select("a"))
        val second = assertIs<Point>(board.select("b"))
        val through = assertIs<Point>(board.select("c"))
        val sourceLine = assertIs<Line>(board.select("baseLine"))
        val points = listOf(
            assertIs<ParallelPoint>(board.select("ppThree")),
            assertIs<ParallelPoint>(board.select("ppLinePoint")),
            assertIs<ParallelPoint>(board.select("ppPointLine")),
        )
        val finite = assertIs<ParallelLine>(board.select("parallelThree"))
        val linePoint =
            assertIs<ParallelLine>(board.select("parallelLinePoint"))
        val pointLine =
            assertIs<ParallelLine>(board.select("parallelPointLine"))

        for (point in points) {
            assertEquals(listOf("a", "b", "c"), point.parents)
            assertEquals(9.0, point.X(), 1.0e-12)
            assertEquals(1.0, point.Y(), 1.0e-12)
            assertFalse(point.isDraggable)
        }
        assertTrue(points.first().isFixed)
        assertEquals(listOf("a", "b", "c"), finite.parents)
        assertIs<ParallelPoint>(finite.point)
        assertEquals(
            listOf("baseLine", "c"),
            linePoint.parents,
        )
        assertEquals(
            listOf("c", "baseLine"),
            pointLine.parents,
        )
        for (line in listOf(linePoint, pointLine)) {
            val helper = assertIs<ParallelDirectionPoint>(line.point)
            assertContentEquals(
                doubleArrayOf(
                    0.0,
                    -0.8320502943378437,
                    -0.5547001962252291,
                ),
                helper.coords.usrCoords,
            )
            assertTrue(helper.isDraggable)
        }
        assertTrue(sourceLine.childElements.isEmpty())

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 1.0),
        )
        through.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -4.0),
        )
        board.update()

        for (point in points) {
            assertEquals(7.0, point.X(), 1.0e-12)
            assertEquals(-5.0, point.Y(), 1.0e-12)
        }
        assertEquals(7.0, finite.point.X(), 1.0e-12)
        assertEquals(-5.0, finite.point.Y(), 1.0e-12)
        assertContentEquals(
            doubleArrayOf(
                0.0,
                -0.9863939238321437,
                0.1643989873053573,
            ),
            linePoint.point.coords.usrCoords,
        )
    }

    @Test
    fun parallelCreatorsOwnCoordinateHelpersAndFailAtomically() {
        val pointBoard = board("parallel-coordinate-point")
        val output = point(
            evaluate(
                source =
                    "parallelpoint([-4, -1], [2, 3], [3, -3]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = pointBoard,
            ),
        )
        assertIs<ParallelPoint>(output)
        assertEquals(4, pointBoard.objects.size)
        assertEquals(
            output.parents.toSet(),
            output.childElements.keys,
        )

        pointBoard.removeObject(output)

        assertTrue(pointBoard.objects.isEmpty())

        val lineBoard = board("parallel-coordinate-line")
        evaluate(
            source =
                "A = point(-4, -1); B = point(2, 3); " +
                    "l = line(A, B) << id: \"baseLine\" >>; " +
                    "output = parallel(l, [3, -3]) << id: \"output\" >>;",
            board = lineBoard,
        )
        val line = assertIs<ParallelLine>(lineBoard.select("output"))
        val ownedThrough = line.throughPoint
        val helper = line.point
        assertTrue(line.ownsThroughPoint)
        assertSame(ownedThrough, line.childElements[ownedThrough.id])

        lineBoard.removeObject(line)

        assertEquals(null, lineBoard.elementById(ownedThrough.id))
        assertSame(helper, lineBoard.elementById(helper.id))
        assertEquals(4, lineBoard.objects.size)

        val invalidBoard = board("invalid-parallel")
        evaluate(
            source =
                "A = point(0, 0); B = point(2, 2); " +
                    "l = line(A, B); " +
                    "point(4, 4) << id: \"taken\", name: \"\" >>;",
            board = invalidBoard,
        )
        val originalIds = invalidBoard.objects.keys.toSet()
        val unsupported = creatorError(
            source = "parallel(l, l);",
            board = invalidBoard,
        )
        assertEquals("parallel", unsupported.creatorName)
        assertEquals(
            listOf("line", "line"),
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                unsupported.error,
            ).parentTypes,
        )
        val duplicate = creatorError(
            source =
                "parallel(A, B, [3, -3]) << " +
                    "id: \"taken\", name: \"\" >>;",
            board = invalidBoard,
        )
        assertEquals(
            ParallelConstructionError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            assertIs<JessieCodeCreatorError.ParallelFactory>(
                duplicate.error,
            ).error,
        )
        assertEquals(originalIds, invalidBoard.objects.keys)
        assertEquals(originalIds.size, invalidBoard.objectsList.size)
    }

    @Test
    fun arrowCreatorsWrapLineAndParallelWithOfficialIdentityAndUpdates() {
        val board = board("arrow-creators")
        evaluate(
            source =
                "A = point(-5, 2) << id: \"a\", name: \"\" >>; " +
                    "B = point(-1, 2) << id: \"b\", name: \"\" >>; " +
                    "C = point(2, -2) << id: \"c\", name: \"\" >>; " +
                    "base = line(A, B) << id: \"base\", name: \"\" >>; " +
                    "direct = arrow(A, B) << id: \"direct\", name: \"\" >>; " +
                    "finite = arrowparallel(A, B, C) << " +
                    "id: \"finite\", name: \"\" >>; " +
                    "ideal = arrowparallel(base, C) << " +
                    "id: \"ideal\", name: \"\" >>;",
            board = board,
        )
        val first = assertIs<Point>(board.select("a"))
        val second = assertIs<Point>(board.select("b"))
        val through = assertIs<Point>(board.select("c"))
        val direct = assertIs<Line>(board.select("direct"))
        val finite = assertIs<ParallelLine>(board.select("finite"))
        val ideal = assertIs<ParallelLine>(board.select("ideal"))

        for (arrow in listOf(direct, finite, ideal)) {
            assertEquals(Const.OBJECT_TYPE_VECTOR, arrow.type)
        }
        assertEquals("arrow", direct.elType)
        assertEquals("arrowparallel", finite.elType)
        assertEquals("arrowparallel", ideal.elType)
        assertEquals(listOf("a", "b"), direct.parents)
        assertEquals(listOf("a", "b", "c"), finite.parents)
        assertEquals(listOf("base", "c"), ideal.parents)

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, -1.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        through.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -3.0),
        )
        board.update()

        assertEquals(-4.0, direct.point1.X(), 1.0e-12)
        assertEquals(-1.0, direct.point1.Y(), 1.0e-12)
        assertEquals(0.0, direct.point2.X(), 1.0e-12)
        assertEquals(3.0, direct.point2.Y(), 1.0e-12)
        assertEquals(7.0, finite.point.X(), 1.0e-12)
        assertEquals(1.0, finite.point.Y(), 1.0e-12)
        assertContentEquals(
            doubleArrayOf(
                0.0,
                -0.7071067811865475,
                -0.7071067811865475,
            ),
            ideal.point.coords.usrCoords,
        )
    }

    @Test
    fun arrowCreatorsRejectUnsupportedParentsWithoutThrowing() {
        val board = board("arrow-errors")
        evaluate(
            source =
                "A = point(0, 0); B = point(2, 2);",
            board = board,
        )

        for (
            (creatorName, source) in listOf(
                "arrow" to "arrow(A);",
                "arrowparallel" to "arrowparallel(A, B);",
            )
        ) {
            val failure = creatorError(source = source, board = board)
            assertEquals(creatorName, failure.creatorName)
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                failure.error,
            )
        }
    }

    @Test
    fun bisectorLinesCreatorReturnsAddressableCompositionAndUpdates() {
        val board = board("bisector-lines")
        val composition = assertIs<BisectorLines>(
            assertIs<JessieCodeRuntimeValue.CompositionReference>(
                evaluate(
                    source =
                        "A = point(-4, -1) << id: \"a\", name: \"\" >>; " +
                            "B = point(2, 3) << id: \"b\", name: \"\" >>; " +
                            "C = point(-3, 4) << id: \"c\", name: \"\" >>; " +
                            "D = point(4, -2) << id: \"d\", name: \"\" >>; " +
                            "first = line(A, B) << " +
                            "id: \"first\", name: \"\" >>; " +
                            "second = line(C, D) << " +
                            "id: \"second\", name: \"\" >>; " +
                            "pair = bisectorlines(first, second) << " +
                            "line1: << id: \"bisectorFirst\", " +
                            "name: \"first-bisector\" >>, " +
                            "line2: << id: \"bisectorSecond\", " +
                            "name: \"second-bisector\" >> >>; pair;",
                    board = board,
                ),
            ).composition,
        )
        val first = assertIs<Line>(composition.line1)
        val second = assertIs<Line>(composition.line2)

        assertTrue("bisectorlines" in NativeJessieCodeCreators.names)
        assertEquals("bisectorFirst", first.id)
        assertEquals("first-bisector", first.name)
        assertEquals("bisectorSecond", second.id)
        assertEquals("second-bisector", second.name)
        assertEquals(listOf("first", "second"), first.parents)
        assertEquals(listOf("first", "second"), second.parents)
        assertEquals(
            0.25014602495712585,
            first.stdform[0],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            1.5502410389027084,
            second.stdform[0],
            absoluteTolerance = 1.0e-12,
        )

        assertEquals(
            "runtimeFirst",
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                evaluate(
                    source =
                        "pair = bisectorlines(first, second) << " +
                            "line1: << id: \"runtimeFirst\" >>, " +
                            "line2: << id: \"runtimeSecond\" >> >>; " +
                            "pair.line1;",
                    board = board,
                ),
            ).element.id,
        )
    }

    @Test
    fun bisectorLinesCreatorRejectsParentsAndRollsBackDuplicateIds() {
        val board = board("bisector-lines-errors")
        evaluate(
            source =
                "A = point(-1, 0) << id: \"a\", name: \"\" >>; " +
                    "B = point(1, 0) << id: \"b\", name: \"\" >>; " +
                    "first = line(A, B) << id: \"first\", name: \"\" >>;",
            board = board,
        )

        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            creatorError(
                source = "bisectorlines(first, A);",
                board = board,
            ).error,
        )
        val before = board.objects.keys.toList()
        val duplicate = creatorError(
            source =
                "bisectorlines(first, first) << " +
                    "line1: << id: \"duplicate\" >>, " +
                    "line2: << id: \"duplicate\" >> >>;",
            board = board,
        )
        val factory = assertIs<JessieCodeCreatorError.BisectorLinesFactory>(
            duplicate.error,
        )
        val output = assertIs<BisectorLinesError.OutputLine>(factory.error)
        assertEquals(1, output.outputIndex)
        assertEquals(before, board.objects.keys.toList())
    }

    @Test
    fun bisectorLinesCompositionPropertiesAndDeletionMatchOfficialRuntime() {
        val board = board("bisector-lines-runtime")
        val type = evaluate(
            source =
                "A = point(-4, -1) << id: \"a\", name: \"\" >>; " +
                    "B = point(2, 3) << id: \"b\", name: \"\" >>; " +
                    "C = point(-3, 4) << id: \"c\", name: \"\" >>; " +
                    "D = point(4, -2) << id: \"d\", name: \"\" >>; " +
                    "first = line(A, B) << id: \"first\", name: \"\" >>; " +
                    "second = line(C, D) << id: \"second\", name: \"\" >>; " +
                    "pair = bisectorlines(first, second) << " +
                    "line1: << id: \"outputFirst\", name: \"\" >>, " +
                    "line2: << id: \"outputSecond\", name: \"\" >> >>; " +
                    "pair.getType();",
            board = board,
        )
        assertEquals(
            JessieCodeRuntimeValue.StringValue("bisectorlines"),
            type,
        )
        assertEquals(12, board.objects.size)

        evaluate(
            source =
                "pair = bisectorlines(first, second) << " +
                    "line1: << id: \"deleteFirst\", name: \"\" >>, " +
                    "line2: << id: \"deleteSecond\", name: \"\" >> >>; " +
                    "delete pair;",
            board = board,
        )

        assertSame(null, board.elementById("deleteFirst"))
        assertSame(null, board.elementById("deleteSecond"))
        assertSame(board.elementById("first"), board.select("first"))
        assertEquals(16, board.objects.size)
    }

    @Test
    fun triangleCenterCreatorsPreserveOfficialFormsAndUpdates() {
        val board = board("triangle-centers")
        evaluate(
            source =
                "A = point(-4, -1) << id: \"a\", name: \"A\" >>; " +
                    "B = point(1, 4) << id: \"b\", name: \"B\" >>; " +
                    "C = point(5, -2) << id: \"c\", name: \"C\" >>; " +
                    "angleBisector = bisector(A, B, C) << " +
                    "id: \"angleBisector\", name: \"\" >>; " +
                    "triangleIncenter = incenter(A, B, C) << " +
                    "id: \"triangleIncenter\", name: \"\", fixed: true >>; " +
                    "triangleIncircle = incircle(A, B, C) << " +
                    "id: \"triangleIncircle\", name: \"\" >>;",
            board = board,
        )
        val first = assertIs<Point>(board.select("a"))
        val vertex = assertIs<Point>(board.select("b"))
        val third = assertIs<Point>(board.select("c"))
        val bisector = assertIs<BisectorLine>(
            board.select("angleBisector"),
        )
        val incenter = assertIs<IncenterPoint>(
            board.select("triangleIncenter"),
        )
        val incircle = assertIs<IncircleCircle>(
            board.select("triangleIncircle"),
        )

        assertEquals(listOf("a", "b", "c"), bisector.parents)
        assertIs<AngleBisectorPoint>(bisector.point)
        assertContentEquals(
            doubleArrayOf(
                1.0,
                0.9014623820333578,
                3.0048666733319296,
            ),
            bisector.point.coords.usrCoords,
        )
        assertEquals(listOf("a", "b", "c"), incenter.parents)
        assertTrue(incenter.isFixed)
        assertContentEquals(
            doubleArrayOf(
                1.0,
                0.6670070476375293,
                0.6370976762025347,
            ),
            incenter.coords.usrCoords,
        )
        assertEquals(listOf("a", "b", "c"), incircle.parents)
        assertEquals(
            2.142469462922355,
            incircle.Radius(),
            absoluteTolerance = 1.0e-12,
        )

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        vertex.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, -4.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(
                1.0,
                -0.42887834669531066,
                2.096637745012613,
            ),
            bisector.point.coords.usrCoords,
        )
        assertEquals(
            -0.9316296783367083,
            incenter.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            1.0376741014781923,
            incenter.Y(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            1.7824673672181919,
            incircle.Radius(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun triangleCenterCreatorsOwnCoordinatesAndFailAtomically() {
        val incenterBoard = board("coordinate-incenter")
        val coordinateIncenter = point(
            evaluate(
                source =
                    "incenter([-4, -1], [1, 4], [5, -2]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = incenterBoard,
            ),
        )
        assertIs<IncenterPoint>(coordinateIncenter)
        assertEquals(4, incenterBoard.objects.size)
        assertEquals(
            coordinateIncenter.parents.toSet(),
            coordinateIncenter.childElements.keys,
        )

        incenterBoard.removeObject(coordinateIncenter)

        assertTrue(incenterBoard.objects.isEmpty())

        val bisectorBoard = board("coordinate-bisector")
        val coordinateBisector = line(
            evaluate(
                source =
                    "bisector([-4, -1], [1, 4], [5, -2]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = bisectorBoard,
            ),
        )
        assertIs<BisectorLine>(coordinateBisector)
        val bisectorHelper = coordinateBisector.point
        assertTrue(
            coordinateBisector.parents.none { it == coordinateBisector.id },
        )

        bisectorBoard.removeObject(coordinateBisector)

        assertSame(
            bisectorHelper,
            bisectorBoard.elementById(bisectorHelper.id),
        )
        assertEquals(4, bisectorBoard.objects.size)

        val incircleBoard = board("coordinate-incircle")
        val coordinateIncircle = circle(
            evaluate(
                source =
                    "incircle([-4, -1], [1, 4], [5, -2]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = incircleBoard,
            ),
        )
        assertIs<IncircleCircle>(coordinateIncircle)
        val center = coordinateIncircle.center

        incircleBoard.removeObject(coordinateIncircle)

        assertSame(center, incircleBoard.elementById(center.id))
        assertEquals(4, incircleBoard.objects.size)

        val invalidBoard = board("invalid-triangle-centers")
        evaluate(
            source =
                "A = point(0, 0); B = point(2, 2); C = point(4, 0); " +
                    "point(8, 8) << id: \"taken\", name: \"\" >>;",
            board = invalidBoard,
        )
        val originalIds = invalidBoard.objects.keys.toSet()
        for (creatorName in listOf("bisector", "incenter", "incircle")) {
            val unsupported = creatorError(
                source = "$creatorName(A, B);",
                board = invalidBoard,
            )
            assertEquals(creatorName, unsupported.creatorName)
            assertEquals(
                listOf("point", "point"),
                assertIs<JessieCodeCreatorError.UnsupportedParents>(
                    unsupported.error,
                ).parentTypes,
            )

            val duplicate = creatorError(
                source =
                    "$creatorName(A, B, [4, 0]) << " +
                        "id: \"taken\", name: \"\" >>;",
                board = invalidBoard,
            )
            assertEquals(
                TriangleCenterConstructionError.Registration(
                    BoardError.DuplicateElementId("taken"),
                ),
                assertIs<
                    JessieCodeCreatorError.TriangleCenterFactory,
                    >(duplicate.error).error,
            )
            assertEquals(originalIds, invalidBoard.objects.keys)
            assertEquals(
                originalIds.size,
                invalidBoard.objectsList.size,
            )
        }
    }

    @Test
    fun segmentCreatorSupportsAllTranslatedFixedLengthForms() {
        val pointBoard = board("point-segment")
        val pointSegment = line(
            evaluate(
                source =
                    "A = point(0, 0); B = point(4, 2); " +
                        "s = segment(A, B); s;",
                board = pointBoard,
            ),
        )
        assertEquals("s", pointSegment.name)
        assertEquals("segment", pointSegment.elType)
        assertEquals(
            listOf(
                assertIs<Point>(pointBoard.select("A")).id,
                assertIs<Point>(pointBoard.select("B")).id,
            ),
            pointSegment.parents,
        )
        assertEquals(0.5, pointSegment.Slope())

        val coordinateBoard = board("coordinate-segment")
        val coordinateSegment = line(
            evaluate(
                source = "segment([0, 0], [2, 3]);",
                board = coordinateBoard,
            ),
        )
        assertEquals("segment", coordinateSegment.elType)
        assertEquals(3, coordinateBoard.numObjects)
        assertEquals(1.5, coordinateSegment.Slope())

        val fixedLength = line(
            evaluate(
                source = "segment([0, 0], [2, 0], 3);",
                board = board("fixed-length-segment"),
            ),
        )
        assertTrue(fixedLength.hasFixedLength)
        assertEquals(-1.0, fixedLength.point1.X())
        assertEquals(2.0, fixedLength.point2.X())
        assertEquals(3.0, fixedLength.L())

        val fixedEndpointBoard = board("fixed-endpoint-segment")
        val fixedEndpoint = line(
            evaluate(
                source =
                    "A = point(0, 0) << fixed: true >>; " +
                        "B = point(2, 0); segment(A, B, 3);",
                board = fixedEndpointBoard,
            ),
        )
        assertEquals(0.0, fixedEndpoint.point1.X())
        assertEquals(3.0, fixedEndpoint.point2.X())

        val nonnegative = line(
            evaluate(
                source =
                    "segment([0, 0], [2, 0], -3) << " +
                        "nonnegativeOnly: true >>;",
                board = board("nonnegative-segment"),
            ),
        )
        assertTrue(nonnegative.nonnegativeOnly)
        assertEquals(0.0, nonnegative.L())

        val expressionBoard = board("string-length-segment")
        val stringLength = line(
            evaluate(
                source =
                    "A = point(3, 0); " +
                        "segment([0, 0], [2, 0], \"A.X() + 1\");",
                board = expressionBoard,
            ),
        )
        val driver = assertIs<Point>(expressionBoard.select("A"))
        assertEquals(4.0, stringLength.L())
        assertSame(stringLength, driver.childElements[stringLength.id])
        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(5.0, 0.0),
        )
        expressionBoard.update()
        assertEquals(6.0, stringLength.L())

        val functionLength = line(
            evaluate(
                source =
                    "segment([0, 0], [2, 0], " +
                        "function () { return 3; });",
                board = board("function-length-segment"),
            ),
        )
        assertEquals(3.0, functionLength.L())
        assertTrue(functionLength.hasFixedLength)
    }

    @Test
    fun lineAndSegmentHelperPointsRollBackOnEveryFactoryFailure() {
        val secondPointBoard = board("second-point-rollback")
        val secondPointFailure = creatorError(
            source = "line([0, 0], [\"Missing.X()\", 1]);",
            board = secondPointBoard,
        )
        assertIs<JessieCodeCreatorError.PointFactory>(
            secondPointFailure.error,
        )
        assertEquals(0, secondPointBoard.objects.size)

        val lineBoard = board("line-registration-rollback")
        evaluate(
            source = "point(0, 0) << id: \"taken\", name: \"\" >>;",
            board = lineBoard,
        )
        val lineFailure = creatorError(
            source = "line([1, 1], [2, 2]) << id: \"taken\" >>;",
            board = lineBoard,
        )
        assertIs<JessieCodeCreatorError.LineFactory>(lineFailure.error)
        assertEquals(1, lineBoard.objects.size)

        val coefficientBoard = board("coefficient-registration-rollback")
        evaluate(
            source = "point(0, 0) << id: \"taken\", name: \"\" >>;",
            board = coefficientBoard,
        )
        val coefficientFailure = creatorError(
            source = "line(1, -2, 3) << id: \"taken\" >>;",
            board = coefficientBoard,
        )
        assertIs<JessieCodeCreatorError.LineFactory>(
            coefficientFailure.error,
        )
        assertEquals(1, coefficientBoard.objects.size)

        val segmentBoard = board("segment-length-rollback")
        val segmentFailure = creatorError(
            source =
                "A = point(0, 0); " +
                    "segment(A, [2, 0], " +
                    "function () { return \"length\"; });",
            board = segmentBoard,
        )
        val lineError = assertIs<JessieCodeCreatorError.LineFactory>(
            segmentFailure.error,
        )
        assertIs<LineError.NonNumericFixedLengthFunction>(lineError.error)
        assertEquals(1, segmentBoard.objects.size)
        assertIs<Point>(segmentBoard.select("A"))
    }

    @Test
    fun circleCreatorSupportsTranslatedParentOverloads() {
        val board = board()
        val radiusCircle = circle(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "c = circle(A, -4); c;",
                board = board,
            ),
        )
        assertEquals("c", radiusCircle.name)
        assertEquals(4.0, radiusCircle.Radius())
        assertSame(board.select("A"), radiusCircle.center)

        val pointCircle = circle(
            evaluate(
                source =
                    "A = point(1, 2); B = point(4, 6); " +
                        "c = circle(A, B); c;",
                board = board("point-circle"),
            ),
        )
        assertEquals(5.0, pointCircle.Radius())

        val coordinateCircle = circle(
            evaluate(
                source = "circle([1, 2], 3);",
                board = board("coordinate-circle"),
            ),
        )
        assertEquals("", coordinateCircle.center.name)
        assertEquals(3.0, coordinateCircle.Radius())

        val elementRadiusBoard = board("element-radius-circle")
        val inheritedRadius = circle(
            evaluate(
                source =
                    "A = point(0, 0); B = point(3, 4); " +
                        "l = line(A, B); C = point(2, 2); " +
                        "c = circle(C, l); d = circle(c, A); d;",
                board = elementRadiusBoard,
            ),
        )
        assertEquals(5.0, inheritedRadius.Radius())
        assertSame(elementRadiusBoard.select("A"), inheritedRadius.center)
        val sourceCircle = assertIs<Circle>(
            elementRadiusBoard.select("c"),
        )
        assertSame(sourceCircle, inheritedRadius.circle)

        val threePointBoard = board("three-point-circle")
        val threePointCircle = circle(
            evaluate(
                source =
                    "A = point(-3, -2); B = point(3, -1); " +
                        "C = point(0, 3); c = circle(A, B, C); c;",
                board = threePointBoard,
            ),
        )
        assertEquals(
            -0.2037037037037037,
            threePointCircle.center.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -0.2777777777777778,
            threePointCircle.center.Y(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            listOf(
                assertIs<Point>(threePointBoard.select("A")).id,
                assertIs<Point>(threePointBoard.select("B")).id,
                assertIs<Point>(threePointBoard.select("C")).id,
            ),
            threePointCircle.parents,
        )

        val coordinateThreePointCircle = circle(
            evaluate(
                source = "circle([-3, -2], [3, -1], [0, 3]);",
                board = board("coordinate-three-point-circle"),
            ),
        )
        assertEquals(
            threePointCircle.Radius(),
            coordinateThreePointCircle.Radius(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun circumcenterAliasesAndCircumcircleMatchOfficialCreatorState() {
        val board = board("circumcircle-creators")
        evaluate(
            source =
                "A = point(-4, -1) << id: \"a\", name: \"\" >>; " +
                    "B = point(1, 4) << id: \"b\", name: \"\" >>; " +
                    "C = point(5, -2) << id: \"c\", name: \"\" >>; " +
                    "center = circumcenter(A, B, C) << " +
                    "id: \"center\", name: \"center-name\", " +
                    "fixed: true, needsRegularUpdate: false >>; " +
                    "alias = circumcirclemidpoint(A, B, C) << " +
                    "id: \"midpointAlias\", name: \"\" >>; " +
                    "output = circumcircle(A, B, C) << " +
                    "id: \"circumcircle\", name: \"\" >>;",
            board = board,
        )
        val first = assertIs<Point>(board.select("a"))
        val second = assertIs<Point>(board.select("b"))
        val third = assertIs<Point>(board.select("c"))
        val center = assertIs<CircumcenterPoint>(board.select("center"))
        val alias = assertIs<CircumcenterPoint>(
            board.select("midpointAlias"),
        )
        val output = assertIs<Circle>(board.select("circumcircle"))
        val helper = assertIs<CircumcenterPoint>(output.center)

        assertEquals("center-name", center.name)
        assertEquals("circumcenter", center.elType)
        assertTrue(center.isFixed)
        assertFalse(center.needsRegularUpdate)
        assertEquals("circumcenter", alias.elType)
        assertEquals("circumcircle", output.elType)
        assertEquals(listOf("a", "b", "c"), output.parents)
        assertSame(helper, output.subs["center"])
        assertEquals(
            listOf(helper, first, output),
            output.inherits,
        )
        assertFalse(helper.dump)
        assertEquals(0.6, center.X(), absoluteTolerance = 1.0e-12)
        assertEquals(0.6, alias.X(), absoluteTolerance = 1.0e-12)
        assertEquals(0.6, helper.X(), absoluteTolerance = 1.0e-12)

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, -4.0),
        )
        board.update()

        assertEquals(0.6, center.X(), absoluteTolerance = 1.0e-12)
        assertEquals(
            -2.108695652173913,
            alias.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.108695652173913,
            helper.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            6.2164030835191495,
            output.Radius(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun circumcenterCoordinateOwnershipAndCreatorFailuresAreStructured() {
        val centerBoard = board("coordinate-circumcenter")
        val center = point(
            evaluate(
                source =
                    "circumcenter([-4, -1], [1, 4], [5, -2]) " +
                        "<< name: \"\" >>;",
                board = centerBoard,
            ),
        )
        val ownedCenter = assertIs<CircumcenterPoint>(center)
        assertEquals(3, ownedCenter.ownedPoints.size)
        assertEquals(4, centerBoard.objects.size)

        centerBoard.removeObject(ownedCenter)

        assertTrue(centerBoard.objects.isEmpty())

        val circleBoard = board("coordinate-circumcircle")
        val output = circle(
            evaluate(
                source =
                    "circumcircle([-4, -1], [1, 4], [5, -2]) " +
                        "<< name: \"\" >>;",
                board = circleBoard,
            ),
        )
        val helper = assertIs<CircumcenterPoint>(output.center)
        assertEquals(3, helper.ownedPoints.size)
        assertEquals(5, circleBoard.objects.size)

        circleBoard.removeObject(output)

        assertEquals(4, circleBoard.objects.size)
        assertSame(helper, circleBoard.elementById(helper.id))

        circleBoard.removeObject(helper)

        assertTrue(circleBoard.objects.isEmpty())

        for (creatorName in listOf(
            "circumcenter",
            "circumcirclemidpoint",
            "circumcircle",
        )) {
            val failure = creatorError(
                source = "$creatorName([0, 0], [1, 1]);",
                board = board("too-few-$creatorName"),
            )
            assertEquals(creatorName, failure.creatorName)
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                failure.error,
            )
        }

        val wrongType = creatorError(
            source =
                "A = point(0, 0); " +
                    "L = line([0, 0], [1, 1]); " +
                    "circumcenter(A, A, L);",
            board = board("wrong-circumcenter-parent"),
        )
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongType.error,
        )

        val invalidFixed = creatorError(
            source =
                "circumcenter([0, 0], [1, 0], [0, 1]) " +
                    "<< fixed: \"yes\" >>;",
            board = board("invalid-circumcenter-fixed"),
        )
        assertEquals(
            "fixed",
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidFixed.error,
            ).attribute,
        )

        val duplicateBoard = board("circumcircle-rollback")
        val duplicate = creatorError(
            source =
                "point(0, 0) << id: \"taken\", name: \"\" >>; " +
                    "circumcircle([-4, -1], [1, 4], [5, -2]) " +
                    "<< id: \"taken\" >>;",
            board = duplicateBoard,
        )
        val circleFailure = assertIs<JessieCodeCreatorError.CircleFactory>(
            duplicate.error,
        )
        assertEquals(
            CircleError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            circleFailure.error,
        )
        assertEquals(1, duplicateBoard.objects.size)

        val foreignOwnedBoard = board("foreign-owned")
        val first = assertIs<Point>(
            evaluate("point(0, 0);", foreignOwnedBoard)
                .let(::point),
        )
        val second = assertIs<Point>(
            evaluate("point(1, 0);", foreignOwnedBoard)
                .let(::point),
        )
        val third = assertIs<Point>(
            evaluate("point(0, 1);", foreignOwnedBoard)
                .let(::point),
        )
        val extra = assertIs<Point>(
            evaluate("point(2, 2);", foreignOwnedBoard)
                .let(::point),
        )
        assertEquals(
            CircumcenterError.OwnedPointNotParent(extra.id),
            assertIs<GMResult.Err<CircumcenterError>>(
                CircumcenterPoint.create(
                    board = foreignOwnedBoard,
                    point1 = first,
                    point2 = second,
                    point3 = third,
                    ownedPoints = setOf(extra),
                ),
            ).error,
        )
    }

    @Test
    fun ellipseCreatorSupportsPointNumericFunctionAndCoordinateParents() {
        val pointBoard = board("ellipse-point")
        val pointEllipse = curve(
            evaluate(
                source =
                    "F1 = point(-3, 0) << id: \"f1\", name: \"\" >>; " +
                        "F2 = point(3, 0) << id: \"f2\", name: \"\" >>; " +
                        "C = point(0, 5) << id: \"c\", name: \"\" >>; " +
                        "E = ellipse(F1, F2, C) << " +
                        "id: \"ellipse\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 32, " +
                        "center: << id: \"center\", name: \"\", " +
                        "fixed: true >> >>; E;",
                board = pointBoard,
            ),
        )
        val focus1 = assertIs<Point>(pointBoard.select("f1"))
        val focus2 = assertIs<Point>(pointBoard.select("f2"))
        val ellipsePoint = assertIs<Point>(pointBoard.select("c"))
        val center = assertIs<Point>(pointBoard.select("center"))

        assertTrue("ellipse" in NativeJessieCodeCreators.names)
        assertTrue(pointEllipse.isEllipse)
        assertEquals("curve", pointEllipse.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, pointEllipse.type)
        assertEquals(32, pointEllipse.numberPoints)
        assertEquals(
            11.661903789690601,
            pointEllipse.majorAxis(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(listOf("f1", "f2", "c"), pointEllipse.parents)
        assertEquals(
            listOf<GeometryElement>(
                center,
                focus1,
                focus2,
                ellipsePoint,
            ),
            pointEllipse.inherits,
        )
        assertTrue(center.isFixed)
        assertSame(center, pointEllipse.subs["center"])

        val numericBoard = board("ellipse-numeric")
        val numericEllipse = curve(
            evaluate(
                source =
                    "ellipse([-2, 1], [4, 1], 10, " +
                        "-1.5707963267948966, 3.141592653589793) << " +
                        "id: \"numeric\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 16, " +
                        "foci: << fixed: true >> >>;",
                board = numericBoard,
            ),
        )
        assertEquals(emptyList(), numericEllipse.parents)
        assertEquals(16, numericEllipse.numberPoints)
        assertEquals(-1.5707963267948966, numericEllipse.minX())
        assertEquals(3.141592653589793, numericEllipse.maxX())
        assertEquals(4, numericBoard.objects.size)
        assertTrue(numericEllipse.foci.all(Point::isFixed))

        val functionBoard = board("ellipse-function")
        val functionEllipse = curve(
            evaluate(
                source =
                    "major = 10; " +
                        "F1 = point(-3, 0) << id: \"f1\", name: \"\" >>; " +
                        "F2 = point(3, 0) << id: \"f2\", name: \"\" >>; " +
                        "C = point(0, 5) << id: \"c\", name: \"\" >>; " +
                        "E = ellipse(" +
                        "function () { return F1; }, " +
                        "function () { return F2; }, " +
                        "function () { return major; }) << " +
                        "id: \"function\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "major = 14; E;",
                board = functionBoard,
            ),
        )
        functionBoard.fullUpdate()
        assertEquals(emptyList(), functionEllipse.parents)
        assertEquals(14.0, functionEllipse.majorAxis())
        assertEquals(7.0, functionEllipse.X(0.0))

        val functionPointBoard = board("ellipse-function-point")
        val functionPointEllipse = curve(
            evaluate(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); C = point(0, 5); " +
                        "E = ellipse(" +
                        "function () { return F1; }, " +
                        "function () { return F2; }, " +
                        "function () { return C; }) << " +
                        "name: \"\", doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>; E;",
                board = functionPointBoard,
            ),
        )
        assertEquals(emptyList(), functionPointEllipse.parents)
        assertSame(
            functionPointBoard.select("C"),
            functionPointEllipse.pointOnEllipse,
        )
    }

    @Test
    fun ellipseCreatorFailuresAreStructuredAndRollBackImplicitPoints() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); L = line([0, 0], [1, 1]); " +
                    "ellipse(A, L, 6) << doAdvancedPlot: false >>;",
            board = board("ellipse-wrong-parent"),
        )
        assertEquals("ellipse", wrongParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongParents.error,
        )

        val duplicateBoard = board("ellipse-duplicate")
        val duplicate = creatorError(
            source =
                "point(7, 5) << id: \"taken\", name: \"\" >>; " +
                    "ellipse([-3, 0], [3, 0], [0, 5]) << " +
                    "id: \"taken\", doAdvancedPlot: false, " +
                    "numberPointsHigh: 8 >>;",
            board = duplicateBoard,
        )
        assertEquals(
            EllipseError.DuplicateElementId("taken"),
            assertIs<JessieCodeCreatorError.EllipseFactory>(
                duplicate.error,
            ).error,
        )
        assertEquals(1, duplicateBoard.objects.size)

        val duplicateCenterBoard = board("ellipse-duplicate-center")
        val duplicateCenter = creatorError(
            source =
                "point(7, 5) << id: \"taken\", name: \"\" >>; " +
                    "ellipse([-3, 0], [3, 0], [0, 5]) << " +
                    "doAdvancedPlot: false, numberPointsHigh: 8, " +
                    "center: << id: \"taken\" >> >>;",
            board = duplicateCenterBoard,
        )
        assertEquals(
            EllipseError.DuplicateElementId("taken"),
            assertIs<JessieCodeCreatorError.EllipseFactory>(
                duplicateCenter.error,
            ).error,
        )
        assertEquals(1, duplicateCenterBoard.objects.size)
    }

    @Test
    fun hyperbolaCreatorSupportsPointNumericFunctionAndCoordinateParents() {
        val pointBoard = board("hyperbola-point")
        val pointHyperbola = curve(
            evaluate(
                source =
                    "F1 = point(-3, 0) << id: \"f1\", name: \"\" >>; " +
                        "F2 = point(3, 0) << id: \"f2\", name: \"\" >>; " +
                        "C = point(5, 2) << id: \"c\", name: \"\" >>; " +
                        "H = hyperbola(F1, F2, C) << " +
                        "id: \"hyperbola\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 32, " +
                        "center: << id: \"center\", name: \"\", " +
                        "fixed: true >> >>; H;",
                board = pointBoard,
            ),
        )
        val focus1 = assertIs<Point>(pointBoard.select("f1"))
        val focus2 = assertIs<Point>(pointBoard.select("f2"))
        val hyperbolaPoint = assertIs<Point>(pointBoard.select("c"))
        val center = assertIs<Point>(pointBoard.select("center"))

        assertTrue("hyperbola" in NativeJessieCodeCreators.names)
        assertTrue(pointHyperbola.isHyperbola)
        assertEquals("curve", pointHyperbola.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, pointHyperbola.type)
        assertEquals(32, pointHyperbola.numberPoints)
        assertEquals(
            5.417784126489131,
            pointHyperbola.majorAxis(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(listOf("f1", "f2", "c"), pointHyperbola.parents)
        assertEquals(
            listOf<GeometryElement>(
                center,
                focus1,
                focus2,
                hyperbolaPoint,
            ),
            pointHyperbola.inherits,
        )
        assertTrue(center.isFixed)
        assertSame(center, pointHyperbola.subs["center"])

        val numericBoard = board("hyperbola-numeric")
        val numericHyperbola = curve(
            evaluate(
                source =
                    "hyperbola([-3, 1], [3, 1], 4, " +
                        "-1.5707963267948966, 1.5707963267948966) << " +
                        "id: \"numeric\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 16, " +
                        "foci: << fixed: true >> >>;",
                board = numericBoard,
            ),
        )
        assertEquals(emptyList(), numericHyperbola.parents)
        assertEquals(16, numericHyperbola.numberPoints)
        assertEquals(-1.5707963267948966, numericHyperbola.minX())
        assertEquals(1.5707963267948966, numericHyperbola.maxX())
        assertEquals(4, numericBoard.objects.size)
        assertTrue(numericHyperbola.foci.all(Point::isFixed))

        val functionBoard = board("hyperbola-function")
        val functionHyperbola = curve(
            evaluate(
                source =
                    "major = 4; " +
                        "F1 = point(-3, 0) << id: \"f1\", name: \"\" >>; " +
                        "F2 = point(3, 0) << id: \"f2\", name: \"\" >>; " +
                        "H = hyperbola(" +
                        "function () { return F1; }, " +
                        "function () { return F2; }, " +
                        "function () { return major; }) << " +
                        "id: \"function\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "major = 2; H;",
                board = functionBoard,
            ),
        )
        functionBoard.fullUpdate()
        assertEquals(emptyList(), functionHyperbola.parents)
        assertEquals(2.0, functionHyperbola.majorAxis())
        assertEquals(-1.0, functionHyperbola.X(0.0))

        val functionPointBoard = board("hyperbola-function-point")
        val functionPointHyperbola = curve(
            evaluate(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); C = point(5, 2); " +
                        "H = hyperbola(" +
                        "function () { return F1; }, " +
                        "function () { return F2; }, " +
                        "function () { return C; }) << " +
                        "name: \"\", doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>; H;",
                board = functionPointBoard,
            ),
        )
        assertEquals(emptyList(), functionPointHyperbola.parents)
        assertSame(
            functionPointBoard.select("C"),
            functionPointHyperbola.pointOnHyperbola,
        )
    }

    @Test
    fun hyperbolaCreatorFailuresAreStructuredAndRollBackImplicitPoints() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); L = line([0, 0], [1, 1]); " +
                    "hyperbola(A, L, 4) << doAdvancedPlot: false >>;",
            board = board("hyperbola-wrong-parent"),
        )
        assertEquals("hyperbola", wrongParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongParents.error,
        )

        val duplicateBoard = board("hyperbola-duplicate")
        val duplicate = creatorError(
            source =
                "point(7, 5) << id: \"taken\", name: \"\" >>; " +
                    "hyperbola([-3, 0], [3, 0], [5, 2]) << " +
                    "id: \"taken\", doAdvancedPlot: false, " +
                    "numberPointsHigh: 8 >>;",
            board = duplicateBoard,
        )
        assertEquals(
            HyperbolaError.DuplicateElementId("taken"),
            assertIs<JessieCodeCreatorError.HyperbolaFactory>(
                duplicate.error,
            ).error,
        )
        assertEquals(1, duplicateBoard.objects.size)

        val duplicateCenterBoard = board("hyperbola-duplicate-center")
        val duplicateCenter = creatorError(
            source =
                "point(7, 5) << id: \"taken\", name: \"\" >>; " +
                    "hyperbola([-3, 0], [3, 0], [5, 2]) << " +
                    "doAdvancedPlot: false, numberPointsHigh: 8, " +
                    "center: << id: \"taken\" >> >>;",
            board = duplicateCenterBoard,
        )
        assertEquals(
            HyperbolaError.DuplicateElementId("taken"),
            assertIs<JessieCodeCreatorError.HyperbolaFactory>(
                duplicateCenter.error,
            ).error,
        )
        assertEquals(1, duplicateCenterBoard.objects.size)
    }

    @Test
    fun parabolaCreatorSupportsPointFunctionAndCoordinateParents() {
        val pointBoard = board("parabola-point")
        val pointParabola = curve(
            evaluate(
                source =
                    "A = point(-1, 4) << id: \"a\", name: \"\" >>; " +
                        "B = point(-1, -4) << id: \"b\", name: \"\" >>; " +
                        "L = line(A, B) << id: \"directrix\", name: \"\" >>; " +
                        "F = point(1, 1) << id: \"focus\", name: \"\" >>; " +
                        "P = parabola(F, L) << " +
                        "id: \"parabola\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 32, " +
                        "center: << id: \"center\", name: \"\", " +
                        "fixed: true >> >>; P;",
                board = pointBoard,
            ),
        )
        val focus = assertIs<Point>(pointBoard.select("focus"))
        val directrix = assertIs<Line>(pointBoard.select("directrix"))
        val center = assertIs<Point>(pointBoard.select("center"))

        assertTrue("parabola" in NativeJessieCodeCreators.names)
        assertTrue(pointParabola.isParabola)
        assertEquals(Const.OBJECT_TYPE_CONIC, pointParabola.type)
        assertEquals(32, pointParabola.numberPoints)
        assertEquals(1.0000000000000002, pointParabola.X(0.0))
        assertEquals(-1.0, pointParabola.Y(0.0))
        assertEquals(listOf("focus", "directrix"), pointParabola.parents)
        assertEquals(
            listOf<GeometryElement>(center, focus),
            pointParabola.inherits,
        )
        assertSame(directrix, pointParabola.parabolaDirectrix)
        assertTrue(center.isFixed)
        assertSame(center, pointParabola.subs["center"])

        val functionBoard = board("parabola-function")
        val functionParabola = curve(
            evaluate(
                source =
                    "F = point(3, 1) << id: \"focus\", name: \"\" >>; " +
                        "L = line([0, 3], [0, -3]) << " +
                        "id: \"directrix\", name: \"\" >>; " +
                        "P = parabola(function () { return F; }, L) << " +
                        "name: \"\", doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>; P;",
                board = functionBoard,
            ),
        )
        assertEquals(listOf("directrix"), functionParabola.parents)
        assertSame(
            functionBoard.select("focus"),
            functionParabola.parabolaFocus,
        )

        val coordinateBoard = board("parabola-coordinate")
        val coordinateParabola = curve(
            evaluate(
                source =
                    "parabola([3.25, 0], [[0.25, 1], [0.25, 0]], " +
                        "-1.5707963267948966, 3.141592653589793) << " +
                        "id: \"coordinate\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 16, " +
                        "foci: << fixed: true >>, " +
                        "center: << id: \"center\", fixed: true >>, " +
                        "line: << id: \"directrix\", name: \"\" >> >>;",
                board = coordinateBoard,
            ),
        )
        assertEquals(emptyList(), coordinateParabola.parents)
        assertEquals(16, coordinateParabola.numberPoints)
        assertEquals(-1.5707963267948966, coordinateParabola.minX())
        assertEquals(3.141592653589793, coordinateParabola.maxX())
        assertEquals(6, coordinateBoard.objects.size)
        assertTrue(assertIs<Point>(coordinateParabola.parabolaFocus).isFixed)
        assertEquals("directrix", coordinateParabola.parabolaDirectrix?.id)
        assertTrue(assertIs<Point>(coordinateParabola.center).isFixed)
    }

    @Test
    fun parabolaCreatorFailuresAreStructuredAndAtomic() {
        val wrongParents = creatorError(
            source =
                "A = point(0, 0); B = point(1, 1); " +
                    "parabola(A, B) << doAdvancedPlot: false >>;",
            board = board("parabola-wrong-parent"),
        )
        assertEquals("parabola", wrongParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            wrongParents.error,
        )

        val duplicateBoard = board("parabola-duplicate")
        val duplicate = creatorError(
            source =
                "point(7, 5) << id: \"taken\", name: \"\" >>; " +
                    "parabola([3, 0], [[0, 1], [0, -1]]) << " +
                    "id: \"taken\", doAdvancedPlot: false, " +
                    "numberPointsHigh: 8 >>;",
            board = duplicateBoard,
        )
        assertEquals(
            ParabolaError.DuplicateElementId("taken"),
            assertIs<JessieCodeCreatorError.ParabolaFactory>(
                duplicate.error,
            ).error,
        )
        assertEquals(1, duplicateBoard.objects.size)

        val duplicateLineBoard = board("parabola-duplicate-line")
        val duplicateLine = creatorError(
            source =
                "point(7, 5) << id: \"taken\", name: \"\" >>; " +
                    "parabola([3, 0], [[0, 1], [0, -1]]) << " +
                    "doAdvancedPlot: false, numberPointsHigh: 8, " +
                    "line: << id: \"taken\" >> >>;",
            board = duplicateLineBoard,
        )
        assertEquals(
            LineError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            assertIs<JessieCodeCreatorError.LineFactory>(
                duplicateLine.error,
            ).error,
        )
        assertEquals(1, duplicateLineBoard.objects.size)
    }

    @Test
    fun ellipseConicInteropRemainsStructuredAndAtomic() {
        fun failure(
            suffix: String,
            expression: String,
        ): Pair<JessieCodeRuntimeError.CreatorFailure, Board> {
            val board = board("ellipse-conic-$suffix")
            val error = creatorError(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); " +
                        "C = point(0, 5); " +
                        "E = ellipse(F1, F2, C) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "P = point(7, 4); L = line([-8, 4], [8, 4]); " +
                        expression,
                board = board,
            )
            assertEquals(9, board.objects.size)
            return error to board
        }

        val tangent = failure("tangent", "tangent(E, P);").first
        assertEquals(
            TangentError.UnsupportedConic(0),
            assertIs<JessieCodeCreatorError.TangentFactory>(
                tangent.error,
            ).error,
        )

        val polar = failure("polar", "polar(P, E);").first
        assertEquals(
            TangentError.UnsupportedConic(1),
            assertIs<JessieCodeCreatorError.TangentFactory>(
                polar.error,
            ).error,
        )

        val polarLine = failure("polarline", "polarline(E, P);").first
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            polarLine.error,
        )

        val normal = failure("normal", "normal(P, E);").first
        assertEquals(
            NormalError.UnsupportedConic(1),
            assertIs<JessieCodeCreatorError.NormalFactory>(
                normal.error,
            ).error,
        )

        for (creator in listOf("intersection", "otherintersection")) {
            val expression =
                if (creator == "intersection") {
                    "intersection(L, E, 0);"
                } else {
                    "otherintersection(E, L, P);"
                }
            val error = failure(creator, expression).first
            assertEquals(
                if (creator == "intersection") {
                    IntersectionError.UnsupportedConic(1)
                } else {
                    IntersectionError.UnsupportedConic(0)
                },
                assertIs<JessieCodeCreatorError.IntersectionFactory>(
                    error.error,
                ).error,
            )
        }

        val polePoint = failure("polepoint", "polepoint(E, L);").first
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            polePoint.error,
        )

        val tangentTo = failure("tangentto", "tangentto(E, P, 0);").first
        assertEquals(
            TangentToError.UnsupportedConic("curve"),
            assertIs<JessieCodeCreatorError.TangentToFactory>(
                tangentTo.error,
            ).error,
        )
    }

    @Test
    fun pointReflectionCreatorsMatchOfficialMetadataDependenciesAndUpdates() {
        val board = board("point-reflections")
        evaluate(
            source =
                "source = point(-3, 1) << id: \"source\", name: \"\" >>; " +
                    "mirror = point(1, -1) << id: \"mirror\", name: \"\" >>; " +
                    "axisFirst = point(0, -4) << " +
                    "id: \"axisFirst\", name: \"\" >>; " +
                    "axisSecond = point(0, 4) << " +
                    "id: \"axisSecond\", name: \"\" >>; " +
                    "axis = line(axisFirst, axisSecond) << " +
                    "id: \"axis\", name: \"\" >>; " +
                    "frozen = reflection(source, axis) << " +
                    "id: \"frozen\", name: \"frozen-name\", " +
                    "fixed: true, needsRegularUpdate: false >>; " +
                    "dynamic = reflection(source, axis) << " +
                    "id: \"dynamic\", name: \"\" >>; " +
                    "mirrored = mirrorelement(source, mirror) << " +
                    "id: \"mirrored\", name: \"\" >>; " +
                    "alias = mirrorpoint(source, mirror) << " +
                    "id: \"alias\", name: \"\" >>;",
            board = board,
        )
        val source = assertIs<Point>(board.select("source"))
        val mirror = assertIs<Point>(board.select("mirror"))
        val axisSecond = assertIs<Point>(board.select("axisSecond"))
        val axis = assertIs<Line>(board.select("axis"))
        val frozen = assertIs<Point>(board.select("frozen"))
        val dynamic = assertIs<Point>(board.select("dynamic"))
        val mirrored = assertIs<Point>(board.select("mirrored"))
        val alias = assertIs<Point>(board.select("alias"))

        assertEquals("reflection", frozen.elType)
        assertEquals("frozen-name", frozen.name)
        assertFalse(frozen.needsRegularUpdate)
        assertTrue(dynamic.isFixed)
        assertFalse(dynamic.isDraggable)
        assertEquals(listOf("source", "axis"), dynamic.parents)
        assertSame(dynamic, axis.childElements["dynamic"])
        assertFalse("dynamic" in source.childElements)
        assertEquals("mirrorelement", mirrored.elType)
        assertEquals("mirrorpoint", alias.elType)
        assertSame(mirrored, mirror.childElements["mirrored"])
        assertSame(alias, mirror.childElements["alias"])
        assertPoint(3.0, 1.0, frozen.coords)
        assertPoint(3.0, 1.0, dynamic.coords)
        assertPoint(5.0, -3.0, mirrored.coords)
        assertPoint(5.0, -3.0, alias.coords)

        source.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 3.0),
        )
        mirror.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 0.0),
        )
        axisSecond.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 4.0),
        )
        board.update()

        assertPoint(3.0, 1.0, frozen.coords)
        assertPoint(
            5.0588235294117645,
            1.2352941176470589,
            dynamic.coords,
        )
        assertPoint(2.0, -3.0, mirrored.coords)
        assertPoint(2.0, -3.0, alias.coords)
    }

    @Test
    fun pointReflectionCreatorsRejectOfficiallyUnsupportedParentForms() {
        for ((creator, source) in listOf(
            "reflection" to
                "L = line([0, -2], [0, 2]); reflection([-2, 1], L);",
            "reflection" to
                "A = point(0, 0); B = point(1, 1); reflection(A, B);",
            "mirrorelement" to
                "M = point(1, 1); mirrorelement([-2, 1], M);",
            "mirrorelement" to
                "A = point(0, 0); mirrorelement(A, [1, 1]);",
            "mirrorpoint" to
                "M = point(1, 1); mirrorpoint([-2, 1], M);",
        )) {
            val failure = creatorError(
                source = source,
                board = board("unsupported-$creator"),
            )
            assertEquals(creator, failure.creatorName)
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                failure.error,
            )
        }
    }

    @Test
    fun circleCreatorSupportsFunctionRadiusInEitherParentOrder() {
        val board = board("function-radius-circle")
        val circle = circle(
            evaluate(
                source =
                    "A = point(0, 0); B = point(2, 0); " +
                        "c = circle(A, function () { return B.X() - 4; }) " +
                        "<< nonnegativeOnly: true >>; c;",
                board = board,
            ),
        )
        val driver = assertIs<Point>(board.select("B"))

        assertTrue(circle.nonnegativeOnly)
        assertEquals(0.0, circle.Radius())
        assertSame(circle, driver.childElements[circle.id])

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(7.0, 0.0),
        )
        board.update()
        assertEquals(3.0, circle.Radius())

        val reverseBoard = board("reverse-function-radius-circle")
        val reverse = circle(
            evaluate(
                source =
                    "A = point(0, 0); B = point(3, 0); " +
                        "circle(function () { return -B.X(); }, A);",
                board = reverseBoard,
            ),
        )
        assertEquals(3.0, reverse.Radius())
    }

    @Test
    fun circleFunctionFailuresAndTwoParentRollbackStayStructured() {
        val nonnumericBoard = board("nonnumeric-function-radius")
        val nonnumeric = creatorError(
            source =
                "A = point(0, 0); " +
                    "circle([1, 1], function () { return \"radius\"; });",
            board = nonnumericBoard,
        )
        assertIs<CircleError.NonNumericRadiusFunction>(
            assertIs<JessieCodeCreatorError.CircleFactory>(
                nonnumeric.error,
            ).error,
        )
        assertEquals(1, nonnumericBoard.objects.size)

        val functionBoard = board("function-radius-rollback")
        creatorError(
            source =
                "point(0, 0) << id: \"taken\", name: \"\" >>; " +
                    "circle([1, 1], function () { return 2; }) " +
                    "<< id: \"taken\" >>;",
            board = functionBoard,
        )
        assertEquals(1, functionBoard.objects.size)

        val twoPointBoard = board("two-point-circle-rollback")
        creatorError(
            source =
                "point(0, 0) << id: \"taken\", name: \"\" >>; " +
                    "circle([1, 1], [2, 2]) << id: \"taken\" >>;",
            board = twoPointBoard,
        )
        assertEquals(1, twoPointBoard.objects.size)

        val elementBoard = board("element-radius-circle-rollback")
        creatorError(
            source =
                "A = point(0, 0); B = point(2, 0); l = line(A, B); " +
                    "point(4, 4) << id: \"taken\", name: \"\" >>; " +
                    "circle([1, 1], l) << id: \"taken\" >>;",
            board = elementBoard,
        )
        assertEquals(4, elementBoard.objects.size)
    }

    @Test
    fun curveCreatorsSupportDataParametricAndFunctionGraphParents() {
        val data = curve(
            evaluate(
                source = "curve([-2, 0, 3], [4, 1, -2]);",
                board = board("data-curve"),
            ),
        )
        assertEquals("plot", data.curveType)
        assertEquals(3, data.numberPoints)
        assertContentEquals(
            doubleArrayOf(-2.0, 0.0, 3.0),
            data.points.map { it.usrCoords[1] }.toDoubleArray(),
        )

        val parametric = curve(
            evaluate(
                source =
                    "curve(\"2 * x\", \"x * x\", -1, 1) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 4 >>;",
                board = board("parametric-curve"),
            ),
        )
        assertEquals("functiongraph", parametric.curveType)
        assertContentEquals(
            doubleArrayOf(-2.0, -1.0, 0.0, 1.0),
            parametric.points.map { it.usrCoords[1] }.toDoubleArray(),
        )

        val functionGraph = curve(
            evaluate(
                source =
                    "functiongraph(\"x * x\", -2, 2) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 4 >>;",
                board = board("function-graph"),
            ),
        )
        assertEquals("functiongraph", functionGraph.curveType)
        assertContentEquals(
            doubleArrayOf(4.0, 1.0, 0.0, 1.0),
            functionGraph.points.map { it.usrCoords[2] }.toDoubleArray(),
        )

        val plot = curve(
            evaluate(
                source =
                    "plot(\"x + 1\", -1, 1) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 2 >>;",
                board = board("plot-alias"),
            ),
        )
        assertEquals("functiongraph", plot.curveType)
        assertContentEquals(
            doubleArrayOf(0.0, 1.0),
            plot.points.map { it.usrCoords[2] }.toDoubleArray(),
        )
    }

    @Test
    fun ticksCreatorSupportsFixedNumericAndCurveForms() {
        val board = board("ticks")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    """
                    A = point(-2, 0);
                    B = point(2, 0);
                    line = segment(A, B);
                    fixed = ticks(
                        line,
                        [-10, -1, 0, 1, 10]
                    ) <<
                        id: "fixed", name: "",
                        drawLabels: true,
                        labels: [
                            "out-left", "minus", "zero",
                            "plus", "out-right"
                        ],
                        majorHeight: 12,
                        tickEndings: [1, 0],
                        label: <<
                            offset: [8, -3],
                            fontSize: 14,
                            anchorX: "middle",
                            anchorY: "top"
                        >>
                    >>;
                    numeric = ticks(line, 2) <<
                        id: "numeric", name: "",
                        ticksDistance: 1,
                        minorTicks: 0
                    >>;
                    curve = functiongraph("x * x", -2, 2) <<
                        id: "curve", name: "",
                        doAdvancedPlot: false,
                        numberPointsHigh: 8
                    >>;
                    curveTicks = ticks(curve, [0, 1, 2, 4]) <<
                        id: "curveTicks", name: "",
                        drawLabels: true,
                        labels: ["left", "inside", "middle", "right"]
                    >>;
                    [fixed, numeric, curveTicks];
                    """.trimIndent(),
                board = board,
            ),
        ).values
        val fixed = ticks(values[0])
        val numeric = ticks(values[1])
        val curveTicks = ticks(values[2])

        assertIs<TicksSource.Fixed>(fixed.source).also { source ->
            assertContentEquals(
                doubleArrayOf(-10.0, -1.0, 0.0, 1.0, 10.0),
                source.values,
            )
        }
        assertEquals(12.0, fixed.attributes.majorHeight)
        assertContentEquals(
            doubleArrayOf(1.0, 0.0),
            fixed.attributes.tickEndings,
        )
        assertContentEquals(
            doubleArrayOf(8.0, -3.0),
            fixed.attributes.labelOffset,
        )
        assertEquals(14.0, fixed.attributes.labelFontSize)
        assertEquals("middle", fixed.attributes.labelAnchorX)
        assertEquals("top", fixed.attributes.labelAnchorY)
        assertEquals(2, fixed.parent.ticks.size)

        assertIs<TicksSource.Equidistant>(numeric.source)
        assertEquals(1.0, numeric.attributes.ticksDistance)
        assertEquals(0, numeric.attributes.minorTicks)

        assertEquals(
            listOf(-2.0, -1.0, 0.0, 2.0),
            curveTicks.curveLocations.map(TicksCurveLocation::baseX),
        )
        assertEquals(
            listOf("left", "inside", "middle", "right"),
            curveTicks.curveLocations.map(TicksCurveLocation::label),
        )
    }

    @Test
    fun axisCreatorBuildsDefaultTicksAndParsesPositioningUnits() {
        val board = board("axis")
        val axis = line(
            evaluate(
                source =
                    """
                    axis([0, 0], [1, 0]) <<
                        id: "axis",
                        position: "fixed",
                        anchor: "right",
                        anchorDist: "25px",
                        ticksAutoPos: true,
                        ticksAutoPosThreshold: "5%",
                        ticks: <<
                            id: "axisTicks",
                            ticks: [-2, 0, 3],
                            ticksDistance: undefined,
                            minorTicks: 0,
                            label: <<
                                anchorX: "middle",
                                anchorY: "top"
                            >>
                        >>
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )
        val ticks = assertIs<Ticks>(axis.defaultTicks)

        assertTrue("axis" in NativeJessieCodeCreators.names)
        assertEquals(Const.OBJECT_TYPE_AXIS, axis.type)
        assertEquals("axis", axis.elType)
        assertFalse(axis.isDraggable)
        assertFalse(axis.point1.isDraggable)
        assertFalse(axis.point2.isDraggable)
        assertEquals(Const.OBJECT_TYPE_AXISPOINT, axis.point1.type)
        assertEquals(Const.OBJECT_TYPE_AXISPOINT, axis.point2.type)
        assertEquals(false, axis.needsRegularUpdate)
        assertEquals("axisTicks", ticks.id)
        assertEquals(false, ticks.needsRegularUpdate)
        assertIs<TicksSource.Fixed>(ticks.source).also { source ->
            assertContentEquals(
                doubleArrayOf(-2.0, 0.0, 3.0),
                source.values,
            )
        }
        assertEquals(0, ticks.attributes.minorTicks)
        assertTrue(ticks.attributes.drawLabels)
        assertTrue(ticks.attributes.insertTicks)
        assertContentEquals(
            doubleArrayOf(4.0, -9.0),
            ticks.attributes.labelOffset,
        )
        assertEquals("middle", ticks.attributes.labelAnchorX)
        assertEquals("top", ticks.attributes.labelAnchorY)
        assertSame(ticks, axis.subs["ticks"])
    }

    @Test
    fun axisCreatorRollsBackGeneratedPointsWhenTicksFail() {
        val board = board("axis-rollback")
        val error = creatorError(
            source =
                """
                existing = point(4, 4) << id: "duplicate" >>;
                axis([0, 0], [1, 0]) <<
                    id: "axis",
                    ticks: << id: "duplicate" >>
                >>;
                """.trimIndent(),
            board = board,
        )

        val axisError = assertIs<JessieCodeCreatorError.AxisFactory>(
            error.error,
        ).error
        assertIs<AxisError.TicksFactory>(axisError)
        assertEquals(setOf("duplicate"), board.objects.keys)
        assertFalse(board.objects.values.any { it is Line })
        assertFalse(board.objects.values.any { it is Ticks })
    }

    @Test
    fun ticksCreatorRejectsFunctionAndMalformedAttributesAtomically() {
        val functionBoard = board("ticks-function")
        val function = creatorError(
            source =
                """
                A = point(-2, 0);
                B = point(2, 0);
                line = segment(A, B);
                ticks(line, function () { return 1; });
                """.trimIndent(),
            board = functionBoard,
        )
        assertEquals("ticks", function.creatorName)
        assertEquals(
            TicksError.FunctionArgumentsNoLongerSupported,
            assertIs<JessieCodeCreatorError.TicksFactory>(
                function.error,
            ).error,
        )
        assertFalse(functionBoard.objects.values.any { it is Ticks })

        val attributeBoard = board("ticks-attribute")
        val attribute = creatorError(
            source =
                """
                A = point(-2, 0);
                B = point(2, 0);
                line = segment(A, B);
                ticks(line) << tickEndings: [1] >>;
                """.trimIndent(),
            board = attributeBoard,
        )
        val invalid =
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                attribute.error,
            )
        assertEquals("tickendings", invalid.attribute)
        assertFalse(attributeBoard.objects.values.any { it is Ticks })
    }

    @Test
    fun stepfunctionCreatorMatchesOfficialStaticAndFunctionParents() {
        val board = board("stepfunction")
        val step = curve(
            evaluate(
                source =
                    "stepfunction([0, 1, 3, 4], [2, -1, 3, 1]) << " +
                        "id: \"step\", name: \"\" >>;",
                board = board,
            ),
        )

        assertTrue("stepfunction" in NativeJessieCodeCreators.names)
        assertEquals("curve", step.elType)
        assertEquals("plot", step.curveType)
        assertTrue(step.isStepFunction)
        assertTrue(step.parents.isEmpty())
        assertContentEquals(
            doubleArrayOf(0.0, 1.0, 1.0, 3.0, 3.0, 4.0, 4.0),
            step.points.map { it.usrCoords[1] }.toDoubleArray(),
        )
        assertContentEquals(
            doubleArrayOf(2.0, 2.0, -1.0, -1.0, 3.0, 3.0, 1.0),
            step.points.map { it.usrCoords[2] }.toDoubleArray(),
        )

        val functionParent = curve(
            evaluate(
                source =
                    "stepfunction(" +
                        "function () { return [0, 1, 2]; }, " +
                        "function () { return [3, 4, 5]; }) " +
                        "<< id: \"function-parent\", name: \"\" >>;",
                board = board("stepfunction-function"),
            ),
        )
        assertEquals("parameter", functionParent.curveType)
        assertEquals(0, functionParent.numberPoints)
        assertTrue(functionParent.parents.isEmpty())
    }

    @Test
    fun stepfunctionCreatorRetainsRuntimeArrayIdentity() {
        val board = board("mutable-stepfunction")
        val xTerm = JessieCodeRuntimeValue.ArrayValue(
            listOf(
                JessieCodeRuntimeValue.NumberValue(-1.0),
                JessieCodeRuntimeValue.NumberValue(2.0),
            ),
        )
        val yTerm = JessieCodeRuntimeValue.ArrayValue(
            listOf(
                JessieCodeRuntimeValue.NumberValue(5.0),
                JessieCodeRuntimeValue.NumberValue(7.0),
            ),
        )
        val creator = assertNotNull(
            NativeJessieCodeCreators.creator("stepfunction"),
        )
        val created = creator.create(
            board = board,
            parents = listOf(xTerm, yTerm),
            attributes = JessieCodeRuntimeValue.ObjectValue(
                mapOf(
                    "id" to JessieCodeRuntimeValue.StringValue("mutable"),
                    "name" to JessieCodeRuntimeValue.StringValue(""),
                ),
            ),
            location = JessieCodeAstLocation(1, 0, 1, 0),
        )
        val step = curve(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(created).value,
        )

        xTerm.values.clear()
        xTerm.values.addAll(
            listOf(-2.0, 0.0, 3.0).map(
                JessieCodeRuntimeValue::NumberValue,
            ),
        )
        yTerm.values.clear()
        yTerm.values.addAll(
            listOf(1.0, 4.0, -2.0).map(
                JessieCodeRuntimeValue::NumberValue,
            ),
        )
        board.update()

        assertCurveCoordinates(
            step,
            listOf(
                -2.0 to 1.0,
                0.0 to 1.0,
                0.0 to 4.0,
                3.0 to 4.0,
                3.0 to -2.0,
            ),
        )
    }

    @Test
    fun stepfunctionCreatorFailuresAreStructuredAndAtomic() {
        for (source in listOf(
            "stepfunction();",
            "stepfunction([0, 1]);",
            "stepfunction([0], [1], [2]);",
            "stepfunction([0, \"x\"], [1, 2]);",
        )) {
            val board = board("invalid-stepfunction")
            val failure = creatorError(source, board)

            assertEquals("stepfunction", failure.creatorName)
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                failure.error,
            )
            assertTrue(board.objects.isEmpty())
        }
    }

    @Test
    fun splineCreatorMatchesOfficialPointAndCoordinateArrayForms() {
        val board = board("spline")
        val spline = curve(
            evaluate(
                source =
                    "A = point(2, 0) << id: \"A\", name: \"\" >>; " +
                        "B = point(-2, 2) << id: \"B\", name: \"\" >>; " +
                        "C = point(0, -1) << id: \"C\", name: \"\" >>; " +
                        "D = point(4, 1) << id: \"D\", name: \"\" >>; " +
                        "spline(A, B, C, D) << id: \"spline\", " +
                        "name: \"\", doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>;",
                board = board,
            ),
        )

        assertTrue("spline" in NativeJessieCodeCreators.names)
        assertTrue(spline.isSpline)
        assertEquals("spline", spline.elType)
        assertEquals("functiongraph", spline.curveType)
        assertEquals(listOf("A", "B", "C", "D"), spline.parents)
        assertEquals(0.1, spline.Y(-1.0), absoluteTolerance = 1.0e-14)
        assertEquals(-0.8, spline.Y(1.0), absoluteTolerance = 1.0e-14)

        val a = assertIs<Point>(board.select("A"))
        a.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, -2.0),
        )
        board.update()
        assertEquals(
            -2.4225352112676055,
            spline.Y(1.0),
            absoluteTolerance = 1.0e-14,
        )

        val arrays = curve(
            evaluate(
                source =
                    "spline([-3, -1, 2, 5], [1, 4, -2, 2]) << " +
                        "id: \"arrays\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>;",
                board = board("spline-arrays"),
            ),
        )
        assertEquals(-3.0, arrays.minX())
        assertEquals(5.0, arrays.maxX())
        assertEquals(
            2.5085085085085086,
            arrays.Y(0.0),
            absoluteTolerance = 1.0e-14,
        )
        assertTrue(arrays.parents.isEmpty())
    }

    @Test
    fun cardinalSplineCreatorTracksPointsAndFunctionTension() {
        val board = board("cardinal-spline")
        val spline = curve(
            evaluate(
                source =
                    "P0 = point(-4, 0) << id: \"P0\", name: \"\" >>; " +
                        "P1 = point(-2, 3) << id: \"P1\", name: \"\" >>; " +
                        "P2 = point(1, -2) << id: \"P2\", name: \"\" >>; " +
                        "P3 = point(4, 2) << id: \"P3\", name: \"\" >>; " +
                        "T = point(0.35, 0) << id: \"T\", name: \"\" >>; " +
                        "cardinalspline(" +
                        "[P0, P1, P2, P3], " +
                        "function () { return T.X(); }, \"uniform\"" +
                        ") << id: \"spline\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>;",
                board = board,
            ),
        )

        assertTrue("cardinalspline" in NativeJessieCodeCreators.names)
        assertTrue(spline.isCardinalSpline)
        assertEquals("cardinalspline", spline.elType)
        assertEquals(listOf("P0", "P1", "P2", "P3"), spline.parents)
        assertEquals(-3.04375, spline.X(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(1.85, spline.Y(0.5), absoluteTolerance = 1.0e-14)

        assertIs<Point>(board.select("T")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.8, 0.0),
        )
        assertIs<Point>(board.select("P2")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, -3.0),
        )
        board.update()

        assertEquals(-3.2, spline.X(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(
            2.4000000000000004,
            spline.Y(0.5),
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun cardinalSplineCoordinateCreationAndFailuresAreAtomic() {
        val createdBoard = board("cardinal-created")
        val created = curve(
            evaluate(
                source =
                    "cardinalspline(" +
                        "[[-4, 0], [-2, 3], [1, -2], [4, 2]], " +
                        "0.5, \"uniform\"" +
                        ") << id: \"created\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>;",
                board = createdBoard,
            ),
        )
        assertEquals(5, createdBoard.numObjects)
        assertEquals(4, created.parents.size)
        assertEquals(4, created.childElements.size)

        val pointLikeBoard = board("cardinal-point-like")
        val pointLike = curve(
            evaluate(
                source =
                    "cardinalspline(" +
                        "[[-4, 0], [-2, 3], [1, -2], [4, 2]], " +
                        "0.5, \"uniform\"" +
                        ") << id: \"pointLike\", name: \"\", " +
                        "createPoints: false, doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>;",
                board = pointLikeBoard,
            ),
        )
        assertEquals(1, pointLikeBoard.numObjects)
        assertTrue(pointLike.parents.isEmpty())
        assertEquals(
            -3.0625,
            pointLike.X(0.5),
            absoluteTolerance = 1.0e-14,
        )

        for (source in listOf(
            "cardinalspline();",
            "cardinalspline([[0, 0], [1, 1]]);",
            "cardinalspline(\"not-an-array\", 0.5);",
            "cardinalspline([[0, 0]], 0.5) << " +
                "doAdvancedPlot: false, numberPointsHigh: 8 >>;",
        )) {
            val board = board("invalid-cardinal")
            val failure = creatorError(source, board)

            assertEquals("cardinalspline", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory,
            )
            assertTrue(board.objects.isEmpty())
        }
    }

    @Test
    fun riemannSumCreatorMatchesOfficialSingleAndBetweenFunctionForms() {
        val single = curve(
            evaluate(
                source =
                    """
                    f = function (x) { return x * x + 1; };
                    riemannsum(f, 3, "left", -1, 2) <<
                        id: "single", name: "", withLabel: false
                    >>;
                    """.trimIndent(),
                board = board("riemann-single"),
            ),
        )

        assertTrue("riemannsum" in NativeJessieCodeCreators.names)
        assertTrue(single.isRiemannSum)
        assertEquals("curve", single.elType)
        assertEquals("plot", single.curveType)
        assertEquals(15, single.numberPoints)
        assertEquals(5.0, single.Value())
        assertEquals(-1.0, single.minX())
        assertEquals(2.0, single.maxX())
        assertTrue(single.parents.isEmpty())

        val between = curve(
            evaluate(
                source =
                    """
                    lower = function (x) { return x * 0.5; };
                    upper = function (x) { return x * x + 1; };
                    riemannsum([lower, upper], 3, "lower", -1, 2) <<
                        id: "between", name: "", withLabel: false,
                        fillColor: "#336699", fillOpacity: 0.45
                    >>;
                    """.trimIndent(),
                board = board("riemann-between"),
            ),
        )
        assertEquals(15, between.numberPoints)
        assertEquals(2.5, between.Value(), absoluteTolerance = 1.0e-13)

        val simpson = curve(
            evaluate(
                source =
                    """
                    f = function (x) { return x * x + 1; };
                    riemannsum(f, 3, "simpson", -1, 2) <<
                        id: "simpson", name: "", withLabel: false
                    >>;
                    """.trimIndent(),
                board = board("riemann-simpson"),
            ),
        )
        assertEquals(102, simpson.numberPoints)
        assertEquals(6.0, simpson.Value(), absoluteTolerance = 1.0e-14)
    }

    @Test
    fun riemannSumCreatorTracksDynamicTermsAndFailsAtomically() {
        val dynamicBoard = board("riemann-dynamic")
        val dynamic = curve(
            evaluate(
                source =
                    """
                    A = point(3.8, -1) <<
                        id: "A", name: "", withLabel: false
                    >>;
                    f = function (x) { return x * x + 1; };
                    kind = function () {
                        return A.Y() < 0 ? "left" : "right";
                    };
                    riemannsum(
                        f, "A.X()", kind,
                        function () { return -1; },
                        function () { return 2; }
                    ) << id: "dynamic", name: "", withLabel: false >>;
                    """.trimIndent(),
                board = dynamicBoard,
            ),
        )
        val driver = assertIs<Point>(dynamicBoard.select("A"))

        assertEquals(15, dynamic.numberPoints)
        assertEquals(5.0, dynamic.Value())
        assertSame(dynamic, driver.childElements[dynamic.id])

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(5.2, 1.0),
        )
        dynamicBoard.update()

        assertEquals(25, dynamic.numberPoints)
        assertEquals(7.08, dynamic.Value(), absoluteTolerance = 1.0e-14)

        for (source in listOf(
            "riemannsum();",
            "riemannsum(function (x) { return x; });",
            "riemannsum(function (x) { return x; }, 2);",
            "riemannsum(5, 2, \"left\", -1, 1);",
            "riemannsum([], 2, \"left\", -1, 1);",
            "riemannsum(function (x) { return \"bad\"; }, 2, " +
                "\"left\", -1, 1);",
            "riemannsum(function (x) { return x; }, " +
                "function () { return \"bad\"; }, \"left\", -1, 1);",
        )) {
            val invalidBoard = board("invalid-riemann")
            val failure = creatorError(source, invalidBoard)

            assertEquals("riemannsum", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory,
            )
            assertTrue(invalidBoard.objects.isEmpty())
        }
    }

    @Test
    fun boxPlotCreatorMatchesOfficialGeometryAndDynamicDependencies() {
        val board = board("boxplot")
        val boxPlot = curve(
            evaluate(
                source =
                    """
                    driver = point(2, 3) <<
                        id: "driver", name: "", withLabel: false
                    >>;
                    boxplot(
                        [
                            "driver.Y() - 7",
                            "driver.X() - 4",
                            "0.5 * driver.X()",
                            "driver.X()",
                            "driver.Y() + 2",
                            [-6, 7]
                        ],
                        "driver.X() - 1",
                        "driver.Y() + 1"
                    ) <<
                        id: "box", name: "", withLabel: false,
                        smallWidth: 0.75,
                        outlier: << face: "plus", size: 4 >>
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )
        val driver = assertIs<Point>(board.select("driver"))

        assertTrue("boxplot" in NativeJessieCodeCreators.names)
        assertTrue(boxPlot.isBoxPlot)
        assertEquals("curve", boxPlot.elType)
        assertEquals("plot", boxPlot.curveType)
        assertEquals(32, boxPlot.numberPoints)
        assertTrue(boxPlot.parents.isEmpty())
        assertSame(boxPlot, driver.childElements[boxPlot.id])
        val initial = assertIs<CurveBoxPlotSnapshot>(
            boxPlot.boxPlotSnapshot(),
        )
        assertContentEquals(
            doubleArrayOf(-4.0, -2.0, 1.0, 2.0, 5.0),
            initial.quantiles,
        )
        assertContentEquals(doubleArrayOf(-6.0, 7.0), initial.outliers)
        assertEquals(1.0, initial.axis)
        assertEquals(4.0, initial.width)
        assertEquals("plus", initial.outlierFace)

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 5.0),
        )
        board.update()

        val updated = assertIs<CurveBoxPlotSnapshot>(
            boxPlot.boxPlotSnapshot(),
        )
        assertContentEquals(
            doubleArrayOf(-2.0, 0.0, 2.0, 4.0, 7.0),
            updated.quantiles,
        )
        assertEquals(3.0, updated.axis)
        assertEquals(6.0, updated.width)

        board.removeObject(boxPlot)
        assertFalse(driver.childElements.containsKey(boxPlot.id))
        assertFalse(board.objects.containsKey(boxPlot.id))
    }

    @Test
    fun boxPlotCreatorRejectsMalformedInputsAtomically() {
        for (source in listOf(
            "boxplot();",
            "boxplot([-2, -1, 0, 1], 0, 2);",
            "boxplot(7, 0, 2);",
            "boxplot([-2, -1, << >>, 1, 2], 0, 2);",
            "boxplot([-2, -1, 0, 1, 2], << >>, 2);",
            "boxplot([-2, -1, 0, 1, 2, [\"bad\"]], 0, 2);",
        )) {
            val invalidBoard = board("invalid-boxplot")
            val failure = creatorError(source, invalidBoard)

            assertEquals("boxplot", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory,
            )
            assertTrue(invalidBoard.objects.isEmpty())
        }

        for (source in listOf(
            "boxplot([-2, -1, 0, 1, 2], 0, 2) << dir: 7 >>;",
            "boxplot([-2, -1, 0, 1, 2], 0, 2) << " +
                "outlier: << face: 7 >> >>;",
            "boxplot([-2, -1, 0, 1, 2], 0, 2) << " +
                "outlier: << size: \"large\" >> >>;",
        )) {
            val invalidBoard = board("invalid-boxplot-attribute")
            val failure = creatorError(source, invalidBoard)

            assertEquals("boxplot", failure.creatorName)
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                failure.error,
            )
            assertTrue(invalidBoard.objects.isEmpty())
        }
    }

    @Test
    fun combCreatorMatchesOfficialDynamicGeometryAndRelations() {
        val board = board("comb")
        val comb = curve(
            evaluate(
                source =
                    """
                    first = point(-3, -1) <<
                        id: "first", name: "", withLabel: false
                    >>;
                    second = point(3, -1) <<
                        id: "second", name: "", withLabel: false
                    >>;
                    driver = point(-1, -1) <<
                        id: "driver", name: "", withLabel: false
                    >>;
                    comb(first, second) <<
                        id: "comb", name: "", withLabel: false,
                        frequency: function () {
                            return driver.X() < 0 ? 2 : 1.5;
                        },
                        width: function () {
                            return driver.Y() < 0 ? 1.5 : 0.75;
                        },
                        angle: function () {
                            return driver.X() < 0 ?
                                1.5707963267948966 :
                                1.0471975511965976;
                        },
                        reverse: function () {
                            return driver.X() > 0;
                        }
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )

        assertTrue("comb" in NativeJessieCodeCreators.names)
        assertTrue(comb.isComb)
        assertEquals("curve", comb.elType)
        assertEquals("plot", comb.curveType)
        assertEquals(9, comb.numberPoints)
        assertTrue(comb.parents.isEmpty())
        assertTrue(comb.childElements.isEmpty())
        assertEquals(-3.0, comb.dataX?.get(0))
        assertEquals(
            -3.0,
            comb.dataX?.get(1) ?: Double.NaN,
            1.0e-12,
        )
        assertEquals(
            0.5,
            comb.dataY?.get(1) ?: Double.NaN,
            1.0e-12,
        )

        assertIs<Point>(board.select("second")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 3.0),
        )
        assertIs<Point>(board.select("driver")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 1.0),
        )
        board.update()

        assertEquals(12, comb.numberPoints)
        assertEquals(1.0, comb.dataX?.get(0))
        assertEquals(3.0, comb.dataY?.get(0))
        assertEquals(
            0.16348369626219206,
            comb.dataX?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            3.224143868042013,
            comb.dataY?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -3.0184968190772716,
            comb.dataX?.get(10) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertTrue(comb.dataX?.get(11)?.isNaN() == true)
    }

    @Test
    fun combCoordinateHelpersSurviveRemovalAndFailuresAreAtomic() {
        val board = board("comb-helpers")
        val comb = curve(
            evaluate(
                source =
                    """
                    comb([-2, 0], [2, 0]) <<
                        id: "comb", name: "", withLabel: false,
                        frequency: 1.5, width: 0.5,
                        point1: <<
                            id: "helper1", name: "", visible: false,
                            withLabel: false, fixed: true
                        >>,
                        point2: <<
                            id: "helper2", name: "", visible: false,
                            withLabel: false
                        >>
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )
        val helper1 = assertIs<Point>(board.select("helper1"))
        val helper2 = assertIs<Point>(board.select("helper2"))

        assertEquals(3, board.numObjects)
        assertEquals(9, comb.numberPoints)
        assertTrue(comb.parents.isEmpty())
        assertTrue(helper1.isFixed)
        assertFalse(helper2.isFixed)
        assertTrue(helper1.childElements.isEmpty())
        assertTrue(helper2.childElements.isEmpty())

        board.removeObject(comb)
        assertEquals(setOf("helper1", "helper2"), board.objects.keys)
        assertSame(helper1, board.select("helper1"))
        assertSame(helper2, board.select("helper2"))

        for (source in listOf(
            "comb([0, 0], << >>);",
            "comb([0, 0], [2, 0]) << " +
                "frequency: function () { return 0; } >>;",
            "comb([0, 0], [2, 0]) << " +
                "point1: << id: \"same\" >>, " +
                "point2: << id: \"same\" >> >>;",
            "comb([0, 0], [2, 0]) << reverse: 1 >>;",
        )) {
            val invalidBoard = board("invalid-comb")
            val failure = creatorError(source, invalidBoard)

            assertEquals("comb", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory ||
                    failure.error is
                    JessieCodeCreatorError.InvalidAttributeType ||
                    failure.error is JessieCodeCreatorError.PointFactory,
            )
            assertTrue(invalidBoard.objects.isEmpty())
        }
    }

    @Test
    fun inequalityCreatorSupportsLineFunctionGraphAndDynamicInverse() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "inequality",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val inequality = curve(
            evaluate(
                source =
                    """
                    first = point(-3, -1) <<
                        id: "first", name: "", withLabel: false
                    >>;
                    second = point(3, 2) <<
                        id: "second", name: "", withLabel: false
                    >>;
                    driver = point(-1, 0) <<
                        id: "driver", name: "", withLabel: false
                    >>;
                    source = line(first, second) <<
                        id: "source", name: "", withLabel: false
                    >>;
                    inequality(source, driver) <<
                        id: "inequality", name: "", withLabel: false,
                        inverse: function () {
                            return driver.X() > 0;
                        }
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )

        assertTrue("inequality" in NativeJessieCodeCreators.names)
        assertTrue(inequality.isInequality)
        assertEquals("curve", inequality.elType)
        assertEquals("plot", inequality.curveType)
        assertEquals(listOf("source"), inequality.parents)
        val source = assertIs<Line>(board.select("source"))
        assertFalse(source.childElements.containsKey(inequality.id))
        assertEquals(
            5.166563145999497,
            inequality.dataX?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )

        assertIs<Point>(board.select("second")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 4.0),
        )
        assertIs<Point>(board.select("driver")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 0.0),
        )
        board.update()
        assertEquals(
            41.76249486662113,
            inequality.dataX?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )

        board.removeObject(source)
        assertSame(inequality, board.select("inequality"))

        val functionInequality = curve(
            evaluate(
                source =
                    """
                    A = point(1, 0) << id: "A", name: "" >>;
                    f = functiongraph(
                        "x == 0 ? 0 / 0 : A.X() * x * x - 2",
                        -3,
                        3
                    ) <<
                        id: "f", name: "", doAdvancedPlot: false,
                        numberPointsHigh: 8
                    >>;
                    inequality(f) <<
                        id: "functionInequality", name: "",
                        withLabel: false
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )
        assertEquals(18, functionInequality.numberPoints)
        assertTrue(functionInequality.dataX?.get(9)?.isNaN() == true)
    }

    @Test
    fun inequalityCreatorFailuresAreStructuredAndAtomic() {
        for (source in listOf(
            "inequality();",
            "A = point(0, 0); inequality(A);",
            "c = curve([-1, 1], [0, 0]); inequality(c);",
            "l = line([0, 0], [1, 1]); inequality(l) << inverse: 1 >>;",
        )) {
            val board = board("invalid-inequality")
            val failure = creatorError(source, board)

            assertEquals("inequality", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory ||
                    failure.error is
                    JessieCodeCreatorError.InvalidAttributeType,
            )
            assertTrue(
                board.objects.values
                    .filterIsInstance<Curve>()
                    .none(Curve::isInequality),
            )
        }
    }

    @Test
    fun vectorFieldCreatorMatchesOfficialDynamicGeometryAndRelations() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            id = "vectorfield",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val field = curve(
            evaluate(
                source =
                    """
                    driver = point(1, 0) <<
                        id: "driver", name: "", withLabel: false
                    >>;
                    vectorfield(
                        [
                            function (x, y) {
                                return driver.X() * y;
                            },
                            function (x, y) {
                                return -x;
                            }
                        ],
                        [
                            -2,
                            function () {
                                return driver.X() < 2 ? 1 : 2.5;
                            },
                            2
                        ],
                        [-1, 2, 1]
                    ) <<
                        id: "field", name: "", withLabel: false,
                        scale: function () {
                            return driver.X() < 2 ? 1 : 0.5;
                        },
                        arrowHead: <<
                            enabled: function () {
                                return driver.X() < 2;
                            },
                            size: 6,
                            angle: 0.5235987755982988
                        >>
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )

        assertTrue("vectorfield" in NativeJessieCodeCreators.names)
        assertTrue(field.isVectorField)
        assertEquals("vectorfield", field.elType)
        assertEquals("plot", field.curveType)
        assertEquals(42, field.numberPoints)
        assertEquals(42L, field.requestedPointCount())
        assertTrue(field.parents.isEmpty())
        val driver = assertIs<Point>(board.select("driver"))
        assertTrue(driver.childElements.isEmpty())
        assertEquals(
            -2.874823210481895,
            field.dataX?.get(3) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.9173515192762743,
            field.dataY?.get(3) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 0.0),
        )
        board.update()

        assertEquals(27, field.numberPoints)
        assertEquals(27L, field.requestedPointCount())
        assertContentEquals(
            doubleArrayOf(-2.0, -3.0, Double.NaN),
            field.dataX?.copyOfRange(0, 3),
        )
        assertContentEquals(
            doubleArrayOf(-1.0, 0.0, Double.NaN),
            field.dataY?.copyOfRange(0, 3),
        )

        board.removeObject(driver)
        assertSame(field, board.select("field"))
    }

    @Test
    fun vectorFieldCreatorSupportsOneFunctionReturningAnArray() {
        val field = curve(
            evaluate(
                source =
                    """
                    vectorfield(
                        function (x, y) {
                            return [x + y, x - y];
                        },
                        [-1, 1, 1],
                        [-1, 1, 1]
                    ) <<
                        id: "field", name: "", withLabel: false,
                        scale: 0.5,
                        arrowHead: << enabled: false >>
                    >>;
                    """.trimIndent(),
                board = board("vectorfield-array"),
            ),
        )

        assertEquals(12, field.numberPoints)
        assertContentEquals(
            doubleArrayOf(
                -1.0, -2.0, Double.NaN,
                -1.0, -1.0, Double.NaN,
                1.0, 1.0, Double.NaN,
                1.0, 2.0, Double.NaN,
            ),
            field.dataX,
        )
        assertContentEquals(
            doubleArrayOf(
                -1.0, -1.0, Double.NaN,
                1.0, 0.0, Double.NaN,
                -1.0, 0.0, Double.NaN,
                1.0, 1.0, Double.NaN,
            ),
            field.dataY,
        )
    }

    @Test
    fun vectorFieldCreatorFailuresAreStructuredAndAtomic() {
        val invalidSources = listOf(
            "vectorfield();",
            "vectorfield(1, [-1, 1, 1], [-1, 1, 1]);",
            "vectorfield([function (x, y) { return x; }, " +
                "function (x, y) { return y; }], [-1, 1], " +
                "[-1, 1, 1]);",
            "vectorfield(function (x, y) { return [x]; }, " +
                "[-1, 1, 1], [-1, 1, 1]);",
            "vectorfield([function (x, y) { return x; }, " +
                "function (x, y) { return y; }], [-1, 100, 1], " +
                "[-1, 100, 1]);",
            "vectorfield([function (x, y) { return x; }, " +
                "function (x, y) { return y; }], [-1, 1, 1], " +
                "[-1, 1, 1]) << " +
                "arrowHead: << enabled: 1 >> >>;",
        )
        for (source in invalidSources) {
            val board = board("invalid-vectorfield")
            val failure = creatorError(source, board)

            assertEquals("vectorfield", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory ||
                    failure.error is
                    JessieCodeCreatorError.InvalidAttributeType,
                "$source: ${failure.error}",
            )
            assertTrue(board.objects.isEmpty(), source)
        }
    }

    @Test
    fun slopeFieldCreatorMatchesOfficialNormalizationAndDynamicGeometry() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            id = "slopefield",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val field = curve(
            evaluate(
                source =
                    """
                    driver = point(1, 0) <<
                        id: "driver", name: "", withLabel: false
                    >>;
                    slopefield(
                        function (x, y) {
                            return driver.X() * x - y;
                        },
                        [
                            -2,
                            function () {
                                return driver.X() < 2 ? 1 : 2.5;
                            },
                            2
                        ],
                        [-1, 2, 1]
                    ) <<
                        id: "field", name: "", withLabel: false,
                        scale: function () {
                            return driver.X() < 2 ? 1 : 0.5;
                        }
                    >>;
                    """.trimIndent(),
                board = board,
            ),
        )

        assertTrue("slopefield" in NativeJessieCodeCreators.names)
        assertTrue(field.isVectorField)
        assertEquals("slopefield", field.elType)
        assertEquals(18, field.numberPoints)
        assertFalse(field.vectorFieldSnapshot()?.arrowEnabled ?: true)
        assertEquals(
            -1.2928932188134525,
            field.dataX?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -1.7071067811865475,
            field.dataY?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )

        val driver = assertIs<Point>(board.select("driver"))
        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 0.0),
        )
        board.update()

        assertEquals(27, field.numberPoints)
        assertEquals(
            -1.841886116991581,
            field.dataX?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -1.474341649025257,
            field.dataY?.get(1) ?: Double.NaN,
            absoluteTolerance = 1.0e-12,
        )
        assertTrue(driver.childElements.isEmpty())
        board.removeObject(driver)
        assertSame(field, board.select("field"))
    }

    @Test
    fun slopeFieldCreatorFailuresAreStructuredAndAtomic() {
        val invalidSources = listOf(
            "slopefield();",
            "slopefield(1, [-1, 1, 1], [-1, 1, 1]);",
            "slopefield([function (x, y) { return x; }], " +
                "[-1, 1, 1], [-1, 1, 1]);",
            "slopefield(function (x, y) { return [x, y]; }, " +
                "[-1, 1, 1], [-1, 1, 1]);",
            "slopefield(function (x, y) { return x + y; }, " +
                "[-1, 100, 1], [-1, 100, 1]);",
            "slopefield(function (x, y) { return x + y; }, " +
                "[-1, 1, 1], [-1, 1, 1]) << " +
                "arrowHead: << enabled: 1 >> >>;",
        )
        for (source in invalidSources) {
            val board = board("invalid-slopefield")
            val failure = creatorError(source, board)

            assertEquals("slopefield", failure.creatorName)
            assertTrue(
                failure.error is JessieCodeCreatorError.UnsupportedParents ||
                    failure.error is JessieCodeCreatorError.CurveFactory ||
                    failure.error is
                    JessieCodeCreatorError.InvalidAttributeType,
                "$source: ${failure.error}",
            )
            assertTrue(board.objects.isEmpty(), source)
        }
    }

    @Test
    fun derivativeCreatorMatchesOfficialFunctionAndDataBehavior() {
        val board = board("derivative")
        val derivative = curve(
            evaluate(
                source =
                    "A = point(2, 0) << id: \"A\", name: \"\" >>; " +
                        "source = functiongraph(" +
                        "\"A.X() * x * x + 3 * x - 1\", -3, 4" +
                        ") << id: \"source\", name: \"\", " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "derivative(source) << id: \"derivative\", " +
                        "name: \"\", doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>;",
                board = board,
            ),
        )

        assertTrue("derivative" in NativeJessieCodeCreators.names)
        assertTrue(derivative.isDerivative)
        assertEquals("curve", derivative.elType)
        assertEquals("parameter", derivative.curveType)
        assertEquals(listOf("source"), derivative.parents)
        val source = assertIs<Curve>(board.select("source"))
        assertFalse(source.childElements.containsKey(derivative.id))
        assertEquals(
            -5.0,
            derivative.Y(-2.0),
            absoluteTolerance = 1.0e-8,
        )

        val coefficient = assertIs<Point>(board.select("A"))
        coefficient.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 0.0),
        )
        board.update()
        assertEquals(
            -13.0,
            derivative.Y(-2.0),
            absoluteTolerance = 1.0e-8,
        )

        val dataBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "data-derivative",
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )
        val dataDerivative = curve(
            evaluate(
                source =
                    "source = curve(" +
                        "[-4, -1, 2, 5], [-2, 2, -1, 3]" +
                        ") << id: \"source\", name: \"\" >>; " +
                        "derivative(\"source\") << id: \"derivative\", " +
                        "name: \"\", doAdvancedPlot: false, " +
                        "numberPointsHigh: 8 >>;",
                board = dataBoard,
            ),
        )
        assertEquals(-9.6, dataDerivative.minX())
        assertEquals(9.6, dataDerivative.maxX())
        assertTrue(dataDerivative.Y(-1.0).isNaN())
        assertEquals(
            4.0 / 3.0,
            dataDerivative.Y(0.0),
            absoluteTolerance = 1.0e-8,
        )
    }

    @Test
    fun derivativeCreatorFailuresAreStructuredAndAtomic() {
        for (source in listOf(
            "derivative();",
            "A = point(0, 0); derivative(A);",
            "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                "sourceCurve = functiongraph(\"x * x\", -1, 1) << " +
                "id: \"sourceCurve\", name: \"\", " +
                "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                "derivative(sourceCurve, A);",
            "derivative(\"missing\");",
        )) {
            val board = board("invalid-derivative")
            val failure = creatorError(source, board)

            assertEquals(
                "derivative",
                failure.creatorName,
                "$source: ${failure.error}",
            )
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                failure.error,
            )
            assertTrue(
                board.objects.values
                    .filterIsInstance<Curve>()
                    .none(Curve::isDerivative),
            )
        }

        val board = board("duplicate-derivative")
        val failure = creatorError(
            source =
                "source = functiongraph(\"x\", -1, 1) << " +
                    "id: \"source\", name: \"\", " +
                    "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                    "derivative(source) << id: \"source\", name: \"\", " +
                    "doAdvancedPlot: false, numberPointsHigh: 8 >>;",
            board = board,
        )
        assertEquals("derivative", failure.creatorName)
        assertIs<CurveError.Registration>(
            assertIs<JessieCodeCreatorError.CurveFactory>(
                failure.error,
            ).error,
        )
        assertEquals(setOf("source"), board.objects.keys)
    }

    @Test
    fun arcSectorAndAngleCreatorsUseThreePointParents() {
        val board = board()

        val arc = arc(
            evaluate(
                source =
                    "A = point(0, 0); B = point(3, 0); " +
                        "C = point(0, 2); " +
                        "a = arc(A, B, C) << selection: \"minor\" >>; a;",
                board = board,
            ),
        )
        assertEquals(Arc.SELECTION_MINOR, arc.selection)
        assertEquals(13, arc.numberPoints)

        val sector = sector(
            evaluate(
                source =
                    "s = sector(A, B, C) << " +
                        "orientation: \"clockwise\" >>; s;",
                board = board,
            ),
        )
        assertEquals(Const.OBJECT_TYPE_SECTOR, sector.type)
        assertEquals(Arc.ORIENTATION_CLOCKWISE, sector.orientation)
        assertEquals(19, sector.numberPoints)

        val angle = sector(
            evaluate(
                source =
                    "alpha = angle(B, A, C) << radius: 2 >>; alpha;",
                board = board,
            ),
        )
        assertEquals(Const.OBJECT_TYPE_ANGLE, angle.type)
        assertEquals(2.0, angle.Radius())
        assertEquals(
            listOf(
                assertIs<Point>(board.select("B")).id,
                assertIs<Point>(board.select("A")).id,
                assertIs<Point>(board.select("C")).id,
            ),
            angle.parents,
        )

        val coordinateArc = arc(
            evaluate(
                source = "arc([0, 0], [2, 0], [0, 2]);",
                board = board,
            ),
        )
        assertEquals(3, coordinateArc.ownedPoints.size)
        assertTrue(
            coordinateArc.ownedPoints.all { point ->
                coordinateArc.childElements[point.id] === point
            },
        )
    }

    @Test
    fun directionPointArcAndSectorCreatorsRequireMatchingParentCounts() {
        val board = board("direction-arc")
        val arc = arc(
            evaluate(
                source =
                    "A = point(0, 0); B = point(2, 0); " +
                        "C = point(0, 2); D = point(0, -2); " +
                        "a = arc(A, B, C, D) << " +
                        "useDirection: true >>; a;",
                board = board,
            ),
        )
        val direction = assertIs<Point>(board.select("D"))

        assertTrue(arc.useDirection)
        assertSame(direction, arc.directionpoint)
        assertEquals(4, arc.parents.size)
        assertTrue(arc.id !in direction.childElements)
        assertTrue(arc.id !in direction.descendants)
        assertEquals(0.0, arc.points.first().usrCoords[1], 1.0e-12)
        assertEquals(2.0, arc.points.first().usrCoords[2], 1.0e-12)

        direction.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 3.0),
        )
        board.update()

        assertEquals(0.0, arc.points.first().usrCoords[1], 1.0e-12)
        assertEquals(2.0, arc.points.first().usrCoords[2], 1.0e-12)

        board.update()

        assertEquals(2.0, arc.points.first().usrCoords[1], 1.0e-12)
        assertEquals(0.0, arc.points.first().usrCoords[2], 1.0e-12)

        val coordinateBoard = board("coordinate-direction-arc")
        val coordinateArc = arc(
            evaluate(
                source =
                    "arc([0, 0], [2, 0], [0, 2], [0, -2]) << " +
                        "useDirection: true >>;",
                board = coordinateBoard,
            ),
        )
        val coordinateDirection = assertIs<Point>(coordinateArc.directionpoint)
        assertEquals(3, coordinateArc.ownedPoints.size)
        assertFalse(coordinateDirection in coordinateArc.ownedPoints)
        assertTrue(coordinateArc.id !in coordinateDirection.ancestors)

        coordinateBoard.removeObject(coordinateArc)

        assertEquals(1, coordinateBoard.objects.size)
        assertSame(
            coordinateDirection,
            coordinateBoard.elementById(coordinateDirection.id),
        )

        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            creatorError(
                source =
                    "arc([0, 0], [2, 0], [0, 2]) << " +
                        "useDirection: true >>;",
                board = board("missing-direction"),
            ).error,
        )
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            creatorError(
                source = "arc([0, 0], [2, 0], [0, 2], [0, -2]);",
                board = board("unexpected-direction"),
            ).error,
        )
        val invalidFlag = assertIs<
            JessieCodeCreatorError.InvalidAttributeType
        >(
            creatorError(
                source =
                    "arc([0, 0], [2, 0], [0, 2]) << " +
                        "useDirection: 1 >>;",
                board = board("invalid-direction-flag"),
            ).error,
        )
        assertEquals("usedirection", invalidFlag.attribute)
        assertEquals("boolean", invalidFlag.expected)
        val directionSector = sector(
            evaluate(
                source =
                    "sector([0, 0], [2, 0], [0, 2], [0, -2]) << " +
                        "useDirection: true >>;",
                board = board("sector-direction"),
            ),
        )
        val sectorDirection = assertIs<Point>(
            directionSector.directionpoint,
        )
        assertTrue(directionSector.useDirection)
        assertEquals(4, directionSector.parents.size)
        assertSame(
            directionSector,
            sectorDirection.childElements[directionSector.id],
        )
        assertEquals(
            0.0,
            directionSector.points[3].usrCoords[1],
            1.0e-12,
        )
        assertEquals(
            2.0,
            directionSector.points[3].usrCoords[2],
            1.0e-12,
        )
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            creatorError(
                source =
                    "sector([0, 0], [2, 0], [0, 2]) << " +
                        "useDirection: true >>;",
                board = board("missing-sector-direction"),
            ).error,
        )
    }

    @Test
    fun arcCompositionCreatorsPreserveAliasesOverridesAndUpdates() {
        val board = board("arc-compositions")
        evaluate(
            source =
                "A = point(-4, -1) << id: \"a\", name: \"\" >>; " +
                    "B = point(1, 4) << id: \"b\", name: \"\" >>; " +
                    "C = point(5, -2) << id: \"c\", name: \"\" >>; " +
                    "semicircle(A, B) << " +
                    "id: \"semicircle\", name: \"\", " +
                    "useDirection: true >>; " +
                    "circumcirclearc(A, B, C) << " +
                    "id: \"circumcircleArc\", name: \"\", " +
                    "useDirection: false >>; " +
                    "minorarc(A, B, C) << " +
                    "id: \"minorArc\", name: \"\", selection: \"major\" >>; " +
                    "majorarc(A, B, C) << " +
                    "id: \"majorArc\", name: \"\", selection: \"minor\" >>;",
            board = board,
        )

        val first = assertIs<Point>(board.select("a"))
        val second = assertIs<Point>(board.select("b"))
        val third = assertIs<Point>(board.select("c"))
        val semicircle = assertIs<Arc>(board.select("semicircle"))
        val circumcircleArc = assertIs<Arc>(
            board.select("circumcircleArc"),
        )
        val minorArc = assertIs<Arc>(board.select("minorArc"))
        val majorArc = assertIs<Arc>(board.select("majorArc"))

        assertTrue(
            NativeJessieCodeCreators.names.containsAll(
                setOf(
                    "semicircle",
                    "circumcirclearc",
                    "minorarc",
                    "majorarc",
                ),
            ),
        )
        assertEquals("semicircle", semicircle.elType)
        assertFalse(semicircle.useDirection)
        assertEquals(listOf("a", "b"), semicircle.parents)
        assertSame(semicircle.midpoint, semicircle.subs["midpoint"])
        assertFalse(assertIs<MidpointPoint>(semicircle.midpoint).dump)

        assertEquals("circumcirclearc", circumcircleArc.elType)
        assertTrue(circumcircleArc.useDirection)
        assertSame(second, circumcircleArc.directionpoint)
        assertEquals(listOf("a", "b", "c"), circumcircleArc.parents)
        assertSame(circumcircleArc.center, circumcircleArc.subs["center"])
        assertFalse(circumcircleArc.center.dump)
        assertTrue(circumcircleArc.id !in second.childElements)
        assertTrue(circumcircleArc.id in second.descendants)

        assertEquals("arc", minorArc.elType)
        assertEquals(Arc.SELECTION_MINOR, minorArc.selection)
        assertEquals("arc", majorArc.elType)
        assertEquals(Arc.SELECTION_MAJOR, majorArc.selection)

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, -4.0),
        )
        board.update()

        val midpoint = assertIs<MidpointPoint>(semicircle.midpoint)
        assertEquals(-3.0, midpoint.X(), absoluteTolerance = 1.0e-12)
        assertEquals(2.5, midpoint.Y(), absoluteTolerance = 1.0e-12)
        assertEquals(
            -2.108695652173913,
            circumcircleArc.center.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.847826086956522,
            circumcircleArc.center.Y(),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun sectorCompositionCreatorsPreserveAliasesOverridesAndUpdates() {
        val board = board("sector-compositions")
        evaluate(
            source =
                "A = point(-4, -1) << id: \"a\", name: \"\" >>; " +
                    "B = point(1, 4) << id: \"b\", name: \"\" >>; " +
                    "C = point(5, -2) << id: \"c\", name: \"\" >>; " +
                    "circumcirclesector(A, B, C) << " +
                    "id: \"circumcircleSector\", name: \"\", " +
                    "useDirection: false >>; " +
                    "minorsector(A, B, C) << " +
                    "id: \"minorSector\", name: \"\", " +
                    "selection: \"major\" >>; " +
                    "majorsector(A, B, C) << " +
                    "id: \"majorSector\", name: \"\", " +
                    "selection: \"minor\" >>; " +
                    "nonreflexangle(A, B, C) << " +
                    "id: \"nonreflex\", name: \"\", radius: 2, " +
                    "selection: \"major\" >>; " +
                    "reflexangle(A, B, C) << " +
                    "id: \"reflex\", name: \"\", radius: 2, " +
                    "selection: \"minor\" >>;",
            board = board,
        )

        val first = assertIs<Point>(board.select("a"))
        val second = assertIs<Point>(board.select("b"))
        val third = assertIs<Point>(board.select("c"))
        val circumcircleSector = assertIs<Sector>(
            board.select("circumcircleSector"),
        )
        val minorSector = assertIs<Sector>(board.select("minorSector"))
        val majorSector = assertIs<Sector>(board.select("majorSector"))
        val nonreflex = assertIs<Sector>(board.select("nonreflex"))
        val reflex = assertIs<Sector>(board.select("reflex"))

        assertTrue(
            NativeJessieCodeCreators.names.containsAll(
                setOf(
                    "circumcirclesector",
                    "minorsector",
                    "majorsector",
                    "nonreflexangle",
                    "reflexangle",
                ),
            ),
        )
        assertEquals("circumcirclesector", circumcircleSector.elType)
        assertTrue(circumcircleSector.useDirection)
        assertSame(second, circumcircleSector.directionpoint)
        assertEquals(
            listOf("a", "b", "c"),
            circumcircleSector.parents,
        )
        assertSame(
            circumcircleSector.center,
            circumcircleSector.subs["center"],
        )
        assertFalse(circumcircleSector.center.dump)
        assertPoint(
            x = 5.0,
            y = -2.0,
            actual = circumcircleSector.points[3],
        )
        assertPoint(
            x = -4.0,
            y = -1.0,
            actual = circumcircleSector.points[15],
        )

        assertEquals(Arc.SELECTION_MINOR, minorSector.selection)
        assertEquals(Arc.SELECTION_MAJOR, majorSector.selection)
        assertEquals("sector", minorSector.elType)
        assertEquals("sector", majorSector.elType)
        assertEquals(Arc.SELECTION_MINOR, nonreflex.selection)
        assertEquals(Arc.SELECTION_MAJOR, reflex.selection)
        assertEquals(
            1.3734007669450157,
            nonreflex.Value(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            4.90978454023457,
            reflex.Value(),
            absoluteTolerance = 1.0e-12,
        )

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-6.0, 2.0),
        )
        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        third.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, -4.0),
        )
        board.update()

        assertEquals(
            -2.108695652173913,
            circumcircleSector.center.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.847826086956522,
            circumcircleSector.center.Y(),
            absoluteTolerance = 1.0e-12,
        )
        assertPoint(
            x = 4.0,
            y = -4.0,
            actual = circumcircleSector.points[3],
        )
        assertPoint(
            x = -6.0,
            y = 2.0,
            actual = circumcircleSector.points[15],
        )
    }

    @Test
    fun sectorCompositionCreatorsPreserveCoordinateOwnershipAndRollback() {
        val circumcircleBoard = board("coordinate-circumcircle-sector")
        val circumcircleSector = sector(
            evaluate(
                source =
                    "circumcirclesector([-4, -1], [1, 4], [5, -2]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = circumcircleBoard,
            ),
        )
        val circumcenter = assertIs<CircumcenterPoint>(
            circumcircleSector.center,
        )
        assertTrue(circumcircleSector.ownedPoints.isEmpty())
        assertEquals(3, circumcenter.ownedPoints.size)

        circumcircleBoard.removeObject(circumcircleSector)

        assertEquals(4, circumcircleBoard.objects.size)
        assertSame(
            circumcenter,
            circumcircleBoard.elementById(circumcenter.id),
        )

        circumcircleBoard.removeObject(circumcenter)

        assertTrue(circumcircleBoard.objects.isEmpty())

        for (
            creatorName in listOf(
                "minorsector",
                "majorsector",
                "nonreflexangle",
                "reflexangle",
            )
        ) {
            val ownedBoard = board("coordinate-$creatorName")
            val radius = if (creatorName.endsWith("angle")) {
                " << radius: 2 >>"
            } else {
                ""
            }
            val output = sector(
                evaluate(
                    source =
                        "$creatorName([0, 0], [2, 0], [0, 2])$radius;",
                    board = ownedBoard,
                ),
            )
            assertEquals(3, output.ownedPoints.size)

            ownedBoard.removeObject(output)

            assertTrue(ownedBoard.objects.isEmpty())
        }

        val rollbackBoard = board("sector-composition-rollback")
        evaluate(
            source = "point(8, 8) << id: \"taken\", name: \"\" >>;",
            board = rollbackBoard,
        )
        val originalIds = rollbackBoard.objects.keys.toSet()
        for (
            source in listOf(
                "circumcirclesector([-4,-1],[1,4],[5,-2])",
                "minorsector([0,0],[2,0],[0,2])",
                "majorsector([0,0],[2,0],[0,2])",
                "nonreflexangle([0,0],[2,0],[0,2])",
                "reflexangle([0,0],[2,0],[0,2])",
            )
        ) {
            val failure = creatorError(
                "$source << id: \"taken\" >>;",
                rollbackBoard,
            )
            assertIs<JessieCodeCreatorError.SectorFactory>(failure.error)
            assertEquals(originalIds, rollbackBoard.objects.keys.toSet())
        }
    }

    @Test
    fun arcCompositionCreatorsPreserveCoordinateOwnershipAndRemoval() {
        val semicircleBoard = board("coordinate-semicircle")
        val semicircle = arc(
            evaluate(
                source =
                    "semicircle([-4, -1], [1, 4]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = semicircleBoard,
            ),
        )
        val midpoint = assertIs<MidpointPoint>(semicircle.midpoint)
        val semicircleSources = midpoint.ownedPoints
        assertTrue(semicircle.ownedPoints.isEmpty())
        assertEquals(2, semicircleSources.size)
        assertTrue(
            semicircleSources.all { source ->
                midpoint.childElements[source.id] === source &&
                    source.childElements[semicircle.id] === semicircle
            },
        )

        semicircleBoard.removeObject(semicircle)

        assertEquals(3, semicircleBoard.objects.size)
        assertSame(midpoint, semicircleBoard.elementById(midpoint.id))
        assertTrue(
            semicircleSources.all { source ->
                semicircleBoard.elementById(source.id) === source
            },
        )

        semicircleBoard.removeObject(midpoint)

        assertTrue(semicircleBoard.objects.isEmpty())

        val circumcircleBoard = board("coordinate-circumcircle-arc")
        val circumcircleArc = arc(
            evaluate(
                source =
                    "circumcirclearc([-4, -1], [1, 4], [5, -2]) << " +
                        "id: \"output\", name: \"\" >>;",
                board = circumcircleBoard,
            ),
        )
        val circumcenter = assertIs<CircumcenterPoint>(
            circumcircleArc.center,
        )
        val circumcircleSources = circumcenter.ownedPoints
        val direction = assertIs<Point>(circumcircleArc.directionpoint)
        assertTrue(circumcircleArc.ownedPoints.isEmpty())
        assertEquals(3, circumcircleSources.size)
        assertFalse(circumcircleArc.id in direction.childElements)

        circumcircleBoard.removeObject(circumcircleArc)

        assertEquals(4, circumcircleBoard.objects.size)
        assertSame(
            circumcenter,
            circumcircleBoard.elementById(circumcenter.id),
        )

        circumcircleBoard.removeObject(circumcenter)

        assertTrue(circumcircleBoard.objects.isEmpty())

        for (creatorName in listOf("minorarc", "majorarc")) {
            val ownedBoard = board("coordinate-$creatorName")
            val output = arc(
                evaluate(
                    source =
                        "$creatorName([0, 0], [2, 0], [0, 2]) << " +
                            "id: \"output\", name: \"\" >>;",
                    board = ownedBoard,
                ),
            )
            val sources = output.ownedPoints
            assertEquals(3, sources.size)
            assertTrue(
                sources.all { source ->
                    output.childElements[source.id] === source
                },
            )

            ownedBoard.removeObject(output)

            assertTrue(ownedBoard.objects.isEmpty())
        }
    }

    @Test
    fun arcCompositionCreatorFailuresRollBackMaterializedCoordinates() {
        val board = board("arc-composition-rollback")
        evaluate(
            source =
                "point(8, 8) << id: \"taken\", name: \"\" >>;",
            board = board,
        )
        val originalIds = board.objects.keys.toSet()
        val duplicateSources = mapOf(
            "semicircle" to
                "semicircle([-4, -1], [1, 4]) << id: \"taken\" >>;",
            "circumcirclearc" to
                "circumcirclearc([-4, -1], [1, 4], [5, -2]) " +
                "<< id: \"taken\" >>;",
            "minorarc" to
                "minorarc([0, 0], [2, 0], [0, 2]) << id: \"taken\" >>;",
            "majorarc" to
                "majorarc([0, 0], [2, 0], [0, 2]) << id: \"taken\" >>;",
        )

        for ((creatorName, source) in duplicateSources) {
            val failure = creatorError(source, board)
            assertEquals(creatorName, failure.creatorName)
            val factory = assertIs<JessieCodeCreatorError.ArcFactory>(
                failure.error,
            )
            assertEquals(
                ArcError.Registration(
                    BoardError.DuplicateElementId("taken"),
                ),
                factory.error,
            )
            assertEquals(originalIds, board.objects.keys.toSet())
        }

        val invalidSources = mapOf(
            "semicircle" to "semicircle([0, 0]);",
            "circumcirclearc" to
                "circumcirclearc([0, 0], [1, 1]);",
            "minorarc" to "minorarc([0, 0], [1, 1]);",
            "majorarc" to "majorarc([0, 0], [1, 1], true);",
        )
        for ((creatorName, source) in invalidSources) {
            val failure = creatorError(source, board)
            assertEquals(creatorName, failure.creatorName)
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                failure.error,
            )
            assertEquals(originalIds, board.objects.keys.toSet())
        }
    }

    @Test
    fun continuousCurveCreatorRequiresTranslatedSamplingMode() {
        val error = creatorError(
            source = "functiongraph(\"x\", -1, 1);",
            board = board(),
        )

        assertEquals("functiongraph", error.creatorName)
        assertEquals(
            JessieCodeCreatorError.UnsupportedAttributeValue(
                attribute = "doAdvancedPlot",
                actual = "true",
            ),
            error.error,
        )
    }

    @Test
    fun curveBooleanCreatorsMatchOfficialLifecycleAndDynamicUpdate() {
        val board = board("curve-boolean")
        curve(
            evaluate(
                source =
                    "A = point(-3, -2) << id: \"a\", name: \"\" >>; " +
                        "B = point(2, -2) << id: \"b\", name: \"\" >>; " +
                        "C = point(2, 2) << id: \"c\", name: \"\" >>; " +
                        "D = point(-3, 2) << id: \"d\", name: \"\" >>; " +
                        "P = polygon(A, B, C, D) << " +
                        "id: \"subject\", name: \"\" >>; " +
                        "Q = curve([-1, 4, 4, -1], [-3, -3, 1, 1]) " +
                        "<< id: \"clip\", name: \"\" >>; " +
                        "I = curveintersection(P, Q) << " +
                        "id: \"intersection\", name: \"\" >>; " +
                        "U = curveunion(P, Q) << " +
                        "id: \"union\", name: \"\" >>; " +
                        "DIF = curvedifference(P, Q) << " +
                        "id: \"difference\", name: \"\" >>; I;",
                board = board,
            ),
        )
        val subject = assertIs<Polygon>(board.select("subject"))
        val clip = assertIs<Curve>(board.select("clip"))
        val intersection = assertIs<Curve>(board.select("intersection"))
        val union = assertIs<Curve>(board.select("union"))
        val difference = assertIs<Curve>(board.select("difference"))

        assertTrue(
            NativeJessieCodeCreators.names.containsAll(
                setOf(
                    "curveintersection",
                    "curveunion",
                    "curvedifference",
                ),
            ),
        )
        assertTrue(intersection.isBooleanComposition)
        assertEquals("plot", intersection.curveType)
        assertTrue(intersection.parents.isEmpty())
        assertFalse(intersection.id in subject.childElements)
        assertFalse(intersection.id in clip.childElements)
        assertCurveCoordinates(
            intersection,
            listOf(
                -1.0 to -2.0,
                2.0 to -2.0,
                2.0 to 1.0,
                -1.0 to 1.0,
                -1.0 to -2.0,
            ),
        )
        assertEquals(9, union.numberPoints)
        assertEquals(7, difference.numberPoints)

        assertIs<Point>(board.select("b")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, -2.0),
        )
        assertIs<Point>(board.select("c")).setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 2.0),
        )
        board.update()
        assertCurveCoordinates(
            intersection,
            listOf(
                -1.0 to -2.0,
                0.0 to -2.0,
                0.0 to 1.0,
                -1.0 to 1.0,
                -1.0 to -2.0,
            ),
        )

        board.removeObject(intersection)
        assertSame(subject, board.select("subject"))
        assertSame(clip, board.select("clip"))
        assertSame(null, board.select("intersection"))
    }

    @Test
    fun curveBooleanCreatorFailuresAreStructuredAndAtomic() {
        val missingParents = creatorError(
            source = "curveintersection();",
            board = board(),
        )
        assertEquals("curveintersection", missingParents.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            missingParents.error,
        )

        val board = board("curve-boolean-invalid")
        val invalidParent = creatorError(
            source =
                "P = point(0, 0) << id: \"point\", name: \"\" >>; " +
                    "C = curve([-1, 1, 0], [0, 0, 2]) " +
                    "<< id: \"curve\", name: \"\" >>; " +
                    "curveunion(P, C) << id: \"output\" >>;",
            board = board,
        )
        val curveFactory = assertIs<JessieCodeCreatorError.CurveFactory>(
            invalidParent.error,
        )
        assertEquals(
            CurveError.BooleanClipping(
                ClipError.UnsupportedPathElement("point"),
            ),
            curveFactory.error,
        )
        assertEquals(setOf("point", "curve"), board.objects.keys)
    }

    @Test
    fun polygonCreatorSupportsCoordinateVerticesAndGeometryMethods() {
        val board = board("polygon")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    "P = polygon([0, 0], [4, 0], [0, 3]); " +
                        "[P.Area(), P.L(), Area(P), Perimeter(P), " +
                        "P.vertices.length, P.borders.length, " +
                        "P.BoundingBox()];",
                board = board,
            ),
        ).values
        val polygon = assertIs<Polygon>(board.select("P"))

        assertEquals(4, polygon.vertices.size)
        assertEquals(3, polygon.ownedVertices.size)
        assertEquals(3, polygon.borders.size)
        assertEquals(
            listOf(6.0, 12.0, 6.0, 12.0, 4.0, 3.0),
            values.dropLast(1).map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )
        assertEquals(
            listOf(0.0, 3.0, 4.0, 0.0),
            assertIs<JessieCodeRuntimeValue.ArrayValue>(
                values.last(),
            ).values.map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )
        assertEquals(
            polygon.vertices.dropLast(1).map(Point::id).toSet(),
            polygon.ownedVertices.map(Point::id).toSet(),
        )
    }

    @Test
    fun polygonCreatorRollsBackMaterializedPointsAfterFailure() {
        val board = board("polygon-failure")
        val error = creatorError(
            source = "polygon([0, 0], [\"x +\", 1]);",
            board = board,
        )

        assertEquals("polygon", error.creatorName)
        assertIs<JessieCodeCreatorError.PointFactory>(error.error)
        assertTrue(board.objects.isEmpty())
        assertTrue(board.objectsList.isEmpty())
    }

    @Test
    fun polygonalChainCreatorPreservesOpenGeometryAndOwnership() {
        val board = board("polygonal-chain")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    "A = point(0, 0) << id: \"A\", name: \"\" >>; " +
                        "C = point(4, 3) << id: \"C\", name: \"\" >>; " +
                        "D = point(-1, 2) << id: \"D\", name: \"\" >>; " +
                        "P = polygonalchain(A, [4, 0], C, D) << " +
                        "id: \"chain\", name: \"\" >>; " +
                        "[P.Area(), P.L(), P.vertices.length, " +
                        "P.borders.length, P.BoundingBox()];",
                board = board,
            ),
        ).values
        val chain = assertIs<Polygon>(board.select("chain"))
        val implicit = chain.ownedVertices.single()

        assertEquals("polygonalchain", chain.elType)
        assertEquals(listOf("A", implicit.id, "C", "D"), chain.vertices.map(Point::id))
        assertEquals(
            listOf(
                "A" to implicit.id,
                implicit.id to "C",
                "C" to "D",
            ),
            chain.borders.map { border ->
                border.point1.id to border.point2.id
            },
        )
        assertEquals(
            listOf(11.5, 7.0 + sqrt(26.0), 4.0, 3.0),
            values.dropLast(1).map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )
        assertEquals(
            listOf(0.0, 3.0, 4.0, 0.0),
            assertIs<JessieCodeRuntimeValue.ArrayValue>(
                values.last(),
            ).values.map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )

        board.removeObject(chain)

        assertEquals(setOf("A", "C", "D"), board.objects.keys)
        assertSame(null, board.select(implicit.id))
    }

    @Test
    fun polygonalChainCreatorSupportsBorderlessAndAtomicFailurePaths() {
        val borderlessBoard = board("polygonal-chain-borderless")
        val borderless = assertIs<Polygon>(
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                evaluate(
                source =
                    "polygonalchain([0, 0], [2, 0], [1, 2]) << " +
                        "id: \"chain\", name: \"\", withLines: false >>;",
                board = borderlessBoard,
                ),
            ).element,
        )
        assertEquals(3, borderless.vertices.size)
        assertTrue(borderless.borders.isEmpty())

        val failureBoard = board("polygonal-chain-failure")
        val error = creatorError(
            source =
                "polygonalchain([0, 0], [\"x +\", 1]) " +
                    "<< id: \"chain\" >>;",
            board = failureBoard,
        )
        assertEquals("polygonalchain", error.creatorName)
        assertIs<JessieCodeCreatorError.PointFactory>(error.error)
        assertTrue(failureBoard.objects.isEmpty())
        assertTrue(failureBoard.objectsList.isEmpty())
    }

    @Test
    fun parallelogramCreatorExposesHelperAndTracksParentUpdates() {
        val board = board("parallelogram")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    "A = point(-4, -1) << id: \"A\", name: \"\" >>; " +
                        "B = point(2, 3) << id: \"B\", name: \"\" >>; " +
                        "C = point(3, -3) << id: \"C\", name: \"\" >>; " +
                        "P = parallelogram(A, B, C) << " +
                        "id: \"P\", name: \"\", " +
                        "parallelPoint: << id: \"helper\", " +
                        "name: \"\", fixed: true >> >>; " +
                        "[P.parallelPoint, P.vertices[2], P.Area(), " +
                        "P.Perimeter()];",
                board = board,
            ),
        ).values
        val parallelogram = assertIs<Polygon>(board.select("P"))
        val helper = assertIs<ParallelPoint>(
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                values[0],
            ).element,
        )

        assertTrue("parallelogram" in NativeJessieCodeCreators.names)
        assertEquals("parallelogram", parallelogram.elType)
        assertSame(helper, parallelogram.parallelPoint)
        assertSame(
            helper,
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                values[1],
            ).element,
        )
        assertEquals("helper", helper.id)
        assertEquals("", helper.name)
        assertTrue(helper.isDraggable)
        assertFalse(helper.isFixed)
        assertPoint(9.0, 1.0, helper.coords)
        assertEquals(
            listOf(40.0, 28.982424880416993),
            values.drop(2).map {
                assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
            },
        )

        assertIs<Point>(board.select("A")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        assertIs<Point>(board.select("B")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 1.0),
        )
        assertIs<Point>(board.select("C")).setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, -4.0),
        )
        board.update()

        assertPoint(7.0, -5.0, helper.coords)
        assertEquals(33.0, parallelogram.Area())
    }

    @Test
    fun parallelogramCreatorPreservesHelperOwnershipAndRollsBackFailures() {
        val board = board("parallelogram-coordinate")
        val parallelogram = assertIs<Polygon>(
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                evaluate(
                    source =
                        "parallelogram([-4, -1], [2, 3], [3, -3]) " +
                            "<< id: \"P\", name: \"\", " +
                            "withLines: false, parallelPoint: " +
                            "<< id: \"helper\", name: \"\" >> >>;",
                    board = board,
                ),
            ).element,
        )
        val helper = assertIs<ParallelPoint>(
            parallelogram.parallelPoint,
        )
        val coordinatePoints = helper.ownedPoints

        assertTrue(parallelogram.borders.isEmpty())
        assertTrue(parallelogram.ownedVertices.isEmpty())
        assertEquals(
            coordinatePoints.toList() + helper,
            parallelogram.implicitVertices,
        )
        board.removeObject(parallelogram)
        assertSame(helper, board.elementById("helper"))
        assertTrue(
            coordinatePoints.all { point ->
                board.elementById(point.id) === point
            },
        )

        val failureBoard = board("parallelogram-failure")
        point(
            evaluate(
                source =
                    "point(8, 8) << id: \"taken\", name: \"\" >>;",
                board = failureBoard,
            ),
        )
        val duplicate = creatorError(
            source =
                "parallelogram([0, 0], [2, 0], [1, 2]) " +
                    "<< id: \"taken\", parallelPoint: " +
                    "<< id: \"temporaryHelper\" >> >>;",
            board = failureBoard,
        )
        val factory = assertIs<
            JessieCodeCreatorError.ParallelogramFactory,
            >(duplicate.error)
        assertIs<ParallelogramError.PolygonCreation>(factory.error)
        assertEquals(setOf("taken"), failureBoard.objects.keys)

        val missing = creatorError(
            source = "parallelogram([0, 0], [1, 1]);",
            board = board("parallelogram-missing"),
        )
        assertEquals("parallelogram", missing.creatorName)
        assertIs<JessieCodeCreatorError.UnsupportedParents>(missing.error)
    }

    @Test
    fun regularPolygonCreatorSupportsNumericAndExistingPointForms() {
        val generatedBoard = board("regular-polygon-generated")
        val generated = assertIs<Polygon>(
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                evaluate(
                    source =
                        "A = point(-3, -1) << id: \"A\", name: \"\" >>; " +
                            "B = point(0, 2) << id: \"B\", name: \"\" >>; " +
                            "regularpolygon(A, B, 5) << " +
                            "id: \"regular\", name: \"\", " +
                            "vertices: << name: \"\", fixed: true, " +
                            "ids: [\"C\", \"D\", \"E\"] >> >>;",
                    board = generatedBoard,
                ),
            ).element,
        )
        generatedBoard.update()

        assertEquals("regularpolygon", generated.elType)
        assertEquals(
            listOf("A", "B", "C", "D", "E", "A"),
            generated.vertices.map(Point::id),
        )
        assertEquals(
            listOf("C", "D", "E"),
            generated.implicitVertices.map(Point::id),
        )
        for (helper in generated.vertices.subList(2, 5)) {
            assertEquals(Const.OBJECT_TYPE_CAS, helper.type)
            assertTrue(helper.isDraggable)
            assertFalse(helper.isFixed)
        }
        assertPoint(
            -1.926118565760618,
            5.780220532010303,
            generated.vertices[2].coords,
        )

        val existingBoard = board("regular-polygon-existing")
        evaluate(
            source =
                "A = point(-2, -2) << id: \"A\", name: \"\" >>; " +
                    "B = point(1, -2) << id: \"B\", name: \"\" >>; " +
                    "C = point(7, 3) << id: \"C\", name: \"\" >>; " +
                    "D = point(-6, 5) << id: \"D\", name: \"\" >>; " +
                    "regularpolygon(A, B, C, D) << " +
                    "id: \"square\", name: \"\", withLines: false >>;",
            board = existingBoard,
        )
        val square = assertIs<Polygon>(existingBoard.select("square"))
        assertPoint(1.0, 1.0, square.vertices[2].coords)
        assertPoint(-2.0, 1.0, square.vertices[3].coords)
        assertTrue(square.implicitVertices.isEmpty())
    }

    @Test
    fun regularPolygonCreatorRejectsInvalidInputAndRollsBack() {
        val invalidCount = creatorError(
            source = "regularpolygon([0, 0], [1, 0], 2);",
            board = board("regular-polygon-invalid-count"),
        )
        val countFactory = assertIs<
            JessieCodeCreatorError.RegularPolygonFactory,
            >(invalidCount.error)
        assertEquals(
            RegularPolygonError.InvalidNumericParentForm(
                actualParentCount = 3,
                vertexCount = 2.0,
            ),
            countFactory.error,
        )

        val invalidParents = creatorError(
            source = "regularpolygon([0, 0], \"bad\", 4);",
            board = board("regular-polygon-invalid-parents"),
        )
        assertIs<JessieCodeCreatorError.UnsupportedParents>(
            invalidParents.error,
        )

        val duplicateBoard = board("regular-polygon-duplicate")
        point(
            evaluate(
                source =
                    "point(8, 8) << id: \"taken\", name: \"\" >>;",
                board = duplicateBoard,
            ),
        )
        val duplicate = creatorError(
            source =
                "regularpolygon([-1, 0], [1, 0], 4) << " +
                    "id: \"unused\", vertices: << name: \"\", " +
                    "ids: [\"temporary\", \"taken\"] >> >>;",
            board = duplicateBoard,
        )
        val duplicateFactory = assertIs<
            JessieCodeCreatorError.RegularPolygonFactory,
            >(duplicate.error)
        assertIs<RegularPolygonError.PointCreation>(
            duplicateFactory.error,
        )
        assertEquals(setOf("taken"), duplicateBoard.objects.keys)
        assertEquals(1, duplicateBoard.objectsList.size)
    }

    @Test
    fun textCreatorSupportsConstrainedCoordinatesAndStaticContent() {
        val board = board("text")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    "A = point(2, 3); " +
                        "T = text(\"A.X() + 1\", 2, \"Hello\"); " +
                        "N = text(0, -1, PI) << " +
                        "formatNumber: true, digits: 2 >>; " +
                        "[T.X(), T.Y(), T.setText(\"After\"), N];",
                board = board,
            ),
        ).values
        val text = assertIs<Text>(board.select("T"))
        val numberText = assertIs<Text>(board.select("N"))

        assertEquals(
            3.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(values[0]).value,
        )
        assertEquals(
            2.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(values[1]).value,
        )
        assertSame(
            text,
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                values[2],
            ).element,
        )
        assertEquals("After", text.plaintext)
        assertEquals("3.14", numberText.plaintext)
        assertFalse(text.isDraggable)
        assertTrue(text.isConstrained)
    }

    @Test
    fun textCreatorCompilesDynamicValueTags() {
        val board = board("dynamicText")
        evaluate(
            source =
                "A = point(2, 3); " +
                    "T = text(0, 0, " +
                    "\"x=<value>X(A)</value>\") << digits: 1 >>;",
            board = board,
        )
        val point = assertIs<Point>(board.select("A"))
        val text = assertIs<Text>(board.select("T"))

        assertEquals("x=2.0", text.plaintext)
        assertSame(text, point.childElements[text.id])

        point.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-1.25, 4.0),
        )
        board.update()

        assertEquals("x=-1.3", text.plaintext)
    }

    @Test
    fun creatorUsesTheBoardSelectedByUse() {
        val first = board("first")
        val second = board("second")

        val created = point(
            evaluate(
                source = "use secondcontainer; A = point(7, 8); A;",
                board = first,
                boardsByContainer = mapOf("secondcontainer" to second),
            ),
        )

        assertSame(second, created.board)
        assertTrue(first.objects.isEmpty())
        assertSame(created, second.select("A"))
    }

    @Test
    fun defaultElementRuntimeExposesCreatedGeometryMethods() {
        val value = evaluate(
            source = "A = point(2, 3); A.X() + A.Y();",
            board = board(),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(5.0),
            value,
        )
    }

    @Test
    fun intersectionCreatorsTrackDynamicIndexAndExcludedPoint() {
        val board = board("intersections")

        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    """
                    O = point(0, 0) << id: "center", name: "" >>;
                    C = circle(O, 3) << id: "circle", name: "" >>;
                    A = point(-6, 0) << id: "a", name: "" >>;
                    B = point(6, 0) << id: "b", name: "" >>;
                    L = line(A, B) << id: "line", name: "" >>;
                    S = point(0, 0) << id: "selector", name: "" >>;
                    I = intersection(
                        C,
                        L,
                        function () { return S.X(); }
                    ) << id: "first", name: "", withLabel: false >>;
                    J = otherintersection(C, L, I) <<
                        id: "other", name: "", withLabel: false
                    >>;
                    [I, J];
                    """.trimIndent(),
                board = board,
            ),
        ).values
        val first = assertIs<IntersectionPoint>(
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                values[0],
            ).element,
        )
        val other = assertIs<OtherIntersectionPoint>(
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                values[1],
            ).element,
        )
        val selector = assertIs<Point>(board.select("selector"))

        assertPoint(3.0, 0.0, first.coords)
        assertPoint(-3.0, 0.0, other.coords)
        assertEquals(listOf("circle", "line"), first.parents)
        assertEquals(
            listOf("circle", "line", "first"),
            other.parents,
        )
        assertFalse(first.id in selector.childElements)

        selector.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 0.0),
        )
        board.update()

        assertPoint(-3.0, 0.0, first.coords)
        assertPoint(3.0, 0.0, other.coords)
        assertEquals(
            doubleArrayOf(1.0, 0.0).toList(),
            assertIs<GMResult.Ok<DoubleArray>>(
                first.currentIntersectionNumbers(),
            ).value.toList(),
        )
    }

    @Test
    fun intersectionCreatorsSupportCurveArcSectorAndPolygonParents() {
        val board = board("path-intersections")

        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    """
                    A = point(-7, 0);
                    B = point(7, 0);
                    L = line(A, B);
                    Z = curve(
                        [-4, -2, 0, 2, 4],
                        [3, -1, 3, -1, 3]
                    );
                    C0 = intersection(Z, L, 0);
                    C3 = intersection(L, Z, 3);
                    CO = otherintersection(Z, L, C0);
                    O = point(0, 0);
                    R = point(3, 0);
                    T = point(0, 3);
                    AC = arc(O, R, T);
                    SC = sector(O, R, T);
                    HC = curve([-7, 7], [1, 1]);
                    AI = intersection(AC, HC, 0);
                    SI = intersection(SC, HC, 0);
                    P = polygon(
                        point(-2, -2),
                        point(2, -2),
                        point(2, 2),
                        point(-2, 2)
                    );
                    PI = intersection(P, L, 1);
                    [C0, C3, CO, AI, SI, PI];
                    """.trimIndent(),
                board = board,
            ),
        ).values

        val expected = listOf(
            -2.5 to 0.0,
            2.5 to 0.0,
            -1.5 to 0.0,
            2.8283770326345152 to 1.0,
            2.8283770326345152 to 1.0,
            -2.0 to 0.0,
        )
        for ((index, coordinates) in expected.withIndex()) {
            val point = assertIs<Point>(
                assertIs<JessieCodeRuntimeValue.ElementReference>(
                    values[index],
                ).element,
            )
            assertPoint(
                x = coordinates.first,
                y = coordinates.second,
                actual = point.coords,
            )
        }
    }

    @Test
    fun polygonPathIntersectionCreatorsMatchOrderingAndRecompute() {
        val board = board("polygon-path-intersections")
        val values = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            evaluate(
                source =
                    """
                    A = point(-2, -2);
                    B = point(2, -2);
                    C = point(2, 2) << id: "C", name: "" >>;
                    D = point(-2, 2);
                    P = polygon(A, B, C, D);
                    O = point(0, 0);
                    circle = circle(O, 2.5);
                    diamond = polygon(
                        point(0, -3),
                        point(3, 0),
                        point(0, 3),
                        point(-3, 0)
                    );
                    PC = intersection(P, circle, 2);
                    CP = intersection(circle, P, 0);
                    PP = intersection(P, diamond, 0);
                    [PC, CP, PP];
                    """.trimIndent(),
                board = board,
            ),
        ).values
        val polygonCircle = point(values[0])
        val circlePolygon = point(values[1])
        val polygonDiamond = point(values[2])

        assertPoint(2.0, -1.499885402306805, polygonCircle.coords)
        assertPoint(2.0, 1.4998854023067922, circlePolygon.coords)
        assertPoint(-1.0, -2.0, polygonDiamond.coords)

        val movable = assertIs<Point>(board.select("C"))
        movable.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, 2.0),
        )
        board.update()

        assertPoint(
            2.2058845193854877,
            -1.176461922458049,
            polygonCircle.coords,
        )
    }

    @Test
    fun intersectionFunctionReturningNaNSelectsTheSecondBranch() {
        val board = board("intersection-nan-index")

        val value = assertIs<JessieCodeRuntimeValue.ElementReference>(
            evaluate(
                source =
                    """
                    O = point(0, 0);
                    C = circle(O, 3);
                    A = point(-6, 0);
                    B = point(6, 0);
                    L = line(A, B);
                    intersection(
                        C,
                        L,
                        function () { return 0 / 0; }
                    );
                    """.trimIndent(),
                board = board,
            ),
        )
        val output = assertIs<IntersectionPoint>(value.element)

        assertPoint(-3.0, 0.0, output.coords)
        val numbers = assertIs<GMResult.Ok<DoubleArray>>(
            output.currentIntersectionNumbers(),
        ).value
        assertTrue(numbers[0].isNaN())
        assertEquals(0.0, numbers[1])
    }

    @Test
    fun intersectionCreatorFailuresRemainStructured() {
        val twoLineBoard = board("two-line-other")
        val twoLines = creatorError(
            source =
                """
                A = point(-1, 0);
                B = point(1, 0);
                C = point(0, -1);
                D = point(0, 1);
                l1 = line(A, B);
                l2 = line(C, D);
                I = intersection(l1, l2);
                otherintersection(l1, l2, I);
                """.trimIndent(),
            board = twoLineBoard,
        )
        assertEquals("otherintersection", twoLines.creatorName)
        assertIs<JessieCodeCreatorError.IntersectionFactory>(
            twoLines.error,
        )

        val invalidPrecision = creatorError(
            source =
                """
                O = point(0, 0);
                C = circle(O, 2);
                A = point(-3, 0);
                B = point(3, 0);
                L = line(A, B);
                I = intersection(C, L);
                otherintersection(C, L, I) << precision: "near" >>;
                """.trimIndent(),
            board = board("invalid-precision"),
        )
        val invalidAttribute =
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidPrecision.error,
            )
        assertEquals("precision", invalidAttribute.attribute)
        assertEquals("number", invalidAttribute.expected)

        val fractional = creatorError(
            source =
                """
                P = polygon(
                    point(-2, -2),
                    point(2, -2),
                    point(2, 2),
                    point(-2, 2)
                );
                C = circle(point(0, 0), 2.5);
                intersection(P, C, 0.5);
                """.trimIndent(),
            board = board("fractional-path-index"),
        )
        val factory = assertIs<
            JessieCodeCreatorError.IntersectionFactory,
            >(fractional.error)
        val computation = assertIs<IntersectionError.ClipComputation>(
            factory.error,
        )
        assertEquals(
            ClipError.InvalidIntersectionIndex(
                index = 0.5,
                intersectionCount = 8,
            ),
            computation.error,
        )
    }

    @Test
    fun creatorFailuresRemainStructuredAndDoNotThrow() {
        val missingBoard = creatorError(
            source = "point(1, 2);",
            board = null,
        )
        assertEquals("point", missingBoard.creatorName)
        assertIs<JessieCodeCreatorError.BoardUnavailable>(
            missingBoard.error,
        )

        val unsupported = creatorError(
            source = "line(1, 2);",
            board = board(),
        )
        assertEquals(
            listOf("number", "number"),
            assertIs<JessieCodeCreatorError.UnsupportedParents>(
                unsupported.error,
            ).parentTypes,
        )

        val invalidAttribute = creatorError(
            source = "point(1, 2) << id: 7 >>;",
            board = board(),
        )
        val attributeError =
            assertIs<JessieCodeCreatorError.InvalidAttributeType>(
                invalidAttribute.error,
            )
        assertEquals("id", attributeError.attribute)
        assertEquals("string", attributeError.expected)
        assertEquals("number", attributeError.actual)

        val duplicateBoard = board("duplicate")
        point(
            evaluate(
                source = "point(1, 2) << id: \"fixed\" >>;",
                board = duplicateBoard,
            ),
        )
        val duplicate = creatorError(
            source = "point(3, 4) << id: \"fixed\" >>;",
            board = duplicateBoard,
        )
        val pointError = assertIs<JessieCodeCreatorError.PointFactory>(
            duplicate.error,
        )
        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("fixed"),
            ),
            pointError.error,
        )
        assertEquals(1, duplicateBoard.numObjects)
    }

    @Test
    fun customCreatorOverridesTheNativeRegistry() {
        val marker = JessieCodeRuntimeValue.StringValue("custom")
        var attributes: JessieCodeRuntimeValue.ObjectValue? = null
        val result = evaluate(
            source = "A = point(1, 2); A;",
            board = board(),
            creators = mapOf(
                "point" to JessieCodeCreator { _, _, value, _ ->
                    attributes = value
                    GMResult.Ok(marker)
                },
            ),
        )

        assertSame(marker, result)
        assertEquals(
            JessieCodeRuntimeValue.StringValue("A"),
            attributes?.properties?.get("name"),
        )
    }

    private fun evaluate(
        source: String,
        board: Board?,
        boardsByContainer: Map<String, Board> = emptyMap(),
        creators: Map<String, JessieCodeCreator> = emptyMap(),
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val result = JessieCodeEvaluator().evaluate(
            ast,
            JessieCodeRuntimeEnvironment(
                creators = creators,
                board = board,
                boardsByContainer = boardsByContainer,
            ),
        )
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            result,
            "Evaluation failed: $result",
        ).value
    }

    private fun creatorError(
        source: String,
        board: Board?,
    ): JessieCodeRuntimeError.CreatorFailure {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val error = assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).error
        return assertIs(error)
    }

    private fun point(value: JessieCodeRuntimeValue): Point =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun line(value: JessieCodeRuntimeValue): Line =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun circle(value: JessieCodeRuntimeValue): Circle =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun curve(value: JessieCodeRuntimeValue): Curve =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun ticks(value: JessieCodeRuntimeValue): Ticks =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun arc(value: JessieCodeRuntimeValue): Arc =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun sector(value: JessieCodeRuntimeValue): Sector =
        assertIs<JessieCodeRuntimeValue.ElementReference>(value)
            .element.let(::assertIs)

    private fun assertPoint(
        x: Double,
        y: Double,
        actual: Coords,
    ) {
        assertEquals(x, actual.usrCoords[1], absoluteTolerance = 1.0e-10)
        assertEquals(y, actual.usrCoords[2], absoluteTolerance = 1.0e-10)
    }

    private fun assertCurveCoordinates(
        curve: Curve,
        expected: List<Pair<Double, Double>>,
    ) {
        assertEquals(expected.size, curve.numberPoints)
        assertEquals(expected.size, curve.points.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index].first,
                curve.points[index].usrCoords[1],
                absoluteTolerance = 1.0e-10,
            )
            assertEquals(
                expected[index].second,
                curve.points[index].usrCoords[2],
                absoluteTolerance = 1.0e-10,
            )
        }
    }

    private fun board(id: String = "board"): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )
}
