package com.kenza.callsim.memory

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Turns stored memory + the current moment into a natural-language briefing that
 * is prepended to Kenza's system prompt. This gives her temporal orientation
 * (where "now" sits in the week/month/year/season), an awareness of call habits,
 * and recall of past facts, events and open plans — so she can reflect on the
 * past and reference the future like a real person.
 */
object MemoryContext {

    fun build(store: MemoryStore, contactName: String, now: Long): String {
        val sb = StringBuilder()
        sb.append("\n\n=== YOU JUST KNOW THIS (don't recite; weave it in naturally) ===\n")
        sb.append("NOW: ").append(temporal(now))

        habits(store, now)?.let { sb.append("RHYTHM: ").append(it) }

        val items = store.items()
        recall(items, now).takeIf { it.isNotBlank() }?.let {
            sb.append("REMEMBER ABOUT THEM:\n").append(it)
        }
        plans(items, now).takeIf { it.isNotBlank() }?.let {
            sb.append("OPEN PLANS:\n").append(it)
        }
        sb.append("Reference the past/future when it fits; gently call out things they said they'd do. ")
        sb.append("Never claim to remember anything not listed. Forgetting small stuff is fine.\n")
        sb.append("=== END ===\n")
        return sb.toString()
    }

    // ---- temporal orientation ----

    private fun temporal(now: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val dom = c.get(Calendar.DAY_OF_MONTH)
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val maxDom = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val month = c.get(Calendar.MONTH) // 0-based

        val partOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            in 21..23 -> "late evening"
            else -> "middle of the night"
        }
        val season = when (month) {
            11, 0, 1 -> "winter"
            2, 3, 4 -> "spring"
            5, 6, 7 -> "summer"
            else -> "autumn"
        }
        val monthPhase = when {
            dom <= 5 -> "the very start of the month"
            dom >= maxDom - 4 -> "the end of the month"
            dom in 13..17 -> "the middle of the month"
            else -> "mid-month"
        }
        val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        val dayVibe = when (dow) {
            Calendar.FRIDAY -> "It's Friday — end-of-week, the weekend's basically here, lighter energy."
            Calendar.SATURDAY -> "It's Saturday — proper weekend, relaxed."
            Calendar.SUNDAY -> "It's Sunday — cozy but with that slight Sunday-night before-the-week feeling."
            Calendar.MONDAY -> "It's Monday — fresh start of the week, can feel a bit heavy."
            Calendar.THURSDAY -> "It's Thursday — almost-Friday, the week's winding up."
            else -> "It's a mid-week day."
        }

        val extra = when {
            !isWeekend && hour >= 22 -> " Late weeknight — they've probably got stuff tomorrow."
            isWeekend -> " It's the weekend, so it's easy and unhurried."
            else -> ""
        }
        return "$dayName $dateStr, ~$timeStr ($partOfDay), $season, $monthPhase. $dayVibe$extra\n"
    }

    // ---- call habits ----

    private fun habits(store: MemoryStore, now: Long): String? {
        val times = store.callTimes()
        if (times.isEmpty()) return null
        val sb = StringBuilder()

        val last = times.last()
        val days = ((now - last) / 86_400_000.0).roundToInt()
        sb.append(
            when {
                days <= 0 -> "- You already talked earlier today.\n"
                days == 1 -> "- You last talked yesterday.\n"
                days in 2..6 -> "- You last talked about $days days ago.\n"
                days in 7..13 -> "- It's been about a week since you last talked.\n"
                else -> "- It's been a while — around ${(days / 7.0).roundToInt()} weeks since you last talked.\n"
            }
        )

        if (times.size >= 3) {
            val avgHour = times.map {
                Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY)
            }.average().roundToInt()
            val nowHour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            val usual = SimpleDateFormat("h a", Locale.getDefault()).format(
                Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, avgHour); set(Calendar.MINUTE, 0) }.time
            )
            sb.append("- They usually call around $usual.")
            when {
                nowHour < avgHour - 2 -> sb.append(" This is earlier than usual.\n")
                nowHour > avgHour + 2 -> sb.append(" This is later than usual.\n")
                else -> sb.append("\n")
            }
        }
        return sb.toString()
    }

    // ---- recall ----

    private fun recall(items: List<MemoryItem>, now: Long): String {
        val facts = items.filter { it.kind == MemoryKind.FACT || it.kind == MemoryKind.PREFERENCE }
            .sortedWith(compareByDescending { it.importance })
            .take(12)
        val events = items.filter { it.kind == MemoryKind.EVENT }
            .sortedByDescending { it.createdAt }
            .take(8)

        val sb = StringBuilder()
        facts.forEach { sb.append("- ").append(it.text).append("\n") }
        events.forEach { sb.append("- ").append(relTime(it.createdAt, now)).append(": ").append(it.text).append("\n") }
        return sb.toString()
    }

    private fun plans(items: List<MemoryItem>, now: Long): String {
        val plans = items.filter { it.kind == MemoryKind.PLAN && !it.done }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }
            .take(8)
        val sb = StringBuilder()
        for (p in plans) {
            sb.append("- ").append(p.text)
            p.dueAt?.let { due ->
                val d = ((due - now) / 86_400_000.0).roundToInt()
                sb.append(
                    when {
                        d < -1 -> " (was meant for ${-d} days ago — may not have happened yet)"
                        d in -1..0 -> " (around now)"
                        d == 1 -> " (coming up tomorrow)"
                        d in 2..10 -> " (in about $d days)"
                        else -> " (in a few weeks)"
                    }
                )
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun relTime(then: Long, now: Long): String {
        val days = ((now - then) / 86_400_000.0).roundToInt()
        return when {
            days <= 0 -> "earlier today"
            days == 1 -> "yesterday"
            days in 2..6 -> "$days days ago"
            days in 7..13 -> "about a week ago"
            days in 14..27 -> "about ${(days / 7.0).roundToInt()} weeks ago"
            days in 28..59 -> "about a month ago"
            else -> "about ${(days / 30.0).roundToInt()} months ago"
        }
    }
}
