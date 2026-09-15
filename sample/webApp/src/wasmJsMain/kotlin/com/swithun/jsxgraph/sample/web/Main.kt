/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.swithun.jsxgraph.sample.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.swithun.jsxgraph.debugui.JsxGraphDebugApp
import com.swithun.jsxgraph.debugui.JsxGraphDebugDestination
import com.swithun.jsxgraph.debugui.JsxGraphDebugOptions
import com.swithun.jsxgraph.debugui.JsxGraphDebugPreview
import com.swithun.jsxgraph.debugui.JsxGraphParityCorpus
import kotlinx.browser.document
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: return
    ComposeViewport(body) {
        JsxGraphDebugApp(
            options = JsxGraphDebugOptions(
                initialDestination =
                    if (queryParameter("openParity") == "true") {
                        JsxGraphDebugDestination.Parity
                    } else {
                        JsxGraphDebugDestination.Roadmap
                    },
                initialPreview = JsxGraphDebugPreview.from(
                    queryParameter("preview"),
                ),
                parityCaseId = queryParameter("caseId")
                    ?: JsxGraphParityCorpus.DEFAULT_CASE_ID,
                sourceOverride = queryParameter("source"),
            ),
        )
    }
}

@JsFun("(name) => new URLSearchParams(globalThis.location.search).get(name)")
private external fun queryParameter(name: String): String?
