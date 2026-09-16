import {
    existsSync,
    mkdirSync,
    readFileSync,
    writeFileSync
} from "node:fs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";
import puppeteer from "puppeteer";
import {
    repositoryRoot,
    selectedParityCaseIds
} from "./corpus.mjs";

const inputDirectory = resolve(
    repositoryRoot,
    process.env.INPUT_DIR ?? "captures/local/web-parity/current"
);
const outputDirectory = resolve(
    repositoryRoot,
    process.env.OUTPUT_DIR ?? `${inputDirectory}/evidence`
);
const pageSize = readPositiveInteger("CONTACTS_PER_PAGE", 8);
const caseIds = selectedParityCaseIds();
const report = JSON.parse(
    readFileSync(resolve(inputDirectory, "report.json"), "utf8")
);
const resultsById = new Map(
    report.cases.map((result) => [result.caseId, result])
);

mkdirSync(outputDirectory, {recursive: true});
const browser = await puppeteer.launch(browserLaunchOptions());
try {
    const page = await browser.newPage();
    await page.setViewport({
        width: 1280,
        height: 1000,
        deviceScaleFactor: 1
    });
    const pageCount = Math.ceil(caseIds.length / pageSize);
    for (let pageIndex = 0; pageIndex < pageCount; pageIndex += 1) {
        const pageCaseIds = caseIds.slice(
            pageIndex * pageSize,
            (pageIndex + 1) * pageSize
        );
        const htmlPath = resolve(
            outputDirectory,
            `contact-page-${pageIndex + 1}.html`
        );
        writeFileSync(
            htmlPath,
            renderHtml(pageCaseIds, pageIndex, pageCount)
        );
        await page.goto(pathToFileURL(htmlPath).href, {
            waitUntil: "networkidle0"
        });
        await page.evaluate(async () => {
            await Promise.all(
                [...document.images].map((image) => image.decode())
            );
        });
        await page.screenshot({
            path: resolve(
                outputDirectory,
                `contact-page-${pageIndex + 1}.png`
            ),
            fullPage: true,
            omitBackground: false
        });
    }
} finally {
    await browser.close();
}

function renderHtml(pageCaseIds, pageIndex, pageCount) {
    const items = pageCaseIds.map((caseId) => {
        const result = resultsById.get(caseId);
        if (result === undefined) {
            throw new Error(`Missing audit result for ${caseId}`);
        }
        const imageUrl = pathToFileURL(
            resolve(inputDirectory, `${caseId}_contact-sheet.png`)
        ).href;
        return `
            <figure>
                <figcaption>
                    <strong>${escapeHtml(caseId)}</strong>
                    <span>SSIM ${result.ssim.toFixed(6)}</span>
                </figcaption>
                <img src="${imageUrl}" alt="${escapeHtml(caseId)}">
            </figure>`;
    }).join("\n");
    return `<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <style>
        * { box-sizing: border-box; }
        body {
            margin: 0;
            padding: 28px;
            color: #17211c;
            background: #f4f6f5;
            font: 14px Arial, sans-serif;
        }
        header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 18px;
        }
        h1 {
            margin: 0;
            font-size: 22px;
            letter-spacing: 0;
        }
        header span { color: #66716b; }
        main {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 14px;
        }
        figure {
            margin: 0;
            padding: 10px;
            overflow: hidden;
            border: 1px solid #dce2de;
            border-radius: 6px;
            background: #ffffff;
        }
        figcaption {
            display: flex;
            justify-content: space-between;
            gap: 12px;
            margin-bottom: 8px;
        }
        figcaption span { color: #66716b; }
        img {
            display: block;
            width: 100%;
            height: auto;
            border: 1px solid #e5e9e7;
        }
    </style>
</head>
<body>
    <header>
        <h1>CMP JSXGraph production parity</h1>
        <span>Page ${pageIndex + 1} / ${pageCount}</span>
    </header>
    <main>${items}</main>
</body>
</html>
`;
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

function readPositiveInteger(name, fallback) {
    const value = Number(process.env[name] ?? fallback);
    if (!Number.isInteger(value) || value <= 0) {
        throw new Error(`${name} must be a positive integer`);
    }
    return value;
}

function escapeHtml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;");
}
