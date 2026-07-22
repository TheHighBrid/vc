package com.kenza.callsim.memory

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/** Categories used to organize durable memory across calls. */
enum class MemoryKind {
    FACT, PREFERENCE, EVENT, PLAN, RELATIONSHIP, GOAL, PERSONALITY, CORRECTION
}

enum class MemoryOwner { USER, KENZA, SHARED }

data class MemoryItem(
    val id: String,
    val kind: MemoryKind,
    val text: String,
    val createdAt: Long,
    val importance: Int = 3,
    val dueAt: Long? = null,
    val done: Boolean = false,
    val owner: MemoryOwner = MemoryOwner.USER,
    val updatedAt: Long = createdAt,
    val confidence: Double = 1.0,
    val pinned: Boolean = false,
    val sourceCallId: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("kind", kind.name)
        put("owner", owner.name)
        put("text", text)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("importance", importance.coerceIn(1, 5))
        put("confidence", confidence.coerceIn(0.0, 1.0))
        put("done", done)
        put("pinned", pinned)
        dueAt?.let { put("dueAt", it) }
        sourceCallId?.let { put("sourceCallId", it) }
    }

    companion object {
        fun fromJson(o: JSONObject) = MemoryItem(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            kind = runCatching { MemoryKind.valueOf(o.optString("kind")) }
                .getOrDefault(MemoryKind.FACT),
            owner = runCatching { MemoryOwner.valueOf(o.optString("owner")) }
                .getOrDefault(MemoryOwner.USER),
            text = o.optString("text").trim(),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", o.optLong("createdAt", System.currentTimeMillis())),
            importance = o.optInt("importance", 3).coerceIn(1, 5),
            dueAt = if (o.has("dueAt") && !o.isNull("dueAt")) o.optLong("dueAt") else null,
            done = o.optBoolean("done", false),
            confidence = o.optDouble("confidence", 1.0).coerceIn(0.0, 1.0),
            pinned = o.optBoolean("pinned", false),
            sourceCallId = o.optString("sourceCallId").takeIf { it.isNotBlank() },
        )
    }
}

data class CallSummary(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val summary: String,
    val mood: String = "",
    val highlights: List<String> = emptyList(),
    val unresolvedTopics: List<String> = emptyList(),
    val followUp: String? = null,
    val memoryIds: List<String> = emptyList(),
    val processing: Boolean = false,
    val processingError: String? = null,
) {
    val durationSeconds: Long get() = ((endedAt - startedAt) / 1000L).coerceAtLeast(0)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startedAt", startedAt)
        put("endedAt", endedAt)
        put("summary", summary)
        put("mood", mood)
        put("highlights", JSONArray(highlights))
        put("unresolvedTopics", JSONArray(unresolvedTopics))
        followUp?.let { put("followUp", it) }
        put("memoryIds", JSONArray(memoryIds))
        put("processing", processing)
        processingError?.let { put("processingError", it) }
    }

    companion object {
        fun fromJson(o: JSONObject) = CallSummary(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            startedAt = o.optLong("startedAt"),
            endedAt = o.optLong("endedAt", o.optLong("startedAt")),
            summary = o.optString("summary"),
            mood = o.optString("mood"),
            highlights = o.optJSONArray("highlights").asStrings(),
            unresolvedTopics = o.optJSONArray("unresolvedTopics").asStrings(),
            followUp = o.optString("followUp").takeIf { it.isNotBlank() },
            memoryIds = o.optJSONArray("memoryIds").asStrings(),
            processing = o.optBoolean("processing", false),
            processingError = o.optString("processingError").takeIf { it.isNotBlank() },
        )
    }
}

data class PersonalityProfiles(
    val kenzaProfile: String = "",
    val listenerProfile: String = "",
    val relationshipProfile: String = "",
    val ambitionsAndGoals: String = "",
    val boundariesAndContext: String = "",
    val updatedAt: Long = 0L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("kenzaProfile", kenzaProfile)
        put("listenerProfile", listenerProfile)
        put("relationshipProfile", relationshipProfile)
        put("ambitionsAndGoals", ambitionsAndGoals)
        put("boundariesAndContext", boundariesAndContext)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(o: JSONObject?) = if (o == null) PersonalityProfiles() else PersonalityProfiles(
            kenzaProfile = o.optString("kenzaProfile"),
            listenerProfile = o.optString("listenerProfile"),
            relationshipProfile = o.optString("relationshipProfile"),
            ambitionsAndGoals = o.optString("ambitionsAndGoals"),
            boundariesAndContext = o.optString("boundariesAndContext"),
            updatedAt = o.optLong("updatedAt"),
        )
    }
}

data class MemorySnapshot(
    val items: List<MemoryItem> = emptyList(),
    val calls: List<CallSummary> = emptyList(),
    val profiles: PersonalityProfiles = PersonalityProfiles(),
    val updatedAt: Long = 0L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("schemaVersion", 2)
        put("updatedAt", updatedAt)
        put("items", JSONArray().also { array -> items.forEach { array.put(it.toJson()) } })
        put("calls", JSONArray().also { array -> calls.forEach { array.put(it.toJson()) } })
        put("profiles", profiles.toJson())
    }

    companion object {
        fun fromJson(o: JSONObject) = MemorySnapshot(
            items = o.optJSONArray("items").asObjects(MemoryItem::fromJson),
            calls = o.optJSONArray("calls").asObjects(CallSummary::fromJson),
            profiles = PersonalityProfiles.fromJson(o.optJSONObject("profiles")),
            updatedAt = o.optLong("updatedAt"),
        )
    }
}

object MemoryPolicy {
    fun normalize(text: String): String = text
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9à-ÿ]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun isNearDuplicate(a: String, b: String): Boolean {
        val leftText = normalize(a)
        val rightText = normalize(b)
        if (leftText.isEmpty() || rightText.isEmpty()) return false
        if (leftText == rightText) return true
        val left = leftText.split(' ').filter { it.length > 2 }.toSet()
        val right = rightText.split(' ').filter { it.length > 2 }.toSet()
        if (left.isEmpty() || right.isEmpty()) return false
        return left.intersect(right).size.toDouble() / left.union(right).size.toDouble() >= 0.84
    }

    fun score(item: MemoryItem, now: Long): Double {
        val ageDays = (now - item.updatedAt).coerceAtLeast(0) / 86_400_000.0
        val recency = (30.0 - ageDays.coerceAtMost(30.0)) / 30.0
        return (if (item.pinned) 100.0 else 0.0) +
            item.importance * 12.0 + item.confidence * 8.0 + recency * 5.0 -
            (if (item.done) 10.0 else 0.0)
    }
}

internal fun JSONArray?.asStrings(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optString(index).trim().takeIf { it.isNotBlank() }
    }
}

internal fun <T> JSONArray?.asObjects(mapper: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(mapper) }
}

internal fun List<String>.cleanMemoryList(limit: Int): List<String> = asSequence()
    .map { it.trim().replace(Regex("\\s+"), " ").take(500) }
    .filter { it.isNotBlank() }
    .distinct()
    .take(limit)
    .toList()
