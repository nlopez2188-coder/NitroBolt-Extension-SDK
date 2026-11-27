package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExtensionTemplate
import com.example.data.ExtensionTemplates
import com.example.ui.components.CodeHighlighter
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CodeBg
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ForgeOrange
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun GuideScreen(
    onLoadTemplate: (ExtensionTemplate) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val templateLoadedMsg = stringResource(R.string.msg_template_loaded)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Studio Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner),
                        contentDescription = "NitroBolt Extension Studio Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color(0xFF0D111A).copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.guide_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Sandboxed vs Unsandboxed architecture, IIFE isolation, and direct Scratch.vm patterns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Sandboxed vs Unsandboxed Comparison
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ComparisonCard(
                        title = "Sandboxed",
                        subtitle = "Cross-origin <iframe>",
                        icon = { Icon(Icons.Filled.Security, contentDescription = null, tint = CyberCyan) },
                        accent = CyberCyan,
                        points = listOf(
                            false to "Cannot access Scratch.vm internals",
                            false to "Cannot read unpassed variables or sprites",
                            false to "1-frame delay per block (even in Turbo Mode)",
                            true to "Can fetch broader internet & Web APIs"
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonCard(
                        title = "Unsandboxed",
                        subtitle = "Main <script> context",
                        icon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = ForgeOrange) },
                        accent = ForgeOrange,
                        points = listOf(
                            true to "Full access to Scratch.vm object",
                            true to "Reads variables & interacts with all sprites",
                            true to "Instantaneous execution (0ms delay)",
                            true to "Backward-compatible if IIFE doesn't use VM"
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Good vs Bad IIFE Scope Rule
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "IIFE Scope Rule ('use strict')",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Every variable, class, and 'const vm = Scratch.vm' declaration MUST stay inside the immediately-invoked function expression so unsandboxed extensions never collide in global scope.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                // GOOD CODE — scoped inside IIFE
                                (function(Scratch) {
                                  'use strict';
                                  const vm = Scratch.vm;
                                  // ...
                                })(Scratch);

                                // BAD CODE — leaks vm globally!
                                const vm = Scratch.vm;
                                (function(Scratch) {
                                  // ...
                                })(Scratch);
                            """.trimIndent()
                        )
                    }
                }
            }

            // Additional Requirements Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AmberWarn.copy(alpha = 0.12f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberWarn)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Additional Unsandboxed Requirements",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "1. Valid Return Values: Reporter and boolean blocks MUST return a valid string, number, or boolean — omitting returns breaks scripts in unknown ways.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. No Infinite Loops: Unsandboxed extensions share the main editor window. An infinite loop freezes the entire tab and risks data loss.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Block Switches Reference Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Block Switches (Context Menu Swaps)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Switches let users right-click a block to swap to related blocks. Use String form for direct swaps, or Object form to remap inputs, drop unused splitInputs, or set rawId: true for built-in/external blocks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                // String form (no remapping):
                                switches: ['def', 'otherblock']

                                // Object form (remap + drop + rawId):
                                switches: [
                                  { id: 'sety', inputs: [['X', 'Y']] },
                                  { id: 'say', splitInputs: ['SECS'] },
                                  { id: 'motion_changexby', rawId: true }
                                ]
                            """.trimIndent()
                        )
                    }
                }
            }

            // Custom Types Reference Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Custom Types (Scratch.types.register)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Available only in unsandboxed extensions. Register each class before Scratch.extensions.register() to control casting, block shapes (e.g., SQUARE), JSON serialization, and HTML visualReport/monitorContent bubbles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                class Counter {
                                  static get shape() { return Scratch.BlockShape.SQUARE; }
                                  constructor(value) { this.value = Scratch.Cast.toNumber(value || 0); }
                                  static cast(v) { return v instanceof Counter ? v : new Counter(v); }
                                  toJSON() { return { count: this.value }; }
                                  static fromJSON(json) { return new Counter(json.count); }
                                }
                                Scratch.types.register('counter', Counter);
                            """.trimIndent()
                        )
                    }
                }
            }

            // Custom Shapes Reference Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Custom Shapes (Scratch.BlockShapes.register)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Unsandboxed-only API to draw custom SVG edge geometry before Scratch.extensions.register(). leftEdge traces bottom-left → top-left summing vertically to -(2·w + 2·halfStraight); rightEdge traces top-right → bottom-right (or mirrors leftEdge if omitted).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                Scratch.BlockShapes.register('nbArrow', {
                                  edgeWidth: (totalHeight) => Math.min(totalHeight / 2, 32),
                                  leftEdge: (steps, w, halfStraight) => {
                                    steps.push(`l ${'$'}{-w} ${'$'}{-w}`);
                                    if (halfStraight > 0) steps.push('v', -halfStraight * 2);
                                    steps.push(`l ${'$'}{w} ${'$'}{-w}`);
                                  },
                                  padding: () => ({ left: 15, right: 50 })
                                });
                            """.trimIndent()
                        )
                    }
                }
            }

            // String Inputs Reference Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "String Inputs (canMultiline & acceptReporters)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Scratch.ArgumentType.STRING supports canMultiline: true (grows vertically with no scrollbar while preserving \\n newlines), acceptReporters: false (direct fixed text field), and shadow (custom reporter block replacing the text shadow).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                TEXT: {
                                  type: Scratch.ArgumentType.STRING,
                                  defaultValue: 'first line\nsecond line',
                                  canMultiline: true,
                                  acceptReporters: false
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Slider Inputs (Scratch.ArgumentType.SLIDER)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Numeric inputs configurable with defaultValue (default 0), min (default 0), max (default 100), and precision (default 1, e.g. 0.01 for decimal steps). Slider blocks keep their rounded numeric shape so users can still type exact values or drop reporter blocks in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                AMOUNT: {
                                  type: Scratch.ArgumentType.SLIDER,
                                  defaultValue: 50,
                                  min: 0,
                                  max: 100,
                                  precision: 1
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Objects & Arrays (Pure JS Values)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Use Scratch.BlockType.OBJECT and Scratch.BlockType.ARRAY to return pure JavaScript objects and arrays with distinct block shapes, and Scratch.ArgumentType.OBJECT / ARRAY for input slots that only accept object/array-shaped reporters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                {
                                  opcode: 'object',
                                  blockType: Scratch.BlockType.OBJECT,
                                  text: 'object with [KEY] = [VALUE]'
                                },
                                {
                                  opcode: 'readKey',
                                  blockType: Scratch.BlockType.REPORTER,
                                  text: 'key [KEY] of [OBJ]',
                                  arguments: {
                                    KEY: { type: Scratch.ArgumentType.STRING, defaultValue: 'key' },
                                    OBJ: { type: Scratch.ArgumentType.OBJECT }
                                  }
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Duplicate on Drag (duplicateOnDrag & shadow)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Set duplicateOnDrag: true on Reporter or Boolean blocks so dragging them creates a copy (ideal for loop variables). Pair with shadow: 'opcode' on an argument to pre-fill that input with your reporter block.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
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
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Extendables (Scratch.ArgumentType.EXTENDABLE)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Extendable arguments give blocks dynamic +/- inputs (like join) and can even nest multiple layers deep. Configure defaultInputs, minInputs, maxInputs, acceptArray, separator, and read flattened values cleanly via util.extendableToArray(args, ...).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                arguments: {
                                  INPUTS: {
                                    type: Scratch.ArgumentType.EXTENDABLE,
                                    text: '[ABC]',
                                    defaultInputs: 2,
                                    minInputs: 1,
                                    maxInputs: 5,
                                    acceptArray: false,
                                    separator: '|',
                                    arguments: {
                                      ABC: { type: Scratch.ArgumentType.STRING, defaultValue: '' }
                                    }
                                  }
                                }
                                // In method(args, util):
                                const values = util.extendableToArray(args, 'INPUTS', 'ABC');
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Branches (CONDITIONAL & LOOP C-Blocks)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Use branchCount with Scratch.BlockType.CONDITIONAL or Scratch.BlockType.LOOP to create C-shaped blocks (like if/else or repeat loops). LOOP adds the loop arrow icon, and util.startBranch(branchNum, isLoop) executes the inner script.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                {
                                  opcode: 'branch',
                                  blockType: Scratch.BlockType.CONDITIONAL,
                                  text: ['hello', 'neighbor'],
                                  branchCount: 1
                                }
                                // In branch(args, util):
                                util.startBranch(1, false);
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Extra Menu Properties (Separators & acceptText)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add '---' inside menu items to draw visual-only horizontal separators (menus still need at least one selectable item). Set acceptText: true to let users type arbitrary values alongside listed items — independently of acceptReporters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                menus: {
                                  colors: {
                                    acceptText: true,
                                    acceptReporters: true,
                                    items: ['red', 'green', 'blue', '---', 'cyan', 'magenta']
                                  }
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Dependent Dropdowns (parentName & Chaining)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Replace items with parentName, optionMapping, and optional defaultOptions so a dropdown updates dynamically when an earlier argument on the same block changes (supports multi-step chaining like ANIMAL → BREED → COAT). Parent arguments must appear before children, and acceptText/acceptReporters are unsupported.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                BREED_MENU: {
                                  parentName: 'ANIMAL',
                                  optionMapping: {
                                    cat: ['Siamese', 'Tabby'],
                                    dog: ['Beagle', 'Poodle']
                                  },
                                  defaultOptions: ['unknown']
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Mutator Dropdowns (Dynamic Shape & Inputs)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add a mutator object to a static menu to transform a block's output (Scratch.BlockType), previousStatement/nextStatement/inputsInline flags, and arguments metadata whenever an option is selected. Transformed inputs reset automatically while unrelated inputs keep their values.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CodeSnippetBox(
                            code = """
                                MODE_MENU: {
                                  items: [
                                    { text: 'split', value: 'SPLIT' },
                                    { text: 'join', value: 'JOIN' }
                                  ],
                                  mutator: {
                                    SPLIT: {
                                      output: Scratch.BlockType.ARRAY,
                                      arguments: { INPUT: { type: Scratch.ArgumentType.STRING, defaultValue: 'a,b,c' } }
                                    },
                                    JOIN: {
                                      output: Scratch.BlockType.REPORTER,
                                      arguments: { INPUT: { type: Scratch.ArgumentType.ARRAY } }
                                    }
                                  }
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            // Interactive Docs Examples
            item {
                Text(
                    text = "Official NitroBolt Examples",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                val helloTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.helloWorldProject.id
                }
                ExampleGuideCard(
                    title = "unsandboxed.js — Hello World",
                    description = "Checks Scratch.extensions.unsandboxed and registers a clean reporter block inside a strict-mode IIFE.",
                    template = helloTemplate,
                    testTag = "guide_load_helloworld",
                    onLoad = {
                        onLoadTemplate(helloTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val turboTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.turboModeProject.id
                }
                ExampleGuideCard(
                    title = "turbo-mode.js — Accessing Scratch.vm",
                    description = "Uses const vm = Scratch.vm inside the IIFE to call vm.setTurboMode(args.ENABLED === 'on') with an acceptReporters menu.",
                    template = turboTemplate,
                    testTag = "guide_load_turbomode",
                    onLoad = {
                        onLoadTemplate(turboTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val switchesTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.switchesProject.id
                }
                ExampleGuideCard(
                    title = "switches.js — Block Switches & Remapping",
                    description = "Demonstrates string switches (abc ⇄ def), input remapping (TEXT → MSG), and splitInputs dropping SECS.",
                    template = switchesTemplate,
                    testTag = "guide_load_switches",
                    onLoad = {
                        onLoadTemplate(switchesTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val customTypesTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.customTypesProject.id
                }
                ExampleGuideCard(
                    title = "custom-types.js — Counter Type & Square Blocks",
                    description = "Registers Counter with Scratch.types.register, SQUARE block shape, outputType, JSON serialization, and visualReport.",
                    template = customTypesTemplate,
                    testTag = "guide_load_custom_types",
                    onLoad = {
                        onLoadTemplate(customTypesTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val customShapesTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.customShapesProject.id
                }
                ExampleGuideCard(
                    title = "custom-shapes.js — Arrow Edges & Dual Blocks",
                    description = "Registers nbArrow, nbArrowNoleft, and nbArrowPadding via Scratch.BlockShapes.register with blockShape and dualType reporters.",
                    template = customShapesTemplate,
                    testTag = "guide_load_custom_shapes",
                    onLoad = {
                        onLoadTemplate(customShapesTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val stringInputsTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.stringInputsProject.id
                }
                ExampleGuideCard(
                    title = "string-inputs.js — Normal, Multiline & Fixed Fields",
                    description = "Demonstrates default string slots, canMultiline: true vertical expansion, and acceptReporters: false literal text fields.",
                    template = stringInputsTemplate,
                    testTag = "guide_load_string_inputs",
                    onLoad = {
                        onLoadTemplate(stringInputsTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val sliderInputsTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.sliderInputsProject.id
                }
                ExampleGuideCard(
                    title = "slider-inputs.js — Range & Decimal Precision",
                    description = "Demonstrates Scratch.ArgumentType.SLIDER with 0..100 integer stepping (default 50) and -1..1 fine decimal precision (0.01).",
                    template = sliderInputsTemplate,
                    testTag = "guide_load_slider_inputs",
                    onLoad = {
                        onLoadTemplate(sliderInputsTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val objectArrayTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.objectArrayProject.id
                }
                ExampleGuideCard(
                    title = "object-array.js — Objects & Arrays",
                    description = "Builds and inspects pure JS objects and arrays with Scratch.BlockType.OBJECT / ARRAY and Scratch.ArgumentType.OBJECT / ARRAY.",
                    template = objectArrayTemplate,
                    testTag = "guide_load_object_array",
                    onLoad = {
                        onLoadTemplate(objectArrayTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val dragDuplicateTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.dragToDuplicateProject.id
                }
                ExampleGuideCard(
                    title = "drag-to-duplicate.js — Duplicate on Drag",
                    description = "Demonstrates duplicateOnDrag: true on a reporter block paired with shadow: 'duplicateReporter' on a command input.",
                    template = dragDuplicateTemplate,
                    testTag = "guide_load_drag_to_duplicate",
                    onLoad = {
                        onLoadTemplate(dragDuplicateTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val simpleExtTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.simpleExtendableProject.id
                }
                ExampleGuideCard(
                    title = "simple-extendable.js — Simple Extendable",
                    description = "Uses Scratch.ArgumentType.EXTENDABLE on hello [INPUTS] and joins all dynamic ABC inputs with util.extendableToArray.",
                    template = simpleExtTemplate,
                    testTag = "guide_load_simple_extendable",
                    onLoad = {
                        onLoadTemplate(simpleExtTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val nestedExtTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.nestedExtendableProject.id
                }
                ExampleGuideCard(
                    title = "nested-extendable.js — Nested Extendables",
                    description = "Two-layer nested extendables (INPUTS → HEY → ABC) with custom '|' / '||' separators and nested util.extendableToArray calls.",
                    template = nestedExtTemplate,
                    testTag = "guide_load_nested_extendable",
                    onLoad = {
                        onLoadTemplate(nestedExtTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val branchesTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.branchesProject.id
                }
                ExampleGuideCard(
                    title = "branches.js — C-Blocks & Loops",
                    description = "Uses CONDITIONAL & LOOP block types with text: ['hello', 'neighbor'], branchCount, and util.startBranch(1, false/true).",
                    template = branchesTemplate,
                    testTag = "guide_load_branches",
                    onLoad = {
                        onLoadTemplate(branchesTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val extraMenuTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.extraMenuPropertiesProject.id
                }
                ExampleGuideCard(
                    title = "extra-menu-properties.js — Separators & Text Menus",
                    description = "Demonstrates '---' menu dividers and acceptText: true editable text dropdowns alongside fixed menus.",
                    template = extraMenuTemplate,
                    testTag = "guide_load_extra_menus",
                    onLoad = {
                        onLoadTemplate(extraMenuTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val dependentTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.dependentDropdownsProject.id
                }
                ExampleGuideCard(
                    title = "dependent-dropdowns.js — Chained Dependent Menus",
                    description = "Demonstrates parentName, optionMapping, and defaultOptions with single-hop and ANIMAL → BREED → COAT chained dropdowns.",
                    template = dependentTemplate,
                    testTag = "guide_load_dependent_dropdowns",
                    onLoad = {
                        onLoadTemplate(dependentTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item {
                val mutatorTemplate = ExtensionTemplates.allTemplates.first {
                    it.project.id == ExtensionTemplates.mutatorDropdownsProject.id
                }
                ExampleGuideCard(
                    title = "mutator-dropdowns.js — Shape & Input Mutators",
                    description = "Demonstrates mutator metadata transforming convert between ARRAY output with STRING input (SPLIT) and REPORTER output with ARRAY input (JOIN).",
                    template = mutatorTemplate,
                    testTag = "guide_load_mutator_dropdowns",
                    onLoad = {
                        onLoadTemplate(mutatorTemplate)
                        onShowMessage(templateLoadedMsg)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    accent: Color,
    points: List<Pair<Boolean, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            points.forEach { (isPositive, text) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (isPositive) EmeraldSafe else CrimsonError,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetBox(code: String) {
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
                text = CodeHighlighter.highlightJs(code),
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ExampleGuideCard(
    title: String,
    description: String,
    template: ExtensionTemplate,
    testTag: String,
    onLoad: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onLoad,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_load_template))
            }
        }
    }
}
