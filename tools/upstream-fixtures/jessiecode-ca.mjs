/*
 * Official JSXGraph 1.13.3 JessieCode computer-algebra fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/jessiecode-ca.mjs
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

const sources = [
    "D(x^2,x);",
    "D(sin(x),x);",
    "D(cos(x),x);",
    "D(sqrt(x),x);",
    "D(log(x),x);",
    "D(x*x+2*x+1,x);",
    "D(D(x^3,x),x);",
    "D(pow(x,3),x);",
    "D(tan(x),x);",
    "D(abs(x),x);",
    "D(cot(x),x);",
    "D(exp(x),x);",
    "D(log2(x),x);",
    "D(log10(x),x);",
    "D(asin(x),x);",
    "D(acos(x),x);",
    "D(atan(x),x);",
    "D(acot(x),x);",
    "D(sinh(x),x);",
    "D(cosh(x),x);",
    "D(tanh(x),x);",
    "D(asinh(x),x);",
    "D(acosh(x),x);",
    "D(atanh(x),x);",
    "f=map(x)->x^3; h=D(f,x);",
    "f=map(x)->D(x^3,x); h=D(f,x);",
    "f=map(x)->sin(x); D(f,x);",
    "D(unknown(x),x);"
];

const cases = sources.map((source) => {
    const jessieCode = new JXG.JessieCode();
    try {
        return {
            source,
            manipulated: jessieCode.manipulate(source, false, true)
        };
    } catch (cause) {
        return {
            source,
            error: {
                name: cause?.name ?? null,
                message: cause?.message ?? null
            }
        };
    }
});

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            cases
        },
        null,
        2
    )
);
