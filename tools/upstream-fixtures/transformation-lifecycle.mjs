/*
 * Official JSXGraph 1.13.3 dynamic 2D transformation lifecycle fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/transformation-lifecycle.mjs
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
            boundingbox: [-10, 8, 10, -8],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = (coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: id,
                withLabel: false
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
        const vector = (values) => values.map(classify);
        const matrix = (transform) => transform.matrix.map(vector);
        const pointState = (element) => ({
            coords: vector(element.coords.usrCoords),
            initialCoords: vector(element.initialCoords.usrCoords),
            actualCoords: vector(element.actualCoords.usrCoords),
            baseElement:
                element.baseElement === null ? null : element.baseElement.id,
            transformations: element.transformations.map(
                (transform) => transform.transformationType
            ),
            draggable: element.isDraggable,
            parents: [...element.parents]
        });

        const driver = point([2, 3], "driver");
        const center = point([1, -1], "center");
        const reflectionP = point([-2, 1], "reflectionP");
        const reflectionQ = point([2, 3], "reflectionQ");
        const reflectionLine = board.create(
            "line",
            [reflectionP, reflectionQ],
            {id: "reflectionLine", name: "reflectionLine", withLabel: false}
        );
        const base = point([3, 2], "base");

        const dynamicTranslate = board.create(
            "transform",
            [
                function () {
                    return driver.X();
                },
                "Y(driver)"
            ],
            {type: "translate"}
        );
        const dynamicRotate = board.create(
            "transform",
            ["Y(driver)", center],
            {type: "rotate"}
        );
        const dynamicReflection = board.create(
            "transform",
            [reflectionLine],
            {type: "reflect"}
        );
        const dynamicMatrix = board.create(
            "transform",
            [
                [
                    [1, 0, 0],
                    [
                        function () {
                            return driver.X();
                        },
                        1,
                        0
                    ],
                    [0, 0, 1]
                ]
            ],
            {type: "matrix"}
        );

        const transformed = point(
            [base, [dynamicTranslate, dynamicRotate]],
            "transformed"
        );
        const reflected = point(
            [base, dynamicReflection],
            "reflected"
        );
        const matrixPoint = point([base, dynamicMatrix], "matrixPoint");

        const bound = point([1, 1], "bound");
        const boundTranslate = board.create(
            "transform",
            [2, -1],
            {type: "translate"}
        );
        boundTranslate.bindTo(bound);

        const meltFirst = point([1, 2], "meltFirst");
        const meltSecond = point([-1, 4], "meltSecond");
        const meltTranslate = board.create(
            "transform",
            [3, -2],
            {type: "translate"}
        );
        const meltScale = board.create(
            "transform",
            [2, 4],
            {type: "scale"}
        );
        meltTranslate.meltTo([meltFirst, meltSecond]);
        const independentClones =
            meltFirst.transformations[0] !== meltSecond.transformations[0] &&
            meltFirst.transformations[0] !== meltTranslate;
        meltScale.meltTo([meltFirst, meltSecond]);

        const removable = point([2, -2], "removable");
        const removeTranslate = board.create(
            "transform",
            [1, 2],
            {type: "translate"}
        );
        const removeScale = board.create(
            "transform",
            [3, 4],
            {type: "scale"}
        );
        removable.addTransform(removable, [
            removeTranslate,
            removeScale,
            removeTranslate
        ]);
        const removeInitial = pointState(removable);
        removable.removeTransform(removeTranslate);
        const removeOne = pointState(removable);
        removable.clearTransforms();
        const removeAll = pointState(removable);

        const snapshot = () => {
            for (const transform of [
                dynamicTranslate,
                dynamicRotate,
                dynamicReflection,
                dynamicMatrix
            ]) {
                transform.update();
            }
            return {
                transforms: {
                    translate: {
                        matrix: matrix(dynamicTranslate),
                        isNumericMatrix: dynamicTranslate.isNumericMatrix,
                        cloneIsNull: dynamicTranslate.clone() === null
                    },
                    rotate: matrix(dynamicRotate),
                    reflect: matrix(dynamicReflection),
                    matrix: matrix(dynamicMatrix)
                },
                points: {
                    base: pointState(base),
                    transformed: pointState(transformed),
                    reflected: pointState(reflected),
                    matrixPoint: pointState(matrixPoint),
                    bound: pointState(bound),
                    meltFirst: pointState(meltFirst),
                    meltSecond: pointState(meltSecond)
                },
                melt: {
                    independentClones,
                    firstMatrix: matrix(meltFirst.transformations[0]),
                    secondMatrix: matrix(meltSecond.transformations[0]),
                    transformationCount: [
                        meltFirst.transformations.length,
                        meltSecond.transformations.length
                    ]
                }
            };
        };

        board.update();
        const initial = snapshot();

        driver.setPositionDirectly(JXG.COORDS_BY_USER, [-1, 0.5]);
        center.setPositionDirectly(JXG.COORDS_BY_USER, [2, 2]);
        reflectionQ.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [reflectionP.X(), 5]
        );
        base.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 4]);
        board.update();
        const moved = snapshot();

        bound.setPositionDirectly(JXG.COORDS_BY_USER, [10, 5]);
        const boundAfterDirectMove = pointState(bound);
        board.update();
        const boundAfterUpdate = pointState(bound);

        return {
            version: JXG.version,
            initial,
            moved,
            boundAfterDirectMove,
            boundAfterUpdate,
            removal: {
                initial: removeInitial,
                afterOneRemoval: removeOne,
                afterClear: removeAll
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
