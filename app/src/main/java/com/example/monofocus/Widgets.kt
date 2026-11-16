package com.example.monofocus

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val WBg = Color(0xFF0D0D0D)
private val WText = Color(0xFFF2F2F2)
private val WMuted = Color(0xFF8E8E93)
private val WAccent = Color(0xFFFFEA00)
private val TaskIdKey = ActionParameters.Key<Long>("taskId")

fun currentPlaylist(context: Context): String =
    context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE).getString("playlist", "main") ?: "main"

/** Most recent moment at or before now when the local clock read [minuteOfDay] (minutes after midnight). */
private fun lastBoundary(minuteOfDay: Int, now: Long): Long {
    val c = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, minuteOfDay / 60); set(java.util.Calendar.MINUTE, minuteOfDay % 60); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }
    if (c.timeInMillis > now) c.add(java.util.Calendar.DAY_OF_YEAR, -1)
    return c.timeInMillis
}

/** When a list unchecks itself, as minutes after midnight. Morning defaults to midnight, Night to 6 am. */
fun resetMinutes(context: Context, list: String): Int =
    context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE).getInt("reset_min_$list", if (list == "night") 360 else 0)

fun setResetMinutes(context: Context, list: String, minutes: Int) {
    val prefs = context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE)
    // Moving the time never unchecks anything by itself: the new schedule starts from now.
    prefs.edit().putInt("reset_min_$list", minutes).putLong("reset_$list", lastBoundary(minutes, System.currentTimeMillis())).apply()
}

/** Morning and Night uncheck themselves at their reset times (local). Safe to call as often as you like. */
suspend fun applyDailyResets(context: Context) {
    val prefs = context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE)
    val dao = AppDatabase.get(context).taskDao()
    val now = System.currentTimeMillis()
    for (list in listOf("morning", "night")) {
        val boundary = lastBoundary(resetMinutes(context, list), now)
        val key = "reset_$list"
        val last = prefs.getLong(key, 0L)
        if (last == 0L) { prefs.edit().putLong(key, boundary).apply(); continue }
        if (last < boundary) { dao.uncheckAll(list); prefs.edit().putLong(key, boundary).apply() }
    }
}


private fun playlistFlow(context: Context): Flow<String> = callbackFlow {
    val prefs = context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE)
    val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, k -> if (k == "playlist") trySend(currentPlaylist(context)) }
    prefs.registerOnSharedPreferenceChangeListener(l)
    trySend(currentPlaylist(context))
    awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(l) }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun listFlow(context: Context, active: Boolean): Flow<List<TaskEntity>> {
    val dao = AppDatabase.get(context).taskDao()
    return playlistFlow(context).flatMapLatest { if (active) dao.getActiveQueue(it) else dao.getTodayList(it) }
}

suspend fun updateAllWidgets(context: Context) {
    ListWidget().updateAll(context)
    FocusWidget().updateAll(context)
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        val dao = AppDatabase.get(context).taskDao()
        dao.getActiveOnce("main").firstOrNull { it.id == id }?.let {
            if (it.playlist == "cleaning") dao.markBankDone(it.id, System.currentTimeMillis())
            else dao.update(it.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        }
        updateAllWidgets(context)
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        val dao = AppDatabase.get(context).taskDao()
        val t = dao.observeTask(id).first() ?: return
        if (t.isCompleted) dao.restore(id)
        else if (t.playlist == "cleaning") dao.markBankDone(id, System.currentTimeMillis())
        else dao.update(t.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        updateAllWidgets(context)
    }
}

class NextTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        val dao = AppDatabase.get(context).taskDao()
        val max = dao.getMaxQueueOrder() ?: System.currentTimeMillis()
        dao.getActiveOnce("main").firstOrNull { it.id == id }?.let { dao.update(it.copy(queueOrder = max + 10L)) }
        updateAllWidgets(context)
    }
}

class ListWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        applyDailyResets(context)
        val initial = listFlow(context, active = false).first()
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        provideContent {
            // Collected live, so checking or unchecking in the app (or here) redraws the widget at once.
            val tasks by listFlow(context, active = false).collectAsState(initial)
            val playlistNow by playlistFlow(context).collectAsState(currentPlaylist(context))
            val shown = tasks
            val tasks_left = shown.count { !it.isSeparator && !it.isCompleted }
            Column(
                modifier = GlanceModifier.fillMaxSize().background(WBg).cornerRadius(24.dp).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth().clickable(openApp), verticalAlignment = Alignment.CenterVertically) {
                    Text(playlistNow.let { k -> if (k == "main") "MONO" else "MONO · " + (PLAYLISTS.firstOrNull { it.first == k }?.second ?: "") }, style = TextStyle(color = ColorProvider(WAccent), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.defaultWeight())
                    if (tasks_left > 0) Text("$tasks_left", style = TextStyle(color = ColorProvider(WMuted), fontSize = 12.sp))
                }
                Spacer(GlanceModifier.height(6.dp))
                if (tasks.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize().clickable(openApp), contentAlignment = Alignment.Center) {
                        Text("Nothing to do.", style = TextStyle(color = ColorProvider(WMuted), fontSize = 14.sp))
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(shown, itemId = { it.id }) { t ->
                            if (t.isSeparator) {
                                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (t.title.isNotBlank()) { Text(t.title.uppercase(), maxLines = 1, style = TextStyle(color = ColorProvider(WMuted), fontSize = 10.sp, fontWeight = FontWeight.Medium)); Spacer(GlanceModifier.width(8.dp)) }
                                    Box(modifier = GlanceModifier.defaultWeight().height(1.dp).background(WMuted.copy(alpha = 0.35f))) {}
                                }
                            } else Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(if (t.isCompleted) R.drawable.ic_w_done else R.drawable.ic_w_circle), contentDescription = if (t.isCompleted) "Mark not done" else "Mark done",
                                    modifier = GlanceModifier.size(32.dp).padding(4.dp).clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to t.id)))
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    t.title + (if (t.focusSessions > 0) "  ⏱${t.focusSessions}" else ""), maxLines = 1, style = TextStyle(color = ColorProvider(if (t.isCompleted) WMuted else WText), fontSize = 15.sp),
                                    modifier = GlanceModifier.defaultWeight().clickable(openApp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class FocusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        applyDailyResets(context)
        val initial = AppDatabase.get(context).taskDao().getActiveQueue("main").first()
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        provideContent {
            // The focus widget always follows the Main list, whichever list is open in the app.
            val tasks by AppDatabase.get(context).taskDao().getActiveQueue("main").collectAsState(initial)
            val current = tasks.firstOrNull()
            Column(
                modifier = GlanceModifier.fillMaxSize().background(WBg).cornerRadius(24.dp).padding(16.dp)
            ) {
                if (current == null) {
                    Box(modifier = GlanceModifier.fillMaxSize().clickable(openApp), contentAlignment = Alignment.Center) {
                        Text("All done.", style = TextStyle(color = ColorProvider(WMuted), fontSize = 16.sp))
                    }
                } else {
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth().clickable(openApp), contentAlignment = Alignment.CenterStart) {
                        Text(current.title, maxLines = 3, style = TextStyle(color = ColorProvider(WText), fontSize = 22.sp, fontWeight = FontWeight.Medium))
                    }
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val label = listOfNotNull(
                            if (tasks.size > 1) "${tasks.size} left" else null,
                            if (current.focusSessions > 0) "⏱ ${current.focusSessions} × 2 min" else null
                        ).joinToString("  ·  ")
                        if (label.isNotEmpty()) Text(label, style = TextStyle(color = ColorProvider(WMuted), fontSize = 12.sp))
                        Spacer(GlanceModifier.defaultWeight())
                        if (tasks.size > 1) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_w_next), contentDescription = "Next",
                                modifier = GlanceModifier.size(40.dp).clickable(actionRunCallback<NextTaskAction>(actionParametersOf(TaskIdKey to current.id)))
                            )
                            Spacer(GlanceModifier.width(8.dp))
                        }
                        Image(
                            provider = ImageProvider(R.drawable.ic_w_done), contentDescription = "Done",
                            modifier = GlanceModifier.size(44.dp).clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(TaskIdKey to current.id)))
                        )
                    }
                }
            }
        }
    }
}

class ListWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ListWidget() }
class FocusWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = FocusWidget() }
