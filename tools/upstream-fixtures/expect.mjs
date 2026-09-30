/*
 * Official JSXGraph 1.13.3 Expect utility fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/expect.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(import.meta.url);
const JXG = require(
    resolve(
        repositoryRoot,
        "jsxgraph-debug-ui/src/androidMain/assets/" +
            "jsxgraphcore-1.13.3.js"
    )
);

const board = {
    origin: {scrCoords: [1, 250, 200]},
    unitX: 50,
    unitY: 40
};
const coordinates = new JXG.Coords(
    JXG.COORDS_BY_USER,
    [2, 3],
    board,
    false
);
const point = {
    elementClass: JXG.OBJECT_CLASS_POINT,
    coords: coordinates
};
const fromPoint = JXG.Expect.coords(point, false);
const copiedPoint = JXG.Expect.coords(point, true);
const shortArray = [2, 3];
const shortResult = JXG.Expect.coordsArray(shortArray, false);
const copiedArray = [4, 5];
const copiedResult = JXG.Expect.coordsArray(copiedArray, true);
const longArray = [1, 2, 3, 4];

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            coords: {
                pointReferencePreserved: fromPoint === coordinates,
                pointCopyDistinct: copiedPoint !== coordinates,
                pointCopyUser: copiedPoint.usrCoords,
                pointCopyScreen: copiedPoint.scrCoords
            },
            arrays: {
                shortValue: shortResult,
                shortMutated: shortArray,
                shortReferencePreserved: shortResult === shortArray,
                copiedValue: copiedResult,
                copiedSourceMutated: copiedArray,
                copyDistinct: copiedResult !== copiedArray,
                longCopy: JXG.Expect.coordsArray(longArray, true),
                longNoCopy: JXG.Expect.coordsArray(longArray, false),
                longNoCopyReference:
                    JXG.Expect.coordsArray(longArray, false) === longArray
            },
            each: JXG.Expect.each(
                [[1, 2], [3, 4]],
                JXG.Expect.coordsArray,
                true
            )
        },
        null,
        2
    )
);
