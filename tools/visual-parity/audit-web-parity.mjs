import {
    existsSync,
    mkdirSync,
    statSync,
    writeFileSync
} from "node:fs";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";
import {
    repositoryRoot,
    selectedParityCaseIds
} from "./corpus.mjs";

const inputDirectory = resolve(
    repositoryRoot,
    process.env.INPUT_DIR ?? "captures/local/web-parity/current"
);
const minimumSsim = readUnitInterval("MIN_BOARD_SSIM", 0.90);
const minimumCaptureBytes = readPositiveNumber(
    "MIN_CAPTURE_BYTES",
    5_000
);
const results = [];
const failures = [];

requireFfmpeg();
mkdirSync(inputDirectory, {recursive: true});
for (const caseId of selectedParityCaseIds()) {
    const nativePath = resolve(inputDirectory, `${caseId}_native.png`);
    const officialPath = resolve(
        inputDirectory,
        `${caseId}_official.png`
    );
    validateCapture(`${caseId}/native`, nativePath);
    validateCapture(`${caseId}/official`, officialPath);

    const ssim = compareSsim(officialPath, nativePath);
    if (ssim < minimumSsim) {
        failures.push(
            `${caseId} SSIM ${ssim.toFixed(6)} is below ` +
                minimumSsim.toFixed(2)
        );
    }
    const contactSheetPath = resolve(
        inputDirectory,
        `${caseId}_contact-sheet.png`
    );
    createContactSheet(officialPath, nativePath, contactSheetPath);
    results.push({
        caseId,
        ssim,
        minimumSsim,
        officialBytes: statSync(officialPath).size,
        nativeBytes: statSync(nativePath).size
    });
}

writeFileSync(
    resolve(inputDirectory, "report.json"),
    `${JSON.stringify({
        schemaVersion: 1,
        generatedAt: new Date().toISOString(),
        minimumSsim,
        caseCount: results.length,
        failures,
        cases: results
    }, null, 2)}\n`
);
writeFileSync(
    resolve(inputDirectory, "summary.tsv"),
    [
        "caseId\tssim\tminimum\tofficialBytes\tnativeBytes",
        ...results.map((result) => [
            result.caseId,
            result.ssim.toFixed(6),
            result.minimumSsim.toFixed(2),
            result.officialBytes,
            result.nativeBytes
        ].join("\t"))
    ].join("\n") + "\n"
);

for (const result of results) {
    console.log(
        `${result.caseId}: SSIM ${result.ssim.toFixed(6)} ` +
            `(minimum ${minimumSsim.toFixed(2)})`
    );
}
if (failures.length > 0) {
    for (const failure of failures) {
        console.error(`- ${failure}`);
    }
    process.exitCode = 1;
}

function requireFfmpeg() {
    const result = spawnSync("ffmpeg", ["-version"], {
        encoding: "utf8"
    });
    if (result.status !== 0) {
        throw new Error("ffmpeg is required for visual parity auditing");
    }
}

function validateCapture(label, filePath) {
    if (!existsSync(filePath)) {
        throw new Error(`Missing capture: ${filePath}`);
    }
    const size = statSync(filePath).size;
    if (size < minimumCaptureBytes) {
        failures.push(
            `${label} contains only ${size} bytes; expected at least ` +
                minimumCaptureBytes
        );
    }
}

function compareSsim(officialPath, nativePath) {
    const result = spawnSync(
        "ffmpeg",
        [
            "-hide_banner",
            "-i",
            officialPath,
            "-i",
            nativePath,
            "-lavfi",
            "[0:v][1:v]ssim",
            "-f",
            "null",
            "-"
        ],
        {encoding: "utf8"}
    );
    const output = `${result.stdout}\n${result.stderr}`;
    const match = output.match(/All:([0-9.]+)/);
    if (result.status !== 0 || match === null) {
        throw new Error(`ffmpeg SSIM failed:\n${output}`);
    }
    return Number(match[1]);
}

function createContactSheet(
    officialPath,
    nativePath,
    outputPath
) {
    const result = spawnSync(
        "ffmpeg",
        [
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            officialPath,
            "-i",
            nativePath,
            "-filter_complex",
            "hstack=inputs=2",
            "-frames:v",
            "1",
            "-y",
            outputPath
        ],
        {encoding: "utf8"}
    );
    if (result.status !== 0) {
        throw new Error(
            `ffmpeg contact sheet failed:\n${result.stderr}`
        );
    }
}

function readPositiveNumber(name, fallback) {
    const value = Number(process.env[name] ?? fallback);
    if (!Number.isFinite(value) || value <= 0) {
        throw new Error(`${name} must be a positive number`);
    }
    return value;
}

function readUnitInterval(name, fallback) {
    const value = Number(process.env[name] ?? fallback);
    if (!Number.isFinite(value) || value < 0 || value > 1) {
        throw new Error(`${name} must be between zero and one`);
    }
    return value;
}
