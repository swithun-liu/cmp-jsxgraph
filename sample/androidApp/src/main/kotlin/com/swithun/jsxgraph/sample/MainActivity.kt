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
import com.swithun.jsxgraph.debugui.JsxGraphDebugOptions
import com.swithun.jsxgraph.debugui.JsxGraphDebugPreview

class MainActivity : ComponentActivity() {
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
        private const val EXTRA_PREVIEW = "preview"
    }
}
