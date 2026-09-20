/*
 * Official JSXGraph 1.13.3 circumcenter behavior fixture.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/circumcircle.mjs
 */
import Geometry from "../../third_party/jsxgraph-src/src/math/geometry.js";

const board = {
    origin: {scrCoords: [1, 0, 0]},
    unitX: 1,
    unitY: 1
};

const point = (x, y) => ({
    coords: {
        usrCoords: [1, x, y]
    }
});

const circumcenter = (coordinates) => Geometry.circumcenter(
    ...coordinates.map(([x, y]) => point(x, y)),
    board
).usrCoords;

console.log(JSON.stringify({
    initial: circumcenter([
        [-3, -2],
        [3, -1],
        [0, 3]
    ]),
    moved: circumcenter([
        [-3, -2],
        [4, -2],
        [0, 3]
    ]),
    collinear: circumcenter([
        [0, 0],
        [1, 0],
        [2, 0]
    ])
}, null, 2));
