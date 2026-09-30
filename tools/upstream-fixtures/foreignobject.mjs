/*
 * Official JSXGraph 1.13.3 ForeignObject behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/foreignobject.mjs
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
        const normalized = (value) => {
            if (typeof value === "undefined") {
                return "<undefined>";
            }
            if (typeof value === "function") {
                return `<function:${value()}>`;
            }
            if (Array.isArray(value)) {
                return value.map(normalized);
            }
            return value;
        };
        const snapshot = (foreignObject) => ({
            id: foreignObject.id,
            name: foreignObject.name,
            type: foreignObject.type,
            elementClass: foreignObject.elementClass,
            elType: foreignObject.elType,
            fixed: foreignObject.visProp.fixed,
            visible: foreignObject.visProp.visible,
            needsRegularUpdate: foreignObject.needsRegularUpdate,
            evaluateOnlyOnce: foreignObject.visProp.evaluateonlyonce,
            useUserSize: foreignObject._useUserSize,
            coordinates: foreignObject.coords.usrCoords.slice(),
            screenCoordinates: foreignObject.coords.scrCoords.slice(),
            contentType: typeof foreignObject.content,
            content:
                typeof foreignObject.content === "function"
                    ? foreignObject.content()
                    : foreignObject.content,
            nodeHtml: foreignObject.rendNode.innerHTML,
            overflow: foreignObject.rendNode.style.overflow,
            size: [...foreignObject.size],
            userSize: foreignObject.usrSize
                ? [...foreignObject.usrSize]
                : null,
            width:
                typeof foreignObject.W === "function"
                    ? foreignObject.W()
                    : "<missing>",
            height:
                typeof foreignObject.H === "function"
                    ? foreignObject.H()
                    : "<missing>",
            span: foreignObject.span
                ? foreignObject.span.map((value) => [...value])
                : null,
            parents: normalized(foreignObject.getParents()),
            transformationCount: foreignObject.transformations.length
        });
        const captureError = (parents) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create("foreignobject", parents, {
                    id: "failure",
                    name: ""
                });
                return {
                    threw: false,
                    elType: value?.elType ?? null,
                    objectDelta: board.objectsList.length - before
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error.name,
                    message: String(error.message),
                    objectDelta: board.objectsList.length - before
                };
            }
        };

        const staticBoard = createBoard();
        const fullName = staticBoard.create(
            "foreignobject",
            [
                '<div style="width:120px;height:50px">full</div>',
                [-3, -2],
                [3, 2]
            ],
            {id: "full", name: ""}
        );
        const alias = staticBoard.create(
            "fo",
            [
                '<div style="width:90px;height:40px">alias</div>',
                [1, 2]
            ],
            {id: "alias", name: ""}
        );
        staticBoard.update();
        const aliases = {
            fullName: snapshot(fullName),
            alias: snapshot(alias)
        };

        const dynamicBoard = createBoard();
        let x = -2;
        let y = -1;
        let width = 2;
        let height = 1.5;
        const dynamic = dynamicBoard.create(
            "foreignobject",
            [
                '<span style="width:70px;height:30px">dynamic</span>',
                [() => x, () => y],
                [() => width, () => height]
            ],
            {id: "dynamic", name: "", needsRegularUpdate: true}
        );
        dynamicBoard.update();
        const dynamicInitial = snapshot(dynamic);
        x = 3;
        y = 2;
        width = 4;
        height = 2.5;
        dynamicBoard.update();
        const dynamicUpdated = snapshot(dynamic);
        dynamic.setSize(() => width + 1, () => height + 1);
        dynamicBoard.update();
        const setSize = snapshot(dynamic);

        const transformedBoard = createBoard();
        const transformed = transformedBoard.create(
            "foreignobject",
            ["<b>transformed</b>", [1, 2], [3, 2]],
            {id: "transformed", name: ""}
        );
        const translation = transformedBoard.create(
            "transform",
            [2, -1],
            {type: "translate"}
        );
        transformed.addTransform(translation);
        transformedBoard.update();
        const transformedAfterBoardUpdate = snapshot(transformed);
        transformed.fullUpdate();
        const transformedAfterFullUpdate = snapshot(transformed);
        let transformedHasPoint;
        try {
            transformedHasPoint = {
                threw: false,
                value: transformed.hasPoint(
                    transformed.coords.scrCoords[1],
                    transformed.coords.scrCoords[2]
                )
            };
        } catch (error) {
            transformedHasPoint = {
                threw: true,
                name: error.name,
                message: String(error.message)
            };
        }

        const contentBoard = createBoard();
        const repeated = contentBoard.create(
            "foreignobject",
            ["<i>first</i>", [-2, 1], [2, 1]],
            {id: "repeated", name: "", evaluateOnlyOnce: false}
        );
        const once = contentBoard.create(
            "foreignobject",
            ["<i>first</i>", [1, 1], [2, 1]],
            {id: "once", name: "", evaluateOnlyOnce: true}
        );
        const functionContent = contentBoard.create(
            "foreignobject",
            [() => "<i>function</i>", [4, 1], [2, 1]],
            {id: "function", name: ""}
        );
        contentBoard.update();
        const contentInitial = {
            repeated: snapshot(repeated),
            once: snapshot(once),
            function: snapshot(functionContent)
        };
        repeated.content = "<i>second</i>";
        once.content = "<i>second</i>";
        repeated.fullUpdate();
        once.fullUpdate();
        const contentUpdated = {
            repeated: snapshot(repeated),
            once: snapshot(once)
        };

        return {
            version: JXG.version,
            aliases,
            dynamicInitial,
            dynamicUpdated,
            setSize,
            transformed: {
                afterBoardUpdate: transformedAfterBoardUpdate,
                afterFullUpdate: transformedAfterFullUpdate,
                hasPoint: transformedHasPoint
            },
            contentInitial,
            contentUpdated,
            failures: {
                missingPosition: captureError(["content"]),
                invalidPosition: captureError(["content", "bad"]),
                invalidSize: captureError(["content", [0, 0], "bad"])
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
