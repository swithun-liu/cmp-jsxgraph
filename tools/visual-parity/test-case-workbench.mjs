import {existsSync} from "node:fs";
import {Buffer} from "node:buffer";
import puppeteer from "puppeteer";

const baseUrl = process.env.BASE_URL ?? "http://127.0.0.1:8093/";
const expectedCaseCount = 95;
const defaultCaseId = "prod_geometry_segment_network";
const secondCaseId = "prod_geometry_fixed_length_segment";
const menuTargetCaseId = "prod_curve_quadratic_trend";
const viewports = [
    {name: "desktop", width: 1280, height: 900},
    {name: "mobile", width: 390, height: 844}
];
const browser = await puppeteer.launch(browserLaunchOptions());

try {
    for (const viewport of viewports) {
        const page = await browser.newPage();
        const errors = [];
        page.on("pageerror", (error) => errors.push(error.message));
        page.on("console", (message) => {
            if (message.type() === "error") {
                errors.push(message.text());
            }
        });
        await page.setViewport({
            width: viewport.width,
            height: viewport.height,
            deviceScaleFactor: 1
        });
        await page.goto(baseUrl, {
            waitUntil: "domcontentloaded",
            timeout: 60_000
        });
        await waitForLabel(
            page,
            `jsxgraph-case:${defaultCaseId}`
        );

        const initialLabels = await ariaLabels(page);
        assertWorkbenchLayout(viewport, initialLabels);
        const initialBoard = await captureBoard(page, initialLabels);

        await clickLabel(page, "Next case");
        await waitForLabel(page, `jsxgraph-case:${secondCaseId}`);
        const secondLabels = await ariaLabels(page);
        const secondBoard = await captureBoard(page, secondLabels);
        if (Buffer.compare(initialBoard, secondBoard) === 0) {
            throw new Error(
                `${viewport.name} next case did not change board pixels`
            );
        }
        assertQueryCase(page, secondCaseId);

        await clickLabel(page, "Previous case");
        await waitForLabel(page, `jsxgraph-case:${defaultCaseId}`);
        assertQueryCase(page, defaultCaseId);

        await clickLabel(page, "Select case:");
        await waitForLabel(page, `Open case:${menuTargetCaseId}`);
        await new Promise((resolve) => setTimeout(resolve, 200));
        const menuLabels = (await ariaLabels(page)).filter((item) =>
            item.label.startsWith("Open case:")
        );
        if (menuLabels.length !== expectedCaseCount) {
            throw new Error(
                `${viewport.name} menu exposes ${menuLabels.length} cases, ` +
                    `expected ${expectedCaseCount}`
            );
        }

        await clickExactLabel(page, `Open case:${menuTargetCaseId}`);
        await waitForQueryCase(page, menuTargetCaseId);
        await new Promise((resolve) => setTimeout(resolve, 500));
        const menuTargetBoard = await captureRectangle(
            page,
            findLabel(initialLabels, "jsxgraph-parity-board")
        );
        if (Buffer.compare(initialBoard, menuTargetBoard) === 0) {
            throw new Error(
                `${viewport.name} menu selection did not change board pixels`
            );
        }
        assertQueryCase(page, menuTargetCaseId);

        await page.reload({
            waitUntil: "domcontentloaded",
            timeout: 60_000
        });
        await waitForLabel(
            page,
            `jsxgraph-case:${menuTargetCaseId}`
        );
        await waitForLabel(
            page,
            `jsxgraph-case-title:${menuTargetCaseId}:` +
                "Quadratic trend envelope"
        );

        if (errors.length > 0) {
            throw new Error(
                `${viewport.name} logged errors: ${errors.join(" | ")}`
            );
        }
        console.log(
            `${viewport.name}: ${expectedCaseCount} cases, navigation and ` +
                "responsive layout verified"
        );
        await page.close();
    }
} finally {
    await browser.close();
}

function assertWorkbenchLayout(viewport, labels) {
    const root = findLabel(labels, "jsxgraph-catalog:");
    const board = findLabel(labels, "jsxgraph-parity-board");
    const title = findLabel(
        labels,
        `jsxgraph-case-title:${defaultCaseId}:`
    );
    const selector = findLabel(labels, "Select case:");
    const previous = findLabel(labels, "Previous case");
    const next = findLabel(labels, "Next case");
    const previewModes = findLabel(
        labels,
        "Preview modes: Source, Official, Native"
    );

    if (!root.label.includes(`jsxgraph-catalog:${expectedCaseCount}`)) {
        throw new Error(
            `${viewport.name} does not expose ${expectedCaseCount} cases`
        );
    }
    for (const item of [
        board,
        title,
        selector,
        previous,
        next,
        previewModes
    ]) {
        assertInsideViewport(viewport, item);
    }
    if (
        board.y < title.y + title.height + 4 ||
        selector.x < previous.x + previous.width ||
        next.x < selector.x + selector.width
    ) {
        throw new Error(
            `${viewport.name} workbench controls overlap the parity board`
        );
    }
    const aspectRatio = board.width / board.height;
    if (Math.abs(aspectRatio - 1.2) > 0.01) {
        throw new Error(
            `${viewport.name} board ratio ${aspectRatio} does not match 1.2`
        );
    }
}

function assertInsideViewport(viewport, item) {
    if (
        item.width <= 0 ||
        item.height <= 0 ||
        item.x < 0 ||
        item.y < 0 ||
        item.x + item.width > viewport.width + 1 ||
        item.y + item.height > viewport.height + 1
    ) {
        throw new Error(
            `${viewport.name} has clipped ${item.label}: ` +
                JSON.stringify(item)
        );
    }
}

function findLabel(labels, fragment) {
    const match = labels.find((item) => item.label.includes(fragment));
    if (match === undefined) {
        throw new Error(`Missing aria label: ${fragment}`);
    }
    return match;
}

async function ariaLabels(page) {
    return page.evaluate(() => {
        const roots = [document];
        const labels = [];
        for (let index = 0; index < roots.length; index += 1) {
            const root = roots[index];
            root.querySelectorAll?.("[aria-label]").forEach((element) => {
                const bounds = element.getBoundingClientRect();
                labels.push({
                    label: element.getAttribute("aria-label") ?? "",
                    x: bounds.x,
                    y: bounds.y,
                    width: bounds.width,
                    height: bounds.height
                });
            });
            root.querySelectorAll?.("*").forEach((element) => {
                if (element.shadowRoot !== null) {
                    roots.push(element.shadowRoot);
                }
            });
        }
        return labels;
    });
}

async function clickLabel(page, fragment) {
    const target = findLabel(await ariaLabels(page), fragment);
    if (target.width <= 0 || target.height <= 0) {
        throw new Error(`Cannot click hidden aria label: ${fragment}`);
    }
    await page.mouse.click(
        target.x + target.width * 0.5,
        target.y + target.height * 0.5
    );
}

async function clickExactLabel(page, label) {
    await page
        .locator(`::-p-aria(${label})`)
        .setTimeout(60_000)
        .click();
}

async function waitForLabel(page, fragment) {
    await page.waitForFunction(
        (expected) => {
            const roots = [document];
            for (let index = 0; index < roots.length; index += 1) {
                const root = roots[index];
                const found = [
                    ...(root.querySelectorAll?.("[aria-label]") ?? [])
                ].some((element) =>
                    (element.getAttribute("aria-label") ?? "")
                        .includes(expected)
                );
                if (found) {
                    return true;
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
        fragment
    );
}

async function captureBoard(page, labels) {
    return captureRectangle(
        page,
        findLabel(labels, "jsxgraph-parity-board")
    );
}

async function captureRectangle(page, rectangle) {
    await new Promise((resolve) => setTimeout(resolve, 250));
    return page.screenshot({
        clip: {
            x: rectangle.x,
            y: rectangle.y,
            width: rectangle.width,
            height: rectangle.height
        }
    });
}

async function waitForQueryCase(page, expectedCaseId) {
    await page.waitForFunction(
        (caseId) =>
            new URL(globalThis.location.href)
                .searchParams
                .get("caseId") === caseId,
        {timeout: 60_000},
        expectedCaseId
    );
}

function assertQueryCase(page, expectedCaseId) {
    const actualCaseId = new URL(page.url()).searchParams.get("caseId");
    if (actualCaseId !== expectedCaseId) {
        throw new Error(
            `URL caseId is ${actualCaseId}, expected ${expectedCaseId}`
        );
    }
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
