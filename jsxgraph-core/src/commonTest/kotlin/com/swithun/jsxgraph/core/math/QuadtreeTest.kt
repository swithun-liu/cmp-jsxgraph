/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuadtreeTest {
    @Test
    fun boundariesSubdivisionAndTraversalMatchOfficialFixture() {
        val tree = assertIs<GMResult.Ok<Quadtree<TestPoint>>>(
            Quadtree.create<TestPoint>(
                boundingBox = doubleArrayOf(-4.0, 4.0, 4.0, -4.0),
                capacity = 2,
            ),
        ).value
        val points = listOf(
            TestPoint("northWestParent", -3.0, 3.0),
            TestPoint("southEastParent", 3.0, -3.0),
            TestPoint("northEastChild", 2.0, 2.0),
            TestPoint("southWestChild", -2.0, -2.0),
            TestPoint("centerBoundary", 0.0, 0.0),
            TestPoint("leftBoundary", -4.0, 1.0),
        )

        val insertions = points.map { point ->
            assertIs<GMResult.Ok<Boolean>>(tree.insert(point)).value
        }

        assertFalse(tree.contains(-4.0, 1.0))
        assertTrue(tree.contains(4.0, 1.0))
        assertFalse(tree.contains(1.0, -4.0))
        assertTrue(tree.contains(1.0, 4.0))
        assertTrue(tree.contains(0.0, 0.0))
        assertEquals(
            listOf(true, true, true, true, true, false),
            insertions,
        )
        assertEquals(
            listOf("northWestParent", "southEastParent"),
            tree.points.map(TestPoint::id),
        )
        assertEquals(emptyList(), tree.query(-2.0, 2.0)?.ids())
        assertEquals(
            listOf("northEastChild"),
            tree.query(2.0, 2.0)?.ids(),
        )
        assertEquals(
            listOf("southWestChild", "centerBoundary"),
            tree.query(-2.0, -2.0)?.ids(),
        )
        assertEquals(emptyList(), tree.query(2.0, -2.0)?.ids())
        assertEquals(
            listOf("southWestChild", "centerBoundary"),
            tree.query(0.0, 0.0)?.ids(),
        )
        assertNull(tree.query(5.0, 0.0))
        assertTrue(tree.hasPoint(2.05, 2.05, 0.1))
        assertFalse(tree.hasPoint(2.1, 2.0, 0.1))
        assertEquals(
            listOf(
                "northWestParent",
                "southEastParent",
                "northEastChild",
                "southWestChild",
                "centerBoundary",
            ),
            tree.getAllPoints().map(TestPoint::id),
        )
    }

    @Test
    fun invalidConfigurationAndUnboundedSubdivisionFailStructurally() {
        assertIs<GMResult.Err<QuadtreeError.InvalidBoundingBox>>(
            Quadtree.create<TestPoint>(
                boundingBox = doubleArrayOf(-1.0, 1.0, -1.0, 1.0),
            ),
        )
        assertIs<GMResult.Err<QuadtreeError.InvalidCapacity>>(
            Quadtree.create<TestPoint>(
                boundingBox = doubleArrayOf(-1.0, 1.0, 1.0, -1.0),
                capacity = -1,
            ),
        )

        val tree = assertIs<GMResult.Ok<Quadtree<TestPoint>>>(
            Quadtree.create<TestPoint>(
                boundingBox = doubleArrayOf(-1.0, 1.0, 1.0, -1.0),
                capacity = 1,
                maximumDepth = 1,
            ),
        ).value
        assertIs<GMResult.Ok<Boolean>>(
            tree.insert(TestPoint("first", 0.5, 0.5)),
        )
        assertIs<GMResult.Ok<Boolean>>(
            tree.insert(TestPoint("second", 0.5, 0.5)),
        )
        val failure = assertIs<GMResult.Err<QuadtreeError>>(
            tree.insert(TestPoint("third", 0.5, 0.5)),
        )
        val depth =
            assertIs<QuadtreeError.MaximumDepthExceeded>(failure.error)
        assertEquals(1, depth.maximumDepth)
        assertEquals(0.5, depth.x)
        assertEquals(0.5, depth.y)
    }

    private fun Quadtree<TestPoint>.ids(): List<String> =
        points.map(TestPoint::id)

    private data class TestPoint(
        val id: String,
        override val quadtreeX: Double,
        override val quadtreeY: Double,
    ) : QuadtreePoint
}
