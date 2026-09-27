/*
 * Official JSXGraph 1.13.3 Measurement and PrefixParser behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/measurement.mjs
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
        const p1 = board.create("point", [1, 1], {
            id: "p1",
            name: ""
        });
        const p2 = board.create("point", [1, 3], {
            id: "p2",
            name: ""
        });
        const circle = board.create("circle", [p1, p2], {
            id: "circle",
            name: ""
        });
        const segment = board.create(
            "segment",
            [[-2, -3], [-2, 3]],
            {id: "segment", name: ""}
        );
        const slider = board.create(
            "slider",
            [[-4, 4], [-1.5, 4], [-10, 1, 10]],
            {id: "slider", name: "a"}
        );
        const radius = board.create(
            "measurement",
            [-6, -2, ["Radius", circle]],
            {
                id: "radius",
                name: "",
                prefix: "r=",
                baseUnit: "cm",
                digits: 3
            }
        );
        const length = board.create(
            "measurement",
            [-6, -4, ["L", segment]],
            {
                id: "length",
                name: "",
                prefix: "l=",
                units: {1: " meter", dim2: " square meter"},
                digits: "auto"
            }
        );
        const total = board.create(
            "measurement",
            [1, -4, ["+", ["V", radius], ["V", length], ["V", slider]]],
            {
                id: "total",
                name: "",
                prefix: "sum=",
                suffix: "!",
                baseUnit: "cm",
                dim: 1,
                digits: 2
            }
        );
        const area = board.create(
            "measurement",
            [1, -2, ["Area", circle]],
            {
                id: "area",
                name: "",
                prefix: "A=",
                baseUnit: " cm",
                digits: "none"
            }
        );
        const sine = board.create(
            "measurement",
            [1, -6, ["exec", "sin", ["V", slider]]],
            {
                id: "sine",
                name: "",
                prefix: "sin=",
                digits: "auto"
            }
        );
        const coords = board.create(
            "measurement",
            [-6, -6, ["Coords", p2]],
            {
                id: "coords",
                name: "",
                prefix: "P=",
                dim: "coords",
                digits: 1
            }
        );
        const direction = board.create(
            "measurement",
            [1, 0, ["Direction", segment]],
            {
                id: "direction",
                name: "",
                prefix: "d=",
                dim: "direction",
                digits: 1
            }
        );
        const dynamicState = {
            digits: 1,
            prefix: "custom=",
            suffix: "!"
        };
        const customCoords = board.create(
            "measurement",
            [-2, 2, ["Coords", p2]],
            {
                id: "customCoords",
                name: "",
                dim: () => "coords",
                digits: () => dynamicState.digits,
                prefix: () => dynamicState.prefix,
                suffix: () => dynamicState.suffix,
                showPrefix: () => true,
                showSuffix: () => dynamicState.suffix !== "",
                formatCoords: (_, x, y, z) => `${x}|${y}|${z}`
            }
        );
        const customDirection = board.create(
            "measurement",
            [-2, 1, ["Direction", segment]],
            {
                id: "customDirection",
                name: "",
                dim: "direction",
                digits: 1,
                prefix: "dir=",
                formatDirection: (_, x, y) => `${y}/${x}`
            }
        );
        const rawArea = board.create(
            "measurement",
            [-2, -1, ["Area", circle]],
            {
                id: "rawArea",
                name: "",
                prefix: "raw=",
                baseUnit: " cm",
                digits: "none",
                parse: false
            }
        );
        board.update();

        const snapshot = (measurement) => ({
            id: measurement.id,
            type: measurement.type,
            elType: measurement.elType,
            plaintext: measurement.plaintext,
            value: measurement.Value(),
            dimension: measurement.Dimension(),
            unit: measurement.Unit(),
            unitArray: measurement.Unit([0, 1, 2]),
            method: measurement.getMethod(),
            term: measurement.getTerm().map((value) => value?.id ?? value),
            prefix: measurement.toPrefix(),
            parents: measurement.getParents().map(
                (value) => value?.id ?? value
            )
        });

        const initial = [
            radius,
            length,
            total,
            area,
            sine,
            coords,
            direction,
            customCoords,
            customDirection,
            rawArea
        ].map(snapshot);
        p2.setPosition(JXG.COORDS_BY_USER, [1, 5]);
        slider.setValue(2);
        dynamicState.digits = 2;
        dynamicState.prefix = "moved=";
        dynamicState.suffix = "";
        board.update();
        const moved = [
            radius,
            length,
            total,
            area,
            sine,
            coords,
            direction,
            customCoords,
            customDirection,
            rawArea
        ].map(snapshot);

        const errors = {};
        for (const [name, term] of Object.entries({
            malformed: ["+"],
            forbidden: ["exec", "alert", 1],
            unknownMethod: ["Missing", circle]
        })) {
            try {
                JXG.PrefixParser.parse(term, "execute");
                errors[name] = "no error";
            } catch (error) {
                errors[name] = String(error.message);
            }
        }

        const measurementErrors = {};
        for (const [name, term, attributes] of [
            ["malformed", ["+"], {}],
            [
                "formatter",
                ["Coords", p2],
                {
                    dim: "coords",
                    formatCoords: () => {
                        throw new Error("formatter boom");
                    }
                }
            ]
        ]) {
            const host = document.createElement("div");
            host.id = `error-${name}`;
            host.style.cssText = "width: 64px; height: 64px";
            document.body.appendChild(host);
            const errorBoard = JXG.JSXGraph.initBoard(host.id, {
                boundingbox: [-2, 2, 2, -2],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
            const localPoint = errorBoard.create("point", [1, 1], {
                id: `${name}Point`,
                name: ""
            });
            const localTerm =
                name === "formatter" ? ["Coords", localPoint] : term;
            const before = errorBoard.objectsList.map(
                (element) => element.id
            );
            try {
                errorBoard.create(
                    "measurement",
                    [0, 0, localTerm],
                    {id: `${name}Measurement`, name: "", ...attributes}
                );
                measurementErrors[name] = {
                    error: "no error",
                    before,
                    after: errorBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            } catch (error) {
                measurementErrors[name] = {
                    error: String(error.message),
                    before,
                    after: errorBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            }
            JXG.JSXGraph.freeBoard(errorBoard);
            host.remove();
        }

        return JSON.stringify({
            version: JXG.version,
            initial,
            moved,
            objectOrder: board.objectsList.map((element) => element.id),
            errors,
            measurementErrors
        }, null, 2);
    });
    console.log(evidence);
} finally {
    await browser.close();
}
