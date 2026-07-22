package com.kenza.callsim.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max

/**
 * Encrypted app-local memory store. It preserves durable memories, compact
 * post-call summaries, editable profiles, plans and unresolved topics. Raw call
 * transcripts are intentionally not persisted.
 */
class MemoryStore(context: Context) {

    private val appContext = context.applicationContext
    private val storage = SecureMemoryStorage(appContext)
    private val legacyPrefs = appContext.getSharedPreferences("kenza_memory", Context.MODE_PRIVATE)

    @Synchronized
    fun snapshot(): MemorySnapshot = readState()

    fun items(): List<MemoryItem> = snapshot().items

    fun callSummaries(): List<CallSummary> = snapshot().calls.sortedByDescending { it.startedAt }

    fun profiles(): PersonalityProfiles = snapshot().profiles

    @Synchronized
    fun saveProfiles(profiles: PersonalityProfiles) {
        val current = readState()
        writeState(current.copy(profiles = profiles.copy(updatedAt = System.currentTimeMillis())))
    }

    /** Compatibility method retained for existing callers. */
    @Synchronized
    fun save(list: List<MemoryItem>) {
        val state = readState()
        writeState(state.copy(items = prune(list)))
    }

    /** Adds memories while merging exact and strong near-duplicates. */
    @Synchronized
    fun addAll(new: List<MemoryItem>): List<String> {
        if (new.isEmpty()) return emptyList()
        val state = readState()
        val existing = state.items.toMutableList()
        val storedIds = mutableListOf<String>()
        val now = System.currentTimeMillis()

        for (candidate in new) {
            val clean = candidate.text.trim().replace(Regex("\\s+"), " ").take(500)
            if (clean.isBlank()) continue
            val incoming = candidate.copy(text = clean, updatedAt = max(candidate.updatedAt, now))
            val index = existing.indexOfFirst { MemoryPolicy.isNearDuplicate(it.text, clean) }
            if (index >= 0) {
                val old = existing[index]
                val merged = old.copy(
                    kind = if (incoming.kind == MemoryKind.CORRECTION) incoming.kind else old.kind,
                    owner = if (old.owner == MemoryOwner.USER) incoming.owner else old.owner,
                    text = if (incoming.kind == MemoryKind.CORRECTION) incoming.text else old.text,
                    updatedAt = now,
                    importance = max(old.importance, incoming.importance),
                    confidence = max(old.confidence, incoming.confidence),
                    dueAt = incoming.dueAt ?: old.dueAt,
                    done = if (incoming.kind == MemoryKind.CORRECTION) incoming.done else old.done,
                    pinned = old.pinned || incoming.pinned,
                    sourceCallId = incoming.sourceCallId ?: old.sourceCallId,
                )
                existing[index] = merged
                storedIds += merged.id
            } else {
                existing += incoming
                storedIds += incoming.id
            }
        }
        writeState(state.copy(items = prune(existing)))
        return storedIds.distinct()
    }

    @Synchronized
    fun addManualMemory(
        text: String,
        owner: MemoryOwner,
        kind: MemoryKind,
        importance: Int = 4,
    ): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val now = System.currentTimeMillis()
        return addAll(
            listOf(
                MemoryItem(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    owner = owner,
                    text = clean,
                    createdAt = now,
                    updatedAt = now,
                    importance = importance.coerceIn(1, 5),
                    confidence = 1.0,
                )
            )
        ).firstOrNull()
    }

    @Synchronized
    fun deleteMemory(id: String) {
        val state = readState()
        writeState(state.copy(items = state.items.filterNot { it.id == id }))
    }

    @Synchronized
    fun togglePinned(id: String) {
        val now = System.currentTimeMillis()
        val state = readState()
        writeState(state.copy(items = state.items.map {
            if (it.id == id) it.copy(pinned = !it.pinned, updatedAt = now) else it
        }))
    }

    @Synchronized
    fun toggleDone(id: String) {
        val now = System.currentTimeMillis()
        val state = readState()
        writeState(state.copy(items = state.items.map {
            if (it.id == id) it.copy(done = !it.done, updatedAt = now) else it
        }))
    }

    /** Starts a pending record that the extractor completes after hang-up. */
    @Synchronized
    fun recordCall(startedAt: Long) {
        val state = readState()
        if (state.calls.any { it.startedAt == startedAt }) return
        val pending = CallSummary(
            id = UUID.randomUUID().toString(),
            startedAt = startedAt,
            endedAt = System.currentTimeMillis(),
            summary = "Creating memory summary…",
            processing = true,
        )
        writeState(state.copy(calls = (state.calls + pending).sortedBy { it.startedAt }.takeLast(MAX_CALLS)))
    }

    @Synchronized
    fun completeLatestCall(
        endedAt: Long,
        summary: String,
        mood: String,
        highlights: List<String>,
        unresolvedTopics: List<String>,
        followUp: String?,
        memoryIds: List<String>,
        error: String? = null,
    ) {
        val state = readState()
        val index = state.calls.indexOfLast { it.processing }
        if (index < 0) return
        val calls = state.calls.toMutableList()
        val old = calls[index]
        calls[index] = old.copy(
            endedAt = endedAt,
            summary = summary.trim().take(1600),
            mood = mood.trim().take(120),
            highlights = highlights.cleanMemoryList(8),
            unresolvedTopics = unresolvedTopics.cleanMemoryList(8),
            followUp = followUp?.trim()?.take(500),
            memoryIds = memoryIds.distinct(),
            processing = false,
            processingError = error,
        )
        writeState(state.copy(calls = calls.sortedBy { it.startedAt }.takeLast(MAX_CALLS)))
    }

    fun callTimes(): List<Long> = snapshot().calls.map { it.startedAt }.filter { it > 0 }.sorted()

    @Synchronized
    fun deleteCall(id: String) {
        val state = readState()
        writeState(state.copy(calls = state.calls.filterNot { it.id == id }))
    }

    @Synchronized
    fun clearAll() {
        storage.clear()
        writeState(MemorySnapshot(updatedAt = System.currentTimeMillis()))
    }

    private fun readState(): MemorySnapshot {
        storage.read()?.let { raw ->
            runCatching { MemorySnapshot.fromJson(JSONObject(raw)) }.getOrNull()?.let { return it }
        }
        val migrated = migrateLegacy()
        storage.write(migrated.toJson().toString())
        return migrated
    }

    private fun writeState(state: MemorySnapshot) {
        storage.write(state.copy(updatedAt = System.currentTimeMillis()).toJson().toString())
    }

    /** Imports the v1 SharedPreferences data on first launch after upgrade. */
    private fun migrateLegacy(): MemorySnapshot {
        val items = runCatching {
            val arr = JSONArray(legacyPrefs.getString("items", "[]"))
            (0 until arr.length()).mapNotNull { index ->
                arr.optJSONObject(index)?.let(MemoryItem::fromJson)
            }
        }.getOrDefault(emptyList())
        val oldTimes = runCatching {
            val arr = JSONArray(legacyPrefs.getString("calls", "[]"))
            (0 until arr.length()).map { arr.optLong(it) }.filter { it > 0 }
        }.getOrDefault(emptyList())
        val calls = oldTimes.takeLast(MAX_CALLS).map { time ->
            CallSummary(
                id = UUID.randomUUID().toString(),
                startedAt = time,
                endedAt = time,
                summary = "Previous call. A detailed summary was not available in the older memory format.",
            )
        }
        return MemorySnapshot(items = prune(items), calls = calls, updatedAt = System.currentTimeMillis())
    }

    private fun prune(items: List<MemoryItem>): List<MemoryItem> {
        val now = System.currentTimeMillis()
        return items
            .filter { it.text.isNotBlank() }
            .sortedByDescending { MemoryPolicy.score(it, now) }
            .take(MAX_MEMORIES)
    }

    private companion object {
        const val MAX_MEMORIES = 400
        const val MAX_CALLS = 60
    }
}
