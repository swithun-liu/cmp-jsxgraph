/*
 * Official JSXGraph 1.13.3 environment utility fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/env.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(import.meta.url);
const JXG = require(
    resolve(
        repositoryRoot,
        "jsxgraph-debug-ui/src/androidMain/assets/" +
            "jsxgraphcore-1.13.3.js"
    )
);

function eventUtilities() {
    const touch = {
        touches: [
            {clientX: 3, clientY: 4},
            {clientX: 5, clientY: 6}
        ]
    };
    const touchEnd = {
        touches: [],
        changedTouches: [
            {clientX: 7, clientY: 8}
        ]
    };
    const pointer = {
        pointerId: 0,
        isPrimary: true,
        clientX: 9,
        clientY: 10
    };

    return {
        touch: {
            isTouch: JXG.isTouchEvent(touch),
            isPointer: JXG.isPointerEvent(touch),
            isMouse: JXG.isMouseEvent(touch),
            count: JXG.getNumberOfTouchPoints(touch),
            first: JXG.isFirstTouch(touch),
            firstPosition: JXG.getPosition(touch, 0, {}),
            firstAvailablePosition: JXG.getPosition(touch, -1, {})
        },
        touchEnd: {
            isTouch: JXG.isTouchEvent(touchEnd),
            count: JXG.getNumberOfTouchPoints(touchEnd),
            changedPosition: JXG.getPosition(touchEnd, 0, {})
        },
        pointer: {
            isTouch: JXG.isTouchEvent(pointer),
            isPointer: JXG.isPointerEvent(pointer),
            isMouse: JXG.isMouseEvent(pointer),
            count: JXG.getNumberOfTouchPoints(pointer),
            first: JXG.isFirstTouch(pointer),
            position: JXG.getPosition(pointer, undefined, {})
        },
        mouse: {
            isMouse: JXG.isMouseEvent({
                clientX: 11,
                clientY: 12
            }),
            position: JXG.getPosition({
                clientX: 11,
                clientY: 12
            }, undefined, {}),
            zeroXPosition: JXG.getPosition({
                clientX: 0,
                clientY: 12
            }, undefined, {})
        }
    };
}

function styleObject(style) {
    return {
        ownerDocument: {},
        style
    };
}

function cssUtilities() {
    const directStyle = styleObject({
        borderLeftWidth: "12.5px",
        invalidValue: "invalid"
    });

    return {
        translated: JXG.getCSSTransform(
            [1, 2],
            styleObject({
                transform: "translate(3px,4px)",
                zoom: "2"
            })
        ),
        translateX: JXG.getCSSTransform(
            [1, 2],
            styleObject({
                webkitTransform: "translateX(-2.5px)"
            })
        ),
        translateY: JXG.getCSSTransform(
            [1, 2],
            styleObject({
                MozTransform: "translateY(5px)"
            })
        ),
        matrix: JXG.getCSSTransformMatrix(
            styleObject({
                transform: "matrix(2,3,4,5,6,7)",
                zoom: "1.5"
            })
        ),
        scale: JXG.getCSSTransformMatrix(
            styleObject({
                transform: "scale(2,3)",
                zoom: "0.5"
            })
        ),
        singleScale: JXG.getCSSTransformMatrix(
            styleObject({
                transform: "scale(2)"
            })
        ),
        scaleX: JXG.getCSSTransformMatrix(
            styleObject({
                msTransform: "scaleX(4)"
            })
        ),
        scaleY: JXG.getCSSTransformMatrix(
            styleObject({
                oTransform: "scaleY(5)"
            })
        ),
        none: JXG.getCSSTransformMatrix(styleObject({})),
        style: {
            camel: JXG.getStyle(
                directStyle,
                "border-left-width"
            ),
            missingType: typeof JXG.getStyle(
                directStyle,
                "missing-value"
            ),
            integer: JXG.getProp(
                directStyle,
                "border-left-width"
            ),
            invalid: JXG.getProp(
                directStyle,
                "invalid-value"
            )
        }
    };
}

const result = {
    version: JXG.version,
    maxScreenCoord: JXG.maxScreenCoord,
    events: eventUtilities(),
    environment: {
        isBrowser: JXG.isBrowser,
        supportsES6: JXG.supportsES6(),
        supportsVML: JXG.supportsVML(),
        supportsSVG: JXG.supportsSVG(),
        supportsCanvas: JXG.supportsCanvas(),
        isNode: JXG.isNode(),
        isWebWorker: JXG.isWebWorker(),
        supportsPointerEvents: JXG.supportsPointerEvents(),
        isTouchDevice: JXG.isTouchDevice(),
        isAndroid: JXG.isAndroid(),
        isWebkitAndroid: JXG.isWebkitAndroid(),
        isApple: JXG.isApple(),
        isMozilla: JXG.isMozilla(),
        isFirefoxOS: JXG.isFirefoxOS(),
        isDesktop: JXG.isDesktop(),
        isMobile: JXG.isMobile(),
        ieVersion: JXG.ieVersion ?? null
    },
    dimensions: JXG.getDimensions(null),
    css: cssUtilities()
};

console.log(JSON.stringify(result, null, 2));
