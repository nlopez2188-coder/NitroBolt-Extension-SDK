package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BuilderScreen
import com.example.ui.screens.CodeAndTestScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.theme.BlockForgeTheme
import com.example.viewmodel.ExtensionStudioViewModel
import com.example.viewmodel.StudioTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockForgeTheme {
                BlockForgeApp()
            }
        }
    }
}

private data class NavTabItem(
    val tab: StudioTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val studioTabs = listOf(
    NavTabItem(
        tab = StudioTab.BUILDER,
        labelRes = R.string.tab_builder,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        testTag = "nav_builder"
    ),
    NavTabItem(
        tab = StudioTab.CONVERTER,
        labelRes = R.string.tab_converter,
        selectedIcon = Icons.Filled.Transform,
        unselectedIcon = Icons.Outlined.Transform,
        testTag = "nav_converter"
    ),
    NavTabItem(
        tab = StudioTab.CODE_AND_TEST,
        labelRes = R.string.tab_code_test,
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code,
        testTag = "nav_code_test"
    ),
    NavTabItem(
        tab = StudioTab.GUIDE,
        labelRes = R.string.tab_guide,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_guide"
    )
)

@Composable
fun BlockForgeApp(
    viewModel: ExtensionStudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val copiedMsg = stringResource(R.string.msg_copied_clipboard)
    val savedMsg = stringResource(R.string.msg_saved_file)

    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Scratch Extension JS", text))
        showMessage(copiedMsg)
    }

    val shareJs: (String) -> Unit = { code ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${uiState.project.name} (${uiState.project.id}.js)")
            putExtra(Intent.EXTRA_TEXT, code)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scratch Extension"))
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/javascript")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(uiState.generatedJs.toByteArray(Charsets.UTF_8))
                }
                showMessage(savedMsg)
            } catch (_: Exception) {
                showMessage("Unable to write file")
            }
        }
    }

    BackHandler(enabled = uiState.activeTab != StudioTab.BUILDER) {
        viewModel.selectTab(StudioTab.BUILDER)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        studioTabs.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        studioTabs.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Crossfade(
                        targetState = uiState.activeTab,
                        label = "studio_tab_transition"
                    ) { tab ->
                        when (tab) {
                            StudioTab.BUILDER -> BuilderScreen(
                                uiState = uiState,
                                onExecutionModeChange = viewModel::setExecutionMode,
                                onExtensionIdChange = viewModel::updateExtensionId,
                                onExtensionNameChange = viewModel::updateExtensionName,
                                onEnforceGuardChange = viewModel::setEnforceGuard,
                                onBindVmChange = viewModel::setBindVmReference,
                                onApplyColorPreset = viewModel::applyColorPreset,
                                onSaveBlock = viewModel::saveBlock,
                                onDeleteBlock = viewModel::deleteBlock,
                                onMoveBlock = viewModel::moveBlock,
                                onSaveMenu = viewModel::saveMenu,
                                onDeleteMenu = viewModel::deleteMenu,
                                onSaveCustomType = viewModel::saveCustomType,
                                onDeleteCustomType = viewModel::deleteCustomType,
                                onSaveCustomShape = viewModel::saveCustomShape,
                                onDeleteCustomShape = viewModel::deleteCustomShape,
                                onApplyLintFix = viewModel::applyLintFix,
                                onLoadTemplate = viewModel::loadTemplate,
                                onShowMessage = showMessage
                            )

                            StudioTab.CONVERTER -> ConverterScreen(
                                uiState = uiState,
                                onInputChange = viewModel::updateConverterInput,
                                onConvertNow = viewModel::runConversion,
                                onImportToBuilder = viewModel::importConvertedToBuilder,
                                onCopyCode = copyToClipboard,
                                onShowMessage = showMessage
                            )

                            StudioTab.CODE_AND_TEST -> CodeAndTestScreen(
                                uiState = uiState,
                                onCopyCode = copyToClipboard,
                                onExportJsFile = {
                                    val fileName = "${uiState.project.id.ifBlank { "extension" }}.js"
                                    exportLauncher.launch(fileName)
                                },
                                onShareJs = shareJs,
                                onUpdateSimArg = viewModel::updateSimulatorArg,
                                onRunBlock = viewModel::runBlockSimulation,
                                onClearLogs = viewModel::clearSimulationLogs
                            )

                            StudioTab.GUIDE -> GuideScreen(
                                onLoadTemplate = viewModel::loadTemplate,
                                onShowMessage = showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
