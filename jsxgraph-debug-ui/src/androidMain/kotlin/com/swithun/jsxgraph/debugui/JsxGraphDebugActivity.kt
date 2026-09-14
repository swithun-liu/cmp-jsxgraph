/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class JsxGraphDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JsxGraphDebugApp(
                options = JsxGraphDebugOptions(
                    initialPreview = JsxGraphDebugPreview.from(
                        intent.getStringExtra(EXTRA_PREVIEW),
                    ),
                ),
            )
        }
    }

    companion object {
        const val EXTRA_PREVIEW = "preview"
    }
}
