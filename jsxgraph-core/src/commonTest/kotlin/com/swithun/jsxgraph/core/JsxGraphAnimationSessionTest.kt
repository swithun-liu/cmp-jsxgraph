package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphAnimationSessionTest {
    @Test
    fun sessionAdvancesMoveToThroughPortableAnimationFrames() {
        val session = session(
            """
            A = point(0, 0) << id: "A", name: "" >>;
            marker = point(-1, -1) << id: "marker", name: "" >>;
            A.moveTo(
                [4, 8],
                100,
                <<
                    effect: "<>",
                    callback: function () {
                        marker.moveTo([9, 9]);
                    }
                >>
            );
            """.trimIndent(),
        )

        assertTrue(session.hasActiveAnimations)
        assertPoint(session.scene, "A", 0.0, 0.0)
        assertPoint(frame(session, 34.0), "A", 0.0, 0.0)
        assertPoint(frame(session, 1.0), "A", 0.0, 0.0)
        assertPoint(frame(session, 35.0), "A", 1.0, 2.0)
        assertPoint(frame(session, 35.0), "A", 3.0, 6.0)
        assertPoint(frame(session, 35.0), "A", 4.0, 8.0)
        assertTrue(session.hasActiveAnimations)

        assertPoint(frame(session, 35.0), "A", 4.0, 8.0)
        assertFalse(session.hasActiveAnimations)
        assertPoint(session.scene, "marker", 9.0, 9.0)
    }

    @Test
    fun functionValuedMoveAlongReceivesElapsedAnimationTime() {
        val session = session(
            """
            A = point(0, 0) << id: "A", name: "" >>;
            A.moveAlong(
                function (elapsed) {
                    if (elapsed > 100) {
                        0 / 0;
                    } else {
                        [elapsed / 35, elapsed / 70];
                    }
                },
                100
            );
            """.trimIndent(),
        )

        assertPoint(frame(session, 35.0), "A", 1.0, 0.5)
        assertPoint(frame(session, 35.0), "A", 2.0, 1.0)
        assertPoint(frame(session, 35.0), "A", 2.0, 1.0)
        assertFalse(session.hasActiveAnimations)
    }

    @Test
    fun point3DAnimationIsPubliclyTriggeredAndSessionDriven() {
        val session = session(
            """
            view = view3d(
                [-4, -3],
                [8, 6],
                [[-5, 5], [-5, 5], [-5, 5]]
            ) << id: "view", name: "", axesPosition: "none" >>;
            P = point3d(view, [0, 0, 0]) << id: "P", name: "" >>;
            """.trimIndent(),
        )
        var callbackCount = 0
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint3D(
                id = "P",
                coordinates = JsxGraphPoint3DCoordinates(3.0, 4.0, 2.0),
                timeMilliseconds = 100.0,
                effect = "==",
                callback = { callbackCount += 1 },
            ),
        )

        assertTrue(session.hasActiveAnimations)
        repeat(4) {
            frame(session, 25.0)
        }
        assertFalse(session.hasActiveAnimations)
        assertEquals(1, callbackCount)

        val expected = session(
            """
            view = view3d(
                [-4, -3],
                [8, 6],
                [[-5, 5], [-5, 5], [-5, 5]]
            ) << id: "view", name: "", axesPosition: "none" >>;
            P = point3d(view, [3, 4, 2]) << id: "P", name: "" >>;
            """.trimIndent(),
        ).scene
        assertEquals(point(expected, "P"), point(session.scene, "P"))

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint3DAlong(
                id = "P",
                path = listOf(
                    JsxGraphPoint3DCoordinates(1.0, 1.0, 1.0),
                    JsxGraphPoint3DCoordinates(-2.0, -1.0, -2.0),
                ),
                timeMilliseconds = 50.0,
            ),
        )
        frame(session, 25.0)
        frame(session, 25.0)
        assertFalse(session.hasActiveAnimations)
    }

    @Test
    fun constructionDocumentSessionUsesTheSamePoint3DFrameContract() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(point3DDocument(0.0, 0.0, 0.0)),
        ).value
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint3D(
                id = "P",
                coordinates = JsxGraphPoint3DCoordinates(3.0, 4.0, 2.0),
                timeMilliseconds = 100.0,
                effect = "==",
            ),
        )

        repeat(4) {
            val result = assertIs<GMResult.Ok<JsxGraphScene>>(
                session.advanceAnimations(25.0),
            )
            assertEquals(result.value, session.scene)
        }
        assertFalse(session.hasActiveAnimations)

        val expected = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(point3DDocument(3.0, 4.0, 2.0)),
        ).value
        assertEquals(point(expected, "P"), point(session.scene, "P"))
    }

    @Test
    fun arrayMoveAlongAndVisitAreAvailableThroughJessieCode() {
        val session = session(
            """
            A = point(0, 0) << id: "A", name: "" >>;
            A.moveAlong(
                [[0, 0], [2, 4], [4, 0]],
                90,
                << interpolate: false >>
            );
            """.trimIndent(),
        )

        drain(session)
        assertPoint(session.scene, "A", 4.0, 0.0)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.execute(
                """
                A.visit(
                    [2, 2],
                    140,
                    << effect: "==", repeat: 1 >>
                );
                """.trimIndent(),
            ),
        )
        drain(session)
        assertPoint(session.scene, "A", 4.0, 0.0)
    }

    @Test
    fun sessionRejectsInvalidElapsedTimeWithoutAdvancing() {
        val session = session(
            """
            A = point(0, 0) << id: "A", name: "" >>;
            A.moveTo([4, 8], 100);
            """.trimIndent(),
        )

        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.InvalidAnimationElapsedTime,
                >,
            >(session.advanceAnimations(Double.NaN))
        assertTrue(session.hasActiveAnimations)
        assertPoint(session.scene, "A", 0.0, 0.0)
    }

    @Test
    fun functionPathFailureIsReturnedByTheFrameBoundary() {
        val session = session(
            """
            A = point(0, 0) << id: "A", name: "" >>;
            A.moveAlong(function (elapsed) { "invalid"; }, 100);
            """.trimIndent(),
        )

        val error = assertIs<
            GMResult.Err<JsxGraphInteractionError.AnimationFailure>,
            >(session.advanceAnimations(35.0)).error
        assertEquals("A", error.elementId)
        assertFalse(session.hasActiveAnimations)
    }

    private fun session(source: String): JsxGraphJessieCodeSession {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        assertIs<GMResult.Ok<JsxGraphScene>>(session.execute(source))
        return session
    }

    private fun frame(
        session: JsxGraphJessieCodeSession,
        elapsedMilliseconds: Double,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.advanceAnimations(elapsedMilliseconds),
        ).value

    private fun drain(session: JsxGraphJessieCodeSession) {
        repeat(20) {
            if (!session.hasActiveAnimations) {
                return
            }
            frame(session, 35.0)
        }
        assertFalse(
            session.hasActiveAnimations,
            "animation did not finish within the test frame budget",
        )
    }

    private fun assertPoint(
        scene: JsxGraphScene,
        id: String,
        x: Double,
        y: Double,
    ) {
        val point = assertIs<JsxGraphSceneElement.Point>(
            scene.elements.single { it.id == id },
        )
        assertEquals(x, point.coordinates.x, 1.0e-12)
        assertEquals(y, point.coordinates.y, 1.0e-12)
    }

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun point3DDocument(
        x: Double,
        y: Double,
        z: Double,
    ): String =
        """
        {
          "boundingBox": [-5, 5, 5, -5],
          "objects": [
            {
              "id": "view",
              "type": "view3d",
              "parents": [
                [-4, -3],
                [8, 6],
                [[-5, 5], [-5, 5], [-5, 5]]
              ],
              "attributes": {
                "name": "",
                "axesPosition": "none"
              }
            },
            {
              "id": "P",
              "type": "point3d",
              "parents": ["view", [$x, $y, $z]],
              "attributes": {"name": ""}
            }
          ]
        }
        """.trimIndent()
}
