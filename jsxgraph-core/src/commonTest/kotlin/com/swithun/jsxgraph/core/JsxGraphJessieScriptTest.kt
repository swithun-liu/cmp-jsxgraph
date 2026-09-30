/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsxGraphJessieScriptTest {
    @Test
    fun legacyConstructionBranchesCompileToNativeJessieCode() {
        val source = officialFixtureSource()
        val compiled = assertIs<GMResult.Ok<String>>(
            JsxGraphJessieScript.compile(source),
        ).value

        assertTrue(
            "legacy0 = point(0, 0) " +
                "<<name: \"A\", withLabel: false>>;" in compiled,
        )
        assertTrue(
            "legacy2 = line(legacy0, legacy1)" in compiled,
        )
        assertTrue(
            "legacy3 = circle(legacy0, 2)" in compiled,
        )
        assertTrue(
            "intersection(legacy2, legacy3, 0)" in compiled,
        )
        assertTrue(
            "intersection(legacy2, legacy3, 1)" in compiled,
        )
        assertTrue(
            "functiongraph(function (x) { return x^2; }, -6.0, 6.0)" in
                compiled,
        )
        assertTrue(
            "id: \"pair.m\"" in compiled,
        )
    }

    @Test
    fun legacyConstructionExecutesThroughTranslatedBoard() {
        val result =
            JsxGraphJessieScript.parse(officialFixtureSource())
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value

        assertEquals(
            15,
            scene.elements.size,
            scene.elements.map(JsxGraphSceneElement::name).toString(),
        )
        assertEquals(
            listOf(
                "A",
                "B",
                "g",
                "c",
                "P",
                "I_1",
                "I_2",
                "n",
                "&alpha;",
                "",
                "M",
                "f",
                "t_{a}",
                "poly",
                "m",
            ),
            scene.elements.map(JsxGraphSceneElement::name),
        )
        assertEquals("pair.m", scene.elements.last().id)
    }

    @Test
    fun ratioPointRetainsOfficialDynamicCoordinateClosures() {
        val session = assertIs<GMResult.Ok<JsxGraphJessieScriptSession>>(
            JsxGraphJessieScript.createSession(
                "A(0,0) nolabel;B(2,0) nolabel;M=1/2(A,B) nolabel",
            ),
        ).value
        val pointB = assertIs<JsxGraphSceneElement.Point>(
            session.scene.elements.single { it.name == "B" },
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                pointB.id,
                JsxGraphPoint2D(4.0, 2.0),
            ),
        ).value
        val midpoint = assertIs<JsxGraphSceneElement.Point>(
            moved.elements.single { it.name == "M" },
        )

        assertEquals(JsxGraphPoint2D(2.0, 1.0), midpoint.coordinates)
    }

    @Test
    fun multilineMacroBodyCompiles() {
        val compiled = assertIs<GMResult.Ok<String>>(
            JsxGraphJessieScript.compile(
                """
                A(0,0) nolabel;
                B(2,0) nolabel;
                Pair=Macro(U,V){
                    m=1/2(U,V) nolabel;
                };
                pair=Pair(A,B)
                """.trimIndent(),
            ),
        ).value

        assertTrue("id: \"pair.m\"" in compiled)
    }

    @Test
    fun unsupportedMutationAndLimitsAreStructured() {
        assertIs<
            GMResult.Err<
                JsxGraphJessieScriptError.UnsupportedPropertyMutation,
                >
            >(
            JsxGraphJessieScript.compile(
                "A(0,0) nolabel; A.visible=false;",
            ),
        )
        assertIs<
            GMResult.Err<JsxGraphJessieScriptError.StatementLimitExceeded>
            >(
            JsxGraphJessieScript.compile(
                "A(0,0);B(1,1)",
                JsxGraphJessieScriptLimits(maxStatements = 1),
            ),
        )
        assertIs<
            GMResult.Err<JsxGraphJessieScriptError.UnknownReference>
            >(
            JsxGraphJessieScript.compile("g=[A B]"),
        )
    }

    private fun officialFixtureSource(): String =
        listOf(
            "A(0,0) nolabel",
            "B(2,0) nolabel",
            "g=[A B] nolabel",
            "c=k(A,2) nolabel",
            "P(g,1,0) nolabel",
            "I=g&c nolabel",
            "n=|_(g,A) nolabel",
            "alpha=<(B,A,P) nolabel",
            "M=1/2(A,B) nolabel",
            "f:x^2",
            "#hello(1,2)",
            "poly[A,B,M] nolabel",
            "Pair=Macro(U,V){m=1/2(U,V) nolabel;}",
            "pair=Pair(A,B)",
        ).joinToString(";")
}
