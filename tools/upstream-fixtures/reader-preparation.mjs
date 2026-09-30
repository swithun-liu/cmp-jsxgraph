/*
 * Official JSXGraph 1.13.3 reader input-preparation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-preparation.mjs
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

const archives = {
    geonext:
        "UEsDBBQAAAAIAAAAAAAAAAAAPgAAAFEAAAAKAAAAc291cmNlLmd4dLNx" +
        "d/X3c40IsbNJzs8rSc0rsXNUUFNwUrBJLEq2y8ksSS1KzLHRh8nZ" +
        "FORngqjMFLsAG30gaaMPFdGHmQMAUEsFBg==",
    intergeo:
        "UEsDBBQAAAAIAAAAAAAAAAAAHwAAACgAAAAZAAAAY29uc3RydWN0aW9u" +
        "L2ludGVyZ2VvLnhtbLNJzs8rLikqTS7JzM+zs0nNSc1NzSsp1rez0U" +
        "eRAQBQSwUG",
    geogebra:
        "UEsDBBQAAAAIAAAAAAAAAAAAGgAAAB8AAAAMAAAAZ2VvZ2VicmEueG1s" +
        "s0lPzU9PTSpKtDvfcGjTo44VjzqX2OjDBQFQSwUG",
    cinderella:
        "UEsDBBQAAAAIAAAAAAAAAAAADwAAAA0AAAAQAAAAY29uc3RydWN0aW9u" +
        "LmNkeXPOzEupVEjLrCgpLUoFAFBLBQY="
};

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
    for (const reader of [
        "geonext.js",
        "intergeo.js",
        "geogebra.js",
        "cinderella.js"
    ]) {
        await page.addScriptTag({
            path: resolve(
                repositoryRoot,
                `third_party/jsxgraph-src/src/reader/${reader}`
            )
        });
    }

    const evidence = await page.evaluate((encodedArchives) => {
        const geonext = Object.create(JXG.GeonextReader.prototype);
        const intergeo = Object.create(JXG.IntergeoReader.prototype);
        const geogebra = Object.create(JXG.GeogebraReader.prototype);
        const cinderella = Object.create(JXG.CinderellaReader.prototype);
        const rawGeonext =
            "<GEONEXT><content>A & B <arc>literal</content>" +
            "<point><id>P</id></point></GEONEXT>";

        return {
            version: JXG.version,
            geonext: {
                raw: geonext.prepareString(rawGeonext),
                archive: geonext.prepareString(encodedArchives.geonext)
            },
            intergeo: {
                raw: intergeo.prepareString(
                    "<construction><elements/></construction>"
                ),
                base64Xml: intergeo.prepareString(
                    btoa("<construction><elements/></construction>")
                ),
                archive: intergeo.prepareString(encodedArchives.intergeo)
            },
            geogebra: {
                symbols: geogebra.utf8replace("π²³≟≠≤≥∧∨"),
                archive: geogebra.prepareString(encodedArchives.geogebra)
            },
            cinderella: {
                raw: cinderella.prepareString("<cindyscript/>"),
                archive: cinderella.prepareString(
                    encodedArchives.cinderella,
                    true
                )
            }
        };
    }, archives);
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
