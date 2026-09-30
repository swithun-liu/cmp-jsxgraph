/*
 * Official JSXGraph 1.13.3 DataSource fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/datasource.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    process.env.CHROME_BIN ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent("<main></main>");
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const classify = (value) => {
            if (value === undefined) {
                return "undefined";
            }
            if (Number.isNaN(value)) {
                return "NaN";
            }
            return value;
        };
        const values = (array) => Array.from(array, classify);
        const matrix = (rows) => rows.map(values);
        const source = new JXG.DataSource().loadFromArray(
            [
                ["", "A", "B"],
                ["r1", "1", "1.0"],
                ["r2", "-", "2x"],
                ["r3", 3, true],
                ["short"]
            ],
            true,
            true
        );
        let missingRowError = null;
        try {
            source.getRow("missing");
        } catch (cause) {
            missingRowError = {
                name: cause?.name ?? null,
                message: cause?.message ?? null
            };
        }
        const explicit = new JXG.DataSource().loadFromArray(
            [["1", "2"], ["3", "4"]],
            ["X", "Y"],
            ["first", "second"]
        );
        const numericSpellings = new JXG.DataSource().loadFromArray([
            [
                "-0",
                "1e-7",
                "1e+21",
                "1e21",
                "01",
                "0.000001",
                "1e-6",
                "100000000000000000000"
            ]
        ]);

        return {
            version: JXG.version,
            embedded: {
                data: matrix(source.data),
                columnHeaders: values(source.columnHeaders),
                rowHeaders: values(source.rowHeaders),
                columnA: values(source.getColumn("A")),
                columnB: values(source.getColumn("B")),
                unknownColumn: values(source.getColumn("missing")),
                rowR2: values(source.getRow("r2")),
                missingRowError
            },
            explicit: {
                data: matrix(explicit.data),
                columnHeaders: values(explicit.columnHeaders),
                rowHeaders: values(explicit.rowHeaders)
            },
            numericSpellings: matrix(numericSpellings.data)
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
