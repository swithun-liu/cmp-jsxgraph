/*
 * Official JSXGraph 1.13.3 relative-coordinate and coordinate-result fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/relative-coordinate-constraints.mjs
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
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const vector = (values) => Array.from(values, (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (!Number.isFinite(value)) {
                return value > 0 ? "Infinity" : "-Infinity";
            }
            return value;
        });
        const snapshot = (element) => ({
            id: element.id,
            coordinates: vector(element.coords.usrCoords),
            screen: vector(element.coords.scrCoords),
            relativeUser: element.relativeCoords
                ? vector(element.relativeCoords.usrCoords)
                : null,
            relativeScreen: element.relativeCoords
                ? vector(element.relativeCoords.scrCoords)
                : null,
            parents: [...element.parents],
            constrained: element.isConstrained,
            draggable: element.isDraggable
        });

        const anchor = board.create("point", [1, 2], {
            id: "anchor",
            name: "",
            withLabel: false
        });
        const relativePoint = board.create("point", [0.5, -1], {
            id: "relativePoint",
            name: "",
            withLabel: false,
            anchor
        });
        const relativeImage = board.create(
            "image",
            [
                "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
                [0.25, -0.5],
                [1, 1]
            ],
            {
                id: "relativeImage",
                name: "",
                anchor
            }
        );
        const relativeForeignObject = board.create(
            "foreignobject",
            ["<span>relative</span>", [-0.75, 0.25], [1, 1]],
            {
                id: "relativeForeignObject",
                name: "",
                anchor
            }
        );
        const relativeText = board.create(
            "text",
            [1, -2, "relative"],
            {
                id: "relativeText",
                name: "",
                anchor
            }
        );
        const relativeLabel = board.create(
            "text",
            [10, 20, "label"],
            {
                id: "relativeLabel",
                name: "",
                anchor,
                isLabel: true,
                offset: [5, 7]
            }
        );
        board.update();

        const relativeInitial = {
            anchor: snapshot(anchor),
            point: snapshot(relativePoint),
            image: snapshot(relativeImage),
            foreignObject: snapshot(relativeForeignObject),
            text: snapshot(relativeText),
            label: snapshot(relativeLabel),
            anchorChildren: Object.keys(anchor.childElements)
        };

        const returnedFromRelativeMove = relativePoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [4, 3]
        );
        const relativeAfterDirectMoveBeforeUpdate = snapshot(relativePoint);
        board.update();
        const relativeAfterDirectMove = snapshot(relativePoint);

        anchor.setPositionDirectly(JXG.COORDS_BY_USER, [-2, -1]);
        board.update();
        const relativeAfterAnchorMove = {
            anchor: snapshot(anchor),
            point: snapshot(relativePoint),
            image: snapshot(relativeImage),
            foreignObject: snapshot(relativeForeignObject),
            text: snapshot(relativeText),
            label: snapshot(relativeLabel)
        };

        const slider = board.create(
            "slider",
            [[-4, -4], [4, -4], [-2, 1, 6]],
            {
                id: "slider",
                name: "",
                withLabel: false,
                withTicks: false
            }
        );
        const sliderPoint = board.create(
            "point",
            [slider, () => slider.Value() + 1],
            {
                id: "sliderPoint",
                name: "",
                withLabel: false
            }
        );

        let coordsX = 2;
        let coordsY = -3;
        const coordsPoint = board.create(
            "point",
            [
                () => new JXG.Coords(
                    JXG.COORDS_BY_USER,
                    [coordsX, coordsY],
                    board
                )
            ],
            {
                id: "coordsPoint",
                name: "",
                withLabel: false
            }
        );
        const coordsObjectBefore = coordsPoint.coords;
        const constraintsInitial = {
            slider: snapshot(slider),
            sliderPoint: snapshot(sliderPoint),
            coordsPoint: snapshot(coordsPoint),
            coordsIdentityIsProviderResult:
                coordsPoint.coords === coordsObjectBefore
        };

        slider.setValue(5);
        coordsX = -6;
        coordsY = 4;
        board.update();
        const constraintsUpdated = {
            slider: snapshot(slider),
            sliderPoint: snapshot(sliderPoint),
            coordsPoint: snapshot(coordsPoint),
            coordsObjectWasReplaced:
                coordsPoint.coords !== coordsObjectBefore
        };

        return {
            version: JXG.version,
            relative: {
                initial: relativeInitial,
                moveReturnedSelf:
                    returnedFromRelativeMove === relativePoint,
                afterDirectMoveBeforeUpdate:
                    relativeAfterDirectMoveBeforeUpdate,
                afterDirectMove: relativeAfterDirectMove,
                afterAnchorMove: relativeAfterAnchorMove
            },
            constraints: {
                initial: constraintsInitial,
                updated: constraintsUpdated
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
