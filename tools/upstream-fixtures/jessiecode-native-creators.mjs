/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode element creators.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-native-creators.mjs
 */
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

JXG.elements.point = true;
JXG.elements.line = true;
JXG.elements.circle = true;

let boardIndex = 0;

const createBoard = () => {
    const calls = [];
    const id = `native-creator-fixture-${++boardIndex}`;
    return {
        calls,
        board: {
            id,
            elementsByName: {},
            objects: {},
            options: {jc: {compile: false}},
            select: (name) => name,
            create: (type, parents, attributes) => {
                const result = {
                    id: `${id}-${calls.length + 1}`,
                    type,
                    parents,
                    attributes
                };
                calls.push(result);
                return result;
            }
        }
    };
};

const summarizeValue = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (value && typeof value === "object" && value.id) {
        return {
            kind: "element",
            id: value.id,
            type: value.type,
            attributes: value.attributes
        };
    }
    return value;
};

const cases = [
    "A = point(1, 2); A;",
    'A = point(1, 2) << name: "Explicit" >>; A;',
    'A = point(1, 2) << id: "fixed" >>; A;',
    "A = point(1, 2); B = point(3, 4); l = line(A, B); l;",
    "A = point(1, 2); c = circle(A, 5); c;",
    "line([0, 0], [2, 3]);",
    "line(1, -2, 3);",
    "circle([1, 2], 4);"
];

console.log(JSON.stringify(cases.map((source) => {
    const {board, calls} = createBoard();
    const jessieCode = new JessieCode();
    jessieCode.board = board;
    try {
        return {
            source,
            value: summarizeValue(
                jessieCode.parse(source, false, true)
            ),
            calls
        };
    } catch (error) {
        return {
            source,
            error: error.message,
            calls
        };
    }
}), null, 2));
