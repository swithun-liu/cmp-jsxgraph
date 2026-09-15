/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode.replaceIDs.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-replace-ids.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const jessieCode = new JessieCode();
jessieCode.board = {
    objects: {
        P1: {name: "A"},
        P2: {name: ""}
    }
};

const replacementNode = (elementId) => {
    const node = jessieCode.createNode(
        "node_op",
        "op_execfun",
        jessieCode.createNode("node_var", "$"),
        [jessieCode.createNode("node_str", elementId)]
    );
    node.replaced = true;
    return node;
};

const summarize = (node) => ({
    type: node.type,
    value: node.value,
    childCount: node.children.length,
    replaced: node.replaced === true
});

const named = replacementNode("P1");
const unnamed = replacementNode("P2");
const missing = replacementNode("P3");
jessieCode.replaceIDs(named);
jessieCode.replaceIDs(unnamed);
jessieCode.replaceIDs(missing);

jessieCode.board.objects.P1.name = "Renamed";
const renamed = replacementNode("P1");
jessieCode.replaceIDs(renamed);

console.log(JSON.stringify({
    named: summarize(named),
    unnamed: summarize(unnamed),
    missing: summarize(missing),
    renamed: summarize(renamed)
}, null, 2));
