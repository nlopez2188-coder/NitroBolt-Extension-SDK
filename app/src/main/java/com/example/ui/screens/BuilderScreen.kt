package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.CustomShapePathEngine
import com.example.data.ExtensionCodeGenerator
import com.example.data.ExtensionTemplate
import com.example.data.ExtensionTemplates
import com.example.model.BlockArgument
import com.example.model.BlockSwitch
import com.example.model.BlockSwitchInputMapping
import com.example.model.CustomShapeDefinition
import com.example.model.CustomTypeDefinition
import com.example.model.CustomTypeShape
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.LintIssue
import com.example.model.LintSeverity
import com.example.model.MenuItemEntry
import com.example.model.MenuMutatorState
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType
import com.example.model.parseHexColor
import com.example.model.snapAndFormatSliderValue
import com.example.ui.components.CodeHighlighter
import com.example.ui.components.ScratchBlockVisual
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CodeBg
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ForgeOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.StudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuilderScreen(
    uiState: StudioUiState,
    onExecutionModeChange: (ExecutionMode) -> Unit,
    onExtensionIdChange: (String) -> Unit,
    onExtensionNameChange: (String) -> Unit,
    onEnforceGuardChange: (Boolean) -> Unit,
    onBindVmChange: (Boolean) -> Unit,
    onApplyColorPreset: (com.example.model.BlockColorPreset) -> Unit,
    onSaveBlock: (ExtensionBlock) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onMoveBlock: (String, Int) -> Unit,
    onSaveMenu: (ExtensionMenu) -> Unit,
    onDeleteMenu: (String) -> Unit,
    onSaveCustomType: (CustomTypeDefinition) -> Unit,
    onDeleteCustomType: (String) -> Unit,
    onSaveCustomShape: (CustomShapeDefinition) -> Unit,
    onDeleteCustomShape: (String) -> Unit,
    onApplyLintFix: (LintIssue) -> Unit,
    onLoadTemplate: (ExtensionTemplate) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val project = uiState.project
    val color1 = parseHexColor(project.color1Hex, ForgeOrange)
    val color2 = parseHexColor(project.color2Hex, Color(0xFFDF7500))
    val color3 = parseHexColor(project.color3Hex, Color(0xFFB85D00))

    var editingBlock by remember { mutableStateOf<ExtensionBlock?>(null) }
    var editingMenu by remember { mutableStateOf<ExtensionMenu?>(null) }
    var editingCustomType by remember { mutableStateOf<CustomTypeDefinition?>(null) }
    var editingCustomShape by remember { mutableStateOf<CustomShapeDefinition?>(null) }
    var showTemplatesSheet by remember { mutableStateOf(false) }

    val templateLoadedMsg = stringResource(R.string.msg_template_loaded)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.studio_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = project.executionMode.timingBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (project.executionMode == ExecutionMode.UNSANDBOXED) {
                                ForgeOrange
                            } else {
                                CyberCyan
                            }
                        )
                    }
                    FilledTonalButton(
                        onClick = { showTemplatesSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("templates_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_templates))
                    }
                }
            }

            // Execution Mode Segmented Switcher
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("execution_mode_selector")
                ) {
                    ExecutionMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = project.executionMode == mode,
                            onClick = { onExecutionModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ExecutionMode.entries.size
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (mode == ExecutionMode.UNSANDBOXED) {
                                        Icons.Filled.Bolt
                                    } else {
                                        Icons.Filled.Security
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) {
                            Text(mode.displayName)
                        }
                    }
                }
            }

            // NitroBolt Safety Audit Issues
            if (uiState.lintIssues.isNotEmpty()) {
                item {
                    SafetyAuditCard(
                        issues = uiState.lintIssues,
                        onApplyFix = onApplyLintFix
                    )
                }
            }

            // Extension Metadata Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.section_extension_meta),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = project.name,
                                onValueChange = onExtensionNameChange,
                                label = { Text(stringResource(R.string.label_extension_name)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extension_name_input")
                            )
                            OutlinedTextField(
                                value = project.id,
                                onValueChange = onExtensionIdChange,
                                label = { Text(stringResource(R.string.label_extension_id)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extension_id_input")
                            )
                        }

                        Text(
                            text = stringResource(R.string.label_block_colors),
                            style = MaterialTheme.typography.titleSmall
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExtensionTemplates.colorPresets.forEach { preset ->
                                val selected = preset.color1Hex.equals(project.color1Hex, true)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(preset.primaryColor)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else preset.tertiaryColor,
                                            shape = CircleShape
                                        )
                                        .clickable { onApplyColorPreset(preset) }
                                        .testTag("color_preset_${preset.name.lowercase().replace(" ", "_")}")
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = preset.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = project.executionMode == ExecutionMode.UNSANDBOXED) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.label_unsandboxed_guard),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Throws if loaded in a sandboxed iframe",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = project.enforceUnsandboxedGuard,
                                        onCheckedChange = onEnforceGuardChange,
                                        modifier = Modifier.testTag("unsandboxed_guard_switch")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.label_bind_vm),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Scoped safely inside the IIFE",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = project.bindVmReference,
                                        onCheckedChange = onBindVmChange,
                                        modifier = Modifier.testTag("bind_vm_switch")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Blocks Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.section_blocks)} (${project.blocks.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = {
                            editingBlock = ExtensionBlock(
                                opcode = "customBlock${project.blocks.size + 1}",
                                blockType = ScratchBlockType.COMMAND,
                                text = "do something with [VALUE]",
                                arguments = listOf(
                                    BlockArgument(
                                        name = "VALUE",
                                        type = ScratchArgumentType.STRING,
                                        defaultValue = "hello"
                                    )
                                ),
                                jsBody = "// args.VALUE"
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_block_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_block))
                    }
                }
            }

            // Blocks List
            if (project.blocks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.empty_blocks_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(project.blocks, key = { _, b -> b.id }) { index, block ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingBlock = block }
                            .testTag("block_card_${block.opcode}")
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = block.blockType.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (block.blockType != ScratchBlockType.LABEL) {
                                        Text(
                                            text = "${block.opcode}()",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (block.isBranchBlock) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "branches:${block.effectiveBranchCount}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (block.dualType != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "dual:${block.dualType.label}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (block.outputType.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "→ ${block.outputType}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (block.blockShape.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "◇ ${block.blockShape.removePrefix("Scratch.BlockShape.")}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (block.duplicateOnDrag) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "⧉ duplicateOnDrag",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { onMoveBlock(block.id, -1) },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowUpward,
                                            contentDescription = "Move block up"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveBlock(block.id, 1) },
                                        enabled = index < project.blocks.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDownward,
                                            contentDescription = "Move block down"
                                        )
                                    }
                                    IconButton(onClick = { editingBlock = block }) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.action_edit)
                                        )
                                    }
                                    IconButton(onClick = { onDeleteBlock(block.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            ScratchBlockVisual(
                                block = block,
                                menus = project.menus,
                                color1 = color1,
                                color2 = color2,
                                color3 = color3,
                                customTypes = project.customTypes,
                                customShapes = project.customShapes,
                                allBlocks = project.blocks
                            )

                            if (block.switches.isNotEmpty()) {
                                BlockSwitchesSummaryRow(
                                    switches = block.switches,
                                    allBlocks = project.blocks,
                                    onSelectTarget = { targetOpcode ->
                                        project.blocks.find { it.opcode == targetOpcode }?.let { target ->
                                            editingBlock = target
                                        }
                                    }
                                )
                            }

                            if (block.jsBody.isNotBlank() && block.blockType != ScratchBlockType.LABEL) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = block.jsBody,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Menus Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.section_menus)} (${project.menus.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            editingMenu = ExtensionMenu(
                                name = "MENU_${project.menus.size + 1}",
                                acceptReporters = true,
                                items = listOf("option1", "option2")
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_menu))
                    }
                }
            }

            if (project.menus.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.empty_menus_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(project.menus, key = { _, m -> m.id }) { _, menu ->
                    MenuItemCard(
                        menu = menu,
                        onEdit = { editingMenu = menu },
                        onDelete = { onDeleteMenu(menu.id) }
                    )
                }
            }

            // Custom Types Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.section_custom_types)} (${project.customTypes.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            editingCustomType = CustomTypeDefinition(
                                typeName = "customType${project.customTypes.size + 1}",
                                className = "CustomType${project.customTypes.size + 1}"
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_type_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_custom_type))
                    }
                }
            }

            if (project.customTypes.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.empty_custom_types_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(project.customTypes, key = { _, ct -> ct.id }) { _, customType ->
                    CustomTypeItemCard(
                        customType = customType,
                        onEdit = { editingCustomType = customType },
                        onDelete = { onDeleteCustomType(customType.id) }
                    )
                }
            }

            // Custom Shapes Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Draw,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.section_custom_shapes)} (${project.customShapes.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            editingCustomShape = CustomShapeDefinition(
                                shapeName = "customShape${project.customShapes.size + 1}"
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_shape_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_custom_shape))
                    }
                }
            }

            if (project.customShapes.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.empty_custom_shapes_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(project.customShapes, key = { _, cs -> cs.id }) { _, customShape ->
                    CustomShapeItemCard(
                        customShape = customShape,
                        color1 = color1,
                        color2 = color2,
                        color3 = color3,
                        onEdit = { editingCustomShape = customShape },
                        onDelete = { onDeleteCustomShape(customShape.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Block Editor Sub-Screen Dialog
    editingBlock?.let { blockToEdit ->
        BlockEditorDialog(
            initialBlock = blockToEdit,
            allBlocks = project.blocks,
            menus = project.menus,
            customTypes = project.customTypes,
            customShapes = project.customShapes,
            color1 = color1,
            color2 = color2,
            color3 = color3,
            onDismiss = { editingBlock = null },
            onSave = { updated ->
                onSaveBlock(updated)
                editingBlock = null
            }
        )
    }

    // Menu Editor Dialog
    editingMenu?.let { menuToEdit ->
        val availableArgNames = remember(project.blocks) {
            project.blocks.flatMap { it.arguments.map { arg -> arg.name } }.distinct()
        }
        MenuEditorDialog(
            initialMenu = menuToEdit,
            availableArgNames = availableArgNames,
            onDismiss = { editingMenu = null },
            onSave = { updated ->
                onSaveMenu(updated)
                editingMenu = null
            }
        )
    }

    // Custom Type Editor Dialog
    editingCustomType?.let { customTypeToEdit ->
        CustomTypeEditorDialog(
            initialType = customTypeToEdit,
            customShapes = project.customShapes,
            onDismiss = { editingCustomType = null },
            onSave = { updated ->
                onSaveCustomType(updated)
                editingCustomType = null
            }
        )
    }

    // Custom Shape Editor Dialog
    editingCustomShape?.let { customShapeToEdit ->
        CustomShapeEditorDialog(
            initialShape = customShapeToEdit,
            color1 = color1,
            color2 = color2,
            color3 = color3,
            onDismiss = { editingCustomShape = null },
            onSave = { updated ->
                onSaveCustomShape(updated)
                editingCustomShape = null
            }
        )
    }

    // Template Picker Bottom Sheet
    if (showTemplatesSheet) {
        BackHandler { showTemplatesSheet = false }
        ModalBottomSheet(onDismissRequest = { showTemplatesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "NitroBolt Extension Templates",
                    style = MaterialTheme.typography.headlineSmall
                )
                ExtensionTemplates.allTemplates.forEach { template ->
                    Card(
                        onClick = {
                            onLoadTemplate(template)
                            showTemplatesSheet = false
                            onShowMessage(templateLoadedMsg)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("template_item_${template.project.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = template.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = template.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SafetyAuditCard(
    issues: List<LintIssue>,
    onApplyFix: (LintIssue) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CrimsonError.copy(alpha = 0.12f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonError.copy(alpha = 0.4f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = AmberWarn
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.section_safety_checks)} (${issues.size})",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            issues.forEach { issue ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (issue.severity == LintSeverity.ERROR) CrimsonError else AmberWarn
                        )
                        Text(
                            text = issue.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (issue.fixAction != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onApplyFix(issue) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_auto_fix))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuItemCard(
    menu: ExtensionMenu,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = menu.name,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (menu.isDependent) {
                            Text(
                                text = "dependent on [${menu.parentName}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            if (menu.defaultOptions.isNotEmpty()) {
                                Text(
                                    text = "• default: ${menu.defaultOptions.joinToString(", ") { it.text }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (menu.isMutator) {
                            Text(
                                text = "mutator (${menu.mutator.size} states)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = "• dynamic shape & inputs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (menu.acceptReporters) "acceptReporters: true" else "acceptReporters: false",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSafe
                            )
                            if (menu.acceptText) {
                                Text(
                                    text = "• acceptText: true",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (menu.isDependent) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    menu.optionMapping.forEach { (parentVal, opts) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$parentVal → ${opts.joinToString(", ") { if (it.isObjectForm) "${it.text} (${it.value})" else "'${it.value}'" }}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    menu.regularEntries.forEach { entry ->
                        if (entry.isSeparator) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "─── sep",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (entry.isObjectForm) "${entry.text} (${entry.value})" else "'${entry.value}'",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                if (menu.isMutator) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        menu.mutator.forEach { (stateKey, mutState) ->
                            val summaryParts = buildList {
                                if (mutState.output != null) add("output: ${mutState.output.name}")
                                if (mutState.previousStatement != null) add("prev: ${mutState.previousStatement}")
                                if (mutState.nextStatement != null) add("next: ${mutState.nextStatement}")
                                if (mutState.inputsInline != null) add("inline: ${mutState.inputsInline}")
                                if (mutState.arguments.isNotEmpty()) {
                                    add(mutState.arguments.joinToString(", ") { "${it.name}: ${it.type.name}" })
                                }
                            }.joinToString(" • ").ifBlank { "no changes" }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$stateKey → $summaryParts",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
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
private fun BlockSwitchesSummaryRow(
    switches: List<BlockSwitch>,
    allBlocks: List<ExtensionBlock>,
    onSelectTarget: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Context Switches (${switches.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            switches.forEach { sw ->
                val isKnown = allBlocks.any { it.opcode == sw.targetId }
                val details = buildList {
                    if (sw.inputs.isNotEmpty()) {
                        add(sw.inputs.joinToString(",") { "${it.from}→${it.to}" })
                    }
                    if (sw.splitInputs.isNotEmpty()) {
                        add(sw.splitInputs.joinToString(",") { "-$it" })
                    }
                    if (sw.rawId) add("rawId")
                }.joinToString(" • ")

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isKnown) { onSelectTarget(sw.targetId) }
                ) {
                    Text(
                        text = if (details.isBlank()) "⇄ ${sw.targetId}" else "⇄ ${sw.targetId} ($details)",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BlockEditorDialog(
    initialBlock: ExtensionBlock,
    allBlocks: List<ExtensionBlock>,
    menus: List<ExtensionMenu>,
    customTypes: List<CustomTypeDefinition>,
    customShapes: List<CustomShapeDefinition>,
    color1: Color,
    color2: Color,
    color3: Color,
    onDismiss: () -> Unit,
    onSave: (ExtensionBlock) -> Unit
) {
    BackHandler { onDismiss() }

    var opcode by remember { mutableStateOf(initialBlock.opcode) }
    var blockType by remember { mutableStateOf(initialBlock.blockType) }
    var dualType by remember { mutableStateOf(initialBlock.dualType) }
    var text by remember { mutableStateOf(initialBlock.text) }
    var branchCount by remember { mutableStateOf(initialBlock.branchCount) }
    val branchTexts = remember {
        mutableStateListOf<String>().apply { addAll(initialBlock.branchTexts) }
    }
    var outputType by remember { mutableStateOf(initialBlock.outputType) }
    var blockShape by remember { mutableStateOf(initialBlock.blockShape) }
    var duplicateOnDrag by remember { mutableStateOf(initialBlock.duplicateOnDrag) }
    var jsBody by remember { mutableStateOf(initialBlock.jsBody) }
    val arguments = remember {
        mutableStateListOf<BlockArgument>().apply { addAll(initialBlock.arguments) }
    }
    val switches = remember {
        mutableStateListOf<BlockSwitch>().apply { addAll(initialBlock.switches) }
    }
    var editingSwitch by remember { mutableStateOf<BlockSwitch?>(null) }
    var outputTypeExpanded by remember { mutableStateOf(false) }
    var blockShapeExpanded by remember { mutableStateOf(false) }

    val isBranchType = blockType == ScratchBlockType.CONDITIONAL || blockType == ScratchBlockType.LOOP
    val supportsDuplicateOnDrag = blockType == ScratchBlockType.REPORTER ||
        blockType == ScratchBlockType.BOOLEAN ||
        (blockType == ScratchBlockType.COMMAND &&
            (dualType == ScratchBlockType.REPORTER || dualType == ScratchBlockType.BOOLEAN))

    val previewBlock = ExtensionBlock(
        id = initialBlock.id,
        opcode = opcode.ifBlank { "op" },
        blockType = blockType,
        dualType = if (blockType == ScratchBlockType.COMMAND) dualType else null,
        text = text.ifBlank { opcode },
        branchCount = if (isBranchType) branchCount.coerceAtLeast(1) else 0,
        branchTexts = if (isBranchType) branchTexts.take(branchCount.coerceAtLeast(1)) else emptyList(),
        outputType = if (blockType == ScratchBlockType.REPORTER) outputType else "",
        blockShape = blockShape.trim(),
        duplicateOnDrag = duplicateOnDrag && supportsDuplicateOnDrag,
        arguments = arguments.toList(),
        switches = switches.toList(),
        jsBody = jsBody
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                    Text(
                        text = "Edit Scratch Block",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = { onSave(previewBlock) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_block_button")
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Preview Box
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            ScratchBlockVisual(
                                block = previewBlock,
                                menus = menus,
                                color1 = color1,
                                color2 = color2,
                                color3 = color3,
                                customTypes = customTypes,
                                customShapes = customShapes,
                                allBlocks = allBlocks
                            )
                        }
                    }

                    // Block Type Chips
                    Text("Block Type", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScratchBlockType.entries.forEach { type ->
                            FilterChip(
                                selected = blockType == type,
                                onClick = {
                                    blockType = type
                                    if (type == ScratchBlockType.CONDITIONAL || type == ScratchBlockType.LOOP) {
                                        if (branchCount < 1) branchCount = 1
                                    }
                                },
                                label = { Text(type.label) }
                            )
                        }
                    }

                    if (blockType == ScratchBlockType.COMMAND) {
                        Text("Dual Type (Optional — Command + Reporter/Boolean)", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = dualType == null,
                                onClick = { dualType = null },
                                label = { Text("None") }
                            )
                            listOf(
                                ScratchBlockType.REPORTER,
                                ScratchBlockType.BOOLEAN,
                                ScratchBlockType.OBJECT,
                                ScratchBlockType.ARRAY
                            ).forEach { dt ->
                                FilterChip(
                                    selected = dualType == dt,
                                    onClick = { dualType = dt },
                                    label = { Text(dt.label) }
                                )
                            }
                        }
                    }

                    if (blockType != ScratchBlockType.LABEL) {
                        OutlinedTextField(
                            value = opcode,
                            onValueChange = { opcode = it },
                            label = { Text("Opcode (JS method name)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (blockType == ScratchBlockType.REPORTER) {
                        ExposedDropdownMenuBox(
                            expanded = outputTypeExpanded,
                            onExpandedChange = { outputTypeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = outputType.ifBlank { "Primitive (Default)" },
                                onValueChange = { outputType = it },
                                label = { Text("Output Type (outputType)") },
                                supportingText = { Text("Assign a registered Scratch.types name (e.g., counter) to return custom objects") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outputTypeExpanded) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                                    .fillMaxWidth()
                                    .testTag("block_output_type_input")
                            )
                            ExposedDropdownMenu(
                                expanded = outputTypeExpanded,
                                onDismissRequest = { outputTypeExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Primitive (Default)") },
                                    onClick = {
                                        outputType = ""
                                        outputTypeExpanded = false
                                    }
                                )
                                customTypes.forEach { ct ->
                                    DropdownMenuItem(
                                        text = { Text("${ct.typeName} (${ct.className})") },
                                        onClick = {
                                            outputType = ct.typeName
                                            outputTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (blockType != ScratchBlockType.LABEL) {
                        ExposedDropdownMenuBox(
                            expanded = blockShapeExpanded,
                            onExpandedChange = { blockShapeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = blockShape.ifBlank { "Default Shape" },
                                onValueChange = { blockShape = if (it == "Default Shape") "" else it },
                                label = { Text("Block Shape (blockShape)") },
                                supportingText = { Text("Select a registered Scratch.BlockShapes name (e.g., nbArrow)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockShapeExpanded) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                                    .fillMaxWidth()
                                    .testTag("block_shape_input")
                            )
                            ExposedDropdownMenu(
                                expanded = blockShapeExpanded,
                                onDismissRequest = { blockShapeExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default Shape") },
                                    onClick = {
                                        blockShape = ""
                                        blockShapeExpanded = false
                                    }
                                )
                                customShapes.forEach { cs ->
                                    DropdownMenuItem(
                                        text = { Text("${cs.shapeName} (Custom Shape)") },
                                        onClick = {
                                            blockShape = cs.shapeName
                                            blockShapeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (supportsDuplicateOnDrag) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "duplicateOnDrag",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Duplicate this reporter/boolean when dragged out of an input (useful for loop variables or shadow fill-ins)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = duplicateOnDrag,
                                onCheckedChange = { duplicateOnDrag = it },
                                modifier = Modifier.testTag("block_duplicate_on_drag_toggle")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { newText ->
                            text = newText
                            // Automatically sync any [ARG] placeholders added to text
                            val tokens = Regex("\\[([A-Za-z0-9_]+)]")
                                .findAll(newText)
                                .map { it.groupValues[1] }
                                .distinct()
                                .toList()
                            tokens.forEach { tokenName ->
                                if (arguments.none { it.name == tokenName }) {
                                    arguments.add(
                                        BlockArgument(
                                            name = tokenName,
                                            type = ScratchArgumentType.STRING,
                                            defaultValue = "value"
                                        )
                                    )
                                }
                            }
                        },
                        label = {
                            Text(if (isBranchType) "Top Header Text (text[0], use [ARG])" else "Block Label (use [ARG] for inputs)")
                        },
                        supportingText = {
                            Text(
                                if (isBranchType) "Header above branch 1, e.g. hello or if [COND] then"
                                else "Example: set turbo mode to [ENABLED]"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isBranchType) {
                        val effectiveBranches = branchCount.coerceAtLeast(1)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Branches (branchCount: $effectiveBranches)",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (blockType == ScratchBlockType.LOOP) {
                                                "LOOP adds the loop icon and supports C-shaped mouths"
                                            } else {
                                                "CONDITIONAL creates C-shaped mouths (set 2 for if/else)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilledTonalButton(
                                            onClick = {
                                                if (effectiveBranches > 1) {
                                                    branchCount = effectiveBranches - 1
                                                }
                                            },
                                            enabled = effectiveBranches > 1,
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("block_branch_count_dec")
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Decrease branchCount", modifier = Modifier.size(16.dp))
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                if (effectiveBranches < 5) {
                                                    branchCount = effectiveBranches + 1
                                                }
                                            },
                                            enabled = effectiveBranches < 5,
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("block_branch_count_inc")
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Increase branchCount", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                for (bIdx in 0 until effectiveBranches) {
                                    val currentBranchText = branchTexts.getOrNull(bIdx).orEmpty()
                                    val labelText = if (bIdx < effectiveBranches - 1) {
                                        "Divider #${bIdx + 1} Text (text[${bIdx + 1}], e.g. else)"
                                    } else {
                                        "Bottom Bar Text (text[${bIdx + 1}], optional e.g. neighbor)"
                                    }
                                    OutlinedTextField(
                                        value = currentBranchText,
                                        onValueChange = { updated ->
                                            while (branchTexts.size <= bIdx) {
                                                branchTexts.add("")
                                            }
                                            branchTexts[bIdx] = updated
                                            val extraTokens = Regex("\\[([A-Za-z0-9_]+)]")
                                                .findAll(updated)
                                                .map { it.groupValues[1] }
                                                .distinct()
                                                .toList()
                                            extraTokens.forEach { tokenName ->
                                                if (arguments.none { it.name == tokenName }) {
                                                    arguments.add(
                                                        BlockArgument(
                                                            name = tokenName,
                                                            type = ScratchArgumentType.STRING,
                                                            defaultValue = "value"
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        label = { Text(labelText) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("block_branch_text_$bIdx")
                                    )
                                }
                            }
                        }
                    }

                    if (blockType != ScratchBlockType.LABEL) {
                        // Arguments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Arguments (${arguments.size})", style = MaterialTheme.typography.titleSmall)
                            TextButton(
                                onClick = {
                                    val nextName = "ARG${arguments.size + 1}"
                                    arguments.add(
                                        BlockArgument(
                                            name = nextName,
                                            type = ScratchArgumentType.STRING,
                                            defaultValue = "text"
                                        )
                                    )
                                    if (!text.contains("[$nextName]")) {
                                        text = "$text [$nextName]".trim()
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Arg")
                            }
                        }

                        val reporterOpcodes = allBlocks
                            .filter {
                                it.id != initialBlock.id && (
                                    it.blockType == ScratchBlockType.REPORTER ||
                                        it.blockType == ScratchBlockType.BOOLEAN ||
                                        it.blockType == ScratchBlockType.OBJECT ||
                                        it.blockType == ScratchBlockType.ARRAY ||
                                        it.dualType != null
                                    )
                            }
                            .map { it.opcode }

                        arguments.forEachIndexed { idx, arg ->
                            ArgumentEditorRow(
                                argument = arg,
                                menus = menus,
                                customTypes = customTypes,
                                reporterOpcodes = reporterOpcodes,
                                onUpdate = { arguments[idx] = it },
                                onDelete = { arguments.removeAt(idx) }
                            )
                        }

                        // JS Body
                        Text("JavaScript Body (inside method)", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssistChip(
                                onClick = { jsBody = "vm.setTurboMode(args.ENABLED === 'on');" },
                                label = { Text("vm.setTurboMode") }
                            )
                            AssistChip(
                                onClick = { jsBody = "return 'World!';" },
                                label = { Text("return 'World!'") }
                            )
                            AssistChip(
                                onClick = { jsBody = "return Boolean(vm.runtime.turboMode);" },
                                label = { Text("return turboMode") }
                            )
                            AssistChip(
                                onClick = { jsBody = "return { [args.KEY]: args.VALUE };" },
                                label = { Text("return { [KEY]: VALUE }") }
                            )
                            AssistChip(
                                onClick = { jsBody = "return [args.ITEM];" },
                                label = { Text("return [ITEM]") }
                            )
                            AssistChip(
                                onClick = {
                                    jsBody = "const values = util.extendableToArray(args, 'INPUTS', 'ABC');\nreturn values.join('');"
                                },
                                label = { Text("util.extendableToArray") }
                            )
                            AssistChip(
                                onClick = {
                                    val loopFlag = blockType == ScratchBlockType.LOOP
                                    jsBody = "util.startBranch(1, $loopFlag);"
                                },
                                label = { Text("util.startBranch") }
                            )
                        }

                        OutlinedTextField(
                            value = jsBody,
                            onValueChange = { jsBody = it },
                            label = { Text("Method Implementation") },
                            minLines = 3,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Context Switches Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${stringResource(R.string.section_switches)} (${switches.size})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            TextButton(
                                onClick = {
                                    val defaultTarget = allBlocks
                                        .firstOrNull { it.id != initialBlock.id && it.blockType != ScratchBlockType.LABEL }
                                        ?.opcode ?: ""
                                    editingSwitch = BlockSwitch(targetId = defaultTarget)
                                },
                                modifier = Modifier.testTag("add_switch_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_add_switch))
                            }
                        }

                        val siblingTargets = allBlocks.filter {
                            it.id != initialBlock.id &&
                                it.blockType != ScratchBlockType.LABEL &&
                                switches.none { sw -> sw.targetId == it.opcode }
                        }
                        if (siblingTargets.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                siblingTargets.forEach { sibling ->
                                    AssistChip(
                                        onClick = {
                                            switches.add(BlockSwitch(targetId = sibling.opcode))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.SwapHoriz,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text("Quick link: ${sibling.opcode}") }
                                    )
                                }
                            }
                        }

                        if (switches.isEmpty()) {
                            Text(
                                text = "Add switches so right-clicking this block offers quick swaps (string form or object form with inputs / splitInputs / rawId).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            switches.forEachIndexed { swIdx, sw ->
                                SwitchItemRow(
                                    blockSwitch = sw,
                                    onEdit = { editingSwitch = sw },
                                    onDelete = { switches.removeAt(swIdx) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    editingSwitch?.let { swToEdit ->
        SwitchEditorDialog(
            initialSwitch = swToEdit,
            currentBlockArgs = arguments.map { it.name },
            siblingBlocks = allBlocks.filter {
                it.id != initialBlock.id && it.blockType != ScratchBlockType.LABEL
            },
            onDismiss = { editingSwitch = null },
            onSave = { updated ->
                val idx = switches.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    switches[idx] = updated
                } else {
                    switches.add(updated)
                }
                editingSwitch = null
            }
        )
    }
}

@Composable
private fun SwitchItemRow(
    blockSwitch: BlockSwitch,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = blockSwitch.targetId,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (blockSwitch.isSimpleStringForm) "STRING FORM" else "OBJECT FORM",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!blockSwitch.isSimpleStringForm) {
                    val parts = buildList {
                        if (blockSwitch.inputs.isNotEmpty()) {
                            add("inputs: " + blockSwitch.inputs.joinToString { "['${it.from}','${it.to}']" })
                        }
                        if (blockSwitch.splitInputs.isNotEmpty()) {
                            add("splitInputs: " + blockSwitch.splitInputs.joinToString { "'$it'" })
                        }
                        if (blockSwitch.rawId) add("rawId: true")
                    }
                    Text(
                        text = parts.joinToString(" • "),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit switch")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete switch",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SwitchEditorDialog(
    initialSwitch: BlockSwitch,
    currentBlockArgs: List<String>,
    siblingBlocks: List<ExtensionBlock>,
    onDismiss: () -> Unit,
    onSave: (BlockSwitch) -> Unit
) {
    BackHandler { onDismiss() }

    var targetId by remember { mutableStateOf(initialSwitch.targetId) }
    var rawId by remember { mutableStateOf(initialSwitch.rawId) }
    val inputs = remember {
        mutableStateListOf<BlockSwitchInputMapping>().apply { addAll(initialSwitch.inputs) }
    }
    val splitInputs = remember {
        mutableStateListOf<String>().apply { addAll(initialSwitch.splitInputs) }
    }
    var customSplitInput by remember { mutableStateOf("") }
    var targetExpanded by remember { mutableStateOf(false) }

    val targetBlockArgs = siblingBlocks.find { it.opcode == targetId }?.arguments?.map { it.name }.orEmpty()

    val previewSwitch = BlockSwitch(
        id = initialSwitch.id,
        targetId = targetId.trim(),
        inputs = inputs.filter { it.from.isNotBlank() && it.to.isNotBlank() },
        splitInputs = splitInputs.filter { it.isNotBlank() },
        rawId = rawId
    )

    val previewJs = remember(previewSwitch) {
        if (previewSwitch.isSimpleStringForm) {
            "'${previewSwitch.targetId}'"
        } else {
            buildString {
                appendLine("{")
                append("  id: '${previewSwitch.targetId}'")
                if (previewSwitch.inputs.isNotEmpty()) {
                    appendLine(",")
                    val pairs = previewSwitch.inputs.joinToString(", ") { "['${it.from}', '${it.to}']" }
                    append("  inputs: [$pairs]")
                }
                if (previewSwitch.splitInputs.isNotEmpty()) {
                    appendLine(",")
                    val dropped = previewSwitch.splitInputs.joinToString(", ") { "'$it'" }
                    append("  splitInputs: [$dropped]")
                }
                if (previewSwitch.rawId) {
                    appendLine(",")
                    append("  rawId: true")
                }
                appendLine()
                append("}")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                    Text("Configure Block Switch", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { onSave(previewSwitch) },
                        enabled = targetId.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_switch_button")
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Target Opcode Picker + Custom Text
                    ExposedDropdownMenuBox(
                        expanded = targetExpanded,
                        onExpandedChange = { targetExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = targetId,
                            onValueChange = { targetId = it },
                            label = { Text("Target Opcode (id)") },
                            supportingText = { Text("Pick a block from this extension or type any opcode (e.g., motion_changexby)") },
                            trailingIcon = {
                                if (siblingBlocks.isNotEmpty()) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded)
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                                .testTag("switch_target_input")
                        )
                        if (siblingBlocks.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false }
                            ) {
                                siblingBlocks.forEach { sb ->
                                    DropdownMenuItem(
                                        text = { Text("${sb.opcode} — ${sb.text}") },
                                        onClick = {
                                            targetId = sb.opcode
                                            targetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // rawId switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("rawId", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Use opcode as-is instead of prepending extension ID (for built-in or external blocks)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rawId,
                            onCheckedChange = { rawId = it },
                            modifier = Modifier.testTag("switch_raw_id_toggle")
                        )
                    }

                    // Input Remappings (inputs: [[from, to]])
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Remap Inputs (inputs)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Rename inputs from current block to match target block",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                val fromDefault = currentBlockArgs.firstOrNull() ?: "X"
                                val toDefault = targetBlockArgs.firstOrNull() ?: "Y"
                                inputs.add(BlockSwitchInputMapping(fromDefault, toDefault))
                            },
                            modifier = Modifier.testTag("add_input_remap_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Pair")
                        }
                    }

                    inputs.forEachIndexed { idx, mapping ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mapping.from,
                                onValueChange = { inputs[idx] = mapping.copy(from = it.uppercase()) },
                                label = { Text("From") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                modifier = Modifier.weight(1f)
                            )
                            Text("→", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = mapping.to,
                                onValueChange = { inputs[idx] = mapping.copy(to = it.uppercase()) },
                                label = { Text("To") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { inputs.removeAt(idx) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Remove remapping",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Drop Inputs (splitInputs)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Drop Inputs (splitInputs)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Discard inputs on this block that do not exist on the target block",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentBlockArgs.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                currentBlockArgs.forEach { argName ->
                                    val selected = argName in splitInputs
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            if (selected) splitInputs.remove(argName)
                                            else splitInputs.add(argName)
                                        },
                                        label = { Text(argName, fontFamily = JetBrainsMonoFamily) }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customSplitInput,
                                onValueChange = { customSplitInput = it.uppercase() },
                                label = { Text("Custom input to drop (e.g., SECS)") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = {
                                    val clean = customSplitInput.trim()
                                    if (clean.isNotEmpty() && clean !in splitInputs) {
                                        splitInputs.add(clean)
                                        customSplitInput = ""
                                    }
                                }
                            ) {
                                Text("Drop")
                            }
                        }
                        if (splitInputs.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                splitInputs.forEach { name ->
                                    AssistChip(
                                        onClick = { splitInputs.remove(name) },
                                        label = { Text("✕ $name", fontFamily = JetBrainsMonoFamily) }
                                    )
                                }
                            }
                        }
                    }

                    // Live Generated Switch Entry Preview
                    Text("Generated switches[] Entry", style = MaterialTheme.typography.titleSmall)
                    Surface(
                        color = CodeBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = CodeHighlighter.highlightJs(previewJs),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ArgumentEditorRow(
    argument: BlockArgument,
    menus: List<ExtensionMenu>,
    customTypes: List<CustomTypeDefinition>,
    reporterOpcodes: List<String>,
    onUpdate: (BlockArgument) -> Unit,
    onDelete: () -> Unit,
    nestingDepth: Int = 0
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var shadowExpanded by remember { mutableStateOf(false) }

    val isPlainString = argument.type == ScratchArgumentType.STRING && argument.customTypeName.isBlank()
    val isSlider = argument.type == ScratchArgumentType.SLIDER && argument.customTypeName.isBlank()
    val isObjectOrArray = (argument.type == ScratchArgumentType.OBJECT || argument.type == ScratchArgumentType.ARRAY) &&
        argument.customTypeName.isBlank()
    val isExtendable = argument.type == ScratchArgumentType.EXTENDABLE && argument.customTypeName.isBlank()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (nestingDepth > 0) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (nestingDepth > 0) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "[${argument.name}]",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isExtendable) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "extendable • ${argument.extendableArguments.size} args",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (argument.extendableAcceptArray) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "acceptArray",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (isPlainString && argument.canMultiline) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "multiline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isPlainString && !argument.acceptReporters && argument.menuName.isBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "no reporters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isSlider) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val minLabel = argument.sliderMin.ifBlank { "0" }
                            val maxLabel = argument.sliderMax.ifBlank { "100" }
                            val precLabel = argument.sliderPrecision.ifBlank { "1" }
                            Text(
                                text = "$minLabel..$maxLabel • step $precLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isObjectOrArray) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "reporter-only slot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!isExtendable && argument.shadow.isNotBlank() && argument.acceptReporters && argument.menuName.isBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "shadow: ${argument.shadow}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove argument",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = argument.name,
                    onValueChange = { onUpdate(argument.copy(name = it.uppercase())) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (!isObjectOrArray && !isExtendable) {
                    OutlinedTextField(
                        value = argument.defaultValue,
                        onValueChange = { onUpdate(argument.copy(defaultValue = it)) },
                        label = { Text(if (isPlainString && argument.canMultiline) "Default (newlines preserved)" else "Default") },
                        singleLine = !(isPlainString && argument.canMultiline),
                        minLines = if (isPlainString && argument.canMultiline) 2 else 1,
                        maxLines = if (isPlainString && argument.canMultiline) 5 else 1,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_default_${argument.name}")
                    )
                }
            }

            if (isObjectOrArray) {
                Text(
                    text = "Only accepts ${argument.type.label.lowercase()}-shaped reporter blocks (no literal defaultValue emitted).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (argument.defaultValue.isNotBlank()) {
                    TextButton(
                        onClick = { onUpdate(argument.copy(defaultValue = "")) }
                    ) {
                        Text("Clear unused defaultValue ('${argument.defaultValue}')")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (argument.customTypeName.isNotBlank()) {
                            "Custom: ${argument.customTypeName}"
                        } else {
                            argument.type.label
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        customTypes.forEach { ct ->
                            DropdownMenuItem(
                                text = { Text("Custom: ${ct.typeName} (${ct.className})") },
                                onClick = {
                                    onUpdate(argument.copy(customTypeName = ct.typeName))
                                    typeExpanded = false
                                }
                            )
                        }
                        val availableTypes = if (nestingDepth >= 2) {
                            ScratchArgumentType.entries.filter { it != ScratchArgumentType.EXTENDABLE }
                        } else {
                            ScratchArgumentType.entries
                        }
                        availableTypes.forEach { argType ->
                            DropdownMenuItem(
                                text = { Text(argType.label) },
                                onClick = {
                                    val updatedDefault = when {
                                        argType == ScratchArgumentType.OBJECT ||
                                            argType == ScratchArgumentType.ARRAY ||
                                            argType == ScratchArgumentType.EXTENDABLE -> ""
                                        argType == ScratchArgumentType.SLIDER && argument.defaultValue.toDoubleOrNull() == null ->
                                            argType.sampleDefault
                                        else -> argument.defaultValue
                                    }
                                    val updatedMenu = if (argType == ScratchArgumentType.OBJECT ||
                                        argType == ScratchArgumentType.ARRAY ||
                                        argType == ScratchArgumentType.EXTENDABLE
                                    ) {
                                        ""
                                    } else {
                                        argument.menuName
                                    }
                                    val updatedChildren = if (argType == ScratchArgumentType.EXTENDABLE && argument.extendableArguments.isEmpty()) {
                                        listOf(
                                            BlockArgument(
                                                name = "ITEM",
                                                type = ScratchArgumentType.STRING,
                                                defaultValue = "value"
                                            )
                                        )
                                    } else {
                                        argument.extendableArguments
                                    }
                                    onUpdate(
                                        argument.copy(
                                            type = argType,
                                            customTypeName = "",
                                            defaultValue = updatedDefault,
                                            menuName = updatedMenu,
                                            shadow = if (argType == ScratchArgumentType.EXTENDABLE) "" else argument.shadow,
                                            extendableArguments = updatedChildren
                                        )
                                    )
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (!isExtendable && !isObjectOrArray) {
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = argument.menuName.ifBlank { "None" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Menu") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    onUpdate(argument.copy(menuName = ""))
                                    menuExpanded = false
                                }
                            )
                            menus.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name) },
                                    onClick = {
                                        onUpdate(argument.copy(menuName = m.name))
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isExtendable) {
                OutlinedTextField(
                    value = argument.extendableText,
                    onValueChange = { newExtText ->
                        val childTokens = Regex("\\[([A-Za-z0-9_]+)]")
                            .findAll(newExtText)
                            .map { it.groupValues[1] }
                            .distinct()
                            .toList()
                        val updatedChildren = argument.extendableArguments.toMutableList()
                        childTokens.forEach { tokenName ->
                            if (updatedChildren.none { it.name == tokenName }) {
                                updatedChildren.add(
                                    BlockArgument(
                                        name = tokenName,
                                        type = ScratchArgumentType.STRING,
                                        defaultValue = "value"
                                    )
                                )
                            }
                        }
                        onUpdate(
                            argument.copy(
                                extendableText = newExtText,
                                extendableArguments = updatedChildren
                            )
                        )
                    },
                    label = { Text("Extendable Slot Text (use [CHILD])") },
                    supportingText = { Text("Repeated for each slot, e.g. [ABC] or layer 1 [HEY] [NUMBER]") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("arg_extendable_text_${argument.name}")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = argument.extendableDefaultInputs,
                        onValueChange = { onUpdate(argument.copy(extendableDefaultInputs = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Default") },
                        placeholder = { Text("2") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_extendable_default_${argument.name}")
                    )
                    OutlinedTextField(
                        value = argument.extendableMinInputs,
                        onValueChange = { onUpdate(argument.copy(extendableMinInputs = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Min") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_extendable_min_${argument.name}")
                    )
                    OutlinedTextField(
                        value = argument.extendableMaxInputs,
                        onValueChange = { onUpdate(argument.copy(extendableMaxInputs = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Max") },
                        placeholder = { Text("∞") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_extendable_max_${argument.name}")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = argument.extendableSeparator,
                        onValueChange = { onUpdate(argument.copy(extendableSeparator = it)) },
                        label = { Text("Separator (optional)") },
                        placeholder = { Text("| or , ") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_extendable_separator_${argument.name}")
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "acceptArray",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Switch(
                            checked = argument.extendableAcceptArray,
                            onCheckedChange = { onUpdate(argument.copy(extendableAcceptArray = it)) },
                            modifier = Modifier.testTag("arg_extendable_accept_array_${argument.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nested Arguments (${argument.extendableArguments.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(
                        onClick = {
                            val nextChildName = "SUB${argument.extendableArguments.size + 1}"
                            val nextChildren = argument.extendableArguments + BlockArgument(
                                name = nextChildName,
                                type = ScratchArgumentType.STRING,
                                defaultValue = "value"
                            )
                            val nextExtText = if (argument.extendableText.contains("[$nextChildName]")) {
                                argument.extendableText
                            } else {
                                "${argument.extendableText} [$nextChildName]".trim()
                            }
                            onUpdate(
                                argument.copy(
                                    extendableText = nextExtText,
                                    extendableArguments = nextChildren
                                )
                            )
                        },
                        modifier = Modifier.testTag("arg_extendable_add_child_${argument.name}")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Nested Arg")
                    }
                }

                argument.extendableArguments.forEachIndexed { childIdx, childArg ->
                    ArgumentEditorRow(
                        argument = childArg,
                        menus = menus,
                        customTypes = customTypes,
                        reporterOpcodes = reporterOpcodes,
                        onUpdate = { updatedChild ->
                            val nextList = argument.extendableArguments.toMutableList()
                            nextList[childIdx] = updatedChild
                            onUpdate(argument.copy(extendableArguments = nextList))
                        },
                        onDelete = {
                            val nextList = argument.extendableArguments.toMutableList()
                            nextList.removeAt(childIdx)
                            onUpdate(argument.copy(extendableArguments = nextList))
                        },
                        nestingDepth = nestingDepth + 1
                    )
                }
            }

            if (isPlainString && argument.menuName.isBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "canMultiline",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Grow input & block vertically to show every line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = argument.canMultiline,
                        onCheckedChange = { onUpdate(argument.copy(canMultiline = it)) },
                        modifier = Modifier.testTag("arg_can_multiline_${argument.name}")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "acceptReporters",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Set false for a fixed text field that blocks reporters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = argument.acceptReporters,
                        onCheckedChange = { onUpdate(argument.copy(acceptReporters = it)) },
                        modifier = Modifier.testTag("arg_accept_reporters_${argument.name}")
                    )
                }
            }

            if (!isExtendable && argument.menuName.isBlank() && argument.acceptReporters) {
                ExposedDropdownMenuBox(
                    expanded = shadowExpanded,
                    onExpandedChange = { shadowExpanded = it }
                ) {
                    OutlinedTextField(
                        value = argument.shadow.ifBlank { "Normal Input Shadow" },
                        onValueChange = { onUpdate(argument.copy(shadow = if (it == "Normal Input Shadow") "" else it)) },
                        label = { Text("Shadow Reporter (shadow)") },
                        supportingText = { Text("Optional reporter/boolean opcode filling this input by default") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shadowExpanded) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                            .testTag("arg_shadow_${argument.name}")
                    )
                    ExposedDropdownMenu(
                        expanded = shadowExpanded,
                        onDismissRequest = { shadowExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Normal Input Shadow") },
                            onClick = {
                                onUpdate(argument.copy(shadow = ""))
                                shadowExpanded = false
                            }
                        )
                        reporterOpcodes.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op) },
                                onClick = {
                                    onUpdate(argument.copy(shadow = op))
                                    shadowExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isSlider) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = argument.sliderMin,
                        onValueChange = { onUpdate(argument.copy(sliderMin = it)) },
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_slider_min_${argument.name}")
                    )
                    OutlinedTextField(
                        value = argument.sliderMax,
                        onValueChange = { onUpdate(argument.copy(sliderMax = it)) },
                        label = { Text("Max") },
                        placeholder = { Text("100") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_slider_max_${argument.name}")
                    )
                    OutlinedTextField(
                        value = argument.sliderPrecision,
                        onValueChange = { onUpdate(argument.copy(sliderPrecision = it)) },
                        label = { Text("Precision") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arg_slider_precision_${argument.name}")
                    )
                }

                val minVal = argument.sliderMin.ifBlank { "0" }.toDoubleOrNull()
                val maxVal = argument.sliderMax.ifBlank { "100" }.toDoubleOrNull()
                val precVal = argument.sliderPrecision.ifBlank { "1" }.toDoubleOrNull()
                if (minVal != null && maxVal != null && precVal != null && minVal < maxVal && precVal > 0.0) {
                    val currentSliderFloat = (argument.defaultValue.toDoubleOrNull() ?: minVal)
                        .coerceIn(minVal, maxVal)
                        .toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Slider Preview (${argument.defaultValue.ifBlank { "0" }})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = currentSliderFloat,
                            onValueChange = { raw ->
                                val formatted = snapAndFormatSliderValue(
                                    raw = raw,
                                    min = minVal,
                                    max = maxVal,
                                    precision = precVal,
                                    precisionText = argument.sliderPrecision.ifBlank { "1" },
                                    minText = argument.sliderMin.ifBlank { "0" }
                                )
                                onUpdate(argument.copy(defaultValue = formatted))
                            },
                            valueRange = minVal.toFloat()..maxVal.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("arg_slider_preview_${argument.name}")
                        )
                    }
                } else {
                    Text(
                        text = "Enter valid numeric min < max and precision > 0 to preview slider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private data class EditableMutatorState(
    val key: String,
    val output: ScratchBlockType?,
    val previousStatement: Boolean?,
    val nextStatement: Boolean?,
    val inputsInline: Boolean?,
    val argsText: String
)

private fun formatMutatorArgsText(args: List<BlockArgument>): String {
    return args.joinToString("\n") { arg ->
        if (arg.defaultValue.isNotEmpty()) {
            "${arg.name}: ${arg.type.name} = ${arg.defaultValue}"
        } else {
            "${arg.name}: ${arg.type.name}"
        }
    }
}

private fun parseMutatorArgsText(raw: String): List<BlockArgument> {
    return raw.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val colonIdx = line.indexOf(':')
            if (colonIdx <= 0) return@mapNotNull null
            val argName = line.substring(0, colonIdx).trim().replace(" ", "_")
            if (argName.isEmpty()) return@mapNotNull null
            val rest = line.substring(colonIdx + 1).trim()
            val eqIdx = rest.indexOf('=')
            val typeToken = (if (eqIdx >= 0) rest.substring(0, eqIdx) else rest).trim().uppercase()
            val defaultVal = if (eqIdx >= 0) rest.substring(eqIdx + 1).trim() else ""
            val argType = ScratchArgumentType.entries.find {
                it.name == typeToken || it.label.equals(typeToken, ignoreCase = true)
            } ?: ScratchArgumentType.STRING
            BlockArgument(
                name = argName,
                type = argType,
                defaultValue = defaultVal
            )
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuEditorDialog(
    initialMenu: ExtensionMenu,
    availableArgNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (ExtensionMenu) -> Unit
) {
    BackHandler { onDismiss() }

    var name by remember { mutableStateOf(initialMenu.name) }
    var isDependent by remember { mutableStateOf(initialMenu.isDependent) }
    var enableMutator by remember { mutableStateOf(initialMenu.isMutator) }
    var acceptReporters by remember { mutableStateOf(initialMenu.acceptReporters) }
    var acceptText by remember { mutableStateOf(initialMenu.acceptText) }
    var itemsCsv by remember {
        mutableStateOf(
            initialMenu.regularEntries.joinToString(", ") { entry ->
                if (entry.isObjectForm) "${entry.text}=${entry.value}" else entry.value
            }
        )
    }
    var parentName by remember {
        mutableStateOf(
            initialMenu.parentName.ifBlank { availableArgNames.firstOrNull().orEmpty() }
        )
    }
    var optionMappingText by remember {
        mutableStateOf(
            if (initialMenu.optionMapping.isNotEmpty()) {
                initialMenu.optionMapping.entries.joinToString("\n") { (k, opts) ->
                    val optsStr = opts.joinToString(", ") {
                        if (it.isObjectForm) "${it.text}=${it.value}" else it.value
                    }
                    "$k: $optsStr"
                }
            } else {
                "cat: Siamese, Tabby\ndog: Beagle, Poodle"
            }
        )
    }
    var defaultOptionsCsv by remember {
        mutableStateOf(
            initialMenu.defaultOptions.joinToString(", ") {
                if (it.isObjectForm) "${it.text}=${it.value}" else it.value
            }.ifBlank { "unknown" }
        )
    }
    val mutatorStates = remember {
        mutableStateListOf<EditableMutatorState>().apply {
            initialMenu.mutator.forEach { (k, st) ->
                add(
                    EditableMutatorState(
                        key = k,
                        output = st.output,
                        previousStatement = st.previousStatement,
                        nextStatement = st.nextStatement,
                        inputsInline = st.inputsInline,
                        argsText = formatMutatorArgsText(st.arguments)
                    )
                )
            }
        }
    }

    val parsedRegularEntries = remember(itemsCsv) {
        itemsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { token ->
                val eqIdx = token.indexOf('=')
                if (eqIdx > 0 && token != ExtensionMenu.SEPARATOR_ITEM) {
                    MenuItemEntry(
                        value = token.substring(eqIdx + 1).trim(),
                        text = token.substring(0, eqIdx).trim()
                    )
                } else {
                    MenuItemEntry(value = token, text = token)
                }
            }
    }
    val hasSelectableItem = parsedRegularEntries.any { !it.isSeparator }

    val parsedMapping = remember(optionMappingText) {
        val map = linkedMapOf<String, List<MenuItemEntry>>()
        optionMappingText.lines().forEach { line ->
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim()
                val rawVals = line.substring(colonIdx + 1)
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (key.isNotEmpty() && rawVals.isNotEmpty()) {
                    map[key] = rawVals.map { token ->
                        val eqIdx = token.indexOf('=')
                        if (eqIdx > 0 && token != ExtensionMenu.SEPARATOR_ITEM) {
                            MenuItemEntry(
                                value = token.substring(eqIdx + 1).trim(),
                                text = token.substring(0, eqIdx).trim()
                            )
                        } else {
                            MenuItemEntry(value = token, text = token)
                        }
                    }
                }
            }
        }
        map
    }
    val parsedDefaultOptions = remember(defaultOptionsCsv) {
        defaultOptionsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { token ->
                val eqIdx = token.indexOf('=')
                if (eqIdx > 0 && token != ExtensionMenu.SEPARATOR_ITEM) {
                    MenuItemEntry(
                        value = token.substring(eqIdx + 1).trim(),
                        text = token.substring(0, eqIdx).trim()
                    )
                } else {
                    MenuItemEntry(value = token, text = token)
                }
            }
    }
    val hasDependentOption = parsedMapping.values.any { l -> l.any { !it.isSeparator } } ||
        parsedDefaultOptions.any { !it.isSeparator }
    val hasValidMutatorStates = !enableMutator || mutatorStates.any { it.key.isNotBlank() }
    val canSave = if (isDependent) {
        parentName.isNotBlank() && hasDependentOption
    } else {
        (parsedRegularEntries.isEmpty() || hasSelectableItem) && hasValidMutatorStates
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Menu", style = MaterialTheme.typography.titleLarge)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isDependent,
                        onClick = { isDependent = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Regular Menu")
                    }
                    SegmentedButton(
                        selected = isDependent,
                        onClick = { isDependent = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("menu_mode_dependent_button")
                    ) {
                        Text("Dependent")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace(" ", "_") },
                    label = { Text("Menu Key (e.g., MODE_MENU or colors)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_name_input")
                )

                if (isDependent) {
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it.replace(" ", "_") },
                        label = { Text("parentName (parent argument)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_parent_name_input")
                    )

                    if (availableArgNames.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableArgNames.forEach { argName ->
                                FilterChip(
                                    selected = parentName == argName,
                                    onClick = { parentName = argName },
                                    label = { Text(argName, fontFamily = JetBrainsMonoFamily) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = optionMappingText,
                        onValueChange = { optionMappingText = it },
                        label = { Text("optionMapping (parentKey: opt1, opt2)") },
                        supportingText = { Text("One parent value per line. Use Label=value for {text, value} items.") },
                        minLines = 3,
                        maxLines = 7,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_option_mapping_input")
                    )

                    OutlinedTextField(
                        value = defaultOptionsCsv,
                        onValueChange = { defaultOptionsCsv = it },
                        label = { Text("defaultOptions (optional fallback)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_default_options_input")
                    )

                    Text(
                        text = "Parent argument must appear before the dependent argument in block text. Dependent dropdowns do not support acceptText, acceptReporters, or mutator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = itemsCsv,
                        onValueChange = { itemsCsv = it },
                        label = { Text("Items (comma-separated)") },
                        supportingText = {
                            Text(
                                if (!hasSelectableItem && parsedRegularEntries.isNotEmpty()) {
                                    "Menu must contain at least one selectable item besides '---'"
                                } else {
                                    "Example: split=SPLIT, join=JOIN or red, ---, blue"
                                }
                            )
                        },
                        isError = !hasSelectableItem && parsedRegularEntries.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_items_input")
                    )

                    AssistChip(
                        onClick = {
                            val trimmed = itemsCsv.trimEnd()
                            itemsCsv = when {
                                trimmed.isEmpty() -> ExtensionMenu.SEPARATOR_ITEM
                                trimmed.endsWith(",") -> "$trimmed ${ExtensionMenu.SEPARATOR_ITEM}, "
                                else -> "$trimmed, ${ExtensionMenu.SEPARATOR_ITEM}, "
                            }
                        },
                        label = { Text("+ Insert Separator (---)") },
                        modifier = Modifier.testTag("menu_insert_separator_chip")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mutator Dropdown (mutator)", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Morph block output shape & inputs when menu option changes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableMutator,
                            onCheckedChange = { checked ->
                                enableMutator = checked
                                if (checked) {
                                    acceptReporters = false
                                    acceptText = false
                                    if (mutatorStates.isEmpty()) {
                                        val firstTwo = parsedRegularEntries
                                            .filter { !it.isSeparator }
                                            .take(2)
                                        if (firstTwo.isEmpty()) {
                                            mutatorStates.add(
                                                EditableMutatorState(
                                                    key = "STATE_1",
                                                    output = ScratchBlockType.REPORTER,
                                                    previousStatement = null,
                                                    nextStatement = null,
                                                    inputsInline = null,
                                                    argsText = "INPUT: STRING = value"
                                                )
                                            )
                                        } else {
                                            firstTwo.forEach { entry ->
                                                mutatorStates.add(
                                                    EditableMutatorState(
                                                        key = entry.value,
                                                        output = ScratchBlockType.REPORTER,
                                                        previousStatement = null,
                                                        nextStatement = null,
                                                        inputsInline = null,
                                                        argsText = ""
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("menu_mutator_switch")
                        )
                    }

                    if (enableMutator) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mutatorStates.forEachIndexed { idx, state ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                            OutlinedTextField(
                                                value = state.key,
                                                onValueChange = { v ->
                                                    mutatorStates[idx] = state.copy(key = v)
                                                },
                                                label = { Text("Option Key (value)") },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = JetBrainsMonoFamily
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("menu_mutator_key_$idx")
                                            )
                                            IconButton(
                                                onClick = { mutatorStates.removeAt(idx) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Remove mutator state",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        Text(
                                            text = "output (BlockType)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val outputOptions: List<ScratchBlockType?> = listOf(
                                            null,
                                            ScratchBlockType.REPORTER,
                                            ScratchBlockType.ARRAY,
                                            ScratchBlockType.OBJECT,
                                            ScratchBlockType.BOOLEAN,
                                            ScratchBlockType.COMMAND
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            outputOptions.forEach { opt ->
                                                FilterChip(
                                                    selected = state.output == opt,
                                                    onClick = {
                                                        mutatorStates[idx] = state.copy(output = opt)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = opt?.name ?: "Unchanged",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = state.previousStatement == false,
                                                onClick = {
                                                    mutatorStates[idx] = state.copy(
                                                        previousStatement = if (state.previousStatement == false) null else false
                                                    )
                                                },
                                                label = { Text("prev: false", style = MaterialTheme.typography.labelSmall) }
                                            )
                                            FilterChip(
                                                selected = state.nextStatement == false,
                                                onClick = {
                                                    mutatorStates[idx] = state.copy(
                                                        nextStatement = if (state.nextStatement == false) null else false
                                                    )
                                                },
                                                label = { Text("next: false", style = MaterialTheme.typography.labelSmall) }
                                            )
                                            FilterChip(
                                                selected = state.inputsInline == false,
                                                onClick = {
                                                    mutatorStates[idx] = state.copy(
                                                        inputsInline = if (state.inputsInline == false) null else false
                                                    )
                                                },
                                                label = { Text("inline: false", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }

                                        OutlinedTextField(
                                            value = state.argsText,
                                            onValueChange = { v ->
                                                mutatorStates[idx] = state.copy(argsText = v)
                                            },
                                            label = { Text("arguments (ARG: TYPE = default)") },
                                            placeholder = { Text("INPUT: STRING = a,b,c") },
                                            minLines = 2,
                                            maxLines = 5,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("menu_mutator_args_$idx")
                                        )
                                    }
                                }
                            }

                            val existingKeys = mutatorStates.map { it.key }.toSet()
                            val unmappedValues = parsedRegularEntries
                                .filter { !it.isSeparator && it.value !in existingKeys }
                                .map { it.value }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                unmappedValues.forEach { candidate ->
                                    AssistChip(
                                        onClick = {
                                            mutatorStates.add(
                                                EditableMutatorState(
                                                    key = candidate,
                                                    output = ScratchBlockType.REPORTER,
                                                    previousStatement = null,
                                                    nextStatement = null,
                                                    inputsInline = null,
                                                    argsText = ""
                                                )
                                            )
                                        },
                                        label = { Text("+ State '$candidate'") }
                                    )
                                }
                                AssistChip(
                                    onClick = {
                                        mutatorStates.add(
                                            EditableMutatorState(
                                                key = "STATE_${mutatorStates.size + 1}",
                                                output = ScratchBlockType.REPORTER,
                                                previousStatement = null,
                                                nextStatement = null,
                                                inputsInline = null,
                                                argsText = ""
                                            )
                                        )
                                    },
                                    label = { Text("+ Add Mutator State") },
                                    modifier = Modifier.testTag("menu_add_mutator_state")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("acceptReporters", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (enableMutator) {
                                    "Disabled for mutator dropdowns"
                                } else {
                                    "Allows inserting reporter blocks into dropdown"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = if (enableMutator) false else acceptReporters,
                            enabled = !enableMutator,
                            onCheckedChange = { acceptReporters = it },
                            modifier = Modifier.testTag("menu_accept_reporters_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("acceptText", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (enableMutator) {
                                    "Disabled for mutator dropdowns"
                                } else {
                                    "Lets users type arbitrary text or pick an item (independent of acceptReporters)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = if (enableMutator) false else acceptText,
                            enabled = !enableMutator,
                            onCheckedChange = { acceptText = it },
                            modifier = Modifier.testTag("menu_accept_text_switch")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = canSave,
                        onClick = {
                            if (isDependent) {
                                onSave(
                                    initialMenu.copy(
                                        name = name.ifBlank { "DEPENDENT_MENU" },
                                        acceptReporters = false,
                                        acceptText = false,
                                        items = emptyList(),
                                        itemEntries = emptyList(),
                                        parentName = parentName.trim(),
                                        optionMapping = parsedMapping,
                                        defaultOptions = parsedDefaultOptions,
                                        mutator = emptyMap()
                                    )
                                )
                            } else {
                                val finalEntries = parsedRegularEntries.ifEmpty {
                                    listOf(MenuItemEntry("on"), MenuItemEntry("off"))
                                }
                                val builtMutator = if (enableMutator) {
                                    mutatorStates
                                        .filter { it.key.isNotBlank() }
                                        .associate { st ->
                                            st.key.trim() to MenuMutatorState(
                                                output = st.output,
                                                arguments = parseMutatorArgsText(st.argsText),
                                                previousStatement = st.previousStatement,
                                                nextStatement = st.nextStatement,
                                                inputsInline = st.inputsInline
                                            )
                                        }
                                } else {
                                    emptyMap()
                                }
                                onSave(
                                    initialMenu.copy(
                                        name = name.ifBlank { "CUSTOM_MENU" },
                                        acceptReporters = if (enableMutator) false else acceptReporters,
                                        acceptText = if (enableMutator) false else acceptText,
                                        items = finalEntries.map { it.value },
                                        itemEntries = finalEntries,
                                        parentName = "",
                                        optionMapping = emptyMap(),
                                        defaultOptions = emptyList(),
                                        mutator = builtMutator
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("menu_save_button")
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomTypeItemCard(
    customType: CustomTypeDefinition,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val implementedHooks = buildList {
        add("constructor")
        if (customType.castBody.isNotBlank()) add("cast")
        if (customType.toStringBody.isNotBlank()) add("toString")
        if (customType.valueOfBody.isNotBlank()) add("valueOf")
        if (customType.toJsonBody.isNotBlank()) add("toJSON")
        if (customType.fromJsonBody.isNotBlank()) add("fromJSON")
        if (customType.visualReportBody.isNotBlank()) add("visualReport")
        if (customType.monitorContentBody.isNotBlank()) add("monitorContent")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("custom_type_card_${customType.typeName}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = customType.typeName,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "class ${customType.className}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Shape: ${if (customType.shape == CustomTypeShape.CUSTOM) customType.customShapeName else customType.shape.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                implementedHooks.forEach { hook ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$hook()",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomTypeEditorDialog(
    initialType: CustomTypeDefinition,
    customShapes: List<CustomShapeDefinition>,
    onDismiss: () -> Unit,
    onSave: (CustomTypeDefinition) -> Unit
) {
    BackHandler { onDismiss() }

    var typeName by remember { mutableStateOf(initialType.typeName) }
    var className by remember { mutableStateOf(initialType.className) }
    var shape by remember { mutableStateOf(initialType.shape) }
    var customShapeName by remember { mutableStateOf(initialType.customShapeName) }
    var constructorBody by remember { mutableStateOf(initialType.constructorBody) }
    var castBody by remember { mutableStateOf(initialType.castBody) }
    var toStringBody by remember { mutableStateOf(initialType.toStringBody) }
    var valueOfBody by remember { mutableStateOf(initialType.valueOfBody) }
    var toJsonBody by remember { mutableStateOf(initialType.toJsonBody) }
    var fromJsonBody by remember { mutableStateOf(initialType.fromJsonBody) }
    var visualReportBody by remember { mutableStateOf(initialType.visualReportBody) }
    var monitorContentBody by remember { mutableStateOf(initialType.monitorContentBody) }

    val previewType = CustomTypeDefinition(
        id = initialType.id,
        typeName = typeName.trim().ifBlank { "customType" },
        className = className.trim().ifBlank { "CustomType" },
        shape = shape,
        customShapeName = customShapeName.trim(),
        constructorBody = constructorBody,
        castBody = castBody,
        toStringBody = toStringBody,
        valueOfBody = valueOfBody,
        toJsonBody = toJsonBody,
        fromJsonBody = fromJsonBody,
        visualReportBody = visualReportBody,
        monitorContentBody = monitorContentBody
    )
    val previewJs = remember(previewType) {
        ExtensionCodeGenerator.generateCustomTypeSnippet(previewType)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                    Text("Edit Custom Type", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { onSave(previewType) },
                        enabled = typeName.isNotBlank() && className.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_custom_type_button")
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = typeName,
                            onValueChange = { typeName = it },
                            label = { Text("Type Key (e.g., counter)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_type_name_input")
                        )
                        OutlinedTextField(
                            value = className,
                            onValueChange = { className = it },
                            label = { Text("JS Class Name") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_type_class_input")
                        )
                    }

                    Text("Block Shape (static get shape)", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CustomTypeShape.entries.forEach { option ->
                            FilterChip(
                                selected = shape == option,
                                onClick = { shape = option },
                                label = { Text(option.label) }
                            )
                        }
                    }

                    if (shape == CustomTypeShape.CUSTOM) {
                        if (customShapes.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                customShapes.forEach { registeredShape ->
                                    AssistChip(
                                        onClick = { customShapeName = registeredShape.shapeName },
                                        label = { Text(registeredShape.shapeName, fontFamily = JetBrainsMonoFamily) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = customShapeName,
                            onValueChange = { customShapeName = it },
                            label = { Text("Custom Shape Name (e.g., nbArrow)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = constructorBody,
                        onValueChange = { constructorBody = it },
                        label = { Text("constructor(value) — Required") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = castBody,
                        onValueChange = { castBody = it },
                        label = { Text("static cast(value) — Optional") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = toStringBody,
                            onValueChange = { toStringBody = it },
                            label = { Text("toString()") },
                            minLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = valueOfBody,
                            onValueChange = { valueOfBody = it },
                            label = { Text("valueOf()") },
                            minLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = toJsonBody,
                            onValueChange = { toJsonBody = it },
                            label = { Text("toJSON()") },
                            minLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fromJsonBody,
                            onValueChange = { fromJsonBody = it },
                            label = { Text("static fromJSON(json)") },
                            minLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = visualReportBody,
                        onValueChange = { visualReportBody = it },
                        label = { Text("static visualReport(instance) — HTML string") },
                        supportingText = { Text("Falls back to monitorContent if omitted") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = monitorContentBody,
                        onValueChange = { monitorContentBody = it },
                        label = { Text("static monitorContent(instance) — HTML string") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Generated Registration", style = MaterialTheme.typography.titleSmall)
                    Surface(
                        color = CodeBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = CodeHighlighter.highlightJs(previewJs),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomShapeItemCard(
    customShape: CustomShapeDefinition,
    color1: Color,
    color2: Color,
    color3: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val validation = remember(customShape) {
        CustomShapePathEngine.validateShapeEdges(customShape)
    }
    val paddingInfo = remember(customShape.paddingBody) {
        CustomShapePathEngine.evaluatePadding(customShape.paddingBody)
    }
    val previewBlock = remember(customShape.shapeName) {
        ExtensionBlock(
            opcode = "shapePreview",
            blockType = ScratchBlockType.REPORTER,
            text = customShape.shapeName,
            blockShape = customShape.shapeName
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("custom_shape_card_${customShape.shapeName}")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customShape.shapeName,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = buildString {
                            append("padding(${paddingInfo.left.toInt()}, ${paddingInfo.right.toInt()})")
                            if (customShape.rightEdgeBody.isBlank()) append(" • mirrored rightEdge")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            ScratchBlockVisual(
                block = previewBlock,
                menus = emptyList(),
                color1 = color1,
                color2 = color2,
                color3 = color3,
                customShapes = listOf(customShape)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val hooks = buildList {
                    add("leftEdge")
                    if (customShape.rightEdgeBody.isNotBlank()) add("rightEdge")
                    if (customShape.edgeWidthBody.isNotBlank()) add("edgeWidth")
                    if (customShape.paddingBody.isNotBlank()) add("padding")
                }
                hooks.forEach { hook ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$hook()",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    color = if (validation.isValid) EmeraldSafe.copy(alpha = 0.16f) else AmberWarn.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (validation.isValid) "SVG Sum OK" else "Check SVG Height",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (validation.isValid) EmeraldSafe else AmberWarn,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomShapeEditorDialog(
    initialShape: CustomShapeDefinition,
    color1: Color,
    color2: Color,
    color3: Color,
    onDismiss: () -> Unit,
    onSave: (CustomShapeDefinition) -> Unit
) {
    BackHandler { onDismiss() }

    var shapeName by remember { mutableStateOf(initialShape.shapeName) }
    var edgeWidthBody by remember { mutableStateOf(initialShape.edgeWidthBody) }
    var leftEdgeBody by remember { mutableStateOf(initialShape.leftEdgeBody) }
    var rightEdgeBody by remember { mutableStateOf(initialShape.rightEdgeBody) }
    var paddingBody by remember { mutableStateOf(initialShape.paddingBody) }

    val previewShape = CustomShapeDefinition(
        id = initialShape.id,
        shapeName = shapeName.trim().ifBlank { "customShape" },
        edgeWidthBody = edgeWidthBody,
        leftEdgeBody = leftEdgeBody,
        rightEdgeBody = rightEdgeBody,
        paddingBody = paddingBody
    )
    val validation = remember(previewShape) {
        CustomShapePathEngine.validateShapeEdges(previewShape)
    }
    val previewJs = remember(previewShape) {
        ExtensionCodeGenerator.generateCustomShapeSnippet(previewShape)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                    Text("Edit Custom Shape", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { onSave(previewShape) },
                        enabled = shapeName.isNotBlank() && leftEdgeBody.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_custom_shape_button")
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live SVG Geometry Preview
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ScratchBlockVisual(
                                block = ExtensionBlock(
                                    opcode = "preview",
                                    blockType = ScratchBlockType.REPORTER,
                                    text = "${previewShape.shapeName} reporter",
                                    blockShape = previewShape.shapeName
                                ),
                                menus = emptyList(),
                                color1 = color1,
                                color2 = color2,
                                color3 = color3,
                                customShapes = listOf(previewShape)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (validation.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (validation.isValid) EmeraldSafe else AmberWarn,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = validation.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (validation.isValid) EmeraldSafe else AmberWarn
                                )
                            }
                        }
                    }

                    // Shape Presets
                    Text("Edge Geometry Presets", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                edgeWidthBody = "return Math.min(totalHeight / 2, 32);"
                                leftEdgeBody = "steps.push(`l \${-w} \${-w}`);\nif (halfStraight > 0) steps.push('v', -halfStraight * 2);\nsteps.push(`l \${w} \${-w}`);"
                                rightEdgeBody = "steps.push(`h \${w}`);\nsteps.push(`l \${-w} \${w}`);\nif (halfStraight > 0) steps.push('v', halfStraight * 2);\nsteps.push(`l \${w} \${w}`);"
                                paddingBody = "return 25;"
                            },
                            label = { Text("Arrow + Notch") }
                        )
                        AssistChip(
                            onClick = {
                                edgeWidthBody = "return Math.min(totalHeight / 2, 32);"
                                leftEdgeBody = "steps.push(`h \${-w}`);\nsteps.push(`l \${w} \${-w}`);\nif (halfStraight > 0) steps.push('v', -halfStraight * 2);\nsteps.push(`l \${-w} \${-w}`);"
                                rightEdgeBody = ""
                                paddingBody = "return 25;"
                            },
                            label = { Text("Mirrored Notch") }
                        )
                        AssistChip(
                            onClick = {
                                paddingBody = "return { left: 15, right: 50 };"
                            },
                            label = { Text("Split L/R Padding") }
                        )
                    }

                    OutlinedTextField(
                        value = shapeName,
                        onValueChange = { shapeName = it },
                        label = { Text("Shape Name (e.g., nbArrow)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_shape_name_input")
                    )

                    OutlinedTextField(
                        value = edgeWidthBody,
                        onValueChange = { edgeWidthBody = it },
                        label = { Text("edgeWidth(totalHeight) — Optional") },
                        supportingText = { Text("Defaults to totalHeight / 2 if blank. Cap with Math.min(totalHeight / 2, 32).") },
                        minLines = 1,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = leftEdgeBody,
                        onValueChange = { leftEdgeBody = it },
                        label = { Text("leftEdge(steps, w, halfStraight) — Required") },
                        supportingText = { Text("Bottom-left to top-left relative SVG (l, h, v). Vertical sum must equal -(2*w + 2*halfStraight).") },
                        minLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_shape_left_edge_input")
                    )

                    OutlinedTextField(
                        value = rightEdgeBody,
                        onValueChange = { rightEdgeBody = it },
                        label = { Text("rightEdge(steps, w, halfStraight) — Optional") },
                        supportingText = { Text("Top-right to bottom-right. Leave blank to mirror leftEdge automatically.") },
                        minLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = paddingBody,
                        onValueChange = { paddingBody = it },
                        label = { Text("padding(innerShape) — Optional") },
                        supportingText = { Text("Return a number (e.g., return 25;) or object (return { left: 15, right: 50 };)") },
                        minLines = 1,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Generated Registration", style = MaterialTheme.typography.titleSmall)
                    Surface(
                        color = CodeBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = CodeHighlighter.highlightJs(previewJs),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

