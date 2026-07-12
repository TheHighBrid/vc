package com.kenza.callsim.schedule

import android.content.Context
import org.json.JSONArray

/** Persists the list of [ScheduledCall]s in SharedPreferences. */
class ScheduleStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("kenza_schedules", Context.MODE_PRIVATE)

    fun all(): List<ScheduledCall> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { ScheduledCall.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun get(id: String): ScheduledCall? = all().firstOrNull { it.id == id }

    fun save(list: List<ScheduledCall>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(call: ScheduledCall) = save(all().filter { it.id != call.id } + call)

    fun remove(id: String) = save(all().filter { it.id != id })

    private companion object {
        const val KEY = "list"
    }
}
