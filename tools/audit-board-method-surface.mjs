import {readFileSync} from "node:fs";
import {dirname, relative, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    ".."
);
const upstreamPath = resolve(
    repositoryRoot,
    "third_party/jsxgraph-src/src/base/board.js"
);
const kotlinPath = resolve(
    repositoryRoot,
    "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/base/Board.kt"
);
const upstreamSource = readFileSync(upstreamPath, "utf8");
const kotlinSource = readFileSync(kotlinPath, "utf8");

const upstreamMethods = matches(
    upstreamSource,
    /^ {8}([A-Za-z_$][A-Za-z0-9_$]*):\s*(?:function|JXG\.shortcut)/gm
);
const kotlinMethods = new Set(
    matches(
        kotlinSource,
        /^ {4}(?:internal |public |private )?(?:suspend )?fun\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(/gm
    )
);

// Pure-KMP ownership collapses these browser/runtime implementation details
// into the corresponding portable Board operation.
const adaptedMethods = new Map([
    ["create", "createElement"],
    ["_removeObj", "removeObject"],
    ["updateRendererCanvas", "updateRenderer"],
    ["__evt", "__evt__"]
]);
const missing = upstreamMethods.filter((method) => {
    const kotlinMethod = adaptedMethods.get(method) ?? method;
    return !kotlinMethods.has(kotlinMethod);
});

if (upstreamMethods.length !== 198) {
    fail(
        "Expected 198 JSXGraph 1.13.3 Board prototype methods, found " +
            upstreamMethods.length
    );
}
if (new Set(upstreamMethods).size !== upstreamMethods.length) {
    fail("Upstream Board prototype method extraction produced duplicates");
}
if (missing.length > 0) {
    fail(`Missing Kotlin Board methods: ${missing.join(", ")}`);
}

console.log(
    [
        "JSXGraph 1.13.3 Board method surface audit",
        `${upstreamMethods.length}/${upstreamMethods.length} methods mapped`,
        `${adaptedMethods.size} explicit pure-KMP adaptations`,
        `upstream: ${relative(repositoryRoot, upstreamPath)}`,
        `kotlin: ${relative(repositoryRoot, kotlinPath)}`
    ].join("\n")
);

function matches(source, pattern) {
    return [...source.matchAll(pattern)].map((match) => match[1]);
}

function fail(message) {
    console.error(`Board method surface audit failed: ${message}`);
    process.exit(1);
}
