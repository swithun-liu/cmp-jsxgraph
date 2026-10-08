/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardHostAdapter
import com.swithun.jsxgraph.core.parser.JessieCodeCreator
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.NativeJessieCodeCreators
import com.swithun.jsxgraph.core.utils.EnvDimensions
import com.swithun.jsxgraph.core.utils.EnvRuntimeCapabilities
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JsxGraphApiTest {
    @AfterTest
    fun resetGlobalAdapters() {
        JXG.logger = JxgLogger { }
        JXG.unregisterElement("custom")
        for (board in JXG.boards.values.toList()) {
            JSXGraph.freeBoard(board)
        }
    }

    @Test
    fun extendConstantsAndShortcutPreserveJxgContracts() {
        val bag = JxgPropertyBag(mapOf("existing" to 1))
        assertSame(
            bag,
            assertIs<GMResult.Ok<JxgPropertyBag>>(
                JXG.extend(
                    target = bag,
                    extension = mapOf("MixedCase" to 2),
                    toLower = true,
                ),
            ).value,
        )
        assertEquals(2, bag["mixedcase"])
        assertIs<GMResult.Ok<Unit>>(
            JXG.defineConstant(bag, "answer", 42),
        )
        assertEquals(42, bag["answer"])
        assertIs<GMResult.Err<JxgApiError.ConstantRedefinition>>(
            bag.set("answer", 7),
        )
        assertIs<GMResult.Ok<JxgPropertyBag>>(
            JXG.extendConstants(
                target = bag,
                constants = mapOf("truth" to true),
                toUpper = true,
            ),
        )
        assertEquals(true, bag["TRUTH"])

        val shortcut = JXG.shortcut(
            target = JxgMethodTarget { name, receiver, arguments ->
                if (name != "sum") {
                    GMResult.Err(JxgApiError.UnknownMethod(name))
                } else {
                    GMResult.Ok(
                        (receiver as Int) +
                            arguments.sumOf { (it as Number).toInt() },
                    )
                }
            },
            method = "sum",
        )
        assertEquals(
            10,
            assertIs<GMResult.Ok<Any?>>(
                shortcut.invoke(4, listOf(1, 2, 3)),
            ).value,
        )
    }

    @Test
    fun boardAliasesRegistryLoggingAndLookupMatchUpstream() {
        val logs = mutableListOf<String>()
        JXG.logger = JxgLogger(logs::add)
        val board = assertIs<GMResult.Ok<Board>>(
            JXG.board(
                containerId = "container",
                attributes = mapOf(
                    "id" to "board-id",
                    "boundingBox" to listOf(-2, 3, 4, -5),
                    "keepAspectRatio" to true,
                ),
                hostAdapter = FixedHostAdapter(800.0, 600.0),
            ),
        ).value

        assertSame(board, JXG.boards["board-id"])
        assertSame(board, JXG.getBoardByContainerId("container"))
        assertEquals(
            listOf(
                -4.333333333333333,
                3.0,
                6.333333333333333,
                -5.0,
            ),
            board.getBoundingBox().toList(),
        )
        assertEquals(800.0, board.canvasWidth)
        assertEquals(600.0, board.canvasHeight)

        JXG.deprecated("old()", "new()")
        JXG.debug("value", 3)
        assertEquals(
            listOf(
                "WARNING: old() is deprecated. Please use new() instead.",
                "value",
                "3",
            ),
            logs,
        )

        assertIs<GMResult.Ok<Unit>>(JSXGraph.freeBoard(board))
        assertNull(JXG.getBoardByContainerId("container"))
    }

    @Test
    fun rendererSelectionAndAriaUseInjectedPlatformAdapters() {
        val renderer = assertIs<GMResult.Ok<JsxGraphRendererAdapter>>(
            JSXGraph.initRenderer(
                containerId = "box",
                dimensions = EnvDimensions(100.0, 80.0),
                requestedRenderer = "auto",
                capabilities = EnvRuntimeCapabilities(
                    supportsSvg = true,
                    supportsCanvas = true,
                ),
                rendererFactory = JsxGraphRendererFactory {
                        type,
                        container,
                        dimensions,
                    ->
                    assertEquals(JsxGraphRendererType.SVG, type)
                    assertEquals("box", container)
                    assertEquals(100.0, dimensions.width)
                    GMResult.Ok(RecordingRenderer(type.value))
                },
            ),
        ).value
        assertEquals("svg", renderer.type)
        assertIs<GMResult.Err<JxgApiError.UnsupportedRenderer>>(
            JSXGraph.initRenderer(
                containerId = "box",
                dimensions = EnvDimensions(100.0, 80.0),
                requestedRenderer = "vml",
            ),
        )

        val aria = RecordingAriaAdapter()
        val board = assertIs<GMResult.Ok<Board>>(
            JSXGraph.initBoard(
                containerId = "aria-box",
                attributes = mapOf("title" to "Construction"),
                ariaAdapter = aria,
            ),
        ).value
        assertEquals(listOf("set:aria-box:Construction"), aria.operations)
        assertIs<GMResult.Ok<Unit>>(
            JSXGraph.freeBoard(board, aria),
        )
        assertEquals(
            listOf(
                "set:aria-box:Construction",
                "remove:aria-box",
            ),
            aria.operations,
        )
    }

    @Test
    fun fileReaderAndGlobalElementRegistrationUseExistingRegistries() {
        val board = assertIs<GMResult.Ok<Board>>(
            JSXGraph.loadBoardFromString(
                containerId = "graph-box",
                source =
                    "-5 5 5 -5\ngraph\n2\nA 0 0\nB 1 1\nA B",
                format = "GRAPH",
            ),
        ).value
        assertEquals(3, board.objects.size)

        val custom = JessieCodeCreator { _, _, _, _ ->
            GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
        }
        JXG.registerElement("CuStOm", custom)
        assertSame(custom, NativeJessieCodeCreators.creator("custom"))

        val legacy = JessieCodeCreator { _, _, _, _ ->
            GMResult.Ok(JessieCodeRuntimeValue.NullValue)
        }
        JSXGraph.registerElement("custom", legacy)
        assertSame(legacy, NativeJessieCodeCreators.creator("CUSTOM"))
    }

    @Test
    fun appBoxComputesTheOfficialRelativeInnerLayout() {
        var request: JxgAppBoxRequest? = null
        val board = assertIs<GMResult.Ok<Board>>(
            JXG.appBox(
                outerContainerId = "outer",
                attributes = mapOf(
                    "boundingBox" to listOf(-5, 4, 5, -4),
                    "jxgbox" to mapOf(
                        "id" to "inner",
                        "cssClass" to "math",
                        "cssStyle" to "width:100%",
                        "outerBox" to listOf(-10, 10, 10, -10),
                    ),
                ),
                host = JxgAppBoxHost {
                    request = it
                    GMResult.Ok(FixedHostAdapter(640.0, 480.0))
                },
            ),
        ).value

        assertEquals("outer_inner", request?.innerContainerId)
        assertEquals("math", request?.cssClass)
        assertEquals(25.0, request?.leftPercent)
        assertEquals(30.0, request?.topPercent)
        assertEquals(50.0, request?.widthPercent)
        assertEquals(40.0, request?.heightPercent)
        assertSame(board, JXG.getBoardByContainerId("outer_inner"))
    }

    @Test
    fun invalidBoardAndReaderInputsReturnStructuredFailures() {
        assertIs<GMResult.Err<JxgApiError.InvalidAttribute>>(
            JSXGraph.initBoard(
                containerId = "bad",
                attributes = mapOf(
                    "boundingBox" to listOf(0, 1, 0, -1),
                ),
            ),
        )
        assertIs<GMResult.Err<JxgApiError.ReaderFailure>>(
            JSXGraph.loadBoardFromString(
                containerId = "unknown-reader",
                source = "data",
                format = "unknown",
            ),
        )
        assertFalse("unknown-reader" in JXG.boards)
        assertIs<GMResult.Err<JxgApiError.UnknownBoard>>(
            JSXGraph.freeBoard("missing"),
        )
    }

    private class FixedHostAdapter(
        private val width: Double,
        private val height: Double,
    ) : BoardHostAdapter {
        override fun size(): DoubleArray =
            doubleArrayOf(width, height)
    }

    private class RecordingRenderer(
        override val type: String,
    ) : JsxGraphRendererAdapter

    private class RecordingAriaAdapter : JsxGraphAriaAdapter {
        val operations = mutableListOf<String>()

        override fun setARIA(
            containerId: String,
            title: String,
        ): GMResult<Unit, JxgApiError> {
            operations += "set:$containerId:$title"
            return GMResult.Ok(Unit)
        }

        override fun removeARIANodes(
            containerId: String,
        ): GMResult<Unit, JxgApiError> {
            operations += "remove:$containerId"
            return GMResult.Ok(Unit)
        }
    }
}
