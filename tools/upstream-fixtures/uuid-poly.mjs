/*
 * Official JSXGraph 1.13.3 UUID and polynomial fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/uuid-poly.mjs
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

const capture = (operation) => {
    try {
        return {value: operation()};
    } catch (cause) {
        return {
            error: {
                name: cause?.name ?? null,
                message: cause?.message ?? null
            }
        };
    }
};

const originalRandom = Math.random;
let deterministicUuid;
try {
    Math.random = () => 0;
    deterministicUuid = {
        plain: JXG.Util.genUUID(),
        prefixed: JXG.Util.genUUID("canvas"),
        trailingHyphen: JXG.Util.genUUID("canvas-")
    };
    const sequence = [0, 0.25, 0.5, 0.75, 0.999999];
    let randomCalls = 0;
    Math.random = () => sequence[randomCalls++ % sequence.length];
    deterministicUuid.sequence = JXG.Util.genUUID();
    deterministicUuid.sequenceRandomCalls = randomCalls;
} finally {
    Math.random = originalRandom;
}

const ring = new JXG.Math.Poly.Ring(["x", "y", "z"]);
const first = new JXG.Math.Poly.Monomial(ring, 4, [1, 2]);
const initialMonomial = {
    coefficient: first.coefficient,
    exponents: first.exponents.slice(),
    print: first.print()
};
const nanCoefficient = new JXG.Math.Poly.Monomial(
    ring,
    Number.NaN,
    [3, 4, 5, 6]
);
const polynomial = new JXG.Math.Poly.Polynomial(ring);
polynomial.add(first);
polynomial.add(new JXG.Math.Poly.Monomial(ring, 3, [1, 2]));
const mergedCoefficient = polynomial.monomials[0].coefficient;
const subtracted = new JXG.Math.Poly.Monomial(ring, 5, [0, 1, 0]);
polynomial.sub(subtracted);
const subtractedSourceCoefficient = subtracted.coefficient;
const copy = polynomial.copy();
const copyPrint = copy.print();
first.coefficient = 99;

const otherRing = new JXG.Math.Poly.Ring(["x", "y", "z"]);
const ringMismatch = capture(() =>
    polynomial.add(new JXG.Math.Poly.Monomial(otherRing, 1, [0, 0, 0]))
);
const stringParsing = capture(() =>
    new JXG.Math.Poly.Polynomial(ring, "x").print()
);

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            uuid: deterministicUuid,
            poly: {
                ringVariables: ring.vars,
                monomial: initialMonomial,
                nanCoefficient: {
                    coefficient: nanCoefficient.coefficient,
                    exponents: nanCoefficient.exponents,
                    print: nanCoefficient.print()
                },
                mergedCoefficient,
                subtractedSourceCoefficient,
                copyPrint,
                originalPrint: polynomial.print(),
                findSignature: polynomial.findSignature([0, 1, 0]),
                ringMismatch,
                stringParsing
            }
        },
        null,
        2
    )
);
