package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
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
    fun anchoredUserCoordinatesFollowTheAnchorAndPreserveRelativeDrag() {
        val anchor = point(2.0, 3.0, "anchor")
        val element = point(0.0, 0.0, "anchored")

        assertIs<GMResult.Ok<CoordsElement>>(
            element.addAnchor(
                anchor = anchor,
                coordinates = doubleArrayOf(1.0, -2.0),
                isLabel = false,
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 1.0),
            element.coords.usrCoords,
        )
        assertSame(element, anchor.childElements[element.id])
        assertTrue(element.isConstrained)
        assertTrue(element.isDraggable)

        anchor.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 5.0),
        )
        element.prepareUpdate().update(fromParent = true)
        assertContentEquals(
            doubleArrayOf(1.0, 5.0, 3.0),
            element.coords.usrCoords,
        )

        assertIs<GMResult.Ok<CoordsElement>>(
            element.setPositionDirectlyResult(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(10.0, 8.0),
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 3.0),
            requireNotNull(element.relativeCoords).usrCoords,
        )
        element.prepareUpdate().update(fromParent = false)
        assertContentEquals(
            doubleArrayOf(1.0, 10.0, 8.0),
            element.coords.usrCoords,
        )
    }

    @Test
    fun anchoredLabelCoordinatesUseScreenOffsets() {
        val anchor = point(2.0, 3.0, "labelAnchor")
        val label = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            id = "label",
        )

        assertIs<GMResult.Ok<CoordsElement>>(
            label.addAnchor(
                anchor = anchor,
                coordinates = doubleArrayOf(10.0, -20.0),
                isLabel = true,
            ),
        )

        assertEquals(360.0, label.coords.scrCoords[1])
        assertEquals(60.0, label.coords.scrCoords[2])
        assertEquals(2.2, label.X(), absoluteTolerance = 1.0e-12)
        assertEquals(3.5, label.Y(), absoluteTolerance = 1.0e-12)
        assertEquals(listOf("labelAnchor"), label.parents)
    }

    @Test
    fun addAnchorRejectsCrossBoardAndUnregisteredAnchors() {
        val element = CoordsElement(board = board, id = "anchored")
        val otherBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val foreign = point(
            horizontal = 0.0,
            vertical = 0.0,
            id = "foreign",
            targetBoard = otherBoard,
        )
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            id = "unregistered",
        )

        assertEquals(
            CoordsElementAnchorError.BoardMismatch,
            assertIs<GMResult.Err<CoordsElementAnchorError>>(
                element.addAnchor(
                    anchor = foreign,
                    coordinates = doubleArrayOf(0.0, 0.0),
                    isLabel = false,
                ),
            ).error,
        )
        assertEquals(
            CoordsElementAnchorError.AnchorNotRegistered("unregistered"),
            assertIs<GMResult.Err<CoordsElementAnchorError>>(
                element.addAnchor(
                    anchor = unregistered,
                    coordinates = doubleArrayOf(0.0, 0.0),
                    isLabel = false,
                ),
            ).error,
        )
    }

    @Test
    fun singleConstraintFunctionMayReturnACoordsObject() {
        val source = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, -3.0),
            board = board,
        )
        val element = CoordsElement(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            coordinateFunctions = listOf(
                CoordsObjectFunction(source),
            ),
        )

        element.updateConstraint()
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -3.0),
            element.coords.usrCoords,
        )

        source.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 4.0, 5.0),
        )
        element.updateConstraint()
        assertContentEquals(
            doubleArrayOf(0.0, 4.0, 5.0),
            element.coords.usrCoords,
        )
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
    fun movementFramesAndCompletionTickMatchOfficialFixture() {
        var scheduledInterval = Double.NaN
        var advance: (() -> Boolean)? = null
        val animationBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            animationDelay = 25.0,
            animationScheduler = BoardAnimationScheduler { interval, step ->
                scheduledInterval = interval
                advance = step
            },
        )
        val moving = point(
            horizontal = 0.0,
            vertical = 0.0,
            id = "moving",
            targetBoard = animationBoard,
        )
        var callbackCount = 0

        assertIs<GMResult.Ok<CoordsElement>>(
            moving.moveTo(
                where = doubleArrayOf(4.0, 8.0),
                timeMilliseconds = 100.0,
                options = CoordsElementMoveOptions(
                    effect = "<>",
                    callback = { callbackCount += 1 },
                ),
            ),
        )

        assertEquals(25.0, scheduledInterval)
        val moveToFrames = runAnimationFrames(
            element = moving,
            advance = advance ?: error("moveTo was not scheduled"),
        )
        assertCoordinateFrames(
            expected = listOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    1.0,
                    0.585786437626905,
                    1.17157287525381,
                ),
                doubleArrayOf(1.0, 2.0, 4.0),
                doubleArrayOf(
                    1.0,
                    3.414213562373095,
                    6.82842712474619,
                ),
                doubleArrayOf(1.0, 4.0, 8.0),
                doubleArrayOf(1.0, 4.0, 8.0),
            ),
            actual = moveToFrames,
        )
        assertEquals(1, callbackCount)

        advance = null
        val along = point(
            horizontal = 0.0,
            vertical = 0.0,
            id = "along",
            targetBoard = animationBoard,
        )
        assertIs<GMResult.Ok<CoordsElement>>(
            along.moveAlong(
                path = listOf(
                    doubleArrayOf(0.0, 0.0),
                    doubleArrayOf(2.0, 4.0),
                    doubleArrayOf(4.0, 0.0),
                ),
                timeMilliseconds = 90.0,
                options = CoordsElementMoveOptions(interpolate = false),
            ),
        )
        val alongFrames = runAnimationFrames(
            element = along,
            advance = advance ?: error("moveAlong was not scheduled"),
        )
        assertCoordinateFrames(
            expected = listOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(
                    1.0,
                    1.1111111111111112,
                    2.2222222222222223,
                ),
                doubleArrayOf(
                    1.0,
                    2.2222222222222223,
                    3.5555555555555554,
                ),
                doubleArrayOf(
                    1.0,
                    3.333333333333333,
                    1.333333333333334,
                ),
                doubleArrayOf(1.0, 4.0, 0.0),
                doubleArrayOf(1.0, 4.0, 0.0),
            ),
            actual = alongFrames,
        )

        advance = null
        val visiting = point(
            horizontal = 0.0,
            vertical = 0.0,
            id = "visiting",
            targetBoard = animationBoard,
        )
        assertIs<GMResult.Ok<CoordsElement>>(
            visiting.visit(
                where = doubleArrayOf(3.0, 6.0),
                timeMilliseconds = 75.0,
                options = CoordsElementMoveOptions(effect = "=="),
            ),
        )
        assertCoordinateFrames(
            expected = listOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(1.0, 2.0, 4.0),
                doubleArrayOf(1.0, 2.0, 4.0),
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(1.0, 0.0, 0.0),
            ),
            actual = runAnimationFrames(
                element = visiting,
                advance = advance ?: error("visit was not scheduled"),
            ),
        )

        var immediateCallbackCount = 0
        assertIs<GMResult.Ok<CoordsElement>>(
            visiting.moveTo(
                where = doubleArrayOf(3.0, 4.0),
                timeMilliseconds = 0.0,
                options = CoordsElementMoveOptions(
                    callback = { immediateCallbackCount += 1 },
                ),
            ),
        )
        assertEquals(0, immediateCallbackCount)
    }

    @Test
    fun attractorBindingDetachingAndParentUpdatesMatchOfficialFixture() {
        val attractorBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val first = point(-2.0, 0.0, "first", attractorBoard)
        val second = point(2.0, 0.0, "second", attractorBoard)
        val line = assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = attractorBoard,
                point1 = first,
                point2 = second,
                id = "line",
            ),
        ).value
        val candidate = point(6.0, 5.0, "candidate", attractorBoard)
        candidate.setAttribute(
            mapOf(
                "attractors" to listOf(line),
                "attractordistance" to 0.75,
                "snatchdistance" to 1.5,
                "attractorunit" to "user",
            ),
        )

        candidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.5, 0.4),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 0.5, 0.0),
            candidate.coords.usrCoords,
        )
        assertEquals(Const.OBJECT_TYPE_GLIDER, candidate.type)
        assertEquals("glider", candidate.elType)
        assertSame(line, candidate.slideObject)
        assertEquals(1, candidate.slideObjects.size)
        assertSame(line, candidate.slideObjects.single())
        assertEquals(listOf(line.id), candidate.parents)
        assertSame(candidate, line.childElements[candidate.id])

        first.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-4.0, 0.0),
        )
        attractorBoard.update()
        assertEquals(-0.25, candidate.X(), 1.0e-12)
        assertEquals(0.0, candidate.Y(), 1.0e-12)

        candidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(6.0, 5.0),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 5.0),
            candidate.coords.usrCoords,
        )
        assertEquals(Const.OBJECT_TYPE_POINT, candidate.type)
        assertEquals("point", candidate.elType)
        assertNull(candidate.slideObject)
        assertTrue(candidate.slideObjects.isEmpty())
        assertEquals(listOf(line.id), candidate.parents)
        assertFalse(candidate.id in line.childElements)

        val vertices = listOf(
            point(-2.0, -2.0, "a", attractorBoard),
            point(2.0, -2.0, "b", attractorBoard),
            point(2.0, 2.0, "c", attractorBoard),
            point(-2.0, 2.0, "d", attractorBoard),
        )
        val polygon = assertIs<GMResult.Ok<Polygon>>(
            Polygon.create(
                board = attractorBoard,
                vertices = vertices,
                id = "polygon",
            ),
        ).value
        val polygonCandidate =
            point(6.0, 5.0, "polygonCandidate", attractorBoard)
        polygonCandidate.setAttribute(
            mapOf(
                "attractors" to listOf(polygon),
                "attractordistance" to 0.75,
                "snatchdistance" to 1.5,
                "attractorunit" to "user",
            ),
        )

        polygonCandidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.2, 0.0),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 0.0),
            polygonCandidate.coords.usrCoords,
        )
        assertTrue(polygonCandidate.onPolygon)
        assertTrue(polygonCandidate.slideObject in polygon.borders)
        assertEquals(1, polygonCandidate.slideObjects.size)
        assertSame(
            polygonCandidate.slideObject,
            polygonCandidate.slideObjects.single(),
        )

        polygonCandidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(6.0, 5.0),
        )
        assertEquals(Const.OBJECT_TYPE_POINT, polygonCandidate.type)
        assertNull(polygonCandidate.slideObject)
        assertTrue(polygonCandidate.onPolygon)
    }

    @Test
    fun pointCircleAndArcAttractorsMatchOfficialProjectionFamilies() {
        val attractorBoard = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
        )
        val target = point(1.0, 1.0, "target", attractorBoard)
        val pointCandidate =
            point(6.0, 5.0, "pointCandidate", attractorBoard)
        configureAttractor(pointCandidate, target)
        pointCandidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.2, 1.1),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 1.0),
            pointCandidate.coords.usrCoords,
        )
        assertSame(target, pointCandidate.slideObject)

        val center = point(0.0, 0.0, "center", attractorBoard)
        val radius = point(2.0, 0.0, "radius", attractorBoard)
        val circle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = attractorBoard,
                center = center,
                point2 = radius,
                id = "circle",
            ),
        ).value
        val circleCandidate =
            point(6.0, 5.0, "circleCandidate", attractorBoard)
        configureAttractor(circleCandidate, circle)
        circleCandidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.3, 0.0),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 0.0),
            circleCandidate.coords.usrCoords,
        )
        assertSame(circle, circleCandidate.slideObject)

        val arcEnd = point(0.0, 2.0, "arcEnd", attractorBoard)
        val arc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = attractorBoard,
                center = center,
                radiuspoint = radius,
                anglepoint = arcEnd,
                id = "arc",
            ),
        ).value
        attractorBoard.update()
        val arcCandidate =
            point(6.0, 5.0, "arcCandidate", attractorBoard)
        configureAttractor(arcCandidate, arc)
        arcCandidate.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.5, 1.5),
        )
        assertEquals(
            1.4142135623730951,
            arcCandidate.X(),
            absoluteTolerance = 1.0e-6,
        )
        assertEquals(
            1.414213562373095,
            arcCandidate.Y(),
            absoluteTolerance = 1.0e-6,
        )
        assertSame(arc, arcCandidate.slideObject)
    }

    @Test
    fun lineGliderAnimationUsesOfficialPixelRoundingAndRoundLimit() {
        var scheduledInterval = Double.NaN
        var advance: (() -> Boolean)? = null
        val animationBoard = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            animationScheduler = BoardAnimationScheduler { interval, step ->
                scheduledInterval = interval
                advance = step
            },
        )
        val first = point(-2.13, 0.17, "first", animationBoard)
        val second = point(2.28, 1.41, "second", animationBoard)
        val host = assertIs<GMResult.Ok<Line>>(
            Line.createSegment(
                board = animationBoard,
                point1 = first,
                point2 = second,
                id = "host",
            ),
        ).value
        val glider = assertIs<GMResult.Ok<Glider>>(
            Glider.create(
                board = animationBoard,
                coordinates = doubleArrayOf(-2.13, 0.17),
                slideObject = host,
                id = "glider",
            ),
        ).value

        assertIs<GMResult.Ok<CoordsElement>>(
            glider.startAnimation(
                direction = 1.0,
                stepCount = 4,
                delayMilliseconds = 0.0,
                maxRounds = 1,
            ),
        )

        assertEquals(250.0, scheduledInterval)
        assertCoordinateFrames(
            expected = listOf(
                doubleArrayOf(
                    1.0,
                    -1.0324226020575913,
                    0.47861586699514436,
                ),
                doubleArrayOf(
                    1.0,
                    0.07166923190553601,
                    0.7890634574972482,
                ),
                doubleArrayOf(
                    1.0,
                    1.1692466298479445,
                    1.0976793244923926,
                ),
                doubleArrayOf(
                    1.0,
                    2.2733384638110716,
                    1.4081269149944962,
                ),
                doubleArrayOf(
                    1.0,
                    2.2733384638110716,
                    1.4081269149944962,
                ),
            ),
            actual = runAnimationFrames(
                element = glider,
                advance = advance ?: error("glider animation was not scheduled"),
            ),
        )
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
                timeMilliseconds = 100.0,
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
        targetBoard: Board = board,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = targetBoard,
                coordinates =
                    doubleArrayOf(horizontal, vertical),
                id = id,
            ),
        ).value

    private fun runAnimationFrames(
        element: CoordsElement,
        advance: () -> Boolean,
    ): List<DoubleArray> = buildList {
        do {
            val keepRunning = advance()
            add(element.coords.usrCoords.copyOf())
        } while (keepRunning)
    }

    private fun assertCoordinateFrames(
        expected: List<DoubleArray>,
        actual: List<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (frameIndex in expected.indices) {
            assertEquals(expected[frameIndex].size, actual[frameIndex].size)
            for (coordinateIndex in expected[frameIndex].indices) {
                assertEquals(
                    expected = expected[frameIndex][coordinateIndex],
                    actual = actual[frameIndex][coordinateIndex],
                    absoluteTolerance = 1.0e-12,
                    message =
                        "frame=$frameIndex coordinate=$coordinateIndex",
                )
            }
        }
    }

    private fun configureAttractor(
        point: Point,
        attractor: GeometryElement,
    ) {
        point.setAttribute(
            mapOf(
                "attractors" to listOf(attractor),
                "attractordistance" to 0.75,
                "snatchdistance" to 1.5,
                "attractorunit" to "user",
            ),
        )
    }

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

    private class CoordsObjectFunction(
        private val value: Coords,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluateCoordsObject():
            GMResult<Coords?, JessieCodeRuntimeError> =
            GMResult.Ok(value)

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
    }
}
