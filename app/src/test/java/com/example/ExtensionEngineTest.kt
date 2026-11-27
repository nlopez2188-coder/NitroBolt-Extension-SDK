package com.example

import com.example.data.BlockSimulator
import com.example.data.ExtensionCodeGenerator
import com.example.data.ExtensionTemplates
import com.example.data.SandboxedConverter
import com.example.model.BlockArgument
import com.example.model.BlockSwitch
import com.example.model.BlockSwitchInputMapping
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.LintSeverity
import com.example.model.MenuItemEntry
import com.example.model.MenuMutatorState
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType
import com.example.model.VmSimulationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionEngineTest {

    @Test
    fun generatesValidUnsandboxedTurboModeIife() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.turboModeProject)
        assertTrue(code.startsWith("(function(Scratch) {"))
        assertTrue(code.contains("'use strict';"))
        assertTrue(code.contains("if (!Scratch.extensions.unsandboxed)"))
        assertTrue(code.contains("const vm = Scratch.vm;"))
        assertTrue(code.contains("vm.setTurboMode(args.ENABLED === 'on');"))
        assertTrue(code.endsWith("})(Scratch);"))
    }

    @Test
    fun generatesStringAndObjectSwitchesCorrectly() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.switchesProject)
        assertTrue(code.contains("switches: ["))
        assertTrue(code.contains("'def'"))
        assertTrue(code.contains("id: 'say'"))
        assertTrue(code.contains("splitInputs: ['SECS']"))
        assertTrue(code.contains("inputs: [['TEXT', 'MSG']]"))

        val rawProject = ExtensionTemplates.switchesProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "moveX",
                    text = "move [X]",
                    switches = listOf(
                        BlockSwitch(
                            targetId = "motion_changexby",
                            inputs = listOf(BlockSwitchInputMapping("X", "DX")),
                            rawId = true
                        )
                    )
                )
            )
        )
        val rawCode = ExtensionCodeGenerator.generate(rawProject)
        assertTrue(rawCode.contains("id: 'motion_changexby'"))
        assertTrue(rawCode.contains("inputs: [['X', 'DX']]"))
        assertTrue(rawCode.contains("rawId: true"))
    }

    @Test
    fun converterParsesSwitchesInBothForms() {
        val sampleWithSwitches = """
            class Switches {
              getInfo() {
                return {
                  id: 'switchesexample',
                  name: 'Switches',
                  blocks: [
                    {
                      opcode: 'sayForSecs',
                      blockType: Scratch.BlockType.COMMAND,
                      text: 'say [TEXT] for [SECS] secs',
                      switches: [
                        'abc',
                        { id: 'think', inputs: [['TEXT', 'MSG']], splitInputs: ['SECS'], rawId: true }
                      ]
                    }
                  ]
                };
              }
              sayForSecs(args) {
                console.log(args.TEXT);
              }
            }
            Scratch.extensions.register(new Switches());
        """.trimIndent()
        val result = SandboxedConverter.convertToUnsandboxed(sampleWithSwitches)
        val switches = result.extractedProject?.blocks?.firstOrNull()?.switches.orEmpty()
        assertEquals(2, switches.size)
        assertEquals("abc", switches[0].targetId)
        assertTrue(switches[0].isSimpleStringForm)
        assertEquals("think", switches[1].targetId)
        assertEquals(listOf(BlockSwitchInputMapping("TEXT", "MSG")), switches[1].inputs)
        assertEquals(listOf("SECS"), switches[1].splitInputs)
        assertTrue(switches[1].rawId)
    }

    @Test
    fun converterMovesLeakedGlobalVmInsideIife() {
        val badSample = ExtensionTemplates.legacySandboxedSamples[1].code
        val result = SandboxedConverter.convertToUnsandboxed(badSample)
        assertTrue(result.convertedCode.startsWith("(function(Scratch) {"))
        assertFalse(result.convertedCode.substringBefore("(function(Scratch)").contains("const vm"))
        assertTrue(result.convertedCode.contains("  const vm = Scratch.vm;"))
        assertNotNull(result.extractedProject)
        assertEquals("turbocontrol", result.extractedProject?.id)
        assertEquals(1, result.extractedProject?.blocks?.size)
        assertEquals(1, result.extractedProject?.menus?.size)
    }

    @Test
    fun linterDetectsMissingReturnOnReporter() {
        val broken = ExtensionTemplates.helloWorldProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "broken",
                    blockType = ScratchBlockType.REPORTER,
                    text = "broken reporter",
                    jsBody = "const x = 1 + 1;"
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(broken)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("must return") })
    }

    @Test
    fun simulatorUpdatesTurboModeAndReportsTiming() {
        val block = ExtensionTemplates.turboModeProject.blocks.first()
        val (nextVm, log) = BlockSimulator.execute(
            project = ExtensionTemplates.turboModeProject,
            block = block,
            argValues = mapOf("ENABLED" to "on"),
            currentVm = VmSimulationState(turboMode = false)
        )
        assertTrue(nextVm.turboMode)
        assertEquals(240, nextVm.fps)
        assertEquals(ExecutionMode.UNSANDBOXED, log.executionMode)
        assertFalse(log.isError)
    }

    @Test
    fun generatesCustomTypesBeforeExtensionRegistration() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.customTypesProject)
        val typeRegIdx = code.indexOf("Scratch.types.register('counter', Counter);")
        val extRegIdx = code.indexOf("Scratch.extensions.register(new CustomTypesDemo());")
        assertTrue(typeRegIdx > 0)
        assertTrue(extRegIdx > typeRegIdx)
        assertTrue(code.contains("return Scratch.BlockShape.SQUARE;"))
        assertTrue(code.contains("outputType: 'counter'"))
        assertTrue(code.contains("type: 'counter'"))
        assertTrue(code.contains("useUnsandboxedRuntime: true"))
        assertTrue(code.contains("static visualReport(instance)"))
    }

    @Test
    fun converterParsesCustomTypesAndOutputType() {
        val sample = ExtensionCodeGenerator.generate(ExtensionTemplates.customTypesProject)
        val result = SandboxedConverter.convertToUnsandboxed(sample)
        val parsed = result.extractedProject
        assertNotNull(parsed)
        assertEquals("CustomTypesDemo", parsed?.className)
        assertEquals(1, parsed?.customTypes?.size)
        assertEquals("counter", parsed?.customTypes?.firstOrNull()?.typeName)
        assertEquals("Counter", parsed?.customTypes?.firstOrNull()?.className)
        assertEquals("counter", parsed?.blocks?.firstOrNull()?.outputType)
    }

    @Test
    fun linterRejectsCustomTypesInSandboxedMode() {
        val sandboxedCustom = ExtensionTemplates.customTypesProject.copy(
            executionMode = ExecutionMode.SANDBOXED
        )
        val issues = ExtensionCodeGenerator.analyzeProject(sandboxedCustom)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("Custom Types") })
    }

    @Test
    fun generatesCustomShapesBeforeExtensionRegistrationAndDualBlocks() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.customShapesProject)
        val shapeRegIdx = code.indexOf("Scratch.BlockShapes.register('nbArrow', {")
        val extRegIdx = code.indexOf("Scratch.extensions.register(new CustomShapesDemo());")
        assertTrue(shapeRegIdx > 0)
        assertTrue(extRegIdx > shapeRegIdx)
        assertTrue(code.contains("blockShape: 'nbArrow'"))
        assertTrue(code.contains("dualType: Scratch.BlockType.REPORTER"))
        assertTrue(code.contains("return Math.min(totalHeight / 2, 32);"))
        assertTrue(code.contains("return { left: 15, right: 50 };"))
    }

    @Test
    fun converterParsesArrowFunctionCustomShapesAndBlockShapes() {
        val docSample = """
            (function (Scratch) {
              "use strict";
              Scratch.BlockShapes.register("nbArrow", {
                edgeWidth: (totalHeight) => Math.min(totalHeight / 2, 32),
                leftEdge: (steps, w, halfStraight) => {
                  steps.push(`l ${'$'}{-w} ${'$'}{-w}`);
                  if (halfStraight > 0) steps.push("v", -halfStraight * 2);
                  steps.push(`l ${'$'}{w} ${'$'}{-w}`);
                },
                padding: () => ({ left: 15, right: 50 })
              });
              class CustomShapesDemo {
                getInfo() {
                  return {
                    id: "customShapesDemo",
                    name: "Custom Shapes Demo",
                    useUnsandboxedRuntime: true,
                    blocks: [
                      {
                        opcode: "dual",
                        blockType: Scratch.BlockType.COMMAND,
                        dualType: Scratch.BlockType.REPORTER,
                        text: "im a dual",
                        blockShape: "nbArrow"
                      }
                    ]
                  };
                }
                dual() {
                  return "ok";
                }
              }
              Scratch.extensions.register(new CustomShapesDemo());
            })(Scratch);
        """.trimIndent()
        val result = SandboxedConverter.convertToUnsandboxed(docSample)
        val parsed = result.extractedProject
        assertNotNull(parsed)
        assertEquals(1, parsed?.customShapes?.size)
        val shape = parsed?.customShapes?.firstOrNull()
        assertEquals("nbArrow", shape?.shapeName)
        assertTrue(shape?.edgeWidthBody?.contains("Math.min(totalHeight / 2, 32)") == true)
        assertTrue(shape?.paddingBody?.contains("left: 15, right: 50") == true)
        val block = parsed?.blocks?.firstOrNull()
        assertEquals("nbArrow", block?.blockShape)
        assertEquals(ScratchBlockType.REPORTER, block?.dualType)
    }

    @Test
    fun customShapePathEngineValidatesVerticalSumAndLinterChecksSandboxed() {
        val validShape = ExtensionTemplates.customShapesProject.customShapes.first()
        val validResult = com.example.data.CustomShapePathEngine.validateShapeEdges(validShape)
        assertTrue(validResult.isValid)

        val brokenShape = validShape.copy(
            leftEdgeBody = "steps.push(`l -3 -3 l 3 -3`);"
        )
        val brokenResult = com.example.data.CustomShapePathEngine.validateShapeEdges(brokenShape)
        assertFalse(brokenResult.isValid)

        val sandboxedShapes = ExtensionTemplates.customShapesProject.copy(
            executionMode = ExecutionMode.SANDBOXED
        )
        val issues = ExtensionCodeGenerator.analyzeProject(sandboxedShapes)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("Custom Shapes") })
    }

    @Test
    fun generatesAndParsesStringInputsWithMultilineAcceptReportersAndShadow() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.stringInputsProject)
        assertTrue(code.contains("defaultValue: 'first line\\nsecond line'"))
        assertTrue(code.contains("canMultiline: true"))
        assertTrue(code.contains("acceptReporters: false"))

        val parsed = SandboxedConverter.convertToUnsandboxed(code).extractedProject
        assertNotNull(parsed)
        assertEquals(3, parsed?.blocks?.size)
        val normalArg = parsed?.blocks?.get(0)?.arguments?.firstOrNull()
        val multiArg = parsed?.blocks?.get(1)?.arguments?.firstOrNull()
        val fixedMultiArg = parsed?.blocks?.get(2)?.arguments?.firstOrNull()
        assertEquals(false, normalArg?.canMultiline)
        assertEquals(true, normalArg?.acceptReporters)
        assertEquals(true, multiArg?.canMultiline)
        assertEquals(true, multiArg?.acceptReporters)
        assertEquals("first line\nsecond line", multiArg?.defaultValue)
        assertEquals(true, fixedMultiArg?.canMultiline)
        assertEquals(false, fixedMultiArg?.acceptReporters)
    }

    @Test
    fun simulatorLogsMultilineStringAndLinterWarnsInvalidMultiline() {
        val multiBlock = ExtensionTemplates.stringInputsProject.blocks[1]
        val (_, log) = BlockSimulator.execute(
            project = ExtensionTemplates.stringInputsProject,
            block = multiBlock,
            argValues = mapOf("TEXT" to "line 1\nline 2\nline 3"),
            currentVm = VmSimulationState()
        )
        assertFalse(log.isError)
        assertTrue(log.output.contains("3 lines"))
        assertTrue(log.output.contains("line 1\nline 2\nline 3"))

        val badMultiline = ExtensionTemplates.stringInputsProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "numMulti",
                    text = "num [N]",
                    arguments = listOf(
                        com.example.model.BlockArgument(
                            name = "N",
                            type = com.example.model.ScratchArgumentType.NUMBER,
                            canMultiline = true
                        )
                    )
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(badMultiline)
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("canMultiline ignored") })
    }

    @Test
    fun generatesAndParsesSliderInputsWithRangeAndDecimalPrecision() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.sliderInputsProject)
        assertTrue(code.contains("Scratch.ArgumentType.SLIDER"))
        assertTrue(code.contains("defaultValue: 50"))
        assertTrue(code.contains("min: 0"))
        assertTrue(code.contains("max: 100"))
        assertTrue(code.contains("precision: 1"))
        assertTrue(code.contains("min: -1"))
        assertTrue(code.contains("max: 1"))
        assertTrue(code.contains("precision: 0.01"))

        val parsed = SandboxedConverter.convertToUnsandboxed(code).extractedProject
        assertNotNull(parsed)
        assertEquals(2, parsed?.blocks?.size)

        val amountArg = parsed?.blocks?.get(0)?.arguments?.firstOrNull()
        assertEquals(com.example.model.ScratchArgumentType.SLIDER, amountArg?.type)
        assertEquals("50", amountArg?.defaultValue)
        assertEquals("0", amountArg?.sliderMin)
        assertEquals("100", amountArg?.sliderMax)
        assertEquals("1", amountArg?.sliderPrecision)

        val fineArg = parsed?.blocks?.get(1)?.arguments?.firstOrNull()
        assertEquals(com.example.model.ScratchArgumentType.SLIDER, fineArg?.type)
        assertEquals("0", fineArg?.defaultValue)
        assertEquals("-1", fineArg?.sliderMin)
        assertEquals("1", fineArg?.sliderMax)
        assertEquals("0.01", fineArg?.sliderPrecision)
    }

    @Test
    fun simulatorReturnsSliderNumericAndLinterValidatesRangeAndSnap() {
        val fineBlock = ExtensionTemplates.sliderInputsProject.blocks[1]
        val (_, log) = BlockSimulator.execute(
            project = ExtensionTemplates.sliderInputsProject,
            block = fineBlock,
            argValues = mapOf("AMOUNT" to "0.25"),
            currentVm = VmSimulationState()
        )
        assertFalse(log.isError)
        assertEquals("Returned: 0.25", log.output)

        val snapped = com.example.model.snapAndFormatSliderValue(
            raw = -0.3489f,
            min = -1.0,
            max = 1.0,
            precision = 0.01,
            precisionText = "0.01",
            minText = "-1"
        )
        assertEquals("-0.35", snapped)

        val badSliders = ExtensionTemplates.sliderInputsProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "badRange",
                    blockType = ScratchBlockType.REPORTER,
                    text = "bad [AMOUNT]",
                    arguments = listOf(
                        com.example.model.BlockArgument(
                            name = "AMOUNT",
                            type = com.example.model.ScratchArgumentType.SLIDER,
                            defaultValue = "20",
                            sliderMin = "50",
                            sliderMax = "10",
                            sliderPrecision = "0"
                        )
                    )
                ),
                ExtensionBlock(
                    opcode = "outOfBounds",
                    blockType = ScratchBlockType.REPORTER,
                    text = "oob [AMOUNT]",
                    arguments = listOf(
                        com.example.model.BlockArgument(
                            name = "AMOUNT",
                            type = com.example.model.ScratchArgumentType.SLIDER,
                            defaultValue = "200",
                            sliderMin = "0",
                            sliderMax = "100",
                            sliderPrecision = "1"
                        )
                    )
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(badSliders)
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("must be less than max") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("precision must be > 0") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("outside") })
    }

    @Test
    fun generatesAndParsesObjectsAndArraysWithoutLiteralDefaultsOnSlots() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.objectArrayProject)
        assertTrue(code.contains("Scratch.BlockType.OBJECT"))
        assertTrue(code.contains("Scratch.BlockType.ARRAY"))
        assertTrue(code.contains("Scratch.ArgumentType.OBJECT"))
        assertTrue(code.contains("Scratch.ArgumentType.ARRAY"))

        val parsed = SandboxedConverter.convertToUnsandboxed(code).extractedProject
        assertNotNull(parsed)
        assertEquals(4, parsed?.blocks?.size)
        assertEquals(ScratchBlockType.OBJECT, parsed?.blocks?.get(0)?.blockType)
        assertEquals(ScratchBlockType.ARRAY, parsed?.blocks?.get(1)?.blockType)

        val objArg = parsed?.blocks?.get(2)?.arguments?.find { it.name == "OBJ" }
        assertEquals(com.example.model.ScratchArgumentType.OBJECT, objArg?.type)
        assertEquals("", objArg?.defaultValue)

        val arrArg = parsed?.blocks?.get(3)?.arguments?.find { it.name == "ARR" }
        assertEquals(com.example.model.ScratchArgumentType.ARRAY, arrArg?.type)
        assertEquals("", arrArg?.defaultValue)
    }

    @Test
    fun simulatorEvaluatesObjectArrayBlocksAndLinterChecksSlots() {
        val blocks = ExtensionTemplates.objectArrayProject.blocks
        val objBlock = blocks[0]
        val arrBlock = blocks[1]
        val readKeyBlock = blocks[2]
        val lengthBlock = blocks[3]

        val (_, objLog) = BlockSimulator.execute(
            project = ExtensionTemplates.objectArrayProject,
            block = objBlock,
            argValues = mapOf("KEY" to "score", "VALUE" to "99"),
            currentVm = VmSimulationState()
        )
        assertFalse(objLog.isError)
        assertEquals("Returned: {\"score\": 99}", objLog.output)

        val (_, arrLog) = BlockSimulator.execute(
            project = ExtensionTemplates.objectArrayProject,
            block = arrBlock,
            argValues = mapOf("ITEM" to "sword"),
            currentVm = VmSimulationState()
        )
        assertFalse(arrLog.isError)
        assertEquals("Returned: [\"sword\"]", arrLog.output)

        val (_, readFound) = BlockSimulator.execute(
            project = ExtensionTemplates.objectArrayProject,
            block = readKeyBlock,
            argValues = mapOf("KEY" to "score", "OBJ" to "{\"score\": 99}"),
            currentVm = VmSimulationState()
        )
        assertEquals("Returned: 99", readFound.output)

        val (_, readMissing) = BlockSimulator.execute(
            project = ExtensionTemplates.objectArrayProject,
            block = readKeyBlock,
            argValues = mapOf("KEY" to "score", "OBJ" to ""),
            currentVm = VmSimulationState()
        )
        assertEquals("Returned: \"\"", readMissing.output)

        val (_, lenFound) = BlockSimulator.execute(
            project = ExtensionTemplates.objectArrayProject,
            block = lengthBlock,
            argValues = mapOf("ARR" to "[\"a\", \"b\", \"c\"]"),
            currentVm = VmSimulationState()
        )
        assertEquals("Returned: 3", lenFound.output)

        val (_, lenMissing) = BlockSimulator.execute(
            project = ExtensionTemplates.objectArrayProject,
            block = lengthBlock,
            argValues = mapOf("ARR" to ""),
            currentVm = VmSimulationState()
        )
        assertEquals("Returned: 0", lenMissing.output)

        val invalidObjProject = ExtensionTemplates.objectArrayProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "emptyObj",
                    blockType = ScratchBlockType.OBJECT,
                    text = "broken obj",
                    arguments = listOf(
                        com.example.model.BlockArgument(
                            name = "OBJ",
                            type = com.example.model.ScratchArgumentType.OBJECT,
                            defaultValue = "oops"
                        )
                    ),
                    jsBody = ""
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(invalidObjProject)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("must return a value") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("defaultValue ignored") })
    }

    @Test
    fun generatesAndParsesDragToDuplicateWithShadowAndDestructuring() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.dragToDuplicateProject)
        assertTrue(code.contains("duplicateOnDrag: true"))
        assertTrue(code.contains("shadow: 'duplicateReporter'"))

        val rawDocSnippet = """
            class DragToDuplicate {
              getInfo() {
                return {
                  id: 'dragtoduplicateexample',
                  name: 'Drag to Duplicate',
                  blocks: [
                    {
                      opcode: 'duplicateReporter',
                      blockType: Scratch.BlockType.REPORTER,
                      text: 'duplicate',
                      duplicateOnDrag: true
                    },
                    {
                      opcode: 'hello',
                      blockType: Scratch.BlockType.COMMAND,
                      text: 'hello [INP]',
                      arguments: {
                        INP: {
                          type: Scratch.ArgumentType.STRING,
                          shadow: 'duplicateReporter'
                        }
                      }
                    }
                  ]
                };
              }
              duplicateReporter() {
                return 'hi';
              }
              hello({ INP }) {
                console.log(INP);
              }
            }
            Scratch.extensions.register(new DragToDuplicate());
        """.trimIndent()

        val parsed = SandboxedConverter.convertToUnsandboxed(rawDocSnippet).extractedProject
        assertNotNull(parsed)
        assertEquals(2, parsed?.blocks?.size)
        val reporter = parsed?.blocks?.get(0)
        val cmd = parsed?.blocks?.get(1)
        assertEquals(true, reporter?.duplicateOnDrag)
        assertEquals("duplicateReporter", cmd?.arguments?.firstOrNull()?.shadow)
        assertTrue(cmd?.jsBody?.contains("const { INP } = args;") == true)
    }

    @Test
    fun simulatorResolvesShadowBlockAndLinterChecksDuplicateOnDragTarget() {
        val shadowVal = BlockSimulator.resolveShadowValue(
            project = ExtensionTemplates.dragToDuplicateProject,
            shadowOpcode = "duplicateReporter",
            currentVm = VmSimulationState()
        )
        assertEquals("hi", shadowVal)

        val cmdBlock = ExtensionTemplates.dragToDuplicateProject.blocks[1]
        val (_, log) = BlockSimulator.execute(
            project = ExtensionTemplates.dragToDuplicateProject,
            block = cmdBlock,
            argValues = mapOf("INP" to (shadowVal ?: "")),
            currentVm = VmSimulationState()
        )
        assertFalse(log.isError)
        assertTrue(log.output.contains("hi"))

        val invalidDuplicate = ExtensionTemplates.dragToDuplicateProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "badCmd",
                    blockType = ScratchBlockType.COMMAND,
                    text = "bad cmd",
                    duplicateOnDrag = true,
                    jsBody = "console.log('cmd');"
                ),
                ExtensionBlock(
                    opcode = "usesBadShadow",
                    blockType = ScratchBlockType.COMMAND,
                    text = "use [X]",
                    arguments = listOf(
                        com.example.model.BlockArgument(
                            name = "X",
                            shadow = "badCmd"
                        )
                    ),
                    jsBody = "console.log(args.X);"
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(invalidDuplicate)
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("duplicateOnDrag ignored") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("not a reporter") })
    }

    @Test
    fun generatesExtendablesWithNestedArgumentsAndUtilSignature() {
        val simpleCode = ExtensionCodeGenerator.generate(ExtensionTemplates.simpleExtendableProject)
        assertTrue(simpleCode.contains("type: Scratch.ArgumentType.EXTENDABLE"))
        assertTrue(simpleCode.contains("text: '[ABC]'"))
        assertTrue(simpleCode.contains("hello(args, util)"))
        assertTrue(simpleCode.contains("util.extendableToArray(args, 'INPUTS', 'ABC')"))

        val nestedCode = ExtensionCodeGenerator.generate(ExtensionTemplates.nestedExtendableProject)
        assertTrue(nestedCode.contains("nested(args, util)"))
        assertTrue(nestedCode.contains("separator: '|'"))
        assertTrue(nestedCode.contains("separator: '||'"))
        assertTrue(nestedCode.contains("HEY: {"))
        assertTrue(nestedCode.contains("ABC: {"))

        val boundedProject = ExtensionTemplates.simpleExtendableProject.copy(
            blocks = listOf(
                ExtensionTemplates.simpleExtendableProject.blocks.first().copy(
                    arguments = listOf(
                        ExtensionTemplates.simpleExtendableProject.blocks.first().arguments.first().copy(
                            extendableDefaultInputs = "3",
                            extendableMinInputs = "1",
                            extendableMaxInputs = "5",
                            extendableAcceptArray = true,
                            extendableSeparator = ", "
                        )
                    )
                )
            )
        )
        val boundedCode = ExtensionCodeGenerator.generate(boundedProject)
        assertTrue(boundedCode.contains("defaultInputs: 3"))
        assertTrue(boundedCode.contains("minInputs: 1"))
        assertTrue(boundedCode.contains("maxInputs: 5"))
        assertTrue(boundedCode.contains("acceptArray: true"))
        assertTrue(boundedCode.contains("separator: ', '"))
    }

    @Test
    fun converterParsesSimpleAndNestedExtendablesWithoutSpuriousKeys() {
        val nestedDocSnippet = """
            class NestedExtendable {
              getInfo() {
                return {
                  id: "nestedextendableexample",
                  name: "Nested Extendable",
                  blocks: [
                    {
                      opcode: "nested",
                      blockType: Scratch.BlockType.COMMAND,
                      text: "nested [INPUTS]",
                      arguments: {
                        INPUTS: {
                          type: Scratch.ArgumentType.EXTENDABLE,
                          text: "layer 1 [HEY] [NUMBER]",
                          defaultInputs: 3,
                          minInputs: 1,
                          maxInputs: 6,
                          acceptArray: true,
                          separator: "|",
                          arguments: {
                            HEY: {
                              type: Scratch.ArgumentType.EXTENDABLE,
                              text: "layer 2 [ABC]",
                              separator: "||",
                              arguments: {
                                ABC: {
                                  type: Scratch.ArgumentType.NUMBER,
                                  defaultValue: 5
                                }
                              }
                            },
                            NUMBER: {
                              type: Scratch.ArgumentType.NUMBER,
                              defaultValue: 10
                            }
                          }
                        }
                      }
                    }
                  ]
                };
              }
              nested(args, util) {
                const numbers = util.extendableToArray(args, "INPUTS", "NUMBER");
                console.log(numbers);
              }
            }
            Scratch.extensions.register(new NestedExtendable());
        """.trimIndent()

        val parsed = SandboxedConverter.convertToUnsandboxed(nestedDocSnippet).extractedProject
        assertNotNull(parsed)
        val block = parsed?.blocks?.firstOrNull()
        assertNotNull(block)
        assertEquals(1, block?.arguments?.size)
        val inputsArg = block?.arguments?.first()
        assertEquals("INPUTS", inputsArg?.name)
        assertEquals(ScratchArgumentType.EXTENDABLE, inputsArg?.type)
        assertEquals("layer 1 [HEY] [NUMBER]", inputsArg?.extendableText)
        assertEquals("3", inputsArg?.extendableDefaultInputs)
        assertEquals("1", inputsArg?.extendableMinInputs)
        assertEquals("6", inputsArg?.extendableMaxInputs)
        assertEquals(true, inputsArg?.extendableAcceptArray)
        assertEquals("|", inputsArg?.extendableSeparator)
        assertEquals(listOf("HEY", "NUMBER"), inputsArg?.extendableArguments?.map { it.name })

        val heyArg = inputsArg?.extendableArguments?.first()
        assertEquals(ScratchArgumentType.EXTENDABLE, heyArg?.type)
        assertEquals("||", heyArg?.extendableSeparator)
        assertEquals(listOf("ABC"), heyArg?.extendableArguments?.map { it.name })
        assertEquals(ScratchArgumentType.NUMBER, heyArg?.extendableArguments?.first()?.type)
    }

    @Test
    fun simulatorEvaluatesSimpleAndNestedExtendables() {
        val simpleBlock = ExtensionTemplates.simpleExtendableProject.blocks.first()
        val defaultSimpleJson = BlockSimulator.buildDefaultExtendableJson(simpleBlock.arguments.first())
        val (_, simpleLog) = BlockSimulator.execute(
            project = ExtensionTemplates.simpleExtendableProject,
            block = simpleBlock,
            argValues = mapOf("INPUTS" to defaultSimpleJson),
            currentVm = VmSimulationState()
        )
        assertFalse(simpleLog.isError)
        assertEquals("Returned: 'hellohello'", simpleLog.output)

        val customSimpleJson = "[{\"ABC\": \"foo\"}, {\"ABC\": \"bar\"}, {\"ABC\": \"!\"}]"
        val (_, customLog) = BlockSimulator.execute(
            project = ExtensionTemplates.simpleExtendableProject,
            block = simpleBlock,
            argValues = mapOf("INPUTS" to customSimpleJson),
            currentVm = VmSimulationState()
        )
        assertFalse(customLog.isError)
        assertEquals("Returned: 'foobar!'", customLog.output)

        val nestedBlock = ExtensionTemplates.nestedExtendableProject.blocks.first()
        val defaultNestedJson = BlockSimulator.buildDefaultExtendableJson(nestedBlock.arguments.first())
        val (_, nestedLog) = BlockSimulator.execute(
            project = ExtensionTemplates.nestedExtendableProject,
            block = nestedBlock,
            argValues = mapOf("INPUTS" to defaultNestedJson),
            currentVm = VmSimulationState()
        )
        assertFalse(nestedLog.isError)
        assertTrue(nestedLog.output.contains("Numbers on layer one: [10, 10]"))
        assertTrue(nestedLog.output.contains("Numbers on layer two: [[5, 5], [5, 5]]"))
        assertTrue(nestedLog.output.contains("Input 0 has 2 items in layer 2"))
        assertTrue(nestedLog.output.contains("Input 1 has 2 items in layer 2"))
    }

    @Test
    fun linterDetectsInvalidExtendableBoundsAndMissingChildren() {
        val brokenExtendable = ExtensionTemplates.simpleExtendableProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "badExt",
                    blockType = ScratchBlockType.REPORTER,
                    text = "bad [INPUTS]",
                    arguments = listOf(
                        BlockArgument(
                            name = "INPUTS",
                            type = ScratchArgumentType.EXTENDABLE,
                            extendableText = "",
                            extendableDefaultInputs = "8",
                            extendableMinInputs = "4",
                            extendableMaxInputs = "2",
                            extendableArguments = emptyList()
                        )
                    ),
                    jsBody = "return '';"
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(brokenExtendable)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("minInputs exceeds maxInputs") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("defaultInputs out of range") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("Incomplete extendable") })
        assertTrue(issues.any { it.severity == LintSeverity.INFO && it.title.contains("util.extendableToArray") })
    }

    @Test
    fun generatorEmitsConditionalLoopBranchesArrayTextAndUtilSignature() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.branchesProject)
        assertTrue(code.contains("blockType: Scratch.BlockType.CONDITIONAL"))
        assertTrue(code.contains("blockType: Scratch.BlockType.LOOP"))
        assertTrue(code.contains("text: ['hello', 'neighbor']"))
        assertTrue(code.contains("branchCount: 1"))
        assertTrue(code.contains("branchCount: 2"))
        assertTrue(code.contains("text: ['if [COND] then', 'else']"))
        assertTrue(code.contains("branch(args, util) {"))
        assertTrue(code.contains("repeatLoop(args, util) {"))
        assertTrue(code.contains("ifElseBranch(args, util) {"))
        assertTrue(code.contains("util.startBranch(1, false);"))
        assertTrue(code.contains("util.startBranch(1, true);"))
    }

    @Test
    fun converterParsesDocsBranchesExtension() {
        val docSnippet = """
            class Branches {
              getInfo() {
                return {
                  id: "branchesexample",
                  name: "Branches",
                  blocks: [
                    {
                      opcode: "branch",
                      blockType: Scratch.BlockType.CONDITIONAL,
                      text: ["hello", "neighbor"],
                      branchCount: 1
                    }
                  ]
                };
              }

              branch(args, util) {
                util.startBranch(1, false);
              }
            }
            Scratch.extensions.register(new Branches());
        """.trimIndent()

        val parsed = SandboxedConverter.convertToUnsandboxed(docSnippet).extractedProject
        assertNotNull(parsed)
        val block = parsed?.blocks?.firstOrNull()
        assertNotNull(block)
        assertEquals("branch", block?.opcode)
        assertEquals(ScratchBlockType.CONDITIONAL, block?.blockType)
        assertEquals("hello", block?.text)
        assertEquals(1, block?.branchCount)
        assertEquals(listOf("neighbor"), block?.branchTexts)
        assertTrue(block?.jsBody?.contains("util.startBranch(1, false)") == true)
    }

    @Test
    fun simulatorEvaluatesBranchesSinglePassLoopAndIfElse() {
        val blocks = ExtensionTemplates.branchesProject.blocks
        val branchBlock = blocks[0]
        val (_, branchLog) = BlockSimulator.execute(
            project = ExtensionTemplates.branchesProject,
            block = branchBlock,
            argValues = emptyMap(),
            currentVm = VmSimulationState()
        )
        assertFalse(branchLog.isError)
        assertEquals("Started branch 1 (single pass) • Conditional", branchLog.output)

        val loopBlock = blocks[1]
        val (_, loopLog) = BlockSimulator.execute(
            project = ExtensionTemplates.branchesProject,
            block = loopBlock,
            argValues = mapOf("COUNT" to "3"),
            currentVm = VmSimulationState()
        )
        assertFalse(loopLog.isError)
        assertEquals("Started branch 1 (looping) • Loop", loopLog.output)

        val ifElseBlock = blocks[2]
        val (_, trueLog) = BlockSimulator.execute(
            project = ExtensionTemplates.branchesProject,
            block = ifElseBlock,
            argValues = mapOf("COND" to "true"),
            currentVm = VmSimulationState()
        )
        assertEquals("Started branch 1 (single pass) • Conditional", trueLog.output)

        val (_, falseLog) = BlockSimulator.execute(
            project = ExtensionTemplates.branchesProject,
            block = ifElseBlock,
            argValues = mapOf("COND" to "false"),
            currentVm = VmSimulationState()
        )
        assertEquals("Started branch 2 (single pass) • Conditional", falseLog.output)
    }

    @Test
    fun linterValidatesBranchCountRulesAndLoopRecommendation() {
        val badBranchesProject = ExtensionTemplates.branchesProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "invalidCmdBranch",
                    blockType = ScratchBlockType.COMMAND,
                    text = "bad command branch",
                    branchCount = 1,
                    jsBody = "console.log('hi');"
                ),
                ExtensionBlock(
                    opcode = "loopingConditional",
                    blockType = ScratchBlockType.CONDITIONAL,
                    text = "forever cond",
                    branchCount = 1,
                    jsBody = "util.startBranch(1, true);"
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(badBranchesProject)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("branchCount invalid") })
        assertTrue(issues.any { it.severity == LintSeverity.INFO && it.title.contains("Consider Scratch.BlockType.LOOP") })
    }

    @Test
    fun generatorEmitsExtraMenuPropertiesAcceptTextAndSeparators() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.extraMenuPropertiesProject)
        assertTrue(code.contains("colors: {"))
        assertTrue(code.contains("acceptText: true,"))
        assertTrue(code.contains("items: ['red', 'green', 'blue', '---', 'cyan', 'magenta', 'yellow']"))
        assertTrue(code.contains("food: {"))
        assertTrue(code.contains("acceptReporters: false,"))
        assertTrue(code.contains("items: ['water', 'juice', '---', 'salad', 'soup']"))
    }

    @Test
    fun converterParsesDocsExtraMenuPropertiesExtension() {
        val docSnippet = """
            class ExtraMenuProperties {
              getInfo() {
                return {
                  id: "extramenupropertiesexample",
                  name: "Extra Menu Properties",
                  blocks: [
                    {
                      opcode: "choose",
                      blockType: Scratch.BlockType.REPORTER,
                      text: "choose [COLOR]",
                      arguments: {
                        COLOR: {
                          type: Scratch.ArgumentType.STRING,
                          menu: "colors",
                          defaultValue: "red"
                        }
                      }
                    }
                  ],
                  menus: {
                    colors: {
                      acceptText: true,
                      items: [
                        "red", "green", "blue",
                        "---",
                        "cyan", "magenta", "yellow"
                      ]
                    }
                  }
                };
              }

              choose(args) {
                return args.COLOR;
              }
            }
            Scratch.extensions.register(new ExtraMenuProperties());
        """.trimIndent()

        val parsed = SandboxedConverter.convertToUnsandboxed(docSnippet).extractedProject
        assertNotNull(parsed)
        val colorsMenu = parsed?.menus?.find { it.name == "colors" }
        assertNotNull(colorsMenu)
        assertEquals(true, colorsMenu?.acceptText)
        assertEquals(true, colorsMenu?.acceptReporters)
        assertEquals(
            listOf("red", "green", "blue", "---", "cyan", "magenta", "yellow"),
            colorsMenu?.items
        )
        assertEquals("colors", parsed?.blocks?.firstOrNull()?.arguments?.firstOrNull()?.menuName)
    }

    @Test
    fun menuHelpersSkipSeparatorsAndSimulatorEvaluatesCustomText() {
        val leadingSeparatorMenu = ExtensionMenu(
            name = "leadingSep",
            items = listOf("---", "alpha", "---", "beta")
        )
        assertEquals("alpha", leadingSeparatorMenu.firstSelectableItem)
        assertEquals(listOf("alpha", "beta"), leadingSeparatorMenu.selectableItems)

        val chooseBlock = ExtensionTemplates.extraMenuPropertiesProject.blocks.first()
        val (_, customTextLog) = BlockSimulator.execute(
            project = ExtensionTemplates.extraMenuPropertiesProject,
            block = chooseBlock,
            argValues = mapOf("COLOR" to "purple"),
            currentVm = VmSimulationState()
        )
        assertFalse(customTextLog.isError)
        assertEquals("Returned: 'purple'", customTextLog.output)
    }

    @Test
    fun linterRejectsSeparatorOnlyMenusAndSeparatorDefaultValues() {
        val brokenMenuProject = ExtensionTemplates.extraMenuPropertiesProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "pickBroken",
                    blockType = ScratchBlockType.REPORTER,
                    text = "pick [OPT]",
                    arguments = listOf(
                        BlockArgument(
                            name = "OPT",
                            type = ScratchArgumentType.STRING,
                            defaultValue = "---",
                            menuName = "sepOnly"
                        )
                    ),
                    jsBody = "return args.OPT;"
                )
            ),
            menus = listOf(
                ExtensionMenu(
                    name = "sepOnly",
                    items = listOf("---", "---")
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(brokenMenuProject)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("has no selectable items") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("Separator '---' cannot be defaultValue") })
    }

    @Test
    fun generatorEmitsDependentDropdownMappingDefaultOptionsAndObjectEntries() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.dependentDropdownsProject)
        assertTrue(code.contains("parentName: 'ANIMAL'"))
        assertTrue(code.contains("optionMapping: {"))
        assertTrue(code.contains("cat: ['Siamese', 'Tabby']"))
        assertTrue(code.contains("dog: ['Beagle', 'Poodle']"))
        assertTrue(code.contains("defaultOptions: ['unknown']"))
        assertTrue(code.contains("parentName: 'BREED'"))
        assertFalse(code.substringAfter("BREED_MENU:").substringBefore("COAT_MENU:").contains("acceptText:"))

        val objectItemMenu = ExtensionMenu(
            name = "BREED_OBJ",
            parentName = "ANIMAL",
            acceptReporters = false,
            optionMapping = mapOf(
                "cat" to listOf(
                    MenuItemEntry("Tabby"),
                    MenuItemEntry(value = "siamese", text = "Siamese cat")
                )
            )
        )
        val objCode = ExtensionCodeGenerator.generate(
            ExtensionTemplates.dependentDropdownsProject.copy(menus = listOf(objectItemMenu))
        )
        assertTrue(objCode.contains("{ text: 'Siamese cat', value: 'siamese' }"))
    }

    @Test
    fun converterParsesDocsDependentDropdownsAndObjectMenuItems() {
        val docSnippet = """
            class DependentDropdowns {
              getInfo() {
                return {
                  id: "dependentdropdownsexample",
                  name: "Dependent Dropdowns",
                  blocks: [
                    {
                      opcode: "describeAnimalCoat",
                      blockType: Scratch.BlockType.REPORTER,
                      text: "[ANIMAL] breed [BREED] with [COAT] coat",
                      arguments: {
                        ANIMAL: { type: Scratch.ArgumentType.STRING, menu: "ANIMAL_MENU", defaultValue: "cat" },
                        BREED: { type: Scratch.ArgumentType.STRING, menu: "BREED_MENU", defaultValue: "Tabby" },
                        COAT: { type: Scratch.ArgumentType.STRING, menu: "COAT_MENU", defaultValue: "short" }
                      }
                    }
                  ],
                  menus: {
                    ANIMAL_MENU: { items: ["cat", "dog"] },
                    BREED_MENU: {
                      parentName: "ANIMAL",
                      optionMapping: {
                        cat: ["Tabby", { text: "Siamese cat", value: "siamese" }],
                        dog: ["Beagle", "Poodle"]
                      },
                      defaultOptions: ["unknown"]
                    },
                    COAT_MENU: {
                      parentName: "BREED",
                      optionMapping: {
                        Tabby: ["short", "long"],
                        siamese: ["short"]
                      },
                      defaultOptions: ["unknown"]
                    }
                  }
                };
              }
              describeAnimalCoat(args) {
                return `${'$'}{args.ANIMAL}: ${'$'}{args.BREED} with a ${'$'}{args.COAT} coat`;
              }
            }
            Scratch.extensions.register(new DependentDropdowns());
        """.trimIndent()

        val parsed = SandboxedConverter.convertToUnsandboxed(docSnippet).extractedProject
        assertNotNull(parsed)
        val breedMenu = parsed?.menus?.find { it.name == "BREED_MENU" }
        assertNotNull(breedMenu)
        assertEquals("ANIMAL", breedMenu?.parentName)
        assertEquals(false, breedMenu?.acceptReporters)
        assertEquals(false, breedMenu?.acceptText)
        assertEquals(
            listOf(MenuItemEntry("Tabby"), MenuItemEntry(value = "siamese", text = "Siamese cat")),
            breedMenu?.optionMapping?.get("cat")
        )
        assertEquals(listOf(MenuItemEntry("unknown")), breedMenu?.defaultOptions)
    }

    @Test
    fun simulatorCascadesChainedDependentDropdownsAndEvaluatesTemplateLiteral() {
        val project = ExtensionTemplates.dependentDropdownsProject
        val coatBlock = project.blocks[1]

        // Changing ANIMAL from cat -> dog cascades BREED -> Beagle and COAT -> smooth
        val dogCascade = BlockSimulator.cascadeDependentMenus(
            block = coatBlock,
            menus = project.menus,
            inputs = mapOf("ANIMAL" to "dog", "BREED" to "Tabby", "COAT" to "short")
        )
        assertEquals("Beagle", dogCascade["BREED"])
        assertEquals("smooth", dogCascade["COAT"])

        // Changing BREED from Tabby -> Siamese keeps COAT = "short" because "short" is still valid
        val siameseCascade = BlockSimulator.cascadeDependentMenus(
            block = coatBlock,
            menus = project.menus,
            inputs = mapOf("ANIMAL" to "cat", "BREED" to "Siamese", "COAT" to "short")
        )
        assertEquals("Siamese", siameseCascade["BREED"])
        assertEquals("short", siameseCascade["COAT"])

        // Unknown parent falls back to defaultOptions ("unknown")
        val unknownCascade = BlockSimulator.cascadeDependentMenus(
            block = coatBlock,
            menus = project.menus,
            inputs = mapOf("ANIMAL" to "bird", "BREED" to "Tabby", "COAT" to "long")
        )
        assertEquals("unknown", unknownCascade["BREED"])
        assertEquals("unknown", unknownCascade["COAT"])

        val (_, log) = BlockSimulator.execute(
            project = project,
            block = coatBlock,
            argValues = dogCascade,
            currentVm = VmSimulationState()
        )
        assertFalse(log.isError)
        assertEquals("Returned: 'dog: Beagle with a smooth coat'", log.output)
    }

    @Test
    fun linterDetectsDependentMenuOrderMissingParentCyclesAndFlags() {
        val badDepProject = ExtensionTemplates.dependentDropdownsProject.copy(
            blocks = listOf(
                ExtensionBlock(
                    opcode = "wrongOrder",
                    blockType = ScratchBlockType.REPORTER,
                    text = "[BREED] of [ANIMAL]",
                    arguments = listOf(
                        BlockArgument(name = "BREED", menuName = "BREED_MENU"),
                        BlockArgument(name = "ANIMAL", menuName = "ANIMAL_MENU")
                    ),
                    jsBody = "return args.BREED;"
                ),
                ExtensionBlock(
                    opcode = "missingParent",
                    blockType = ScratchBlockType.REPORTER,
                    text = "only [BREED]",
                    arguments = listOf(
                        BlockArgument(name = "BREED", menuName = "BREED_MENU")
                    ),
                    jsBody = "return args.BREED;"
                )
            ),
            menus = listOf(
                ExtensionMenu(name = "ANIMAL_MENU", items = listOf("cat", "dog")),
                ExtensionMenu(
                    name = "BREED_MENU",
                    acceptReporters = true,
                    acceptText = true,
                    parentName = "ANIMAL",
                    optionMapping = mapOf("cat" to listOf(MenuItemEntry("Tabby")))
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(badDepProject)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("must appear before") })
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.title.contains("Parent argument 'ANIMAL' missing") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.title.contains("ignores acceptText/acceptReporters") })
    }

    @Test
    fun generatorEmitsMutatorDropdownMenuObjectItemsAndMutatorMetadata() {
        val code = ExtensionCodeGenerator.generate(ExtensionTemplates.mutatorDropdownsProject)
        assertTrue(code.contains("MODE_MENU: {"))
        assertTrue(code.contains("{ text: 'split', value: 'SPLIT' }"))
        assertTrue(code.contains("{ text: 'join', value: 'JOIN' }"))
        assertTrue(code.contains("mutator: {"))
        assertTrue(code.contains("SPLIT: {"))
        assertTrue(code.contains("output: Scratch.BlockType.ARRAY"))
        assertTrue(code.contains("JOIN: {"))
        assertTrue(code.contains("output: Scratch.BlockType.REPORTER"))
        assertTrue(code.contains("type: Scratch.ArgumentType.ARRAY"))
        val menuSection = code.substringAfter("MODE_MENU:").substringBefore("};")
        assertFalse(menuSection.contains("acceptReporters:"))
        assertFalse(menuSection.contains("acceptText:"))
    }

    @Test
    fun converterParsesDocsMutatorDropdownsExtension() {
        val docSnippet = """
            class MutatorDropdownExample {
              getInfo() {
                return {
                  id: "mutatordropdownexample",
                  name: "Mutator Dropdown Example",
                  blocks: [
                    {
                      opcode: "convert",
                      blockType: Scratch.BlockType.REPORTER,
                      text: "[MODE] [INPUT] with delimiter [DELIMITER]",
                      arguments: {
                        MODE: {
                          type: Scratch.ArgumentType.STRING,
                          menu: "MODE_MENU",
                          defaultValue: "SPLIT"
                        },
                        INPUT: {
                          type: Scratch.ArgumentType.STRING,
                          defaultValue: "a,b,c"
                        },
                        DELIMITER: {
                          type: Scratch.ArgumentType.STRING,
                          defaultValue: ","
                        }
                      }
                    }
                  ],
                  menus: {
                    MODE_MENU: {
                      items: [
                        { text: "split", value: "SPLIT" },
                        { text: "join", value: "JOIN" }
                      ],
                      mutator: {
                        SPLIT: {
                          output: Scratch.BlockType.ARRAY,
                          previousStatement: false,
                          inputsInline: true,
                          arguments: {
                            INPUT: {
                              type: Scratch.ArgumentType.STRING,
                              defaultValue: "a,b,c"
                            }
                          }
                        },
                        JOIN: {
                          output: Scratch.BlockType.REPORTER,
                          nextStatement: false,
                          inputsInline: false,
                          arguments: {
                            INPUT: {
                              type: Scratch.ArgumentType.ARRAY
                            }
                          }
                        }
                      }
                    }
                  }
                };
              }

              convert(args) {
                if (args.MODE === "JOIN") {
                  const arr = Array.isArray(args.INPUT) ? args.INPUT : [args.INPUT];
                  return arr.join(args.DELIMITER);
                }
                return String(args.INPUT).split(args.DELIMITER);
              }
            }
            Scratch.extensions.register(new MutatorDropdownExample());
        """.trimIndent()

        val parsed = SandboxedConverter.convertToUnsandboxed(docSnippet).extractedProject
        assertNotNull(parsed)
        val modeMenu = parsed?.menus?.find { it.name == "MODE_MENU" }
        assertNotNull(modeMenu)
        assertEquals(true, modeMenu?.isMutator)
        assertEquals(false, modeMenu?.acceptReporters)
        assertEquals(
            listOf(
                MenuItemEntry(value = "SPLIT", text = "split"),
                MenuItemEntry(value = "JOIN", text = "join")
            ),
            modeMenu?.regularEntries
        )
        val splitState = modeMenu?.mutator?.get("SPLIT")
        assertNotNull(splitState)
        assertEquals(ScratchBlockType.ARRAY, splitState?.output)
        assertEquals(false, splitState?.previousStatement)
        assertEquals(true, splitState?.inputsInline)
        assertEquals(ScratchArgumentType.STRING, splitState?.arguments?.firstOrNull()?.type)
        assertEquals("a,b,c", splitState?.arguments?.firstOrNull()?.defaultValue)

        val joinState = modeMenu?.mutator?.get("JOIN")
        assertNotNull(joinState)
        assertEquals(ScratchBlockType.REPORTER, joinState?.output)
        assertEquals(false, joinState?.nextStatement)
        assertEquals(false, joinState?.inputsInline)
        assertEquals(ScratchArgumentType.ARRAY, joinState?.arguments?.firstOrNull()?.type)
    }

    @Test
    fun simulatorMorphsMutatedBlockAndPreservesUnaffectedArgs() {
        val project = ExtensionTemplates.mutatorDropdownsProject
        val convertBlock = project.blocks.first()

        // Default SPLIT mutator yields ARRAY output & STRING INPUT
        val splitSpec = BlockSimulator.resolveMutatedBlock(convertBlock, project.menus, emptyMap())
        assertEquals(ScratchBlockType.ARRAY, splitSpec.block.blockType)
        assertEquals(ScratchArgumentType.STRING, splitSpec.block.arguments.find { it.name == "INPUT" }?.type)

        // Switching MODE to JOIN morphs output to REPORTER, INPUT to ARRAY, resets INPUT, keeps DELIMITER
        val switchedInputs = BlockSimulator.applyInputChange(
            project = project,
            block = convertBlock,
            changedArgName = "MODE",
            newValue = "JOIN",
            existingInputs = mapOf("MODE" to "SPLIT", "INPUT" to "x,y,z", "DELIMITER" to "-")
        )
        assertEquals("JOIN", switchedInputs["MODE"])
        assertEquals("-", switchedInputs["DELIMITER"])
        assertEquals("", switchedInputs["INPUT"])

        val switchedBackInputs = BlockSimulator.applyInputChange(
            project = project,
            block = convertBlock,
            changedArgName = "MODE",
            newValue = "SPLIT",
            existingInputs = switchedInputs + ("INPUT" to "[\"x\", \"y\"]")
        )
        assertEquals("SPLIT", switchedBackInputs["MODE"])
        assertEquals("-", switchedBackInputs["DELIMITER"])
        assertEquals("a,b,c", switchedBackInputs["INPUT"])

        val joinSpec = BlockSimulator.resolveMutatedBlock(convertBlock, project.menus, switchedInputs)
        assertEquals(ScratchBlockType.REPORTER, joinSpec.block.blockType)
        assertEquals(ScratchArgumentType.ARRAY, joinSpec.block.arguments.find { it.name == "INPUT" }?.type)

        // Execute SPLIT
        val (_, splitLog) = BlockSimulator.execute(
            project = project,
            block = convertBlock,
            argValues = mapOf("MODE" to "SPLIT", "INPUT" to "a,b,c", "DELIMITER" to ","),
            currentVm = VmSimulationState()
        )
        assertFalse(splitLog.isError)
        assertEquals("Returned: [\"a\", \"b\", \"c\"]", splitLog.output)

        // Execute JOIN
        val (_, joinLog) = BlockSimulator.execute(
            project = project,
            block = convertBlock,
            argValues = mapOf("MODE" to "JOIN", "INPUT" to "[\"x\", \"y\", \"z\"]", "DELIMITER" to "-"),
            currentVm = VmSimulationState()
        )
        assertFalse(joinLog.isError)
        assertEquals("Returned: 'x-y-z'", joinLog.output)
    }

    @Test
    fun linterValidatesMutatorDropdownConstraintsAndFlags() {
        val badMutatorProject = ExtensionTemplates.mutatorDropdownsProject.copy(
            menus = listOf(
                ExtensionMenu(
                    name = "MODE_MENU",
                    acceptReporters = true,
                    acceptText = true,
                    parentName = "OTHER",
                    itemEntries = listOf(MenuItemEntry("SPLIT", "split")),
                    items = listOf("SPLIT"),
                    mutator = linkedMapOf(
                        "SPLIT" to MenuMutatorState(
                            output = ScratchBlockType.ARRAY,
                            previousStatement = false
                        ),
                        "MISSING_KEY" to MenuMutatorState(
                            output = ScratchBlockType.REPORTER,
                            nextStatement = false
                        )
                    )
                )
            )
        )
        val issues = ExtensionCodeGenerator.analyzeProject(badMutatorProject)
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.id.startsWith("mutator_with_dependent_") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.id.startsWith("mutator_unsupported_flags_") })
        assertTrue(issues.any { it.severity == LintSeverity.WARNING && it.id.startsWith("mutator_orphan_key_") })
        assertTrue(issues.any { it.severity == LintSeverity.INFO && it.id.startsWith("mutator_statement_removal_") })
    }
}
