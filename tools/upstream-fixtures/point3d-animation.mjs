/*
 * Official JSXGraph 1.13.3 Point3D animation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/point3d-animation.mjs
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
        const clean = (value) => Object.is(value, -0) ? 0 : value;
        const vector = (values) => Array.from(values, clean);
        const path = (values) => values.map(vector);
        const intervals = [];
        const cleared = [];
        window.setInterval = (callback, delay) => {
            const id = intervals.length + 1;
            intervals.push({id, callback, delay});
            return id;
        };
        window.clearInterval = (id) => {
            cleared.push(id);
        };

        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-8, 8, 8, -8],
            axis: false,
            showCopyright: false,
            showNavigation: false,
            animationDelay: 25
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
        let pointIndex = 0;
        const createPoint = () => {
            const id = `point-${pointIndex}`;
            pointIndex += 1;
            return view.create("point3d", [0, 0, 0], {
                id,
                name: ""
            });
        };
        const captureMoveTo = (effect) => {
            const point = createPoint();
            let callbackCount = 0;
            const beforeIntervalCount = intervals.length;
            point.moveTo([4, 8, -4], 100, {
                effect,
                callback: () => {
                    callbackCount += 1;
                }
            });
            const interval = intervals[beforeIntervalCount];
            const storedPath = path(point.movePath);
            const frames = [];
            let callbackFrame = null;
            for (let frame = 1; frame <= storedPath.length + 1; frame += 1) {
                interval.callback();
                frames.push(vector(point.coords));
                if (callbackCount > 0 && callbackFrame === null) {
                    callbackFrame = frame;
                }
                if (cleared.includes(interval.id)) {
                    break;
                }
            }
            return {
                interval: interval.delay,
                storedPath,
                frames,
                callbackCount,
                callbackFrame
            };
        };

        const effects = Object.fromEntries(
            ["==", "<>", "<", ">"].map((effect) => [
                effect,
                captureMoveTo(effect)
            ])
        );

        const replacingPoint = createPoint();
        const callbacks = [];
        const beforeReplacementIntervalCount = intervals.length;
        replacingPoint.moveAlong(
            [[1, 1, 1], [2, 2, 2], [3, 3, 3]],
            90,
            {callback: () => callbacks.push("first")}
        );
        replacingPoint.moveAlong(
            [[-1, -2, -3], [-4, -5, -6]],
            20,
            {callback: () => callbacks.push("replacement")}
        );
        const replacementInterval =
            intervals[beforeReplacementIntervalCount];
        while (!cleared.includes(replacementInterval.id)) {
            replacementInterval.callback();
        }

        const immediatePoint = createPoint();
        let immediateCallbackCount = 0;
        immediatePoint.moveTo([1, 2, 3], 0, {
            callback: () => {
                immediateCallbackCount += 1;
            }
        });

        return {
            effects,
            replacement: {
                interval: replacementInterval.delay,
                coordinates: vector(replacingPoint.coords),
                callbacks
            },
            immediate: {
                coordinates: vector(immediatePoint.coords),
                callbackCount: immediateCallbackCount
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
