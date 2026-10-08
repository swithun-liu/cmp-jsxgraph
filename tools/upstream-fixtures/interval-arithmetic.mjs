/*
 * Official JSXGraph 1.13.3 interval-arithmetic behavior fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/interval-arithmetic.mjs
 */
import IntervalArithmetic from "../../third_party/jsxgraph-src/src/math/ia.js";

const IA = IntervalArithmetic;
const interval = (low, high = low) => IA.Interval(low, high);
const snapshot = (value) => ({
    lo: classify(value.lo),
    hi: classify(value.hi)
});
const attempt = (operation) => {
    try {
        return {ok: snapshot(operation())};
    } catch (error) {
        return {
            error: error instanceof Error
                ? error.message
                : String(error)
        };
    }
};

IA.disable();
const disabled = {
    add: snapshot(IA.add(interval(1, 2), interval(3, 5))),
    divAcrossZero: snapshot(
        IA.div(interval(2, 3), interval(-1, 1))
    ),
    inversePositive: snapshot(
        IA.multiplicativeInverse(interval(2, 4))
    ),
    oddRootNegative: snapshot(IA.nthRoot(interval(-8, -1), 3)),
    evenRootMixed: snapshot(IA.nthRoot(interval(-4, 9), 2)),
    logarithm: snapshot(IA.log(interval(-1, Math.E))),
    hull: snapshot(IA.hull(interval(1, 2), interval(4, 5))),
    intersection: snapshot(
        IA.intersection(interval(1, 4), interval(3, 5))
    ),
    unionOverlap: attempt(
        () => IA.union(interval(1, 4), interval(3, 5))
    ),
    unionDisjoint: attempt(
        () => IA.union(interval(1, 2), interval(3, 4))
    ),
    differenceLeft: attempt(
        () => IA.difference(interval(1, 5), interval(-1, 3))
    ),
    differenceDisconnected: attempt(
        () => IA.difference(interval(1, 5), interval(2, 3))
    ),
    coshMixed: snapshot(IA.cosh(interval(-2, 1))),
    comparisons: {
        equalEmpty: IA.equal(IA.EMPTY, IA.EMPTY),
        notEqualSeparated: IA.notEqual(interval(1, 2), interval(3, 4)),
        lessThan: IA.lt(interval(1, 2), interval(3, 4)),
        greaterThanOrEqual: IA.geq(interval(4, 5), interval(2, 4))
    }
};

IA.enable();
const enabled = {
    add: snapshot(IA.add(interval(0.1), interval(0.2))),
    nextafterUp: IA.nextafter(1, Infinity),
    nextafterDown: IA.nextafter(1, -Infinity),
    nextafterNegativeUp: IA.nextafter(-1, Infinity),
    nextafterNegativeDown: IA.nextafter(-1, -Infinity)
};
IA.disable();

console.log(JSON.stringify({
    version: "1.13.3",
    disabled,
    enabled
}, null, 2));

function classify(value) {
    if (Number.isNaN(value)) {
        return "NaN";
    }
    if (value === Number.POSITIVE_INFINITY) {
        return "Infinity";
    }
    if (value === Number.NEGATIVE_INFINITY) {
        return "-Infinity";
    }
    return value;
}
