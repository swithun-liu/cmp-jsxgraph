import {
    readdirSync,
    readFileSync,
    statSync
} from "node:fs";
import {dirname, relative, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    ".."
);
const upstreamRoot = resolve(
    repositoryRoot,
    "third_party/jsxgraph-src/src"
);
const kotlinPath = resolve(
    repositoryRoot,
    "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/parser/NativeJessieCodeCreators.kt"
);

const upstreamCreators = new Set();
for (const path of sourceFiles(upstreamRoot)) {
    if (!path.endsWith(".js") || path.includes("/unused/")) {
        continue;
    }
    const source = stripComments(readFileSync(path, "utf8"));
    for (const match of source.matchAll(
        /registerElement\(\s*["']([^"']+)["']/g
    )) {
        upstreamCreators.add(match[1].toLowerCase());
    }
}

const kotlinSource = readFileSync(kotlinPath, "utf8");
const kotlinCreators = new Set(
    [...kotlinSource.matchAll(
        /^ {8}"([a-z0-9]+)" to JessieCodeCreator/gm
    )].map((match) => match[1])
);

// JSXGraph 1.13.3 registers msector after commenting out its implementation.
// The official runtime consequently reports "Unknown element type".
const upstreamUnavailable = new Set(["msector"]);
const expectedPortableCreators = new Set(
    [...upstreamCreators].filter(
        (creator) => !upstreamUnavailable.has(creator)
    )
);
const missing = difference(expectedPortableCreators, kotlinCreators);
const extra = difference(kotlinCreators, expectedPortableCreators);

if (upstreamCreators.size !== 119) {
    fail(
        "Expected 119 active non-unused JSXGraph 1.13.3 registrations, " +
            `found ${upstreamCreators.size}`
    );
}
if (expectedPortableCreators.size !== 118) {
    fail(
        "Expected 118 callable upstream creator registrations, found " +
            expectedPortableCreators.size
    );
}
if (missing.length > 0) {
    fail(`Missing Kotlin creators: ${missing.join(", ")}`);
}
if (extra.length > 0) {
    fail(`Unexpected Kotlin creators: ${extra.join(", ")}`);
}

console.log(
    [
        "JSXGraph 1.13.3 creator surface audit",
        `${kotlinCreators.size}/${expectedPortableCreators.size} ` +
            "callable creators mapped",
        "1 upstream-unavailable registration: msector",
        `upstream: ${relative(repositoryRoot, upstreamRoot)}`,
        `kotlin: ${relative(repositoryRoot, kotlinPath)}`
    ].join("\n")
);

function sourceFiles(path) {
    if (!statSync(path).isDirectory()) {
        return [path];
    }
    return readdirSync(path)
        .flatMap((name) => sourceFiles(resolve(path, name)));
}

function stripComments(source) {
    return source
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
}

function difference(left, right) {
    return [...left]
        .filter((value) => !right.has(value))
        .sort();
}

function fail(message) {
    console.error(`Creator surface audit failed: ${message}`);
    process.exit(1);
}
