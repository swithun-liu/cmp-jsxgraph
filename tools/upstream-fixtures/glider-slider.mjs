/*
 * Official JSXGraph 1.13.3 Glider/Slider behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/glider-slider.mjs
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
        const coordinates = (point) => point.coords.usrCoords.slice();
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors),
            descendants: Object.keys(element.descendants)
        });
        const pointSnapshot = (point) => ({
            id: point.id,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            coordinates: coordinates(point),
            position: point.position,
            draggable: point.isDraggable,
            slideObject: point.slideObject?.id ?? null,
            slideObjects: point.slideObjects.map((element) => element.id),
            relations: relations(point)
        });
        const captureFailure = (create) => {
            const board = createBoard();
            const before = board.objectsList.map((element) => element.id);
            try {
                create(board);
                return {
                    threw: false,
                    before,
                    after: board.objectsList.map((element) => element.id)
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error),
                    before,
                    after: board.objectsList.map((element) => element.id)
                };
            }
        };

        const gliderBoard = createBoard();
        const lineStart = gliderBoard.create("point", [-4, 1], {
            id: "lineStart",
            name: ""
        });
        const lineEnd = gliderBoard.create("point", [4, 3], {
            id: "lineEnd",
            name: ""
        });
        const segment = gliderBoard.create("segment", [lineStart, lineEnd], {
            id: "segment",
            name: ""
        });
        const glider = gliderBoard.create("glider", [0, 5, segment], {
            id: "glider",
            name: ""
        });
        const gliderInitial = pointSnapshot(glider);

        glider.setPosition(JXG.COORDS_BY_USER, [8, -3]);
        gliderBoard.update(glider);
        const gliderClamped = pointSnapshot(glider);

        glider.setGliderPosition(0.25);
        const gliderPositioned = pointSnapshot(glider);

        lineEnd.setPosition(JXG.COORDS_BY_USER, [4, 5]);
        gliderBoard.update();
        const gliderAfterParentMove = pointSnapshot(glider);

        const oneParentBoard = createBoard();
        const oneParentLine = oneParentBoard.create(
            "line",
            [[-2, 1], [2, 1]],
            {id: "oneParentLine", name: ""}
        );
        const oneParentGlider = oneParentBoard.create(
            "glider",
            [oneParentLine],
            {id: "oneParentGlider", name: ""}
        );

        const sliderBoard = createBoard();
        const slider = sliderBoard.create(
            "slider",
            [[-4, -1], [4, 3], [-10, 3, 10]],
            {
                id: "slider",
                name: "s",
                point1: {id: "sliderStart"},
                point2: {id: "sliderEnd"},
                baseline: {id: "baseline"},
                highline: {id: "highline"},
                ticks: {id: "sliderTicks"},
                label: {id: "sliderLabel"}
            }
        );
        sliderBoard.update();
        const sliderSnapshot = () => ({
            point: pointSnapshot(slider),
            value: slider.Value(),
            smin: slider._smin,
            smax: slider._smax,
            point1: pointSnapshot(slider.point1),
            point2: pointSnapshot(slider.point2),
            baseline: {
                id: slider.baseline.id,
                elType: slider.baseline.elType,
                dump: slider.baseline.dump,
                straightFirst: slider.baseline.visProp.straightfirst,
                straightLast: slider.baseline.visProp.straightlast,
                relations: relations(slider.baseline)
            },
            highline: {
                id: slider.highline.id,
                elType: slider.highline.elType,
                dump: slider.highline.dump,
                relations: relations(slider.highline)
            },
            ticks: {
                id: slider.ticks.id,
                dump: slider.ticks.dump,
                fixedTicks: slider.ticks.fixedTicks,
                ticksDistance: slider.ticks.evalVisProp("ticksdistance"),
                drawLabels: slider.ticks.evalVisProp("drawlabels")
            },
            label: {
                id: slider.label.id,
                dump: slider.label.dump,
                plaintext: slider.label.plaintext
            },
            subs: Object.fromEntries(
                Object.entries(slider.subs).map(([key, element]) => [
                    key,
                    element.id
                ])
            ),
            inherits: slider.inherits.map((element) => element.id),
            objectOrder: sliderBoard.objectsList.map((element) => element.id)
        });
        const sliderInitial = sliderSnapshot();

        slider.setValue(8);
        sliderBoard.update();
        const sliderSetValue = sliderSnapshot();

        slider.setMin(-20).setMax(20).setValue(-5);
        sliderBoard.update();
        const sliderRangeChanged = sliderSnapshot();

        const snapBoard = createBoard();
        const snapSlider = snapBoard.create(
            "slider",
            [[-4, 0], [4, 0], [0, 2.6, 10]],
            {
                id: "snapSlider",
                name: "",
                withLabel: false,
                withTicks: false,
                snapWidth: 2,
                snapValues: [1, 7, 9],
                snapValueDistance: 0.6
            }
        );
        snapBoard.update();
        const snapInitial = {
            point: pointSnapshot(snapSlider),
            value: snapSlider.Value()
        };
        snapSlider.setPosition(JXG.COORDS_BY_USER, [1.44, 0]);
        snapBoard.update(snapSlider);
        const snapValues = {
            point: pointSnapshot(snapSlider),
            value: snapSlider.Value()
        };

        const removalOrder = sliderBoard.objectsList.map(
            (element) => element.id
        );
        sliderBoard.removeObject(slider);
        const afterRemoval = sliderBoard.objectsList.map(
            (element) => element.id
        );

        const failures = {
            gliderOnTicks: captureFailure((board) => {
                const line = board.create("segment", [[-2, 0], [2, 0]], {
                    id: "line"
                });
                const ticks = board.create("ticks", [line, 1], {
                    id: "ticks"
                });
                board.create("glider", [0, 0, ticks], {
                    id: "candidate"
                });
            }),
            gliderOnPoint: captureFailure((board) => {
                const point = board.create("point", [0, 0], {
                    id: "point"
                });
                board.create("glider", [1, 1, point], {
                    id: "candidate"
                });
            }),
            malformedSlider: captureFailure((board) => {
                board.create(
                    "slider",
                    [[-2, 0], [2, 0], [0, 1]],
                    {id: "candidate"}
                );
            })
        };

        return {
            version: JXG.version,
            glider: {
                initial: gliderInitial,
                clamped: gliderClamped,
                positioned: gliderPositioned,
                afterParentMove: gliderAfterParentMove,
                oneParentLine: pointSnapshot(oneParentGlider)
            },
            slider: {
                initial: sliderInitial,
                setValue: sliderSetValue,
                rangeChanged: sliderRangeChanged,
                snapInitial,
                snapValues,
                removalOrder,
                afterRemoval
            },
            failures
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
