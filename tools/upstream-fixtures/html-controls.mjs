/*
 * Official JSXGraph 1.13.3 HTML control behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/html-controls.mjs
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
        const baseSnapshot = (element) => ({
            id: element.id,
            name: element.name,
            type: element.type,
            elementClass: element.elementClass,
            elType: element.elType,
            fixed: element.visProp.fixed,
            frozen: element.visProp.frozen,
            visible: element.visProp.visible,
            needsRegularUpdate: element.needsRegularUpdate,
            coordinates: element.coords.usrCoords.slice(),
            plaintext: element.plaintext,
            orgTextType: typeof element.orgText,
            orgText:
                typeof element.orgText === "function"
                    ? element.orgText()
                    : element.orgText
        });
        const captureError = (type, parents, attributes = {}) => {
            const board = createBoard();
            const before = board.objectsList.length;
            try {
                const value = board.create(type, parents, {
                    id: `failure-${type}`,
                    name: "",
                    ...attributes
                });
                return {
                    threw: false,
                    type: value?.type ?? null,
                    elType: value?.elType ?? null,
                    plaintext: value?.plaintext ?? null,
                    orgTextType: typeof value?.orgText,
                    orgText:
                        typeof value?.orgText === "function"
                            ? value.orgText()
                            : value?.orgText ?? null,
                    nodeText:
                        value?.rendNodeButton?.textContent ??
                        value?.rendNodeLabel?.textContent ??
                        null,
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

        const board = createBoard();
        let dynamicLabel = "dynamic-1";
        let functionClicks = 0;
        const point = board.create("point", [1, 1], {
            id: "control-point",
            name: ""
        });
        const functionButton = board.create(
            "button",
            [
                -6,
                4,
                () => dynamicLabel,
                () => {
                    functionClicks += 1;
                    point.moveTo([point.X() + 1, point.Y()]);
                }
            ],
            {id: "function-button", name: "", disabled: true}
        );
        const stringButton = board.create(
            "button",
            [
                -6,
                3,
                "String handler",
                "$('control-point').Y = $('control-point').Y() + 2;"
            ],
            {id: "string-button", name: ""}
        );
        const checkbox = board.create(
            "checkbox",
            [-6, 2, () => dynamicLabel],
            {id: "checkbox", name: "", checked: true, disabled: true}
        );
        const input = board.create(
            "input",
            [-6, 1, "initial", () => dynamicLabel],
            {id: "input", name: "", maxlength: 7, disabled: true}
        );
        const slider = board.create(
            "htmlslider",
            [[-6, 0], [-2, 1.5, 8]],
            {
                id: "htmlslider",
                name: "alpha",
                step: 0.25,
                widthRange: 140,
                widthOut: 48,
                withLabel: true
            }
        );
        board.update();

        const snapshot = () => ({
            button: {
                ...baseSnapshot(functionButton),
                disabled: functionButton.rendNodeButton.disabled,
                nodeText: functionButton.rendNodeButton.textContent,
                hasHandler: typeof functionButton._handler === "function"
            },
            stringButton: {
                ...baseSnapshot(stringButton),
                disabled: stringButton.rendNodeButton.disabled,
                nodeText: stringButton.rendNodeButton.textContent,
                hasHandler: typeof stringButton._handler === "function"
            },
            checkbox: {
                ...baseSnapshot(checkbox),
                disabled: checkbox.rendNodeCheckbox.disabled,
                checked: checkbox.rendNodeCheckbox.checked,
                value: checkbox.Value(),
                nodeLabel: checkbox.rendNodeLabel.textContent
            },
            input: {
                ...baseSnapshot(input),
                disabled: input.rendNodeInput.disabled,
                maxlength: input.rendNodeInput.maxLength,
                nodeValue: input.rendNodeInput.value,
                value: input.Value(),
                nodeLabel: input.rendNodeLabel.textContent
            },
            slider: {
                ...baseSnapshot(slider),
                min: slider.rendNodeRange.min,
                max: slider.rendNodeRange.max,
                step: slider.rendNodeRange.step,
                rangeValue: slider.rendNodeRange.value,
                outputValue: slider.rendNodeOut.value,
                value: slider.Value(),
                label: slider.rendNodeLabel.textContent,
                rangeWidth: slider.rendNodeRange.style.width,
                outputWidth: slider.rendNodeOut.style.width
            },
            point: [point.X(), point.Y()],
            functionClicks
        });

        const initial = snapshot();
        dynamicLabel = "dynamic-2";
        board.update();
        const dynamic = snapshot();

        functionButton.rendNodeButton.disabled = false;
        functionButton.rendNodeButton.dispatchEvent(
            new MouseEvent("click", {bubbles: true})
        );
        stringButton.rendNodeButton.dispatchEvent(
            new MouseEvent("click", {bubbles: true})
        );
        checkbox.rendNodeCheckbox.disabled = false;
        checkbox.rendNodeCheckbox.checked = false;
        checkbox.rendNodeCheckbox.dispatchEvent(
            new Event("change", {bubbles: true})
        );
        input.rendNodeInput.disabled = false;
        input.rendNodeInput.value = "changed";
        input.rendNodeInput.dispatchEvent(
            new Event("input", {bubbles: true})
        );
        input.set("set-value");
        slider.rendNodeRange.value = "4.25";
        slider.rendNodeRange.dispatchEvent(
            new Event("input", {bubbles: true})
        );
        const interacted = snapshot();

        return {
            version: JXG.version,
            initial,
            dynamic,
            interacted,
            failures: {
                buttonMissingLabel: captureError("button", [0, 0]),
                checkboxMissingLabel: captureError("checkbox", [0, 0]),
                inputMissingLabel: captureError("input", [0, 0, "value"]),
                sliderMissingRange: captureError("htmlslider", [[0, 0]]),
                sliderInvalidPosition: captureError(
                    "htmlslider",
                    ["bad", [0, 1, 2]]
                ),
                sliderInvalidRange: captureError(
                    "htmlslider",
                    [[0, 0], "bad"]
                )
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
