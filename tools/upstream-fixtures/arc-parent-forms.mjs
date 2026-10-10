/*
 * Official JSXGraph 1.13.3 Arc parent-form fixture.
 *
 * Run after cloning the pinned upstream source:
 *   node tools/upstream-fixtures/arc-parent-forms.mjs
 */
import Const from "../../third_party/jsxgraph-src/src/base/constants.js";
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";
import "../../third_party/jsxgraph-src/src/element/arc.js";

const sourceArc = {
    id: "sourceArc",
    type: Const.OBJECT_TYPE_ARC,
    elementClass: Const.OBJECT_CLASS_CURVE
};
const transformation = {
    id: "scale",
    type: Const.OBJECT_TYPE_TRANSFORMATION,
    elementClass: Const.OBJECT_CLASS_OTHER
};
const board = {
    options: {
        arc: {
            center: {},
            radiuspoint: {},
            anglepoint: {}
        }
    },
    select(value) {
        return value;
    }
};

let result;
try {
    result = JXG.createArc(
        board,
        [sourceArc, transformation],
        {}
    );
} catch (error) {
    result = {
        errorName: error.name,
        errorMessage: error.message
    };
}

console.log(JSON.stringify({
    sourceType: sourceArc.type,
    transformationType: transformation.type,
    result
}, null, 2));
