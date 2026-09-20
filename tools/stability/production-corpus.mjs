const fixedAttributes = {
    name: "",
    withLabel: false,
    fixed: true,
    highlight: false
};

const hiddenPointAttributes = {
    ...fixedAttributes,
    visible: false
};

function point(id, x, y, attributes = {}) {
    return {
        id,
        type: "point",
        parents: [x, y],
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function line(id, first, second, attributes = {}) {
    return {
        id,
        type: "line",
        parents: [first, second],
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function segment(
    id,
    first,
    second,
    attributes = {},
    fixedLength = undefined
) {
    return {
        id,
        type: "segment",
        parents: fixedLength === undefined
            ? [first, second]
            : [first, second, fixedLength],
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function circle(id, center, radius, attributes = {}) {
    return {
        id,
        type: "circle",
        parents: [center, radius],
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function circumcircle(id, first, second, third, attributes = {}) {
    return {
        id,
        type: "circle",
        parents: [first, second, third],
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function midpoint(id, parents, attributes = {}) {
    return {
        id,
        type: "midpoint",
        parents,
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function orthogonal(id, type, first, second, attributes = {}) {
    return {
        id,
        type,
        parents: [first, second],
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function curve(id, type, parents, attributes = {}) {
    return {
        id,
        type,
        parents,
        attributes: {
            ...fixedAttributes,
            fillColor: "none",
            doAdvancedPlot: false,
            numberPointsHigh: 480,
            ...attributes
        }
    };
}

function polygon(id, parents, attributes = {}) {
    return {
        id,
        type: "polygon",
        parents,
        attributes: {
            ...fixedAttributes,
            ...attributes
        }
    };
}

function text(id, x, y, content, attributes = {}) {
    return {
        id,
        type: "text",
        parents: [x, y, content],
        attributes: {
            ...fixedAttributes,
            fontUnit: "px",
            display: "internal",
            parse: false,
            ...attributes
        }
    };
}

function circular(id, type, parents, attributes = {}) {
    return {
        id,
        type,
        parents,
        attributes: {
            ...fixedAttributes,
            selection: "minor",
            orientation: "counterclockwise",
            ...attributes
        }
    };
}

function documentSource(
    objects,
    {
        boundingBox = [-6, 5, 6, -5],
        axis = true,
        grid = true,
        keepAspectRatio = true
    } = {}
) {
    return JSON.stringify({
        schemaVersion: 1,
        boundingBox,
        axis,
        grid,
        keepAspectRatio,
        objects
    }, null, 2);
}

function productionCase({
    id,
    title,
    scenario,
    objects,
    expectedElementIds,
    expectedTexts = [],
    features,
    document = {},
    interaction = null
}) {
    return {
        id,
        title,
        scenario,
        source: documentSource(objects, document),
        expectedElementIds,
        expectedTexts,
        features,
        interaction
    };
}

export const requiredFeatures = [
    "angle",
    "arc",
    "auto-angle-radius",
    "circle",
    "circle-fill",
    "nonnegative-circle-radius",
    "clockwise-orientation",
    "concave-polygon",
    "coordinate-parents",
    "circumcircle",
    "cubic-bezier",
    "data-plot",
    "dependent-update",
    "direction-point-arc",
    "dynamic-segment-length",
    "dynamic-text",
    "element-parents",
    "fill-opacity",
    "fixed-angle-radius",
    "fixed-length-segment",
    "fixed-point",
    "free-point",
    "functiongraph",
    "grid",
    "hidden-point",
    "implicit-vertices",
    "infinite-line",
    "interaction-state",
    "keep-aspect-ratio",
    "line",
    "major-arc",
    "mixed-scene",
    "midpoint",
    "line-parent-midpoint",
    "numeric-text",
    "orthogonal-projection",
    "parametric-curve",
    "perpendicular-line",
    "perpendicular-point",
    "perpendicular-segment",
    "point",
    "point-style",
    "polygon",
    "polygon-border",
    "borderless-polygon",
    "sector",
    "segment",
    "shared-vertices",
    "shifted-bounds",
    "stroke-width",
    "stretched-viewport",
    "style-colors",
    "text",
    "text-anchors",
    "text-opacity"
];

export const cases = [
    productionCase({
        id: "prod_geometry_segment_network",
        title: "Survey segment network",
        scenario: "A bounded survey triangle with explicit control points and segment edges.",
        objects: [
            point("surveyA", -4.5, -2.5, {
                size: 5,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("surveyB", 3.8, -1.8, {
                size: 5,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("surveyC", 0.8, 3.7, {
                size: 6,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2.5
            }),
            segment("edgeAB", "surveyA", "surveyB", {
                strokeColor: "#314652",
                strokeWidth: 2.5
            }),
            segment("edgeBC", "surveyB", "surveyC", {
                strokeColor: "#314652",
                strokeWidth: 2.5
            }),
            segment("edgeCA", "surveyC", "surveyA", {
                strokeColor: "#314652",
                strokeWidth: 2.5
            })
        ],
        expectedElementIds: [
            "surveyA",
            "surveyB",
            "surveyC",
            "edgeAB",
            "edgeBC",
            "edgeCA"
        ],
        features: [
            "point",
            "point-style",
            "fixed-point",
            "segment",
            "element-parents",
            "style-colors",
            "stroke-width",
            "grid",
            "keep-aspect-ratio"
        ]
    }),
    productionCase({
        id: "prod_geometry_fixed_length_segment",
        title: "Dynamic constrained survey arm",
        scenario: "A free driver controls the exact length of a segment with a fixed anchor.",
        objects: [
            point("lengthDriver", 3, -2.5, {
                name: "LengthDriver",
                size: 5,
                strokeColor: "#6C3FA0",
                fillColor: "#FFFFFF",
                strokeWidth: 2,
                fixed: false
            }),
            point("fixedAnchor", -3, -1, {
                size: 6,
                strokeColor: "#314652",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("movableEndpoint", -1, -1, {
                size: 6,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2,
                fixed: false
            }),
            segment(
                "constrainedArm",
                "fixedAnchor",
                "movableEndpoint",
                {
                    strokeColor: "#167C73",
                    strokeWidth: 4
                },
                "LengthDriver.X() + 1"
            )
        ],
        expectedElementIds: [
            "lengthDriver",
            "fixedAnchor",
            "movableEndpoint",
            "constrainedArm"
        ],
        features: [
            "point",
            "fixed-point",
            "free-point",
            "segment",
            "fixed-length-segment",
            "dynamic-segment-length",
            "dependent-update",
            "interaction-state",
            "element-parents",
            "style-colors",
            "stroke-width"
        ],
        interaction: {
            pointId: "lengthDriver",
            target: [5, -2.5]
        }
    }),
    productionCase({
        id: "prod_geometry_infinite_crosshair",
        title: "Infinite alignment guides",
        scenario: "Two coordinate-parent guide lines crossing a calibration circle.",
        objects: [
            line("horizontalGuide", [-5.5, 1.25], [5.5, 1.25], {
                strokeColor: "#6C737A",
                strokeWidth: 1.5
            }),
            line("diagonalGuide", [-4.5, -3.5], [4.5, 4.0], {
                strokeColor: "#D15B42",
                strokeWidth: 2
            }),
            circle("calibrationRing", [0.5, 0.25], 2.2, {
                strokeColor: "#167C73",
                fillColor: "none",
                strokeWidth: 2.5
            })
        ],
        expectedElementIds: [
            "horizontalGuide",
            "diagonalGuide",
            "calibrationRing"
        ],
        features: [
            "line",
            "infinite-line",
            "coordinate-parents",
            "circle",
            "style-colors",
            "stroke-width"
        ]
    }),
    productionCase({
        id: "prod_geometry_shifted_map",
        title: "Shifted coordinate map",
        scenario: "A non-origin viewport with independently scaled axes and hidden scaffolding.",
        objects: [
            point("mapOrigin", 12, 22, {
                ...hiddenPointAttributes
            }),
            point("mapTarget", 17.5, 27.5, {
                size: 6,
                strokeColor: "#9A4E1F",
                fillColor: "#F4D44D",
                strokeWidth: 2
            }),
            segment("mapVector", "mapOrigin", "mapTarget", {
                strokeColor: "#1F5A94",
                strokeWidth: 3
            }),
            circle("mapRange", "mapOrigin", 4.25, {
                strokeColor: "#167C73",
                fillColor: "#167C7330",
                fillOpacity: 0.35,
                strokeWidth: 2
            })
        ],
        expectedElementIds: [
            "mapOrigin",
            "mapTarget",
            "mapVector",
            "mapRange"
        ],
        features: [
            "shifted-bounds",
            "stretched-viewport",
            "hidden-point",
            "element-parents",
            "segment",
            "circle-fill",
            "fill-opacity"
        ],
        document: {
            boundingBox: [8, 31, 20, 19],
            keepAspectRatio: false
        }
    }),
    productionCase({
        id: "prod_geometry_circle_fill",
        title: "Concentric capacity zones",
        scenario: "Three filled circles communicate nested operating thresholds.",
        objects: [
            circle("outerZone", [0, 0], 4.2, {
                strokeColor: "#1F5A94",
                fillColor: "#1F5A94",
                fillOpacity: 0.10,
                strokeWidth: 2
            }),
            circle("middleZone", [0, 0], 2.8, {
                strokeColor: "#167C73",
                fillColor: "#167C73",
                fillOpacity: 0.18,
                strokeWidth: 2.5
            }),
            circle("innerZone", [0, 0], 1.2, {
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                fillOpacity: 0.45,
                strokeWidth: 3
            })
        ],
        expectedElementIds: ["outerZone", "middleZone", "innerZone"],
        features: [
            "circle",
            "circle-fill",
            "coordinate-parents",
            "fill-opacity",
            "stroke-width",
            "style-colors"
        ]
    }),
    productionCase({
        id: "prod_geometry_circumcircle",
        title: "Three-point survey circle",
        scenario: "Three survey Points define a circumcircle whose implicit center follows a moved parent.",
        objects: [
            point("circumA", -3, -2, {
                size: 5,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("circumB", 3, -1, {
                size: 6,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2,
                fixed: false
            }),
            point("circumC", 0, 3, {
                size: 5,
                strokeColor: "#167C73",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            circumcircle(
                "surveyCircumcircle",
                "circumA",
                "circumB",
                "circumC",
                {
                    strokeColor: "#167C73",
                    fillColor: "#7BC8B8",
                    fillOpacity: 0.12,
                    strokeWidth: 2.5
                }
            )
        ],
        expectedElementIds: [
            "circumA",
            "circumB",
            "circumC",
            "surveyCircumcircle"
        ],
        features: [
            "circle",
            "circumcircle",
            "dependent-update",
            "element-parents",
            "free-point",
            "interaction-state"
        ],
        interaction: {
            pointId: "circumB",
            target: [4, -2]
        }
    }),
    productionCase({
        id: "prod_geometry_midpoints",
        title: "Dependent route midpoints",
        scenario: "Point-pair and Line-parent midpoints follow their defining geometry.",
        objects: [
            point("midpointA", -4, -2, {
                size: 5,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("midpointB", 4, 2, {
                size: 6,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2,
                fixed: false
            }),
            segment("midpointRoute", "midpointA", "midpointB", {
                strokeColor: "#49545D",
                strokeWidth: 2
            }),
            midpoint(
                "routeMidpoint",
                ["midpointA", "midpointB"],
                {
                    size: 7,
                    strokeColor: "#167C73",
                    fillColor: "#7BC8B8",
                    strokeWidth: 2
                }
            ),
            point("lineMidpointA", -4, 3.2, {
                visible: false
            }),
            point("lineMidpointB", 2, 3.2, {
                visible: false
            }),
            segment(
                "lineMidpointSource",
                "lineMidpointA",
                "lineMidpointB",
                {
                    strokeColor: "#9A4E1F",
                    strokeWidth: 2.5
                }
            ),
            midpoint(
                "lineMidpoint",
                ["lineMidpointSource"],
                {
                    size: 6,
                    strokeColor: "#9A4E1F",
                    fillColor: "#FFFFFF",
                    strokeWidth: 2
                }
            )
        ],
        expectedElementIds: [
            "midpointA",
            "midpointB",
            "midpointRoute",
            "routeMidpoint",
            "lineMidpointA",
            "lineMidpointB",
            "lineMidpointSource",
            "lineMidpoint"
        ],
        features: [
            "midpoint",
            "line-parent-midpoint",
            "dependent-update",
            "element-parents",
            "free-point",
            "hidden-point",
            "interaction-state",
            "segment",
            "point-style"
        ],
        interaction: {
            pointId: "midpointB",
            target: [2, 4]
        }
    }),
    productionCase({
        id: "prod_geometry_orthogonal_constructions",
        title: "Orthogonal survey constructions",
        scenario: "Projection Points, an infinite perpendicular, and a finite drop follow movable survey controls.",
        objects: [
            point("orthogonalA", -4, -1, {
                visible: false
            }),
            point("orthogonalB", 2, 3, {
                visible: false
            }),
            line(
                "orthogonalBase",
                "orthogonalA",
                "orthogonalB",
                {
                    strokeColor: "#49545D",
                    strokeWidth: 2.5
                }
            ),
            point("orthogonalDriver", 3, -3, {
                size: 7,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2,
                fixed: false
            }),
            point("footDriver", -3.5, 3.8, {
                size: 5,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("dropDriver", 4, 3.5, {
                size: 6,
                strokeColor: "#6C3FA0",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            orthogonal(
                "projection",
                "orthogonalprojection",
                "orthogonalDriver",
                "orthogonalBase",
                {
                    size: 6,
                    strokeColor: "#167C73",
                    fillColor: "#7BC8B8",
                    strokeWidth: 2
                }
            ),
            orthogonal(
                "perpendicularFoot",
                "perpendicularpoint",
                "orthogonalBase",
                "footDriver",
                {
                    size: 6,
                    strokeColor: "#1F5A94",
                    fillColor: "#7BB7E8",
                    strokeWidth: 2
                }
            ),
            orthogonal(
                "normal",
                "perpendicular",
                "orthogonalDriver",
                "orthogonalBase",
                {
                    strokeColor: "#B44335",
                    strokeWidth: 2
                }
            ),
            orthogonal(
                "drop",
                "perpendicularsegment",
                "orthogonalBase",
                "dropDriver",
                {
                    strokeColor: "#167C73",
                    strokeWidth: 4
                }
            )
        ],
        expectedElementIds: [
            "orthogonalA",
            "orthogonalB",
            "orthogonalBase",
            "orthogonalDriver",
            "footDriver",
            "dropDriver",
            "projection",
            "perpendicularFoot",
            "normal",
            "drop"
        ],
        features: [
            "orthogonal-projection",
            "perpendicular-point",
            "perpendicular-line",
            "perpendicular-segment",
            "dependent-update",
            "element-parents",
            "free-point",
            "hidden-point",
            "interaction-state",
            "infinite-line",
            "segment",
            "point-style",
            "style-colors",
            "stroke-width"
        ],
        interaction: {
            pointId: "orthogonalDriver",
            target: [0, 4]
        }
    }),
    productionCase({
        id: "prod_curve_quadratic_trend",
        title: "Quadratic trend envelope",
        scenario: "A sampled function graph shows a bounded trend over the visible domain.",
        objects: [
            curve(
                "trend",
                "functiongraph",
                ["0.11 * x * x - 0.45 * x - 2.2", -5.5, 5.5],
                {
                    strokeColor: "#B44335",
                    strokeWidth: 3,
                    numberPointsHigh: 520
                }
            )
        ],
        expectedElementIds: ["trend"],
        features: [
            "functiongraph",
            "stroke-width",
            "style-colors",
            "grid"
        ]
    }),
    productionCase({
        id: "prod_curve_parametric_orbit",
        title: "Parametric orbit",
        scenario: "An offset elliptical orbit is sampled from two JessieCode expressions.",
        objects: [
            curve(
                "orbit",
                "curve",
                [
                    "3.6 * cos(x) - 0.4",
                    "1.8 * sin(x) + 0.7",
                    0,
                    6.283185307179586
                ],
                {
                    strokeColor: "#167C73",
                    strokeWidth: 2.75,
                    numberPointsHigh: 640
                }
            ),
            point("focus", -0.4, 0.7, {
                size: 4,
                strokeColor: "#9A4E1F",
                fillColor: "#F4D44D",
                strokeWidth: 2
            })
        ],
        expectedElementIds: ["orbit", "focus"],
        features: [
            "parametric-curve",
            "point",
            "style-colors",
            "stroke-width"
        ]
    }),
    productionCase({
        id: "prod_curve_sampled_series",
        title: "Sampled sensor series",
        scenario: "An irregular data series preserves ordered sample coordinates.",
        objects: [
            curve(
                "samples",
                "curve",
                [
                    [-5.5, -4.4, -3.1, -1.6, -0.2, 1.4, 2.9, 4.1, 5.4],
                    [-2.4, -1.0, -1.8, 0.4, 0.1, 1.9, 1.2, 3.0, 2.4]
                ],
                {
                    strokeColor: "#1F5A94",
                    strokeWidth: 2.5
                }
            )
        ],
        expectedElementIds: ["samples"],
        features: [
            "data-plot",
            "style-colors",
            "stroke-width"
        ]
    }),
    productionCase({
        id: "prod_curve_wave_comparison",
        title: "Wave comparison",
        scenario: "Two expression-driven curves compare phase-shifted periodic signals.",
        objects: [
            curve(
                "primaryWave",
                "functiongraph",
                ["1.7 * sin(x)", -6, 6],
                {
                    strokeColor: "#1F5A94",
                    strokeWidth: 2.5,
                    numberPointsHigh: 600
                }
            ),
            curve(
                "secondaryWave",
                "functiongraph",
                ["1.1 * cos(1.4 * x) + 1.6", -6, 6],
                {
                    strokeColor: "#B44335",
                    strokeWidth: 2,
                    numberPointsHigh: 600
                }
            )
        ],
        expectedElementIds: ["primaryWave", "secondaryWave"],
        features: [
            "functiongraph",
            "mixed-scene",
            "style-colors",
            "stroke-width"
        ]
    }),
    productionCase({
        id: "prod_polygon_convex_floorplan",
        title: "Convex floor plan",
        scenario: "A filled five-sided region uses implicit vertices and default borders.",
        objects: [
            polygon(
                "floorPlan",
                [[-4.8, -2.5], [-4.2, 2.6], [-0.5, 4], [3.9, 2.1], [4.6, -2.7]],
                {
                    strokeColor: "#1F5A94",
                    fillColor: "#7BB7E8",
                    fillOpacity: 0.28,
                    strokeWidth: 2.5
                }
            )
        ],
        expectedElementIds: ["floorPlan"],
        features: [
            "polygon",
            "polygon-border",
            "implicit-vertices",
            "fill-opacity",
            "stroke-width"
        ]
    }),
    productionCase({
        id: "prod_polygon_concave_zone",
        title: "Concave service zone",
        scenario: "A concave operating region validates fill winding and ordered vertices.",
        objects: [
            polygon(
                "serviceZone",
                [[-5, -3], [-4.2, 3.5], [-0.8, 1.2], [1.5, 4], [5, 2], [2.7, -3.2], [-0.4, -1.0]],
                {
                    strokeColor: "#167C73",
                    fillColor: "#84C7B8",
                    fillOpacity: 0.32,
                    strokeWidth: 2
                }
            )
        ],
        expectedElementIds: ["serviceZone"],
        features: [
            "polygon",
            "concave-polygon",
            "polygon-border",
            "implicit-vertices",
            "fill-opacity"
        ]
    }),
    productionCase({
        id: "prod_polygon_borderless_overlay",
        title: "Borderless overlay",
        scenario: "Two translucent triangles form an overlay without generated border lines.",
        objects: [
            polygon(
                "leftOverlay",
                [[-5, -2.8], [-1, 3.8], [1.2, -2.3]],
                {
                    withLines: false,
                    strokeColor: "#1F5A94",
                    fillColor: "#1F5A94",
                    fillOpacity: 0.25
                }
            ),
            polygon(
                "rightOverlay",
                [[-1.2, -2.3], [1.3, 3.2], [5, -2.8]],
                {
                    withLines: false,
                    strokeColor: "#B44335",
                    fillColor: "#B44335",
                    fillOpacity: 0.25
                }
            )
        ],
        expectedElementIds: ["leftOverlay", "rightOverlay"],
        features: [
            "polygon",
            "borderless-polygon",
            "implicit-vertices",
            "fill-opacity",
            "mixed-scene"
        ]
    }),
    productionCase({
        id: "prod_polygon_shared_vertices",
        title: "Shared-vertex parcel",
        scenario: "A polygon references source Points so later Board updates preserve ownership.",
        objects: [
            point("parcelA", -4.5, -2.8, {
                size: 4,
                strokeColor: "#314652",
                fillColor: "#FFFFFF"
            }),
            point("parcelB", -2.5, 3.2, {
                size: 4,
                strokeColor: "#314652",
                fillColor: "#FFFFFF"
            }),
            point("parcelC", 3.8, 2.7, {
                size: 4,
                strokeColor: "#314652",
                fillColor: "#FFFFFF"
            }),
            point("parcelD", 4.7, -2.5, {
                size: 4,
                strokeColor: "#314652",
                fillColor: "#FFFFFF"
            }),
            polygon(
                "parcel",
                ["parcelA", "parcelB", "parcelC", "parcelD"],
                {
                    strokeColor: "#9A4E1F",
                    fillColor: "#F4D44D",
                    fillOpacity: 0.24,
                    strokeWidth: 2
                }
            )
        ],
        expectedElementIds: [
            "parcelA",
            "parcelB",
            "parcelC",
            "parcelD",
            "parcel"
        ],
        features: [
            "polygon",
            "shared-vertices",
            "element-parents",
            "polygon-border",
            "point"
        ]
    }),
    productionCase({
        id: "prod_text_anchor_board",
        title: "Text anchor board",
        scenario: "Three labels exercise left, centered, and right anchor combinations.",
        objects: [
            text("northwest", -5.2, 3.6, "Northwest", {
                fontSize: 18,
                strokeColor: "#314652",
                anchorX: "left",
                anchorY: "bottom"
            }),
            text("center", 0, 0.5, "Centered status", {
                fontSize: 24,
                strokeColor: "#1F5A94",
                anchorX: "middle",
                anchorY: "middle"
            }),
            text("southeast", 5.2, -3.2, "Southeast", {
                fontSize: 18,
                strokeColor: "#B44335",
                anchorX: "right",
                anchorY: "top"
            })
        ],
        expectedElementIds: ["northwest", "center", "southeast"],
        expectedTexts: ["Northwest", "Centered status", "Southeast"],
        features: [
            "text",
            "text-anchors",
            "style-colors"
        ]
    }),
    productionCase({
        id: "prod_text_numeric_metric",
        title: "Numeric metric",
        scenario: "A numeric Text parent is formatted with a fixed digit budget.",
        objects: [
            text("metricTitle", 0, 1.0, "Measured coefficient", {
                fontSize: 18,
                strokeColor: "#314652",
                anchorX: "middle",
                anchorY: "bottom"
            }),
            text("metricValue", 0, -0.4, 2.718281828459045, {
                fontSize: 28,
                strokeColor: "#167C73",
                anchorX: "middle",
                anchorY: "middle",
                formatNumber: true,
                digits: 3
            })
        ],
        expectedElementIds: ["metricTitle", "metricValue"],
        expectedTexts: ["Measured coefficient", "2.718"],
        features: [
            "text",
            "numeric-text",
            "text-anchors"
        ]
    }),
    productionCase({
        id: "prod_text_dynamic_coordinate",
        title: "Dynamic coordinate readout",
        scenario: "A hidden named Point drives a JessieCode value embedded in Text.",
        objects: [
            point("sensorPoint", 3.25, -1.5, {
                ...hiddenPointAttributes,
                name: "Sensor"
            }),
            text(
                "sensorReadout",
                -0.5,
                0,
                "Sensor x = <value>X(Sensor)</value>",
                {
                    fontSize: 22,
                    strokeColor: "#1F5A94",
                    anchorX: "middle",
                    anchorY: "middle",
                    parse: true,
                    digits: 2
                }
            )
        ],
        expectedElementIds: ["sensorPoint", "sensorReadout"],
        expectedTexts: ["Sensor x = 3.25"],
        features: [
            "text",
            "dynamic-text",
            "hidden-point",
            "element-parents"
        ]
    }),
    productionCase({
        id: "prod_text_opacity_caption",
        title: "Layered captions",
        scenario: "Two overlapping labels validate stroke opacity and font-size translation.",
        objects: [
            text("primaryCaption", 0, 1, "Primary layer", {
                fontSize: 30,
                strokeColor: "#1F5A94",
                strokeOpacity: 1,
                anchorX: "middle",
                anchorY: "middle"
            }),
            text("secondaryCaption", 0, -1.2, "Secondary layer", {
                fontSize: 24,
                strokeColor: "#B44335",
                strokeOpacity: 0.45,
                anchorX: "middle",
                anchorY: "middle"
            })
        ],
        expectedElementIds: ["primaryCaption", "secondaryCaption"],
        expectedTexts: ["Primary layer", "Secondary layer"],
        features: [
            "text",
            "text-opacity",
            "style-colors",
            "text-anchors"
        ]
    }),
    productionCase({
        id: "prod_arc_minor_route",
        title: "Minor route arc",
        scenario: "A minor counterclockwise Arc connects two radial directions.",
        objects: [
            point("routeCenter", -1.2, 0.2, hiddenPointAttributes),
            point("routeRadius", 1.2, 0.2, hiddenPointAttributes),
            point("routeAngle", -0.2, 2.5, hiddenPointAttributes),
            circular(
                "routeArc",
                "arc",
                ["routeCenter", "routeRadius", "routeAngle"],
                {
                    strokeColor: "#B44335",
                    fillColor: "none",
                    strokeWidth: 3
                }
            )
        ],
        expectedElementIds: [
            "routeCenter",
            "routeRadius",
            "routeAngle",
            "routeArc"
        ],
        features: [
            "arc",
            "cubic-bezier",
            "element-parents",
            "hidden-point",
            "stroke-width"
        ]
    }),
    productionCase({
        id: "prod_arc_major_clockwise",
        title: "Major clockwise arc",
        scenario: "A major clockwise Arc validates selection and orientation together.",
        objects: [
            point("majorCenter", 0, 0, hiddenPointAttributes),
            point("majorRadius", 2.4, 0, hiddenPointAttributes),
            point("majorAngle", 0.8, 2.2, hiddenPointAttributes),
            circular(
                "majorArc",
                "arc",
                ["majorCenter", "majorRadius", "majorAngle"],
                {
                    selection: "major",
                    orientation: "clockwise",
                    strokeColor: "#1F5A94",
                    fillColor: "none",
                    strokeWidth: 3
                }
            )
        ],
        expectedElementIds: [
            "majorCenter",
            "majorRadius",
            "majorAngle",
            "majorArc"
        ],
        features: [
            "arc",
            "major-arc",
            "clockwise-orientation",
            "cubic-bezier",
            "hidden-point"
        ]
    }),
    productionCase({
        id: "prod_arc_direction_route",
        title: "Direction-selected route arc",
        scenario: "A movable fourth Point selects which circular path connects two fixed Arc endpoints.",
        objects: [
            point("directionCenter", 0, 0, hiddenPointAttributes),
            point("directionStart", 3, 0, {
                size: 6,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("directionEnd", 0, 3, {
                size: 6,
                strokeColor: "#B44335",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("directionSelector", 0, -2.2, {
                size: 6,
                strokeColor: "#9A4E1F",
                fillColor: "#F4D44D",
                strokeWidth: 2,
                fixed: false
            }),
            circular(
                "directionRoute",
                "arc",
                [
                    "directionCenter",
                    "directionStart",
                    "directionEnd",
                    "directionSelector"
                ],
                {
                    useDirection: true,
                    selection: "auto",
                    strokeColor: "#167C73",
                    fillColor: "none",
                    strokeWidth: 3
                }
            )
        ],
        expectedElementIds: [
            "directionCenter",
            "directionStart",
            "directionEnd",
            "directionSelector",
            "directionRoute"
        ],
        features: [
            "arc",
            "direction-point-arc",
            "cubic-bezier",
            "element-parents",
            "fixed-point",
            "free-point",
            "interaction-state",
            "dependent-update"
        ],
        interaction: {
            pointId: "directionSelector",
            target: [0, 3.8]
        }
    }),
    productionCase({
        id: "prod_sector_capacity",
        title: "Capacity sector",
        scenario: "A filled sector highlights a bounded radial capacity range.",
        objects: [
            point("capacityCenter", -0.5, -0.5, hiddenPointAttributes),
            point("capacityRadius", 3.2, -0.5, hiddenPointAttributes),
            point("capacityAngle", 1.2, 3.0, hiddenPointAttributes),
            circular(
                "capacitySector",
                "sector",
                ["capacityCenter", "capacityRadius", "capacityAngle"],
                {
                    strokeColor: "#167C73",
                    fillColor: "#84C7B8",
                    fillOpacity: 0.38,
                    strokeWidth: 2.5
                }
            )
        ],
        expectedElementIds: [
            "capacityCenter",
            "capacityRadius",
            "capacityAngle",
            "capacitySector"
        ],
        features: [
            "sector",
            "cubic-bezier",
            "fill-opacity",
            "element-parents"
        ]
    }),
    productionCase({
        id: "prod_angle_fixed_threshold",
        title: "Fixed threshold angle",
        scenario: "A fixed-radius sector Angle marks a decision threshold.",
        objects: [
            point("thresholdFirst", -3.5, -1.5, hiddenPointAttributes),
            point("thresholdVertex", -1.0, -1.5, hiddenPointAttributes),
            point("thresholdThird", -0.1, 1.7, hiddenPointAttributes),
            circular(
                "thresholdAngle",
                "angle",
                ["thresholdFirst", "thresholdVertex", "thresholdThird"],
                {
                    radius: 1.6,
                    type: "sector",
                    orthoType: "sector",
                    strokeColor: "#9A4E1F",
                    fillColor: "#F4D44D",
                    fillOpacity: 0.32,
                    strokeWidth: 2
                }
            )
        ],
        expectedElementIds: [
            "thresholdFirst",
            "thresholdVertex",
            "thresholdThird",
            "thresholdAngle"
        ],
        features: [
            "angle",
            "fixed-angle-radius",
            "cubic-bezier",
            "fill-opacity"
        ]
    }),
    productionCase({
        id: "prod_angle_auto_wedge",
        title: "Automatic radius angle",
        scenario: "An acute Angle uses the upstream automatic radius rule.",
        objects: [
            point("autoFirst", 0.4, -2.8, hiddenPointAttributes),
            point("autoVertex", 2.7, -2.4, hiddenPointAttributes),
            point("autoThird", 4.2, 1.4, hiddenPointAttributes),
            circular(
                "autoAngle",
                "angle",
                ["autoFirst", "autoVertex", "autoThird"],
                {
                    type: "sector",
                    orthoType: "sector",
                    strokeColor: "#1F5A94",
                    fillColor: "#7BB7E8",
                    fillOpacity: 0.3,
                    strokeWidth: 1.5
                }
            )
        ],
        expectedElementIds: [
            "autoFirst",
            "autoVertex",
            "autoThird",
            "autoAngle"
        ],
        features: [
            "angle",
            "auto-angle-radius",
            "cubic-bezier",
            "fill-opacity"
        ]
    }),
    productionCase({
        id: "prod_interaction_line_dependency",
        title: "Movable route endpoint",
        scenario: "A free Point moves a finite route while a fixed endpoint remains stable.",
        objects: [
            point("routeStart", -4, -2, {
                size: 6,
                strokeColor: "#1F5A94",
                fillColor: "#7BB7E8",
                strokeWidth: 2,
                fixed: false
            }),
            point("routeEnd", 4, 2.5, {
                size: 5,
                strokeColor: "#314652",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            segment("routeSegment", "routeStart", "routeEnd", {
                strokeColor: "#B44335",
                strokeWidth: 3
            })
        ],
        expectedElementIds: ["routeStart", "routeEnd", "routeSegment"],
        features: [
            "free-point",
            "fixed-point",
            "interaction-state",
            "dependent-update",
            "segment",
            "element-parents"
        ],
        interaction: {
            pointId: "routeStart",
            target: [-1.25, 2.75]
        }
    }),
    productionCase({
        id: "prod_interaction_circle_dependency",
        title: "Movable coverage center",
        scenario: "A free center Point moves a dependent filled Circle without changing its radius.",
        objects: [
            point("coverageCenter", -2.5, 0.5, {
                size: 6,
                strokeColor: "#167C73",
                fillColor: "#84C7B8",
                strokeWidth: 2,
                fixed: false
            }),
            circle("coverageArea", "coverageCenter", 2.1, {
                strokeColor: "#167C73",
                fillColor: "#167C73",
                fillOpacity: 0.16,
                strokeWidth: 2.5
            })
        ],
        expectedElementIds: ["coverageCenter", "coverageArea"],
        features: [
            "free-point",
            "interaction-state",
            "dependent-update",
            "circle",
            "circle-fill",
            "element-parents"
        ],
        interaction: {
            pointId: "coverageCenter",
            target: [2.25, -1.75]
        }
    }),
    productionCase({
        id: "prod_interaction_dynamic_circle_radius",
        title: "Nonnegative dynamic radius",
        scenario: "A free driver Point controls a string radius that clamps at zero before and after interaction.",
        objects: [
            point("radiusDriver", 3.0, 0.0, {
                name: "RadiusDriver",
                size: 6,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2,
                fixed: false
            }),
            circle(
                "dynamicRadius",
                [0, 0],
                "RadiusDriver.X() - 1",
                {
                    nonnegativeOnly: true,
                    strokeColor: "#167C73",
                    fillColor: "#167C73",
                    fillOpacity: 0.14,
                    strokeWidth: 3
                }
            )
        ],
        expectedElementIds: ["radiusDriver", "dynamicRadius"],
        features: [
            "circle",
            "circle-fill",
            "coordinate-parents",
            "dependent-update",
            "free-point",
            "interaction-state",
            "nonnegative-circle-radius"
        ],
        interaction: {
            pointId: "radiusDriver",
            target: [-2, 1.5]
        }
    }),
    productionCase({
        id: "prod_mixed_operations_board",
        title: "Mixed operations board",
        scenario: "A compact board combines geometry, a sampled trend, a polygon zone, and a status label.",
        objects: [
            point("operationA", -4.8, -2.7, {
                size: 5,
                strokeColor: "#1F5A94",
                fillColor: "#FFFFFF",
                strokeWidth: 2
            }),
            point("operationB", 4.6, 2.9, {
                size: 5,
                strokeColor: "#B44335",
                fillColor: "#F4D44D",
                strokeWidth: 2
            }),
            segment("operationLink", "operationA", "operationB", {
                strokeColor: "#314652",
                strokeWidth: 2
            }),
            circle("operationRange", [-2.0, 1.2], 1.4, {
                strokeColor: "#167C73",
                fillColor: "#167C73",
                fillOpacity: 0.12,
                strokeWidth: 2
            }),
            polygon(
                "operationZone",
                [[0.5, -3.5], [1.2, 1.0], [3.3, 2.1], [5.0, -2.7]],
                {
                    strokeColor: "#9A4E1F",
                    fillColor: "#F4D44D",
                    fillOpacity: 0.2,
                    strokeWidth: 1.5
                }
            ),
            curve(
                "operationTrend",
                "curve",
                [
                    [-5, -3.5, -2, -0.5, 1, 2.5, 4.5],
                    [3.6, 2.2, 2.8, 1.1, 0.2, 0.7, -0.8]
                ],
                {
                    strokeColor: "#1F5A94",
                    strokeWidth: 2
                }
            ),
            text("operationLabel", -0.3, 4.0, "Operations overview", {
                fontSize: 20,
                strokeColor: "#314652",
                anchorX: "middle",
                anchorY: "middle"
            })
        ],
        expectedElementIds: [
            "operationA",
            "operationB",
            "operationLink",
            "operationRange",
            "operationZone",
            "operationTrend",
            "operationLabel"
        ],
        expectedTexts: ["Operations overview"],
        features: [
            "mixed-scene",
            "segment",
            "point",
            "circle",
            "polygon",
            "data-plot",
            "text",
            "style-colors",
            "fill-opacity"
        ]
    })
];
