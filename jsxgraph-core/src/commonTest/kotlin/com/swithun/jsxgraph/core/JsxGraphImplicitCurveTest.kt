/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphImplicitCurveTest {
    @Test
    fun jessieCodeProducesPortableImplicitCurveScene() {
        val result = JsxGraphJessieCode.parse(
                """
                implicitcurve(
                    function (x, y) {
                        return x * x + y * y - 1;
                    },
                    function (x, y) { return 2 * x; },
                    function (x, y) { return 2 * y; },
                    [-2, 2],
                    [-2, 2]
                ) <<
                    id: "implicit",
                    name: "",
                    withLabel: false,
                    maxSteps: 64,
                    resolutionOuter: 0.1,
                    resolutionInner: 0.1
                >>;
                """.trimIndent(),
            )
        assertTrue(result is GMResult.Ok, result.toString())
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(result).value

        val curve = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single(),
        )
        assertEquals("implicit", curve.id)
        assertEquals(129, curve.points.size)
        assertTrue(curve.points.all { it != null })
    }

    @Test
    fun jessieCodePointLimitRejectsImplicitCurveSafely() {
        val error = assertIs<GMResult.Err<JsxGraphJessieCodeError>>(
            JsxGraphJessieCode.parse(
                source =
                    """
                    implicitcurve(
                        "x * x + y * y - 1",
                        [-2, 2],
                        [-2, 2]
                    ) <<
                        id: "implicit", name: "", maxSteps: 64,
                        resolutionOuter: 0.1,
                        resolutionInner: 0.1
                    >>;
                    """.trimIndent(),
                limits = JsxGraphJessieCodeLimits(maxCurvePoints = 10),
            ),
        ).error

        assertIs<JsxGraphJessieCodeError.Runtime>(error)
    }
}
