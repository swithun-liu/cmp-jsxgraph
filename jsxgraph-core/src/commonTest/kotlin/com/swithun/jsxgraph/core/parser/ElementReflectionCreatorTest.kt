package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ElementReflectionCreatorTest {
    @Test
    fun nativeCreatorsDispatchAllOfficialElementClasses() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            canvasWidth = 640.0,
            canvasHeight = 480.0,
            id = "board",
        )
        evaluate(
            source =
                """
                axisA = point(0, -5) <<
                    id: "axisA", name: "", withLabel: false
                >>;
                axisB = point(0, 5) <<
                    id: "axisB", name: "", withLabel: false
                >>;
                axis = line(axisA, axisB) <<
                    id: "axis", name: "", withLabel: false
                >>;
                mirror = point(1, -1) <<
                    id: "mirror", name: "", withLabel: false
                >>;
                sourcePoint = point(-2, 1) <<
                    id: "sourcePoint", name: "", withLabel: false
                >>;
                sourceLine = line([-3, -2], [-1, 1]) <<
                    id: "sourceLine", name: "", withLabel: false
                >>;
                sourceCurve = curve([-4, -3, -2], [-1, 2, 0]) <<
                    id: "sourceCurve", name: "", withLabel: false
                >>;
                sourcePolygon = polygon(
                    [-5, -3],
                    [-3, -4],
                    [-2, -2]
                ) <<
                    id: "sourcePolygon", name: "", withLabel: false
                >>;
                sourceCircle = circle([-3, 2], [-2, 2]) <<
                    id: "sourceCircle", name: "", withLabel: false
                >>;
                sourceArc = arc([-4, 1], [-3, 1], [-4, 2]) <<
                    id: "sourceArc", name: "", withLabel: false
                >>;
                sourceSector = sector(
                    [-4, -1],
                    [-3, -1],
                    [-4, -2]
                ) <<
                    id: "sourceSector", name: "", withLabel: false
                >>;
                reflection(sourcePoint, axis) <<
                    id: "reflectedPoint", name: "", withLabel: false
                >>;
                reflection(sourceLine, axis) <<
                    id: "reflectedLine", name: "", fixed: false,
                    withLabel: false,
                    point1: <<
                        id: "reflectedLinePoint1",
                        name: "",
                        fixed: true
                    >>,
                    point2: <<
                        id: "reflectedLinePoint2",
                        name: "",
                        fixed: true
                    >>
                >>;
                mirrorelement(sourceCurve, mirror) <<
                    id: "mirroredCurve", name: "", withLabel: false
                >>;
                reflection(sourcePolygon, axis) <<
                    id: "reflectedPolygon", name: "",
                    withLabel: false,
                    vertices: << name: "", fixed: true >>
                >>;
                mirrorelement(sourceCircle, mirror) <<
                    id: "mirroredCircle", name: "", withLabel: false,
                    center: <<
                        id: "mirroredCircleCenter",
                        name: "",
                        fixed: true
                    >>
                >>;
                reflection(sourceCircle, axis) <<
                    id: "projectiveCircle", name: "",
                    withLabel: false, type: "projective",
                    center: <<
                        id: "projectiveCircleCenter",
                        name: ""
                    >>
                >>;
                reflection(sourceArc, axis) <<
                    id: "reflectedArc", name: "", withLabel: false
                >>;
                mirrorelement(sourceSector, mirror) <<
                    id: "mirroredSector", name: "", withLabel: false
                >>;
                """.trimIndent(),
            board = board,
        )
        board.update()

        assertIs<Point>(board.select("reflectedPoint"))
        val line = assertIs<Line>(board.select("reflectedLine"))
        assertEquals("reflection", line.elType)
        assertEquals(false, line.evalVisProp("fixed"))
        assertEquals("reflectedLinePoint1", line.point1.id)
        assertEquals("reflectedLinePoint2", line.point2.id)
        assertTrue(line.point1.isFixed)
        assertTrue(line.point2.isFixed)

        val curve = assertIs<Curve>(board.select("mirroredCurve"))
        assertEquals("mirrorelement", curve.elType)
        assertEquals(
            listOf("sourceCurve", "mirror"),
            curve.parents,
        )
        val polygon = assertIs<Polygon>(board.select("reflectedPolygon"))
        assertEquals("reflection", polygon.elType)
        assertTrue(
            polygon.vertices.dropLast(1).all(Point::isFixed),
        )
        val circle = assertIs<Circle>(board.select("mirroredCircle"))
        assertEquals("mirrorelement", circle.elType)
        assertEquals("mirroredCircleCenter", circle.center.id)
        assertTrue(circle.center.isFixed)
        val projectiveCircle =
            assertIs<Curve>(board.select("projectiveCircle"))
        assertEquals("reflection", projectiveCircle.elType)
        assertEquals(Const.OBJECT_TYPE_CONIC, projectiveCircle.type)
        assertEquals(
            "projectiveCircleCenter",
            projectiveCircle.subs["center"]?.id,
        )

        assertEquals(
            "reflection",
            assertIs<Curve>(board.select("reflectedArc")).elType,
        )
        assertEquals(
            "mirrorelement",
            assertIs<Curve>(board.select("mirroredSector")).elType,
        )
    }

    private fun evaluate(
        source: String,
        board: Board,
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).value
    }
}
