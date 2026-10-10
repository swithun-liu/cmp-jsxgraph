/*
 * Official JSXGraph 1.13.3 fixture for Text label auto-position scoring.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/text-auto-position.mjs
 */
import Const from "../../third_party/jsxgraph-src/src/base/constants.js";
import Text from "../../third_party/jsxgraph-src/src/base/text.js";

const point = (id, screenX, screenY) => ({
    id,
    elementClass: Const.OBJECT_CLASS_POINT,
    coords: {
        scrCoords: [1, screenX, screenY]
    },
    size: [0, 0],
    visProp: {},
    visPropCalc: {
        visible: true
    },
    evalVisProp(key) {
        return this.visProp[key];
    }
});

const anchor = point("anchor", 100, 100);
const blocker = point("blocker", 125, 94.6);
const board = {
    unitX: 1,
    unitY: 1,
    options: {
        precision: {
            hasPoint: 4
        }
    },
    objectsList: [],
    getBoundingBox() {
        return [-100, 100, 100, -100];
    }
};
const label = Object.create(Text.prototype);
label.id = "label";
label.board = board;
label.element = {
    getLabelAnchor() {
        return anchor.coords;
    }
};
label.size = [27, 10.8];
label.visProp = {
    anchorx: "left",
    anchory: "bottom",
    autopositionmaxdistance: 28,
    autopositionmindistance: 12,
    autopositionwhitelist: [],
    islabel: true,
    offset: [12, 0]
};
label.visPropCalc = {
    visible: true
};
label.evalVisProp = function (key) {
    return this.visProp[key];
};
label.getSize = function () {
    return this.size;
};
board.objectsList = [anchor, blocker, label];

const score = (degrees) =>
    label.calculateScore(12, degrees * Math.PI / 180);

const result = {
    score0: score(0),
    score6: score(6),
    score30: score(30)
};
label.setAutoPosition();
result.anchorX = label.visProp.anchorx;
result.anchorY = label.visProp.anchory;
result.offset = label.visProp.offset;

console.log(JSON.stringify(result, null, 2));
