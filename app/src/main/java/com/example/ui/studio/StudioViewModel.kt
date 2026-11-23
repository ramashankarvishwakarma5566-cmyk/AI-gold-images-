package com.example.ui.studio

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CreationDatabase
import com.example.data.local.CreationEntity
import com.example.data.repository.AiImageRepository
import com.example.data.repository.BatchVariantOutcome
import com.example.data.repository.CreationRepository
import com.example.data.repository.ImageStorageHelper
import com.example.data.repository.PdfLayoutMode
import com.example.data.repository.PdfPageMath
import com.example.data.repository.PdfPortfolioExporter
import com.example.data.repository.PromptHistoryEntry
import com.example.data.repository.PromptHistoryRepository
import com.example.data.repository.UnsupportedApiLevelException
import com.example.model.AspectRatioOption
import com.example.model.ImageFilterPreset
import com.example.model.ImageModelOption
import com.example.model.InspirationCategory
import com.example.model.NegativePromptExample
import com.example.model.NegativePromptPreset
import com.example.model.InspirationTemplate
import com.example.model.PromptStarter
import com.example.model.SeedParseResult
import com.example.model.SeedUtils
import com.example.model.StudioCatalog
import com.example.model.StudioMode
import com.example.model.StylePreset
import com.example.model.TagUtils
import com.example.model.UpscaleFactor
import kotlin.random.Random
import com.google.firebase.ai.type.QuotaExceededException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val TAG = "StudioViewModel"
private const val MAX_RECENT_GENERATIONS = 5
private const val MAX_SESSION_CACHE = 8

data class RecentGenerationItem(
    val key: String,
    val prompt: String,
    val negativePrompt: String,
    val styleName: String,
    val aspectRatioLabel: String,
    val modelId: String,
    val isRemix: Boolean,
    val seed: Int? = null,
    val createdAt: Long,
    val bitmap: Bitmap? = null,
    val savedCreation: CreationEntity? = null
) {
    val isSavedInGallery: Boolean
        get() = savedCreation != null
}

data class BatchVariantItem(
    val index: Int,
    val seed: Int? = null,
    val originalBitmap: Bitmap? = null,
    val bitmap: Bitmap? = null,
    val isCropped: Boolean = false,
    val errorMessage: String? = null,
    val draftKey: String? = null,
    val savedCreation: CreationEntity? = null,
    val isSavingToGallery: Boolean = false,
    val isSavingToPictures: Boolean = false,
    val isRetrying: Boolean = false
) {
    val isReady: Boolean get() = bitmap != null
    val isFailed: Boolean get() = bitmap == null && errorMessage != null && !isRetrying
}

sealed interface GenerationUiState {
    data object Idle : GenerationUiState
    data class Loading(val elapsedSeconds: Int) : GenerationUiState
    data class BatchLoading(
        val elapsedSeconds: Int,
        val completedCount: Int,
        val totalCount: Int = AiImageRepository.DEFAULT_BATCH_SIZE,
        val slotReady: List<Boolean> = List(AiImageRepository.DEFAULT_BATCH_SIZE) { false },
        val slotFailed: List<Boolean> = List(AiImageRepository.DEFAULT_BATCH_SIZE) { false }
    ) : GenerationUiState
    data class Success(
        val originalBitmap: Bitmap,
        val bitmap: Bitmap,
        val prompt: String,
        val negativePrompt: String,
        val styleName: String,
        val aspectRatioLabel: String,
        val modelId: String,
        val isRemix: Boolean,
        val seed: Int? = null,
        val draftKey: String? = null,
        val savedCreation: CreationEntity? = null,
        val isCropped: Boolean = false,
        val upscaleFactor: UpscaleFactor? = null,
        val isUpscaling: Boolean = false,
        val upscalingTarget: UpscaleFactor? = null,
        val isSavingToGallery: Boolean = false,
        val isSavingToPictures: Boolean = false
    ) : GenerationUiState
    data class BatchSuccess(
        val variants: List<BatchVariantItem>,
        val prompt: String,
        val negativePrompt: String,
        val styleName: String,
        val aspectRatioLabel: String,
        val modelId: String,
        val isRemix: Boolean,
        val baseSeed: Int? = null,
        val isSavingAll: Boolean = false
    ) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

sealed interface EnhanceUiState {
    data object Idle : EnhanceUiState
    data object Loading : EnhanceUiState
}

data class PromptInspiratorState(
    val selectedCategory: InspirationCategory = InspirationCategory.ALL,
    val currentStarter: PromptStarter = StudioCatalog.promptStarters.first(),
    val isAiLoading: Boolean = false
)

sealed interface StudioEffect {
    data class ShowMessage(val message: String) : StudioEffect
    data class SavedToPublicPictures(val uri: Uri, val message: String) : StudioEffect
    data class LaunchSafExport(val sourcePath: String, val suggestedFileName: String) : StudioEffect
    data class ShareImage(val uri: Uri, val prompt: String) : StudioEffect
    data class SharePdf(val uri: Uri, val title: String) : StudioEffect
    data class LaunchPdfSave(val sourcePath: String, val suggestedFileName: String) : StudioEffect
}

data class StudioComposerState(
    val mode: StudioMode = StudioMode.TEXT_TO_IMAGE,
    val prompt: String = "",
    val negativePrompt: String = "",
    val selectedStyle: StylePreset = StylePreset.PHOTOREALISTIC,
    val selectedAspectRatio: AspectRatioOption = AspectRatioOption.SQUARE,
    val selectedModel: ImageModelOption = ImageModelOption.NANO_BANANA_2_1,
    val seedInput: String = "",
    val referenceBitmap: Bitmap? = null,
    val showNegativeGuide: Boolean = false
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository = AiImageRepository()
    private val storageHelper = ImageStorageHelper(application)
    private val creationRepository = CreationRepository(
        creationDao = CreationDatabase.getInstance(application).creationDao(),
        storageHelper = storageHelper
    )
    private val promptHistoryRepository = PromptHistoryRepository(application)
    private val pdfExporter = PdfPortfolioExporter(application)

    private val _composer = MutableStateFlow(StudioComposerState())
    val composer: StateFlow<StudioComposerState> = _composer.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _enhanceState = MutableStateFlow<EnhanceUiState>(EnhanceUiState.Idle)
    val enhanceState: StateFlow<EnhanceUiState> = _enhanceState.asStateFlow()

    private val _inspiratorState = MutableStateFlow(PromptInspiratorState())
    val inspiratorState: StateFlow<PromptInspiratorState> = _inspiratorState.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    val savedCreations: StateFlow<List<CreationEntity>> = creationRepository.allCreations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val promptHistory: StateFlow<List<PromptHistoryEntry>> = promptHistoryRepository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _sessionGenerations = MutableStateFlow<List<RecentGenerationItem>>(emptyList())

    val recentGenerations: StateFlow<List<RecentGenerationItem>> =
        combine(_sessionGenerations, savedCreations) { sessionItems, dbItems ->
            val dbById = dbItems.associateBy { it.id }
            val reconciledSession = sessionItems.mapNotNull { item ->
                val saved = item.savedCreation
                if (saved != null) {
                    val latestDb = dbById[saved.id] ?: return@mapNotNull null
                    item.copy(savedCreation = latestDb)
                } else {
                    item
                }
            }
            val coveredDbIds = reconciledSession.mapNotNull { it.savedCreation?.id }.toSet()
            val dbOnly = dbItems
                .filter { it.id !in coveredDbIds }
                .map { entity ->
                    RecentGenerationItem(
                        key = "db_${entity.id}",
                        prompt = entity.prompt,
                        negativePrompt = entity.negativePrompt,
                        styleName = entity.styleName,
                        aspectRatioLabel = entity.aspectRatioLabel,
                        modelId = entity.modelId,
                        isRemix = entity.isRemix,
                        seed = entity.seed,
                        createdAt = entity.createdAt,
                        bitmap = null,
                        savedCreation = entity
                    )
                }
            (reconciledSession + dbOnly)
                .sortedByDescending { it.createdAt }
                .take(MAX_RECENT_GENERATIONS)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _effects = MutableSharedFlow<StudioEffect>()
    val effects: SharedFlow<StudioEffect> = _effects.asSharedFlow()

    private var timerJob: Job? = null
    private var generationJob: Job? = null
    private var lastAttemptWasBatch: Boolean = false
    private val saveMutex = Mutex()

    fun setMode(mode: StudioMode) {
        _composer.update { current ->
            if (mode == StudioMode.TEXT_TO_IMAGE) {
                current.copy(mode = mode)
            } else {
                current.copy(mode = mode)
            }
        }
    }

    fun updatePrompt(value: String) {
        _composer.update { it.copy(prompt = value) }
    }

    fun updateNegativePrompt(value: String) {
        _composer.update { it.copy(negativePrompt = value) }
    }

    fun clearNegativePrompt() {
        _composer.update { it.copy(negativePrompt = "") }
    }

    fun toggleNegativePreset(preset: NegativePromptPreset) {
        _composer.update { current ->
            val tokens = current.negativePrompt
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()

            val presetTokens = preset.keywords
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val allPresent = presetTokens.all { target ->
                tokens.any { it.equals(target, ignoreCase = true) }
            }

            if (allPresent) {
                tokens.removeAll { existing ->
                    presetTokens.any { it.equals(existing, ignoreCase = true) }
                }
            } else {
                presetTokens.forEach { target ->
                    if (tokens.none { it.equals(target, ignoreCase = true) }) {
                        tokens.add(target)
                    }
                }
            }

            current.copy(negativePrompt = tokens.joinToString(", "))
        }
    }

    fun selectStyle(style: StylePreset) {
        _composer.update { it.copy(selectedStyle = style) }
    }

    fun appendSelectedStyleToPrompt() {
        val current = _composer.value
        val suffix = current.selectedStyle.promptSuffix
        if (suffix.isBlank()) return
        if (current.prompt.contains(suffix, ignoreCase = true)) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("${current.selectedStyle.label} modifier is already in your prompt"))
            }
            return
        }
        val updatedPrompt = if (current.prompt.isBlank()) {
            suffix
        } else {
            "${current.prompt.trim().trimEnd(',', '.')}, $suffix"
        }
        _composer.update { it.copy(prompt = updatedPrompt) }
    }

    fun selectAspectRatio(aspectRatio: AspectRatioOption) {
        _composer.update { it.copy(selectedAspectRatio = aspectRatio) }
    }

    fun selectModel(model: ImageModelOption) {
        _composer.update { it.copy(selectedModel = model) }
    }

    fun updateSeedInput(raw: String) {
        val sanitized = SeedUtils.sanitizeDigits(raw)
        _composer.update { it.copy(seedInput = sanitized) }
    }

    fun randomizeSeed() {
        val nextSeed = Random.nextInt(100_000, 999_999_999)
        _composer.update { it.copy(seedInput = nextSeed.toString()) }
    }

    fun clearSeed() {
        _composer.update { it.copy(seedInput = "") }
    }

    fun setNegativeGuideVisible(visible: Boolean) {
        _composer.update { it.copy(showNegativeGuide = visible) }
    }

    fun applyNegativeExample(example: NegativePromptExample) {
        _composer.update {
            it.copy(
                mode = StudioMode.TEXT_TO_IMAGE,
                prompt = example.positivePrompt,
                negativePrompt = example.negativePrompt,
                selectedStyle = example.style,
                showNegativeGuide = false
            )
        }
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShowMessage("Loaded \"${example.title}\" into Studio"))
        }
    }

    fun applyInspiration(template: InspirationTemplate) {
        _composer.update {
            it.copy(
                mode = StudioMode.TEXT_TO_IMAGE,
                prompt = template.prompt,
                negativePrompt = template.negativePrompt,
                selectedStyle = template.style,
                selectedAspectRatio = template.aspectRatio
            )
        }
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShowMessage("Loaded template: ${template.title}"))
        }
    }

    fun selectInspirationCategory(category: InspirationCategory) {
        val pool = if (category == InspirationCategory.ALL) {
            StudioCatalog.promptStarters
        } else {
            StudioCatalog.promptStarters.filter { it.category == category }
                .ifEmpty { StudioCatalog.promptStarters }
        }
        val currentId = _inspiratorState.value.currentStarter.id
        val next = pool.filter { it.id != currentId }.randomOrNull() ?: pool.first()
        _inspiratorState.update {
            it.copy(selectedCategory = category, currentStarter = next)
        }
    }

    fun shufflePromptStarter() {
        val current = _inspiratorState.value
        val pool = if (current.selectedCategory == InspirationCategory.ALL) {
            StudioCatalog.promptStarters
        } else {
            StudioCatalog.promptStarters.filter { it.category == current.selectedCategory }
                .ifEmpty { StudioCatalog.promptStarters }
        }
        val next = pool.filter { it.id != current.currentStarter.id }.randomOrNull() ?: pool.random()
        _inspiratorState.update { it.copy(currentStarter = next) }
    }

    fun brainstormPromptWithAi() {
        val current = _inspiratorState.value
        if (current.isAiLoading) return
        viewModelScope.launch {
            _inspiratorState.update { it.copy(isAiLoading = true) }
            try {
                val aiStarter = aiRepository.generateInspirationIdea(current.selectedCategory)
                _inspiratorState.update { it.copy(currentStarter = aiStarter) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI prompt inspirator timed out", e)
                _effects.emit(StudioEffect.ShowMessage("AI Spark timed out. Showing a curated starter instead."))
                shufflePromptStarter()
            } catch (e: Exception) {
                Log.e(TAG, "AI prompt inspirator failed", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Could not brainstorm with AI right now.")
                )
            } finally {
                _inspiratorState.update { it.copy(isAiLoading = false) }
            }
        }
    }

    fun applyPromptStarter(starter: PromptStarter) {
        _composer.update {
            it.copy(
                mode = StudioMode.TEXT_TO_IMAGE,
                prompt = starter.prompt,
                negativePrompt = starter.negativePrompt,
                selectedStyle = starter.style,
                selectedAspectRatio = starter.aspectRatio
            )
        }
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShowMessage("Loaded \"${starter.title}\" into Studio"))
        }
    }

    fun reuseCreationPrompts(creation: CreationEntity) {
        val style = StylePreset.entries.find { it.label == creation.styleName } ?: StylePreset.NONE
        val ratio = AspectRatioOption.entries.find { it.label == creation.aspectRatioLabel }
            ?: AspectRatioOption.SQUARE
        _composer.update {
            it.copy(
                mode = StudioMode.TEXT_TO_IMAGE,
                prompt = creation.prompt,
                negativePrompt = creation.negativePrompt,
                selectedStyle = style,
                selectedAspectRatio = ratio,
                seedInput = creation.seed?.toString().orEmpty()
            )
        }
        viewModelScope.launch {
            val suffix = if (creation.seed != null) " (seed ${creation.seed})" else ""
            _effects.emit(StudioEffect.ShowMessage("Prompts loaded into Studio$suffix"))
        }
    }

    fun onReferencePhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val bitmap = storageHelper.decodeUriForRemix(uri)
            if (bitmap != null) {
                _composer.update {
                    it.copy(
                        mode = StudioMode.IMAGE_REMIX,
                        referenceBitmap = bitmap
                    )
                }
            } else {
                _effects.emit(StudioEffect.ShowMessage("Could not load selected image."))
            }
        }
    }

    fun clearReferencePhoto() {
        _composer.update { it.copy(referenceBitmap = null) }
    }

    fun startRemixFromBitmap(bitmap: Bitmap, suggestedPrompt: String = "") {
        _composer.update {
            it.copy(
                mode = StudioMode.IMAGE_REMIX,
                referenceBitmap = bitmap,
                prompt = if (suggestedPrompt.isNotBlank()) suggestedPrompt else it.prompt
            )
        }
    }

    fun startRemixFromCreation(creation: CreationEntity) {
        viewModelScope.launch {
            val bitmap = storageHelper.loadBitmapFromPath(creation.imagePath)
            if (bitmap != null) {
                _composer.update {
                    it.copy(
                        mode = StudioMode.IMAGE_REMIX,
                        referenceBitmap = bitmap,
                        prompt = "",
                        negativePrompt = creation.negativePrompt
                    )
                }
                _effects.emit(StudioEffect.ShowMessage("Loaded image for remixing — describe your edit"))
            } else {
                _effects.emit(StudioEffect.ShowMessage("Original image file could not be opened."))
            }
        }
    }

    fun enhancePromptWithAi() {
        val current = _composer.value
        if (current.prompt.isBlank()) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Enter a short idea first, then tap Enhance."))
            }
            return
        }
        if (_enhanceState.value is EnhanceUiState.Loading) return

        viewModelScope.launch {
            _enhanceState.value = EnhanceUiState.Loading
            try {
                val result = aiRepository.enhancePrompt(
                    rawPrompt = current.prompt,
                    currentNegativePrompt = current.negativePrompt,
                    style = current.selectedStyle
                )
                _composer.update {
                    it.copy(
                        prompt = result.enhancedPrompt,
                        negativePrompt = result.suggestedNegativePrompt
                    )
                }
                _effects.emit(StudioEffect.ShowMessage("Refined prompt and negative prompt with Gemini"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Prompt enhancement timed out", e)
                _effects.emit(StudioEffect.ShowMessage("Prompt enhancement timed out. Please try again."))
            } catch (e: Exception) {
                Log.e(TAG, "Prompt enhancement failed", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Prompt enhancement failed.")
                )
            } finally {
                _enhanceState.value = EnhanceUiState.Idle
            }
        }
    }

    fun generateArtwork() {
        val current = _composer.value
        if (current.prompt.isBlank()) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Describe what you want to create first."))
            }
            return
        }
        if (current.mode == StudioMode.IMAGE_REMIX && current.referenceBitmap == null) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Choose a reference photo to remix first."))
            }
            return
        }

        val parsedSeed = when (val seedResult = SeedUtils.parseSeed(current.seedInput)) {
            is SeedParseResult.Empty -> null
            is SeedParseResult.Valid -> seedResult.seed
            is SeedParseResult.Invalid -> {
                viewModelScope.launch {
                    _effects.emit(StudioEffect.ShowMessage(seedResult.error))
                }
                return
            }
        }

        lastAttemptWasBatch = false
        generationJob?.cancel()
        timerJob?.cancel()

        viewModelScope.launch {
            promptHistoryRepository.recordPrompt(
                PromptHistoryEntry(
                    prompt = current.prompt,
                    negativePrompt = current.negativePrompt,
                    styleName = current.selectedStyle.label,
                    aspectRatioLabel = current.selectedAspectRatio.label,
                    seed = parsedSeed
                )
            )
        }

        generationJob = viewModelScope.launch {
            _generationState.value = GenerationUiState.Loading(elapsedSeconds = 0)
            timerJob = launch {
                var seconds = 0
                while (true) {
                    delay(1_000L)
                    seconds += 1
                    if (_generationState.value is GenerationUiState.Loading) {
                        _generationState.value = GenerationUiState.Loading(elapsedSeconds = seconds)
                    }
                }
            }

            try {
                val refBitmap = if (current.mode == StudioMode.IMAGE_REMIX) {
                    current.referenceBitmap
                } else {
                    null
                }

                val generatedBitmap = aiRepository.generateOrRemixImage(
                    prompt = current.prompt,
                    negativePrompt = current.negativePrompt,
                    style = current.selectedStyle,
                    aspectRatio = current.selectedAspectRatio,
                    modelOption = current.selectedModel,
                    referenceBitmap = refBitmap,
                    seed = parsedSeed
                )

                val trimmedPrompt = current.prompt.trim()
                val trimmedNegative = current.negativePrompt.trim()
                val draftKey = "session_${UUID.randomUUID()}"
                val recentEntry = RecentGenerationItem(
                    key = draftKey,
                    prompt = trimmedPrompt,
                    negativePrompt = trimmedNegative,
                    styleName = current.selectedStyle.label,
                    aspectRatioLabel = current.selectedAspectRatio.label,
                    modelId = current.selectedModel.displayName,
                    isRemix = refBitmap != null,
                    seed = parsedSeed,
                    createdAt = System.currentTimeMillis(),
                    bitmap = generatedBitmap,
                    savedCreation = null
                )
                _sessionGenerations.update { list ->
                    (listOf(recentEntry) + list).take(MAX_SESSION_CACHE)
                }

                _generationState.value = GenerationUiState.Success(
                    originalBitmap = generatedBitmap,
                    bitmap = generatedBitmap,
                    prompt = trimmedPrompt,
                    negativePrompt = trimmedNegative,
                    styleName = current.selectedStyle.label,
                    aspectRatioLabel = current.selectedAspectRatio.label,
                    modelId = current.selectedModel.displayName,
                    isRemix = refBitmap != null,
                    seed = parsedSeed,
                    draftKey = draftKey,
                    savedCreation = null,
                    isCropped = false
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image generation timed out", e)
                _generationState.value = GenerationUiState.Error(
                    "Image synthesis timed out after 60 seconds. Please try again."
                )
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Quota exceeded", e)
                _generationState.value = GenerationUiState.Error(
                    "Image generation quota exceeded. Please check your Firebase Blaze plan or wait and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _generationState.value = GenerationUiState.Error(
                    e.localizedMessage ?: "Failed to generate image. Please check connection and App Check."
                )
            } finally {
                timerJob?.cancel()
            }
        }
    }

    fun generateBatchArtworks() {
        val current = _composer.value
        if (current.prompt.isBlank()) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Describe what you want to create first."))
            }
            return
        }
        if (current.mode == StudioMode.IMAGE_REMIX && current.referenceBitmap == null) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Choose a reference photo to remix first."))
            }
            return
        }

        val parsedSeed = when (val seedResult = SeedUtils.parseSeed(current.seedInput)) {
            is SeedParseResult.Empty -> null
            is SeedParseResult.Valid -> seedResult.seed
            is SeedParseResult.Invalid -> {
                viewModelScope.launch {
                    _effects.emit(StudioEffect.ShowMessage(seedResult.error))
                }
                return
            }
        }

        lastAttemptWasBatch = true
        generationJob?.cancel()
        timerJob?.cancel()

        viewModelScope.launch {
            promptHistoryRepository.recordPrompt(
                PromptHistoryEntry(
                    prompt = current.prompt,
                    negativePrompt = current.negativePrompt,
                    styleName = current.selectedStyle.label,
                    aspectRatioLabel = current.selectedAspectRatio.label,
                    seed = parsedSeed
                )
            )
        }

        val batchSize = AiImageRepository.DEFAULT_BATCH_SIZE
        generationJob = viewModelScope.launch {
            _generationState.value = GenerationUiState.BatchLoading(
                elapsedSeconds = 0,
                completedCount = 0,
                totalCount = batchSize
            )
            timerJob = launch {
                var seconds = 0
                while (true) {
                    delay(1_000L)
                    seconds += 1
                    _generationState.update { state ->
                        if (state is GenerationUiState.BatchLoading) {
                            state.copy(elapsedSeconds = seconds)
                        } else {
                            state
                        }
                    }
                }
            }

            try {
                val refBitmap = if (current.mode == StudioMode.IMAGE_REMIX) {
                    current.referenceBitmap
                } else {
                    null
                }

                val outcomes = aiRepository.generateBatchVariants(
                    prompt = current.prompt,
                    negativePrompt = current.negativePrompt,
                    style = current.selectedStyle,
                    aspectRatio = current.selectedAspectRatio,
                    modelOption = current.selectedModel,
                    referenceBitmap = refBitmap,
                    seed = parsedSeed,
                    count = batchSize,
                    onVariantCompleted = { outcome ->
                        _generationState.update { state ->
                            if (state is GenerationUiState.BatchLoading) {
                                val nextReady = state.slotReady.toMutableList()
                                val nextFailed = state.slotFailed.toMutableList()
                                if (outcome.index in nextReady.indices) {
                                    when (outcome) {
                                        is BatchVariantOutcome.Success -> nextReady[outcome.index] = true
                                        is BatchVariantOutcome.Failure -> nextFailed[outcome.index] = true
                                    }
                                }
                                state.copy(
                                    completedCount = (state.completedCount + 1).coerceAtMost(batchSize),
                                    slotReady = nextReady,
                                    slotFailed = nextFailed
                                )
                            } else {
                                state
                            }
                        }
                    }
                )

                val successes = outcomes.filterIsInstance<BatchVariantOutcome.Success>()
                if (successes.isEmpty()) {
                    val firstError = outcomes.filterIsInstance<BatchVariantOutcome.Failure>()
                        .firstOrNull()?.error
                    val message = formatGenerationError(firstError)
                    _generationState.value = GenerationUiState.Error(message)
                    return@launch
                }

                val trimmedPrompt = current.prompt.trim()
                val trimmedNegative = current.negativePrompt.trim()
                val baseTime = System.currentTimeMillis()
                val recentItemsToAdd = mutableListOf<RecentGenerationItem>()

                val variantItems = outcomes.sortedBy { it.index }.map { outcome ->
                    val variantSeed = SeedUtils.deriveVariantSeed(parsedSeed, outcome.index)
                    when (outcome) {
                        is BatchVariantOutcome.Success -> {
                            val draftKey = "session_${UUID.randomUUID()}"
                            recentItemsToAdd.add(
                                RecentGenerationItem(
                                    key = draftKey,
                                    prompt = trimmedPrompt,
                                    negativePrompt = trimmedNegative,
                                    styleName = current.selectedStyle.label,
                                    aspectRatioLabel = current.selectedAspectRatio.label,
                                    modelId = current.selectedModel.displayName,
                                    isRemix = refBitmap != null,
                                    seed = variantSeed,
                                    createdAt = baseTime + (batchSize - outcome.index),
                                    bitmap = outcome.bitmap,
                                    savedCreation = null
                                )
                            )
                            BatchVariantItem(
                                index = outcome.index,
                                seed = variantSeed,
                                originalBitmap = outcome.bitmap,
                                bitmap = outcome.bitmap,
                                draftKey = draftKey
                            )
                        }

                        is BatchVariantOutcome.Failure -> {
                            Log.e(TAG, "Batch variant ${outcome.index + 1} failed", outcome.error)
                            BatchVariantItem(
                                index = outcome.index,
                                seed = variantSeed,
                                errorMessage = formatGenerationError(outcome.error)
                            )
                        }
                    }
                }

                if (recentItemsToAdd.isNotEmpty()) {
                    _sessionGenerations.update { list ->
                        (recentItemsToAdd + list).take(MAX_SESSION_CACHE)
                    }
                }

                _generationState.value = GenerationUiState.BatchSuccess(
                    variants = variantItems,
                    prompt = trimmedPrompt,
                    negativePrompt = trimmedNegative,
                    styleName = current.selectedStyle.label,
                    aspectRatioLabel = current.selectedAspectRatio.label,
                    modelId = current.selectedModel.displayName,
                    isRemix = refBitmap != null,
                    baseSeed = parsedSeed
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Batch generation failed", e)
                _generationState.value = GenerationUiState.Error(formatGenerationError(e))
            } finally {
                timerJob?.cancel()
            }
        }
    }

    private fun formatGenerationError(error: Throwable?): String = when (error) {
        is TimeoutCancellationException ->
            "Image synthesis timed out after 60 seconds. Please try again."
        is QuotaExceededException ->
            "Image generation quota exceeded. Please check your Firebase Blaze plan or wait and try again."
        null -> "Failed to generate image. Please check connection and App Check."
        else -> error.localizedMessage ?: "Failed to generate image. Please check connection and App Check."
    }

    fun retryLastGeneration() {
        if (lastAttemptWasBatch) {
            generateBatchArtworks()
        } else {
            generateArtwork()
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        timerJob?.cancel()
        _generationState.value = GenerationUiState.Idle
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShowMessage("Generation cancelled"))
        }
    }

    fun resetCanvasToIdle() {
        _generationState.value = GenerationUiState.Idle
    }

    fun openBatchVariantInCanvas(index: Int) {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        val variant = batch.variants.find { it.index == index } ?: return
        val bmp = variant.bitmap ?: return
        val origBmp = variant.originalBitmap ?: bmp
        _generationState.value = GenerationUiState.Success(
            originalBitmap = origBmp,
            bitmap = bmp,
            prompt = batch.prompt,
            negativePrompt = batch.negativePrompt,
            styleName = batch.styleName,
            aspectRatioLabel = batch.aspectRatioLabel,
            modelId = batch.modelId,
            isRemix = batch.isRemix,
            seed = variant.seed ?: batch.baseSeed,
            draftKey = variant.draftKey,
            savedCreation = variant.savedCreation,
            isCropped = variant.isCropped
        )
    }

    fun retryBatchVariant(index: Int) {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        val target = batch.variants.find { it.index == index } ?: return
        if (target.isRetrying) return

        val composerSnapshot = _composer.value
        val style = StylePreset.entries.find { it.label == batch.styleName } ?: StylePreset.NONE
        val ratio = AspectRatioOption.entries.find { it.label == batch.aspectRatioLabel }
            ?: AspectRatioOption.SQUARE
        val model = ImageModelOption.entries.find { it.displayName == batch.modelId }
            ?: composerSnapshot.selectedModel
        val refBitmap = if (batch.isRemix) composerSnapshot.referenceBitmap else null
        val variantSeed = target.seed ?: SeedUtils.deriveVariantSeed(batch.baseSeed, index)

        _generationState.update { state ->
            if (state is GenerationUiState.BatchSuccess) {
                state.copy(
                    variants = state.variants.map { item ->
                        if (item.index == index) item.copy(isRetrying = true, errorMessage = null) else item
                    }
                )
            } else {
                state
            }
        }

        viewModelScope.launch {
            try {
                val bitmap = aiRepository.generateOrRemixImage(
                    prompt = batch.prompt,
                    negativePrompt = batch.negativePrompt,
                    style = style,
                    aspectRatio = ratio,
                    modelOption = model,
                    referenceBitmap = refBitmap,
                    seed = variantSeed,
                    variantIndex = index,
                    batchSize = batch.variants.size.coerceAtLeast(AiImageRepository.DEFAULT_BATCH_SIZE)
                )
                val draftKey = "session_${UUID.randomUUID()}"
                val recentEntry = RecentGenerationItem(
                    key = draftKey,
                    prompt = batch.prompt,
                    negativePrompt = batch.negativePrompt,
                    styleName = batch.styleName,
                    aspectRatioLabel = batch.aspectRatioLabel,
                    modelId = batch.modelId,
                    isRemix = batch.isRemix,
                    seed = variantSeed,
                    createdAt = System.currentTimeMillis(),
                    bitmap = bitmap,
                    savedCreation = null
                )
                _sessionGenerations.update { list ->
                    (listOf(recentEntry) + list).take(MAX_SESSION_CACHE)
                }
                _generationState.update { state ->
                    if (state is GenerationUiState.BatchSuccess) {
                        state.copy(
                            variants = state.variants.map { item ->
                                if (item.index == index) {
                                    item.copy(
                                        originalBitmap = bitmap,
                                        bitmap = bitmap,
                                        isCropped = false,
                                        errorMessage = null,
                                        draftKey = draftKey,
                                        savedCreation = null,
                                        isRetrying = false
                                    )
                                } else {
                                    item
                                }
                            }
                        )
                    } else {
                        state
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Retry batch variant ${index + 1} failed", e)
                _generationState.update { state ->
                    if (state is GenerationUiState.BatchSuccess) {
                        state.copy(
                            variants = state.variants.map { item ->
                                if (item.index == index) {
                                    item.copy(
                                        isRetrying = false,
                                        errorMessage = formatGenerationError(e)
                                    )
                                } else {
                                    item
                                }
                            }
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun applyCropToBatchVariant(index: Int, left: Float, top: Float, right: Float, bottom: Float) {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        val variant = batch.variants.find { it.index == index } ?: return
        val sourceBitmap = variant.bitmap ?: return
        viewModelScope.launch {
            val trimmed = storageHelper.cropBitmap(
                source = sourceBitmap,
                leftFraction = left,
                topFraction = top,
                rightFraction = right,
                bottomFraction = bottom
            )
            _generationState.update { state ->
                if (state is GenerationUiState.BatchSuccess) {
                    state.copy(
                        variants = state.variants.map { item ->
                            if (item.index == index) {
                                item.copy(bitmap = trimmed, savedCreation = null, isCropped = true)
                            } else {
                                item
                            }
                        }
                    )
                } else {
                    state
                }
            }
            variant.draftKey?.let { key ->
                _sessionGenerations.update { items ->
                    items.map { item ->
                        if (item.key == key) item.copy(bitmap = trimmed, savedCreation = null) else item
                    }
                }
            }
            _effects.emit(StudioEffect.ShowMessage("Trimmed Variant #${index + 1} to ${trimmed.width}×${trimmed.height} px"))
        }
    }

    fun saveBatchVariantToGallery(index: Int) {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        val variant = batch.variants.find { it.index == index } ?: return
        val bitmap = variant.bitmap ?: return
        if (variant.isSavingToGallery) return
        if (variant.savedCreation != null) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Variant #${index + 1} is already in Atelier Gallery"))
            }
            return
        }

        viewModelScope.launch {
            updateBatchVariant(index) { it.copy(isSavingToGallery = true) }
            try {
                val entity = persistBatchVariantLocked(batch, index, bitmap)
                if (entity != null) {
                    _effects.emit(StudioEffect.ShowMessage("Saved Variant #${index + 1} to Atelier Gallery"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save batch variant", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Could not save variant to gallery.")
                )
            } finally {
                updateBatchVariant(index) { it.copy(isSavingToGallery = false) }
            }
        }
    }

    fun saveAllBatchVariantsToGallery() {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        if (batch.isSavingAll) return
        val unsavedIndices = batch.variants.filter { it.bitmap != null && it.savedCreation == null }
        if (unsavedIndices.isEmpty()) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("All ready variants are already saved to Gallery"))
            }
            return
        }

        viewModelScope.launch {
            _generationState.update { state ->
                if (state is GenerationUiState.BatchSuccess) state.copy(isSavingAll = true) else state
            }
            try {
                var savedCount = 0
                for (item in unsavedIndices) {
                    val bmp = item.bitmap ?: continue
                    val entity = persistBatchVariantLocked(batch, item.index, bmp)
                    if (entity != null) savedCount++
                }
                if (savedCount > 0) {
                    _effects.emit(StudioEffect.ShowMessage("Saved $savedCount variants to Atelier Gallery"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save all batch variants", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Could not save variants to gallery.")
                )
            } finally {
                _generationState.update { state ->
                    if (state is GenerationUiState.BatchSuccess) state.copy(isSavingAll = false) else state
                }
            }
        }
    }

    private suspend fun persistBatchVariantLocked(
        batch: GenerationUiState.BatchSuccess,
        index: Int,
        fallbackBitmap: Bitmap
    ): CreationEntity? = saveMutex.withLock {
        val latestBatch = _generationState.value as? GenerationUiState.BatchSuccess ?: batch
        val latestVariant = latestBatch.variants.find { it.index == index }
        latestVariant?.savedCreation?.let { return@withLock it }
        val targetBitmap = latestVariant?.bitmap ?: fallbackBitmap

        val entity = creationRepository.saveCreation(
            bitmap = targetBitmap,
            prompt = latestBatch.prompt,
            negativePrompt = latestBatch.negativePrompt,
            styleName = latestBatch.styleName,
            aspectRatioLabel = latestBatch.aspectRatioLabel,
            modelId = latestBatch.modelId,
            isRemix = latestBatch.isRemix,
            seed = latestVariant?.seed ?: latestBatch.baseSeed
        )
        updateBatchVariant(index) { it.copy(savedCreation = entity, isSavingToGallery = false) }
        latestVariant?.draftKey?.let { key ->
            _sessionGenerations.update { items ->
                items.map { item ->
                    if (item.key == key) item.copy(savedCreation = entity) else item
                }
            }
        }
        entity
    }

    fun saveBatchVariantToPhotos(index: Int) {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        val variant = batch.variants.find { it.index == index } ?: return
        val bitmap = variant.bitmap ?: return
        if (variant.isSavingToPictures) return

        viewModelScope.launch {
            updateBatchVariant(index) { it.copy(isSavingToPictures = true) }
            try {
                storageHelper.saveBitmapToPublicPictures(bitmap)
                    .onSuccess { result ->
                        _effects.emit(
                            StudioEffect.SavedToPublicPictures(
                                uri = result.uri,
                                message = "Saved Variant #${index + 1} to ${result.displayLocation}"
                            )
                        )
                    }
                    .onFailure { error ->
                        if (error is UnsupportedApiLevelException) {
                            val tempPath = storageHelper.stageBitmapForSafExport(bitmap)
                            _effects.emit(
                                StudioEffect.LaunchSafExport(
                                    sourcePath = tempPath,
                                    suggestedFileName = "Lumina_Var${index + 1}_${System.currentTimeMillis()}.png"
                                )
                            )
                        } else {
                            _effects.emit(
                                StudioEffect.ShowMessage(
                                    error.localizedMessage ?: "Could not save image to Pictures."
                                )
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export batch variant to Pictures", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Could not save image to Pictures.")
                )
            } finally {
                updateBatchVariant(index) { it.copy(isSavingToPictures = false) }
            }
        }
    }

    fun shareBatchVariant(index: Int) {
        val batch = _generationState.value as? GenerationUiState.BatchSuccess ?: return
        val variant = batch.variants.find { it.index == index } ?: return
        val bitmap = variant.bitmap ?: return
        viewModelScope.launch {
            try {
                val entity = persistBatchVariantLocked(batch, index, bitmap) ?: return@launch
                shareCreationInternal(entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist variant before sharing", e)
                _effects.emit(StudioEffect.ShowMessage(e.localizedMessage ?: "Could not share image."))
            }
        }
    }

    private fun updateBatchVariant(
        index: Int,
        transform: (BatchVariantItem) -> BatchVariantItem
    ) {
        _generationState.update { state ->
            if (state is GenerationUiState.BatchSuccess) {
                state.copy(
                    variants = state.variants.map { item ->
                        if (item.index == index) transform(item) else item
                    }
                )
            } else {
                state
            }
        }
    }

    fun applyCrop(left: Float, top: Float, right: Float, bottom: Float) {
        val current = _generationState.value as? GenerationUiState.Success ?: return
        viewModelScope.launch {
            val trimmed = storageHelper.cropBitmap(
                source = current.bitmap,
                leftFraction = left,
                topFraction = top,
                rightFraction = right,
                bottomFraction = bottom
            )
            _generationState.value = current.copy(
                bitmap = trimmed,
                savedCreation = null,
                isCropped = true
            )
            current.draftKey?.let { key ->
                _sessionGenerations.update { items ->
                    items.map { item ->
                        if (item.key == key) item.copy(bitmap = trimmed, savedCreation = null) else item
                    }
                }
            }
            _effects.emit(StudioEffect.ShowMessage("Trimmed to ${trimmed.width}×${trimmed.height} px"))
        }
    }

    fun resetCrop() {
        val current = _generationState.value as? GenerationUiState.Success ?: return
        _generationState.value = current.copy(
            bitmap = current.originalBitmap,
            savedCreation = null,
            isCropped = false,
            upscaleFactor = null
        )
        current.draftKey?.let { key ->
            _sessionGenerations.update { items ->
                items.map { item ->
                    if (item.key == key) {
                        item.copy(bitmap = current.originalBitmap, savedCreation = null)
                    } else {
                        item
                    }
                }
            }
        }
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShowMessage("Restored original generated image"))
        }
    }

    fun upscaleCurrentResult(factor: UpscaleFactor) {
        val current = _generationState.value as? GenerationUiState.Success ?: return
        if (current.isUpscaling) return

        val style = StylePreset.entries.find { it.label == current.styleName } ?: StylePreset.NONE
        val ratio = AspectRatioOption.entries.find { it.label == current.aspectRatioLabel }
            ?: AspectRatioOption.SQUARE
        val model = ImageModelOption.entries.find { it.displayName == current.modelId }
            ?: _composer.value.selectedModel

        viewModelScope.launch {
            _generationState.update { state ->
                if (state is GenerationUiState.Success) {
                    state.copy(isUpscaling = true, upscalingTarget = factor)
                } else {
                    state
                }
            }
            try {
                val upscaledBitmap = aiRepository.upscaleImage(
                    sourceBitmap = current.bitmap,
                    factor = factor,
                    promptHint = current.prompt,
                    style = style,
                    aspectRatio = ratio,
                    modelOption = model
                )
                _generationState.update { state ->
                    if (state is GenerationUiState.Success) {
                        state.copy(
                            bitmap = upscaledBitmap,
                            savedCreation = null,
                            upscaleFactor = factor,
                            isUpscaling = false,
                            upscalingTarget = null
                        )
                    } else {
                        state
                    }
                }
                current.draftKey?.let { key ->
                    _sessionGenerations.update { items ->
                        items.map { item ->
                            if (item.key == key) {
                                item.copy(bitmap = upscaledBitmap, savedCreation = null)
                            } else {
                                item
                            }
                        }
                    }
                }
                _effects.emit(
                    StudioEffect.ShowMessage(
                        "AI Upscaled ${factor.label} to ${upscaledBitmap.width}×${upscaledBitmap.height} px"
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI upscale timed out", e)
                _effects.emit(StudioEffect.ShowMessage("AI upscaling timed out. Please try again."))
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI upscale quota exceeded", e)
                _effects.emit(StudioEffect.ShowMessage("Image upscaling quota exceeded. Please try again later."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI upscale failed", e)
                _effects.emit(StudioEffect.ShowMessage(e.localizedMessage ?: "AI upscaling failed."))
            } finally {
                _generationState.update { state ->
                    if (state is GenerationUiState.Success) {
                        state.copy(isUpscaling = false, upscalingTarget = null)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun saveDraftToLocalGallery() {
        val current = _generationState.value as? GenerationUiState.Success ?: return
        if (current.isSavingToGallery) return
        if (current.savedCreation != null) {
            viewModelScope.launch {
                _effects.emit(StudioEffect.ShowMessage("Already saved to Atelier Gallery"))
            }
            return
        }
        viewModelScope.launch {
            _generationState.update { state ->
                if (state is GenerationUiState.Success) state.copy(isSavingToGallery = true) else state
            }
            try {
                persistDraftState()
                _effects.emit(StudioEffect.ShowMessage("Saved to Atelier Gallery"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save creation to local Room database", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Could not save artwork to gallery.")
                )
            } finally {
                _generationState.update { state ->
                    if (state is GenerationUiState.Success) state.copy(isSavingToGallery = false) else state
                }
            }
        }
    }

    private suspend fun persistDraftState(): CreationEntity? = saveMutex.withLock {
        val latest = _generationState.value as? GenerationUiState.Success ?: return@withLock null
        latest.savedCreation?.let { return@withLock it }

        val entity = creationRepository.saveCreation(
            bitmap = latest.bitmap,
            prompt = latest.prompt,
            negativePrompt = latest.negativePrompt,
            styleName = latest.styleName,
            aspectRatioLabel = latest.aspectRatioLabel,
            modelId = latest.modelId,
            isRemix = latest.isRemix,
            seed = latest.seed
        )
        _generationState.update { state ->
            if (state is GenerationUiState.Success) {
                state.copy(savedCreation = entity, isSavingToGallery = false)
            } else {
                state
            }
        }
        latest.draftKey?.let { key ->
            _sessionGenerations.update { items ->
                items.map { item ->
                    if (item.key == key) item.copy(savedCreation = entity) else item
                }
            }
        }
        entity
    }

    fun saveCurrentResultToPhotos() {
        val current = _generationState.value as? GenerationUiState.Success ?: return
        if (current.isSavingToPictures) return
        viewModelScope.launch {
            _generationState.update { state ->
                if (state is GenerationUiState.Success) state.copy(isSavingToPictures = true) else state
            }
            try {
                storageHelper.saveBitmapToPublicPictures(current.bitmap)
                    .onSuccess { result ->
                        _effects.emit(
                            StudioEffect.SavedToPublicPictures(
                                uri = result.uri,
                                message = "Saved to ${result.displayLocation}"
                            )
                        )
                    }
                    .onFailure { error ->
                        if (error is UnsupportedApiLevelException) {
                            val tempPath = storageHelper.stageBitmapForSafExport(current.bitmap)
                            _effects.emit(
                                StudioEffect.LaunchSafExport(
                                    sourcePath = tempPath,
                                    suggestedFileName = "Lumina_${System.currentTimeMillis()}.png"
                                )
                            )
                        } else {
                            _effects.emit(
                                StudioEffect.ShowMessage(
                                    error.localizedMessage ?: "Could not save image to Pictures."
                                )
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export generated bitmap to Pictures", e)
                _effects.emit(
                    StudioEffect.ShowMessage(e.localizedMessage ?: "Could not save image to Pictures.")
                )
            } finally {
                _generationState.update { state ->
                    if (state is GenerationUiState.Success) state.copy(isSavingToPictures = false) else state
                }
            }
        }
    }

    fun shareCurrentResult() {
        if (_generationState.value !is GenerationUiState.Success) return
        viewModelScope.launch {
            try {
                val entity = persistDraftState() ?: return@launch
                shareCreationInternal(entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist before sharing", e)
                _effects.emit(StudioEffect.ShowMessage(e.localizedMessage ?: "Could not share image."))
            }
        }
    }

    fun saveCreationToPhotos(
        imagePath: String,
        filterPreset: ImageFilterPreset = ImageFilterPreset.ORIGINAL
    ) {
        viewModelScope.launch {
            if (filterPreset == ImageFilterPreset.ORIGINAL) {
                saveCreationToPhotosInternal(imagePath)
            } else {
                exportFilteredBitmapToPictures(
                    imagePath = imagePath,
                    filterPreset = filterPreset,
                    fileNamePrefix = "Lumina_${filterPreset.name.lowercase()}",
                    successPrefix = "Saved ${filterPreset.label} image"
                )
            }
        }
    }

    fun exportCreationAsPng(
        creation: CreationEntity,
        filterPreset: ImageFilterPreset = ImageFilterPreset.ORIGINAL
    ) {
        viewModelScope.launch {
            exportFilteredBitmapToPictures(
                imagePath = creation.imagePath,
                filterPreset = filterPreset,
                fileNamePrefix = "Lumina_PNG_${creation.id}",
                successPrefix = if (filterPreset == ImageFilterPreset.ORIGINAL) {
                    "Saved lossless PNG"
                } else {
                    "Saved ${filterPreset.label} lossless PNG"
                }
            )
        }
    }

    fun saveFilteredCreationCopy(
        creation: CreationEntity,
        filterPreset: ImageFilterPreset
    ) {
        if (filterPreset == ImageFilterPreset.ORIGINAL) return
        viewModelScope.launch {
            try {
                val source = storageHelper.loadBitmapFromPath(creation.imagePath)
                if (source == null) {
                    _effects.emit(StudioEffect.ShowMessage("Original image file could not be opened."))
                    return@launch
                }
                val filtered = storageHelper.applyColorFilter(source, filterPreset)
                val updatedTags = TagUtils.mergeTags(creation.tags, filterPreset.label)
                creationRepository.saveCreation(
                    bitmap = filtered,
                    prompt = creation.prompt,
                    negativePrompt = creation.negativePrompt,
                    styleName = creation.styleName,
                    aspectRatioLabel = creation.aspectRatioLabel,
                    modelId = creation.modelId,
                    isRemix = creation.isRemix,
                    seed = creation.seed,
                    tags = updatedTags
                )
                _effects.emit(StudioEffect.ShowMessage("Saved ${filterPreset.label} copy to Atelier Gallery"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save filtered copy", e)
                _effects.emit(StudioEffect.ShowMessage(e.localizedMessage ?: "Could not save filtered copy."))
            }
        }
    }

    fun addTagsToCreations(creations: List<CreationEntity>, rawTags: String) {
        val parsedNew = TagUtils.parseTags(rawTags)
        if (creations.isEmpty() || parsedNew.isEmpty()) return
        viewModelScope.launch {
            creations.forEach { entity ->
                val merged = TagUtils.mergeTags(entity.tags, rawTags)
                creationRepository.updateTags(entity.id, merged)
            }
            val countWord = if (creations.size == 1) "artwork" else "artworks"
            _effects.emit(
                StudioEffect.ShowMessage(
                    "Added ${parsedNew.size} tag${if (parsedNew.size == 1) "" else "s"} to ${creations.size} $countWord"
                )
            )
        }
    }

    fun removeTagFromCreation(creation: CreationEntity, tag: String) {
        viewModelScope.launch {
            val updated = TagUtils.removeTag(creation.tags, tag)
            creationRepository.updateTags(creation.id, updated)
        }
    }

    private suspend fun exportFilteredBitmapToPictures(
        imagePath: String,
        filterPreset: ImageFilterPreset,
        fileNamePrefix: String,
        successPrefix: String
    ) {
        val raw = storageHelper.loadBitmapFromPath(imagePath)
        if (raw == null) {
            _effects.emit(StudioEffect.ShowMessage("Image file could not be loaded."))
            return
        }
        val bitmapToSave = if (filterPreset == ImageFilterPreset.ORIGINAL) {
            raw
        } else {
            storageHelper.applyColorFilter(raw, filterPreset)
        }
        storageHelper.saveBitmapToPublicPictures(bitmapToSave, fileNamePrefix = fileNamePrefix)
            .onSuccess { result ->
                _effects.emit(
                    StudioEffect.SavedToPublicPictures(
                        uri = result.uri,
                        message = "$successPrefix to ${result.displayLocation}"
                    )
                )
            }
            .onFailure { error ->
                if (error is UnsupportedApiLevelException) {
                    val tempPath = storageHelper.stageBitmapForSafExport(bitmapToSave)
                    _effects.emit(
                        StudioEffect.LaunchSafExport(
                            sourcePath = tempPath,
                            suggestedFileName = "${fileNamePrefix}_${System.currentTimeMillis()}.png"
                        )
                    )
                } else {
                    _effects.emit(
                        StudioEffect.ShowMessage(error.localizedMessage ?: "Could not save PNG image.")
                    )
                }
            }
    }

    private suspend fun saveCreationToPhotosInternal(imagePath: String) {
        storageHelper.saveToDeviceGallery(imagePath)
            .onSuccess { result ->
                _effects.emit(
                    StudioEffect.SavedToPublicPictures(
                        uri = result.uri,
                        message = "Saved to ${result.displayLocation}"
                    )
                )
            }
            .onFailure { error ->
                if (error is UnsupportedApiLevelException) {
                    _effects.emit(
                        StudioEffect.LaunchSafExport(
                            sourcePath = imagePath,
                            suggestedFileName = "Lumina_${System.currentTimeMillis()}.png"
                        )
                    )
                } else {
                    _effects.emit(
                        StudioEffect.ShowMessage(error.localizedMessage ?: "Could not save image.")
                    )
                }
            }
    }

    fun completeSafExport(sourcePath: String, destinationUri: Uri) {
        viewModelScope.launch {
            storageHelper.writeToUri(sourcePath, destinationUri)
                .onSuccess {
                    _effects.emit(StudioEffect.ShowMessage("Image exported successfully."))
                }
                .onFailure { error ->
                    _effects.emit(
                        StudioEffect.ShowMessage(error.localizedMessage ?: "Export failed.")
                    )
                }
        }
    }

    fun shareCreation(
        creation: CreationEntity,
        filterPreset: ImageFilterPreset = ImageFilterPreset.ORIGINAL
    ) {
        viewModelScope.launch {
            shareCreationInternal(creation, filterPreset)
        }
    }

    private suspend fun shareCreationInternal(
        creation: CreationEntity,
        filterPreset: ImageFilterPreset = ImageFilterPreset.ORIGINAL
    ) {
        val uri = if (filterPreset == ImageFilterPreset.ORIGINAL) {
            storageHelper.getShareUri(creation.imagePath)
        } else {
            val source = storageHelper.loadBitmapFromPath(creation.imagePath)
            source?.let { bmp ->
                val filtered = storageHelper.applyColorFilter(bmp, filterPreset)
                storageHelper.getShareUriForBitmap(filtered)
            }
        }
        if (uri != null) {
            _effects.emit(StudioEffect.ShareImage(uri = uri, prompt = creation.prompt))
        } else {
            _effects.emit(StudioEffect.ShowMessage("Image file not found for sharing."))
        }
    }

    fun toggleFavorite(creation: CreationEntity) {
        viewModelScope.launch {
            val nextFavorite = !creation.isFavorite
            creationRepository.setFavorite(creation.id, nextFavorite)
            val currentState = _generationState.value
            if (currentState is GenerationUiState.Success &&
                currentState.savedCreation?.id == creation.id
            ) {
                _generationState.value = currentState.copy(
                    savedCreation = currentState.savedCreation.copy(isFavorite = nextFavorite)
                )
            }
        }
    }

    fun selectRecentGeneration(item: RecentGenerationItem) {
        viewModelScope.launch {
            val loadedBitmap = item.bitmap
                ?: item.savedCreation?.imagePath?.let { storageHelper.loadBitmapFromPath(it) }
            if (loadedBitmap == null) {
                _effects.emit(StudioEffect.ShowMessage("Image file is no longer available."))
                return@launch
            }

            syncComposerFromRecent(item, forRegeneration = false)
            _generationState.value = GenerationUiState.Success(
                originalBitmap = loadedBitmap,
                bitmap = loadedBitmap,
                prompt = item.prompt,
                negativePrompt = item.negativePrompt,
                styleName = item.styleName,
                aspectRatioLabel = item.aspectRatioLabel,
                modelId = item.modelId,
                isRemix = item.isRemix,
                seed = item.seed,
                draftKey = item.key,
                savedCreation = item.savedCreation,
                isCropped = false
            )
        }
    }

    fun regenerateFromRecent(item: RecentGenerationItem) {
        syncComposerFromRecent(item, forRegeneration = true)
        generateArtwork()
    }

    fun saveRecentGeneration(item: RecentGenerationItem) {
        val existingSaved = item.savedCreation
        if (existingSaved != null) {
            saveCreationToPhotos(existingSaved.imagePath)
            return
        }
        val draftBitmap = item.bitmap ?: return
        viewModelScope.launch {
            saveMutex.withLock {
                val alreadySaved = _sessionGenerations.value
                    .find { it.key == item.key }
                    ?.savedCreation
                if (alreadySaved != null) {
                    _effects.emit(StudioEffect.ShowMessage("Already saved to Atelier Gallery"))
                    return@withLock
                }
                val entity = creationRepository.saveCreation(
                    bitmap = draftBitmap,
                    prompt = item.prompt,
                    negativePrompt = item.negativePrompt,
                    styleName = item.styleName,
                    aspectRatioLabel = item.aspectRatioLabel,
                    modelId = item.modelId,
                    isRemix = item.isRemix,
                    seed = item.seed
                )
                _sessionGenerations.update { items ->
                    items.map { current ->
                        if (current.key == item.key) current.copy(savedCreation = entity) else current
                    }
                }
                _generationState.update { state ->
                    when {
                        state is GenerationUiState.Success && state.draftKey == item.key ->
                            state.copy(savedCreation = entity, isSavingToGallery = false)
                        state is GenerationUiState.BatchSuccess ->
                            state.copy(
                                variants = state.variants.map { v ->
                                    if (v.draftKey == item.key) v.copy(savedCreation = entity, isSavingToGallery = false) else v
                                }
                            )
                        else -> state
                    }
                }
                _effects.emit(StudioEffect.ShowMessage("Saved to Atelier Gallery"))
            }
        }
    }

    private fun syncComposerFromRecent(item: RecentGenerationItem, forRegeneration: Boolean) {
        val style = StylePreset.entries.find { it.label == item.styleName } ?: StylePreset.NONE
        val ratio = AspectRatioOption.entries.find { it.label == item.aspectRatioLabel }
            ?: AspectRatioOption.SQUARE
        val model = ImageModelOption.entries.find { it.displayName == item.modelId }
            ?: _composer.value.selectedModel

        _composer.update { current ->
            val targetMode = if (forRegeneration && item.isRemix && current.referenceBitmap == null) {
                StudioMode.TEXT_TO_IMAGE
            } else if (item.isRemix && current.referenceBitmap != null) {
                StudioMode.IMAGE_REMIX
            } else {
                StudioMode.TEXT_TO_IMAGE
            }
            current.copy(
                mode = targetMode,
                prompt = item.prompt,
                negativePrompt = item.negativePrompt,
                selectedStyle = style,
                selectedAspectRatio = ratio,
                selectedModel = model,
                seedInput = item.seed?.toString().orEmpty()
            )
        }
    }

    fun recallPromptHistory(entry: PromptHistoryEntry) {
        val style = StylePreset.entries.find { it.label == entry.styleName }
        val ratio = AspectRatioOption.entries.find { it.label == entry.aspectRatioLabel }
        _composer.update { current ->
            current.copy(
                prompt = entry.prompt,
                negativePrompt = entry.negativePrompt,
                selectedStyle = style ?: current.selectedStyle,
                selectedAspectRatio = ratio ?: current.selectedAspectRatio,
                seedInput = entry.seed?.toString().orEmpty()
            )
        }
        viewModelScope.launch {
            _effects.emit(StudioEffect.ShowMessage("Prompt recalled — edit or generate"))
        }
    }

    fun deletePromptHistoryEntry(entry: PromptHistoryEntry) {
        viewModelScope.launch {
            promptHistoryRepository.removePrompt(entry.prompt)
        }
    }

    fun clearPromptHistory() {
        viewModelScope.launch {
            promptHistoryRepository.clearHistory()
            _effects.emit(StudioEffect.ShowMessage("Prompt history cleared"))
        }
    }

    fun sharePortfolioPdf(
        creations: List<CreationEntity>,
        title: String,
        layoutMode: PdfLayoutMode
    ) {
        exportPortfolioPdfInternal(creations, title, layoutMode, shareImmediately = true)
    }

    fun savePortfolioPdf(
        creations: List<CreationEntity>,
        title: String,
        layoutMode: PdfLayoutMode
    ) {
        exportPortfolioPdfInternal(creations, title, layoutMode, shareImmediately = false)
    }

    private fun exportPortfolioPdfInternal(
        creations: List<CreationEntity>,
        title: String,
        layoutMode: PdfLayoutMode,
        shareImmediately: Boolean
    ) {
        if (creations.isEmpty() || _isExportingPdf.value) return
        viewModelScope.launch {
            _isExportingPdf.value = true
            try {
                val portfolioTitle = title.trim().ifBlank { "Lumina Studio Portfolio" }
                pdfExporter.generatePortfolioPdf(
                    creations = creations,
                    title = portfolioTitle,
                    layoutMode = layoutMode
                ).onSuccess { pdfFile ->
                    if (shareImmediately) {
                        val uri = pdfExporter.getPdfShareUri(pdfFile)
                        _effects.emit(StudioEffect.SharePdf(uri = uri, title = portfolioTitle))
                    } else {
                        val slug = PdfPageMath.sanitizeFileName(portfolioTitle)
                        _effects.emit(
                            StudioEffect.LaunchPdfSave(
                                sourcePath = pdfFile.absolutePath,
                                suggestedFileName = "${slug}.pdf"
                            )
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to generate PDF portfolio", error)
                    _effects.emit(
                        StudioEffect.ShowMessage(error.localizedMessage ?: "PDF export failed.")
                    )
                }
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    fun completePdfSave(sourcePath: String, destinationUri: Uri) {
        viewModelScope.launch {
            pdfExporter.copyPdfToUri(sourcePath, destinationUri)
                .onSuccess {
                    _effects.emit(StudioEffect.ShowMessage("PDF portfolio saved successfully."))
                }
                .onFailure { error ->
                    _effects.emit(
                        StudioEffect.ShowMessage(error.localizedMessage ?: "Could not save PDF file.")
                    )
                }
        }
    }

    fun deleteCreation(creation: CreationEntity) {
        viewModelScope.launch {
            creationRepository.deleteCreation(creation)
            _sessionGenerations.update { items ->
                items.filterNot { it.savedCreation?.id == creation.id }
            }
            val currentState = _generationState.value
            if (currentState is GenerationUiState.Success &&
                currentState.savedCreation?.id == creation.id
            ) {
                _generationState.value = GenerationUiState.Idle
            } else if (currentState is GenerationUiState.BatchSuccess) {
                _generationState.value = currentState.copy(
                    variants = currentState.variants.map { v ->
                        if (v.savedCreation?.id == creation.id) v.copy(savedCreation = null) else v
                    }
                )
            }
            _effects.emit(StudioEffect.ShowMessage("Removed from Gallery"))
        }
    }
}
