/*
 * Official JSXGraph 1.13.3 3D transformation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/transformation-3d.mjs
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
            boundingbox: [-8, 8, 8, -8],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const bound = [-5, 5];
        const view = board.create(
            "view3d",
            [[-5, -5], [8, 8], [bound, bound, bound]],
            {
                projection: "parallel",
                depthOrder: {enabled: false},
                xPlaneRear: {visible: false},
                yPlaneRear: {visible: false}
            }
        );
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
        const vector = (values) => values.map(classify);
        const matrix = (transform) => transform.matrix.map(vector);
        const applied = (transform) =>
            vector(transform.apply({coords: [1, 2, 3, 4]}));
        const snapshot = (transform) => {
            try {
                transform.update();
                const matrixAfterUpdate = matrix(transform);
                const transformed = applied(transform);
                return {
                    transformationType: transform.transformationType,
                    is3D: transform.is3D,
                    isNumericMatrix: transform.isNumericMatrix,
                    matrixAfterUpdate,
                    matrixAfterApply: matrix(transform),
                    applied: transformed
                };
            } catch (error) {
                return {
                    transformationType: transform.transformationType,
                    is3D: transform.is3D,
                    isNumericMatrix: transform.isNumericMatrix,
                    matrix: matrix(transform),
                    updateError: `${error.name}: ${error.message}`
                };
            }
        };
        const create = (type, parents) =>
            view.create("transform3d", parents, {type});

        let dynamicOffset = 2;
        let dynamicAngle = Math.PI / 4;
        let dynamicNormal = [0, 0, 2];
        let dynamicCenter = [1, -1, 2];
        const center = view.create("point3d", [1, -1, 2], {
            id: "center3d",
            name: "",
            withLabel: false
        });
        const transforms = {
            translate: create("translate", [2, -3, 4]),
            dynamicTranslate: create(
                "translate",
                [() => dynamicOffset, 1, -2]
            ),
            scale3: create("scale", [2, 3, 4]),
            scale4: create("scale", [2, 3, 4, 99]),
            rotateX: create("rotateX", [Math.PI / 2]),
            rotateY: create("rotateY", [Math.PI / 2]),
            rotateZ: create("rotateZ", [Math.PI / 2]),
            rotateAxis: create(
                "rotate",
                [Math.PI / 3, [1, 2, 3]]
            ),
            rotateExtraIgnored: create(
                "rotate",
                [
                    Math.PI / 2,
                    [0, 0, 1],
                    [10, 20, 30],
                    [99, 99, 99]
                ]
            ),
            rotateZExtraIgnored: create(
                "rotateZ",
                [Math.PI / 2, [10, 20, 30], [99, 99, 99]]
            ),
            rotateAroundArray: create(
                "rotate",
                [Math.PI / 3, [1, 2, 3], [2, -1, 4]]
            ),
            rotateAroundPoint: create(
                "rotate",
                [Math.PI / 3, [1, 2, 3], center]
            ),
            dynamicRotate: create(
                "rotate",
                [
                    () => dynamicAngle,
                    () => dynamicNormal,
                    () => dynamicCenter
                ]
            ),
            homogeneousNormal: create(
                "rotate",
                [Math.PI / 2, [1, 0, 0, 1]]
            ),
            zeroNormal: create(
                "rotate",
                [Math.PI / 2, [0, 0, 0]]
            ),
            affine: create(
                "affine",
                [1, 2, 3, 4, 5, 6, 7, 8, 9]
            ),
            affineMatrix: create(
                "affinematrix",
                [[[1, 2, 3], [4, 5, 6], [7, 8, 9]]]
            ),
            generic: create(
                "generic",
                [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]
            ),
            matrix: create(
                "matrix",
                [
                    [
                        [1, 2, 3, 4],
                        [5, 6, 7, 8],
                        [9, 10, 11, 12],
                        [13, 14, 15, 16]
                    ]
                ]
            ),
            dynamicMatrix: create(
                "matrix",
                [
                    [
                        [1, 0, 0, 0],
                        [() => dynamicOffset, 1, 0, 0],
                        [0, 0, 1, 0],
                        [0, 0, 0, 1]
                    ]
                ]
            )
        };
        const takeSnapshot = () =>
            Object.fromEntries(
                Object.entries(transforms).map(([name, transform]) => [
                    name,
                    snapshot(transform)
                ])
            );

        const initial = takeSnapshot();
        dynamicOffset = -5;
        dynamicAngle = Math.PI / 2;
        dynamicNormal = [0, 1, 0];
        dynamicCenter = [-2, 3, 1];
        center.coords = [1, -2, 3, 1];
        const moved = takeSnapshot();

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
