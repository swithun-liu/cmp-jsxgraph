/*
 * Official JSXGraph 1.13.3 curve boolean clipping fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/curve-boolean-clipping.mjs
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
        const snapshot = (curve) => ({
            id: curve.id,
            elType: curve.elType,
            type: curve.type,
            elementClass: curve.elementClass,
            curveType: curve.curveType,
            numberPoints: curve.numberPoints,
            parents: [...curve.parents],
            x: curve.points.map((point) => classify(point.usrCoords[1])),
            y: curve.points.map((point) => classify(point.usrCoords[2]))
        });
        const createBoard = () => {
            const container = document.createElement("div");
            container.id = `board-${boardIndex++}`;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(container.id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const curve = (board, id, coordinates) =>
            board.create(
                "curve",
                [
                    coordinates.map((point) => point[0]),
                    coordinates.map((point) => point[1])
                ],
                {id, name: "", withLabel: false}
            );
        const outputs = (board, first, second, prefix) => {
            const result = {};
            for (const creatorName of [
                "curveintersection",
                "curveunion",
                "curvedifference"
            ]) {
                const output = board.create(
                    creatorName,
                    [first, second],
                    {
                        id: `${prefix}-${creatorName}`,
                        name: "",
                        withLabel: false
                    }
                );
                board.update();
                result[creatorName] = snapshot(output);
            }
            return result;
        };
        const staticCase = (firstCoordinates, secondCoordinates, prefix) => {
            const board = createBoard();
            const first = curve(board, `${prefix}-first`, firstCoordinates);
            const second = curve(board, `${prefix}-second`, secondCoordinates);
            return outputs(board, first, second, prefix);
        };

        const cases = {
            crossing: staticCase(
                [[-3, -2], [2, -2], [2, 2], [-3, 2]],
                [[-1, -3], [4, -3], [4, 1], [-1, 1]],
                "crossing"
            ),
            disjoint: staticCase(
                [[-5, -2], [-3, -2], [-3, 0], [-5, 0]],
                [[2, 1], [4, 1], [4, 3], [2, 3]],
                "disjoint"
            ),
            containment: staticCase(
                [[-4, -4], [4, -4], [4, 4], [-4, 4]],
                [[-1, -1], [1, -1], [1, 1], [-1, 1]],
                "containment"
            ),
            sharedEdge: staticCase(
                [[-4, -2], [0, -2], [0, 2], [-4, 2]],
                [[0, -2], [4, -2], [4, 2], [0, 2]],
                "shared-edge"
            ),
            touchingCorner: staticCase(
                [[-4, -4], [0, -4], [0, 0], [-4, 0]],
                [[0, 0], [4, 0], [4, 4], [0, 4]],
                "touching-corner"
            ),
            reverseDifference: staticCase(
                [[-1, -1], [-1, 1], [1, 1], [1, -1]],
                [[-4, -4], [4, -4], [4, 4], [-4, 4]],
                "reverse-difference"
            ),
            multiComponent: staticCase(
                [
                    [-5, -2], [-2, -2], [-2, 1], [-5, 1],
                    [NaN, NaN],
                    [1, -1], [4, -1], [4, 2], [1, 2]
                ],
                [[-3, -3], [2, -3], [2, 3], [-3, 3]],
                "multi-component"
            )
        };

        const dynamicBoard = createBoard();
        const points = [
            dynamicBoard.create(
                "point",
                [-3, -2],
                {id: "dynamic-a", name: "", withLabel: false}
            ),
            dynamicBoard.create(
                "point",
                [2, -2],
                {id: "dynamic-b", name: "", withLabel: false}
            ),
            dynamicBoard.create(
                "point",
                [2, 2],
                {id: "dynamic-c", name: "", withLabel: false}
            ),
            dynamicBoard.create(
                "point",
                [-3, 2],
                {id: "dynamic-d", name: "", withLabel: false}
            )
        ];
        const dynamicFirst = dynamicBoard.create(
            "polygon",
            points,
            {id: "dynamic-first", name: "", withLabel: false}
        );
        const dynamicSecond = curve(
            dynamicBoard,
            "dynamic-second",
            [[-1, -3], [4, -3], [4, 1], [-1, 1]]
        );
        const dynamicOutput = dynamicBoard.create(
            "curveintersection",
            [dynamicFirst, dynamicSecond],
            {id: "dynamic-output", name: "", withLabel: false}
        );
        dynamicBoard.update();
        const before = snapshot(dynamicOutput);
        points[1].setPosition(JXG.COORDS_BY_USER, [0, -2]);
        points[2].setPosition(JXG.COORDS_BY_USER, [0, 2]);
        dynamicBoard.update();
        const after = snapshot(dynamicOutput);
        const lifecycle = {
            firstHasChildBeforeRemoval:
                dynamicFirst.childElements[dynamicOutput.id] === dynamicOutput,
            secondHasChildBeforeRemoval:
                dynamicSecond.childElements[dynamicOutput.id] === dynamicOutput,
            outputParentsBeforeRemoval: [...dynamicOutput.parents]
        };
        dynamicBoard.removeObject(dynamicOutput);
        lifecycle.outputExistsAfterRemoval =
            dynamicBoard.objects[dynamicOutput.id] !== undefined;
        lifecycle.firstHasChildAfterRemoval =
            dynamicFirst.childElements[dynamicOutput.id] !== undefined;
        lifecycle.secondHasChildAfterRemoval =
            dynamicSecond.childElements[dynamicOutput.id] !== undefined;
        cases.dynamic = {before, after, lifecycle};

        const invalidBoard = createBoard();
        const invalid = {};
        for (const creatorName of [
            "curveintersection",
            "curveunion",
            "curvedifference"
        ]) {
            try {
                invalidBoard.create(creatorName, []);
                invalid[creatorName] = null;
            } catch (error) {
                invalid[creatorName] = error.message;
            }
        }

        return {
            version: JXG.version,
            cases,
            invalid
        };
    });

    process.stdout.write(`${JSON.stringify(evidence, null, 2)}\n`);
} finally {
    await browser.close();
}
