/*
 * Official JSXGraph 1.13.3 behavior fixture for persistent parser sessions.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-session.mjs
 */
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createBoard = (id, container) => ({
    id,
    container,
    elementsByName: {},
    objects: {},
    options: {jc: {compile: false}},
    select: (name) => name,
    create: () => {},
    removeObject: () => {},
    update: () => {}
});

const firstBoard = createBoard("first", "firstcontainer");
const secondBoard = createBoard("second", "secondcontainer");
const jessieCode = new JessieCode();
jessieCode.board = firstBoard;

const summarizeValue = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (value === firstBoard || value === secondBoard) {
        return {kind: "board", id: value.id};
    }
    if (typeof value === "function") {
        return {
            kind: "function",
            parameters: value.scope.args
        };
    }
    return value;
};

const sources = [
    "a = 1;",
    "a = a + 2;",
    "f = function (x) { return a + x; };",
    "f(4);",
    "use secondcontainer;",
    "$board;",
    "a;"
];

JXG.boards.jessieCodeSessionFirst = firstBoard;
JXG.boards.jessieCodeSessionSecond = secondBoard;

try {
    const steps = sources.map((source) => ({
        source,
        value: summarizeValue(
            jessieCode.parse(source, false, false)
        ),
        board: summarizeValue(jessieCode.board),
        a: summarizeValue(jessieCode.resolve("a"))
    }));

    console.log(JSON.stringify({
        steps,
        code: jessieCode.code
    }, null, 2));
} finally {
    delete JXG.boards.jessieCodeSessionFirst;
    delete JXG.boards.jessieCodeSessionSecond;
}
