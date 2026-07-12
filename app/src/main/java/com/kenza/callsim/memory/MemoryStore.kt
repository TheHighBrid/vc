package com.kenza.callsim.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class MemoryKind { FACT, PREFERENCE, EVENT, PLAN }

/**
 * One durable thing Kenza remembers. [importance] 1..5 controls what survives
 * pruning and what she leans on. [dueAt] marks a future plan/promise; [done]
 * flags it resolved.
 */
data class MemoryItem(
    val id: String,
    val kind: MemoryKind,
    val text: String,
    val createdAt: Long,
    val importance: Int = 3,
    val dueAt: Long? = null,
    val done: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("kind", kind.name); put("text", text)
        put("createdAt", createdAt); put("importance", importance)
        dueAt?.let { put("dueAt", it) }; put("done", done)
    }

    companion object {
        fun fromJson(o: JSONObject) = MemoryItem(
            id = o.optString("id"),
            kind = runCatching { MemoryKind.valueOf(o.optString("kind")) }.getOrDefault(MemoryKind.FACT),
            text = o.optString("text"),
            createdAt = o.optLong("createdAt"),
            importance = o.optInt("importance", 3),
            dueAt = if (o.has("dueAt")) o.optLong("dueAt") else null,
            done = o.optBoolean("done", false),
        )
    }
}

/**
 * Persistent long-term memory: durable items plus a lightweight call log used
 * to learn call habits (usual time, days since last talk).
 */
class MemoryStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("kenza_memory", Context.MODE_PRIVATE)

    // ---- durable items -------------------------------------------------------

    fun items(): List<MemoryItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { MemoryItem.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun save(list: List<MemoryItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    /** Add items, skipping near-duplicates, keeping the store bounded. */
    @Synchronized
    fun addAll(new: List<MemoryItem>) {
        if (new.isEmpty()) return
        val existing = items().toMutableList()
        val seen = existing.map { it.text.trim().lowercase() }.toMutableSet()
        for (item in new) {
            val k = item.text.trim().lowercase()
            if (k.isEmpty() || k in seen) continue
            seen += k
            existing += item
        }
        // Keep the 200 most useful (importance first, then recency).
        val pruned = existing.sortedWith(
            compareByDescending<MemoryItem> { it.importance }.thenByDescending { it.createdAt }
        ).take(200)
        save(pruned)
    }

    // ---- call log ------------------------------------------------------------

    /** Epoch-millis start times of past calls (most recent last), capped. */
    fun callTimes(): List<Long> {
        val raw = prefs.getString(KEY_CALLS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getLong(it) }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun recordCall(startedAt: Long) {
        val list = (callTimes() + startedAt).takeLast(100)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_CALLS, arr.toString()).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_CALLS = "calls"
    }
}
