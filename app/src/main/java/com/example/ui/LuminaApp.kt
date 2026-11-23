package com.example.ui

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.gallery.GalleryScreen
import com.example.ui.studio.NegativePromptGuideSheet
import com.example.ui.studio.StudioEffect
import com.example.ui.studio.StudioScreen
import com.example.ui.studio.StudioViewModel
import com.example.ui.theme.LocalThemeController
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private const val ROUTE_STUDIO = "studio"
private const val ROUTE_GALLERY = "gallery"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaApp(
    viewModel: StudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_STUDIO

    val composer by viewModel.composer.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val enhanceState by viewModel.enhanceState.collectAsStateWithLifecycle()
    val inspiratorState by viewModel.inspiratorState.collectAsStateWithLifecycle()
    val savedCreations by viewModel.savedCreations.collectAsStateWithLifecycle()
    val recentGenerations by viewModel.recentGenerations.collectAsStateWithLifecycle()
    val promptHistory by viewModel.promptHistory.collectAsStateWithLifecycle()
    val isExportingPdf by viewModel.isExportingPdf.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSafSourcePath by remember { mutableStateOf<String?>(null) }
    var pendingPdfSourcePath by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onReferencePhotoPicked(uri)
        }
    }

    val safCreateDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val source = pendingSafSourcePath
        pendingSafSourcePath = null
        if (uri != null && source != null) {
            viewModel.completeSafExport(sourcePath = source, destinationUri = uri)
        }
    }

    val pdfCreateDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val source = pendingPdfSourcePath
        pendingPdfSourcePath = null
        if (uri != null && source != null) {
            viewModel.completePdfSave(sourcePath = source, destinationUri = uri)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StudioEffect.ShowMessage -> {
                    launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }

                is StudioEffect.SavedToPublicPictures -> {
                    launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = "View",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(effect.uri, "image/png")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(viewIntent)
                            }.onFailure {
                                snackbarHostState.showSnackbar("No image viewer found on device.")
                            }
                        }
                    }
                }

                is StudioEffect.LaunchSafExport -> {
                    pendingSafSourcePath = effect.sourcePath
                    safCreateDocLauncher.launch(effect.suggestedFileName)
                }

                is StudioEffect.ShareImage -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, effect.uri)
                        putExtra(Intent.EXTRA_TEXT, "Created with Lumina Studio: ${effect.prompt}")
                        clipData = ClipData.newUri(
                            context.contentResolver,
                            "Lumina Studio Artwork",
                            effect.uri
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share AI Artwork").apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(chooser)
                    }.onFailure {
                        snackbarHostState.showSnackbar("No app available to share this image.")
                    }
                }

                is StudioEffect.SharePdf -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, effect.uri)
                        putExtra(Intent.EXTRA_SUBJECT, effect.title)
                        putExtra(Intent.EXTRA_TEXT, "${effect.title} — Created with Lumina Studio")
                        clipData = ClipData.newUri(
                            context.contentResolver,
                            effect.title,
                            effect.uri
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share PDF Portfolio").apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(chooser)
                    }.onFailure {
                        snackbarHostState.showSnackbar("No app available to share this PDF.")
                    }
                }

                is StudioEffect.LaunchPdfSave -> {
                    pendingPdfSourcePath = effect.sourcePath
                    pdfCreateDocLauncher.launch(effect.suggestedFileName)
                }
            }
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(ROUTE_STUDIO) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        Row(
                            modifier = Modifier.padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.lumina_icon_fg_1795415244699),
                                contentDescription = stringResource(R.string.cd_app_logo),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        val themeController = LocalThemeController.current
                        var themeMenuExpanded by remember { mutableStateOf(false) }

                        Box {
                            IconButton(
                                onClick = { themeMenuExpanded = true },
                                modifier = Modifier.testTag("theme_mode_button")
                            ) {
                                Icon(
                                    imageVector = when (themeController.themeMode) {
                                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                                    },
                                    contentDescription = "Theme: ${themeController.themeMode.label}"
                                )
                            }

                            DropdownMenu(
                                expanded = themeMenuExpanded,
                                onDismissRequest = { themeMenuExpanded = false }
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val selected = themeController.themeMode == mode
                                    DropdownMenuItem(
                                        text = { Text(mode.label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (mode) {
                                                    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                                                },
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = if (selected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Active theme mode",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            themeController.setThemeMode(mode)
                                            themeMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setNegativeGuideVisible(true) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("topbar_negative_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = stringResource(R.string.cd_negative_guide),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Neg Guide",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_STUDIO,
                            onClick = { navigateToTab(ROUTE_STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_STUDIO) {
                                        Icons.Rounded.Palette
                                    } else {
                                        Icons.Outlined.Palette
                                    },
                                    contentDescription = stringResource(R.string.nav_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            modifier = Modifier.testTag("nav_studio")
                        )
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_GALLERY,
                            onClick = { navigateToTab(ROUTE_GALLERY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_GALLERY) {
                                        Icons.Rounded.Collections
                                    } else {
                                        Icons.Outlined.Collections
                                    },
                                    contentDescription = stringResource(R.string.nav_gallery)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_gallery)) },
                            modifier = Modifier.testTag("nav_gallery")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_STUDIO,
                            onClick = { navigateToTab(ROUTE_STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_STUDIO) {
                                        Icons.Rounded.Palette
                                    } else {
                                        Icons.Outlined.Palette
                                    },
                                    contentDescription = stringResource(R.string.nav_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            modifier = Modifier.testTag("nav_studio")
                        )
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_GALLERY,
                            onClick = { navigateToTab(ROUTE_GALLERY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_GALLERY) {
                                        Icons.Rounded.Collections
                                    } else {
                                        Icons.Outlined.Collections
                                    },
                                    contentDescription = stringResource(R.string.nav_gallery)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_gallery)) },
                            modifier = Modifier.testTag("nav_gallery")
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDIO,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            composer = composer,
                            generationState = generationState,
                            enhanceState = enhanceState,
                            inspiratorState = inspiratorState,
                            recentGenerations = recentGenerations,
                            promptHistory = promptHistory,
                            onSelectRecent = viewModel::selectRecentGeneration,
                            onRegenerateRecent = viewModel::regenerateFromRecent,
                            onSaveRecent = viewModel::saveRecentGeneration,
                            onRecallPromptHistory = viewModel::recallPromptHistory,
                            onDeletePromptHistory = viewModel::deletePromptHistoryEntry,
                            onClearPromptHistory = viewModel::clearPromptHistory,
                            onSelectInspirationCategory = viewModel::selectInspirationCategory,
                            onShuffleInspiration = viewModel::shufflePromptStarter,
                            onBrainstormInspiration = viewModel::brainstormPromptWithAi,
                            onApplyPromptStarter = viewModel::applyPromptStarter,
                            onModeChange = viewModel::setMode,
                            onPromptChange = viewModel::updatePrompt,
                            onNegativePromptChange = viewModel::updateNegativePrompt,
                            onClearNegativePrompt = viewModel::clearNegativePrompt,
                            onToggleNegativePreset = viewModel::toggleNegativePreset,
                            onOpenNegativeGuide = { viewModel.setNegativeGuideVisible(true) },
                            onSelectStyle = viewModel::selectStyle,
                            onInsertStyleIntoPrompt = viewModel::appendSelectedStyleToPrompt,
                            onSelectAspectRatio = viewModel::selectAspectRatio,
                            onSelectModel = viewModel::selectModel,
                            onSeedInputChange = viewModel::updateSeedInput,
                            onRandomizeSeed = viewModel::randomizeSeed,
                            onClearSeed = viewModel::clearSeed,
                            onPickReferencePhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onClearReferencePhoto = viewModel::clearReferencePhoto,
                            onEnhancePrompt = viewModel::enhancePromptWithAi,
                            onGenerate = viewModel::generateArtwork,
                            onGenerateBatch = viewModel::generateBatchArtworks,
                            onRetryGeneration = viewModel::retryLastGeneration,
                            onCancelGeneration = viewModel::cancelGeneration,
                            onApplyInspiration = viewModel::applyInspiration,
                            onUpscaleResult = viewModel::upscaleCurrentResult,
                            onSaveToLocalGallery = viewModel::saveDraftToLocalGallery,
                            onApplyCrop = viewModel::applyCrop,
                            onResetCrop = viewModel::resetCrop,
                            onSaveResult = viewModel::saveCurrentResultToPhotos,
                            onShareResult = viewModel::shareCurrentResult,
                            onRemixResult = { bitmap -> viewModel.startRemixFromBitmap(bitmap) },
                            onOpenBatchVariantInCanvas = viewModel::openBatchVariantInCanvas,
                            onApplyBatchCrop = viewModel::applyCropToBatchVariant,
                            onSaveBatchVariantToGallery = viewModel::saveBatchVariantToGallery,
                            onSaveAllBatchVariantsToGallery = viewModel::saveAllBatchVariantsToGallery,
                            onSaveBatchVariantToPhotos = viewModel::saveBatchVariantToPhotos,
                            onShareBatchVariant = viewModel::shareBatchVariant,
                            onRetryBatchVariant = viewModel::retryBatchVariant,
                            onResetCanvas = viewModel::resetCanvasToIdle
                        )
                    }

                    composable(ROUTE_GALLERY) {
                        GalleryScreen(
                            creations = savedCreations,
                            isExportingPdf = isExportingPdf,
                            onGoToStudio = { navigateToTab(ROUTE_STUDIO) },
                            onSaveToPhotos = { creation, filter ->
                                viewModel.saveCreationToPhotos(creation.imagePath, filter)
                            },
                            onSaveAsPng = viewModel::exportCreationAsPng,
                            onSaveFilteredCopy = viewModel::saveFilteredCreationCopy,
                            onShare = viewModel::shareCreation,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onAddTagsToSelection = viewModel::addTagsToCreations,
                            onRemoveTag = viewModel::removeTagFromCreation,
                            onExportPdfShare = viewModel::sharePortfolioPdf,
                            onExportPdfSave = viewModel::savePortfolioPdf,
                            onRemix = { creation ->
                                viewModel.startRemixFromCreation(creation)
                                navigateToTab(ROUTE_STUDIO)
                            },
                            onReusePrompts = { creation ->
                                viewModel.reuseCreationPrompts(creation)
                                navigateToTab(ROUTE_STUDIO)
                            },
                            onDelete = viewModel::deleteCreation
                        )
                    }
                }
            }
        }
    }

    if (composer.showNegativeGuide) {
        NegativePromptGuideSheet(
            onDismiss = { viewModel.setNegativeGuideVisible(false) },
            onLoadExample = { example ->
                viewModel.applyNegativeExample(example)
                navigateToTab(ROUTE_STUDIO)
            }
        )
    }
}
