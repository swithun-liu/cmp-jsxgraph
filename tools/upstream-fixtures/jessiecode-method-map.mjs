/*
 * Official JSXGraph 1.13.3 behavior fixture for the translated methodMap
 * subset.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-method-map.mjs
 */
import Const from "../../third_party/jsxgraph-src/src/base/constants.js";
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createFixture = () => {
    const calls = [];
    const board = {
        id: "method-map-fixture",
        elementsByName: {},
        objects: {},
        options: {jc: {compile: false}},
        select(reference) {
            return this.objects[reference] || this.elementsByName[reference];
        },
        update(element) {
            calls.push({
                method: "board.update",
                element: element?.id
            });
            return this;
        }
    };
    const createPoint = (id, name, x, y) => {
        const point = {
            id,
            name,
            type: Const.OBJECT_TYPE_POINT,
            elementClass: Const.OBJECT_CLASS_POINT,
            visProp: {},
            methodMap: {
                X: "X",
                Y: "Y",
                Bounds: "bounds",
                addChild: "addChild",
                setName: "setName",
                getName: "getName",
                Name: "getName",
                move: "moveTo",
                moveTo: "moveTo",
                addConstraint: "addConstraint"
            },
            coords: {
                usrCoords: [1, x, y]
            },
            X() {
                return this.coords.usrCoords[1];
            },
            Y() {
                return this.coords.usrCoords[2];
            },
            bounds() {
                return [this.X(), this.Y(), this.X(), this.Y()];
            },
            addChild(child) {
                calls.push({
                    method: "addChild",
                    owner: this.id,
                    child: child.id
                });
                return this;
            },
            setName(value) {
                const escaped = value
                    .replace(/</g, "&lt;")
                    .replace(/>/g, "&gt;");
                delete board.elementsByName[this.name];
                this.name = escaped;
                board.elementsByName[this.name] = this;
            },
            getName() {
                return this.name;
            },
            setPosition(method, coordinates) {
                calls.push({
                    method: "setPosition",
                    coordinateMethod: method,
                    coordinates
                });
                const offset = coordinates.length === 2 ? 0 : 1;
                this.coords.usrCoords = [
                    coordinates.length === 2 ? 1 : coordinates[0],
                    coordinates[offset],
                    coordinates[offset + 1]
                ];
                return this;
            },
            moveTo(where, time) {
                calls.push({
                    method: "moveTo",
                    where,
                    time: time === undefined ? "undefined" : time
                });
                this.setPosition(Const.COORDS_BY_USER, where);
                return board.update(this);
            },
            addConstraint(terms) {
                calls.push({
                    method: "addConstraint",
                    terms
                });
                return this;
            }
        };
        board.elementsByName[name] = point;
        board.objects[id] = point;
        return point;
    };
    const first = createPoint("point-a", "A", 1, 2);
    const second = createPoint("point-b", "B", 3, 4);
    const jessieCode = new JessieCode();
    jessieCode.board = board;
    return {board, calls, first, jessieCode, second};
};

const summarize = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (value?.elementsByName) {
        return {kind: "board", id: value.id};
    }
    if (value?.id) {
        return {kind: "element", id: value.id};
    }
    return value;
};

const cases = [
    "A.Bounds();",
    "A.addChild(B);",
    'A.setName("<B>");',
    "A.move([4, 5]);",
    "A.moveTo([6, 7], 0);",
    'A.addConstraint(["B.X() + 1", 2]);'
];

console.log(JSON.stringify(cases.map((source) => {
    const {calls, first, jessieCode} = createFixture();
    try {
        return {
            source,
            value: summarize(jessieCode.parse(source, false, true)),
            state: {
                name: first.name,
                coordinates: first.coords.usrCoords
            },
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
