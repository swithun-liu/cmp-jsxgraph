/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import com.swithun.jsxgraph.core.JsxGraphNavigationAction
import com.swithun.jsxgraph.core.JsxGraphNavigationControl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsxGraphNavigationControlTest {
    @Test
    fun onlyPortableDefaultFullscreenSymbolUsesTheIconAdapter() {
        assertTrue(
            JsxGraphNavigationControl(
                action = JsxGraphNavigationAction.FULLSCREEN,
                upstreamType = "fullscreen",
                symbol = "\u26f6",
            ).usesDefaultFullscreenIcon(),
        )
        assertFalse(
            JsxGraphNavigationControl(
                action = JsxGraphNavigationAction.FULLSCREEN,
                upstreamType = "fullscreen",
                symbol = "F",
            ).usesDefaultFullscreenIcon(),
        )
        assertFalse(
            JsxGraphNavigationControl(
                action = JsxGraphNavigationAction.SCREENSHOT,
                upstreamType = "screenshot",
                symbol = "\u26f6",
            ).usesDefaultFullscreenIcon(),
        )
    }
}
