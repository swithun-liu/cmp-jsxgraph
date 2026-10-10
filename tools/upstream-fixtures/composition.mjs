/*
 * Official JSXGraph 1.13.3 Composition behavior fixture.
 *
 * Run after cloning the pinned upstream source:
 *   node tools/upstream-fixtures/composition.mjs
 */
import Composition from "../../third_party/jsxgraph-src/src/base/composition.js";
import "../../third_party/jsxgraph-src/src/base/board.js";
import JXG from "../../third_party/jsxgraph-src/src/jxg.js";

const summarizeArgument = (value) => {
    if (Array.isArray(value)) {
        return value.map(summarizeArgument);
    }
    if (value?.id) {
        return {id: value.id};
    }
    return value;
};

const createElement = (id, name, rank) => {
    const listeners = {};
    const calls = [];
    const element = {
        id,
        name,
        rank,
        visProp: {
            visible: rank !== 2,
            strokecolor: rank === 1 ? "red" : "blue"
        },
        calls,
        on(event, callback, context) {
            listeners[event] = {callback, context};
        }
    };
    for (const method of [
        "setAttribute",
        "setParents",
        "prepareUpdate",
        "updateRenderer",
        "update",
        "fullUpdate",
        "highlight",
        "noHighlight"
    ]) {
        element[method] = function (...args) {
            calls.push({
                method,
                args: args.map(summarizeArgument)
            });
            return this;
        };
    }
    element.rename = function (nextName) {
        const oldName = this.name;
        this.name = nextName;
        const listener = listeners["attribute:name"];
        listener.callback.call(
            listener.context,
            oldName,
            nextName,
            this
        );
    };
    return element;
};

const first = createElement("first", "shared", 1);
const second = createElement("second", "shared", 2);
const third = createElement("third", "third-name", 3);
const nestedElement = createElement("nested", "nested-name", 4);
const nested = new Composition({nestedElement});
const nestedProbe = new Composition();
let nestedError;
try {
    nestedProbe.add("nestedRole", nested);
} catch (error) {
    nestedError = {
        name: error.name,
        message: error.message,
        elementKeys: Object.keys(nestedProbe.elements),
        objectCount: nestedProbe.objectsList.length,
        roleInstalled: nestedProbe.nestedRole === nested
    };
}
const composition = new Composition({
    firstRole: first,
    secondRole: second
});

const additions = {
    third: composition.add("thirdRole", third),
    duplicateRole: composition.add("firstRole", third),
    reservedRole: composition.add("update", third),
    undefinedElement: composition.add("missing", undefined)
};
const initial = {
    elementKeys: Object.keys(composition.elements),
    nameKeys: Object.keys(composition.elementsByName),
    objectIds: composition.objectsList.map((value) => value.id ?? null),
    selectedById: composition.select("first") === first,
    selectedByDuplicateName: composition.select("shared") === second,
    selectedGroup: composition.select("group") ?? null,
    selectedMissing: composition.select("missing"),
    nestedElementKey: Object.keys(nested.elements)
};

first.rename("first-renamed");
second.rename("second-renamed");
const renamed = {
    nameKeys: Object.keys(composition.elementsByName),
    first: composition.select("first-renamed") === first,
    second: composition.select("second-renamed") === second,
    oldShared: composition.select("shared")
};

composition
    .setAttribute({strokeColor: "green"})
    .setParents([first])
    .prepareUpdate()
    .updateRenderer()
    .update()
    .fullUpdate()
    .highlight(true)
    .noHighlight();

const filteredByObject = composition.select({
    visible: true,
    strokeColor: "red"
});
const filteredByFunction = composition.select(
    (element) => element.rank >= 3
);
const filtered = {
    objectIds: filteredByObject.objectsList.map((value) => value.id),
    functionIds: filteredByFunction.objectsList.map(
        (value) => value.id ?? null
    )
};

const firstRemoval = composition.remove("firstRole");
let missingRemovalError;
try {
    composition.remove("firstRole");
} catch (error) {
    missingRemovalError = {
        name: error.name,
        message: error.message
    };
}
const removals = {
    first: firstRemoval,
    firstAgainError: missingRemovalError,
    elementKeys: Object.keys(composition.elements),
    nameKeys: Object.keys(composition.elementsByName),
    objectIds: composition.objectsList.map((value) => value.id ?? null),
    roleKeys: ["firstRole", "secondRole", "thirdRole"]
        .filter((role) => Object.hasOwn(composition, role))
};

console.log(JSON.stringify({
    additions,
    nestedError,
    initial,
    renamed,
    forwardedCalls: {
        first: first.calls,
        second: second.calls,
        third: third.calls,
        nested: nestedElement.calls
    },
    filtered,
    removals,
    methodMap: composition.methodMap,
    jxgCompositionIdentity: JXG.Composition === Composition
}, null, 2));
