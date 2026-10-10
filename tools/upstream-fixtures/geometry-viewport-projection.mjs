/*
 * Official JSXGraph 1.13.3 geometry viewport/projection fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/geometry-viewport-projection.mjs
 */
import "../../third_party/jsxgraph-src/src/math/nlp.js";
import Geometry from "../../third_party/jsxgraph-src/src/math/geometry.js";
import Coords from "../../third_party/jsxgraph-src/src/base/coords.js";
import Const from "../../third_party/jsxgraph-src/src/base/constants.js";

const board = {
    canvasWidth: 800,
    canvasHeight: 600,
    unitX: 80,
    unitY: 60,
    origin: {
        scrCoords: [1, 400, 300]
    },
    getBoundingBox() {
        return [-5, 5, 5, -5];
    }
};

const point = (coordinates) =>
    new Coords(Const.COORDS_BY_USER, coordinates, board);
const classify = (values) =>
    values.map((value) => {
        if (Number.isNaN(value)) {
            return "NaN";
        }
        if (!Number.isFinite(value)) {
            return String(value);
        }
        return value;
    });
const classifyDeep = (value) => {
    if (Array.isArray(value)) {
        return value.map(classifyDeep);
    }
    if (Number.isNaN(value)) {
        return "NaN";
    }
    if (!Number.isFinite(value)) {
        return String(value);
    }
    return value;
};
const snapshot = (coordinates) => ({
    user: classify(coordinates.usrCoords),
    screen: classify(coordinates.scrCoords)
});
const delimit = (
    first,
    second,
    standardForm,
    straightFirst,
    straightLast,
    slope
) => {
    const point1 = point(first);
    const point2 = point(second);
    Geometry.calcLineDelimitingPoints(
        {
            stdform: standardForm,
            board,
            evalVisProp(name) {
                return name === "straightfirst"
                    ? straightFirst
                    : straightLast;
            },
            getSlope() {
                return slope;
            }
        },
        point1,
        point2
    );
    return [snapshot(point1), snapshot(point2)];
};

const target = {
    range: [0, 2 * Math.PI],
    X(parameter) {
        return Math.cos(parameter);
    },
    Y(parameter) {
        return Math.sin(parameter);
    },
    Z(parameter) {
        return parameter / Math.PI;
    },
    view: {
        project3DTo2D(coordinates) {
            return [1, coordinates[1], coordinates[2]];
        }
    }
};
const parameters = [0.3];
const projection = Geometry.projectScreenCoordsToParametric(
    [0.2, 1.2],
    target,
    parameters,
    false
);
const cyclicParameters = [6.1];
const cyclicProjection = Geometry.projectScreenCoordsToParametric(
    [1, 0],
    target,
    cyclicParameters,
    true
);
const worldParameters = [0.3];
const worldProjection = Geometry.projectCoordsToParametric(
    [1, 0.2, 1.2, 0.4],
    target,
    1,
    worldParameters
);

const cubicControlPoints = [
    [0, 0],
    [1, 3],
    [2, -1],
    [4, 2]
];
const cubicCoordinate = (parameter, coordinate) => {
    const opposite = 1 - parameter;
    return opposite * opposite * opposite *
            cubicControlPoints[0][coordinate] +
        3 * opposite * opposite * parameter *
            cubicControlPoints[1][coordinate] +
        3 * opposite * parameter * parameter *
            cubicControlPoints[2][coordinate] +
        parameter * parameter * parameter *
            cubicControlPoints[3][coordinate];
};
const bezierProjection = Geometry.projectCoordsToBeziersegment(
    [1, 1, 1],
    {
        X(parameter) {
            return cubicCoordinate(parameter, 0);
        },
        Y(parameter) {
            return cubicCoordinate(parameter, 1);
        }
    },
    0
);

const continuousCurve = {
    board,
    minX() {
        return 0;
    },
    maxX() {
        return 2 * Math.PI;
    },
    X(parameter) {
        return Math.cos(parameter);
    },
    Y(parameter) {
        return Math.sin(parameter);
    },
    evalVisProp(name) {
        return name === "curvetype" ? "parameter" : undefined;
    },
    updateTransform(coordinates) {
        return coordinates;
    }
};
const continuousProjection = Geometry.projectCoordsToCurve(
    0.8,
    0.7,
    0,
    continuousCurve,
    board
);

const parabola = {
    minX() {
        return -3;
    },
    maxX() {
        return 3;
    },
    Ft(parameter) {
        return [1, parameter, parameter * parameter - 1];
    }
};
const horizontalAxis = {
    minX() {
        return -3;
    },
    maxX() {
        return 3;
    },
    Ft(parameter) {
        return [1, parameter, 0];
    }
};
const curveNewton = Geometry.meetCurveCurveNewton(
    parabola,
    horizontalAxis,
    [-1.5, -0.5],
    [-1.5, -0.5],
    false
);

const reuleauxPoint = (x, y) => {
    const pointCoordinates = [1, x, y];
    return {
        coords: {
            usrCoords: pointCoordinates
        },
        X() {
            return pointCoordinates[1];
        },
        Y() {
            return pointCoordinates[2];
        },
        Dist(other) {
            return Math.hypot(
                pointCoordinates[1] - other.X(),
                pointCoordinates[2] - other.Y()
            );
        }
    };
};
const reuleaux = Geometry.reuleauxPolygon(
    [
        reuleauxPoint(0, 2),
        reuleauxPoint(-Math.sqrt(3), -1),
        reuleauxPoint(Math.sqrt(3), -1)
    ],
    3
);
const reuleauxParameters = [
    -Math.PI,
    0,
    Math.PI / 3,
    2 * Math.PI / 3,
    Math.PI,
    2 * Math.PI
];
const reuleauxCoordinates = reuleauxParameters.map((parameter) => [
    parameter,
    reuleaux[0](parameter, false),
    reuleaux[1](parameter, true)
]);

console.log(JSON.stringify({
    version: "1.13.3",
    lineBoard: {
        diagonal: Geometry.meetLineBoard(
            [0, 1, -1],
            board
        ).map(snapshot),
        vertical: Geometry.meetLineBoard(
            [-250, 1, 0],
            board
        ).map(snapshot),
        horizontal: Geometry.meetLineBoard(
            [-120, 0, 1],
            board
        ).map(snapshot)
    },
    delimiting: {
        line: delimit(
            [1, -1, -1],
            [1, 1, 1],
            [0, 1, -1],
            true,
            true,
            1
        ),
        ray: delimit(
            [1, 0, 0],
            [1, 1, 0],
            [0, 0, 1],
            false,
            true,
            0
        ),
        segment: delimit(
            [1, -1, 0],
            [1, 1, 0],
            [0, 0, 1],
            false,
            false,
            0
        )
    },
    boardProjection: {
        outside: snapshot(
            Geometry.projectPointToBoard(point([1, 8, 7]), board)
        ),
        inside: snapshot(
            Geometry.projectPointToBoard(point([1, 2, -3]), board)
        )
    },
    screenProjection: {
        point: classify(projection),
        parameters,
        cyclicPoint: classify(cyclicProjection),
        cyclicParameters
    },
    specializedHelpers: {
        curveNewton: classifyDeep(curveNewton),
        bezierProjection: classifyDeep(bezierProjection),
        continuousProjection: {
            point: snapshot(continuousProjection[0]),
            parameter: continuousProjection[1]
        },
        worldProjection: {
            point: classify(worldProjection),
            parameters: worldParameters
        },
        reuleaux: {
            start: reuleaux[2],
            end: reuleaux[3],
            coordinates: classifyDeep(reuleauxCoordinates),
            nonFinite: [
                classifyDeep(reuleaux[0](Number.NaN, false)),
                classifyDeep(
                    reuleaux[1](Number.POSITIVE_INFINITY, false)
                )
            ]
        }
    }
}, null, 2));
