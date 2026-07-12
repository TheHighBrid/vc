package com.kenza.callsim.schedule

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

enum class ScheduleKind { ONE_SHOT, WEEKLY, MONTHLY }

/**
 * A user-defined rule for when Kenza should "call". Covers every requested
 * shape by collapsing them into three kinds plus an optional time-of-day window:
 *
 *  - Fixed delay / specific date-time -> ONE_SHOT with [triggerAtMillis].
 *  - "every weekend at 1 PM" / "every Thu & Fri at 1 AM" -> WEEKLY + [daysOfWeek].
 *  - "every 1st of the month between 1:00 and 3:30 PM" -> MONTHLY + [dayOfMonth] + window.
 *
 * When [endMinute] is a valid minute-of-day greater than [startMinute], each
 * occurrence fires at a random time inside that window (so it feels natural
 * rather than clockwork).
 */
data class ScheduledCall(
    val id: String,
    val kind: ScheduleKind,
    val label: String = "",
    val enabled: Boolean = true,
    // ONE_SHOT
    val triggerAtMillis: Long = 0L,
    // WEEKLY — Calendar.SUNDAY(1)..Calendar.SATURDAY(7)
    val daysOfWeek: Set<Int> = emptySet(),
    // MONTHLY
    val dayOfMonth: Int = 1,
    // Time-of-day window, minutes from midnight. endMinute < 0 => exact time at startMinute.
    val startMinute: Int = 13 * 60,
    val endMinute: Int = -1,
) {
    private val hasWindow: Boolean get() = endMinute in 0..(24 * 60) && endMinute > startMinute

    private fun pickMinute(rand: Random): Int =
        if (hasWindow) startMinute + rand.nextInt(endMinute - startMinute + 1) else startMinute

    private fun Calendar.atMinute(m: Int) {
        set(Calendar.HOUR_OF_DAY, m / 60)
        set(Calendar.MINUTE, m % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /** Next fire time strictly after [now], or null if the rule has no future occurrence. */
    fun nextTriggerAfter(now: Long, rand: Random = Random.Default): Long? = when (kind) {
        ScheduleKind.ONE_SHOT -> triggerAtMillis.takeIf { it > now }

        ScheduleKind.WEEKLY -> {
            if (daysOfWeek.isEmpty()) null else {
                var out: Long? = null
                for (offset in 0..7) {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = now
                        add(Calendar.DAY_OF_YEAR, offset)
                    }
                    if (c.get(Calendar.DAY_OF_WEEK) in daysOfWeek) {
                        c.atMinute(pickMinute(rand))
                        if (c.timeInMillis > now) { out = c.timeInMillis; break }
                    }
                }
                out
            }
        }

        ScheduleKind.MONTHLY -> {
            var out: Long? = null
            for (offset in 0..12) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, offset)
                }
                if (dayOfMonth <= c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    c.atMinute(pickMinute(rand))
                    if (c.timeInMillis > now) { out = c.timeInMillis; break }
                }
            }
            out
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("kind", kind.name)
        put("label", label)
        put("enabled", enabled)
        put("triggerAtMillis", triggerAtMillis)
        put("daysOfWeek", JSONArray(daysOfWeek.toList()))
        put("dayOfMonth", dayOfMonth)
        put("startMinute", startMinute)
        put("endMinute", endMinute)
    }

    companion object {
        fun fromJson(o: JSONObject): ScheduledCall {
            val days = mutableSetOf<Int>()
            o.optJSONArray("daysOfWeek")?.let { for (i in 0 until it.length()) days += it.getInt(i) }
            return ScheduledCall(
                id = o.getString("id"),
                kind = ScheduleKind.valueOf(o.optString("kind", "ONE_SHOT")),
                label = o.optString("label"),
                enabled = o.optBoolean("enabled", true),
                triggerAtMillis = o.optLong("triggerAtMillis"),
                daysOfWeek = days,
                dayOfMonth = o.optInt("dayOfMonth", 1),
                startMinute = o.optInt("startMinute", 13 * 60),
                endMinute = o.optInt("endMinute", -1),
            )
        }
    }
}
