/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode setProp.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-set-prop.mjs
 */
import Const from "../../third_party/jsxgraph-src/src/base/constants.js";
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const functionWithOrigin = (origin, value) => {
    const result = () => value;
    result.origin = origin;
    return result;
};

const createFixture = ({draggable = true} = {}) => {
    const calls = [];
    const point = {
        id: "point-id",
        name: "A",
        type: Const.OBJECT_TYPE_POINT,
        elementClass: Const.OBJECT_CLASS_POINT,
        isDraggable: draggable,
        needsRegularUpdate: true,
        coords: {
            usrCoords: [1, 1, 2]
        },
        visProp: {},
        methodMap: {
            X: "X",
            Y: "Y",
            Name: "getName"
        },
        X() {
            return this.coords.usrCoords[1];
        },
        Y() {
            return this.coords.usrCoords[2];
        },
        XEval: functionWithOrigin(draggable ? 1 : "driver.X()", 1),
        YEval: functionWithOrigin(draggable ? 2 : "driver.Y()", 2),
        getName() {
            return this.name;
        },
        setPosition(method, coordinates) {
            calls.push({
                method: "setPosition",
                coordinateMethod: method,
                coordinates
            });
            this.coords.usrCoords = [1, coordinates[0], coordinates[1]];
        },
        addConstraint(terms) {
            calls.push({
                method: "addConstraint",
                terms
            });
            this.isDraggable = false;
            this.XEval = functionWithOrigin(terms[0], this.X());
            this.YEval = functionWithOrigin(terms[1], this.Y());
        },
        setAttribute(attributes) {
            calls.push({
                method: "setAttribute",
                attributes
            });
            for (const [rawKey, value] of Object.entries(attributes)) {
                const key = rawKey.replace(/\s+/g, "").toLowerCase();
                this.visProp[key] = value;
                if (key === "name") {
                    this.name = value;
                } else if (key === "needsregularupdate") {
                    this.needsRegularUpdate = !(
                        value === "false" || value === false
                    );
                }
            }
        }
    };
    const board = {
        id: "set-prop-fixture",
        elementsByName: {A: point},
        objects: {[point.id]: point},
        options: {jc: {compile: false}},
        select(reference) {
            return this.objects[reference] || this.elementsByName[reference];
        },
        update() {
            calls.push({method: "board.update"});
        }
    };
    const jessieCode = new JessieCode();
    jessieCode.board = board;
    return {calls, jessieCode, point};
};

const cases = [
    {source: "A.X = 4;"},
    {source: "A.Y = -3;"},
    {source: 'A.X = "driver.X() + 1";'},
    {source: "A.X = 4;", draggable: false},
    {source: 'A.name = "Renamed";'},
    {source: 'A.Name = "Mapped";'},
    {source: "A.needsRegularUpdate = false;"},
    {source: 'A.needsRegularUpdate = "false";'},
    {source: "A.x = 9;"}
];

console.log(JSON.stringify(cases.map(({source, draggable}) => {
    const {calls, jessieCode, point} = createFixture({draggable});
    try {
        const value = jessieCode.parse(source, false, true);
        return {
            source,
            initialDraggable: draggable ?? true,
            value,
            state: {
                name: point.name,
                x: point.X(),
                y: point.Y(),
                draggable: point.isDraggable,
                xOrigin: point.XEval.origin,
                yOrigin: point.YEval.origin,
                needsRegularUpdate: point.needsRegularUpdate,
                visProp: point.visProp
            },
            calls
        };
    } catch (error) {
        return {
            source,
            initialDraggable: draggable ?? true,
            error: error.message,
            calls
        };
    }
}), null, 2));
