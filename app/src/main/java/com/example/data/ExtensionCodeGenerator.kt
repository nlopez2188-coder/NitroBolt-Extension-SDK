package com.example.data

import com.example.model.BlockArgument
import com.example.model.CustomShapeDefinition
import com.example.model.CustomTypeDefinition
import com.example.model.CustomTypeShape
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.ExtensionProject
import com.example.model.LintFixAction
import com.example.model.LintIssue
import com.example.model.LintSeverity
import com.example.model.MenuItemEntry
import com.example.model.MenuMutatorState
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType

object ExtensionCodeGenerator {

    fun generate(project: ExtensionProject): String {
        val sb = StringBuilder()
        val safeClassName = sanitizeClassName(project.className.ifBlank { project.name })
        val safeId = project.id.ifBlank { "customextension" }
        val safeName = escapeJsString(project.name.ifBlank { "Custom Extension" })

        val wrapInIife = project.executionMode == ExecutionMode.UNSANDBOXED || true
        val indent = if (wrapInIife) "  " else ""

        if (wrapInIife) {
            sb.appendLine("(function(Scratch) {")
            sb.appendLine("  'use strict';")
            sb.appendLine()
        }

        if (project.executionMode == ExecutionMode.UNSANDBOXED && project.enforceUnsandboxedGuard) {
            sb.appendLine("${indent}if (!Scratch.extensions.unsandboxed) {")
            sb.appendLine("${indent}  throw new Error('${escapeJsString(project.name)} must run unsandboxed');")
            sb.appendLine("${indent}}")
            sb.appendLine()
        }

        if (project.executionMode == ExecutionMode.UNSANDBOXED && project.bindVmReference) {
            sb.appendLine("${indent}const vm = Scratch.vm;")
            sb.appendLine()
        }

        // Custom Shapes registered BEFORE Custom Types & Scratch.extensions.register()
        if (project.customShapes.isNotEmpty()) {
            project.customShapes.forEach { customShape ->
                appendCustomShapeRegistration(sb, customShape, indent)
                sb.appendLine()
            }
        }

        // Custom Types registered BEFORE Scratch.extensions.register()
        if (project.customTypes.isNotEmpty()) {
            project.customTypes.forEach { customType ->
                appendCustomTypeClass(sb, customType, indent)
                sb.appendLine()
            }
        }

        sb.appendLine("${indent}class $safeClassName {")
        if (project.constructorBody.isNotBlank()) {
            sb.appendLine("${indent}  constructor() {")
            project.constructorBody.trimEnd().lines().forEach { line ->
                sb.appendLine("${indent}    $line")
            }
            sb.appendLine("${indent}  }")
            sb.appendLine()
        }
        sb.appendLine("${indent}  getInfo() {")
        sb.appendLine("${indent}    return {")
        sb.appendLine("${indent}      id: '${escapeJsString(safeId)}',")
        sb.appendLine("${indent}      name: '$safeName',")
        if (project.useUnsandboxedRuntime) {
            sb.appendLine("${indent}      useUnsandboxedRuntime: true,")
        }
        sb.appendLine("${indent}      color1: '${project.color1Hex}',")
        sb.appendLine("${indent}      color2: '${project.color2Hex}',")
        sb.appendLine("${indent}      color3: '${project.color3Hex}',")

        // Blocks
        if (project.blocks.isEmpty()) {
            sb.appendLine("${indent}      blocks: []${if (project.menus.isNotEmpty()) "," else ""}")
        } else {
            sb.appendLine("${indent}      blocks: [")
            project.blocks.forEachIndexed { index, block ->
                val isLastBlock = index == project.blocks.lastIndex
                if (block.blockType == ScratchBlockType.LABEL) {
                    sb.appendLine("${indent}        {")
                    sb.appendLine("${indent}          blockType: ${block.blockType.scratchConstant},")
                    sb.appendLine("${indent}          text: '${escapeJsString(block.text)}'")
                    sb.appendLine("${indent}        }${if (isLastBlock) "" else ","}")
                } else {
                    sb.appendLine("${indent}        {")
                    sb.appendLine("${indent}          opcode: '${escapeJsString(block.opcode)}',")
                    sb.appendLine("${indent}          blockType: ${block.blockType.scratchConstant},")
                    if (block.dualType != null) {
                        sb.appendLine("${indent}          dualType: ${block.dualType.scratchConstant},")
                    }
                    val hasBranchCount = block.isBranchBlock || block.branchCount > 0
                    val hasOutputType = block.outputType.isNotBlank()
                    val hasBlockShape = block.blockShape.isNotBlank()
                    val hasDuplicateOnDrag = block.duplicateOnDrag
                    val hasArgs = block.arguments.isNotEmpty()
                    val hasSwitches = block.switches.isNotEmpty()
                    val cleanBranchTexts = block.branchTexts
                        .take(block.effectiveBranchCount.coerceAtLeast(0))
                        .dropLastWhile { it.isBlank() }
                    val textJs = if (block.isBranchBlock && cleanBranchTexts.isNotEmpty()) {
                        (listOf(block.text) + cleanBranchTexts)
                            .joinToString(", ", prefix = "[", postfix = "]") { "'${escapeJsString(it)}'" }
                    } else {
                        "'${escapeJsString(block.text)}'"
                    }
                    val hasMoreAfterText = hasBranchCount || hasOutputType || hasBlockShape || hasDuplicateOnDrag || hasArgs || hasSwitches
                    sb.appendLine("${indent}          text: $textJs${if (hasMoreAfterText) "," else ""}")
                    if (hasBranchCount) {
                        sb.appendLine("${indent}          branchCount: ${block.effectiveBranchCount}${if (hasOutputType || hasBlockShape || hasDuplicateOnDrag || hasArgs || hasSwitches) "," else ""}")
                    }
                    if (hasOutputType) {
                        sb.appendLine("${indent}          outputType: '${escapeJsString(block.outputType)}'${if (hasBlockShape || hasDuplicateOnDrag || hasArgs || hasSwitches) "," else ""}")
                    }
                    if (hasBlockShape) {
                        sb.appendLine("${indent}          blockShape: '${escapeJsString(block.blockShape)}'${if (hasDuplicateOnDrag || hasArgs || hasSwitches) "," else ""}")
                    }
                    if (hasDuplicateOnDrag) {
                        sb.appendLine("${indent}          duplicateOnDrag: true${if (hasArgs || hasSwitches) "," else ""}")
                    }
                    if (hasArgs) {
                        sb.appendLine("${indent}          arguments: {")
                        block.arguments.forEachIndexed { argIdx, arg ->
                            val isLastArg = argIdx == block.arguments.lastIndex
                            appendArgumentDefinition(
                                sb = sb,
                                arg = arg,
                                argIndent = "${indent}            ",
                                isLastArg = isLastArg
                            )
                        }
                        sb.appendLine("${indent}          }${if (hasSwitches) "," else ""}")
                    }
                    if (hasSwitches) {
                        sb.appendLine("${indent}          switches: [")
                        block.switches.forEachIndexed { swIdx, sw ->
                            val isLastSw = swIdx == block.switches.lastIndex
                            val comma = if (isLastSw) "" else ","
                            if (sw.isSimpleStringForm) {
                                sb.appendLine("${indent}            '${escapeJsString(sw.targetId)}'$comma")
                            } else {
                                sb.appendLine("${indent}            {")
                                val props = mutableListOf<String>()
                                props.add("id: '${escapeJsString(sw.targetId)}'")
                                if (sw.inputs.isNotEmpty()) {
                                    val pairs = sw.inputs.joinToString(", ") {
                                        "['${escapeJsString(it.from)}', '${escapeJsString(it.to)}']"
                                    }
                                    props.add("inputs: [$pairs]")
                                }
                                if (sw.splitInputs.isNotEmpty()) {
                                    val dropped = sw.splitInputs.joinToString(", ") {
                                        "'${escapeJsString(it)}'"
                                    }
                                    props.add("splitInputs: [$dropped]")
                                }
                                if (sw.rawId) {
                                    props.add("rawId: true")
                                }
                                props.forEachIndexed { pIdx, prop ->
                                    val pComma = if (pIdx == props.lastIndex) "" else ","
                                    sb.appendLine("${indent}              $prop$pComma")
                                }
                                sb.appendLine("${indent}            }$comma")
                            }
                        }
                        sb.appendLine("${indent}          ]")
                    }
                    sb.appendLine("${indent}        }${if (isLastBlock) "" else ","}")
                }
            }
            sb.appendLine("${indent}      ]${if (project.menus.isNotEmpty()) "," else ""}")
        }

        // Menus
        if (project.menus.isNotEmpty()) {
            sb.appendLine("${indent}      menus: {")
            project.menus.forEachIndexed { mIdx, menu ->
                val isLastMenu = mIdx == project.menus.lastIndex
                sb.appendLine("${indent}        ${sanitizeIdentifier(menu.name)}: {")
                if (menu.isDependent) {
                    sb.appendLine("${indent}          parentName: '${escapeJsString(menu.parentName)}',")
                    val hasDefaultOpts = menu.defaultOptions.isNotEmpty()
                    sb.appendLine("${indent}          optionMapping: {")
                    val entries = menu.optionMapping.entries.toList()
                    entries.forEachIndexed { eIdx, (parentVal, options) ->
                        val isLastEntry = eIdx == entries.lastIndex
                        val keyExpr = if (Regex("^[A-Za-z_][A-Za-z0-9_]*$").matches(parentVal)) {
                            parentVal
                        } else {
                            "'${escapeJsString(parentVal)}'"
                        }
                        val formattedOpts = formatMenuEntries(options)
                        sb.appendLine("${indent}            $keyExpr: [$formattedOpts]${if (isLastEntry) "" else ","}")
                    }
                    sb.appendLine("${indent}          }${if (hasDefaultOpts) "," else ""}")
                    if (hasDefaultOpts) {
                        sb.appendLine("${indent}          defaultOptions: [${formatMenuEntries(menu.defaultOptions)}]")
                    }
                } else {
                    val itemsFormatted = formatMenuEntries(menu.regularEntries)
                    if (menu.isMutator) {
                        sb.appendLine("${indent}          items: [$itemsFormatted],")
                        sb.appendLine("${indent}          mutator: {")
                        val mutEntries = menu.mutator.entries.toList()
                        mutEntries.forEachIndexed { sIdx, (stateKey, stateDef) ->
                            val isLastState = sIdx == mutEntries.lastIndex
                            val keyExpr = if (Regex("^[A-Za-z_][A-Za-z0-9_]*$").matches(stateKey)) {
                                stateKey
                            } else {
                                "'${escapeJsString(stateKey)}'"
                            }
                            sb.appendLine("${indent}            $keyExpr: {")
                            val props = mutableListOf<String>()
                            stateDef.output?.let { props.add("output: ${it.scratchConstant}") }
                            stateDef.previousStatement?.let { props.add("previousStatement: $it") }
                            stateDef.nextStatement?.let { props.add("nextStatement: $it") }
                            stateDef.inputsInline?.let { props.add("inputsInline: $it") }
                            val hasMutArgs = stateDef.arguments.isNotEmpty()
                            props.forEachIndexed { pIdx, prop ->
                                val trailing = if (pIdx == props.lastIndex && !hasMutArgs) "" else ","
                                sb.appendLine("${indent}              $prop$trailing")
                            }
                            if (hasMutArgs) {
                                sb.appendLine("${indent}              arguments: {")
                                stateDef.arguments.forEachIndexed { aIdx, mArg ->
                                    appendArgumentDefinition(
                                        sb = sb,
                                        arg = mArg,
                                        argIndent = "${indent}                ",
                                        isLastArg = aIdx == stateDef.arguments.lastIndex
                                    )
                                }
                                sb.appendLine("${indent}              }")
                            }
                            sb.appendLine("${indent}            }${if (isLastState) "" else ","}")
                        }
                        sb.appendLine("${indent}          }")
                    } else {
                        sb.appendLine("${indent}          acceptReporters: ${menu.acceptReporters},")
                        if (menu.acceptText) {
                            sb.appendLine("${indent}          acceptText: true,")
                        }
                        sb.appendLine("${indent}          items: [$itemsFormatted]")
                    }
                }
                sb.appendLine("${indent}        }${if (isLastMenu) "" else ","}")
            }
            sb.appendLine("${indent}      }")
        }

        sb.appendLine("${indent}    };")
        sb.appendLine("${indent}  }")

        // Methods for blocks
        project.blocks
            .filter { it.blockType != ScratchBlockType.LABEL }
            .forEach { block ->
                sb.appendLine()
                val methodName = sanitizeIdentifier(block.opcode)
                val usesUtil = block.jsBody.contains("util.") ||
                    block.isBranchBlock ||
                    block.arguments.any { it.type == ScratchArgumentType.EXTENDABLE }
                val param = when {
                    usesUtil -> "args, util"
                    block.arguments.isNotEmpty() -> "args"
                    else -> ""
                }
                sb.appendLine("${indent}  $methodName($param) {")
                val bodyLines = block.jsBody.trimEnd().lines()
                if (bodyLines.isEmpty() || block.jsBody.isBlank()) {
                    when (block.blockType) {
                        ScratchBlockType.REPORTER -> sb.appendLine("${indent}    return '';")
                        ScratchBlockType.BOOLEAN -> sb.appendLine("${indent}    return false;")
                        ScratchBlockType.OBJECT -> sb.appendLine("${indent}    return {};")
                        ScratchBlockType.ARRAY -> sb.appendLine("${indent}    return [];")
                        ScratchBlockType.CONDITIONAL -> sb.appendLine("${indent}    util.startBranch(1, false);")
                        ScratchBlockType.LOOP -> sb.appendLine("${indent}    util.startBranch(1, true);")
                        else -> sb.appendLine("${indent}    // Implement ${block.opcode}")
                    }
                } else {
                    bodyLines.forEach { line ->
                        sb.appendLine("${indent}    $line")
                    }
                }
                sb.appendLine("${indent}  }")
            }

        sb.appendLine("${indent}}")
        sb.appendLine()
        sb.appendLine("${indent}Scratch.extensions.register(new $safeClassName());")

        if (wrapInIife) {
            sb.append("})(Scratch);")
        }

        return sb.toString()
    }

    fun analyzeProject(project: ExtensionProject): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        if (project.id.isBlank() || !project.id.all { it.isLetterOrDigit() } || project.id.any { it.isUpperCase() }) {
            issues.add(
                LintIssue(
                    id = "invalid_ext_id",
                    severity = LintSeverity.WARNING,
                    title = "Extension ID formatting",
                    detail = "NitroBolt extension IDs should be lowercase alphanumeric without spaces (e.g., '${sanitizeExtensionId(project.id)}').",
                    fixAction = LintFixAction.SANITIZE_EXTENSION_ID
                )
            )
        }

        if (project.executionMode == ExecutionMode.UNSANDBOXED && !project.enforceUnsandboxedGuard) {
            issues.add(
                LintIssue(
                    id = "missing_unsandboxed_guard",
                    severity = LintSeverity.WARNING,
                    title = "Missing Scratch.extensions.unsandboxed guard",
                    detail = "Unsandboxed extensions that rely on VM internals should throw if loaded in a sandboxed iframe.",
                    fixAction = LintFixAction.ENABLE_UNSANDBOXED_GUARD
                )
            )
        }

        if (project.customShapes.isNotEmpty() && project.executionMode == ExecutionMode.SANDBOXED) {
            issues.add(
                LintIssue(
                    id = "custom_shapes_require_unsandboxed",
                    severity = LintSeverity.ERROR,
                    title = "Custom Shapes require Unsandboxed mode",
                    detail = "Scratch.BlockShapes.register() is only available to unsandboxed extensions. Switch execution mode to Unsandboxed.",
                    fixAction = LintFixAction.SWITCH_TO_UNSANDBOXED
                )
            )
        }

        if (project.customTypes.isNotEmpty() && project.executionMode == ExecutionMode.SANDBOXED) {
            issues.add(
                LintIssue(
                    id = "custom_types_require_unsandboxed",
                    severity = LintSeverity.ERROR,
                    title = "Custom Types require Unsandboxed mode",
                    detail = "Scratch.types.register() is only available to unsandboxed extensions. Switch execution mode to Unsandboxed.",
                    fixAction = LintFixAction.SWITCH_TO_UNSANDBOXED
                )
            )
        }

        val registeredShapeNames = project.customShapes.map { it.shapeName }.toSet()
        val registeredTypeNames = project.customTypes.map { it.typeName }.toSet()

        project.customShapes.forEach { shape ->
            val validation = CustomShapePathEngine.validateShapeEdges(shape)
            if (!validation.isValid) {
                issues.add(
                    LintIssue(
                        id = "invalid_shape_edges_${shape.id}",
                        severity = LintSeverity.WARNING,
                        title = "Shape '${shape.shapeName}' edge geometry warning",
                        detail = validation.message
                    )
                )
            }
        }

        project.customTypes.forEach { customType ->
            if (customType.shape == CustomTypeShape.CUSTOM &&
                customType.customShapeName.isNotBlank() &&
                customType.customShapeName !in registeredShapeNames
            ) {
                issues.add(
                    LintIssue(
                        id = "unknown_type_shape_${customType.id}",
                        severity = LintSeverity.WARNING,
                        title = "Unregistered shape '${customType.customShapeName}' on type '${customType.typeName}'",
                        detail = "Register '${customType.customShapeName}' in Custom Shapes before Scratch.extensions.register()."
                    )
                )
            }
        }

        project.menus.forEach { menu ->
            if (menu.isDependent) {
                val hasAnyOption = menu.optionMapping.values.any { list -> list.any { !it.isSeparator } } ||
                    menu.defaultOptions.any { !it.isSeparator }
                if (!hasAnyOption) {
                    issues.add(
                        LintIssue(
                            id = "dependent_menu_empty_${menu.id}",
                            severity = LintSeverity.ERROR,
                            title = "Dependent menu '${menu.name}' has no selectable options",
                            detail = "Add at least one option in optionMapping or defaultOptions for dependent menu '${menu.name}'."
                        )
                    )
                }
                if (menu.acceptText || menu.acceptReporters) {
                    issues.add(
                        LintIssue(
                            id = "dependent_menu_unsupported_flags_${menu.id}",
                            severity = LintSeverity.WARNING,
                            title = "Dependent menu '${menu.name}' ignores acceptText/acceptReporters",
                            detail = "Dependent dropdowns do not support acceptText or acceptReporters in NitroBolt."
                        )
                    )
                }
            } else if (menu.selectableItems.isEmpty()) {
                issues.add(
                    LintIssue(
                        id = "menu_no_selectable_items_${menu.id}",
                        severity = LintSeverity.ERROR,
                        title = "Menu '${menu.name}' has no selectable items",
                        detail = "Separators ('---') are visual only. Every menu must contain at least one selectable item."
                    )
                )
            }
            if (menu.isMutator) {
                if (menu.isDependent) {
                    issues.add(
                        LintIssue(
                            id = "mutator_with_dependent_${menu.id}",
                            severity = LintSeverity.ERROR,
                            title = "Mutator menu '${menu.name}' cannot use parentName",
                            detail = "Mutator menus must be static direct dropdowns and cannot be combined with dependent dropdown parentName."
                        )
                    )
                }
                if (menu.acceptText || menu.acceptReporters) {
                    issues.add(
                        LintIssue(
                            id = "mutator_unsupported_flags_${menu.id}",
                            severity = LintSeverity.WARNING,
                            title = "Mutator menu '${menu.name}' ignores acceptText/acceptReporters",
                            detail = "Mutator menus must be static direct dropdowns without acceptText or acceptReporters."
                        )
                    )
                }
                val validValues = menu.selectableItems.toSet()
                menu.mutator.forEach { (stateKey, stateDef) ->
                    if (stateKey !in validValues) {
                        issues.add(
                            LintIssue(
                                id = "mutator_orphan_key_${menu.id}_$stateKey",
                                severity = LintSeverity.WARNING,
                                title = "Mutator key '$stateKey' matches no item in '${menu.name}'",
                                detail = "Every mutator key must match a selectable item value in '${menu.name}'."
                            )
                        )
                    }
                    if (stateDef.previousStatement == false || stateDef.nextStatement == false) {
                        issues.add(
                            LintIssue(
                                id = "mutator_statement_removal_${menu.id}_$stateKey",
                                severity = LintSeverity.INFO,
                                title = "Statement removal in '${menu.name}' ($stateKey)",
                                detail = "Removing previousStatement or nextStatement is only supported while that connection has no attached blocks."
                            )
                        )
                    }
                }
            }
        }

        val menusByName = project.menus.associateBy { it.name }

        project.blocks.forEach { block ->
            if (block.blockShape.isNotBlank() &&
                !block.blockShape.startsWith("Scratch.BlockShape.") &&
                block.blockShape !in registeredShapeNames
            ) {
                issues.add(
                    LintIssue(
                        id = "unknown_block_shape_${block.id}",
                        severity = LintSeverity.WARNING,
                        title = "Unregistered blockShape '${block.blockShape}' on '${block.opcode}'",
                        detail = "Register '${block.blockShape}' in Custom Shapes before Scratch.extensions.register() so the VM applies it.",
                        blockId = block.id
                    )
                )
            }
            if (block.outputType.isNotBlank() && block.outputType !in registeredTypeNames) {
                issues.add(
                    LintIssue(
                        id = "unknown_output_type_${block.id}",
                        severity = LintSeverity.WARNING,
                        title = "Unregistered outputType '${block.outputType}' on '${block.opcode}'",
                        detail = "Register '${block.outputType}' in Custom Types before Scratch.extensions.register() so the VM resolves it.",
                        blockId = block.id
                    )
                )
            }
            if (block.duplicateOnDrag) {
                val supportsDuplicate = block.blockType == ScratchBlockType.REPORTER ||
                    block.blockType == ScratchBlockType.BOOLEAN ||
                    block.dualType == ScratchBlockType.REPORTER ||
                    block.dualType == ScratchBlockType.BOOLEAN
                if (!supportsDuplicate) {
                    issues.add(
                        LintIssue(
                            id = "duplicate_on_drag_invalid_type_${block.id}",
                            severity = LintSeverity.WARNING,
                            title = "duplicateOnDrag ignored on '${block.opcode}'",
                            detail = "duplicateOnDrag can only be used on Reporter and Boolean blocks.",
                            blockId = block.id
                        )
                    )
                }
            }
            if (block.branchCount > 0 && !block.isBranchBlock) {
                issues.add(
                    LintIssue(
                        id = "branch_count_invalid_type_${block.id}",
                        severity = LintSeverity.ERROR,
                        title = "branchCount invalid on '${block.opcode}'",
                        detail = "branchCount can only be used in blocks with blockType set to CONDITIONAL or LOOP.",
                        blockId = block.id
                    )
                )
            }
            if (block.isBranchBlock && block.branchCount < 1) {
                issues.add(
                    LintIssue(
                        id = "branch_count_missing_${block.id}",
                        severity = LintSeverity.WARNING,
                        title = "branchCount should be >= 1 on '${block.opcode}'",
                        detail = "Set branchCount to 1 or more on '${block.opcode}' so branch mouths are defined explicitly.",
                        blockId = block.id
                    )
                )
            }
            if (block.isBranchBlock && block.jsBody.isNotBlank() && !block.jsBody.contains("util.startBranch")) {
                issues.add(
                    LintIssue(
                        id = "branch_missing_start_${block.id}",
                        severity = LintSeverity.INFO,
                        title = "Call util.startBranch in '${block.opcode}'",
                        detail = "Call util.startBranch(1, ${block.blockType == ScratchBlockType.LOOP}) inside ${block.opcode}(args, util) to execute branch scripts.",
                        blockId = block.id
                    )
                )
            }
            if (block.blockType == ScratchBlockType.CONDITIONAL &&
                Regex("util\\.startBranch\\([^,]+,\\s*true").containsMatchIn(block.jsBody)
            ) {
                issues.add(
                    LintIssue(
                        id = "branch_prefer_loop_${block.id}",
                        severity = LintSeverity.INFO,
                        title = "Consider Scratch.BlockType.LOOP on '${block.opcode}'",
                        detail = "LOOP is recommended when util.startBranch(..., true) loops, as NitroBolt displays a loop icon on the block.",
                        blockId = block.id
                    )
                )
            }
            block.arguments.forEach { arg ->
                validateArgumentLint(block, arg, registeredTypeNames, project.blocks, issues)
            }
            if (block.arguments.any { it.type == ScratchArgumentType.EXTENDABLE && it.customTypeName.isBlank() } &&
                block.jsBody.isNotBlank() &&
                !block.jsBody.contains("util.extendableToArray")
            ) {
                issues.add(
                    LintIssue(
                        id = "extendable_missing_helper_${block.id}",
                        severity = LintSeverity.INFO,
                        title = "Use util.extendableToArray in '${block.opcode}'",
                        detail = "Call util.extendableToArray(args, ...) to read extendable inputs as an ordered array.",
                        blockId = block.id
                    )
                )
            }
            if (block.blockType.requiresReturn && !hasReturnStatement(block.jsBody)) {
                issues.add(
                    LintIssue(
                        id = "missing_return_${block.id}",
                        severity = LintSeverity.ERROR,
                        title = "${block.blockType.label} '${block.opcode}' must return a value",
                        detail = "In unsandboxed extensions, reporter and boolean blocks that omit a valid return value (string, number, or boolean) break scripts in unknown ways.",
                        blockId = block.id,
                        fixAction = LintFixAction.ADD_RETURN_DEFAULT
                    )
                )
            }

            if (hasInfiniteLoopRisk(block.jsBody)) {
                issues.add(
                    LintIssue(
                        id = "infinite_loop_${block.id}",
                        severity = LintSeverity.ERROR,
                        title = "Infinite loop risk in '${block.opcode}'",
                        detail = "Unsandboxed blocks run on the main UI thread. Infinite loops (while(true)/for(;;)) freeze the entire editor and can cause data loss.",
                        blockId = block.id
                    )
                )
            }

            if (project.executionMode == ExecutionMode.SANDBOXED &&
                (block.jsBody.contains("vm.") || block.jsBody.contains("Scratch.vm"))
            ) {
                issues.add(
                    LintIssue(
                        id = "sandbox_vm_${block.id}",
                        severity = LintSeverity.ERROR,
                        title = "VM access blocked in Sandboxed mode",
                        detail = "Block '${block.opcode}' references Scratch.vm, which is unavailable inside a cross-origin sandboxed iframe. Switch to Unsandboxed.",
                        blockId = block.id,
                        fixAction = LintFixAction.SWITCH_TO_UNSANDBOXED
                    )
                )
            }

            val argOrderInText = Regex("\\[([A-Za-z0-9_]+)]")
                .findAll(block.text)
                .map { it.groupValues[1] }
                .toList()
            val argByName = block.arguments.associateBy { it.name }
            block.arguments.forEachIndexed { argIdx, arg ->
                val menu = arg.menuName.takeIf { it.isNotBlank() }?.let { menusByName[it] }
                if (menu != null && menu.isDependent) {
                    val parentName = menu.parentName
                    val parentArg = argByName[parentName]
                    if (parentArg == null) {
                        issues.add(
                            LintIssue(
                                id = "dep_menu_missing_parent_${block.id}_${arg.name}",
                                severity = LintSeverity.ERROR,
                                title = "Parent argument '$parentName' missing on '${block.opcode}'",
                                detail = "Menu '${menu.name}' on '${arg.name}' depends on '$parentName', which is not defined on block '${block.opcode}'.",
                                blockId = block.id
                            )
                        )
                    } else {
                        val parentTextIdx = argOrderInText.indexOf(parentName)
                        val childTextIdx = argOrderInText.indexOf(arg.name)
                        val parentArgIdx = block.arguments.indexOfFirst { it.name == parentName }
                        val outOfOrder = when {
                            parentTextIdx >= 0 && childTextIdx >= 0 -> parentTextIdx >= childTextIdx
                            else -> parentArgIdx >= argIdx
                        }
                        if (outOfOrder) {
                            issues.add(
                                LintIssue(
                                    id = "dep_menu_parent_order_${block.id}_${arg.name}",
                                    severity = LintSeverity.ERROR,
                                    title = "Parent '[$parentName]' must appear before '[${arg.name}]' in '${block.opcode}'",
                                    detail = "Dependent dropdowns require the parent argument to appear earlier than the dependent child argument in the block text.",
                                    blockId = block.id
                                )
                            )
                        }
                    }

                    // Cycle detection
                    val visited = mutableSetOf(arg.name)
                    var cursorName: String? = parentName
                    while (cursorName != null) {
                        if (!visited.add(cursorName)) {
                            issues.add(
                                LintIssue(
                                    id = "dep_menu_cycle_${block.id}_${arg.name}",
                                    severity = LintSeverity.ERROR,
                                    title = "Circular dependent menu chain on '${block.opcode}'",
                                    detail = "Dependent dropdowns on '${block.opcode}' form a cycle involving '$cursorName'.",
                                    blockId = block.id
                                )
                            )
                            break
                        }
                        val nextMenu = argByName[cursorName]?.menuName?.let { menusByName[it] }
                        cursorName = nextMenu?.takeIf { it.isDependent }?.parentName
                    }
                }
            }

            val knownOpcodes = project.blocks.map { it.opcode }.toSet()
            block.switches.forEach { sw ->
                if (!sw.rawId && sw.targetId.isNotBlank() && sw.targetId !in knownOpcodes) {
                    issues.add(
                        LintIssue(
                            id = "switch_target_${block.id}_${sw.id}",
                            severity = LintSeverity.WARNING,
                            title = "Switch target '${sw.targetId}' not found in extension",
                            detail = "Block '${block.opcode}' switches to '${sw.targetId}', which isn't an opcode in this extension. Set rawId: true if targeting a built-in or external block.",
                            blockId = block.id
                        )
                    )
                }
            }
        }

        return issues
    }

    private fun hasReturnStatement(jsBody: String): Boolean {
        val cleaned = jsBody
            .replace(Regex("//.*"), "")
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        return Regex("\\breturn\\b\\s*[^;\\s]").containsMatchIn(cleaned)
    }

    private fun hasInfiniteLoopRisk(jsBody: String): Boolean {
        val noBreak = !jsBody.contains("break") && !jsBody.contains("return")
        val loopPattern = Regex("(while\\s*\\(\\s*true\\s*\\)|for\\s*\\(\\s*;\\s*;\\s*\\))")
        return loopPattern.containsMatchIn(jsBody) && noBreak
    }

    fun sanitizeExtensionId(raw: String): String {
        val cleaned = raw.lowercase().filter { it.isLetterOrDigit() }
        return cleaned.ifBlank { "customextension" }
    }

    private fun sanitizeClassName(raw: String): String {
        val parts = raw.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
        val pascal = parts.joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
        val safe = pascal.ifBlank { "CustomExtension" }
        return if (safe.first().isDigit()) "Ext$safe" else safe
    }

    private fun sanitizeIdentifier(raw: String): String {
        val cleaned = raw.replace(Regex("[^A-Za-z0-9_]"), "_")
        if (cleaned.isBlank()) return "op"
        return if (cleaned.first().isDigit()) "_$cleaned" else cleaned
    }

    private fun formatDefaultValue(type: ScratchArgumentType, value: String): String {
        return when (type) {
            ScratchArgumentType.NUMBER,
            ScratchArgumentType.ANGLE,
            ScratchArgumentType.NOTE,
            ScratchArgumentType.SLIDER -> {
                value.toDoubleOrNull()?.let { value.trim() } ?: "'${escapeJsString(value)}'"
            }
            ScratchArgumentType.BOOLEAN -> {
                if (value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)) {
                    value.lowercase()
                } else {
                    "'${escapeJsString(value)}'"
                }
            }
            else -> "'${escapeJsString(value)}'"
        }
    }

    private fun formatNumericLiteral(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        return if (trimmed.toDoubleOrNull() != null) trimmed else fallback
    }

    fun generateCustomShapeSnippet(customShape: CustomShapeDefinition): String {
        val sb = StringBuilder()
        appendCustomShapeRegistration(sb, customShape, "")
        return sb.toString().trimEnd()
    }

    private fun appendCustomShapeRegistration(
        sb: StringBuilder,
        customShape: CustomShapeDefinition,
        indent: String
    ) {
        val name = escapeJsString(customShape.shapeName.ifBlank { "customShape" })
        sb.appendLine("${indent}Scratch.BlockShapes.register('$name', {")
        val methods = mutableListOf<Pair<String, String>>()
        if (customShape.edgeWidthBody.isNotBlank()) {
            methods.add("edgeWidth(totalHeight)" to customShape.edgeWidthBody)
        }
        val leftBody = customShape.leftEdgeBody.ifBlank {
            "steps.push(`l \${-w} \${-w}`);\nif (halfStraight > 0) steps.push('v', -halfStraight * 2);\nsteps.push(`l \${w} \${-w}`);"
        }
        methods.add("leftEdge(steps, w, halfStraight)" to leftBody)
        if (customShape.rightEdgeBody.isNotBlank()) {
            methods.add("rightEdge(steps, w, halfStraight)" to customShape.rightEdgeBody)
        }
        if (customShape.paddingBody.isNotBlank()) {
            methods.add("padding(innerShape)" to customShape.paddingBody)
        }

        methods.forEachIndexed { idx, (signature, body) ->
            val comma = if (idx == methods.lastIndex) "" else ","
            sb.appendLine("${indent}  $signature {")
            body.trimEnd().lines().forEach { line ->
                sb.appendLine("${indent}    $line")
            }
            sb.appendLine("${indent}  }$comma")
        }
        sb.appendLine("${indent}});")
    }

    fun generateCustomTypeSnippet(customType: CustomTypeDefinition): String {
        val sb = StringBuilder()
        appendCustomTypeClass(sb, customType, "")
        return sb.toString().trimEnd()
    }

    private fun appendCustomTypeClass(
        sb: StringBuilder,
        customType: CustomTypeDefinition,
        indent: String
    ) {
        val cls = sanitizeClassName(customType.className.ifBlank { customType.typeName })
        sb.appendLine("${indent}class $cls {")

        val shapeExpr = when (customType.shape) {
            CustomTypeShape.DEFAULT -> null
            CustomTypeShape.CUSTOM -> customType.customShapeName.takeIf { it.isNotBlank() }?.let {
                "'${escapeJsString(it)}'"
            }
            else -> customType.shape.scratchShapeConst
        }
        if (shapeExpr != null) {
            sb.appendLine("${indent}  static get shape() {")
            sb.appendLine("${indent}    return $shapeExpr;")
            sb.appendLine("${indent}  }")
            sb.appendLine()
        }

        sb.appendLine("${indent}  constructor(value) {")
        val ctorLines = customType.constructorBody.ifBlank { "this.value = value;" }.trimEnd().lines()
        ctorLines.forEach { sb.appendLine("${indent}    $it") }
        sb.appendLine("${indent}  }")

        fun appendMethod(signature: String, body: String) {
            if (body.isNotBlank()) {
                sb.appendLine()
                sb.appendLine("${indent}  $signature {")
                body.trimEnd().lines().forEach { sb.appendLine("${indent}    $it") }
                sb.appendLine("${indent}  }")
            }
        }

        appendMethod("static cast(value)", customType.castBody)
        appendMethod("toString()", customType.toStringBody)
        appendMethod("static visualReport(instance)", customType.visualReportBody)
        appendMethod("static monitorContent(instance)", customType.monitorContentBody)
        appendMethod("valueOf()", customType.valueOfBody)
        appendMethod("toJSON()", customType.toJsonBody)
        appendMethod("static fromJSON(json)", customType.fromJsonBody)

        sb.appendLine("${indent}}")
        sb.appendLine("${indent}Scratch.types.register('${escapeJsString(customType.typeName)}', $cls);")
    }

    private fun appendArgumentDefinition(
        sb: StringBuilder,
        arg: BlockArgument,
        argIndent: String,
        isLastArg: Boolean
    ) {
        val comma = if (isLastArg) "" else ","
        val argKey = sanitizeIdentifier(arg.name)
        sb.appendLine("${argIndent}$argKey: {")
        val props = mutableListOf<String>()
        val isCustom = arg.customTypeName.isNotBlank()
        if (isCustom) {
            props.add("type: '${escapeJsString(arg.customTypeName)}'")
        } else {
            props.add("type: ${arg.type.scratchConstant}")
        }

        if (!isCustom && arg.type == ScratchArgumentType.EXTENDABLE) {
            props.add("text: '${escapeJsString(arg.extendableText.ifBlank { "[ITEM]" })}'")
            if (arg.extendableDefaultInputs.isNotBlank()) {
                props.add("defaultInputs: ${formatNumericLiteral(arg.extendableDefaultInputs, "2")}")
            }
            if (arg.extendableMinInputs.isNotBlank()) {
                props.add("minInputs: ${formatNumericLiteral(arg.extendableMinInputs, "1")}")
            }
            if (arg.extendableMaxInputs.isNotBlank()) {
                props.add("maxInputs: ${formatNumericLiteral(arg.extendableMaxInputs, "2")}")
            }
            if (arg.extendableAcceptArray) {
                props.add("acceptArray: true")
            }
            if (arg.extendableSeparator.isNotEmpty()) {
                props.add("separator: '${escapeJsString(arg.extendableSeparator)}'")
            }

            props.forEach { prop ->
                sb.appendLine("${argIndent}  $prop,")
            }
            if (arg.extendableArguments.isEmpty()) {
                sb.appendLine("${argIndent}  arguments: {}")
            } else {
                sb.appendLine("${argIndent}  arguments: {")
                arg.extendableArguments.forEachIndexed { childIdx, childArg ->
                    appendArgumentDefinition(
                        sb = sb,
                        arg = childArg,
                        argIndent = "${argIndent}    ",
                        isLastArg = childIdx == arg.extendableArguments.lastIndex
                    )
                }
                sb.appendLine("${argIndent}  }")
            }
            sb.appendLine("${argIndent}}$comma")
            return
        }

        if (arg.defaultValue.isNotBlank()) {
            props.add("defaultValue: ${formatDefaultValue(arg.type, arg.defaultValue)}")
        }
        if (!isCustom && arg.type == ScratchArgumentType.SLIDER) {
            props.add("min: ${formatNumericLiteral(arg.sliderMin, "0")}")
            props.add("max: ${formatNumericLiteral(arg.sliderMax, "100")}")
            if (arg.sliderPrecision.isNotBlank()) {
                props.add("precision: ${formatNumericLiteral(arg.sliderPrecision, "1")}")
            }
        }
        if (!isCustom && arg.type == ScratchArgumentType.STRING) {
            if (!arg.acceptReporters) {
                props.add("acceptReporters: false")
            }
            if (arg.canMultiline) {
                props.add("canMultiline: true")
            }
        }
        if (arg.menuName.isNotBlank()) {
            props.add("menu: '${escapeJsString(arg.menuName)}'")
        }
        if (arg.shadow.isNotBlank() && arg.acceptReporters) {
            props.add("shadow: '${escapeJsString(arg.shadow)}'")
        }

        props.forEachIndexed { pIdx, prop ->
            val pComma = if (pIdx == props.lastIndex) "" else ","
            sb.appendLine("${argIndent}  $prop$pComma")
        }
        sb.appendLine("${argIndent}}$comma")
    }

    private fun validateArgumentLint(
        block: ExtensionBlock,
        arg: BlockArgument,
        registeredTypeNames: Set<String>,
        allBlocks: List<ExtensionBlock>,
        issues: MutableList<LintIssue>
    ) {
        if (arg.customTypeName.isNotBlank() && arg.customTypeName !in registeredTypeNames) {
            issues.add(
                LintIssue(
                    id = "unknown_arg_type_${block.id}_${arg.name}",
                    severity = LintSeverity.WARNING,
                    title = "Unregistered argument type '${arg.customTypeName}' on '${block.opcode}'",
                    detail = "Register '${arg.customTypeName}' in Custom Types before Scratch.extensions.register() so argument '${arg.name}' works.",
                    blockId = block.id
                )
            )
        }
        if (arg.canMultiline && (arg.type != ScratchArgumentType.STRING || arg.customTypeName.isNotBlank())) {
            issues.add(
                LintIssue(
                    id = "invalid_multiline_${block.id}_${arg.name}",
                    severity = LintSeverity.WARNING,
                    title = "canMultiline ignored on '${arg.name}'",
                    detail = "canMultiline only applies to STRING arguments and has no effect on ${arg.type.label}.",
                    blockId = block.id
                )
            )
        }
        if ((arg.type == ScratchArgumentType.OBJECT || arg.type == ScratchArgumentType.ARRAY) &&
            arg.customTypeName.isBlank() &&
            arg.defaultValue.isNotBlank()
        ) {
            issues.add(
                LintIssue(
                    id = "slot_default_ignored_${block.id}_${arg.name}",
                    severity = LintSeverity.WARNING,
                    title = "defaultValue ignored on ${arg.type.label} slot '${arg.name}'",
                    detail = "${arg.type.label} arguments only accept reporter blocks and do not use literal defaultValue strings.",
                    blockId = block.id
                )
            )
        }
        if (arg.shadow.isNotBlank()) {
            val shadowTarget = allBlocks.find { it.opcode == arg.shadow }
            if (shadowTarget == null) {
                issues.add(
                    LintIssue(
                        id = "unknown_shadow_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Shadow reporter '${arg.shadow}' not found",
                        detail = "Argument '${arg.name}' on '${block.opcode}' references shadow '${arg.shadow}', which is not in this extension.",
                        blockId = block.id
                    )
                )
            } else {
                val validShadow = shadowTarget.blockType == ScratchBlockType.REPORTER ||
                    shadowTarget.blockType == ScratchBlockType.BOOLEAN ||
                    shadowTarget.blockType == ScratchBlockType.OBJECT ||
                    shadowTarget.blockType == ScratchBlockType.ARRAY ||
                    shadowTarget.dualType != null
                if (!validShadow) {
                    issues.add(
                        LintIssue(
                            id = "invalid_shadow_type_${block.id}_${arg.name}",
                            severity = LintSeverity.WARNING,
                            title = "Shadow '${arg.shadow}' is not a reporter",
                            detail = "Shadow blocks for '${arg.name}' must be Reporter, Boolean, Object, Array, or dualType blocks.",
                            blockId = block.id
                        )
                    )
                }
            }
        }
        if (arg.menuName.isNotBlank() && arg.defaultValue == ExtensionMenu.SEPARATOR_ITEM) {
            issues.add(
                LintIssue(
                    id = "menu_separator_default_${block.id}_${arg.name}",
                    severity = LintSeverity.WARNING,
                    title = "Separator '---' cannot be defaultValue on '${arg.name}'",
                    detail = "Menu separators ('---') are visual only and cannot be selected or used as an argument value on '${block.opcode}'.",
                    blockId = block.id
                )
            )
        }
        if (arg.type == ScratchArgumentType.SLIDER && arg.customTypeName.isBlank()) {
            val minVal = arg.sliderMin.ifBlank { "0" }.toDoubleOrNull()
            val maxVal = arg.sliderMax.ifBlank { "100" }.toDoubleOrNull()
            val precVal = arg.sliderPrecision.ifBlank { "1" }.toDoubleOrNull()
            val defVal = arg.defaultValue.toDoubleOrNull()
            if (minVal != null && maxVal != null && minVal >= maxVal) {
                issues.add(
                    LintIssue(
                        id = "slider_range_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Slider min ($minVal) must be less than max ($maxVal) on '${arg.name}'",
                        detail = "Set slider min strictly below max on '${block.opcode}'.",
                        blockId = block.id
                    )
                )
            }
            if (precVal != null && precVal <= 0.0) {
                issues.add(
                    LintIssue(
                        id = "slider_prec_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Slider precision must be > 0 on '${arg.name}'",
                        detail = "Set a positive step precision (e.g. 1 or 0.01) on '${block.opcode}'.",
                        blockId = block.id
                    )
                )
            }
            if (minVal != null && maxVal != null && minVal < maxVal && defVal != null && (defVal < minVal || defVal > maxVal)) {
                issues.add(
                    LintIssue(
                        id = "slider_default_bounds_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Slider default ($defVal) outside [$minVal, $maxVal] on '${arg.name}'",
                        detail = "Adjust defaultValue to fall inside the slider's min..max range.",
                        blockId = block.id
                    )
                )
            }
        }
        if (arg.type == ScratchArgumentType.EXTENDABLE && arg.customTypeName.isBlank()) {
            if (arg.extendableText.isBlank() || arg.extendableArguments.isEmpty()) {
                issues.add(
                    LintIssue(
                        id = "extendable_incomplete_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Incomplete extendable '${arg.name}' on '${block.opcode}'",
                        detail = "Extendable arguments require template text (e.g. '[ABC]') and at least one child argument.",
                        blockId = block.id
                    )
                )
            }
            val minInputs = arg.extendableMinInputs.takeIf { it.isNotBlank() }?.toIntOrNull()
            val maxInputs = arg.extendableMaxInputs.takeIf { it.isNotBlank() }?.toIntOrNull()
            val defInputs = arg.extendableDefaultInputs.takeIf { it.isNotBlank() }?.toIntOrNull()
            if (minInputs != null && minInputs < 0) {
                issues.add(
                    LintIssue(
                        id = "extendable_min_neg_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Negative minInputs on '${arg.name}'",
                        detail = "minInputs ($minInputs) cannot be negative.",
                        blockId = block.id
                    )
                )
            }
            if (minInputs != null && maxInputs != null && minInputs > maxInputs) {
                issues.add(
                    LintIssue(
                        id = "extendable_bounds_${block.id}_${arg.name}",
                        severity = LintSeverity.ERROR,
                        title = "Extendable minInputs exceeds maxInputs on '${arg.name}'",
                        detail = "minInputs ($minInputs) cannot exceed maxInputs ($maxInputs) on block '${block.opcode}'.",
                        blockId = block.id
                    )
                )
            }
            if (defInputs != null &&
                (defInputs < 0 ||
                    (minInputs != null && defInputs < minInputs) ||
                    (maxInputs != null && defInputs > maxInputs))
            ) {
                issues.add(
                    LintIssue(
                        id = "extendable_default_bounds_${block.id}_${arg.name}",
                        severity = LintSeverity.WARNING,
                        title = "Extendable defaultInputs out of range on '${arg.name}'",
                        detail = "defaultInputs ($defInputs) should fall between minInputs (${minInputs ?: 1}) and maxInputs (${maxInputs?.toString() ?: "unlimited"}).",
                        blockId = block.id
                    )
                )
            }
            arg.extendableArguments.forEach { child ->
                validateArgumentLint(block, child, registeredTypeNames, allBlocks, issues)
            }
        }
    }

    private fun formatMenuEntries(entries: List<MenuItemEntry>): String {
        return entries.joinToString(", ") { entry ->
            if (entry.isObjectForm) {
                "{ text: '${escapeJsString(entry.text)}', value: '${escapeJsString(entry.value)}' }"
            } else {
                "'${escapeJsString(entry.value)}'"
            }
        }
    }

    private fun escapeJsString(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
    }
}
