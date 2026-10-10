/*
 * Official JSXGraph 1.13.3 Arc/Sector method fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/arc-sector-methods.mjs
 */
import { createRequire } from "node:module";
import { resolve } from "node:path";

const require = createRequire(
    resolve("tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const browser = await puppeteer.launch({
    headless: true,
    executablePath:
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<div id="board" style="width:640px;height:480px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const result = await page.evaluate(() => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = (coordinates) =>
            board.create("point", coordinates, {
                name: "",
                withLabel: false
            });
        const center = point([0, 0]);
        const radiusPoint = point([3, 0]);
        const anglePoint = point([0, 3]);
        const arc = board.create("arc", [
            center,
            radiusPoint,
            anglePoint
        ], {
            name: "A",
            withLabel: true,
            label: {position: "top"}
        });
        const sector = board.create("sector", [
            center,
            radiusPoint,
            anglePoint
        ], {
            name: "S",
            withLabel: true,
            label: {position: "top"}
        });
        const classify = (value) =>
            value === undefined ? "undefined" : value;
        const snapshot = (element) => ({
            radius: element.Radius(),
            legacyRadius: element.getRadius(),
            length: element.L(),
            area: classify(element.Area?.()),
            perimeter: classify(element.Perimeter?.()),
            textAnchor: Array.from(element.getTextAnchor().usrCoords),
            labelAnchor: Array.from(element.getLabelAnchor().usrCoords),
            maxX: element.maxX(),
            startHit: element.hasPoint(
                radiusPoint.coords.scrCoords[1],
                radiusPoint.coords.scrCoords[2]
            ),
            interiorHit: element.hasPoint(360, 200),
            sectorHit: element.hasPointSector(360, 200)
        });

        const initial = {
            arc: snapshot(arc),
            sector: snapshot(sector)
        };
        sector.setRadius(2);
        board.update();
        const numericRadius = snapshot(sector);
        sector.setRadius(() => 4);
        board.update();
        const functionRadius = snapshot(sector);
        sector.setRadius("auto");
        board.update();
        const autoRadius = snapshot(sector);
        return {
            unitX: board.unitX,
            unitY: board.unitY,
            initial,
            numericRadius,
            functionRadius,
            autoRadius
        };
    });
    console.log(JSON.stringify(result, null, 2));
} finally {
    await browser.close();
}
