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
    val auditMode = queryParameter("audit") == "true"
    val loadMode = queryParameter("load") == "true"
    val roadmapMode = queryParameter("roadmap") == "true"
    ComposeViewport(body) {
        JsxGraphDebugApp(
            options = JsxGraphDebugOptions(
                initialDestination =
                    if (loadMode) {
                        JsxGraphDebugDestination.StableLoad
                    } else if (roadmapMode && !auditMode) {
                        JsxGraphDebugDestination.Roadmap
                    } else {
                        JsxGraphDebugDestination.Parity
                    },
                initialPreview = JsxGraphDebugPreview.from(
                    queryParameter("preview"),
                ),
                parityCaseId = queryParameter("caseId")
                    ?: JsxGraphParityCorpus.DEFAULT_CASE_ID,
                sourceOverride = queryParameter("source"),
                boardOnly = auditMode,
            ),
            onParityCaseChange = ::replaceParityCaseQuery,
        )
    }
}

@JsFun("(name) => new URLSearchParams(globalThis.location.search).get(name)")
private external fun queryParameter(name: String): String?

@JsFun(
    """(caseId) => {
        const url = new URL(globalThis.location.href);
        url.searchParams.set("caseId", caseId);
        url.searchParams.delete("source");
        globalThis.history.replaceState(null, "", url);
    }""",
)
private external fun replaceParityCaseQuery(caseId: String)
