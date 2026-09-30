/*
 * Official JSXGraph 1.13.3 Type utility fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/type-utils.mjs
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

const shared = {value: 1};

function unique(values) {
    const input = values.slice();
    const result = JXG.uniqueArray(input);
    return {input, result};
}

function remove(values, target) {
    const input = values.slice();
    const result = JXG.removeElementFromArray(input, target);
    return {input, result};
}

function selfConcat(values) {
    const input = values.slice();
    const result = JXG.concat(input, input);
    return {input, result};
}

function cloneUtilities() {
    const prototype = {base: 1};
    const cloned = JXG.clone(prototype);
    const nested = {x: 1};
    const callable = JXG.cloneAndCopy(
        prototype,
        {
            value: 2,
            nested
        }
    );

    return {
        clone: cloned,
        cloneKeys: Object.keys(cloned),
        clonePrototypeSame: cloned.prototype === prototype,
        callableType: typeof callable,
        callableResultType: typeof callable(),
        callableKeys: Object.keys(callable),
        callablePrototypeSame: callable.prototype === prototype,
        callableNestedSame: callable.nested === nested,
        callableValues: {
            value: callable.value,
            nested: callable.nested
        }
    };
}

function evaluateUtilities() {
    const returnedArray = [5, () => 6];
    const input = [
        1,
        () => 2,
        [
            () => 3,
            [() => 4]
        ],
        () => returnedArray
    ];
    const evaluated = JXG.evaluate(input);
    let thrown;
    try {
        JXG.evaluate(() => {
            throw new Error("evaluation failed");
        });
    } catch (error) {
        thrown = {
            name: error.name,
            message: error.message
        };
    }

    return {
        scalar: JXG.evaluate("value"),
        array: evaluated,
        arrayCopied: evaluated !== input,
        nestedCopied: evaluated[2] !== input[2],
        functionArraySame: evaluated[3] === returnedArray,
        functionArrayNestedType: typeof evaluated[3][1],
        thrown
    };
}

function jsonUtilities() {
    const ordered = {};
    ordered.beta = 1;
    ordered["10"] = 2;
    ordered["2"] = 3;
    ordered.alpha = 4;
    ordered.omitted = undefined;
    ordered.callback = () => 5;

    const topUndefined = JXG.toJSON(undefined);
    const topFunction = JXG.toJSON(() => 1);

    return {
        standard: JXG.toJSON({
            text: "a'b\"c\\d\nx",
            number: 1.25,
            exponent: 1e-7,
            nil: null,
            array: [
                undefined,
                () => 1,
                Number.NaN,
                Number.POSITIVE_INFINITY,
                -0
            ],
            ordered
        }),
        topUndefined: {
            type: typeof topUndefined,
            value: String(topUndefined)
        },
        topFunction: {
            type: typeof topFunction,
            value: String(topFunction)
        },
        noquote: JXG.toJSON({
            text: "a'b\"c\\d\nx",
            nil: null,
            array: [undefined, () => 1, Number.NaN],
            ordered
        }, true),
        noquoteUndefined: JXG.toJSON(undefined, true),
        noquoteFunction: JXG.toJSON(() => 1, true)
    };
}

function cssUtilities() {
    const parseCases = [
        null,
        1,
        "color:blue; background-color:yellow",
        " color : blue ; ",
        "",
        "a:1;;b:2",
        "a:1:2",
        "a:",
        "a",
        "a:\\n",
        "a:\\\"x\\\"",
        "10:c;2:b;a:x",
        "\u00A0a\u00A0:\u00A0b\u00A0;\u00A0"
    ].map((value) => {
        try {
            return {
                value,
                result: JXG.cssParse(value)
            };
        } catch (error) {
            return {
                value,
                error: error.name
            };
        }
    });
    const css2jsCases = [
        "",
        "color:blue; background-color:yellow",
        "a:1;;b:2",
        "a:1:2",
        "a:",
        "a",
        "-webkit-transform: x",
        "--custom-name:v",
        "A-B:c"
    ].map((value) => {
        try {
            return {
                value,
                result: JXG.css2js(value)
            };
        } catch (error) {
            return {
                value,
                error: error.name
            };
        }
    });

    return {
        parse: parseCases,
        css2js: css2jsCases,
        stringify: [
            null,
            [],
            {},
            {
                color: "red",
                width: 2,
                skip: true,
                nan: Number.NaN,
                inf: Number.POSITIVE_INFINITY,
                nil: null,
                nested: {x: 1}
            },
            {
                10: "c",
                2: "b",
                a: "x"
            }
        ].map((value) => JXG.cssStringify(value))
    };
}

function evalSliderUtilities() {
    const sliderLike = {
        type: JXG.OBJECT_TYPE_GLIDER,
        Value: () => 7
    };
    const wrongType = {
        type: JXG.OBJECT_TYPE_POINT,
        Value: () => 8
    };
    const missingValue = {
        type: JXG.OBJECT_TYPE_GLIDER,
        Value: 9
    };

    return {
        evaluated: JXG.evalSlider(sliderLike),
        scalar: JXG.evalSlider(3),
        nullValue: JXG.evalSlider(null),
        wrongTypeSame: JXG.evalSlider(wrongType) === wrongType,
        missingValueSame: JXG.evalSlider(missingValue) === missingValue
    };
}

function copyAttributesUtilities() {
    const options = {
        elements: {
            Visible: true,
            StrokeColor: "base",
            nested: {
                base: 1,
                replace: "elements"
            },
            label: {
                fontSize: 10,
                fromElements: true
            }
        },
        layer: {
            line: 5,
            board: 0,
            custom: 7
        },
        label: {
            fontSize: 12,
            color: "global",
            globalOnly: true
        },
        line: {
            strokeColor: "line",
            nested: {
                line: 2,
                replace: "line"
            },
            label: {
                fontSize: 14,
                lineOnly: true
            },
            point1: {
                size: 3,
                nested: {
                    point: 4
                },
                label: {
                    fontSize: 16,
                    pointOnly: true
                }
            }
        },
        board: {
            showNavigation: true,
            label: {
                ignored: true
            }
        },
        custom: {
            customDefault: 1,
            label: {
                customLabel: true
            }
        }
    };
    const attributes = {
        StrokeColor: "user",
        NESTED: {
            User: 3,
            Replace: "user"
        },
        LABEL: {
            color: "user",
            userOnly: true
        },
        Point1: {
            SIZE: 8,
            NESTED: {
                UserPoint: 5
            },
            LABEL: {
                color: "point-user"
            }
        }
    };

    return {
        line: JXG.copyAttributes(attributes, options, "line"),
        point1: JXG.copyAttributes(
            attributes,
            options,
            "line",
            "point1"
        ),
        board: JXG.copyAttributes(
            {ShowNavigation: false},
            options,
            "board"
        ),
        custom: JXG.copyAttributes(attributes, options, "custom"),
        missing: JXG.copyAttributes(attributes, options, "missing")
    };
}

function filterElementsUtilities() {
    const items = [
        {
            id: "a",
            rank: 1,
            Value: () => 5,
            visProp: {
                visible: true,
                color: () => "red"
            }
        },
        {
            id: "b",
            rank: 2,
            Value: () => 3,
            visProp: {
                visible: false,
                color: "blue"
            }
        },
        {
            id: "c",
            rank: 1,
            visProp: {
                visible: true
            }
        }
    ];
    const ids = (values) => values.map((value) => value.id);

    return {
        missing: ids(JXG.filterElements(items)),
        nullValue: ids(JXG.filterElements(items, null)),
        invalid: ids(JXG.filterElements(items, "rank")),
        callback: ids(
            JXG.filterElements(items, (item) => item.rank === 1)
        ),
        directAndVisual: ids(
            JXG.filterElements(items, {
                rank: 1,
                visible: true
            })
        ),
        dynamicDirect: ids(
            JXG.filterElements(items, {
                Value: (value) => value > 4
            })
        ),
        dynamicVisual: ids(
            JXG.filterElements(items, {
                color: "red"
            })
        )
    };
}

function bindUtilities() {
    const owner = {base: 10};
    const bound = JXG.bind(function (left, right) {
        return this.base + left + right;
    }, owner);
    const initial = bound(2, 3);
    owner.base = 20;

    return {
        initial,
        updatedOwner: bound(2, 3),
        wrapperArity: bound.length
    };
}

function typedPredicateUtilities() {
    const point = {
        elementClass: JXG.OBJECT_CLASS_POINT,
        type: JXG.OBJECT_TYPE_POINT
    };
    const point3D = {
        elementClass: JXG.OBJECT_CLASS_3D,
        type: JXG.OBJECT_TYPE_POINT3D
    };
    const transformation = {
        type: JXG.OBJECT_TYPE_TRANSFORMATION
    };
    const lookupBoard = {
        objects: {
            point
        },
        elementsByName: {
            A: point
        },
        groups: {
            group: {}
        },
        select(value) {
            if (typeof value === "string") {
                return this.objects[value] ||
                    this.elementsByName[value] ||
                    undefined;
            }
            return value;
        }
    };
    const validBoard = {
        BOARD_MODE_NONE: 0,
        objects: {},
        jc: {},
        update() {},
        containerObj: {},
        id: "board"
    };

    return {
        isBoard: [
            validBoard,
            {
                ...validBoard,
                containerObj: null
            },
            {
                ...validBoard,
                id: 1
            },
            null
        ].map((value) => JXG.isBoard(value)),
        isId: [
            JXG.isId(lookupBoard, "point"),
            JXG.isId(lookupBoard, ""),
            JXG.isId(lookupBoard, point)
        ],
        isName: [
            JXG.isName(lookupBoard, "A"),
            JXG.isName(lookupBoard, "point"),
            JXG.isName(lookupBoard, point)
        ],
        isGroup: [
            JXG.isGroup(lookupBoard, "group"),
            JXG.isGroup(lookupBoard, ""),
            JXG.isGroup(lookupBoard, point)
        ],
        isPoint: [
            point,
            point3D,
            {
                elementClass: JXG.OBJECT_CLASS_POINT
            },
            null
        ].map((value) => JXG.isPoint(value)),
        isPoint3D: [
            point3D,
            point,
            {
                type: JXG.OBJECT_TYPE_POINT3D
            },
            null
        ].map((value) => JXG.isPoint3D(value)),
        isPointType: [
            [],
            [1],
            () => [],
            () => [1, 2],
            "point",
            "A",
            point,
            "missing"
        ].map((value) => JXG.isPointType(lookupBoard, value)),
        isPointType3D: [
            [],
            [1, 2],
            [1, 2, 3],
            () => [1, 2],
            () => [1, 2, 3],
            point3D,
            point,
            "missing"
        ].map((value) => JXG.isPointType3D(lookupBoard, value)),
        isTransformationOrArray: [
            transformation,
            [transformation],
            [transformation, {}],
            [[transformation]],
            [],
            [{}],
            null
        ].map((value) => JXG.isTransformationOrArray(value))
    };
}

function methodMapUtilities() {
    function SuperClass() {}
    SuperClass.prototype.superMethod = function () {
        return "super";
    };
    SuperClass.prototype.sharedValue = "super";
    SuperClass.prototype.methodMap = {
        Base: "base",
        Shared: "super",
        nested: {
            fromSuper: true
        }
    };

    function SubClass() {}
    SubClass.prototype.subMethod = function () {
        return "sub";
    };
    SubClass.prototype.sharedValue = "sub";
    SubClass.prototype.methodMap = {
        Own: "own",
        Shared: "sub",
        nested: {
            fromSub: true
        }
    };
    JXG.copyPrototypeMethods(
        SubClass,
        SuperClass,
        "superConstructor"
    );

    function DirectClass() {}
    DirectClass.prototype.methodMap = {
        A: "a",
        nested: {
            left: 1
        }
    };
    const directBefore = DirectClass.prototype.methodMap;
    JXG.copyMethodMap(DirectClass, {
        B: "b",
        nested: {
            right: 2
        }
    });

    const objectExtension = Object.create(SubClass.prototype);
    const objectExtensionBefore =
        Object.prototype.hasOwnProperty.call(
            objectExtension,
            "methodMap"
        );
    JXG.extendInstanceMethodMap(objectExtension, {
        Instance: "instance",
        nested: {
            fromInstance: true
        }
    });

    const stringExtension = Object.create(SubClass.prototype);
    JXG.extendInstanceMethodMap(
        stringExtension,
        "alias",
        "target"
    );

    const ignoredExtension = Object.create(SubClass.prototype);
    JXG.extendInstanceMethodMap(
        ignoredExtension,
        "alias",
        undefined
    );

    return {
        copiedPrototype: {
            keys: Object.keys(SubClass.prototype),
            constructorSame:
                SubClass.prototype.superConstructor === SuperClass,
            superMethodSame:
                SubClass.prototype.superMethod ===
                    SuperClass.prototype.superMethod,
            sharedValue: SubClass.prototype.sharedValue,
            methodMap: SubClass.prototype.methodMap
        },
        directCopy: {
            replaced: DirectClass.prototype.methodMap !== directBefore,
            value: DirectClass.prototype.methodMap
        },
        objectExtension: {
            ownBefore: objectExtensionBefore,
            ownAfter: Object.prototype.hasOwnProperty.call(
                objectExtension,
                "methodMap"
            ),
            value: objectExtension.methodMap,
            prototypeUnchanged: SubClass.prototype.methodMap
        },
        stringExtension: {
            value: stringExtension.methodMap,
            prototypeUnchanged: SubClass.prototype.methodMap
        },
        ignoredExtension: {
            value: ignoredExtension.methodMap,
            prototypeUnchanged: SubClass.prototype.methodMap
        }
    };
}

function cloneObjectUtilities() {
    const coordinates = {
        usrCoords: [1, 2, 3]
    };
    const board = {
        options: {
            layer: {
                trace: 7
            }
        }
    };
    const element = {
        id: "point",
        numTraces: 2,
        coords: coordinates,
        board,
        elementClass: JXG.OBJECT_CLASS_POINT,
        visProp: {
            visible: () => true,
            strokecolor: () => "base",
            strokewidth: 2,
            traceattributes: {
                strokecolor: () => "trace",
                fillcolor: "fill",
                ariaLabel: "skip",
                highlightStrokeColor: "skip",
                attractorDistance: 5,
                label: {
                    visible: true
                },
                needsRegularUpdate: true,
                infoboxDigits: 4,
                tabindex: 8
            }
        },
        eval(value) {
            return typeof value === "function" ? value() : value;
        },
        evalVisProp(name) {
            return this.eval(this.visProp[name]);
        }
    };

    const clone = JXG.getCloneObject(element);
    const clearTarget = {
        visPropOld: {
            stale: true
        }
    };
    const clearResult = JXG.clearVisPropOld(clearTarget);

    return {
        clone: {
            id: clone.id,
            sourceNumTraces: element.numTraces,
            coordsSame: clone.coords === coordinates,
            boardSame: clone.board === board,
            elementClass: clone.elementClass,
            visProp: clone.visProp,
            visPropOld: clone.visPropOld,
            visPropCalc: clone.visPropCalc,
            evalStrokeColor: clone.evalVisProp("strokecolor"),
            evalIdentity: clone.eval("value")
        },
        clear: {
            sameObject: clearResult === clearTarget,
            visPropOld: clearTarget.visPropOld
        }
    };
}

const result = {
    version: JXG.version,
    cloneUtilities: cloneUtilities(),
    evaluateUtilities: evaluateUtilities(),
    jsonUtilities: jsonUtilities(),
    cssUtilities: cssUtilities(),
    evalSliderUtilities: evalSliderUtilities(),
    copyAttributesUtilities: copyAttributesUtilities(),
    filterElementsUtilities: filterElementsUtilities(),
    bindUtilities: bindUtilities(),
    typedPredicateUtilities: typedPredicateUtilities(),
    methodMapUtilities: methodMapUtilities(),
    cloneObjectUtilities: cloneObjectUtilities(),
    predicates: {
        isString: [
            undefined,
            null,
            "",
            "3",
            2
        ].map((value) => JXG.isString(value)),
        isNumber: [
            undefined,
            null,
            "",
            "3",
            "3.0",
            "NaN",
            "1e-7",
            "0.000001",
            "1e+21",
            Number.NaN,
            2
        ].map((value) => ({
            default: JXG.isNumber(value),
            string: JXG.isNumber(value, true),
            finite: JXG.isNumber(value, true, false)
        })),
        isArray: [
            [],
            [1],
            {},
            null,
            "x"
        ].map((value) => JXG.isArray(value)),
        isObject: [
            [],
            {},
            null,
            "x",
            1
        ].map((value) => JXG.isObject(value)),
        isFunction: [
            () => 1,
            function () {},
            {},
            null,
            "x"
        ].map((value) => JXG.isFunction(value)),
        exists: [
            undefined,
            null,
            "",
            false,
            0
        ].map((value) => ({
            default: JXG.exists(value),
            nonempty: JXG.exists(value, true)
        })),
        defaults: [
            JXG.def(undefined, "default"),
            JXG.def(null, "default"),
            JXG.def(false, "default"),
            JXG.def("", "default")
        ]
    },
    str2Bool: [
        undefined,
        null,
        true,
        false,
        "true",
        "TRUE",
        "false",
        " true ",
        1,
        {}
    ].map((value) => JXG.str2Bool(value)),
    cmpArrays: {
        numeric: JXG.cmpArrays([1, -0], [1.0, 0]),
        nested: JXG.cmpArrays(
            [1, ["x", true]],
            [1.0, ["x", true]]
        ),
        nan: JXG.cmpArrays([Number.NaN], [Number.NaN]),
        sharedObject: JXG.cmpArrays([shared], [shared]),
        distinctObject: JXG.cmpArrays(
            [{value: 1}],
            [{value: 1}]
        )
    },
    uniqueArray: {
        scalar: unique([1, 2, 1.0, "x", "x"]),
        nested: unique([[1], [2], [1.0]]),
        missing: unique([null, undefined, "x"]),
        sharedObject: unique([shared, shared]),
        nan: unique([Number.NaN, Number.NaN])
    },
    toUniqueArrayFloat: {
        sample: JXG.toUniqueArrayFloat(
            [2.3, 4, Math.PI, 2.300001, Math.PI + 0.000000001],
            0.00001
        ),
        strictEps: JXG.toUniqueArrayFloat([1, 1.1, 1.2], 0.1),
        infinity: JXG.toUniqueArrayFloat(
            [Infinity, 1, Infinity, -Infinity],
            0.1
        )
    },
    listOperations: {
        indexOf: {
            number: JXG.indexOf([1], 1.0),
            nan: JXG.indexOf([Number.NaN], Number.NaN),
            sharedObject: JXG.indexOf([shared], shared),
            distinctObject: JXG.indexOf(
                [{value: 1}],
                {value: 1}
            ),
            property: JXG.indexOf(
                [{id: 1}, {id: 2}],
                2,
                "id"
            )
        },
        isInArray: [
            JXG.isInArray([1, 2], 2),
            JXG.isInArray([Number.NaN], Number.NaN)
        ],
        removeScalar: remove(
            [1, 2, 1.0, Number.NaN, shared],
            1.0
        ),
        removeObject: remove(
            [shared, {value: 1}, shared],
            shared
        ),
        concat: JXG.concat([1], [2, 3]),
        selfConcat: selfConcat([1, 2])
    },
    objectAndArrayUtilities: {
        eliminateDuplicates: JXG.eliminateDuplicates([
            "10",
            "2",
            "beta",
            1,
            "1",
            "01",
            -0,
            Number.NaN,
            Number.POSITIVE_INFINITY,
            "__proto__",
            "beta"
        ]),
        keys: (() => {
            const value = {};
            value.beta = 1;
            value["10"] = 2;
            value["2"] = 3;
            value.alpha = 4;
            return JXG.keys(value, true);
        })(),
        isInObject: [
            JXG.isInObject({number: 1}, 1.0),
            JXG.isInObject({object: shared}, shared),
            JXG.isInObject({object: {value: 1}}, {value: 1}),
            JXG.isInObject({number: Number.NaN}, Number.NaN)
        ],
        swap: (() => {
            const value = [1, "x", true];
            return {
                result: JXG.swap(value, 0, 2),
                sameReference: JXG.swap(value, 0, 0) === value
            };
        })(),
        coordsArrayToMatrix: {
            matrix: JXG.coordsArrayToMatrix(
                [
                    {usrCoords: [1, 2, 3]},
                    {usrCoords: [1, -1, 4]}
                ],
                false
            ),
            split: JXG.coordsArrayToMatrix(
                [
                    {usrCoords: [1, 2, 3]},
                    {usrCoords: [1, -1, 4]}
                ],
                true
            )
        },
        isEmpty: [
            {},
            [],
            {value: 1},
            [1],
            ""
        ].map((value) => JXG.isEmpty(value))
    },
    copyUtilities: (() => {
        const base = {
            Alpha: 1,
            nested: {
                left: 1,
                shared: {x: 1}
            },
            array: [{a: 1}, 2],
            scalar: "base"
        };
        const secondary = {
            nested: {
                right: 2,
                shared: {y: 2}
            },
            array: [{b: 3}],
            scalar: "override",
            added: true
        };
        const collision = {};
        collision.firstArrow = 1;
        collision.firstarrow = 2;
        const callback = () => 1;
        const handle = {
            board: {},
            id: "point"
        };
        const withReferences = JXG.deepCopy({
            callback,
            handle
        });

        return {
            copy: JXG.deepCopy(base),
            merged: JXG.deepCopy(base, secondary),
            lowerMerged: JXG.deepCopy(base, secondary, true),
            arraySecondaryIgnored: JXG.deepCopy(
                [1, {A: 2}],
                {x: 3},
                true
            ),
            primitiveSecondaryIgnored: JXG.deepCopy(
                "x",
                {a: 1},
                true
            ),
            lowerCollision: JXG.keysToLowerCase(collision),
            lowerNested: JXG.keysToLowerCase({
                Outer: {Inner: 1},
                Items: [{KeepCase: 2}],
                Zed: 3
            }),
            rootFunctionSame: JXG.deepCopy(callback) === callback,
            callbackSame: withReferences.callback === callback,
            handleValue: withReferences.handle
        };
    })(),
    mergeUtilities: (() => {
        const mergeTarget = {
            board: {
                showNavigation: true,
                keep: 1
            },
            values: [
                {x: 1},
                {keep: true},
                3,
                "tail"
            ],
            lastArrow: false
        };
        const mergeResult = JXG.merge(mergeTarget, {
            board: {
                showNavigation: false,
                showInfobox: true
            },
            values: [
                {y: 2},
                {z: 3},
                4
            ],
            lastArrow: {type: 7},
            added: "new"
        });
        const nullTarget = {
            nested: {keep: 1}
        };
        JXG.merge(nullTarget, {nested: null});

        const handle = {
            board: true,
            id: "point"
        };
        const array = [3];
        const attrTarget = {
            Mixed: {Left: 1},
            mixed: {right: 2},
            shadow: {
                enabled: true,
                blur: 3
            },
            draft: false,
            anchor: {local: true},
            array: [1, 2],
            keepUndefined: "base"
        };
        const attrReturn = JXG.mergeAttr(
            attrTarget,
            {
                Mixed: {Added: 3},
                shadow: {
                    blur: 5,
                    Color: "black"
                },
                draft: {StrokeWidth: 4},
                anchor: handle,
                array,
                keepUndefined: undefined
            },
            false,
            false
        );
        const ignoredUndefined = {
            keepUndefined: "base"
        };
        JXG.mergeAttr(
            ignoredUndefined,
            {keepUndefined: undefined},
            true,
            true
        );

        return {
            merge: mergeResult,
            mergeReturnsTarget: mergeResult === mergeTarget,
            booleanTarget: JXG.merge(false, {type: 7}),
            nullSourceValue: nullTarget,
            mergeAttr: attrTarget,
            mergeAttrReturnType: typeof attrReturn,
            mergeAttrAlwaysLowers: (
                Object.hasOwn(attrTarget, "mixed") &&
                !Object.hasOwn(attrTarget, "Mixed")
            ),
            mergeAttrHandleSame: attrTarget.anchor === handle,
            mergeAttrArraySame: attrTarget.array === array,
            undefinedApplied: {
                has: Object.hasOwn(attrTarget, "keepundefined"),
                type: typeof attrTarget.keepundefined
            },
            undefinedIgnored: ignoredUndefined
        };
    })(),
    parseNumber: [
        "50%",
        " 50 % ",
        "2fr",
        " 2 fr ",
        "12px",
        " 12 px ",
        "1\u00A0%\u00A02",
        "1e2px",
        "foo",
        "3.5xyz",
        4,
        null
    ].map((value) => ({
        value,
        plain: JXG.parseNumber(value, 200),
        scale: JXG.parseNumber(value, 200, 2),
        transform: JXG.parseNumber(
            value,
            200,
            (number) => number + 5
        )
    })),
    numberFormatting: {
        adjust: [
            3.14159,
            -3.14159,
            1.005,
            -0.000001,
            Number.POSITIVE_INFINITY,
            Number.NaN
        ].flatMap((value) => (
            [0, -2, 2].map((exponent) => ({
                value,
                exponent,
                round: JXG._round10(value, exponent),
                floor: JXG._floor10(value, exponent),
                ceil: JXG._ceil10(value, exponent)
            }))
        )),
        toFixed: [
            3.14159,
            -3.14159,
            1.005,
            -0.000001,
            Number.POSITIVE_INFINITY,
            Number.NaN
        ].map((value) => ({
            value,
            digits0: JXG.toFixed(value, 0),
            digits2: JXG.toFixed(value, 2)
        })),
        autoDigits: [
            1.2345,
            0.012345,
            0.0012345,
            0.000012345,
            -0.000001,
            Number.POSITIVE_INFINITY,
            Number.NaN
        ].map((value) => ({
            value,
            result: JXG.autoDigits(value),
            resultType: typeof JXG.autoDigits(value)
        })),
        trunc: [
            [3.14159],
            [3.14159, 2],
            [-3.14159, 2]
        ].map(([value, decimalPlaces]) => ({
            value,
            decimalPlaces,
            result: JXG.trunc(value, decimalPlaces)
        })),
        invalidDigits: [-1, 101].map((digits) => {
            try {
                return {
                    digits,
                    result: JXG.toFixed(1, digits)
                };
            } catch (error) {
                return {
                    digits,
                    error: error.name
                };
            }
        })
    },
    parsePosition: [
        "",
        "left top",
        "bottom right",
        "left,right,center",
        "left\tright",
        "  right   top  "
    ].map((value) => ({
        value,
        result: JXG.parsePosition(value)
    })),
    strings: {
        escapeHTML: JXG.escapeHTML("&<tag>\""),
        unescapeHTML: JXG.unescapeHTML(
            "<b>A&amp;B</b>&lt;x&gt;<broken"
        ),
        sanitizeHTML: [
            "<b>x</b>",
            "A&B",
            ""
        ].map((value) => JXG.sanitizeHTML(value, false)),
        capitalize: ["", "hELLO", "ßABC"].map(
            (value) => JXG.capitalize(value)
        ),
        trimNumber: [
            "0001200",
            "0.00100",
            "1000",
            "0",
            "0000",
            "-00120",
            "001,2300"
        ].map((value) => ({
            value,
            result: JXG.trimNumber(value)
        })),
        trim: [
            "  x\t",
            "\u00A0x\u00A0",
            "\uFEFFx\uFEFF",
            "\u2003x\u2003"
        ].map((value) => ({
            value,
            result: JXG.trim(value)
        }))
    },
    toFraction: [
        0,
        3,
        1 / 3,
        10 / 3,
        -10 / 3,
        2.0001
    ].map((value) => ({
        value,
        plain: JXG.toFraction(value, false),
        tex: JXG.toFraction(value, true)
    })),
    stack2jsxgraph: [
        " %pi + %e ",
        "[%pi*x, %phi, %gamma]",
        "[]",
        "[a,]",
        "[ a , b ]",
        "[a\u00A0,\u00A0b]",
        "x,%pi"
    ].map((value) => ({
        value,
        result: JXG.stack2jsxgraph(value)
    }))
};

console.log(
    JSON.stringify(
        result,
        (key, value) => (
            typeof value === "number" && !Number.isFinite(value)
                ? String(value)
                : value
        ),
        2
    )
);
