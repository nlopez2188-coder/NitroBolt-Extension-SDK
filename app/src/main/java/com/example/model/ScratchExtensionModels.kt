package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class ExecutionMode(
    val displayName: String,
    val contextLabel: String,
    val timingBadge: String
) {
    UNSANDBOXED(
        displayName = "Unsandboxed",
        contextLabel = "<script> Main Context",
        timingBadge = "0ms Instant • Full VM"
    ),
    SANDBOXED(
        displayName = "Sandboxed",
        contextLabel = "<iframe> Cross-Origin",
        timingBadge = "1-Frame Delay • Isolated"
    )
}

enum class ScratchBlockType(
    val scratchConstant: String,
    val label: String,
    val requiresReturn: Boolean
) {
    COMMAND("Scratch.BlockType.COMMAND", "Command", false),
    REPORTER("Scratch.BlockType.REPORTER", "Reporter", true),
    BOOLEAN("Scratch.BlockType.BOOLEAN", "Boolean", true),
    HAT("Scratch.BlockType.HAT", "Hat Event", false),
    BUTTON("Scratch.BlockType.BUTTON", "Button", false),
    LABEL("Scratch.BlockType.LABEL", "Label", false),
    OBJECT("Scratch.BlockType.OBJECT", "Object", true),
    ARRAY("Scratch.BlockType.ARRAY", "Array", true),
    CONDITIONAL("Scratch.BlockType.CONDITIONAL", "Conditional", false),
    LOOP("Scratch.BlockType.LOOP", "Loop", false)
}

enum class ScratchArgumentType(
    val scratchConstant: String,
    val label: String,
    val sampleDefault: String
) {
    STRING("Scratch.ArgumentType.STRING", "String", "hello"),
    NUMBER("Scratch.ArgumentType.NUMBER", "Number", "10"),
    BOOLEAN("Scratch.ArgumentType.BOOLEAN", "Boolean", "true"),
    COLOR("Scratch.ArgumentType.COLOR", "Color", "#FF8A00"),
    ANGLE("Scratch.ArgumentType.ANGLE", "Angle", "90"),
    NOTE("Scratch.ArgumentType.NOTE", "Note", "60"),
    MATRIX("Scratch.ArgumentType.MATRIX", "Matrix", "0101010101100010101000100"),
    IMAGE("Scratch.ArgumentType.IMAGE", "Image", ""),
    SLIDER("Scratch.ArgumentType.SLIDER", "Slider", "50"),
    OBJECT("Scratch.ArgumentType.OBJECT", "Object", ""),
    ARRAY("Scratch.ArgumentType.ARRAY", "Array", ""),
    EXTENDABLE("Scratch.ArgumentType.EXTENDABLE", "Extendable", "")
}

data class BlockArgument(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ScratchArgumentType = ScratchArgumentType.STRING,
    val customTypeName: String = "",
    val defaultValue: String = "",
    val sliderMin: String = "0",
    val sliderMax: String = "100",
    val sliderPrecision: String = "1",
    val acceptReporters: Boolean = true,
    val canMultiline: Boolean = false,
    val menuName: String = "",
    val shadow: String = "",
    val extendableText: String = "[ITEM]",
    val extendableArguments: List<BlockArgument> = emptyList(),
    val extendableDefaultInputs: String = "",
    val extendableMinInputs: String = "",
    val extendableMaxInputs: String = "",
    val extendableAcceptArray: Boolean = false,
    val extendableSeparator: String = ""
)

enum class CustomTypeShape(
    val label: String,
    val scratchShapeConst: String?
) {
    DEFAULT("Default (Round)", null),
    SQUARE("Square", "Scratch.BlockShape.SQUARE"),
    HEXAGONAL("Hexagonal", "Scratch.BlockShape.HEXAGONAL"),
    LEAF("Leaf", "Scratch.BlockShape.LEAF"),
    PLUS("Plus", "Scratch.BlockShape.PLUS"),
    OCTAGONAL("Octagonal", "Scratch.BlockShape.OCTAGONAL"),
    CUSTOM("Custom Shape Name", null)
}

data class CustomTypeDefinition(
    val id: String = UUID.randomUUID().toString(),
    val typeName: String = "counter",
    val className: String = "Counter",
    val shape: CustomTypeShape = CustomTypeShape.SQUARE,
    val customShapeName: String = "",
    val constructorBody: String = "this.value = Scratch.Cast.toNumber(value || 0);",
    val castBody: String = "if (value instanceof Counter) return value;\nreturn new Counter(value);",
    val toStringBody: String = "return `counter:\${this.value}`;",
    val valueOfBody: String = "return this.value;",
    val toJsonBody: String = "return { count: this.value };",
    val fromJsonBody: String = "return new Counter(json.count);",
    val visualReportBody: String = "",
    val monitorContentBody: String = ""
)

data class CustomShapeDefinition(
    val id: String = UUID.randomUUID().toString(),
    val shapeName: String = "nbArrow",
    val edgeWidthBody: String = "return Math.min(totalHeight / 2, 32);",
    val leftEdgeBody: String = "steps.push(`l \${-w} \${-w}`);\nif (halfStraight > 0) steps.push(\"v\", -halfStraight * 2);\nsteps.push(`l \${w} \${-w}`);",
    val rightEdgeBody: String = "",
    val paddingBody: String = "return 25;"
)

data class BlockSwitchInputMapping(
    val from: String,
    val to: String
)

data class BlockSwitch(
    val id: String = UUID.randomUUID().toString(),
    val targetId: String,
    val inputs: List<BlockSwitchInputMapping> = emptyList(),
    val splitInputs: List<String> = emptyList(),
    val rawId: Boolean = false
) {
    val isSimpleStringForm: Boolean
        get() = inputs.isEmpty() && splitInputs.isEmpty() && !rawId
}

data class ExtensionBlock(
    val id: String = UUID.randomUUID().toString(),
    val opcode: String,
    val blockType: ScratchBlockType = ScratchBlockType.COMMAND,
    val dualType: ScratchBlockType? = null,
    val text: String,
    val branchCount: Int = 0,
    val branchTexts: List<String> = emptyList(),
    val outputType: String = "",
    val blockShape: String = "",
    val duplicateOnDrag: Boolean = false,
    val arguments: List<BlockArgument> = emptyList(),
    val switches: List<BlockSwitch> = emptyList(),
    val jsBody: String = ""
) {
    val isBranchBlock: Boolean
        get() = blockType == ScratchBlockType.CONDITIONAL || blockType == ScratchBlockType.LOOP

    val effectiveBranchCount: Int
        get() = if (isBranchBlock) branchCount.coerceAtLeast(1) else branchCount
}

data class MenuItemEntry(
    val value: String,
    val text: String = value
) {
    val isSeparator: Boolean
        get() = value == ExtensionMenu.SEPARATOR_ITEM && text == ExtensionMenu.SEPARATOR_ITEM

    val isObjectForm: Boolean
        get() = text != value
}

data class MenuMutatorState(
    val output: ScratchBlockType? = null,
    val arguments: List<BlockArgument> = emptyList(),
    val previousStatement: Boolean? = null,
    val nextStatement: Boolean? = null,
    val inputsInline: Boolean? = null
)

data class MutatedBlockSpec(
    val block: ExtensionBlock,
    val previousStatement: Boolean? = null,
    val nextStatement: Boolean? = null,
    val inputsInline: Boolean? = null,
    val transformedArgNames: Set<String> = emptySet()
)

data class ExtensionMenu(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val acceptReporters: Boolean = true,
    val acceptText: Boolean = false,
    val items: List<String> = listOf("on", "off"),
    val itemEntries: List<MenuItemEntry> = emptyList(),
    val parentName: String = "",
    val optionMapping: Map<String, List<MenuItemEntry>> = emptyMap(),
    val defaultOptions: List<MenuItemEntry> = emptyList(),
    val mutator: Map<String, MenuMutatorState> = emptyMap()
) {
    val isDependent: Boolean
        get() = parentName.isNotBlank()

    val isMutator: Boolean
        get() = mutator.isNotEmpty()

    val regularEntries: List<MenuItemEntry>
        get() = if (itemEntries.isNotEmpty()) {
            itemEntries
        } else {
            items.map { MenuItemEntry(value = it, text = it) }
        }

    val selectableItems: List<String>
        get() = regularEntries.filter { !it.isSeparator }.map { it.value }

    val firstSelectableItem: String?
        get() = regularEntries.firstOrNull { !it.isSeparator }?.value

    fun resolveOptions(parentValue: String? = null): List<MenuItemEntry> {
        if (!isDependent) {
            return regularEntries
        }
        val mapped = if (parentValue != null && optionMapping.containsKey(parentValue)) {
            optionMapping[parentValue].orEmpty()
        } else {
            defaultOptions
        }
        return mapped.ifEmpty { defaultOptions }
    }

    fun firstSelectableValue(parentValue: String? = null): String? {
        return resolveOptions(parentValue).firstOrNull { !it.isSeparator }?.value
            ?: defaultOptions.firstOrNull { !it.isSeparator }?.value
    }

    companion object {
        const val SEPARATOR_ITEM = "---"
    }
}

data class BlockColorPreset(
    val name: String,
    val color1Hex: String,
    val color2Hex: String,
    val color3Hex: String
) {
    val primaryColor: Color get() = parseHexColor(color1Hex, Color(0xFFFF8A00))
    val secondaryColor: Color get() = parseHexColor(color2Hex, Color(0xFFE67600))
    val tertiaryColor: Color get() = parseHexColor(color3Hex, Color(0xFFC25E00))
}

data class ExtensionProject(
    val id: String = "turbomodeunsandboxed",
    val name: String = "Turbo Mode",
    val className: String = "TurboMode",
    val executionMode: ExecutionMode = ExecutionMode.UNSANDBOXED,
    val enforceUnsandboxedGuard: Boolean = true,
    val useUnsandboxedRuntime: Boolean = false,
    val bindVmReference: Boolean = true,
    val color1Hex: String = "#FF8A00",
    val color2Hex: String = "#E07500",
    val color3Hex: String = "#BD6000",
    val constructorBody: String = "",
    val customShapes: List<CustomShapeDefinition> = emptyList(),
    val customTypes: List<CustomTypeDefinition> = emptyList(),
    val blocks: List<ExtensionBlock> = emptyList(),
    val menus: List<ExtensionMenu> = emptyList()
)

enum class LintSeverity {
    ERROR,
    WARNING,
    INFO
}

enum class LintFixAction {
    ADD_RETURN_DEFAULT,
    SWITCH_TO_UNSANDBOXED,
    ENABLE_UNSANDBOXED_GUARD,
    SANITIZE_EXTENSION_ID
}

data class LintIssue(
    val id: String,
    val severity: LintSeverity,
    val title: String,
    val detail: String,
    val blockId: String? = null,
    val fixAction: LintFixAction? = null
)

data class ConversionStep(
    val title: String,
    val description: String,
    val applied: Boolean
)

data class ConversionResult(
    val convertedCode: String,
    val steps: List<ConversionStep>,
    val extractedProject: ExtensionProject?,
    val warnings: List<String>
)

data class VmSimulationState(
    val turboMode: Boolean = false,
    val fps: Int = 30,
    val activeSpriteName: String = "Sprite1",
    val spriteX: Double = 0.0,
    val spriteY: Double = 0.0,
    val projectVariables: Map<String, String> = mapOf(
        "score" to "42",
        "level" to "3",
        "playerName" to "NitroCoder"
    )
)

data class ExecutionLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val opcode: String,
    val blockLabel: String,
    val output: String,
    val executionTimeLabel: String,
    val executionMode: ExecutionMode,
    val isError: Boolean = false
)

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (_: NumberFormatException) {
        fallback
    }
}

fun snapAndFormatSliderValue(
    raw: Float,
    min: Double,
    max: Double,
    precision: Double,
    precisionText: String,
    minText: String = ""
): String {
    if (min >= max || precision <= 0.0) {
        return raw.toString()
    }
    val stepped = kotlin.math.round((raw.toDouble() - min) / precision) * precision + min
    val clamped = stepped.coerceIn(min, max)

    fun decimalPlaces(text: String): Int {
        val trimmed = text.trim()
        val dotIndex = trimmed.indexOf('.')
        if (dotIndex < 0) return 0
        return trimmed.substring(dotIndex + 1).trimEnd('0').length
    }

    val scale = maxOf(decimalPlaces(precisionText), decimalPlaces(minText), 0).coerceAtMost(8)
    val bd = java.math.BigDecimal.valueOf(clamped)
        .setScale(scale, java.math.RoundingMode.HALF_UP)
    return if (bd.compareTo(java.math.BigDecimal.ZERO) == 0) {
        "0"
    } else {
        bd.stripTrailingZeros().toPlainString()
    }
}
