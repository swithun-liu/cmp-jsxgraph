/*
 * Official JSXGraph 1.13.3 Dump utility fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/dump.mjs
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

const strCases = [
    "plain",
    "function (x) { return x; }",
    2,
    null
].map((value) => {
    const result = JXG.Dump.str(value);
    return {
        type: typeof result,
        value: result
    };
});

const nested = {
    text: "a'b\"c\\d\nx",
    integer: 1,
    decimal: 1.25,
    boolean: true,
    nil: null,
    array: [1, "x", null, false],
    nested: {z: 2}
};
const ordered = {};
ordered.beta = 1;
ordered["10"] = 2;
ordered["2"] = 3;
ordered.alpha = 4;

function element({
    id,
    name,
    type,
    parents,
    attributes,
    draggable = false,
    visible = true,
    defaultTicks = undefined
}) {
    return {
        id,
        name,
        elType: type,
        dump: true,
        subs: {},
        childElements: {},
        ancestors: {},
        isDraggable: draggable,
        visPropCalc: {visible},
        defaultTicks,
        getType() {
            return type;
        },
        getParents() {
            return parents.slice();
        },
        getAttributes() {
            return {...attributes};
        }
    };
}

const point = element({
    id: "A",
    name: "A",
    type: "point",
    parents: [1, 2, 3],
    attributes: {
        id: "A",
        name: "A",
        customflag: true
    },
    draggable: true
});
const axis = element({
    id: "axis",
    name: "",
    type: "axis",
    parents: ["A", "B"],
    attributes: {
        id: "axis",
        name: "",
        customflag: "axis"
    },
    defaultTicks: null
});
const glider = element({
    id: "G",
    name: "",
    type: "glider",
    parents: [4, 5, "A"],
    attributes: {
        id: "G",
        name: "",
        customflag: "glider"
    }
});
glider.onPolygon = true;
point.childElements.axis = axis;
axis.ancestors.A = point;

const board = {
    objects: {
        A: point,
        axis,
        G: glider
    },
    objectsList: [point, axis, glider],
    keepaspectratio: false,
    userLog: [
        {
            type: "drag",
            id: "A",
            start: 10,
            end: 20
        }
    ],
    getBoundingBox() {
        return [-5, 5, 5, -5];
    }
};
const boardExports = {
    jessie: JXG.Dump.toJessie(board),
    jessieNoAttributes: JXG.Dump.toJessie(board, true),
    json: JXG.Dump.toJSON(board),
    javaScript: JXG.Dump.toJavaScript(board)
};
const functionParent = function (x) {
    return x * x;
};
const functionElement = element({
    id: "f",
    name: "",
    type: "functiongraph",
    parents: [functionParent, -2, 2],
    attributes: {
        id: "f",
        name: ""
    }
});
const functionBoard = {
    objects: {f: functionElement},
    objectsList: [functionElement],
    keepaspectratio: false,
    userLog: [],
    getBoundingBox() {
        return [-5, 5, 5, -5];
    }
};
const functionParentExports = {
    jessie: JXG.Dump.toJessie(functionBoard),
    json: JXG.Dump.toJSON(functionBoard),
    javaScript: JXG.Dump.toJavaScript(functionBoard)
};
const markersCleaned = board.objectsList.every(
    (item) => !Object.prototype.hasOwnProperty.call(item, "dumped")
);

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            str: strCases,
            jcan: {
                nested: JXG.Dump.toJCAN(nested),
                emptyArray: JXG.Dump.toJCAN([]),
                emptyObject: JXG.Dump.toJCAN({}),
                nan: JXG.Dump.toJCAN(Number.NaN),
                positiveInfinity:
                    JXG.Dump.toJCAN(Number.POSITIVE_INFINITY),
                negativeInfinity:
                    JXG.Dump.toJCAN(Number.NEGATIVE_INFINITY),
                negativeZero: JXG.Dump.toJCAN(-0),
                undefinedValue: String(JXG.Dump.toJCAN(undefined)),
                functionValue:
                    String(JXG.Dump.toJCAN(function identity(x) {
                        return x;
                    })),
                propertyOrder: JXG.Dump.toJCAN(ordered)
            },
            parameters: JXG.Dump.arrayToParamStr(
                [1, "x", null, [true, 2]],
                JXG.Dump.toJCAN
            ),
            boardExports,
            functionParentExports,
            markersCleaned
        },
        null,
        2
    )
);
