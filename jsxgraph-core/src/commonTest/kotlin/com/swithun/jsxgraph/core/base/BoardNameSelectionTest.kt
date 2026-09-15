package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class BoardNameSelectionTest {
    @Test
    fun elementDefaultsToTheOfficialOtherClass() {
        assertEquals(
            Const.OBJECT_CLASS_OTHER,
            GeometryElement(board()).elementClass,
        )
    }

    @Test
    fun generatedNamesMatchOfficialTypePrefixes() {
        assertEquals(
            "A",
            generatedName(Const.OBJECT_TYPE_POINT, Const.OBJECT_CLASS_POINT),
        )
        assertEquals(
            "A",
            generatedName(Const.OBJECT_TYPE_POINT3D, Const.OBJECT_CLASS_3D),
        )
        assertEquals(
            "a",
            generatedName(Const.OBJECT_TYPE_LINE, Const.OBJECT_CLASS_LINE),
        )
        assertEquals(
            "&alpha;",
            generatedName(Const.OBJECT_TYPE_ANGLE, Const.OBJECT_CLASS_OTHER),
        )
        assertEquals(
            "P_{a}",
            generatedName(Const.OBJECT_TYPE_POLYGON, Const.OBJECT_CLASS_AREA),
        )
        assertEquals(
            "k_{a}",
            generatedName(Const.OBJECT_TYPE_CIRCLE, Const.OBJECT_CLASS_CIRCLE),
        )
        assertEquals(
            "t_{a}",
            generatedName(Const.OBJECT_TYPE_TEXT, Const.OBJECT_CLASS_TEXT),
        )
        assertEquals(
            "s_{a}",
            generatedName(Const.OBJECT_TYPE_CURVE, Const.OBJECT_CLASS_CURVE),
        )
        assertEquals(
            "",
            generatedName(Const.OBJECT_TYPE_TICKS, Const.OBJECT_CLASS_OTHER),
        )
    }

    @Test
    fun generatedNamesAdvanceThroughConfiguredMaximumLength() {
        val board = board(maxNameLength = 2)

        val names = List(28) {
            registeredElement(
                board = board,
                type = Const.OBJECT_TYPE_POINT,
                elementClass = Const.OBJECT_CLASS_POINT,
            ).name
        }

        assertEquals(
            ('A'..'Z').map(Char::toString) + listOf("AA", "AB"),
            names,
        )
    }

    @Test
    fun exhaustedGeneratedNamesRemainUnregistered() {
        val board = board(maxNameLength = 1)
        repeat(26) {
            registeredElement(
                board = board,
                type = Const.OBJECT_TYPE_POINT,
                elementClass = Const.OBJECT_CLASS_POINT,
            )
        }

        val exhausted = registeredElement(
            board = board,
            type = Const.OBJECT_TYPE_POINT,
            elementClass = Const.OBJECT_CLASS_POINT,
        )

        assertEquals("", exhausted.name)
        assertNull(board.elementByName(""))
    }

    @Test
    fun explicitAndRenamedNamesSynchronizeTheNameRegistry() {
        val board = board()
        val unnamed = registeredElement(board = board, name = "")
        val first = registeredElement(board = board, name = "A")
        val replacement = registeredElement(board = board, name = "A")

        assertNull(board.elementByName(unnamed.name))
        assertSame(replacement, board.elementByName("A"))

        replacement.setName("B")
        assertNull(board.elementByName("A"))
        assertSame(replacement, board.elementByName("B"))

        replacement.setName("")
        assertNull(board.elementByName("B"))
        assertSame(replacement, board.elementByName(""))
        assertNull(board.select(""))
        assertEquals("A", first.name)
    }

    @Test
    fun selectUsesIdBeforeNameAndRejectsDeletedElements() {
        val board = board()
        val byId = registeredElement(
            board = board,
            id = "shared",
            name = "by-id",
        )
        val byName = registeredElement(
            board = board,
            id = "other",
            name = "shared",
        )

        assertSame(byId, board.select("shared"))
        assertSame(byId, board.select("by-id"))
        assertSame(byName, board.select(byName))
        assertNull(board.select("missing"))

        board.removeObject(byName)

        assertNull(board.select(byName))
        assertNull(board.elementByName("shared"))
    }

    @Test
    fun removeObjectAcceptsNamesAndCleansTheRegistry() {
        val board = board()
        val element = registeredElement(
            board = board,
            id = "element-id",
            name = "element-name",
        )

        board.removeObject("element-name")

        assertNull(board.elementById(element.id))
        assertNull(board.elementByName(element.name))
    }

    @Test
    fun rejectedDuplicateIdDoesNotLeaveAnOrphanedName() {
        val board = board()
        registeredElement(board = board, id = "fixed", name = "first")
        val duplicate = GeometryElement(
            board = board,
            id = "fixed",
            name = "orphan",
        )

        assertIs<GMResult.Err<BoardError.DuplicateElementId>>(
            board.setId(duplicate, type = Const.OBJECT_TYPE_POINT),
        )
        assertNull(board.elementByName("orphan"))
    }

    private fun generatedName(
        type: Int,
        elementClass: Int,
    ): String {
        val board = board()
        return registeredElement(
            board = board,
            type = type,
            elementClass = elementClass,
        ).name
    }

    private fun board(maxNameLength: Int = 1): Board = Board(
        originX = 0.0,
        originY = 0.0,
        unitX = 1.0,
        unitY = 1.0,
        id = "board",
        maxNameLength = maxNameLength,
    )

    private fun registeredElement(
        board: Board,
        id: String = "",
        name: String? = null,
        type: Int = Const.OBJECT_TYPE_CURVE,
        elementClass: Int = Const.OBJECT_CLASS_CURVE,
    ): GeometryElement = GeometryElement(
        board = board,
        id = id,
        name = name,
        type = type,
        elementClass = elementClass,
    ).also { element ->
        assertIs<GMResult.Ok<String>>(board.setId(element, type))
    }
}
