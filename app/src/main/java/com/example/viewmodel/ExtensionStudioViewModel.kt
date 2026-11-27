package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.BlockSimulator
import com.example.data.ExtensionCodeGenerator
import com.example.data.ExtensionTemplate
import com.example.data.ExtensionTemplates
import com.example.data.SandboxedConverter
import com.example.model.BlockColorPreset
import com.example.model.ConversionResult
import com.example.model.ExecutionLogEntry
import com.example.model.ExecutionMode
import com.example.model.ExtensionBlock
import com.example.model.ExtensionMenu
import com.example.model.ExtensionProject
import com.example.model.LintFixAction
import com.example.model.LintIssue
import com.example.model.ScratchArgumentType
import com.example.model.ScratchBlockType
import com.example.model.VmSimulationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class StudioTab {
    BUILDER,
    CONVERTER,
    CODE_AND_TEST,
    GUIDE
}

data class StudioUiState(
    val activeTab: StudioTab = StudioTab.BUILDER,
    val project: ExtensionProject = ExtensionTemplates.turboModeProject,
    val generatedJs: String = ExtensionCodeGenerator.generate(ExtensionTemplates.turboModeProject),
    val lintIssues: List<LintIssue> = ExtensionCodeGenerator.analyzeProject(ExtensionTemplates.turboModeProject),
    val converterInput: String = ExtensionTemplates.legacySandboxedSamples.first().code,
    val conversionResult: ConversionResult = SandboxedConverter.convertToUnsandboxed(
        ExtensionTemplates.legacySandboxedSamples.first().code
    ),
    val vmState: VmSimulationState = VmSimulationState(),
    val executionLogs: List<ExecutionLogEntry> = emptyList(),
    val simulatorInputs: Map<String, Map<String, String>> = emptyMap()
)

class ExtensionStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    private fun updateProject(transform: (ExtensionProject) -> ExtensionProject) {
        _uiState.update { state ->
            val updated = transform(state.project)
            state.copy(
                project = updated,
                generatedJs = ExtensionCodeGenerator.generate(updated),
                lintIssues = ExtensionCodeGenerator.analyzeProject(updated)
            )
        }
    }

    fun updateExtensionId(id: String) {
        updateProject { it.copy(id = id) }
    }

    fun updateExtensionName(name: String) {
        updateProject { it.copy(name = name) }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        updateProject { project ->
            project.copy(
                executionMode = mode,
                enforceUnsandboxedGuard = if (mode == ExecutionMode.UNSANDBOXED) true else false,
                bindVmReference = if (mode == ExecutionMode.SANDBOXED) false else project.bindVmReference
            )
        }
    }

    fun setEnforceGuard(enabled: Boolean) {
        updateProject { it.copy(enforceUnsandboxedGuard = enabled) }
    }

    fun setBindVmReference(enabled: Boolean) {
        updateProject { it.copy(bindVmReference = enabled) }
    }

    fun applyColorPreset(preset: BlockColorPreset) {
        updateProject {
            it.copy(
                color1Hex = preset.color1Hex,
                color2Hex = preset.color2Hex,
                color3Hex = preset.color3Hex
            )
        }
    }

    fun loadTemplate(template: ExtensionTemplate) {
        loadProject(template.project)
    }

    fun loadProject(project: ExtensionProject) {
        _uiState.update { state ->
            state.copy(
                activeTab = StudioTab.BUILDER,
                project = project,
                generatedJs = ExtensionCodeGenerator.generate(project),
                lintIssues = ExtensionCodeGenerator.analyzeProject(project),
                executionLogs = emptyList(),
                simulatorInputs = emptyMap()
            )
        }
    }

    fun saveBlock(block: ExtensionBlock) {
        updateProject { project ->
            val exists = project.blocks.any { it.id == block.id }
            val updatedBlocks = if (exists) {
                project.blocks.map { if (it.id == block.id) block else it }
            } else {
                project.blocks + block
            }
            project.copy(blocks = updatedBlocks)
        }
    }

    fun deleteBlock(blockId: String) {
        updateProject { project ->
            project.copy(blocks = project.blocks.filterNot { it.id == blockId })
        }
    }

    fun moveBlock(blockId: String, direction: Int) {
        updateProject { project ->
            val list = project.blocks.toMutableList()
            val idx = list.indexOfFirst { it.id == blockId }
            val targetIdx = idx + direction
            if (idx != -1 && targetIdx in list.indices) {
                val item = list.removeAt(idx)
                list.add(targetIdx, item)
            }
            project.copy(blocks = list)
        }
    }

    fun saveMenu(menu: ExtensionMenu) {
        updateProject { project ->
            val exists = project.menus.any { it.id == menu.id }
            val updatedMenus = if (exists) {
                project.menus.map { if (it.id == menu.id) menu else it }
            } else {
                project.menus + menu
            }
            project.copy(menus = updatedMenus)
        }
    }

    fun deleteMenu(menuId: String) {
        updateProject { project ->
            project.copy(menus = project.menus.filterNot { it.id == menuId })
        }
    }

    fun saveCustomType(customType: com.example.model.CustomTypeDefinition) {
        updateProject { project ->
            val exists = project.customTypes.any { it.id == customType.id }
            val updatedTypes = if (exists) {
                project.customTypes.map { if (it.id == customType.id) customType else it }
            } else {
                project.customTypes + customType
            }
            project.copy(customTypes = updatedTypes)
        }
    }

    fun deleteCustomType(customTypeId: String) {
        updateProject { project ->
            project.copy(customTypes = project.customTypes.filterNot { it.id == customTypeId })
        }
    }

    fun saveCustomShape(customShape: com.example.model.CustomShapeDefinition) {
        updateProject { project ->
            val exists = project.customShapes.any { it.id == customShape.id }
            val updatedShapes = if (exists) {
                project.customShapes.map { if (it.id == customShape.id) customShape else it }
            } else {
                project.customShapes + customShape
            }
            project.copy(customShapes = updatedShapes)
        }
    }

    fun deleteCustomShape(customShapeId: String) {
        updateProject { project ->
            project.copy(customShapes = project.customShapes.filterNot { it.id == customShapeId })
        }
    }

    fun applyLintFix(issue: LintIssue) {
        when (issue.fixAction) {
            LintFixAction.ADD_RETURN_DEFAULT -> {
                val blockId = issue.blockId ?: return
                updateProject { project ->
                    project.copy(
                        blocks = project.blocks.map { block ->
                            if (block.id == blockId) {
                                val defaultRet = when (block.blockType) {
                                    ScratchBlockType.BOOLEAN -> "return false;"
                                    ScratchBlockType.OBJECT -> "return {};"
                                    ScratchBlockType.ARRAY -> "return [];"
                                    else -> "return '';"
                                }
                                val newBody = if (block.jsBody.isBlank()) defaultRet
                                else "${block.jsBody.trimEnd()}\n$defaultRet"
                                block.copy(jsBody = newBody)
                            } else {
                                block
                            }
                        }
                    )
                }
            }
            LintFixAction.SWITCH_TO_UNSANDBOXED -> {
                setExecutionMode(ExecutionMode.UNSANDBOXED)
                setBindVmReference(true)
            }
            LintFixAction.ENABLE_UNSANDBOXED_GUARD -> {
                setEnforceGuard(true)
            }
            LintFixAction.SANITIZE_EXTENSION_ID -> {
                updateProject { it.copy(id = ExtensionCodeGenerator.sanitizeExtensionId(it.id)) }
            }
            null -> Unit
        }
    }

    // Converter Actions
    fun updateConverterInput(code: String) {
        _uiState.update { state ->
            state.copy(
                converterInput = code,
                conversionResult = SandboxedConverter.convertToUnsandboxed(code)
            )
        }
    }

    fun runConversion() {
        _uiState.update { state ->
            state.copy(
                conversionResult = SandboxedConverter.convertToUnsandboxed(state.converterInput)
            )
        }
    }

    fun importConvertedToBuilder(): Boolean {
        val extracted = _uiState.value.conversionResult.extractedProject ?: return false
        loadProject(extracted)
        return true
    }

    // Simulator Actions
    fun updateSimulatorArg(blockId: String, argName: String, value: String) {
        _uiState.update { state ->
            val currentBlockMap = state.simulatorInputs[blockId].orEmpty()
            val targetBlock = state.project.blocks.find { it.id == blockId }
            val nextMap = if (targetBlock != null) {
                BlockSimulator.applyInputChange(
                    project = state.project,
                    block = targetBlock,
                    changedArgName = argName,
                    newValue = value,
                    existingInputs = currentBlockMap
                )
            } else {
                currentBlockMap + (argName to value)
            }
            val updatedMap = state.simulatorInputs.toMutableMap()
            updatedMap[blockId] = nextMap
            state.copy(simulatorInputs = updatedMap)
        }
    }

    fun runBlockSimulation(block: ExtensionBlock) {
        val state = _uiState.value
        val customArgs = state.simulatorInputs[block.id].orEmpty()
        val effectiveBlock = BlockSimulator.resolveMutatedBlock(block, state.project.menus, customArgs).block
        val resolvedArgs = BlockSimulator.resolveDefaultArgValues(
            project = state.project,
            block = effectiveBlock,
            currentVm = state.vmState,
            existingInputs = customArgs
        )

        val (nextVm, logEntry) = BlockSimulator.execute(
            project = state.project,
            block = block,
            argValues = resolvedArgs,
            currentVm = state.vmState
        )

        _uiState.update { s ->
            s.copy(
                vmState = nextVm,
                executionLogs = (listOf(logEntry) + s.executionLogs).take(12)
            )
        }
    }

    fun clearSimulationLogs() {
        _uiState.update {
            it.copy(
                vmState = VmSimulationState(),
                executionLogs = emptyList()
            )
        }
    }
}
