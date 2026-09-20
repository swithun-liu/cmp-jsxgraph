package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.RandomSource
import com.swithun.jsxgraph.core.parser.JessieCodeAstLocation
import com.swithun.jsxgraph.core.parser.JessieCodeCallable
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LineTest {
    @Test
    fun twoPointFactoryRegistersIdentityDependenciesAndOrdinaryMetrics() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))

        val line = line(Line.create(board, point1, point2))

        assertEquals("boardL2", line.id)
        assertEquals("a", line.name)
        assertEquals("line", line.elType)
        assertEquals(Const.OBJECT_TYPE_LINE, line.type)
        assertEquals(Const.OBJECT_TYPE_LINE, line.originalType)
        assertEquals(Const.OBJECT_CLASS_LINE, line.elementClass)
        assertNull(line.baseElement)
        assertTrue(line.isDraggable)
        assertSame(line, board.select(line.id))
        assertSame(line, board.select(line.name))
        assertEquals(listOf(point1.id, point2.id), line.parents)
        assertSame(line, point1.childElements[line.id])
        assertSame(line, point2.childElements[line.id])
        assertSame(point1, line.ancestors[point1.id])
        assertSame(point2, line.ancestors[point2.id])

        assertArrayMatches(
            expected = doubleArrayOf(
                0.0,
                -0.4472135954999579,
                0.8944271909999159,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            actual = line.stdform,
        )
        assertArrayMatches(doubleArrayOf(4.0, 2.0), line.Direction())
        assertEquals(0.5, line.Slope(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, line.getRise(), absoluteTolerance = TOLERANCE)
        assertEquals(
            4.47213595499958,
            line.L(),
            absoluteTolerance = TOLERANCE,
        )
        assertArrayMatches(doubleArrayOf(0.0, 2.0, 4.0, 0.0), line.bounds())
        assertEquals(false, line.isVertical())
    }

    @Test
    fun verticalAndCoincidentLinesMatchOfficialNonFiniteResults() {
        val board = board()
        val vertical = line(
            Line.create(
                board,
                point(board, doubleArrayOf(2.0, -3.0)),
                point(board, doubleArrayOf(2.0, 5.0)),
            ),
        )

        assertArrayMatches(
            doubleArrayOf(
                2.0,
                -1.0,
                0.0,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NaN,
            ),
            vertical.stdform,
        )
        assertArrayMatches(doubleArrayOf(0.0, 8.0), vertical.Direction())
        assertEquals(Double.POSITIVE_INFINITY, vertical.Slope())
        assertEquals(Double.POSITIVE_INFINITY, vertical.getRise())
        assertEquals(8.0, vertical.L())
        assertArrayMatches(doubleArrayOf(2.0, 5.0, 2.0, -3.0), vertical.bounds())
        assertTrue(vertical.isVertical())

        val coincidentPoint = point(board, doubleArrayOf(2.0, 3.0))
        val coincident = line(Line.create(board, coincidentPoint, coincidentPoint))

        assertTrue(coincident.stdform[0].isNaN())
        assertTrue(coincident.stdform[1].isNaN())
        assertTrue(coincident.stdform[2].isNaN())
        assertArrayMatches(doubleArrayOf(0.0, 0.0), coincident.Direction())
        assertEquals(Double.POSITIVE_INFINITY, coincident.Slope())
        assertEquals(Double.POSITIVE_INFINITY, coincident.getRise())
        assertEquals(0.0, coincident.L())
        assertEquals(false, coincident.isVertical())
    }

    @Test
    fun idealPointDirectionBranchesMatchOfficialLineBehavior() {
        val board = board()
        val finite = point(board, doubleArrayOf(2.0, 3.0))
        val ideal = point(board, doubleArrayOf(0.0, 4.0, -5.0))
        val secondIdeal = point(board, doubleArrayOf(0.0, -2.0, 7.0))

        val finiteToIdeal = line(Line.create(board, finite, ideal))
        val idealToFinite = line(Line.create(board, ideal, finite))
        val idealToIdeal = line(Line.create(board, ideal, secondIdeal))

        assertArrayMatches(doubleArrayOf(4.0, -5.0), finiteToIdeal.Direction())
        assertArrayMatches(doubleArrayOf(-4.0, 5.0), idealToFinite.Direction())
        assertArrayMatches(doubleArrayOf(-6.0, 12.0), idealToIdeal.Direction())
        assertEquals(-1.25, finiteToIdeal.Slope(), absoluteTolerance = TOLERANCE)
        assertEquals(5.5, finiteToIdeal.getRise(), absoluteTolerance = TOLERANCE)
        assertTrue(finiteToIdeal.L().isNaN())
        assertArrayMatches(
            doubleArrayOf(2.0, 3.0, 4.0, -5.0),
            finiteToIdeal.bounds(),
        )
    }

    @Test
    fun angleUnitsAndHorizontalCheckMatchOfficialLineBehavior() {
        val board = board()
        val ordinary = line(
            Line.create(
                board,
                point(board, doubleArrayOf(0.0, 0.0)),
                point(board, doubleArrayOf(4.0, 2.0)),
            ),
        )

        assertEquals(
            0.4636476090008061,
            ordinary.getAngle(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.4636476090008061,
            angle(ordinary.getAngle("")),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.4636476090008061,
            angle(ordinary.getAngle("Radians")),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            26.56505117707799,
            angle(ordinary.getAngle("DEGREES")),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.14758361765043326,
            angle(ordinary.getAngle("semicircle")),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            0.07379180882521663,
            angle(ordinary.getAngle("circle")),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            LineError.UnsupportedAngleUnit("turns"),
            assertIs<GMResult.Err<LineError.UnsupportedAngleUnit>>(
                ordinary.getAngle("turns"),
            ).error,
        )
        assertEquals(false, ordinary.isHorizontal())

        val horizontal = line(
            Line.create(
                board,
                point(board, doubleArrayOf(-2.0, 3.0)),
                point(board, doubleArrayOf(5.0, 3.0)),
            ),
        )
        assertTrue(horizontal.isHorizontal())
        assertEquals(false, horizontal.isVertical())
        assertEquals((-0.0).toBits(), horizontal.getAngle().toBits())

        val coincidentPoint = point(board, doubleArrayOf(2.0, 3.0))
        val coincident = line(Line.create(board, coincidentPoint, coincidentPoint))
        assertEquals(false, coincident.isHorizontal())
        assertTrue(coincident.getAngle().isNaN())
    }

    @Test
    fun finiteParametricCoordinatesMatchOfficialLineBehavior() {
        val board = board()
        val line = line(
            Line.create(
                board,
                point(board, doubleArrayOf(0.0, 0.0)),
                point(board, doubleArrayOf(4.0, 2.0)),
            ),
        )

        assertEquals(1.0, line.X(0.25), absoluteTolerance = TOLERANCE)
        assertEquals(0.5, line.Y(0.25), absoluteTolerance = TOLERANCE)
        assertEquals(1.0, line.Z(0.25), absoluteTolerance = TOLERANCE)
        assertArrayMatches(
            doubleArrayOf(1.0, 1.0, 0.5),
            line.Ft(0.25),
        )
        assertArrayMatches(
            doubleArrayOf(1.0, 4.0, 2.0),
            line.Ft(1.0),
        )
        assertEquals(0.0, line.minX())
        assertEquals(1.0, line.maxX())
    }

    @Test
    fun idealPointParametricBranchesMatchOfficialLineBehavior() {
        val board = board()
        val finite = point(board, doubleArrayOf(2.0, 3.0))
        val ideal = point(board, doubleArrayOf(0.0, 4.0, -5.0))
        val secondIdeal = point(board, doubleArrayOf(0.0, -2.0, 7.0))
        val finiteToIdeal = line(Line.create(board, finite, ideal))
        val idealToFinite = line(Line.create(board, ideal, finite))
        val idealToIdeal = line(Line.create(board, ideal, secondIdeal))

        assertArrayMatches(
            doubleArrayOf(
                1.0,
                15619.376188860608,
                -19518.72023607576,
            ),
            finiteToIdeal.Ft(0.25),
            absoluteTolerance = PARAMETRIC_TOLERANCE,
        )
        assertEquals(0.0, finiteToIdeal.Z(1.0))
        assertArrayMatches(
            doubleArrayOf(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            finiteToIdeal.Ft(1.0),
        )

        assertArrayMatches(
            doubleArrayOf(
                1.0,
                -15615.376188860608,
                19524.72023607576,
            ),
            idealToFinite.Ft(0.25),
            absoluteTolerance = PARAMETRIC_TOLERANCE,
        )
        assertArrayMatches(
            doubleArrayOf(
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
            ),
            idealToFinite.Ft(1.0),
        )

        assertArrayMatches(
            doubleArrayOf(1.0, Double.NaN, Double.NaN),
            idealToIdeal.Ft(0.25),
        )
        assertArrayMatches(
            doubleArrayOf(Double.NaN, Double.NaN, Double.NaN),
            idealToIdeal.Ft(1.0),
        )
    }

    @Test
    fun updateRecomputesStandardFormOnlyWhenRequested() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))
        val line = line(Line.create(board, point1, point2))
        val initialStandardForm = line.stdform.copyOf()

        point2.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 4.0),
        )
        line.needsUpdate = false
        assertSame(line, line.update(fromParent = true))
        assertArrayMatches(initialStandardForm, line.stdform)

        line.needsUpdate = true
        assertSame(line, line.update(fromParent = true))
        assertArrayMatches(
            doubleArrayOf(
                0.0,
                -1.0,
                0.0,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NaN,
            ),
            line.stdform,
        )
    }

    @Test
    fun fixedLengthSegmentNormalizesAndFollowsEitherDraggedEndpoint() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(2.0, 0.0))
        val segment = line(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLength = 3.0,
            ),
        )

        assertEquals("segment", segment.elType)
        assertTrue(segment.hasFixedLength)
        assertFalse(segment.nonnegativeOnly)
        assertPoint(point1, -1.0, 0.0)
        assertPoint(point2, 2.0, 0.0)
        assertEquals(3.0, segment.L(), absoluteTolerance = TOLERANCE)

        point1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 1.0),
        )
        board.update(draggedElement = point1)
        val firstScale = 3.0 / sqrt(2.0)
        assertPoint(point1, 1.0, 1.0)
        assertPoint(point2, 1.0 + firstScale, 1.0 - firstScale)
        assertEquals(3.0, segment.L(), absoluteTolerance = TOLERANCE)

        point2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 4.0),
        )
        board.update(draggedElement = point2)
        val secondScale = 3.0 / sqrt(18.0)
        assertPoint(point2, -2.0, 4.0)
        assertPoint(
            point1,
            -2.0 + 3.0 * secondScale,
            4.0 - 3.0 * secondScale,
        )
        assertEquals(3.0, segment.L(), absoluteTolerance = TOLERANCE)
    }

    @Test
    fun fixedLengthSegmentRespectsFixedEndpointFallbacks() {
        val board = board()
        val anchor = point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            fixed = true,
        )
        val movable = point(board, doubleArrayOf(2.0, 0.0))
        val segment = line(
            Line.createSegment(
                board = board,
                point1 = anchor,
                point2 = movable,
                fixedLength = 3.0,
            ),
        )

        assertTrue(anchor.isFixed)
        assertPoint(anchor, 0.0, 0.0)
        assertPoint(movable, 3.0, 0.0)

        movable.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 3.0),
        )
        board.update(draggedElement = movable)
        assertPoint(anchor, 0.0, 0.0)
        assertPoint(movable, 2.4, 1.8)
        assertEquals(3.0, segment.L(), absoluteTolerance = TOLERANCE)

        val fixedBoard = board()
        val fixed1 = point(
            fixedBoard,
            doubleArrayOf(0.0, 0.0),
            fixed = true,
        )
        val fixed2 = point(
            fixedBoard,
            doubleArrayOf(2.0, 0.0),
            fixed = true,
        )
        val fixedSegment = line(
            Line.createSegment(
                board = fixedBoard,
                point1 = fixed1,
                point2 = fixed2,
                fixedLength = 5.0,
            ),
        )
        assertPoint(fixed1, 0.0, 0.0)
        assertPoint(fixed2, 2.0, 0.0)
        assertEquals(2.0, fixedSegment.L(), absoluteTolerance = TOLERANCE)
    }

    @Test
    fun fixedLengthSignPolicyAndMutationMatchUpstream() {
        val absoluteBoard = board()
        val absolute1 = point(absoluteBoard, doubleArrayOf(0.0, 0.0))
        val absolute2 = point(absoluteBoard, doubleArrayOf(2.0, 0.0))
        val absoluteSegment = line(
            Line.createSegment(
                board = absoluteBoard,
                point1 = absolute1,
                point2 = absolute2,
                fixedLength = -3.0,
            ),
        )
        assertPoint(absolute1, -1.0, 0.0)
        assertEquals(
            3.0,
            absoluteSegment.L(),
            absoluteTolerance = TOLERANCE,
        )

        absoluteSegment.setFixedLength(-4.0)
        assertPoint(absolute1, -2.0, 0.0)
        assertEquals(
            4.0,
            absoluteSegment.L(),
            absoluteTolerance = TOLERANCE,
        )

        val nonnegativeBoard = board()
        val nonnegative1 =
            point(nonnegativeBoard, doubleArrayOf(0.0, 0.0))
        val nonnegative2 =
            point(nonnegativeBoard, doubleArrayOf(2.0, 0.0))
        val nonnegativeSegment = line(
            Line.createSegment(
                board = nonnegativeBoard,
                point1 = nonnegative1,
                point2 = nonnegative2,
                fixedLength = -3.0,
                nonnegativeOnly = true,
            ),
        )
        assertTrue(nonnegativeSegment.nonnegativeOnly)
        assertPoint(nonnegative1, 2.0, 0.0)
        assertPoint(nonnegative2, 2.0, 0.0)
        assertEquals(
            0.0,
            nonnegativeSegment.L(),
            absoluteTolerance = TOLERANCE,
        )

        val ordinaryBoard = board()
        val ordinary1 = point(ordinaryBoard, doubleArrayOf(0.0, 0.0))
        val ordinary2 = point(ordinaryBoard, doubleArrayOf(2.0, 0.0))
        val ordinary = line(
            Line.create(ordinaryBoard, ordinary1, ordinary2),
        )
        ordinary.setFixedLength(4.0)
        assertFalse(ordinary.hasFixedLength)
        assertPoint(ordinary1, 0.0, 0.0)
        assertPoint(ordinary2, 2.0, 0.0)
    }

    @Test
    fun coincidentFixedLengthSegmentUsesUpstreamRandomDirection() {
        val board = board()
        val point1 = point(board, doubleArrayOf(1.0, 1.0))
        val point2 = point(board, doubleArrayOf(1.0, 1.0))
        val values = listOf(0.75, 0.5).iterator()
        val segment = line(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLength = 2.0,
                randomSource = RandomSource { values.next() },
            ),
        )

        assertPoint(point1, 1.0, 1.0)
        assertPoint(point2, 3.0, 1.0)
        assertEquals(2.0, segment.L(), absoluteTolerance = TOLERANCE)
    }

    @Test
    fun stringFixedLengthTracksStableDependencyAndNonnegativeOnly() {
        val board = board()
        val point1 = point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            fixed = true,
        )
        val point2 = point(board, doubleArrayOf(2.0, 0.0))
        val driver = point(
            board = board,
            coordinates = doubleArrayOf(3.0, 0.0),
            name = "A",
        )
        val segment = line(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLengthExpression = "A.X() - 5",
                nonnegativeOnly = true,
            ),
        )

        assertEquals(0.0, segment.L(), absoluteTolerance = TOLERANCE)
        assertSame(segment, driver.childElements[segment.id])
        assertSame(driver, segment.ancestors[driver.id])
        assertNull(segment.fixedLengthEvaluationError)

        driver.setName("Renamed")
        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(7.0, 0.0),
        )
        board.update()

        assertEquals(2.0, segment.L(), absoluteTolerance = TOLERANCE)
        assertNull(segment.fixedLengthEvaluationError)
    }

    @Test
    fun functionFixedLengthUsesFreshCallableAndKeepsGeometryOnFailure() {
        val board = board()
        val point1 = point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            fixed = true,
        )
        val point2 = point(board, doubleArrayOf(2.0, 0.0))
        val driver = point(board, doubleArrayOf(4.0, 0.0))
        val location = JessieCodeAstLocation(1, 0, 1, 8)
        var fail = false
        var initialCalls = 0
        var externalCalls = 0
        val function = JessieCodeRuntimeValue.FunctionValue(
            name = "function",
            callable = JessieCodeCallable { _, _ ->
                initialCalls += 1
                GMResult.Ok(JessieCodeRuntimeValue.NumberValue(3.0))
            },
            externalCallable = JessieCodeCallable { _, callLocation ->
                externalCalls += 1
                if (fail) {
                    GMResult.Err(
                        JessieCodeRuntimeError.BuiltInInvocationFailure(
                            functionName = "length",
                            reason = "failed",
                            location = callLocation,
                        ),
                    )
                } else {
                    GMResult.Ok(
                        JessieCodeRuntimeValue.NumberValue(driver.X() + 1.0),
                    )
                }
            },
            dependencies = mapOf(driver.id to driver),
        )
        val segment = line(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLengthFunction = function,
                fixedLengthFunctionLocation = location,
            ),
        )

        assertEquals(1, initialCalls)
        assertEquals(0, externalCalls)
        assertEquals(3.0, segment.L(), absoluteTolerance = TOLERANCE)
        assertSame(segment, driver.childElements[segment.id])

        board.update()
        assertEquals(1, externalCalls)
        assertEquals(5.0, segment.L(), absoluteTolerance = TOLERANCE)

        val beforePoint1 = point1.Coords()
        val beforePoint2 = point2.Coords()
        fail = true
        board.update()

        assertArrayMatches(beforePoint1, point1.Coords())
        assertArrayMatches(beforePoint2, point2.Coords())
        val error = assertIs<LineError.FixedLengthFunctionEvaluation>(
            segment.fixedLengthEvaluationError,
        )
        assertIs<JessieCodeRuntimeError.BuiltInInvocationFailure>(error.error)
    }

    @Test
    fun dynamicFixedLengthCreationFailuresAreStructuredAndAtomic() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(2.0, 0.0))
        val driver = point(
            board = board,
            coordinates = doubleArrayOf(3.0, 0.0),
            name = "A",
        )
        val initialObjectCount = board.numObjects

        val compileError = assertIs<
            GMResult.Err<LineError.FixedLengthExpressionCompile>
            >(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLengthExpression = "1 +",
            ),
        ).error.error
        assertIs<JessieCodeExpressionCompileError.Parser>(compileError)

        val evaluationError = assertIs<
            GMResult.Err<LineError.FixedLengthExpressionEvaluation>
            >(
            Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLengthExpression = "A.Unknown()",
            ),
        ).error.error
        assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
            evaluationError,
        )

        assertEquals(
            LineError.NonNumericFixedLengthExpression("string"),
            assertIs<
                GMResult.Err<LineError.NonNumericFixedLengthExpression>
                >(
                Line.createSegment(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    fixedLengthExpression = "\"length\"",
                ),
            ).error,
        )

        val location = JessieCodeAstLocation(1, 0, 1, 8)
        assertEquals(
            LineError.NonNumericFixedLengthFunction("string"),
            assertIs<
                GMResult.Err<LineError.NonNumericFixedLengthFunction>
                >(
                Line.createSegment(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    fixedLengthFunction =
                        JessieCodeRuntimeValue.FunctionValue(
                            name = "function",
                            callable = JessieCodeCallable { _, _ ->
                                GMResult.Ok(
                                    JessieCodeRuntimeValue.StringValue(
                                        "length",
                                    ),
                                )
                            },
                        ),
                    fixedLengthFunctionLocation = location,
                ),
            ).error,
        )

        assertEquals(initialObjectCount, board.numObjects)
        assertTrue(point1.childElements.isEmpty())
        assertTrue(point2.childElements.isEmpty())
        assertTrue(driver.childElements.isEmpty())
    }

    @Test
    fun factoryRejectsForeignAndUnregisteredParents() {
        val board = board()
        val registered = point(board, doubleArrayOf(0.0, 0.0))
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
            id = "unregistered",
        )
        val foreignBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "foreign",
        )
        val foreign = point(foreignBoard, doubleArrayOf(2.0, 2.0))

        assertEquals(
            LineError.ParentNotRegistered(
                parentIndex = 1,
                id = "unregistered",
            ),
            assertIs<GMResult.Err<LineError.ParentNotRegistered>>(
                Line.create(board, registered, unregistered),
            ).error,
        )
        assertEquals(
            LineError.ParentBoardMismatch(parentIndex = 0),
            assertIs<GMResult.Err<LineError.ParentBoardMismatch>>(
                Line.create(board, foreign, registered),
            ).error,
        )
        assertEquals(1, board.numObjects)
        assertTrue(registered.childElements.isEmpty())
    }

    @Test
    fun duplicateLineIdDoesNotLeakNameOrDependencyEntries() {
        val board = board()
        val point1 = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))
        val first = line(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "fixed",
                name = "Defined",
            ),
        )

        val duplicate = assertIs<GMResult.Err<LineError.Registration>>(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = "fixed",
                name = "Orphan",
            ),
        )

        assertEquals(
            BoardError.DuplicateElementId("fixed"),
            duplicate.error.error,
        )
        assertSame(first, board.elementById("fixed"))
        assertSame(first, board.elementByName("Defined"))
        assertNull(board.elementByName("Orphan"))
        assertEquals(setOf(first.id), point1.childElements.keys)
        assertEquals(setOf(first.id), point2.childElements.keys)
        assertEquals(3, board.numObjects)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = "board",
    )

    private fun point(
        board: Board,
        coordinates: DoubleArray,
        fixed: Boolean = false,
        name: String? = null,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = coordinates,
            fixed = fixed,
            name = name,
        ),
    ).value

    private fun assertPoint(
        point: Point,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = TOLERANCE)
        assertEquals(y, point.Y(), absoluteTolerance = TOLERANCE)
    }

    private fun line(result: GMResult<Line, LineError>): Line =
        assertIs<GMResult.Ok<Line>>(result).value

    private fun angle(result: GMResult<Double, LineError>): Double =
        assertIs<GMResult.Ok<Double>>(result).value

    private fun assertArrayMatches(
        expected: DoubleArray,
        actual: DoubleArray,
        absoluteTolerance: Double = TOLERANCE,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            when {
                expected[index].isNaN() ->
                    assertTrue(actual[index].isNaN(), "Expected NaN at index $index")

                expected[index].isInfinite() ->
                    assertEquals(expected[index], actual[index], "Mismatch at index $index")

                else -> assertEquals(
                    expected = expected[index],
                    actual = actual[index],
                    absoluteTolerance = absoluteTolerance,
                    message = "Mismatch at index $index",
                )
            }
        }
    }

    private companion object {
        const val PARAMETRIC_TOLERANCE = 1.0e-9
        const val TOLERANCE = 1.0e-14
    }
}
