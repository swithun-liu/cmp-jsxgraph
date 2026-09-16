/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.sample.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.swithun.jsxgraph.debugui.JsxGraphDebugApp
import com.swithun.jsxgraph.debugui.JsxGraphDebugDestination
import com.swithun.jsxgraph.debugui.JsxGraphDebugOptions

fun main(args: Array<String>) = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CMP JSXGraph",
        state = WindowState(size = DpSize(1100.dp, 820.dp)),
    ) {
        JsxGraphDebugApp(
            options = JsxGraphDebugOptions(
                initialDestination =
                    if ("--load-test" in args) {
                        JsxGraphDebugDestination.StableLoad
                    } else {
                        JsxGraphDebugDestination.Roadmap
                    },
                autoRunLoadTest = "--load-test" in args,
            ),
        )
    }
}
