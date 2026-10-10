package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class View3DPoint3DTest {
    @Test
    fun creationAttributesBelongToProxyAndDynamicValuesReceivePoint3D() {
        val point = point3D(
            Point3D.create(
                view = createView(projection = "parallel"),
                coordinates = doubleArrayOf(1.0, 2.0, 3.0),
                id = "point",
                name = "P",
                fixed = true,
                attributes = linkedMapOf(
                    "strokeColor" to "#123456",
                    "size" to GeometryElementPropertyEvaluator {
                        if (it is Point3D && it.name == "P") 7.0 else 1.0
                    },
                    "withLabel" to true,
                    "label" to mapOf("offset" to listOf(4.0, 5.0)),
                ),
            ),
        )

        val label = assertNotNull(point.point2D.elementLabel)
        assertNull(point.elementLabel)
        assertEquals("#123456", point.getAttribute("strokeColor"))
        assertEquals(7.0, point.point2D.evalVisProp("size"))
        assertSame(point.point2D, label.anchor)
        assertEquals(listOf(4.0, 5.0), label.screenOffset.toList())
        assertEquals(
            listOf<GeometryElement>(point.point2D),
            point.inherits,
        )

        point.setAttribute(
            mapOf(
                "size" to 11.0,
                "withLabel" to false,
            ),
        )

        assertEquals(11.0, point.point2D.getAttribute("size"))
        assertFalse(point.point2D.hasLabel)
        assertSame(label, point.point2D.elementLabel)
        assertNull(point.elementLabel)
    }

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
    fun lineGliderProjectionAndDependenciesMatchOfficialFixture() {
        val board = createBoard()
        val view = createView(board = board, projection = "parallel")
        val anchor = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                id = "anchor",
                name = "",
            ),
        )
        val host = assertIs<GMResult.Ok<Line3D>>(
            Line3D.create(
                view = view,
                point = anchor,
                directionSource = Line3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(2.0),
                        Line3DCoordinateValue.Numeric(1.0),
                        Line3DCoordinateValue.Numeric(-1.0),
                    ),
                ),
                rangeSource = listOf(
                    Line3DCoordinateValue.Numeric(-1.0),
                    Line3DCoordinateValue.Numeric(2.0),
                ),
                id = "host",
                name = "",
            ),
        ).value
        val glider = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(4.0, 5.0, 2.0),
                id = "glider",
                name = "",
                slide = host,
            ),
        )
        val clamped = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(20.0, 20.0, -20.0),
                id = "clamped",
                name = "",
                slide = host,
            ),
        )

        board.fullUpdate()

        assertSame(host, glider.slide)
        assertVectorMatches(
            doubleArrayOf(1.0, 4.0, 2.0, -2.0),
            glider.coords,
        )
        assertVectorMatches(doubleArrayOf(2.0), glider.position)
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                -2.066348496841048,
                -4.017821966498767,
            ),
            glider.point2D.coords.usrCoords,
        )
        assertVectorMatches(glider.coords, clamped.coords)
        assertVectorMatches(glider.position, clamped.position)
        assertEquals(listOf("host"), glider.parents)
        assertSame(glider, host.childElements["glider"])
        assertSame(host, glider.ancestors["host"])
        assertSame(anchor, glider.ancestors["anchor"])

        assertIs<GMResult.Ok<Point3D>>(
            anchor.setPosition(doubleArrayOf(1.0, -2.0, 3.0)),
        )
        board.fullUpdate()

        assertVectorMatches(
            doubleArrayOf(1.0, 5.0, 0.0, 1.0),
            glider.coords,
        )
        assertVectorMatches(doubleArrayOf(2.0), glider.position)

        glider.point2D.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-0.25, 0.75),
        )
        glider.update()

        assertVectorMatches(
            doubleArrayOf(
                1.0,
                -0.634863542226534,
                -2.817431771113267,
                3.817431771113267,
            ),
            glider.coords,
        )
        assertVectorMatches(doubleArrayOf(2.0), glider.position)
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                -3.3183457875680102,
                1.2700888589526664,
            ),
            glider.point2D.coords.usrCoords,
        )

        glider.update()

        assertVectorMatches(
            doubleArrayOf(
                1.0,
                -0.6348635422265339,
                -2.817431771113267,
                3.817431771113267,
            ),
            glider.coords,
        )
        assertVectorMatches(
            doubleArrayOf(-0.8174317711132669),
            glider.position,
        )
    }

    @Test
    fun allProjectable3DHostsConstrainPointAndProxyDrag() {
        val board = createBoard()
        val view = createView(board = board, projection = "parallel")
        val center = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                id = "center",
                name = "",
                attributes = mapOf("withLabel" to false),
            ),
        )
        val range = listOf(
            Line3DCoordinateValue.Numeric(-2.0),
            Line3DCoordinateValue.Numeric(2.0),
        )
        val circle = assertIs<GMResult.Ok<Circle3D>>(
            Circle3D.create(
                view = view,
                center = center,
                normalSource = Circle3DNormalSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(1.0),
                    ),
                ),
                radiusSource = Line3DCoordinateValue.Numeric(2.0),
                sampleCount = 9,
                id = "circle",
                name = "",
            ),
        ).value
        val curve = assertIs<GMResult.Ok<Curve3D>>(
            Curve3D.create(
                view = view,
                source = Curve3DSource.Components(
                    x = Curve3DScalarEvaluator { GMResult.Ok(it) },
                    y = Curve3DScalarEvaluator { GMResult.Ok(it * it) },
                    z = Curve3DScalarEvaluator { GMResult.Ok(-it) },
                ),
                rangeSource = range,
                sampleCount = 9,
                id = "curve",
                name = "",
            ),
        ).value
        val plane = assertIs<GMResult.Ok<Plane3D>>(
            Plane3D.create(
                view = view,
                point = center,
                direction1Source = Plane3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(1.0),
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(0.0),
                    ),
                ),
                direction2Source = Plane3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(1.0),
                        Line3DCoordinateValue.Numeric(0.0),
                    ),
                ),
                rangeUSource = range,
                rangeVSource = range,
                id = "plane",
                name = "",
            ),
        ).value
        val sphere = assertIs<GMResult.Ok<Sphere3D>>(
            Sphere3D.create(
                view = view,
                center = center,
                radiusSource = Line3DCoordinateValue.Numeric(2.0),
                sampleCount = 17,
                id = "sphere",
                name = "",
            ),
        ).value
        val surface = assertIs<GMResult.Ok<Surface3D>>(
            Surface3D.create(
                view = view,
                source = Surface3DSource.Components(
                    x = Surface3DScalarEvaluator { u, _ ->
                        GMResult.Ok(u)
                    },
                    y = Surface3DScalarEvaluator { _, v ->
                        GMResult.Ok(v)
                    },
                    z = Surface3DScalarEvaluator { u, v ->
                        GMResult.Ok(u + v)
                    },
                ),
                rangeUSource = range,
                rangeVSource = range,
                attributes = Surface3DAttributes(
                    stepsU = 2,
                    stepsV = 2,
                ),
                id = "surface",
                name = "",
            ),
        ).value
        val hosted = listOf(
            circle to doubleArrayOf(1.4, 1.4, 0.7),
            curve to doubleArrayOf(0.7, 0.8, -0.9),
            plane to doubleArrayOf(1.5, -0.5, 2.0),
            sphere to doubleArrayOf(1.4, 1.4, 0.2),
            surface to doubleArrayOf(0.8, -0.4, 1.1),
        ).mapIndexed { index, (host, coordinates) ->
            point3D(
                Point3D.create(
                    view = view,
                    coordinates = coordinates,
                    id = "hosted$index",
                    name = "",
                    slide = host,
                    attributes = mapOf("withLabel" to false),
                ),
            )
        }

        board.fullUpdate()

        assertEquals(listOf(1, 1, 2, 2, 2), hosted.map {
            it.position.size
        })
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                1.0836163267851853,
                0.977063937492063,
                1.3678895124888881,
            ),
            hosted[3].coords,
        )
        for ((index, point) in hosted.withIndex()) {
            assertNull(point.coordinateEvaluationError)
            assertTrue(point.coords.all(Double::isFinite))
            assertSame(point.slide, point.ancestors[point.slide?.id])
            assertSame(point, point.slide?.childElements?.get(point.id))
            assertIs<GMResult.Ok<Point3D>>(
                point.setPositionFrom2D(doubleArrayOf(-0.25, 0.75)),
                "host index $index",
            )
            assertTrue(point.coords.all(Double::isFinite))
            assertNull(point.coordinateEvaluationError)
        }

        val circlePoint = hosted[0]
        assertEquals(0.0, circlePoint.Z(), 1.0e-6)
        assertEquals(
            2.0,
            kotlin.math.hypot(circlePoint.X(), circlePoint.Y()),
            1.0e-5,
        )
        val curvePoint = hosted[1]
        assertEquals(
            curvePoint.X() * curvePoint.X(),
            curvePoint.Y(),
            1.0e-5,
        )
        assertEquals(-curvePoint.X(), curvePoint.Z(), 1.0e-5)
        assertEquals(0.0, hosted[2].Z(), 1.0e-6)
        val spherePoint = hosted[3]
        assertEquals(
            2.0,
            kotlin.math.sqrt(
                spherePoint.X() * spherePoint.X() +
                    spherePoint.Y() * spherePoint.Y() +
                    spherePoint.Z() * spherePoint.Z(),
            ),
            1.0e-5,
        )
        val surfacePoint = hosted[4]
        assertEquals(
            surfacePoint.X() + surfacePoint.Y(),
            surfacePoint.Z(),
            1.0e-5,
        )
    }

    @Test
    fun lineGliderRejectsInvalidHostLifecycle() {
        val board = createBoard()
        val view = createView(board = board, projection = "parallel")
        val otherView = assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-4.0, -3.0),
                size = doubleArrayOf(2.0, 2.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-2.0, 2.0),
                    doubleArrayOf(-2.0, 2.0),
                    doubleArrayOf(-2.0, 2.0),
                ),
                id = "otherView",
                name = "",
            ),
        ).value
        val anchor = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                id = "anchor",
                name = "",
            ),
        )
        val host = assertIs<GMResult.Ok<Line3D>>(
            Line3D.create(
                view = view,
                point = anchor,
                directionSource = Line3DDirectionSource.Values(
                    listOf(
                        Line3DCoordinateValue.Numeric(1.0),
                        Line3DCoordinateValue.Numeric(0.0),
                        Line3DCoordinateValue.Numeric(0.0),
                    ),
                ),
                id = "host",
                name = "",
            ),
        ).value

        assertIs<GMResult.Err<Point3DError.SlideViewMismatch>>(
            Point3D.create(
                view = otherView,
                coordinates = doubleArrayOf(1.0, 2.0, 3.0),
                slide = host,
            ),
        )

        board.removeObject(host)

        assertIs<GMResult.Err<Point3DError.SlideNotRegistered>>(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(1.0, 2.0, 3.0),
                slide = host,
            ),
        )

        assertIs<GMResult.Err<Point3DError.UnsupportedSlide>>(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(1.0, 2.0, 3.0),
                slide = anchor,
            ),
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
    fun cameraNavigationMatchesOfficialFixture() {
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            keepAspectRatio = false,
            canvasWidth = 500.0,
            canvasHeight = 400.0,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )
        val view = assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-4.0, -3.0),
                size = doubleArrayOf(8.0, 6.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-5.0, 5.0),
                ),
                storedViews = listOf(
                    doubleArrayOf(0.2, 0.3, 2.0),
                    doubleArrayOf(1.2, 0.7, 3.0),
                ),
                id = "view",
                name = "",
            ),
        ).value

        view.setCurrentView(-1)
        assertAngles(view, azimuth = 1.2, elevation = 0.7, bank = 0.0)
        assertEquals(1.0 / 3.0, view.getCameraDistance(), TOLERANCE)
        view.nextView()
        assertAngles(view, azimuth = 0.2, elevation = 0.3, bank = 0.0)
        assertEquals(0.5, view.getCameraDistance(), TOLERANCE)
        view.previousView()
        view.freeR()
        assertAngles(view, azimuth = 1.2, elevation = 0.7, bank = 0.0)
        assertEquals(1.01, view.getCameraDistance(), TOLERANCE)

        view.azimuthEventHandler(
            View3DNavigationEvent(key = "ArrowRight"),
        )
        assertAngles(
            view,
            azimuth = 1.374532925199433,
            elevation = 0.7,
            bank = 0.0,
        )
        view.elevationEventHandler(
            View3DNavigationEvent(key = "ArrowUp"),
        )
        assertAngles(
            view,
            azimuth = 1.374532925199433,
            elevation = 0.525467074800567,
            bank = 0.0,
        )
        view.bankEventHandler(View3DNavigationEvent(key = ">"))
        assertAngles(
            view,
            azimuth = 1.374532925199433,
            elevation = 0.525467074800567,
            bank = 0.1745329251994332,
        )

        view.pointerDownHandler(
            View3DNavigationEvent(
                screenX = 100.0,
                screenY = 100.0,
                button = 0,
            ),
        )
        view.azimuthElevationEventHandler(
            View3DNavigationEvent(
                screenX = 150.0,
                screenY = 120.0,
                button = 0,
            ),
        )
        view.pointerUpHandler()
        assertAngles(
            view,
            azimuth = 2.0028514559173916,
            elevation = 0.7767944870877505,
            bank = 0.1745329251994332,
        )

        view.pointerDownHandler(
            View3DNavigationEvent(
                screenX = 150.0,
                screenY = 120.0,
                button = 0,
            ),
        )
        view.bankEventHandler(View3DNavigationEvent(deltaY = 25.0))
        view.pointerUpHandler()
        assertAngles(
            view,
            azimuth = 2.0028514559173916,
            elevation = 0.7767944870877505,
            bank = 0.20594885173533095,
        )
    }

    @Test
    fun trackballNavigationMatchesOfficialFixture() {
        val board = Board.fromBoundingBox(
            boundingBox = doubleArrayOf(-5.0, 4.0, 5.0, -4.0),
            keepAspectRatio = false,
            canvasWidth = 500.0,
            canvasHeight = 400.0,
            defaultCurveMinimum = -6.0,
            defaultCurveMaximum = 6.0,
        )
        val view = assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-4.0, -3.0),
                size = doubleArrayOf(8.0, 6.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-5.0, 5.0),
                ),
                trackballAttributes =
                    View3DTrackballAttributes(enabled = true),
                id = "view",
                name = "",
            ),
        ).value

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

        assertAngles(
            view,
            azimuth = 1.2071850704924463,
            elevation = 0.4024113197793909,
            bank = 0.09787457829086581,
        )
        assertMatrixMatches(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(
                    0.0,
                    -0.3897176692613368,
                    0.9165347057602141,
                    0.08991257644261447,
                ),
                doubleArrayOf(
                    0.0,
                    -0.32952681439092557,
                    -0.2299495912578235,
                    0.9157157113852139,
                ),
                doubleArrayOf(
                    0.0,
                    0.8599605902963693,
                    0.3272419878582172,
                    0.39163818062048367,
                ),
            ),
            actual = view.matrix3DRot,
        )
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

    @Test
    fun pointMovementEasingAndPathReplacementMatchOfficialFixture() {
        val expectedFrames = mapOf(
            "<>" to doubleArrayOf(
                0.0,
                0.5857864376269051,
                2.0000000000000004,
                3.414213562373095,
                4.0,
            ),
            "<" to doubleArrayOf(
                0.0,
                2.3125,
                3.5,
                3.9375,
                4.0,
            ),
            ">" to doubleArrayOf(
                0.0,
                0.0625,
                0.5,
                1.6875,
                4.0,
            ),
        )
        for ((effect, expectedX) in expectedFrames) {
            var scheduledInterval = Double.NaN
            var advance: (() -> Boolean)? = null
            val board = Board(
                originX = 0.0,
                originY = 0.0,
                unitX = 1.0,
                unitY = 1.0,
                animationDelay = 25.0,
                animationScheduler =
                    BoardAnimationScheduler { interval, step ->
                        scheduledInterval = interval
                        advance = step
                    },
            )
            val point = point3D(
                Point3D.create(
                    view = createView(
                        board = board,
                        projection = "parallel",
                    ),
                    coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                ),
            )
            var callbackCount = 0

            assertIs<GMResult.Ok<Point3D>>(
                point.moveTo(
                    where = doubleArrayOf(4.0, 8.0, -4.0),
                    timeMilliseconds = 100.0,
                    options = Point3DMoveOptions(
                        callback = { callbackCount += 1 },
                        effect = effect,
                    ),
                ),
            )

            assertEquals(20.0, scheduledInterval)
            val step = advance ?: error("$effect animation was not scheduled")
            val actualX = buildList {
                do {
                    val keepRunning = step()
                    add(point.X())
                } while (keepRunning)
            }
            assertVectorMatches(expectedX, actualX.toDoubleArray())
            assertEquals(1, callbackCount)
        }

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
        val replacingPoint = point3D(
            Point3D.create(
                view = createView(board = board, projection = "parallel"),
                coordinates = doubleArrayOf(0.0, 0.0, 0.0),
            ),
        )
        val callbacks = mutableListOf<String>()
        assertIs<GMResult.Ok<Point3D>>(
            replacingPoint.moveAlong(
                traversePath = listOf(
                    doubleArrayOf(1.0, 1.0, 1.0),
                    doubleArrayOf(2.0, 2.0, 2.0),
                    doubleArrayOf(3.0, 3.0, 3.0),
                ),
                timeMilliseconds = 90.0,
                options = Point3DMoveOptions(
                    callback = { callbacks += "first" },
                ),
            ),
        )
        assertIs<GMResult.Ok<Point3D>>(
            replacingPoint.moveAlong(
                traversePath = listOf(
                    doubleArrayOf(-1.0, -2.0, -3.0),
                    doubleArrayOf(-4.0, -5.0, -6.0),
                ),
                timeMilliseconds = 20.0,
                options = Point3DMoveOptions(
                    callback = { callbacks += "replacement" },
                ),
            ),
        )

        assertEquals(30.0, scheduledInterval)
        val replacementStep =
            advance ?: error("replacement animation was not scheduled")
        while (replacementStep()) {
            Unit
        }
        assertVectorMatches(
            doubleArrayOf(1.0, -4.0, -5.0, -6.0),
            replacingPoint.coords,
        )
        assertEquals(listOf("replacement"), callbacks)

        var immediateCallbackCount = 0
        assertIs<GMResult.Ok<Point3D>>(
            replacingPoint.moveTo(
                where = doubleArrayOf(1.0, 2.0, 3.0),
                timeMilliseconds = 0.0,
                options = Point3DMoveOptions(
                    callback = { immediateCallbackCount += 1 },
                ),
            ),
        )
        assertEquals(0, immediateCallbackCount)
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

    private fun assertAngles(
        view: View3D,
        azimuth: Double,
        elevation: Double,
        bank: Double,
    ) {
        assertEquals(azimuth, view.angles.az, TOLERANCE)
        assertEquals(elevation, view.angles.el, TOLERANCE)
        assertEquals(bank, view.angles.bank, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
