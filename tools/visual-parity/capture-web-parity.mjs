import { existsSync, mkdirSync, statSync } from "node:fs";
import { resolve } from "node:path";
import puppeteer from "puppeteer";
import {
    repositoryRoot,
    selectedParityCaseIds
} from "./corpus.mjs";

const baseUrl = process.env.BASE_URL ?? "http://127.0.0.1:8093/";
const outputDirectory = resolve(
    repositoryRoot,
    process.env.OUTPUT_DIR ?? "captures/local/web-parity/current"
);
const viewportWidth = readPositiveNumber("VIEWPORT_WIDTH", 1200);
const viewportHeight = readPositiveNumber("VIEWPORT_HEIGHT", 900);
const minimumCaptureBytes = readPositiveNumber(
    "MIN_CAPTURE_BYTES",
    5_000
);
const previews = ["native", "official"];
const pageErrors = [];

mkdirSync(outputDirectory, {recursive: true});
const browser = await puppeteer.launch(browserLaunchOptions());
try {
    const page = await browser.newPage();
    await page.setViewport({
        width: viewportWidth,
        height: viewportHeight,
        deviceScaleFactor: 1
    });
    page.on("pageerror", (error) => {
        pageErrors.push(error.message);
    });
    page.on("console", (message) => {
        if (
            message.type() === "error" ||
            message.text().includes("error compiling function")
        ) {
            pageErrors.push(message.text());
        }
    });

    for (const caseId of selectedParityCaseIds()) {
        for (const preview of previews) {
            const errorCountBeforeNavigation = pageErrors.length;
            const url = new URL(baseUrl);
            url.searchParams.set("audit", "true");
            url.searchParams.set("preview", preview);
            url.searchParams.set("caseId", caseId);
            await page.goto(url.href, {
                waitUntil: "domcontentloaded",
                timeout: 60_000
            });
            await waitForPreview(page, preview);
            await page.evaluate(() => new Promise((resolveFrame) => {
                requestAnimationFrame(() => requestAnimationFrame(resolveFrame));
            }));
            await new Promise((resolveWait) => setTimeout(resolveWait, 500));

            const target = resolve(
                outputDirectory,
                `${caseId}_${preview}.png`
            );
            await page.screenshot({
                path: target,
                omitBackground: false
            });
            const captureBytes = statSync(target).size;
            if (captureBytes < minimumCaptureBytes) {
                throw new Error(
                    `${caseId}/${preview} produced only ${captureBytes} bytes`
                );
            }
            const newErrors = pageErrors.slice(errorCountBeforeNavigation);
            if (newErrors.length > 0) {
                throw new Error(
                    `${caseId}/${preview} logged errors: ${newErrors.join(" | ")}`
                );
            }
            console.log(
                `${caseId}/${preview}: ${captureBytes} bytes`
            );
        }
    }
} finally {
    await browser.close();
}

function browserLaunchOptions() {
    const candidates = [
        process.env.PUPPETEER_EXECUTABLE_PATH,
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    ].filter(Boolean);
    const executablePath = candidates.find(existsSync);
    return {
        headless: true,
        args: ["--disable-dev-shm-usage", "--no-sandbox"],
        ...(executablePath === undefined ? {} : {executablePath})
    };
}

async function waitForPreview(page, preview) {
    const outcomeHandle = await page.waitForFunction(
        (selectedPreview) => {
            const roots = [document];
            for (let index = 0; index < roots.length; index += 1) {
                const root = roots[index];
                if (selectedPreview === "native") {
                    const canvas = root.querySelector?.("canvas");
                    if (canvas?.width > 0 && canvas?.height > 0) {
                        return {status: "ready"};
                    }
                } else {
                    const frame = root.querySelector?.(
                        'iframe[title="Official JSXGraph 1.13.3 rendering"]'
                    );
                    const error = frame?.contentDocument?.querySelector(
                        "#error[style*='display: flex']"
                    );
                    if (error !== null && error !== undefined) {
                        return {
                            status: "error",
                            message: error.textContent?.trim() ??
                                "Unknown official rendering error"
                        };
                    }
                    const svg = frame?.contentDocument?.querySelector(
                        "#jxgbox svg"
                    );
                    if (
                        svg !== null &&
                        svg !== undefined &&
                        svg.getBoundingClientRect().width > 0 &&
                        svg.getBoundingClientRect().height > 0
                    ) {
                        return {status: "ready"};
                    }
                }
                root.querySelectorAll?.("*").forEach((element) => {
                    if (element.shadowRoot !== null) {
                        roots.push(element.shadowRoot);
                    }
                });
            }
            return null;
        },
        {timeout: 60_000},
        preview
    );
    const outcome = await outcomeHandle.jsonValue();
    await outcomeHandle.dispose();
    if (outcome.status === "error") {
        throw new Error(outcome.message);
    }
}

function readPositiveNumber(name, fallback) {
    const value = Number(process.env[name] ?? fallback);
    if (!Number.isFinite(value) || value <= 0) {
        throw new Error(`${name} must be a positive number`);
    }
    return value;
}
