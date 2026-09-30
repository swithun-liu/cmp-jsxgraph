/*
 * Official JSXGraph 1.13.3 color utility fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/color.mjs
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
    const evidence = await page.evaluate(() => ({
        version: JXG.version,
        parser: {
            named: JXG.rgbParser("CornflowerBlue"),
            spacedNamed: JXG.rgbParser(" light goldenrod yellow "),
            rgb: JXG.rgbParser("rgb(12, 132, 233)"),
            rgba: JXG.rgbParser("rgba(123, 234, 45, 0.5)"),
            shortHex: JXG.rgbParser("#fb0"),
            longHex: JXG.rgbParser("336699"),
            alphaHex: JXG.rgbParser("#112233aa"),
            clampedRgb: JXG.rgbParser("rgb(999, 2, 3)"),
            normalizedArray: JXG.rgbParser([0.5, 1, 0]),
            rawArray: JXG.rgbParser([0.5, 2, 0]),
            channels: JXG.rgbParser(12, 34, 56),
            invalid: JXG.rgbParser("not-a-color")
        },
        formatting: {
            css: JXG.rgb2css("#336699"),
            hex: JXG.rgb2hex("rgb(51, 102, 153)"),
            deprecatedHexToRgb: JXG.hex2rgb("#369")
        },
        colorSpaces: {
            hsvToHsl: JXG.hsv2hsl(210, 2 / 3, 0.6),
            hsvToRgb: JXG.hsv2rgb(210, 2 / 3, 0.6),
            wrappedHsvToRgb: JXG.hsv2rgb(-30, 1, 1),
            achromaticHsvToRgb: JXG.hsv2rgb(0, 0, 0.5),
            nonzeroHueAchromatic: JXG.hsv2rgb(10, 0, 0.5),
            rgbToHsv: JXG.rgb2hsv("#336699"),
            rgbToLms: JXG.rgb2LMS("#336699"),
            lmsRoundTrip: (() => {
                const lms = JXG.rgb2LMS("#336699");
                return JXG.LMS2rgb(lms[0], lms[1], lms[2]);
            })()
        },
        opacity: {
            split: JXG.rgba2rgbo("#11223380"),
            splitPlain: JXG.rgba2rgbo("#112233"),
            join: JXG.rgbo2rgba("#112233", 0.5),
            joinNone: JXG.rgbo2rgba("none", 0.5),
            joinTransparent: JXG.rgbo2rgba("transparent", 0.5)
        },
        transforms: {
            blackAndWhite: JXG.rgb2bw("#336699"),
            blackAndWhiteNone: JXG.rgb2bw("none"),
            protanopia: JXG.rgb2cb("#336699", "protanopia"),
            deuteranopia: JXG.rgb2cb("#336699", "deuteranopia"),
            tritanopia: JXG.rgb2cb("#336699", "tritanopia"),
            shade: JXG.shadeColor("#336699", 0.2),
            lighten: JXG.lightenColor("#336699", 0.2),
            darken: JXG.darkenColor("#336699", 0.2),
            mixDefault: JXG.mixColor("#ff0000", "#0000ff"),
            mixQuarter: JXG.mixColor("#ff0000", "#0000ff", 0.25),
            autoHighlightOpaque: JXG.autoHighlight("#112233"),
            autoHighlightTransparent: JXG.autoHighlight("#11223320"),
            autoHighlightNamed: JXG.autoHighlight("red"),
            contrastLight: JXG.contrast("#ffffff"),
            contrastDark: JXG.contrast("#000000"),
            contrastWeightedChannels: JXG.contrast("#0088cc"),
            contrastEmptyDefaults: JXG.contrast("#ffffff", "", ""),
            contrastCustom: JXG.contrast(
                "#808080",
                "#111111",
                "#eeeeee",
                2
            )
        },
        paletteWong: JXG.paletteWong
    }));
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
