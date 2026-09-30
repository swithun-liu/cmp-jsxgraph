/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphAngleBehaviorTest {
    @Test
    fun displayTypesAndDotHelpersAreSerializedFromCoreGeometry() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                """
                sectorAngle = angle([4, 0], [0, 0], [0, 4]) <<
                    id: "sector", name: "", withLabel: false,
                    radius: 2, type: "sector", orthoType: "sector",
                    dot: << id: "sectorDot" >>
                >>;
                squareAngle = angle([4, 0], [0, 0], [0, 4]) <<
                    id: "square", name: "", withLabel: false,
                    radius: 2, type: "square",
                    dot: << id: "squareDot" >>
                >>;
                noneAngle = angle([4, 0], [0, 0], [0, 4]) <<
                    id: "none", name: "", withLabel: false,
                    radius: 2, type: "none", orthoType: "none",
                    dot: << id: "noneDot" >>
                >>;
                dotAngle = angle([4, 0], [0, 0], [0, 4]) <<
                    id: "sectordot", name: "", withLabel: false,
                    radius: 2, type: "sectordot", orthoType: "sectordot",
                    dot: << id: "visibleDot", size: 3 >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(8, scene.elements.size)
        assertEquals(19, curve(scene, "sector").points.size)
        val square = curve(scene, "square")
        assertEquals(1, square.bezierDegree)
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 0.0),
                JsxGraphPoint2D(2.0, 0.0),
                JsxGraphPoint2D(2.0, 2.0),
                JsxGraphPoint2D(0.0, 2.0),
                JsxGraphPoint2D(0.0, 0.0),
            ),
            square.points,
        )
        assertEquals(listOf(null), curve(scene, "none").points)
        assertFalse(point(scene, "sectorDot").style.visible)
        assertFalse(point(scene, "squareDot").style.visible)
        assertFalse(point(scene, "noneDot").style.visible)
        val dot = point(scene, "visibleDot")
        assertTrue(dot.style.visible)
        assertEquals(JsxGraphPoint2D(0.7071067811865476, 0.7071067811865475), dot.coordinates)
        assertEquals(3.0, dot.size)
    }

    @Test
    fun twoLineSectorAndAngleSerializeWithoutVirtualPoints() {
        val scene = scene(
            JsxGraphJessieCode.parse(
                """
                A = point(0, 0) <<
                    id: "A", name: "", withLabel: false
                >>;
                B = point(4, 0) <<
                    id: "B", name: "", withLabel: false
                >>;
                C = point(0, 4) <<
                    id: "C", name: "", withLabel: false
                >>;
                l1 = line(A, B) <<
                    id: "l1", name: "", withLabel: false
                >>;
                l2 = line(A, C) <<
                    id: "l2", name: "", withLabel: false
                >>;
                s = sector(l1, l2, [3, 1], [1, 3], 2) <<
                    id: "s", name: "", withLabel: false
                >>;
                a = angle(l1, l2, -1, 1) <<
                    id: "a", name: "", withLabel: false,
                    radius: 2, type: "sector", orthoType: "sector",
                    dot: << id: "dot", name: "" >>
                >>;
                """.trimIndent(),
            ),
        )

        assertEquals(8, scene.elements.size)
        val sector = curve(scene, "s")
        val angle = curve(scene, "a")
        assertEquals(19, sector.points.size)
        assertEquals(19, angle.points.size)
        assertCurvePoint(sector, 3, 2.0, 0.0)
        assertCurvePoint(sector, 15, 0.0, 2.0)
        assertCurvePoint(angle, 3, -2.0, 0.0)
        assertCurvePoint(angle, 15, 0.0, 2.0)
    }

    @Test
    fun setAngleAndFreeUpdateTheSceneThroughJessieCode() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
            JsxGraphJessieCode.createSession(),
        ).value
        val fixed = scene(
            session.execute(
                """
                A = point(4, 0) << id: "A", name: "", withLabel: false >>;
                B = point(0, 0) << id: "B", name: "", withLabel: false >>;
                C = point(0, 4) << id: "C", name: "", withLabel: false >>;
                a = angle(A, B, C) <<
                    id: "a", name: "", withLabel: false,
                    radius: 2, type: "sector", orthoType: "sector",
                    dot: << id: "dot" >>
                >>;
                a.setAngle(1.0471975511965976);
                """.trimIndent(),
            ),
        )
        assertPoint(fixed, "C", 2.0, 3.4641016151377544)
        assertEquals(19, curve(fixed, "a").points.size)

        val freed = scene(
            session.execute(
                """
                a.free();
                C.X = -2;
                C.Y = 3;
                """.trimIndent(),
            ),
        )
        assertPoint(freed, "C", -2.0, 3.0)

        val dynamic = scene(
            session.execute(
                "a.setAngle(function () { return 0.7853981633974483; });",
            ),
        )
        assertPoint(
            dynamic,
            "C",
            2.8284271247461903,
            2.82842712474619,
        )

        val unchanged = scene(session.execute("a.setAngle(0.25);"))
        assertPoint(
            unchanged,
            "C",
            2.8284271247461903,
            2.82842712474619,
        )
    }

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        assertIs(scene.elements.single { it.id == id })

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun assertPoint(
        scene: JsxGraphScene,
        id: String,
        x: Double,
        y: Double,
    ) {
        val point = point(scene, id)
        assertEquals(x, point.coordinates.x, absoluteTolerance = 1.0e-12)
        assertEquals(y, point.coordinates.y, absoluteTolerance = 1.0e-12)
    }

    private fun assertCurvePoint(
        curve: JsxGraphSceneElement.Curve,
        index: Int,
        x: Double,
        y: Double,
    ) {
        val point = assertIs<JsxGraphPoint2D>(curve.points[index])
        assertEquals(x, point.x, absoluteTolerance = 1.0e-12)
        assertEquals(y, point.y, absoluteTolerance = 1.0e-12)
    }

    private fun scene(
        result: GMResult<JsxGraphScene, JsxGraphJessieCodeError>,
    ): JsxGraphScene =
        assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
}
