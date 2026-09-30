/*
 * Official JSXGraph 1.13.3 Angle mutation/display and Turtle fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/angle-turtle.mjs
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
        const classify = (value) => {
            if (value === undefined) {
                return "undefined";
            }
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (Object.is(value, -0)) {
                return "-0";
            }
            return value;
        };
        const values = (array) => Array.from(array, classify);
        const point = (element) => values(element.coords.usrCoords);
        const angleSnapshot = (angle) => ({
            type: angle.visProp.type,
            dataX: values(angle.dataX),
            dataY: values(angle.dataY),
            bezierDegree: angle.bezierDegree,
            radiuspoint: point(angle.radiuspoint),
            anglepoint: point(angle.anglepoint),
            dot: point(angle.dot),
            dotVisible: angle.dot.visProp.visible,
            hasFixedAngle: Boolean(angle.hasFixedAngle),
            angleParents: [...angle.parents],
            pointParents: [...angle.anglepoint.parents],
            transformations: angle.anglepoint.transformations.length,
            value: angle.Value("radians")
        });

        const angleBoard = createBoard();
        const first = angleBoard.create("point", [4, 0], {
            id: "first",
            name: ""
        });
        const vertex = angleBoard.create("point", [0, 0], {
            id: "vertex",
            name: ""
        });
        const third = angleBoard.create("point", [0, 4], {
            id: "third",
            name: ""
        });
        const sectorAngle = angleBoard.create(
            "angle",
            [first, vertex, third],
            {id: "sectorAngle", name: "", radius: 2, type: "sector"}
        );
        const squareAngle = angleBoard.create(
            "angle",
            [first, vertex, third],
            {id: "squareAngle", name: "", radius: 2, type: "square"}
        );
        const noneAngle = angleBoard.create(
            "angle",
            [first, vertex, third],
            {id: "noneAngle", name: "", radius: 2, type: "none"}
        );
        const dotAngle = angleBoard.create(
            "angle",
            [first, vertex, third],
            {id: "dotAngle", name: "", radius: 2, type: "sectordot"}
        );
        const fixedAngle = angleBoard.create(
            "angle",
            [first, vertex, third],
            {id: "fixedAngle", name: "", radius: 2}
        );
        fixedAngle.setAngle(Math.PI / 3);
        angleBoard.update();
        const angleInitial = {
            sector: angleSnapshot(sectorAngle),
            square: angleSnapshot(squareAngle),
            none: angleSnapshot(noneAngle),
            sectordot: angleSnapshot(dotAngle),
            fixed: angleSnapshot(fixedAngle)
        };
        first.setPositionDirectly(JXG.COORDS_BY_USER, [3, 1]);
        angleBoard.update();
        const angleMoved = angleSnapshot(fixedAngle);
        fixedAngle.free();
        third.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 3]);
        angleBoard.update();
        const angleFreed = angleSnapshot(fixedAngle);

        const twoLineSnapshot = (element) => ({
            point1: point(element.point1),
            point2: point(element.point2),
            point3: point(element.point3),
            dataX: values(element.dataX),
            dataY: values(element.dataY),
            radius: classify(element.Radius()),
            parents: [...element.parents],
            line1: element.line1.id,
            line2: element.line2.id,
            pointAlias: typeof element.point,
            pointSquareAlias: typeof element.pointsquare,
            virtualPointsRegistered: [
                element.point1,
                element.point2,
                element.point3
            ].some((candidate) =>
                Object.values(element.board.objects).includes(candidate)
            )
        });
        const twoLineBoard = createBoard();
        const lineOrigin = twoLineBoard.create("point", [0, 0], {
            id: "lineOrigin",
            name: ""
        });
        const horizontal = twoLineBoard.create("point", [4, 0], {
            id: "horizontal",
            name: ""
        });
        const vertical = twoLineBoard.create("point", [0, 4], {
            id: "vertical",
            name: ""
        });
        const line1 = twoLineBoard.create(
            "line",
            [lineOrigin, horizontal],
            {id: "line1", name: ""}
        );
        const line2 = twoLineBoard.create(
            "line",
            [lineOrigin, vertical],
            {id: "line2", name: ""}
        );
        let dynamicRadius = 2;
        const twoLineSector = twoLineBoard.create(
            "sector",
            [line1, line2, [3, 1], [1, 3], () => dynamicRadius],
            {id: "twoLineSector", name: ""}
        );
        const twoLineAngle = twoLineBoard.create(
            "angle",
            [line1, line2, -1, 1],
            {
                id: "twoLineAngle",
                name: "",
                radius: 2,
                type: "sector",
                orthoType: "sector"
            }
        );
        const twoLineInitial = {
            sector: twoLineSnapshot(twoLineSector),
            angle: twoLineSnapshot(twoLineAngle),
            setAngleReturn: typeof twoLineAngle.setAngle(Math.PI / 3),
            freeReturn: typeof twoLineAngle.free()
        };
        dynamicRadius = 3;
        horizontal.setPositionDirectly(JXG.COORDS_BY_USER, [5, 1]);
        twoLineBoard.update();
        const twoLineMoved = {
            sector: twoLineSnapshot(twoLineSector),
            angle: twoLineSnapshot(twoLineAngle)
        };

        const coincidentBoard = createBoard();
        const common = coincidentBoard.create("point", [0, 0]);
        const firstEnd = coincidentBoard.create("point", [1, 0]);
        const secondEnd = coincidentBoard.create("point", [2, 0]);
        const sharedFirstSector = coincidentBoard.create(
            "sector",
            [
                coincidentBoard.create("line", [common, firstEnd]),
                coincidentBoard.create("line", [common, secondEnd]),
                1,
                1,
                2
            ]
        );

        const repeatedPointBoard = createBoard();
        const repeated = repeatedPointBoard.create("point", [0, 0]);
        const repeatedFirst = repeatedPointBoard.create("point", [1, 0]);
        const repeatedSecond = repeatedPointBoard.create("point", [2, 0]);
        const sharedSecondSector = repeatedPointBoard.create(
            "sector",
            [
                repeatedPointBoard.create(
                    "line",
                    [repeatedFirst, repeated]
                ),
                repeatedPointBoard.create(
                    "line",
                    [repeatedSecond, repeated]
                ),
                1,
                1,
                2
            ]
        );

        const parallelBoard = createBoard();
        const parallelSector = parallelBoard.create(
            "sector",
            [
                parallelBoard.create("line", [[0, 0], [1, 0]]),
                parallelBoard.create("line", [[0, 1], [1, 1]]),
                1,
                1,
                2
            ]
        );

        const turtleSnapshot = (turtle) => ({
            id: turtle.id,
            registered: Boolean(turtle.board.objects[turtle.id]),
            pos: values(turtle.pos),
            dir: classify(turtle.dir),
            penDown: turtle.isPenDown,
            hidden: turtle.turtleIsHidden,
            stack: turtle.stack.map(values),
            objectTypes: turtle.objects.map((object) => ({
                id: object.id,
                type: object.type,
                elementClass: object.elementClass,
                elType: object.elType
            })),
            curves: turtle.objects
                .filter((object) => object.type === JXG.OBJECT_TYPE_CURVE)
                .map((curve) => ({
                    id: curve.id,
                    dataX: values(curve.dataX),
                    dataY: values(curve.dataY),
                    numberPoints: curve.numberPoints,
                    strokeWidth: curve.visProp.strokewidth,
                    strokeColor: curve.visProp.strokecolor,
                    highlightStrokeColor:
                        curve.visProp.highlightstrokecolor
                })),
            head: point(turtle.turtle),
            head2: point(turtle.turtle2),
            arrowVisible: turtle.arrow.visProp.visible,
            X: turtle.X(),
            Y: turtle.Y(),
            Z: turtle.Z(),
            minX: turtle.minX(),
            maxX: turtle.maxX(),
            samples: [0, 1, 2, 3, 4, 5].map((parameter) => ({
                parameter,
                x: classify(turtle.X(parameter)),
                y: classify(turtle.Y(parameter))
            })),
            penSize: turtle.getPenSize(),
            penColor: turtle.getPenColor(),
            highlightPenColor: turtle.getHighlightPenColor()
        });

        const turtleBoard = createBoard();
        const turtle = turtleBoard.create(
            "turtle",
            [[1, 2], 30],
            {
                id: "turtle",
                name: "",
                strokeWidth: 2,
                strokeColor: "#123456",
                highlightStrokeColor: "#abcdef"
            }
        );
        const turtleInitial = turtleSnapshot(turtle);
        turtle.forward(4);
        turtle.left(60);
        turtle.forward(2);
        turtle.penUp();
        turtle.moveTo([-2, 3]);
        turtle.penDown();
        turtle.setPenSize(5);
        turtle.setPenColor("#654321");
        turtle.setHighlightPenColor("#fedcba");
        turtle.lookTo([0, 0]);
        turtle.forward(3);
        turtle.pushTurtle();
        turtle.right(45);
        turtle.forward(1);
        const turtleDrawn = turtleSnapshot(turtle);
        turtle.popTurtle();
        const turtlePopped = turtleSnapshot(turtle);
        turtle.hideTurtle();
        turtle.forward(2);
        const turtleHidden = turtleSnapshot(turtle);
        turtle.showTurtle();
        const turtleShown = turtleSnapshot(turtle);
        turtle.clean();
        const turtleCleaned = turtleSnapshot(turtle);
        turtle.clearScreen();
        const turtleCleared = turtleSnapshot(turtle);

        return {
            version: JXG.version,
            angle: {
                initial: angleInitial,
                moved: angleMoved,
                freed: angleFreed
            },
            twoLineSectorAngle: {
                initial: twoLineInitial,
                moved: twoLineMoved,
                sharedFirst: twoLineSnapshot(sharedFirstSector),
                sharedSecond: twoLineSnapshot(sharedSecondSector),
                parallelNoCommon: twoLineSnapshot(parallelSector)
            },
            turtle: {
                initial: turtleInitial,
                drawn: turtleDrawn,
                popped: turtlePopped,
                hidden: turtleHidden,
                shown: turtleShown,
                cleaned: turtleCleaned,
                cleared: turtleCleared
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
