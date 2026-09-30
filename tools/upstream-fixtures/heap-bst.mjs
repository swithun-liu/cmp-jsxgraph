/*
 * Official JSXGraph 1.13.3 unused Heap and BST fixture.
 *
 * The upstream files retain historical imports that no longer resolve from
 * src/unused. This fixture evaluates their exact implementation bodies with
 * only those imports replaced by local namespace stubs.
 *
 * Run with:
 *   node tools/upstream-fixtures/heap-bst.mjs
 */
import {readFileSync} from "node:fs";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";
import vm from "node:vm";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");

const loadUnusedModule = (fileName) => {
    const sourcePath = resolve(
        repositoryRoot,
        "third_party/jsxgraph-src/src/unused",
        fileName
    );
    const source = readFileSync(sourcePath, "utf8")
        .replace(
            /^import Mat from "\.\/math\.js";$/m,
            "const Mat = globalThis.Mat;"
        )
        .replace(
            /^import Type from "\.\.\/utils\/type\.js";$/m,
            "const Type = globalThis.Type;"
        )
        .replace(/^export default [^;]+;$/m, "");
    const math = Object.create(Math);
    const context = {
        Mat: {},
        Math: math,
        Type: {
            exists: (value) => value !== undefined && value !== null
        }
    };
    vm.runInNewContext(source, context, {filename: sourcePath});
    return context;
};

const numberValue = (value) => {
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
};

const heapContext = loadUnusedModule("heap.js");
const heap = new heapContext.Mat.Heap();
const heapNodes = [
    {id: "four", v: 4},
    {id: "one", v: 1},
    {id: "seven", v: 7},
    {id: "three", v: 3},
    {id: "nine", v: 9},
    {id: "two", v: 2},
    {id: "eight", v: 8}
];
const heapInsertions = heapNodes.map((node) => {
    heap.insert(node);
    return heap.pq.slice(0, heap.N).map((entry) => entry.v);
});
const heapDeleted = [];
while (heap.N > 0) {
    const node = heap.delmax();
    heapDeleted.push({id: node.id, v: node.v});
}
const emptyHeap = new heapContext.Mat.Heap();
const emptyDeletion = emptyHeap.delmax();

const bstContext = loadUnusedModule("bst.js");
const nodeEvidence = (tree, node) => {
    if (node === undefined) {
        return {kind: "undefined"};
    }
    if (node === null) {
        return {kind: "null"};
    }
    if (tree.isNil(node)) {
        return {kind: "nil", count: numberValue(node.N)};
    }
    return {
        kind: "node",
        item: node.item,
        count: numberValue(node.N)
    };
};
const preOrder = (tree) => {
    const result = [];
    tree.traverse(tree.head, (node) => {
        result.push({
            item: node.item,
            count: numberValue(node.N)
        });
    });
    return result;
};
const depth = (tree, node = tree.head) => {
    if (tree.isNil(node)) {
        return 0;
    }
    return 1 + Math.max(depth(tree, node.l), depth(tree, node.r));
};

const deterministic = new bstContext.Mat.BST();
deterministic.init(false);
[4, 2, 6, 1, 3, 5, 7].forEach((value) => deterministic.insert(value));
const four = deterministic.search(4);
const one = deterministic.search(1);
const seven = deterministic.search(7);
const deterministicEvidence = {
    count: numberValue(deterministic.count()),
    preOrder: preOrder(deterministic),
    selected: Array.from({length: 9}, (_, index) =>
        deterministic.select(index - 1)
    ),
    search: {
        four: nodeEvidence(deterministic, four),
        missing: nodeEvidence(deterministic, deterministic.search(99))
    },
    minimum: nodeEvidence(
        deterministic,
        deterministic.minimum(deterministic.head)
    ),
    maximum: nodeEvidence(
        deterministic,
        deterministic.maximum(deterministic.head)
    ),
    neighbors: {
        previousFour: nodeEvidence(deterministic, deterministic.prev(four)),
        nextFour: nodeEvidence(deterministic, deterministic.next(four)),
        previousMinimum: nodeEvidence(deterministic, deterministic.prev(one)),
        nextMaximum: nodeEvidence(deterministic, deterministic.next(seven))
    }
};

deterministic.deleteNode(1);
deterministicEvidence.afterLeafDelete = {
    count: numberValue(deterministic.count()),
    nilCount: numberValue(deterministic.z.N),
    preOrder: preOrder(deterministic),
    selected: Array.from({length: 8}, (_, index) =>
        deterministic.select(index)
    )
};

const balanced = new bstContext.Mat.BST();
balanced.init(false);
[1, 2, 3, 4, 5, 6, 7].forEach((value) => balanced.insert(value));
const beforeBalance = {
    depth: depth(balanced),
    preOrder: preOrder(balanced)
};
balanced.balance();
const balanceEvidence = {
    before: beforeBalance,
    after: {
        depth: depth(balanced),
        count: numberValue(balanced.count()),
        preOrder: preOrder(balanced)
    }
};

const randomized = new bstContext.Mat.BST();
const randomSequence = [0.0, 0.9, 0.2, 0.8, 0.4, 0.7, 0.1, 0.6];
let randomCalls = 0;
bstContext.Math.random = () =>
    randomSequence[randomCalls++ % randomSequence.length];
randomized.init(true);
[4, 2, 6, 1, 3, 5, 7].forEach((value) => randomized.insert(value));
const randomizedEvidence = {
    randomCalls,
    count: numberValue(randomized.count()),
    preOrder: preOrder(randomized)
};
randomized.deleteNode(2);
randomizedEvidence.afterRootDelete = {
    randomCalls,
    count: numberValue(randomized.count()),
    preOrder: preOrder(randomized)
};

console.log(
    JSON.stringify(
        {
            version: "1.13.3",
            heap: {
                insertions: heapInsertions,
                deleted: heapDeleted,
                finalCount: heap.N,
                retainedStorage: heap.pq.map((node) => node?.v ?? null),
                emptyDeletion: {
                    value:
                        emptyDeletion === undefined ? "undefined" : emptyDeletion,
                    count: emptyHeap.N,
                    length: emptyHeap.pq.length,
                    hasNegativeIndex: Object.hasOwn(emptyHeap.pq, "-1")
                }
            },
            bst: {
                deterministic: deterministicEvidence,
                balance: balanceEvidence,
                randomized: randomizedEvidence
            }
        },
        null,
        2
    )
);
