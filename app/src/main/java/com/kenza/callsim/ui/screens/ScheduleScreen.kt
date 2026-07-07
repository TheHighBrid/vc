package com.kenza.callsim.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.schedule.CallScheduler
import com.kenza.callsim.schedule.ScheduleKind
import com.kenza.callsim.schedule.ScheduledCall
import com.kenza.callsim.ui.theme.IOSColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") // index0 -> Calendar.SUNDAY(1)

@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scheduler = remember { CallScheduler(context) }
    val items = remember { mutableStateListOf<ScheduledCall>().apply { addAll(scheduler.schedules()) } }
    fun refresh() { items.clear(); items.addAll(scheduler.schedules()) }

    Column(
        Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = IOSColors.Blue,
                modifier = Modifier.size(26.dp).clickable(onClick = onBack))
            Spacer(Modifier.size(12.dp))
            Text("Schedule a call", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        PermissionsSection(scheduler)

        SectionTitle("Quick test — ring after")
        val quick = listOf(
            "5s" to 5_000L, "10s" to 10_000L, "1 min" to 60_000L,
            "10 min" to 600_000L, "30 min" to 1_800_000L, "1 hr" to 3_600_000L,
        )
        WrapRow {
            quick.forEach { (label, ms) ->
                Chip(label) {
                    scheduler.add(oneShot(System.currentTimeMillis() + ms, "In $label"))
                    refresh()
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SpecificDateTimeBuilder(scheduler) { refresh() }
        Spacer(Modifier.height(12.dp))
        RecurringBuilder(scheduler) { refresh() }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Scheduled (${items.size})")
        if (items.isEmpty()) {
            Text("Nothing scheduled yet.", color = IOSColors.SecondaryLabel, fontSize = 14.sp)
        } else {
            items.sortedBy { it.nextTriggerAfter(System.currentTimeMillis()) ?: Long.MAX_VALUE }
                .forEach { call ->
                    ScheduleRow(
                        call = call,
                        onToggle = { scheduler.setEnabled(call.id, it); refresh() },
                        onDelete = { scheduler.remove(call.id); refresh() },
                    )
                }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PermissionsSection(scheduler: CallScheduler) {
    val context = LocalContext.current
    SectionTitle("Permissions")
    if (!scheduler.canScheduleExact()) {
        PermRow("Exact alarms — needed for on-time ringing") {
            CallScheduler.exactAlarmSettingsIntent()?.let { context.startActivity(it) }
        }
    }
    val canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    if (!canOverlay) {
        PermRow("Appear on top — needed to show over the lock screen") {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        }
    }
    if (scheduler.canScheduleExact() && canOverlay) {
        Text("All set — calls can ring on time and over the lock screen.",
            color = IOSColors.SecondaryLabel, fontSize = 13.sp)
    }
    Text("Also disable battery optimization for this app if calls don't fire while idle.",
        color = IOSColors.SecondaryLabel, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SpecificDateTimeBuilder(scheduler: CallScheduler, onAdded: () -> Unit) {
    val context = LocalContext.current
    Card {
        SectionTitle("Specific date & time")
        Chip("Pick date & time…") {
            val now = Calendar.getInstance()
            DatePickerDialog(context, { _, y, mo, d ->
                TimePickerDialog(context, { _, h, min ->
                    val c = Calendar.getInstance().apply {
                        set(y, mo, d, h, min, 0); set(Calendar.MILLISECOND, 0)
                    }
                    scheduler.add(oneShot(c.timeInMillis, "One-time"))
                    onAdded()
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
    }
}

@Composable
private fun RecurringBuilder(scheduler: CallScheduler, onAdded: () -> Unit) {
    val context = LocalContext.current
    var monthly by remember { mutableStateOf(false) }
    val days = remember { mutableStateListOf<Int>() }        // Calendar day-of-week values
    var dayOfMonth by remember { mutableStateOf(1) }
    var startMin by remember { mutableStateOf(13 * 60) }     // default 1:00 PM
    var useWindow by remember { mutableStateOf(false) }
    var endMin by remember { mutableStateOf(15 * 60 + 30) }  // default 3:30 PM

    Card {
        SectionTitle("Recurring")
        Row {
            Chip("Weekly", selected = !monthly) { monthly = false }
            Spacer(Modifier.size(8.dp))
            Chip("Monthly", selected = monthly) { monthly = true }
        }
        Spacer(Modifier.height(8.dp))

        if (!monthly) {
            Text("Days", color = Color.White, fontSize = 13.sp)
            WrapRow {
                DAY_LABELS.forEachIndexed { i, label ->
                    val calDay = i + 1
                    Chip(label, selected = days.contains(calDay)) {
                        if (days.contains(calDay)) days.remove(calDay) else days.add(calDay)
                    }
                }
            }
            Row {
                Chip("Weekdays") { days.clear(); days.addAll(listOf(2, 3, 4, 5, 6)) }
                Spacer(Modifier.size(8.dp))
                Chip("Weekend") { days.clear(); days.addAll(listOf(1, 7)) }
            }
        } else {
            Text("Day of month: $dayOfMonth", color = Color.White, fontSize = 13.sp)
            Row {
                Chip("−") { if (dayOfMonth > 1) dayOfMonth-- }
                Spacer(Modifier.size(8.dp))
                Chip("+") { if (dayOfMonth < 31) dayOfMonth++ }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (useWindow) "From ${minLabel(startMin)}" else "At ${minLabel(startMin)}",
                color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.size(10.dp))
            Chip("Set") {
                TimePickerDialog(context, { _, h, m -> startMin = h * 60 + m },
                    startMin / 60, startMin % 60, false).show()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Random time window", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.size(10.dp))
            Switch(checked = useWindow, onCheckedChange = { useWindow = it })
        }
        if (useWindow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Until ${minLabel(endMin)}", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.size(10.dp))
                Chip("Set") {
                    TimePickerDialog(context, { _, h, m -> endMin = h * 60 + m },
                        endMin / 60, endMin % 60, false).show()
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val valid = if (monthly) true else days.isNotEmpty()
        Chip(if (valid) "Add schedule" else "Pick day(s) first", selected = valid) {
            if (!valid) return@Chip
            val call = ScheduledCall(
                id = UUID.randomUUID().toString(),
                kind = if (monthly) ScheduleKind.MONTHLY else ScheduleKind.WEEKLY,
                label = if (monthly) "Monthly" else "Weekly",
                daysOfWeek = days.toSet(),
                dayOfMonth = dayOfMonth,
                startMinute = startMin,
                endMinute = if (useWindow && endMin > startMin) endMin else -1,
            )
            scheduler.add(call)
            days.clear()
            onAdded()
        }
    }
}

@Composable
private fun ScheduleRow(call: ScheduledCall, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(summarize(call), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            val next = call.nextTriggerAfter(System.currentTimeMillis())
            Text(
                if (!call.enabled) "Off"
                else if (next == null) "No upcoming time"
                else "Next: ${fmtDateTime(next)}",
                color = IOSColors.SecondaryLabel, fontSize = 12.sp
            )
        }
        Switch(checked = call.enabled, onCheckedChange = onToggle)
        Spacer(Modifier.size(8.dp))
        Text("Delete", color = IOSColors.Red, fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onDelete))
    }
}

// ---- small building blocks ----

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
}

@Composable
private fun Card(content: ColumnContent) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E)).padding(14.dp)
    ) { content() }
}

private typealias ColumnContent = @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit

@Composable
private fun WrapRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(18.dp))
            .background(if (selected) IOSColors.Blue else Color(0xFF2C2C2E))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun PermRow(text: String, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Chip("Grant", onClick = onGrant)
    }
}

// ---- helpers ----

private fun oneShot(at: Long, label: String) = ScheduledCall(
    id = UUID.randomUUID().toString(), kind = ScheduleKind.ONE_SHOT, label = label, triggerAtMillis = at
)

private fun minLabel(m: Int): String {
    val c = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.time)
}

private fun fmtDateTime(t: Long): String =
    SimpleDateFormat("EEE MMM d, h:mm a", Locale.getDefault()).format(t)

private fun summarize(call: ScheduledCall): String = when (call.kind) {
    ScheduleKind.ONE_SHOT -> if (call.label.isNotBlank()) call.label else "One-time"
    ScheduleKind.WEEKLY -> {
        val days = call.daysOfWeek.sorted().joinToString(" ") { DAY_LABELS[it - 1] }
        "$days · ${timePart(call)}"
    }
    ScheduleKind.MONTHLY -> "Day ${call.dayOfMonth} · ${timePart(call)}"
}

private fun timePart(call: ScheduledCall): String =
    if (call.endMinute > call.startMinute) "${minLabel(call.startMinute)}–${minLabel(call.endMinute)}"
    else minLabel(call.startMinute)
