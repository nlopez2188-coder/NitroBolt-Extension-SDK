package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockSimulator
import com.example.data.CustomShapePathEngine
import com.example.data.SvgRelStep
import com.example.model.BlockArgument
import com.example.model.CustomShapeDefinition
import com.example.model.CustomTypeDefinition
import com.example.model.CustomTypeShape
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily

private sealed class TextToken {
    data class Plain(val text: String) : TextToken()
    data class Arg(val name: String) : TextToken()
}

private fun tokenizeBlockText(text: String): List<TextToken> {
    val tokens = mutableListOf<TextToken>()
    val regex = Regex("\\[([A-Za-z0-9_]+)]")
    var lastIndex = 0
    for (match in regex.findAll(text)) {
        if (match.range.first > lastIndex) {
            tokens.add(TextToken.Plain(text.substring(lastIndex, match.range.first)))
        }
        tokens.add(TextToken.Arg(match.groupValues[1]))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        tokens.add(TextToken.Plain(text.substring(lastIndex)))
    }
    return tokens
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScratchBlockVisual(
    block: ExtensionBlock,
    menus: List<ExtensionMenu>,
    color1: Color,
    color2: Color,
    color3: Color,
    modifier: Modifier = Modifier,
    customTypes: List<CustomTypeDefinition> = emptyList(),
    customShapes: List<CustomShapeDefinition> = emptyList(),
    allBlocks: List<ExtensionBlock> = emptyList(),
    activeValues: Map<String, String> = emptyMap()
) {
    val mutatedSpec = BlockSimulator.resolveMutatedBlock(block, menus, activeValues)
    val effectiveBlock = mutatedSpec.block
    val hasPreviousStatement = mutatedSpec.previousStatement ?: true
    val hasNextStatement = mutatedSpec.nextStatement ?: true
    val isInlineInputs = mutatedSpec.inputsInline ?: true

    val tokens = tokenizeBlockText(effectiveBlock.text.ifBlank { effectiveBlock.opcode })
    val argMap = effectiveBlock.arguments.associateBy { it.name }
    val menusByName = menus.associateBy { it.name }
    val previewArgValues = linkedMapOf<String, String>()
    effectiveBlock.arguments.forEach { a ->
        val m = a.menuName.takeIf { it.isNotBlank() }?.let { menusByName[it] }
        val active = activeValues[a.name]
        val resolved = when {
            active != null -> active
            a.defaultValue.isNotBlank() && a.defaultValue != ExtensionMenu.SEPARATOR_ITEM -> a.defaultValue
            m != null -> m.firstSelectableValue(if (m.isDependent) previewArgValues[m.parentName] else null) ?: a.name
            else -> a.type.sampleDefault.ifBlank { a.name }
        }
        previewArgValues[a.name] = resolved
    }
    val outputCustomType = effectiveBlock.outputType.takeIf { it.isNotBlank() }?.let { outName ->
        customTypes.find { it.typeName == outName }
    }
    val outputCustomShape = outputCustomType?.shape

    val resolvedCustomShapeName = when {
        effectiveBlock.blockShape.isNotBlank() -> effectiveBlock.blockShape
        outputCustomShape == CustomTypeShape.CUSTOM && outputCustomType.customShapeName.isNotBlank() ->
            outputCustomType.customShapeName
        else -> ""
    }
    val registeredShapeDef = resolvedCustomShapeName.takeIf { it.isNotBlank() }?.let { shapeName ->
        customShapes.find { it.shapeName == shapeName }
    }

    val builtInFromBlockShape = when (effectiveBlock.blockShape) {
        "Scratch.BlockShape.SQUARE" -> CustomTypeShape.SQUARE
        "Scratch.BlockShape.HEXAGONAL" -> CustomTypeShape.HEXAGONAL
        "Scratch.BlockShape.LEAF" -> CustomTypeShape.LEAF
        "Scratch.BlockShape.PLUS" -> CustomTypeShape.PLUS
        "Scratch.BlockShape.OCTAGONAL" -> CustomTypeShape.OCTAGONAL
        else -> null
    }
    val effectiveBuiltInShape = builtInFromBlockShape ?: outputCustomShape

    val topPadding = when {
        registeredShapeDef != null -> 11.dp
        effectiveBlock.blockType == ScratchBlockType.HAT -> 18.dp
        effectiveBlock.blockType == ScratchBlockType.COMMAND -> if (hasPreviousStatement) 12.dp else 9.dp
        else -> 9.dp
    }
    val bottomPadding = when {
        registeredShapeDef != null -> 11.dp
        effectiveBlock.blockType == ScratchBlockType.HAT -> 14.dp
        effectiveBlock.blockType == ScratchBlockType.COMMAND -> if (hasNextStatement) 14.dp else 9.dp
        else -> 9.dp
    }

    val shapePadding = registeredShapeDef?.let { CustomShapePathEngine.evaluatePadding(it.paddingBody) }
    val startPadding = when {
        shapePadding != null -> (shapePadding.left * 0.72f + 10f).coerceIn(18f, 52f).dp
        effectiveBlock.blockType == ScratchBlockType.BOOLEAN -> 20.dp
        effectiveBlock.blockType == ScratchBlockType.OBJECT -> 18.dp
        effectiveBlock.blockType == ScratchBlockType.ARRAY -> 14.dp
        effectiveBlock.blockType == ScratchBlockType.REPORTER && effectiveBuiltInShape == CustomTypeShape.HEXAGONAL -> 20.dp
        effectiveBlock.blockType == ScratchBlockType.REPORTER -> 16.dp
        else -> 14.dp
    }
    val endPadding = when {
        shapePadding != null -> (shapePadding.right * 0.72f + 10f).coerceIn(18f, 58f).dp
        else -> startPadding
    }

    if (effectiveBlock.blockType == ScratchBlockType.LABEL) {
        Text(
            text = effectiveBlock.text.ifBlank { "Label" },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp)
        )
        return
    }

    if (effectiveBlock.isBranchBlock) {
        CShapeScratchBlock(
            block = effectiveBlock,
            argMap = argMap,
            menus = menus,
            customTypes = customTypes,
            allBlocks = allBlocks,
            color1 = color1,
            color2 = color2,
            color3 = color3,
            hasPreviousStatement = hasPreviousStatement,
            hasNextStatement = hasNextStatement,
            previewArgValues = previewArgValues,
            modifier = modifier
        )
        return
    }

    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .drawBehind {
                val w = size.width
                val h = size.height
                val path = if (registeredShapeDef != null) {
                    buildCustomEdgeBlockPath(
                        shapeDef = registeredShapeDef,
                        w = w,
                        h = h,
                        density = density
                    )
                } else {
                    buildScratchBlockPath(
                        type = effectiveBlock.blockType,
                        w = w,
                        h = h,
                        outputCustomShape = effectiveBuiltInShape,
                        hasPreviousStatement = hasPreviousStatement,
                        hasNextStatement = hasNextStatement
                    )
                }
                if (effectiveBlock.duplicateOnDrag) {
                    val shift = 3.dp.toPx()
                    translate(left = shift, top = shift) {
                        drawPath(path = path, color = color2.copy(alpha = 0.55f))
                        drawPath(
                            path = path,
                            color = color3.copy(alpha = 0.65f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
                drawPath(path = path, color = color1)
                drawPath(
                    path = path,
                    color = color3,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
            .padding(
                start = startPadding,
                end = endPadding,
                top = topPadding,
                bottom = bottomPadding
            )
    ) {
        if (isInlineInputs) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.Center
            ) {
                tokens.forEach { token ->
                    when (token) {
                        is TextToken.Plain -> {
                            val clean = token.text.trim()
                            if (clean.isNotEmpty()) {
                                Text(
                                    text = clean,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                        is TextToken.Arg -> {
                            val arg = argMap[token.name]
                            val shadowBlock = arg?.shadow?.takeIf { it.isNotBlank() }?.let { sOp ->
                                allBlocks.find { it.opcode == sOp }
                            }
                            BlockArgPill(
                                argName = token.name,
                                arg = arg,
                                shadowBlock = shadowBlock,
                                menus = menus,
                                customTypes = customTypes,
                                allBlocks = allBlocks,
                                color2 = color2,
                                color3 = color3,
                                previewArgValues = previewArgValues
                            )
                        }
                    }
                }
            }
        } else {
            val groupedRows = mutableListOf<List<TextToken>>()
            var currentLine = mutableListOf<TextToken>()
            tokens.forEach { token ->
                when (token) {
                    is TextToken.Plain -> {
                        if (token.text.trim().isNotEmpty()) {
                            currentLine.add(token)
                        }
                    }
                    is TextToken.Arg -> {
                        currentLine.add(token)
                        groupedRows.add(currentLine)
                        currentLine = mutableListOf()
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                groupedRows.add(currentLine)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                groupedRows.forEach { rowTokens ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowTokens.forEach { token ->
                            when (token) {
                                is TextToken.Plain -> {
                                    Text(
                                        text = token.text.trim(),
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                is TextToken.Arg -> {
                                    val arg = argMap[token.name]
                                    val shadowBlock = arg?.shadow?.takeIf { it.isNotBlank() }?.let { sOp ->
                                        allBlocks.find { it.opcode == sOp }
                                    }
                                    BlockArgPill(
                                        argName = token.name,
                                        arg = arg,
                                        shadowBlock = shadowBlock,
                                        menus = menus,
                                        customTypes = customTypes,
                                        allBlocks = allBlocks,
                                        color2 = color2,
                                        color3 = color3,
                                        previewArgValues = previewArgValues
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockArgPill(
    argName: String,
    arg: BlockArgument?,
    shadowBlock: ExtensionBlock?,
    menus: List<ExtensionMenu>,
    customTypes: List<CustomTypeDefinition>,
    allBlocks: List<ExtensionBlock>,
    color2: Color,
    color3: Color,
    depth: Int = 0,
    previewArgValues: Map<String, String> = emptyMap()
) {
    val isCustomType = arg?.customTypeName?.isNotBlank() == true

    if (arg?.type == ScratchArgumentType.EXTENDABLE && !isCustomType) {
        val rawDefaultCount = arg.extendableDefaultInputs.toIntOrNull() ?: 2
        val previewCount = if (depth > 0) {
            rawDefaultCount.coerceIn(1, 2)
        } else {
            rawDefaultCount.coerceIn(1, 3)
        }
        val childMap = arg.extendableArguments.associateBy { it.name }
        val innerTokens = tokenizeBlockText(arg.extendableText.ifBlank { "[ITEM]" })

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color2.copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            if (arg.extendableAcceptArray) {
                Text(
                    text = "[ ]",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            repeat(previewCount) { instanceIdx ->
                if (instanceIdx > 0 && arg.extendableSeparator.isNotEmpty()) {
                    Text(
                        text = arg.extendableSeparator,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                if (depth < 3) {
                    innerTokens.forEach { innerToken ->
                        when (innerToken) {
                            is TextToken.Plain -> {
                                val clean = innerToken.text.trim()
                                if (clean.isNotEmpty()) {
                                    Text(
                                        text = clean,
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            is TextToken.Arg -> {
                                val childArg = childMap[innerToken.name]
                                val childShadow = childArg?.shadow?.takeIf { it.isNotBlank() }?.let { sOp ->
                                    allBlocks.find { it.opcode == sOp }
                                }
                                BlockArgPill(
                                    argName = innerToken.name,
                                    arg = childArg,
                                    shadowBlock = childShadow,
                                    menus = menus,
                                    customTypes = customTypes,
                                    allBlocks = allBlocks,
                                    color2 = color2,
                                    color3 = color3,
                                    depth = depth + 1
                                )
                            }
                        }
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "−/+",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
        return
    }

    val menu = arg?.menuName?.takeIf { it.isNotBlank() }?.let { mName ->
        menus.find { it.name == mName }
    }
    val customType = arg?.customTypeName?.takeIf { it.isNotBlank() }?.let { typeName ->
        customTypes.find { it.typeName == typeName }
    }

    val parentPreviewValue = menu?.takeIf { it.isDependent }?.let { previewArgValues[it.parentName] }
    val activeRawValue = previewArgValues[argName]?.takeIf {
        it.isNotBlank() && it != ExtensionMenu.SEPARATOR_ITEM
    } ?: arg?.defaultValue?.takeIf {
        it.isNotBlank() && it != ExtensionMenu.SEPARATOR_ITEM
    }
    val resolvedMenuEntry = menu?.let { m ->
        val options = m.resolveOptions(parentPreviewValue).filter { !it.isSeparator }
        if (activeRawValue != null && (!m.isDependent || options.any { it.value == activeRawValue })) {
            options.find { it.value == activeRawValue }?.text ?: activeRawValue
        } else {
            options.firstOrNull()?.text ?: m.firstSelectableValue(parentPreviewValue)
        }
    }

    val displayValue = when {
        isCustomType -> arg?.customTypeName ?: argName
        menu != null -> resolvedMenuEntry ?: argName
        activeRawValue != null -> activeRawValue
        arg != null -> arg.type.sampleDefault.ifBlank { argName }
        else -> argName
    }

    val isBooleanArg = arg?.type == ScratchArgumentType.BOOLEAN && !isCustomType
    val isObjectArg = arg?.type == ScratchArgumentType.OBJECT && !isCustomType
    val isArrayArg = arg?.type == ScratchArgumentType.ARRAY && !isCustomType
    val isColorArg = arg?.type == ScratchArgumentType.COLOR && !isCustomType
    val isMenu = menu != null
    val isDependentMenu = menu?.isDependent == true
    val isMutatorMenu = menu?.isMutator == true
    val isTextMenu = menu?.acceptText == true && !isDependentMenu && !isMutatorMenu
    val isMenuReporterCapable = menu?.acceptReporters == true && !isDependentMenu && !isMutatorMenu
    val isPlainString = arg?.type == ScratchArgumentType.STRING && !isCustomType
    val isShadowReporter = arg?.shadow?.isNotBlank() == true && arg.acceptReporters && !isMenu
    val isFixedField = isPlainString && !arg.acceptReporters && !isMenu
    val isMultiline = isPlainString && arg.canMultiline && !isMenu && !isShadowReporter

    val effectiveText = when {
        isShadowReporter -> {
            val prefix = if (shadowBlock?.duplicateOnDrag == true) "⧉" else "◇"
            val label = shadowBlock?.text?.ifBlank { shadowBlock.opcode } ?: arg!!.shadow
            "$prefix $label"
        }
        isBooleanArg -> argName
        isObjectArg -> "{ } ${activeRawValue ?: argName}"
        isArrayArg -> "[ ] ${activeRawValue ?: argName}"
        else -> displayValue
    }

    val isDarkSlot = isBooleanArg || isObjectArg || isArrayArg
    val pillBg = when {
        isDarkSlot -> color3
        isCustomType -> color3.copy(alpha = 0.9f)
        isShadowReporter -> color2
        isTextMenu -> Color.White.copy(alpha = 0.95f)
        isMenu -> color2
        isFixedField -> Color.White.copy(alpha = 0.86f)
        else -> Color.White.copy(alpha = 0.95f)
    }
    val pillText = if ((isMenu && !isTextMenu) || isCustomType || isShadowReporter || isDarkSlot) {
        Color.White
    } else {
        Color(0xFF1E293B)
    }
    val objectSlotShape = GenericShape { size, _ ->
        val c = (size.height * 0.32f).coerceAtMost(size.width * 0.16f)
        moveTo(c, 0f)
        lineTo(size.width - c, 0f)
        lineTo(size.width, c)
        lineTo(size.width, size.height - c)
        lineTo(size.width - c, size.height)
        lineTo(c, size.height)
        lineTo(0f, size.height - c)
        lineTo(0f, c)
        close()
    }
    val pillShape = when {
        isObjectArg -> objectSlotShape
        isArrayArg -> RoundedCornerShape(4.dp)
        isCustomType && customType?.shape == CustomTypeShape.SQUARE -> RoundedCornerShape(4.dp)
        isCustomType -> RoundedCornerShape(7.dp)
        isFixedField -> RoundedCornerShape(4.dp)
        isMultiline -> RoundedCornerShape(8.dp)
        isTextMenu && isMenuReporterCapable -> CircleShape
        isTextMenu -> RoundedCornerShape(4.dp)
        isMenu && isMenuReporterCapable -> RoundedCornerShape(12.dp)
        isMenu -> RoundedCornerShape(5.dp)
        else -> CircleShape
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(pillShape)
            .background(pillBg)
            .border(
                width = if (isFixedField) 1.4.dp else 1.dp,
                color = when {
                    (isMenu && !isTextMenu) || isCustomType || isShadowReporter || isDarkSlot -> Color.White.copy(alpha = 0.45f)
                    isTextMenu -> color3.copy(alpha = 0.55f)
                    isFixedField -> color3.copy(alpha = 0.65f)
                    else -> Color.Black.copy(alpha = 0.14f)
                },
                shape = pillShape
            )
            .padding(
                horizontal = 8.dp,
                vertical = if (isMultiline) 5.dp else 3.dp
            )
    ) {
        if (isColorArg) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8A00))
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = effectiveText,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            softWrap = true,
            maxLines = if (isMultiline) Int.MAX_VALUE else 1,
            color = if (isBooleanArg) Color.White.copy(alpha = 0.9f) else pillText
        )
        if (isMenu) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = if (isTextMenu) color3 else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun buildCustomEdgeBlockPath(
    shapeDef: CustomShapeDefinition,
    w: Float,
    h: Float,
    density: Float
): Path {
    val hUnits = (h / density).coerceAtLeast(16f)
    val ewUnits = CustomShapePathEngine.computeEdgeWidth(shapeDef.edgeWidthBody, hUnits)
    val ew = (ewUnits * density * 0.72f).coerceIn(4f, h / 2f)
    val halfStraight = ((h - 2f * ew) / 2f).coerceAtLeast(0f)

    val leftSteps = CustomShapePathEngine.parseSvgEdgeSteps(shapeDef.leftEdgeBody, ew, halfStraight)
    if (leftSteps.isEmpty()) {
        return buildScratchBlockPath(ScratchBlockType.REPORTER, w, h, CustomTypeShape.DEFAULT)
    }
    val rightSteps = CustomShapePathEngine.resolveRightEdgeSteps(shapeDef, ew, halfStraight)

    // Measure horizontal excursion so outward points stay inside [0, w]
    var curX = 0f
    var minLeftX = 0f
    leftSteps.forEach { step ->
        curX += step.totalDx
        if (curX < minLeftX) minLeftX = curX
    }
    val leftEndOffsetX = curX

    curX = 0f
    var maxRightX = 0f
    rightSteps.forEach { step ->
        curX += step.totalDx
        if (curX > maxRightX) maxRightX = curX
    }

    val leftStartX = (-minLeftX).coerceIn(0f, w * 0.28f)
    val rightStartX = (w - maxRightX).coerceIn(w * 0.55f, w)

    val path = Path()
    // Start at bottom-left anchor and trace leftEdge upward
    path.moveTo(leftStartX, h)
    leftSteps.forEach { step ->
        when (step) {
            is SvgRelStep.Line -> path.relativeLineTo(step.dx, step.dy)
            is SvgRelStep.Horiz -> path.relativeLineTo(step.dx, 0f)
            is SvgRelStep.Vert -> path.relativeLineTo(0f, step.dy)
        }
    }

    // Top horizontal bar to top-right anchor
    val topRightAnchorX = (rightStartX).coerceAtLeast(leftStartX + leftEndOffsetX + 8f)
    path.lineTo(topRightAnchorX, 0f)

    // Trace rightEdge downward
    rightSteps.forEach { step ->
        when (step) {
            is SvgRelStep.Line -> path.relativeLineTo(step.dx, step.dy)
            is SvgRelStep.Horiz -> path.relativeLineTo(step.dx, 0f)
            is SvgRelStep.Vert -> path.relativeLineTo(0f, step.dy)
        }
    }

    // Bottom horizontal bar back to start
    path.close()
    return path
}

private fun buildScratchBlockPath(
    type: ScratchBlockType,
    w: Float,
    h: Float,
    outputCustomShape: CustomTypeShape? = null,
    hasPreviousStatement: Boolean = true,
    hasNextStatement: Boolean = true
): Path {
    val path = Path()
    when (type) {
        ScratchBlockType.REPORTER -> {
            when (outputCustomShape) {
                CustomTypeShape.SQUARE -> {
                    val r = 10f
                    path.moveTo(r, 0f)
                    path.lineTo(w - r, 0f)
                    path.quadraticTo(w, 0f, w, r)
                    path.lineTo(w, h - r)
                    path.quadraticTo(w, h, w - r, h)
                    path.lineTo(r, h)
                    path.quadraticTo(0f, h, 0f, h - r)
                    path.lineTo(0f, r)
                    path.quadraticTo(0f, 0f, r, 0f)
                    path.close()
                }
                CustomTypeShape.HEXAGONAL -> {
                    val tip = (h * 0.45f).coerceAtMost(w * 0.22f)
                    path.moveTo(tip, 0f)
                    path.lineTo(w - tip, 0f)
                    path.lineTo(w, h / 2f)
                    path.lineTo(w - tip, h)
                    path.lineTo(tip, h)
                    path.lineTo(0f, h / 2f)
                    path.close()
                }
                else -> {
                    val r = h / 2f
                    path.moveTo(r, 0f)
                    path.lineTo(w - r, 0f)
                    path.cubicTo(w - r * 0.25f, 0f, w, r * 0.35f, w, r)
                    path.cubicTo(w, h - r * 0.35f, w - r * 0.25f, h, w - r, h)
                    path.lineTo(r, h)
                    path.cubicTo(r * 0.25f, h, 0f, h - r * 0.35f, 0f, r)
                    path.cubicTo(0f, r * 0.35f, r * 0.25f, 0f, r, 0f)
                    path.close()
                }
            }
        }
        ScratchBlockType.OBJECT -> {
            val c = (h * 0.34f).coerceAtMost(w * 0.18f)
            path.moveTo(c, 0f)
            path.lineTo(w - c, 0f)
            path.lineTo(w, c)
            path.lineTo(w, h - c)
            path.lineTo(w - c, h)
            path.lineTo(c, h)
            path.lineTo(0f, h - c)
            path.lineTo(0f, c)
            path.close()
        }
        ScratchBlockType.ARRAY -> {
            val r = 6f
            path.moveTo(r, 0f)
            path.lineTo(w - r, 0f)
            path.quadraticTo(w, 0f, w, r)
            path.lineTo(w, h - r)
            path.quadraticTo(w, h, w - r, h)
            path.lineTo(r, h)
            path.quadraticTo(0f, h, 0f, h - r)
            path.lineTo(0f, r)
            path.quadraticTo(0f, 0f, r, 0f)
            path.close()
        }
        ScratchBlockType.BOOLEAN -> {
            val tip = (h * 0.45f).coerceAtMost(w * 0.22f)
            path.moveTo(tip, 0f)
            path.lineTo(w - tip, 0f)
            path.lineTo(w, h / 2f)
            path.lineTo(w - tip, h)
            path.lineTo(tip, h)
            path.lineTo(0f, h / 2f)
            path.close()
        }
        ScratchBlockType.HAT -> {
            val corner = 12f
            val domeWidth = (w * 0.48f).coerceAtMost(220f)
            val bodyBottom = if (hasNextStatement) h - 6f else h
            path.moveTo(0f, 22f)
            path.quadraticTo(domeWidth * 0.5f, -12f, domeWidth, 16f)
            path.lineTo(w - corner, 16f)
            path.quadraticTo(w, 16f, w, 16f + corner)
            path.lineTo(w, bodyBottom - corner)
            path.quadraticTo(w, bodyBottom, w - corner, bodyBottom)
            if (hasNextStatement) {
                path.lineTo(74f, bodyBottom)
                path.lineTo(66f, h)
                path.lineTo(42f, h)
                path.lineTo(34f, bodyBottom)
            }
            path.lineTo(corner, bodyBottom)
            path.quadraticTo(0f, bodyBottom, 0f, bodyBottom - corner)
            path.close()
        }
        ScratchBlockType.BUTTON -> {
            val r = 16f
            path.moveTo(r, 0f)
            path.lineTo(w - r, 0f)
            path.quadraticTo(w, 0f, w, r)
            path.lineTo(w, h - r)
            path.quadraticTo(w, h, w - r, h)
            path.lineTo(r, h)
            path.quadraticTo(0f, h, 0f, h - r)
            path.lineTo(0f, r)
            path.quadraticTo(0f, 0f, r, 0f)
            path.close()
        }
        ScratchBlockType.COMMAND,
        ScratchBlockType.LABEL,
        ScratchBlockType.CONDITIONAL,
        ScratchBlockType.LOOP -> {
            val corner = 12f
            val notchDepth = 10f
            val bodyBottom = if (hasNextStatement) h - 8f else h
            path.moveTo(corner, 0f)
            if (hasPreviousStatement) {
                path.lineTo(32f, 0f)
                path.lineTo(40f, notchDepth)
                path.lineTo(64f, notchDepth)
                path.lineTo(72f, 0f)
            }
            path.lineTo(w - corner, 0f)
            path.quadraticTo(w, 0f, w, corner)
            path.lineTo(w, bodyBottom - corner)
            path.quadraticTo(w, bodyBottom, w - corner, bodyBottom)
            if (hasNextStatement) {
                path.lineTo(72f, bodyBottom)
                path.lineTo(64f, h)
                path.lineTo(40f, h)
                path.lineTo(32f, bodyBottom)
            }
            path.lineTo(corner, bodyBottom)
            path.quadraticTo(0f, bodyBottom, 0f, bodyBottom - corner)
            path.lineTo(0f, corner)
            path.quadraticTo(0f, 0f, corner, 0f)
            path.close()
        }
    }
    return path
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CShapeScratchBlock(
    block: ExtensionBlock,
    argMap: Map<String, BlockArgument>,
    menus: List<ExtensionMenu>,
    customTypes: List<CustomTypeDefinition>,
    allBlocks: List<ExtensionBlock>,
    color1: Color,
    color2: Color,
    color3: Color,
    hasPreviousStatement: Boolean = true,
    hasNextStatement: Boolean = true,
    previewArgValues: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val branches = block.effectiveBranchCount.coerceAtLeast(1)

    Column(
        modifier = modifier.widthIn(min = 190.dp)
    ) {
        // Top C-block Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val corner = 12f
                    val notchDepth = 10f
                    val path = Path().apply {
                        moveTo(corner, 0f)
                        if (hasPreviousStatement) {
                            lineTo(32f, 0f)
                            lineTo(40f, notchDepth)
                            lineTo(64f, notchDepth)
                            lineTo(72f, 0f)
                        }
                        lineTo(w - corner, 0f)
                        quadraticTo(w, 0f, w, corner)
                        lineTo(w, h - corner)
                        quadraticTo(w, h, w - corner, h)
                        lineTo(18.dp.toPx(), h)
                        lineTo(0f, h)
                        lineTo(0f, corner)
                        quadraticTo(0f, 0f, corner, 0f)
                        close()
                    }
                    drawPath(path = path, color = color1)
                    drawPath(path = path, color = color3, style = Stroke(width = 1.6.dp.toPx()))
                }
                .padding(start = 14.dp, end = 14.dp, top = if (hasPreviousStatement) 12.dp else 9.dp, bottom = 10.dp)
        ) {
            BranchTextTokensRow(
                segmentText = block.text.ifBlank { block.opcode },
                argMap = argMap,
                menus = menus,
                customTypes = customTypes,
                allBlocks = allBlocks,
                color2 = color2,
                color3 = color3,
                previewArgValues = previewArgValues
            )
        }

        // Branch Mouths + Divider/Footer Bars
        for (i in 0 until branches) {
            // C-Mouth Cavity Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight()
                        .background(color1)
                        .drawBehind {
                            val strokePx = 1.6.dp.toPx()
                            drawLine(
                                color = color3,
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                                strokeWidth = strokePx
                            )
                            drawLine(
                                color = color3,
                                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                strokeWidth = strokePx
                            )
                        }
                )
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 28.dp)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color2.copy(alpha = 0.12f))
                        .border(1.dp, color3.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (branches > 1) "branch ${i + 1} script" else "branch script",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isLast = i == branches - 1
            val segText = block.branchTexts.getOrNull(i).orEmpty()
            if (!isLast) {
                // Mid Divider Bar between branches (e.g. 'else')
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 28.dp)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val corner = 10f
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w - corner, 0f)
                                quadraticTo(w, 0f, w, corner)
                                lineTo(w, h - corner)
                                quadraticTo(w, h, w - corner, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(path = path, color = color1)
                            drawPath(path = path, color = color3, style = Stroke(width = 1.6.dp.toPx()))
                        }
                        .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 7.dp)
                ) {
                    if (segText.isNotBlank()) {
                        BranchTextTokensRow(
                            segmentText = segText,
                            argMap = argMap,
                            menus = menus,
                            customTypes = customTypes,
                            allBlocks = allBlocks,
                            color2 = color2,
                            color3 = color3
                        )
                    }
                }
            } else {
                // Final Footer Bar + bottom puzzle notch + optional LOOP icon
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 28.dp)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val corner = 12f
                            val bodyBottom = if (hasNextStatement) h - 8f else h
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w - corner, 0f)
                                quadraticTo(w, 0f, w, corner)
                                lineTo(w, bodyBottom - corner)
                                quadraticTo(w, bodyBottom, w - corner, bodyBottom)
                                if (hasNextStatement) {
                                    lineTo(72f, bodyBottom)
                                    lineTo(64f, h)
                                    lineTo(40f, h)
                                    lineTo(32f, bodyBottom)
                                }
                                lineTo(corner, bodyBottom)
                                quadraticTo(0f, bodyBottom, 0f, bodyBottom - corner)
                                close()
                            }
                            drawPath(path = path, color = color1)
                            drawPath(path = path, color = color3, style = Stroke(width = 1.6.dp.toPx()))
                        }
                        .padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = if (hasNextStatement) 12.dp else 7.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (segText.isNotBlank()) {
                            BranchTextTokensRow(
                                segmentText = segText,
                                argMap = argMap,
                                menus = menus,
                                customTypes = customTypes,
                                allBlocks = allBlocks,
                                color2 = color2,
                                color3 = color3,
                                previewArgValues = previewArgValues
                            )
                        }
                    }
                    if (block.blockType == ScratchBlockType.LOOP) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Loop branch indicator",
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchTextTokensRow(
    segmentText: String,
    argMap: Map<String, BlockArgument>,
    menus: List<ExtensionMenu>,
    customTypes: List<CustomTypeDefinition>,
    allBlocks: List<ExtensionBlock>,
    color2: Color,
    color3: Color,
    previewArgValues: Map<String, String> = emptyMap()
) {
    val tokens = tokenizeBlockText(segmentText)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        tokens.forEach { token ->
            when (token) {
                is TextToken.Plain -> {
                    val clean = token.text.trim()
                    if (clean.isNotEmpty()) {
                        Text(
                            text = clean,
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
                is TextToken.Arg -> {
                    val arg = argMap[token.name]
                    val shadowBlock = arg?.shadow?.takeIf { it.isNotBlank() }?.let { sOp ->
                        allBlocks.find { it.opcode == sOp }
                    }
                    BlockArgPill(
                        argName = token.name,
                        arg = arg,
                        shadowBlock = shadowBlock,
                        menus = menus,
                        customTypes = customTypes,
                        allBlocks = allBlocks,
                        color2 = color2,
                        color3 = color3,
                        previewArgValues = previewArgValues
                    )
                }
            }
        }
    }
}
