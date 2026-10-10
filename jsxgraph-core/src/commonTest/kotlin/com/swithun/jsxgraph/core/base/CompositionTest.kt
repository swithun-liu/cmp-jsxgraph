/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.TypeFilterCriterion
import com.swithun.jsxgraph.core.utils.TypeFilterPredicate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CompositionTest {
    @Test
    fun nameIndexTracksOfficialDuplicateAndRenameOrder() {
        val board = board()
        val first = point(board, "first", "shared")
        val second = point(board, "second", "shared")
        val composition = Composition(
            linkedMapOf(
                "firstRole" to first,
                "secondRole" to second,
            ),
        )

        assertSame(first, composition.select("first"))
        assertSame(second, composition.select("shared"))
        assertEquals("missing", composition.select("missing"))
        assertEquals("", composition.select(""))

        first.setAttribute(mapOf("name" to "first-renamed"))
        second.setAttribute(mapOf("name" to "second-renamed"))

        assertSame(first, composition.select("first-renamed"))
        assertSame(second, composition.select("second-renamed"))
        assertEquals("shared", composition.select("shared"))
        assertEquals(
            listOf("first-renamed", "second-renamed"),
            composition.elementsByName.keys.toList(),
        )
    }

    @Test
    fun objectAndFunctionFiltersReturnIndependentCompositions() {
        val board = board()
        val first = point(board, "first", "first-name").apply {
            setAttribute(
                mapOf(
                    "visible" to true,
                    "strokeColor" to "red",
                ),
            )
        }
        val second = point(board, "second", "second-name").apply {
            setAttribute(
                mapOf(
                    "visible" to false,
                    "strokeColor" to "blue",
                ),
            )
        }
        val third = point(board, "third", "third-name").apply {
            setAttribute(
                mapOf(
                    "visible" to true,
                    "strokeColor" to "blue",
                ),
            )
        }
        val composition = Composition(
            linkedMapOf(
                "firstRole" to first,
                "secondRole" to second,
                "thirdRole" to third,
            ),
        )

        val objectFiltered = assertIs<GMResult.Ok<Composition>>(
            composition.select(
                linkedMapOf(
                    "visible" to TypeFilterCriterion.Exact(true),
                    "strokeColor" to TypeFilterCriterion.Exact("red"),
                ),
            ),
        ).value
        val functionFiltered = assertIs<GMResult.Ok<Composition>>(
            composition.select(
                TypeFilterPredicate { element ->
                    element.id >= "second"
                },
            ),
        ).value

        assertEquals(
            listOf<GeometryElement>(first),
            objectFiltered.objectsList,
        )
        assertEquals(
            listOf<GeometryElement>(second, third),
            functionFiltered.objectsList,
        )
        assertSame(first, objectFiltered.member("first"))
        assertSame(second, composition.member("secondRole"))
        assertFalse(objectFiltered.remove("missing"))
        assertTrue(composition.remove("firstRole"))
        assertEquals(
            listOf<GeometryElement>(second, third),
            composition.objectsList,
        )
    }

    @Test
    fun generatedMethodsForwardInMemberOrderAndReturnComposition() {
        val board = board()
        val first = point(board, "first", "")
        val second = point(board, "second", "")
        val composition = Composition(
            members = linkedMapOf(
                "firstRole" to first,
                "secondRole" to second,
            ),
            attributes = mapOf("Layer" to 5),
        )

        assertSame(
            composition,
            composition.setAttribute(
                mapOf(
                    "strokeColor" to "green",
                    "strokeWidth" to 4,
                ),
            ),
        )
        assertSame(composition, composition.prepareUpdate())
        assertSame(composition, composition.update())
        assertSame(composition, composition.fullUpdate())
        assertSame(composition, composition.updateRenderer())
        assertSame(composition, composition.highlight(force = true))
        assertSame(composition, composition.noHighlight())

        for (element in listOf(first, second)) {
            assertEquals("green", element.evalVisProp("strokecolor"))
            assertEquals(4, element.evalVisProp("strokewidth"))
            assertFalse(element.highlighted)
        }
        assertEquals(
            mapOf(
                "layer" to 5,
                "strokecolor" to "green",
                "strokewidth" to 4,
            ),
            composition.getAttributes(),
        )
    }

    private fun point(
        board: Board,
        id: String,
        name: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                id = id,
                name = name,
            ),
        ).value

    private fun board(): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = "composition-board",
    )
}
