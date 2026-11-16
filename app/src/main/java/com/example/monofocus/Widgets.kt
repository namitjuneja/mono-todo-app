package com.example.monofocus

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
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

suspend fun updateAllWidgets(context: Context) {
    ListWidget().updateAll(context)
    FocusWidget().updateAll(context)
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        val dao = AppDatabase.get(context).taskDao()
        dao.getActiveOnce(currentPlaylist(context)).firstOrNull { it.id == id }?.let {
            if (it.playlist == "cleaning") dao.markBankDone(it.id, System.currentTimeMillis())
            else dao.update(it.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        }
        updateAllWidgets(context)
    }
}

class NextTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        val dao = AppDatabase.get(context).taskDao()
        val max = dao.getMaxQueueOrder() ?: System.currentTimeMillis()
        dao.getActiveOnce(currentPlaylist(context)).firstOrNull { it.id == id }?.let { dao.update(it.copy(queueOrder = max + 10L)) }
        updateAllWidgets(context)
    }
}

class ListWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = AppDatabase.get(context).taskDao().getActiveOnce(currentPlaylist(context))
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(WBg).cornerRadius(24.dp).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth().clickable(openApp), verticalAlignment = Alignment.CenterVertically) {
                    Text(currentPlaylist(context).let { k -> if (k == "main") "MONO" else "MONO · " + (PLAYLISTS.firstOrNull { it.first == k }?.second ?: "") }, style = TextStyle(color = ColorProvider(WAccent), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.defaultWeight())
                    if (tasks.isNotEmpty()) Text("${tasks.size}", style = TextStyle(color = ColorProvider(WMuted), fontSize = 12.sp))
                }
                Spacer(GlanceModifier.height(6.dp))
                if (tasks.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize().clickable(openApp), contentAlignment = Alignment.Center) {
                        Text("Nothing to do.", style = TextStyle(color = ColorProvider(WMuted), fontSize = 14.sp))
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(tasks, itemId = { it.id }) { t ->
                            Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_w_circle), contentDescription = "Mark done",
                                    modifier = GlanceModifier.size(32.dp).padding(4.dp).clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(TaskIdKey to t.id)))
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                Text(
                                    t.title + (if (t.focusSessions > 0) "  ⏱${t.focusSessions}" else ""), maxLines = 1, style = TextStyle(color = ColorProvider(WText), fontSize = 15.sp),
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
        val tasks = AppDatabase.get(context).taskDao().getActiveOnce(currentPlaylist(context))
        val current = tasks.firstOrNull()
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        provideContent {
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
