import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import puppeteer from "puppeteer";
import { repositoryRoot } from "./corpus.mjs";

const baseUrl = process.env.BASE_URL ?? "http://127.0.0.1:8093/";
const outputDirectory = resolve(
    repositoryRoot,
    process.env.OUTPUT_DIR ?? "captures/local/web-load/current"
);
const viewportWidth = readPositiveNumber("VIEWPORT_WIDTH", 1200);
const viewportHeight = readPositiveNumber("VIEWPORT_HEIGHT", 900);
const expectedCaseCount = 30;
const finalCaseId = "prod_mixed_operations_board";
const maximumFirstContentMillis = readPositiveNumber(
    "MAX_FIRST_CONTENT_MS",
    5_000
);
const maximumTraversalMillis = readPositiveNumber(
    "MAX_TRAVERSAL_MS",
    15_000
);
const maximumRetainedHeapBytes = readPositiveNumber(
    "MAX_RETAINED_HEAP_BYTES",
    96 * 1024 * 1024
);
const expectChunkedWasm = process.env.EXPECT_CHUNKED_WASM === "true";
const simulateCompressedPartRetry =
    process.env.SIMULATE_COMPRESSED_PART_RETRY === "true";
const pageErrors = [];
const requestedUrls = [];
const simulatedCompressedPartRequests = new WeakSet();
let activeCompressedPartRequests = 0;
let maximumConcurrentCompressedPartRequests = 0;
let simulatedFailedCompressedPartPath = null;
let ignoredSimulatedConsoleErrorCount = 0;

mkdirSync(outputDirectory, {recursive: true});
const browser = await puppeteer.launch(browserLaunchOptions());
try {
    const page = await browser.newPage();
    if (simulateCompressedPartRetry) {
        await page.setRequestInterception(true);
    }
    await page.setViewport({
        width: viewportWidth,
        height: viewportHeight,
        deviceScaleFactor: 1
    });
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("request", (request) => {
        requestedUrls.push(request.url());
        const isCompressedPart = isCompressedPartRequest(request.url());
        if (
            simulateCompressedPartRetry &&
            simulatedFailedCompressedPartPath === null &&
            isCompressedPart
        ) {
            simulatedFailedCompressedPartPath =
                new URL(request.url()).pathname;
            simulatedCompressedPartRequests.add(request);
            void request.respond({
                status: 503,
                contentType: "text/plain",
                body: "Simulated transient payload failure"
            });
            return;
        }
        if (isCompressedPart) {
            activeCompressedPartRequests += 1;
            maximumConcurrentCompressedPartRequests = Math.max(
                maximumConcurrentCompressedPartRequests,
                activeCompressedPartRequests
            );
        }
        if (simulateCompressedPartRetry) {
            void request.continue();
        }
    });
    const finishRequest = (request) => {
        if (
            isCompressedPartRequest(request.url()) &&
            !simulatedCompressedPartRequests.has(request)
        ) {
            activeCompressedPartRequests -= 1;
        }
    };
    page.on("requestfinished", finishRequest);
    page.on("requestfailed", finishRequest);
    page.on("console", (message) => {
        if (message.type() === "error") {
            const messageText = message.text();
            if (
                simulateCompressedPartRetry &&
                ignoredSimulatedConsoleErrorCount === 0 &&
                messageText.includes(
                    "server responded with a status of 503"
                )
            ) {
                ignoredSimulatedConsoleErrorCount += 1;
            } else {
                pageErrors.push(messageText);
            }
        }
    });

    const startedAt = performance.now();
    const url = new URL(baseUrl);
    url.searchParams.set("load", "true");
    await page.goto(url.href, {
        waitUntil: "domcontentloaded",
        timeout: 60_000
    });
    await waitForSemanticLabel(
        page,
        `stable-load:ready:${expectedCaseCount}`
    );
    const firstContentMillis = performance.now() - startedAt;
    await page.screenshot({
        path: resolve(outputDirectory, "web-load-top.png"),
        omitBackground: false
    });

    await page.mouse.move(viewportWidth * 0.5, viewportHeight * 0.6);
    let reachedFinalCase = false;
    for (let attempt = 0; attempt < 160; attempt += 1) {
        reachedFinalCase = await hasSemanticLabel(
            page,
            `stable-load-case:${finalCaseId}`
        );
        if (reachedFinalCase) {
            break;
        }
        await page.mouse.wheel({deltaY: 720});
        await new Promise((resolveWait) => setTimeout(resolveWait, 40));
    }
    if (!reachedFinalCase) {
        throw new Error(`Did not reach final load case ${finalCaseId}`);
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 250));
    const traversalMillis = performance.now() - startedAt;

    await page.evaluate(() => {
        if (typeof globalThis.gc === "function") {
            globalThis.gc();
        }
    });
    await new Promise((resolveWait) => setTimeout(resolveWait, 150));
    const metrics = await page.metrics();
    await page.screenshot({
        path: resolve(outputDirectory, "web-load-bottom.png"),
        omitBackground: false
    });
    const wasmRequests = summarizeWasmRequests(requestedUrls);
    const simulatedRetryRequestCount =
        simulatedFailedCompressedPartPath === null
            ? 0
            : requestedUrls.filter(
                (requestUrl) =>
                    new URL(requestUrl).pathname ===
                        simulatedFailedCompressedPartPath
            ).length;
    const usesNativeInstantiateStreaming = await page.evaluate(() =>
        Function.prototype.toString
            .call(WebAssembly.instantiateStreaming)
            .includes("[native code]")
    );
    const supportsStreamingDecompression = await page.evaluate(() =>
        typeof DecompressionStream === "function"
    );
    const failures = [];
    if (firstContentMillis > maximumFirstContentMillis) {
        failures.push(
            `First content took ${Math.round(firstContentMillis)}ms; ` +
                `budget is ${maximumFirstContentMillis}ms`
        );
    }
    if (traversalMillis > maximumTraversalMillis) {
        failures.push(
            `Traversal took ${Math.round(traversalMillis)}ms; ` +
                `budget is ${maximumTraversalMillis}ms`
        );
    }
    if (metrics.JSHeapUsedSize > maximumRetainedHeapBytes) {
        failures.push(
            `Retained JS heap was ${metrics.JSHeapUsedSize} bytes; ` +
                `budget is ${maximumRetainedHeapBytes} bytes`
        );
    }
    if (pageErrors.length > 0) {
        failures.push(`Browser errors: ${pageErrors.join(" | ")}`);
    }
    if (
        expectChunkedWasm &&
        (
            wasmRequests.manifestCount === 0 ||
            wasmRequests.partCount === 0 ||
            wasmRequests.directCount > 0
        )
    ) {
        failures.push(
            "Expected chunked Wasm requests without direct Wasm downloads, " +
                `observed ${JSON.stringify(wasmRequests)}`
        );
    }
    if (expectChunkedWasm && !usesNativeInstantiateStreaming) {
        failures.push(
            "Chunked loading replaced WebAssembly.instantiateStreaming"
        );
    }
    if (
        expectChunkedWasm &&
        supportsStreamingDecompression &&
        wasmRequests.compressedPartCount === 0
    ) {
        failures.push("Chunked loading did not use compressed Wasm parts");
    }
    if (
        expectChunkedWasm &&
        supportsStreamingDecompression &&
        wasmRequests.compressedJsonPartCount > 0
    ) {
        failures.push(
            "Chunked loading used throttled JSON Wasm payloads"
        );
    }
    if (
        expectChunkedWasm &&
        maximumConcurrentCompressedPartRequests > 1
    ) {
        failures.push(
            "Chunked loading started concurrent compressed payloads: " +
                maximumConcurrentCompressedPartRequests
        );
    }
    if (
        simulateCompressedPartRetry &&
        simulatedRetryRequestCount < 2
    ) {
        failures.push(
            "Chunked loading did not retry the interrupted payload"
        );
    }
    const report = {
        schemaVersion: 1,
        caseCount: expectedCaseCount,
        finalCaseId,
        firstContentMillis: Math.round(firstContentMillis),
        traversalMillis: Math.round(traversalMillis),
        retainedJsHeapBytes: metrics.JSHeapUsedSize,
        jsHeapTotalBytes: metrics.JSHeapTotalSize,
        viewport: {
            width: viewportWidth,
            height: viewportHeight
        },
        budgets: {
            maximumFirstContentMillis,
            maximumTraversalMillis,
            maximumRetainedHeapBytes
        },
        wasmRequests,
        maximumConcurrentCompressedPartRequests,
        simulateCompressedPartRetry,
        simulatedFailedCompressedPartPath,
        simulatedRetryRequestCount,
        ignoredSimulatedConsoleErrorCount,
        usesNativeInstantiateStreaming,
        supportsStreamingDecompression,
        browserErrors: pageErrors,
        failures
    };
    writeFileSync(
        resolve(outputDirectory, "metrics.json"),
        `${JSON.stringify(report, null, 2)}\n`
    );
    console.log(JSON.stringify(report));
    if (failures.length > 0) {
        throw new Error(failures.join(" | "));
    }
} finally {
    await browser.close();
}

async function waitForSemanticLabel(page, label) {
    await page.waitForFunction(
        (expectedLabel) => {
            const roots = [document];
            for (let index = 0; index < roots.length; index += 1) {
                const root = roots[index];
                for (const element of root.querySelectorAll?.("*") ?? []) {
                    if (element.shadowRoot !== null) {
                        roots.push(element.shadowRoot);
                    }
                    if (element.getAttribute?.("aria-label") === expectedLabel) {
                        return true;
                    }
                }
            }
            return false;
        },
        {timeout: 60_000},
        label
    );
}

async function hasSemanticLabel(page, label) {
    return page.evaluate((expectedLabel) => {
        const roots = [document];
        for (let index = 0; index < roots.length; index += 1) {
            const root = roots[index];
            for (const element of root.querySelectorAll?.("*") ?? []) {
                if (element.shadowRoot !== null) {
                    roots.push(element.shadowRoot);
                }
                if (element.getAttribute?.("aria-label") === expectedLabel) {
                    return true;
                }
            }
        }
        return false;
    }, label);
}

function browserLaunchOptions() {
    const candidates = [
        process.env.PUPPETEER_EXECUTABLE_PATH,
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    ].filter(Boolean);
    const executablePath = candidates.find(existsSync);
    return {
        headless: true,
        args: [
            "--disable-dev-shm-usage",
            "--no-sandbox",
            "--js-flags=--expose-gc"
        ],
        ...(executablePath === undefined ? {} : {executablePath})
    };
}

function readPositiveNumber(name, fallback) {
    const value = Number(process.env[name] ?? fallback);
    if (!Number.isFinite(value) || value <= 0) {
        throw new Error(`${name} must be a positive number`);
    }
    return value;
}

function summarizeWasmRequests(urls) {
    const paths = urls.map((url) => new URL(url).pathname);
    return {
        directCount: paths.filter((path) => path.endsWith(".wasm")).length,
        manifestCount: paths.filter((path) =>
            path.endsWith(".wasm.chunks.json")
        ).length,
        partCount: paths.filter((path) =>
            /\.wasm\.part-|\.payload\.part-/.test(path)
        ).length,
        compressedPartCount: paths.filter((path) =>
            path.includes(".payload.part-")
        ).length,
        compressedJsonPartCount: paths.filter((path) =>
            path.includes(".payload.part-") && path.endsWith(".json")
        ).length
    };
}

function isCompressedPartRequest(url) {
    return new URL(url).pathname.includes(".payload.part-");
}
