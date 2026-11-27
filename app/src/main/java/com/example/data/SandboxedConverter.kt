package com.example.data

import com.example.model.BlockArgument
import com.example.model.BlockSwitch
import com.example.model.BlockSwitchInputMapping
import com.example.model.ConversionResult
import com.example.model.ConversionStep
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

object SandboxedConverter {

    fun convertToUnsandboxed(rawInput: String): ConversionResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return ConversionResult(
                convertedCode = "",
                steps = emptyList(),
                extractedProject = null,
                warnings = listOf("Paste a sandboxed or legacy Scratch extension script to convert.")
            )
        }

        val warnings = mutableListOf<String>()
        val steps = mutableListOf<ConversionStep>()

        // 1. Detect and strip any outer IIFE if already present so we can normalize cleanly
        var innerCode = trimmed
        val hadIife = Regex("^\\(?function\\s*\\(\\s*Scratch\\s*\\)").containsMatchIn(trimmed)
        if (hadIife) {
            innerCode = innerCode
                .replaceFirst(Regex("^\\(?function\\s*\\(\\s*Scratch\\s*\\)\\s*\\{"), "")
                .replaceFirst(Regex("\\}?\\s*\\)\\s*\\(\\s*Scratch\\s*\\)\\s*;?\\s*$"), "")
                .trim()
        }

        // Remove existing 'use strict'; so we place it deterministically at the top
        val hadStrict = innerCode.contains("'use strict'") || innerCode.contains("\"use strict\"")
        innerCode = innerCode
            .replace(Regex("['\"]use strict['\"];?\\s*"), "")
            .trim()

        // 2. Detect leaked top-level `const vm = Scratch.vm;` or `var vm = Scratch.vm`
        val leakedVmRegex = Regex("(?m)^\\s*(?:const|let|var)\\s+vm\\s*=\\s*Scratch\\.vm\\s*;?\\s*$")
        val hadGlobalVm = leakedVmRegex.containsMatchIn(innerCode)
        val usesVm = innerCode.contains("Scratch.vm") || innerCode.contains("vm.")
        if (hadGlobalVm) {
            innerCode = leakedVmRegex.replace(innerCode, "").trim()
        }

        // Remove existing unsandboxed check to avoid duplication
        val hadGuard = innerCode.contains("Scratch.extensions.unsandboxed")
        innerCode = innerCode
            .replace(Regex("if\\s*\\(\\s*!Scratch\\.extensions\\.unsandboxed\\s*\\)\\s*\\{[^}]*\\}\\s*"), "")
            .trim()

        // Remove "// BAD CODE" comments if present from examples
        innerCode = innerCode
            .replace(Regex("(?m)^\\s*//\\s*BAD CODE.*$"), "")
            .trim()

        steps.add(
            ConversionStep(
                title = "IIFE Scope Isolation",
                description = if (hadIife) "Normalized existing (function(Scratch) { ... })(Scratch) wrapper"
                else "Wrapped extension inside (function(Scratch) { ... })(Scratch); to stop global leaks",
                applied = true
            )
        )
        steps.add(
            ConversionStep(
                title = "Strict Mode Enabled",
                description = if (hadStrict) "Preserved 'use strict'; directive inside IIFE"
                else "Injected 'use strict'; at top of IIFE",
                applied = true
            )
        )
        steps.add(
            ConversionStep(
                title = "Unsandboxed Runtime Guard",
                description = if (hadGuard) "Normalized Scratch.extensions.unsandboxed check"
                else "Added guard throwing Error if loaded inside a sandboxed iframe",
                applied = true
            )
        )
        if (usesVm || hadGlobalVm) {
            steps.add(
                ConversionStep(
                    title = "Scoped Scratch.vm Inside IIFE",
                    description = if (hadGlobalVm) "Moved leaked top-level 'const vm = Scratch.vm' safely inside IIFE"
                    else "Bound 'const vm = Scratch.vm;' inside IIFE scope",
                    applied = true
                )
            )
        }

        // 3. Extract metadata, blocks, menus to build a full ExtensionProject!
        val extracted = parseExtensionProject(innerCode, usesVm || hadGlobalVm)

        // Check for missing return statements in extracted reporter/boolean blocks
        extracted?.blocks?.forEach { block ->
            if (block.blockType.requiresReturn && !Regex("\\breturn\\b").containsMatchIn(block.jsBody)) {
                warnings.add(
                    "Block '${block.opcode}' (${block.blockType.label}) has no 'return' statement — unsandboxed extensions must return a valid string, number, or boolean."
                )
            }
        }
        if (Regex("(while\\s*\\(\\s*true\\s*\\)|for\\s*\\(\\s*;\\s*;\\s*\\))").containsMatchIn(innerCode)) {
            warnings.add(
                "Potential infinite loop detected. Unsandboxed loops block the main editor thread and can cause data loss."
            )
        }

        val finalScript = buildString {
            appendLine("(function(Scratch) {")
            appendLine("  'use strict';")
            appendLine()
            appendLine("  if (!Scratch.extensions.unsandboxed) {")
            appendLine("    throw new Error('This extension must run unsandboxed');")
            appendLine("  }")
            appendLine()
            if (usesVm || hadGlobalVm) {
                appendLine("  const vm = Scratch.vm;")
                appendLine()
            }
            innerCode.lines().forEach { line ->
                if (line.isBlank()) {
                    appendLine()
                } else {
                    appendLine("  $line")
                }
            }
            append("})(Scratch);")
        }

        return ConversionResult(
            convertedCode = finalScript,
            steps = steps,
            extractedProject = extracted,
            warnings = warnings
        )
    }

    private fun parseExtensionProject(code: String, usesVm: Boolean): ExtensionProject? {
        val registeredClassName = Regex("Scratch\\.extensions\\.register\\(\\s*new\\s+([A-Za-z0-9_]+)")
            .find(code)?.groupValues?.getOrNull(1)
        val className = registeredClassName
            ?: Regex("class\\s+([A-Za-z0-9_]+)").find(code)?.groupValues?.getOrNull(1)
            ?: "ConvertedExtension"

        val extId = Regex("\\bid\\s*:\\s*['\"]([^'\"]+)['\"]")
            .find(code)?.groupValues?.getOrNull(1) ?: ExtensionCodeGenerator.sanitizeExtensionId(className)

        val extName = Regex("\\bname\\s*:\\s*['\"]([^'\"]+)['\"]")
            .find(code)?.groupValues?.getOrNull(1) ?: className

        val useUnsandboxedRuntime = Regex("\\buseUnsandboxedRuntime\\s*:\\s*true")
            .containsMatchIn(code)

        val color1 = Regex("\\bcolor1\\s*:\\s*['\"](#[0-9A-Fa-f]{6})['\"]")
            .find(code)?.groupValues?.getOrNull(1) ?: "#FF8A00"
        val color2 = Regex("\\bcolor2\\s*:\\s*['\"](#[0-9A-Fa-f]{6})['\"]")
            .find(code)?.groupValues?.getOrNull(1) ?: "#DF7500"
        val color3 = Regex("\\bcolor3\\s*:\\s*['\"](#[0-9A-Fa-f]{6})['\"]")
            .find(code)?.groupValues?.getOrNull(1) ?: "#B85D00"

        val customShapes = parseCustomShapes(code)
        val customTypes = parseCustomTypes(code)
        val menus = parseMenus(code)
        val blocks = parseBlocks(code)

        return ExtensionProject(
            id = ExtensionCodeGenerator.sanitizeExtensionId(extId),
            name = extName,
            className = className,
            executionMode = ExecutionMode.UNSANDBOXED,
            enforceUnsandboxedGuard = true,
            useUnsandboxedRuntime = useUnsandboxedRuntime,
            bindVmReference = usesVm,
            color1Hex = color1,
            color2Hex = color2,
            color3Hex = color3,
            customShapes = customShapes,
            customTypes = customTypes,
            blocks = blocks,
            menus = menus
        )
    }

    private fun parseCustomShapes(code: String): List<CustomShapeDefinition> {
        val registerRegex = Regex("Scratch\\.BlockShapes\\.register\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*\\{")
        return registerRegex.findAll(code).map { match ->
            val shapeName = match.groupValues[1]
            val braceIdx = code.indexOf('{', match.range.first)
            val objBody = if (braceIdx != -1) extractBalancedBlock(code, braceIdx) ?: "" else ""

            CustomShapeDefinition(
                shapeName = shapeName,
                edgeWidthBody = extractShapeMemberBody(objBody, "edgeWidth"),
                leftEdgeBody = extractShapeMemberBody(objBody, "leftEdge"),
                rightEdgeBody = extractShapeMemberBody(objBody, "rightEdge"),
                paddingBody = extractShapeMemberBody(objBody, "padding")
            )
        }.toList()
    }

    private fun extractShapeMemberBody(objBody: String, memberName: String): String {
        // 1. Arrow function with block body: leftEdge: (steps, w, halfStraight) => { ... }
        val arrowBlockRegex = Regex("${Regex.escape(memberName)}\\s*:\\s*(?:\\([^)]*\\)|[A-Za-z0-9_]+)\\s*=>\\s*\\{")
        arrowBlockRegex.find(objBody)?.let { match ->
            val braceIdx = objBody.indexOf('{', match.range.first)
            if (braceIdx != -1) {
                return (extractBalancedBlock(objBody, braceIdx) ?: "").trimIndent().trim()
            }
        }
        // 2. Concise parenthesized object arrow function: padding: () => ({ left: 15, right: 50 })
        val arrowObjRegex = Regex("${Regex.escape(memberName)}\\s*:\\s*(?:\\([^)]*\\)|[A-Za-z0-9_]+)\\s*=>\\s*\\(\\s*\\{")
        arrowObjRegex.find(objBody)?.let { match ->
            val braceIdx = objBody.indexOf('{', match.range.first)
            if (braceIdx != -1) {
                val inner = (extractBalancedBlock(objBody, braceIdx) ?: "").trim()
                return "return { $inner };"
            }
        }
        // 3. Concise arrow function expression: edgeWidth: (totalHeight) => Math.min(totalHeight / 2, 32)
        val arrowHeaderRegex = Regex("${Regex.escape(memberName)}\\s*:\\s*(?:\\([^)]*\\)|[A-Za-z0-9_]+)\\s*=>\\s*")
        arrowHeaderRegex.find(objBody)?.let { match ->
            var idx = match.range.last + 1
            var parenDepth = 0
            var braceDepth = 0
            val start = idx
            while (idx < objBody.length) {
                when (val ch = objBody[idx]) {
                    '(' -> parenDepth++
                    ')' -> if (parenDepth > 0) parenDepth-- else break
                    '{' -> braceDepth++
                    '}' -> if (braceDepth > 0) braceDepth-- else break
                    ',' -> if (parenDepth == 0 && braceDepth == 0) break
                    '\n' -> if (parenDepth == 0 && braceDepth == 0) break
                }
                idx++
            }
            val expr = objBody.substring(start, idx).trim().removeSuffix(";")
            if (expr.isNotBlank()) return "return $expr;"
        }
        // 4. Method shorthand: leftEdge(steps, w, halfStraight) { ... }
        return extractStaticOrInstanceMethod(objBody, memberName)
    }

    private fun parseCustomTypes(code: String): List<CustomTypeDefinition> {
        val registerRegex = Regex("Scratch\\.types\\.register\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*([A-Za-z0-9_]+)\\s*\\)")
        return registerRegex.findAll(code).map { match ->
            val typeName = match.groupValues[1]
            val clsName = match.groupValues[2]
            val classRegex = Regex("class\\s+${Regex.escape(clsName)}\\s*\\{")
            val clsMatch = classRegex.find(code)
            val clsBody = if (clsMatch != null) {
                val braceIdx = code.indexOf('{', clsMatch.range.first)
                if (braceIdx != -1) extractBalancedBlock(code, braceIdx) ?: "" else ""
            } else {
                ""
            }

            val shapeMatch = Regex("static\\s+get\\s+shape\\s*\\(\\)\\s*\\{[^}]*return\\s+([^;\\n}]+)")
                .find(clsBody)?.groupValues?.getOrNull(1)?.trim() ?: ""
            val (shapeEnum, customShapeName) = when {
                shapeMatch.contains("SQUARE") -> CustomTypeShape.SQUARE to ""
                shapeMatch.contains("HEXAGONAL") -> CustomTypeShape.HEXAGONAL to ""
                shapeMatch.contains("LEAF") -> CustomTypeShape.LEAF to ""
                shapeMatch.contains("PLUS") -> CustomTypeShape.PLUS to ""
                shapeMatch.contains("OCTAGONAL") -> CustomTypeShape.OCTAGONAL to ""
                shapeMatch.startsWith("'") || shapeMatch.startsWith("\"") ->
                    CustomTypeShape.CUSTOM to shapeMatch.trim('\'', '"')
                else -> CustomTypeShape.DEFAULT to ""
            }

            CustomTypeDefinition(
                typeName = typeName,
                className = clsName,
                shape = shapeEnum,
                customShapeName = customShapeName,
                constructorBody = extractMethodBody(clsBody, "constructor")
                    .ifBlank { "this.value = value;" },
                castBody = extractStaticOrInstanceMethod(clsBody, "cast"),
                toStringBody = extractStaticOrInstanceMethod(clsBody, "toString"),
                valueOfBody = extractStaticOrInstanceMethod(clsBody, "valueOf"),
                toJsonBody = extractStaticOrInstanceMethod(clsBody, "toJSON"),
                fromJsonBody = extractStaticOrInstanceMethod(clsBody, "fromJSON"),
                visualReportBody = extractStaticOrInstanceMethod(clsBody, "visualReport"),
                monitorContentBody = extractStaticOrInstanceMethod(clsBody, "monitorContent")
            )
        }.toList()
    }

    private fun extractStaticOrInstanceMethod(classBody: String, methodName: String): String {
        val regex = Regex("(?m)^\\s*(?:static\\s+)?(?:async\\s+)?${Regex.escape(methodName)}\\s*\\([^)]*\\)\\s*\\{")
        val match = regex.find(classBody) ?: return ""
        val braceIdx = classBody.indexOf('{', match.range.first)
        if (braceIdx == -1) return ""
        val body = extractBalancedBlock(classBody, braceIdx) ?: return ""
        return body.trimIndent().trim()
    }

    private fun parseMenus(code: String): List<ExtensionMenu> {
        val menusIdx = code.indexOf("menus:")
        if (menusIdx == -1) return emptyList()
        val braceStart = code.indexOf('{', menusIdx)
        if (braceStart == -1) return emptyList()
        val menusBlock = extractBalancedBlock(code, braceStart) ?: return emptyList()

        val result = mutableListOf<ExtensionMenu>()
        val menuHeaderRegex = Regex("([A-Za-z0-9_]+)\\s*:\\s*\\{")
        for (match in menuHeaderRegex.findAll(menusBlock)) {
            val menuName = match.groupValues[1]
            val bodyStart = match.range.last
            val menuBody = extractBalancedBlock(menusBlock, bodyStart) ?: continue
            val parentName = extractTopLevelStringProp(menuBody, "parentName") ?: ""
            val isDependent = parentName.isNotBlank()
            val mutator = if (!isDependent) parseMutatorMap(menuBody) else emptyMap()
            val isMutator = mutator.isNotEmpty()
            val acceptReporters = Regex("\\bacceptReporters\\s*:\\s*(true|false)")
                .find(menuBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
                ?: (!isDependent && !isMutator)
            val acceptText = Regex("\\bacceptText\\s*:\\s*(true|false)")
                .find(menuBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull() ?: false

            val optionMapping = if (isDependent) parseOptionMapping(menuBody) else emptyMap()
            val defaultOptions = if (isDependent) {
                val defIdx = menuBody.indexOf("defaultOptions")
                val bracketStart = if (defIdx != -1) menuBody.indexOf('[', defIdx) else -1
                val arrInner = if (bracketStart != -1) extractBalancedBrackets(menuBody, bracketStart) else null
                if (arrInner != null) parseMenuEntriesArray(arrInner) else emptyList()
            } else {
                emptyList()
            }

            val itemsIdx = menuBody.indexOf("items:")
            val itemsBracket = if (itemsIdx != -1) menuBody.indexOf('[', itemsIdx) else -1
            val itemsRaw = if (itemsBracket != -1) extractBalancedBrackets(menuBody, itemsBracket) ?: "" else ""
            val itemEntries = parseMenuEntriesArray(itemsRaw)
            val items = itemEntries.map { it.value }
            result.add(
                ExtensionMenu(
                    name = menuName,
                    acceptReporters = acceptReporters,
                    acceptText = acceptText,
                    items = if (isDependent) emptyList() else items.ifEmpty { listOf("item1", "item2") },
                    itemEntries = if (isDependent) emptyList() else itemEntries,
                    parentName = parentName,
                    optionMapping = optionMapping,
                    defaultOptions = defaultOptions,
                    mutator = mutator
                )
            )
        }
        return result
    }

    private fun parseMutatorMap(menuBody: String): Map<String, MenuMutatorState> {
        val mutHeader = Regex("\\bmutator\\s*:\\s*\\{").find(menuBody) ?: return emptyMap()
        val openBrace = menuBody.indexOf('{', mutHeader.range.first)
        if (openBrace == -1) return emptyMap()
        val mutBody = extractBalancedBlock(menuBody, openBrace) ?: return emptyMap()

        val result = linkedMapOf<String, MenuMutatorState>()
        var i = 0
        while (i < mutBody.length) {
            while (i < mutBody.length && (mutBody[i].isWhitespace() || mutBody[i] == ',')) i++
            if (i >= mutBody.length) break
            val tail = mutBody.substring(i)
            val keyMatch = Regex("^(?:'((?:[^'\\\\]|\\\\.)*)'|\"((?:[^\"\\\\]|\\\\.)*)\"|([A-Za-z0-9_]+))\\s*:\\s*\\{")
                .find(tail)
            if (keyMatch == null) {
                i++
                continue
            }
            val key = keyMatch.groupValues[1].ifEmpty {
                keyMatch.groupValues[2].ifEmpty { keyMatch.groupValues[3] }
            }
            val braceIdx = i + keyMatch.range.last
            val stateBody = extractBalancedBlock(mutBody, braceIdx) ?: break
            val outputStr = Regex("\\boutput\\s*:\\s*Scratch\\.BlockType\\.([A-Z_]+)")
                .find(stateBody)?.groupValues?.getOrNull(1)
            val outputType = outputStr?.let { o -> ScratchBlockType.entries.find { it.name == o } }
            val previousStatement = Regex("\\bpreviousStatement\\s*:\\s*(true|false)")
                .find(stateBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
            val nextStatement = Regex("\\bnextStatement\\s*:\\s*(true|false)")
                .find(stateBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
            val inputsInline = Regex("\\binputsInline\\s*:\\s*(true|false)")
                .find(stateBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
            val mutArgs = parseArguments(stateBody)
            result[key] = MenuMutatorState(
                output = outputType,
                arguments = mutArgs,
                previousStatement = previousStatement,
                nextStatement = nextStatement,
                inputsInline = inputsInline
            )
            i = braceIdx + stateBody.length + 2
        }
        return result
    }

    private fun parseOptionMapping(menuBody: String): Map<String, List<MenuItemEntry>> {
        val mapHeader = Regex("\\boptionMapping\\s*:\\s*\\{").find(menuBody) ?: return emptyMap()
        val openBrace = menuBody.indexOf('{', mapHeader.range.first)
        if (openBrace == -1) return emptyMap()
        val mappingBody = extractBalancedBlock(menuBody, openBrace) ?: return emptyMap()

        val result = linkedMapOf<String, List<MenuItemEntry>>()
        var i = 0
        while (i < mappingBody.length) {
            while (i < mappingBody.length && (mappingBody[i].isWhitespace() || mappingBody[i] == ',')) i++
            if (i >= mappingBody.length) break
            val tail = mappingBody.substring(i)
            val keyMatch = Regex("^(?:'((?:[^'\\\\]|\\\\.)*)'|\"((?:[^\"\\\\]|\\\\.)*)\"|([A-Za-z0-9_]+))\\s*:\\s*\\[")
                .find(tail)
            if (keyMatch == null) {
                i++
                continue
            }
            val key = (keyMatch.groupValues[1].ifEmpty {
                keyMatch.groupValues[2].ifEmpty { keyMatch.groupValues[3] }
            })
            val bracketIdx = i + keyMatch.range.last
            val arrInner = extractBalancedBrackets(mappingBody, bracketIdx) ?: break
            result[key] = parseMenuEntriesArray(arrInner)
            i = bracketIdx + arrInner.length + 2
        }
        return result
    }

    private fun parseMenuEntriesArray(arrayInner: String): List<MenuItemEntry> {
        val entries = mutableListOf<MenuItemEntry>()
        var i = 0
        while (i < arrayInner.length) {
            val ch = arrayInner[i]
            when {
                ch == '\'' || ch == '"' -> {
                    val m = Regex("^('((?:[^'\\\\]|\\\\.)*)'|\"((?:[^\"\\\\]|\\\\.)*)\")").find(arrayInner.substring(i))
                    if (m != null) {
                        val raw = m.groupValues[2].ifEmpty { m.groupValues[3] }
                            .replace("\\'", "'")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        entries.add(MenuItemEntry(value = raw, text = raw))
                        i += m.value.length
                    } else {
                        i++
                    }
                }
                ch == '{' -> {
                    val objBody = extractBalancedBlock(arrayInner, i) ?: break
                    val textProp = extractTopLevelStringProp(objBody, "text")
                    val valProp = extractTopLevelStringProp(objBody, "value")
                    val resolvedVal = valProp ?: textProp ?: ""
                    val resolvedText = textProp ?: resolvedVal
                    if (resolvedVal.isNotEmpty() || resolvedText.isNotEmpty()) {
                        entries.add(MenuItemEntry(value = resolvedVal, text = resolvedText))
                    }
                    i += objBody.length + 2
                }
                else -> i++
            }
        }
        return entries
    }

    private fun parseBlocks(code: String): List<ExtensionBlock> {
        val blocksIdx = code.indexOf("blocks:")
        if (blocksIdx == -1) return emptyList()
        val bracketStart = code.indexOf('[', blocksIdx)
        if (bracketStart == -1) return emptyList()
        val blocksArray = extractBalancedBrackets(code, bracketStart) ?: return emptyList()

        val result = mutableListOf<ExtensionBlock>()
        var cursor = 0
        while (cursor < blocksArray.length) {
            val objStart = blocksArray.indexOf('{', cursor)
            if (objStart == -1) break
            val objBody = extractBalancedBlock(blocksArray, objStart) ?: break
            cursor = objStart + objBody.length + 2

            val opcode = Regex("opcode\\s*:\\s*['\"]([^'\"]+)['\"]")
                .find(objBody)?.groupValues?.getOrNull(1) ?: continue
            val typeStr = Regex("blockType\\s*:\\s*Scratch\\.BlockType\\.([A-Z_]+)")
                .find(objBody)?.groupValues?.getOrNull(1) ?: "COMMAND"
            val blockType = ScratchBlockType.entries.find { it.name == typeStr }
                ?: ScratchBlockType.COMMAND
            val (text, branchTexts) = extractTopLevelTextAndBranches(objBody, opcode)
            val branchCount = Regex("\\bbranchCount\\s*:\\s*([0-9]+)")
                .find(objBody)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: if (blockType == ScratchBlockType.CONDITIONAL || blockType == ScratchBlockType.LOOP) 1 else 0
            val dualTypeStr = Regex("dualType\\s*:\\s*Scratch\\.BlockType\\.([A-Z_]+)")
                .find(objBody)?.groupValues?.getOrNull(1)
            val dualType = dualTypeStr?.let { dt -> ScratchBlockType.entries.find { it.name == dt } }
            val outputType = Regex("outputType\\s*:\\s*['\"]([^'\"]+)['\"]")
                .find(objBody)?.groupValues?.getOrNull(1) ?: ""
            val blockShape = Regex("blockShape\\s*:\\s*['\"]([^'\"]+)['\"]")
                .find(objBody)?.groupValues?.getOrNull(1) ?: ""
            val duplicateOnDrag = Regex("\\bduplicateOnDrag\\s*:\\s*(true|false)")
                .find(objBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull() ?: false

            val args = parseArguments(objBody)
            val switches = parseSwitches(objBody)
            val methodBody = extractMethodBody(code, opcode)

            result.add(
                ExtensionBlock(
                    opcode = opcode,
                    blockType = blockType,
                    dualType = dualType,
                    text = text,
                    branchCount = branchCount,
                    branchTexts = branchTexts,
                    outputType = outputType,
                    blockShape = blockShape,
                    duplicateOnDrag = duplicateOnDrag,
                    arguments = args,
                    switches = switches,
                    jsBody = methodBody
                )
            )
        }
        return result
    }

    private fun parseSwitches(blockObj: String): List<BlockSwitch> {
        val switchesIdx = blockObj.indexOf("switches:")
        if (switchesIdx == -1) return emptyList()
        val bracketStart = blockObj.indexOf('[', switchesIdx)
        if (bracketStart == -1) return emptyList()
        val arrayBody = extractBalancedBrackets(blockObj, bracketStart) ?: return emptyList()

        val result = mutableListOf<BlockSwitch>()
        var i = 0
        while (i < arrayBody.length) {
            val ch = arrayBody[i]
            when {
                ch == '\'' || ch == '"' -> {
                    val endQuote = arrayBody.indexOf(ch, i + 1)
                    if (endQuote == -1) break
                    val target = arrayBody.substring(i + 1, endQuote)
                    if (target.isNotBlank()) {
                        result.add(BlockSwitch(targetId = target))
                    }
                    i = endQuote + 1
                }
                ch == '{' -> {
                    val obj = extractBalancedBlock(arrayBody, i) ?: break
                    val targetId = Regex("\\bid\\s*:\\s*['\"]([^'\"]+)['\"]")
                        .find(obj)?.groupValues?.getOrNull(1) ?: ""
                    val rawId = Regex("\\brawId\\s*:\\s*(true|false)")
                        .find(obj)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull() ?: false

                    val inputs = mutableListOf<BlockSwitchInputMapping>()
                    val inputsIdx = obj.indexOf("inputs:")
                    if (inputsIdx != -1) {
                        val inStart = obj.indexOf('[', inputsIdx)
                        val inBody = if (inStart != -1) extractBalancedBrackets(obj, inStart) else null
                        if (inBody != null) {
                            val pairRegex = Regex("\\[\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]+)['\"]\\s*]")
                            pairRegex.findAll(inBody).forEach { m ->
                                inputs.add(BlockSwitchInputMapping(m.groupValues[1], m.groupValues[2]))
                            }
                        }
                    }

                    val splitInputs = mutableListOf<String>()
                    val splitIdx = obj.indexOf("splitInputs:")
                    if (splitIdx != -1) {
                        val spStart = obj.indexOf('[', splitIdx)
                        val spBody = if (spStart != -1) extractBalancedBrackets(obj, spStart) else null
                        if (spBody != null) {
                            Regex("['\"]([^'\"]+)['\"]").findAll(spBody).forEach { m ->
                                splitInputs.add(m.groupValues[1])
                            }
                        }
                    }

                    if (targetId.isNotBlank()) {
                        result.add(
                            BlockSwitch(
                                targetId = targetId,
                                inputs = inputs,
                                splitInputs = splitInputs,
                                rawId = rawId
                            )
                        )
                    }
                    i += obj.length + 2
                }
                else -> i++
            }
        }
        return result
    }

    private fun parseArguments(blockObj: String): List<BlockArgument> {
        // Find top-level `arguments:` key (outside quotes/nested braces)
        var depth = 0
        var quote: Char? = null
        var argsBraceStart = -1
        var scan = 0
        while (scan < blockObj.length) {
            val ch = blockObj[scan]
            if (quote != null) {
                if (ch == quote && blockObj.getOrNull(scan - 1) != '\\') quote = null
                scan++
                continue
            }
            when (ch) {
                '\'', '"' -> quote = ch
                '{' -> depth++
                '}' -> if (depth > 0) depth--
                else -> if (depth == 0 && blockObj.startsWith("arguments", scan)) {
                    val after = blockObj.substring(scan)
                    val m = Regex("^arguments\\s*:\\s*\\{").find(after)
                    if (m != null && (scan == 0 || !blockObj[scan - 1].isLetterOrDigit())) {
                        argsBraceStart = scan + m.range.last
                        break
                    }
                }
            }
            scan++
        }
        if (argsBraceStart == -1) return emptyList()
        val argsBody = extractBalancedBlock(blockObj, argsBraceStart) ?: return emptyList()

        val args = mutableListOf<BlockArgument>()
        var cursor = 0
        while (cursor < argsBody.length) {
            while (cursor < argsBody.length && (argsBody[cursor].isWhitespace() || argsBody[cursor] == ',')) {
                cursor++
            }
            if (cursor >= argsBody.length) break
            val headerMatch = Regex("^([A-Za-z0-9_]+)\\s*:\\s*\\{").find(argsBody.substring(cursor))
            if (headerMatch == null) {
                cursor++
                continue
            }
            val argName = headerMatch.groupValues[1]
            val openBraceIdx = cursor + headerMatch.range.last
            val argBody = extractBalancedBlock(argsBody, openBraceIdx) ?: break
            cursor = openBraceIdx + argBody.length + 2

            val customTypeName = extractTopLevelStringProp(argBody, "type")
                ?: ""
            val typeStr = Regex("\\btype\\s*:\\s*Scratch\\.ArgumentType\\.([A-Z_]+)")
                .find(argBody)?.groupValues?.getOrNull(1) ?: "STRING"
            val argType = ScratchArgumentType.entries.find { it.name == typeStr }
                ?: ScratchArgumentType.STRING

            val rawDefault = extractTopLevelStringProp(argBody, "defaultValue")
                ?: Regex("\\bdefaultValue\\s*:\\s*([^,\\n}]+)")
                    .find(argBody)?.groupValues?.getOrNull(1)?.trim()
                ?: ""

            val sliderMin = Regex("\\bmin\\s*:\\s*([-0-9.]+)")
                .find(argBody)?.groupValues?.getOrNull(1) ?: "0"
            val sliderMax = Regex("\\bmax\\s*:\\s*([-0-9.]+)")
                .find(argBody)?.groupValues?.getOrNull(1) ?: "100"
            val sliderPrecision = Regex("\\bprecision\\s*:\\s*([-0-9.]+)")
                .find(argBody)?.groupValues?.getOrNull(1) ?: "1"
            val acceptReporters = Regex("\\bacceptReporters\\s*:\\s*(true|false)")
                .find(argBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull() ?: true
            val canMultiline = Regex("\\bcanMultiline\\s*:\\s*(true|false)")
                .find(argBody)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull() ?: false
            val menuName = extractTopLevelStringProp(argBody, "menu") ?: ""
            val shadow = extractTopLevelStringProp(argBody, "shadow") ?: ""

            val isExtendable = argType == ScratchArgumentType.EXTENDABLE && customTypeName.isBlank()
            val extendableText = if (isExtendable) {
                extractTopLevelStringProp(argBody, "text") ?: "[ITEM]"
            } else {
                "[ITEM]"
            }
            val extendableArguments = if (isExtendable) {
                parseArguments(argBody)
            } else {
                emptyList()
            }
            val extendableDefaultInputs = if (isExtendable) {
                Regex("\\bdefaultInputs\\s*:\\s*([0-9]+)").find(argBody)?.groupValues?.getOrNull(1) ?: ""
            } else ""
            val extendableMinInputs = if (isExtendable) {
                Regex("\\bminInputs\\s*:\\s*([0-9]+)").find(argBody)?.groupValues?.getOrNull(1) ?: ""
            } else ""
            val extendableMaxInputs = if (isExtendable) {
                Regex("\\bmaxInputs\\s*:\\s*([0-9]+)").find(argBody)?.groupValues?.getOrNull(1) ?: ""
            } else ""
            val extendableAcceptArray = if (isExtendable) {
                Regex("\\bacceptArray\\s*:\\s*(true|false)").find(argBody)?.groupValues?.getOrNull(1)
                    ?.toBooleanStrictOrNull() ?: false
            } else false
            val extendableSeparator = if (isExtendable) {
                extractTopLevelStringProp(argBody, "separator") ?: ""
            } else ""

            args.add(
                BlockArgument(
                    name = argName,
                    type = argType,
                    customTypeName = customTypeName,
                    defaultValue = rawDefault,
                    sliderMin = sliderMin,
                    sliderMax = sliderMax,
                    sliderPrecision = sliderPrecision,
                    acceptReporters = acceptReporters,
                    canMultiline = canMultiline,
                    menuName = menuName,
                    shadow = shadow,
                    extendableText = extendableText,
                    extendableArguments = extendableArguments,
                    extendableDefaultInputs = extendableDefaultInputs,
                    extendableMinInputs = extendableMinInputs,
                    extendableMaxInputs = extendableMaxInputs,
                    extendableAcceptArray = extendableAcceptArray,
                    extendableSeparator = extendableSeparator
                )
            )
        }
        return args
    }

    private fun extractTopLevelTextAndBranches(objBody: String, fallback: String): Pair<String, List<String>> {
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < objBody.length) {
            val ch = objBody[i]
            if (quote != null) {
                if (ch == quote && objBody.getOrNull(i - 1) != '\\') quote = null
                i++
                continue
            }
            when (ch) {
                '\'', '"' -> quote = ch
                '{', '[' -> depth++
                '}', ']' -> if (depth > 0) depth--
                else -> if (depth == 0 && objBody.startsWith("text", i) &&
                    (i == 0 || !objBody[i - 1].isLetterOrDigit())
                ) {
                    val arrMatch = Regex("^text\\s*:\\s*\\[").find(objBody.substring(i))
                    if (arrMatch != null) {
                        val bracketStart = i + arrMatch.range.last
                        val arrInner = extractBalancedBrackets(objBody, bracketStart)
                        if (arrInner != null) {
                            val items = mutableListOf<String>()
                            val strRegex = Regex("'((?:[^'\\\\]|\\\\.)*)'|\"((?:[^\"\\\\]|\\\\.)*)\"")
                            strRegex.findAll(arrInner).forEach { m ->
                                val raw = m.groupValues[1].ifEmpty { m.groupValues[2] }
                                items.add(
                                    raw.replace("\\n", "\n")
                                        .replace("\\'", "'")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                )
                            }
                            if (items.isNotEmpty()) {
                                return items.first() to items.drop(1)
                            }
                        }
                    }
                }
            }
            i++
        }
        val single = extractTopLevelStringProp(objBody, "text") ?: fallback
        return single to emptyList()
    }

    private fun extractTopLevelStringProp(objBody: String, propName: String): String? {
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < objBody.length) {
            val ch = objBody[i]
            if (quote != null) {
                if (ch == quote && objBody.getOrNull(i - 1) != '\\') quote = null
                i++
                continue
            }
            when (ch) {
                '\'', '"' -> quote = ch
                '{', '[' -> depth++
                '}', ']' -> if (depth > 0) depth--
                else -> if (depth == 0 && objBody.startsWith(propName, i) &&
                    (i == 0 || !objBody[i - 1].isLetterOrDigit())
                ) {
                    val tail = objBody.substring(i)
                    val singleMatch = Regex("^${Regex.escape(propName)}\\s*:\\s*'((?:[^'\\\\]|\\\\.)*)'").find(tail)
                    if (singleMatch != null) {
                        return singleMatch.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\'", "'")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                    }
                    val doubleMatch = Regex("^${Regex.escape(propName)}\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(tail)
                    if (doubleMatch != null) {
                        return doubleMatch.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\'", "'")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                    }
                }
            }
            i++
        }
        return null
    }

    private fun extractMethodBody(code: String, opcode: String): String {
        val methodRegex = Regex("(?m)^\\s*(?:async\\s+)?${Regex.escape(opcode)}\\s*\\(([^)]*)\\)\\s*\\{")
        val match = methodRegex.find(code) ?: return ""
        val rawParam = match.groupValues.getOrNull(1)?.trim().orEmpty()
        val braceIdx = code.indexOf('{', match.range.first)
        if (braceIdx == -1) return ""
        val body = (extractBalancedBlock(code, braceIdx) ?: return "").trimIndent().trim()
        return if (rawParam.startsWith("{") && rawParam.endsWith("}") && !body.contains("= args")) {
            "const $rawParam = args;\n$body"
        } else {
            body
        }
    }

    private fun extractBalancedBlock(source: String, openBraceIndex: Int): String? {
        if (openBraceIndex !in source.indices || source[openBraceIndex] != '{') return null
        var depth = 0
        for (i in openBraceIndex until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(openBraceIndex + 1, i)
                    }
                }
            }
        }
        return null
    }

    private fun extractBalancedBrackets(source: String, openBracketIndex: Int): String? {
        if (openBracketIndex !in source.indices || source[openBracketIndex] != '[') return null
        var depth = 0
        for (i in openBracketIndex until source.length) {
            when (source[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(openBracketIndex + 1, i)
                    }
                }
            }
        }
        return null
    }
}
