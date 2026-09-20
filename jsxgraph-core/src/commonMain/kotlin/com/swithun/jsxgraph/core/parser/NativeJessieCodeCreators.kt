/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/jessiecode.js -> creator / isCreator,
 * src/base/point.js -> createPoint / createPolePoint,
 * src/base/line.js -> createLine / createSegment / createArrow /
 * createRadicalAxis / createTangent / createTangentTo / createNormal /
 * createPolarLine,
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
 * src/element/arc.js -> createArc / createSemicircle /
 * createCircumcircleArc / createMinorArc / createMajorArc,
 * src/element/sector.js -> createSector / createAngle /
 * createCircumcircleSector / createMinorSector / createMajorSector /
 * createNonreflexAngle / createReflexAngle,
 * src/element/conic.js -> createEllipse / createHyperbola
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.AngleRadius
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.ArcError
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.BisectorLineAttributes
import com.swithun.jsxgraph.core.base.BisectorLines
import com.swithun.jsxgraph.core.base.BisectorLinesError
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircleError
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CircumcenterError
import com.swithun.jsxgraph.core.base.CircumcenterPoint
import com.swithun.jsxgraph.core.base.Curve
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
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Hyperbola
import com.swithun.jsxgraph.core.base.HyperbolaError
import com.swithun.jsxgraph.core.base.IncenterPoint
import com.swithun.jsxgraph.core.base.IncircleCircle
import com.swithun.jsxgraph.core.base.IntersectionError
import com.swithun.jsxgraph.core.base.IntersectionIndexSource
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.MidpointError
import com.swithun.jsxgraph.core.base.MidpointPoint
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
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.PointReflectionError
import com.swithun.jsxgraph.core.base.PointReflections
import com.swithun.jsxgraph.core.base.PolePoint
import com.swithun.jsxgraph.core.base.PolePointError
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.PolygonError
import com.swithun.jsxgraph.core.base.RegularPolygon
import com.swithun.jsxgraph.core.base.RegularPolygonError
import com.swithun.jsxgraph.core.base.RadicalAxis
import com.swithun.jsxgraph.core.base.RadicalAxisError
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.SectorError
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TextError
import com.swithun.jsxgraph.core.base.Tangent
import com.swithun.jsxgraph.core.base.TangentError
import com.swithun.jsxgraph.core.base.TangentTo
import com.swithun.jsxgraph.core.base.TangentToError
import com.swithun.jsxgraph.core.base.TangentToIdentity
import com.swithun.jsxgraph.core.base.TangentToLineAttributes
import com.swithun.jsxgraph.core.base.TangentToPointAttributes
import com.swithun.jsxgraph.core.base.Transformation
import com.swithun.jsxgraph.core.base.TransformationDynamicParameter
import com.swithun.jsxgraph.core.base.TransformationDynamicParameterError
import com.swithun.jsxgraph.core.base.TransformationError
import com.swithun.jsxgraph.core.base.TransformationParameter
import com.swithun.jsxgraph.core.base.TriangleCenterConstructionError
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
    private val creators = mapOf(
        "transform" to JessieCodeCreator {
                board,
                parents,
                attributes,
                location,
            ->
            createTransform(board, parents, attributes, location)
        },
        "point" to JessieCodeCreator { board, parents, attributes, location ->
            createPoint(board, parents, attributes, location)
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
                val parameters = listOf(
                    angle,
                    TransformationParameter.Numeric(x),
                    TransformationParameter.Numeric(y),
                )
                return Transformation.create(
                    board = board,
                    type = "rotate",
                    parameters = parameters,
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
    ): CreatorResult =
        when (result) {
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.TransformationReference(
                    result.value,
                ),
            )
            is GMResult.Err -> failure(
                creatorName = "transform",
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
                default = true,
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
