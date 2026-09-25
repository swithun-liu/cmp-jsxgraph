package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphJessieCode3DTest {
    @Test
    fun nativeView3DAutomaticallyExpandsBorderAxes() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    axesPosition: "border",
                    xAxisBorder: << ticks3d: << drawLabels: false >> >>,
                    yAxisBorder: << ticks3d: << drawLabels: false >> >>,
                    zAxisBorder: << ticks3d: << drawLabels: false >> >>,
                    xPlaneRear: << visible: false >>,
                    yPlaneRear: << visible: false >>,
                    zPlaneRear: << visible: false >>
                >>;
                """.trimIndent(),
            ),
        ).value

        assertEquals(27, scene.elements.size)
        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
                .count { it.ticks3D != null },
        )
        assertEquals(
            15,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Line>()
                .size,
        )
        assertEquals(
            3,
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
                .count { it.style.strokeColor == JsxGraphColor(154, 154, 154) },
        )
    }

    @Test
    fun nativeText3DCreatorProjectsDynamicCoordinates() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    az: << slider: << start: 1 >> >>,
                    el: << slider: << start: 0.3 >> >>,
                    bank: << slider: << start: 0 >> >>,
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                x = -2;
                label = text3d(
                    view,
                    [function() { return x; }, 1.519615242270663, -1],
                    "-2"
                ) <<
                    id: "label",
                    name: "",
                    anchorX: "middle",
                    anchorY: "middle"
                >>;
                """.trimIndent(),
            ),
        ).value
        val initialLabel = assertIs<JsxGraphSceneElement.Text>(
            initial.elements.single { it.id == "label" },
        )
        assertCoordinates(
            expectedX = 0.22641937518774236,
            expectedY = -2.233304675143941,
            actual = initialLabel.coordinates,
        )

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("x = 0;"),
        ).value
        val updatedLabel = assertIs<JsxGraphSceneElement.Text>(
            updated.elements.single { it.id == "label" },
        )
        assertCoordinates(
            expectedX = -0.6467091510951717,
            expectedY = -2.584926429716491,
            actual = updatedLabel.coordinates,
        )
    }

    @Test
    fun nativeTicks3DCreatorBuildsBrokenCurveAndUpdatesFunctions() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    az: << slider: << start: 1 >> >>,
                    el: << slider: << start: 0.3 >> >>,
                    bank: << slider: << start: 0 >> >>,
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                offset = 1;
                ticks = ticks3d(
                    view,
                    function() { return [1, -2, offset, -1]; },
                    [2, 0, 0],
                    4,
                    [0, 1, 0]
                ) <<
                    id: "ticks",
                    name: "",
                    ticksDistance: 2,
                    majorHeight: 12,
                    tickEndings: [0.25, 0.75],
                    drawLabels: false
                >>;
                """.trimIndent(),
            ),
        ).value

        val initialTicks = assertIs<JsxGraphSceneElement.Curve>(
            initial.elements.single { it.id == "ticks" },
        )
        assertEquals("ticks", initialTicks.id)
        assertEquals(9, initialTicks.points.size)
        assertEquals(null, initialTicks.points[2])

        val updated = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute("offset = 2;"),
        ).value
        val updatedTicks = assertIs<JsxGraphSceneElement.Curve>(
            updated.elements.single { it.id == "ticks" },
        )
        assertEquals(9, updatedTicks.points.size)
        kotlin.test.assertNotEquals(
            initialTicks.points.first(),
            updatedTicks.points.first(),
        )
    }

    @Test
    fun nativeAxis3DCreatorUsesLine3DWithOfficialArrowDefault() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                axis = axis3d(
                    view,
                    [-5, 6, -3],
                    [5, 6, -3]
                ) <<
                    id: "axis",
                    name: "",
                    withLabel: false
                >>;
                """.trimIndent(),
            ),
        ).value

        val axis = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { it.id == "axis" },
        )
        assertEquals("axis", axis.id)
        assertEquals(null, axis.firstArrow)
        assertEquals(1, requireNotNull(axis.lastArrow).type)
        assertEquals(6.0, requireNotNull(axis.lastArrow).size)
    }

    @Test
    fun nativePlane3DCreatorProjectsOfficialWireframeOutline() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    az: << slider: << start: 1 >> >>,
                    el: << slider: << start: 0.3 >> >>,
                    bank: << slider: << start: 0 >> >>,
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                plane = plane3d(
                    view,
                    [1, 2, 2],
                    [1, 0, 0],
                    [0, 1, 1],
                    [-2, 3],
                    [-1, 2]
                ) <<
                    id: "plane",
                    name: "",
                    withLabel: false,
                    type: "wireframe",
                    mesh3d: << visible: false >>
                >>;
                """.trimIndent(),
            ),
        ).value

        val plane = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { it.id == "plane" },
        )
        assertEquals("plane", plane.id)
        assertEquals(5, plane.points.size)
        assertCoordinates(
            expectedX = -0.36429513147350645,
            expectedY = -0.8348490315008023,
            actual = requireNotNull(plane.points[0]),
        )
        assertCoordinates(
            expectedX = -2.5471164471807914,
            expectedY = -1.713903417932177,
            actual = requireNotNull(plane.points[1]),
        )
        assertCoordinates(
            expectedX = plane.points.first()!!.x,
            expectedY = plane.points.first()!!.y,
            actual = requireNotNull(plane.points.last()),
        )
        assertEquals(
            JsxGraphColor(187, 187, 187),
            plane.style.fillColor,
        )
        assertEquals(0.3, plane.style.fillOpacity)
        val mesh = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.points.size == 58 }
        assertFalse(mesh.style.visible)
        assertEquals(JsxGraphColor(154, 154, 154), mesh.style.strokeColor)
        assertEquals(0.6, mesh.style.strokeOpacity)
        assertEquals(12, mesh.style.layer)
    }

    @Test
    fun nativeMesh3DCreatorAndPlaneInheritanceMatchOfficialDefaults() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
            """
            view = view3d(
                [-5, -4],
                [8, 7],
                [[-5, 5], [-4, 6], [-3, 7]]
            ) <<
                name: "",
                projection: "parallel",
                $HIDDEN_DEFAULT_AXES_ATTRIBUTES
            >>;
            plane3d(
                view,
                [1, 2, 2],
                [1, 0, 0],
                [0, 1, 1],
                [-2, 3],
                [-1, 2]
            ) <<
                id: "plane",
                name: "",
                withLabel: false,
                type: "wireframe"
            >>;
            mesh3d(
                view,
                [1, 1, 2, 2],
                [0, 1, 0, 0],
                [0, 0, 1, 1],
                [-2, 3],
                [-1, 2]
            ) <<
                id: "mesh",
                name: "",
                stepWidthU: 2
            >>;
            """.trimIndent(),
            ),
        ).value

        val planeMesh = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { it.id != "mesh" && it.points.size == 58 }
        assertTrue(planeMesh.style.visible)
        val directMesh = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { it.id == "mesh" },
        )
        assertEquals(31, directMesh.points.size)
        assertEquals(
            JsxGraphColor(154, 154, 154),
            directMesh.style.strokeColor,
        )
        assertEquals(0.6, directMesh.style.strokeOpacity)
        assertEquals(12, directMesh.style.layer)

        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    view = view3d(
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                    ) <<
                        name: "",
                        projection: "parallel",
                        $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                    >>;
                    mesh3d(
                        view,
                        [1, 1, 2, 2],
                        [0, 1, 0, 0],
                        [0, 0, 1, 1],
                        [-2, 3],
                        [-1, 2]
                    ) << id: "mesh", name: "", stepWidthU: 2 >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 30),
            ),
        ).error
        val limit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(error)
        assertEquals("curve point count", limit.resource)
        assertEquals(31, limit.requestedSize)
    }

    @Test
    fun nativeLine3DCreatorProjectsOfficialTwoPointAndDirectionForms() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    az: << slider: << start: 1 >> >>,
                    el: << slider: << start: 0.3 >> >>,
                    bank: << slider: << start: 0 >> >>,
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                twoPoint = line3d(
                    view,
                    [1, 2, 2],
                    [2, -1, 3]
                ) <<
                    id: "twoPoint",
                    name: "",
                    withLabel: false,
                    straightFirst: true,
                    straightLast: true
                >>;
                direction = line3d(
                    view,
                    [-2, 3, 1],
                    [0, 2, -1],
                    [-Infinity, 2]
                ) <<
                    id: "direction",
                    name: "",
                    withLabel: false
                >>;
                """.trimIndent(),
            ),
        ).value

        val twoPoint = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { it.id == "twoPoint" },
        )
        assertEquals("twoPoint", twoPoint.id)
        assertCoordinates(
            expectedX = 2.5450641996710544,
            expectedY = -1.9063945685192492,
            actual = twoPoint.point1,
        )
        assertCoordinates(
            expectedX = -5.709235568048273,
            expectedY = 0.8878474942970511,
            actual = twoPoint.point2,
        )
        assertEquals(JsxGraphColor(0, 0, 0), twoPoint.style.strokeColor)
        assertEquals(1.0, twoPoint.style.strokeWidth)
        assertEquals(12, twoPoint.style.layer)
        kotlin.test.assertFalse(twoPoint.straightFirst)
        kotlin.test.assertFalse(twoPoint.straightLast)

        val direction = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { it.id == "direction" },
        )
        assertEquals("direction", direction.id)
        assertCoordinates(
            expectedX = -3.5264142523409885,
            expectedY = 2.1046133296343275,
            actual = direction.point1,
        )
        assertCoordinates(
            expectedX = 3.272671304906816,
            expectedY = -2.4013698204892284,
            actual = direction.point2,
        )
    }

    @Test
    fun nativeCreatorsBuildViewPointAndTransformLifecycle() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                source = point3d(
                    view,
                    1,
                    2,
                    2
                ) <<
                    id: "source",
                    name: "",
                    withLabel: false,
                    size: 4
                >>;
                translation = transform3d(
                    view,
                    2,
                    -3,
                    4
                ) << type: "translate" >>;
                transformed = point3d(
                    view,
                    source,
                    translation
                ) <<
                    id: "transformed",
                    name: "",
                    withLabel: false,
                    fixed: true
                >>;
                """.trimIndent(),
            ),
        ).value

        assertCoordinates(
            expectedX = -0.7566557074166769,
            expectedY = -0.7886977433927291,
            point = assertIs<JsxGraphSceneElement.Point>(
                scene.elements.single { it.id == "source" },
            ),
        )
        assertCoordinates(
            expectedX = -3.669509900873932,
            expectedY = 1.9000326916012957,
            point = assertIs<JsxGraphSceneElement.Point>(
                scene.elements.single { it.id == "transformed" },
            ),
        )
    }

    @Test
    fun dynamicPoint3DCoordinatesReevaluateInPersistentSession() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val initial = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    name: "",
                    projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                driver = point(2, 3) <<
                    id: "driver", name: "", withLabel: false
                >>;
                dynamic = point3d(
                    view,
                    function () {
                        return [
                            driver.X(),
                            driver.Y(),
                            driver.X() - driver.Y()
                        ];
                    }
                ) << id: "dynamic", name: "", withLabel: false, fixed: true >>;
                """.trimIndent(),
            ),
        ).value
        val original = assertIs<JsxGraphSceneElement.Point>(
            initial.elements.single { it.id == "dynamic" },
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(-2.0, 1.0),
            ),
        ).value
        val updated = assertIs<JsxGraphSceneElement.Point>(
            moved.elements.single { it.id == "dynamic" },
        )
        assertEquals("dynamic", updated.id)
        kotlin.test.assertNotEquals(
            original.coordinates,
            updated.coordinates,
        )
    }

    @Test
    fun defaultCenterAxesFailExplicitlyAndDoNotLeakTheViewRegistration() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) << id: "view", name: "", projection: "parallel" >>;
                """.trimIndent(),
            ),
        ).error
        val runtime = assertIs<JsxGraphJessieCodeError.Runtime>(error)
        assertTrue(runtime.reason.contains("center-origin"))

        val recovered = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) <<
                    id: "view",
                    name: "",
                    projection: "parallel",
                    $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                >>;
                """.trimIndent(),
            ),
        ).value
        assertEquals(21, recovered.elements.size)
    }

    @Test
    fun automaticBorderAxesConsumeTheirFullSceneObjectBudget() {
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    view3d(
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                    ) <<
                        name: "",
                        projection: "parallel",
                        axesPosition: "border"
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxObjects = 23),
            ),
        ).error
        val limit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(error)
        assertEquals("created element count", limit.resource)
        assertEquals(60, limit.requestedSize)
    }

    @Test
    fun ticks3DAndText3DRespectPublicResourceLimits() {
        val ticksError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    view = view3d(
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                    ) <<
                        id: "view",
                        name: "",
                        projection: "parallel",
                        $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                    >>;
                    ticks3d(
                        view,
                        [-2, 1, -1],
                        [2, 0, 0],
                        4,
                        [0, 1, 0]
                    ) <<
                        id: "ticks",
                        name: "",
                        ticksDistance: 2,
                        drawLabels: false
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 8),
            ),
        ).error
        val ticksLimit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(
                ticksError,
            )
        assertEquals("curve point count", ticksLimit.resource)
        assertEquals(9, ticksLimit.requestedSize)

        val textError = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    view = view3d(
                        [-5, -4],
                        [8, 7],
                        [[-5, 5], [-4, 6], [-3, 7]]
                    ) <<
                        id: "view",
                        name: "",
                        projection: "parallel",
                        $HIDDEN_DEFAULT_AXES_ATTRIBUTES
                    >>;
                    text3d(view, [0, 0, 0], "long") <<
                        id: "label",
                        name: ""
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxTextLength = 3),
            ),
        ).error
        val textLimit =
            assertIs<JsxGraphJessieCodeError.ResourceLimitExceeded>(
                textError,
            )
        assertEquals("text length", textLimit.resource)
        assertEquals(4, textLimit.requestedSize)
    }

    private fun assertCoordinates(
        expectedX: Double,
        expectedY: Double,
        point: JsxGraphSceneElement.Point,
    ) {
        assertEquals(expectedX, point.coordinates.x, TOLERANCE)
        assertEquals(expectedY, point.coordinates.y, TOLERANCE)
    }

    private fun assertCoordinates(
        expectedX: Double,
        expectedY: Double,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(expectedX, actual.x, TOLERANCE)
        assertEquals(expectedY, actual.y, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12

        val HIDDEN_DEFAULT_AXES_ATTRIBUTES =
            """
            axesPosition: "none",
            xPlaneRear: << visible: false >>,
            yPlaneRear: << visible: false >>,
            zPlaneRear: << visible: false >>
            """.trimIndent()
    }
}
