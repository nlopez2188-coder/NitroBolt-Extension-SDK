package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BlockSimulator
import com.example.model.BlockArgument
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType
import com.example.model.parseHexColor
import com.example.model.snapAndFormatSliderValue
import com.example.ui.components.CodeHighlighter
import com.example.ui.components.ScratchBlockVisual
import com.example.ui.theme.CodeBg
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ForgeOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.StudioUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeAndTestScreen(
    uiState: StudioUiState,
    onCopyCode: (String) -> Unit,
    onExportJsFile: () -> Unit,
    onShareJs: (String) -> Unit,
    onUpdateSimArg: (blockId: String, argName: String, value: String) -> Unit,
    onRunBlock: (ExtensionBlock) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project = uiState.project
    val color1 = parseHexColor(project.color1Hex, ForgeOrange)
    val color2 = parseHexColor(project.color2Hex, Color(0xFFDF7500))
    val color3 = parseHexColor(project.color3Hex, Color(0xFFB85D00))

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.code_test_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${project.id}.js • ${project.executionMode.contextLabel}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Export Actions Bar
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onCopyCode(uiState.generatedJs) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_js_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_copy_js))
                    }
                    FilledTonalButton(
                        onClick = onExportJsFile,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("export_js_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_export_js))
                    }
                    OutlinedButton(
                        onClick = { onShareJs(uiState.generatedJs) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_js_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_share_js))
                    }
                }
            }

            // Live Generated JS Code Window
            item {
                Surface(
                    color = CodeBg,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generated_code_view")
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${project.id.ifBlank { "extension" }}.js",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Surface(
                                color = if (project.executionMode == ExecutionMode.UNSANDBOXED) {
                                    ForgeOrange.copy(alpha = 0.2f)
                                } else {
                                    CyberCyan.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = project.executionMode.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (project.executionMode == ExecutionMode.UNSANDBOXED) ForgeOrange else CyberCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                        ) {
                            Text(
                                text = CodeHighlighter.highlightJs(uiState.generatedJs),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Live VM State & Block Simulator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.simulator_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    if (uiState.executionLogs.isNotEmpty()) {
                        IconButton(onClick = onClearLogs) {
                            Icon(Icons.Filled.ClearAll, contentDescription = "Reset VM & logs")
                        }
                    }
                }
            }

            // Virtual VM Telemetry Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VmStatChip(
                            label = "turboMode",
                            value = uiState.vmState.turboMode.toString(),
                            highlight = uiState.vmState.turboMode
                        )
                        VmStatChip(
                            label = "steps/sec",
                            value = "${uiState.vmState.fps} FPS",
                            highlight = uiState.vmState.turboMode
                        )
                        VmStatChip(
                            label = uiState.vmState.activeSpriteName,
                            value = "(${uiState.vmState.spriteX.toInt()}, ${uiState.vmState.spriteY.toInt()})",
                            highlight = false
                        )
                    }
                }
            }

            // Interactive Block Runner Cards
            items(
                project.blocks.filter { it.blockType != ScratchBlockType.LABEL },
                key = { it.id }
            ) { block ->
                SimulatorBlockCard(
                    block = block,
                    uiState = uiState,
                    color1 = color1,
                    color2 = color2,
                    color3 = color3,
                    onArgChange = { argName, value -> onUpdateSimArg(block.id, argName, value) },
                    onSwitchToTarget = { targetBlock, remapped ->
                        remapped.forEach { (k, v) -> onUpdateSimArg(targetBlock.id, k, v) }
                        onRunBlock(targetBlock)
                    },
                    onRun = { onRunBlock(block) }
                )
            }

            // Execution Log Output
            if (uiState.executionLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "Execution Trace",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(uiState.executionLogs, key = { it.id }) { log ->
                    Surface(
                        color = if (log.isError) CrimsonError.copy(alpha = 0.14f) else EmeraldSafe.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${log.opcode}() — ${log.blockLabel}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.executionTimeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.executionMode == ExecutionMode.UNSANDBOXED) ForgeOrange else CyberCyan
                                )
                            }
                            Text(
                                text = log.output,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (log.isError) CrimsonError else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun VmStatChip(label: String, value: String, highlight: Boolean) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) ForgeOrange else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SimulatorBlockCard(
    block: ExtensionBlock,
    uiState: StudioUiState,
    color1: Color,
    color2: Color,
    color3: Color,
    onArgChange: (String, String) -> Unit,
    onSwitchToTarget: (ExtensionBlock, Map<String, String>) -> Unit,
    onRun: () -> Unit
) {
    val blockInputs = uiState.simulatorInputs[block.id].orEmpty()
    val mutatedSpec = remember(block, blockInputs, uiState.project.menus) {
        BlockSimulator.resolveMutatedBlock(block, uiState.project.menus, blockInputs)
    }
    val effectiveBlock = mutatedSpec.block
    val hasActiveMutation = mutatedSpec.transformedArgNames.isNotEmpty() ||
        effectiveBlock.blockType != block.blockType ||
        mutatedSpec.previousStatement != null ||
        mutatedSpec.nextStatement != null ||
        mutatedSpec.inputsInline != null

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ScratchBlockVisual(
                        block = block,
                        menus = uiState.project.menus,
                        color1 = color1,
                        color2 = color2,
                        color3 = color3,
                        customTypes = uiState.project.customTypes,
                        customShapes = uiState.project.customShapes,
                        allBlocks = uiState.project.blocks,
                        activeValues = blockInputs
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onRun,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("run_block_${block.opcode}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_run_block))
                }
            }

            if (hasActiveMutation) {
                val mutDetails = buildList {
                    add("output: ${effectiveBlock.blockType.label}")
                    if (mutatedSpec.previousStatement == false) add("prev: false")
                    if (mutatedSpec.nextStatement == false) add("next: false")
                    if (mutatedSpec.inputsInline == false) add("inline: false")
                    if (mutatedSpec.transformedArgNames.isNotEmpty()) {
                        add("mutated: ${mutatedSpec.transformedArgNames.joinToString(", ")}")
                    }
                }.joinToString(" • ")
                Text(
                    text = "Mutator state → $mutDetails",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    modifier = Modifier.testTag("sim_mutator_status_${block.opcode}")
                )
            }

            val resolvedBlockInputs = remember(blockInputs, effectiveBlock, uiState.project, uiState.vmState) {
                BlockSimulator.resolveDefaultArgValues(
                    project = uiState.project,
                    block = effectiveBlock,
                    currentVm = uiState.vmState,
                    existingInputs = blockInputs
                )
            }

            effectiveBlock.arguments.forEach { arg ->
                val menu = arg.menuName.takeIf { it.isNotBlank() }?.let { mName ->
                    uiState.project.menus.find { it.name == mName }
                }
                val shadowValue = remember(arg.shadow, arg.acceptReporters, arg.menuName, uiState.project.blocks, uiState.vmState) {
                    if (arg.shadow.isNotBlank() && arg.acceptReporters && menu == null) {
                        BlockSimulator.resolveShadowValue(uiState.project, arg.shadow, uiState.vmState)
                    } else {
                        null
                    }
                }
                val isExtendable = arg.type == ScratchArgumentType.EXTENDABLE &&
                    arg.customTypeName.isBlank()
                val parentValueForMenu = menu?.takeIf { it.isDependent }?.let { resolvedBlockInputs[it.parentName] }
                val currentValue = resolvedBlockInputs[arg.name]
                    ?: if (isExtendable) {
                        BlockSimulator.buildDefaultExtendableJson(arg, uiState.project.menus)
                    } else {
                        arg.defaultValue.takeIf { it.isNotBlank() && it != ExtensionMenu.SEPARATOR_ITEM }
                            ?: menu?.firstSelectableValue(parentValueForMenu)
                            ?: shadowValue
                            ?: arg.type.sampleDefault
                    }

                if (isExtendable) {
                    ExtendableSimulatorEditor(
                        opcode = block.opcode,
                        arg = arg,
                        valueJson = currentValue,
                        menus = uiState.project.menus,
                        onValueChange = { onArgChange(arg.name, it) }
                    )
                } else if (menu != null) {
                    var expanded by remember { mutableStateOf(false) }
                    val menuEntries = menu.resolveOptions(parentValueForMenu)
                    val canEditMenuText = !menu.isDependent && !menu.isMutator && (menu.acceptText || menu.acceptReporters)
                    val modeBadge = when {
                        menu.isDependent -> "← ${menu.parentName}"
                        menu.isMutator -> "mutator"
                        menu.acceptText && menu.acceptReporters -> "acceptText + reporters"
                        menu.acceptText -> "acceptText"
                        menu.acceptReporters -> "reporters"
                        else -> "fixed dropdown"
                    }
                    val displaySelection = menuEntries.find { it.value == currentValue }?.let { entry ->
                        if (entry.isObjectForm) "${entry.text} (${entry.value})" else entry.text
                    } ?: currentValue

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (canEditMenuText) currentValue else displaySelection,
                            onValueChange = { if (canEditMenuText) onArgChange(arg.name, it) },
                            readOnly = !canEditMenuText,
                            label = { Text("${arg.name} (${menu.name} • $modeBadge)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(
                                    if (canEditMenuText) {
                                        MenuAnchorType.PrimaryEditable
                                    } else {
                                        MenuAnchorType.PrimaryNotEditable
                                    }
                                )
                                .fillMaxWidth()
                                .testTag("sim_menu_input_${block.opcode}_${arg.name}")
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            menuEntries.forEach { entry ->
                                if (entry.isSeparator) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (entry.isObjectForm) "${entry.text} (${entry.value})" else entry.text
                                            )
                                        },
                                        onClick = {
                                            onArgChange(arg.name, entry.value)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val isMultiline = arg.canMultiline &&
                        arg.type == ScratchArgumentType.STRING &&
                        arg.customTypeName.isBlank()
                    val isSlider = arg.type == ScratchArgumentType.SLIDER &&
                        arg.customTypeName.isBlank()
                    val isObject = arg.type == ScratchArgumentType.OBJECT &&
                        arg.customTypeName.isBlank()
                    val isArray = arg.type == ScratchArgumentType.ARRAY &&
                        arg.customTypeName.isBlank()
                    val qualifiers = buildList {
                        add(arg.customTypeName.ifBlank { arg.type.label })
                        if (isObject) add("JSON / reporter output")
                        if (isArray) add("JSON array / reporter output")
                        if (isSlider) {
                            add("${arg.sliderMin.ifBlank { "0" }}..${arg.sliderMax.ifBlank { "100" }}")
                            add("step ${arg.sliderPrecision.ifBlank { "1" }}")
                        }
                        if (isMultiline) add("multiline")
                        if (!arg.acceptReporters && arg.type == ScratchArgumentType.STRING) add("fixed field")
                        if (arg.shadow.isNotBlank() && arg.acceptReporters) add("shadow: ${arg.shadow}")
                    }.joinToString(" • ")

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { onArgChange(arg.name, it) },
                            label = { Text("${arg.name} ($qualifiers)") },
                            placeholder = when {
                                isObject -> ({ Text("{\"key\": \"value\"}") })
                                isArray -> ({ Text("[\"item\"]") })
                                else -> null
                            },
                            singleLine = !isMultiline,
                            minLines = if (isMultiline) 2 else 1,
                            maxLines = if (isMultiline) 6 else 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_arg_input_${block.opcode}_${arg.name}")
                        )
                        if (shadowValue != null && blockInputs[arg.name] != null && blockInputs[arg.name] != shadowValue) {
                            AssistChip(
                                onClick = { onArgChange(arg.name, shadowValue) },
                                label = { Text("Fill from shadow ${arg.shadow}() → $shadowValue") }
                            )
                        }
                        if (isObject || isArray) {
                            val lastOutput = uiState.executionLogs
                                .mapNotNull { entry ->
                                    val raw = entry.output.removePrefix("Returned:").trim()
                                    when {
                                        isObject && raw.startsWith("{") && raw.endsWith("}") -> raw
                                        isArray && raw.startsWith("[") && raw.endsWith("]") -> raw
                                        else -> null
                                    }
                                }
                                .firstOrNull()
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (lastOutput != null) {
                                    AssistChip(
                                        onClick = { onArgChange(arg.name, lastOutput) },
                                        label = { Text("Use last ${arg.type.label}") }
                                    )
                                }
                                AssistChip(
                                    onClick = {
                                        onArgChange(
                                            arg.name,
                                            if (isObject) "{\"key\": \"value\"}" else "[\"item\", \"two\"]"
                                        )
                                    },
                                    label = {
                                        Text(if (isObject) "Sample {\"key\": \"value\"}" else "Sample [\"item\", \"two\"]")
                                    }
                                )
                                if (currentValue.isNotBlank()) {
                                    AssistChip(
                                        onClick = { onArgChange(arg.name, "") },
                                        label = { Text("Empty") }
                                    )
                                }
                            }
                        }
                        if (isSlider) {
                            val minVal = arg.sliderMin.ifBlank { "0" }.toDoubleOrNull()
                            val maxVal = arg.sliderMax.ifBlank { "100" }.toDoubleOrNull()
                            val precVal = arg.sliderPrecision.ifBlank { "1" }.toDoubleOrNull()
                            if (minVal != null && maxVal != null && precVal != null && minVal < maxVal && precVal > 0.0) {
                                val sliderFloat = (currentValue.toDoubleOrNull()
                                    ?: arg.defaultValue.toDoubleOrNull()
                                    ?: minVal)
                                    .coerceIn(minVal, maxVal)
                                    .toFloat()
                                Slider(
                                    value = sliderFloat,
                                    onValueChange = { raw ->
                                        val formatted = snapAndFormatSliderValue(
                                            raw = raw,
                                            min = minVal,
                                            max = maxVal,
                                            precision = precVal,
                                            precisionText = arg.sliderPrecision.ifBlank { "1" },
                                            minText = arg.sliderMin.ifBlank { "0" }
                                        )
                                        onArgChange(arg.name, formatted)
                                    },
                                    valueRange = minVal.toFloat()..maxVal.toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sim_arg_slider_${block.opcode}_${arg.name}")
                                )
                            }
                        }
                    }
                }
            }

            if (block.switches.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    block.switches.forEach { sw ->
                        val targetBlock = uiState.project.blocks.find { it.opcode == sw.targetId }
                        FilledTonalButton(
                            onClick = {
                                if (targetBlock != null) {
                                    val currentValues = block.arguments.associate { arg ->
                                        arg.name to (blockInputs[arg.name] ?: arg.defaultValue)
                                    }.toMutableMap()
                                    sw.splitInputs.forEach { currentValues.remove(it) }
                                    val mapped = mutableMapOf<String, String>()
                                    currentValues.forEach { (k, v) ->
                                        val remap = sw.inputs.find { it.from == k }?.to ?: k
                                        if (targetBlock.arguments.any { it.name == remap }) {
                                            mapped[remap] = v
                                        }
                                    }
                                    onSwitchToTarget(targetBlock, mapped)
                                }
                            },
                            enabled = targetBlock != null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch → ${sw.targetId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtendableSimulatorEditor(
    opcode: String,
    arg: BlockArgument,
    valueJson: String,
    menus: List<ExtensionMenu>,
    onValueChange: (String) -> Unit,
    depth: Int = 0
) {
    var rawArrayMode by remember(arg.name, arg.extendableAcceptArray) { mutableStateOf(false) }
    val minCount = (arg.extendableMinInputs.toIntOrNull() ?: 1).coerceAtLeast(0)
    val maxCount = arg.extendableMaxInputs.toIntOrNull()?.coerceAtLeast(minCount)
    val instances = BlockSimulator.parseExtendableInstances(valueJson)
        ?: BlockSimulator.parseExtendableInstances(
            BlockSimulator.buildDefaultExtendableJson(arg, menus, depth)
        ).orEmpty()

    Surface(
        color = if (depth > 0) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[${arg.name}] (${instances.size} slots)",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    val boundsText = buildString {
                        append("min ${arg.extendableMinInputs.ifBlank { "1" }}")
                        append(" • max ${arg.extendableMaxInputs.ifBlank { "∞" }}")
                        if (arg.extendableSeparator.isNotEmpty()) {
                            append(" • sep '${arg.extendableSeparator}'")
                        }
                    }
                    Text(
                        text = boundsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!rawArrayMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = {
                                if (instances.size > minCount) {
                                    val next = instances.dropLast(1)
                                    onValueChange(BlockSimulator.serializeExtendableInstances(next, arg))
                                }
                            },
                            enabled = instances.size > minCount,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sim_ext_remove_${opcode}_${arg.name}")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Remove extendable slot", modifier = Modifier.size(16.dp))
                        }
                        FilledTonalButton(
                            onClick = {
                                if (maxCount == null || instances.size < maxCount) {
                                    val next = instances + BlockSimulator.buildDefaultExtendableInstance(arg, menus, depth)
                                    onValueChange(BlockSimulator.serializeExtendableInstances(next, arg))
                                }
                            },
                            enabled = maxCount == null || instances.size < maxCount,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sim_ext_add_${opcode}_${arg.name}")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add extendable slot", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (arg.extendableAcceptArray && depth == 0) {
                AssistChip(
                    onClick = { rawArrayMode = !rawArrayMode },
                    label = {
                        Text(if (rawArrayMode) "Switch to Slot Inputs" else "Pass Array JSON (acceptArray)")
                    }
                )
            }

            if (rawArrayMode) {
                OutlinedTextField(
                    value = valueJson,
                    onValueChange = onValueChange,
                    label = { Text("${arg.name} Raw Array JSON") },
                    placeholder = { Text("[\"item1\", \"item2\"]") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                instances.forEachIndexed { instIdx, instanceMap ->
                    if (instIdx > 0 && arg.extendableSeparator.isNotEmpty()) {
                        Text(
                            text = "Separator: '${arg.extendableSeparator}'",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Slot #${instIdx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            arg.extendableArguments.forEach { child ->
                                val childVal = instanceMap[child.name] ?: when {
                                    child.type == ScratchArgumentType.EXTENDABLE && child.customTypeName.isBlank() ->
                                        BlockSimulator.buildDefaultExtendableJson(child, menus, depth + 1)
                                    child.defaultValue.isNotBlank() -> child.defaultValue
                                    else -> child.type.sampleDefault
                                }
                                if (child.type == ScratchArgumentType.EXTENDABLE && child.customTypeName.isBlank() && depth < 2) {
                                    ExtendableSimulatorEditor(
                                        opcode = opcode,
                                        arg = child,
                                        valueJson = childVal,
                                        menus = menus,
                                        onValueChange = { updatedChildJson ->
                                            val updatedInstances = instances.toMutableList()
                                            val updatedMap = instanceMap.toMutableMap()
                                            updatedMap[child.name] = updatedChildJson
                                            updatedInstances[instIdx] = updatedMap
                                            onValueChange(BlockSimulator.serializeExtendableInstances(updatedInstances, arg))
                                        },
                                        depth = depth + 1
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = childVal,
                                        onValueChange = { newText ->
                                            val updatedInstances = instances.toMutableList()
                                            val updatedMap = instanceMap.toMutableMap()
                                            updatedMap[child.name] = newText
                                            updatedInstances[instIdx] = updatedMap
                                            onValueChange(BlockSimulator.serializeExtendableInstances(updatedInstances, arg))
                                        },
                                        label = { Text("${child.name} (${child.type.label})") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("sim_ext_field_${opcode}_${arg.name}_${instIdx}_${child.name}")
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
