/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class JsxGraphNavigationTest {
    @Test
    fun constructionDocumentBuildsOfficialControlOrderAndPortableStyle() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(navigationDocument()),
        ).value
        val navigationBar = requireNotNull(session.scene.navigationBar)

        assertEquals(
            listOf(
                JsxGraphNavigationAction.FULLSCREEN,
                JsxGraphNavigationAction.SCREENSHOT,
                JsxGraphNavigationAction.RELOAD,
                JsxGraphNavigationAction.CLEAR_TRACES,
                JsxGraphNavigationAction.ZOOM_OUT,
                JsxGraphNavigationAction.ZOOM_RESET,
                JsxGraphNavigationAction.ZOOM_IN,
                JsxGraphNavigationAction.PAN_LEFT,
                JsxGraphNavigationAction.PAN_DOWN,
                JsxGraphNavigationAction.PAN_UP,
                JsxGraphNavigationAction.PAN_RIGHT,
            ),
            navigationBar.controls.map(JsxGraphNavigationControl::action),
        )
        assertEquals(
            listOf(
                "fullscreen",
                "screenshot",
                "reload",
                "cleartraces",
                "out",
                "100",
                "in",
                "left",
                "down",
                "up",
                "right",
            ),
            navigationBar.controls.map(
                JsxGraphNavigationControl::upstreamType,
            ),
        )
        assertEquals(
            listOf(
                "F",
                "S",
                "\u21bb",
                "\u2297",
                "\u2013",
                "o",
                "+",
                "\u2190",
                "\u2193",
                "\u2191",
                "\u2192",
            ),
            navigationBar.controls.map(JsxGraphNavigationControl::symbol),
        )
        assertEquals(
            JsxGraphNavigationBarStyle(
                strokeColor = JsxGraphColor(18, 52, 86),
                fillColor = JsxGraphColor(171, 205, 239),
                highlightFillColor = JsxGraphColor(101, 67, 33),
                paddingPixels = 3.0,
                fontSizePixels = 15.0,
                zIndex = 101,
                rightPixels = 6.0,
                bottomPixels = 7.0,
            ),
            navigationBar.style,
        )
    }

    @Test
    fun navigationCommandsMatchOfficialViewportAndHostDispatch() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(navigationDocument()),
        ).value

        assertBounds(-5.0, 4.0, 5.0, -4.0, session.scene)
        assertBounds(
            -6.25,
            5.0,
            6.25,
            -5.0,
            session.navigate(JsxGraphNavigationAction.ZOOM_OUT).scene,
        )
        assertBounds(
            -5.0,
            4.0,
            5.0,
            -4.0,
            session.navigate(JsxGraphNavigationAction.ZOOM_RESET).scene,
        )
        assertBounds(
            -4.0,
            3.2,
            4.0,
            -3.2,
            session.navigate(JsxGraphNavigationAction.ZOOM_IN).scene,
        )
        assertBounds(
            -4.0,
            3.2,
            4.0,
            -3.2,
            assertIs<GMResult.Ok<JsxGraphScene>>(
                session.advanceAnimations(0.0),
            ).value,
        )
        session.navigate(JsxGraphNavigationAction.ZOOM_RESET)
        assertBounds(
            -6.0,
            4.0,
            4.0,
            -4.0,
            session.navigate(JsxGraphNavigationAction.PAN_LEFT).scene,
        )
        session.navigate(JsxGraphNavigationAction.PAN_RIGHT)
        assertBounds(
            -5.0,
            3.2,
            5.0,
            -4.8,
            session.navigate(JsxGraphNavigationAction.PAN_DOWN).scene,
        )
        assertBounds(
            -5.0,
            4.0,
            5.0,
            -4.0,
            session.navigate(JsxGraphNavigationAction.PAN_UP).scene,
        )

        assertEquals(
            JsxGraphNavigationHostAction.FULLSCREEN,
            session.navigate(JsxGraphNavigationAction.FULLSCREEN).hostAction,
        )
        assertEquals(
            JsxGraphNavigationHostAction.SCREENSHOT,
            session.navigate(JsxGraphNavigationAction.SCREENSHOT).hostAction,
        )
        assertEquals(
            JsxGraphNavigationHostAction.RELOAD,
            session.navigate(JsxGraphNavigationAction.RELOAD).hostAction,
        )
        assertNull(
            session.navigate(JsxGraphNavigationAction.CLEAR_TRACES).hostAction,
        )
    }

    @Test
    fun unavailableControlAndNonPortableCssAreStructuredFailures() {
        val hidden = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "schemaVersion": 1,
                  "boundingBox": [-5, 4, 5, -4],
                  "objects": []
                }
                """.trimIndent(),
            ),
        ).value
        assertNull(hidden.scene.navigationBar)
        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.NavigationControlUnavailable,
                >,
            >(
            hidden.interactNavigation(JsxGraphNavigationAction.ZOOM_IN),
        )

        val invalid = assertIs<GMResult.Err<JsxGraphDocumentError.InvalidField>>(
            JsxGraphEngine.createSession(
                """
                {
                  "schemaVersion": 1,
                  "boundingBox": [-5, 4, 5, -4],
                  "showNavigation": true,
                  "navbar": {"fontSize": "1em"},
                  "objects": []
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("navbar.fontSize", invalid.path)
    }

    @Test
    fun jessieCodeKeepsCompatibilityDefaultAndCanShowArrowsWithoutZoom() {
        val defaultSession =
            assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
                JsxGraphJessieCode.createSession(),
            ).value
        assertNull(defaultSession.scene.navigationBar)

        val arrowsOnly =
            assertIs<GMResult.Ok<JsxGraphJessieCodeSession>>(
                JsxGraphJessieCode.createSession(
                    boardOptions = JsxGraphJessieCodeBoardOptions(
                        showNavigation = true,
                        showZoom = false,
                    ),
                ),
            ).value
        assertEquals(
            listOf(
                JsxGraphNavigationAction.PAN_LEFT,
                JsxGraphNavigationAction.PAN_DOWN,
                JsxGraphNavigationAction.PAN_UP,
                JsxGraphNavigationAction.PAN_RIGHT,
            ),
            arrowsOnly.scene.navigationBar
                ?.controls
                ?.map(JsxGraphNavigationControl::action),
        )
        assertIs<
            GMResult.Err<
                JsxGraphInteractionError.NavigationControlUnavailable,
                >,
            >(
            arrowsOnly.interactNavigation(
                JsxGraphNavigationAction.ZOOM_IN,
            ),
        )
        assertBounds(
            -6.0,
            5.0,
            4.0,
            -5.0,
            assertIs<GMResult.Ok<JsxGraphNavigationInteractionResult>>(
                arrowsOnly.interactNavigation(
                    JsxGraphNavigationAction.PAN_LEFT,
                ),
            ).value.scene,
        )
        assertBounds(
            -6.0,
            5.0,
            4.0,
            -5.0,
            assertIs<GMResult.Ok<JsxGraphScene>>(
                arrowsOnly.execute("value = 1;"),
            ).value,
        )
    }

    private fun JsxGraphSession.navigate(
        action: JsxGraphNavigationAction,
    ): JsxGraphNavigationInteractionResult =
        assertIs<GMResult.Ok<JsxGraphNavigationInteractionResult>>(
            interactNavigation(action),
        ).value

    private fun assertBounds(
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        scene: JsxGraphScene,
    ) {
        assertEquals(left, scene.boundingBox.left, TOLERANCE)
        assertEquals(top, scene.boundingBox.top, TOLERANCE)
        assertEquals(right, scene.boundingBox.right, TOLERANCE)
        assertEquals(bottom, scene.boundingBox.bottom, TOLERANCE)
    }

    private fun navigationDocument(): String =
        """
        {
          "schemaVersion": 1,
          "boundingBox": [-5, 4, 5, -4],
          "showNavigation": true,
          "showZoom": true,
          "showClearTraces": true,
          "showFullscreen": true,
          "showScreenshot": true,
          "showReload": true,
          "fullscreenSymbol": "F",
          "screenshotSymbol": "S",
          "navbar": {
            "strokeColor": "#123456",
            "fillColor": "#abcdef",
            "highlightFillColor": "#654321",
            "padding": "3px",
            "fontSize": "15px",
            "zIndex": "101",
            "right": "6px",
            "bottom": "7px"
          },
          "objects": []
        }
        """.trimIndent()

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
