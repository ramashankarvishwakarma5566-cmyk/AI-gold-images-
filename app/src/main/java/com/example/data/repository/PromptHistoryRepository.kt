package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.promptHistoryDataStore by preferencesDataStore(name = "lumina_prompt_history_prefs")

data class PromptHistoryEntry(
    val prompt: String,
    val negativePrompt: String = "",
    val styleName: String = "",
    val aspectRatioLabel: String = "",
    val seed: Int? = null,
    val usedAt: Long = System.currentTimeMillis()
)

object PromptHistoryCodec {
    const val MAX_HISTORY_SIZE = 10

    fun recordEntry(
        existing: List<PromptHistoryEntry>,
        newEntry: PromptHistoryEntry,
        maxSize: Int = MAX_HISTORY_SIZE
    ): List<PromptHistoryEntry> {
        val cleanedPrompt = newEntry.prompt.trim()
        if (cleanedPrompt.isEmpty()) return existing

        val normalized = newEntry.copy(
            prompt = cleanedPrompt,
            negativePrompt = newEntry.negativePrompt.trim()
        )
        val filtered = existing.filterNot {
            it.prompt.trim().equals(cleanedPrompt, ignoreCase = true)
        }
        return (listOf(normalized) + filtered).take(maxSize)
    }

    fun filterEntries(
        entries: List<PromptHistoryEntry>,
        query: String
    ): List<PromptHistoryEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return entries
        return entries.filter { entry ->
            entry.prompt.contains(trimmed, ignoreCase = true) ||
                entry.negativePrompt.contains(trimmed, ignoreCase = true) ||
                entry.styleName.contains(trimmed, ignoreCase = true) ||
                entry.aspectRatioLabel.contains(trimmed, ignoreCase = true) ||
                (entry.seed?.toString()?.contains(trimmed) == true)
        }
    }

    fun encode(entries: List<PromptHistoryEntry>): String {
        val array = JSONArray()
        entries.take(MAX_HISTORY_SIZE).forEach { entry ->
            val obj = JSONObject().apply {
                put("prompt", entry.prompt)
                put("negativePrompt", entry.negativePrompt)
                put("styleName", entry.styleName)
                put("aspectRatioLabel", entry.aspectRatioLabel)
                if (entry.seed != null) {
                    put("seed", entry.seed)
                }
                put("usedAt", entry.usedAt)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decode(raw: String?): List<PromptHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val prompt = obj.optString("prompt").trim()
                    if (prompt.isNotEmpty()) {
                        val parsedSeed = if (obj.has("seed") && !obj.isNull("seed")) {
                            obj.optInt("seed")
                        } else {
                            null
                        }
                        add(
                            PromptHistoryEntry(
                                prompt = prompt,
                                negativePrompt = obj.optString("negativePrompt"),
                                styleName = obj.optString("styleName"),
                                aspectRatioLabel = obj.optString("aspectRatioLabel"),
                                seed = parsedSeed,
                                usedAt = obj.optLong("usedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }.take(MAX_HISTORY_SIZE)
        }.getOrDefault(emptyList())
    }
}

class PromptHistoryRepository(private val context: Context) {

    private val historyKey = stringPreferencesKey("prompt_history_json")

    val history: Flow<List<PromptHistoryEntry>> = context.promptHistoryDataStore.data.map { prefs ->
        PromptHistoryCodec.decode(prefs[historyKey])
    }

    suspend fun recordPrompt(entry: PromptHistoryEntry) {
        if (entry.prompt.isBlank()) return
        context.promptHistoryDataStore.edit { prefs ->
            val current = PromptHistoryCodec.decode(prefs[historyKey])
            val updated = PromptHistoryCodec.recordEntry(current, entry)
            prefs[historyKey] = PromptHistoryCodec.encode(updated)
        }
    }

    suspend fun removePrompt(prompt: String) {
        context.promptHistoryDataStore.edit { prefs ->
            val current = PromptHistoryCodec.decode(prefs[historyKey])
            val updated = current.filterNot {
                it.prompt.trim().equals(prompt.trim(), ignoreCase = true)
            }
            prefs[historyKey] = PromptHistoryCodec.encode(updated)
        }
    }

    suspend fun clearHistory() {
        context.promptHistoryDataStore.edit { prefs ->
            prefs.remove(historyKey)
        }
    }
}
