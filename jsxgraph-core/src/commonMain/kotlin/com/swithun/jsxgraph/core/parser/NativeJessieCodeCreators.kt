/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> creator / isCreator,
 * src/base/point.js -> createPoint / createPolePoint,
 * src/base/line.js -> createLine / createSegment / createArrow /
 * createRadicalAxis / createTangent / createTangentTo / createNormal /
 * createPolarLine,
 * src/base/ticks.js -> createTicks / createHatchmark,
 * src/base/circle.js -> createCircle,
 * src/element/composition.js -> createMidpoint / createCircumcenter /
 * createCircumcircle / createOrthogonalProjection / createPerpendicular /
 * createPerpendicularPoint / createPerpendicularSegment /
 * createParallelPoint / createParallel / createArrowParallel /
 * createBisector /
 * createAngularBisectorsOfTwoLines / createIncenter /
 * createIncircle / createReflection / createMirrorElement /
 * createMirrorPoint,
 * src/base/curve.js -> createCurve / createFunctiongraph /
 * createStepfunction / createDerivative / createSpline /
 * createCardinalSpline / createRiemannsum / createBoxPlot /
 * createCurveIntersection / createCurveUnion / createCurveDifference,
 * src/element/comb.js -> createComb,
 * src/element/composition.js -> createInequality,
 * src/element/vectorfield.js -> createVectorField / createSlopeField,
 * src/base/polygon.js -> createPolygon / createPolygonalChain /
 * createParallelogram / createRegularPolygon,
 * src/base/text.js -> createText,
 * src/base/transformation.js -> createTransform,
 * src/3d/point3d.js -> createPoint3D,
 * src/3d/linspace3d.js -> createLine3D / createIntersectionLine3D /
 * createPlane3D,
 * src/3d/box3d.js -> createAxis3D,
 * src/3d/ticks3d.js -> createTicks3D,
 * src/3d/text3d.js -> createText3D,
 * src/3d/polygon3d.js -> createPolygon3D,
 * src/3d/polyhedron3d.js -> createPolyhedron3D,
 * src/3d/curve3d.js -> createCurve3D / createVectorfield3D,
 * src/3d/circle3d.js -> createCircle3D / createIntersectionCircle3D,
 * src/3d/sphere3d.js -> createSphere3D,
 * src/element/arc.js -> createArc / createSemicircle /
 * createCircumcircleArc / createMinorArc / createMajorArc,
 * src/element/sector.js -> createSector / createAngle /
 * createCircumcircleSector / createMinorSector / createMajorSector /
 * createNonreflexAngle / createReflexAngle,
 * src/element/conic.js ->
 * createEllipse / createHyperbola / createParabola
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.AngleRadius
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.ArcError
import com.swithun.jsxgraph.core.base.Axis
import com.swithun.jsxgraph.core.base.AxisAttributes
import com.swithun.jsxgraph.core.base.AxisDistance
import com.swithun.jsxgraph.core.base.AxisError
import com.swithun.jsxgraph.core.base.Axes3D
import com.swithun.jsxgraph.core.base.Axes3DError
import com.swithun.jsxgraph.core.base.Axes3DTicksAttributes
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.BisectorLineAttributes
import com.swithun.jsxgraph.core.base.BisectorLines
import com.swithun.jsxgraph.core.base.BisectorLinesError
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Circle3D
import com.swithun.jsxgraph.core.base.Circle3DError
import com.swithun.jsxgraph.core.base.Circle3DNormalSource
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CircumcenterError
import com.swithun.jsxgraph.core.base.CircumcenterPoint
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Curve3D
import com.swithun.jsxgraph.core.base.Curve3DArrayEvaluator
import com.swithun.jsxgraph.core.base.Curve3DDynamicError
import com.swithun.jsxgraph.core.base.Curve3DError
import com.swithun.jsxgraph.core.base.Curve3DScalarEvaluator
import com.swithun.jsxgraph.core.base.Curve3DSource
import com.swithun.jsxgraph.core.base.Curve3DVectorFieldArrayFunction
import com.swithun.jsxgraph.core.base.Curve3DVectorFieldComponentFunction
import com.swithun.jsxgraph.core.base.Curve3DVectorFieldFunction
import com.swithun.jsxgraph.core.base.CurveCoordinateSplinePoint
import com.swithun.jsxgraph.core.base.CurveElementSplinePoint
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.CurveFunctionSplinePoint
import com.swithun.jsxgraph.core.base.CurveSplinePoint
import com.swithun.jsxgraph.core.base.CurveStaticPoint
import com.swithun.jsxgraph.core.base.CurveStepTerm
import com.swithun.jsxgraph.core.base.CurveSlopeFieldFunction
import com.swithun.jsxgraph.core.base.CurveVectorFieldArrayFunction
import com.swithun.jsxgraph.core.base.CurveVectorFieldComponentFunction
import com.swithun.jsxgraph.core.base.CurveVectorFieldFunction
import com.swithun.jsxgraph.core.base.Ellipse
import com.swithun.jsxgraph.core.base.EllipseError
import com.swithun.jsxgraph.core.base.Face3DAttributes
import com.swithun.jsxgraph.core.base.Face3DLightAttributes
import com.swithun.jsxgraph.core.base.Face3DShaderAttributes
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.GeometryElement3D
import com.swithun.jsxgraph.core.base.Hatch
import com.swithun.jsxgraph.core.base.Hyperbola
import com.swithun.jsxgraph.core.base.HyperbolaError
import com.swithun.jsxgraph.core.base.IncenterPoint
import com.swithun.jsxgraph.core.base.IncircleCircle
import com.swithun.jsxgraph.core.base.IntersectionCircle3D
import com.swithun.jsxgraph.core.base.IntersectionCircle3DError
import com.swithun.jsxgraph.core.base.IntersectionLine3D
import com.swithun.jsxgraph.core.base.IntersectionLine3DError
import com.swithun.jsxgraph.core.base.IntersectionLine3DPointAttributes
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Line3D
import com.swithun.jsxgraph.core.base.Line3DArrayEvaluator
import com.swithun.jsxgraph.core.base.Line3DCoordinateValue
import com.swithun.jsxgraph.core.base.Line3DDirectionSource
import com.swithun.jsxgraph.core.base.Line3DDynamicError
import com.swithun.jsxgraph.core.base.Line3DError
import com.swithun.jsxgraph.core.base.Line3DScalarEvaluator
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.Mesh3D
import com.swithun.jsxgraph.core.base.Mesh3DError
import com.swithun.jsxgraph.core.base.Mesh3DPointSource
import com.swithun.jsxgraph.core.base.Mesh3DVectorSource
import com.swithun.jsxgraph.core.base.Normal
import com.swithun.jsxgraph.core.base.NormalError
import com.swithun.jsxgraph.core.base.OrthogonalConstructionError
import com.swithun.jsxgraph.core.base.OrthogonalPoint
import com.swithun.jsxgraph.core.base.OrthogonalPointKind
import com.swithun.jsxgraph.core.base.OtherIntersectionPoint
import com.swithun.jsxgraph.core.base.ParallelConstructionError
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.ParallelPoint
import com.swithun.jsxgraph.core.base.Parallelogram
import com.swithun.jsxgraph.core.base.ParallelogramError
import com.swithun.jsxgraph.core.base.PerpendicularLine
import com.swithun.jsxgraph.core.base.PerpendicularSegmentLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Point3D
import com.swithun.jsxgraph.core.base.Point3DArrayEvaluator
import com.swithun.jsxgraph.core.base.Point3DCoordinateSource
import com.swithun.jsxgraph.core.base.Point3DCoordinateValue
import com.swithun.jsxgraph.core.base.Point3DDynamicError
import com.swithun.jsxgraph.core.base.Point3DError
import com.swithun.jsxgraph.core.base.Point3DScalarEvaluator
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.Parabola
import com.swithun.jsxgraph.core.base.ParabolaError
import com.swithun.jsxgraph.core.base.Plane3D
import com.swithun.jsxgraph.core.base.Plane3DColormapAttributes
import com.swithun.jsxgraph.core.base.Plane3DDirectionSource
import com.swithun.jsxgraph.core.base.Plane3DError
import com.swithun.jsxgraph.core.base.Plane3DSurfaceAttributes
import com.swithun.jsxgraph.core.base.PointReflectionError
import com.swithun.jsxgraph.core.base.PointReflections
import com.swithun.jsxgraph.core.base.PolePoint
import com.swithun.jsxgraph.core.base.PolePointError
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Polygon3D
import com.swithun.jsxgraph.core.base.Polygon3DError
import com.swithun.jsxgraph.core.base.Polygon3DVertexAttributes
import com.swithun.jsxgraph.core.base.PolygonError
import com.swithun.jsxgraph.core.base.Polyhedron3D
import com.swithun.jsxgraph.core.base.Polyhedron3DError
import com.swithun.jsxgraph.core.base.Polyhedron3DFaceInput
import com.swithun.jsxgraph.core.base.Polyhedron3DVertexSource
import com.swithun.jsxgraph.core.base.RegularPolygon
import com.swithun.jsxgraph.core.base.RegularPolygonError
import com.swithun.jsxgraph.core.base.RadicalAxis
import com.swithun.jsxgraph.core.base.RadicalAxisError
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.SectorError
import com.swithun.jsxgraph.core.base.Sphere3D
import com.swithun.jsxgraph.core.base.Sphere3DError
import com.swithun.jsxgraph.core.base.Surface3D
import com.swithun.jsxgraph.core.base.Surface3DArrayEvaluator
import com.swithun.jsxgraph.core.base.Surface3DAttributes
import com.swithun.jsxgraph.core.base.Surface3DDynamicError
import com.swithun.jsxgraph.core.base.Surface3DError
import com.swithun.jsxgraph.core.base.Surface3DScalarEvaluator
import com.swithun.jsxgraph.core.base.Surface3DSource
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.Text3D
import com.swithun.jsxgraph.core.base.Text3DError
import com.swithun.jsxgraph.core.base.TextError
import com.swithun.jsxgraph.core.base.Ticks
import com.swithun.jsxgraph.core.base.TicksAnchor
import com.swithun.jsxgraph.core.base.TicksAttributes
import com.swithun.jsxgraph.core.base.TicksError
import com.swithun.jsxgraph.core.base.TicksSource
import com.swithun.jsxgraph.core.base.Ticks3D
import com.swithun.jsxgraph.core.base.Ticks3DError
import com.swithun.jsxgraph.core.base.Ticks3DPointSource
import com.swithun.jsxgraph.core.base.Tangent
import com.swithun.jsxgraph.core.base.TangentError
import com.swithun.jsxgraph.core.base.TangentTo
import com.swithun.jsxgraph.core.base.TangentToError
import com.swithun.jsxgraph.core.base.TangentToIdentity
import com.swithun.jsxgraph.core.base.TangentToLineAttributes
import com.swithun.jsxgraph.core.base.TangentToPointAttributes
import com.swithun.jsxgraph.core.base.Transformation
import com.swithun.jsxgraph.core.base.Transformation3DParameter
import com.swithun.jsxgraph.core.base.TransformationDynamicParameter
import com.swithun.jsxgraph.core.base.TransformationDynamicParameterError
import com.swithun.jsxgraph.core.base.TransformationDynamicVectorParameter
import com.swithun.jsxgraph.core.base.TransformationError
import com.swithun.jsxgraph.core.base.TransformationParameter
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
import com.swithun.jsxgraph.core.base.View3D
import com.swithun.jsxgraph.core.base.View3DError
import com.swithun.jsxgraph.core.math.ClipBooleanOperation
import com.swithun.jsxgraph.core.math.NumericsPoint2D
import com.swithun.jsxgraph.core.utils.JsNumberFormat

internal sealed interface JessieCodeCreatorError {
    data object BoardUnavailable : JessieCodeCreatorError

    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : JessieCodeCreatorError

    data class InvalidAttributeType(
        val attribute: String,
        val expected: String,
        val actual: String,
    ) : JessieCodeCreatorError

    data class UnsupportedAttributeValue(
        val attribute: String,
        val actual: String,
    ) : JessieCodeCreatorError

    data class PointFactory(
        val error: PointError,
    ) : JessieCodeCreatorError

    data class View3DFactory(
        val error: View3DError,
    ) : JessieCodeCreatorError

    data class View3DDefaultAxesFactory(
        val error: Axes3DError,
    ) : JessieCodeCreatorError

    data class Point3DFactory(
        val error: Point3DError,
    ) : JessieCodeCreatorError

    data class Line3DFactory(
        val error: Line3DError,
    ) : JessieCodeCreatorError

    data class IntersectionLine3DFactory(
        val error: IntersectionLine3DError,
    ) : JessieCodeCreatorError

    data class Axes3DFactory(
        val error: Axes3DError,
    ) : JessieCodeCreatorError

    data class Plane3DFactory(
        val error: Plane3DError,
    ) : JessieCodeCreatorError

    data class Mesh3DFactory(
        val error: Mesh3DError,
    ) : JessieCodeCreatorError

    data class Polyhedron3DFactory(
        val error: Polyhedron3DError,
    ) : JessieCodeCreatorError

    data class Polygon3DFactory(
        val error: Polygon3DError,
    ) : JessieCodeCreatorError

    data class Curve3DFactory(
        val error: Curve3DError,
    ) : JessieCodeCreatorError

    data class Circle3DFactory(
        val error: Circle3DError,
    ) : JessieCodeCreatorError

    data class IntersectionCircle3DFactory(
        val error: IntersectionCircle3DError,
    ) : JessieCodeCreatorError

    data class Sphere3DFactory(
        val error: Sphere3DError,
    ) : JessieCodeCreatorError

    data class Surface3DFactory(
        val error: Surface3DError,
    ) : JessieCodeCreatorError

    data class Ticks3DFactory(
        val error: Ticks3DError,
    ) : JessieCodeCreatorError

    data class TicksFactory(
        val error: TicksError,
    ) : JessieCodeCreatorError

    data class Text3DFactory(
        val error: Text3DError,
    ) : JessieCodeCreatorError

    data class PolePointFactory(
        val error: PolePointError,
    ) : JessieCodeCreatorError

    data class IntersectionFactory(
        val error: IntersectionError,
    ) : JessieCodeCreatorError

    data class PointReflectionFactory(
        val error: PointReflectionError,
    ) : JessieCodeCreatorError

    data class TransformationFactory(
        val error: TransformationError,
    ) : JessieCodeCreatorError

    data class LineFactory(
        val error: LineError,
    ) : JessieCodeCreatorError

    data class AxisFactory(
        val error: AxisError,
    ) : JessieCodeCreatorError

    data class RadicalAxisFactory(
        val error: RadicalAxisError,
    ) : JessieCodeCreatorError

    data class TangentFactory(
        val error: TangentError,
    ) : JessieCodeCreatorError

    data class TangentToFactory(
        val error: TangentToError,
    ) : JessieCodeCreatorError

    data class NormalFactory(
        val error: NormalError,
    ) : JessieCodeCreatorError

    data class MidpointFactory(
        val error: MidpointError,
    ) : JessieCodeCreatorError

    data class CircumcenterFactory(
        val error: CircumcenterError,
    ) : JessieCodeCreatorError

    data class OrthogonalFactory(
        val error: OrthogonalConstructionError,
    ) : JessieCodeCreatorError

    data class ParallelFactory(
        val error: ParallelConstructionError,
    ) : JessieCodeCreatorError

    data class TriangleCenterFactory(
        val error: TriangleCenterConstructionError,
    ) : JessieCodeCreatorError

    data class BisectorLinesFactory(
        val error: BisectorLinesError,
    ) : JessieCodeCreatorError

    data class CircleFactory(
        val error: CircleError,
    ) : JessieCodeCreatorError

    data class CurveFactory(
        val error: CurveError,
    ) : JessieCodeCreatorError

    data class EllipseFactory(
        val error: EllipseError,
    ) : JessieCodeCreatorError

    data class HyperbolaFactory(
        val error: HyperbolaError,
    ) : JessieCodeCreatorError

    data class ParabolaFactory(
        val error: ParabolaError,
    ) : JessieCodeCreatorError

    data class PolygonFactory(
        val error: PolygonError,
    ) : JessieCodeCreatorError

    data class ParallelogramFactory(
        val error: ParallelogramError,
    ) : JessieCodeCreatorError

    data class RegularPolygonFactory(
        val error: RegularPolygonError,
    ) : JessieCodeCreatorError

    data class TextFactory(
        val error: TextError,
    ) : JessieCodeCreatorError

    data class ArcFactory(
        val error: ArcError,
    ) : JessieCodeCreatorError

    data class SectorFactory(
        val error: SectorError,
    ) : JessieCodeCreatorError
}

/**
 * Native creator subset registered by JSXGraph 1.13.3 through
 * JXG.registerElement. Custom environment creators retain precedence.
 */
internal object NativeJessieCodeCreators {
    private val AXIS_DISTANCE_PATTERN = Regex(
        """^\s*([+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)\s*(%|fr|px)?""",
    )

    private val creators = mapOf(
        "transform" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTransform(board, parents, attributes, location)
        },
        "transform3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTransform3D(board, parents, attributes, location)
        },
        "view3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createView3D(board, parents, attributes, location)
        },
        "point" to JessieCodeCreator { board, parents, attributes, location ->
            createPoint(board, parents, attributes, location)
        },
        "point3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPoint3D(board, parents, attributes, location)
        },
        "line3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createLine3D(board, parents, attributes, location)
        },
        "intersectionline3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createIntersectionLine3D(
                board,
                parents,
                attributes,
                location,
            )
        },
        "axis3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createLine3D(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "axis3d",
            )
        },
        "axes3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createAxes3D(board, parents, attributes, location)
        },
        "ticks3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTicks3D(board, parents, attributes, location)
        },
        "text3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createText3D(board, parents, attributes, location)
        },
        "plane3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPlane3D(board, parents, attributes, location)
        },
        "mesh3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createMesh3D(board, parents, attributes, location)
        },
        "polyhedron3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPolyhedron3D(board, parents, attributes, location)
        },
        "polygon3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPolygon3D(board, parents, attributes, location)
        },
        "curve3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createCurve3D(board, parents, attributes, location)
        },
        "vectorfield3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createVectorField3D(board, parents, attributes, location)
        },
        "circle3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createCircle3D(board, parents, attributes, location)
        },
        "intersectioncircle3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createIntersectionCircle3D(
                board,
                parents,
                attributes,
                location,
            )
        },
        "sphere3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSphere3D(board, parents, attributes, location)
        },
        "parametricsurface3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSurface3D(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                functionGraph = false,
            )
        },
        "functiongraph3d" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSurface3D(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                functionGraph = true,
            )
        },
        "polepoint" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPolePoint(board, parents, attributes, location)
        },
        "intersection" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createIntersection(
                board,
                parents,
                attributes,
                location,
            )
        },
        "otherintersection" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createOtherIntersection(
                board,
                parents,
                attributes,
                location,
            )
        },
        "reflection" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPointReflection(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = PointReflectionCreatorKind.REFLECTION,
            )
        },
        "mirrorelement" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPointReflection(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = PointReflectionCreatorKind.MIRROR_ELEMENT,
            )
        },
        "mirrorpoint" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPointReflection(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = PointReflectionCreatorKind.MIRROR_POINT,
            )
        },
        "midpoint" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createMidpoint(board, parents, attributes, location)
        },
        "circumcenter" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createCircumcenter(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "circumcenter",
            )
        },
        "circumcirclemidpoint" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createCircumcenter(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "circumcirclemidpoint",
            )
        },
        "bisector" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createBisector(board, parents, attributes, location)
        },
        "bisectorlines" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createBisectorLines(board, parents, attributes, location)
        },
        "incenter" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createIncenter(board, parents, attributes, location)
        },
        "incircle" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createIncircle(board, parents, attributes, location)
        },
        "parallelpoint" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createParallelPoint(board, parents, attributes, location)
        },
        "parallel" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createParallel(board, parents, attributes, location)
        },
        "arrowparallel" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createParallel(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "arrowparallel",
                elementType = "arrowparallel",
            )
        },
        "orthogonalprojection" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createOrthogonalPoint(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = OrthogonalPointKind.ORTHOGONAL_PROJECTION,
            )
        },
        "perpendicularpoint" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createOrthogonalPoint(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = OrthogonalPointKind.PERPENDICULAR_POINT,
            )
        },
        "perpendicular" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPerpendicular(
                board,
                parents,
                attributes,
                location,
            )
        },
        "perpendicularsegment" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPerpendicularSegment(
                board,
                parents,
                attributes,
                location,
            )
        },
        "line" to JessieCodeCreator { board, parents, attributes, location ->
            createLine(board, parents, attributes, location)
        },
        "axis" to JessieCodeCreator { board, parents, attributes, location ->
            createAxis(board, parents, attributes, location)
        },
        "ticks" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTicks(board, parents, attributes, location)
        },
        "hatch" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createHatch(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "hatch",
            )
        },
        "hash" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createHatch(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "hash",
            )
        },
        "radicalaxis" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createRadicalAxis(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "tangent" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTangent(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "tangent",
                polarLine = false,
            )
        },
        "tangentto" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTangentTo(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "normal" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createNormal(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "polar" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTangent(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "polar",
                polarLine = false,
            )
        },
        "polarline" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTangent(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "polarline",
                polarLine = true,
            )
        },
        "arrow" to JessieCodeCreator { board, parents, attributes, location ->
            createLine(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "arrow",
                elementType = "arrow",
            )
        },
        "segment" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createLine(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "segment",
                supportsCoefficients = false,
                elementType = "segment",
            )
        },
        "circle" to JessieCodeCreator { board, parents, attributes, location ->
            createCircle(board, parents, attributes, location)
        },
        "circumcircle" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createCircumcircle(board, parents, attributes, location)
        },
        "ellipse" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createEllipse(board, parents, attributes, location)
        },
        "hyperbola" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createHyperbola(board, parents, attributes, location)
        },
        "parabola" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createParabola(board, parents, attributes, location)
        },
        "arc" to JessieCodeCreator { board, parents, attributes, location ->
            createArc(board, parents, attributes, location)
        },
        "semicircle" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createArc(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = ArcCreatorKind.SEMICIRCLE,
            )
        },
        "circumcirclearc" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createArc(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = ArcCreatorKind.CIRCUMCIRCLE_ARC,
            )
        },
        "minorarc" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createArc(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = ArcCreatorKind.MINOR_ARC,
            )
        },
        "majorarc" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createArc(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = ArcCreatorKind.MAJOR_ARC,
            )
        },
        "sector" to JessieCodeCreator { board, parents, attributes, location ->
            createSector(board, parents, attributes, location)
        },
        "circumcirclesector" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSector(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = SectorCreatorKind.CIRCUMCIRCLE_SECTOR,
            )
        },
        "minorsector" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSector(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = SectorCreatorKind.MINOR_SECTOR,
            )
        },
        "majorsector" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSector(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = SectorCreatorKind.MAJOR_SECTOR,
            )
        },
        "angle" to JessieCodeCreator { board, parents, attributes, location ->
            createAngle(board, parents, attributes, location)
        },
        "nonreflexangle" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSectorOrAngle(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = SectorCreatorKind.NONREFLEX_ANGLE,
            )
        },
        "reflexangle" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSectorOrAngle(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = SectorCreatorKind.REFLEX_ANGLE,
            )
        },
        "curve" to JessieCodeCreator { board, parents, attributes, location ->
            createCurve(board, parents, attributes, location)
        },
        "curveintersection" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createBooleanCurve(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "curveintersection",
                operation = ClipBooleanOperation.INTERSECTION,
            )
        },
        "curveunion" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createBooleanCurve(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "curveunion",
                operation = ClipBooleanOperation.UNION,
            )
        },
        "curvedifference" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createBooleanCurve(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                creatorName = "curvedifference",
                operation = ClipBooleanOperation.DIFFERENCE,
            )
        },
        "functiongraph" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createFunctionGraph(
                board,
                parents,
                attributes,
                location,
                creatorName = "functiongraph",
            )
        },
        "plot" to JessieCodeCreator { board, parents, attributes, location ->
            createFunctionGraph(
                board,
                parents,
                attributes,
                location,
                creatorName = "plot",
            )
        },
        "stepfunction" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createStepfunction(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "derivative" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createDerivative(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "spline" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSpline(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "cardinalspline" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createCardinalSpline(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "riemannsum" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createRiemannSum(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "boxplot" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createBoxPlot(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "comb" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createComb(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "inequality" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createInequality(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "vectorfield" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createVectorField(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "slopefield" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createSlopeField(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "polygon" to JessieCodeCreator { board, parents, attributes, location ->
            createPolygon(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = PolygonCreatorKind.POLYGON,
            )
        },
        "polygonalchain" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createPolygon(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
                kind = PolygonCreatorKind.POLYGONAL_CHAIN,
            )
        },
        "parallelogram" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createParallelogram(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "regularpolygon" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createRegularPolygon(
                board = board,
                parents = parents,
                attributes = attributes,
                location = location,
            )
        },
        "text" to JessieCodeCreator { board, parents, attributes, location ->
            createText(board, parents, attributes, location)
        },
    )

    internal val names: Set<String>
        get() = creators.keys

    internal fun creator(name: String): JessieCodeCreator? = creators[name]

    private fun createPointReflection(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        kind: PointReflectionCreatorKind,
    ): CreatorResult {
        val creatorName = kind.creatorName
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val source = resolveElement(resolvedBoard, parents[0]) as? Point
            ?: return unsupported(creatorName, parents, location)
        val reflector = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val result = when (kind) {
            PointReflectionCreatorKind.REFLECTION -> {
                val line = reflector as? Line
                    ?: return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                PointReflections.createReflection(
                    board = resolvedBoard,
                    source = source,
                    line = line,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = fixed,
                )
            }
            PointReflectionCreatorKind.MIRROR_ELEMENT -> {
                val mirror = reflector as? Point
                    ?: return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                PointReflections.createMirrorElement(
                    board = resolvedBoard,
                    source = source,
                    mirror = mirror,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = fixed,
                )
            }
            PointReflectionCreatorKind.MIRROR_POINT -> {
                val mirror = reflector as? Point
                    ?: return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                PointReflections.createMirrorPoint(
                    board = resolvedBoard,
                    source = source,
                    mirror = mirror,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = fixed,
                )
            }
        }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.PointReflectionFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun createPoint(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure("point", JessieCodeCreatorError.BoardUnavailable, location)
        val identity = when (
            val result = creatorAttributes("point", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = "point",
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (parents.size == 2) {
            val basePoint =
                resolveElement(resolvedBoard, parents[0]) as? CoordsElement
            val transformations =
                transformationReferences(parents[1])
            if (basePoint != null && transformations != null) {
                return when (
                    val result = Point.create(
                        board = resolvedBoard,
                        basePoint = basePoint,
                        transformations = transformations,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate =
                            identity.needsRegularUpdate,
                        fixed = fixed,
                    )
                ) {
                    is GMResult.Ok -> element(result.value)
                    is GMResult.Err -> failure(
                        creatorName = "point",
                        error = JessieCodeCreatorError.PointFactory(
                            result.error,
                        ),
                        location = location,
                    )
                }
            }
        }
        if (
            parents.any {
                it !is JessieCodeRuntimeValue.NumberValue &&
                    it !is JessieCodeRuntimeValue.StringValue &&
                    it !is JessieCodeRuntimeValue.FunctionValue
            }
        ) {
            return unsupported("point", parents, location)
        }
        val result = createPointFromCoordinates(
            board = resolvedBoard,
            coordinates = parents,
            attributes = identity.copy(fixed = fixed),
            coordinateLocation = location,
        )
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "point",
                error = JessieCodeCreatorError.PointFactory(result.error),
                location = location,
            )
        }
    }

    // JSXGraph: src/3d/view3d.js -> createView3D.
    private fun createView3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "view3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val lowerLeftCorner = numericArray(parents[0])
            ?: return unsupported(creatorName, parents, location)
        val size = numericArray(parents[1])
            ?: return unsupported(creatorName, parents, location)
        val boundingBoxValues = (
            parents[2] as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val boundingBox = mutableListOf<DoubleArray>()
        for (dimension in boundingBoxValues) {
            boundingBox += numericArray(dimension)
                ?: return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val projection = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "projection",
                default = "parallel",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val azimuth = when (
            val result = view3DAngleAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "az",
                default = View3D.DEFAULT_AZIMUTH,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val elevation = when (
            val result = view3DAngleAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "el",
                default = View3D.DEFAULT_ELEVATION,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val bank = when (
            val result = view3DAngleAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "bank",
                default = View3D.DEFAULT_BANK,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fieldOfView = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fov",
                default = View3D.DEFAULT_FIELD_OF_VIEW,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val cameraDistance = when (
            val value = attributes.properties["r"]
        ) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> null
            is JessieCodeRuntimeValue.StringValue ->
                if (value.value == "auto") {
                    null
                } else {
                    return failure(
                        creatorName = creatorName,
                        error =
                            JessieCodeCreatorError
                                .UnsupportedAttributeValue(
                                    attribute = "r",
                                    actual = value.value,
                                ),
                        location = location,
                    )
                }
            is JessieCodeRuntimeValue.NumberValue -> value.value
            else -> return invalidAttribute(
                creatorName = creatorName,
                attribute = "r",
                expected = "number or 'auto'",
                actual = value,
                location = location,
            )
        }
        val axesAttributes = when (
            val result = axes3DAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val view = when (
            val result = View3D.create(
                board = resolvedBoard,
                lowerLeftCorner = lowerLeftCorner,
                size = size,
                boundingBox = boundingBox.toTypedArray(),
                projection = projection,
                azimuth = azimuth,
                elevation = elevation,
                bank = bank,
                cameraDistance = cameraDistance,
                fieldOfView = fieldOfView,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.View3DFactory(
                    result.error,
                ),
                location = location,
            )
        }
        return when (
            val result = view.createDefaultAxes(
                axesPosition = axesAttributes.axesPosition,
                planeTypes = axesAttributes.planeTypes,
                planeSurfaceAttributes =
                    axesAttributes.planeSurfaceAttributes,
                ticksAttributes = axesAttributes.ticksAttributes,
                needsRegularUpdate =
                    axesAttributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(view)
            is GMResult.Err -> {
                resolvedBoard.removeObject(view)
                failure(
                    creatorName = creatorName,
                    error =
                        JessieCodeCreatorError
                            .View3DDefaultAxesFactory(result.error),
                    location = location,
                )
            }
        }
    }

    // JSXGraph: src/3d/point3d.js -> createPoint3D.
    private fun createPoint3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "point3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val pointParents = parents.drop(1)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (pointParents.size == 2) {
            val basePoint =
                resolveElement(resolvedBoard, pointParents[0]) as? Point3D
            val transformations =
                transformationReferences(pointParents[1])
            if (basePoint != null && transformations != null) {
                return point3DResult(
                    result = Point3D.create(
                        view = view,
                        basePoint = basePoint,
                        transformations = transformations,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate =
                            identity.needsRegularUpdate,
                        fixed = fixed,
                    ),
                    location = location,
                )
            }
        }
        if (
            pointParents.size == 1 &&
            pointParents[0] is JessieCodeRuntimeValue.FunctionValue
        ) {
            val function =
                pointParents[0] as JessieCodeRuntimeValue.FunctionValue
            return point3DResult(
                result = Point3D.create(
                    view = view,
                    coordinateSource =
                        Point3DCoordinateSource.Function(
                            point3DArrayEvaluator(
                                function = function,
                                location = location,
                            ),
                        ),
                    dependencies = function.dependencies.values,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = fixed,
                ),
                location = location,
            )
        }
        val coordinateValues =
            if (
                pointParents.size == 1 &&
                pointParents[0] is JessieCodeRuntimeValue.ArrayValue
            ) {
                (
                    pointParents[0] as JessieCodeRuntimeValue.ArrayValue
                    ).values
            } else {
                pointParents
            }
        if (coordinateValues.size !in setOf(3, 4)) {
            return unsupported(creatorName, parents, location)
        }
        val values = mutableListOf<Point3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (value in coordinateValues) {
            when (
                val result = point3DCoordinateValue(
                    value = value,
                    location = location,
                )
            ) {
                is GMResult.Ok -> {
                    values += result.value
                    if (value is JessieCodeRuntimeValue.FunctionValue) {
                        dependencies.putAll(value.dependencies)
                    }
                }
                is GMResult.Err -> return result
            }
        }
        return point3DResult(
            result = Point3D.create(
                view = view,
                coordinateSource =
                    Point3DCoordinateSource.Values(values),
                dependencies = dependencies.values,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            ),
            location = location,
        )
    }

    private fun view3DAngleAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Double,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> {
        val angle = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = name,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val slider = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = angle,
                name = "slider",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return numberAttribute(
            creatorName = creatorName,
            attributes = slider,
            name = "start",
            default = default,
            location = location,
        )
    }

    private fun point3DCoordinateValue(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
        creatorName: String = "point3d",
    ): GMResult<Point3DCoordinateValue, JessieCodeRuntimeError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(
                    Point3DCoordinateValue.Numeric(value.value),
                )
            is JessieCodeRuntimeValue.FunctionValue ->
                GMResult.Ok(
                    Point3DCoordinateValue.Dynamic(
                        Point3DScalarEvaluator {
                            when (
                                val result =
                                    value.externalCallable.call(
                                        arguments = emptyList(),
                                        location = location,
                                    )
                            ) {
                                is GMResult.Err -> GMResult.Err(
                                    Point3DDynamicError.Rejected(
                                        result.error.toString(),
                                    ),
                                )
                                is GMResult.Ok -> {
                                    val number = result.value as?
                                        JessieCodeRuntimeValue.NumberValue
                                    if (number != null) {
                                        GMResult.Ok(number.value)
                                    } else {
                                        GMResult.Err(
                                            Point3DDynamicError.Rejected(
                                                "Expected number, got " +
                                                    typeName(result.value),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                    ),
                )
            else -> invalidAttribute(
                creatorName = creatorName,
                attribute = "parents",
                expected = "numbers or zero-argument functions",
                actual = value,
                location = location,
            )
        }

    private fun point3DArrayEvaluator(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): Point3DArrayEvaluator =
        Point3DArrayEvaluator {
            when (
                val result = function.externalCallable.call(
                    arguments = emptyList(),
                    location = location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    Point3DDynamicError.Rejected(
                        result.error.toString(),
                    ),
                )
                is GMResult.Ok -> {
                    val values = (
                        result.value as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values
                    val coordinates = values?.let(::numericRuntimeArray)
                    if (coordinates != null) {
                        GMResult.Ok(coordinates)
                    } else {
                        GMResult.Err(
                            Point3DDynamicError.Rejected(
                                "Expected numeric coordinate array, got " +
                                    typeName(result.value),
                            ),
                        )
                    }
                }
            }
        }

    private fun point3DResult(
        result: GMResult<Point3D, Point3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "point3d",
                error = JessieCodeCreatorError.Point3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    // JSXGraph: src/3d/linspace3d.js -> createLine3D.
    private fun createLine3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String = "line3d",
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val lineParents = parents.drop(1)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightFirst = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightfirst",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightLast = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightlast",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (lineParents.size in 2..3) {
            val baseLine =
                resolveElement(resolvedBoard, lineParents[0]) as? Line3D
            val transformations =
                transformationReferences(lineParents[1])
            if (baseLine != null && transformations != null) {
                val range = if (lineParents.size == 3) {
                    when (
                        val result = line3DRange(
                            value = lineParents[2],
                            location = location,
                            creatorName = creatorName,
                        )
                    ) {
                        is GMResult.Ok -> result.value.values
                        is GMResult.Err -> return result
                    }
                } else {
                    null
                }
                return line3DResult(
                    result =
                        if (range == null) {
                            Line3D.create(
                                view = view,
                                baseLine = baseLine,
                                transformations = transformations,
                                id = identity.id,
                                name = identity.name,
                                needsRegularUpdate =
                                    identity.needsRegularUpdate,
                                fixed = fixed,
                            )
                        } else {
                            Line3D.create(
                                view = view,
                                baseLine = baseLine,
                                transformations = transformations,
                                rangeSource = range,
                                id = identity.id,
                                name = identity.name,
                                needsRegularUpdate =
                                    identity.needsRegularUpdate,
                                fixed = fixed,
                            )
                        },
                    location = location,
                    creatorName = creatorName,
                )
            }
        }

        val isTwoPointForm =
            lineParents.size == 2 &&
                (
                    resolveElement(
                        resolvedBoard,
                        lineParents[1],
                    ) is Point3D ||
                        lineParents[1] is JessieCodeRuntimeValue.ArrayValue ||
                        lineParents[1] is JessieCodeRuntimeValue.FunctionValue
                    )
        if (isTwoPointForm) {
            val first = when (
                val result = provideLine3DPoint(
                    board = resolvedBoard,
                    view = view,
                    value = lineParents[0],
                    attributes = attributes,
                    role = "point1",
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = provideLine3DPoint(
                    board = resolvedBoard,
                    view = view,
                    value = lineParents[1],
                    attributes = attributes,
                    role = "point2",
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    discardProvidedPoint3D(resolvedBoard, first)
                    return result
                }
            }
            return line3DResult(
                result = Line3D.create(
                    view = view,
                    point1 = first.point,
                    point2 = second.point,
                    ownsPoint1 = first.owned,
                    ownsPoint2 = second.owned,
                    straightFirst = straightFirst,
                    straightLast = straightLast,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = fixed,
                ),
                location = location,
                creatorName = creatorName,
            )
        }

        if (lineParents.size !in 2..3) {
            return unsupported(creatorName, parents, location)
        }
        val definingPoint = when (
            val result = provideLine3DPoint(
                board = resolvedBoard,
                view = view,
                value = lineParents[0],
                attributes = attributes,
                role = "point",
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction = when (
            val result = line3DDirection(
                board = resolvedBoard,
                value = lineParents[1],
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                discardProvidedPoint3D(resolvedBoard, definingPoint)
                return result
            }
        }
        val range = if (lineParents.size == 3) {
            when (
                val result = line3DRange(
                    value = lineParents[2],
                    location = location,
                            creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    discardProvidedPoint3D(resolvedBoard, definingPoint)
                    return result
                }
            }
        } else {
            ParsedLine3DValues(
                values = listOf(
                    Line3DCoordinateValue.Numeric(
                        Double.NEGATIVE_INFINITY,
                    ),
                    Line3DCoordinateValue.Numeric(
                        Double.POSITIVE_INFINITY,
                    ),
                ),
                dependencies = emptyList(),
            )
        }
        return line3DResult(
            result = Line3D.create(
                view = view,
                point = definingPoint.point,
                directionSource = direction.source,
                rangeSource = range.values,
                ownsPoint = definingPoint.owned,
                dependencies =
                    direction.dependencies + range.dependencies,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            ),
            location = location,
            creatorName = creatorName,
        )
    }

    private fun provideLine3DPoint(
        board: Board,
        view: View3D,
        value: JessieCodeRuntimeValue,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        role: String,
        location: JessieCodeAstLocation,
        creatorName: String = "line3d",
    ): GMResult<ProvidedLine3DPoint, JessieCodeRuntimeError> {
        val existing = resolveElement(board, value) as? Point3D
        if (existing != null) {
            return GMResult.Ok(
                ProvidedLine3DPoint(
                    point = existing,
                    owned = false,
                ),
            )
        }
        val identity = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = role,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointResult = when (value) {
            is JessieCodeRuntimeValue.FunctionValue ->
                Point3D.create(
                    view = view,
                    coordinateSource =
                        Point3DCoordinateSource.Function(
                            point3DArrayEvaluator(
                                function = value,
                                location = location,
                            ),
                        ),
                    dependencies = value.dependencies.values,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = identity.fixed,
                )
            is JessieCodeRuntimeValue.ArrayValue -> {
                if (value.values.size !in setOf(3, 4)) {
                    return invalidAttribute(
                        creatorName = creatorName,
                        attribute = role,
                        expected = "Point3D or 3D/4D coordinate array",
                        actual = value,
                        location = location,
                    )
                }
                val coordinates = mutableListOf<Point3DCoordinateValue>()
                val dependencies = linkedMapOf<String, GeometryElement>()
                for (coordinate in value.values) {
                    when (
                        val result = point3DCoordinateValue(
                            value = coordinate,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> coordinates += result.value
                        is GMResult.Err -> return result
                    }
                    if (
                        coordinate is
                            JessieCodeRuntimeValue.FunctionValue
                    ) {
                        dependencies.putAll(coordinate.dependencies)
                    }
                }
                Point3D.create(
                    view = view,
                    coordinateSource =
                        Point3DCoordinateSource.Values(coordinates),
                    dependencies = dependencies.values,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = identity.fixed,
                )
            }
            else -> return invalidAttribute(
                creatorName = creatorName,
                attribute = role,
                expected = "Point3D, coordinate array, or function",
                actual = value,
                location = location,
            )
        }
        return when (pointResult) {
            is GMResult.Ok -> GMResult.Ok(
                ProvidedLine3DPoint(
                    point = pointResult.value,
                    owned = true,
                ),
            )
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Point3DFactory(
                    pointResult.error,
                ),
                location = location,
            )
        }
    }

    // JSXGraph: src/3d/box3d.js -> createAxes3D.
    private fun createAxes3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "axes3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 1) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val axesAttributes = when (
            val result = axes3DAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Axes3D.create(
                view = view,
                axesPosition = axesAttributes.axesPosition,
                planeTypes = axesAttributes.planeTypes,
                planeSurfaceAttributes =
                    axesAttributes.planeSurfaceAttributes,
                ticksAttributes = axesAttributes.ticksAttributes,
                needsRegularUpdate =
                    axesAttributes.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.CompositionReference(result.value),
            )
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Axes3DFactory(result.error),
                location = location,
            )
        }
    }

    private fun axes3DAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedAxes3DAttributes, JessieCodeRuntimeError> {
        val axesPosition = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "axesposition",
                default = "center",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val planeTypes = linkedMapOf<String, String>()
        val planeSurfaceAttributes =
            linkedMapOf<String, Plane3DSurfaceAttributes>()
        for (role in AXES_3D_PLANE_ROLES) {
            val nested = when (
                val result = nestedObjectAttribute(
                    creatorName = creatorName,
                    attributes = attributes,
                    name = role.lowercase(),
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val defaultType =
                if (role.endsWith("Front")) "wireframe" else "shader"
            val planeType = when (
                val result = stringAttribute(
                    creatorName = creatorName,
                    attributes = nested,
                    name = "type",
                    default = defaultType,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value.lowercase()
                is GMResult.Err -> return result
            }
            planeTypes[role] = planeType
            if (planeType != "wireframe") {
                val defaults = Plane3DSurfaceAttributes.axes3DDefaults(
                    visible = role.endsWith("Rear"),
                )
                planeSurfaceAttributes[role] = when (
                    val result = plane3DSurfaceAttributes(
                        attributes = nested,
                        location = location,
                        defaults = defaults,
                        planeVisibleDefault =
                            defaults.faceAttributes.visible,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        }
        val ticksAttributes = linkedMapOf<String, Axes3DTicksAttributes>()
        for (direction in listOf("x", "y", "z")) {
            val axisRole = "${direction}AxisBorder"
            val axis = when (
                val result = nestedObjectAttribute(
                    creatorName = creatorName,
                    attributes = attributes,
                    name = axisRole.lowercase(),
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val ticks = when (
                val result = nestedObjectAttribute(
                    creatorName = creatorName,
                    attributes = axis,
                    name = "ticks3d",
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val ticksDistance = when (
                val result = numberAttribute(
                    creatorName = creatorName,
                    attributes = ticks,
                    name = "ticksdistance",
                    default = 1.0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val majorHeight = when (
                val result = numberAttribute(
                    creatorName = creatorName,
                    attributes = ticks,
                    name = "majorheight",
                    default = 10.0,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val drawLabels = when (
                val result = booleanAttribute(
                    creatorName = creatorName,
                    attributes = ticks,
                    name = "drawlabels",
                    default = true,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val tickEndingsValue = ticks.properties["tickendings"]
            val tickEndings =
                if (
                    tickEndingsValue == null ||
                    tickEndingsValue ===
                    JessieCodeRuntimeValue.UndefinedValue
                ) {
                    doubleArrayOf(0.0, 1.0)
                } else {
                    numericArray(tickEndingsValue)
                        ?: return invalidAttribute(
                            creatorName = creatorName,
                            attribute =
                                "${axisRole.lowercase()}." +
                                    "ticks3d.tickendings",
                            expected = "array of two numbers",
                            actual = tickEndingsValue,
                            location = location,
                        )
                }
            ticksAttributes["${axisRole}Ticks"] =
                Axes3DTicksAttributes(
                    ticksDistance = ticksDistance,
                    tickEndings = tickEndings,
                    majorHeight = majorHeight,
                    drawLabels = drawLabels,
                )
        }
        val needsRegularUpdate = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "needsregularupdate",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedAxes3DAttributes(
                axesPosition = axesPosition,
                planeTypes = planeTypes,
                planeSurfaceAttributes = planeSurfaceAttributes,
                ticksAttributes = ticksAttributes,
                needsRegularUpdate = needsRegularUpdate,
            )
        )
    }

    // JSXGraph: src/3d/ticks3d.js -> createTicks3D.
    private fun createTicks3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "ticks3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 5) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val point = when (
            val result = ticks3DPointSource(
                value = parents[1],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction1 = when (
            val result = ticks3DValues(
                value = parents[2],
                attribute = "direction1",
                count = 3,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val length = (
            parents[3] as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return invalidAttribute(
            creatorName = creatorName,
            attribute = "length",
            expected = "number",
            actual = parents[3],
            location = location,
        )
        val direction2 = when (
            val result = ticks3DValues(
                value = parents[4],
                attribute = "direction2",
                count = 3,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val ticksDistance = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "ticksdistance",
                default = 1.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val majorHeight = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "majorheight",
                default = 10.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val tickEndingsValue = attributes.properties["tickendings"]
        val tickEndings =
            if (
                tickEndingsValue == null ||
                tickEndingsValue === JessieCodeRuntimeValue.UndefinedValue
            ) {
                doubleArrayOf(0.0, 1.0)
            } else {
                numericArray(tickEndingsValue)
                    ?: return invalidAttribute(
                        creatorName = creatorName,
                        attribute = "tickendings",
                        expected = "array of two numbers",
                        actual = tickEndingsValue,
                        location = location,
                    )
            }
        val drawLabels = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "drawlabels",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Ticks3D.create(
                view = view,
                pointSource = point.source,
                direction1 = direction1.values,
                length = length,
                direction2 = direction2.values,
                ticksDistance = ticksDistance,
                tickEndings = tickEndings,
                majorHeight = majorHeight,
                drawLabels = drawLabels,
                dependencies =
                    point.dependencies +
                        direction1.dependencies +
                        direction2.dependencies,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Ticks3DFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun ticks3DPointSource(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedTicks3DPoint, JessieCodeRuntimeError> {
        if (value is JessieCodeRuntimeValue.FunctionValue) {
            return GMResult.Ok(
                ParsedTicks3DPoint(
                    source = Ticks3DPointSource.Function(
                        line3DArrayEvaluator(value, location),
                    ),
                    dependencies = value.dependencies.values.toList(),
                ),
            )
        }
        return when (
            val result = ticks3DValues(
                value = value,
                attribute = "point",
                count = 3,
                location = location,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                ParsedTicks3DPoint(
                    source = Ticks3DPointSource.Values(
                        result.value.values,
                    ),
                    dependencies = result.value.dependencies,
                ),
            )
            is GMResult.Err -> result
        }
    }

    private fun ticks3DValues(
        value: JessieCodeRuntimeValue,
        attribute: String,
        count: Int,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedLine3DValues, JessieCodeRuntimeError> {
        val creatorName = "ticks3d"
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = creatorName,
                attribute = attribute,
                expected = "array of $count values",
                actual = value,
                location = location,
            )
        if (array.values.size != count) {
            return invalidAttribute(
                creatorName = creatorName,
                attribute = attribute,
                expected = "array of $count values",
                actual = value,
                location = location,
            )
        }
        val values = mutableListOf<Line3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (coordinate in array.values) {
            when (
                val result = line3DCoordinateValue(
                    value = coordinate,
                    attribute = attribute,
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
            if (coordinate is JessieCodeRuntimeValue.FunctionValue) {
                dependencies.putAll(coordinate.dependencies)
            }
        }
        return GMResult.Ok(
            ParsedLine3DValues(
                values = values,
                dependencies = dependencies.values.toList(),
            ),
        )
    }

    // JSXGraph: src/3d/text3d.js -> createText3D.
    private fun createText3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "text3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val coordinateValues: List<JessieCodeRuntimeValue>
        val contentValue: JessieCodeRuntimeValue
        when (parents.size) {
            3 -> {
                val coordinateParent = parents[1]
                if (coordinateParent is JessieCodeRuntimeValue.FunctionValue) {
                    val identity = when (
                        val result = creatorAttributes(
                            creatorName,
                            attributes,
                            location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    return createText3DResult(
                        view = view,
                        coordinateSource =
                            Point3DCoordinateSource.Function(
                                point3DArrayEvaluator(
                                    coordinateParent,
                                    location,
                                ),
                            ),
                        dependencies =
                            coordinateParent.dependencies.values,
                        contentValue = parents[2],
                        identity = identity,
                        location = location,
                    )
                }
                coordinateValues = (
                    coordinateParent as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values ?: return unsupported(
                    creatorName,
                    parents,
                    location,
                )
                contentValue = parents[2]
            }
            5 -> {
                coordinateValues = parents.subList(1, 4)
                contentValue = parents[4]
            }
            else -> return unsupported(creatorName, parents, location)
        }
        if (coordinateValues.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val coordinates = mutableListOf<Point3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (value in coordinateValues) {
            when (
                val result = point3DCoordinateValue(
                    value = value,
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> coordinates += result.value
                is GMResult.Err -> return result
            }
            if (value is JessieCodeRuntimeValue.FunctionValue) {
                dependencies.putAll(value.dependencies)
            }
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return createText3DResult(
            view = view,
            coordinateSource =
                Point3DCoordinateSource.Values(coordinates),
            dependencies = dependencies.values,
            contentValue = contentValue,
            identity = identity,
            location = location,
        )
    }

    private fun createText3DResult(
        view: View3D,
        coordinateSource: Point3DCoordinateSource,
        dependencies: Iterable<GeometryElement>,
        contentValue: JessieCodeRuntimeValue,
        identity: CreatorAttributes,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val content = when (contentValue) {
            is JessieCodeRuntimeValue.StringValue -> contentValue.value
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(contentValue.value)
            else -> return invalidAttribute(
                creatorName = "text3d",
                attribute = "text",
                expected = "string or number",
                actual = contentValue,
                location = location,
            )
        }
        return when (
            val result = Text3D.create(
                view = view,
                coordinateSource = coordinateSource,
                content = content,
                dependencies = dependencies,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "text3d",
                error = JessieCodeCreatorError.Text3DFactory(result.error),
                location = location,
            )
        }
    }

    private fun line3DDirection(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
        creatorName: String = "line3d",
    ): GMResult<ParsedLine3DDirection, JessieCodeRuntimeError> {
        val line = resolveElement(board, value) as? Line3D
        if (line != null) {
            return GMResult.Ok(
                ParsedLine3DDirection(
                    source = Line3DDirectionSource.Line(line),
                    dependencies = listOf(line),
                ),
            )
        }
        if (value is JessieCodeRuntimeValue.FunctionValue) {
            return GMResult.Ok(
                ParsedLine3DDirection(
                    source = Line3DDirectionSource.Function(
                        line3DArrayEvaluator(
                            function = value,
                            location = location,
                        ),
                    ),
                    dependencies = value.dependencies.values.toList(),
                ),
            )
        }
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = creatorName,
                attribute = "direction",
                expected = "Line3D, function, or 3D/4D vector",
                actual = value,
                location = location,
            )
        if (array.values.size !in setOf(3, 4)) {
            return invalidAttribute(
                creatorName = creatorName,
                attribute = "direction",
                expected = "array of three or four values",
                actual = value,
                location = location,
            )
        }
        val values = mutableListOf<Line3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (coordinate in array.values) {
            when (
                val result = line3DCoordinateValue(
                    value = coordinate,
                    attribute = "direction",
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
            if (coordinate is JessieCodeRuntimeValue.FunctionValue) {
                dependencies.putAll(coordinate.dependencies)
            }
        }
        return GMResult.Ok(
            ParsedLine3DDirection(
                source = Line3DDirectionSource.Values(values),
                dependencies = dependencies.values.toList(),
            ),
        )
    }

    private fun line3DRange(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
        creatorName: String = "line3d",
    ): GMResult<ParsedLine3DValues, JessieCodeRuntimeError> {
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = creatorName,
                attribute = "range",
                expected = "array of two values",
                actual = value,
                location = location,
            )
        if (array.values.size != 2) {
            return invalidAttribute(
                creatorName = creatorName,
                attribute = "range",
                expected = "array of two values",
                actual = value,
                location = location,
            )
        }
        val values = mutableListOf<Line3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (rangeValue in array.values) {
            when (
                val result = line3DCoordinateValue(
                    value = rangeValue,
                    attribute = "range",
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
            if (rangeValue is JessieCodeRuntimeValue.FunctionValue) {
                dependencies.putAll(rangeValue.dependencies)
            }
        }
        return GMResult.Ok(
            ParsedLine3DValues(
                values = values,
                dependencies = dependencies.values.toList(),
            ),
        )
    }

    private fun line3DCoordinateValue(
        value: JessieCodeRuntimeValue,
        attribute: String,
        location: JessieCodeAstLocation,
        creatorName: String = "line3d",
    ): GMResult<Line3DCoordinateValue, JessieCodeRuntimeError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(
                    Line3DCoordinateValue.Numeric(value.value),
                )
            is JessieCodeRuntimeValue.FunctionValue ->
                GMResult.Ok(
                    Line3DCoordinateValue.Dynamic(
                        Line3DScalarEvaluator {
                            when (
                                val result =
                                    value.externalCallable.call(
                                        arguments = emptyList(),
                                        location = location,
                                    )
                            ) {
                                is GMResult.Err -> GMResult.Err(
                                    Line3DDynamicError.Rejected(
                                        result.error.toString(),
                                    ),
                                )
                                is GMResult.Ok -> {
                                    val number = result.value as?
                                        JessieCodeRuntimeValue.NumberValue
                                    if (number != null) {
                                        GMResult.Ok(number.value)
                                    } else {
                                        GMResult.Err(
                                            Line3DDynamicError.Rejected(
                                                "Expected number, got " +
                                                    typeName(result.value),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                    ),
                )
            else -> invalidAttribute(
                creatorName = creatorName,
                attribute = attribute,
                expected = "numbers or zero-argument functions",
                actual = value,
                location = location,
            )
        }

    private fun line3DArrayEvaluator(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): Line3DArrayEvaluator =
        Line3DArrayEvaluator {
            when (
                val result = function.externalCallable.call(
                    arguments = emptyList(),
                    location = location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    Line3DDynamicError.Rejected(
                        result.error.toString(),
                    ),
                )
                is GMResult.Ok -> {
                    val values = (
                        result.value as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values
                    val coordinates = values?.let(::numericRuntimeArray)
                    if (coordinates != null) {
                        GMResult.Ok(coordinates)
                    } else {
                        GMResult.Err(
                            Line3DDynamicError.Rejected(
                                "Expected numeric direction array, got " +
                                    typeName(result.value),
                            ),
                        )
                    }
                }
            }
        }

    private fun line3DResult(
        result: GMResult<Line3D, Line3DError>,
        location: JessieCodeAstLocation,
        creatorName: String = "line3d",
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Line3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    private fun discardProvidedPoint3D(
        board: Board,
        point: ProvidedLine3DPoint,
    ) {
        if (point.owned) {
            board.removeObject(point.point)
        }
    }

    // JSXGraph: src/3d/linspace3d.js -> createIntersectionLine3D.
    private fun createIntersectionLine3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "intersectionline3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val first = resolveElement(
            resolvedBoard,
            parents[1],
        ) as? GeometryElement3D
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(
            resolvedBoard,
            parents[2],
        ) as? GeometryElement3D
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Attributes = when (
            val result = intersectionLine3DPointAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "point1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Attributes = when (
            val result = intersectionLine3DPointAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "point2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return intersectionLine3DResult(
            result = IntersectionLine3D.create(
                view = view,
                first = first,
                second = second,
                point1Attributes = point1Attributes,
                point2Attributes = point2Attributes,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            ),
            location = location,
        )
    }

    private fun intersectionLine3DPointAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<
        IntersectionLine3DPointAttributes,
        JessieCodeRuntimeError,
        > =
        when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = name,
                location = location,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                IntersectionLine3DPointAttributes(
                    id = result.value.id,
                    name = result.value.name,
                    needsRegularUpdate =
                        result.value.needsRegularUpdate,
                    fixed = result.value.fixed,
                ),
            )
            is GMResult.Err -> result
        }

    private fun intersectionLine3DResult(
        result: GMResult<Line3D, IntersectionLine3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "intersectionline3d",
                error =
                    JessieCodeCreatorError.IntersectionLine3DFactory(
                        result.error,
                    ),
                location = location,
            )
        }

    // JSXGraph: src/3d/polygon3d.js -> createPolygon3D.
    private fun createPolygon3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "polygon3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val nestedVertices = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "vertices",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val vertexIdentity = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "vertices",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val vertexWithLabel = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = nestedVertices,
                name = "withlabel",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val vertexAttributes = Polygon3DVertexAttributes(
            id = vertexIdentity.id,
            name = vertexIdentity.name,
            needsRegularUpdate = vertexIdentity.needsRegularUpdate,
            fixed = vertexIdentity.fixed,
            withLabel = vertexWithLabel,
        )
        val withLines = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "withlines",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (parents.size == 3) {
            val base = resolveElement(resolvedBoard, parents[1])
                as? Polygon3D
            val transformations = transformationReferences(parents[2])
            if (base != null && transformations != null) {
                return polygon3DResult(
                    result = Polygon3D.create(
                        view = view,
                        base = base,
                        transformations = transformations,
                        vertexAttributes = vertexAttributes,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                        withLines = withLines,
                    ),
                    location = location,
                )
            }
        }
        if (parents.size < 2) {
            return unsupported(creatorName, parents, location)
        }

        val vertexValues = polygon3DVertexValues(
            board = resolvedBoard,
            values = parents.drop(1),
        )
        val providedVertices = mutableListOf<ProvidedLine3DPoint>()
        for (value in vertexValues) {
            val provided = when (
                val result = provideLine3DPoint(
                    board = resolvedBoard,
                    view = view,
                    value = value,
                    attributes = attributes,
                    role = "vertices",
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    for (vertex in providedVertices.asReversed()) {
                        discardProvidedPoint3D(resolvedBoard, vertex)
                    }
                    return result
                }
            }
            providedVertices += provided
        }
        return polygon3DResult(
            result = Polygon3D.create(
                view = view,
                vertices = providedVertices.map(ProvidedLine3DPoint::point),
                ownedVertices = providedVertices
                    .filter(ProvidedLine3DPoint::owned)
                    .mapTo(linkedSetOf(), ProvidedLine3DPoint::point),
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                withLines = withLines,
            ),
            location = location,
        )
    }

    private fun polygon3DVertexValues(
        board: Board,
        values: List<JessieCodeRuntimeValue>,
    ): List<JessieCodeRuntimeValue> {
        if (values.size != 1) {
            return values
        }
        val nested = values[0] as? JessieCodeRuntimeValue.ArrayValue
            ?: return values
        if (nested.values.isEmpty()) {
            return values
        }
        val isPointList = nested.values.all { value ->
            resolveElement(board, value) is Point3D
        }
        val isCoordinateList = nested.values.all { value ->
            val coordinates = value as? JessieCodeRuntimeValue.ArrayValue
                ?: return@all false
            coordinates.values.firstOrNull() is
                JessieCodeRuntimeValue.NumberValue
        }
        return if (isPointList || isCoordinateList) {
            nested.values
        } else {
            values
        }
    }

    private fun polygon3DResult(
        result: GMResult<Polygon3D, Polygon3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "polygon3d",
                error = JessieCodeCreatorError.Polygon3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    // JSXGraph: src/3d/curve3d.js -> createCurve3D.
    private fun createCurve3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "curve3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = integerAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "numberpointshigh",
                default = Curve3D.DEFAULT_SAMPLE_COUNT,
                minimum = 1,
                maximum = Curve3D.MAX_SAMPLE_COUNT,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (parents.size == 3) {
            val base = resolveElement(resolvedBoard, parents[1])
                as? Curve3D
            val transformations = transformationReferences(parents[2])
            if (base != null && transformations != null) {
                return curve3DResult(
                    result = Curve3D.create(
                        view = view,
                        baseCurve = base,
                        transformations = transformations,
                        sampleCount = sampleCount,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                    ),
                    location = location,
                )
            }
        }

        if (parents.size == 2) {
            val matrix = parents[1] as?
                JessieCodeRuntimeValue.ArrayValue
                ?: return unsupported(creatorName, parents, location)
            val coordinates = mutableListOf<DoubleArray>()
            for (row in matrix.values) {
                val values = numericCurve3DArray(row)
                    ?: return unsupported(creatorName, parents, location)
                if (values.size != 3) {
                    return unsupported(creatorName, parents, location)
                }
                coordinates += values
            }
            return curve3DResult(
                result = Curve3D.create(
                    view = view,
                    source = Curve3DSource.Arrays(
                        x = DoubleArray(coordinates.size) {
                            coordinates[it][0]
                        },
                        y = DoubleArray(coordinates.size) {
                            coordinates[it][1]
                        },
                        z = DoubleArray(coordinates.size) {
                            coordinates[it][2]
                        },
                    ),
                    sampleCount = sampleCount,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                ),
                location = location,
            )
        }

        if (parents.size == 3) {
            val function = parents[1] as?
                JessieCodeRuntimeValue.FunctionValue
                ?: return unsupported(creatorName, parents, location)
            val range = when (
                val result = line3DRange(
                    value = parents[2],
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return curve3DResult(
                result = Curve3D.create(
                    view = view,
                    source = Curve3DSource.Function(
                        curve3DArrayEvaluator(function, location),
                    ),
                    rangeSource = range.values,
                    sampleCount = sampleCount,
                    dependencies = range.dependencies,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                ),
                location = location,
            )
        }

        if (parents.size == 5) {
            val arrays = parents.subList(1, 4)
                .map(::numericCurve3DArray)
            if (arrays.all { it != null }) {
                val range = when (
                    val result = line3DRange(
                        value = parents[4],
                        location = location,
                        creatorName = creatorName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                return curve3DResult(
                    result = Curve3D.create(
                        view = view,
                        source = Curve3DSource.Arrays(
                            x = arrays[0] ?: DoubleArray(0),
                            y = arrays[1] ?: DoubleArray(0),
                            z = arrays[2] ?: DoubleArray(0),
                        ),
                        rangeSource = range.values,
                        sampleCount = sampleCount,
                        dependencies = range.dependencies,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                    ),
                    location = location,
                )
            }
            val functions = parents.subList(1, 4).map {
                it as? JessieCodeRuntimeValue.FunctionValue
            }
            if (functions.all { it != null }) {
                val range = when (
                    val result = line3DRange(
                        value = parents[4],
                        location = location,
                        creatorName = creatorName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                return curve3DResult(
                    result = Curve3D.create(
                        view = view,
                        source = Curve3DSource.Components(
                            x = curve3DScalarEvaluator(
                                functions[0] ?: return unsupported(
                                    creatorName,
                                    parents,
                                    location,
                                ),
                                location,
                            ),
                            y = curve3DScalarEvaluator(
                                functions[1] ?: return unsupported(
                                    creatorName,
                                    parents,
                                    location,
                                ),
                                location,
                            ),
                            z = curve3DScalarEvaluator(
                                functions[2] ?: return unsupported(
                                    creatorName,
                                    parents,
                                    location,
                                ),
                                location,
                            ),
                        ),
                        rangeSource = range.values,
                        sampleCount = sampleCount,
                        dependencies = range.dependencies,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                    ),
                    location = location,
                )
            }
        }
        return unsupported(creatorName, parents, location)
    }

    // JSXGraph 1.13.3:
    // src/3d/curve3d.js -> createVectorfield3D.
    private fun createVectorField3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "vectorfield3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 5) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0])
            as? View3D
            ?: return unsupported(creatorName, parents, location)
        val field = when (
            val result = vectorField3DFunction(
                board = resolvedBoard,
                value = parents[1],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return vectorField3DFailure(
                error = result.error,
                location = location,
            )
        }
        val meshes = mutableListOf<List<JessieCodeCoordinateFunction>>()
        for ((index, axis) in listOf("x", "y", "z").withIndex()) {
            when (
                val result = vectorFieldMesh(
                    board = resolvedBoard,
                    value = parents[index + 2],
                    termName = "$creatorName.${axis}Data",
                    location = location,
                )
            ) {
                is GMResult.Ok -> meshes += result.value
                is GMResult.Err -> return vectorField3DFailure(
                    error = result.error,
                    location = location,
                )
            }
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val scaleTerm = when (
            val result = numericAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "scale",
                default = Curve3D.VECTOR_FIELD_DEFAULT_SCALE,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowHead = when (
            val value = attributes.properties["arrowhead"]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue ->
                JessieCodeRuntimeValue.ObjectValue(emptyMap())
            is JessieCodeRuntimeValue.ObjectValue -> value
            else -> return invalidAttribute(
                creatorName = creatorName,
                attribute = "arrowhead",
                expected = "object",
                actual = value,
                location = location,
            )
        }
        val arrowEnabledTerm = when (
            val result = booleanAttributeTerm(
                creatorName = creatorName,
                attributes = arrowHead,
                name = "enabled",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowSizeTerm = when (
            val result = numericAttributeTerm(
                creatorName = creatorName,
                attributes = arrowHead,
                name = "size",
                default = Curve3D.VECTOR_FIELD_DEFAULT_ARROW_SIZE,
                location = location,
                attributePrefix = "arrowhead.",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowAngleTerm = when (
            val result = numericAttributeTerm(
                creatorName = creatorName,
                attributes = arrowHead,
                name = "angle",
                default = Curve3D.VECTOR_FIELD_DEFAULT_ARROW_ANGLE,
                location = location,
                attributePrefix = "arrowhead.",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curve3DResult(
            result = Curve3D.createVectorField(
                view = view,
                field = field,
                xData = meshes[0],
                yData = meshes[1],
                zData = meshes[2],
                scaleTerm = scaleTerm,
                arrowEnabledTerm = arrowEnabledTerm,
                arrowSizeTerm = arrowSizeTerm,
                arrowAngleTerm = arrowAngleTerm,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
            location = location,
            creatorName = creatorName,
        )
    }

    private fun numericCurve3DArray(
        value: JessieCodeRuntimeValue,
    ): DoubleArray? {
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return null
        val numbers = array.values.map {
            (it as? JessieCodeRuntimeValue.NumberValue)?.value
                ?: return null
        }
        return numbers.toDoubleArray()
    }

    private fun curve3DArrayEvaluator(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): Curve3DArrayEvaluator =
        Curve3DArrayEvaluator { parameter ->
            when (
                val result = function.externalCallable.call(
                    arguments = listOf(
                        JessieCodeRuntimeValue.NumberValue(parameter),
                    ),
                    location = location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    Curve3DDynamicError.Rejected(
                        result.error.toString(),
                    ),
                )
                is GMResult.Ok -> {
                    val values = (
                        result.value as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values
                    val coordinates = values?.map {
                        (it as? JessieCodeRuntimeValue.NumberValue)?.value
                            ?: return@Curve3DArrayEvaluator GMResult.Err(
                                Curve3DDynamicError.Rejected(
                                    "Expected numeric coordinate array, got " +
                                        typeName(result.value),
                                ),
                            )
                    }
                    if (coordinates == null) {
                        GMResult.Err(
                            Curve3DDynamicError.Rejected(
                                "Expected numeric coordinate array, got " +
                                    typeName(result.value),
                            ),
                        )
                    } else {
                        GMResult.Ok(coordinates.toDoubleArray())
                    }
                }
            }
        }

    private fun curve3DScalarEvaluator(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): Curve3DScalarEvaluator =
        Curve3DScalarEvaluator { parameter ->
            when (
                val result = function.externalCallable.call(
                    arguments = listOf(
                        JessieCodeRuntimeValue.NumberValue(parameter),
                    ),
                    location = location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    Curve3DDynamicError.Rejected(
                        result.error.toString(),
                    ),
                )
                is GMResult.Ok -> {
                    val number = result.value as?
                        JessieCodeRuntimeValue.NumberValue
                    if (number == null) {
                        GMResult.Err(
                            Curve3DDynamicError.Rejected(
                                "Expected number, got " +
                                    typeName(result.value),
                            ),
                        )
                    } else {
                        GMResult.Ok(number.value)
                    }
                }
            }
        }

    private fun curve3DResult(
        result: GMResult<Curve3D, Curve3DError>,
        location: JessieCodeAstLocation,
        creatorName: String = "curve3d",
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Curve3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    private fun vectorField3DFailure(
        error: CurveError,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        failure(
            creatorName = "vectorfield3d",
            error = JessieCodeCreatorError.Curve3DFactory(
                Curve3DError.VectorField(error),
            ),
            location = location,
        )

    // JSXGraph: src/3d/surface3d.js ->
    // createParametricSurface3D / createFunctiongraph3D.
    private fun createSurface3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        functionGraph: Boolean,
    ): CreatorResult {
        val creatorName =
            if (functionGraph) "functiongraph3d" else "parametricsurface3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val surfaceType = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "type",
                default = "wireframe",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value.lowercase()
            is GMResult.Err -> return result
        }
        val tiling = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "tiling",
                default = "rectangle",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value.lowercase()
            is GMResult.Err -> return result
        }
        val surfaceAttributes = when (
            val result = plane3DSurfaceAttributes(
                attributes = attributes,
                location = location,
                defaults = Plane3DSurfaceAttributes(
                    stepsU = Surface3D.DEFAULT_STEPS_U,
                    stepsV = Surface3D.DEFAULT_STEPS_V,
                ),
                creatorName = creatorName,
                minimumStepsU = if (surfaceType == "wireframe") 0 else 1,
                minimumStepsV =
                    if (
                        surfaceType == "wireframe" ||
                        tiling == "triangle"
                    ) {
                        0
                    } else {
                        1
                    },
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }.let {
            Surface3DAttributes(
                surfaceType = surfaceType,
                tiling = it.tiling,
                stepsU = it.stepsU,
                stepsV = it.stepsV,
                fillColorArray = it.fillColorArray,
                faceAttributes = it.faceAttributes,
                colormap = it.colormap,
            )
        }

        if (!functionGraph && parents.size == 3) {
            val base = resolveElement(resolvedBoard, parents[1])
                as? Surface3D
            val transformations = transformationReferences(parents[2])
            if (base != null && transformations != null) {
                return surface3DResult(
                    creatorName = creatorName,
                    result = Surface3D.create(
                        view = view,
                        baseSurface = base,
                        transformations = transformations,
                        attributes = surfaceAttributes,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                    ),
                    location = location,
                )
            }
        }

        val expectedCount = if (functionGraph) 4 else null
        if (expectedCount != null && parents.size != expectedCount) {
            return unsupported(creatorName, parents, location)
        }
        val rangeIndexes = when {
            functionGraph -> 2 to 3
            parents.size == 4 -> 2 to 3
            parents.size == 6 -> 4 to 5
            else -> return unsupported(creatorName, parents, location)
        }
        val rangeU = when (
            val result = line3DRange(
                value = parents[rangeIndexes.first],
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeV = when (
            val result = line3DRange(
                value = parents[rangeIndexes.second],
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val sourceAndDependencies = when {
            functionGraph -> {
                val z = when (
                    val result = surface3DScalarEvaluator(
                        board = resolvedBoard,
                        value = parents[1],
                        variableNames = listOf("x", "y"),
                        location = location,
                        creatorName = creatorName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                Surface3DSource.Components(
                    x = Surface3DScalarEvaluator { u, _ ->
                        GMResult.Ok(u)
                    },
                    y = Surface3DScalarEvaluator { _, v ->
                        GMResult.Ok(v)
                    },
                    z = z.evaluator,
                ) to z.dependencies
            }
            parents.size == 4 -> {
                val function = parents[1] as?
                    JessieCodeRuntimeValue.FunctionValue
                    ?: return unsupported(creatorName, parents, location)
                Surface3DSource.Function(
                    surface3DArrayEvaluator(function, location),
                ) to function.dependencies.values.toList()
            }
            else -> {
                val parsed = mutableListOf<ParsedSurface3DScalarEvaluator>()
                for (value in parents.subList(1, 4)) {
                    val evaluator = when (
                        val result = surface3DScalarEvaluator(
                            board = resolvedBoard,
                            value = value,
                            variableNames = listOf("u", "v"),
                            location = location,
                            creatorName = creatorName,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    parsed += evaluator
                }
                Surface3DSource.Components(
                    x = parsed[0].evaluator,
                    y = parsed[1].evaluator,
                    z = parsed[2].evaluator,
                ) to parsed.flatMap(ParsedSurface3DScalarEvaluator::dependencies)
            }
        }
        return surface3DResult(
            creatorName = creatorName,
            result = Surface3D.create(
                view = view,
                source = sourceAndDependencies.first,
                rangeUSource = rangeU.values,
                rangeVSource = rangeV.values,
                attributes = surfaceAttributes,
                dependencies =
                    sourceAndDependencies.second +
                        rangeU.dependencies +
                        rangeV.dependencies,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                functionGraph = functionGraph,
            ),
            location = location,
        )
    }

    private fun surface3DArrayEvaluator(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): Surface3DArrayEvaluator =
        Surface3DArrayEvaluator { parameterU, parameterV ->
            when (
                val result = function.externalCallable.call(
                    arguments = listOf(
                        JessieCodeRuntimeValue.NumberValue(parameterU),
                        JessieCodeRuntimeValue.NumberValue(parameterV),
                    ),
                    location = location,
                )
            ) {
                is GMResult.Err -> GMResult.Err(
                    Surface3DDynamicError.Rejected(
                        result.error.toString(),
                    ),
                )
                is GMResult.Ok -> {
                    val values = (
                        result.value as?
                            JessieCodeRuntimeValue.ArrayValue
                        )?.values
                    val coordinates = values?.map {
                        (it as? JessieCodeRuntimeValue.NumberValue)?.value
                            ?: return@Surface3DArrayEvaluator GMResult.Err(
                                Surface3DDynamicError.Rejected(
                                    "Expected numeric coordinate array, got " +
                                        typeName(result.value),
                                ),
                            )
                    }
                    if (coordinates == null) {
                        GMResult.Err(
                            Surface3DDynamicError.Rejected(
                                "Expected numeric coordinate array, got " +
                                    typeName(result.value),
                            ),
                        )
                    } else {
                        GMResult.Ok(coordinates.toDoubleArray())
                    }
                }
            }
        }

    private fun surface3DScalarEvaluator(
        board: Board,
        value: JessieCodeRuntimeValue,
        variableNames: List<String>,
        location: JessieCodeAstLocation,
        creatorName: String,
    ): GMResult<
        ParsedSurface3DScalarEvaluator,
        JessieCodeRuntimeError,
        > =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                GMResult.Ok(
                    ParsedSurface3DScalarEvaluator(
                        evaluator = Surface3DScalarEvaluator { _, _ ->
                            GMResult.Ok(value.value)
                        },
                        dependencies = emptyList(),
                    ),
                )
            is JessieCodeRuntimeValue.FunctionValue ->
                GMResult.Ok(
                    ParsedSurface3DScalarEvaluator(
                        evaluator = Surface3DScalarEvaluator { u, v ->
                            surface3DScalarResult(
                                result = value.externalCallable.call(
                                    arguments = listOf(
                                        JessieCodeRuntimeValue.NumberValue(u),
                                        JessieCodeRuntimeValue.NumberValue(v),
                                    ),
                                    location = location,
                                ),
                            )
                        },
                        dependencies =
                            value.dependencies.values.toList(),
                    ),
                )
            is JessieCodeRuntimeValue.StringValue -> {
                val expression = when (
                    val result = JessieCodeExpressionFunction.compile(
                        source = value.value,
                        board = board,
                        variableNames = variableNames,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return failure(
                        creatorName = creatorName,
                        error =
                            JessieCodeCreatorError.InvalidAttributeType(
                                attribute = "surface function",
                                expected = "valid JessieCode expression",
                                actual = result.error.toString(),
                            ),
                        location = location,
                    )
                }
                GMResult.Ok(
                    ParsedSurface3DScalarEvaluator(
                        evaluator = Surface3DScalarEvaluator { u, v ->
                            surface3DScalarResult(
                                result = expression.evaluate(
                                    listOf(
                                        JessieCodeRuntimeValue.NumberValue(u),
                                        JessieCodeRuntimeValue.NumberValue(v),
                                    ),
                                ),
                            )
                        },
                        dependencies =
                            expression.dependencies.values.toList(),
                    ),
                )
            }
            else -> invalidAttribute(
                creatorName = creatorName,
                attribute = "surface function",
                expected = "number, function, or JessieCode expression",
                actual = value,
                location = location,
            )
        }

    private fun surface3DScalarResult(
        result: GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>,
    ): GMResult<Double, Surface3DDynamicError> =
        when (result) {
            is GMResult.Err -> GMResult.Err(
                Surface3DDynamicError.Rejected(
                    result.error.toString(),
                ),
            )
            is GMResult.Ok -> {
                val number = result.value as?
                    JessieCodeRuntimeValue.NumberValue
                if (number == null) {
                    GMResult.Err(
                        Surface3DDynamicError.Rejected(
                            "Expected number, got ${typeName(result.value)}",
                        ),
                    )
                } else {
                    GMResult.Ok(number.value)
                }
            }
        }

    private fun surface3DResult(
        creatorName: String,
        result: GMResult<Surface3D, Surface3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Surface3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    // JSXGraph: src/3d/circle3d.js -> createCircle3D.
    private fun createCircle3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "circle3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 4) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = integerAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "numberpointshigh",
                default = Circle3D.DEFAULT_SAMPLE_COUNT,
                minimum = 1,
                maximum = Circle3D.MAX_SAMPLE_COUNT,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val center = when (
            val result = provideLine3DPoint(
                board = resolvedBoard,
                view = view,
                value = parents[1],
                attributes = attributes,
                role = "point",
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val normal = when (
            val result = circle3DNormal(
                value = parents[2],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                discardProvidedPoint3D(resolvedBoard, center)
                return result
            }
        }
        val radius = when (
            val result = line3DCoordinateValue(
                value = parents[3],
                attribute = "radius",
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                discardProvidedPoint3D(resolvedBoard, center)
                return result
            }
        }
        val radiusDependencies =
            (parents[3] as? JessieCodeRuntimeValue.FunctionValue)
                ?.dependencies
                ?.values
                ?.toList()
                ?: emptyList()
        val result = Circle3D.create(
            view = view,
            center = center.point,
            normalSource = normal,
            radiusSource = radius,
            ownsCenter = center.owned,
            radiusDependencies = radiusDependencies,
            sampleCount = sampleCount,
            id = identity.id,
            name = identity.name,
            needsRegularUpdate = identity.needsRegularUpdate,
        )
        if (result is GMResult.Err) {
            discardProvidedPoint3D(resolvedBoard, center)
        }
        return circle3DResult(result, location)
    }

    private fun circle3DNormal(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Circle3DNormalSource, JessieCodeRuntimeError> {
        if (value is JessieCodeRuntimeValue.FunctionValue) {
            return GMResult.Ok(
                Circle3DNormalSource.Function(
                    line3DArrayEvaluator(
                        function = value,
                        location = location,
                    ),
                ),
            )
        }
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = "circle3d",
                attribute = "normal",
                expected = "function or 3D/4D vector",
                actual = value,
                location = location,
            )
        if (array.values.size !in setOf(3, 4)) {
            return invalidAttribute(
                creatorName = "circle3d",
                attribute = "normal",
                expected = "array of three or four values",
                actual = value,
                location = location,
            )
        }
        val coordinates = mutableListOf<Line3DCoordinateValue>()
        for (coordinate in array.values) {
            when (
                val result = line3DCoordinateValue(
                    value = coordinate,
                    attribute = "normal",
                    location = location,
                    creatorName = "circle3d",
                )
            ) {
                is GMResult.Ok -> coordinates += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(Circle3DNormalSource.Values(coordinates))
    }

    private fun circle3DResult(
        result: GMResult<Circle3D, Circle3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "circle3d",
                error = JessieCodeCreatorError.Circle3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    // JSXGraph: src/3d/circle3d.js -> createIntersectionCircle3D.
    private fun createIntersectionCircle3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "intersectioncircle3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val first = resolveElement(
            resolvedBoard,
            parents[1],
        ) as? GeometryElement3D
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(
            resolvedBoard,
            parents[2],
        ) as? GeometryElement3D
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = integerAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "numberpointshigh",
                default = Circle3D.DEFAULT_SAMPLE_COUNT,
                minimum = 1,
                maximum = Circle3D.MAX_SAMPLE_COUNT,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return intersectionCircle3DResult(
            result = IntersectionCircle3D.create(
                view = view,
                first = first,
                second = second,
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
            location = location,
        )
    }

    private fun intersectionCircle3DResult(
        result: GMResult<Circle3D, IntersectionCircle3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "intersectioncircle3d",
                error = JessieCodeCreatorError.IntersectionCircle3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    // JSXGraph: src/3d/sphere3d.js -> createSphere3D.
    private fun createSphere3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "sphere3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = integerAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "numberpointshigh",
                default = Sphere3D.DEFAULT_SAMPLE_COUNT,
                minimum = 1,
                maximum = Sphere3D.MAX_SAMPLE_COUNT,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val center = when (
            val result = provideLine3DPoint(
                board = resolvedBoard,
                view = view,
                value = parents[1],
                attributes = attributes,
                role = "center",
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val second = parents[2]
        val pointValue = when (second) {
            is JessieCodeRuntimeValue.ArrayValue,
            is JessieCodeRuntimeValue.ElementReference,
            is JessieCodeRuntimeValue.StringValue,
            -> resolveSphere3DPointValue(
                board = resolvedBoard,
                value = second,
            )
            is JessieCodeRuntimeValue.FunctionValue ->
                when (
                    val result = second.externalCallable.call(
                        arguments = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok ->
                        if (
                            result.value is
                                JessieCodeRuntimeValue.ArrayValue
                        ) {
                            second
                        } else {
                            null
                        }
                    is GMResult.Err -> {
                        discardProvidedPoint3D(resolvedBoard, center)
                        return failure(
                            creatorName = creatorName,
                            error =
                                JessieCodeCreatorError.InvalidAttributeType(
                                    attribute = "point or radius",
                                    expected =
                                        "Point3D, coordinate array, " +
                                            "number, function, or " +
                                            "JessieCode expression",
                                    actual = result.error.toString(),
                                ),
                            location = location,
                        )
                    }
                }
            else -> null
        }
        if (pointValue != null) {
            val point = when (
                val result = provideLine3DPoint(
                    board = resolvedBoard,
                    view = view,
                    value = pointValue,
                    attributes = attributes,
                    role = "point",
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    discardProvidedPoint3D(resolvedBoard, center)
                    return result
                }
            }
            val result = Sphere3D.create(
                view = view,
                center = center.point,
                point2 = point.point,
                ownsCenter = center.owned,
                ownsPoint2 = point.owned,
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
            if (result is GMResult.Err) {
                discardProvidedPoint3D(resolvedBoard, center)
                discardProvidedPoint3D(resolvedBoard, point)
            }
            return sphere3DResult(result, location)
        }

        val radius = when (
            val result = sphere3DRadius(
                board = resolvedBoard,
                value = second,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                discardProvidedPoint3D(resolvedBoard, center)
                return result
            }
        }
        val result = Sphere3D.create(
            view = view,
            center = center.point,
            radiusSource = radius.source,
            ownsCenter = center.owned,
            radiusDependencies = radius.dependencies,
            sampleCount = sampleCount,
            id = identity.id,
            name = identity.name,
            needsRegularUpdate = identity.needsRegularUpdate,
        )
        if (result is GMResult.Err) {
            discardProvidedPoint3D(resolvedBoard, center)
        }
        return sphere3DResult(result, location)
    }

    private fun resolveSphere3DPointValue(
        board: Board,
        value: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue? =
        when (value) {
            is JessieCodeRuntimeValue.ArrayValue ->
                if (value.values.size >= 3) value else null
            is JessieCodeRuntimeValue.ElementReference ->
                if (value.element is Point3D) value else null
            is JessieCodeRuntimeValue.StringValue ->
                if (board.select(value.value) is Point3D) value else null
            else -> null
        }

    private fun sphere3DRadius(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedSphere3DRadius, JessieCodeRuntimeError> {
        if (value is JessieCodeRuntimeValue.StringValue) {
            val expression = when (
                val result = JessieCodeExpressionFunction.compile(
                    source = value.value,
                    board = board,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return failure(
                    creatorName = "sphere3d",
                    error = JessieCodeCreatorError.InvalidAttributeType(
                        attribute = "radius",
                        expected = "valid JessieCode expression",
                        actual = result.error.toString(),
                    ),
                    location = location,
                )
            }
            return GMResult.Ok(
                ParsedSphere3DRadius(
                    source = Line3DCoordinateValue.Dynamic(
                        Line3DScalarEvaluator {
                            sphere3DRadiusResult(expression.evaluate())
                        },
                    ),
                    dependencies = expression.dependencies.values.toList(),
                ),
            )
        }
        val source = when (
            val result = line3DCoordinateValue(
                value = value,
                attribute = "radius",
                location = location,
                creatorName = "sphere3d",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedSphere3DRadius(
                source = source,
                dependencies =
                    (value as? JessieCodeRuntimeValue.FunctionValue)
                        ?.dependencies
                        ?.values
                        ?.toList()
                        ?: emptyList(),
            ),
        )
    }

    private fun sphere3DRadiusResult(
        result: GMResult<
            JessieCodeRuntimeValue,
            JessieCodeRuntimeError,
            >,
    ): GMResult<Double, Line3DDynamicError> =
        when (result) {
            is GMResult.Err -> GMResult.Err(
                Line3DDynamicError.Rejected(result.error.toString()),
            )
            is GMResult.Ok -> {
                val number = result.value as?
                    JessieCodeRuntimeValue.NumberValue
                if (number == null) {
                    GMResult.Err(
                        Line3DDynamicError.Rejected(
                            "Expected number, got ${typeName(result.value)}",
                        ),
                    )
                } else {
                    GMResult.Ok(number.value)
                }
            }
        }

    private fun sphere3DResult(
        result: GMResult<Sphere3D, Sphere3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "sphere3d",
                error = JessieCodeCreatorError.Sphere3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    // JSXGraph: src/3d/polyhedron3d.js -> createPolyhedron3D.
    private fun createPolyhedron3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "polyhedron3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fillColors = when (
            val result = polyhedron3DFillColors(
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val base = resolveElement(resolvedBoard, parents[1])
            as? Polyhedron3D
        if (base != null) {
            val transformations = transformationReferences(parents[2])
                ?: return unsupported(creatorName, parents, location)
            val faceAttributes = mutableListOf<Face3DAttributes>()
            for (faceNumber in base.faces.indices) {
                when (
                    val result = polyhedron3DFaceAttributes(
                        overall = attributes,
                        faceSpecific = null,
                        cyclicFillColor = fillColors
                            .takeIf(List<String>::isNotEmpty)
                            ?.let { it[faceNumber % it.size] },
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> faceAttributes += result.value
                    is GMResult.Err -> return result
                }
            }
            return polyhedron3DResult(
                result = Polyhedron3D.create(
                    view = view,
                    base = base,
                    transformations = transformations,
                    faceAttributes = faceAttributes,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                ),
                location = location,
            )
        }

        val vertices = when (
            val result = polyhedron3DVertices(
                board = resolvedBoard,
                value = parents[1],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val faces = when (
            val result = polyhedron3DFaces(
                value = parents[2],
                attributes = attributes,
                fillColors = fillColors,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return polyhedron3DResult(
            result = Polyhedron3D.create(
                view = view,
                vertices = vertices.sources,
                faceInputs = faces,
                dependencies = vertices.dependencies,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
            location = location,
        )
    }

    private fun polyhedron3DResult(
        result: GMResult<Polyhedron3D, Polyhedron3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "polyhedron3d",
                error = JessieCodeCreatorError.Polyhedron3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    private fun polyhedron3DVertices(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedPolyhedron3DVertices, JessieCodeRuntimeError> {
        val entries = when (value) {
            is JessieCodeRuntimeValue.ArrayValue ->
                value.values.mapIndexed { index, vertex ->
                    index.toString() to vertex
                }
            is JessieCodeRuntimeValue.ObjectValue ->
                value.properties.entries.map { it.key to it.value }
            else -> return invalidAttribute(
                creatorName = "polyhedron3d",
                attribute = "vertices",
                expected = "array or object",
                actual = value,
                location = location,
            )
        }
        val sources = linkedMapOf<String, Polyhedron3DVertexSource>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for ((key, vertex) in entries) {
            val point = resolveElement(board, vertex) as? Point3D
            if (point != null) {
                sources[key] = Polyhedron3DVertexSource.Point(point)
                dependencies[point.id] = point
                continue
            }
            if (vertex is JessieCodeRuntimeValue.FunctionValue) {
                sources[key] = Polyhedron3DVertexSource.Function(
                    line3DArrayEvaluator(vertex, location),
                )
                dependencies.putAll(vertex.dependencies)
                continue
            }
            val parsed = when (
                val result = polyhedron3DVertexValues(
                    value = vertex,
                    key = key,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            sources[key] = Polyhedron3DVertexSource.Values(parsed.values)
            for (dependency in parsed.dependencies) {
                dependencies[dependency.id] = dependency
            }
        }
        return GMResult.Ok(
            ParsedPolyhedron3DVertices(
                sources = sources,
                dependencies = dependencies.values.toList(),
            ),
        )
    }

    private fun polyhedron3DVertexValues(
        value: JessieCodeRuntimeValue,
        key: String,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedLine3DValues, JessieCodeRuntimeError> {
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = "polyhedron3d",
                attribute = "vertices.$key",
                expected =
                    "Point3D, point reference, function, or array of " +
                        "three or four values",
                actual = value,
                location = location,
            )
        if (array.values.size !in setOf(3, 4)) {
            return invalidAttribute(
                creatorName = "polyhedron3d",
                attribute = "vertices.$key",
                expected = "array of three or four values",
                actual = value,
                location = location,
            )
        }
        val values = mutableListOf<Line3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (coordinate in array.values) {
            when (
                val result = line3DCoordinateValue(
                    value = coordinate,
                    attribute = "vertices.$key",
                    location = location,
                    creatorName = "polyhedron3d",
                )
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
            if (coordinate is JessieCodeRuntimeValue.FunctionValue) {
                dependencies.putAll(coordinate.dependencies)
            }
        }
        return GMResult.Ok(
            ParsedLine3DValues(
                values = values,
                dependencies = dependencies.values.toList(),
            ),
        )
    }

    private fun polyhedron3DFaces(
        value: JessieCodeRuntimeValue,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        fillColors: List<String>,
        location: JessieCodeAstLocation,
    ): GMResult<List<Polyhedron3DFaceInput>, JessieCodeRuntimeError> {
        val faces = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = "polyhedron3d",
                attribute = "faces",
                expected = "array",
                actual = value,
                location = location,
            )
        val inputs = mutableListOf<Polyhedron3DFaceInput>()
        for ((faceNumber, faceValue) in faces.values.withIndex()) {
            val face = faceValue as? JessieCodeRuntimeValue.ArrayValue
                ?: return invalidAttribute(
                    creatorName = "polyhedron3d",
                    attribute = "faces[$faceNumber]",
                    expected = "array",
                    actual = faceValue,
                    location = location,
                )
            val first = face.values.getOrNull(0)
            val second = face.values.getOrNull(1)
            val hasFaceAttributes =
                face.values.size == 2 &&
                    first is JessieCodeRuntimeValue.ArrayValue &&
                    second is JessieCodeRuntimeValue.ObjectValue
            val vertexValues =
                if (hasFaceAttributes) {
                    first.values
                } else {
                    face.values
                }
            val faceSpecific =
                if (hasFaceAttributes) {
                    second
                } else {
                    null
                }
            val vertexKeys = mutableListOf<String>()
            for (vertexValue in vertexValues) {
                val key = when (vertexValue) {
                    is JessieCodeRuntimeValue.StringValue ->
                        vertexValue.value
                    is JessieCodeRuntimeValue.NumberValue ->
                        vertexValue.value
                            .takeIf(Double::isFinite)
                            ?.let(JsNumberFormat::compact)
                    else -> null
                } ?: return invalidAttribute(
                    creatorName = "polyhedron3d",
                    attribute = "faces[$faceNumber]",
                    expected = "array of finite numbers or strings",
                    actual = vertexValue,
                    location = location,
                )
                vertexKeys += key
            }
            val faceAttributes = when (
                val result = polyhedron3DFaceAttributes(
                    overall = attributes,
                    faceSpecific = faceSpecific,
                    cyclicFillColor = fillColors
                        .takeIf(List<String>::isNotEmpty)
                        ?.let { it[faceNumber % it.size] },
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            inputs += Polyhedron3DFaceInput(
                vertexKeys = vertexKeys,
                attributes = faceAttributes,
            )
        }
        return GMResult.Ok(inputs)
    }

    private fun polyhedron3DFillColors(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<List<String>, JessieCodeRuntimeError> {
        val value = attributes.properties["fillcolorarray"]
            ?: return GMResult.Ok(listOf("white", "black"))
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(listOf("white", "black"))
        }
        val colors = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = "polyhedron3d",
                attribute = "fillcolorarray",
                expected = "array of strings",
                actual = value,
                location = location,
            )
        val result = mutableListOf<String>()
        for (color in colors.values) {
            val string = (color as? JessieCodeRuntimeValue.StringValue)?.value
                ?: return invalidAttribute(
                    creatorName = "polyhedron3d",
                    attribute = "fillcolorarray",
                    expected = "array of strings",
                    actual = color,
                    location = location,
                )
            result += string
        }
        return GMResult.Ok(result)
    }

    private fun polyhedron3DFaceAttributes(
        overall: JessieCodeRuntimeValue.ObjectValue,
        faceSpecific: JessieCodeRuntimeValue.ObjectValue?,
        cyclicFillColor: String?,
        location: JessieCodeAstLocation,
    ): GMResult<Face3DAttributes, JessieCodeRuntimeError> {
        val properties = linkedMapOf<String, JessieCodeRuntimeValue>()
        properties.putAll(
            overall.properties.filterKeys {
                it !in setOf("id", "name", "fillcolorarray")
            },
        )
        if (cyclicFillColor != null) {
            properties["fillcolor"] =
                JessieCodeRuntimeValue.StringValue(cyclicFillColor)
        }
        if (faceSpecific != null) {
            val normalized = lowercaseAttributeObject(faceSpecific)
            for ((key, value) in normalized.properties) {
                properties[key] =
                    if (
                        key == "shader" &&
                        properties[key] is JessieCodeRuntimeValue.ObjectValue &&
                        value is JessieCodeRuntimeValue.ObjectValue
                    ) {
                        mergeFace3DNestedAttributes(
                            properties.getValue(key) as
                                JessieCodeRuntimeValue.ObjectValue,
                            value,
                        )
                    } else {
                        value
                    }
            }
        }
        val merged = JessieCodeRuntimeValue.ObjectValue(properties)
        val unsupported = merged.properties.keys.firstOrNull {
            it !in FACE_3D_ATTRIBUTES
        }
        if (unsupported != null) {
            return failure(
                creatorName = "polyhedron3d",
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = unsupported,
                    actual = "unsupported Face3D attribute",
                ),
                location = location,
            )
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName = "polyhedron3d",
                attributes = merged,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val shader = when (
            val result = face3DShaderAttributes(merged, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            Face3DAttributes(
                id = identity.id,
                name = identity.name ?: "",
                needsRegularUpdate = identity.needsRegularUpdate,
                visible = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        merged,
                        "visible",
                        true,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                strokeColor = when (
                    val result = stringAttribute(
                        "polyhedron3d",
                        merged,
                        "strokecolor",
                        "#0072b2",
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                fillColor = when (
                    val result = stringAttribute(
                        "polyhedron3d",
                        merged,
                        "fillcolor",
                        "yellow",
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                strokeWidth = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        merged,
                        "strokewidth",
                        1.0,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                strokeOpacity = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        merged,
                        "strokeopacity",
                        1.0,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                fillOpacity = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        merged,
                        "fillopacity",
                        0.4,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                layer = when (
                    val result = integerAttribute(
                        "polyhedron3d",
                        merged,
                        "layer",
                        12,
                        0,
                        Int.MAX_VALUE,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                fixed = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        merged,
                        "fixed",
                        false,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                highlight = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        merged,
                        "highlight",
                        false,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                withLabel = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        merged,
                        "withlabel",
                        false,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                dash = when (
                    val result = integerAttribute(
                        "polyhedron3d",
                        merged,
                        "dash",
                        0,
                        0,
                        7,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                dashScale = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        merged,
                        "dashscale",
                        false,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                lineCap = when (
                    val result = stringAttribute(
                        "polyhedron3d",
                        merged,
                        "linecap",
                        "round",
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                shader = shader,
            ),
        )
    }

    private fun face3DShaderAttributes(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<Face3DShaderAttributes, JessieCodeRuntimeError> {
        val shader = when (
            val result = nestedObjectAttribute(
                creatorName = "polyhedron3d",
                attributes = attributes,
                name = "shader",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        shader.properties.keys.firstOrNull {
            it !in FACE_3D_SHADER_ATTRIBUTES
        }?.let { unsupported ->
            return failure(
                creatorName = "polyhedron3d",
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "shader.$unsupported",
                    actual = "unsupported Face3D shader attribute",
                ),
                location = location,
            )
        }
        val light = when (
            val result = nestedObjectAttribute(
                creatorName = "polyhedron3d",
                attributes = shader,
                name = "light",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        light.properties.keys.firstOrNull {
            it !in FACE_3D_LIGHT_ATTRIBUTES
        }?.let { unsupported ->
            return failure(
                creatorName = "polyhedron3d",
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "shader.light.$unsupported",
                    actual = "unsupported Face3D light attribute",
                ),
                location = location,
            )
        }
        val lightAttributes = Face3DLightAttributes(
            type = when (
                val result = integerAttribute(
                    "polyhedron3d",
                    light,
                    "type",
                    1,
                    1,
                    3,
                    location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            },
            azimuth = when (
                val result = numberAttribute(
                    "polyhedron3d",
                    light,
                    "az",
                    -45.0,
                    location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            },
            elevation = when (
                val result = numberAttribute(
                    "polyhedron3d",
                    light,
                    "el",
                    20.0,
                    location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            },
            bank = when (
                val result = numberAttribute(
                    "polyhedron3d",
                    light,
                    "bank",
                    0.0,
                    location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            },
            direction = when (
                val result = integerAttribute(
                    "polyhedron3d",
                    light,
                    "dir",
                    -1,
                    -1,
                    1,
                    location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            },
        )
        return GMResult.Ok(
            Face3DShaderAttributes(
                enabled = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        shader,
                        "enabled",
                        false,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                fixed = when (
                    val result = booleanAttribute(
                        "polyhedron3d",
                        shader,
                        "fixed",
                        true,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                type = when (
                    val result = stringAttribute(
                        "polyhedron3d",
                        shader,
                        "type",
                        "angle",
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                hue = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        shader,
                        "hue",
                        60.0,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                saturation = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        shader,
                        "saturation",
                        90.0,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                minimumLightness = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        shader,
                        "minlightness",
                        30.0,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                maximumLightness = when (
                    val result = numberAttribute(
                        "polyhedron3d",
                        shader,
                        "maxlightness",
                        90.0,
                        location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                },
                light = lightAttributes,
            ),
        )
    }

    private fun lowercaseAttributeObject(
        value: JessieCodeRuntimeValue.ObjectValue,
    ): JessieCodeRuntimeValue.ObjectValue =
        JessieCodeRuntimeValue.ObjectValue(
            value.properties.map { (name, child) ->
                name.lowercase() to
                    if (child is JessieCodeRuntimeValue.ObjectValue) {
                        lowercaseAttributeObject(child)
                    } else {
                        child
                    }
            }.toMap(),
        )

    private fun mergeFace3DNestedAttributes(
        base: JessieCodeRuntimeValue.ObjectValue,
        override: JessieCodeRuntimeValue.ObjectValue,
    ): JessieCodeRuntimeValue.ObjectValue {
        val properties = base.properties.toMutableMap()
        for ((key, value) in override.properties) {
            properties[key] =
                if (
                    key == "light" &&
                    properties[key] is JessieCodeRuntimeValue.ObjectValue &&
                    value is JessieCodeRuntimeValue.ObjectValue
                ) {
                    val baseLight =
                        properties.getValue(key) as
                            JessieCodeRuntimeValue.ObjectValue
                    JessieCodeRuntimeValue.ObjectValue(
                        baseLight.properties + value.properties,
                    )
                } else {
                    value
                }
        }
        return JessieCodeRuntimeValue.ObjectValue(properties)
    }

    // JSXGraph: src/3d/box3d.js -> createMesh3D.
    private fun createMesh3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "mesh3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 6) {
            return unsupported(creatorName, parents, location)
        }
        val view = resolveElement(resolvedBoard, parents[0]) as? View3D
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point = when (
            val result = mesh3DPoint(
                board = resolvedBoard,
                value = parents[1],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction1 = when (
            val result = mesh3DVector(
                value = parents[2],
                attribute = "direction1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction2 = when (
            val result = mesh3DVector(
                value = parents[3],
                attribute = "direction2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeU = when (
            val result = line3DRange(
                value = parents[4],
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rangeV = when (
            val result = line3DRange(
                value = parents[5],
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val stepWidthU = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "stepwidthu",
                default = 1.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val stepWidthV = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "stepwidthv",
                default = 1.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val dependencies = buildList {
            addAll(point.dependencies)
            addAll(direction1.dependencies)
            addAll(direction2.dependencies)
            addAll(rangeU.dependencies)
            addAll(rangeV.dependencies)
        }
        return when (
            val result = Mesh3D.create(
                view = view,
                pointSource = point.source,
                direction1Source = direction1.source,
                direction2Source = direction2.source,
                rangeUSource = rangeU.values,
                rangeVSource = rangeV.values,
                stepWidthU = stepWidthU,
                stepWidthV = stepWidthV,
                dependencies = dependencies,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.Mesh3DFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun mesh3DPoint(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedMesh3DPoint, JessieCodeRuntimeError> {
        val point = resolveElement(board, value) as? Point3D
        if (point != null) {
            return GMResult.Ok(
                ParsedMesh3DPoint(
                    source = Mesh3DPointSource.Point(point),
                    dependencies = listOf(point),
                ),
            )
        }
        if (value is JessieCodeRuntimeValue.FunctionValue) {
            return GMResult.Ok(
                ParsedMesh3DPoint(
                    source = Mesh3DPointSource.Function(
                        line3DArrayEvaluator(value, location),
                    ),
                    dependencies = value.dependencies.values.toList(),
                ),
            )
        }
        val parsed = when (
            val result = mesh3DValues(
                value = value,
                attribute = "point",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedMesh3DPoint(
                source = Mesh3DPointSource.Values(parsed.values),
                dependencies = parsed.dependencies,
            ),
        )
    }

    private fun mesh3DVector(
        value: JessieCodeRuntimeValue,
        attribute: String,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedMesh3DVector, JessieCodeRuntimeError> {
        if (value is JessieCodeRuntimeValue.FunctionValue) {
            return GMResult.Ok(
                ParsedMesh3DVector(
                    source = Mesh3DVectorSource.Function(
                        line3DArrayEvaluator(value, location),
                    ),
                    dependencies = value.dependencies.values.toList(),
                ),
            )
        }
        val parsed = when (
            val result = mesh3DValues(
                value = value,
                attribute = attribute,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ParsedMesh3DVector(
                source = Mesh3DVectorSource.Values(parsed.values),
                dependencies = parsed.dependencies,
            ),
        )
    }

    private fun mesh3DValues(
        value: JessieCodeRuntimeValue,
        attribute: String,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedLine3DValues, JessieCodeRuntimeError> {
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName = "mesh3d",
                attribute = attribute,
                expected = "function or array of three or four values",
                actual = value,
                location = location,
            )
        if (array.values.size !in setOf(3, 4)) {
            return invalidAttribute(
                creatorName = "mesh3d",
                attribute = attribute,
                expected = "array of three or four values",
                actual = value,
                location = location,
            )
        }
        val values = mutableListOf<Line3DCoordinateValue>()
        val dependencies = linkedMapOf<String, GeometryElement>()
        for (coordinate in array.values) {
            when (
                val result = line3DCoordinateValue(
                    value = coordinate,
                    attribute = attribute,
                    location = location,
                    creatorName = "mesh3d",
                )
            ) {
                is GMResult.Ok -> values += result.value
                is GMResult.Err -> return result
            }
            if (coordinate is JessieCodeRuntimeValue.FunctionValue) {
                dependencies.putAll(coordinate.dependencies)
            }
        }
        return GMResult.Ok(
            ParsedLine3DValues(
                values = values,
                dependencies = dependencies.values.toList(),
            ),
        )
    }

    // JSXGraph: src/3d/linspace3d.js -> createPlane3D.
    private fun createPlane3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "plane3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val view = parents.firstOrNull()?.let {
            resolveElement(resolvedBoard, it)
        } as? View3D ?: return unsupported(
            creatorName,
            parents,
            location,
        )
        val planeParents = parents.drop(1)
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val planeType = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "type",
                default = "shader",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value.lowercase()
            is GMResult.Err -> return result
        }
        val surfaceAttributes =
            if (planeType == "wireframe") {
                Plane3DSurfaceAttributes()
            } else {
                when (
                    val result = plane3DSurfaceAttributes(
                        attributes = attributes,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        val meshAttributes = when (
            val value = attributes.properties["mesh3d"]
        ) {
            null, JessieCodeRuntimeValue.UndefinedValue ->
                JessieCodeRuntimeValue.ObjectValue(emptyMap())
            is JessieCodeRuntimeValue.ObjectValue -> value
            else -> return invalidAttribute(
                creatorName = creatorName,
                attribute = "mesh3d",
                expected = "object",
                actual = value,
                location = location,
            )
        }
        val meshStepWidthU = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = meshAttributes,
                name = "stepwidthu",
                default = 1.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val meshStepWidthV = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = meshAttributes,
                name = "stepwidthv",
                default = 1.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val threePointsAttribute = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "threepoints",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (planeParents.size in 2..4) {
            val basePlane =
                resolveElement(resolvedBoard, planeParents[0]) as? Plane3D
            val transformations = transformationReferences(planeParents[1])
            if (basePlane != null && transformations != null) {
                val rangeU = if (planeParents.size >= 3) {
                    when (
                        val result = line3DRange(
                            value = planeParents[2],
                            location = location,
                            creatorName = creatorName,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                } else {
                    defaultPlane3DRange()
                }
                val rangeV = if (planeParents.size >= 4) {
                    when (
                        val result = line3DRange(
                            value = planeParents[3],
                            location = location,
                            creatorName = creatorName,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                } else {
                    defaultPlane3DRange()
                }
                return plane3DResult(
                    result = Plane3D.create(
                        view = view,
                        basePlane = basePlane,
                        transformations = transformations,
                        rangeUSource = rangeU.values,
                        rangeVSource = rangeV.values,
                        dependencies =
                            rangeU.dependencies + rangeV.dependencies,
                        planeType = planeType,
                        meshStepWidthU = meshStepWidthU,
                        meshStepWidthV = meshStepWidthV,
                        surfaceAttributes = surfaceAttributes,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate =
                            identity.needsRegularUpdate,
                        fixed = fixed,
                    ),
                    location = location,
                )
            }
        }

        val threePointForm =
            planeParents.size >= 3 &&
                (
                    threePointsAttribute ||
                        resolveElement(
                            resolvedBoard,
                            planeParents[1],
                        ) is Point3D ||
                        resolveElement(
                            resolvedBoard,
                            planeParents[2],
                        ) is Point3D
                    )
        if (threePointForm) {
            if (planeParents.size !in 3..5) {
                return unsupported(creatorName, parents, location)
            }
            val providedPoints = mutableListOf<ProvidedLine3DPoint>()
            for (index in 0 until 3) {
                val provided = when (
                    val result = provideLine3DPoint(
                        board = resolvedBoard,
                        view = view,
                        value = planeParents[index],
                        attributes = attributes,
                        role = "point${index + 1}",
                        location = location,
                        creatorName = creatorName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        for (point in providedPoints) {
                            discardProvidedPoint3D(resolvedBoard, point)
                        }
                        return result
                    }
                }
                providedPoints += provided
            }
            val rangeU = if (planeParents.size >= 4) {
                when (
                    val result = line3DRange(
                        value = planeParents[3],
                        location = location,
                        creatorName = creatorName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        for (point in providedPoints) {
                            discardProvidedPoint3D(resolvedBoard, point)
                        }
                        return result
                    }
                }
            } else {
                defaultPlane3DRange()
            }
            val rangeV = if (planeParents.size >= 5) {
                when (
                    val result = line3DRange(
                        value = planeParents[4],
                        location = location,
                        creatorName = creatorName,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        for (point in providedPoints) {
                            discardProvidedPoint3D(resolvedBoard, point)
                        }
                        return result
                    }
                }
            } else {
                defaultPlane3DRange()
            }
            return plane3DResult(
                result = Plane3D.create(
                    view = view,
                    point1 = providedPoints[0].point,
                    point2 = providedPoints[1].point,
                    point3 = providedPoints[2].point,
                    ownsPoint1 = providedPoints[0].owned,
                    ownsPoint2 = providedPoints[1].owned,
                    ownsPoint3 = providedPoints[2].owned,
                    rangeUSource = rangeU.values,
                    rangeVSource = rangeV.values,
                    dependencies =
                        rangeU.dependencies + rangeV.dependencies,
                    planeType = planeType,
                    meshStepWidthU = meshStepWidthU,
                    meshStepWidthV = meshStepWidthV,
                    surfaceAttributes = surfaceAttributes,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    fixed = fixed,
                ),
                location = location,
            )
        }

        if (planeParents.size !in 3..5) {
            return unsupported(creatorName, parents, location)
        }
        val definingPoint = when (
            val result = provideLine3DPoint(
                board = resolvedBoard,
                view = view,
                value = planeParents[0],
                attributes = attributes,
                role = "point",
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val firstDirection = when (
            val result = plane3DDirection(
                board = resolvedBoard,
                value = planeParents[1],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                discardProvidedPoint3D(resolvedBoard, definingPoint)
                return result
            }
        }
        val secondDirection = when (
            val result = plane3DDirection(
                board = resolvedBoard,
                value = planeParents[2],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                discardProvidedPoint3D(resolvedBoard, definingPoint)
                return result
            }
        }
        val rangeU = if (planeParents.size >= 4) {
            when (
                val result = line3DRange(
                    value = planeParents[3],
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    discardProvidedPoint3D(resolvedBoard, definingPoint)
                    return result
                }
            }
        } else {
            defaultPlane3DRange()
        }
        val rangeV = if (planeParents.size >= 5) {
            when (
                val result = line3DRange(
                    value = planeParents[4],
                    location = location,
                    creatorName = creatorName,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    discardProvidedPoint3D(resolvedBoard, definingPoint)
                    return result
                }
            }
        } else {
            defaultPlane3DRange()
        }
        return plane3DResult(
            result = Plane3D.create(
                view = view,
                point = definingPoint.point,
                direction1Source = firstDirection.source,
                direction2Source = secondDirection.source,
                rangeUSource = rangeU.values,
                rangeVSource = rangeV.values,
                ownsPoint = definingPoint.owned,
                dependencies =
                    firstDirection.dependencies +
                        secondDirection.dependencies +
                        rangeU.dependencies +
                        rangeV.dependencies,
                planeType = planeType,
                meshStepWidthU = meshStepWidthU,
                meshStepWidthV = meshStepWidthV,
                surfaceAttributes = surfaceAttributes,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            ),
            location = location,
        )
    }

    private fun plane3DSurfaceAttributes(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        defaults: Plane3DSurfaceAttributes =
            Plane3DSurfaceAttributes(),
        planeVisibleDefault: Boolean = true,
        creatorName: String = "plane3d",
        minimumStepsU: Int = 1,
        minimumStepsV: Int = 1,
    ): GMResult<Plane3DSurfaceAttributes, JessieCodeRuntimeError> {
        val tiling = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "tiling",
                default = defaults.tiling,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value.lowercase()
            is GMResult.Err -> return result
        }
        val stepsU = when (
            val result = integerAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "stepsu",
                default = defaults.stepsU,
                minimum = minimumStepsU,
                maximum = Polyhedron3D.MAX_VERTEX_COUNT,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val stepsV = when (
            val result = integerAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "stepsv",
                default = defaults.stepsV,
                minimum = minimumStepsV,
                maximum = Polyhedron3D.MAX_VERTEX_COUNT,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val planeVisible = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "visible",
                default = planeVisibleDefault,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val configuredPolyhedron = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "polyhedron",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val defaultFace = defaults.faceAttributes
        val defaultLightAttributes = defaultFace.shader.light
        val defaultLight = JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "type" to JessieCodeRuntimeValue.NumberValue(
                    defaultLightAttributes.type.toDouble(),
                ),
                "az" to JessieCodeRuntimeValue.NumberValue(
                    defaultLightAttributes.azimuth,
                ),
                "el" to JessieCodeRuntimeValue.NumberValue(
                    defaultLightAttributes.elevation,
                ),
                "bank" to JessieCodeRuntimeValue.NumberValue(
                    defaultLightAttributes.bank,
                ),
                "dir" to JessieCodeRuntimeValue.NumberValue(
                    defaultLightAttributes.direction.toDouble(),
                ),
            ),
        )
        val defaultShader = JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "enabled" to JessieCodeRuntimeValue.BooleanValue(
                    defaultFace.shader.enabled,
                ),
                "fixed" to JessieCodeRuntimeValue.BooleanValue(
                    defaultFace.shader.fixed,
                ),
                "type" to JessieCodeRuntimeValue.StringValue(
                    defaultFace.shader.type,
                ),
                "hue" to JessieCodeRuntimeValue.NumberValue(
                    defaultFace.shader.hue,
                ),
                "saturation" to JessieCodeRuntimeValue.NumberValue(
                    defaultFace.shader.saturation,
                ),
                "minlightness" to
                    JessieCodeRuntimeValue.NumberValue(
                        defaultFace.shader.minimumLightness,
                    ),
                "maxlightness" to
                    JessieCodeRuntimeValue.NumberValue(
                        defaultFace.shader.maximumLightness,
                    ),
                "light" to defaultLight,
            ),
        )
        val effectiveProperties =
            linkedMapOf<String, JessieCodeRuntimeValue>(
                "visible" to
                    JessieCodeRuntimeValue.BooleanValue(planeVisible),
                "layer" to JessieCodeRuntimeValue.NumberValue(12.0),
                "strokecolor" to JessieCodeRuntimeValue.StringValue(
                    defaultFace.strokeColor,
                ),
                "fillcolor" to JessieCodeRuntimeValue.StringValue(
                    defaultFace.fillColor,
                ),
                "strokewidth" to
                    JessieCodeRuntimeValue.NumberValue(
                        defaultFace.strokeWidth,
                    ),
                "strokeopacity" to JessieCodeRuntimeValue.NumberValue(
                    defaultFace.strokeOpacity,
                ),
                "fillopacity" to
                    JessieCodeRuntimeValue.NumberValue(
                        defaultFace.fillOpacity,
                    ),
                "fixed" to JessieCodeRuntimeValue.BooleanValue(
                    defaultFace.fixed,
                ),
                "highlight" to JessieCodeRuntimeValue.BooleanValue(
                    defaultFace.highlight,
                ),
                "withlabel" to JessieCodeRuntimeValue.BooleanValue(
                    defaultFace.withLabel,
                ),
                "dash" to JessieCodeRuntimeValue.NumberValue(
                    defaultFace.dash.toDouble(),
                ),
                "dashscale" to JessieCodeRuntimeValue.BooleanValue(
                    defaultFace.dashScale,
                ),
                "linecap" to JessieCodeRuntimeValue.StringValue(
                    defaultFace.lineCap,
                ),
                "fillcolorarray" to
                    JessieCodeRuntimeValue.ArrayValue(
                        defaults.fillColorArray.map(
                            JessieCodeRuntimeValue::StringValue,
                        ),
                    ),
                "shader" to defaultShader,
            )
        for ((key, value) in configuredPolyhedron.properties) {
            effectiveProperties[key] =
                when {
                    key == "visible" &&
                        value is JessieCodeRuntimeValue.StringValue &&
                        value.value.lowercase() == "inherit" ->
                        JessieCodeRuntimeValue.BooleanValue(planeVisible)
                    key == "shader" &&
                        value is JessieCodeRuntimeValue.ObjectValue ->
                        mergeFace3DNestedAttributes(defaultShader, value)
                    else -> value
                }
        }
        val effectivePolyhedron =
            JessieCodeRuntimeValue.ObjectValue(effectiveProperties)
        val fillColors = when (
            val result = polyhedron3DFillColors(
                attributes = effectivePolyhedron,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val faceAttributes = when (
            val result = polyhedron3DFaceAttributes(
                overall = effectivePolyhedron,
                faceSpecific = null,
                cyclicFillColor = null,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val colormap = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "colormap",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        colormap.properties.keys.firstOrNull {
            it !in PLANE_3D_COLORMAP_ATTRIBUTES
        }?.let { unsupported ->
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "colormap.$unsupported",
                    actual = "unsupported Plane3D colormap attribute",
                ),
                location = location,
            )
        }
        val minimum = when (
            val result = plane3DColormapRange(
                attributes = colormap,
                name = "min",
                default = doubleArrayOf(
                    defaults.colormap.minimumHeight,
                    defaults.colormap.minimumHue,
                ),
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val maximum = when (
            val result = plane3DColormapRange(
                attributes = colormap,
                name = "max",
                default = doubleArrayOf(
                    defaults.colormap.maximumHeight,
                    defaults.colormap.maximumHue,
                ),
                location = location,
                creatorName = creatorName,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val saturation = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = colormap,
                name = "s",
                default = defaults.colormap.saturation,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val value = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = colormap,
                name = "v",
                default = defaults.colormap.value,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            Plane3DSurfaceAttributes(
                tiling = tiling,
                stepsU = stepsU,
                stepsV = stepsV,
                fillColorArray = fillColors,
                faceAttributes = faceAttributes,
                colormap = Plane3DColormapAttributes(
                    minimumHeight = minimum[0],
                    minimumHue = minimum[1],
                    maximumHeight = maximum[0],
                    maximumHue = maximum[1],
                    saturation = saturation,
                    value = value,
                ),
            ),
        )
    }

    private fun plane3DColormapRange(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: DoubleArray,
        location: JessieCodeAstLocation,
        creatorName: String = "plane3d",
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        val values = (value as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return invalidAttribute(
                creatorName = creatorName,
                attribute = "colormap.$name",
                expected = "array of two numbers",
                actual = value,
                location = location,
            )
        if (values.size != 2) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "colormap.$name",
                    actual = "array of ${values.size} values",
                ),
                location = location,
            )
        }
        val result = DoubleArray(2)
        for (index in values.indices) {
            val number =
                (values[index] as? JessieCodeRuntimeValue.NumberValue)
                    ?.value
            if (number == null || !number.isFinite()) {
                return invalidAttribute(
                    creatorName = creatorName,
                    attribute = "colormap.$name",
                    expected = "array of two finite numbers",
                    actual = values[index],
                    location = location,
                )
            }
            result[index] = number
        }
        return GMResult.Ok(result)
    }

    private fun plane3DDirection(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<ParsedPlane3DDirection, JessieCodeRuntimeError> =
        when (
            val result = line3DDirection(
                board = board,
                value = value,
                location = location,
                creatorName = "plane3d",
            )
        ) {
            is GMResult.Err -> result
            is GMResult.Ok -> GMResult.Ok(
                ParsedPlane3DDirection(
                    source = when (val source = result.value.source) {
                        is Line3DDirectionSource.Points ->
                            Plane3DDirectionSource.Points(
                                source.point1,
                                source.point2,
                            )
                        is Line3DDirectionSource.Line ->
                            Plane3DDirectionSource.Line(source.line)
                        is Line3DDirectionSource.Values ->
                            Plane3DDirectionSource.Values(source.values)
                        is Line3DDirectionSource.Function ->
                            Plane3DDirectionSource.Function(source.evaluator)
                    },
                    dependencies = result.value.dependencies,
                ),
            )
        }

    private fun defaultPlane3DRange(): ParsedLine3DValues =
        ParsedLine3DValues(
            values = listOf(
                Line3DCoordinateValue.Numeric(Double.NEGATIVE_INFINITY),
                Line3DCoordinateValue.Numeric(Double.POSITIVE_INFINITY),
            ),
            dependencies = emptyList(),
        )

    private fun plane3DResult(
        result: GMResult<Plane3D, Plane3DError>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "plane3d",
                error = JessieCodeCreatorError.Plane3DFactory(
                    result.error,
                ),
                location = location,
            )
        }

    private fun createPolePoint(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "polepoint"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val first = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        if (
            !(
                (first is Circle && second is Line) ||
                    (first is Line && second is Circle)
            )
        ) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        return when (
            val result = PolePoint.create(
                board = resolvedBoard,
                firstParent = first,
                secondParent = second,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.PolePointFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun createIntersection(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "intersection"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 2..4) {
            return unsupported(creatorName, parents, location)
        }
        val first = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        val firstIndex = intersectionIndexParent(
            value = parents.getOrNull(2),
            location = location,
        ) ?: return unsupported(creatorName, parents, location)
        val secondIndex = intersectionIndexParent(
            value = parents.getOrNull(3),
            location = location,
        ) ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val alwaysIntersect = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "alwaysintersect",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = IntersectionPoint.create(
                board = resolvedBoard,
                first = first,
                second = second,
                firstIndex = firstIndex,
                secondIndex = secondIndex,
                alwaysIntersect = alwaysIntersect,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.IntersectionFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun createOtherIntersection(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "otherintersection"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val first = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        val excludedValues =
            (parents[2] as? JessieCodeRuntimeValue.ArrayValue)
                ?.values
                ?: listOf(parents[2])
        val excludedPoints = mutableListOf<Point>()
        for (value in excludedValues) {
            val point = resolveElement(resolvedBoard, value) as? Point
                ?: return unsupported(creatorName, parents, location)
            excludedPoints += point
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val alwaysIntersect = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "alwaysintersect",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val precision = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "precision",
                default = 0.001,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = OtherIntersectionPoint.create(
                board = resolvedBoard,
                first = first,
                second = second,
                excludedPoints = excludedPoints,
                alwaysIntersect = alwaysIntersect,
                precision = precision,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.IntersectionFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    // JSXGraph: src/base/transformation.js -> createTransform3D / setMatrix3D.
    private fun createTransform3D(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "transform3d"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (
            parents.isEmpty() ||
            resolveElement(resolvedBoard, parents[0]) !is View3D
        ) {
            return unsupported(creatorName, parents, location)
        }
        val type = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "type",
                default = "",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parameters = parents.drop(1)
        val transformation =
            if (type == "matrix" || type == "affinematrix") {
                createTransform3DMatrix(
                    board = resolvedBoard,
                    type = type,
                    parameters = parameters,
                    location = location,
                )
            } else {
                val resolved = mutableListOf<Transformation3DParameter>()
                for ((index, value) in parameters.withIndex()) {
                    val vector =
                        type == "rotate" && index in 1..2 ||
                            type in setOf(
                                "rotateX",
                                "rotateY",
                                "rotateZ",
                            ) && index == 1
                    val parameter = when (
                        val result = transformation3DParameter(
                            value = value,
                            vector = vector,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
                    resolved += parameter
                }
                Transformation.create3D(
                    board = resolvedBoard,
                    type = type,
                    parameters = resolved,
                )
            }
        return transformationReference(
            result = transformation,
            location = location,
            creatorName = creatorName,
        )
    }

    private fun createTransform3DMatrix(
        board: Board,
        type: String,
        parameters: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<Transformation, TransformationError> {
        val rows = (
            parameters.singleOrNull() as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return GMResult.Err(
            TransformationError.InvalidParameterForm(
                transformationType = type,
                expectedForm = "one nested scalar matrix",
            ),
        )
        val matrix = mutableListOf<List<TransformationParameter>>()
        for (row in rows) {
            val values = (
                row as? JessieCodeRuntimeValue.ArrayValue
                )?.values ?: return GMResult.Err(
                TransformationError.InvalidParameterForm(
                    transformationType = type,
                    expectedForm = "one nested scalar matrix",
                ),
            )
            val converted = transformationParameters(
                parents = values,
                location = location,
            ) ?: return GMResult.Err(
                TransformationError.InvalidParameterForm(
                    transformationType = type,
                    expectedForm = "one nested scalar matrix",
                ),
            )
            matrix += converted
        }
        return Transformation.create3DMatrix(
            board = board,
            type = type,
            matrix = matrix,
        )
    }

    private fun transformation3DParameter(
        value: JessieCodeRuntimeValue,
        vector: Boolean,
        location: JessieCodeAstLocation,
    ): GMResult<Transformation3DParameter, JessieCodeRuntimeError> {
        if (!vector) {
            val scalar = transformationParameter(value, location)
                ?: return invalidAttribute(
                    creatorName = "transform3d",
                    attribute = "parents",
                    expected = "scalar transformation parameters",
                    actual = value,
                    location = location,
                )
            return GMResult.Ok(
                Transformation3DParameter.Scalar(scalar),
            )
        }
        return when (value) {
            is JessieCodeRuntimeValue.ArrayValue -> {
                val static = numericRuntimeArray(value.values)
                if (static != null) {
                    GMResult.Ok(
                        Transformation3DParameter.Vector(static),
                    )
                } else {
                    GMResult.Ok(
                        Transformation3DParameter.DynamicVector(
                            TransformationDynamicVectorParameter {
                                evaluateDynamicVector(
                                    values = value.values,
                                    location = location,
                                )
                            },
                        ),
                    )
                }
            }
            is JessieCodeRuntimeValue.FunctionValue ->
                GMResult.Ok(
                    Transformation3DParameter.DynamicVector(
                        TransformationDynamicVectorParameter {
                            evaluateDynamicVector(
                                function = value,
                                location = location,
                            )
                        },
                    ),
                )
            is JessieCodeRuntimeValue.ElementReference -> {
                val point = value.element as? Point3D
                    ?: return invalidAttribute(
                        creatorName = "transform3d",
                        attribute = "parents",
                        expected = "3D vector or Point3D",
                        actual = value,
                        location = location,
                    )
                GMResult.Ok(
                    Transformation3DParameter.DynamicVector(
                        TransformationDynamicVectorParameter {
                            GMResult.Ok(point.coords.copyOf())
                        },
                    ),
                )
            }
            is JessieCodeRuntimeValue.StringValue ->
                GMResult.Ok(
                    Transformation3DParameter.VectorExpression(
                        value.value,
                    ),
                )
            else -> invalidAttribute(
                creatorName = "transform3d",
                attribute = "parents",
                expected = "3D vector or Point3D",
                actual = value,
                location = location,
            )
        }
    }

    private fun numericRuntimeArray(
        values: List<JessieCodeRuntimeValue>,
    ): DoubleArray? {
        val result = DoubleArray(values.size)
        for ((index, value) in values.withIndex()) {
            result[index] = (
                value as? JessieCodeRuntimeValue.NumberValue
                )?.value ?: return null
        }
        return result
    }

    private fun evaluateDynamicVector(
        values: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, TransformationDynamicParameterError> {
        val result = DoubleArray(values.size)
        for ((index, value) in values.withIndex()) {
            result[index] = when (value) {
                is JessieCodeRuntimeValue.NumberValue -> value.value
                is JessieCodeRuntimeValue.FunctionValue -> when (
                    val evaluated = value.externalCallable.call(
                        arguments = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Err -> return GMResult.Err(
                        TransformationDynamicParameterError.Rejected(
                            evaluated.error.toString(),
                        ),
                    )
                    is GMResult.Ok -> (
                        evaluated.value as?
                            JessieCodeRuntimeValue.NumberValue
                        )?.value ?: return GMResult.Err(
                        TransformationDynamicParameterError.Rejected(
                            "Expected number at vector index $index, got " +
                                typeName(evaluated.value),
                        ),
                    )
                }
                else -> return GMResult.Err(
                    TransformationDynamicParameterError.Rejected(
                        "Expected number or function at vector index " +
                            "$index, got ${typeName(value)}",
                    ),
                )
            }
        }
        return GMResult.Ok(result)
    }

    private fun evaluateDynamicVector(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, TransformationDynamicParameterError> =
        when (
            val result = function.externalCallable.call(
                arguments = emptyList(),
                location = location,
            )
        ) {
            is GMResult.Err -> GMResult.Err(
                TransformationDynamicParameterError.Rejected(
                    result.error.toString(),
                ),
            )
            is GMResult.Ok -> {
                val values = (
                    result.value as?
                        JessieCodeRuntimeValue.ArrayValue
                    )?.values
                val vector = values?.let(::numericRuntimeArray)
                if (vector != null) {
                    GMResult.Ok(vector)
                } else {
                    GMResult.Err(
                        TransformationDynamicParameterError.Rejected(
                            "Expected numeric vector, got " +
                                typeName(result.value),
                        ),
                    )
                }
            }
        }

    // JSXGraph: src/base/transformation.js -> createTransform / setMatrix
    private fun createTransform(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "transform"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val type = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "type",
                default = "",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val transformation = when (type) {
            "reflect" -> createReflectionTransform(
                board = resolvedBoard,
                parents = parents,
                location = location,
            )
            "rotate" -> createRotationTransform(
                board = resolvedBoard,
                parents = parents,
                location = location,
            )
            "affinematrix",
            "matrix",
            -> createMatrixTransform(
                board = resolvedBoard,
                type = type,
                parents = parents,
                location = location,
            )
            else -> {
                val parameters = transformationParameters(
                    parents = parents,
                    location = location,
                ) ?: return unsupported(
                    creatorName,
                    parents,
                    location,
                )
                Transformation.create(
                    board = resolvedBoard,
                    type = type,
                    parameters = parameters,
                )
            }
        }
        return transformationReference(
            result = transformation,
            location = location,
        )
    }

    private fun createReflectionTransform(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<Transformation, TransformationError> {
        if (parents.size == 1) {
            val line = resolveElement(board, parents[0]) as? Line
            if (line != null) {
                return GMResult.Ok(
                    Transformation.createReflectionFromLine(line),
                )
            }
        }
        if (parents.size == 2) {
            val first =
                resolveElement(board, parents[0]) as? CoordsElement
            val second =
                resolveElement(board, parents[1]) as? CoordsElement
            if (first != null && second != null) {
                return GMResult.Ok(
                    Transformation.createReflectionFromPoints(
                        first = first,
                        second = second,
                    ),
                )
            }
        }
        val parameters = transformationParameters(
            parents = parents,
            location = location,
        ) ?: return GMResult.Err(
            TransformationError.InvalidParameterForm(
                transformationType = "reflect",
                expectedForm =
                    "one line, two coordinate elements, or four scalars",
            ),
        )
        return Transformation.create(
            board = board,
            type = "reflect",
            parameters = parameters,
        )
    }

    private fun createRotationTransform(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<Transformation, TransformationError> {
        if (parents.size == 2) {
            val angle = transformationParameter(
                value = parents[0],
                location = location,
            )
            val center =
                resolveElement(board, parents[1]) as? CoordsElement
            if (angle != null && center != null) {
                return Transformation.createRotation(
                    board = board,
                    angle = angle,
                    center = center,
                )
            }
            val centerCoordinates = (
                parents[1] as? JessieCodeRuntimeValue.ArrayValue
            )?.values
            if (
                angle != null &&
                centerCoordinates?.size == 2 &&
                centerCoordinates.all {
                    it is JessieCodeRuntimeValue.NumberValue
                }
            ) {
                val x = (
                    centerCoordinates[0] as
                        JessieCodeRuntimeValue.NumberValue
                    ).value
                val y = (
                    centerCoordinates[1] as
                        JessieCodeRuntimeValue.NumberValue
                    ).value
                return Transformation.createRotationAroundCoordinates(
                    board = board,
                    angle = angle,
                    center = doubleArrayOf(x, y),
                )
            }
        }
        val parameters = transformationParameters(
            parents = parents,
            location = location,
        ) ?: return GMResult.Err(
            TransformationError.InvalidParameterForm(
                transformationType = "rotate",
                expectedForm =
                    "angle, angle plus center, or angle plus x and y",
            ),
        )
        return Transformation.create(
            board = board,
            type = "rotate",
            parameters = parameters,
        )
    }

    private fun createMatrixTransform(
        board: Board,
        type: String,
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<Transformation, TransformationError> {
        val rows = (
            parents.singleOrNull() as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values
            ?: return GMResult.Err(
                TransformationError.InvalidParameterForm(
                    transformationType = type,
                    expectedForm = "one nested scalar matrix",
                ),
            )
        val matrix = mutableListOf<List<TransformationParameter>>()
        for (row in rows) {
            val values = (
                row as? JessieCodeRuntimeValue.ArrayValue
                )?.values
                ?: return GMResult.Err(
                    TransformationError.InvalidParameterForm(
                        transformationType = type,
                        expectedForm = "one nested scalar matrix",
                    ),
                )
            val parameters = transformationParameters(
                parents = values,
                location = location,
            ) ?: return GMResult.Err(
                TransformationError.InvalidParameterForm(
                    transformationType = type,
                    expectedForm = "one nested scalar matrix",
                ),
            )
            matrix += parameters
        }
        return Transformation.createMatrix(
            board = board,
            type = type,
            matrix = matrix,
        )
    }

    private fun transformationParameters(
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): List<TransformationParameter>? {
        val parameters = mutableListOf<TransformationParameter>()
        for (parent in parents) {
            val parameter = transformationParameter(parent, location)
                ?: return null
            parameters += parameter
        }
        return parameters
    }

    private fun transformationParameter(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): TransformationParameter? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                TransformationParameter.Numeric(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                TransformationParameter.Expression(value.value)
            is JessieCodeRuntimeValue.FunctionValue ->
                TransformationParameter.Dynamic(
                    TransformationDynamicParameter {
                        when (
                            val result = value.externalCallable.call(
                                arguments = emptyList(),
                                location = location,
                            )
                        ) {
                            is GMResult.Err -> GMResult.Err(
                                TransformationDynamicParameterError.Rejected(
                                    reason = result.error.toString(),
                                ),
                            )
                            is GMResult.Ok -> {
                                val number = result.value as?
                                    JessieCodeRuntimeValue.NumberValue
                                if (number != null) {
                                    GMResult.Ok(number.value)
                                } else {
                                    GMResult.Err(
                                        TransformationDynamicParameterError
                                            .Rejected(
                                                reason =
                                                    "Expected number, got " +
                                                        typeName(result.value),
                                            ),
                                    )
                                }
                            }
                        }
                    },
                )
            else -> null
        }

    private fun transformationReferences(
        value: JessieCodeRuntimeValue,
    ): List<Transformation>? {
        return when (value) {
            is JessieCodeRuntimeValue.TransformationReference ->
                listOf(value.transformation)
            is JessieCodeRuntimeValue.ArrayValue -> {
                if (value.values.isEmpty()) {
                    return null
                }
                val transformations = mutableListOf<Transformation>()
                for (item in value.values) {
                    val reference = item as?
                        JessieCodeRuntimeValue.TransformationReference
                        ?: return null
                    transformations += reference.transformation
                }
                transformations
            }
            else -> null
        }
    }

    private fun transformationReference(
        result: GMResult<Transformation, TransformationError>,
        location: JessieCodeAstLocation,
        creatorName: String = "transform",
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.TransformationReference(
                    result.value,
                ),
            )
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.TransformationFactory(
                    result.error,
                ),
                location = location,
            )
        }

    private fun createMidpoint(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "midpoint"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when {
            parents.size == 1 -> {
                val line = resolveElement(resolvedBoard, parents[0]) as? Line
                    ?: return unsupported(creatorName, parents, location)
                MaterializedPointParents(
                    points = listOf(line.point1, line.point2),
                    ownedPoints = emptySet(),
                )
            }

            parents.size == 2 -> when (
                val result = materializePointParents(
                    board = resolvedBoard,
                    parents = parents,
                    creatorName = creatorName,
                    expectedCount = 2,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }

            else -> return unsupported(creatorName, parents, location)
        }

        val midpoint = MidpointPoint.create(
            board = resolvedBoard,
            point1 = materialized.points[0],
            point2 = materialized.points[1],
            ownedPoints = materialized.ownedPoints,
            id = identity.id,
            name = identity.name,
            needsRegularUpdate = identity.needsRegularUpdate,
            fixed = fixed,
        )
        return when (midpoint) {
            is GMResult.Ok -> element(midpoint.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.MidpointFactory(
                        midpoint.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createCircumcenter(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializeThreePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        return when (
            val result = CircumcenterPoint.create(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.CircumcenterFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createBisector(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "bisector"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializeThreePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        return when (
            val result = BisectorLine.create(
                board = resolvedBoard,
                point1 = points[0],
                vertex = points[1],
                point3 = points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.TriangleCenterFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createBisectorLines(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "bisectorlines"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val first = resolveElement(resolvedBoard, parents[0]) as? Line
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(resolvedBoard, parents[1]) as? Line
            ?: return unsupported(creatorName, parents, location)
        val line1Attributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "line1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line2Attributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "line2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val firstIdentity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = line1Attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val secondIdentity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = line2Attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        return when (
            val result = BisectorLines.create(
                board = resolvedBoard,
                first = first,
                second = second,
                line1Attributes = firstIdentity.toBisectorLineAttributes(),
                line2Attributes = secondIdentity.toBisectorLineAttributes(),
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.CompositionReference(result.value),
            )
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.BisectorLinesFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun createIncenter(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "incenter"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializeThreePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        return when (
            val result = IncenterPoint.create(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.TriangleCenterFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createIncircle(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "incircle"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializeThreePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        return when (
            val result = IncircleCircle.create(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.TriangleCenterFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createOrthogonalPoint(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        kind: OrthogonalPointKind,
    ): CreatorResult {
        val creatorName = kind.elementType
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializePointLineParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = OrthogonalPoint.create(
                board = resolvedBoard,
                point = materialized.point,
                line = materialized.line,
                kind = kind,
                ownsPoint = materialized.ownsPoint,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                cleanupOwnedPoint(resolvedBoard, materialized)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.OrthogonalFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createParallelPoint(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "parallelpoint"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val materialized = if (parents.size == 3) {
            when (
                val result = materializePointParents(
                    board = resolvedBoard,
                    parents = parents,
                    creatorName = creatorName,
                    expectedCount = 3,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            when (
                val result = materializePointLineParents(
                    board = resolvedBoard,
                    parents = parents,
                    creatorName = creatorName,
                    location = location,
                )
            ) {
                is GMResult.Ok -> MaterializedPointParents(
                    points = listOf(
                        result.value.line.point1,
                        result.value.line.point2,
                        result.value.point,
                    ),
                    ownedPoints =
                        if (result.value.ownsPoint) {
                            setOf(result.value.point)
                        } else {
                            emptySet()
                        },
                )
                is GMResult.Err -> return result
            }
        }
        val result = ParallelPoint.create(
            board = resolvedBoard,
            point1 = materialized.points[0],
            point2 = materialized.points[1],
            point3 = materialized.points[2],
            ownedPoints = materialized.ownedPoints,
            id = identity.id,
            name = identity.name,
            needsRegularUpdate = identity.needsRegularUpdate,
            fixed = fixed,
        )
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.ParallelFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createParallel(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String = "parallel",
        elementType: String = "parallel",
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        if (parents.size == 3) {
            val materialized = when (
                val result = materializePointParents(
                    board = resolvedBoard,
                    parents = parents,
                    creatorName = creatorName,
                    expectedCount = 3,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val result = ParallelLine.create(
                board = resolvedBoard,
                point1 = materialized.points[0],
                point2 = materialized.points[1],
                throughPoint = materialized.points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
            return when (result) {
                is GMResult.Ok -> element(
                    configureArrowWrapper(result.value, elementType),
                )
                is GMResult.Err -> {
                    resolvedBoard.removeObjects(materialized.ownedPoints)
                    failure(
                        creatorName = creatorName,
                        error = JessieCodeCreatorError.ParallelFactory(
                            result.error,
                        ),
                        location = location,
                    )
                }
            }
        }

        val materialized = when (
            val result = materializePointLineParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lineIsFirst =
            resolveElement(resolvedBoard, parents[0]) === materialized.line
        val parentMetadata =
            if (lineIsFirst) {
                listOf(materialized.line, materialized.point)
            } else {
                listOf(materialized.point, materialized.line)
            }
        val result = ParallelLine.create(
            board = resolvedBoard,
            sourceLine = materialized.line,
            throughPoint = materialized.point,
            ownsThroughPoint = materialized.ownsPoint,
            parentMetadata = parentMetadata,
            id = identity.id,
            name = identity.name,
            needsRegularUpdate = identity.needsRegularUpdate,
        )
        return when (result) {
            is GMResult.Ok -> element(
                configureArrowWrapper(result.value, elementType),
            )
            is GMResult.Err -> {
                cleanupOwnedPoint(resolvedBoard, materialized)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.ParallelFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createPerpendicular(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "perpendicular"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializePointLineParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = PerpendicularLine.create(
                board = resolvedBoard,
                line = materialized.line,
                point = materialized.point,
                ownsPoint = materialized.ownsPoint,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                cleanupOwnedPoint(resolvedBoard, materialized)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.OrthogonalFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createPerpendicularSegment(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "perpendicularsegment"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializePointLineParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = PerpendicularSegmentLine.create(
                board = resolvedBoard,
                line = materialized.line,
                sourcePoint = materialized.point,
                ownsSourcePoint = materialized.ownsPoint,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                cleanupOwnedPoint(resolvedBoard, materialized)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.OrthogonalFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/base/line.js -> JXG.createAxis;
    // src/options.js -> axis.
    private fun createAxis(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "axis"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
                defaultNeedsRegularUpdate = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                expectedCount = 2,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val position = stringAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "position",
            default = "static",
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }.lowercase()
        val anchor = stringAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "anchor",
            default = "",
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }.lowercase()
        val anchorDist = axisDistanceAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "anchordist",
            default = AxisDistance.Percent(10.0),
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val ticksAutoPos = booleanAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "ticksautopos",
            default = false,
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val ticksAutoPosThreshold = axisDistanceAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "ticksautoposthreshold",
            default = AxisDistance.Percent(5.0),
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val straightFirst = booleanAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "straightfirst",
            default = true,
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val straightLast = booleanAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "straightlast",
            default = true,
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val ticksInput = nestedObjectAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "ticks",
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val mergedTicks = axisTicksAttributes(
            ticksAttributes = ticksInput,
        )
        val ticksIdentity = creatorAttributes(
            creatorName = creatorName,
            attributes = mergedTicks,
            location = location,
            defaultNeedsRegularUpdate = false,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val parsedTicksAttributes = ticksAttributes(
            attributes = mergedTicks,
            location = location,
        ).valueOrReturn {
            resolvedBoard.removeObjects(materialized.ownedPoints)
            return it
        }
        val ticksSource =
            if (
                ticksInput.properties["ticksdistance"] ===
                JessieCodeRuntimeValue.UndefinedValue
            ) {
                when (val ticks = ticksInput.properties["ticks"]) {
                    null,
                    JessieCodeRuntimeValue.UndefinedValue,
                    -> TicksSource.Equidistant
                    is JessieCodeRuntimeValue.ArrayValue -> {
                        val values = numericArray(ticks)
                            ?: run {
                                resolvedBoard.removeObjects(
                                    materialized.ownedPoints,
                                )
                                return invalidAttribute(
                                    creatorName = creatorName,
                                    attribute = "ticks.ticks",
                                    expected = "an array of finite numbers",
                                    actual = ticks,
                                    location = location,
                                )
                            }
                        TicksSource.Fixed(values)
                    }
                    else -> {
                        resolvedBoard.removeObjects(materialized.ownedPoints)
                        return invalidAttribute(
                            creatorName = creatorName,
                            attribute = "ticks.ticks",
                            expected = "an array of finite numbers",
                            actual = ticks,
                            location = location,
                        )
                    }
                }
            } else {
                // options.axis.ticks.ticksDistance defaults to 1.0, so the
                // upstream existence check wins over a ticks array unless
                // callers explicitly assign undefined.
                TicksSource.Equidistant
            }
        val points = materialized.points
        return when (
            val result = Axis.create(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                attributes = AxisAttributes(
                    position = position,
                    anchor = anchor,
                    anchorDist = anchorDist,
                    ticksAutoPos = ticksAutoPos,
                    ticksAutoPosThreshold = ticksAutoPosThreshold,
                ),
                ticksSource = ticksSource,
                ticksAttributes = parsedTicksAttributes,
                id = identity.id,
                name = identity.name ?: "",
                needsRegularUpdate = identity.needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
                ticksId = ticksIdentity.id,
                ticksName = ticksIdentity.name,
                ticksNeedsRegularUpdate =
                    ticksIdentity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.AxisFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun axisTicksAttributes(
        ticksAttributes: JessieCodeRuntimeValue.ObjectValue,
    ): JessieCodeRuntimeValue.ObjectValue {
        val label = (
            ticksAttributes.properties["label"] as?
                JessieCodeRuntimeValue.ObjectValue
            )
        val mergedLabel =
            linkedMapOf<String, JessieCodeRuntimeValue>(
                "offset" to JessieCodeRuntimeValue.ArrayValue(
                    mutableListOf(
                        JessieCodeRuntimeValue.NumberValue(4.0),
                        JessieCodeRuntimeValue.NumberValue(-9.0),
                    ),
                ),
                "visible" to
                    JessieCodeRuntimeValue.StringValue("inherit"),
                "needsregularupdate" to
                    JessieCodeRuntimeValue.BooleanValue(false),
                "layer" to JessieCodeRuntimeValue.NumberValue(9.0),
            ).apply {
                putAll(label?.properties.orEmpty())
            }
        val merged =
            linkedMapOf<String, JessieCodeRuntimeValue>(
                "visible" to
                    JessieCodeRuntimeValue.StringValue("inherit"),
                "needsregularupdate" to
                    JessieCodeRuntimeValue.BooleanValue(false),
                "strokewidth" to
                    JessieCodeRuntimeValue.NumberValue(1.0),
                "strokecolor" to
                    JessieCodeRuntimeValue.StringValue("#666666"),
                "drawlabels" to
                    JessieCodeRuntimeValue.BooleanValue(true),
                "drawzero" to
                    JessieCodeRuntimeValue.BooleanValue(false),
                "insertticks" to
                    JessieCodeRuntimeValue.BooleanValue(true),
                "minticksdistance" to
                    JessieCodeRuntimeValue.NumberValue(5.0),
                "minorheight" to
                    JessieCodeRuntimeValue.NumberValue(10.0),
                "majorheight" to
                    JessieCodeRuntimeValue.NumberValue(-1.0),
                "tickendings" to numericRuntimeArray(0.0, 1.0),
                "majortickendings" to numericRuntimeArray(1.0, 1.0),
                "minorticks" to
                    JessieCodeRuntimeValue.NumberValue(4.0),
                "ticksdistance" to
                    JessieCodeRuntimeValue.NumberValue(1.0),
                "strokeopacity" to
                    JessieCodeRuntimeValue.NumberValue(0.25),
            ).apply {
                putAll(ticksAttributes.properties)
                this["label"] =
                    JessieCodeRuntimeValue.ObjectValue(mergedLabel)
            }
        return JessieCodeRuntimeValue.ObjectValue(merged)
    }

    private fun numericRuntimeArray(
        first: Double,
        second: Double,
    ): JessieCodeRuntimeValue.ArrayValue =
        JessieCodeRuntimeValue.ArrayValue(
            mutableListOf(
                JessieCodeRuntimeValue.NumberValue(first),
                JessieCodeRuntimeValue.NumberValue(second),
            ),
        )

    private fun axisDistanceAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: AxisDistance,
        location: JessieCodeAstLocation,
    ): GMResult<AxisDistance, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        val parsed = when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                value.value.takeIf(Double::isFinite)
                    ?.let(AxisDistance::User)
            is JessieCodeRuntimeValue.StringValue ->
                parseAxisDistance(value.value)
            else -> null
        }
        return parsed?.let { GMResult.Ok(it) }
            ?: invalidAttribute(
                creatorName = creatorName,
                attribute = name,
                expected =
                    "a finite number or a numeric %, fr, or px string",
                actual = value,
                location = location,
            )
    }

    private fun parseAxisDistance(value: String): AxisDistance? {
        val match = AXIS_DISTANCE_PATTERN.find(value) ?: return null
        val number = match.groupValues[1].toDoubleOrNull()
            ?.takeIf(Double::isFinite) ?: return null
        return when (match.groupValues[2].lowercase()) {
            "%" -> AxisDistance.Percent(number)
            "fr" -> AxisDistance.Fraction(number)
            "px" -> AxisDistance.Pixels(number)
            else -> AxisDistance.User(number)
        }
    }

    private fun createLine(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String = "line",
        supportsCoefficients: Boolean = true,
        elementType: String = "line",
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val isSegment = elementType == "segment"
        val isArrow = elementType == "arrow"
        val straightFirst =
            if (isSegment || isArrow) {
                false
            } else {
                when (
                    val result = booleanAttribute(
                        creatorName = creatorName,
                        attributes = attributes,
                        name = "straightfirst",
                        default = true,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        val straightLast =
            if (isSegment || isArrow) {
                false
            } else {
                when (
                    val result = booleanAttribute(
                        creatorName = creatorName,
                        attributes = attributes,
                        name = "straightlast",
                        default = true,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
        val nonnegativeOnly =
            if (isSegment) {
                when (
                    val result = booleanAttribute(
                        creatorName = creatorName,
                        attributes = attributes,
                        name = "nonnegativeonly",
                        default = false,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                false
            }
        val fixedLength =
            if (isSegment && parents.size == 3) {
                segmentLengthParent(parents[2])
                    ?: return unsupported(creatorName, parents, location)
            } else {
                null
            }
        val twoPointParents = when {
            isSegment && parents.size in 2..3 -> parents.take(2)
            !isSegment && parents.size == 2 -> parents
            else -> null
        }

        val materialized = when {
            twoPointParents != null -> {
                when (
                    val result = materializePointParents(
                        board = resolvedBoard,
                        parents = twoPointParents,
                        creatorName = creatorName,
                        expectedCount = 2,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            }
            supportsCoefficients &&
                parents.size == 3 &&
                parents.all { it is JessieCodeRuntimeValue.NumberValue } -> {
                when (
                    val result = coefficientPoints(resolvedBoard, parents)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return failure(
                        creatorName,
                        JessieCodeCreatorError.PointFactory(result.error),
                        location,
                    )
                }
            }
            else -> return unsupported(creatorName, parents, location)
        }

        val points = materialized.points
        // JSXGraph: src/base/line.js -> createLine / createSegment
        val lineResult =
            if (isSegment) {
                createSegment(
                    board = resolvedBoard,
                    point1 = points[0],
                    point2 = points[1],
                    fixedLength = fixedLength,
                    nonnegativeOnly = nonnegativeOnly,
                    attributes = identity,
                    location = location,
                )
            } else {
                Line.create(
                    board = resolvedBoard,
                    point1 = points[0],
                    point2 = points[1],
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )
            }
        return when (
            val result = lineResult
        ) {
            is GMResult.Ok -> {
                result.value.configureVisibleRange(
                    straightFirst = straightFirst,
                    straightLast = straightLast,
                )
                element(
                    configureArrowWrapper(result.value, elementType),
                )
            }
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.LineFactory(result.error),
                    location = location,
                )
            }
        }
    }

    // JSXGraph 1.13.3:
    // src/base/line.js -> createArrow;
    // src/element/composition.js -> createArrowParallel.
    private fun configureArrowWrapper(
        line: Line,
        elementType: String,
    ): Line {
        if (elementType == "arrow" || elementType == "arrowparallel") {
            line.type = Const.OBJECT_TYPE_VECTOR
            line.elType = elementType
            line.configureVisibleRange(
                straightFirst = false,
                straightLast = false,
            )
        }
        return line
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> createTicks / Ticks.
    private fun createTicks(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "ticks"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 1..2) {
            return unsupported(creatorName, parents, location)
        }
        val parent = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        if (parent !is Line && parent !is Curve) {
            return unsupported(creatorName, parents, location)
        }
        val source = when (val value = parents.getOrNull(1)) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            is JessieCodeRuntimeValue.NumberValue,
            -> TicksSource.Equidistant
            is JessieCodeRuntimeValue.ArrayValue -> {
                val values = numericArray(value)
                    ?: return unsupported(creatorName, parents, location)
                TicksSource.Fixed(values)
            }
            is JessieCodeRuntimeValue.FunctionValue ->
                return failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.TicksFactory(
                        TicksError.FunctionArgumentsNoLongerSupported,
                    ),
                    location = location,
                )
            else -> return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val ticksAttributes = when (
            val result = ticksAttributes(
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Ticks.create(
                board = resolvedBoard,
                parent = parent,
                source = source,
                attributes = ticksAttributes,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.TicksFactory(result.error),
                location = location,
            )
        }
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> createHatchmark.
    private fun createHatch(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val parent = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        if (parent !is Line && parent !is Curve) {
            return unsupported(creatorName, parents, location)
        }
        val numberOfHashes = (
            parents[1] as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val mergedAttributes =
            JessieCodeRuntimeValue.ObjectValue(
                linkedMapOf<String, JessieCodeRuntimeValue>(
                    "anchor" to
                        JessieCodeRuntimeValue.StringValue("middle"),
                    "drawzero" to
                        JessieCodeRuntimeValue.BooleanValue(true),
                    "majorheight" to
                        JessieCodeRuntimeValue.NumberValue(20.0),
                    "ticksdistance" to
                        JessieCodeRuntimeValue.NumberValue(0.2),
                ).apply {
                    putAll(attributes.properties)
                },
            )
        val hatchAttributes = when (
            val result = ticksAttributes(
                attributes = mergedAttributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Hatch.create(
                board = resolvedBoard,
                parent = parent,
                numberOfHashes = numberOfHashes,
                attributes = hatchAttributes,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.TicksFactory(result.error),
                location = location,
            )
        }
    }

    private fun ticksAttributes(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<TicksAttributes, JessieCodeRuntimeError> {
        val creatorName = "ticks"
        val anchor = when (val value = attributes.properties["anchor"]) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> TicksAnchor.Left
            is JessieCodeRuntimeValue.NumberValue ->
                if (value.value.isFinite()) {
                    TicksAnchor.Fraction(value.value)
                } else {
                    return invalidAttribute(
                        creatorName,
                        "anchor",
                        "left, right, middle, or a finite number",
                        value,
                        location,
                    )
                }
            is JessieCodeRuntimeValue.StringValue ->
                when (value.value.lowercase()) {
                    "left" -> TicksAnchor.Left
                    "right" -> TicksAnchor.Right
                    "middle" -> TicksAnchor.Middle
                    else -> return failure(
                        creatorName = creatorName,
                        error =
                            JessieCodeCreatorError
                                .UnsupportedAttributeValue(
                                    attribute = "anchor",
                                    actual = value.value,
                                ),
                        location = location,
                    )
                }
            else -> return invalidAttribute(
                creatorName,
                "anchor",
                "left, right, middle, or a finite number",
                value,
                location,
            )
        }
        val tickEndings = when (
            val result = ticksPairAttribute(
                attributes = attributes,
                name = "tickendings",
                default = doubleArrayOf(1.0, 1.0),
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val majorTickEndings = when (
            val result = ticksPairAttribute(
                attributes = attributes,
                name = "majortickendings",
                default = doubleArrayOf(1.0, 1.0),
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val ticksPerLabel = when (
            val value = attributes.properties["ticksperlabel"]
        ) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> null
            is JessieCodeRuntimeValue.BooleanValue ->
                if (!value.value) {
                    null
                } else {
                    return invalidAttribute(
                        creatorName,
                        "ticksperlabel",
                        "false or a positive integer",
                        value,
                        location,
                    )
                }
            is JessieCodeRuntimeValue.NumberValue -> {
                val integer = value.value.toInt()
                if (
                    value.value.isFinite() &&
                    integer.toDouble() == value.value &&
                    integer > 0
                ) {
                    integer
                } else {
                    return failure(
                        creatorName = creatorName,
                        error =
                            JessieCodeCreatorError
                                .UnsupportedAttributeValue(
                                    attribute = "ticksperlabel",
                                    actual = value.value.toString(),
                                ),
                        location = location,
                    )
                }
            }
            else -> return invalidAttribute(
                creatorName,
                "ticksperlabel",
                "false or a positive integer",
                value,
                location,
            )
        }
        val labels = when (val value = attributes.properties["labels"]) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> emptyList()
            is JessieCodeRuntimeValue.ArrayValue ->
                value.values.map { item ->
                    when (item) {
                        is JessieCodeRuntimeValue.StringValue -> item.value
                        is JessieCodeRuntimeValue.NumberValue ->
                            JsNumberFormat.compact(item.value)
                        JessieCodeRuntimeValue.NullValue,
                        JessieCodeRuntimeValue.UndefinedValue,
                        -> null
                        else -> return invalidAttribute(
                            creatorName,
                            "labels",
                            "an array of strings, numbers, or null",
                            item,
                            location,
                        )
                    }
                }
            else -> return invalidAttribute(
                creatorName,
                "labels",
                "an array",
                value,
                location,
            )
        }
        val label = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "label",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val labelOffset = when (
            val result = ticksPairAttribute(
                attributes = label,
                name = "offset",
                default = doubleArrayOf(10.0, 0.0),
                location = location,
                attributePrefix = "label.",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        fun boolean(
            name: String,
            default: Boolean,
        ): GMResult<Boolean, JessieCodeRuntimeError> =
            booleanAttribute(
                creatorName,
                attributes,
                name,
                default,
                location,
            )
        fun number(
            name: String,
            default: Double,
        ): GMResult<Double, JessieCodeRuntimeError> =
            numberAttribute(
                creatorName,
                attributes,
                name,
                default,
                location,
            )
        fun integer(
            name: String,
            default: Int,
        ): GMResult<Int, JessieCodeRuntimeError> =
            integerAttribute(
                creatorName,
                attributes,
                name,
                default,
                0,
                Int.MAX_VALUE,
                location,
            )
        val drawZero = boolean("drawzero", false).valueOrReturn { return it }
        val insertTicks =
            boolean("insertticks", false).valueOrReturn { return it }
        val minTicksDistance =
            number("minticksdistance", 10.0).valueOrReturn { return it }
        val minorHeight =
            number("minorheight", 4.0).valueOrReturn { return it }
        val majorHeight =
            number("majorheight", 10.0).valueOrReturn { return it }
        val ignoreInfiniteTickEndings =
            boolean("ignoreinfinitetickendings", true)
                .valueOrReturn { return it }
        val minorTicks =
            integer("minorticks", 4).valueOrReturn { return it }
        val scale = number("scale", 1.0).valueOrReturn { return it }
        val scaleSymbol =
            stringAttribute(
                creatorName,
                attributes,
                "scalesymbol",
                "",
                location,
            ).valueOrReturn { return it }
        val maxLabelLength =
            integer("maxlabellength", 5).valueOrReturn { return it }
        val precision =
            integer("precision", 3).valueOrReturn { return it }
        val digits = integer("digits", 3).valueOrReturn { return it }
        val beautifulScientificTickLabels =
            boolean("beautifulscientificticklabels", false)
                .valueOrReturn { return it }
        val useUnicodeMinus =
            boolean("useunicodeminus", true).valueOrReturn { return it }
        val face =
            stringAttribute(
                creatorName,
                attributes,
                "face",
                "|",
                location,
            ).valueOrReturn { return it }
        val includeBoundaries =
            boolean("includeboundaries", false).valueOrReturn { return it }
        val ticksType =
            stringAttribute(
                creatorName,
                attributes,
                "type",
                "linear",
                location,
            ).valueOrReturn { return it }.lowercase()
        val ticksDistance =
            number("ticksdistance", 1.0).valueOrReturn { return it }
        val drawLabels =
            boolean("drawlabels", false).valueOrReturn { return it }
        val clip = boolean("clip", true).valueOrReturn { return it }
        val labelFontSize =
            numberAttribute(
                creatorName,
                label,
                "fontsize",
                12.0,
                location,
            ).valueOrReturn { return it }
        val labelFontUnit =
            stringAttribute(
                creatorName,
                label,
                "fontunit",
                "px",
                location,
            ).valueOrReturn { return it }.lowercase()
        if (labelFontUnit != "px") {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "label.fontunit",
                    actual = labelFontUnit,
                ),
                location = location,
            )
        }
        val labelAnchorX =
            stringAttribute(
                creatorName,
                label,
                "anchorx",
                "left",
                location,
            ).valueOrReturn { return it }.lowercase()
        if (labelAnchorX !in setOf("left", "middle", "right")) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "label.anchorx",
                    actual = labelAnchorX,
                ),
                location = location,
            )
        }
        val labelAnchorY =
            stringAttribute(
                creatorName,
                label,
                "anchory",
                "middle",
                location,
            ).valueOrReturn { return it }.lowercase()
        if (labelAnchorY !in setOf("top", "middle", "bottom")) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "label.anchory",
                    actual = labelAnchorY,
                ),
                location = location,
            )
        }
        return GMResult.Ok(
            TicksAttributes(
                anchor = anchor,
                drawZero = drawZero,
                insertTicks = insertTicks,
                minTicksDistance = minTicksDistance,
                minorHeight = minorHeight,
                majorHeight = majorHeight,
                tickEndings = tickEndings,
                majorTickEndings = majorTickEndings,
                ignoreInfiniteTickEndings = ignoreInfiniteTickEndings,
                minorTicks = minorTicks,
                ticksPerLabel = ticksPerLabel,
                scale = scale,
                scaleSymbol = scaleSymbol,
                labels = labels,
                maxLabelLength = maxLabelLength,
                precision = precision,
                digits = digits,
                beautifulScientificTickLabels =
                    beautifulScientificTickLabels,
                useUnicodeMinus = useUnicodeMinus,
                face = face,
                includeBoundaries = includeBoundaries,
                ticksType = ticksType,
                ticksDistance = ticksDistance,
                drawLabels = drawLabels,
                clip = clip,
                labelOffset = labelOffset,
                labelFontSize = labelFontSize,
                labelAnchorX = labelAnchorX,
                labelAnchorY = labelAnchorY,
            ),
        )
    }

    private fun ticksPairAttribute(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: DoubleArray,
        location: JessieCodeAstLocation,
        attributePrefix: String = "",
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default.copyOf())
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default.copyOf())
        }
        val numbers = (value as? JessieCodeRuntimeValue.ArrayValue)
            ?.let(::numericArray)
        if (
            numbers == null ||
            numbers.size != 2 ||
            numbers.any { !it.isFinite() }
        ) {
            return invalidAttribute(
                creatorName = "ticks",
                attribute = attributePrefix + name,
                expected = "an array of two finite numbers",
                actual = value,
                location = location,
            )
        }
        return GMResult.Ok(numbers)
    }

    private inline fun <T> GMResult<T, JessieCodeRuntimeError>.valueOrReturn(
        error: (GMResult.Err<JessieCodeRuntimeError>) -> Nothing,
    ): T =
        when (this) {
            is GMResult.Ok -> value
            is GMResult.Err -> error(this)
        }

    private fun createRadicalAxis(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "radicalaxis"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val circles = parents.map { parent ->
            resolveElement(resolvedBoard, parent) as? Circle
                ?: return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightFirst = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightfirst",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightLast = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightlast",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Attributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "point1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Identity = when (
            val result = creatorAttributes(
                creatorName,
                point1Attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Attributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "point2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Identity = when (
            val result = creatorAttributes(
                creatorName,
                point2Attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        return when (
            val result = RadicalAxis.create(
                board = resolvedBoard,
                circle1 = circles[0],
                circle2 = circles[1],
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
                point1Id = point1Identity.id,
                point1Name = point1Identity.name,
                point1NeedsRegularUpdate =
                    point1Identity.needsRegularUpdate,
                point2Id = point2Identity.id,
                point2Name = point2Identity.name,
                point2NeedsRegularUpdate =
                    point2Identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.RadicalAxisFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun createTangent(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
        polarLine: Boolean,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val first = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        val supportsParents =
            if (polarLine) {
                (first is Circle && second is Point) ||
                    (first is Point && second is Circle)
            } else {
                (
                    (
                        first is Circle ||
                            first is Curve ||
                            first is Line
                    ) &&
                        second is Point
                ) ||
                    (
                        first is Point &&
                            (
                                second is Circle ||
                                    second is Curve ||
                                    second is Line
                            )
                    )
            }
        if (!supportsParents) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightFirst = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightfirst",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightLast = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightlast",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Attributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "point1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Identity = when (
            val result = creatorAttributes(
                creatorName,
                point1Attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Attributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "point2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Identity = when (
            val result = creatorAttributes(
                creatorName,
                point2Attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val result =
            if (polarLine) {
                Tangent.createPolarLine(
                    board = resolvedBoard,
                    firstParent = first,
                    secondParent = second,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    straightFirst = straightFirst,
                    straightLast = straightLast,
                    point1Id = point1Identity.id,
                    point1Name = point1Identity.name,
                    point1NeedsRegularUpdate =
                        point1Identity.needsRegularUpdate,
                    point2Id = point2Identity.id,
                    point2Name = point2Identity.name,
                    point2NeedsRegularUpdate =
                        point2Identity.needsRegularUpdate,
                )
            } else {
                Tangent.create(
                    board = resolvedBoard,
                    firstParent = first,
                    secondParent = second,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    straightFirst = straightFirst,
                    straightLast = straightLast,
                    point1Id = point1Identity.id,
                    point1Name = point1Identity.name,
                    point1NeedsRegularUpdate =
                        point1Identity.needsRegularUpdate,
                    point2Id = point2Identity.id,
                    point2Name = point2Identity.name,
                    point2NeedsRegularUpdate =
                        point2Identity.needsRegularUpdate,
                )
            }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.TangentFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    // JSXGraph 1.13.3: src/base/line.js -> createTangentTo.
    private fun createTangentTo(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "tangentto"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 2..3) {
            return unsupported(creatorName, parents, location)
        }
        val conic = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val pointFrom = resolveElement(resolvedBoard, parents[1]) as? Point
            ?: return unsupported(creatorName, parents, location)
        val number = when (val value = parents.getOrNull(2)) {
            null,
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> 0.0
            is JessieCodeRuntimeValue.NumberValue -> value.value
            else -> return unsupported(creatorName, parents, location)
        }
        val tangentAttributes = when (
            val result = tangentToLineAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val polarSource = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "polar",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val polarAttributes = when (
            val result = tangentToLineAttributes(
                creatorName = creatorName,
                attributes = polarSource,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointSource = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "point",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointIdentity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = pointSource,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointFixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = pointSource,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = TangentTo.create(
                board = resolvedBoard,
                conic = conic,
                pointFrom = pointFrom,
                number = number,
                tangentAttributes = tangentAttributes,
                polarAttributes = polarAttributes,
                pointAttributes = TangentToPointAttributes(
                    identity = pointIdentity.toTangentToIdentity(),
                    fixed = pointFixed,
                ),
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.TangentToFactory(
                    result.error,
                ),
                location = location,
            )
        }
    }

    private fun createNormal(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "normal"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val first = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val second = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        val supportsParents =
            (
                (
                    first is Circle ||
                        first is Curve ||
                        first is Line
                ) &&
                    second is Point
            ) ||
                (
                    first is Point &&
                        (
                            second is Circle ||
                                second is Curve ||
                                second is Line
                            )
                    )
        if (!supportsParents) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightFirst = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightfirst",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightLast = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightlast",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val pointIdentity = when (
            val result = nestedCreatorIdentity(
                creatorName = creatorName,
                attributes = attributes,
                name = "point",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Identity = when (
            val result = nestedCreatorIdentity(
                creatorName = creatorName,
                attributes = attributes,
                name = "point1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Identity = when (
            val result = nestedCreatorIdentity(
                creatorName = creatorName,
                attributes = attributes,
                name = "point2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Normal.create(
                board = resolvedBoard,
                firstParent = first,
                secondParent = second,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                straightFirst = straightFirst,
                straightLast = straightLast,
                pointId = pointIdentity.id,
                pointName = pointIdentity.name,
                pointNeedsRegularUpdate =
                    pointIdentity.needsRegularUpdate,
                point1Id = point1Identity.id,
                point1Name = point1Identity.name,
                point1NeedsRegularUpdate =
                    point1Identity.needsRegularUpdate,
                point2Id = point2Identity.id,
                point2Name = point2Identity.name,
                point2NeedsRegularUpdate =
                    point2Identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.NormalFactory(result.error),
                location = location,
            )
        }
    }

    private fun createCircle(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure("circle", JessieCodeCreatorError.BoardUnavailable, location)
        val identity = when (
            val result = creatorAttributes("circle", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (parents.size == 3) {
            val materialized = when (
                val result = materializeThreePointParents(
                    board = resolvedBoard,
                    parents = parents,
                    creatorName = "circle",
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val points = materialized.points
            return when (
                val result = Circle.create(
                    board = resolvedBoard,
                    point1 = points[0],
                    point2 = points[1],
                    point3 = points[2],
                    ownedPoints = materialized.ownedPoints,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> element(result.value)
                is GMResult.Err -> {
                    resolvedBoard.removeObjects(materialized.ownedPoints)
                    failure(
                        creatorName = "circle",
                        error = JessieCodeCreatorError.CircleFactory(
                            result.error,
                        ),
                        location = location,
                    )
                }
            }
        }
        if (parents.size != 2) {
            return unsupported("circle", parents, location)
        }

        val firstPoint = pointParent(resolvedBoard, parents[0])
        val secondPoint = pointParent(resolvedBoard, parents[1])
        val firstElement = resolveElement(resolvedBoard, parents[0])
        val secondElement = resolveElement(resolvedBoard, parents[1])
        val firstRadius = radiusParent(parents[0])
        val secondRadius = radiusParent(parents[1])
        val ownedPoints = linkedSetOf<Point>()

        fun materialize(parent: PointParent): GMResult<Point, PointError> =
            when (
                val materialized = materializePoint(
                    board = resolvedBoard,
                    parent = parent,
                    coordinateLocation = location,
                )
            ) {
                is GMResult.Ok -> {
                    if (parent is PointParent.Coordinates) {
                        ownedPoints += materialized.value
                    }
                    materialized
                }
                is GMResult.Err -> materialized
            }

        fun cleanupOwnedPoints() {
            if (ownedPoints.isNotEmpty()) {
                resolvedBoard.removeObjects(ownedPoints)
            }
        }

        val result = when {
            firstPoint != null && secondPoint != null -> {
                val center = when (
                    val point = materialize(firstPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> {
                        cleanupOwnedPoints()
                        return failure(
                            "circle",
                            JessieCodeCreatorError.PointFactory(point.error),
                            location,
                        )
                    }
                }
                val point2 = when (
                    val point = materialize(secondPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> {
                        cleanupOwnedPoints()
                        return failure(
                            "circle",
                            JessieCodeCreatorError.PointFactory(point.error),
                            location,
                        )
                    }
                }
                Circle.create(
                    board = resolvedBoard,
                    center = center,
                    point2 = point2,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )
            }
            firstPoint != null && secondRadius != null -> {
                val nonnegativeOnly = when (
                    val attribute = booleanAttribute(
                        creatorName = "circle",
                        attributes = attributes,
                        name = "nonnegativeonly",
                        default = false,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> attribute.value
                    is GMResult.Err -> return attribute
                }
                val center = when (
                    val point = materialize(firstPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> {
                        cleanupOwnedPoints()
                        return failure(
                            "circle",
                            JessieCodeCreatorError.PointFactory(point.error),
                            location,
                        )
                    }
                }
                createRadiusCircle(
                    board = resolvedBoard,
                    center = center,
                    radius = secondRadius,
                    attributes = identity,
                    nonnegativeOnly = nonnegativeOnly,
                    location = location,
                )
            }
            firstRadius != null && secondPoint != null -> {
                val nonnegativeOnly = when (
                    val attribute = booleanAttribute(
                        creatorName = "circle",
                        attributes = attributes,
                        name = "nonnegativeonly",
                        default = false,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> attribute.value
                    is GMResult.Err -> return attribute
                }
                val center = when (
                    val point = materialize(secondPoint)
                ) {
                    is GMResult.Ok -> point.value
                    is GMResult.Err -> {
                        cleanupOwnedPoints()
                        return failure(
                            "circle",
                            JessieCodeCreatorError.PointFactory(point.error),
                            location,
                        )
                    }
                }
                createRadiusCircle(
                    board = resolvedBoard,
                    center = center,
                    radius = firstRadius,
                    attributes = identity,
                    nonnegativeOnly = nonnegativeOnly,
                    location = location,
                )
            }
            firstPoint != null && secondElement is Line -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    firstPoint,
                    secondElement,
                    identity,
                    location,
                )
            }
            firstPoint != null && secondElement is Circle -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    firstPoint,
                    secondElement,
                    identity,
                    location,
                )
            }
            firstElement is Line && secondPoint != null -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    secondPoint,
                    firstElement,
                    identity,
                    location,
                )
            }
            firstElement is Circle && secondPoint != null -> {
                return createElementRadiusCircle(
                    resolvedBoard,
                    secondPoint,
                    firstElement,
                    identity,
                    location,
                )
            }
            else -> return unsupported("circle", parents, location)
        }

        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                cleanupOwnedPoints()
                failure(
                    creatorName = "circle",
                    error = JessieCodeCreatorError.CircleFactory(result.error),
                    location = location,
                )
            }
        }
    }

    private fun createCircumcircle(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "circumcircle"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializeThreePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        return when (
            val result = Circle.createCircumcircle(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.CircleFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/element/conic.js -> createEllipse.
    private fun createEllipse(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "ellipse"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 3..5) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val centerAttributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "center",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fociAttributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "foci",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val minimum = (
            parents.getOrNull(3) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: if (parents.size > 3) {
            return unsupported(creatorName, parents, location)
        } else {
            0.0
        }
        val maximum = (
            parents.getOrNull(4) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: if (parents.size > 4) {
            return unsupported(creatorName, parents, location)
        } else {
            2.0 * kotlin.math.PI
        }

        val pointParents = mutableListOf<FocalConicPointParent>()
        for (index in 0..1) {
            when (
                val result = focalConicPointParent(
                    board = resolvedBoard,
                    value = parents[index],
                    location = location,
                )
            ) {
                is GMResult.Ok -> {
                    val pointParent = result.value
                        ?: return unsupported(
                            creatorName,
                            parents,
                            location,
                        )
                    pointParents += pointParent
                }
                is GMResult.Err -> return result
            }
        }
        val thirdFunction =
            parents[2] as? JessieCodeRuntimeValue.FunctionValue
        val thirdValue =
            if (thirdFunction != null) {
                when (
                    val result = thirdFunction.externalCallable.call(
                        arguments = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                parents[2]
            }
        val thirdPointParent = pointParent(resolvedBoard, thirdValue)?.let {
            FocalConicPointParent(
                parent = it,
                parentless =
                    thirdFunction != null ||
                        it is PointParent.Coordinates,
            )
        }
        val majorAxisTerm =
            if (thirdPointParent == null) {
                when (val parent = thirdValue) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        if (
                            thirdFunction != null
                        ) {
                            JessieCodeRuntimeCoordinateFunction(
                                function = thirdFunction,
                                location = location,
                                returnsCoordinateArray = false,
                            )
                        } else {
                            JessieCodeNumericCoordinateFunction(parent.value)
                        }
                    else -> return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                }
            } else {
                null
            }

        val materializedPoints = mutableListOf<Point>()
        val ownedPoints = linkedSetOf<Point>()
        val parentlessPoints = linkedSetOf<Point>()
        val requiredPointParents =
            pointParents + listOfNotNull(thirdPointParent)
        for (resolvedParent in requiredPointParents) {
            val pointParent = resolvedParent.parent
            val point = when (pointParent) {
                is PointParent.Existing -> pointParent.point
                is PointParent.Coordinates -> when (
                    val result = createPointFromCoordinates(
                        board = resolvedBoard,
                        coordinates = pointParent.values,
                        attributes = fociAttributes,
                        coordinateLocation = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        resolvedBoard.removeObjects(ownedPoints)
                        return failure(
                            creatorName = creatorName,
                            error = JessieCodeCreatorError.PointFactory(
                                result.error,
                            ),
                            location = location,
                        )
                    }
                }
            }
            materializedPoints += point
            if (pointParent is PointParent.Coordinates) {
                ownedPoints += point
            }
            if (resolvedParent.parentless) {
                parentlessPoints += point
            }
        }
        val result =
            if (thirdPointParent != null) {
                Ellipse.create(
                    board = resolvedBoard,
                    focus1 = materializedPoints[0],
                    focus2 = materializedPoints[1],
                    pointOnEllipse = materializedPoints[2],
                    parentlessPoints = parentlessPoints,
                    minimum = minimum,
                    maximum = maximum,
                    sampleCount = sampleCount,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    centerId = centerAttributes.id,
                    centerName = centerAttributes.name,
                    centerNeedsRegularUpdate =
                        centerAttributes.needsRegularUpdate,
                    centerFixed = centerAttributes.fixed,
                )
            } else {
                val resolvedMajorAxisTerm = majorAxisTerm
                    ?: return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                Ellipse.create(
                    board = resolvedBoard,
                    focus1 = materializedPoints[0],
                    focus2 = materializedPoints[1],
                    majorAxisTerm = resolvedMajorAxisTerm,
                    parentlessPoints = parentlessPoints,
                    minimum = minimum,
                    maximum = maximum,
                    sampleCount = sampleCount,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    centerId = centerAttributes.id,
                    centerName = centerAttributes.name,
                    centerNeedsRegularUpdate =
                        centerAttributes.needsRegularUpdate,
                    centerFixed = centerAttributes.fixed,
                )
            }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.EllipseFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/element/conic.js -> createHyperbola.
    private fun createHyperbola(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "hyperbola"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 3..5) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val centerAttributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "center",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fociAttributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "foci",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val minimum = (
            parents.getOrNull(3) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: if (parents.size > 3) {
            return unsupported(creatorName, parents, location)
        } else {
            -1.0001 * kotlin.math.PI
        }
        val maximum = (
            parents.getOrNull(4) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: if (parents.size > 4) {
            return unsupported(creatorName, parents, location)
        } else {
            1.0001 * kotlin.math.PI
        }

        val pointParents = mutableListOf<FocalConicPointParent>()
        for (index in 0..1) {
            when (
                val result = focalConicPointParent(
                    board = resolvedBoard,
                    value = parents[index],
                    location = location,
                )
            ) {
                is GMResult.Ok -> {
                    val pointParent = result.value
                        ?: return unsupported(
                            creatorName,
                            parents,
                            location,
                        )
                    pointParents += pointParent
                }
                is GMResult.Err -> return result
            }
        }
        val thirdFunction =
            parents[2] as? JessieCodeRuntimeValue.FunctionValue
        val thirdValue =
            if (thirdFunction != null) {
                when (
                    val result = thirdFunction.externalCallable.call(
                        arguments = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            } else {
                parents[2]
            }
        val thirdPointParent = pointParent(resolvedBoard, thirdValue)?.let {
            FocalConicPointParent(
                parent = it,
                parentless =
                    thirdFunction != null ||
                        it is PointParent.Coordinates,
            )
        }
        val majorAxisTerm =
            if (thirdPointParent == null) {
                when (val parent = thirdValue) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        if (thirdFunction != null) {
                            JessieCodeRuntimeCoordinateFunction(
                                function = thirdFunction,
                                location = location,
                                returnsCoordinateArray = false,
                            )
                        } else {
                            JessieCodeNumericCoordinateFunction(parent.value)
                        }
                    else -> return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                }
            } else {
                null
            }

        val materializedPoints = mutableListOf<Point>()
        val ownedPoints = linkedSetOf<Point>()
        val parentlessPoints = linkedSetOf<Point>()
        val requiredPointParents =
            pointParents + listOfNotNull(thirdPointParent)
        for (resolvedParent in requiredPointParents) {
            val pointParent = resolvedParent.parent
            val point = when (pointParent) {
                is PointParent.Existing -> pointParent.point
                is PointParent.Coordinates -> when (
                    val result = createPointFromCoordinates(
                        board = resolvedBoard,
                        coordinates = pointParent.values,
                        attributes = fociAttributes,
                        coordinateLocation = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        resolvedBoard.removeObjects(ownedPoints)
                        return failure(
                            creatorName = creatorName,
                            error = JessieCodeCreatorError.PointFactory(
                                result.error,
                            ),
                            location = location,
                        )
                    }
                }
            }
            materializedPoints += point
            if (pointParent is PointParent.Coordinates) {
                ownedPoints += point
            }
            if (resolvedParent.parentless) {
                parentlessPoints += point
            }
        }
        val result =
            if (thirdPointParent != null) {
                Hyperbola.create(
                    board = resolvedBoard,
                    focus1 = materializedPoints[0],
                    focus2 = materializedPoints[1],
                    pointOnHyperbola = materializedPoints[2],
                    parentlessPoints = parentlessPoints,
                    minimum = minimum,
                    maximum = maximum,
                    sampleCount = sampleCount,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    centerId = centerAttributes.id,
                    centerName = centerAttributes.name,
                    centerNeedsRegularUpdate =
                        centerAttributes.needsRegularUpdate,
                    centerFixed = centerAttributes.fixed,
                )
            } else {
                val resolvedMajorAxisTerm = majorAxisTerm
                    ?: return unsupported(
                        creatorName,
                        parents,
                        location,
                    )
                Hyperbola.create(
                    board = resolvedBoard,
                    focus1 = materializedPoints[0],
                    focus2 = materializedPoints[1],
                    majorAxisTerm = resolvedMajorAxisTerm,
                    parentlessPoints = parentlessPoints,
                    minimum = minimum,
                    maximum = maximum,
                    sampleCount = sampleCount,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    centerId = centerAttributes.id,
                    centerName = centerAttributes.name,
                    centerNeedsRegularUpdate =
                        centerAttributes.needsRegularUpdate,
                    centerFixed = centerAttributes.fixed,
                )
            }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.HyperbolaFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    // JSXGraph 1.13.3: src/element/conic.js -> createParabola.
    private fun createParabola(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "parabola"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 2..4) {
            return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val centerAttributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "center",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val focusAttributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "foci",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val lineAttributes = when (
            val result = nestedCreatorIdentity(
                creatorName = creatorName,
                attributes = attributes,
                name = "line",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val minimum = (
            parents.getOrNull(2) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: if (parents.size > 2) {
            return unsupported(creatorName, parents, location)
        } else {
            0.0
        }
        val maximum = (
            parents.getOrNull(3) as?
                JessieCodeRuntimeValue.NumberValue
            )?.value ?: if (parents.size > 3) {
            return unsupported(creatorName, parents, location)
        } else {
            2.0 * kotlin.math.PI
        }

        val focusParent = when (
            val result = focalConicPointParent(
                board = resolvedBoard,
                value = parents[0],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
                ?: return unsupported(creatorName, parents, location)
            is GMResult.Err -> return result
        }
        val ownedPoints = linkedSetOf<Point>()
        val parentlessElements = linkedSetOf<GeometryElement>()
        val focus = when (val parent = focusParent.parent) {
            is PointParent.Existing -> parent.point
            is PointParent.Coordinates -> when (
                val result = createPointFromCoordinates(
                    board = resolvedBoard,
                    coordinates = parent.values,
                    attributes = focusAttributes,
                    coordinateLocation = location,
                )
            ) {
                is GMResult.Ok -> {
                    ownedPoints += result.value
                    result.value
                }
                is GMResult.Err -> return failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.PointFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
        if (focusParent.parentless) {
            parentlessElements += focus
        }

        var ownedLine: Line? = null
        val directrix = (
            resolveElement(resolvedBoard, parents[1]) as? Line
            ) ?: run {
            val lineParentValues = (
                parents[1] as? JessieCodeRuntimeValue.ArrayValue
                )?.values
            if (lineParentValues == null || lineParentValues.size != 2) {
                resolvedBoard.removeObjects(ownedPoints)
                return unsupported(creatorName, parents, location)
            }
            val materialized = when (
                val result = materializePointParents(
                    board = resolvedBoard,
                    parents = lineParentValues,
                    creatorName = creatorName,
                    expectedCount = 2,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    resolvedBoard.removeObjects(ownedPoints)
                    return result
                }
            }
            ownedPoints += materialized.ownedPoints
            when (
                val result = Line.create(
                    board = resolvedBoard,
                    point1 = materialized.points[0],
                    point2 = materialized.points[1],
                    id = lineAttributes.id,
                    name = lineAttributes.name,
                    needsRegularUpdate =
                        lineAttributes.needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    ownedLine = result.value
                    parentlessElements += result.value
                    result.value
                }
                is GMResult.Err -> {
                    resolvedBoard.removeObjects(ownedPoints)
                    return failure(
                        creatorName = creatorName,
                        error = JessieCodeCreatorError.LineFactory(
                            result.error,
                        ),
                        location = location,
                    )
                }
            }
        }

        return when (
            val result = Parabola.create(
                board = resolvedBoard,
                focus = focus,
                directrix = directrix,
                parentlessElements = parentlessElements,
                minimum = minimum,
                maximum = maximum,
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                centerId = centerAttributes.id,
                centerName = centerAttributes.name,
                centerNeedsRegularUpdate =
                    centerAttributes.needsRegularUpdate,
                centerFixed = centerAttributes.fixed,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                ownedLine?.let(resolvedBoard::removeObject)
                resolvedBoard.removeObjects(ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.ParabolaFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun focalConicPointParent(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<FocalConicPointParent?, JessieCodeRuntimeError> {
        if (value is JessieCodeRuntimeValue.FunctionValue) {
            return when (
                val result = value.externalCallable.call(
                    arguments = emptyList(),
                    location = location,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    pointParent(board, result.value)?.let {
                        FocalConicPointParent(
                            parent = it,
                            parentless = true,
                        )
                    },
                )
                is GMResult.Err -> result
            }
        }
        val parent = pointParent(board, value)
        return GMResult.Ok(
            parent?.let {
                FocalConicPointParent(
                    parent = it,
                    parentless = it is PointParent.Coordinates,
                )
            },
        )
    }

    private fun createCurve(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                "curve",
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes("curve", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            parents.size == 2 &&
            parents[0] is JessieCodeRuntimeValue.ArrayValue &&
            parents[1] is JessieCodeRuntimeValue.ArrayValue
        ) {
            val dataX = numericArray(parents[0]) ?: return unsupported(
                "curve",
                parents,
                location,
            )
            val dataY = numericArray(parents[1]) ?: return unsupported(
                "curve",
                parents,
                location,
            )
            return curveResult(
                creatorName = "curve",
                location = location,
                result = Curve.createData(
                    board = resolvedBoard,
                    dataX = dataX,
                    dataY = dataY,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                ),
            )
        }
        if (parents.size != 4) {
            return unsupported("curve", parents, location)
        }
        val sources = parents.map(::curveTermSource)
        if (sources.any { it == null }) {
            return unsupported("curve", parents, location)
        }
        val sampleCount = when (
            val result = curveSampleCount("curve", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = "curve",
            location = location,
            result = Curve.createStringParametric(
                board = resolvedBoard,
                xSource = sources[0] ?: "",
                ySource = sources[1] ?: "",
                minimumSource = sources[2] ?: "",
                maximumSource = sources[3] ?: "",
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph: src/base/curve.js -> createCurveIntersection,
    // createCurveUnion, createCurveDifference.
    private fun createBooleanCurve(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
        operation: ClipBooleanOperation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val subject = resolveElement(resolvedBoard, parents[0])
            ?: return unsupported(creatorName, parents, location)
        val clip = resolveElement(resolvedBoard, parents[1])
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createBoolean(
                board = resolvedBoard,
                subject = subject,
                clip = clip,
                operation = operation,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    private fun createArc(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        kind: ArcCreatorKind = ArcCreatorKind.ARC,
    ): CreatorResult {
        val creatorName = kind.elementType
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val curveAttributes = when (
            val result = arcAttributes(
                creatorName,
                attributes,
                location,
                selectionOverride = kind.selectionOverride,
                useDirectionOverride = kind.useDirectionOverride,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                expectedCount = when (kind) {
                    ArcCreatorKind.SEMICIRCLE -> 2
                    ArcCreatorKind.CIRCUMCIRCLE_ARC -> 3
                    else -> if (curveAttributes.useDirection) 4 else 3
                },
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        val dependencyPointCount =
            if (kind == ArcCreatorKind.SEMICIRCLE) 2 else 3
        val ownedDependencyPoints =
            materialized.ownedPoints.filterTo(linkedSetOf()) { point ->
                point in points.take(dependencyPointCount)
            }
        val result = when (kind) {
            ArcCreatorKind.ARC -> Arc.create(
                board = resolvedBoard,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                directionpoint = points.getOrNull(3),
                useDirection = curveAttributes.useDirection,
                selection = curveAttributes.selection,
                orientation = curveAttributes.orientation,
                ownedPoints = ownedDependencyPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            ArcCreatorKind.SEMICIRCLE -> Arc.createSemicircle(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                selection = curveAttributes.selection,
                orientation = curveAttributes.orientation,
                ownedPoints = ownedDependencyPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            ArcCreatorKind.CIRCUMCIRCLE_ARC -> Arc.createCircumcircleArc(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                selection = curveAttributes.selection,
                orientation = curveAttributes.orientation,
                ownedPoints = ownedDependencyPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            ArcCreatorKind.MINOR_ARC -> Arc.createMinorArc(
                board = resolvedBoard,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                directionpoint = points.getOrNull(3),
                useDirection = curveAttributes.useDirection,
                orientation = curveAttributes.orientation,
                ownedPoints = ownedDependencyPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            ArcCreatorKind.MAJOR_ARC -> Arc.createMajorArc(
                board = resolvedBoard,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                directionpoint = points.getOrNull(3),
                useDirection = curveAttributes.useDirection,
                orientation = curveAttributes.orientation,
                ownedPoints = ownedDependencyPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        }
        return when (
            result
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.ArcFactory(result.error),
                    location = location,
                )
            }
        }
    }

    private fun createSector(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        kind: SectorCreatorKind = SectorCreatorKind.SECTOR,
    ): CreatorResult =
        createSectorOrAngle(
            board = board,
            parents = parents,
            attributes = attributes,
            location = location,
            kind = kind,
        )

    private fun createAngle(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        createSectorOrAngle(
            board = board,
            parents = parents,
            attributes = attributes,
            location = location,
            kind = SectorCreatorKind.ANGLE,
        )

    private fun createSectorOrAngle(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        kind: SectorCreatorKind,
    ): CreatorResult {
        val creatorName = kind.elementType
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val curveAttributes = when (
            val result = arcAttributes(
                creatorName,
                attributes,
                location,
                selectionOverride = kind.selectionOverride,
                useDirectionOverride = kind.useDirectionOverride,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val radius = if (kind.isAngle) {
            when (
                val result = angleRadius(
                    attributes = attributes,
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
        } else {
            null
        }
        val materialized = when (
            val result = materializePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                expectedCount =
                    if (kind == SectorCreatorKind.CIRCUMCIRCLE_SECTOR) {
                        3
                    } else if (
                        !kind.isAngle && curveAttributes.useDirection
                    ) {
                        4
                    } else {
                        3
                    },
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        val result = when (kind) {
            SectorCreatorKind.SECTOR -> Sector.create(
                board = resolvedBoard,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                directionpoint = points.getOrNull(3),
                useDirection = curveAttributes.useDirection,
                selection = curveAttributes.selection,
                orientation = curveAttributes.orientation,
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            SectorCreatorKind.CIRCUMCIRCLE_SECTOR ->
                Sector.createCircumcircleSector(
                    board = resolvedBoard,
                    point1 = points[0],
                    point2 = points[1],
                    point3 = points[2],
                    selection = curveAttributes.selection,
                    orientation = curveAttributes.orientation,
                    ownedPoints = materialized.ownedPoints,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )

            SectorCreatorKind.MINOR_SECTOR -> Sector.createMinorSector(
                board = resolvedBoard,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                directionpoint = points.getOrNull(3),
                useDirection = curveAttributes.useDirection,
                orientation = curveAttributes.orientation,
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            SectorCreatorKind.MAJOR_SECTOR -> Sector.createMajorSector(
                board = resolvedBoard,
                center = points[0],
                radiuspoint = points[1],
                anglepoint = points[2],
                directionpoint = points.getOrNull(3),
                useDirection = curveAttributes.useDirection,
                orientation = curveAttributes.orientation,
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            SectorCreatorKind.ANGLE -> Sector.createAngle(
                board = resolvedBoard,
                first = points[0],
                vertex = points[1],
                third = points[2],
                radius = radius ?: AngleRadius.Auto,
                selection = curveAttributes.selection,
                orientation = curveAttributes.orientation,
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )

            SectorCreatorKind.NONREFLEX_ANGLE ->
                Sector.createNonreflexAngle(
                    board = resolvedBoard,
                    first = points[0],
                    vertex = points[1],
                    third = points[2],
                    radius = radius ?: AngleRadius.Auto,
                    orientation = curveAttributes.orientation,
                    ownedPoints = materialized.ownedPoints,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )

            SectorCreatorKind.REFLEX_ANGLE -> Sector.createReflexAngle(
                board = resolvedBoard,
                first = points[0],
                vertex = points[1],
                third = points[2],
                radius = radius ?: AngleRadius.Auto,
                orientation = curveAttributes.orientation,
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.SectorFactory(result.error),
                    location = location,
                )
            }
        }
    }

    private fun createFunctionGraph(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val sources = parents.map(::curveTermSource)
        if (sources.any { it == null }) {
            return unsupported(creatorName, parents, location)
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createFunctionGraph(
                board = resolvedBoard,
                ySource = sources[0] ?: "",
                minimumSource = sources[1] ?: "",
                maximumSource = sources[2] ?: "",
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createStepfunction.
    private fun createStepfunction(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "stepfunction"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val xTerm = curveStepTerm(parents[0])
            ?: return unsupported(creatorName, parents, location)
        val yTerm = curveStepTerm(parents[1])
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createStepfunction(
                board = resolvedBoard,
                xTerm = xTerm,
                yTerm = yTerm,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3: src/element/comb.js -> createComb.
    private fun createComb(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "comb"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 2) {
            return unsupported(creatorName, parents, location)
        }
        val pointParents = parents.map { parent ->
            pointParent(resolvedBoard, parent)
                ?: return unsupported(creatorName, parents, location)
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1Attributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "point1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2Attributes = when (
            val result = nestedPointCreatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                name = "point2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val frequencyTerm = when (
            val result = combNumericAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "frequency",
                default = Curve.COMB_DEFAULT_FREQUENCY,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val widthTerm = when (
            val result = combNumericAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "width",
                default = Curve.COMB_DEFAULT_WIDTH,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val angleTerm = when (
            val result = combNumericAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "angle",
                default = Curve.COMB_DEFAULT_ANGLE,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val reverseTerm = when (
            val result = booleanAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "reverse",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val points = mutableListOf<Point>()
        val ownedPoints = linkedSetOf<Point>()
        val pointAttributes = listOf(point1Attributes, point2Attributes)
        for ((index, pointParent) in pointParents.withIndex()) {
            val point = when (pointParent) {
                is PointParent.Existing -> pointParent.point
                is PointParent.Coordinates -> when (
                    val result = createPointFromCoordinates(
                        board = resolvedBoard,
                        coordinates = pointParent.values,
                        attributes = pointAttributes[index],
                        coordinateLocation = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        resolvedBoard.removeObjects(ownedPoints)
                        return failure(
                            creatorName = creatorName,
                            error = JessieCodeCreatorError.PointFactory(
                                result.error,
                            ),
                            location = location,
                        )
                    }
                }
            }
            points += point
            if (pointParent is PointParent.Coordinates) {
                ownedPoints += point
            }
        }

        return when (
            val result = Curve.createComb(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                frequencyTerm = frequencyTerm,
                widthTerm = widthTerm,
                angleTerm = angleTerm,
                reverseTerm = reverseTerm,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(ownedPoints)
                curveFailure(creatorName, result.error, location)
            }
        }
    }

    // JSXGraph 1.13.3:
    // src/element/composition.js -> createInequality.
    private fun createInequality(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "inequality"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val source = parents.firstOrNull()?.let { parent ->
            resolveElement(resolvedBoard, parent)
        } ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val inverseTerm = when (
            val result = booleanAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "inverse",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createInequality(
                board = resolvedBoard,
                source = source,
                inverseTerm = inverseTerm,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3:
    // src/element/vectorfield.js -> createVectorField.
    private fun createVectorField(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        createVectorFieldLike(
            board = board,
            parents = parents,
            attributes = attributes,
            location = location,
            creatorName = "vectorfield",
            slopeField = false,
        )

    // JSXGraph 1.13.3:
    // src/element/vectorfield.js -> createSlopeField.
    private fun createSlopeField(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        createVectorFieldLike(
            board = board,
            parents = parents,
            attributes = attributes,
            location = location,
            creatorName = "slopefield",
            slopeField = true,
        )

    private fun createVectorFieldLike(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        creatorName: String,
        slopeField: Boolean,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size < 3) {
            return unsupported(creatorName, parents, location)
        }
        val field = when (
            val result =
                if (slopeField) {
                    slopeFieldFunction(
                        board = resolvedBoard,
                        value = parents[0],
                        location = location,
                    )
                } else {
                    vectorFieldFunction(
                        board = resolvedBoard,
                        value = parents[0],
                        location = location,
                    )
                }
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val xData = when (
            val result = vectorFieldMesh(
                board = resolvedBoard,
                value = parents[1],
                termName = "$creatorName.xData",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val yData = when (
            val result = vectorFieldMesh(
                board = resolvedBoard,
                value = parents[2],
                termName = "$creatorName.yData",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val scaleTerm = when (
            val result = numericAttributeTerm(
                creatorName = creatorName,
                attributes = attributes,
                name = "scale",
                default = Curve.VECTOR_FIELD_DEFAULT_SCALE,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowHead = when (
            val value = attributes.properties["arrowhead"]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue ->
                JessieCodeRuntimeValue.ObjectValue(emptyMap())
            is JessieCodeRuntimeValue.ObjectValue -> value
            else -> return invalidAttribute(
                creatorName = creatorName,
                attribute = "arrowhead",
                expected = "object",
                actual = value,
                location = location,
            )
        }
        val arrowEnabledTerm = when (
            val result = booleanAttributeTerm(
                creatorName = creatorName,
                attributes = arrowHead,
                name = "enabled",
                default = !slopeField,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowSizeTerm = when (
            val result = numericAttributeTerm(
                creatorName = creatorName,
                attributes = arrowHead,
                name = "size",
                default = Curve.VECTOR_FIELD_DEFAULT_ARROW_SIZE,
                location = location,
                attributePrefix = "arrowhead.",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arrowAngleTerm = when (
            val result = numericAttributeTerm(
                creatorName = creatorName,
                attributes = arrowHead,
                name = "angle",
                default = Curve.VECTOR_FIELD_DEFAULT_ARROW_ANGLE,
                location = location,
                attributePrefix = "arrowhead.",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val result =
            if (slopeField) {
                Curve.createSlopeField(
                    board = resolvedBoard,
                    field = field,
                    xData = xData,
                    yData = yData,
                    scaleTerm = scaleTerm,
                    arrowEnabledTerm = arrowEnabledTerm,
                    arrowSizeTerm = arrowSizeTerm,
                    arrowAngleTerm = arrowAngleTerm,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )
            } else {
                Curve.createVectorField(
                    board = resolvedBoard,
                    field = field,
                    xData = xData,
                    yData = yData,
                    scaleTerm = scaleTerm,
                    arrowEnabledTerm = arrowEnabledTerm,
                    arrowSizeTerm = arrowSizeTerm,
                    arrowAngleTerm = arrowAngleTerm,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                )
            }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = result,
        )
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createDerivative.
    private fun createDerivative(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "derivative"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 1) {
            return unsupported(creatorName, parents, location)
        }
        val source = resolveElement(resolvedBoard, parents[0]) as? Curve
            ?: return unsupported(creatorName, parents, location)
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createDerivative(
                board = resolvedBoard,
                source = source,
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createRiemannsum.
    private fun createRiemannSum(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "riemannsum"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size !in 3..5) {
            return unsupported(creatorName, parents, location)
        }
        val functionValues = (
            parents[0] as? JessieCodeRuntimeValue.ArrayValue
            )?.values
        val upperValue: JessieCodeRuntimeValue
        val lowerValue: JessieCodeRuntimeValue?
        if (functionValues == null) {
            upperValue = parents[0]
            lowerValue = null
        } else {
            if (functionValues.size != 2) {
                return unsupported(creatorName, parents, location)
            }
            lowerValue = functionValues[0]
            upperValue = functionValues[1]
        }
        val upperFunction = when (
            val result = riemannFunctionTerm(
                board = resolvedBoard,
                value = upperValue,
                termName = "riemannsum.f",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val lowerFunction = if (lowerValue == null) {
            null
        } else {
            when (
                val result = riemannFunctionTerm(
                    board = resolvedBoard,
                    value = lowerValue,
                    termName = "riemannsum.g",
                    location = location,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return curveFailure(
                    creatorName,
                    result.error,
                    location,
                )
            }
        }
        val rectangleCount = when (
            val result = riemannNumericTerm(
                board = resolvedBoard,
                value = parents[1],
                termName = "riemannsum.n",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val approximationType = when (
            val result = riemannTypeTerm(
                value = parents[2],
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val minimum = when (
            val result = riemannNumericTerm(
                board = resolvedBoard,
                value = parents.getOrNull(3)
                    ?: JessieCodeRuntimeValue.NumberValue(
                        resolvedBoard.defaultCurveMinimum,
                    ),
                termName = "riemannsum.minX",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val maximum = when (
            val result = riemannNumericTerm(
                board = resolvedBoard,
                value = parents.getOrNull(4)
                    ?: JessieCodeRuntimeValue.NumberValue(
                        resolvedBoard.defaultCurveMaximum,
                    ),
                termName = "riemannsum.maxX",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createRiemannSum(
                board = resolvedBoard,
                upperFunction = upperFunction,
                lowerFunction = lowerFunction,
                rectangleCount = rectangleCount,
                approximationType = approximationType,
                minimum = minimum,
                maximum = maximum,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createBoxPlot.
    private fun createBoxPlot(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "boxplot"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size != 3) {
            return unsupported(creatorName, parents, location)
        }
        val quantileValues = (
            parents[0] as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return unsupported(creatorName, parents, location)
        if (quantileValues.size < Curve.BOX_PLOT_QUANTILE_COUNT) {
            return unsupported(creatorName, parents, location)
        }
        val quantileTerms = mutableListOf<JessieCodeCoordinateFunction>()
        for ((index, value) in quantileValues.withIndex()) {
            val allowArray = index >= Curve.BOX_PLOT_QUANTILE_COUNT
            when (
                val result = boxPlotTerm(
                    board = resolvedBoard,
                    value = value,
                    termName = "boxplot.Q[$index]",
                    location = location,
                    allowArray = allowArray,
                )
            ) {
                is GMResult.Ok -> quantileTerms += result.value
                is GMResult.Err -> return curveFailure(
                    creatorName,
                    result.error,
                    location,
                )
            }
        }
        val axisTerm = when (
            val result = boxPlotTerm(
                board = resolvedBoard,
                value = parents[1],
                termName = "boxplot.x",
                location = location,
                allowArray = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val widthTerm = when (
            val result = boxPlotTerm(
                board = resolvedBoard,
                value = parents[2],
                termName = "boxplot.w",
                location = location,
                allowArray = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return curveFailure(
                creatorName,
                result.error,
                location,
            )
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val direction = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "dir",
                default = "vertical",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val smallWidth = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "smallwidth",
                default = 0.5,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val outlierAttributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "outlier",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val outlierFace = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = outlierAttributes,
                name = "face",
                default = "o",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val outlierSize = when (
            val result = numberAttribute(
                creatorName = creatorName,
                attributes = outlierAttributes,
                name = "size",
                default = 3.0,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createBoxPlot(
                board = resolvedBoard,
                quantileTerms = quantileTerms,
                axisTerm = axisTerm,
                widthTerm = widthTerm,
                direction = direction,
                smallWidth = smallWidth,
                outlierFace = outlierFace,
                outlierSize = outlierSize,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createSpline.
    private fun createSpline(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "spline"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = mutableListOf<CurveSplinePoint>()
        val firstArray = (
            parents.getOrNull(0) as? JessieCodeRuntimeValue.ArrayValue
            )?.values
        val secondArray = (
            parents.getOrNull(1) as? JessieCodeRuntimeValue.ArrayValue
            )?.values
        if (
            parents.size == 2 &&
            firstArray != null &&
            secondArray != null &&
            firstArray.size == secondArray.size
        ) {
            for (index in firstArray.indices) {
                when (
                    val result = splineCoordinatePoint(
                        x = firstArray[index],
                        y = secondArray[index],
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> points += result.value
                    is GMResult.Err -> return curveFailure(
                        creatorName,
                        result.error,
                        location,
                    )
                }
            }
        } else {
            for (parent in parents) {
                val selected = resolveElement(resolvedBoard, parent)
                if (selected is Point) {
                    points += CurveElementSplinePoint(selected)
                    continue
                }
                val coordinatePair = (
                    parent as? JessieCodeRuntimeValue.ArrayValue
                    )?.values
                if (coordinatePair?.size == 2) {
                    // The nested loop intentionally mirrors createSpline:
                    // every coordinate-pair parent replays the full list.
                    for (candidate in parents) {
                        val pair = (
                            candidate as?
                                JessieCodeRuntimeValue.ArrayValue
                            )?.values
                        if (pair?.size != 2) {
                            return unsupported(
                                creatorName,
                                parents,
                                location,
                            )
                        }
                        when (
                            val result = splineCoordinatePoint(
                                x = pair[0],
                                y = pair[1],
                                location = location,
                            )
                        ) {
                            is GMResult.Ok -> points += result.value
                            is GMResult.Err -> return curveFailure(
                                creatorName,
                                result.error,
                                location,
                            )
                        }
                    }
                    continue
                }
                if (parent is JessieCodeRuntimeValue.FunctionValue) {
                    points += CurveFunctionSplinePoint(parent, location)
                    continue
                }
                return unsupported(creatorName, parents, location)
            }
        }
        return curveResult(
            creatorName = creatorName,
            location = location,
            result = Curve.createSpline(
                board = resolvedBoard,
                points = points,
                sampleCount = sampleCount,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            ),
        )
    }

    // JSXGraph 1.13.3: src/base/curve.js -> createCardinalSpline.
    private fun createCardinalSpline(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "cardinalspline"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        if (parents.size < 2) {
            return unsupported(creatorName, parents, location)
        }
        val inputPoints = (
            parents[0] as? JessieCodeRuntimeValue.ArrayValue
            )?.values ?: return unsupported(creatorName, parents, location)
        val tensionTerm = when (val value = parents[1]) {
            is JessieCodeRuntimeValue.NumberValue ->
                JessieCodeNumericCoordinateFunction(value.value)
            is JessieCodeRuntimeValue.FunctionValue ->
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                )
            else -> return unsupported(creatorName, parents, location)
        }
        val type = (
            parents.getOrNull(2) as? JessieCodeRuntimeValue.StringValue
            )?.value ?: "uniform"
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val createPoints = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "createpoints",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val isArrayOfCoordinates = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "isarrayofcoordinates",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sampleCount = when (
            val result = curveSampleCount(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val normalized = mutableListOf<JessieCodeRuntimeValue>()
        val xValues = (
            inputPoints.getOrNull(0) as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values
        val yValues = (
            inputPoints.getOrNull(1) as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values
        if (
            !isArrayOfCoordinates &&
            inputPoints.size == 2 &&
            xValues != null &&
            yValues != null &&
            xValues.size == yValues.size
        ) {
            for (index in xValues.indices) {
                val pair = when (
                    val result = evaluateCardinalCoordinatePair(
                        values = listOf(xValues[index], yValues[index]),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return curveFailure(
                        creatorName,
                        result.error,
                        location,
                    )
                }
                normalized += pair
            }
        } else {
            for (value in inputPoints) {
                val selected = resolveElement(resolvedBoard, value)
                if (selected is Point) {
                    normalized += JessieCodeRuntimeValue.ElementReference(
                        selected,
                    )
                    continue
                }
                val pair = (
                    value as? JessieCodeRuntimeValue.ArrayValue
                    )?.values
                if (pair?.size == 2) {
                    when (
                        val result = evaluateCardinalCoordinatePair(
                            values = pair,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> normalized += result.value
                        is GMResult.Err -> return curveFailure(
                            creatorName,
                            result.error,
                            location,
                        )
                    }
                    continue
                }
                if (value is JessieCodeRuntimeValue.FunctionValue) {
                    when (
                        val result = evaluateCardinalPointFunction(
                            value,
                            location,
                        )
                    ) {
                        is GMResult.Ok -> normalized += result.value
                        is GMResult.Err -> return curveFailure(
                            creatorName,
                            result.error,
                            location,
                        )
                    }
                }
            }
        }

        val points = mutableListOf<NumericsPoint2D>()
        val ownedPoints = linkedSetOf<Point>()
        for (value in normalized) {
            val selected = resolveElement(resolvedBoard, value)
            if (selected is Point) {
                points += selected
                continue
            }
            val pointParent = pointParent(resolvedBoard, value)
                ?: run {
                    resolvedBoard.removeObjects(ownedPoints)
                    return unsupported(creatorName, parents, location)
                }
            if (createPoints) {
                when (
                    val result = materializePoint(
                        board = resolvedBoard,
                        parent = pointParent,
                        coordinateLocation = location,
                    )
                ) {
                    is GMResult.Ok -> {
                        points += result.value
                        if (pointParent is PointParent.Coordinates) {
                            ownedPoints += result.value
                        }
                    }
                    is GMResult.Err -> {
                        resolvedBoard.removeObjects(ownedPoints)
                        return failure(
                            creatorName = creatorName,
                            error = JessieCodeCreatorError.PointFactory(
                                result.error,
                            ),
                            location = location,
                        )
                    }
                }
            } else {
                val coordinates = when (
                    val result = staticCurveCoordinates(
                        board = resolvedBoard,
                        parent = pointParent,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        resolvedBoard.removeObjects(ownedPoints)
                        return curveFailure(
                            creatorName,
                            result.error,
                            location,
                        )
                    }
                }
                points += CurveStaticPoint(
                    x = coordinates.first,
                    y = coordinates.second,
                )
            }
        }

        val result = Curve.createCardinalSpline(
            board = resolvedBoard,
            points = points,
            tensionTerm = tensionTerm,
            type = type,
            ownedPoints = ownedPoints,
            sampleCount = sampleCount,
            id = identity.id,
            name = identity.name,
            needsRegularUpdate = identity.needsRegularUpdate,
        )
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(ownedPoints)
                curveFailure(creatorName, result.error, location)
            }
        }
    }

    private fun createPolygon(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        kind: PolygonCreatorKind,
    ): CreatorResult {
        val creatorName = kind.elementType
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val withLines = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "withlines",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val pointParents = mutableListOf<PointParent>()
        for (parent in parents) {
            pointParents += pointParent(resolvedBoard, parent)
                ?: return unsupported(creatorName, parents, location)
        }

        val vertices = mutableListOf<Point>()
        val ownedVertices = linkedSetOf<Point>()
        for (parent in pointParents) {
            when (
                val result = materializePoint(
                    board = resolvedBoard,
                    parent = parent,
                    coordinateLocation = location,
                )
            ) {
                is GMResult.Ok -> {
                    vertices += result.value
                    if (parent is PointParent.Coordinates) {
                        ownedVertices += result.value
                    }
                }
                is GMResult.Err -> {
                    resolvedBoard.removeObjects(ownedVertices)
                    return failure(
                        creatorName = creatorName,
                        error = JessieCodeCreatorError.PointFactory(
                            result.error,
                        ),
                        location = location,
                    )
                }
            }
        }

        return when (
            val result =
                if (kind == PolygonCreatorKind.POLYGONAL_CHAIN) {
                    Polygon.createPolygonalChain(
                        board = resolvedBoard,
                        vertices = vertices,
                        ownedVertices = ownedVertices,
                        withLines = withLines,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                    )
                } else {
                    Polygon.create(
                        board = resolvedBoard,
                        vertices = vertices,
                        ownedVertices = ownedVertices,
                        withLines = withLines,
                        id = identity.id,
                        name = identity.name,
                        needsRegularUpdate = identity.needsRegularUpdate,
                    )
                }
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(ownedVertices)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.PolygonFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createParallelogram(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "parallelogram"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val withLines = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "withlines",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parallelPointAttributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "parallelpoint",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parallelPointIdentity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = parallelPointAttributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val materialized = when (
            val result = materializeThreePointParents(
                board = resolvedBoard,
                parents = parents,
                creatorName = creatorName,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val points = materialized.points
        return when (
            val result = Parallelogram.create(
                board = resolvedBoard,
                point1 = points[0],
                point2 = points[1],
                point3 = points[2],
                ownedPoints = materialized.ownedPoints,
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
                withLines = withLines,
                parallelPointId = parallelPointIdentity.id,
                parallelPointName = parallelPointIdentity.name ?: "",
                parallelPointNeedsRegularUpdate =
                    parallelPointIdentity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.ParallelogramFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createRegularPolygon(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val creatorName = "regularpolygon"
        val resolvedBoard = board
            ?: return failure(
                creatorName,
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val numericVertexCount = (
            parents.lastOrNull() as?
                JessieCodeRuntimeValue.NumberValue
            )?.value
        if (
            numericVertexCount != null &&
            (parents.size != 3 || numericVertexCount < 3.0)
        ) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.RegularPolygonFactory(
                    RegularPolygonError.InvalidNumericParentForm(
                        actualParentCount = parents.size,
                        vertexCount = numericVertexCount,
                    ),
                ),
                location = location,
            )
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName,
                attributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val withLines = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "withlines",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val vertexAttributes = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "vertices",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val vertexIdentity = when (
            val result = creatorAttributes(
                creatorName,
                vertexAttributes,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = vertexAttributes,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val vertexIds = when (
            val result = stringListAttribute(
                creatorName = creatorName,
                attributes = vertexAttributes,
                name = "ids",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }

        val pointValues =
            if (numericVertexCount == null) parents else parents.dropLast(1)
        val materialized = when (
            val result = materializePointParents(
                board = resolvedBoard,
                parents = pointValues,
                creatorName = creatorName,
                expectedCount = pointValues.size,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val result =
            if (numericVertexCount == null) {
                RegularPolygon.create(
                    board = resolvedBoard,
                    vertices = materialized.points,
                    ownedPoints = materialized.ownedPoints,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    withLines = withLines,
                )
            } else {
                RegularPolygon.create(
                    board = resolvedBoard,
                    point1 = materialized.points[0],
                    point2 = materialized.points[1],
                    numberOfVertices = numericVertexCount,
                    ownedPoints = materialized.ownedPoints,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    withLines = withLines,
                    vertexIds = vertexIds,
                    vertexId = vertexIdentity.id,
                    vertexName = vertexIdentity.name ?: "",
                    vertexNeedsRegularUpdate =
                        vertexIdentity.needsRegularUpdate,
                )
            }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                resolvedBoard.removeObjects(materialized.ownedPoints)
                failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.RegularPolygonFactory(
                        result.error,
                    ),
                    location = location,
                )
            }
        }
    }

    private fun createText(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val resolvedBoard = board
            ?: return failure(
                "text",
                JessieCodeCreatorError.BoardUnavailable,
                location,
            )
        val identity = when (
            val result = creatorAttributes("text", attributes, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (parents.size !in 3..4) {
            return unsupported("text", parents, location)
        }
        val coordinates = parents.dropLast(1)
        if (
            coordinates.any {
                it !is JessieCodeRuntimeValue.NumberValue &&
                    it !is JessieCodeRuntimeValue.StringValue
            }
        ) {
            return unsupported("text", parents, location)
        }
        val parse = when (
            val result = booleanAttribute(
                creatorName = "text",
                attributes = attributes,
                name = "parse",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val formatNumber = when (
            val result = booleanAttribute(
                creatorName = "text",
                attributes = attributes,
                name = "formatnumber",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val digits = when (
            val result = integerAttribute(
                creatorName = "text",
                attributes = attributes,
                name = "digits",
                default = 2,
                minimum = 0,
                maximum = 100,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val content = when (val value = parents.last()) {
            is JessieCodeRuntimeValue.StringValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                if (formatNumber) {
                    JsNumberFormat.fixed(value.value, digits)
                } else {
                    JsNumberFormat.compact(value.value)
                }
            else -> return unsupported("text", parents, location)
        }
        val numericCoordinates = coordinates.mapNotNull {
            (it as? JessieCodeRuntimeValue.NumberValue)?.value
        }
        val coordinateExpressions = coordinates.map { coordinate ->
            when (coordinate) {
                is JessieCodeRuntimeValue.NumberValue ->
                    JsNumberFormat.compact(coordinate.value)
                is JessieCodeRuntimeValue.StringValue -> coordinate.value
                else -> return unsupported("text", parents, location)
            }
        }
        val result =
            if (numericCoordinates.size == coordinates.size) {
                Text.create(
                    board = resolvedBoard,
                    coordinates = numericCoordinates.toDoubleArray(),
                    content = content,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    parse = parse,
                    digits = digits,
                )
            } else {
                Text.create(
                    board = resolvedBoard,
                    coordinateExpressions = coordinateExpressions,
                    content = content,
                    id = identity.id,
                    name = identity.name,
                    needsRegularUpdate = identity.needsRegularUpdate,
                    parse = parse,
                    digits = digits,
                )
            }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = "text",
                error = JessieCodeCreatorError.TextFactory(result.error),
                location = location,
            )
        }
    }

    private fun curveSampleCount(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<Int, JessieCodeRuntimeError> {
        val advanced = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "doadvancedplot",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (advanced) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = "doAdvancedPlot",
                    actual = "true",
                ),
                location = location,
            )
        }
        return integerAttribute(
            creatorName = creatorName,
            attributes = attributes,
            name = "numberpointshigh",
            default = Curve.DEFAULT_SAMPLE_COUNT,
            minimum = 1,
            maximum = Curve.MAX_SAMPLE_COUNT,
            location = location,
        )
    }

    private fun arcAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        selectionOverride: String? = null,
        useDirectionOverride: Boolean? = null,
    ): GMResult<ArcAttributes, JessieCodeRuntimeError> {
        val selection = selectionOverride ?: when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "selection",
                default = Arc.SELECTION_AUTO,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val orientation = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "orientation",
                default = Arc.ORIENTATION_COUNTERCLOCKWISE,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val useDirection = useDirectionOverride ?: when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "usedirection",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            ArcAttributes(
                selection = selection,
                orientation = orientation,
                useDirection = useDirection,
            ),
        )
    }

    private fun angleRadius(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<AngleRadius, JessieCodeRuntimeError> =
        when (val value = attributes.properties["radius"]) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> GMResult.Ok(AngleRadius.Auto)

            is JessieCodeRuntimeValue.NumberValue ->
                if (value.value.isFinite()) {
                    GMResult.Ok(AngleRadius.Fixed(value.value))
                } else {
                    failure(
                        creatorName = "angle",
                        error =
                            JessieCodeCreatorError.UnsupportedAttributeValue(
                                attribute = "radius",
                                actual = value.value.toString(),
                            ),
                        location = location,
                    )
                }

            is JessieCodeRuntimeValue.StringValue ->
                if (value.value.lowercase() == "auto") {
                    GMResult.Ok(AngleRadius.Auto)
                } else {
                    failure(
                        creatorName = "angle",
                        error =
                            JessieCodeCreatorError.UnsupportedAttributeValue(
                                attribute = "radius",
                                actual = value.value,
                            ),
                        location = location,
                    )
                }

            else -> invalidAttribute(
                creatorName = "angle",
                attribute = "radius",
                expected = "number or \"auto\"",
                actual = value,
                location = location,
            )
        }

    private fun curveResult(
        creatorName: String,
        location: JessieCodeAstLocation,
        result: GMResult<Curve, CurveError>,
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.CurveFactory(result.error),
                location = location,
            )
        }

    private fun curveFailure(
        creatorName: String,
        error: CurveError,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        failure(
            creatorName = creatorName,
            error = JessieCodeCreatorError.CurveFactory(error),
            location = location,
        )

    private fun combNumericAttributeTerm(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Double,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, JessieCodeRuntimeError> =
        when (
            val value = attributes.properties[name]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(default),
            )
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(value.value),
            )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> invalidAttribute(
                creatorName = creatorName,
                attribute = name,
                expected = "number or function",
                actual = value,
                location = location,
            )
        }

    private fun numericAttributeTerm(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Double,
        location: JessieCodeAstLocation,
        attributePrefix: String = "",
    ): GMResult<JessieCodeCoordinateFunction, JessieCodeRuntimeError> =
        when (
            val value = attributes.properties[name]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(default),
            )
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(value.value),
            )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> invalidAttribute(
                creatorName = creatorName,
                attribute = "$attributePrefix$name",
                expected = "number or function",
                actual = value,
                location = location,
            )
        }

    private fun booleanAttributeTerm(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Boolean,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, JessieCodeRuntimeError> =
        when (
            val value = attributes.properties[name]
                ?: JessieCodeRuntimeValue.UndefinedValue
        ) {
            JessieCodeRuntimeValue.UndefinedValue -> GMResult.Ok(
                JessieCodeConstantCoordinateFunction(
                    JessieCodeRuntimeValue.BooleanValue(default),
                ),
            )
            is JessieCodeRuntimeValue.BooleanValue -> GMResult.Ok(
                JessieCodeConstantCoordinateFunction(value),
            )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> invalidAttribute(
                creatorName = creatorName,
                attribute = name,
                expected = "boolean or function",
                actual = value,
                location = location,
            )
        }

    private fun riemannFunctionTerm(
        board: Board,
        value: JessieCodeRuntimeValue,
        termName: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.StringValue ->
                compileRiemannTerm(
                    board = board,
                    source = value.value,
                    termName = termName,
                    variableNames = listOf("x"),
                )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = termName,
                    actualType = typeName(value),
                ),
            )
        }

    private fun boxPlotTerm(
        board: Board,
        value: JessieCodeRuntimeValue,
        termName: String,
        location: JessieCodeAstLocation,
        allowArray: Boolean,
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(value.value),
            )
            is JessieCodeRuntimeValue.StringValue ->
                compileRiemannTerm(
                    board = board,
                    source = value.value,
                    termName = termName,
                )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = allowArray,
                ),
            )
            is JessieCodeRuntimeValue.ArrayValue ->
                if (allowArray) {
                    GMResult.Ok(JessieCodeConstantCoordinateFunction(value))
                } else {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = termName,
                            actualType = typeName(value),
                        ),
                    )
                }
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = termName,
                    actualType = typeName(value),
                ),
            )
        }

    private fun riemannNumericTerm(
        board: Board,
        value: JessieCodeRuntimeValue,
        termName: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(value.value),
            )
            is JessieCodeRuntimeValue.StringValue ->
                compileRiemannTerm(
                    board = board,
                    source = value.value,
                    termName = termName,
                )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = termName,
                    actualType = typeName(value),
                ),
            )
        }

    private fun riemannTypeTerm(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.StringValue,
            is JessieCodeRuntimeValue.NumberValue,
            -> GMResult.Ok(JessieCodeConstantCoordinateFunction(value))
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "riemannsum.type",
                    actualType = typeName(value),
                ),
            )
        }

    private fun compileRiemannTerm(
        board: Board,
        source: String,
        termName: String,
        variableNames: List<String> = emptyList(),
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (
            val result = JessieCodeExpressionFunction.compile(
                source = source,
                board = board,
                variableNames = variableNames,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionCompile(
                    term = termName,
                    error = result.error,
                ),
            )
        }

    private fun vectorFieldFunction(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<CurveVectorFieldFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.ArrayValue -> {
                val first = value.values.getOrNull(0)
                    ?: return GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "vectorfield.F[0]",
                            actualType = "undefined",
                        ),
                    )
                val second = value.values.getOrNull(1)
                    ?: return GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "vectorfield.F[1]",
                            actualType = "undefined",
                        ),
                    )
                val xTerm = when (
                    val result = vectorFieldTerm(
                        board = board,
                        value = first,
                        termName = "vectorfield.F[0]",
                        variableNames = listOf("x", "y"),
                        returnsArray = false,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val yTerm = when (
                    val result = vectorFieldTerm(
                        board = board,
                        value = second,
                        termName = "vectorfield.F[1]",
                        variableNames = listOf("x", "y"),
                        returnsArray = false,
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(
                    CurveVectorFieldComponentFunction(xTerm, yTerm),
                )
            }
            is JessieCodeRuntimeValue.StringValue,
            is JessieCodeRuntimeValue.FunctionValue,
            -> when (
                val result = vectorFieldTerm(
                    board = board,
                    value = value,
                    termName = "vectorfield.F",
                    variableNames = listOf("x", "y"),
                    returnsArray = true,
                    location = location,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    CurveVectorFieldArrayFunction(result.value),
                )
                is GMResult.Err -> result
            }
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "vectorfield.F",
                    actualType = typeName(value),
                ),
            )
        }

    private fun vectorField3DFunction(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Curve3DVectorFieldFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.ArrayValue -> {
                if (value.values.size != 3) {
                    return GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "vectorfield3d.F",
                            actualType = "invalid length",
                        ),
                    )
                }
                val terms = mutableListOf<JessieCodeCoordinateFunction>()
                for ((index, component) in value.values.withIndex()) {
                    when (
                        val result = vectorFieldTerm(
                            board = board,
                            value = component,
                            termName = "vectorfield3d.F[$index]",
                            variableNames = listOf("x", "y", "z"),
                            returnsArray = false,
                            location = location,
                        )
                    ) {
                        is GMResult.Ok -> terms += result.value
                        is GMResult.Err -> return result
                    }
                }
                GMResult.Ok(
                    Curve3DVectorFieldComponentFunction(
                        xTerm = terms[0],
                        yTerm = terms[1],
                        zTerm = terms[2],
                    ),
                )
            }
            is JessieCodeRuntimeValue.StringValue,
            is JessieCodeRuntimeValue.FunctionValue,
            -> when (
                val result = vectorFieldTerm(
                    board = board,
                    value = value,
                    termName = "vectorfield3d.F",
                    variableNames = listOf("x", "y", "z"),
                    returnsArray = true,
                    location = location,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    Curve3DVectorFieldArrayFunction(result.value),
                )
                is GMResult.Err -> result
            }
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "vectorfield3d.F",
                    actualType = typeName(value),
                ),
            )
        }

    private fun slopeFieldFunction(
        board: Board,
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<CurveVectorFieldFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.StringValue,
            is JessieCodeRuntimeValue.FunctionValue,
            -> when (
                val result = vectorFieldTerm(
                    board = board,
                    value = value,
                    termName = "slopefield.F",
                    variableNames = listOf("x", "y"),
                    returnsArray = false,
                    location = location,
                )
            ) {
                is GMResult.Ok -> GMResult.Ok(
                    CurveSlopeFieldFunction(result.value),
                )
                is GMResult.Err -> result
            }
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "slopefield.F",
                    actualType = typeName(value),
                ),
            )
        }

    private fun vectorFieldMesh(
        board: Board,
        value: JessieCodeRuntimeValue,
        termName: String,
        location: JessieCodeAstLocation,
    ): GMResult<List<JessieCodeCoordinateFunction>, CurveError> {
        val values = (
            value as? JessieCodeRuntimeValue.ArrayValue
            )?.values
        if (values?.size != 3) {
            return GMResult.Err(
                CurveError.NonNumericExpression(
                    term = termName,
                    actualType = typeName(value),
                ),
            )
        }
        val terms = mutableListOf<JessieCodeCoordinateFunction>()
        for ((index, item) in values.withIndex()) {
            when (
                val result = vectorFieldTerm(
                    board = board,
                    value = item,
                    termName = "$termName[$index]",
                    variableNames = emptyList(),
                    returnsArray = false,
                    location = location,
                )
            ) {
                is GMResult.Ok -> terms += result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(terms)
    }

    private fun vectorFieldTerm(
        board: Board,
        value: JessieCodeRuntimeValue,
        termName: String,
        variableNames: List<String>,
        returnsArray: Boolean,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(value.value),
            )
            is JessieCodeRuntimeValue.StringValue -> when (
                val result = JessieCodeExpressionFunction.compile(
                    source = value.value,
                    board = board,
                    variableNames = variableNames,
                )
            ) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    CurveError.ExpressionCompile(
                        term = termName,
                        error = result.error,
                    ),
                )
            }
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = returnsArray,
                ),
            )
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = termName,
                    actualType = typeName(value),
                ),
            )
        }

    private fun splineCoordinatePoint(
        x: JessieCodeRuntimeValue,
        y: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<CurveSplinePoint, CurveError> {
        val xTerm = when (
            val result = splineCoordinateTerm(x, "x", location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val yTerm = when (
            val result = splineCoordinateTerm(y, "y", location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            CurveCoordinateSplinePoint(
                xTerm = xTerm,
                yTerm = yTerm,
            ),
        )
    }

    private fun splineCoordinateTerm(
        value: JessieCodeRuntimeValue,
        coordinate: String,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeCoordinateFunction, CurveError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                JessieCodeNumericCoordinateFunction(value.value),
            )
            is JessieCodeRuntimeValue.FunctionValue -> GMResult.Ok(
                JessieCodeRuntimeCoordinateFunction(
                    function = value,
                    location = location,
                    returnsCoordinateArray = false,
                ),
            )
            else -> GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "spline.$coordinate",
                    actualType = typeName(value),
                ),
            )
        }

    private fun evaluateCardinalCoordinatePair(
        values: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue.ArrayValue, CurveError> {
        val evaluated = mutableListOf<JessieCodeRuntimeValue>()
        for ((index, value) in values.withIndex()) {
            if (value is JessieCodeRuntimeValue.FunctionValue) {
                when (
                    val result = value.externalCallable.call(
                        arguments = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> evaluated += result.value
                    is GMResult.Err -> return GMResult.Err(
                        CurveError.ExpressionEvaluation(
                            term = "cardinalspline.point[$index]",
                            error = result.error,
                        ),
                    )
                }
            } else {
                evaluated += value
            }
        }
        return GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(evaluated))
    }

    private fun evaluateCardinalPointFunction(
        function: JessieCodeRuntimeValue.FunctionValue,
        location: JessieCodeAstLocation,
    ): GMResult<JessieCodeRuntimeValue.ArrayValue, CurveError> =
        when (
            val result = function.externalCallable.call(
                arguments = emptyList(),
                location = location,
            )
        ) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "cardinalspline.point",
                    error = result.error,
                ),
            )
            is GMResult.Ok -> {
                val values = (
                    result.value as? JessieCodeRuntimeValue.ArrayValue
                    )?.values
                if (values?.size != 2) {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "cardinalspline.point",
                            actualType = typeName(result.value),
                        ),
                    )
                } else {
                    evaluateCardinalCoordinatePair(values, location)
                }
            }
        }

    private fun staticCurveCoordinates(
        board: Board,
        parent: PointParent,
        location: JessieCodeAstLocation,
    ): GMResult<Pair<Double, Double>, CurveError> =
        when (parent) {
            is PointParent.Existing ->
                GMResult.Ok(parent.point.X() to parent.point.Y())
            is PointParent.Coordinates -> {
                val x = when (
                    val result = staticCurveCoordinate(
                        board = board,
                        value = parent.values[0],
                        coordinate = "x",
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val y = when (
                    val result = staticCurveCoordinate(
                        board = board,
                        value = parent.values[1],
                        coordinate = "y",
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                GMResult.Ok(x to y)
            }
        }

    private fun staticCurveCoordinate(
        board: Board,
        value: JessieCodeRuntimeValue,
        coordinate: String,
        location: JessieCodeAstLocation,
    ): GMResult<Double, CurveError> {
        val evaluated = when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                return GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.StringValue -> {
                val expression = when (
                    val result = JessieCodeExpressionFunction.compile(
                        source = value.value,
                        board = board,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        CurveError.ExpressionCompile(
                            term = "cardinalspline.$coordinate",
                            error = result.error,
                        ),
                    )
                }
                expression.evaluate()
            }
            is JessieCodeRuntimeValue.FunctionValue ->
                value.externalCallable.call(
                    arguments = emptyList(),
                    location = location,
                )
            else -> return GMResult.Err(
                CurveError.NonNumericExpression(
                    term = "cardinalspline.$coordinate",
                    actualType = typeName(value),
                ),
            )
        }
        return when (evaluated) {
            is GMResult.Err -> GMResult.Err(
                CurveError.ExpressionEvaluation(
                    term = "cardinalspline.$coordinate",
                    error = evaluated.error,
                ),
            )
            is GMResult.Ok -> {
                val number = evaluated.value as?
                    JessieCodeRuntimeValue.NumberValue
                if (number == null) {
                    GMResult.Err(
                        CurveError.NonNumericExpression(
                            term = "cardinalspline.$coordinate",
                            actualType = typeName(evaluated.value),
                        ),
                    )
                } else {
                    GMResult.Ok(number.value)
                }
            }
        }
    }

    private fun numericArray(
        value: JessieCodeRuntimeValue,
    ): DoubleArray? {
        val values = (value as? JessieCodeRuntimeValue.ArrayValue)?.values
            ?: return null
        val result = DoubleArray(values.size)
        for ((index, item) in values.withIndex()) {
            result[index] = (
                item as? JessieCodeRuntimeValue.NumberValue
            )?.value ?: return null
        }
        return result
    }

    private fun curveStepTerm(
        value: JessieCodeRuntimeValue,
    ): CurveStepTerm? =
        when (value) {
            is JessieCodeRuntimeValue.ArrayValue -> {
                if (
                    value.values.any {
                        it !is JessieCodeRuntimeValue.NumberValue
                    }
                ) {
                    null
                } else {
                    RuntimeCurveStepTerm(value)
                }
            }
            is JessieCodeRuntimeValue.FunctionValue ->
                RuntimeCurveStepTerm(value)
            else -> null
        }

    private fun curveTermSource(
        value: JessieCodeRuntimeValue,
    ): String? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(value.value)
            is JessieCodeRuntimeValue.StringValue -> value.value
            else -> null
        }

    private fun createElementRadiusCircle(
        board: Board,
        centerParent: PointParent,
        radiusElement: GeometryElement,
        attributes: CreatorAttributes,
        location: JessieCodeAstLocation,
    ): CreatorResult {
        val center = when (
            val result = materializePoint(
                board = board,
                parent = centerParent,
                coordinateLocation = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return failure(
                creatorName = "circle",
                error = JessieCodeCreatorError.PointFactory(result.error),
                location = location,
            )
        }
        val ownsCenter = centerParent is PointParent.Coordinates
        fun cleanupCenter() {
            if (ownsCenter) {
                board.removeObject(center)
            }
        }
        val result = when (radiusElement) {
            is Line -> Circle.create(
                board = board,
                center = center,
                radiusLine = radiusElement,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is Circle -> Circle.create(
                board = board,
                center = center,
                radiusCircle = radiusElement,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            else -> {
                cleanupCenter()
                return failure(
                    creatorName = "circle",
                    error = JessieCodeCreatorError.UnsupportedParents(
                        listOf(radiusElement.elType),
                    ),
                    location = location,
                )
            }
        }
        return when (result) {
            is GMResult.Ok -> element(result.value)
            is GMResult.Err -> {
                cleanupCenter()
                failure(
                    creatorName = "circle",
                    error = JessieCodeCreatorError.CircleFactory(result.error),
                    location = location,
                )
            }
        }
    }

    private fun createRadiusCircle(
        board: Board,
        center: Point,
        radius: RadiusParent,
        attributes: CreatorAttributes,
        nonnegativeOnly: Boolean,
        location: JessieCodeAstLocation,
    ): GMResult<Circle, CircleError> =
        when (radius) {
            is RadiusParent.Number -> Circle.create(
                board = board,
                center = center,
                radius = radius.value,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is RadiusParent.Expression -> Circle.create(
                board = board,
                center = center,
                radiusExpression = radius.source,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is RadiusParent.Function -> Circle.create(
                board = board,
                center = center,
                radiusFunction = radius.value,
                radiusFunctionLocation = location,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        }

    private fun createSegment(
        board: Board,
        point1: Point,
        point2: Point,
        fixedLength: SegmentLengthParent?,
        nonnegativeOnly: Boolean,
        attributes: CreatorAttributes,
        location: JessieCodeAstLocation,
    ): GMResult<Line, LineError> =
        when (fixedLength) {
            null -> Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLength = null,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is SegmentLengthParent.Number -> Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLength = fixedLength.value,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is SegmentLengthParent.Expression -> Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLengthExpression = fixedLength.source,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
            is SegmentLengthParent.Function -> Line.createSegment(
                board = board,
                point1 = point1,
                point2 = point2,
                fixedLengthFunction = fixedLength.value,
                fixedLengthFunctionLocation = location,
                nonnegativeOnly = nonnegativeOnly,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
            )
        }

    private fun coefficientPoints(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
    ): GMResult<MaterializedPointParents, PointError> {
        val coefficients = parents.mapNotNull {
            (it as? JessieCodeRuntimeValue.NumberValue)?.value
        }
        if (coefficients.size != parents.size) {
            return GMResult.Err(PointError.InvalidCoordinateCount(parents.size))
        }
        val a = coefficients[0]
        val b = coefficients[1]
        val c = coefficients[2]
        val homogeneous = c * c + b * b
        val point1 = when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(
                    homogeneous,
                    c - b * a + c,
                    -b - c * a - b,
                ),
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = Point.create(
                board = board,
                coordinates = doubleArrayOf(
                    homogeneous,
                    -b * a + c,
                    -c * a - b,
                ),
                name = "",
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(point1)
                return result
            }
        }
        return GMResult.Ok(
            MaterializedPointParents(
                points = listOf(point1, point2),
                ownedPoints = setOf(point1, point2),
            ),
        )
    }

    private fun pointParent(
        board: Board,
        value: JessieCodeRuntimeValue,
    ): PointParent? {
        val selected = resolveElement(board, value)
        if (selected is Point) {
            return PointParent.Existing(selected)
        }
        val coordinates = (
            value as? JessieCodeRuntimeValue.ArrayValue
        )?.values ?: return null
        if (
            coordinates.size < 2 ||
            coordinates.any {
                it !is JessieCodeRuntimeValue.NumberValue &&
                    it !is JessieCodeRuntimeValue.StringValue
            }
        ) {
            return null
        }
        return PointParent.Coordinates(coordinates.toList())
    }

    private fun materializeThreePointParents(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        creatorName: String,
        location: JessieCodeAstLocation,
    ): GMResult<MaterializedPointParents, JessieCodeRuntimeError> =
        materializePointParents(
            board = board,
            parents = parents,
            creatorName = creatorName,
            expectedCount = 3,
            location = location,
        )

    private fun materializePointParents(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        creatorName: String,
        expectedCount: Int,
        location: JessieCodeAstLocation,
    ): GMResult<MaterializedPointParents, JessieCodeRuntimeError> {
        if (parents.size != expectedCount) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedParents(
                    parents.map(::typeName),
                ),
                location = location,
            )
        }
        val pointParents = parents.map { parent ->
            pointParent(board, parent)
                ?: return failure(
                    creatorName = creatorName,
                    error = JessieCodeCreatorError.UnsupportedParents(
                        parents.map(::typeName),
                    ),
                    location = location,
                )
        }
        val points = mutableListOf<Point>()
        val ownedPoints = linkedSetOf<Point>()
        for (pointParent in pointParents) {
            when (
                val result = materializePoint(
                    board = board,
                    parent = pointParent,
                    coordinateLocation = location,
                )
            ) {
                is GMResult.Ok -> {
                    points += result.value
                    if (pointParent is PointParent.Coordinates) {
                        ownedPoints += result.value
                    }
                }
                is GMResult.Err -> {
                    board.removeObjects(ownedPoints)
                    return failure(
                        creatorName = creatorName,
                        error = JessieCodeCreatorError.PointFactory(
                            result.error,
                        ),
                        location = location,
                    )
                }
            }
        }
        return GMResult.Ok(
            MaterializedPointParents(
                points = points,
                ownedPoints = ownedPoints,
            ),
        )
    }

    private fun materializePointLineParents(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        creatorName: String,
        location: JessieCodeAstLocation,
    ): GMResult<MaterializedPointLineParents, JessieCodeRuntimeError> {
        if (parents.size != 2) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedParents(
                    parents.map(::typeName),
                ),
                location = location,
            )
        }
        val firstPoint = pointParent(board, parents[0])
        val secondPoint = pointParent(board, parents[1])
        val firstLine = resolveElement(board, parents[0]) as? Line
        val secondLine = resolveElement(board, parents[1]) as? Line
        val pointParent: PointParent
        val line: Line
        when {
            firstPoint != null && secondLine != null -> {
                pointParent = firstPoint
                line = secondLine
            }
            secondPoint != null && firstLine != null -> {
                pointParent = secondPoint
                line = firstLine
            }
            else -> return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedParents(
                    parents.map(::typeName),
                ),
                location = location,
            )
        }
        return when (
            val result = materializePoint(
                board = board,
                parent = pointParent,
                coordinateLocation = location,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(
                MaterializedPointLineParents(
                    point = result.value,
                    line = line,
                    ownsPoint = pointParent is PointParent.Coordinates,
                ),
            )
            is GMResult.Err -> failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.PointFactory(result.error),
                location = location,
            )
        }
    }

    private fun cleanupOwnedPoint(
        board: Board,
        materialized: MaterializedPointLineParents,
    ) {
        if (materialized.ownsPoint) {
            board.removeObject(materialized.point)
        }
    }

    private fun materializePoint(
        board: Board,
        parent: PointParent,
        coordinateLocation: JessieCodeAstLocation,
    ): GMResult<Point, PointError> =
        when (parent) {
            is PointParent.Existing -> GMResult.Ok(parent.point)
            is PointParent.Coordinates -> createPointFromCoordinates(
                board = board,
                coordinates = parent.values,
                attributes = CreatorAttributes(
                    id = "",
                    name = "",
                    needsRegularUpdate = true,
                ),
                coordinateLocation = coordinateLocation,
            )
        }

    private fun createPointFromCoordinates(
        board: Board,
        coordinates: List<JessieCodeRuntimeValue>,
        attributes: CreatorAttributes,
        coordinateLocation: JessieCodeAstLocation,
    ): GMResult<Point, PointError> {
        val numericCoordinates = coordinates.mapNotNull {
            (it as? JessieCodeRuntimeValue.NumberValue)?.value
        }
        return if (numericCoordinates.size == coordinates.size) {
            Point.create(
                board = board,
                coordinates = numericCoordinates.toDoubleArray(),
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
            )
        } else if (
            coordinates.any {
                it is JessieCodeRuntimeValue.FunctionValue
            } &&
            coordinates.all {
                it is JessieCodeRuntimeValue.NumberValue ||
                    it is JessieCodeRuntimeValue.StringValue ||
                    it is JessieCodeRuntimeValue.FunctionValue
            }
        ) {
            val functions =
                mutableListOf<JessieCodeCoordinateFunction>()
            for ((index, coordinate) in coordinates.withIndex()) {
                when (coordinate) {
                    is JessieCodeRuntimeValue.NumberValue ->
                        functions +=
                            JessieCodeNumericCoordinateFunction(
                                coordinate.value,
                            )
                    is JessieCodeRuntimeValue.StringValue -> {
                        when (
                            val result =
                                JessieCodeExpressionFunction.compile(
                                    source = coordinate.value,
                                    board = board,
                                )
                        ) {
                            is GMResult.Ok -> functions += result.value
                            is GMResult.Err -> return GMResult.Err(
                                PointError.CoordinateExpressionCompile(
                                    coordinateIndex = index,
                                    error = result.error,
                                ),
                            )
                        }
                    }
                    is JessieCodeRuntimeValue.FunctionValue ->
                        functions += JessieCodeRuntimeCoordinateFunction(
                            function = coordinate,
                            location = coordinateLocation,
                            returnsCoordinateArray =
                                coordinates.size == 1,
                        )
                    else -> return GMResult.Err(
                        PointError.InvalidCoordinateCount(
                            coordinates.size,
                        ),
                    )
                }
            }
            Point.createConstrained(
                board = board,
                coordinateFunctions = functions,
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
                xjc = (
                    coordinates.getOrNull(0) as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value,
                yjc = (
                    coordinates.getOrNull(1) as?
                        JessieCodeRuntimeValue.StringValue
                    )?.value,
            )
        } else if (
            coordinates.all {
                it is JessieCodeRuntimeValue.NumberValue ||
                    it is JessieCodeRuntimeValue.StringValue
            }
        ) {
            Point.create(
                board = board,
                coordinateExpressions = coordinates.map {
                    when (it) {
                        is JessieCodeRuntimeValue.NumberValue ->
                            JsNumberFormat.compact(it.value)
                        is JessieCodeRuntimeValue.StringValue -> it.value
                        else -> ""
                    }
                },
                id = attributes.id,
                name = attributes.name,
                needsRegularUpdate = attributes.needsRegularUpdate,
                fixed = attributes.fixed,
            )
        } else {
            GMResult.Err(
                PointError.InvalidCoordinateCount(coordinates.size),
            )
        }
    }

    private fun radiusParent(
        value: JessieCodeRuntimeValue,
    ): RadiusParent? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                RadiusParent.Number(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                RadiusParent.Expression(value.value)
            is JessieCodeRuntimeValue.FunctionValue ->
                RadiusParent.Function(value)
            else -> null
        }

    private fun intersectionIndexParent(
        value: JessieCodeRuntimeValue?,
        location: JessieCodeAstLocation,
    ): IntersectionIndexSource? =
        when (value) {
            null,
            JessieCodeRuntimeValue.NullValue,
            JessieCodeRuntimeValue.UndefinedValue,
            -> IntersectionIndexSource.Number(0.0)
            is JessieCodeRuntimeValue.NumberValue ->
                IntersectionIndexSource.Number(value.value)
            is JessieCodeRuntimeValue.FunctionValue ->
                IntersectionIndexSource.Function(
                    value = value,
                    location = location,
                )
            else -> null
        }

    private fun segmentLengthParent(
        value: JessieCodeRuntimeValue,
    ): SegmentLengthParent? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue ->
                SegmentLengthParent.Number(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                SegmentLengthParent.Expression(value.value)
            is JessieCodeRuntimeValue.FunctionValue ->
                SegmentLengthParent.Function(value)
            else -> null
        }

    private fun resolveElement(
        board: Board,
        value: JessieCodeRuntimeValue,
    ): GeometryElement? =
        when (value) {
            is JessieCodeRuntimeValue.ElementReference -> value.element
            is JessieCodeRuntimeValue.StringValue -> board.select(value.value)
            else -> null
        }

    private fun creatorAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
        defaultNeedsRegularUpdate: Boolean = true,
    ): GMResult<CreatorAttributes, JessieCodeRuntimeError> {
        val id = when (
            val result = stringAttribute(
                creatorName,
                attributes,
                "id",
                default = "",
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val name = when (
            val result = nullableStringAttribute(
                creatorName,
                attributes,
                "name",
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val needsRegularUpdate = when (
            val result = booleanAttribute(
                creatorName,
                attributes,
                "needsregularupdate",
                default = defaultNeedsRegularUpdate,
                location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            CreatorAttributes(
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            ),
        )
    }

    private fun nestedCreatorIdentity(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<CreatorAttributes, JessieCodeRuntimeError> {
        val nested = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = name,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return creatorAttributes(
            creatorName = creatorName,
            attributes = nested,
            location = location,
        )
    }

    private fun nestedPointCreatorAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<CreatorAttributes, JessieCodeRuntimeError> {
        val nested = when (
            val result = nestedObjectAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = name,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = nested,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val fixed = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = nested,
                name = "fixed",
                default = false,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            identity.copy(
                name = identity.name ?: "",
                fixed = fixed,
            ),
        )
    }

    private fun nestedObjectAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<
        JessieCodeRuntimeValue.ObjectValue,
        JessieCodeRuntimeError,
        > {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(emptyMap()),
            )
        if (
            value === JessieCodeRuntimeValue.UndefinedValue ||
            value === JessieCodeRuntimeValue.NullValue
        ) {
            return GMResult.Ok(
                JessieCodeRuntimeValue.ObjectValue(emptyMap()),
            )
        }
        return if (value is JessieCodeRuntimeValue.ObjectValue) {
            GMResult.Ok(value)
        } else {
            invalidAttribute(
                creatorName = creatorName,
                attribute = name,
                expected = "object",
                actual = value,
                location = location,
            )
        }
    }

    private fun stringAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: String,
        location: JessieCodeAstLocation,
    ): GMResult<String, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        return if (value is JessieCodeRuntimeValue.StringValue) {
            GMResult.Ok(value.value)
        } else {
            invalidAttribute(
                creatorName,
                name,
                "string",
                value,
                location,
            )
        }
    }

    private fun nullableStringAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<String?, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(null)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(null)
        }
        return if (value is JessieCodeRuntimeValue.StringValue) {
            GMResult.Ok(value.value)
        } else {
            invalidAttribute(
                creatorName,
                name,
                "string",
                value,
                location,
            )
        }
    }

    private fun stringListAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        location: JessieCodeAstLocation,
    ): GMResult<List<String>, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(emptyList())
        if (
            value === JessieCodeRuntimeValue.UndefinedValue ||
            value === JessieCodeRuntimeValue.NullValue
        ) {
            return GMResult.Ok(emptyList())
        }
        val array = value as? JessieCodeRuntimeValue.ArrayValue
            ?: return invalidAttribute(
                creatorName,
                name,
                "array of strings",
                value,
                location,
            )
        val strings = mutableListOf<String>()
        for (item in array.values) {
            val string = (item as? JessieCodeRuntimeValue.StringValue)?.value
                ?: return invalidAttribute(
                    creatorName,
                    name,
                    "array of strings",
                    item,
                    location,
                )
            strings += string
        }
        return GMResult.Ok(strings)
    }

    private fun booleanAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Boolean,
        location: JessieCodeAstLocation,
    ): GMResult<Boolean, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        return if (value is JessieCodeRuntimeValue.BooleanValue) {
            GMResult.Ok(value.value)
        } else {
            invalidAttribute(
                creatorName,
                name,
                "boolean",
                value,
                location,
            )
        }
    }

    private fun numberAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Double,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
            ?: return invalidAttribute(
                creatorName,
                name,
                "number",
                value,
                location,
            )
        return if (number.isFinite()) {
            GMResult.Ok(number)
        } else {
            failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = name,
                    actual = number.toString(),
                ),
                location = location,
            )
        }
    }

    private fun integerAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: Int,
        minimum: Int,
        maximum: Int,
        location: JessieCodeAstLocation,
    ): GMResult<Int, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        val number = (value as? JessieCodeRuntimeValue.NumberValue)?.value
            ?: return invalidAttribute(
                creatorName,
                name,
                "integer",
                value,
                location,
            )
        val integer = number.toInt()
        if (
            !number.isFinite() ||
            integer.toDouble() != number ||
            integer !in minimum..maximum
        ) {
            return failure(
                creatorName = creatorName,
                error = JessieCodeCreatorError.UnsupportedAttributeValue(
                    attribute = name,
                    actual = number.toString(),
                ),
                location = location,
            )
        }
        return GMResult.Ok(integer)
    }

    private fun <T> invalidAttribute(
        creatorName: String,
        attribute: String,
        expected: String,
        actual: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<T, JessieCodeRuntimeError> =
        failure(
            creatorName = creatorName,
            error = JessieCodeCreatorError.InvalidAttributeType(
                attribute = attribute,
                expected = expected,
                actual = typeName(actual),
            ),
            location = location,
        )

    private fun unsupported(
        creatorName: String,
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): CreatorResult =
        failure(
            creatorName = creatorName,
            error = JessieCodeCreatorError.UnsupportedParents(
                parentTypes = parents.map(::typeName),
            ),
            location = location,
        )

    private fun element(element: GeometryElement): CreatorResult =
        GMResult.Ok(JessieCodeRuntimeValue.ElementReference(element))

    private fun <T> failure(
        creatorName: String,
        error: JessieCodeCreatorError,
        location: JessieCodeAstLocation,
    ): GMResult<T, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.CreatorFailure(
                creatorName = creatorName,
                error = error,
                location = location,
            ),
        )

    private fun typeName(value: JessieCodeRuntimeValue): String =
        when (value) {
            JessieCodeRuntimeValue.UndefinedValue -> "undefined"
            JessieCodeRuntimeValue.NullValue -> "null"
            is JessieCodeRuntimeValue.NumberValue -> "number"
            is JessieCodeRuntimeValue.BooleanValue -> "boolean"
            is JessieCodeRuntimeValue.StringValue -> "string"
            is JessieCodeRuntimeValue.ArrayValue -> "array"
            is JessieCodeRuntimeValue.ObjectValue -> "object"
            is JessieCodeRuntimeValue.FunctionValue -> "function"
            is JessieCodeRuntimeValue.BoardReference -> "board"
            is JessieCodeRuntimeValue.TransformationReference ->
                "transformation"
            is JessieCodeRuntimeValue.CompositionReference ->
                value.composition.elType.ifEmpty { "composition" }
            is JessieCodeRuntimeValue.ElementReference ->
                value.element.elType.ifEmpty { "element" }
        }

    private data class CreatorAttributes(
        val id: String,
        val name: String?,
        val needsRegularUpdate: Boolean,
        val fixed: Boolean = false,
    )

    private data class ParsedAxes3DAttributes(
        val axesPosition: String,
        val planeTypes: Map<String, String>,
        val planeSurfaceAttributes:
            Map<String, Plane3DSurfaceAttributes>,
        val ticksAttributes: Map<String, Axes3DTicksAttributes>,
        val needsRegularUpdate: Boolean,
    )

    private data class ProvidedLine3DPoint(
        val point: Point3D,
        val owned: Boolean,
    )

    private data class ParsedLine3DDirection(
        val source: Line3DDirectionSource,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedSphere3DRadius(
        val source: Line3DCoordinateValue,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedPlane3DDirection(
        val source: Plane3DDirectionSource,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedSurface3DScalarEvaluator(
        val evaluator: Surface3DScalarEvaluator,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedMesh3DPoint(
        val source: Mesh3DPointSource,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedMesh3DVector(
        val source: Mesh3DVectorSource,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedPolyhedron3DVertices(
        val sources: LinkedHashMap<String, Polyhedron3DVertexSource>,
        val dependencies: List<GeometryElement>,
    )

    private data class ParsedTicks3DPoint(
        val source: Ticks3DPointSource,
        val dependencies: List<GeometryElement>,
    )

    private val AXES_3D_PLANE_ROLES = listOf(
        "xPlaneRear",
        "xPlaneFront",
        "yPlaneRear",
        "yPlaneFront",
        "zPlaneRear",
        "zPlaneFront",
    )

    private data class ParsedLine3DValues(
        val values: List<Line3DCoordinateValue>,
        val dependencies: List<GeometryElement>,
    )

    private val FACE_3D_ATTRIBUTES = setOf(
        "id",
        "name",
        "needsregularupdate",
        "visible",
        "strokecolor",
        "fillcolor",
        "strokewidth",
        "strokeopacity",
        "fillopacity",
        "layer",
        "fixed",
        "highlight",
        "withlabel",
        "dash",
        "dashscale",
        "linecap",
        "shader",
    )
    private val FACE_3D_SHADER_ATTRIBUTES = setOf(
        "enabled",
        "fixed",
        "type",
        "hue",
        "saturation",
        "minlightness",
        "maxlightness",
        "light",
    )
    private val FACE_3D_LIGHT_ATTRIBUTES =
        setOf("type", "az", "el", "bank", "dir")
    private val PLANE_3D_COLORMAP_ATTRIBUTES =
        setOf("min", "max", "s", "v")

    private fun CreatorAttributes.toTangentToIdentity(): TangentToIdentity =
        TangentToIdentity(
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
        )

    private fun tangentToLineAttributes(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<TangentToLineAttributes, JessieCodeRuntimeError> {
        val identity = when (
            val result = creatorAttributes(
                creatorName = creatorName,
                attributes = attributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightFirst = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightfirst",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val straightLast = when (
            val result = booleanAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "straightlast",
                default = true,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point1 = when (
            val result = nestedCreatorIdentity(
                creatorName = creatorName,
                attributes = attributes,
                name = "point1",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val point2 = when (
            val result = nestedCreatorIdentity(
                creatorName = creatorName,
                attributes = attributes,
                name = "point2",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            TangentToLineAttributes(
                identity = identity.toTangentToIdentity(),
                straightFirst = straightFirst,
                straightLast = straightLast,
                point1 = point1.toTangentToIdentity(),
                point2 = point2.toTangentToIdentity(),
            ),
        )
    }

    private fun CreatorAttributes.toBisectorLineAttributes():
        BisectorLineAttributes =
        BisectorLineAttributes(
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
        )

    private enum class PointReflectionCreatorKind(
        val creatorName: String,
    ) {
        REFLECTION("reflection"),
        MIRROR_ELEMENT("mirrorelement"),
        MIRROR_POINT("mirrorpoint"),
    }

    private enum class PolygonCreatorKind(
        val elementType: String,
    ) {
        POLYGON("polygon"),
        POLYGONAL_CHAIN("polygonalchain"),
    }

    private data class ArcAttributes(
        val selection: String,
        val orientation: String,
        val useDirection: Boolean,
    )

    private enum class ArcCreatorKind(
        val elementType: String,
        val selectionOverride: String? = null,
        val useDirectionOverride: Boolean? = null,
    ) {
        ARC("arc"),
        SEMICIRCLE(
            elementType = "semicircle",
            useDirectionOverride = false,
        ),
        CIRCUMCIRCLE_ARC(
            elementType = "circumcirclearc",
            useDirectionOverride = true,
        ),
        MINOR_ARC(
            elementType = "minorarc",
            selectionOverride = Arc.SELECTION_MINOR,
        ),
        MAJOR_ARC(
            elementType = "majorarc",
            selectionOverride = Arc.SELECTION_MAJOR,
        ),
    }

    private enum class SectorCreatorKind(
        val elementType: String,
        val isAngle: Boolean = false,
        val selectionOverride: String? = null,
        val useDirectionOverride: Boolean? = null,
    ) {
        SECTOR("sector"),
        CIRCUMCIRCLE_SECTOR(
            elementType = "circumcirclesector",
            useDirectionOverride = true,
        ),
        MINOR_SECTOR(
            elementType = "minorsector",
            selectionOverride = Arc.SELECTION_MINOR,
        ),
        MAJOR_SECTOR(
            elementType = "majorsector",
            selectionOverride = Arc.SELECTION_MAJOR,
        ),
        ANGLE(
            elementType = "angle",
            isAngle = true,
        ),
        NONREFLEX_ANGLE(
            elementType = "nonreflexangle",
            isAngle = true,
            selectionOverride = Arc.SELECTION_MINOR,
        ),
        REFLEX_ANGLE(
            elementType = "reflexangle",
            isAngle = true,
            selectionOverride = Arc.SELECTION_MAJOR,
        ),
    }

    private data class MaterializedPointParents(
        val points: List<Point>,
        val ownedPoints: Set<Point>,
    )

    private data class MaterializedPointLineParents(
        val point: Point,
        val line: Line,
        val ownsPoint: Boolean,
    )

    private data class FocalConicPointParent(
        val parent: PointParent,
        val parentless: Boolean,
    )

    private class RuntimeCurveStepTerm(
        private val value: JessieCodeRuntimeValue,
    ) : CurveStepTerm {
        override val length: Int
            get() = when (value) {
                is JessieCodeRuntimeValue.ArrayValue -> value.values.size
                is JessieCodeRuntimeValue.FunctionValue ->
                    value.parameterNames.size
                else -> 0
            }

        override val isFunction: Boolean
            get() = value is JessieCodeRuntimeValue.FunctionValue

        override fun valueAt(index: Int): Double =
            when (value) {
                is JessieCodeRuntimeValue.ArrayValue ->
                    (
                        value.values.getOrNull(index) as?
                            JessieCodeRuntimeValue.NumberValue
                    )?.value ?: Double.NaN
                is JessieCodeRuntimeValue.FunctionValue -> Double.NaN
                else -> Double.NaN
            }
    }

    private sealed interface PointParent {
        data class Existing(
            val point: Point,
        ) : PointParent

        data class Coordinates(
            val values: List<JessieCodeRuntimeValue>,
        ) : PointParent
    }

    private sealed interface RadiusParent {
        data class Number(
            val value: Double,
        ) : RadiusParent

        data class Expression(
            val source: String,
        ) : RadiusParent

        data class Function(
            val value: JessieCodeRuntimeValue.FunctionValue,
        ) : RadiusParent
    }

    private sealed interface SegmentLengthParent {
        data class Number(
            val value: Double,
        ) : SegmentLengthParent

        data class Expression(
            val source: String,
        ) : SegmentLengthParent

        data class Function(
            val value: JessieCodeRuntimeValue.FunctionValue,
        ) : SegmentLengthParent
    }
}

private typealias CreatorResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>
