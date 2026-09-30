/*
 * Official JSXGraph 1.13.3 theme-data fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/themes.mjs
 */
import fs from "node:fs";
import vm from "node:vm";
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(import.meta.url);
const JXG = require(
    resolve(
        repositoryRoot,
        "jsxgraph-debug-ui/src/androidMain/assets/" +
            "jsxgraphcore-1.13.3.js"
    )
);

const merge = (target, source) => {
    for (const [key, value] of Object.entries(source)) {
        if (value !== null && typeof value === "object" &&
            !Array.isArray(value)) {
            const current = target[key];
            target[key] = merge(
                current !== null && typeof current === "object" &&
                    !Array.isArray(current) ? current : {},
                value
            );
        } else {
            target[key] = value;
        }
    }
    return target;
};

const loadLegacyPatch = (name) => {
    const sandbox = {
        JXG: {
            Options: {},
            merge,
            isAndroid: () => false,
            isApple: () => false
        }
    };
    vm.runInNewContext(
        fs.readFileSync(
            resolve(
                repositoryRoot,
                `third_party/jsxgraph-src/src/themes/${name}.js`
            ),
            "utf8"
        ),
        sandbox
    );
    return sandbox.JXG.Options;
};

const flatten = (value, path = "", output = []) => {
    if (Array.isArray(value)) {
        value.forEach((child, index) => {
            flatten(child, `${path}[${index}]`, output);
        });
    } else if (value !== null && typeof value === "object") {
        Object.keys(value).sort().forEach((key) => {
            flatten(value[key], path ? `${path}.${key}` : key, output);
        });
    } else {
        output.push(
            `${path}=${
                typeof value === "string" ? JSON.stringify(value) : String(value)
            }`
        );
    }
    return output;
};

const fingerprint = (value) => {
    const leaves = flatten(value);
    let hash = 0xcbf29ce484222325n;
    for (const character of leaves.join("\n")) {
        hash ^= BigInt(character.codePointAt(0));
        hash = BigInt.asUintN(64, hash * 0x100000001b3n);
    }
    return {
        leaves: leaves.length,
        hash: hash.toString(16).padStart(16, "0")
    };
};

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            dark: fingerprint(loadLegacyPatch("dark")),
            gui: fingerprint(loadLegacyPatch("gui")),
            mono_thin: fingerprint(JXG.themes.mono_thin)
        },
        null,
        2
    )
);
