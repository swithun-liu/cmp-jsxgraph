package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoardElementLifecycleTest {
    @Test
    fun boardRegistersExplicitAndGeneratedIdsInCreationOrder() {
        val board = board(id = "board")
        val explicit = GeometryElement(board, id = "board71")
        val generated = GeometryElement(board)
        val next = GeometryElement(board)

        assertEquals("board71", registeredId(board.setId(explicit, type = 7)))
        assertEquals("board71-1", registeredId(board.setId(generated, type = 7)))
        assertEquals("board22", registeredId(board.setId(next, type = 2)))
        assertEquals(3, board.numObjects)
        assertEquals(listOf(explicit, generated, next), board.objectsList)
        assertEquals(0, explicit.positionInBoard)
        assertEquals(1, generated.positionInBoard)
        assertEquals(2, next.positionInBoard)
        assertSame(generated, board.elementById("board71-1"))
    }

    @Test
    fun duplicateExplicitIdReportsErrorWithoutMutatingRegistry() {
        val board = board()
        val first = GeometryElement(board, id = "fixed")
        val duplicate = GeometryElement(board, id = "fixed")

        registeredId(board.setId(first, type = 1))
        assertIs<GMResult.Err<BoardError.DuplicateElementId>>(
            board.setId(duplicate, type = 1),
        )
        assertEquals(1, board.numObjects)
        assertEquals(listOf(first), board.objectsList)
        assertSame(first, board.elementById("fixed"))
        assertEquals(-1, duplicate.positionInBoard)
    }

    @Test
    fun elementDependenciesPropagateAcrossExistingAndNewChains() {
        val board = board()
        val first = registeredElement(board, "first")
        val second = registeredElement(board, "second")
        val third = registeredElement(board, "third")
        val fourth = registeredElement(board, "fourth")

        second.addChild(third)
        first.addChild(second)
        third.addChild(fourth)

        assertEquals(listOf("second"), first.childElements.keys.toList())
        assertEquals(
            setOf("second", "third", "fourth"),
            first.descendants.keys,
        )
        assertEquals(setOf("first"), second.ancestors.keys)
        assertEquals(setOf("first", "second"), third.ancestors.keys)
        assertEquals(
            setOf("first", "second", "third"),
            fourth.ancestors.keys,
        )

        fourth.addParents(listOf(first, second, first))
        assertEquals(listOf("first", "second"), fourth.parents)
        fourth.setParents(listOf(third))
        assertEquals(listOf("third"), fourth.parents)
    }

    @Test
    fun childRemovalMatchesUpstreamDirectCleanup() {
        val board = board()
        val first = registeredElement(board, "first")
        val second = registeredElement(board, "second")
        val third = registeredElement(board, "third")
        second.addChild(third)
        first.addChild(second)

        first.removeChild(second)

        assertTrue(first.childElements.isEmpty())
        assertTrue(first.descendants.isEmpty())
        assertFalse("first" in second.ancestors)
        assertTrue("second" in third.ancestors)
    }

    @Test
    fun childCountExcludesLabelsLikeOfficialElement() {
        val board = board()
        val parent = registeredElement(board, "parent")
        parent.addChild(registeredElement(board, "child"))
        parent.addChild(registeredElement(board, "childLabel"))
        parent.addChild(registeredElement(board, "LabelPrefix"))

        assertEquals(1, parent.countChildren())
    }

    @Test
    fun boardUpdatePreservesCreationOrderAndDragFlag() {
        val board = board()
        val log = mutableListOf<String>()
        val first = RecordingElement(
            board = board,
            id = "first",
            needsRegularUpdate = true,
            log = log,
        )
        val second = RecordingElement(
            board = board,
            id = "second",
            needsRegularUpdate = false,
            log = log,
        )
        registeredId(board.setId(first, type = 1))
        registeredId(board.setId(second, type = 1))

        board.update(draggedElement = first)

        assertEquals(
            listOf(
                "first:update:false:true",
                "first:visibility",
                "second:update:true:false",
                "second:visibility",
                "first:renderer",
                "second:renderer",
            ),
            log,
        )
        assertFalse(board.inUpdate)
    }

    @Test
    fun suspendedAndReentrantUpdatesMatchBoardGuards() {
        val board = board()
        val log = mutableListOf<String>()
        val element = RecordingElement(
            board = board,
            id = "element",
            needsRegularUpdate = false,
            log = log,
            reenterBoard = true,
            suspendBoardDuringUpdate = true,
        )
        registeredId(board.setId(element, type = 1))

        board.unsuspendUpdate()
        assertTrue(log.isEmpty())

        board.suspendUpdate().update()
        assertTrue(log.isEmpty())

        board.unsuspendUpdate()
        assertEquals(
            listOf(
                "element:update:true:true",
                "element:visibility",
                "element:renderer",
            ),
            log,
        )
        assertFalse(board.isSuspendedUpdate)
        assertFalse(board.needsFullUpdate)
        assertFalse(board.inUpdate)
    }

    @Test
    fun elementFullUpdateRunsItsOwnLifecycleChain() {
        val board = board()
        val log = mutableListOf<String>()
        val element = RecordingElement(
            board = board,
            id = "element",
            needsRegularUpdate = true,
            log = log,
        )

        element.needsUpdate = false
        element.fullUpdate()

        assertEquals(
            listOf(
                "element:prepare",
                "element:update:true:true",
                "element:visibility",
                "element:renderer",
            ),
            log,
        )
    }

    @Test
    fun parentIdsAndPropertyGettersMatchOfficialElement() {
        val board = board()
        val first = registeredElement(board, "first")
        val second = registeredElement(board, "second")
        val target = registeredElement(board, "target")

        target
            .addParents(listOf(first, first))
            .addParentIds(listOf("missing", second.id, first.id))

        assertEquals(listOf("first", "second"), target.parents)

        target.setParents(listOf(second))
        assertEquals(listOf("second"), target.parents)
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
    ): GeometryElement = GeometryElement(board, id = id).also { element ->
        registeredId(board.setId(element, type = 1))
    }

    private fun registeredId(
        result: GMResult<String, BoardError>,
    ): String = assertIs<GMResult.Ok<String>>(result).value

    private class RecordingElement(
        board: Board,
        id: String,
        needsRegularUpdate: Boolean,
        private val log: MutableList<String>,
        private val reenterBoard: Boolean = false,
        private val suspendBoardDuringUpdate: Boolean = false,
    ) : GeometryElement(
        board = board,
        id = id,
        needsRegularUpdate = needsRegularUpdate,
    ) {
        override fun prepareUpdate(): GeometryElement {
            log += "$id:prepare"
            return super.prepareUpdate()
        }

        override fun update(fromParent: Boolean): GeometryElement {
            log += "$id:update:$fromParent:$needsUpdate"
            if (suspendBoardDuringUpdate) {
                board.suspendUpdate()
            }
            if (reenterBoard) {
                board.update()
            }
            return this
        }

        override fun updateVisibility(): GeometryElement {
            log += "$id:visibility"
            return this
        }

        override fun updateRenderer(): GeometryElement {
            log += "$id:renderer"
            return this
        }
    }
}
