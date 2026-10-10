import {
    existsSync,
    mkdirSync,
    readFileSync,
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
const manifestPath = resolve(
    repositoryRoot,
    process.argv[2] ?? "docs/portable-behavior-compatibility.json"
);
const outputPath = resolve(
    repositoryRoot,
    process.argv[3] ??
        "build/reports/portable-behavior-compatibility.json"
);
const validStatuses = new Set([
    "verified",
    "implemented",
    "pending",
    "excluded"
]);

const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));
const upstreamVersion = JSON.parse(
    readFileSync(
        resolve(repositoryRoot, "third_party/jsxgraph-src/package.json"),
        "utf8"
    )
).version;

assertEqual(manifest.schemaVersion, 1, "schemaVersion");
assertEqual(
    manifest.upstream.version,
    upstreamVersion,
    "upstream.version"
);
assertPositiveNumber(manifest.targetPercent, "targetPercent");
if (!Array.isArray(manifest.exclusions) || manifest.exclusions.length === 0) {
    fail("exclusions must be a non-empty array");
}
if (
    !Array.isArray(manifest.capabilities) ||
    manifest.capabilities.length === 0
) {
    fail("capabilities must be a non-empty array");
}

const capabilityIds = new Set();
const behaviorIds = new Set();
const categoryTotals = new Map();
let totalWeight = 0;
let verifiedWeight = 0;
let implementedBehaviorCount = 0;
let pendingBehaviorCount = 0;
let verifiedBehaviorCount = 0;
let excludedBehaviorCount = 0;
let behaviorCount = 0;
let portableBehaviorCount = 0;

for (const [capabilityIndex, capability] of
    manifest.capabilities.entries()) {
    const capabilityPath = `capabilities[${capabilityIndex}]`;
    assertNonEmptyString(capability.id, `${capabilityPath}.id`);
    assertUnique(capabilityIds, capability.id, `${capabilityPath}.id`);
    assertNonEmptyString(
        capability.category,
        `${capabilityPath}.category`
    );
    assertNonEmptyString(capability.title, `${capabilityPath}.title`);
    assertPositiveNumber(capability.weight, `${capabilityPath}.weight`);
    if (
        !Array.isArray(capability.behaviors) ||
        capability.behaviors.length === 0
    ) {
        fail(`${capabilityPath}.behaviors must be a non-empty array`);
    }

    validateEvidence(capability, capabilityPath);
    const portableBehaviors = capability.behaviors.filter(
        (behavior) => behavior.status !== "excluded"
    );
    if (portableBehaviors.length === 0) {
        fail(`${capabilityPath}.behaviors must include portable behavior`);
    }
    const verified = portableBehaviors.filter(
        (behavior) => behavior.status === "verified"
    ).length;
    const completion = verified / portableBehaviors.length;
    const capabilityVerifiedWeight = capability.weight * completion;
    totalWeight += capability.weight;
    verifiedWeight += capabilityVerifiedWeight;

    for (const [behaviorIndex, behavior] of
        capability.behaviors.entries()) {
        const behaviorPath =
            `${capabilityPath}.behaviors[${behaviorIndex}]`;
        assertNonEmptyString(behavior.id, `${behaviorPath}.id`);
        assertUnique(
            behaviorIds,
            `${capability.id}.${behavior.id}`,
            `${behaviorPath}.id`
        );
        if (!validStatuses.has(behavior.status)) {
            fail(
                `${behaviorPath}.status must be one of ` +
                    [...validStatuses].join(", ")
            );
        }
        assertNonEmptyString(behavior.note, `${behaviorPath}.note`);
        behaviorCount += 1;
        if (behavior.status === "excluded") {
            excludedBehaviorCount += 1;
        } else if (behavior.status === "verified") {
            verifiedBehaviorCount += 1;
        } else if (behavior.status === "implemented") {
            implementedBehaviorCount += 1;
        } else {
            pendingBehaviorCount += 1;
        }
    }
    portableBehaviorCount += portableBehaviors.length;

    const category = categoryTotals.get(capability.category) ?? {
        weight: 0,
        verifiedWeight: 0,
        capabilityCount: 0
    };
    category.weight += capability.weight;
    category.verifiedWeight += capabilityVerifiedWeight;
    category.capabilityCount += 1;
    categoryTotals.set(capability.category, category);
}

const scorePercent = round(100 * verifiedWeight / totalWeight, 4);
const result = {
    schemaVersion: 1,
    manifest: relative(repositoryRoot, manifestPath),
    upstreamVersion,
    assessedAt: manifest.assessedAt,
    methodology: manifest.methodology,
    exclusions: manifest.exclusions,
    targetPercent: manifest.targetPercent,
    scorePercent,
    targetMet: scorePercent >= manifest.targetPercent,
    totalWeight: round(totalWeight, 4),
    verifiedWeight: round(verifiedWeight, 4),
    capabilityCount: manifest.capabilities.length,
    behaviorCount,
    portableBehaviorCount,
    verifiedBehaviorCount,
    implementedBehaviorCount,
    pendingBehaviorCount,
    excludedBehaviorCount,
    categories: [...categoryTotals.entries()]
        .map(([category, totals]) => ({
            category,
            capabilityCount: totals.capabilityCount,
            weight: round(totals.weight, 4),
            verifiedWeight: round(totals.verifiedWeight, 4),
            scorePercent: round(
                100 * totals.verifiedWeight / totals.weight,
                4
            )
        }))
        .sort((left, right) =>
            left.category.localeCompare(right.category)
        ),
    capabilities: manifest.capabilities.map((capability) => {
        const portableBehaviors = capability.behaviors.filter(
            (behavior) => behavior.status !== "excluded"
        );
        const verified = portableBehaviors.filter(
            (behavior) => behavior.status === "verified"
        ).length;
        return {
            id: capability.id,
            category: capability.category,
            title: capability.title,
            weight: capability.weight,
            behaviorCount: capability.behaviors.length,
            portableBehaviorCount: portableBehaviors.length,
            verifiedBehaviorCount: verified,
            scorePercent: round(
                100 * verified / portableBehaviors.length,
                4
            ),
            weightedScore: round(
                capability.weight *
                    verified /
                    portableBehaviors.length,
                4
            ),
            excluded: capability.behaviors
                .filter((behavior) => behavior.status === "excluded")
                .map((behavior) => ({
                    id: behavior.id,
                    note: behavior.note
                })),
            pending: capability.behaviors
                .filter(
                    (behavior) =>
                        behavior.status !== "verified" &&
                        behavior.status !== "excluded"
                )
                .map((behavior) => ({
                    id: behavior.id,
                    status: behavior.status,
                    note: behavior.note
                }))
        };
    })
};

mkdirSync(dirname(outputPath), {recursive: true});
writeFileSync(outputPath, `${JSON.stringify(result, null, 2)}\n`);

console.log(
    [
        `JSXGraph ${upstreamVersion} portable behavior audit`,
        `${result.capabilityCount} weighted capabilities`,
        `${result.verifiedBehaviorCount}/${result.portableBehaviorCount} ` +
            "portable behavior slices verified " +
            `(${result.excludedBehaviorCount} excluded of ` +
            `${result.behaviorCount} assessed)`,
        `weighted score: ${scorePercent.toFixed(2)}%`,
        `target: ${manifest.targetPercent.toFixed(2)}% (${result.targetMet ? "met" : "not met"})`,
        `report: ${relative(repositoryRoot, outputPath)}`
    ].join("\n")
);

function validateEvidence(capability, capabilityPath) {
    const evidence = capability.evidence;
    if (evidence == null || typeof evidence !== "object") {
        fail(`${capabilityPath}.evidence must be an object`);
    }
    const requiredGroups = ["sources", "implementation", "tests"];
    for (const group of requiredGroups) {
        if (!Array.isArray(evidence[group]) || evidence[group].length === 0) {
            fail(
                `${capabilityPath}.evidence.${group} must be a non-empty array`
            );
        }
    }
    for (const group of [
        "sources",
        "implementation",
        "tests",
        "fixtures",
        "visual"
    ]) {
        const entries = evidence[group] ?? [];
        if (!Array.isArray(entries)) {
            fail(`${capabilityPath}.evidence.${group} must be an array`);
        }
        for (const [index, entry] of entries.entries()) {
            assertNonEmptyString(
                entry,
                `${capabilityPath}.evidence.${group}[${index}]`
            );
            const sourcePath = entry.split("#", 1)[0];
            if (!existsSync(resolve(repositoryRoot, sourcePath))) {
                fail(
                    `${capabilityPath}.evidence.${group}[${index}] ` +
                        `does not exist: ${sourcePath}`
                );
            }
        }
    }
}

function assertEqual(actual, expected, path) {
    if (actual !== expected) {
        fail(`${path} must be ${JSON.stringify(expected)}`);
    }
}

function assertPositiveNumber(value, path) {
    if (
        typeof value !== "number" ||
        !Number.isFinite(value) ||
        value <= 0
    ) {
        fail(`${path} must be a positive finite number`);
    }
}

function assertNonEmptyString(value, path) {
    if (typeof value !== "string" || value.trim().length === 0) {
        fail(`${path} must be a non-empty string`);
    }
}

function assertUnique(values, value, path) {
    if (values.has(value)) {
        fail(`${path} is duplicated: ${value}`);
    }
    values.add(value);
}

function round(value, digits) {
    const scale = 10 ** digits;
    return Math.round(value * scale) / scale;
}

function fail(message) {
    throw new Error(`Portable behavior manifest error: ${message}`);
}
