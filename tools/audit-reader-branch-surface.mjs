import {readFileSync} from "node:fs";
import {dirname, relative, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    ".."
);

const geonext = auditSwitchCases({
    upstream: "third_party/jsxgraph-src/src/reader/geonext.js",
    kotlin:
        "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/reader/GeonextReader.kt",
    start: "readNode: function",
    end: "\n            read: function",
    expectedCount: 30,
    upstreamUnavailable: new Set(["210180"])
});
const geogebra = auditSwitchCases({
    upstream: "third_party/jsxgraph-src/src/reader/geogebra.js",
    kotlin:
        "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/reader/GeogebraReader.kt",
    start: "writeElement: function",
    end: "\n            read: function",
    expectedCount: 34
});
const intergeo = auditComparedNames({
    upstream: "third_party/jsxgraph-src/src/reader/intergeo.js",
    kotlin:
        "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/reader/IntergeoReader.kt",
    start: "readConstraint: function",
    end: "readConstraints: function",
    pattern: /node\.nodeName\s*===\s*["']([^"']+)["']/g,
    expectedCount: 36
});
const tracenpoche = auditComparedNames({
    upstream: "third_party/jsxgraph-src/src/reader/tracenpoche.js",
    kotlin:
        "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/reader/TracenpocheReader.kt",
    start: "this.tepElements = [",
    end: "];",
    pattern: /["']([^"']+)["']/g,
    expectedCount: 29
});
const cinderella = auditExpectedNames({
    upstream: "third_party/jsxgraph-src/src/reader/cinderella.js",
    kotlin:
        "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/reader/CinderellaReader.kt",
    names: [
        "FreePoint",
        "Join",
        "Segment",
        "CircleMP",
        "CircleByFixedRadius",
        "CircleByRadius",
        "PointOnCircle",
        "PointOnLine",
        "Mid",
        "CircleBy3",
        "Parallel",
        "Orthogonal",
        "ConicBy5",
        "ConicFoci",
        "ConicFociH",
        "ConicParabolaPL",
        "Poly",
        "Arc",
        "Through",
        "Compass",
        "AngularBisector",
        "Meet",
        "IntersectionConicLine",
        "IntersectionCircleCircle"
    ]
});

console.log(
    [
        "JSXGraph 1.13.3 reader branch surface audit",
        `${geonext.mapped}/${geonext.portable} GEONExT cases mapped ` +
            "(210180 is upstream-unavailable)",
        `${intergeo.mapped}/${intergeo.portable} Intergeo constraints mapped`,
        `${geogebra.mapped}/${geogebra.portable} Geogebra commands mapped`,
        `${cinderella.mapped}/${cinderella.portable} Cinderella definitions mapped`,
        `${tracenpoche.mapped}/${tracenpoche.portable} TraceEnPoche functions mapped`
    ].join("\n")
);

function auditSwitchCases(options) {
    return auditComparedNames({
        ...options,
        pattern: /\bcase\s+["']([^"']+)["']\s*:/g
    });
}

function auditComparedNames({
    upstream,
    kotlin,
    start,
    end,
    pattern,
    expectedCount,
    upstreamUnavailable = new Set()
}) {
    const upstreamSource = stripComments(read(upstream));
    const section = sourceSection(upstreamSource, start, end, upstream);
    const names = uniqueMatches(section, pattern);
    if (names.length !== expectedCount) {
        fail(
            `${upstream} expected ${expectedCount} branches, found ` +
                names.length
        );
    }
    const portable = names.filter(
        (name) => !upstreamUnavailable.has(name)
    );
    const kotlinSource = read(kotlin);
    const missing = portable.filter(
        (name) => !containsLiteral(kotlinSource, name)
    );
    if (missing.length > 0) {
        fail(`${kotlin} is missing branches: ${missing.join(", ")}`);
    }
    return {mapped: portable.length, portable: portable.length};
}

function auditExpectedNames({upstream, kotlin, names}) {
    const upstreamSource = read(upstream);
    const kotlinSource = read(kotlin);
    const absentUpstream = names.filter(
        (name) => !upstreamSource.includes(name)
    );
    const absentKotlin = names.filter(
        (name) => !kotlinSource.includes(name)
    );
    if (absentUpstream.length > 0) {
        fail(`${upstream} is missing expected names: ${absentUpstream}`);
    }
    if (absentKotlin.length > 0) {
        fail(`${kotlin} is missing branches: ${absentKotlin.join(", ")}`);
    }
    return {mapped: names.length, portable: names.length};
}

function read(path) {
    return readFileSync(resolve(repositoryRoot, path), "utf8");
}

function sourceSection(source, start, end, path) {
    const startIndex = source.indexOf(start);
    const endIndex = source.indexOf(end, startIndex + start.length);
    if (startIndex < 0 || endIndex < 0) {
        fail(`Cannot locate ${start} .. ${end} in ${path}`);
    }
    return source.slice(startIndex, endIndex);
}

function uniqueMatches(source, pattern) {
    return [...new Set(
        [...source.matchAll(pattern)].map((match) => match[1])
    )];
}

function containsLiteral(source, value) {
    return source.includes(`"${value}"`) || source.includes(`'${value}'`);
}

function stripComments(source) {
    return source
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
}

function fail(message) {
    console.error(`Reader branch surface audit failed: ${message}`);
    process.exit(1);
}
