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

class View3DPoint3DTest {
    @Test
    fun parallelProjectionMatchesOfficialFixture() {
        val view = createView(projection = "parallel")

        assertEquals(Const.OBJECT_TYPE_VIEW3D, view.type)
        assertEquals(Const.OBJECT_CLASS_3D, view.elementClass)
        assertEquals("view3d", view.elType)
        assertContentEquals(doubleArrayOf(-5.0, -4.0), view.llftCorner)
        assertContentEquals(doubleArrayOf(8.0, 7.0), view.size)
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(
                    0.0,
                    -0.5403023058681398,
                    0.8414709848078965,
                    0.0,
                ),
                doubleArrayOf(
                    0.0,
                    -0.2486716793299505,
                    -0.15967024908975094,
                    0.955336489125606,
                ),
                doubleArrayOf(
                    0.0,
                    0.8038879363274419,
                    0.5161705079545379,
                    0.29552020666133955,
                ),
            ),
            actual = view.matrix3DRot,
        )
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(
                    -1.6799085557247806,
                    -0.436564263141457,
                    0.6799085557247805,
                    0.0,
                ),
                doubleArrayOf(
                    -1.737958929517153,
                    -0.17581087728627498,
                    -0.11288686610645392,
                    0.6754228978118033,
                ),
            ),
            actual = view.matrix3D,
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                -0.7566557074166769,
                -0.7886977433927291,
            ),
            actual = view.project3DTo2D(
                doubleArrayOf(1.0, 1.0, 2.0, 2.0),
            ),
        )
        assertNull(view.viewPortTransform)
        assertTrue(view.boxToCam.isEmpty())
        assertEquals(-1.0, view.focalDist)
    }

    @Test
    fun centralProjectionAndFocalCoordinatesMatchOfficialFixture() {
        val view = createView(projection = "central")

        assertEquals("central", view.projectionType)
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                -0.7437047271739274,
                -0.8040624710019673,
            ),
            actual = view.project3DTo2D(
                doubleArrayOf(1.0, 1.0, 2.0, 2.0),
            ),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                0.30116867893975674,
                -0.4083419284197014,
                -17.550036632634857,
            ),
            actual = view.worldToFocal(
                doubleArrayOf(1.0, 1.0, 2.0, 2.0),
            ),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                0.30116867893975674,
                -0.4083419284197014,
                -17.550036632634857,
            ),
            actual = view.worldToFocal(
                world = doubleArrayOf(1.0, 1.0, 2.0, 2.0),
                homogeneous = false,
            ),
        )
        assertEquals(
            1.3763819204711738,
            view.focalDist,
            TOLERANCE,
        )
    }

    @Test
    fun projectionHelpersMatchOfficialFixtureAndRoundTripPlane() {
        val view = createView(projection = "parallel")
        val clamped = view.project3DToCube(
            doubleArrayOf(1.0, -8.0, 9.0, -4.0),
        )

        assertContentEquals(
            doubleArrayOf(1.0, -5.0, 6.0, -3.0),
            clamped.first,
        )
        assertTrue(clamped.second)
        assertTrue(
            view.isInCube(doubleArrayOf(1.0, 1.0, 2.0, 2.0)),
        )
        assertFalse(
            view.isInCube(doubleArrayOf(1.0, 8.0, 2.0, 2.0)),
        )
        assertEquals(
            2.3333333333333335,
            view.intersectionLineCube(
                point = doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                direction = doubleArrayOf(1.0, 2.0, 3.0),
                ratio = Double.POSITIVE_INFINITY,
            ),
            TOLERANCE,
        )
        assertEquals(
            -1.0,
            view.intersectionLineCube(
                point = doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                direction = doubleArrayOf(1.0, 2.0, 3.0),
                ratio = Double.NEGATIVE_INFINITY,
            ),
            TOLERANCE,
        )

        val source = doubleArrayOf(1.0, 2.0, -1.0, 3.0)
        val projected = view.project3DTo2D(source)
        val restored = view.project2DTo3DPlane(
            coordinates2D = projected,
            normal = doubleArrayOf(1.0, 0.0, 0.0, 1.0),
            foot = doubleArrayOf(1.0, 0.0, 0.0, source[3]),
        )
        assertVectorMatches(source, restored)
    }

    @Test
    fun numericDynamicAndTransformedPointsMatchOfficialLifecycle() {
        val board = createBoard()
        val view = createView(board = board, projection = "parallel")
        var dynamicCoordinates = doubleArrayOf(2.0, -1.0, 3.0)
        var dynamicX = -2.0
        val numeric = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(1.0, 2.0, 2.0),
                id = "numeric",
                name = "",
            ),
        )
        val homogeneous = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(2.0, 4.0, 6.0, 8.0),
                id = "homogeneous",
                name = "",
            ),
        )
        val functionPoint = point3D(
            Point3D.create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Function(
                    Point3DArrayEvaluator {
                        GMResult.Ok(dynamicCoordinates.copyOf())
                    },
                ),
                id = "functionPoint",
                name = "",
                fixed = true,
            ),
        )
        val mixedPoint = point3D(
            Point3D.create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Values(
                    listOf(
                        Point3DCoordinateValue.Dynamic(
                            Point3DScalarEvaluator {
                                GMResult.Ok(dynamicX)
                            },
                        ),
                        Point3DCoordinateValue.Numeric(3.0),
                        Point3DCoordinateValue.Dynamic(
                            Point3DScalarEvaluator {
                                GMResult.Ok(numeric.Z() - 1.0)
                            },
                        ),
                    ),
                ),
                dependencies = listOf(numeric),
                id = "mixedPoint",
                name = "",
                fixed = true,
            ),
        )
        val translation = transformation3D(
            Transformation.create3D(
                "translate",
                doubleArrayOf(2.0, -3.0, 4.0),
            ),
        )
        val transformed = point3D(
            Point3D.create(
                view = view,
                basePoint = numeric,
                transformations = listOf(translation),
                id = "transformed",
                name = "",
                fixed = true,
            ),
        )

        board.fullUpdate()
        assertVectorMatches(
            doubleArrayOf(1.0, 1.0, 2.0, 2.0),
            numeric.coords,
        )
        assertVectorMatches(
            doubleArrayOf(2.0, 4.0, 6.0, 8.0),
            homogeneous.coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 2.0, -1.0, 3.0),
            functionPoint.coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, -2.0, 3.0, 1.0),
            mixedPoint.coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 3.0, -1.0, 6.0),
            transformed.coords,
        )
        assertSame(numeric, view.select("numeric"))
        assertSame(numeric, view.objects["numeric"])
        assertEquals(listOf("numeric"), transformed.parents)
        assertEquals(listOf("transformed"), transformed.point2D.parents)
        assertFalse(transformed.point2D.dump)
        assertFalse(transformed.point2D.isDraggable)

        dynamicCoordinates = doubleArrayOf(-3.0, 4.0, 1.0)
        dynamicX = 5.0
        assertIs<GMResult.Ok<Point3D>>(
            numeric.setPosition(doubleArrayOf(2.0, -2.0, 1.0)),
        )
        board.fullUpdate()

        assertVectorMatches(
            doubleArrayOf(1.0, 2.0, -2.0, 1.0),
            numeric.coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, -3.0, 4.0, 1.0),
            functionPoint.coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 5.0, 3.0, 0.0),
            mixedPoint.coords,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 4.0, -5.0, 5.0),
            transformed.coords,
        )

        assertIs<GMResult.Ok<Unit>>(translation.applyOnce(numeric))
        board.fullUpdate()
        assertVectorMatches(
            doubleArrayOf(1.0, 4.0, -5.0, 5.0),
            numeric.coords,
        )

        assertIs<GMResult.Ok<Point3D>>(
            numeric.setPosition(doubleArrayOf(2.0, 8.0, 10.0, 12.0)),
        )
        board.fullUpdate()
        assertVectorMatches(
            doubleArrayOf(1.0, 4.0, 5.0, 6.0),
            numeric.coords,
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                1.0,
                -0.02662282966670615,
                1.0469006176762976,
            ),
            actual = numeric.point2D.coords.usrCoords,
        )
    }

    @Test
    fun pointRemovalCleansProxyAndViewRegistry() {
        val board = createBoard()
        val view = createView(board = board, projection = "parallel")
        val point = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(1.0, 2.0, 3.0),
                id = "point",
                name = "",
            ),
        )
        val proxyId = point.point2D.id

        board.removeObject(point)

        assertNull(board.elementById(point.id))
        assertNull(board.elementById(proxyId))
        assertNull(view.select(point.id))
    }

    @Test
    fun malformedConstructionReturnsStructuredErrors() {
        val view = createView(projection = "parallel")

        assertIs<GMResult.Err<Point3DError.InvalidCoordinateCount>>(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Err<Point3DError.InvalidTransformationCount>>(
            Point3D.create(
                view = view,
                basePoint = point3D(
                    Point3D.create(
                        view = view,
                        coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                    ),
                ),
                transformations = emptyList(),
            ),
        )
        assertIs<GMResult.Err<View3DError.InvalidBoundingBox>>(
            View3D.create(
                board = createBoard(),
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                ),
            ),
        )
        assertIs<GMResult.Err<View3DError.ProjectionEvaluation>>(
            View3D.create(
                board = createBoard(),
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                projectionSource = View3DProjectionSource.Dynamic(
                    View3DProjectionEvaluator {
                        GMResult.Err(
                            View3DProjectionDynamicError.Rejected(
                                "projection",
                            ),
                        )
                    },
                ),
            ),
        )
    }

    @Test
    fun rotationCanBeRecoveredAsTaitBryanAngles() {
        val view = createView(projection = "parallel")
        view.setAngles(
            azimuth = 2.2,
            elevation = -0.4,
            bank = 0.6,
        )
        val expected = view.matrix3DRot.map(DoubleArray::copyOf)

        view.setAnglesFromRotation()

        assertEquals(2.2, view.angles.az, TOLERANCE)
        assertEquals(-0.4, view.angles.el, TOLERANCE)
        assertEquals(0.6, view.angles.bank, TOLERANCE)
        assertMatrixMatches(expected.toTypedArray(), view.matrix3DRot)
    }

    @Test
    fun trackballNavigationChangesRotationWithoutDomEvents() {
        val view = assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = createBoard(),
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                trackballAttributes =
                    View3DTrackballAttributes(enabled = true),
                id = "view",
                name = "",
            ),
        ).value
        val initial = view.matrix3DRot.map(DoubleArray::copyOf)

        view.pointerDownHandler(
            View3DNavigationEvent(
                screenX = 100.0,
                screenY = 100.0,
                button = 0,
            ),
        )
        view.trackballHandler(
            View3DNavigationEvent(
                screenX = 130.0,
                screenY = 115.0,
                boardX = 130.0,
                boardY = 115.0,
                button = 0,
            ),
        )
        view.pointerUpHandler()

        assertFalse(
            initial.indices.all { row ->
                initial[row].contentEquals(view.matrix3DRot[row])
            },
        )
        assertFalse(view.angles.az.isNaN())
        assertFalse(view.angles.el.isNaN())
        assertFalse(view.angles.bank.isNaN())
    }

    @Test
    fun storedViewsAndAzimuthAnimationFollowPortableScheduler() {
        var scheduledDelay = -1L
        var scheduledAction: (() -> Unit)? = null
        val view = assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = createBoard(),
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                storedViews = listOf(
                    doubleArrayOf(0.2, 0.3, 2.0),
                    doubleArrayOf(1.2, 0.7, 3.0),
                ),
                azimuthAnimationScheduler =
                    View3DAzimuthAnimationScheduler { delay, action ->
                        scheduledDelay = delay
                        scheduledAction = action
                    },
                id = "view",
                name = "",
            ),
        ).value

        view.setCurrentView(-1)

        assertEquals(1, view.currentView)
        assertEquals(1.2, view.angles.az, TOLERANCE)
        assertEquals(0.7, view.angles.el, TOLERANCE)
        assertEquals(1.0 / 3.0, view.getCameraDistance(), TOLERANCE)

        val beforeAnimation = view.angles.az
        view.animateAzimuth()

        assertEquals(200L, scheduledDelay)
        assertEquals(
            beforeAnimation + 0.1,
            view.angles.az,
            TOLERANCE,
        )
        view.stopAzimuth()
        scheduledAction?.invoke()
        assertEquals(
            beforeAnimation + 0.1,
            view.angles.az,
            TOLERANCE,
        )

        view.freeR()
        assertEquals(1.01, view.getCameraDistance(), TOLERANCE)
    }

    @Test
    fun pointMovementMatchesOfficialPathAndCallbackSemantics() {
        var scheduledInterval = Double.NaN
        var advance: (() -> Boolean)? = null
        val board = Board(
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
        val view = createView(board = board, projection = "parallel")
        val point = point3D(
            Point3D.create(view, doubleArrayOf(0.0, 0.0, 0.0)),
        )
        var callbackCount = 0

        assertIs<GMResult.Ok<Point3D>>(
            point.moveTo(
                where = doubleArrayOf(3.0, 6.0, 9.0),
                timeMilliseconds = 100.0,
                options = Point3DMoveOptions(
                    callback = { callbackCount += 1 },
                    effect = "==",
                ),
            ),
        )
        assertEquals(20.0, scheduledInterval)
        val step = advance ?: error("animation was not scheduled")
        var frameCount = 0
        do {
            frameCount += 1
        } while (step())

        assertEquals(5, frameCount)
        assertEquals(3.0, point.X())
        assertEquals(6.0, point.Y())
        assertEquals(9.0, point.Z())
        assertEquals(1, callbackCount)
        assertIs<GMResult.Err<Point3DError.InvalidAnimationEffect>>(
            point.moveTo(
                where = doubleArrayOf(0.0, 0.0, 0.0),
                timeMilliseconds = 100.0,
                options = Point3DMoveOptions(effect = "invalid"),
            ),
        )
    }

    private fun createBoard(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun createView(
        board: Board = createBoard(),
        projection: String,
    ): View3D =
        assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                projection = projection,
                id = "view",
                name = "",
            ),
        ).value

    private fun point3D(
        result: GMResult<Point3D, Point3DError>,
    ): Point3D = assertIs<GMResult.Ok<Point3D>>(result).value

    private fun transformation3D(
        result: GMResult<Transformation, TransformationError>,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(result).value

    private fun assertMatrixMatches(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertVectorMatches(expected[index], actual[index])
        }
    }

    private fun assertVectorMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected = expected[index],
                actual = actual[index],
                absoluteTolerance = TOLERANCE,
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
