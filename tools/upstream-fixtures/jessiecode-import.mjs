/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode importModule.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-import.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const sources = [
    'import("math").eps;',
    'import("MaTh").clamp(9, 1, 4);',
    'import("math/geometry").distance([0, 0], [3, 4]);',
    'import("math/statistics").mean([1, 3, 5]);',
    'import("math/numerics").det([[1, 2], [3, 4]]);',
    'import("missing");',
    'import("math") == import("MATH");',
    'import(1);',
    'import();',
    'import("math").missing;'
];

const summarize = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (Number.isNaN(value)) {
        return {kind: "number", value: "NaN"};
    }
    return {kind: typeof value, value};
};

console.log(JSON.stringify(sources.map((source) => {
    const jessieCode = new JessieCode();
    try {
        return {
            source,
            result: summarize(jessieCode.parse(source, false, true))
        };
    } catch (error) {
        return {
            source,
            error: `${error.name}: ${error.message}`
        };
    }
}), null, 2));
