import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const repositoryRoot = resolve(
    dirname(fileURLToPath(import.meta.url)),
    "../.."
);

export function parityCaseIds() {
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
