package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphGridForceSquare
import com.swithun.jsxgraph.core.JsxGraphGridMinorElements
import com.swithun.jsxgraph.core.JsxGraphGridPair
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JessieCodeElementMethodMapTest {
    @Test
    fun boundsUsesTheTranslatedElementImplementations() {
        val values = array(
            evaluate(
                source =
                    "A = point(1, 2); B = point(3, 4); " +
                        "l = line(A, B); c = circle(A, 2); " +
                        "[A.Bounds(), l.Bounds(), c.Bounds()];",
                board = board(),
            ),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 1.0, 2.0),
            numbers(array(values.values[0])),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 3.0, 2.0),
            numbers(array(values.values[1])),
        )
        assertContentEquals(
            doubleArrayOf(-1.0, 4.0, 3.0, 0.0),
            numbers(array(values.values[2])),
        )
    }

    @Test
    fun addChildReturnsTheOwnerAndUpdatesDependencyMaps() {
        val board = board()
        val value = element(
            evaluate(
                source =
                    "A = point(1, 2); B = point(3, 4); " +
                        "A.addChild(B);",
                board = board,
            ),
        )
        val child = assertIs<Point>(board.select("B"))

        assertSame(board.select("A"), value)
        assertSame(child, value.childElements[child.id])
        assertSame(value, child.ancestors[value.id])
    }

    @Test
    fun setNameEscapesMarkupAndReturnsUndefined() {
        val board = board()
        val value = evaluate(
            source = "A = point(1, 2); A.setName(\"<B>\");",
            board = board,
        )

        assertSame(JessieCodeRuntimeValue.UndefinedValue, value)
        assertNull(board.select("A"))
        assertIs<Point>(board.select("&lt;B&gt;"))
    }

    @Test
    fun moveAndMoveToUseTheImmediateBoardUpdatePath() {
        val board = board()
        val moved = assertIs<JessieCodeRuntimeValue.BoardReference>(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "D = point(\"A.X() + 1\", \"A.Y() + 2\"); " +
                        "A.move([4, 5]);",
                board = board,
            ),
        )
        val point = assertIs<Point>(board.select("A"))
        val dependent = assertIs<Point>(board.select("D"))

        assertSame(board, moved.board)
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 5.0),
            point.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 7.0),
            dependent.coords.usrCoords,
        )

        val movedAgain = assertIs<JessieCodeRuntimeValue.BoardReference>(
            evaluate(
                source = "A.moveTo([6, 7], 0);",
                board = board,
            ),
        )
        assertSame(board, movedAgain.board)
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 7.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun addConstraintReturnsThePointAndTracksDependencies() {
        val board = board()
        val point = assertIs<Point>(
            element(
                evaluate(
                    source =
                        "A = point(1, 2); B = point(3, 4); " +
                            "A.addConstraint([\"B.X() + 1\", 2]);",
                    board = board,
                ),
            ),
        )
        val driver = assertIs<Point>(board.select("B"))

        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 2.0),
            point.coords.usrCoords,
        )
        assertSame(point, driver.childElements[point.id])

        driver.setPosition(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(8.0, 9.0),
        )
        board.update()
        assertContentEquals(
            doubleArrayOf(1.0, 9.0, 2.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun animatedMovementAndInvalidArgumentsAreStructured() {
        val animationBoard = board("animation")
        val animation = assertIs<JessieCodeRuntimeValue.BoardReference>(
            evaluate(
                source =
                    "A = point(1, 2); " +
                        "A.move([4, 5], 100, << effect: \"<>\" >>);",
                board = animationBoard,
            ),
        )
        assertSame(animationBoard, animation.board)
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 5.0),
            assertIs<Point>(animationBoard.select("A"))
                .coords.usrCoords,
        )

        val invalidDuration = evaluateError(
            source = "A = point(1, 2); A.move([4, 5], -1);",
            board = board("invalid-duration"),
        )
        assertEquals(
            "moveTo",
            assertIs<JessieCodeRuntimeError.ElementMethodUnavailable>(
                invalidDuration,
            ).method,
        )

        val coordinateCount = evaluateError(
            source = "A = point(1, 2); A.move([4]);",
            board = board("coordinate-count"),
        )
        assertEquals(
            "2 or 3",
            assertIs<JessieCodeRuntimeError.InvalidArgumentCount>(
                coordinateCount,
            ).expected,
        )

        val constraintType = evaluateError(
            source =
                "A = point(1, 2); " +
                    "A.addConstraint([1, << value: 2 >>]);",
            board = board("constraint-type"),
        )
        assertEquals(
            "object",
            assertIs<JessieCodeRuntimeError.InvalidArgumentType>(
                constraintType,
            ).actual,
        )
    }

    @Test
    fun arcAndSectorMethodsExposeTranslatedMeasurementsAndPoints() {
        val values = array(
            evaluate(
                source =
                    "A = point(-4, -1); B = point(1, 4); " +
                        "C = point(5, -2); " +
                        "a = arc(B, A, C); " +
                        "n = nonreflexangle(A, B, C) << radius: 2 >>; " +
                        "r = reflexangle(A, B, C) << radius: 2 >>; " +
                        "[a.Value(\"radians\"), n.Value(), " +
                        "r.Value(\"degrees\"), n.Radius(), V(n), " +
                        "n.center == B, n.point2 == A, " +
                        "n.Value(\"turn\")];",
                board = board(),
            ),
        )

        assertEquals(
            1.3734007669450157,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            1.3734007669450157,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[1],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            281.30993247402023,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[2],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(2.0),
            values.values[3],
        )
        assertEquals(values.values[1], values.values[4])
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[5],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[6],
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[7],
        )
    }

    @Test
    fun arcAndSectorLengthAreaPerimeterAndRadiusMutationAreCallable() {
        val values = array(
            evaluate(
                source =
                    "O = point(0, 0); A = point(3, 0); " +
                        "B = point(0, 3); " +
                        "a = arc(O, A, B); s = sector(O, A, B); " +
                        "before = [a.L(), L(a), s.L(), L(s), " +
                        "s.Area(), Area(s), s.Perimeter(), Perimeter(s), " +
                        "Radius(a), Radius(s)]; " +
                        "same = s.setRadius(2) == s; " +
                        "after = [s.Radius(), s.L(), s.Area(), " +
                        "s.Perimeter(), same]; " +
                        "[before, after];",
                board = board(),
            ),
        )
        val before = array(values.values[0])
        val after = array(values.values[1])

        assertEquals(10, before.values.size)
        assertEquals(3.0 * kotlin.math.PI / 2.0, number(before.values[0]))
        assertEquals(before.values[0], before.values[1])
        assertEquals(before.values[0], before.values[2])
        assertEquals(before.values[2], before.values[3])
        assertEquals(9.0 * kotlin.math.PI / 4.0, number(before.values[4]))
        assertEquals(before.values[4], before.values[5])
        assertEquals(
            6.0 + 3.0 * kotlin.math.PI / 2.0,
            number(before.values[6]),
        )
        assertEquals(before.values[6], before.values[7])
        assertEquals(JessieCodeRuntimeValue.NumberValue(3.0), before.values[8])
        assertEquals(JessieCodeRuntimeValue.NumberValue(3.0), before.values[9])

        assertEquals(JessieCodeRuntimeValue.NumberValue(2.0), after.values[0])
        assertEquals(kotlin.math.PI, number(after.values[1]))
        assertEquals(kotlin.math.PI, number(after.values[2]))
        assertEquals(4.0 + kotlin.math.PI, number(after.values[3]))
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            after.values[4],
        )
    }

    @Test
    fun ellipseExposesItsMajorAxisAndCenterMetadata() {
        val values = array(
            evaluate(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); C = point(0, 5); " +
                        "e = ellipse(F1, F2, C) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "[e.majorAxis(), e.center == e.midpoint, " +
                        "e.subs.center == e.center];",
                board = board(),
            ),
        )

        assertEquals(
            11.661903789690601,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[1],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[2],
        )
    }

    @Test
    fun hyperbolaExposesItsMajorAxisAndCenterMetadata() {
        val values = array(
            evaluate(
                source =
                    "F1 = point(-3, 0); F2 = point(3, 0); C = point(5, 2); " +
                        "h = hyperbola(F1, F2, C) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "[h.majorAxis(), h.center == h.midpoint, " +
                        "h.subs.center == h.center];",
                board = board(),
            ),
        )

        assertEquals(
            5.417784126489131,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[1],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[2],
        )
    }

    @Test
    fun parabolaExposesCenterMetadataWithoutAMajorAxis() {
        val values = array(
            evaluate(
                source =
                    "F = point(1, 1); " +
                        "L = line([-1, 4], [-1, -4]); " +
                        "p = parabola(F, L) << " +
                        "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                        "[p.center == p.midpoint, " +
                        "p.subs.center == p.center];",
                board = board(),
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[0],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[1],
        )

        val unavailable = evaluateError(
            source =
                "F = point(1, 1); " +
                    "L = line([-1, 4], [-1, -4]); " +
                    "p = parabola(F, L) << " +
                    "doAdvancedPlot: false, numberPointsHigh: 8 >>; " +
                    "p.majorAxis;",
            board = board("parabola-major-axis"),
        )
        assertEquals(
            "majorAxis",
            assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
                unavailable,
            ).property,
        )
    }

    @Test
    fun tangentToExposesItsPublicPointAndPolarReferences() {
        val board = board("tangent-to-properties")
        val values = array(
            evaluate(
                source =
                    """
                    O = point(0, 0);
                    R = point(3, 0);
                    C = circle(O, R);
                    P = point(5, 1);
                    T = tangentto(C, P) <<
                        id: "tangent",
                        name: "",
                        point1: << id: "tangentPoint1", name: "" >>,
                        point2: << id: "tangentPoint2", name: "" >>,
                        polar: <<
                            id: "polar",
                            name: "",
                            point1: << id: "polarPoint1", name: "" >>,
                            point2: << id: "polarPoint2", name: "" >>
                        >>,
                        point: << id: "intersection", name: "" >>
                    >>;
                    [T.point, T.polar, T.point1, T.point2];
                    """.trimIndent(),
                board = board,
            ),
        )
        val tangent = assertIs<Line>(board.select("tangent"))
        val intersection = assertIs<IntersectionPoint>(
            element(values.values[0]),
        )
        val polar = assertIs<Line>(element(values.values[1]))

        assertSame(tangent.tangentToPoint, intersection)
        assertSame(tangent.tangentToPolar, polar)
        assertSame(tangent.point1, element(values.values[2]))
        assertSame(tangent.point2, element(values.values[3]))

        val plainLinePoint = evaluateError(
            source =
                "A = point(0, 0); B = point(1, 1); " +
                    "L = line(A, B); L.point;",
            board = board("plain-line-point"),
        )
        assertEquals(
            "point",
            assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
                plainLinePoint,
            ).property,
        )
        val plainLinePolar = evaluateError(
            source =
                "A = point(0, 0); B = point(1, 1); " +
                    "L = line(A, B); L.polar;",
            board = board("plain-line-polar"),
        )
        assertEquals(
            "polar",
            assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
                plainLinePolar,
            ).property,
        )
    }

    @Test
    fun riemannSumValueMethodAndValueBuiltinExposeCachedArea() {
        val values = array(
            evaluate(
                source =
                    """
                    f = function (x) { return x * x + 1; };
                    r = riemannsum(f, 3, "left", -1, 2);
                    [r.Value(), V(r)];
                    """.trimIndent(),
                board = board(),
            ),
        )

        assertEquals(
            5.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[0],
            ).value,
        )
        assertEquals(
            5.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[1],
            ).value,
        )
    }

    @Test
    fun sliderMethodMapExposesValueRangeAndHelperElements() {
        val board = board()
        val values = array(
            evaluate(
                source =
                    """
                    s = slider(
                        [-4, 0],
                        [4, 0],
                        [-10, 3, 10]
                    ) <<
                        id: "s", name: "",
                        withLabel: false, withTicks: false
                    >>;
                    initial = [s.Value(), V(s), s.smin, s.smax];
                    s.setMin(-20).setMax(20).setValue(-5);
                    [
                        initial,
                        s.Value(),
                        s.point1,
                        s.point2,
                        s.baseline,
                        s.highline,
                        s.ticks,
                        s.label
                    ];
                    """.trimIndent(),
                board = board,
            ),
        )

        assertContentEquals(
            doubleArrayOf(3.0, 3.0, -10.0, 10.0),
            numbers(array(values.values[0])),
        )
        assertEquals(
            -5.0,
            assertIs<JessieCodeRuntimeValue.NumberValue>(
                values.values[1],
            ).value,
        )
        listOf(2, 3, 4, 5).forEach { index ->
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                values.values[index],
            )
        }
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[6],
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[7],
        )
    }

    @Test
    fun genericElementMethodsMutateAttributesLabelsAndEvents() {
        val board = board("generic-element-methods")
        val values = array(
            evaluate(
                source =
                    """
                    A = point(1, 2);
                    hits = 0;
                    handler = function (value) {
                        hits = hits + value;
                    };
                    A.on("custom", handler);
                    A.trigger("custom", 2);
                    A.off("custom", handler);
                    A.trigger("custom", 4);
                    A.setAttribute(<<
                        strokeColor: "#123456",
                        visible: false
                    >>);
                    labelResult = A.setLabel("label");
                    [
                        hits,
                        A.strokeColor,
                        A.visible,
                        labelResult,
                        A.label
                    ];
                    """.trimIndent(),
                board = board,
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(2.0),
            values.values[0],
        )
        assertEquals(
            JessieCodeRuntimeValue.StringValue("#123456"),
            values.values[1],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(false),
            values.values[2],
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[3],
        )
        assertSame(
            assertNotNull(
                assertIs<Point>(board.select("A")).elementLabel,
            ),
            element(values.values[4]),
        )
    }

    @Test
    fun gridAndAxisAttributesMutateThroughTheFullUpdatePath() {
        val board = Board(
            originX = 250.0,
            originY = 250.0,
            unitX = 50.0,
            unitY = 50.0,
            id = "grid-runtime-mutation",
        )
        evaluate(
            source =
                """
                xAxis = axis([0, 0], [1, 0]) <<
                    id: "xAxis",
                    ticks: <<
                        ticksDistance: 2,
                        minorTicks: 1,
                        insertTicks: false
                    >>
                >>;
                yAxis = axis([0, 0], [0, 1]) <<
                    id: "yAxis",
                    ticks: <<
                        ticksDistance: 2.5,
                        minorTicks: 3,
                        insertTicks: false
                    >>
                >>;
                grid = grid(xAxis, yAxis) <<
                    id: "grid",
                    majorStep: "auto",
                    minorElements: "auto",
                    major: << face: "line" >>,
                    minor: << face: "point" >>
                >>;
                grid.setAttribute(<<
                    majorStep: [2, 1],
                    forceSquare: "max",
                    includeBoundaries: true,
                    face: "circle",
                    size: 10,
                    drawZero: << origin: false, x: true, y: false >>
                >>);
                grid.minorGrid.setAttribute(<<
                    minorElements: [1, 2],
                    includeBoundaries: true,
                    face: "plus",
                    size: 6
                >>);
                xAxis.setAttribute(<<
                    position: "fixed",
                    anchor: "right",
                    anchorDist: "20%",
                    ticks: <<
                        ticksDistance: 1.5,
                        minorTicks: 2,
                        labels: ["zero", "one"],
                        label: <<
                            offset: [8, -3],
                            anchorX: "middle",
                            anchorY: "top"
                        >>
                    >>
                >>);
                yAxis.setAttribute(<<
                    ticks: << ticksDistance: 3, minorTicks: 4 >>
                >>);
                grid.setAttribute(<<
                    majorStep: ["auto", "auto"],
                    forceSquare: false
                >>);
                grid.minorGrid.setAttribute(<<
                    minorElements: ["auto", "auto"]
                >>);
                """.trimIndent(),
            board = board,
        )

        val major = assertIs<Curve>(board.select("grid"))
        val minor = assertNotNull(major.minorGrid)
        val majorDefinition = assertNotNull(major.gridDefinition)
        val minorDefinition = assertNotNull(minor.gridDefinition)
        val xAxis = assertIs<Line>(board.select("xAxis"))
        val xTicks = assertNotNull(xAxis.defaultTicks)
        assertEquals(false, major.needsRegularUpdate)
        assertEquals(false, minor.needsRegularUpdate)
        assertEquals(
            JsxGraphGridForceSquare.None,
            majorDefinition.forceSquare,
        )
        assertEquals("line", majorDefinition.major.face)
        assertEquals("point", minorDefinition.minor.face)
        assertEquals(
            JsxGraphGridPair<Double?>(1.5, 3.0),
            majorDefinition.parentMajorStep,
        )
        assertEquals(
            JsxGraphGridPair<Double?>(2.0, 4.0),
            minorDefinition.parentMinorElements,
        )
        assertEquals(
            JsxGraphGridPair<JsxGraphGridMinorElements>(
                JsxGraphGridMinorElements.Auto,
                JsxGraphGridMinorElements.Auto,
            ),
            minorDefinition.minorElements,
        )
        assertEquals(
            -3.0,
            xAxis.point1.Y(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(1.5, xTicks.attributes.ticksDistance)
        assertEquals(2, xTicks.attributes.minorTicks)
        assertEquals(listOf("zero", "one"), xTicks.attributes.labels)
        assertContentEquals(
            doubleArrayOf(8.0, -3.0),
            xTicks.attributes.labelOffset,
        )
        assertEquals("middle", xTicks.attributes.labelAnchorX)
        assertEquals("top", xTicks.attributes.labelAnchorY)
        assertTrue(major.numberPoints > 0)
        assertTrue(minor.numberPoints > 0)
    }

    @Test
    fun boardMethodMapControlsViewportUpdatesEventsAndRemoval() {
        val board = board("board-method-map")
        val values = array(
            evaluate(
                source =
                    """
                    hits = 0;
                    handler = function (value) {
                        hits = hits + value;
                    };
                    ${'$'}board.on("custom", handler);
                    ${'$'}board.trigger("custom", 3);
                    ${'$'}board.off("custom", handler);
                    ${'$'}board.trigger("custom", 5);
                    sameView = ${'$'}board.setView(
                        [-2, 2, 2, -2],
                        false,
                        "reset"
                    ) == ${'$'}board;
                    suspended = ${'$'}board.suspendUpdate() == ${'$'}board;
                    resumed = ${'$'}board.unsuspendUpdate() == ${'$'}board;
                    A = point(1, 1);
                    removed = ${'$'}board.remove(A) == ${'$'}board;
                    [
                        hits,
                        sameView,
                        suspended,
                        resumed,
                        removed,
                        ${'$'}board.View()
                    ];
                    """.trimIndent(),
                board = board,
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(3.0),
            values.values[0],
        )
        for (index in 1..4) {
            assertEquals(
                JessieCodeRuntimeValue.BooleanValue(true),
                values.values[index],
            )
        }
        assertContentEquals(
            doubleArrayOf(-2.0, 2.0, 2.0, -2.0),
            numbers(array(values.values[5])),
        )
        assertNull(board.select("A"))
    }

    @Test
    fun textAutoPositionMethodMapUsesTranslatedConflictSearch() {
        val board = Board(
            originX = 100.0,
            originY = 100.0,
            unitX = 1.0,
            unitY = 1.0,
            canvasWidth = 200.0,
            canvasHeight = 200.0,
            id = "text-auto-position-method-map",
        )
        val values = array(
            evaluate(
                source =
                    """
                    A = point(0, 0);
                    B = point(25, 5.4);
                    A.setLabel("label");
                    A.label.setAttribute(<<
                        visible: true,
                        isLabel: true,
                        anchorX: "left",
                        anchorY: "bottom",
                        offset: [12, 0],
                        autoPositionMinDistance: 12,
                        autoPositionMaxDistance: 28
                    >>);
                    ${'$'}board.update();
                    same = A.label.setAutoPosition() == A.label;
                    [same, A.label.Size()];
                    """.trimIndent(),
                board = board,
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[0],
        )
        assertContentEquals(
            doubleArrayOf(27.0, 10.8),
            numbers(array(values.values[1])),
        )
        val label = assertNotNull(
            assertIs<Point>(board.select("A")).elementLabel,
        )
        val angle = 59.0 * kotlin.math.PI / 30.0
        assertEquals(
            12.0 * kotlin.math.cos(angle),
            label.screenOffset[0],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            12.0 * kotlin.math.sin(angle),
            label.screenOffset[1],
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun transformationAndCoordinateMutationMethodsUseNativeKernels() {
        val board = board("transformation-method-map")
        val values = array(
            evaluate(
                source =
                    """
                    A = point(1, 2);
                    B = point(0, 0);
                    lineHost = line([0, 0], [4, 0]);
                    shift = transform(2, 3) << type: "translate" >>;
                    scale = transform(2, 2) << type: "scale" >>;
                    applied = shift.apply(A);
                    once = shift.applyOnce(A);
                    bound = shift.bind(B);
                    ${'$'}board.update();
                    melted = shift.melt(scale) == shift;
                    C = point(1, 1);
                    glider = C.glide(lineHost) == C;
                    onPolygon = C.onPolygon;
                    freed = C.free() == C;
                    [
                        applied,
                        once,
                        A.Coords(),
                        bound,
                        B.Coords(),
                        melted,
                        glider,
                        onPolygon,
                        freed
                    ];
                    """.trimIndent(),
                board = board,
            ),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 5.0),
            numbers(array(values.values[0])),
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[1],
        )
        assertContentEquals(
            doubleArrayOf(3.0, 5.0),
            numbers(array(values.values[2])),
        )
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            values.values[3],
        )
        assertContentEquals(
            doubleArrayOf(2.0, 3.0),
            numbers(array(values.values[4])),
        )
        for (index in 5..6) {
            assertEquals(
                JessieCodeRuntimeValue.BooleanValue(true),
                values.values[index],
            )
        }
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(false),
            values.values[7],
        )
        assertEquals(
            JessieCodeRuntimeValue.BooleanValue(true),
            values.values[8],
        )
        val point = assertIs<Point>(board.select("C"))
        assertNull(point.slideObject)
        assertEquals(Const.OBJECT_TYPE_POINT, point.type)
    }

    @Test
    fun curveGenerateTermReplacesDynamicDataAndPolarEvaluators() {
        val continuousBoard = board("curve-generate-continuous")
        val continuousResult = evaluate(
            source =
                """
                A = point(1, 0);
                c = curve([0, 1], [0, 1]) << id: "c", name: "" >>;
                result = c.generateTerm(
                    "t",
                    function (t) { return t + A.X(); },
                    function (t) { return t * t; },
                    -1,
                    2
                );
                A.move([3, 0]);
                ${'$'}board.update();
                [result, c.MinX(), c.MaxX()];
                """.trimIndent(),
            board = continuousBoard,
        )
        val continuousValues = array(continuousResult)
        val continuous = assertIs<Curve>(continuousBoard.select("c"))

        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            continuousValues.values[0],
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(-1.0),
            continuousValues.values[1],
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(2.0),
            continuousValues.values[2],
        )
        assertEquals("parameter", continuous.curveType)
        assertEquals(3.0, continuous.X(0.0), absoluteTolerance = 1.0e-12)
        assertEquals(4.0, continuous.Y(2.0), absoluteTolerance = 1.0e-12)

        val polarBoard = board("curve-generate-polar")
        evaluate(
            source =
                """
                c = curve([0, 1], [0, 1]) << id: "c", name: "" >>;
                c.setTerm(
                    "phi",
                    function (phi) { return 2; },
                    [1, -1]
                );
                """.trimIndent(),
            board = polarBoard,
        )
        val polar = assertIs<Curve>(polarBoard.select("c"))
        assertEquals("polar", polar.curveType)
        assertEquals(0.0, polar.minX(), absoluteTolerance = 1.0e-12)
        assertEquals(
            2.0 * kotlin.math.PI,
            polar.maxX(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(3.0, polar.X(0.0), absoluteTolerance = 1.0e-12)
        assertEquals(-1.0, polar.Y(0.0), absoluteTolerance = 1.0e-12)

        val dataBoard = board("curve-generate-data")
        evaluate(
            source =
                """
                c = curve([0], [0]) << id: "c", name: "" >>;
                c.setTerm("t", [[1, 2], [3, 4]]);
                """.trimIndent(),
            board = dataBoard,
        )
        val data = assertIs<Curve>(dataBoard.select("c"))
        assertEquals("plot", data.curveType)
        assertContentEquals(doubleArrayOf(1.0, 3.0), data.dataX)
        assertContentEquals(doubleArrayOf(2.0, 4.0), data.dataY)

        evaluate(
            source =
                """
                c.setTerm(
                    "x",
                    [1, 2],
                    function (x) { return x * 10; }
                );
                """.trimIndent(),
            board = dataBoard,
        )
        val mixed = assertIs<Curve>(dataBoard.select("c"))
        assertContentEquals(doubleArrayOf(1.0, 2.0), mixed.dataX)
        assertContentEquals(doubleArrayOf(2.0, 4.0), mixed.dataY)
        assertEquals(
            2.0,
            mixed.points[0].usrCoords[2],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            4.0,
            mixed.points[1].usrCoords[2],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(10.0, mixed.Y(1.0), absoluteTolerance = 1.0e-12)
    }

    @Test
    fun curveGenerateTermCompileFailureKeepsPreviousState() {
        val board = board("curve-generate-atomic")
        evaluate(
            source =
                """
                c = curve([1, 3], [2, 4]) <<
                    id: "c",
                    name: ""
                >>;
                """.trimIndent(),
            board = board,
        )
        val curve = assertIs<Curve>(board.select("c"))
        val xBefore = curve.dataX?.copyOf()
        val yBefore = curve.dataY?.copyOf()

        val error = evaluateError(
            source = "c = c; c.setTerm(\"t\", \"t +\", \"t\");",
            board = board,
        )

        assertEquals(
            "generateTerm",
            assertIs<JessieCodeRuntimeError.ElementMethodUnavailable>(
                error,
            ).method,
        )
        assertContentEquals(xBefore, curve.dataX)
        assertContentEquals(yBefore, curve.dataY)
        assertEquals("plot", curve.curveType)
    }

    @Test
    fun vectorAndSlopeFieldSetFReuseTheUpstreamRuntimeContract() {
        val vectorBoard = board("vector-set-f")
        val vectorResult = element(
            evaluate(
                source =
                    """
                    f = vectorfield(
                        [function (x, y) { return 1; }, 0],
                        [2, 0, 2],
                        [3, 0, 3]
                    ) <<
                        id: "f",
                        name: "",
                        arrowHead: << enabled: false >>
                    >>;
                    result = f.setF(["v", "u"], "u, v");
                    ${'$'}board.update();
                    result;
                    """.trimIndent(),
                board = vectorBoard,
            ),
        )
        val vector = assertIs<Curve>(vectorBoard.select("f"))

        assertSame(vector, vectorResult)
        assertContentEquals(
            doubleArrayOf(2.0, 5.0, Double.NaN),
            vector.dataX,
        )
        assertContentEquals(
            doubleArrayOf(3.0, 5.0, Double.NaN),
            vector.dataY,
        )

        val xBefore = vector.dataX?.copyOf()
        val yBefore = vector.dataY?.copyOf()
        val compileError = evaluateError(
            source = "f.setF([\"u +\", \"v\"], \"u, v\");",
            board = vectorBoard,
        )
        assertEquals(
            "setF",
            assertIs<JessieCodeRuntimeError.ElementMethodUnavailable>(
                compileError,
            ).method,
        )
        assertContentEquals(xBefore, vector.dataX)
        assertContentEquals(yBefore, vector.dataY)

        val slopeBoard = board("slope-set-f")
        val slopeResult = evaluate(
            source =
                """
                f = slopefield(
                    function (x, y) { return 0; },
                    [2, 0, 2],
                    [3, 0, 3]
                ) << id: "f", name: "" >>;
                result = f.setF(function (x, y) { return 1; });
                ${'$'}board.update();
                result;
                """.trimIndent(),
            board = slopeBoard,
        )
        val slope = assertIs<Curve>(slopeBoard.select("f"))
        val slopeX = assertNotNull(slope.dataX)
        val slopeY = assertNotNull(slope.dataY)

        assertSame(JessieCodeRuntimeValue.UndefinedValue, slopeResult)
        assertEquals(
            2.0 + 1.0 / kotlin.math.sqrt(2.0),
            slopeX[1],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            3.0 + 1.0 / kotlin.math.sqrt(2.0),
            slopeY[1],
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun methodFailuresDoNotChangeObjectAndArrayAssignment() {
        val values = array(
            evaluate(
                source =
                    "o = << value: 1 >>; a = [2]; " +
                        "o.value = 3; a[0] = 4; [o.value, a[0]];",
                board = board(),
            ),
        )

        assertEquals(
            listOf<JessieCodeRuntimeValue>(
                JessieCodeRuntimeValue.NumberValue(3.0),
                JessieCodeRuntimeValue.NumberValue(4.0),
            ),
            values.values,
        )
    }

    private fun evaluate(
        source: String,
        board: Board,
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        val result = JessieCodeEvaluator().evaluate(
            ast,
            JessieCodeRuntimeEnvironment(board = board),
        )
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            result,
            result.toString(),
        ).value
    }

    private fun evaluateError(
        source: String,
        board: Board,
    ): JessieCodeRuntimeError {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Err<JessieCodeRuntimeError>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).error
    }

    private fun numbers(
        value: JessieCodeRuntimeValue.ArrayValue,
    ): DoubleArray = value.values.map {
        assertIs<JessieCodeRuntimeValue.NumberValue>(it).value
    }.toDoubleArray()

    private fun number(
        value: JessieCodeRuntimeValue,
    ): Double = assertIs<JessieCodeRuntimeValue.NumberValue>(value).value

    private fun array(
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue = assertIs(value)

    private fun element(
        value: JessieCodeRuntimeValue,
    ) = assertIs<JessieCodeRuntimeValue.ElementReference>(value).element

    private fun board(id: String = "method-map"): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )
}
