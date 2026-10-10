/*
 * Official JSXGraph 1.13.3 CoordsElement attractor and animation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/coords-attractor-animation.mjs
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
        const clean = (value) => Object.is(value, -0) ? 0 : value;
        const vector = (values) => Array.from(values, clean);
        const path = (values) => values.map(vector);
        const createBoard = (animationDelay = 25) => {
            const id = `board-${boardIndex}`;
            boardIndex += 1;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false,
                animationDelay
            });
        };
        const point = (board, coordinates, id, attributes = {}) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });
        const snapshotAttractor = (candidate) => ({
            coordinates: vector(candidate.coords.usrCoords),
            type: candidate.type,
            elType: candidate.elType,
            slideObject: candidate.slideObject?.id ?? null,
            onPolygon: candidate.onPolygon,
            slideObjects: candidate.slideObjects.map((element) => element.id),
            parents: [...candidate.parents]
        });
        const exerciseAttractor = (
            board,
            id,
            attractor,
            near,
            far
        ) => {
            const candidate = point(board, far, id, {
                attractors: [attractor],
                attractorDistance: 0.75,
                snatchDistance: 1.5,
                attractorUnit: "user"
            });
            candidate.setPositionDirectly(JXG.COORDS_BY_USER, near);
            const attached = snapshotAttractor(candidate);
            candidate.setPositionDirectly(JXG.COORDS_BY_USER, far);
            const detached = snapshotAttractor(candidate);
            return {attached, detached};
        };

        const attractorBoard = createBoard();
        const line = attractorBoard.create(
            "segment",
            [[-2, 0], [2, 0]],
            {id: "line", name: "", withLabel: false}
        );
        const center = point(attractorBoard, [0, 0], "center");
        const radius = point(attractorBoard, [2, 0], "radius");
        const circle = attractorBoard.create(
            "circle",
            [center, radius],
            {id: "circle", name: "", withLabel: false}
        );
        const curve = attractorBoard.create(
            "curve",
            [(t) => t, (t) => t * t, -2, 2],
            {id: "curve", name: "", withLabel: false}
        );
        const polygon = attractorBoard.create(
            "polygon",
            [[-2, -2], [2, -2], [2, 2], [-2, 2]],
            {
                id: "polygon",
                name: "",
                withLabel: false,
                vertices: {withLabel: false},
                borders: {withLabel: false}
            }
        );
        const target = point(attractorBoard, [1, 1], "target");
        const arc = attractorBoard.create(
            "arc",
            [center, radius, point(attractorBoard, [0, 2], "arcEnd")],
            {id: "arc", name: "", withLabel: false}
        );
        const sectorCenter = point(
            attractorBoard,
            [-4, 0],
            "sectorCenter"
        );
        const sector = attractorBoard.create(
            "sector",
            [
                sectorCenter,
                point(attractorBoard, [-2, 0], "sectorRadius"),
                point(attractorBoard, [-4, 2], "sectorEnd")
            ],
            {id: "sector", name: "", withLabel: false}
        );

        const attractors = {
            point: exerciseAttractor(
                attractorBoard,
                "pointCandidate",
                target,
                [1.2, 1.1],
                [6, 5]
            ),
            line: exerciseAttractor(
                attractorBoard,
                "lineCandidate",
                line,
                [0.5, 0.4],
                [6, 5]
            ),
            circle: exerciseAttractor(
                attractorBoard,
                "circleCandidate",
                circle,
                [2.3, 0],
                [6, 5]
            ),
            curve: exerciseAttractor(
                attractorBoard,
                "curveCandidate",
                curve,
                [1, 1.2],
                [6, 5]
            ),
            arc: exerciseAttractor(
                attractorBoard,
                "arcCandidate",
                arc,
                [1.5, 1.5],
                [6, 5]
            ),
            sector: exerciseAttractor(
                attractorBoard,
                "sectorCandidate",
                sector,
                [-2.5, 1.5],
                [6, 5]
            ),
            polygon: exerciseAttractor(
                attractorBoard,
                "polygonCandidate",
                polygon,
                [2.2, 0],
                [6, 5]
            )
        };

        const animationBoard = createBoard();
        animationBoard.addAnimation = function (element) {
            this.animationObjects[element.id] = element;
            return this;
        };
        const captureAnimation = (id, action) => {
            const candidate = point(animationBoard, [0, 0], id);
            let callbackCount = 0;
            action(candidate, () => {
                callbackCount += 1;
            });
            const storedPath = path(candidate.animationPath ?? []);
            const frames = [];
            let callbackFrame = null;
            for (let frame = 1; frame <= storedPath.length + 2; frame += 1) {
                animationBoard.animate();
                frames.push(vector(candidate.coords.usrCoords));
                if (callbackCount > 0 && callbackFrame === null) {
                    callbackFrame = frame;
                }
                if (!candidate.animationPath) {
                    break;
                }
            }
            return {
                storedPath,
                frames,
                callbackCount,
                callbackFrame
            };
        };
        const animations = {
            moveToLinear: captureAnimation(
                "moveToLinear",
                (candidate, callback) => candidate.moveTo(
                    [4, 8],
                    100,
                    {effect: "==", callback}
                )
            ),
            moveToEase: captureAnimation(
                "moveToEase",
                (candidate, callback) => candidate.moveTo(
                    [4, 8],
                    100,
                    {effect: "<>", callback}
                )
            ),
            moveAlongNeville: captureAnimation(
                "moveAlongNeville",
                (candidate, callback) => candidate.moveAlong(
                    [[0, 0], [2, 4], [4, 0]],
                    100,
                    {callback}
                )
            ),
            moveAlongLinearFractional: captureAnimation(
                "moveAlongLinearFractional",
                (candidate, callback) => candidate.moveAlong(
                    [[0, 0], [2, 4], [4, 0]],
                    90,
                    {interpolate: false, callback}
                )
            ),
            visitOddSteps: captureAnimation(
                "visitOddSteps",
                (candidate, callback) => candidate.visit(
                    [3, 6],
                    75,
                    {effect: "==", repeat: 1, callback}
                )
            )
        };
        const functionCandidate = point(
            animationBoard,
            [0, 0],
            "moveAlongFunction"
        );
        const functionElapsed = [];
        let functionInvocationCount = 0;
        let functionCallbackCount = 0;
        functionCandidate.moveAlong(
            (elapsed) => {
                functionElapsed.push(elapsed);
                functionInvocationCount += 1;
                if (functionInvocationCount > 2) {
                    return Number.NaN;
                }
                return [
                    functionInvocationCount,
                    functionInvocationCount * 2
                ];
            },
            100,
            {
                callback: () => {
                    functionCallbackCount += 1;
                }
            }
        );
        const functionFrames = [];
        for (let frame = 1; frame <= 4; frame += 1) {
            animationBoard.animate();
            functionFrames.push(
                vector(functionCandidate.coords.usrCoords)
            );
            if (!functionCandidate.animationPath) {
                break;
            }
        }
        animations.moveAlongFunction = {
            frames: functionFrames,
            invocationCount: functionInvocationCount,
            elapsedIsMonotonic: functionElapsed.every(
                (value, index) =>
                    index === 0 || value >= functionElapsed[index - 1]
            ),
            callbackCount: functionCallbackCount
        };

        let immediateCallbackCount = 0;
        const immediate = point(animationBoard, [1, 2], "immediate");
        immediate.moveTo([3, 4], 0, {
            callback: () => {
                immediateCallbackCount += 1;
            }
        });

        const attributePoint = point(
            animationBoard,
            [0, 0],
            "attributePoint",
            {
                strokeColor: "#ff0000",
                fillColor: "#000000",
                strokeOpacity: 0,
                strokeWidth: 1,
                fillOpacity: 0.25,
                size: 1
            }
        );
        let attributeCallbackCount = 0;
        attributePoint.animate(
            {
                strokeColor: "#00ff00",
                fillColor: "#ffffff",
                strokeOpacity: 1,
                strokeWidth: 5,
                fillOpacity: 0.75,
                size: 6.9
            },
            75,
            {
                callback: () => {
                    attributeCallbackCount += 1;
                }
            }
        );
        const attributeFrames = [];
        let attributeCallbackFrame = null;
        for (let frame = 1; frame <= 5; frame += 1) {
            animationBoard.animate();
            attributeFrames.push({
                strokeColor: attributePoint.visProp.strokecolor,
                fillColor: attributePoint.visProp.fillcolor,
                strokeOpacity: attributePoint.visProp.strokeopacity,
                strokeWidth: attributePoint.visProp.strokewidth,
                fillOpacity: attributePoint.visProp.fillopacity,
                size: attributePoint.visProp.size
            });
            if (
                attributeCallbackCount > 0 &&
                attributeCallbackFrame === null
            ) {
                attributeCallbackFrame = frame;
            }
            if (!attributePoint.animationData) {
                break;
            }
        }

        const gliderBoard = createBoard();
        const intervalCallbacks = [];
        const clearedIntervals = [];
        window.setInterval = (callback, delay) => {
            const id = intervalCallbacks.length + 1;
            intervalCallbacks.push({id, callback, delay});
            return id;
        };
        window.clearInterval = (id) => {
            clearedIntervals.push(id);
        };
        const gliderHost = gliderBoard.create(
            "segment",
            [[-2.13, 0.17], [2.28, 1.41]],
            {id: "gliderHost", name: "", withLabel: false}
        );
        const glider = gliderBoard.create(
            "glider",
            [-2.13, 0.17, gliderHost],
            {id: "glider", name: "", withLabel: false}
        );
        glider.startAnimation(1, 4, 0, 1);
        const gliderInterval = intervalCallbacks[0];
        const gliderFrames = [];
        for (let frame = 0; frame < 8; frame += 1) {
            gliderInterval.callback();
            gliderFrames.push(vector(glider.coords.usrCoords));
            if (clearedIntervals.includes(gliderInterval.id)) {
                break;
            }
        }
        glider.stopAnimation();
        glider.startAnimation(-1, 4, 30, 1);
        const restartInterval = intervalCallbacks[1];
        restartInterval.callback();

        return {
            attractors,
            animations,
            immediate: {
                coordinates: vector(immediate.coords.usrCoords),
                callbackCount: immediateCallbackCount,
                hasAnimationPath: Boolean(immediate.animationPath)
            },
            attributeAnimation: {
                frames: attributeFrames,
                callbackCount: attributeCallbackCount,
                callbackFrame: attributeCallbackFrame
            },
            gliderAnimation: {
                interval: gliderInterval.delay,
                frames: gliderFrames,
                stoppedAfterFrame: gliderFrames.length,
                restartInterval: restartInterval.delay,
                restartFrame: vector(glider.coords.usrCoords)
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
