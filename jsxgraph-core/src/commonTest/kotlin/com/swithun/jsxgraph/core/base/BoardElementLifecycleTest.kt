package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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

    @Test
    fun elementAttributesExpandShortcutsAndMergeNestedValues() {
        val element = registeredElement(board(), "element")

        element.setAttribute(
            mapOf(
                "color" to "#123456",
                "opacity" to 0.4,
                "strokeWidth" to 3,
                "label" to mapOf(
                    "visible" to true,
                    "anchorX" to "left",
                ),
            ),
        )
        element.setAttribute(
            mapOf(
                "label" to mapOf("visible" to false),
            ),
        )

        assertEquals("#123456", element.getAttribute("strokecolor"))
        assertEquals("#123456", element.getAttribute("fillcolor"))
        assertEquals(0.4, element.getAttribute("strokeopacity"))
        assertEquals(0.4, element.getAttribute("fillopacity"))
        assertEquals(3, element.getAttribute("strokewidth"))
        assertEquals(3, element.getAttribute("highlightstrokewidth"))
        assertEquals(false, element.getAttribute("label.visible"))
        assertEquals("left", element.getAttribute("label.anchorx"))
    }

    @Test
    fun elementDynamicPropertiesUseElementAndContainFailures() {
        val element = registeredElement(board(), "element")
        element.setAttribute(
            mapOf(
                "dynamic" to GeometryElementPropertyEvaluator {
                    it.id
                },
                "zeroarg" to { 7 },
                "broken" to GeometryElementPropertyEvaluator {
                    error("expected")
                },
            ),
        )

        assertEquals("element", element.evalVisProp("dynamic"))
        assertEquals(7, element.evalVisProp("zeroarg"))
        assertNull(element.evalVisProp("broken"))
        assertEquals(
            GeometryElementEvaluationError.CallbackFailed("expected"),
            element.propertyEvaluationError,
        )
    }

    @Test
    fun elementAliasesAndSharedStateMatchOfficialElement() {
        val board = board()
        val element = registeredElement(board, "element")
        element.isDraggable = true
        element.elType = "line"
        element.setAttribute(
            mapOf(
                "fixed" to false,
                "highlight" to true,
            ),
        )

        assertTrue(element.draggable())
        element.setAttribute(mapOf("fixed" to true))
        assertFalse(element.draggable())
        element.setAttribute(mapOf("fixed" to false))
        element.type = Const.OBJECT_TYPE_GLIDER
        assertFalse(element.draggable())

        element.type = Const.OBJECT_TYPE_LINE
        element
            .strokeColor("#112233")
            .fillColor("#445566")
            .strokeWidth(4)
            .dash(2)
            .visible(false)
            .shadow(true)
        assertEquals("#112233", element.getProperty("strokeColor"))
        assertEquals("#445566", element.getProperty("fillColor"))
        assertEquals(4, element.getProperty("strokeWidth"))
        assertEquals(2, element.getProperty("dash"))
        assertEquals(false, element.getProperty("visible"))
        assertEquals(true, element.getProperty("shadow"))

        element.highlight()
        assertTrue(element.highlighted)
        assertSame(element, board.highlightedObjects[element.id])
        element.noHighlight()
        assertFalse(element.highlighted)
        assertFalse(element.id in board.highlightedObjects)
        assertEquals("line", element.getType())
        assertEquals(element.name, element.nameValue())
    }

    @Test
    fun elementTicksAndFallbackBoundsMatchBaseContract() {
        val board = board()
        val element = registeredElement(board, "element")
        val axis = Line(
            board = board,
            point1 = Point(board, doubleArrayOf(0.0, 0.0)),
            point2 = Point(board, doubleArrayOf(1.0, 0.0)),
            id = "axis",
        )
        registeredId(board.setId(axis, type = Const.OBJECT_TYPE_LINE))
        val ticks = assertIs<GMResult.Ok<Ticks>>(
            Ticks.create(board = board, parent = axis),
        ).value
        axis.ticks.clear()

        assertEquals(ticks.id, axis.addTicks(ticks))
        assertEquals(listOf(ticks), axis.ticks)
        axis.removeTicks(ticks)
        assertTrue(axis.ticks.isEmpty())
        assertTrue(element.getBoundingBox()[0].isInfinite())
    }

    @Test
    fun genericPositionMergesTranslationsWithoutParentPoints() {
        val element = registeredElement(board(), "element")

        assertIs<GMResult.Ok<GeometryElement>>(
            element.setPositionResult(
                Const.COORDS_BY_USER,
                doubleArrayOf(2.0, 3.0),
            ),
        )
        assertIs<GMResult.Ok<GeometryElement>>(
            element.setPositionResult(
                Const.COORDS_BY_USER,
                doubleArrayOf(1.0, -1.0),
            ),
        )

        assertEquals(1, element.transformations.size)
        val matrix = element.transformations.single().matrix
        assertEquals(3.0, matrix[1][0], absoluteTolerance = 1e-12)
        assertEquals(2.0, matrix[2][0], absoluteTolerance = 1e-12)
    }

    @Test
    fun genericPositionMovesOnlyDraggableParentPoints() {
        val board = board()
        val first = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 2.0),
            id = "first",
        )
        val second = Point(
            board = board,
            coordinates = doubleArrayOf(-2.0, 4.0),
            id = "second",
        )
        registeredId(board.setId(first, Const.OBJECT_TYPE_POINT))
        registeredId(board.setId(second, Const.OBJECT_TYPE_POINT))
        val element = registeredElement(board, "element")
        element.addParents(listOf(first, second))

        assertIs<GMResult.Ok<GeometryElement>>(
            element.setPositionDirectlyBase(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(5.0, 7.0),
                oldCoordinates = doubleArrayOf(3.0, 4.0),
            ),
        )
        assertEquals(3.0, first.X(), absoluteTolerance = 1e-12)
        assertEquals(5.0, first.Y(), absoluteTolerance = 1e-12)
        assertEquals(0.0, second.X(), absoluteTolerance = 1e-12)
        assertEquals(7.0, second.Y(), absoluteTolerance = 1e-12)

        first.setAttribute(mapOf("fixed" to true))
        element.setPosition(
            Const.COORDS_BY_USER,
            doubleArrayOf(10.0, 10.0),
        )
        assertEquals(3.0, first.X(), absoluteTolerance = 1e-12)
        assertEquals(0.0, second.X(), absoluteTolerance = 1e-12)
    }

    @Test
    fun elementAnimationInterpolatesSupportedPropertiesAndCallsBack() {
        val board = board()
        val point = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 2.0),
            id = "point",
        )
        registeredId(board.setId(point, Const.OBJECT_TYPE_POINT))
        point.setAttribute(
            mapOf(
                "strokeColor" to "#ff0000",
                "fillColor" to "#000000",
                "strokeOpacity" to 0.0,
                "strokeWidth" to 1.0,
                "fillOpacity" to 0.25,
                "size" to 1.0,
            ),
        )
        var callbacks = 0

        val result = point.animateResult(
            targetAttributes = mapOf(
                "strokeColor" to "#00ff00",
                "fillColor" to "#ffffff",
                "strokeOpacity" to 1.0,
                "strokeWidth" to 5.0,
                "fillOpacity" to 0.75,
                "size" to 6.9,
                "unsupported" to 9,
            ),
            timeMilliseconds = board.animationDelay * 3.0,
            options = GeometryElementAnimationOptions {
                callbacks += 1
            },
        )

        assertIs<GMResult.Ok<GeometryElement>>(result)
        assertEquals("#00ff00", point.getAttribute("strokecolor"))
        assertEquals("#ffffff", point.getAttribute("fillcolor"))
        assertEquals(
            1.0,
            point.getAttribute("strokeopacity") as Double,
            absoluteTolerance = 1e-12,
        )
        assertEquals(
            5.0,
            point.getAttribute("strokewidth") as Double,
            absoluteTolerance = 1e-12,
        )
        assertEquals(
            0.75,
            point.getAttribute("fillopacity") as Double,
            absoluteTolerance = 1e-12,
        )
        assertEquals(6.0, point.getAttribute("size"))
        assertNull(point.getAttribute("unsupported"))
        assertEquals(1, callbacks)
        assertTrue(point.animationData.isEmpty())
    }

    @Test
    fun elementAnimationContainsInvalidColorAndCallbackFailures() {
        val element = registeredElement(board(), "element")
        element.setAttribute(mapOf("strokeColor" to "not-a-color"))

        assertIs<GMResult.Err<GeometryElementAnimationError.InvalidColor>>(
            element.animateResult(
                targetAttributes = mapOf("strokeColor" to "#ffffff"),
                timeMilliseconds = 10.0,
            ),
        )

        element.setAttribute(mapOf("strokeWidth" to 1.0))
        element.animate(
            targetAttributes = mapOf("strokeWidth" to 2.0),
            timeMilliseconds = 10.0,
            options = GeometryElementAnimationOptions {
                error("callback")
            },
        )
        assertEquals(
            GeometryElementAnimationError.CallbackFailed("callback"),
            element.animationError,
        )
    }

    @Test
    fun elementEventsAndAttributeEventsMatchEventEmitterContract() {
        val element = registeredElement(board(), "element")
        val calls = mutableListOf<String>()
        val handler = com.swithun.jsxgraph.core.utils.EventHandler {
                context,
                arguments,
            ->
            calls += "$context:${arguments[1]}"
        }

        element.addEvent("attribute:visible", handler, "ctx")
        element.setAttribute(mapOf("visible" to false))
        element.removeEvent("attribute:visible", handler)
        element.setAttribute(mapOf("visible" to true))

        assertEquals(listOf("ctx:false"), calls)
    }

    @Test
    fun elementLabelIsRegisteredAnchoredEscapedAndVisibilityAware() {
        val board = board()
        val point = Point(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
            id = "point",
            name = "P",
        )
        registeredId(board.setId(point, Const.OBJECT_TYPE_POINT))
        point.setAttribute(
            mapOf(
                "label" to mapOf(
                    "position" to "urt",
                    "offset" to listOf(4.0, 5.0),
                ),
            ),
        )

        point.setLabel("<P>")

        val label = requireNotNull(point.elementLabel)
        assertTrue(point.hasLabel)
        assertSame(label, board.elementById("pointLabel"))
        assertSame(point, label.anchor)
        assertEquals("&lt;P&gt;", label.plaintext)
        assertEquals("inherit", label.getAttribute("visible"))
        assertEquals(listOf(4.0, 5.0), label.screenOffset.toList())

        point.hideElement().updateVisibility()
        assertFalse(point.displayed)
        assertEquals(false, label.visualPropertiesCalculated["visible"])
        point.showElement().updateVisibility()
        assertTrue(point.displayed)
        assertEquals(true, label.visualPropertiesCalculated["visible"])
    }

    @Test
    fun inheritedVisibilityAndBaseNoOpsMatchOfficialElement() {
        val board = board()
        val parent = registeredElement(board, "parent")
        val inherited = registeredElement(board, "inherited")
        parent.inherits += inherited
        inherited.setAttribute(mapOf("visible" to "inherit"))

        parent.hideElement().updateVisibility()
        assertFalse(inherited.displayed)
        parent.showElement().updateVisibility()
        assertTrue(inherited.displayed)

        assertSame(parent, parent.snapToGrid())
        assertSame(parent, parent.snapToPoints())
        assertSame(parent, parent.cloneToBackground())
        assertFalse(parent.hasPoint(1.0, 2.0))
        assertEquals("parent", parent.getAttributes()["id"])
        assertEquals(parent.name, parent.getAttributes()["name"])
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

        override fun updateVisibility(
            parentVisible: Boolean?,
        ): GeometryElement {
            log += "$id:visibility"
            return this
        }

        override fun updateRenderer(): GeometryElement {
            log += "$id:renderer"
            return this
        }
    }
}
