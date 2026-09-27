/*
 * Official JSXGraph 1.13.3 Tapemeasure behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/tapemeasure.mjs
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
    await page.setContent('<main id="fixtures"></main>');
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        let boardIndex = 0;

        const createBoard = () => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors),
            descendants: Object.keys(element.descendants)
        });
        const pointSnapshot = (point) => ({
            id: point.id,
            coordinates: point.coords.usrCoords.slice(),
            visible: point.evalVisProp("visible"),
            fixed: point.evalVisProp("fixed"),
            withLabel: point.evalVisProp("withlabel"),
            size: point.evalVisProp("size"),
            strokeColor: point.evalVisProp("strokecolor"),
            fillColor: point.evalVisProp("fillcolor"),
            fillOpacity: point.evalVisProp("fillopacity"),
            dump: point.dump,
            relations: relations(point)
        });
        const snapshot = (tape) => ({
            id: tape.id,
            elType: tape.elType,
            type: tape.type,
            value: tape.Value(),
            straightFirst: tape.evalVisProp("straightfirst"),
            straightLast: tape.evalVisProp("straightlast"),
            strokeColor: tape.evalVisProp("strokecolor"),
            strokeWidth: tape.evalVisProp("strokewidth"),
            point1: pointSnapshot(tape.point1),
            point2: pointSnapshot(tape.point2),
            label: tape.label
                ? {
                    id: tape.label.id,
                    plaintext: tape.label.plaintext,
                    coordinates: tape.label.coords.usrCoords.slice(),
                    digits: tape.label.evalVisProp("digits"),
                    position: tape.label.evalVisProp("position"),
                    offset: tape.label.evalVisProp("offset"),
                    dump: tape.label.dump,
                    relations: relations(tape.label)
                }
                : null,
            ticks: tape.ticks?.[0]
                ? {
                    id: tape.ticks[0].id,
                    drawLabels: tape.ticks[0].evalVisProp("drawlabels"),
                    drawZero: tape.ticks[0].evalVisProp("drawzero"),
                    insertTicks: tape.ticks[0].evalVisProp("insertticks"),
                    ticksDistance:
                        tape.ticks[0].evalVisProp("ticksdistance"),
                    minorHeight:
                        tape.ticks[0].evalVisProp("minorheight"),
                    majorHeight:
                        tape.ticks[0].evalVisProp("majorheight"),
                    minorTicks:
                        tape.ticks[0].evalVisProp("minorticks"),
                    tickEndings:
                        tape.ticks[0].evalVisProp("tickendings"),
                    labelOffset:
                        tape.ticks[0].visProp.label.offset,
                    labelAnchorX:
                        tape.ticks[0].visProp.label.anchorx,
                    labelAnchorY:
                        tape.ticks[0].visProp.label.anchory,
                    dump: tape.ticks[0].dump
                }
                : null,
            parents: tape.getParents(),
            inherits: tape.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(tape.subs).map(([key, element]) => [
                    key,
                    element.id
                ])
            ),
            relations: relations(tape),
            objectOrder: tape.board.objectsList.map((element) => element.id)
        });

        const board = createBoard();
        const tape = board.create(
            "tapemeasure",
            [[1, 2], [4, 6]],
            {
                id: "tape",
                name: "dist",
                digits: 4,
                precision: 5,
                point1: {id: "start"},
                point2: {id: "end"},
                label: {id: "label", digits: 3},
                ticks: {id: "ticks"}
            }
        );
        board.update();
        const initial = snapshot(tape);
        tape.point2.setPosition(JXG.COORDS_BY_USER, [7, 10]);
        board.update(tape.point2);
        const moved = snapshot(tape);

        const plainBoard = createBoard();
        const plain = plainBoard.create(
            "tapemeasure",
            [[-2, 1], [2, 1]],
            {
                id: "plain",
                name: "",
                digits: 4,
                precision: 5,
                withLabel: true,
                withTicks: false,
                point1: {id: "plainStart"},
                point2: {id: "plainEnd"},
                label: {id: "plainLabel"}
            }
        );
        plainBoard.update();
        const rootDigitsIgnored = snapshot(plain);

        const removalBefore = snapshot(tape);
        board.removeObject(tape);
        const removalAfter = board.objectsList.map((element) => element.id);

        return {
            version: JXG.version,
            initial,
            moved,
            rootDigitsIgnored,
            removal: {
                before: removalBefore,
                after: removalAfter
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
