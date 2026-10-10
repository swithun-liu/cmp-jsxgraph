import {readFileSync} from "node:fs";
import {dirname, relative, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    ".."
);
const coreRuntimePath = resolve(
    repositoryRoot,
    "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/parser/CoreGeometryElementRuntime.kt"
);
const evaluatorPath = resolve(
    repositoryRoot,
    "jsxgraph-core/src/commonMain/kotlin/" +
        "com/swithun/jsxgraph/core/parser/JessieCodeEvaluator.kt"
);
const coreRuntime = readFileSync(coreRuntimePath, "utf8");
const evaluator = readFileSync(evaluatorPath, "utf8");

const mappings = [
    ["src/base/element.js", "GeometryElement", coreRuntime],
    ["src/base/coordselement.js", "CoordsElement", coreRuntime],
    ["src/base/line.js", "Line", coreRuntime],
    ["src/base/circle.js", "Circle", coreRuntime],
    ["src/base/curve.js", "Curve", coreRuntime],
    ["src/base/polygon.js", "Polygon", coreRuntime],
    ["src/base/text.js", "Text", coreRuntime],
    ["src/base/image.js", "Image", coreRuntime],
    ["src/base/foreignobject.js", "ForeignObject", coreRuntime],
    ["src/base/turtle.js", "Turtle", coreRuntime],
    ["src/base/transformation.js", "Transformation", evaluator],
    ["src/base/board.js", "Board", evaluator],
    ["src/base/composition.js", "Composition", evaluator],
    ["src/base/group.js", "Group", evaluator],
    ["src/3d/polyhedron3d.js", "Polyhedron3D", coreRuntime],
    ["src/3d/sphere3d.js", "Sphere3D", coreRuntime]
];
const emptyMappings = [
    ["src/3d/face3d.js", "Face3D"],
    ["src/3d/point3d.js", "Point3D"],
    ["src/3d/linspace3d.js", "Line3D"],
    ["src/3d/linspace3d.js", "Plane3D"],
    ["src/3d/text3d.js", "Text3D"],
    ["src/3d/surface3d.js", "Surface3D"],
    ["src/3d/curve3d.js", "Curve3D"],
    ["src/3d/view3d.js", "View3D"]
];

let aliasCount = 0;
const rows = [];
for (const [sourcePath, className, kotlinSource] of mappings) {
    const aliases = methodMapAliases(sourcePath, className);
    if (aliases.length === 0) {
        fail(`${className} methodMap unexpectedly has no aliases`);
    }
    const missing = aliases.filter(
        (alias) => !quotedAliasPattern(alias).test(kotlinSource)
    );
    if (missing.length > 0) {
        fail(
            `${className} aliases missing from Kotlin runtime: ` +
                missing.join(", ")
        );
    }
    aliasCount += aliases.length;
    rows.push(`${className}: ${aliases.length}/${aliases.length}`);
}

for (const [sourcePath, className] of emptyMappings) {
    const aliases = methodMapAliases(sourcePath, className);
    if (aliases.length !== 0) {
        fail(
            `${className} methodMap is no longer an upstream TODO: ` +
                aliases.join(", ")
        );
    }
}

console.log(
    [
        "JSXGraph 1.13.3 JessieCode methodMap audit",
        `${aliasCount}/${aliasCount} active aliases have Kotlin routing evidence`,
        `${emptyMappings.length}/${emptyMappings.length} upstream-empty 3D maps preserved`,
        ...rows,
        `core runtime: ${relative(repositoryRoot, coreRuntimePath)}`,
        `evaluator: ${relative(repositoryRoot, evaluatorPath)}`
    ].join("\n")
);

function methodMapAliases(sourcePath, className) {
    const absolutePath = resolve(
        repositoryRoot,
        "third_party/jsxgraph-src",
        sourcePath
    );
    const source = readFileSync(absolutePath, "utf8");
    const marker = `Type.copyMethodMap(JXG.${className}, {`;
    const start = source.indexOf(marker);
    if (start < 0) {
        fail(`Missing ${className} methodMap in ${sourcePath}`);
    }
    const bodyStart = start + marker.length;
    const bodyEnd = source.indexOf("\n});", bodyStart);
    if (bodyEnd < 0) {
        fail(`Unterminated ${className} methodMap in ${sourcePath}`);
    }
    const body = source.slice(bodyStart, bodyEnd);
    return [...body.matchAll(
        /^\s*([A-Za-z_$][A-Za-z0-9_$]*):\s*["']/gm
    )].map((match) => match[1]);
}

function quotedAliasPattern(alias) {
    const escaped = alias.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    return new RegExp(`["']${escaped}["']`);
}

function fail(message) {
    console.error(`JessieCode methodMap audit failed: ${message}`);
    process.exit(1);
}
