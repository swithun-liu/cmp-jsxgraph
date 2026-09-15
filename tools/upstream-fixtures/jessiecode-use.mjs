/*
 * Official JSXGraph 1.13.3 behavior fixture for the deprecated use statement.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-use.mjs
 */
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createElement = (id) => ({
    id,
    type: 1,
    elementClass: 1,
    elType: "point",
    visProp: {},
    methodMap: {}
});

const createBoard = (id, container, target) => ({
    id,
    container,
    elementsByName: {target},
    objects: {[target.id]: target},
    options: {jc: {compile: false}},
    select: (name) => {
        if (name === "target" || name === target.id) {
            return target;
        }
        return name;
    },
    create: (type, parents, attributes) => ({
        id: `${id}-created`,
        type,
        parents,
        attributes
    }),
    removeObject: () => {},
    update: () => {}
});

const firstTarget = createElement("first-target");
const secondTarget = createElement("second-target");
const firstBoard = createBoard(
    "first",
    "firstcontainer",
    firstTarget
);
const secondBoard = createBoard(
    "second",
    "secondcontainer",
    secondTarget
);

const summarizeValue = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (value === firstBoard || value === secondBoard) {
        return {kind: "board", id: value.id};
    }
    if (value === firstTarget || value === secondTarget) {
        return {kind: "element", id: value.id};
    }
    if (value && typeof value === "object" && value.id) {
        return {kind: "object", id: value.id};
    }
    return value;
};

const summarizeNode = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (Array.isArray(value)) {
        return value.map(summarizeNode);
    }
    if (value === null || typeof value !== "object") {
        return value;
    }
    if (value.type === undefined) {
        return {kind: "raw-object"};
    }
    return {
        type: value.type,
        value: value.value,
        children: value.children.map(summarizeNode),
        isMath: value.isMath ?? null,
        line: value.line,
        column: value.col,
        endLine: value.eline,
        endColumn: value.ecol
    };
};

const cases = [
    "use secondcontainer; target;",
    "use secondcontainer; $board;",
    "a = 1; use secondcontainer; a;",
    "use secondcontainer; capture(1);",
    "use missingcontainer; 7;"
];

JXG.elements.capture = true;
JXG.boards.jessieCodeUseFirst = firstBoard;
JXG.boards.jessieCodeUseSecond = secondBoard;

try {
    console.log(JSON.stringify(cases.map((source) => {
        const jessieCode = new JessieCode();
        jessieCode.board = firstBoard;
        try {
            return {
                source,
                ast: summarizeNode(
                    jessieCode.getAST(source, false, true)
                ),
                value: summarizeValue(
                    jessieCode.parse(source, false, true)
                ),
                board: summarizeValue(jessieCode.board)
            };
        } catch (error) {
            return {
                source,
                error: error.message,
                board: summarizeValue(jessieCode.board)
            };
        }
    }), null, 2));
} finally {
    delete JXG.boards.jessieCodeUseFirst;
    delete JXG.boards.jessieCodeUseSecond;
}
