/*
 * Official JSXGraph 1.13.3 two-dimensional Image behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/image.mjs
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
    process.env.CHROME_BIN ||
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<div id="board" style="width: 500px; height: 500px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(async () => {
        const imageCanvas = document.createElement("canvas");
        imageCanvas.width = 4;
        imageCanvas.height = 4;
        const context = imageCanvas.getContext("2d");
        context.fillStyle = "#D92626";
        context.fillRect(0, 0, 2, 2);
        context.fillStyle = "#1A73E8";
        context.fillRect(2, 0, 2, 2);
        context.fillStyle = "#18A05E";
        context.fillRect(0, 2, 2, 2);
        context.fillStyle = "#F2C037";
        context.fillRect(2, 2, 2, 2);
        const url = imageCanvas.toDataURL("image/png");
        const boardOptions = {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            grid: false,
            resize: {enabled: false},
            showCopyright: false,
            showNavigation: false
        };
        const round = (value) => (
            Number.isFinite(value)
                ? Math.round(value * 1000000) / 1000000
                : String(value)
        );
        const vector = (values) => values.map(round);
        const snapshot = (image) => ({
            id: image.id,
            name: image.name,
            type: image.type,
            elementClass: image.elementClass,
            elType: image.elType,
            dump: image.dump,
            needsRegularUpdate: image.needsRegularUpdate,
            isDraggable: image.isDraggable,
            baseElementIsSelf: image.baseElement === image,
            coords: vector(image.coords.usrCoords),
            actualCoords: vector(image.actualCoords.usrCoords),
            usrSize: vector(image.usrSize),
            size: vector(image.size),
            span: image.span.map(vector),
            width: round(image.W()),
            height: round(image.H()),
            transformations: image.transformations.length,
            parents: [...image.parents],
            attributes: {
                fillOpacity: image.evalVisProp("fillopacity"),
                layer: image.evalVisProp("layer"),
                rotate: image.evalVisProp("rotate"),
                visible: image.evalVisProp("visible")
            }
        });
        const withBoard = async (action) => {
            const board = JXG.JSXGraph.initBoard("board", boardOptions);
            try {
                return await action(board);
            } finally {
                JXG.JSXGraph.freeBoard(board);
            }
        };
        const waitForImage = async (image) => {
            if (!image.rendNode.complete) {
                await new Promise((resolveImage, rejectImage) => {
                    image.rendNode.addEventListener(
                        "load",
                        resolveImage,
                        {once: true}
                    );
                    image.rendNode.addEventListener(
                        "error",
                        rejectImage,
                        {once: true}
                    );
                });
            }
        };

        const defaults = await withBoard(async (board) => {
            const image = board.create(
                "image",
                [url, [-3, -2], [3, 4]],
                {id: "defaultImage", name: ""}
            );
            await waitForImage(image);
            board.fullUpdate();
            return snapshot(image);
        });

        const dynamic = await withBoard(async (board) => {
            const driver = board.create(
                "point",
                [-2, -1],
                {id: "driver", name: "", withLabel: false}
            );
            const image = board.create(
                "image",
                [
                    url,
                    [
                        function () {
                            return driver.X();
                        },
                        "driver.Y()"
                    ],
                    [
                        function () {
                            return driver.X() + 4;
                        },
                        "driver.Y() + 3"
                    ]
                ],
                {id: "dynamicImage", name: ""}
            );
            await waitForImage(image);
            board.fullUpdate();
            const initial = snapshot(image);
            driver.setPositionDirectly(JXG.COORDS_BY_USER, [1, 2]);
            board.update();
            return {
                initial,
                moved: snapshot(image),
                driverChildren: Object.keys(driver.childElements)
            };
        });

        const resized = await withBoard(async (board) => {
            const image = board.create(
                "image",
                [url, [-1, -1], [2, 2]],
                {id: "resizedImage", name: ""}
            );
            await waitForImage(image);
            board.fullUpdate();
            const initial = snapshot(image);
            image.setSize(4, 1.5);
            board.update();
            return {initial, updated: snapshot(image)};
        });

        const negativeSize = await withBoard(async (board) => {
            const image = board.create(
                "image",
                [url, [1, 2], [-2, -3]],
                {id: "negativeImage", name: ""}
            );
            await waitForImage(image);
            board.fullUpdate();
            return snapshot(image);
        });

        const rotated = await withBoard(async (board) => {
            const image = board.create(
                "image",
                [url, [-1, -2], [3, 2]],
                {
                    id: "rotatedImage",
                    name: "",
                    rotate: 30,
                    fillOpacity: 0.4
                }
            );
            await waitForImage(image);
            board.fullUpdate();
            return snapshot(image);
        });

        const failures = await withBoard((board) => {
            const cases = [
                [url, [0], [1, 1]],
                [url, [0, 0], [1]],
                [url, {x: 0, y: 0}, [1, 1]]
            ];
            return cases.map((parents) => {
                const before = Object.keys(board.objects);
                try {
                    board.create("image", parents, {name: ""});
                    return {threw: false};
                } catch (error) {
                    const after = Object.keys(board.objects);
                    return {
                        threw: true,
                        message: String(error),
                        unchanged:
                            JSON.stringify(before) === JSON.stringify(after)
                    };
                }
            });
        });

        return {
            version: JXG.version,
            dataUri: url,
            dataUriPrefix: url.slice(0, 30),
            dataUriLength: url.length,
            defaults,
            dynamic,
            resized,
            negativeSize,
            rotated,
            failures
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
