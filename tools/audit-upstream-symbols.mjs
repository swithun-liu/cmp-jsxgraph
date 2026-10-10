import {
    existsSync,
    mkdirSync,
    readFileSync,
    readdirSync,
    writeFileSync
} from "node:fs";
import {
    dirname,
    relative,
    resolve
} from "node:path";
import {fileURLToPath} from "node:url";

const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    ".."
);
const upstreamRoot = resolve(
    repositoryRoot,
    "third_party/jsxgraph-src/src"
);
const kotlinRoots = [
    resolve(repositoryRoot, "jsxgraph-core/src/commonMain/kotlin"),
    resolve(repositoryRoot, "jsxgraph-compose/src/commonMain/kotlin")
];
const outputPath = resolve(
    repositoryRoot,
    process.argv[2] ?? "build/reports/upstream-symbol-audit.json"
);
const excludedSources = new Map([
    [
        "src/renderer/vml.js",
        "Legacy Internet Explorer VML backend is outside the Compose target."
    ]
]);
const excludedSymbols = new Map([
    [
        "src/math/numerics.js#glomin",
        "GLopt/GLPK-derived glomin is excluded from the MIT-only project."
    ]
]);
const adaptedSymbols = new Map([
    [
        "src/math/ia.js#doubleBitsLE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#toDoubleLE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#lowUintLE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#highUintLE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#doubleBitsBE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#toDoubleBE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#lowUintBE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/math/ia.js#highUintBE",
        "KMP raw-bit conversion is independent of host byte order."
    ],
    [
        "src/utils/env.js#timerFun",
        "The browser timeout closure is represented by EnvTimeoutScheduler."
    ],
    [
        "src/utils/xml.js#DP",
        "The browser DOMParser constructor is replaced by the bounded KMP XML parser."
    ],
    [
        "src/utils/xml.js#parseFromString",
        "The browser DOMParser call is replaced by the bounded KMP XML parser."
    ],
    [
        "src/parser/jessiecode.js#r",
        "Kotlin resolves native creators through a stateless registry instead of caching board-bound JavaScript closures."
    ],
    [
        "src/parser/jessiecode.js#clearCache",
        "The stateless Kotlin creator registry has no board-bound closure cache to clear."
    ],
    [
        "src/parser/jessiecode.js#o",
        "The generated Jison table-expansion helper is replaced by the native bounded recursive-descent parser."
    ],
    [
        "src/parser/jessiecode.js#Parser",
        "The generated Jison Parser wrapper is replaced by the native bounded recursive-descent parser."
    ],
    [
        "src/reader/cinderella.js#makeRadiusFun",
        "The malformed upstream Compass radius closure is preserved as a structured KMP reader failure instead of leaking TypeError."
    ]
]);
const inScopeExcludedSymbols = new Map([
    ...[
        ["src/parser/jessiecode.js#create", "Legacy IE 6-8 Object.create shim is not needed by the Kotlin runtime."],
        ["src/jsxgraph.js#onmousemove", "Browser document event plumbing is owned by the Compose host."],
        ["src/jsxgraph.js#init", "Browser script-tag bootstrap is outside the pure KMP production runtime."],
        ["src/jsxgraph.js#makeReload", "Browser script reload plumbing is outside the pure KMP production runtime."],
        ["src/reader/file.js#cbp", "FileReader/XMLHttpRequest callbacks are host I/O; the KMP reader accepts supplied source bytes or text."],
        ["src/renderer/abstract.js#cancelbubble", "DOM event cancellation is owned by the Compose host."],
        ["src/renderer/abstract.js#createButton", "DOM form-node creation is outside the Compose Canvas backend."],
        ["src/renderer/canvas.js#onload", "Browser image onload dispatch is owned by the KMP image resolver."],
        ["src/renderer/svg.js#createShadowFilter", "SVG DOM filter construction is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#uniqName", "SVG DOM filter ID generation is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#toStr", "SVG DOM filter serialization is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#toURL", "SVG URL reference formatting is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#createClip", "SVG clipPath DOM construction is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#setClipPath", "SVG clipPath DOM mutation is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#updateClipPathRect", "SVG clipPath DOM mutation is outside the Compose Canvas backend."],
        ["src/renderer/svg.js#onload", "Browser image onload dispatch is owned by the KMP image resolver."],
        ["src/renderer/svg.js#onclick", "Browser navigation event dispatch is owned by the Compose host."],
        ["src/renderer/svg.js#_copyCanvasToImg", "SVG foreignObject/canvas DOM copying is outside the Compose Canvas backend."],
        ["src/element/button.js#_handler", "DOM button event wiring is owned by a platform control adapter."],
        ["src/element/checkbox.js#update", "DOM checkbox state synchronization is owned by a platform control adapter."],
        ["src/element/input.js#update", "DOM input state synchronization is owned by a platform control adapter."],
        ["src/base/coordselement.js#generatePolynomial", "Symbolic locus polynomial generation is outside the declared CAS scope."],
        ["src/base/point.js#generatePolynomial", "Symbolic locus polynomial generation is outside the declared CAS scope."],
        ["src/element/composition.js#generatePolynomial", "Symbolic locus polynomial generation is outside the declared CAS scope."]
    ]
]);

if (!existsSync(upstreamRoot)) {
    throw new Error(
        "Missing third_party/jsxgraph-src. Clone JSXGraph 1.13.3 first."
    );
}

const kotlinSources = kotlinRoots
    .flatMap((root) => listFiles(root, ".kt"))
    .map((path) => ({
        path,
        source: readFileSync(path, "utf8")
    }));
const kotlinCorpus = kotlinSources
    .map(({path, source}) => {
        const repoPath = relative(repositoryRoot, path);
        return `\n// FILE: ${repoPath}\n${source}`;
    })
    .join("\n");
const kotlinFunctionNames = new Set(
    [...kotlinCorpus.matchAll(
        /\bfun\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:<[^>{}]*>)?\s*\(/g
    )].map((match) => match[1].toLowerCase())
);

const files = listFiles(upstreamRoot, ".js")
    .filter((path) => !path.includes("/unused/"))
    .filter((path) => {
        const sourcePath = relative(
            dirname(upstreamRoot),
            path
        ).replaceAll("\\", "/");
        return !excludedSources.has(sourcePath);
    })
    .map((path) => {
        const sourcePath = relative(
            dirname(upstreamRoot),
            path
        ).replaceAll("\\", "/");
        const source = readFileSync(path, "utf8");
        const symbols = extractSymbols(source)
            .filter((symbol) => !excludedSymbols.has(
                `${sourcePath}#${symbol.name}`
            ))
            .map((symbol) => {
            const symbolKey = `${sourcePath}#${symbol.name}`;
            const mappedLocations = findMappedLocations(
                sourcePath,
                symbol.name,
                kotlinSources
            );
            const mapped = mappedLocations.length > 0;
            const adaptationReason = adaptedSymbols.get(symbolKey);
            const exclusionReason = inScopeExcludedSymbols.get(symbolKey);
            const classification = exclusionReason !== undefined
                ? "excluded"
                : adaptationReason !== undefined
                    ? mapped
                        ? "adapted"
                        : "pending"
                    : mapped
                        ? "translated"
                        : "pending";
            return {
                ...symbol,
                mapped,
                mappedLocations,
                classification,
                classificationReason:
                    exclusionReason ?? adaptationReason ?? null,
                kotlinNameMatch: kotlinFunctionNames.has(
                    symbol.name.toLowerCase()
                )
            };
        });
        return {
            source: sourcePath,
            symbolCount: symbols.length,
            mappedSymbolCount: symbols.filter(
                (symbol) => symbol.mapped
            ).length,
            surfaceSymbolCount: symbols.filter(
                (symbol) => symbol.surface
            ).length,
            mappedSurfaceSymbolCount: symbols.filter(
                (symbol) => symbol.surface && symbol.mapped
            ).length,
            symbols
        };
    });

const result = {
    schemaVersion: 2,
    upstreamVersion: readUpstreamVersion(),
    exclusions: [
        ...[...excludedSources].map(([source, reason]) => ({
            kind: "source",
            source,
            reason
        })),
        ...[...excludedSymbols].map(([key, reason]) => {
            const separator = key.lastIndexOf("#");
            return {
                kind: "symbol",
                source: key.slice(0, separator),
                symbol: key.slice(separator + 1),
                reason
            };
        }),
        {
            kind: "directory",
            source: "src/unused/",
            reason: "Upstream unused modules, including symbolic.js, are outside scope."
        }
    ],
    sourceFileCount: files.length,
    symbolCount: files.reduce(
        (count, file) => count + file.symbolCount,
        0
    ),
    mappedSymbolCount: files.reduce(
        (count, file) => count + file.mappedSymbolCount,
        0
    ),
    surfaceSymbolCount: files.reduce(
        (count, file) => count + file.surfaceSymbolCount,
        0
    ),
    mappedSurfaceSymbolCount: files.reduce(
        (count, file) => count + file.mappedSurfaceSymbolCount,
        0
    ),
    translatedSymbolCount: countClassification(files, "translated"),
    adaptedSymbolCount: countClassification(files, "adapted"),
    excludedSymbolCount: countClassification(files, "excluded"),
    pendingSymbolCount: countClassification(files, "pending"),
    files
};

mkdirSync(dirname(outputPath), {recursive: true});
writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`);
console.log(
    [
        `JSXGraph ${result.upstreamVersion}`,
        `${result.sourceFileCount} source files`,
        `${result.symbolCount} source symbols`,
        `${result.mappedSymbolCount} symbols with source-specific Kotlin mapping`,
        `${result.surfaceSymbolCount} API-surface symbols`,
        `${result.mappedSurfaceSymbolCount} API-surface symbols with source-specific Kotlin mapping`,
        `${result.translatedSymbolCount} translated symbols`,
        `${result.adaptedSymbolCount} KMP-adapted symbols`,
        `${result.excludedSymbolCount} platform/CAS-excluded symbols`,
        `${result.pendingSymbolCount} pending symbols`,
        `report: ${relative(repositoryRoot, outputPath)}`
    ].join("\n")
);

function countClassification(sourceFiles, classification) {
    return sourceFiles.reduce(
        (count, file) =>
            count + file.symbols.filter(
                (symbol) => symbol.classification === classification
            ).length,
        0
    );
}

function listFiles(root, suffix) {
    if (!existsSync(root)) {
        return [];
    }
    return readdirSync(root, {withFileTypes: true})
        .flatMap((entry) => {
            const path = resolve(root, entry.name);
            if (entry.isDirectory()) {
                return listFiles(path, suffix);
            }
            return entry.isFile() && entry.name.endsWith(suffix)
                ? [path]
                : [];
        })
        .sort();
}

function readUpstreamVersion() {
    const packagePath = resolve(
        repositoryRoot,
        "third_party/jsxgraph-src/package.json"
    );
    return JSON.parse(readFileSync(packagePath, "utf8")).version;
}

function extractSymbols(source) {
    const sanitized = maskStringsAndComments(source);
    const patterns = [
        {
            kind: "object-method",
            regex: /^[ \t]+([A-Za-z_$][A-Za-z0-9_$]*):[ \t]*function\b/gm
        },
        {
            kind: "prototype-method",
            regex: /\bprototype\.([A-Za-z_$][A-Za-z0-9_$]*)[ \t]*=[ \t]*function\b/g
        },
        {
            kind: "assigned-function",
            regex: /^(?:export[ \t]+)?(?:const|let|var)?[ \t]*(?:[A-Za-z_$][A-Za-z0-9_$]*\.)*([A-Za-z_$][A-Za-z0-9_$]*)[ \t]*=[ \t]*function\b/gm
        },
        {
            kind: "function",
            regex: /^(?:export[ \t]+)?function[ \t]+([A-Za-z_$][A-Za-z0-9_$]*)[ \t]*\(/gm
        },
        {
            kind: "arrow-function",
            regex: /^(?:export[ \t]+)?(?:const|let|var)[ \t]+([A-Za-z_$][A-Za-z0-9_$]*)[ \t]*=[ \t]*(?:async[ \t]+)?(?:\([^)]*\)|[A-Za-z_$][A-Za-z0-9_$]*)[ \t]*=>/gm
        }
    ];
    const seen = new Set();
    const symbols = [];
    for (const {kind, regex} of patterns) {
        for (const match of sanitized.matchAll(regex)) {
            const key = `${match.index}:${match[1]}`;
            if (seen.has(key)) {
                continue;
            }
            seen.add(key);
            const scopeDepth = braceDepthAt(sanitized, match.index);
            symbols.push({
                name: match[1],
                kind,
                line: lineNumberAt(source, match.index),
                scopeDepth,
                surface: isSurfaceSymbol(kind, scopeDepth)
            });
        }
    }
    return symbols.sort(
        (left, right) =>
            left.line - right.line ||
            left.name.localeCompare(right.name)
    );
}

function braceDepthAt(source, index) {
    let depth = 0;
    for (let cursor = 0; cursor < index; cursor += 1) {
        if (source[cursor] === "{") {
            depth += 1;
        } else if (source[cursor] === "}") {
            depth = Math.max(0, depth - 1);
        }
    }
    return depth;
}

function isSurfaceSymbol(kind, scopeDepth) {
    if (kind === "object-method") {
        return scopeDepth <= 1;
    }
    return scopeDepth === 0;
}

function maskStringsAndComments(source) {
    let output = "";
    let index = 0;
    let state = "code";
    let quote = "";
    while (index < source.length) {
        const current = source[index];
        const next = source[index + 1] ?? "";
        if (state === "code") {
            if (current === "/" && next === "/") {
                state = "line-comment";
                output += "  ";
                index += 2;
                continue;
            }
            if (current === "/" && next === "*") {
                state = "block-comment";
                output += "  ";
                index += 2;
                continue;
            }
            if (current === "\"" || current === "'" || current === "`") {
                state = "string";
                quote = current;
                output += " ";
                index += 1;
                continue;
            }
            output += current;
            index += 1;
            continue;
        }
        if (state === "line-comment") {
            if (current === "\n") {
                state = "code";
                output += "\n";
            } else {
                output += " ";
            }
            index += 1;
            continue;
        }
        if (state === "block-comment") {
            if (current === "*" && next === "/") {
                state = "code";
                output += "  ";
                index += 2;
            } else {
                output += current === "\n" ? "\n" : " ";
                index += 1;
            }
            continue;
        }
        if (current === "\\") {
            output += " ";
            if (next.length > 0) {
                output += next === "\n" ? "\n" : " ";
            }
            index += 2;
            continue;
        }
        if (current === quote) {
            state = "code";
            output += " ";
            index += 1;
            continue;
        }
        output += current === "\n" ? "\n" : " ";
        index += 1;
    }
    return output;
}

function lineNumberAt(source, index) {
    let line = 1;
    for (let cursor = 0; cursor < index; cursor += 1) {
        if (source[cursor] === "\n") {
            line += 1;
        }
    }
    return line;
}

function findMappedLocations(sourcePath, symbol, sources) {
    const escapedPath = escapeRegExp(sourcePath);
    const escapedSymbol = escapeRegExp(symbol);
    const mapping = new RegExp(
        `${escapedPath}[\\s\\S]{0,320}\\b${escapedSymbol}\\b`,
        "g"
    );
    return sources.flatMap(({path, source}) => {
        const locations = [];
        for (const match of source.matchAll(mapping)) {
            locations.push({
                kotlinSource: relative(repositoryRoot, path)
                    .replaceAll("\\", "/"),
                line: lineNumberAt(source, match.index)
            });
        }
        return locations;
    });
}

function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
