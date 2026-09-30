/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ReaderTest {
    @Test
    fun registrationIsCaseInsensitiveAndFirstRegistrationWins() {
        val events = mutableListOf<String>()
        val board = FixtureBoard()
        val registry = ReaderRegistry<FixtureBoard>()
        registry.registerReader(
            reader = JsxGraphReaderFactory { target, source ->
                events += "first:new:$source"
                GMResult.Ok(
                    JsxGraphReader {
                        events += "first:read"
                        target.selected = "first"
                        GMResult.Ok(Unit)
                    },
                )
            },
            extensions = listOf("FixtureReader"),
        )
        registry.registerReader(
            reader = JsxGraphReaderFactory { target, source ->
                events += "second:new:$source"
                GMResult.Ok(
                    JsxGraphReader {
                        events += "second:read"
                        target.selected = "second"
                        GMResult.Ok(Unit)
                    },
                )
            },
            extensions = listOf("fixturereader"),
        )

        val result = FileReader.parseString(
            source = "payload",
            board = board,
            format = "FIXTUREREADER",
            registry = registry,
            callback = { events += "callback" },
        )

        assertIs<GMResult.Ok<Unit>>(result)
        assertEquals("first", board.selected)
        assertEquals(
            listOf(
                "first:new:payload",
                "first:read",
                "callback",
            ),
            events,
        )
    }

    @Test
    fun jessieCodeSkipsReaderAndStillInvokesCallback() {
        val board = FixtureBoard()
        val registry = ReaderRegistry<FixtureBoard>()
        var callbackBoard: FixtureBoard? = null

        val result = FileReader.parseString(
            source = "ignored",
            board = board,
            format = "JESSIECODE",
            registry = registry,
            callback = { callbackBoard = it },
        )

        assertIs<GMResult.Ok<Unit>>(result)
        assertEquals(board, callbackBoard)
    }

    @Test
    fun unknownFormatIsStructuredAndDoesNotInvokeCallback() {
        val registry = ReaderRegistry<FixtureBoard>()
        var callbackBoard: FixtureBoard? = null

        val result = FileReader.parseString(
            source = "",
            board = FixtureBoard(),
            format = "Missing-Reader",
            registry = registry,
            callback = { callbackBoard = it },
        )

        val error = assertIs<
            GMResult.Err<ReaderError.UnknownFormat>
        >(result).error
        assertEquals("missing-reader", error.format)
        assertNull(callbackBoard)
    }

    @Test
    fun extensionFailuresAreContainedAtTheFileReaderBoundary() {
        val constructionRegistry = ReaderRegistry<FixtureBoard>()
        constructionRegistry.registerReader(
            reader = JsxGraphReaderFactory { _, _ ->
                throw IllegalStateException("construction")
            },
            extensions = listOf("construct"),
        )
        val constructionError = assertIs<
            GMResult.Err<ReaderError.ConstructionFailed>
        >(
            FileReader.parseString(
                source = "",
                board = FixtureBoard(),
                format = "construct",
                registry = constructionRegistry,
            ),
        ).error
        assertEquals("construction", constructionError.message)

        val readRegistry = ReaderRegistry<FixtureBoard>()
        readRegistry.registerReader(
            reader = JsxGraphReaderFactory { _, _ ->
                GMResult.Ok(
                    JsxGraphReader {
                        throw IllegalStateException("read")
                    },
                )
            },
            extensions = listOf("read"),
        )
        val readError = assertIs<
            GMResult.Err<ReaderError.ReadFailed>
        >(
            FileReader.parseString(
                source = "",
                board = FixtureBoard(),
                format = "read",
                registry = readRegistry,
            ),
        ).error
        assertEquals("read", readError.message)

        val callbackError = assertIs<
            GMResult.Err<ReaderError.CallbackFailed>
        >(
            FileReader.parseString(
                source = "",
                board = FixtureBoard(),
                format = "jessiecode",
                registry = ReaderRegistry(),
                callback = {
                    throw IllegalStateException("callback")
                },
            ),
        ).error
        assertEquals("callback", callbackError.message)
    }

    @Test
    fun browserOnlySourcesAreExplicitlyUnsupportedInCommonMain() {
        val remote = FileContentSource.RemoteUrl(
            "https://example.invalid/graph.txt",
        )
        val blob = FileContentSource.LocalBlob("fixture")

        assertEquals(
            ReaderError.UnsupportedSource(remote),
            assertIs<GMResult.Err<ReaderError.UnsupportedSource>>(
                FileReader.parseFileContent(remote),
            ).error,
        )
        assertEquals(
            ReaderError.UnsupportedSource(blob),
            assertIs<GMResult.Err<ReaderError.UnsupportedSource>>(
                FileReader.parseFileContent(blob),
            ).error,
        )
    }

    private data class FixtureBoard(
        var selected: String? = null,
    )
}
