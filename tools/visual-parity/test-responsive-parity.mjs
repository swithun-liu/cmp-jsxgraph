import { existsSync } from "node:fs";
import puppeteer from "puppeteer";

const baseUrl = process.env.BASE_URL ?? "http://127.0.0.1:8093/";
const viewports = [
    {width: 1280, height: 900},
    {width: 1024, height: 700},
    {width: 1024, height: 520},
    {width: 800, height: 1000},
    {width: 390, height: 844}
];
const previews = ["official", "native"];
const expectedAspectRatio = 1.2;
const measurements = new Map();
const browser = await puppeteer.launch(browserLaunchOptions());

try {
    for (const viewport of viewports) {
        for (const preview of previews) {
            const page = await browser.newPage();
            const errors = [];
            page.on("pageerror", (error) => errors.push(error.message));
            page.on("console", (message) => {
                if (message.type() === "error") {
                    errors.push(message.text());
                }
            });
            await page.setViewport({
                ...viewport,
                deviceScaleFactor: 1
            });

            const url = new URL(baseUrl);
            url.searchParams.set("openParity", "true");
            url.searchParams.set("preview", preview);
            url.searchParams.set("caseId", "baseline_geometry");
            await page.goto(url.href, {
                waitUntil: "domcontentloaded",
                timeout: 60_000
            });
            await waitForPreview(page, preview);
            await new Promise((resolve) => setTimeout(resolve, 350));

            const measurement = await measurePreview(page, preview);
            assertPreviewLayout(viewport, preview, measurement, errors);
            measurements.set(
                `${viewport.width}x${viewport.height}:${preview}`,
                measurement
            );
            await page.close();
        }

        const key = `${viewport.width}x${viewport.height}`;
        const official = measurements.get(`${key}:official`);
        const native = measurements.get(`${key}:native`);
        if (!sameRectangle(official.board, native.board)) {
            throw new Error(
                `${key} Official/Native board bounds differ: ` +
                    `${JSON.stringify(official.board)} vs ` +
                    `${JSON.stringify(native.board)}`
            );
        }
        console.log(
            `${key}: shared board ${JSON.stringify(official.board)}`
        );
    }
} finally {
    await browser.close();
}

async function waitForPreview(page, preview) {
    await page.waitForFunction(
        (selectedPreview) => {
            const roots = [document];
            for (let index = 0; index < roots.length; index += 1) {
                const root = roots[index];
                if (selectedPreview === "official") {
                    const frame = root.querySelector?.(
                        'iframe[title="Official JSXGraph 1.13.3 rendering"]'
                    );
                    const svg = frame?.contentDocument?.querySelector(
                        "#jxgbox svg"
                    );
                    if (
                        svg !== null &&
                        svg !== undefined &&
                        svg.getBoundingClientRect().width > 0
                    ) {
                        return true;
                    }
                } else {
                    const canvas = root.querySelector?.("canvas");
                    if (canvas?.width > 0 && canvas?.height > 0) {
                        return true;
                    }
                }
                root.querySelectorAll?.("*").forEach((element) => {
                    if (element.shadowRoot !== null) {
                        roots.push(element.shadowRoot);
                    }
                });
            }
            return false;
        },
        {timeout: 60_000},
        preview
    );
}

async function measurePreview(page, preview) {
    return page.evaluate((selectedPreview) => {
        const roots = [document];
        let board = null;
        let officialFrame = null;
        for (let index = 0; index < roots.length; index += 1) {
            const root = roots[index];
            root.querySelectorAll?.("[aria-label]").forEach((element) => {
                const label = element.getAttribute("aria-label") ?? "";
                if (label.includes("jsxgraph-parity-board")) {
                    board = rectangle(element.getBoundingClientRect());
                }
            });
            if (selectedPreview === "official") {
                officialFrame = officialFrame ?? root.querySelector?.(
                    'iframe[title="Official JSXGraph 1.13.3 rendering"]'
                );
            }
            root.querySelectorAll?.("*").forEach((element) => {
                if (element.shadowRoot !== null) {
                    roots.push(element.shadowRoot);
                }
            });
        }
        const officialSvg = officialFrame?.contentDocument?.querySelector(
            "#jxgbox svg"
        );
        return {
            board,
            officialFrame: officialFrame === null
                ? null
                : rectangle(officialFrame.getBoundingClientRect()),
            officialSvg: officialSvg === null || officialSvg === undefined
                ? null
                : rectangle(officialSvg.getBoundingClientRect())
        };

        function rectangle(bounds) {
            return {
                x: bounds.x,
                y: bounds.y,
                width: bounds.width,
                height: bounds.height
            };
        }
    }, preview);
}

function assertPreviewLayout(viewport, preview, measurement, errors) {
    if (errors.length > 0) {
        throw new Error(
            `${preview} ${viewport.width}x${viewport.height} logged errors: ` +
                errors.join(" | ")
        );
    }
    const board = measurement.board;
    if (board === null) {
        throw new Error(
            `${preview} ${viewport.width}x${viewport.height} has no board`
        );
    }
    const aspectRatio = board.width / board.height;
    if (Math.abs(aspectRatio - expectedAspectRatio) > 0.01) {
        throw new Error(
            `${preview} ${viewport.width}x${viewport.height} board ratio ` +
                `${aspectRatio} does not match ${expectedAspectRatio}`
        );
    }
    if (board.y < 100) {
        throw new Error(
            `${preview} ${viewport.width}x${viewport.height} board overlaps ` +
                "the parity controls"
        );
    }
    if (board.y + board.height > viewport.height - 20) {
        throw new Error(
            `${preview} ${viewport.width}x${viewport.height} board clips ` +
                "the parity caption"
        );
    }
    if (
        preview === "official" &&
        (
            measurement.officialFrame === null ||
            measurement.officialSvg === null ||
            !sameSize(
                measurement.officialFrame,
                measurement.officialSvg
            )
        )
    ) {
        throw new Error(
            `${viewport.width}x${viewport.height} official renderer did not ` +
                "resize with its frame"
        );
    }
}

function sameRectangle(first, second) {
    return (
        sameNumber(first.x, second.x) &&
        sameNumber(first.y, second.y) &&
        sameNumber(first.width, second.width) &&
        sameNumber(first.height, second.height)
    );
}

function sameSize(first, second) {
    return (
        sameNumber(first.width, second.width) &&
        sameNumber(first.height, second.height)
    );
}

function sameNumber(first, second) {
    return Math.abs(first - second) <= 1;
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
