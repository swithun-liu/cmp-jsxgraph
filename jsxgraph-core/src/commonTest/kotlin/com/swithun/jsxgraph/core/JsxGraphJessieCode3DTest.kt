package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsxGraphJessieCode3DTest {
    @Test
    fun nativeCreatorsBuildViewPointAndTransformLifecycle() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphJessieCode.parse(
                """
                view = view3d(
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ) << name: "", projection: "parallel" >>;
                source = point3d(
                    view,
                    1,
                    2,
                    2
                ) << name: "", withLabel: false, size: 4 >>;
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
                ) << name: "", withLabel: false, fixed: true >>;
                """.trimIndent(),
            ),
        ).value

        assertEquals(2, scene.elements.size)
        assertCoordinates(
            expectedX = -0.7566557074166769,
            expectedY = -0.7886977433927291,
            point = assertIs<JsxGraphSceneElement.Point>(
                scene.elements[0],
            ),
        )
        assertCoordinates(
            expectedX = -3.669509900873932,
            expectedY = 1.9000326916012957,
            point = assertIs<JsxGraphSceneElement.Point>(
                scene.elements[1],
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
                ) << name: "", projection: "parallel" >>;
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
        assertEquals(2, initial.elements.size)
        val original = assertIs<JsxGraphSceneElement.Point>(
            initial.elements[1],
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(-2.0, 1.0),
            ),
        ).value
        val updated = assertIs<JsxGraphSceneElement.Point>(
            moved.elements[1],
        )
        assertEquals("dynamic", updated.id)
        kotlin.test.assertNotEquals(
            original.coordinates,
            updated.coordinates,
        )
    }

    private fun assertCoordinates(
        expectedX: Double,
        expectedY: Double,
        point: JsxGraphSceneElement.Point,
    ) {
        assertEquals(expectedX, point.coordinates.x, TOLERANCE)
        assertEquals(expectedY, point.coordinates.y, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
