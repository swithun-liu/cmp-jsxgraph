/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsxGraphJessieCodeTest {
    @Test
    fun sourceCreatesStyledPortableSceneWithoutJavaScriptRuntime() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                source =
                    """
                    use graph;
                    A = point(1, 2) <<
                        id: "A", name: "", withLabel: false,
                        size: 4, fillColor: "#6f7780"
                    >>;
                    B = point(3, 1) <<
                        id: "B", name: "", withLabel: false
                    >>;
                    l = segment(A, B) <<
                        id: "line", name: "", withLabel: false,
                        strokeWidth: 2.5
                    >>;
                    c = circle(A, 2.5) <<
                        id: "circle", name: "", withLabel: false,
                        strokeColor: "#16877a"
                    >>;
                    """.trimIndent(),
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "graph",
                    boundingBox =
                        JsxGraphBoundingBox(-6.0, 5.0, 6.0, -5.0),
                    axis = true,
                    grid = true,
                    keepAspectRatio = true,
                ),
            ),
        )

        assertEquals(
            JsxGraphBoundingBox(-6.0, 5.0, 6.0, -5.0),
            scene.boundingBox,
        )
        assertTrue(scene.axis)
        assertTrue(scene.grid)
        assertTrue(scene.keepAspectRatio)
        assertEquals(4, scene.elements.size)

        val point = assertIs<JsxGraphSceneElement.Point>(scene.elements[0])
        assertEquals("A", point.id)
        assertEquals(JsxGraphPoint2D(1.0, 2.0), point.coordinates)
        assertEquals(4.0, point.size)
        assertEquals(JsxGraphColor(111, 119, 128), point.style.fillColor)

        val line = assertIs<JsxGraphSceneElement.Line>(scene.elements[2])
        assertEquals(false, line.straightFirst)
        assertEquals(false, line.straightLast)
        assertEquals(2.5, line.style.strokeWidth)

        val circle = assertIs<JsxGraphSceneElement.Circle>(scene.elements[3])
        assertEquals(2.5, circle.radius)
        assertEquals(JsxGraphColor(22, 135, 122), circle.style.strokeColor)
    }

    @Test
    fun stepfunctionRetainsSourceArraysAcrossSessionExecutions() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                xs = [-1, 2];
                ys = [5, 7];
                stepfunction(xs, ys) <<
                    id: "step", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(-1.0, 5.0),
                JsxGraphPoint2D(2.0, 5.0),
                JsxGraphPoint2D(2.0, 7.0),
            ),
            sceneCurve(initial, "step").points,
        )

        val updated = scene(
            session.execute(
                """
                xs[0] = -2;
                xs[1] = 0;
                xs[2] = 3;
                ys[0] = 1;
                ys[1] = 4;
                ys[2] = -2;
                """.trimIndent(),
            ),
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(-2.0, 1.0),
                JsxGraphPoint2D(0.0, 1.0),
                JsxGraphPoint2D(0.0, 4.0),
                JsxGraphPoint2D(3.0, 4.0),
                JsxGraphPoint2D(3.0, -2.0),
            ),
            sceneCurve(updated, "step").points,
        )
    }

    @Test
    fun derivativeRendersAndTracksFunctionUpdatesThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(2, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                source = functiongraph(
                    "A.X() * x * x + 3 * x - 1", -3, 4
                ) <<
                    id: "source", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 8
                >>;
                derivative(source) <<
                    id: "derivative", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 8,
                    strokeColor: "#D55E00", strokeWidth: 3
                >>;
                """.trimIndent(),
            ),
        )
        val initialDerivative = sceneCurve(initial, "derivative")

        assertEquals(3, initial.elements.size)
        assertEquals(8, initialDerivative.points.size)
        assertEquals(
            JsxGraphColor(213, 94, 0),
            initialDerivative.style.strokeColor,
        )
        assertEquals(3.0, initialDerivative.style.strokeWidth)
        val initialFirst = assertIs<JsxGraphPoint2D>(
            initialDerivative.points.first(),
        )
        val initialMiddle = assertIs<JsxGraphPoint2D>(
            initialDerivative.points[4],
        )
        assertEquals(-3.0, initialFirst.x)
        assertEquals(
            -9.0,
            initialFirst.y,
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            5.0,
            initialMiddle.y,
            absoluteTolerance = 1.0e-8,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(4.0, 0.0),
            ),
        ).value
        val movedDerivative = sceneCurve(moved, "derivative")
        val movedFirst = assertIs<JsxGraphPoint2D>(
            movedDerivative.points.first(),
        )
        val movedMiddle = assertIs<JsxGraphPoint2D>(
            movedDerivative.points[4],
        )
        assertEquals(
            -21.0,
            movedFirst.y,
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            7.0,
            movedMiddle.y,
            absoluteTolerance = 1.0e-8,
        )
    }

    @Test
    fun splineCreatorsRenderAndUpdateThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "splines",
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                use splines;
                A = point(2, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(-2, 2) <<
                    id: "B", name: "", withLabel: false
                >>;
                C = point(0, -1) <<
                    id: "C", name: "", withLabel: false
                >>;
                D = point(4, 1) <<
                    id: "D", name: "", withLabel: false
                >>;
                natural = spline(A, B, C, D) <<
                    id: "natural", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 8
                >>;
                T = point(0.35, 0) <<
                    id: "T", name: "", withLabel: false
                >>;
                cardinal = cardinalspline(
                    [B, C, A, D],
                    function () { return T.X(); },
                    "uniform"
                ) <<
                    id: "cardinal", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 8
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            -0.8,
            assertIs<JsxGraphPoint2D>(
                sceneCurve(initial, "natural").points[4],
            ).y,
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            1.0,
            assertIs<JsxGraphPoint2D>(
                sceneCurve(initial, "cardinal").points[4],
            ).x,
            absoluteTolerance = 1.0e-14,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(3.0, -2.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("T", JsxGraphPoint2D(0.8, 0.0)),
        ).value

        assertEquals(
            -2.4225352112676055,
            assertIs<JsxGraphPoint2D>(
                sceneCurve(moved, "natural").points[4],
            ).y,
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            1.6,
            assertIs<JsxGraphPoint2D>(
                sceneCurve(moved, "cardinal").points[4],
            ).x,
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun riemannSumRendersAndUpdatesThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "riemann",
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                use riemann;
                A = point(3.8, -1) <<
                    id: "A", name: "", withLabel: false
                >>;
                f = function (x) { return x * x + 1; };
                kind = function () {
                    return A.Y() < 0 ? "left" : "right";
                };
                sum = riemannsum(
                    f, "A.X()", kind,
                    function () { return -1; },
                    function () { return 2; }
                ) << id: "sum", name: "", withLabel: false >>;
                total = point(
                    function () { return sum.Value(); },
                    -5
                ) << id: "total", name: "", withLabel: false >>;
                """.trimIndent(),
            ),
        )

        val initialCurve = sceneCurve(initial, "sum")
        assertEquals(15, initialCurve.points.size)
        assertEquals(JsxGraphColor(240, 228, 66), initialCurve.style.fillColor)
        assertEquals(0.3, initialCurve.style.fillOpacity)
        assertEquals(
            5.0,
            assertIs<JsxGraphSceneElement.Point>(
                initial.elements.single { it.id == "total" },
            ).coordinates.x,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(5.2, 1.0)),
        ).value
        assertEquals(25, sceneCurve(moved, "sum").points.size)
        assertEquals(
            7.08,
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements.single { it.id == "total" },
            ).coordinates.x,
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun boxPlotRendersAndUpdatesThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "boxplot",
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 8.0, 8.0, -8.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                use boxplot;
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
            ),
        )

        val initialCurve = sceneCurve(initial, "box")
        assertEquals(JsxGraphColor(0, 114, 178), initialCurve.style.fillColor)
        assertEquals(0.2, initialCurve.style.fillOpacity)
        assertEquals(32, initialCurve.points.size)
        val initialBoxPlot = assertIs<JsxGraphBoxPlot>(
            initialCurve.boxPlot,
        )
        assertEquals(listOf(-4.0, -2.0, 1.0, 2.0, 5.0), initialBoxPlot.quantiles)
        assertEquals(1.0, initialBoxPlot.axis)
        assertEquals(4.0, initialBoxPlot.width)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(4.0, 5.0),
            ),
        ).value
        val movedBoxPlot = assertIs<JsxGraphBoxPlot>(
            sceneCurve(moved, "box").boxPlot,
        )
        assertEquals(listOf(-2.0, 0.0, 2.0, 4.0, 7.0), movedBoxPlot.quantiles)
        assertEquals(3.0, movedBoxPlot.axis)
        assertEquals(6.0, movedBoxPlot.width)
    }

    @Test
    fun polygonalChainRemainsOpenAcrossPointInteraction() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-5, 1) <<
                    id: "A", name: "", withLabel: false, fixed: true
                >>;
                B = point(-2, 4) <<
                    id: "B", name: "", withLabel: false, fixed: true
                >>;
                C = point(1, 2) <<
                    id: "C", name: "", withLabel: false, fixed: true
                >>;
                D = point(4, -3) <<
                    id: "D", name: "", withLabel: false, fixed: false
                >>;
                polygonalchain(A, B, C, D) <<
                    id: "chain", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        val chain = assertIs<JsxGraphSceneElement.Polygon>(
            initial.elements.single { it.id == "chain" },
        )
        assertFalse(chain.isClosed)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-5.0, 1.0),
                JsxGraphPoint2D(-2.0, 4.0),
                JsxGraphPoint2D(1.0, 2.0),
                JsxGraphPoint2D(4.0, -3.0),
            ),
            chain.vertices,
        )
        assertEquals(JsxGraphColor.Transparent, chain.style.fillColor)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "D",
                coordinates = JsxGraphPoint2D(6.0, -4.0),
            ),
        ).value
        val movedChain = assertIs<JsxGraphSceneElement.Polygon>(
            moved.elements.single { it.id == "chain" },
        )
        assertFalse(movedChain.isClosed)
        assertEquals(
            JsxGraphPoint2D(6.0, -4.0),
            movedChain.vertices.last(),
        )
    }

    @Test
    fun radicalAxisRendersAndTracksCircleUpdatesThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                center1 = point(-3, -1) <<
                    id: "center1", name: "", withLabel: false
                >>;
                radius1 = point(-1, -1) <<
                    id: "radius1", name: "", withLabel: false
                >>;
                circle1 = circle(center1, radius1) <<
                    id: "circle1", name: "", withLabel: false
                >>;
                center2 = point(2, 2) <<
                    id: "center2", name: "", withLabel: false
                >>;
                radius2 = point(5, 2) <<
                    id: "radius2", name: "", withLabel: false
                >>;
                circle2 = circle(center2, radius2) <<
                    id: "circle2", name: "", withLabel: false
                >>;
                radicalaxis(circle1, circle2) <<
                    id: "axis", name: "", withLabel: false,
                    straightFirst: false, straightLast: true,
                    strokeColor: "#0072B2", strokeWidth: 3,
                    point1: << id: "axisPoint1", name: "" >>,
                    point2: << id: "axisPoint2", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )
        val initialAxis = sceneLine(initial, "axis")

        assertEquals(7, initial.elements.size)
        assertFalse(initialAxis.straightFirst)
        assertTrue(initialAxis.straightLast)
        assertEquals(JsxGraphColor(0, 114, 178), initialAxis.style.strokeColor)
        assertEquals(3.0, initialAxis.style.strokeWidth)
        assertPointCoordinates(
            JsxGraphPoint2D(
                1.6029411764705883,
                -3.8382352941176476,
            ),
            initialAxis.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.5441176470588236,
                -2.073529411764706,
            ),
            initialAxis.point2,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center1",
                JsxGraphPoint2D(-4.0, 1.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius1",
                JsxGraphPoint2D(-1.0, 1.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center2",
                JsxGraphPoint2D(1.0, -2.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius2",
                JsxGraphPoint2D(3.0, -2.0),
            ),
        ).value
        val movedAxis = sceneLine(moved, "axis")

        assertPointCoordinates(
            JsxGraphPoint2D(
                -2.6323529411764706,
                -3.2205882352941178,
            ),
            movedAxis.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                -1.5735294117647058,
                -1.4558823529411766,
            ),
            movedAxis.point2,
        )
    }

    @Test
    fun polePointRendersAndTracksParentsThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                center = point(1, 1) <<
                    id: "center", name: "", withLabel: false
                >>;
                radius = point(3, 1) <<
                    id: "radius", name: "", withLabel: false
                >>;
                circle = circle(center, radius) <<
                    id: "circle", name: "", withLabel: false
                >>;
                linePoint1 = point(-1, 4) <<
                    id: "linePoint1", name: "", withLabel: false
                >>;
                linePoint2 = point(4, -1) <<
                    id: "linePoint2", name: "", withLabel: false
                >>;
                line = line(linePoint1, linePoint2) <<
                    id: "line", name: "", withLabel: false
                >>;
                polepoint(line, circle) <<
                    id: "pole", name: "", withLabel: false,
                    size: 7, fillColor: "#D9553F"
                >>;
                """.trimIndent(),
            ),
        )
        val initialPole = scenePoint(initial, "pole")

        assertEquals(7, initial.elements.size)
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, 5.0),
            initialPole.coordinates,
        )
        assertEquals(7.0, initialPole.size)
        assertEquals(JsxGraphColor(217, 85, 63), initialPole.style.fillColor)
        assertFalse(initialPole.draggable)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center",
                JsxGraphPoint2D(-2.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius",
                JsxGraphPoint2D(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "linePoint1",
                JsxGraphPoint2D(-3.0, -1.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "linePoint2",
                JsxGraphPoint2D(2.0, 4.0),
            ),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(
                2.499999999999999,
                -2.499999999999999,
            ),
            scenePoint(moved, "pole").coordinates,
        )
    }

    @Test
    fun tangentPolarAliasesRenderAndTrackParentsThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                center = point(1, 1) <<
                    id: "center", name: "", withLabel: false
                >>;
                radius = point(4, 1) <<
                    id: "radius", name: "", withLabel: false
                >>;
                circle = circle(center, radius) <<
                    id: "circle", name: "", withLabel: false
                >>;
                P = point(5, 4) <<
                    id: "P", name: "", withLabel: false
                >>;
                tangent(circle, P) <<
                    id: "tangent", name: "", withLabel: false,
                    straightFirst: false, straightLast: true,
                    strokeColor: "#0072B2",
                    point1: << id: "tangentPoint1", name: "" >>,
                    point2: << id: "tangentPoint2", name: "" >>
                >>;
                polar(P, circle) <<
                    id: "polar", name: "", withLabel: false,
                    strokeColor: "#D55E00",
                    point1: << id: "polarPoint1", name: "" >>,
                    point2: << id: "polarPoint2", name: "" >>
                >>;
                polarline(P, circle) <<
                    id: "polarLine", name: "", withLabel: false,
                    strokeColor: "#009E73",
                    point1: << id: "polarLinePoint1", name: "" >>,
                    point2: << id: "polarLinePoint2", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(7, initial.elements.size)
        val tangent = sceneLine(initial, "tangent")
        val polar = sceneLine(initial, "polar")
        val polarLine = sceneLine(initial, "polarLine")
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals(JsxGraphColor(0, 114, 178), tangent.style.strokeColor)
        assertEquals(JsxGraphColor(213, 94, 0), polar.style.strokeColor)
        assertEquals(JsxGraphColor(0, 158, 115), polarLine.style.strokeColor)
        for (line in listOf(tangent, polar, polarLine)) {
            assertPointCoordinates(
                JsxGraphPoint2D(2.8, 1.6),
                line.point1,
            )
            assertPointCoordinates(
                JsxGraphPoint2D(2.68, 1.76),
                line.point2,
            )
        }

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center",
                JsxGraphPoint2D(-2.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius",
                JsxGraphPoint2D(2.0, 2.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "P",
                JsxGraphPoint2D(2.0, -2.0),
            ),
        ).value
        for (id in listOf("tangent", "polar", "polarLine")) {
            val line = sceneLine(moved, id)
            assertPointCoordinates(
                JsxGraphPoint2D(-0.25, -0.25),
                line.point1,
            )
            assertPointCoordinates(
                JsxGraphPoint2D(-0.125, -0.125),
                line.point2,
            )
        }
    }

    @Test
    fun lineTangentsReuseSourceEndpointsThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-3, -1) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(4, 2) <<
                    id: "B", name: "", withLabel: false
                >>;
                source = line(A, B) <<
                    id: "source", name: "", withLabel: false,
                    strokeColor: "#666666"
                >>;
                P = point(1, 5) <<
                    id: "P", name: "", withLabel: false
                >>;
                tangent(source, P) <<
                    id: "tangent", name: "", withLabel: false,
                    straightFirst: false, straightLast: true,
                    strokeColor: "#0072B2",
                    point1: << id: "A", name: "ignored" >>,
                    point2: << id: "B", name: "ignored" >>
                >>;
                polar(P, source) <<
                    id: "polar", name: "", withLabel: false,
                    strokeColor: "#D55E00"
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(6, initial.elements.size)
        val source = sceneLine(initial, "source")
        val tangent = sceneLine(initial, "tangent")
        val polar = sceneLine(initial, "polar")
        assertPointCoordinates(source.point1, tangent.point1)
        assertPointCoordinates(source.point2, tangent.point2)
        assertPointCoordinates(source.point1, polar.point1)
        assertPointCoordinates(source.point2, polar.point2)
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals(JsxGraphColor(0, 114, 178), tangent.style.strokeColor)
        assertEquals(JsxGraphColor(213, 94, 0), polar.style.strokeColor)

        val afterParameterMove = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "P",
                JsxGraphPoint2D(7.0, 1.0),
            ),
        ).value
        assertPointCoordinates(
            source.point1,
            sceneLine(afterParameterMove, "tangent").point1,
        )
        assertPointCoordinates(
            source.point2,
            sceneLine(afterParameterMove, "tangent").point2,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "A",
                JsxGraphPoint2D(-5.0, 3.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "B",
                JsxGraphPoint2D(2.0, -4.0),
            ),
        ).value
        for (id in listOf("source", "tangent", "polar")) {
            val line = sceneLine(moved, id)
            assertPointCoordinates(
                JsxGraphPoint2D(-5.0, 3.0),
                line.point1,
            )
            assertPointCoordinates(
                JsxGraphPoint2D(2.0, -4.0),
                line.point2,
            )
        }
    }

    @Test
    fun curveTangentsRenderAndTrackPointsThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                functionCurve = functiongraph("x * x - 1", -4, 4) <<
                    id: "functionCurve", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                functionPoint = point(2, 5) <<
                    id: "functionPoint", name: "", withLabel: false
                >>;
                tangent(functionCurve, functionPoint) <<
                    id: "functionTangent", name: "", withLabel: false,
                    straightFirst: false, straightLast: true,
                    strokeColor: "#0072B2",
                    point1: << id: "functionTangentPoint1", name: "" >>,
                    point2: << id: "functionTangentPoint2", name: "" >>
                >>;
                polar(functionPoint, functionCurve) <<
                    id: "functionPolar", name: "", withLabel: false,
                    strokeColor: "#D55E00",
                    point1: << id: "functionPolarPoint1", name: "" >>,
                    point2: << id: "functionPolarPoint2", name: "" >>
                >>;
                parametricCurve = curve(
                    "2 * cos(x)", "sin(x)", 0, 6.283185307179586
                ) <<
                    id: "parametricCurve", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 64
                >>;
                parametricPoint = point(3, 0.75) <<
                    id: "parametricPoint", name: "", withLabel: false
                >>;
                tangent(parametricPoint, parametricCurve) <<
                    id: "parametricTangent", name: "", withLabel: false,
                    strokeColor: "#009E73",
                    point1: << id: "parametricTangentPoint1", name: "" >>,
                    point2: << id: "parametricTangentPoint2", name: "" >>
                >>;
                plotCurve = curve(
                    [-4, -1, 2, 5], [-2, 2, -1, 3]
                ) <<
                    id: "plotCurve", name: "", withLabel: false
                >>;
                plotPoint = point(0.25, 2.5) <<
                    id: "plotPoint", name: "", withLabel: false
                >>;
                tangent(plotCurve, plotPoint) <<
                    id: "plotTangent", name: "", withLabel: false,
                    straightFirst: false, straightLast: false,
                    strokeColor: "#CC79A7",
                    point1: << id: "plotTangentPoint1", name: "" >>,
                    point2: << id: "plotTangentPoint2", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(10, initial.elements.size)
        val functionTangent = sceneLine(initial, "functionTangent")
        val functionPolar = sceneLine(initial, "functionPolar")
        val parametricTangent = sceneLine(initial, "parametricTangent")
        val plotTangent = sceneLine(initial, "plotTangent")
        assertFalse(functionTangent.straightFirst)
        assertTrue(functionTangent.straightLast)
        assertFalse(plotTangent.straightFirst)
        assertFalse(plotTangent.straightLast)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            functionTangent.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            functionPolar.style.strokeColor,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.5882352941184178,
                -0.6470588235263288,
            ),
            functionTangent.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                3.1094038429387445,
                1.1337477941451766,
            ),
            parametricTangent.point1,
            tolerance = 1.0e-9,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.16666666666666666,
                0.8333333333333334,
            ),
            plotTangent.point1,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "functionPoint",
                JsxGraphPoint2D(-1.0, 4.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "parametricPoint",
                JsxGraphPoint2D(-2.5, -1.25),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "plotPoint",
                JsxGraphPoint2D(4.0, 4.0),
            ),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(
                0.40000000000040004,
                1.1999999999992,
            ),
            sceneLine(moved, "functionTangent").point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                -2.505232495502133,
                -1.2464977648448397,
            ),
            sceneLine(moved, "parametricTangent").point1,
            tolerance = 1.0e-9,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.52, -1.64),
            sceneLine(moved, "plotTangent").point1,
        )
    }

    @Test
    fun tangentFailureDoesNotLeakHelpersThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(0, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(2, 0) <<
                    id: "B", name: "", withLabel: false
                >>;
                P = point(3, 1) <<
                    id: "P", name: "", withLabel: false
                >>;
                circle = circle(A, B) <<
                    id: "circle", name: "", withLabel: false
                >>;
                taken = point(8, 8) <<
                    id: "taken", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            setOf("A", "B", "P", "circle", "taken"),
            initial.elements.map(JsxGraphSceneElement::id).toSet(),
        )

        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            session.execute(
                """
                tangent(circle, P) <<
                    id: "unused", name: "", withLabel: false,
                    point1: << id: "temporary", name: "" >>,
                    point2: << id: "taken", name: "" >>
                >>;
                """.trimIndent(),
            ),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(error)
        assertTrue("TangentFactory" in runtime.reason)
        assertTrue("DuplicateElementId" in runtime.reason)

        val afterFailure = scene(session.execute("0;"))
        assertEquals(
            setOf("A", "B", "P", "circle", "taken"),
            afterFailure.elements.map(JsxGraphSceneElement::id).toSet(),
        )
    }

    @Test
    fun normalBranchesRenderAndUpdateThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-4, -2) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(3, 2) <<
                    id: "B", name: "", withLabel: false
                >>;
                sourceLine = line(A, B) <<
                    id: "sourceLine", name: "", withLabel: false
                >>;
                P = point(-1, 4) <<
                    id: "P", name: "", withLabel: false
                >>;
                lineNormal = normal(sourceLine, P) <<
                    id: "lineNormal", name: "", withLabel: false,
                    straightFirst: false, straightLast: true,
                    point: << id: "lineNormalPoint", name: "" >>
                >>;
                lineNormal.point.X();
                lineNormal.subs.point.Y();
                center = point(2, -1) <<
                    id: "center", name: "", withLabel: false
                >>;
                radius = point(5, -1) <<
                    id: "radius", name: "", withLabel: false
                >>;
                circle = circle(center, radius) <<
                    id: "circle", name: "", withLabel: false
                >>;
                Q = point(4, 3) <<
                    id: "Q", name: "", withLabel: false
                >>;
                circleNormal = normal(Q, circle) <<
                    id: "circleNormal", name: "", withLabel: false
                >>;
                curve = functiongraph("0.5 * x * x - 2", -5, 5) <<
                    id: "curve", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 32
                >>;
                R = point(-2, 3) <<
                    id: "R", name: "", withLabel: false
                >>;
                curveNormal = normal(curve, R) <<
                    id: "curveNormal", name: "", withLabel: false,
                    point1: << id: "curveNormalPoint1", name: "" >>,
                    point2: << id: "curveNormalPoint2", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "A",
                "B",
                "sourceLine",
                "P",
                "lineNormal",
                "center",
                "radius",
                "circle",
                "Q",
                "circleNormal",
                "curve",
                "R",
                "curveNormal",
            ),
            initial.elements.map(JsxGraphSceneElement::id),
        )
        val initialLineNormal = sceneLine(initial, "lineNormal")
        val initialCircleNormal = sceneLine(initial, "circleNormal")
        val initialCurveNormal = sceneLine(initial, "curveNormal")
        assertFalse(initialLineNormal.straightFirst)
        assertTrue(initialLineNormal.straightLast)

        val movedLine = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(2.0, -4.0)),
        ).value
        assertNotEquals(
            initialLineNormal.point2,
            sceneLine(movedLine, "lineNormal").point2,
        )
        val movedCircle = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("center", JsxGraphPoint2D(-2.0, 2.0)),
        ).value
        assertNotEquals(
            initialCircleNormal.point1,
            sceneLine(movedCircle, "circleNormal").point1,
        )
        val movedCurve = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("R", JsxGraphPoint2D(1.5, 4.0)),
        ).value
        assertNotEquals(
            initialCurveNormal.point1,
            sceneLine(movedCurve, "curveNormal").point1,
        )
    }

    @Test
    fun normalFailureDoesNotLeakHelpersThroughPublicSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        scene(
            session.execute(
                """
                A = point(0, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                P = point(3, 1) <<
                    id: "P", name: "", withLabel: false
                >>;
                curve = functiongraph("x * x", -2, 2) <<
                    id: "curve", name: "", withLabel: false,
                    doAdvancedPlot: false, numberPointsHigh: 16
                >>;
                """.trimIndent(),
            ),
        )

        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            session.execute(
                """
                normal(curve, P) <<
                    id: "unused", name: "", withLabel: false,
                    point1: << id: "temporary", name: "" >>,
                    point2: << id: "A", name: "" >>
                >>;
                """.trimIndent(),
            ),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(error)
        assertTrue("NormalFactory" in runtime.reason)
        assertTrue("DuplicateElementId" in runtime.reason)

        assertEquals(
            setOf("A", "P", "curve"),
            scene(session.execute("0;"))
                .elements
                .map(JsxGraphSceneElement::id)
                .toSet(),
        )
    }

    @Test
    fun transformationsCreatePointsWithoutConsumingSceneObjectBudget() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                source =
                    """
                    A = point(1, 2) <<
                        id: "A", name: "", withLabel: false
                    >>;
                    scale = transform(2, 3) << type: "scale" >>;
                    shift = transform(1, -1) << type: "translate" >>;
                    B = point(A, [scale, shift]) <<
                        id: "B", name: "", withLabel: false
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 2),
            ),
        )

        assertEquals(2, scene.elements.size)
        assertPointCoordinates(
            expected = JsxGraphPoint2D(1.0, 2.0),
            actual = scenePoint(scene, "A").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(3.0, 5.0),
            actual = scenePoint(scene, "B").coordinates,
        )
    }

    @Test
    fun functionValuedTransformationTracksSessionUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(1, 2) <<
                    id: "A", name: "", withLabel: false
                >>;
                driver = point(2, 0) <<
                    id: "driver", name: "", withLabel: false
                >>;
                shift = transform(
                    function () { return driver.X(); },
                    function () { return driver.Y(); }
                ) << type: "translate" >>;
                B = point(A, shift) <<
                    id: "B", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(3.0, 2.0),
            actual = scenePoint(initial, "B").coordinates,
        )
        assertIs<JsxGraphInteractionError.PointNotDraggable>(
            assertIs<GMResult.Err<JsxGraphInteractionError>>(
                session.movePoint(
                    id = "B",
                    coordinates = JsxGraphPoint2D(9.0, 9.0),
                ),
            ).error,
        )

        val moved = assertIs<
            GMResult.Ok<JsxGraphScene>
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(4.0, -1.0),
            ),
        ).value
        assertPointCoordinates(
            expected = JsxGraphPoint2D(5.0, 1.0),
            actual = scenePoint(moved, "B").coordinates,
        )
    }

    @Test
    fun functionCoordinatePointsTrackSessionUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                Driver = point(2, 3) <<
                    id: "driver", name: "", withLabel: false
                >>;
                arrayPoint = point(
                    function () {
                        return [Driver.X() + 1, Driver.Y() + 2];
                    }
                ) <<
                    id: "arrayPoint", name: "", withLabel: false
                >>;
                mixedPoint = point(
                    function () { return Driver.X() * 2; },
                    function () { return Driver.Y() - 1; }
                ) <<
                    id: "mixedPoint", name: "", withLabel: false
                >>;
                homogeneousPoint = point(
                    function () {
                        return [2, Driver.X() * 2, Driver.Y() * 2];
                    }
                ) <<
                    id: "homogeneousPoint", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertPointCoordinates(
            expected = JsxGraphPoint2D(3.0, 5.0),
            actual = scenePoint(initial, "arrayPoint").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(4.0, 2.0),
            actual = scenePoint(initial, "mixedPoint").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(2.0, 3.0),
            actual = scenePoint(initial, "homogeneousPoint").coordinates,
        )
        assertIs<JsxGraphInteractionError.PointNotDraggable>(
            assertIs<GMResult.Err<JsxGraphInteractionError>>(
                session.movePoint(
                    id = "arrayPoint",
                    coordinates = JsxGraphPoint2D(0.0, 0.0),
                ),
            ).error,
        )

        val moved = assertIs<
            GMResult.Ok<JsxGraphScene>
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(5.0, -1.0),
            ),
        ).value
        assertPointCoordinates(
            expected = JsxGraphPoint2D(6.0, 1.0),
            actual = scenePoint(moved, "arrayPoint").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(10.0, -2.0),
            actual = scenePoint(moved, "mixedPoint").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(5.0, -1.0),
            actual = scenePoint(moved, "homogeneousPoint").coordinates,
        )
    }

    @Test
    fun polygonPathIntersectionsWorkThroughPublicJessieCodeApi() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-2, -2) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(2, -2) <<
                    id: "B", name: "", withLabel: false
                >>;
                C = point(2, 2) <<
                    id: "C", name: "", withLabel: false
                >>;
                D = point(-2, 2) <<
                    id: "D", name: "", withLabel: false
                >>;
                P = polygon(A, B, C, D) <<
                    id: "polygon", name: "", withLabel: false
                >>;
                O = point(0, 0) <<
                    id: "O", name: "", withLabel: false
                >>;
                circle = circle(O, 2.5) <<
                    id: "circle", name: "", withLabel: false
                >>;
                hit = intersection(P, circle, 2) <<
                    id: "hit", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                2.0,
                -1.499885402306805,
            ),
            actual = scenePoint(initial, "hit").coordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(3.0, 2.0),
            ),
        ).value
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                2.2058845193854877,
                -1.176461922458049,
            ),
            actual = scenePoint(moved, "hit").coordinates,
        )

        val fractional = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
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
            ),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(fractional)
        assertTrue("ClipComputation" in runtime.reason)
        assertTrue("InvalidIntersectionIndex" in runtime.reason)
    }

    @Test
    fun functionRadiusTracksPointAssignmentsAndNonnegativeOnly() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                C = point(0, 0) <<
                    id: "C", name: "", withLabel: false
                >>;
                A = point(2, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                c = circle(
                    C,
                    function () { return A.X() - 4; }
                ) <<
                    id: "circle", name: "", withLabel: false,
                    nonnegativeOnly: true
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            0.0,
            assertIs<JsxGraphSceneElement.Circle>(
                initial.elements.last(),
            ).radius,
        )

        val moved = scene(
            session.execute(
                source = "A.X = 7;",
                storeSource = false,
            ),
        )
        assertEquals(
            3.0,
            assertIs<JsxGraphSceneElement.Circle>(
                moved.elements.last(),
            ).radius,
        )
    }

    @Test
    fun sourceCreatesNumericStringAndFunctionFixedLengthSegments() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                source =
                    """
                    A = point(0, 0) <<
                        id: "A", name: "", withLabel: false, fixed: true
                    >>;
                    B = point(2, 0) <<
                        id: "B", name: "", withLabel: false
                    >>;
                    s = segment(A, B, -3) <<
                        id: "s", name: "", withLabel: false
                    >>;
                    """.trimIndent(),
            ),
        )
        val pointA = assertIs<JsxGraphSceneElement.Point>(scene.elements[0])
        val pointB = assertIs<JsxGraphSceneElement.Point>(scene.elements[1])
        val segment = assertIs<JsxGraphSceneElement.Line>(scene.elements[2])
        assertEquals(JsxGraphPoint2D(0.0, 0.0), pointA.coordinates)
        assertEquals(JsxGraphPoint2D(3.0, 0.0), pointB.coordinates)
        assertEquals(pointA.coordinates, segment.point1)
        assertEquals(pointB.coordinates, segment.point2)

        val dynamic = scene(
            JsxGraphJessieCode.parse(
                """
                a = segment([0, 0], [2, 0], "4") <<
                    id: "a", name: "", withLabel: false
                >>;
                b = segment([0, 2], [2, 2], function () { return 5; }) <<
                    id: "b", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        val lines = dynamic.elements.filterIsInstance<JsxGraphSceneElement.Line>()
        assertEquals(4.0, lines[0].point2.x - lines[0].point1.x)
        assertEquals(5.0, lines[1].point2.x - lines[1].point1.x)
    }

    @Test
    fun dynamicSegmentLengthsTrackSessionUpdatesAndSurfaceFailures() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                D = point(2, 0) <<
                    id: "D", name: "", withLabel: false
                >>;
                A = point(0, 0) <<
                    id: "A", name: "", withLabel: false, fixed: true
                >>;
                B = point(2, 0) <<
                    id: "B", name: "", withLabel: false
                >>;
                C = point(0, 2) <<
                    id: "C", name: "", withLabel: false, fixed: true
                >>;
                E = point(2, 2) <<
                    id: "E", name: "", withLabel: false
                >>;
                s1 = segment(A, B, "D.X() + 1") <<
                    id: "s1", name: "", withLabel: false
                >>;
                s2 = segment(
                    C,
                    E,
                    function () { return D.X() + 2; }
                ) <<
                    id: "s2", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(3.0, segmentLength(initial, "s1"))
        assertEquals(4.0, segmentLength(initial, "s2"))

        val moved = scene(
            session.execute(
                source = "D.X = 5;",
                storeSource = false,
            ),
        )
        assertEquals(6.0, segmentLength(moved, "s1"))
        assertEquals(7.0, segmentLength(moved, "s2"))

        val failingSession = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>
            >(JsxGraphJessieCode.createSession()).value
        scene(
            failingSession.execute(
                """
                D = point(2, 0) << id: "D", name: "", withLabel: false >>;
                A = point(0, 0) <<
                    id: "A", name: "", withLabel: false, fixed: true
                >>;
                B = point(2, 0) << id: "B", name: "", withLabel: false >>;
                segment(
                    A,
                    B,
                    function () {
                        return D.X() > 4 ? "length" : 3;
                    }
                ) << id: "s", name: "", withLabel: false >>;
                """.trimIndent(),
            ),
        )
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            failingSession.execute(
                source = "D.X = 5;",
                storeSource = false,
            ),
        ).error
        val sceneError = assertIs<JsxGraphJessieCodeError.Scene>(error)
        val creation = assertIs<JsxGraphDocumentError.ElementCreation>(
            sceneError.error,
        )
        assertTrue("NonNumericFixedLengthFunction" in creation.reason)
    }

    @Test
    fun sessionPersistsLocalsMutationsSourceAndBoardLifecycle() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value

        scene(
            session.execute(
                """
                A = point(0, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        val withLine = scene(
            session.execute(
                """
                B = point(2, 0) <<
                    id: "B", name: "", withLabel: false
                >>;
                l = segment(A, B) <<
                    id: "line", name: "", withLabel: false,
                    straightFirst: true, straightLast: true
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(listOf("A", "B", "line"), withLine.elements.map { it.id })

        val moved = scene(
            session.execute(
                "A.moveTo([1, 2]);",
                storeSource = false,
            ),
        )
        assertEquals(
            JsxGraphPoint2D(1.0, 2.0),
            assertIs<JsxGraphSceneElement.Point>(moved.elements[0]).coordinates,
        )
        assertTrue("moveTo" !in session.code)

        val deleted = scene(session.execute("delete A;"))
        assertEquals(listOf("B"), deleted.elements.map { it.id })
        assertTrue("delete A;" in session.code)
        assertEquals(deleted, session.scene)
    }

    @Test
    fun bisectorLinesTracksTwoStyledOutputsAndInteractiveUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-4, -1) <<
                    id: "a", name: "", withLabel: false
                >>;
                B = point(2, 3) <<
                    id: "b", name: "", withLabel: false
                >>;
                C = point(-3, 4) <<
                    id: "c", name: "", withLabel: false
                >>;
                D = point(4, -2) <<
                    id: "d", name: "", withLabel: false
                >>;
                first = line(A, B) <<
                    id: "first", name: "", withLabel: false
                >>;
                second = line(C, D) <<
                    id: "second", name: "", withLabel: false
                >>;
                pair = bisectorlines(first, second) <<
                    layer: 5,
                    line1: <<
                        id: "bisectorFirst", name: "",
                        withLabel: false,
                        strokeColor: "#1565c0", strokeWidth: 3
                    >>,
                    line2: <<
                        id: "bisectorSecond", name: "",
                        withLabel: false,
                        strokeColor: "#c62828", strokeWidth: 2
                    >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "a",
                "b",
                "c",
                "d",
                "first",
                "second",
                "bisectorFirst",
                "bisectorSecond",
            ),
            initial.elements.map { it.id },
        )
        val firstBisector = sceneLine(initial, "bisectorFirst")
        val secondBisector = sceneLine(initial, "bisectorSecond")
        assertEquals(
            JsxGraphColor(21, 101, 192),
            firstBisector.style.strokeColor,
        )
        assertEquals(3.0, firstBisector.style.strokeWidth)
        assertEquals(7, firstBisector.style.layer)
        assertEquals(
            JsxGraphColor(198, 40, 40),
            secondBisector.style.strokeColor,
        )
        assertEquals(2.0, secondBisector.style.strokeWidth)
        assertEquals(7, secondBisector.style.layer)
        assertPointCoordinates(
            JsxGraphPoint2D(
                -0.14987187202709523,
                1.668124255252562,
            ),
            firstBisector.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                1.3457036003260912,
                1.471804427762934,
            ),
            secondBisector.point1,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "a",
                coordinates = JsxGraphPoint2D(-2.0, -3.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "d",
                coordinates = JsxGraphPoint2D(3.0, 1.0),
            ),
        ).value
        assertPointCoordinates(
            JsxGraphPoint2D(
                1.2449506364078966,
                1.8940137039722855,
            ),
            sceneLine(moved, "bisectorFirst").point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.9064371824581011,
                1.7837619337597095,
            ),
            sceneLine(moved, "bisectorSecond").point1,
        )

        val removed = scene(session.execute("delete pair;"))
        assertEquals(
            listOf("a", "b", "c", "d", "first", "second"),
            removed.elements.map { it.id },
        )
    }

    @Test
    fun bisectorLinesConsumesTwoSceneObjectSlots() {
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    A = point(-4, -1) << id: "a", name: "" >>;
                    B = point(2, 3) << id: "b", name: "" >>;
                    C = point(-3, 4) << id: "c", name: "" >>;
                    D = point(4, -2) << id: "d", name: "" >>;
                    first = line(A, B) << id: "first", name: "" >>;
                    second = line(C, D) << id: "second", name: "" >>;
                    bisectorlines(first, second);
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 7),
            ),
        ).error

        assertResourceLimit(
            error = error,
            resource = "created element count",
            requestedSize = 8,
        )
    }

    @Test
    fun tangentToTracksThreeStyledOutputsAndInteractiveUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                O = point(1, 1) <<
                    id: "center", name: "", withLabel: false
                >>;
                R = point(4, 1) <<
                    id: "radius", name: "", withLabel: false
                >>;
                C = circle(O, R) <<
                    id: "circle", name: "", withLabel: false
                >>;
                P = point(5, 4) <<
                    id: "source", name: "", withLabel: false
                >>;
                T = tangentto(C, P, 1) <<
                    id: "tangent", name: "", withLabel: false,
                    strokeColor: "#204060", strokeWidth: 4,
                    straightFirst: false, straightLast: true,
                    point1: <<
                        id: "tangentPoint1", name: "", withLabel: false
                    >>,
                    point2: <<
                        id: "tangentPoint2", name: "", withLabel: false
                    >>,
                    polar: <<
                        id: "polar", name: "", withLabel: false,
                        visible: true,
                        strokeColor: "#a02040", strokeWidth: 5,
                        point1: <<
                            id: "polarPoint1", name: "", withLabel: false
                        >>,
                        point2: <<
                            id: "polarPoint2", name: "", withLabel: false
                        >>
                    >>,
                    point: <<
                        id: "intersection", name: "", withLabel: false,
                        visible: true, size: 7,
                        fillColor: "#009e73", strokeColor: "#009e73"
                    >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "center",
                "radius",
                "circle",
                "source",
                "polar",
                "intersection",
                "tangent",
            ),
            initial.elements.map(JsxGraphSceneElement::id),
        )
        val tangent = sceneLine(initial, "tangent")
        val polar = sceneLine(initial, "polar")
        val intersection = scenePoint(initial, "intersection")
        assertEquals(JsxGraphColor(32, 64, 96), tangent.style.strokeColor)
        assertTrue(tangent.style.strokeDashPattern.isEmpty())
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals(JsxGraphColor(160, 32, 64), polar.style.strokeColor)
        assertEquals(listOf(10.0, 10.0), polar.style.strokeDashPattern)
        assertEquals(JsxGraphColor(0, 158, 115), intersection.style.fillColor)
        assertEquals(7.0, intersection.size)
        assertPointCoordinates(
            JsxGraphPoint2D(3.88, 0.16000000000000036),
            intersection.coordinates,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("center", JsxGraphPoint2D(-2.0, 2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("radius", JsxGraphPoint2D(2.0, 2.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("source", JsxGraphPoint2D(3.0, -3.0)),
        ).value
        assertPointCoordinates(
            JsxGraphPoint2D(-2.73238075793812, -1.93238075793812),
            scenePoint(moved, "intersection").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-0.6, 0.2),
            sceneLine(moved, "polar").point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-0.5, 0.3),
            sceneLine(moved, "polar").point2,
        )
    }

    @Test
    fun tangentToConsumesThreeSceneSlotsAndFailuresDoNotLeakObjects() {
        val tooSmall = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source = "tangentto(0, 0);",
                limits = JsxGraphJessieCodeLimits(maxObjects = 2),
            ),
        ).error
        assertResourceLimit(
            error = tooSmall,
            resource = "created element count",
            requestedSize = 3,
        )
        val admitted = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source = "tangentto(0, 0);",
                limits = JsxGraphJessieCodeLimits(maxObjects = 3),
            ),
        ).error
        val admittedRuntime =
            assertIs<JsxGraphJessieCodeError.Runtime>(admitted)
        assertTrue("UnsupportedParents" in admittedRuntime.reason)

        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(maxObjects = 8),
            ),
        ).value
        val sources = scene(
            session.execute(
                """
                O = point(0, 0) <<
                    id: "O", name: "", withLabel: false
                >>;
                R = point(3, 0) <<
                    id: "R", name: "", withLabel: false
                >>;
                C = circle(O, R) <<
                    id: "C", name: "", withLabel: false
                >>;
                P = point(5, 1) <<
                    id: "P", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(listOf("O", "R", "C", "P"), sources.elements.map { it.id })

        val duplicate = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            session.execute(
                """
                T = tangentto(C, P) <<
                    id: "shared", name: "", withLabel: false,
                    point1: << id: "tangentPoint1", name: "" >>,
                    point2: << id: "tangentPoint2", name: "" >>,
                    polar: <<
                        id: "polar", name: "",
                        point1: << id: "shared", name: "" >>,
                        point2: << id: "polarPoint2", name: "" >>
                    >>,
                    point: << id: "intersection", name: "" >>
                >>;
                """.trimIndent(),
            ),
        ).error
        val duplicateRuntime =
            assertIs<JsxGraphJessieCodeError.Runtime>(duplicate)
        assertTrue("DuplicateElementId(id=shared)" in duplicateRuntime.reason)
        assertEquals(sources, session.scene)

        val afterFailure = scene(
            session.execute(
                """
                Q = point(-1, -1) <<
                    id: "Q", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            listOf("O", "R", "C", "P", "Q"),
            afterFailure.elements.map(JsxGraphSceneElement::id),
        )
    }

    @Test
    fun midpointScenesTrackPointAndLineParentsAndRemoveOwnedHelpers() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-4, 2) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(2, -2) <<
                    id: "B", name: "", withLabel: false
                >>;
                C = point(-6, -4) <<
                    id: "C", name: "", withLabel: false
                >>;
                D = point(2, -4) <<
                    id: "D", name: "", withLabel: false
                >>;
                l = segment(C, D) <<
                    id: "line", name: "", withLabel: false
                >>;
                M1 = midpoint(A, B) <<
                    id: "M1", name: "", withLabel: false
                >>;
                M2 = midpoint(l) <<
                    id: "M2", name: "", withLabel: false
                >>;
                M3 = midpoint([-2, 4], [4, 2]) <<
                    id: "M3", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(8, initial.elements.size)
        assertEquals(
            JsxGraphPoint2D(-1.0, 0.0),
            scenePoint(initial, "M1").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(-2.0, -4.0),
            scenePoint(initial, "M2").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(1.0, 3.0),
            scenePoint(initial, "M3").coordinates,
        )

        val moved = scene(
            session.execute(
                source =
                    "A.X = 0; A.Y = 4; C.X = -2; C.Y = -2;",
                storeSource = false,
            ),
        )
        assertEquals(
            JsxGraphPoint2D(1.0, 1.0),
            scenePoint(moved, "M1").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(0.0, -3.0),
            scenePoint(moved, "M2").coordinates,
        )

        val removed = scene(session.execute("delete M3;"))
        assertEquals(7, removed.elements.size)
        assertTrue(removed.elements.none { it.id == "M3" })
    }

    @Test
    fun orthogonalScenesTrackParentsWithoutExposingHelpers() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-4, -1) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(2, 3) <<
                    id: "B", name: "", withLabel: false
                >>;
                baseLine = line(A, B) <<
                    id: "baseLine", name: "", withLabel: false
                >>;
                P = point(3, -3) <<
                    id: "P", name: "", withLabel: false
                >>;
                projection = orthogonalprojection(P, baseLine) <<
                    id: "projection", name: "", withLabel: false
                >>;
                foot = perpendicularpoint(baseLine, P) <<
                    id: "foot", name: "", withLabel: false
                >>;
                normal = perpendicular(P, baseLine) <<
                    id: "normal", name: "", withLabel: false
                >>;
                drop = perpendicularsegment(baseLine, P) <<
                    id: "drop", name: "", withLabel: false,
                    straightFirst: true, straightLast: true
                >>;
                on = point(-1, 1) <<
                    id: "on", name: "", withLabel: false
                >>;
                projectionOn = orthogonalprojection(on, baseLine) <<
                    id: "projectionOn", name: "", withLabel: false
                >>;
                footOn = perpendicularpoint(on, baseLine) <<
                    id: "footOn", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "A",
                "B",
                "baseLine",
                "P",
                "projection",
                "foot",
                "normal",
                "drop",
                "on",
                "projectionOn",
                "footOn",
            ),
            initial.elements.map { it.id },
        )
        val projection = scenePoint(initial, "projection")
        val foot = scenePoint(initial, "foot")
        assertPointCoordinates(
            JsxGraphPoint2D(
                -0.07692307692307687,
                1.6153846153846156,
            ),
            projection.coordinates,
        )
        assertPointCoordinates(projection.coordinates, foot.coordinates)

        val normal = sceneLine(initial, "normal")
        assertEquals(true, normal.straightFirst)
        assertEquals(true, normal.straightLast)
        val drop = sceneLine(initial, "drop")
        assertEquals(true, drop.straightFirst)
        assertEquals(true, drop.straightLast)
        assertPointCoordinates(projection.coordinates, drop.point1)
        assertPointCoordinates(
            scenePoint(initial, "P").coordinates,
            drop.point2,
        )

        assertPointCoordinates(
            JsxGraphPoint2D(-1.0, 1.0),
            scenePoint(initial, "projectionOn").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, -2.0),
            scenePoint(initial, "footOn").coordinates,
        )

        val moved = scene(
            session.execute(
                source = "P.X = 0; P.Y = 4;",
                storeSource = false,
            ),
        )
        val expectedProjection = JsxGraphPoint2D(14.0 / 13.0, 31.0 / 13.0)
        assertPointCoordinates(
            expectedProjection,
            scenePoint(moved, "projection").coordinates,
        )
        assertPointCoordinates(
            expectedProjection,
            scenePoint(moved, "foot").coordinates,
        )
        val movedDrop = sceneLine(moved, "drop")
        assertPointCoordinates(expectedProjection, movedDrop.point1)
        assertPointCoordinates(JsxGraphPoint2D(0.0, 4.0), movedDrop.point2)
    }

    @Test
    fun parallelScenesTrackFiniteAndIdealFormsWithoutExposingHelpers() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-4, -1) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(2, 3) <<
                    id: "B", name: "", withLabel: false
                >>;
                baseLine = line(A, B) <<
                    id: "baseLine", name: "", withLabel: false
                >>;
                C = point(3, -3) <<
                    id: "C", name: "", withLabel: false
                >>;
                pp = parallelpoint(A, B, C) <<
                    id: "parallelPoint", name: "", withLabel: false
                >>;
                finite = parallel(A, B, C) <<
                    id: "finiteParallel", name: "", withLabel: false,
                    straightFirst: false, straightLast: false
                >>;
                ideal = parallel(baseLine, C) <<
                    id: "idealParallel", name: "", withLabel: false
                >>;
                reverse = parallel(C, baseLine) <<
                    id: "reverseParallel", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "A",
                "B",
                "baseLine",
                "C",
                "parallelPoint",
                "finiteParallel",
                "idealParallel",
                "reverseParallel",
            ),
            initial.elements.map { it.id },
        )
        assertPointCoordinates(
            JsxGraphPoint2D(9.0, 1.0),
            scenePoint(initial, "parallelPoint").coordinates,
        )
        val finite = sceneLine(initial, "finiteParallel")
        assertEquals(false, finite.straightFirst)
        assertEquals(false, finite.straightLast)
        assertPointCoordinates(JsxGraphPoint2D(3.0, -3.0), finite.point1)
        assertPointCoordinates(JsxGraphPoint2D(9.0, 1.0), finite.point2)
        for (id in listOf("idealParallel", "reverseParallel")) {
            val parallel = sceneLine(initial, id)
            assertEquals(true, parallel.straightFirst)
            assertEquals(true, parallel.straightLast)
            assertParallelLine(
                baseLine = sceneLine(initial, "baseLine"),
                parallel = parallel,
                through = JsxGraphPoint2D(3.0, -3.0),
            )
        }

        val moved = scene(
            session.execute(
                source =
                    "A.X = -2; A.Y = 2; " +
                        "B.X = 4; B.Y = 1; " +
                        "C.X = 1; C.Y = -4;",
                storeSource = false,
            ),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(7.0, -5.0),
            scenePoint(moved, "parallelPoint").coordinates,
        )
        assertParallelLine(
            baseLine = sceneLine(moved, "baseLine"),
            parallel = sceneLine(moved, "idealParallel"),
            through = JsxGraphPoint2D(1.0, -4.0),
        )

        val removed = scene(session.execute("delete ideal;"))
        assertTrue(removed.elements.none { it.id == "idealParallel" })
        assertEquals(7, removed.elements.size)
    }

    @Test
    fun invalidConfigurationAndFailuresRemainStructured() {
        val configurationError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.createSession(
                    boardOptions = JsxGraphJessieCodeBoardOptions(
                        containerId = "",
                    ),
                ),
            ).error
        assertIs<JsxGraphJessieCodeError.InvalidConfiguration>(
            configurationError,
        )

        val parseError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse("var point = 1;"),
        ).error
        val parse = assertIs<JsxGraphJessieCodeError.Parse>(parseError)
        assertEquals(1, parse.location?.line)
        assertEquals(0, parse.location?.column)

        val runtimeError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse("use missing;"),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(runtimeError)
        assertTrue("BoardNotFound" in runtime.reason)
    }

    @Test
    fun sessionAndCreatorResourceLimitsAreEnforcedBeforeGrowth() {
        val historySession = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>
            >(
            JsxGraphJessieCode.createSession(
                limits = JsxGraphJessieCodeLimits(
                    maxStoredSourceLength = 3,
                ),
            ),
        ).value
        assertIs<JsxGraphJessieCodeError.SourceHistoryLimitExceeded>(
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                historySession.execute("1 + 1;"),
            ).error,
        )

        val objectError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    point(0, 0) << id: "A", name: "", withLabel: false >>;
                    point(1, 1) << id: "B", name: "", withLabel: false >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 1),
            ),
        ).error
        assertResourceLimit(objectError, "created element count", 2)

        val curveError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    functiongraph("x * x", -1, 1) <<
                        id: "f", name: "", withLabel: false,
                        doAdvancedPlot: false, numberPointsHigh: 5
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 4),
            ),
        ).error
        assertResourceLimit(curveError, "curve point count", 5)

        val hyperbolaError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        hyperbola([-3, 0], [3, 0], 4) <<
                            id: "hyperbola", name: "", withLabel: false,
                            doAdvancedPlot: false, numberPointsHigh: 5
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 4),
                ),
            ).error
        assertResourceLimit(
            hyperbolaError,
            "curve point count",
            5,
        )

        val derivativeError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        source = functiongraph("x * x", -1, 1) <<
                            id: "source", name: "", withLabel: false,
                            doAdvancedPlot: false, numberPointsHigh: 4
                        >>;
                        derivative(source) <<
                            id: "derivative", name: "", withLabel: false,
                            doAdvancedPlot: false, numberPointsHigh: 5
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 4),
                ),
            ).error
        assertResourceLimit(
            derivativeError,
            "curve point count",
            5,
        )

        val stepfunctionError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        stepfunction([0, 1, 2], [2, 1, 3]) <<
                            id: "step", name: "", withLabel: false
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 4),
                ),
            ).error
        assertResourceLimit(
            stepfunctionError,
            "curve point count",
            5,
        )

        val splineError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        spline([-1, 0, 1], [0, 1, 0]) <<
                            id: "spline", name: "", withLabel: false,
                            doAdvancedPlot: false, numberPointsHigh: 5
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 4),
                ),
            ).error
        assertResourceLimit(
            splineError,
            "curve point count",
            5,
        )

        val cardinalSplineError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        cardinalspline(
                            [[-1, 0], [0, 1], [1, 0]],
                            0.5
                        ) <<
                            id: "cardinal", name: "", withLabel: false,
                            doAdvancedPlot: false, numberPointsHigh: 5
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 4),
                ),
            ).error
        assertResourceLimit(
            cardinalSplineError,
            "curve point count",
            5,
        )

        val riemannError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        f = function (x) { return x * x + 1; };
                        riemannsum(f, 5, "left", -1, 2) <<
                            id: "riemann", name: "", withLabel: false
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 24),
                ),
            ).error
        assertResourceLimit(
            riemannError,
            "curve point count",
            25,
        )

        val dynamicRiemannSession = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                limits =
                    JsxGraphJessieCodeLimits(maxCurvePoints = 20),
            ),
        ).value
        scene(
            dynamicRiemannSession.execute(
                """
                A = point(4, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                f = function (x) { return x * x + 1; };
                riemannsum(f, "A.X()", "left", -1, 2) <<
                    id: "riemann", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        val dynamicLimit = assertIs<
            GMResult.Err<JsxGraphInteractionError>,
            >(
            dynamicRiemannSession.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(6.0, 0.0),
            ),
        ).error
        val interactionLimit = assertIs<
            JsxGraphInteractionError.ResourceLimitExceeded,
            >(dynamicLimit)
        assertEquals("curve point count", interactionLimit.resource)
        assertEquals(20, interactionLimit.limit)
        assertEquals(30, interactionLimit.requestedSize)
        assertEquals(
            20,
            sceneCurve(dynamicRiemannSession.scene, "riemann").points.size,
        )

        val boxPlotError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        boxplot(
                            [-2, -1, 0, 1, 2, [3]],
                            0,
                            2
                        ) <<
                            id: "box", name: "", withLabel: false,
                            outlier: << face: "circle", size: 6 >>
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxCurvePoints = 38),
                ),
            ).error
        assertResourceLimit(
            boxPlotError,
            "curve point count",
            39,
        )

        val dynamicBoxPlotSession = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                limits =
                    JsxGraphJessieCodeLimits(maxCurvePoints = 38),
            ),
        ).value
        scene(
            dynamicBoxPlotSession.execute(
                """
                driver = point(-1, 0) <<
                    id: "driver", name: "", withLabel: false
                >>;
                outliers = function () {
                    return driver.X() > 0 ? [3] : [];
                };
                boxplot(
                    [-2, -1, 0, 1, 2, outliers],
                    0,
                    2
                ) <<
                    id: "box", name: "", withLabel: false,
                    outlier: << face: "circle", size: 6 >>
                >>;
                """.trimIndent(),
            ),
        )
        assertEquals(
            20,
            sceneCurve(dynamicBoxPlotSession.scene, "box").points.size,
        )
        val dynamicBoxPlotLimit = assertIs<
            GMResult.Err<JsxGraphInteractionError>,
            >(
            dynamicBoxPlotSession.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 0.0),
            ),
        ).error
        val boxPlotInteractionLimit = assertIs<
            JsxGraphInteractionError.ResourceLimitExceeded,
            >(dynamicBoxPlotLimit)
        assertEquals(
            "curve point count",
            boxPlotInteractionLimit.resource,
        )
        assertEquals(38, boxPlotInteractionLimit.limit)
        assertEquals(39, boxPlotInteractionLimit.requestedSize)
        assertEquals(
            20,
            sceneCurve(dynamicBoxPlotSession.scene, "box").points.size,
        )

        val polygonalChainError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        polygonalchain(
                            [0, 0],
                            [1, 0],
                            [1, 1],
                            [0, 1]
                        ) << id: "chain", name: "", withLabel: false >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxPolygonVertices = 3),
                ),
            ).error
        assertResourceLimit(
            polygonalChainError,
            "polygon vertex count",
            4,
        )

        val parallelogramError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        parallelogram(
                            [0, 0],
                            [1, 0],
                            [1, 1]
                        ) << id: "parallelogram", name: "" >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxPolygonVertices = 2),
                ),
            ).error
        assertResourceLimit(
            parallelogramError,
            "polygon vertex count",
            3,
        )

        val regularPolygonError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    source =
                        """
                        regularpolygon([0, 0], [1, 0], 5) <<
                            id: "regular", name: "", withLabel: false
                        >>;
                        """.trimIndent(),
                    limits =
                        JsxGraphJessieCodeLimits(maxPolygonVertices = 4),
                ),
            ).error
        assertResourceLimit(
            regularPolygonError,
            "polygon vertex count",
            5,
        )

        val mutableStepSession = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                limits =
                    JsxGraphJessieCodeLimits(maxCurvePoints = 4),
            ),
        ).value
        scene(
            mutableStepSession.execute(
                """
                xs = [0, 1];
                ys = [2, 1];
                stepfunction(xs, ys) <<
                    id: "step", name: "", withLabel: false
                >>;
                """.trimIndent(),
            ),
        )
        val mutatedStepError =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                mutableStepSession.execute(
                    "xs[2] = 2; ys[2] = 3;",
                ),
            ).error
        assertResourceLimit(
            mutatedStepError,
            "curve point count",
            5,
        )

        val textError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    text(0, 0, "long") <<
                        id: "t", name: "", withLabel: false
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxTextLength = 3),
            ),
        ).error
        assertResourceLimit(textError, "text length", 4)
    }

    @Test
    fun combFunctionAttributesRenderAndResourceFailureRollsBack() {
        val staticLimit = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    comb([-2, 0], [2, 0]) <<
                        id: "comb", name: "", withLabel: false,
                        frequency: 0.5
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 23),
            ),
        ).error
        assertResourceLimit(
            staticLimit,
            resource = "curve point count",
            requestedSize = 24,
        )

        val session = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                limits =
                    JsxGraphJessieCodeLimits(maxCurvePoints = 12),
            ),
        ).value
        val initial = scene(
            session.execute(
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
                        return driver.X() < 0 ? 2 : 1;
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
            ),
        )
        val initialComb = sceneCurve(initial, "comb")
        assertEquals(9, initialComb.points.size)
        assertEquals(JsxGraphColor(0, 0, 255), initialComb.style.strokeColor)
        assertEquals(JsxGraphColor.Transparent, initialComb.style.fillColor)

        val limit = assertIs<
            GMResult.Err<JsxGraphInteractionError.ResourceLimitExceeded>,
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 1.0),
            ),
        ).error
        assertEquals("curve point count", limit.resource)
        assertEquals(12, limit.limit)
        assertEquals(18, limit.requestedSize)
        assertEquals(
            JsxGraphPoint2D(-1.0, -1.0),
            scenePoint(session.scene, "driver").coordinates,
        )
        assertEquals(9, sceneCurve(session.scene, "comb").points.size)
    }

    @Test
    fun inequalityFunctionAttributeRendersAndUpdatesThroughPublicSession() {
        val session = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "inequality",
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
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
                inequality(source) <<
                    id: "inequality", name: "", withLabel: false,
                    inverse: function () {
                        return driver.X() > 0;
                    }
                >>;
                """.trimIndent(),
            ),
        )
        val initialInequality = sceneCurve(initial, "inequality")
        assertEquals(5, initialInequality.points.size)
        assertEquals(
            JsxGraphColor.Transparent,
            initialInequality.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            initialInequality.style.fillColor,
        )
        assertEquals(0.2, initialInequality.style.fillOpacity)
        val initialSecond = assertIs<JsxGraphPoint2D>(
            initialInequality.points[1],
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(1.0, 0.0),
            ),
        ).value
        val movedInequality = sceneCurve(moved, "inequality")
        val movedSecond = assertIs<JsxGraphPoint2D>(
            movedInequality.points[1],
        )
        assertNotEquals(initialSecond, movedSecond)
        assertEquals(
            JsxGraphPoint2D(1.0, 0.0),
            scenePoint(moved, "driver").coordinates,
        )
    }

    @Test
    fun vectorFieldFunctionsRenderAndDynamicLimitRollsBack() {
        val session = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "vectorfield",
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
                limits = JsxGraphJessieCodeLimits(
                    maxCurvePoints = 15,
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
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
                        function () { return driver.X(); },
                        2
                    ],
                    [-1, 1, 1]
                ) <<
                    id: "field", name: "", withLabel: false,
                    scale: 0.5,
                    arrowHead: << enabled: false >>
                >>;
                """.trimIndent(),
            ),
        )
        val initialField = sceneCurve(initial, "field")
        assertEquals(12, initialField.points.size)
        assertEquals(0.5, initialField.style.strokeWidth)
        assertEquals(
            JsxGraphPoint2D(-2.5, 0.0),
            assertIs<JsxGraphPoint2D>(initialField.points[1]),
        )

        val limit = assertIs<
            GMResult.Err<JsxGraphInteractionError.ResourceLimitExceeded>,
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(2.0, 0.0),
            ),
        ).error
        assertEquals("curve point count", limit.resource)
        assertEquals(15, limit.limit)
        assertEquals(18, limit.requestedSize)
        assertEquals(
            JsxGraphPoint2D(1.0, 0.0),
            scenePoint(session.scene, "driver").coordinates,
        )
        assertEquals(12, sceneCurve(session.scene, "field").points.size)
    }

    @Test
    fun slopeFieldFunctionRendersAndDynamicLimitRollsBack() {
        val session = assertIs<
            GMResult.Ok<JsxGraphJessieCodeSession>,
            >(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "slopefield",
                    boundingBox =
                        JsxGraphBoundingBox(-8.0, 6.0, 8.0, -6.0),
                ),
                limits = JsxGraphJessieCodeLimits(
                    maxCurvePoints = 15,
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
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
                        function () { return driver.X(); },
                        2
                    ],
                    [-1, 1, 1]
                ) <<
                    id: "field", name: "", withLabel: false,
                    scale: 0.5
                >>;
                """.trimIndent(),
            ),
        )
        val initialField = sceneCurve(initial, "field")
        assertEquals(12, initialField.points.size)
        assertEquals(0.5, initialField.style.strokeWidth)
        assertFalse(
            assertIs<JsxGraphVectorField>(
                initialField.vectorField,
            ).arrowEnabled,
        )
        assertEquals(
            JsxGraphPoint2D(
                -1.6464466094067263,
                -1.3535533905932737,
            ),
            assertIs<JsxGraphPoint2D>(initialField.points[1]),
        )

        val limit = assertIs<
            GMResult.Err<JsxGraphInteractionError.ResourceLimitExceeded>,
            >(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(2.0, 0.0),
            ),
        ).error
        assertEquals("curve point count", limit.resource)
        assertEquals(15, limit.limit)
        assertEquals(18, limit.requestedSize)
        assertEquals(
            JsxGraphPoint2D(1.0, 0.0),
            scenePoint(session.scene, "driver").coordinates,
        )
        assertEquals(12, sceneCurve(session.scene, "field").points.size)
    }

    @Test
    fun ticksRenderFromTheSameSourceAndTrackParentMovement() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(
                boardOptions = JsxGraphJessieCodeBoardOptions(
                    containerId = "ticks",
                    boundingBox =
                        JsxGraphBoundingBox(-5.0, 5.0, 5.0, -5.0),
                ),
            ),
        ).value
        val initial = scene(
            session.execute(
                """
                A = point(-2, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(2, 0) <<
                    id: "B", name: "", withLabel: false
                >>;
                line = segment(A, B) <<
                    id: "line", name: "", withLabel: false
                >>;
                fixed = ticks(
                    line,
                    [-10, -1, 0, 1, 10]
                ) <<
                    id: "fixed", name: "",
                    drawLabels: true,
                    labels: [
                        "out-left", "minus", "zero",
                        "plus", "out-right"
                    ]
                >>;
                numeric = ticks(line, 2) <<
                    id: "numeric", name: "",
                    drawLabels: true,
                    drawZero: true,
                    minorTicks: 0
                >>;
                curve = functiongraph("x * x", -2, 2) <<
                    id: "curve", name: "", withLabel: false,
                    doAdvancedPlot: false,
                    numberPointsHigh: 8
                >>;
                curveTicks = ticks(curve, [0, 1, 2, 4]) <<
                    id: "curveTicks", name: "",
                    drawLabels: true,
                    labels: ["left", "inside", "middle", "right"]
                >>;
                """.trimIndent(),
            ),
        )

        val fixed = resolveTicks(sceneTicks(initial, "fixed"))
        assertEquals(1, fixed.paths.size)
        assertEquals(JsxGraphPoint2D(-1.0, 0.0), fixed.paths[0].points[1])
        assertEquals("plus", fixed.labels.single().content)

        val numeric = resolveTicks(sceneTicks(initial, "numeric"))
        assertEquals(
            listOf(-1.0, 0.0, 1.0),
            numeric.paths.map { it.points[1].x },
        )
        assertEquals(
            listOf("1", "2", "3"),
            numeric.labels.map(JsxGraphTickLabel::content),
        )

        val curve = resolveTicks(sceneTicks(initial, "curveTicks"))
        assertEquals(4, curve.paths.size)
        assertEquals(
            listOf("left", "inside", "middle", "right"),
            curve.labels.map(JsxGraphTickLabel::content),
        )
        assertEquals(
            -1.9038476052359176,
            curve.paths[0].points[0].x,
            absoluteTolerance = 1.0e-12,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(2.0, 2.0),
            ),
        ).value
        val movedFixed = resolveTicks(sceneTicks(moved, "fixed"))
        assertEquals(
            -1.1055728090000843,
            movedFixed.paths.single().points[1].x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.4472135954999579,
            movedFixed.paths.single().points[1].y,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals("plus", movedFixed.labels.single().content)
    }

    @Test
    fun ticksInheritParentVisibilityUnlessExplicitlyOverridden() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                """
                line = segment([-2, 0], [2, 0]) <<
                    id: "line", name: "", withLabel: false,
                    visible: false
                >>;
                defaultTicks = ticks(line, [0]) <<
                    id: "defaultTicks", name: ""
                >>;
                inheritedTicks = ticks(line, [0]) <<
                    id: "inheritedTicks", name: "",
                    visible: "inherit"
                >>;
                visibleTicks = ticks(line, [0]) <<
                    id: "visibleTicks", name: "",
                    visible: true
                >>;
                """.trimIndent(),
            ),
        )

        assertFalse(sceneTicks(scene, "defaultTicks").style.visible)
        assertFalse(sceneTicks(scene, "inheritedTicks").style.visible)
        assertTrue(sceneTicks(scene, "visibleTicks").style.visible)
    }

    @Test
    fun ticksFunctionParentFailsThroughThePublicApi() {
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                """
                A = point(-2, 0);
                B = point(2, 0);
                line = segment(A, B);
                ticks(line, function () { return 1; });
                """.trimIndent(),
            ),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(error)
        assertTrue("FunctionArgumentsNoLongerSupported" in runtime.reason)
    }

    @Test
    fun ticksRejectExcessiveFixedTickCountAsResourceLimit() {
        val fixedTicks = (0..2048).joinToString(",")
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                """
                line = segment([-2, 0], [2, 0]) <<
                    id: "line", name: "", withLabel: false
                >>;
                ticks(line, [$fixedTicks]) <<
                    id: "ticks", name: ""
                >>;
                """.trimIndent(),
            ),
        ).error
        val limit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(error)

        assertEquals("tick count", limit.resource)
        assertEquals(2048, limit.limit)
        assertEquals(2049, limit.requestedSize)
    }

    @Test
    fun unsupportedOrExcessiveSceneAttributesFailExplicitly() {
        val unsupported = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                """
                point(0, 0) <<
                    id: "A", name: "", withLabel: false,
                    custom: function () { return 1; }
                >>;
                """.trimIndent(),
            ),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(unsupported)
        assertTrue("UnsupportedSceneAttribute" in runtime.reason)

        val unsupportedVectorFieldArrow =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    """
                    vectorfield(
                        ["y", "-x"],
                        [-1, 1, 1],
                        [-1, 1, 1]
                    ) <<
                        id: "field", name: "", withLabel: false,
                        arrowHead: << type: 2 >>
                    >>;
                    """.trimIndent(),
                ),
            ).error
        val vectorFieldScene = assertIs<JsxGraphJessieCodeError.Scene>(
            unsupportedVectorFieldArrow,
        )
        val unsupportedArrowAttribute =
            assertIs<JsxGraphDocumentError.UnsupportedAttribute>(
                vectorFieldScene.error,
            )
        assertEquals("arrowhead.type", unsupportedArrowAttribute.attribute)

        val excessiveDepth =
            assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
                JsxGraphJessieCode.parse(
                    """
                    point(0, 0) <<
                        id: "A", name: "", withLabel: false,
                        custom: << nested: << value: 1 >> >>
                    >>;
                    """.trimIndent(),
                    limits = JsxGraphJessieCodeLimits(
                        maxAttributeDepth = 2,
                    ),
                ),
            ).error
        assertResourceLimit(
            excessiveDepth,
            resource = "scene attribute depth",
            requestedSize = 3,
        )
    }

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(result).value

    private fun segmentLength(
        scene: JsxGraphScene,
        id: String,
    ): Double {
        val segment = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { it.id == id },
        )
        val dx = segment.point2.x - segment.point1.x
        val dy = segment.point2.y - segment.point1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun scenePoint(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneLine(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneCurve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneTicks(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Ticks =
        assertIs(scene.elements.single { it.id == id })

    private fun resolveTicks(
        ticks: JsxGraphSceneElement.Ticks,
    ): JsxGraphResolvedTicks =
        ticks.definition.resolve(
            visibleLeft = -5.0,
            visibleTop = 5.0,
            visibleRight = 5.0,
            visibleBottom = -5.0,
            cssPixelsPerUnitX = 50.0,
            cssPixelsPerUnitY = 50.0,
        )

    private fun assertPointCoordinates(
        expected: JsxGraphPoint2D,
        actual: JsxGraphPoint2D,
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.x, actual.x, absoluteTolerance = tolerance)
        assertEquals(expected.y, actual.y, absoluteTolerance = tolerance)
    }

    private fun assertParallelLine(
        baseLine: JsxGraphSceneElement.Line,
        parallel: JsxGraphSceneElement.Line,
        through: JsxGraphPoint2D,
    ) {
        val baseDx = baseLine.point2.x - baseLine.point1.x
        val baseDy = baseLine.point2.y - baseLine.point1.y
        val parallelDx = parallel.point2.x - parallel.point1.x
        val parallelDy = parallel.point2.y - parallel.point1.y
        assertEquals(
            0.0,
            baseDx * parallelDy - baseDy * parallelDx,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.0,
            (through.x - parallel.point1.x) * parallelDy -
                (through.y - parallel.point1.y) * parallelDx,
            absoluteTolerance = 1.0e-12,
        )
    }

    private fun assertResourceLimit(
        error: JsxGraphJessieCodeError,
        resource: String,
        requestedSize: Long,
    ) {
        val limit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(error)
        assertEquals(resource, limit.resource)
        assertEquals(requestedSize, limit.requestedSize)
    }
}
