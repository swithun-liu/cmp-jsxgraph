package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoardObjectRemovalTest {
    @Test
    fun removalRecursesThroughChildrenAndReindexesSurvivors() {
        val board = board()
        val removalLog = mutableListOf<String>()
        val parent = registeredElement(board, "parent", removalLog)
        val child = registeredElement(board, "child", removalLog)
        val grandchild = registeredElement(board, "grandchild", removalLog)
        val survivor = registeredElement(board, "survivor", removalLog)

        parent.addChild(child)
        child.addChild(grandchild)
        survivor.addChild(child)

        board.removeObject(parent)

        assertEquals(listOf("grandchild", "child", "parent"), removalLog)
        assertEquals(setOf("survivor"), board.objects.keys)
        assertEquals(listOf<GeometryElement>(survivor), board.objectsList)
        assertEquals(0, survivor.positionInBoard)
        assertEquals(-1, parent.positionInBoard)
        assertEquals(-1, child.positionInBoard)
        assertEquals(-1, grandchild.positionInBoard)
        assertEquals(4, board.numObjects)
        assertTrue(survivor.childElements.isEmpty())
        assertTrue(survivor.descendants.isEmpty())
    }

    @Test
    fun batchRemovalUpdatesRemainingElementsOnce() {
        val board = board()
        val removalLog = mutableListOf<String>()
        val first = registeredElement(board, "first", removalLog)
        val second = registeredElement(board, "second", removalLog)
        val survivor = UpdatingElement(board, id = "survivor")
        register(board, survivor)

        board.removeObjects(listOf(first, second))

        assertEquals(listOf("first", "second"), removalLog)
        assertEquals(1, survivor.updateCount)
        assertEquals(1, survivor.visibilityUpdateCount)
        assertEquals(1, survivor.rendererUpdateCount)
        assertEquals(listOf<GeometryElement>(survivor), board.objectsList)
    }

    @Test
    fun saveMethodRepairsAChildLinkMissingFromAncestors() {
        val board = board()
        val owner = registeredElement(board, "owner")
        val target = registeredElement(board, "target")
        owner.childElements[target.id] = target
        owner.descendants[target.id] = target

        board.removeObject(target, saveMethod = true)

        assertTrue(owner.childElements.isEmpty())
        assertTrue(owner.descendants.isEmpty())
        assertEquals(listOf(owner), board.objectsList)
    }

    @Test
    fun removingAnAlreadyRemovedReferenceDoesNotRemoveItsFormerNeighbor() {
        val board = board()
        val first = registeredElement(board, "first")
        val survivor = registeredElement(board, "survivor")

        board.removeObject(first)
        board.removeObject(first)

        assertSame(survivor, board.elementById("survivor"))
        assertEquals(listOf(survivor), board.objectsList)
        assertEquals(0, survivor.positionInBoard)
    }

    @Test
    fun recursiveRemovalUsesTheChildsBoardWithoutUpdatingIt() {
        val parentBoard = board("parentBoard")
        val childBoard = board("childBoard")
        val parent = registeredElement(parentBoard, "parent")
        val child = registeredElement(childBoard, "child")
        val childBoardSurvivor = UpdatingElement(childBoard, "survivor")
        register(childBoard, childBoardSurvivor)
        parent.addChild(child)

        parentBoard.removeObject(parent)

        assertTrue(parentBoard.objects.isEmpty())
        assertEquals(
            listOf<GeometryElement>(childBoardSurvivor),
            childBoard.objectsList,
        )
        assertEquals(0, childBoardSurvivor.updateCount)
        assertEquals(0, childBoardSurvivor.positionInBoard)
    }

    @Test
    fun removeAncestorsRemovesTheDependencyChainAndTarget() {
        val board = board()
        val removalLog = mutableListOf<String>()
        val grandparent = registeredElement(board, "grandparent", removalLog)
        val parent = registeredElement(board, "parent", removalLog)
        val target = registeredElement(board, "target", removalLog)
        val survivor = registeredElement(board, "survivor", removalLog)
        grandparent.addChild(parent)
        parent.addChild(target)

        board.removeAncestors(target)

        assertEquals(listOf("target", "parent", "grandparent"), removalLog)
        assertEquals(listOf<GeometryElement>(survivor), board.objectsList)
        assertEquals(0, survivor.positionInBoard)
    }

    private fun board(id: String = "board"): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = id,
    )

    private fun registeredElement(
        board: Board,
        id: String,
        removalLog: MutableList<String>? = null,
    ): GeometryElement {
        val element = if (removalLog == null) {
            GeometryElement(board, id = id)
        } else {
            RemovableElement(board, id, removalLog)
        }
        register(board, element)
        return element
    }

    private fun register(
        board: Board,
        element: GeometryElement,
    ) {
        val result = board.setId(element, type = 1)
        assertTrue(result is GMResult.Ok)
    }

    private class RemovableElement(
        board: Board,
        id: String,
        private val removalLog: MutableList<String>,
    ) : GeometryElement(board = board, id = id) {
        override fun remove(): GeometryElement {
            removalLog += id
            return this
        }
    }

    private class UpdatingElement(
        board: Board,
        id: String,
    ) : GeometryElement(board = board, id = id) {
        var updateCount = 0
        var visibilityUpdateCount = 0
        var rendererUpdateCount = 0

        override fun update(fromParent: Boolean): GeometryElement {
            updateCount += 1
            return this
        }

        override fun updateVisibility(): GeometryElement {
            visibilityUpdateCount += 1
            return this
        }

        override fun updateRenderer(): GeometryElement {
            rendererUpdateCount += 1
            return this
        }
    }
}
