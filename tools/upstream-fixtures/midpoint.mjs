/*
 * Official JSXGraph 1.13.3 Midpoint behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/midpoint.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const visualParityPackage = resolve(
    repositoryRoot,
    "tools/visual-parity/package.json"
);
const require = createRequire(visualParityPackage);
const puppeteer = require("puppeteer");
const executablePath =
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<div id="board" style="width: 640px; height: 480px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            return value;
        };
        const coordinates = (point) =>
            point.coords.usrCoords.map(classify);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors)
        });

        const first = board.create("point", [-4, 2], {
            id: "first",
            name: "First",
            withLabel: false
        });
        const second = board.create("point", [2, -2], {
            id: "second",
            name: "Second",
            withLabel: false
        });
        const existing = board.create("midpoint", [first, second], {
            id: "existing",
            name: "Existing",
            withLabel: false
        });

        const lineFirst = board.create("point", [-6, -4], {
            id: "lineFirst",
            name: "",
            withLabel: false
        });
        const lineSecond = board.create("point", [2, -4], {
            id: "lineSecond",
            name: "",
            withLabel: false
        });
        const line = board.create(
            "segment",
            [lineFirst, lineSecond],
            {id: "line", name: "Line", withLabel: false}
        );
        const lineMidpoint = board.create("midpoint", [line], {
            id: "lineMidpoint",
            name: "Line midpoint",
            withLabel: false
        });

        const helper = board.create(
            "midpoint",
            [[-2, 4], [4, 2]],
            {name: "", withLabel: false}
        );
        const helperPoints = helper.parents.map((id) => board.objects[id]);
        const initial = {
            existing: coordinates(existing),
            line: coordinates(lineMidpoint),
            helper: coordinates(helper),
            existingRelations: relations(existing),
            existingParentChildren: [
                Object.keys(first.childElements),
                Object.keys(second.childElements)
            ],
            helperRelations: relations(helper),
            helperPointRelations: helperPoints.map(relations),
            objectCount: board.objectsList.length
        };

        first.setPositionDirectly(JXG.COORDS_BY_USER, [0, 4]);
        line.point1.setPositionDirectly(JXG.COORDS_BY_USER, [-2, -2]);
        board.update();
        const moved = {
            existing: coordinates(existing),
            line: coordinates(lineMidpoint),
            helper: coordinates(helper)
        };

        const ideal = board.create("point", [0, 2, 3], {
            id: "ideal",
            name: "Ideal",
            withLabel: false
        });
        const finite = board.create("point", [2, 4], {
            id: "finite",
            name: "Finite",
            withLabel: false
        });
        const idealMidpoint = board.create("midpoint", [ideal, finite], {
            id: "idealMidpoint",
            withLabel: false
        });
        const nanX = board.create("point", [Number.NaN, 6], {
            id: "nanX",
            withLabel: false
        });
        const partialNanMidpoint = board.create(
            "midpoint",
            [nanX, finite],
            {id: "partialNanMidpoint", withLabel: false}
        );
        const nonfinite = {
            ideal: coordinates(ideal),
            idealMidpoint: coordinates(idealMidpoint),
            nanX: coordinates(nanX),
            partialNanMidpoint: coordinates(partialNanMidpoint)
        };

        const existingIds = [first.id, second.id, existing.id];
        board.removeObject(existing);
        const existingRemoval = Object.fromEntries(
            existingIds.map((id) => [id, Boolean(board.objects[id])])
        );

        const helperIds = [helper.id, ...helperPoints.map((point) => point.id)];
        board.removeObject(helper);
        const helperRemoval = Object.fromEntries(
            helperIds.map((id) => [id, Boolean(board.objects[id])])
        );

        return {
            version: JXG.version,
            initial,
            moved,
            nonfinite,
            existingRemoval,
            helperRemoval
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
