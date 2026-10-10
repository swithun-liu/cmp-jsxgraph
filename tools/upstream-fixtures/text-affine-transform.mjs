/*
 * Official JSXGraph 1.13.3 text affine-transform fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/text-affine-transform.mjs
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
            boundingbox: [-8, 6, 8, -6],
            keepaspectratio: false,
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const driver = board.create("point", [1.5, -0.5], {
            id: "driver",
            name: "",
            visible: false,
            withLabel: false
        });
        const createTransforms = () => [
            board.create(
                "transform",
                [
                    function () {
                        return driver.X();
                    },
                    function () {
                        return driver.Y();
                    }
                ],
                {type: "translate"}
            ),
            board.create("transform", [0.2, -0.1], {type: "shear"}),
            board.create("transform", [1.25, 0.75], {type: "scale"}),
            board.create("transform", [Math.PI / 6], {type: "rotate"})
        ];
        const internal = board.create("text", [2, 1, "internal"], {
            id: "internal",
            name: "",
            display: "internal",
            anchorX: "middle",
            anchorY: "middle",
            fontSize: 18
        });
        const html = board.create("text", [-2, -1, "html"], {
            id: "html",
            name: "",
            display: "html",
            anchorX: "right",
            anchorY: "top",
            fontSize: 18
        });
        const rotated = board.create("text", [0, 3, "rotated"], {
            id: "rotated",
            name: "",
            display: "internal",
            anchorX: "left",
            anchorY: "bottom",
            rotate: 35,
            fontSize: 18
        });
        internal.addTransform(internal, createTransforms());
        html.addTransform(html, createTransforms());

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
        const vector = (values) => Array.from(values, classify);
        const matrix = (values) => values.map(vector);
        const six = (values) => [
            values[1][1],
            values[2][1],
            values[1][2],
            values[2][2],
            values[1][0],
            values[2][0]
        ].map(classify);
        const textState = (text) => {
            const joined = board.renderer.joinTransforms(
                text,
                text.transformations
            );
            const localInside = [
                1,
                text.coords.scrCoords[1],
                text.coords.scrCoords[2]
            ];
            const transformedInside = JXG.Math.matVecMult(
                joined,
                localInside
            );
            const localOutside = [
                1,
                text.coords.scrCoords[1] + text.size[0] + 80,
                text.coords.scrCoords[2] + text.size[1] + 80
            ];
            const transformedOutside = JXG.Math.matVecMult(
                joined,
                localOutside
            );
            return {
                coords: {
                    user: vector(text.coords.usrCoords),
                    screen: vector(text.coords.scrCoords),
                    initial: vector(text.initialCoords.usrCoords),
                    actual: vector(text.actualCoords.usrCoords)
                },
                size: vector(text.size),
                transformations: text.transformations.map((transform) => ({
                    type: transform.transformationType,
                    matrix: matrix(transform.matrix)
                })),
                joined: matrix(joined),
                canvasSvgHtmlSix: six(joined),
                node: {
                    transformAttribute:
                        text.rendNode.getAttribute("transform"),
                    styleTransform: text.rendNode.style.transform,
                    styleTransformOrigin:
                        text.rendNode.style.transformOrigin
                },
                hit: {
                    transformedInside: vector(transformedInside),
                    inside: text.hasPoint(
                        transformedInside[1],
                        transformedInside[2]
                    ),
                    transformedOutside: vector(transformedOutside),
                    outside: text.hasPoint(
                        transformedOutside[1],
                        transformedOutside[2]
                    )
                }
            };
        };
        const snapshot = () => {
            board.update();
            return {
                board: {
                    origin: vector(board.origin.scrCoords),
                    unitX: classify(board.unitX),
                    unitY: classify(board.unitY)
                },
                internal: textState(internal),
                html: textState(html),
                rotated: textState(rotated)
            };
        };

        const initial = snapshot();
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [-0.75, 1.25]);
        const moved = snapshot();
        return {
            version: JXG.version,
            initial,
            moved
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
