import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    "../.."
);

export function parityCaseIds() {
    if (process.env.CORPUS_SOURCE === "production") {
        return productionCaseIds();
    }
    if (
        process.env.CORPUS_SOURCE !== undefined &&
        process.env.CORPUS_SOURCE !== "parity"
    ) {
        throw new Error(
            `Unknown CORPUS_SOURCE: ${process.env.CORPUS_SOURCE}`
        );
    }
    const source = readFileSync(
        resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/commonMain/kotlin/" +
                "com/swithun/jsxgraph/debugui/JsxGraphParityCorpus.kt"
        ),
        "utf8"
    );
    const defaultId = source.match(
        /const val DEFAULT_CASE_ID:\s*String\s*=\s*"([^"]+)"/
    )?.[1];
    if (defaultId === undefined) {
        throw new Error("Could not read DEFAULT_CASE_ID from parity corpus");
    }

    const ids = [];
    const casePattern =
        /JsxGraphParityCase\(\s*id\s*=\s*(DEFAULT_CASE_ID|"[^"]+")/g;
    for (const match of source.matchAll(casePattern)) {
        ids.push(
            match[1] === "DEFAULT_CASE_ID"
                ? defaultId
                : match[1].slice(1, -1)
        );
    }
    if (ids.length === 0 || new Set(ids).size !== ids.length) {
        throw new Error("Parity corpus must contain unique case IDs");
    }
    return ids;
}

function productionCaseIds() {
    const source = readFileSync(
        resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/commonMain/kotlin/" +
                "com/swithun/jsxgraph/debugui/generated/" +
                "ProductionCorpus.kt"
        ),
        "utf8"
    );
    const ids = [
        ...source.matchAll(
            /ProductionCorpusCase\(\s*id\s*=\s*"([^"]+)"/g
        )
    ].map((match) => match[1]);
    if (ids.length === 0 || new Set(ids).size !== ids.length) {
        throw new Error("Production corpus must contain unique case IDs");
    }
    return ids;
}

export function selectedParityCaseIds() {
    const requested = new Set(
        (process.env.PARITY_CASE_IDS ?? "")
            .split(/[\s,]+/)
            .filter(Boolean)
    );
    const available = parityCaseIds();
    if (requested.size === 0) {
        return available;
    }
    const unknown = [...requested].filter((id) => !available.includes(id));
    if (unknown.length > 0) {
        throw new Error(`Unknown parity case IDs: ${unknown.join(", ")}`);
    }
    return available.filter((id) => requested.has(id));
}
