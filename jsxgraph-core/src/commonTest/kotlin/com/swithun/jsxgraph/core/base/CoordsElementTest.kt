package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CoordsElementTest {
    private val board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
    )

    @Test
    fun coordinateAccessorsMatchOfficialBehavior() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, -3.0),
        )

        assertEquals(2.0, element.X())
        assertEquals(-3.0, element.Y())
        assertEquals(1.0, element.Z())
        assertEquals(element.X(), element.XEval())
        assertEquals(element.Y(), element.YEval())
        assertEquals(element.Z(), element.ZEval())
        assertContentEquals(doubleArrayOf(2.0, -3.0), element.Coords())
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -3.0),
            element.Coords(withZ = true),
        )
    }

    @Test
    fun distanceUsesUserCoordinatesAndRejectsIdealPoints() {
        val first = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
        )
        val second = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(4.0, 5.0),
        )
        val ideal = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(0.0, 1.0, 1.0),
        )

        assertEquals(5.0, first.Dist(second))
        assertEquals(5.0, second.Dist(first))
        assertTrue(first.Dist(ideal).isNaN())
        assertTrue(ideal.Dist(first).isNaN())
    }

    @Test
    fun coordinateCopiesDoNotExposeInternalArrays() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
        )

        val coordinates = element.Coords(withZ = true)
        coordinates[1] = 100.0

        assertEquals(2.0, element.X())
        assertEquals(3.0, element.Y())
    }

    @Test
    fun constructorStateAndElementInheritanceMatchOfficialBehavior() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
        )

        assertIs<GeometryElement>(element)
        assertNull(element.position)
        assertFalse(element.isConstrained)
        assertFalse(element.onPolygon)
        assertNull(element.slideObject)
        assertTrue(element.slideObjects.isEmpty())
        assertTrue(element.needsUpdateFromParent)
        assertTrue(element.isDraggable)
    }

    @Test
    fun setPositionDirectlyRunsTheFreeElementLifecycleInOfficialOrder() {
        val calls = mutableListOf<String>()
        val element = RecordingCoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
            calls = calls,
        )
        element.needsUpdate = false

        val returned = element.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, -1.0),
        )

        assertSame(element, returned)
        assertContentEquals(doubleArrayOf(1.0, 4.0, -1.0), element.coords.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 450.0, 240.0), element.coords.scrCoords)
        assertContentEquals(doubleArrayOf(1.0, 4.0, -1.0), element.actualCoords.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0), element.initialCoords.usrCoords)
        assertEquals(
            listOf("grid", "points", "attractors", "prepare", "update:false"),
            calls,
        )
        assertTrue(element.needsUpdate)
    }

    @Test
    fun setPositionConvertsScreenAndHomogeneousUserCoordinates() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
        )

        element.setPosition(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(350.0, 120.0),
        )
        assertContentEquals(doubleArrayOf(1.0, 2.0, 2.0), element.coords.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 2.0, 2.0), element.actualCoords.usrCoords)

        element.setPosition(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, 8.0, -4.0),
        )
        assertContentEquals(doubleArrayOf(1.0, 4.0, -2.0), element.coords.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 450.0, 280.0), element.coords.scrCoords)
        assertContentEquals(doubleArrayOf(1.0, 4.0, -2.0), element.actualCoords.usrCoords)
    }

    @Test
    fun nonPointTransformLifecycleUsesActualCoordsAndStableBaseElement() {
        val base = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(3.0, 2.0),
            id = "base",
        )
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            id = "transformed",
        )
        val translate = transformation("translate", 2.0, 3.0)
        val scale = transformation("scale", 2.0, -1.0)

        element.addTransform(base, listOf(translate, scale))
        element.updateTransform(fromParent = true)

        assertSame(base, element.baseElement)
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            element.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 10.0, -5.0),
            element.actualCoords.usrCoords,
        )
        assertNull(element.transformationEvaluationError)

        val otherBase = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(100.0, 100.0),
        )
        element.addTransform(
            otherBase,
            transformation("translate", -1.0, 4.0),
        )
        element.updateTransform(fromParent = false)

        assertSame(base, element.baseElement)
        assertContentEquals(
            doubleArrayOf(1.0, 9.0, -1.0),
            element.actualCoords.usrCoords,
        )
    }

    @Test
    fun removeTransformDeletesFirstMatchAndClearResetsBase() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, -2.0),
            id = "element",
        )
        val translate = transformation("translate", 1.0, 2.0)
        val scale = transformation("scale", 3.0, 4.0)
        element.addTransform(
            element,
            listOf(translate, scale, translate),
        )

        element.removeTransform(translate)

        assertEquals(listOf(scale, translate), element.transformations)
        assertSame(element, element.baseElement)

        element.clearTransforms()

        assertTrue(element.transformations.isEmpty())
        assertNull(element.baseElement)
        assertNull(element.transformationEvaluationError)
    }

    @Test
    fun singularTransformPreimageReturnsStructuredPositionError() {
        val point = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 3.0),
            ),
        ).value
        val singular = transformation("scale", 0.0, 1.0)
        singular.bindTo(point)
        board.update()

        val failure = assertIs<
            GMResult.Err<
                CoordinateTransformationError.NonInvertibleCompositeMatrix
                >
            >(
            point.setPositionDirectlyResult(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(4.0, 5.0),
            ),
        )

        assertEquals(1, failure.error.transformationCount)
        assertEquals(failure.error, point.transformationEvaluationError)
    }

    @Test
    fun gridAndPointSnappingFollowVisualProperties() {
        val target = point(3.0, 4.0, id = "target")
        val point = point(2.7, 4.2, id = "moving")
        point.setAttribute(
            mapOf(
                "snaptogrid" to true,
                "snapsizex" to 0.5,
                "snapsizey" to 1.0,
            ),
        )

        point.handleSnapToGrid()

        assertContentEquals(
            doubleArrayOf(1.0, 2.5, 4.0),
            point.coords.usrCoords,
        )

        point.setAttribute(
            mapOf(
                "snaptogrid" to false,
                "snaptopoints" to true,
                "attractordistance" to 1.0,
            ),
        )
        point.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.8, 4.1),
        )

        assertContentEquals(
            target.coords.usrCoords,
            point.coords.usrCoords,
        )
    }

    @Test
    fun positionByTransformMeltsConsecutiveTranslations() {
        val point = point(1.0, 2.0, id = "translated")

        assertIs<GMResult.Ok<CoordsElement>>(
            point.setPositionByTransformResult(
                Const.COORDS_BY_USER,
                doubleArrayOf(3.0, -1.0),
            ),
        )
        assertIs<GMResult.Ok<CoordsElement>>(
            point.setPositionByTransformResult(
                Const.COORDS_BY_USER,
                doubleArrayOf(2.0, 4.0),
            ),
        )
        board.update()

        assertEquals(1, point.transformations.size)
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 5.0),
            point.coords.usrCoords,
        )
    }

    @Test
    fun moveToMoveAlongAndVisitCompleteThroughBoardScheduler() {
        val point = point(1.0, 2.0, id = "animated")
        var callbacks = 0

        assertIs<GMResult.Ok<CoordsElement>>(
            point.moveTo(
                where = doubleArrayOf(5.0, 6.0),
                timeMilliseconds = 100.0,
                options = CoordsElementMoveOptions(
                    callback = { callbacks += 1 },
                    effect = "<>",
                ),
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 6.0),
            point.coords.usrCoords,
        )

        assertIs<GMResult.Ok<CoordsElement>>(
            point.moveAlong(
                path = listOf(
                    doubleArrayOf(5.0, 6.0),
                    doubleArrayOf(7.0, 4.0),
                    doubleArrayOf(9.0, 8.0),
                ),
                timeMilliseconds = 100.0,
                options = CoordsElementMoveOptions(
                    callback = { callbacks += 1 },
                    interpolate = false,
                ),
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 9.0, 8.0),
            point.coords.usrCoords,
        )

        assertIs<GMResult.Ok<CoordsElement>>(
            point.visit(
                where = doubleArrayOf(-2.0, 3.0),
                timeMilliseconds = 100.0,
                options = CoordsElementMoveOptions(
                    callback = { callbacks += 1 },
                    repeat = 2,
                ),
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 9.0, 8.0),
            point.coords.usrCoords,
        )
        assertEquals(3, callbacks)
        assertNull(point.movementError)
    }

    @Test
    fun animationValidationAndCallbackFailuresAreStructured() {
        val point = point(1.0, 2.0, id = "animation-errors")

        assertIs<
            GMResult.Err<
                CoordsElementAnimationError.InvalidCoordinateCount
                >
            >(
            point.moveTo(
                where = doubleArrayOf(1.0),
                timeMilliseconds = 100.0,
            ),
        )
        assertIs<
            GMResult.Err<
                CoordsElementAnimationError.InvalidDuration
                >
            >(
            point.moveTo(
                where = doubleArrayOf(3.0, 4.0),
                timeMilliseconds = -1.0,
            ),
        )
        assertIs<
            GMResult.Err<CoordsElementAnimationError.Callback>
            >(
            point.moveTo(
                where = doubleArrayOf(3.0, 4.0),
                timeMilliseconds = 0.0,
                options = CoordsElementMoveOptions(
                    callback = { throw IllegalStateException("failed") },
                ),
            ),
        )
    }

    @Test
    fun parentDataUsesCoordinatesParentsAndGliderHost() {
        val parent = point(0.0, 0.0, id = "parent")
        val point = point(2.0, 3.0, id = "child")

        assertEquals(listOf(1.0, 2.0, 3.0), point.parentData())

        point.addParents(listOf(parent))
        assertEquals(listOf("parent"), point.parentData())

        point.slideObject = parent
        point.slideObjects += parent
        point.type = Const.OBJECT_TYPE_GLIDER
        assertEquals(
            listOf(2.0, 3.0, "parent"),
            point.parentData(),
        )

        point.popSlideObject()
        assertEquals(Const.OBJECT_TYPE_POINT, point.type)
        assertNull(point.slideObject)
    }

    @Test
    fun rendererAdapterOnlyInvokesVisibleRealElementsNeedingUpdate() {
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(2.0, 3.0),
        )
        element.visualPropertiesCalculated["visible"] = true
        var renderCount = 0

        assertSame(
            element,
            element.updateRendererGeneric { renderCount += 1 },
        )
        assertEquals(1, renderCount)
        assertFalse(element.needsUpdate)

        element.updateRendererGeneric { renderCount += 1 }
        assertEquals(1, renderCount)

        element.needsUpdate = true
        element.coords.setCoordinates(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 2.0, 3.0),
        )
        element.visualPropertiesCalculated["visible"] = true
        element.updateRendererGeneric { renderCount += 1 }
        assertEquals(1, renderCount)
        assertEquals(
            false,
            element.visualPropertiesCalculated["visible"],
        )
    }

    private fun point(
        horizontal: Double,
        vertical: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates =
                    doubleArrayOf(horizontal, vertical),
                id = id,
            ),
        ).value

    private fun transformation(
        type: String,
        vararg parameters: Double,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(
            Transformation.create(type, parameters),
        ).value

    private class RecordingCoordsElement(
        board: Board,
        coordinates: DoubleArray,
        private val calls: MutableList<String>,
    ) : CoordsElement(
        board = board,
        coordinates = coordinates,
    ) {
        override fun handleSnapToGrid(): CoordsElement {
            calls += "grid"
            return this
        }

        override fun handleSnapToPoints(): CoordsElement {
            calls += "points"
            return this
        }

        override fun handleAttractors(): CoordsElement {
            calls += "attractors"
            return this
        }

        override fun prepareUpdate(): GeometryElement {
            calls += "prepare"
            return super.prepareUpdate()
        }

        override fun update(fromParent: Boolean): GeometryElement {
            calls += "update:$fromParent"
            return this
        }
    }
}
