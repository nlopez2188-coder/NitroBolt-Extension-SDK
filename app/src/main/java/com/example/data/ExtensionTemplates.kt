package com.example.data

import com.example.model.BlockArgument
import com.example.model.BlockColorPreset
import com.example.model.BlockSwitch
import com.example.model.BlockSwitchInputMapping
import com.example.model.CustomShapeDefinition
import com.example.model.CustomTypeDefinition
import com.example.model.CustomTypeShape
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.ExtensionProject
import com.example.model.MenuItemEntry
import com.example.model.MenuMutatorState
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType

data class ExtensionTemplate(
    val title: String,
    val subtitle: String,
    val badge: String,
    val project: ExtensionProject
)

data class LegacySandboxedSample(
    val title: String,
    val description: String,
    val code: String
)

object ExtensionTemplates {

    val colorPresets = listOf(
        BlockColorPreset("Nitro Amber", "#FF8A00", "#DF7500", "#B85D00"),
        BlockColorPreset("Cyber Cyan", "#00A8CC", "#0089A7", "#006980"),
        BlockColorPreset("Bolt Violet", "#7C4DFF", "#6538E6", "#4D26BF"),
        BlockColorPreset("Emerald Lab", "#10B981", "#059669", "#047857"),
        BlockColorPreset("Crimson Core", "#EF4444", "#DC2626", "#B91C1C"),
        BlockColorPreset("Scratch Motion", "#4C97FF", "#4280D7", "#3373CC")
    )

    val turboModeProject = ExtensionProject(
        id = "turbomodeunsandboxed",
        name = "Turbo Mode",
        className = "TurboMode",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = true,
        color1Hex = "#FF8A00",
        color2Hex = "#DF7500",
        color3Hex = "#B85D00",
        blocks = listOf(
            ExtensionBlock(
                opcode = "set",
                blockType = ScratchBlockType.COMMAND,
                text = "set turbo mode to [ENABLED]",
                arguments = listOf(
                    BlockArgument(
                        name = "ENABLED",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "on",
                        menuName = "ENABLED_MENU"
                    )
                ),
                jsBody = "vm.setTurboMode(args.ENABLED === 'on');"
            ),
            ExtensionBlock(
                opcode = "isTurbo",
                blockType = ScratchBlockType.BOOLEAN,
                text = "is turbo mode on?",
                arguments = emptyList(),
                jsBody = "return Boolean(vm.runtime.turboMode);"
            )
        ),
        menus = listOf(
            ExtensionMenu(
                name = "ENABLED_MENU",
                acceptReporters = true,
                items = listOf("on", "off")
            )
        )
    )

    val helloWorldProject = ExtensionProject(
        id = "helloworldunsandboxed",
        name = "Unsandboxed Hello World",
        className = "HelloWorld",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#00A8CC",
        color2Hex = "#0089A7",
        color3Hex = "#006980",
        blocks = listOf(
            ExtensionBlock(
                opcode = "hello",
                blockType = ScratchBlockType.REPORTER,
                text = "Hello!",
                arguments = emptyList(),
                jsBody = "return 'World!';"
            )
        ),
        menus = emptyList()
    )

    val vmInspectorProject = ExtensionProject(
        id = "vminspector",
        name = "VM Sprite Studio",
        className = "VmInspector",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = true,
        color1Hex = "#7C4DFF",
        color2Hex = "#6538E6",
        color3Hex = "#4D26BF",
        blocks = listOf(
            ExtensionBlock(
                opcode = "getVar",
                blockType = ScratchBlockType.REPORTER,
                text = "get stage variable [NAME]",
                arguments = listOf(
                    BlockArgument(
                        name = "NAME",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "score"
                    )
                ),
                jsBody = "const target = vm.runtime.getTargetForStage();\nconst v = target ? target.lookupVariableByNameAndType(args.NAME, '') : null;\nreturn v ? v.value : '';"
            ),
            ExtensionBlock(
                opcode = "setSpritePos",
                blockType = ScratchBlockType.COMMAND,
                text = "teleport active sprite to x: [X] y: [Y]",
                arguments = listOf(
                    BlockArgument(
                        name = "X",
                        type = ScratchArgumentType.NUMBER,
                        defaultValue = "0"
                    ),
                    BlockArgument(
                        name = "Y",
                        type = ScratchArgumentType.NUMBER,
                        defaultValue = "0"
                    )
                ),
                jsBody = "const sprite = vm.editingTarget;\nif (sprite) sprite.setXY(Number(args.X), Number(args.Y));"
            ),
            ExtensionBlock(
                opcode = "spriteCount",
                blockType = ScratchBlockType.REPORTER,
                text = "total sprites in project",
                arguments = emptyList(),
                jsBody = "return vm.runtime.targets.filter(t => !t.isStage).length;"
            )
        ),
        menus = emptyList()
    )

    val sandboxedStarterProject = ExtensionProject(
        id = "sandboxedstarter",
        name = "Sandboxed Utilities",
        className = "SandboxedUtilities",
        executionMode = ExecutionMode.SANDBOXED,
        enforceUnsandboxedGuard = false,
        bindVmReference = false,
        color1Hex = "#10B981",
        color2Hex = "#059669",
        color3Hex = "#047857",
        blocks = listOf(
            ExtensionBlock(
                opcode = "formatGreeting",
                blockType = ScratchBlockType.REPORTER,
                text = "greet [NAME] with [SYMBOL]",
                arguments = listOf(
                    BlockArgument(
                        name = "NAME",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "NitroBolt"
                    ),
                    BlockArgument(
                        name = "SYMBOL",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "!",
                        menuName = "PUNCTUATION_MENU"
                    )
                ),
                jsBody = "return 'Hello, ' + String(args.NAME) + String(args.SYMBOL);"
            )
        ),
        menus = listOf(
            ExtensionMenu(
                name = "PUNCTUATION_MENU",
                acceptReporters = true,
                items = listOf("!", "?", "...")
            )
        )
    )

    val switchesProject = ExtensionProject(
        id = "switchesexample",
        name = "Switches",
        className = "Switches",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#4C97FF",
        color2Hex = "#4280D7",
        color3Hex = "#3373CC",
        blocks = listOf(
            ExtensionBlock(
                opcode = "abc",
                blockType = ScratchBlockType.COMMAND,
                text = "i can switch with def",
                switches = listOf(BlockSwitch(targetId = "def")),
                jsBody = "console.log('abc');"
            ),
            ExtensionBlock(
                opcode = "def",
                blockType = ScratchBlockType.COMMAND,
                text = "i can switch with abc",
                switches = listOf(BlockSwitch(targetId = "abc")),
                jsBody = "console.log('def');"
            ),
            ExtensionBlock(
                opcode = "sayForSecs",
                blockType = ScratchBlockType.COMMAND,
                text = "say [TEXT] for [SECS] secs",
                arguments = listOf(
                    BlockArgument(name = "TEXT", type = ScratchArgumentType.STRING, defaultValue = "Hello!"),
                    BlockArgument(name = "SECS", type = ScratchArgumentType.NUMBER, defaultValue = "2")
                ),
                switches = listOf(
                    BlockSwitch(
                        targetId = "say",
                        splitInputs = listOf("SECS")
                    ),
                    BlockSwitch(
                        targetId = "think",
                        inputs = listOf(BlockSwitchInputMapping("TEXT", "MSG")),
                        splitInputs = listOf("SECS")
                    )
                ),
                jsBody = "console.log(args.TEXT, args.SECS);"
            ),
            ExtensionBlock(
                opcode = "say",
                blockType = ScratchBlockType.COMMAND,
                text = "say [TEXT]",
                arguments = listOf(
                    BlockArgument(name = "TEXT", type = ScratchArgumentType.STRING, defaultValue = "Hello!")
                ),
                switches = listOf(BlockSwitch(targetId = "sayForSecs")),
                jsBody = "console.log(args.TEXT);"
            ),
            ExtensionBlock(
                opcode = "think",
                blockType = ScratchBlockType.COMMAND,
                text = "think [MSG]",
                arguments = listOf(
                    BlockArgument(name = "MSG", type = ScratchArgumentType.STRING, defaultValue = "Hmm...")
                ),
                switches = listOf(
                    BlockSwitch(
                        targetId = "say",
                        inputs = listOf(BlockSwitchInputMapping("MSG", "TEXT"))
                    )
                ),
                jsBody = "console.log(args.MSG);"
            )
        ),
        menus = emptyList()
    )

    val customTypesProject = ExtensionProject(
        id = "customTypesDemo",
        name = "Custom Types Demo",
        className = "CustomTypesDemo",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        useUnsandboxedRuntime = true,
        bindVmReference = false,
        color1Hex = "#9966FF",
        color2Hex = "#7C4DFF",
        color3Hex = "#5E35B1",
        constructorBody = "this.lastCounter = null;",
        customTypes = listOf(
            CustomTypeDefinition(
                typeName = "counter",
                className = "Counter",
                shape = CustomTypeShape.SQUARE,
                constructorBody = "this.value = Scratch.Cast.toNumber(value || 0);",
                castBody = "if (value instanceof Counter) return value;\nreturn new Counter(value);",
                toStringBody = "return `counter:\${this.value}`;",
                valueOfBody = "return this.value;",
                toJsonBody = "return { count: this.value };",
                fromJsonBody = "return new Counter(json.count);",
                visualReportBody = "return `<p>\${instance.value}</p><hr><p>that's like \${instance.value - 31} more than 31</p>`;",
                monitorContentBody = ""
            )
        ),
        blocks = listOf(
            ExtensionBlock(
                opcode = "newCounter",
                blockType = ScratchBlockType.REPORTER,
                text = "counter [VALUE]",
                outputType = "counter",
                arguments = listOf(
                    BlockArgument(name = "VALUE", type = ScratchArgumentType.NUMBER, defaultValue = "0")
                ),
                jsBody = "return new Counter(args.VALUE);"
            ),
            ExtensionBlock(
                opcode = "counterPlus",
                blockType = ScratchBlockType.REPORTER,
                text = "[COUNTER] plus [VALUE]",
                outputType = "counter",
                arguments = listOf(
                    BlockArgument(name = "COUNTER", customTypeName = "counter"),
                    BlockArgument(name = "VALUE", type = ScratchArgumentType.NUMBER, defaultValue = "1")
                ),
                jsBody = "return new Counter((args.COUNTER?.value || 0) + args.VALUE);"
            ),
            ExtensionBlock(
                opcode = "counterEquals",
                blockType = ScratchBlockType.BOOLEAN,
                text = "[COUNTER] = [OTHER]",
                arguments = listOf(
                    BlockArgument(name = "COUNTER", customTypeName = "counter"),
                    BlockArgument(name = "OTHER", customTypeName = "counter")
                ),
                jsBody = "return args.COUNTER?.value === args.OTHER?.value;"
            ),
            ExtensionBlock(
                opcode = "showCounter",
                blockType = ScratchBlockType.COMMAND,
                text = "remember counter [COUNTER]",
                arguments = listOf(
                    BlockArgument(name = "COUNTER", customTypeName = "counter")
                ),
                jsBody = "this.lastCounter = args.COUNTER;"
            ),
            ExtensionBlock(
                opcode = "reportCounter",
                blockType = ScratchBlockType.REPORTER,
                text = "last counter",
                outputType = "counter",
                arguments = emptyList(),
                jsBody = "return this.lastCounter;"
            )
        ),
        menus = emptyList()
    )

    val customShapesProject = ExtensionProject(
        id = "customShapesDemo",
        name = "Custom Shapes Demo",
        className = "CustomShapesDemo",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        useUnsandboxedRuntime = true,
        bindVmReference = false,
        color1Hex = "#9966FF",
        color2Hex = "#5C8DD6",
        color3Hex = "#4DC3CB",
        customShapes = listOf(
            CustomShapeDefinition(
                shapeName = "nbArrow",
                edgeWidthBody = "return Math.min(totalHeight / 2, 32);",
                leftEdgeBody = "steps.push(`l \${-w} \${-w}`);\nif (halfStraight > 0) steps.push('v', -halfStraight * 2);\nsteps.push(`l \${w} \${-w}`);",
                rightEdgeBody = "steps.push(`h \${w}`);\nsteps.push(`l \${-w} \${w}`);\nif (halfStraight > 0) steps.push('v', halfStraight * 2);\nsteps.push(`l \${w} \${w}`);",
                paddingBody = "return 25;"
            ),
            CustomShapeDefinition(
                shapeName = "nbArrowNoleft",
                edgeWidthBody = "return Math.min(totalHeight / 2, 32);",
                leftEdgeBody = "steps.push(`h \${-w}`);\nsteps.push(`l \${w} \${-w}`);\nif (halfStraight > 0) steps.push('v', -halfStraight * 2);\nsteps.push(`l \${-w} \${-w}`);",
                rightEdgeBody = "",
                paddingBody = "return 25;"
            ),
            CustomShapeDefinition(
                shapeName = "nbArrowPadding",
                edgeWidthBody = "return Math.min(totalHeight / 2, 32);",
                leftEdgeBody = "steps.push(`l \${-w} \${-w}`);\nif (halfStraight > 0) steps.push('v', -halfStraight * 2);\nsteps.push(`l \${w} \${-w}`);",
                rightEdgeBody = "steps.push(`h \${w}`);\nsteps.push(`l \${-w} \${w}`);\nif (halfStraight > 0) steps.push('v', halfStraight * 2);\nsteps.push(`l \${w} \${w}`);",
                paddingBody = "return { left: 15, right: 50 };"
            )
        ),
        blocks = listOf(
            ExtensionBlock(
                opcode = "reporter",
                blockType = ScratchBlockType.REPORTER,
                text = "im a reporter",
                blockShape = "nbArrow",
                jsBody = "return 'nbArrow';"
            ),
            ExtensionBlock(
                opcode = "dual",
                blockType = ScratchBlockType.COMMAND,
                dualType = ScratchBlockType.REPORTER,
                text = "im a dual",
                blockShape = "nbArrow",
                jsBody = "return 'dual nbArrow';"
            ),
            ExtensionBlock(
                opcode = "reporterNoleft",
                blockType = ScratchBlockType.REPORTER,
                text = "im a reporter (no left)",
                blockShape = "nbArrowNoleft",
                jsBody = "return 'nbArrowNoleft';"
            ),
            ExtensionBlock(
                opcode = "dualNoLeft",
                blockType = ScratchBlockType.COMMAND,
                dualType = ScratchBlockType.REPORTER,
                text = "im a dual (no left)",
                blockShape = "nbArrowNoleft",
                jsBody = "return 'dual nbArrowNoleft';"
            ),
            ExtensionBlock(
                opcode = "reporterLR",
                blockType = ScratchBlockType.REPORTER,
                text = "im a reporter (l&r padding)",
                blockShape = "nbArrowPadding",
                jsBody = "return 'nbArrowPadding';"
            ),
            ExtensionBlock(
                opcode = "dualLR",
                blockType = ScratchBlockType.COMMAND,
                dualType = ScratchBlockType.REPORTER,
                text = "im a dual (l&r padding)",
                blockShape = "nbArrowPadding",
                jsBody = "return 'dual nbArrowPadding';"
            )
        ),
        menus = emptyList()
    )

    val stringInputsProject = ExtensionProject(
        id = "stringinputsexample",
        name = "String Inputs",
        className = "StringInputsExample",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#10B981",
        color2Hex = "#059669",
        color3Hex = "#047857",
        blocks = listOf(
            ExtensionBlock(
                opcode = "logNormal",
                blockType = ScratchBlockType.COMMAND,
                text = "log [TEXT]",
                arguments = listOf(
                    BlockArgument(
                        name = "TEXT",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "hello"
                    )
                ),
                jsBody = "console.log(args.TEXT);"
            ),
            ExtensionBlock(
                opcode = "logMultiline",
                blockType = ScratchBlockType.COMMAND,
                text = "log multiline [TEXT]",
                arguments = listOf(
                    BlockArgument(
                        name = "TEXT",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "first line\nsecond line",
                        canMultiline = true
                    )
                ),
                jsBody = "console.log(args.TEXT);"
            ),
            ExtensionBlock(
                opcode = "logFixedMultiline",
                blockType = ScratchBlockType.COMMAND,
                text = "log fixed multiline [TEXT]",
                arguments = listOf(
                    BlockArgument(
                        name = "TEXT",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "first line\nsecond line",
                        acceptReporters = false,
                        canMultiline = true
                    )
                ),
                jsBody = "console.log(args.TEXT);"
            )
        ),
        menus = emptyList()
    )

    val sliderInputsProject = ExtensionProject(
        id = "sliderinputsexample",
        name = "Slider Inputs",
        className = "SliderInputs",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#00A8CC",
        color2Hex = "#0089A7",
        color3Hex = "#006980",
        blocks = listOf(
            ExtensionBlock(
                opcode = "amount",
                blockType = ScratchBlockType.REPORTER,
                text = "amount [AMOUNT]",
                arguments = listOf(
                    BlockArgument(
                        name = "AMOUNT",
                        type = ScratchArgumentType.SLIDER,
                        defaultValue = "50",
                        sliderMin = "0",
                        sliderMax = "100",
                        sliderPrecision = "1"
                    )
                ),
                jsBody = "return args.AMOUNT;"
            ),
            ExtensionBlock(
                opcode = "fineAmount",
                blockType = ScratchBlockType.REPORTER,
                text = "fine amount [AMOUNT]",
                arguments = listOf(
                    BlockArgument(
                        name = "AMOUNT",
                        type = ScratchArgumentType.SLIDER,
                        defaultValue = "0",
                        sliderMin = "-1",
                        sliderMax = "1",
                        sliderPrecision = "0.01"
                    )
                ),
                jsBody = "return args.AMOUNT;"
            )
        ),
        menus = emptyList()
    )

    val objectArrayProject = ExtensionProject(
        id = "objectarrayexample",
        name = "Object & Array",
        className = "ObjectArray",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#EC4899",
        color2Hex = "#DB2777",
        color3Hex = "#BE185D",
        blocks = listOf(
            ExtensionBlock(
                opcode = "object",
                blockType = ScratchBlockType.OBJECT,
                text = "object with [KEY] = [VALUE]",
                arguments = listOf(
                    BlockArgument(
                        name = "KEY",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "key"
                    ),
                    BlockArgument(
                        name = "VALUE",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "value"
                    )
                ),
                jsBody = "return { [args.KEY]: args.VALUE };"
            ),
            ExtensionBlock(
                opcode = "array",
                blockType = ScratchBlockType.ARRAY,
                text = "array with [ITEM]",
                arguments = listOf(
                    BlockArgument(
                        name = "ITEM",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "item"
                    )
                ),
                jsBody = "return [args.ITEM];"
            ),
            ExtensionBlock(
                opcode = "readKey",
                blockType = ScratchBlockType.REPORTER,
                text = "key [KEY] of [OBJ]",
                arguments = listOf(
                    BlockArgument(
                        name = "KEY",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "key"
                    ),
                    BlockArgument(
                        name = "OBJ",
                        type = ScratchArgumentType.OBJECT
                    )
                ),
                jsBody = "if (!args.OBJ) return \"\";\nreturn args.OBJ[args.KEY];"
            ),
            ExtensionBlock(
                opcode = "length",
                blockType = ScratchBlockType.REPORTER,
                text = "length of [ARR]",
                arguments = listOf(
                    BlockArgument(
                        name = "ARR",
                        type = ScratchArgumentType.ARRAY
                    )
                ),
                jsBody = "if (!args.ARR) return 0;\nreturn args.ARR.length;"
            )
        ),
        menus = emptyList()
    )

    val dragToDuplicateProject = ExtensionProject(
        id = "dragtoduplicateexample",
        name = "Drag to Duplicate",
        className = "DragToDuplicate",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#8B5CF6",
        color2Hex = "#7C3AED",
        color3Hex = "#6D28D9",
        blocks = listOf(
            ExtensionBlock(
                opcode = "duplicateReporter",
                blockType = ScratchBlockType.REPORTER,
                text = "duplicate",
                duplicateOnDrag = true,
                jsBody = "return 'hi';"
            ),
            ExtensionBlock(
                opcode = "hello",
                blockType = ScratchBlockType.COMMAND,
                text = "hello [INP]",
                arguments = listOf(
                    BlockArgument(
                        name = "INP",
                        type = ScratchArgumentType.STRING,
                        shadow = "duplicateReporter"
                    )
                ),
                jsBody = "const { INP } = args;\nconsole.log(INP);"
            )
        ),
        menus = emptyList()
    )

    val simpleExtendableProject = ExtensionProject(
        id = "simpleextendableexample",
        name = "Simple Extendable",
        className = "SimpleExtendable",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#14B8A6",
        color2Hex = "#0D9488",
        color3Hex = "#0F766E",
        blocks = listOf(
            ExtensionBlock(
                opcode = "hello",
                blockType = ScratchBlockType.REPORTER,
                text = "hello [INPUTS]",
                arguments = listOf(
                    BlockArgument(
                        name = "INPUTS",
                        type = ScratchArgumentType.EXTENDABLE,
                        extendableText = "[ABC]",
                        extendableArguments = listOf(
                            BlockArgument(
                                name = "ABC",
                                type = ScratchArgumentType.STRING,
                                defaultValue = "hello"
                            )
                        )
                    )
                ),
                jsBody = "const values = util.extendableToArray(args, 'INPUTS', 'ABC');\nreturn values.join('');"
            )
        ),
        menus = emptyList()
    )

    val nestedExtendableProject = ExtensionProject(
        id = "nestedextendableexample",
        name = "Nested Extendable",
        className = "NestedExtendable",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#6366F1",
        color2Hex = "#4F46E5",
        color3Hex = "#4338CA",
        blocks = listOf(
            ExtensionBlock(
                opcode = "nested",
                blockType = ScratchBlockType.COMMAND,
                text = "nested [INPUTS]",
                arguments = listOf(
                    BlockArgument(
                        name = "INPUTS",
                        type = ScratchArgumentType.EXTENDABLE,
                        extendableText = "layer 1 [HEY] [NUMBER]",
                        extendableSeparator = "|",
                        extendableArguments = listOf(
                            BlockArgument(
                                name = "HEY",
                                type = ScratchArgumentType.EXTENDABLE,
                                extendableText = "layer 2 [ABC]",
                                extendableSeparator = "||",
                                extendableArguments = listOf(
                                    BlockArgument(
                                        name = "ABC",
                                        type = ScratchArgumentType.NUMBER,
                                        defaultValue = "5"
                                    )
                                )
                            ),
                            BlockArgument(
                                name = "NUMBER",
                                type = ScratchArgumentType.NUMBER,
                                defaultValue = "10"
                            )
                        )
                    )
                ),
                jsBody = """
                    const numbers = util.extendableToArray(args, 'INPUTS', 'NUMBER');
                    const abcValues = util.extendableToArray(args, 'INPUTS', 'HEY', 'ABC');
                    console.log('Numbers on layer one:', numbers);
                    console.log('Numbers on layer two:', abcValues);
                    abcValues.forEach((innerList, i) => {
                      console.log(`Input ${'$'}{i} has ${'$'}{innerList.length} items in layer 2`);
                    });
                """.trimIndent()
            )
        ),
        menus = emptyList()
    )

    val branchesProject = ExtensionProject(
        id = "branchesexample",
        name = "Branches",
        className = "Branches",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#F59E0B",
        color2Hex = "#D97706",
        color3Hex = "#B45309",
        blocks = listOf(
            ExtensionBlock(
                opcode = "branch",
                blockType = ScratchBlockType.CONDITIONAL,
                text = "hello",
                branchCount = 1,
                branchTexts = listOf("neighbor"),
                jsBody = "util.startBranch(1, false);"
            ),
            ExtensionBlock(
                opcode = "repeatLoop",
                blockType = ScratchBlockType.LOOP,
                text = "repeat [COUNT] times",
                branchCount = 1,
                arguments = listOf(
                    BlockArgument(
                        name = "COUNT",
                        type = ScratchArgumentType.NUMBER,
                        defaultValue = "3"
                    )
                ),
                jsBody = "util.startBranch(1, true);"
            ),
            ExtensionBlock(
                opcode = "ifElseBranch",
                blockType = ScratchBlockType.CONDITIONAL,
                text = "if [COND] then",
                branchCount = 2,
                branchTexts = listOf("else"),
                arguments = listOf(
                    BlockArgument(
                        name = "COND",
                        type = ScratchArgumentType.BOOLEAN
                    )
                ),
                jsBody = "if (args.COND) {\n  util.startBranch(1, false);\n} else {\n  util.startBranch(2, false);\n}"
            )
        ),
        menus = emptyList()
    )

    val extraMenuPropertiesProject = ExtensionProject(
        id = "extramenupropertiesexample",
        name = "Extra Menu Properties",
        className = "ExtraMenuProperties",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#EC4899",
        color2Hex = "#DB2777",
        color3Hex = "#BE185D",
        blocks = listOf(
            ExtensionBlock(
                opcode = "choose",
                blockType = ScratchBlockType.REPORTER,
                text = "choose [COLOR]",
                arguments = listOf(
                    BlockArgument(
                        name = "COLOR",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "red",
                        menuName = "colors"
                    )
                ),
                jsBody = "return args.COLOR;"
            ),
            ExtensionBlock(
                opcode = "chooseFood",
                blockType = ScratchBlockType.REPORTER,
                text = "order [FOOD]",
                arguments = listOf(
                    BlockArgument(
                        name = "FOOD",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "water",
                        menuName = "food"
                    )
                ),
                jsBody = "return args.FOOD;"
            )
        ),
        menus = listOf(
            ExtensionMenu(
                name = "colors",
                acceptReporters = true,
                acceptText = true,
                items = listOf("red", "green", "blue", "---", "cyan", "magenta", "yellow")
            ),
            ExtensionMenu(
                name = "food",
                acceptReporters = false,
                acceptText = false,
                items = listOf("water", "juice", "---", "salad", "soup")
            )
        )
    )

    val dependentDropdownsProject = ExtensionProject(
        id = "dependentdropdownsexample",
        name = "Dependent Dropdowns",
        className = "DependentDropdowns",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#0EA5E9",
        color2Hex = "#0284C7",
        color3Hex = "#0369A1",
        blocks = listOf(
            ExtensionBlock(
                opcode = "describeAnimal",
                blockType = ScratchBlockType.REPORTER,
                text = "[ANIMAL] breed [BREED]",
                arguments = listOf(
                    BlockArgument(
                        name = "ANIMAL",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "cat",
                        menuName = "ANIMAL_MENU"
                    ),
                    BlockArgument(
                        name = "BREED",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "Tabby",
                        menuName = "BREED_MENU"
                    )
                ),
                jsBody = "return `\${args.ANIMAL}: \${args.BREED}`;"
            ),
            ExtensionBlock(
                opcode = "describeAnimalCoat",
                blockType = ScratchBlockType.REPORTER,
                text = "[ANIMAL] breed [BREED] with [COAT] coat",
                arguments = listOf(
                    BlockArgument(
                        name = "ANIMAL",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "cat",
                        menuName = "ANIMAL_MENU"
                    ),
                    BlockArgument(
                        name = "BREED",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "Tabby",
                        menuName = "BREED_MENU"
                    ),
                    BlockArgument(
                        name = "COAT",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "short",
                        menuName = "COAT_MENU"
                    )
                ),
                jsBody = "return `\${args.ANIMAL}: \${args.BREED} with a \${args.COAT} coat`;"
            )
        ),
        menus = listOf(
            ExtensionMenu(
                name = "ANIMAL_MENU",
                acceptReporters = true,
                items = listOf("cat", "dog")
            ),
            ExtensionMenu(
                name = "BREED_MENU",
                acceptReporters = false,
                acceptText = false,
                parentName = "ANIMAL",
                optionMapping = mapOf(
                    "cat" to listOf(MenuItemEntry("Siamese"), MenuItemEntry("Tabby")),
                    "dog" to listOf(MenuItemEntry("Beagle"), MenuItemEntry("Poodle"))
                ),
                defaultOptions = listOf(MenuItemEntry("unknown"))
            ),
            ExtensionMenu(
                name = "COAT_MENU",
                acceptReporters = false,
                acceptText = false,
                parentName = "BREED",
                optionMapping = mapOf(
                    "Siamese" to listOf(MenuItemEntry("short")),
                    "Tabby" to listOf(MenuItemEntry("short"), MenuItemEntry("long")),
                    "Beagle" to listOf(MenuItemEntry("smooth"), MenuItemEntry("rough")),
                    "Poodle" to listOf(MenuItemEntry("curly"), MenuItemEntry("corded"))
                ),
                defaultOptions = listOf(MenuItemEntry("unknown"))
            )
        )
    )

    val mutatorDropdownsProject = ExtensionProject(
        id = "mutatordropdownexample",
        name = "Mutator Dropdown Example",
        className = "MutatorDropdownExample",
        executionMode = ExecutionMode.UNSANDBOXED,
        enforceUnsandboxedGuard = true,
        bindVmReference = false,
        color1Hex = "#F43F5E",
        color2Hex = "#E11D48",
        color3Hex = "#BE123C",
        blocks = listOf(
            ExtensionBlock(
                opcode = "convert",
                blockType = ScratchBlockType.REPORTER,
                text = "[MODE] [INPUT] with delimiter [DELIMITER]",
                arguments = listOf(
                    BlockArgument(
                        name = "MODE",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "SPLIT",
                        menuName = "MODE_MENU"
                    ),
                    BlockArgument(
                        name = "INPUT",
                        type = ScratchArgumentType.STRING,
                        defaultValue = "a,b,c"
                    ),
                    BlockArgument(
                        name = "DELIMITER",
                        type = ScratchArgumentType.STRING,
                        defaultValue = ","
                    )
                ),
                jsBody = """
                    if (args.MODE === 'JOIN') {
                      const value = Array.isArray(args.INPUT) ? args.INPUT : [args.INPUT];
                      return value.join(args.DELIMITER);
                    }
                    return String(args.INPUT).split(args.DELIMITER);
                """.trimIndent()
            )
        ),
        menus = listOf(
            ExtensionMenu(
                name = "MODE_MENU",
                acceptReporters = false,
                acceptText = false,
                items = listOf("SPLIT", "JOIN"),
                itemEntries = listOf(
                    MenuItemEntry(value = "SPLIT", text = "split"),
                    MenuItemEntry(value = "JOIN", text = "join")
                ),
                mutator = linkedMapOf(
                    "SPLIT" to MenuMutatorState(
                        output = ScratchBlockType.ARRAY,
                        arguments = listOf(
                            BlockArgument(
                                name = "INPUT",
                                type = ScratchArgumentType.STRING,
                                defaultValue = "a,b,c"
                            )
                        )
                    ),
                    "JOIN" to MenuMutatorState(
                        output = ScratchBlockType.REPORTER,
                        arguments = listOf(
                            BlockArgument(
                                name = "INPUT",
                                type = ScratchArgumentType.ARRAY
                            )
                        )
                    )
                )
            )
        )
    )

    val allTemplates = listOf(
        ExtensionTemplate(
            title = "Turbo Mode (VM)",
            subtitle = "Instantaneous VM control with menu dropdown & boolean reporter",
            badge = "Docs Example",
            project = turboModeProject
        ),
        ExtensionTemplate(
            title = "Mutator Dropdowns (Dynamic Shape)",
            subtitle = "Static menu mutator states transforming block output & argument types",
            badge = "Mutators",
            project = mutatorDropdownsProject
        ),
        ExtensionTemplate(
            title = "Dependent Dropdowns (Chained Menus)",
            subtitle = "parentName, optionMapping & defaultOptions with ANIMAL → BREED → COAT chaining",
            badge = "Dependent Menus",
            project = dependentDropdownsProject
        ),
        ExtensionTemplate(
            title = "Extra Menu Properties (Separators & Text)",
            subtitle = "Dropdown '---' separators and acceptText: true custom text entry",
            badge = "Menu Props",
            project = extraMenuPropertiesProject
        ),
        ExtensionTemplate(
            title = "Branches (C-Blocks & Loops)",
            subtitle = "Scratch.BlockType.CONDITIONAL & LOOP with branchCount and util.startBranch",
            badge = "Branches",
            project = branchesProject
        ),
        ExtensionTemplate(
            title = "Simple Extendable (Dynamic)",
            subtitle = "Scratch.ArgumentType.EXTENDABLE with util.extendableToArray(args, ...)",
            badge = "Extendables",
            project = simpleExtendableProject
        ),
        ExtensionTemplate(
            title = "Nested Extendable (Multi-Layer)",
            subtitle = "Two-layer nested extendables with custom '|' and '||' separators",
            badge = "Nested Extendables",
            project = nestedExtendableProject
        ),
        ExtensionTemplate(
            title = "Duplicate on Drag (Shadow)",
            subtitle = "Reporter blocks with duplicateOnDrag: true & shadow input fill-ins",
            badge = "Duplicate on Drag",
            project = dragToDuplicateProject
        ),
        ExtensionTemplate(
            title = "Objects & Arrays (JSON)",
            subtitle = "Pure JS OBJECT & ARRAY block shapes plus reporter-only object/array slots",
            badge = "Objects & Arrays",
            project = objectArrayProject
        ),
        ExtensionTemplate(
            title = "Slider Inputs (Range & Step)",
            subtitle = "Numeric slider arguments with per-block min, max, and decimal precision",
            badge = "Slider Inputs",
            project = sliderInputsProject
        ),
        ExtensionTemplate(
            title = "String Inputs (Multiline & Fixed)",
            subtitle = "Normal, canMultiline, and acceptReporters: false string input configurations",
            badge = "String Inputs",
            project = stringInputsProject
        ),
        ExtensionTemplate(
            title = "Custom Shapes (Geometry)",
            subtitle = "Scratch.BlockShapes.register with SVG leftEdge, rightEdge, edgeWidth & padding",
            badge = "Custom Shapes",
            project = customShapesProject
        ),
        ExtensionTemplate(
            title = "Custom Types (Counter)",
            subtitle = "Scratch.types.register with SQUARE shape, JSON serialization & visualReport",
            badge = "Custom Types",
            project = customTypesProject
        ),
        ExtensionTemplate(
            title = "Block Switches (Context Swap)",
            subtitle = "String & object switches with input remapping and splitInputs",
            badge = "Switches",
            project = switchesProject
        ),
        ExtensionTemplate(
            title = "Unsandboxed Hello World",
            subtitle = "Strict-mode IIFE with unsandboxed runtime guard",
            badge = "Docs Example",
            project = helloWorldProject
        ),
        ExtensionTemplate(
            title = "VM Sprite & Variable Studio",
            subtitle = "Read stage variables and move sprites directly via Scratch.vm",
            badge = "VM Power",
            project = vmInspectorProject
        ),
        ExtensionTemplate(
            title = "Sandboxed Utilities",
            subtitle = "Backward-compatible template safe for cross-origin iframe execution",
            badge = "Sandboxed",
            project = sandboxedStarterProject
        )
    )

    val legacySandboxedSamples = listOf(
        LegacySandboxedSample(
            title = "Classic Sandboxed Hello World",
            description = "Unwrapped class + global registration from older worker/iframe extensions",
            code = """
                class HelloWorld {
                  getInfo() {
                    return {
                      id: 'helloworld',
                      name: 'Hello World',
                      blocks: [
                        {
                          opcode: 'hello',
                          blockType: Scratch.BlockType.REPORTER,
                          text: 'Hello!'
                        }
                      ]
                    };
                  }
                  hello() {
                    return 'World!';
                  }
                }
                Scratch.extensions.register(new HelloWorld());
            """.trimIndent()
        ),
        LegacySandboxedSample(
            title = "Unsafe Global VM Leak (Bad Pattern)",
            description = "Demonstrates 'const vm = Scratch.vm' leaked outside IIFE + missing guard",
            code = """
                // BAD CODE: leaks vm globally outside IIFE
                const vm = Scratch.vm;
                class TurboControl {
                  getInfo() {
                    return {
                      id: 'turbocontrol',
                      name: 'Turbo Control',
                      blocks: [
                        {
                          opcode: 'setTurbo',
                          blockType: Scratch.BlockType.COMMAND,
                          text: 'set turbo mode to [ENABLED]',
                          arguments: {
                            ENABLED: {
                              type: Scratch.ArgumentType.STRING,
                              menu: 'ENABLED_MENU'
                            }
                          }
                        }
                      ],
                      menus: {
                        ENABLED_MENU: {
                          acceptReporters: true,
                          items: ['on', 'off']
                        }
                      }
                    };
                  }
                  setTurbo(args) {
                    vm.setTurboMode(args.ENABLED === 'on');
                  }
                }
                Scratch.extensions.register(new TurboControl());
            """.trimIndent()
        ),
        LegacySandboxedSample(
            title = "Sandboxed Reporter Missing Return",
            description = "Legacy reporter that forgets to return a valid primitive value",
            code = """
                class QuickMath {
                  getInfo() {
                    return {
                      id: 'quickmath',
                      name: 'Quick Math',
                      blocks: [
                        {
                          opcode: 'doubleIt',
                          blockType: Scratch.BlockType.REPORTER,
                          text: 'double [NUM]',
                          arguments: {
                            NUM: {
                              type: Scratch.ArgumentType.NUMBER,
                              defaultValue: 5
                            }
                          }
                        },
                        {
                          opcode: 'isEven',
                          blockType: Scratch.BlockType.BOOLEAN,
                          text: 'is [NUM] even?',
                          arguments: {
                            NUM: {
                              type: Scratch.ArgumentType.NUMBER,
                              defaultValue: 4
                            }
                          }
                        }
                      ]
                    };
                  }
                  doubleIt(args) {
                    const result = Number(args.NUM) * 2;
                    return result;
                  }
                  isEven(args) {
                    // Notice missing return statement!
                    Number(args.NUM) % 2 === 0;
                  }
                }
                Scratch.extensions.register(new QuickMath());
            """.trimIndent()
        )
    )
}
