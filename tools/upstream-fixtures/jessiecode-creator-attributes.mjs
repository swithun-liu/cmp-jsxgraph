/*
 * Official JSXGraph 1.13.3 behavior fixture for creator attribute lists.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-creator-attributes.mjs
 */
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

JXG.elements.capture = true;

let boardIndex = 0;

const createJessieCode = () => {
    const calls = [];
    const order = [];
    const jessieCode = new JessieCode();
    jessieCode.board = {
        id: `fixture-board-${++boardIndex}`,
        elementsByName: {},
        objects: {},
        options: {jc: {compile: false}},
        select: (name) => name,
        create: (type, parents, attributes) => {
            const result = {
                id: `result-${calls.length + 1}`,
                type,
                parents,
                attributes
            };
            calls.push(result);
            return result;
        }
    };
    jessieCode.scope.locals.record = (value) => {
        order.push(value);
        return value;
    };
    return {jessieCode, calls, order};
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
    'capture(1, 2) << strokeColor: "red", size: 3 >>;',
    'base = << color: "red", nested: << a: 1, b: 2 >> >>; capture(1) base, << color: "blue", nested: << b: 3, c: 4 >> >>;',
    'capture(record(2)) << first: record(1) >>;',
    'capture(1) missing;',
    'sin(1) << color: "red" >>;'
];

console.log(JSON.stringify(cases.map((source) => {
    const {jessieCode, calls, order} = createJessieCode();
    try {
        return {
            source,
            ast: summarizeNode(jessieCode.getAST(source, false, true)),
            value: jessieCode.parse(source, false, true),
            calls,
            order
        };
    } catch (error) {
        return {
            source,
            error: error.message,
            calls,
            order
        };
    }
}), null, 2));
