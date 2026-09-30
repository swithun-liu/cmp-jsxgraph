/*
 * Official JSXGraph 1.13.3 Group behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/group.mjs
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
        const points = (board, prefix = "") => {
            const a = board.create(
                "point",
                [0, 0],
                {id: `${prefix}A`, name: ""}
            );
            const b = board.create(
                "point",
                [2, 0],
                {id: `${prefix}B`, name: ""}
            );
            const c = board.create(
                "point",
                [0, 2],
                {id: `${prefix}C`, name: ""}
            );
            return {a, b, c};
        };
        const pointSet = ({a, b, c}) => ({
            a: coordinates(a),
            b: coordinates(b),
            c: coordinates(c)
        });
        const centerSnapshot = (center) => {
            if (center?.coords) {
                return {
                    kind: "element",
                    id: center.id,
                    coordinates: coordinates(center)
                };
            }
            if (Array.isArray(center)) {
                return {kind: "coordinates", value: [...center]};
            }
            if (typeof center === "function") {
                return {kind: "function", value: center()};
            }
            return center;
        };
        const groupSnapshot = (group) => ({
            id: group.id,
            name: group.name,
            elType: group.elType,
            objectIds: Object.keys(group.objects),
            parents: [...group.parents],
            rotationCenter: centerSnapshot(group.rotationCenter),
            scaleCenter: centerSnapshot(group.scaleCenter),
            rotationPointIds: group.rotationPoints.map((point) => point.id),
            translationPointIds:
                group.translationPoints.map((point) => point.id),
            scalePointIds: group.scalePoints.map((point) => point.id),
            scaleDirections: {...group.scaleDirections},
            coordCache: Object.fromEntries(
                Object.entries(group.coords).map(([id, value]) => [
                    id,
                    value.usrCoords.slice()
                ])
            )
        });

        const creationBoard = createBoard();
        const creationPoints = points(creationBoard);
        const fixed = creationBoard.create(
            "point",
            [4, 4],
            {id: "fixed", name: "", fixed: true}
        );
        const group = creationBoard.create(
            "group",
            [creationPoints.a, creationPoints.b, creationPoints.c, fixed],
            {id: "group", name: "named", needsRegularUpdate: false}
        );
        const generated = creationBoard.create(
            "group",
            [creationPoints.a],
            {}
        );
        const duplicate = creationBoard.create(
            "group",
            [creationPoints.b],
            {id: "group", name: ""}
        );
        const creation = {
            group: groupSnapshot(group),
            generated: groupSnapshot(generated),
            duplicate: groupSnapshot(duplicate),
            boardGroupIds: Object.keys(creationBoard.groups),
            selectedGroupIsDuplicate:
                creationBoard.select("group") === duplicate,
            pointGroups: {
                a: [...creationPoints.a.groups],
                b: [...creationPoints.b.groups],
                c: [...creationPoints.c.groups],
                fixed: [...fixed.groups]
            },
            numObjects: creationBoard.numObjects
        };

        const translationBoard = createBoard();
        const translationPoints = points(translationBoard, "t");
        const translationGroup = translationBoard.create(
            "group",
            [
                translationPoints.a,
                translationPoints.b,
                translationPoints.c
            ],
            {id: "translation", name: ""}
        );
        translationPoints.a.setPosition(JXG.COORDS_BY_USER, [1, 1]);
        translationBoard.update(translationPoints.a);
        const translated = {
            points: pointSet(translationPoints),
            group: groupSnapshot(translationGroup)
        };

        const rotationBoard = createBoard();
        const rotationPoints = points(rotationBoard, "r");
        const rotationGroup = rotationBoard.create(
            "group",
            [rotationPoints.a, rotationPoints.b, rotationPoints.c],
            {id: "rotation", name: ""}
        );
        rotationGroup
            .setRotationCenter(rotationPoints.a)
            .setRotationPoints([rotationPoints.b]);
        rotationPoints.b.setPosition(JXG.COORDS_BY_USER, [0, 2]);
        rotationBoard.update(rotationPoints.b);
        const rotated = {
            points: pointSet(rotationPoints),
            group: groupSnapshot(rotationGroup)
        };

        const scaleBoard = createBoard();
        const scalePoints = points(scaleBoard, "s");
        const scaleGroup = scaleBoard.create(
            "group",
            [scalePoints.a, scalePoints.b, scalePoints.c],
            {id: "scale", name: ""}
        );
        scaleGroup
            .setScaleCenter(scalePoints.a)
            .setScalePoints([scalePoints.b], "x");
        scalePoints.b.setPosition(JXG.COORDS_BY_USER, [4, 0]);
        scaleBoard.update(scalePoints.b);
        const scaled = {
            points: pointSet(scalePoints),
            group: groupSnapshot(scaleGroup)
        };

        const membershipBoard = createBoard();
        const membershipPoints = points(membershipBoard, "m");
        const first = membershipBoard.create(
            "group",
            [membershipPoints.a],
            {id: "first", name: ""}
        );
        const second = membershipBoard.create(
            "group",
            [membershipPoints.b],
            {id: "second", name: ""}
        );
        first.addPoint(membershipPoints.c);
        first.addGroup(second);
        first.removePoint(membershipPoints.c);
        const afterRemove = {
            group: groupSnapshot(first),
            cStillListsGroup: membershipPoints.c.groups.includes(first.id)
        };
        first.ungroup();
        const afterUngroup = {
            group: groupSnapshot(first),
            pointGroupMembership: {
                a: membershipPoints.a.groups.includes(first.id),
                b: membershipPoints.b.groups.includes(first.id),
                c: membershipPoints.c.groups.includes(first.id)
            }
        };

        return {
            version: JXG.version,
            creation,
            translated,
            rotated,
            scaled,
            afterRemove,
            afterUngroup
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
