/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.swithun.jsxgraph.debugui.JsxGraphDebugApp
import com.swithun.jsxgraph.debugui.JsxGraphDebugDestination
import com.swithun.jsxgraph.debugui.JsxGraphDebugOptions
import com.swithun.jsxgraph.debugui.JsxGraphDebugPreview
import com.swithun.jsxgraph.debugui.JsxGraphParityCorpus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JsxGraphDebugApp(
                options = JsxGraphDebugOptions(
                    initialDestination = when {
                        intent.getBooleanExtra(EXTRA_STABLE_LOAD, false) ->
                            JsxGraphDebugDestination.StableLoad
                        intent.hasExtra(EXTRA_PREVIEW) ->
                            JsxGraphDebugDestination.Parity
                        else -> JsxGraphDebugDestination.Roadmap
                    },
                    initialPreview = JsxGraphDebugPreview.from(
                        intent.getStringExtra(EXTRA_PREVIEW),
                    ),
                    parityCaseId = intent.getStringExtra(EXTRA_CASE_ID)
                        ?: JsxGraphParityCorpus.DEFAULT_CASE_ID,
                    autoRunLoadTest =
                        intent.getBooleanExtra(EXTRA_STABLE_LOAD, false),
                ),
            )
        }
    }

    companion object {
        private const val EXTRA_PREVIEW = "preview"
        private const val EXTRA_CASE_ID = "caseId"
        private const val EXTRA_STABLE_LOAD = "stableLoad"
    }
}
