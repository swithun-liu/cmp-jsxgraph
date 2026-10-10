/*
 * Official JSXGraph 1.13.3 Board multi-pointer fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/board-multi-pointer.mjs
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
        const createBoard = (attributes = {}) => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "500px";
            container.style.height = "400px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-5, 4, 5, -4],
                keepaspectratio: false,
                axis: false,
                showCopyright: false,
                showNavigation: false,
                ...attributes
            });
        };
        const values = (array) => Array.from(array, (value) =>
            Object.is(value, -0) ? 0 : value
        );
        const point = (element) => values(element.coords.usrCoords);
        const snapshot = (board) => ({
            boundingBox: values(board.getBoundingBox()),
            origin: values(board.origin.scrCoords),
            unitX: board.unitX,
            unitY: board.unitY,
            zoomX: board.zoomX,
            zoomY: board.zoomY
        });
        const targets = () => [
            {
                num: 1,
                Xprev: 150,
                Yprev: 200,
                X: 200,
                Y: 250
            },
            {
                num: 2,
                Xprev: 350,
                Yprev: 200,
                X: 400,
                Y: 150
            }
        ];

        const matrixBoard = createBoard();
        const matrixTargets = targets();
        const matrix = values(matrixBoard.getTwoFingerTransform(
            matrixTargets[0],
            matrixTargets[1],
            true,
            true
        ));
        const translationOnly = values(matrixBoard.getTwoFingerTransform(
            matrixTargets[0],
            matrixTargets[1],
            false,
            false
        ));

        const lineBoard = createBoard();
        const lineP1 = lineBoard.create("point", [-1, 0], {name: ""});
        const lineP2 = lineBoard.create("point", [1, 0], {name: ""});
        const line = lineBoard.create("line", [lineP1, lineP2], {
            name: "",
            scalable: true,
            rotatable: true
        });
        lineBoard.twoFingerTouchObject(targets(), line, 2);

        const polygonBoard = createBoard();
        const polygon = polygonBoard.create(
            "polygon",
            [[-1, -1], [1, -1], [0, 1]],
            {
                name: "",
                scalable: true,
                rotatable: true,
                vertices: {name: ""}
            }
        );
        polygonBoard.twoFingerTouchObject(targets(), polygon, 2);

        const snappedPolygonBoard = createBoard();
        const snappedPolygon = snappedPolygonBoard.create(
            "polygon",
            [[-1, -1], [1, -1], [0, 1]],
            {
                name: "",
                scalable: true,
                rotatable: true,
                vertices: {name: ""}
            }
        );
        snappedPolygon.vertices[1].setAttribute({snapToGrid: true});
        snappedPolygonBoard.twoFingerTouchObject(
            targets(),
            snappedPolygon,
            2
        );

        const circleBoard = createBoard();
        const circle = circleBoard.create(
            "circle",
            [[0, 0], [1, 0]],
            {name: "", scalable: true}
        );
        circleBoard.twoFingerTouchCircle(targets(), circle, 2);

        const radiusBoard = createBoard();
        const radiusCircle = radiusBoard.create(
            "circle",
            [[0, 0], 1],
            {name: "", scalable: true}
        );
        radiusBoard.twoFingerTouchCircle(targets(), radiusCircle, 2);

        const gestureEvent = (first, second) => ({
            touches: [
                {clientX: first[0], clientY: first[1]},
                {clientX: second[0], clientY: second[1]}
            ],
            preventDefault() {}
        });
        const pinchBoard = createBoard();
        pinchBoard.gestureStartListener(
            gestureEvent([200, 200], [300, 200])
        );
        pinchBoard.gestureChangeListener(
            gestureEvent([185, 200], [315, 200])
        );

        const panBoard = createBoard({
            pan: {enabled: true, needTwoFingers: true}
        });
        panBoard.gestureStartListener(
            gestureEvent([200, 200], [300, 200])
        );
        panBoard.gestureChangeListener(
            gestureEvent([220, 220], [320, 220])
        );

        return {
            version: JXG.version,
            matrix,
            translationOnly,
            line: [point(lineP1), point(lineP2)],
            polygon: polygon.vertices.slice(0, -1).map(point),
            snappedPolygon: snappedPolygon.vertices
                .slice(0, -1)
                .map(point),
            circle: {
                center: point(circle.center),
                point2: point(circle.point2),
                radius: circle.Radius()
            },
            radiusCircle: {
                center: point(radiusCircle.center),
                radius: radiusCircle.Radius()
            },
            pinch: snapshot(pinchBoard),
            pan: snapshot(panBoard)
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
