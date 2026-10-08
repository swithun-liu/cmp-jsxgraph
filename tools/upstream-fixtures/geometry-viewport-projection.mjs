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
    }
}, null, 2));
