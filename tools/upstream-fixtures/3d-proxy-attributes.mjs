/*
 * Official JSXGraph 1.13.3 3D proxy attribute and label ownership fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/3d-proxy-attributes.mjs
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
    await page.setContent(
        '<div id="board" style="width:640px;height:480px"></div>'
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
            boundingbox: [-8, 8, 8, -8],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const view = board.create(
            "view3d",
            [
                [-5, -4],
                [8, 7],
                [[-5, 5], [-4, 6], [-3, 7]]
            ],
            {
                id: "view",
                name: "",
                projection: "parallel",
                axesPosition: "none",
                depthOrder: {enabled: false},
                az: {slider: {visible: false, start: 1}},
                el: {slider: {visible: false, start: 0.3}},
                bank: {slider: {visible: false, start: 0}}
            }
        );
        const snapshot = (element) => ({
            id: element.id,
            name: element.name,
            ownLabel: element.label?.id ?? null,
            proxy: {
                id: element.element2D.id,
                name: element.element2D.name,
                strokeColor: element.element2D.getAttribute("strokeColor"),
                strokeWidth: element.element2D.getAttribute("strokeWidth"),
                size: element.element2D.getAttribute("size"),
                fixed: element.element2D.getAttribute("fixed"),
                withLabel: element.element2D.getAttribute("withLabel"),
                label: element.element2D.label?.id ?? null,
                labelAnchor:
                    element.element2D.label?.getAttribute("anchor")?.id ?? null,
                element3d:
                    element.element2D.getAttribute("element3d")?.id ?? null
            },
            inherits: element.inherits.map((child) => child.id)
        });

        const point = view.create("point3d", [1, 2, 3], {
            id: "point",
            name: "P",
            strokeColor: "#123456",
            size: (element) => element.name === "P" ? 7 : 1,
            fixed: true,
            withLabel: true,
            label: {offset: [4, 5]}
        });
        const line = view.create(
            "line3d",
            [[-2, 0, 0], [2, 1, 0]],
            {
                id: "line",
                name: "L",
                strokeColor: "#654321",
                strokeWidth: 4,
                withLabel: true,
                point1: {
                    id: "nested-a",
                    name: "A",
                    visible: true,
                    size: 8,
                    withLabel: true
                },
                point2: {
                    id: "nested-b",
                    name: "",
                    visible: false,
                    size: 9
                }
            }
        );
        const text = view.create(
            "text3d",
            [[0, 1, 2], "hello"],
            {
                id: "text",
                name: "T",
                strokeColor: "#abcdef",
                fontSize: 19,
                fixed: true,
                withLabel: true
            }
        );
        board.fullUpdate();
        const created = {
            point: snapshot(point),
            line: snapshot(line),
            text: snapshot(text),
            nested: [
                snapshot(line.point1),
                snapshot(line.point2)
            ]
        };

        point.setAttribute({
            strokeColor: "#fedcba",
            size: 11,
            withLabel: false
        });
        line.setAttribute({
            strokeWidth: 6,
            withLabel: false
        });
        text.setAttribute({
            fontSize: 23,
            withLabel: false
        });
        board.fullUpdate();

        return {
            created,
            updated: {
                point: snapshot(point),
                line: snapshot(line),
                text: snapshot(text)
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
