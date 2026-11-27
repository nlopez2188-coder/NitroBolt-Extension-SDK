package com.example.data

import com.example.model.BlockArgument
import com.example.model.ExecutionLogEntry
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.ExtensionProject
import com.example.model.MutatedBlockSpec
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType
import com.example.model.VmSimulationState

object BlockSimulator {

    fun buildDefaultExtendableJson(
        arg: BlockArgument,
        menus: List<ExtensionMenu> = emptyList(),
        depth: Int = 0
    ): String {
        if (depth > 3) return "[]"
        val minCount = (arg.extendableMinInputs.toIntOrNull() ?: 1).coerceAtLeast(0)
        val maxCount = arg.extendableMaxInputs.toIntOrNull()?.coerceAtLeast(minCount) ?: 12
        val count = (arg.extendableDefaultInputs.toIntOrNull() ?: 2).coerceIn(minCount, maxCount)
        val instances = (0 until count).map {
            buildDefaultExtendableInstance(arg, menus, depth)
        }
        return serializeExtendableInstances(instances, arg)
    }

    fun buildDefaultExtendableInstance(
        arg: BlockArgument,
        menus: List<ExtensionMenu> = emptyList(),
        depth: Int = 0
    ): Map<String, String> {
        val map = linkedMapOf<String, String>()
        arg.extendableArguments.forEach { child ->
            val value = when {
                child.type == ScratchArgumentType.EXTENDABLE && child.customTypeName.isBlank() ->
                    buildDefaultExtendableJson(child, menus, depth + 1)
                child.defaultValue.isNotBlank() -> child.defaultValue
                child.menuName.isNotBlank() ->
                    menus.find { it.name == child.menuName }?.items?.firstOrNull() ?: ""
                else -> child.type.sampleDefault
            }
            map[child.name] = value
        }
        return map
    }

    fun parseExtendableInstances(raw: String): List<Map<String, String>>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        val elements = splitTopLevelComma(inner)
        val parsed = elements.map { elem -> parseJsObject(elem.trim()) ?: return null }
        return parsed
    }

    fun serializeExtendableInstances(
        instances: List<Map<String, String>>,
        argDef: BlockArgument? = null
    ): String {
        val childTypes = argDef?.extendableArguments?.associateBy { it.name }.orEmpty()
        val objStrings = instances.map { instance ->
            val entries = instance.entries.joinToString(", ") { (k, v) ->
                val childType = childTypes[k]?.type
                val formattedVal = when {
                    childType == ScratchArgumentType.EXTENDABLE ->
                        v.trim().takeIf { it.startsWith("[") && it.endsWith("]") } ?: "[]"
                    childType in listOf(
                        ScratchArgumentType.NUMBER,
                        ScratchArgumentType.SLIDER,
                        ScratchArgumentType.ANGLE,
                        ScratchArgumentType.NOTE
                    ) && v.trim().toDoubleOrNull() != null -> v.trim()
                    childType == ScratchArgumentType.BOOLEAN &&
                        (v.equals("true", true) || v.equals("false", true)) -> v.lowercase()
                    else -> formatJsonScalar(v)
                }
                "\"${escapeJson(k)}\": $formattedVal"
            }
            "{$entries}"
        }
        return "[${objStrings.joinToString(", ")}]"
    }

    fun resolveShadowValue(
        project: ExtensionProject,
        shadowOpcode: String,
        currentVm: VmSimulationState,
        depth: Int = 0
    ): String? {
        if (shadowOpcode.isBlank() || depth > 2) return null
        val shadowBlock = project.blocks.find { it.opcode == shadowOpcode } ?: return null
        val resolvedArgs = shadowBlock.arguments.associate { arg ->
            val fallback = when {
                arg.type == ScratchArgumentType.EXTENDABLE && arg.customTypeName.isBlank() ->
                    buildDefaultExtendableJson(arg, project.menus)
                arg.defaultValue.isNotBlank() -> arg.defaultValue
                arg.menuName.isNotBlank() ->
                    project.menus.find { it.name == arg.menuName }?.firstSelectableValue() ?: ""
                arg.shadow.isNotBlank() && arg.acceptReporters ->
                    resolveShadowValue(project, arg.shadow, currentVm, depth + 1) ?: arg.type.sampleDefault
                else -> arg.type.sampleDefault
            }
            arg.name to fallback
        }
        val (_, log) = execute(project, shadowBlock, resolvedArgs, currentVm)
        if (log.isError || !log.output.startsWith("Returned:")) return null
        val raw = log.output.removePrefix("Returned:").trim()
        return if ((raw.startsWith("'") && raw.endsWith("'") && raw.length >= 2) ||
            (raw.startsWith("\"") && raw.endsWith("\"") && raw.length >= 2)
        ) {
            raw.substring(1, raw.length - 1)
        } else {
            raw
        }
    }

    fun resolveDefaultArgValues(
        project: ExtensionProject,
        block: ExtensionBlock,
        currentVm: VmSimulationState,
        existingInputs: Map<String, String> = emptyMap()
    ): Map<String, String> {
        val resolved = linkedMapOf<String, String>()
        val menusByName = project.menus.associateBy { it.name }
        block.arguments.forEach { arg ->
            val explicit = existingInputs[arg.name]
            if (explicit != null) {
                resolved[arg.name] = explicit
            } else {
                val menu = arg.menuName.takeIf { it.isNotBlank() }?.let { menusByName[it] }
                val fallback = when {
                    arg.type == ScratchArgumentType.EXTENDABLE && arg.customTypeName.isBlank() ->
                        buildDefaultExtendableJson(arg, project.menus)
                    arg.defaultValue.isNotBlank() && arg.defaultValue != ExtensionMenu.SEPARATOR_ITEM ->
                        arg.defaultValue
                    menu != null -> {
                        val parentVal = if (menu.isDependent) resolved[menu.parentName] else null
                        menu.firstSelectableValue(parentVal) ?: ""
                    }
                    arg.shadow.isNotBlank() && arg.acceptReporters ->
                        resolveShadowValue(project, arg.shadow, currentVm) ?: arg.type.sampleDefault
                    else -> arg.type.sampleDefault
                }
                resolved[arg.name] = fallback
            }
        }
        return resolved
    }

    fun cascadeDependentMenus(
        block: ExtensionBlock,
        menus: List<ExtensionMenu>,
        inputs: Map<String, String>
    ): Map<String, String> {
        val updated = inputs.toMutableMap()
        val menusByName = menus.associateBy { it.name }
        val argsByName = block.arguments.associateBy { it.name }

        // Iterate up to arguments.size passes so chained dependencies (ANIMAL -> BREED -> COAT) settle cleanly
        repeat(block.arguments.size.coerceAtLeast(1)) {
            var changed = false
            block.arguments.forEach { arg ->
                val menu = arg.menuName.takeIf { it.isNotBlank() }?.let { menusByName[it] }
                if (menu != null && menu.isDependent) {
                    val parentMenu = argsByName[menu.parentName]?.menuName?.let { menusByName[it] }
                    val parentVal = updated[menu.parentName]
                        ?: argsByName[menu.parentName]?.defaultValue?.takeIf {
                            it.isNotBlank() && it != ExtensionMenu.SEPARATOR_ITEM
                        }
                        ?: parentMenu?.firstSelectableValue()
                        ?: ""
                    val validOptions = menu.resolveOptions(parentVal).filter { !it.isSeparator }
                    val validValues = validOptions.map { it.value }
                    val currentChild = updated[arg.name]
                        ?: arg.defaultValue.takeIf { it.isNotBlank() && it != ExtensionMenu.SEPARATOR_ITEM }
                    if (validValues.isNotEmpty() && (currentChild == null || currentChild !in validValues)) {
                        updated[arg.name] = validValues.first()
                        changed = true
                    }
                }
            }
            if (!changed) return updated
        }
        return updated
    }

    fun resolveMutatedBlock(
        block: ExtensionBlock,
        menus: List<ExtensionMenu>,
        currentValues: Map<String, String> = emptyMap()
    ): MutatedBlockSpec {
        val menusByName = menus.associateBy { it.name }
        var effectiveBlockType = block.blockType
        val effectiveArgs = block.arguments.toMutableList()
        var previousStatement: Boolean? = null
        var nextStatement: Boolean? = null
        var inputsInline: Boolean? = null
        val transformedNames = linkedSetOf<String>()

        block.arguments.forEach { arg ->
            val menu = arg.menuName.takeIf { it.isNotBlank() }?.let { menusByName[it] }
            if (menu != null && menu.isMutator) {
                val activeValue = currentValues[arg.name]?.takeIf { it.isNotBlank() }
                    ?: arg.defaultValue.takeIf { it.isNotBlank() && it != ExtensionMenu.SEPARATOR_ITEM }
                    ?: menu.firstSelectableValue()
                val mutState = activeValue?.let { menu.mutator[it] }
                if (mutState != null) {
                    if (mutState.output != null) {
                        effectiveBlockType = mutState.output
                    }
                    if (mutState.previousStatement != null) {
                        previousStatement = mutState.previousStatement
                    }
                    if (mutState.nextStatement != null) {
                        nextStatement = mutState.nextStatement
                    }
                    if (mutState.inputsInline != null) {
                        inputsInline = mutState.inputsInline
                    }
                    mutState.arguments.forEach { mutArg ->
                        transformedNames.add(mutArg.name)
                        val idx = effectiveArgs.indexOfFirst { it.name == mutArg.name }
                        if (idx >= 0) {
                            effectiveArgs[idx] = mutArg.copy(id = effectiveArgs[idx].id)
                        } else {
                            effectiveArgs.add(mutArg)
                        }
                    }
                }
            }
        }

        return MutatedBlockSpec(
            block = block.copy(
                blockType = effectiveBlockType,
                arguments = effectiveArgs
            ),
            previousStatement = previousStatement,
            nextStatement = nextStatement,
            inputsInline = inputsInline,
            transformedArgNames = transformedNames
        )
    }

    fun applyInputChange(
        project: ExtensionProject,
        block: ExtensionBlock,
        changedArgName: String,
        newValue: String,
        existingInputs: Map<String, String>
    ): Map<String, String> {
        val updated = existingInputs.toMutableMap()
        val prevSpec = resolveMutatedBlock(block, project.menus, existingInputs)
        updated[changedArgName] = newValue

        val changedArgDef = block.arguments.find { it.name == changedArgName }
        val changedMenu = changedArgDef?.menuName?.takeIf { it.isNotBlank() }?.let { mName ->
            project.menus.find { it.name == mName }
        }
        if (changedMenu != null && changedMenu.isMutator) {
            val nextSpec = resolveMutatedBlock(block, project.menus, updated)
            // Disconnect/reset arguments transformed by the mutator to the new state's defaults
            nextSpec.transformedArgNames.union(prevSpec.transformedArgNames).forEach { tName ->
                if (tName != changedArgName) {
                    val newArg = nextSpec.block.arguments.find { it.name == tName }
                    updated[tName] = when {
                        newArg == null -> ""
                        newArg.type == ScratchArgumentType.ARRAY -> ""
                        newArg.type == ScratchArgumentType.OBJECT -> ""
                        newArg.type == ScratchArgumentType.EXTENDABLE ->
                            buildDefaultExtendableJson(newArg, project.menus)
                        newArg.defaultValue.isNotBlank() -> newArg.defaultValue
                        else -> ""
                    }
                }
            }
        }

        return cascadeDependentMenus(block, project.menus, updated)
    }

    fun execute(
        project: ExtensionProject,
        block: ExtensionBlock,
        argValues: Map<String, String>,
        currentVm: VmSimulationState
    ): Pair<VmSimulationState, ExecutionLogEntry> {
        val timing = if (project.executionMode == ExecutionMode.UNSANDBOXED) {
            "0.04ms (Instant)"
        } else {
            "16.7ms+ (1-Frame Sandboxed Delay)"
        }

        val body = block.jsBody.trim()

        // Check sandboxed VM violation
        if (project.executionMode == ExecutionMode.SANDBOXED &&
            (body.contains("vm.") || body.contains("Scratch.vm"))
        ) {
            return currentVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "ReferenceError: Scratch.vm is inaccessible inside a sandboxed <iframe>",
                executionTimeLabel = timing,
                executionMode = project.executionMode,
                isError = true
            )
        }

        val effectiveBlock = resolveMutatedBlock(block, project.menus, argValues).block

        // Check return requirement for Reporter / Boolean / Object / Array
        val hasReturn = Regex("\\breturn\\b\\s*[^;\\s]").containsMatchIn(body)
        if (effectiveBlock.blockType.requiresReturn && !hasReturn) {
            return currentVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(effectiveBlock, argValues),
                output = "TypeError: ${effectiveBlock.blockType.label} block returned undefined (unsandboxed requires valid string/number/boolean)",
                executionTimeLabel = timing,
                executionMode = project.executionMode,
                isError = true
            )
        }

        var nextVm = currentVm

        // Handle VM Turbo Mode mutations
        if (body.contains("setTurboMode")) {
            val enabled = argValues.values.any { it.equals("on", true) || it.equals("true", true) }
            nextVm = nextVm.copy(
                turboMode = enabled,
                fps = if (enabled) 240 else 30
            )
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "VM updated: turboMode = $enabled (${nextVm.fps} steps/sec)",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Handle Sprite setXY mutations
        if (body.contains("setXY")) {
            val x = argValues["X"]?.toDoubleOrNull() ?: 0.0
            val y = argValues["Y"]?.toDoubleOrNull() ?: 0.0
            nextVm = nextVm.copy(spriteX = x, spriteY = y)
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "VM updated: ${nextVm.activeSpriteName} moved to ($x, $y)",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Handle boolean turboMode query
        if (body.contains("vm.runtime.turboMode")) {
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "Returned: ${nextVm.turboMode} (boolean)",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Handle variable lookup
        if (body.contains("lookupVariableByNameAndType")) {
            val varName = argValues["NAME"] ?: argValues.values.firstOrNull() ?: "score"
            val found = nextVm.projectVariables[varName] ?: "(not found)"
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "Returned: '$found' from stage variable '$varName'",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Handle sprite count
        if (body.contains("targets.filter")) {
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "Returned: 3 (sprites)",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Custom Type return simulation (e.g., new Counter(args.VALUE))
        if (block.outputType.isNotBlank()) {
            val customType = project.customTypes.find { it.typeName == block.outputType }
            val numericArg = argValues.values.mapNotNull { it.toDoubleOrNull() }.sum()
            val formattedVal = if (numericArg % 1.0 == 0.0) numericArg.toLong().toString() else numericArg.toString()
            val typeLabel = customType?.className ?: block.outputType
            val reportNote = if (customType?.visualReportBody?.isNotBlank() == true) {
                " • visualReport ready"
            } else if (customType?.monitorContentBody?.isNotBlank() == true) {
                " • monitorContent ready"
            } else {
                ""
            }
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "Returned: $typeLabel(${block.outputType}:$formattedVal)$reportNote",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Handle util.startBranch(...) on CONDITIONAL / LOOP blocks
        if (body.contains("util.startBranch")) {
            val ifElseMatch = Regex(
                "if\\s*\\(\\s*args\\.([A-Za-z0-9_]+)\\s*\\)\\s*\\{([\\s\\S]*?)}\\s*else\\s*\\{([\\s\\S]*?)}"
            ).find(body)
            val effectiveBranchBody = if (ifElseMatch != null) {
                val condArg = ifElseMatch.groupValues[1]
                val rawCond = argValues[condArg].orEmpty().trim()
                val isTrue = rawCond.isNotBlank() &&
                    !rawCond.equals("false", ignoreCase = true) &&
                    rawCond != "0" &&
                    !rawCond.equals("null", ignoreCase = true)
                if (isTrue) ifElseMatch.groupValues[2] else ifElseMatch.groupValues[3]
            } else {
                body
            }
            val branchMatches = Regex("util\\.startBranch\\(\\s*([^,]+?)\\s*,\\s*(true|false)\\s*\\)")
                .findAll(effectiveBranchBody)
                .toList()
            if (branchMatches.isNotEmpty()) {
                val started = branchMatches.map { m ->
                    val branchIdx = m.groupValues[1].trim()
                    val isLoop = m.groupValues[2].toBooleanStrictOrNull() ?: false
                    val modeText = if (isLoop) "looping" else "single pass"
                    "branch $branchIdx ($modeText)"
                }
                return nextVm to ExecutionLogEntry(
                    opcode = block.opcode,
                    blockLabel = formatLabel(block, argValues),
                    output = "Started ${started.joinToString(" → ")} • ${block.blockType.label}",
                    executionTimeLabel = timing,
                    executionMode = project.executionMode
                )
            }
        }

        // Evaluate statement-by-statement with top-level splitting (preserving blocks like forEach { ... })
        val statements = splitTopLevelStatements(body)
        val localVars = linkedMapOf<String, String>()
        val consoleLogs = mutableListOf<String>()

        for (stmt in statements) {
            val ifEqMatch = Regex(
                "^if\\s*\\(\\s*args\\.([A-Za-z0-9_]+)\\s*===\\s*(['\"])(.*?)\\2\\s*\\)\\s*\\{([\\s\\S]*)\\}$"
            ).find(stmt)
            if (ifEqMatch != null) {
                val condArg = ifEqMatch.groupValues[1]
                val expectedVal = ifEqMatch.groupValues[3]
                val innerBody = ifEqMatch.groupValues[4].trim()
                if (argValues[condArg] == expectedVal && innerBody.isNotBlank()) {
                    val (innerVm, innerLog) = execute(
                        project = project,
                        block = effectiveBlock.copy(jsBody = innerBody),
                        argValues = argValues,
                        currentVm = nextVm
                    )
                    nextVm = innerVm
                    if (innerLog.output.startsWith("Returned:") || innerLog.isError) {
                        return nextVm to innerLog
                    }
                }
                continue
            }

            val guardMatch = Regex("^if\\s*\\(\\s*!args\\.([A-Za-z0-9_]+)\\s*\\)\\s*return\\s+(.+)$").find(stmt)
            if (guardMatch != null) {
                val argName = guardMatch.groupValues[1]
                val guardReturnExpr = guardMatch.groupValues[2].trim()
                val rawArg = argValues[argName].orEmpty().trim()
                val argDef = block.arguments.find { it.name == argName }
                val isMissing = when (argDef?.type) {
                    ScratchArgumentType.OBJECT -> parseJsObject(rawArg) == null
                    ScratchArgumentType.ARRAY -> parseJsArray(rawArg) == null
                    else -> rawArg.isBlank() || rawArg == "false" || rawArg == "0" || rawArg == "null" || rawArg == "undefined"
                }
                if (isMissing) {
                    val evaluatedGuard = evaluateReturnExpression(guardReturnExpr, block, argValues, localVars)
                    return nextVm to ExecutionLogEntry(
                        opcode = block.opcode,
                        blockLabel = formatLabel(block, argValues),
                        output = "Returned: $evaluatedGuard",
                        executionTimeLabel = timing,
                        executionMode = project.executionMode
                    )
                }
                continue
            }

            // Local variable assignment: const/let/var name = expr
            val declMatch = Regex("^(?:const|let|var)\\s+([A-Za-z0-9_]+)\\s*=\\s*([\\s\\S]+)$").find(stmt)
            if (declMatch != null) {
                val varName = declMatch.groupValues[1]
                val rhs = declMatch.groupValues[2].trim()
                localVars[varName] = evaluateRhsExpression(rhs, block, argValues, localVars)
                continue
            }

            // arrayVar.forEach((itemVar, idxVar?) => { ... })
            val forEachMatch = Regex(
                "^([A-Za-z0-9_]+)\\.forEach\\(\\s*\\(?\\s*([A-Za-z0-9_]+)(?:\\s*,\\s*([A-Za-z0-9_]+))?\\s*\\)?\\s*=>\\s*\\{([\\s\\S]*)}\\s*\\)$"
            ).find(stmt)
            if (forEachMatch != null) {
                val arrayVar = forEachMatch.groupValues[1]
                val itemVar = forEachMatch.groupValues[2]
                val idxVar = forEachMatch.groupValues[3]
                val loopBody = forEachMatch.groupValues[4].trim()
                val items = parseRawArrayElements(localVars[arrayVar].orEmpty())
                items.forEachIndexed { idx, itemJson ->
                    val loopVars = localVars.toMutableMap()
                    loopVars[itemVar] = itemJson
                    if (idxVar.isNotBlank()) loopVars[idxVar] = idx.toString()
                    splitTopLevelStatements(loopBody).forEach { innerStmt ->
                        Regex("^console\\.log\\(([\\s\\S]+)\\)$").find(innerStmt)?.let { cm ->
                            consoleLogs.add(evaluateConsoleArgs(cm.groupValues[1], block, argValues, loopVars))
                        }
                    }
                }
                continue
            }

            // console.log(...)
            val consoleStmtMatch = Regex("^console\\.log\\(([\\s\\S]+)\\)$").find(stmt)
            if (consoleStmtMatch != null) {
                consoleLogs.add(evaluateConsoleArgs(consoleStmtMatch.groupValues[1], block, argValues, localVars))
                continue
            }

            val returnMatch = Regex("^return\\s+([\\s\\S]+)$").find(stmt)
            if (returnMatch != null) {
                val evaluated = evaluateReturnExpression(returnMatch.groupValues[1].trim(), block, argValues, localVars)
                return nextVm to ExecutionLogEntry(
                    opcode = block.opcode,
                    blockLabel = formatLabel(block, argValues),
                    output = "Returned: $evaluated",
                    executionTimeLabel = timing,
                    executionMode = project.executionMode
                )
            }
        }

        if (consoleLogs.isNotEmpty()) {
            val combined = consoleLogs.joinToString("\n")
            val lineCount = combined.count { it == '\n' } + 1
            val lineInfo = if (lineCount > 1) " ($lineCount lines)" else ""
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "console.log$lineInfo:\n$combined",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Fallback regex for multi-line return expressions
        val returnMatch = Regex("return\\s+([^;]+)").find(body)
        if (returnMatch != null) {
            val evaluated = evaluateReturnExpression(returnMatch.groupValues[1].trim(), block, argValues, localVars)
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "Returned: $evaluated",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        // Handle console.log(args.FOO) or console.log(FOO)
        val consoleMatch = Regex("console\\.log\\(([^)]+)\\)").find(body)
        if (consoleMatch != null) {
            var expr = consoleMatch.groupValues[1].trim()
            argValues.forEach { (k, v) ->
                expr = expr.replace("args.$k", v)
                    .replace(Regex("\\b${Regex.escape(k)}\\b"), v)
            }
            val lineCount = expr.count { it == '\n' } + 1
            val lineInfo = if (lineCount > 1) " ($lineCount lines)" else ""
            return nextVm to ExecutionLogEntry(
                opcode = block.opcode,
                blockLabel = formatLabel(block, argValues),
                output = "console.log$lineInfo:\n$expr",
                executionTimeLabel = timing,
                executionMode = project.executionMode
            )
        }

        val shapeNote = buildList {
            if (block.blockShape.isNotBlank()) add("shape: ${block.blockShape}")
            if (block.dualType != null) add("dualType: ${block.dualType.label}")
        }.joinToString(" • ")

        val fallbackMsg = if (block.blockType == ScratchBlockType.COMMAND) {
            "Command '${block.opcode}' completed cleanly"
        } else {
            "Executed '${block.opcode}'"
        }
        val finalMsg = if (shapeNote.isNotBlank()) "$fallbackMsg ($shapeNote)" else fallbackMsg
        return nextVm to ExecutionLogEntry(
            opcode = block.opcode,
            blockLabel = formatLabel(block, argValues),
            output = finalMsg,
            executionTimeLabel = timing,
            executionMode = project.executionMode
        )
    }

    private fun splitTopLevelStatements(body: String): List<String> {
        val statements = mutableListOf<String>()
        var depth = 0
        var quote: Char? = null
        var start = 0
        var i = 0
        while (i < body.length) {
            val ch = body[i]
            if (quote != null) {
                if (ch == quote && body.getOrNull(i - 1) != '\\') quote = null
                i++
                continue
            }
            when (ch) {
                '\'', '"', '`' -> quote = ch
                '{', '[', '(' -> depth++
                '}', ']', ')' -> if (depth > 0) depth--
                ';' -> if (depth == 0) {
                    val stmt = body.substring(start, i).trim()
                    if (stmt.isNotBlank()) statements.add(stmt)
                    start = i + 1
                }
                '\n' -> if (depth == 0) {
                    val candidate = body.substring(start, i).trim()
                    if (candidate.isNotBlank() && !candidate.endsWith("{") && !candidate.endsWith(",")) {
                        statements.add(candidate.removeSuffix(";").trim())
                        start = i + 1
                    }
                }
            }
            i++
        }
        if (start < body.length) {
            val tail = body.substring(start).trim().removeSuffix(";").trim()
            if (tail.isNotBlank()) statements.add(tail)
        }
        return statements
    }

    private fun evaluateRhsExpression(
        rhs: String,
        block: ExtensionBlock,
        argValues: Map<String, String>,
        localVars: Map<String, String>
    ): String {
        val trimmed = rhs.trim()
        val isArrayWrapMatch = Regex(
            "^Array\\.isArray\\(\\s*args\\.([A-Za-z0-9_]+)\\s*\\)\\s*\\?\\s*args\\.\\1\\s*:\\s*\\[\\s*args\\.\\1\\s*]$"
        ).find(trimmed)
        if (isArrayWrapMatch != null) {
            val raw = argValues[isArrayWrapMatch.groupValues[1]].orEmpty()
            return if (parseJsArray(raw) != null) raw else "[${formatJsonScalar(raw)}]"
        }
        val extMatch = Regex("^util\\.extendableToArray\\(\\s*args\\s*,([\\s\\S]+)\\)$").find(trimmed)
        if (extMatch != null) {
            val pathParts = splitTopLevelComma(extMatch.groupValues[1]).map { unquoteJs(it.trim()) }
            if (pathParts.isNotEmpty()) {
                return evalExtendableToArray(pathParts, block, argValues)
            }
        }
        return evaluateReturnExpression(trimmed, block, argValues, localVars)
    }

    private fun evalExtendableToArray(
        pathParts: List<String>,
        block: ExtensionBlock,
        argValues: Map<String, String>
    ): String {
        val rootName = pathParts.firstOrNull() ?: return "[]"
        val rootArg = block.arguments.find { it.name == rootName }
        val rawRoot = argValues[rootName]?.takeIf { it.isNotBlank() }
            ?: rootArg?.let { buildDefaultExtendableJson(it) }
            ?: "[]"

        // acceptArray passthrough if rawRoot is a flat array of primitives (e.g. ["a", "b"])
        if (rootArg?.extendableAcceptArray == true && pathParts.size == 2) {
            val rawElems = parseRawArrayElements(rawRoot)
            if (rawElems.isNotEmpty() && rawElems.none { it.trim().startsWith("{") }) {
                val targetChild = rootArg.extendableArguments.find { it.name == pathParts[1] }
                val formatted = rawElems.map { formatTypedJsonValue(unquoteJs(it.trim()), targetChild) }
                return "[${formatted.joinToString(", ")}]"
            }
        }

        if (pathParts.size == 1) {
            return rawRoot
        }

        fun extractLayer(rawArrayJson: String, pathIdx: Int, currentArgDef: BlockArgument?): String {
            val elements = parseRawArrayElements(rawArrayJson)
            val key = pathParts[pathIdx]
            val childDef = currentArgDef?.extendableArguments?.find { it.name == key }
            return if (pathIdx == pathParts.lastIndex) {
                val mapped = elements.map { elemJson ->
                    val obj = parseJsObject(elemJson)
                    val valueStr = obj?.get(key) ?: ""
                    formatTypedJsonValue(valueStr, childDef)
                }
                "[${mapped.joinToString(", ")}]"
            } else {
                val nested = elements.map { elemJson ->
                    val obj = parseJsObject(elemJson)
                    val subArrayRaw = obj?.get(key) ?: "[]"
                    extractLayer(subArrayRaw, pathIdx + 1, childDef)
                }
                "[${nested.joinToString(", ")}]"
            }
        }

        return extractLayer(rawRoot, 1, rootArg)
    }

    private fun formatTypedJsonValue(rawValue: String, argDef: BlockArgument?): String {
        val isNumeric = argDef?.type in listOf(
            ScratchArgumentType.NUMBER,
            ScratchArgumentType.SLIDER,
            ScratchArgumentType.ANGLE,
            ScratchArgumentType.NOTE
        )
        return when {
            isNumeric && rawValue.trim().toDoubleOrNull() != null -> rawValue.trim()
            argDef?.type == ScratchArgumentType.BOOLEAN &&
                (rawValue.equals("true", true) || rawValue.equals("false", true)) -> rawValue.lowercase()
            argDef?.type == ScratchArgumentType.EXTENDABLE &&
                rawValue.trim().startsWith("[") && rawValue.trim().endsWith("]") -> rawValue.trim()
            argDef?.type == ScratchArgumentType.STRING -> "\"${escapeJson(rawValue)}\""
            else -> formatJsonScalar(rawValue)
        }
    }

    private fun parseRawArrayElements(raw: String): List<String> {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        return splitTopLevelComma(inner).map { it.trim() }
    }

    private fun evaluateConsoleArgs(
        rawArgs: String,
        block: ExtensionBlock,
        argValues: Map<String, String>,
        localVars: Map<String, String>
    ): String {
        return splitTopLevelComma(rawArgs).joinToString(" ") { rawPart ->
            val part = rawPart.trim()
            when {
                part.startsWith("`") && part.endsWith("`") && part.length >= 2 -> {
                    val template = part.substring(1, part.length - 1)
                    Regex("\\$\\{([^}]+)}").replace(template) { m ->
                        evaluateLocalOrArgToken(m.groupValues[1].trim(), block, argValues, localVars)
                    }
                }
                (part.startsWith("'") && part.endsWith("'")) ||
                    (part.startsWith("\"") && part.endsWith("\"")) -> unquoteJs(part)
                else -> evaluateLocalOrArgToken(part, block, argValues, localVars)
            }
        }
    }

    private fun evaluateLocalOrArgToken(
        token: String,
        block: ExtensionBlock,
        argValues: Map<String, String>,
        localVars: Map<String, String>
    ): String {
        val trimmed = token.trim()
        localVars[trimmed]?.let { return it }
        Regex("^([A-Za-z0-9_]+)\\.length$").find(trimmed)?.let { m ->
            val v = localVars[m.groupValues[1]] ?: argValues[m.groupValues[1]] ?: ""
            return parseRawArrayElements(v).size.toString()
        }
        Regex("^args\\.([A-Za-z0-9_]+)$").find(trimmed)?.let { m ->
            return argValues[m.groupValues[1]] ?: ""
        }
        if (trimmed in argValues) return argValues[trimmed] ?: ""
        return evaluateReturnExpression(trimmed, block, argValues, localVars)
    }

    private fun evaluateReturnExpression(
        rawExpr: String,
        block: ExtensionBlock,
        argValues: Map<String, String>,
        localVars: Map<String, String> = emptyMap()
    ): String {
        val trimmed = rawExpr.trim()

        // 0. Direct localVar.join(sep) or util.extendableToArray(...).join(sep) or String(args.X).split(sep)
        Regex("^([A-Za-z0-9_]+)\\.join\\(([^)]*)\\)$").find(trimmed)?.let { m ->
            val arrText = localVars[m.groupValues[1]] ?: "[]"
            val sepArg = m.groupValues[2].trim()
            val sep = if (sepArg.isBlank()) "," else resolveScalarExpr(sepArg, argValues)
            val elems = parseJsArray(arrText) ?: emptyList()
            return "'${elems.joinToString(sep)}'"
        }
        Regex("^String\\(\\s*args\\.([A-Za-z0-9_]+)\\s*\\)\\.split\\(([^)]*)\\)$").find(trimmed)?.let { m ->
            val src = argValues[m.groupValues[1]].orEmpty()
            val sepArg = m.groupValues[2].trim()
            val sep = if (sepArg.isBlank()) "," else resolveScalarExpr(sepArg, argValues)
            val parts = if (sep.isEmpty()) src.map { it.toString() } else src.split(sep)
            val jsonItems = parts.joinToString(", ") { "\"${escapeJson(it)}\"" }
            return "[$jsonItems]"
        }
        Regex("^util\\.extendableToArray\\(\\s*args\\s*,([^)]+)\\)\\.join\\(([^)]*)\\)$").find(trimmed)?.let { m ->
            val pathParts = splitTopLevelComma(m.groupValues[1]).map { unquoteJs(it.trim()) }
            val arrText = evalExtendableToArray(pathParts, block, argValues)
            val sep = if (m.groupValues[2].isBlank()) "," else unquoteJs(m.groupValues[2].trim())
            val elems = parseJsArray(arrText) ?: emptyList()
            return "'${elems.joinToString(sep)}'"
        }
        Regex("^util\\.extendableToArray\\(\\s*args\\s*,([\\s\\S]+)\\)$").find(trimmed)?.let { m ->
            val pathParts = splitTopLevelComma(m.groupValues[1]).map { unquoteJs(it.trim()) }
            return evalExtendableToArray(pathParts, block, argValues)
        }
        localVars[trimmed]?.let { return it }

        // 0b. Template literal: `${args.ANIMAL}: ${args.BREED}`
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length >= 2) {
            val template = trimmed.substring(1, trimmed.length - 1)
            val interpolated = Regex("\\$\\{([^}]+)}").replace(template) { m ->
                evaluateLocalOrArgToken(m.groupValues[1].trim(), block, argValues, localVars)
            }
            return "'$interpolated'"
        }

        // 1. Dynamic object literal: { [args.KEY]: args.VALUE } or { k: v }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return "{}"
            val entries = splitTopLevelComma(inner).mapNotNull { entry ->
                val colonIdx = findTopLevelColon(entry)
                if (colonIdx == -1) null
                else {
                    val rawKey = entry.substring(0, colonIdx).trim()
                    val rawVal = entry.substring(colonIdx + 1).trim()
                    val resolvedKey = if (rawKey.startsWith("[") && rawKey.endsWith("]")) {
                        resolveScalarExpr(rawKey.substring(1, rawKey.length - 1).trim(), argValues)
                    } else {
                        rawKey.removeSurrounding("'").removeSurrounding("\"")
                    }
                    val resolvedVal = resolveScalarExpr(rawVal, argValues)
                    "\"${escapeJson(resolvedKey)}\": ${formatJsonScalar(resolvedVal)}"
                }
            }
            if (entries.isNotEmpty()) {
                return "{${entries.joinToString(", ")}}"
            }
        }

        // 2. Array literal: [args.ITEM] or [args.A, args.B]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.isEmpty()) return "[]"
            val items = splitTopLevelComma(inner).map { item ->
                formatJsonScalar(resolveScalarExpr(item.trim(), argValues))
            }
            return "[${items.joinToString(", ")}]"
        }

        // 3. Array length: args.ARR.length
        Regex("^args\\.([A-Za-z0-9_]+)\\.length$").find(trimmed)?.let { m ->
            val arrText = argValues[m.groupValues[1]].orEmpty()
            val arr = parseJsArray(arrText) ?: return "0"
            return arr.size.toString()
        }

        // 4. Object/Array bracket access: args.OBJ[args.KEY] or args.OBJ['key'] or args.ARR[0]
        Regex("^args\\.([A-Za-z0-9_]+)\\[([^\\]]+)]$").find(trimmed)?.let { m ->
            val targetName = m.groupValues[1]
            val keyExpr = m.groupValues[2].trim()
            val resolvedKey = resolveScalarExpr(keyExpr, argValues)
            val targetText = argValues[targetName].orEmpty()
            val obj = parseJsObject(targetText)
            if (obj != null) {
                return obj[resolvedKey] ?: ""
            }
            val arr = parseJsArray(targetText)
            val idx = resolvedKey.toIntOrNull()
            if (arr != null && idx != null && idx in arr.indices) {
                return arr[idx]
            }
            return ""
        }

        // 5. Object dot access: args.OBJ.prop
        Regex("^args\\.([A-Za-z0-9_]+)\\.([A-Za-z0-9_]+)$").find(trimmed)?.let { m ->
            val targetName = m.groupValues[1]
            val propName = m.groupValues[2]
            val obj = parseJsObject(argValues[targetName].orEmpty())
            if (obj != null) {
                return obj[propName] ?: ""
            }
        }

        // 6. Standard primitive / expression substitution
        var expr = trimmed
        argValues.forEach { (k, v) ->
            val argDef = block.arguments.find { it.name == k }
            val isNumericType = argDef?.type in listOf(
                ScratchArgumentType.NUMBER,
                ScratchArgumentType.SLIDER,
                ScratchArgumentType.ANGLE,
                ScratchArgumentType.NOTE
            )
            val isObjectOrArray = argDef?.type in listOf(
                ScratchArgumentType.OBJECT,
                ScratchArgumentType.ARRAY,
                ScratchArgumentType.EXTENDABLE
            )
            val replacement = when {
                isNumericType && v.toDoubleOrNull() != null -> v.trim()
                isObjectOrArray -> v.trim().ifBlank { "null" }
                else -> "'$v'"
            }
            expr = expr.replace("String(args.$k)", "'$v'")
                .replace("Number(args.$k)", v.toDoubleOrNull()?.toString() ?: "0")
                .replace("args.$k", replacement)
        }
        return evaluateSimpleExpression(expr)
    }

    private fun resolveScalarExpr(expr: String, argValues: Map<String, String>): String {
        val trimmed = expr.trim()
        Regex("^args\\.([A-Za-z0-9_]+)$").find(trimmed)?.let { m ->
            return argValues[m.groupValues[1]] ?: ""
        }
        if ((trimmed.startsWith("'") && trimmed.endsWith("'")) ||
            (trimmed.startsWith("\"") && trimmed.endsWith("\""))
        ) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return trimmed
    }

    private fun formatJsonScalar(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed == "true" || trimmed == "false" || trimmed == "null" -> trimmed
            trimmed.toDoubleOrNull() != null -> trimmed
            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]")) -> trimmed
            else -> "\"${escapeJson(value)}\""
        }
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    fun parseJsObject(raw: String): Map<String, String>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyMap()
        val map = linkedMapOf<String, String>()
        splitTopLevelComma(inner).forEach { entry ->
            val colonIdx = findTopLevelColon(entry)
            if (colonIdx != -1) {
                val key = unquoteJs(entry.substring(0, colonIdx).trim())
                val value = unquoteJs(entry.substring(colonIdx + 1).trim())
                map[key] = value
            }
        }
        return map
    }

    fun parseJsArray(raw: String): List<String>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        return splitTopLevelComma(inner).map { unquoteJs(it.trim()) }
    }

    private fun unquoteJs(token: String): String {
        if ((token.startsWith("\"") && token.endsWith("\"")) ||
            (token.startsWith("'") && token.endsWith("'"))
        ) {
            return token.substring(1, token.length - 1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\")
        }
        return token
    }

    private fun splitTopLevelComma(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var quote: Char? = null
        var start = 0
        for (i in text.indices) {
            val ch = text[i]
            if (quote != null) {
                if (ch == quote && text.getOrNull(i - 1) != '\\') quote = null
                continue
            }
            when (ch) {
                '\'', '"' -> quote = ch
                '{', '[', '(' -> depth++
                '}', ']', ')' -> if (depth > 0) depth--
                ',' -> if (depth == 0) {
                    parts.add(text.substring(start, i))
                    start = i + 1
                }
            }
        }
        if (start <= text.length) {
            parts.add(text.substring(start))
        }
        return parts.filter { it.isNotBlank() }
    }

    private fun findTopLevelColon(text: String): Int {
        var depth = 0
        var quote: Char? = null
        for (i in text.indices) {
            val ch = text[i]
            if (quote != null) {
                if (ch == quote && text.getOrNull(i - 1) != '\\') quote = null
                continue
            }
            when (ch) {
                '\'', '"' -> quote = ch
                '{', '[', '(' -> depth++
                '}', ']', ')' -> if (depth > 0) depth--
                ':' -> if (depth == 0) return i
            }
        }
        return -1
    }

    private fun evaluateSimpleExpression(expr: String): String {
        val trimmed = expr.trim()
        // String concatenation with +
        if (trimmed.contains("+") && (trimmed.contains("'") || trimmed.contains("\""))) {
            val parts = trimmed.split("+").map { part ->
                part.trim().removeSurrounding("'").removeSurrounding("\"")
            }
            return "'${parts.joinToString("")}'"
        }
        // Simple arithmetic multiplication
        if (trimmed.contains("*")) {
            val nums = trimmed.split("*").mapNotNull { it.trim().toDoubleOrNull() }
            if (nums.size == 2) {
                val res = nums[0] * nums[1]
                return if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
            }
        }
        return trimmed
    }

    private fun formatLabel(block: ExtensionBlock, argValues: Map<String, String>): String {
        var result = block.text
        if (block.isBranchBlock && block.branchTexts.isNotEmpty()) {
            val extra = block.branchTexts.filter { it.isNotBlank() }
            if (extra.isNotEmpty()) {
                result = "$result / ${extra.joinToString(" / ")}"
            }
        }
        argValues.forEach { (k, v) ->
            val argDef = block.arguments.find { it.name == k }
            val display = if (argDef?.type == ScratchArgumentType.EXTENDABLE && argDef.customTypeName.isBlank()) {
                val count = parseRawArrayElements(v).size
                "$count inputs"
            } else {
                v
            }
            result = result.replace("[$k]", "[$display]")
        }
        return result
    }
}
