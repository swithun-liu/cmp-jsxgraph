/*
 * Official JSXGraph 1.13.3 behavior fixture for translated JessieCode
 * built-ins.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-builtins.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";

const point = (id, name, x, y) => ({
    id,
    name,
    type: 1,
    elementClass: 1,
    visProp: {},
    methodMap: {},
    X: () => x,
    Y: () => y,
    Dist(other) {
        return Math.hypot(x - other.X(), y - other.Y());
    },
    getName() {
        return this.name;
    }
});

const createBoard = (id, container) => {
    const removed = [];
    const A = point(`${id}-a`, "A", 1, 2);
    const B = point(`${id}-b`, "B", 4, 6);
    const U = point(`${id}-u`, "", 0, 0);
    const l = {
        id: `${id}-line`,
        name: "l",
        type: 2,
        elementClass: 2,
        visProp: {},
        methodMap: {},
        L: () => 5,
        Slope: () => 0.5,
        getName() {
            return this.name;
        }
    };
    const c = {
        id: `${id}-circle`,
        name: "c",
        type: 3,
        elementClass: 3,
        visProp: {},
        methodMap: {},
        Area: () => 12.5,
        Perimeter: () => 14.5,
        Radius: () => 2.5,
        getName() {
            return this.name;
        }
    };
    const s = {
        id: `${id}-slider`,
        name: "s",
        type: 13,
        elementClass: 1,
        visProp: {},
        methodMap: {},
        Value: () => 7,
        getName() {
            return this.name;
        }
    };
    const elements = {A, B, U, l, c, s};
    const objects = Object.fromEntries(
        Object.values(elements).map((element) => [element.id, element])
    );
    return {
        id,
        container,
        removed,
        elementsByName: elements,
        objects,
        options: {jc: {compile: false}},
        select(reference) {
            return this.objects[reference] || this.elementsByName[reference];
        },
        removeObject(element) {
            removed.push(element.id);
            delete this.objects[element.id];
            delete this.elementsByName[element.name];
            return this;
        }
    };
};

const sources = [
    "binomial(5, 2);",
    "gcd(84, 30);",
    "lcm(21, 6);",
    "lcm(0, 6);",
    "ratpow(-8, 1, 3);",
    "ratpow(-8, 0, 3);",
    "ratpow(-8, 1, 0);",
    "deg([1, 0], [0, 0], [0, 1]);",
    "rad([1, 0], [0, 0], [0, 1]);",
    "X(A);",
    "Y(A);",
    "V(s);",
    "Value(s);",
    "L(l);",
    "Length(l);",
    "Area(c);",
    "perimeter(c);",
    "Dist(A, B);",
    "Radius(c);",
    "Slope(l);",
    "getName(A);",
    "name(U, true);",
    "IfThen(0, 1, 2);",
    "eval(7);",
    "eval(function () { return 9; });",
    "eval([function () { return 3; }, [function () { return 4; }]]);",
    "randint(2, 10, 2);",
    "randint(2, 10, null);",
    "randint(2, 10, missing);",
    "remove(A);",
    "remove(7);"
];

const summarize = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (value?.container) {
        return {kind: "board", id: value.id};
    }
    return value;
};

const originalRandom = Math.random;
Math.random = () => 0.5;
try {
    console.log(JSON.stringify(sources.map((source) => {
        const board = createBoard("case-primary", "case-primary");
        JXG.boards[board.id] = board;
        const jessieCode = new JessieCode();
        jessieCode.board = board;
        try {
            return {
                source,
                value: summarize(
                    jessieCode.parse(source, false, true)
                ),
                removed: board.removed
            };
        } catch (error) {
            return {
                source,
                error: error.message,
                removed: board.removed
            };
        } finally {
            delete JXG.boards[board.id];
        }
    }), null, 2));
} finally {
    Math.random = originalRandom;
}
