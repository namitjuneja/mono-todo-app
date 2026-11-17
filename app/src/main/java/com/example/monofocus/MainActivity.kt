package com.example.monofocus

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.onSizeChanged
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.gestures.awaitEachGesture
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val queueOrder: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val focusSessions: Int = 0,
    val archivedFromList: Boolean = false,
    val playlist: String = "main",
    val doneCount: Int = 0,
    val lastPickedAt: Long = 0L,
    val isSeparator: Boolean = false,
    val archivedAt: Long = 0L
)

@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
    val position: Long = System.currentTimeMillis()
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM subtasks ORDER BY position ASC, id ASC")
    fun getAllSubtasks(): kotlinx.coroutines.flow.Flow<List<SubtaskEntity>>

    @Insert
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("UPDATE tasks SET title = :title, notes = :notes WHERE id = :id")
    suspend fun updateText(id: Long, title: String, notes: String)

    @Query("SELECT * FROM tasks WHERE playlist = :playlist AND archivedFromList = 0 ORDER BY queueOrder ASC")
    fun getTodayList(playlist: String): kotlinx.coroutines.flow.Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET archivedFromList = 1, archivedAt = :now WHERE playlist = :playlist AND archivedFromList = 0 AND isSeparator = 0")
    suspend fun archiveCompleted(playlist: String, now: Long)

    @Query("DELETE FROM tasks WHERE playlist = :playlist AND isSeparator = 1 AND archivedFromList = 0")
    suspend fun deleteSeparators(playlist: String)

    @Query("UPDATE tasks SET archivedFromList = 0, archivedAt = 0 WHERE id IN (:ids)")
    suspend fun unarchive(ids: List<Long>)

    @Query("UPDATE tasks SET archivedFromList = 1, archivedAt = :now WHERE id = :id")
    suspend fun archiveOne(id: Long, now: Long)

    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE playlist = :playlist AND isCompleted = 1 AND archivedFromList = 0")
    suspend fun uncheckAll(playlist: String)

    @Query("UPDATE tasks SET title = :label WHERE id = :id")
    suspend fun setLabel(id: Long, label: String)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): kotlinx.coroutines.flow.Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE playlist = 'cleaning' AND isCompleted = 0 AND archivedFromList = 0 AND isSeparator = 0")
    suspend fun cleaningBank(): List<TaskEntity>

    @Query("UPDATE tasks SET lastPickedAt = :now WHERE id = :id")
    suspend fun markPicked(id: Long, now: Long)

    @Query("UPDATE tasks SET doneCount = doneCount + 1, lastPickedAt = :now WHERE id = :id")
    suspend fun markBankDone(id: Long, now: Long)

    @Query("UPDATE tasks SET queueOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Long)

    /** Renumbers the list with a hole at [index] and inserts [task] into it, all in one transaction so the list only changes once (no flash at the bottom first). */
    @Transaction
    suspend fun insertAtIndex(ids: List<Long>, index: Int, task: TaskEntity): Long {
        val at = index.coerceIn(0, ids.size)
        ids.forEachIndexed { i, id -> updateOrder(id, (if (i < at) i + 1 else i + 2) * 10L) }
        return insert(task.copy(queueOrder = (at + 1) * 10L))
    }

    /** Inserts [tasks] in order at [at]. With [orders] they take those positions; without, the whole list is renumbered around them. */
    @Transaction
    suspend fun insertMany(ids: List<Long>, at: Int, tasks: List<TaskEntity>, orders: List<Long>?): List<Long> {
        if (orders == null) ids.forEachIndexed { i, id -> updateOrder(id, (if (i < at) i + 1 else i + tasks.size + 1) * 10L) }
        return tasks.mapIndexed { k, t -> insert(t.copy(queueOrder = orders?.get(k) ?: ((at + 1 + k) * 10L))) }
    }

    @Transaction
    suspend fun setOrders(ids: List<Long>) {
        ids.forEachIndexed { i, id -> updateOrder(id, (i + 1) * 10L) }
    }

    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL, archivedFromList = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("UPDATE tasks SET focusSessions = focusSessions + 1 WHERE id = :id")
    suspend fun addFocusSession(id: Long)

    @Query("UPDATE tasks SET focusSessions = :count WHERE id = :id")
    suspend fun setFocusSessions(id: Long, count: Int)

    @Query("DELETE FROM subtasks WHERE taskId NOT IN (SELECT id FROM tasks)")
    suspend fun purgeOrphanSubtasks()

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND archivedFromList = 0 AND isSeparator = 0 AND playlist = :playlist ORDER BY queueOrder ASC")
    fun getActiveQueue(playlist: String): kotlinx.coroutines.flow.Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE archivedFromList = 1 ORDER BY archivedAt DESC")
    fun getArchivedTasks(): kotlinx.coroutines.flow.Flow<List<TaskEntity>>

    @Query("SELECT MAX(queueOrder) FROM tasks")
    suspend fun getMaxQueueOrder(): Long?

    @Query("SELECT MIN(queueOrder) FROM tasks WHERE isCompleted = 0 AND isSeparator = 0")
    suspend fun getMinQueueOrder(): Long?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND archivedFromList = 0 AND isSeparator = 0 AND playlist = :playlist ORDER BY queueOrder ASC")
    suspend fun getActiveOnce(playlist: String): List<TaskEntity>

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Update
    suspend fun updateAll(tasks: List<TaskEntity>)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE archivedFromList = 1")
    suspend fun clearArchive()

    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun allTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM subtasks ORDER BY id")
    suspend fun allSubtasksOnce(): List<SubtaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM subtasks")
    suspend fun deleteAllSubtasks()
}

@Database(entities = [TaskEntity::class, SubtaskEntity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monofocus.db"
                ).addMigrations(object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS subtasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskId INTEGER NOT NULL, title TEXT NOT NULL, isDone INTEGER NOT NULL, position INTEGER NOT NULL)")
                    }
                }, object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN focusSessions INTEGER NOT NULL DEFAULT 0")
                    }
                }, object : androidx.room.migration.Migration(3, 4) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN archivedFromList INTEGER NOT NULL DEFAULT 0")
                    }
                }, object : androidx.room.migration.Migration(4, 5) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN playlist TEXT NOT NULL DEFAULT 'main'")
                    }
                }, object : androidx.room.migration.Migration(5, 6) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN doneCount INTEGER NOT NULL DEFAULT 0")
                        db.execSQL("ALTER TABLE tasks ADD COLUMN lastPickedAt INTEGER NOT NULL DEFAULT 0")
                    }
                }, object : androidx.room.migration.Migration(6, 7) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE tasks ADD COLUMN isSeparator INTEGER NOT NULL DEFAULT 0")
                        db.execSQL("ALTER TABLE tasks ADD COLUMN archivedAt INTEGER NOT NULL DEFAULT 0")
                        db.execSQL("UPDATE tasks SET archivedAt = COALESCE(completedAt, ${System.currentTimeMillis()}) WHERE archivedFromList = 1")
                    }
                }).build().also { INSTANCE = it }
            }
        }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val MD_ITEM = Regex("""^\s*(?:(?:[-*+]|\d+[.)])\s+)?(?:\[([ xX])\]\s+)?(.*)$""")
private val MD_RULE = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")

/**
 * Turns pasted text into tasks, one per line. Markdown is understood: bullets (- * + and 1.), checklists (- [ ] / - [x]) and **bold**,
 * ~~strikethrough~~ and `code` markers are stripped. A ticked box or a ~~crossed out~~ item comes back as already done.
 */
fun parseTaskLines(raw: String): List<Pair<String, Boolean>> = raw.lines().mapNotNull { line ->
    if (line.isBlank() || MD_RULE.matches(line)) return@mapNotNull null
    val m = MD_ITEM.matchEntire(line.trimEnd()) ?: return@mapNotNull null
    var done = m.groupValues[1].equals("x", ignoreCase = true)
    var text = m.groupValues[2].trim()
    if (Regex("""~~.+~~""").containsMatchIn(text)) done = true
    text = text.replace("~~", "").replace("**", "").replace("`", "").trim()
    if (text.isEmpty()) null else text to done
}

class TaskViewModel(private val dao: TaskDao, private val prefs: android.content.SharedPreferences) : ViewModel() {
    // A fresh start always opens on the Main list.
    private val _playlist = kotlinx.coroutines.flow.MutableStateFlow("main").also { prefs.edit().putString("playlist", "main").apply() }
    val playlist: StateFlow<String> = _playlist

    fun setPlaylist(key: String) {
        if (key != _playlist.value) {
            _playlist.value = key
            prefs.edit().putString("playlist", key).apply()
        }
    }

    val activeQueue: StateFlow<List<TaskEntity>> = _playlist.flatMapLatest { dao.getActiveQueue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Everything still to do, plus what was finished today. Checked items keep their place in this list.
    private val startOfDay = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayList: StateFlow<List<TaskEntity>> = _playlist.flatMapLatest { dao.getTodayList(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTask: StateFlow<TaskEntity?> = activeQueue.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val archivedTasks: StateFlow<List<TaskEntity>> = dao.getArchivedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtasksByTask: StateFlow<Map<Long, List<SubtaskEntity>>> = dao.getAllSubtasks()
        .map { list -> list.groupBy { it.taskId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun editTask(task: TaskEntity, title: String, notes: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { dao.updateText(task.id, t, notes.trim()) }
    }

    fun addFocusSession(taskId: Long) {
        viewModelScope.launch { dao.addFocusSession(taskId) }
    }

    /** A short-lived "Undo" offer shown after archiving; [undo] puts everything back exactly as it was. */
    data class UndoBanner(val id: Long, val label: String, val undo: suspend () -> Unit)
    var undoBanner by androidx.compose.runtime.mutableStateOf<UndoBanner?>(null)
    private var undoSeq = 0L
    fun dismissUndo() { undoBanner = null }
    fun runUndo() { val b = undoBanner ?: return; undoBanner = null; viewModelScope.launch { b.undo() } }

    fun archiveTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.archiveOne(task.id, System.currentTimeMillis())
            undoBanner = UndoBanner(++undoSeq, "Task archived") { dao.unarchive(listOf(task.id)) }
        }
    }

    fun deleteSeparator(task: TaskEntity) { viewModelScope.launch { dao.delete(task) } }
    fun setSeparatorLabel(task: TaskEntity, label: String) { viewModelScope.launch { dao.setLabel(task.id, label.trim()) } }
    fun addSeparator(label: String) {
        viewModelScope.launch {
            val order = (dao.getMaxQueueOrder() ?: System.currentTimeMillis()) + 10L
            dao.insert(TaskEntity(title = label.trim(), queueOrder = order, playlist = _playlist.value, isSeparator = true))
        }
    }

    fun archiveAll() {
        val before = todayList.value
        val tasks = before.filter { !it.isSeparator }
        if (tasks.isEmpty()) return
        val seps = before.filter { it.isSeparator }
        viewModelScope.launch {
            dao.archiveCompleted(_playlist.value, System.currentTimeMillis())
            dao.deleteSeparators(_playlist.value)   // archiving everything clears the separators too
            undoBanner = UndoBanner(++undoSeq, if (tasks.size == 1) "1 task archived" else "${tasks.size} tasks archived") {
                dao.unarchive(tasks.map { it.id })
                if (seps.isNotEmpty()) dao.insertAll(seps)   // separators come back with their old ids and places
            }
        }
    }

    fun setFocusSessions(taskId: Long, count: Int) {
        viewModelScope.launch { dao.setFocusSessions(taskId, count.coerceIn(0, 99)) }
    }

    fun addSubtask(taskId: Long, title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { dao.insertSubtask(SubtaskEntity(taskId = taskId, title = t)) }
    }

    fun toggleSubtask(sub: SubtaskEntity) {
        viewModelScope.launch { dao.updateSubtask(sub.copy(isDone = !sub.isDone)) }
    }

    fun deleteSubtask(sub: SubtaskEntity) {
        viewModelScope.launch { dao.deleteSubtask(sub) }
    }

    fun addTasks(rawInput: String, notes: String) {
        val lines = parseTaskLines(rawInput)
        if (lines.isEmpty()) return
        viewModelScope.launch {
            val baseOrder = (dao.getMaxQueueOrder() ?: System.currentTimeMillis()) + 10L
            val now = System.currentTimeMillis()
            val entities = lines.mapIndexed { index, (line, done) ->
                TaskEntity(title = line, notes = notes.trim(), queueOrder = baseOrder + (index * 10L), playlist = _playlist.value,
                    isCompleted = done, completedAt = if (done) now else null)
            }
            dao.insertAll(entities)
        }
    }

    /** The rows that were just slotted in by hand, in order; each fades in (one after another) where it belongs. */
    var freshIds by androidx.compose.runtime.mutableStateOf<List<Long>>(emptyList())
    private val insertMutex = kotlinx.coroutines.sync.Mutex()

    /** One or more lines typed in the bar, slotted in right after [afterId] (TOP_SEAM = the very top). [onPlaced] gets the id of the last new row. */
    fun insertAt(afterId: Long, raw: String, onPlaced: (Long) -> Unit) {
        val lines = parseTaskLines(raw)
        if (lines.isEmpty()) return
        val now = System.currentTimeMillis()
        insertBlock(afterId, lines.map { (line, done) ->
            TaskEntity(title = line, playlist = _playlist.value, isCompleted = done, completedAt = if (done) now else null)
        }, onPlaced)
    }

    /** A separator (the typed text is its label) slotted in right after [afterId]. */
    fun insertSeparatorAt(afterId: Long, label: String, onPlaced: (Long) -> Unit) {
        insertBlock(afterId, listOf(TaskEntity(title = label.trim(), playlist = _playlist.value, isSeparator = true)), onPlaced)
    }

    /** Writes [entities] in order into the spot right after [afterId], in one transaction so the list changes once (nothing flashes at the bottom first). */
    private fun insertBlock(afterId: Long, entities: List<TaskEntity>, onPlaced: (Long) -> Unit) {
        viewModelScope.launch {
            val ids = insertMutex.withLock {
                val list = todayList.value
                val at = if (afterId == TOP_SEAM) 0 else list.indexOfFirst { it.id == afterId }.let { if (it >= 0) it + 1 else list.size }
                val n = entities.size
                val lo = list.getOrNull(at - 1)?.queueOrder
                val hi = list.getOrNull(at)?.queueOrder
                val orders: List<Long>? = when {
                    lo != null && hi != null -> if (hi - lo > n) List(n) { k -> lo + (hi - lo) * (k + 1) / (n + 1) } else null   // no whole numbers left: renumber
                    hi != null -> List(n) { k -> hi - 10L * (n - k) }
                    lo != null -> List(n) { k -> lo + 10L * (k + 1) }
                    else -> { val base = (dao.getMaxQueueOrder() ?: System.currentTimeMillis()) + 10L; List(n) { k -> base + 10L * k } }
                }
                dao.insertMany(list.map { it.id }, at, entities, orders).also { freshIds = it }
            }
            // Hand the new spot back once the rows are really in the list, so a quick next add lands after them.
            kotlinx.coroutines.withTimeoutOrNull(800) { todayList.first { l -> l.any { it.id == ids.last() } } }
            onPlaced(ids.last())
            delay(1500)
            if (freshIds === ids) freshIds = emptyList()
        }
    }

    fun addTasksToFront(rawInput: String, notes: String, onReady: () -> Unit) {
        val lines = parseTaskLines(rawInput)
        if (lines.isEmpty()) return
        viewModelScope.launch {
            val currentMin = dao.getMinQueueOrder() ?: System.currentTimeMillis()
            val now = System.currentTimeMillis()
            val entities = lines.mapIndexed { index, (line, done) ->
                TaskEntity(title = line, notes = notes.trim(), queueOrder = currentMin - ((lines.size - index) * 10L),
                    isCompleted = done, completedAt = if (done) now else null)
            }
            dao.insertAll(entities)
            onReady()
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            // The cleaning bank is meant to be repeated, so finishing one counts it but keeps it in the bank.
            if (task.playlist == "cleaning") dao.markBankDone(task.id, System.currentTimeMillis())
            else dao.update(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        }
    }

    fun taskFlow(id: Long): kotlinx.coroutines.flow.Flow<TaskEntity?> = dao.observeTask(id)

    /** One random task from the cleaning bank. Tasks not done lately are favoured; repeats are still possible. */
    suspend fun rollCleaning(avoidId: Long?): TaskEntity? {
        val bank = dao.cleaningBank()
        if (bank.isEmpty()) return null
        val now = System.currentTimeMillis()
        val pool = if (bank.size > 1 && avoidId != null) bank.filter { it.id != avoidId } else bank
        val weights = pool.map { t ->
            var w = 1.0 / (1 + t.doneCount) + (if (t.lastPickedAt == 0L) 1.5 else 0.0)
            if (now - t.lastPickedAt < 30 * 60 * 1000L) w *= 0.25
            w
        }
        var r = kotlin.random.Random.nextDouble() * weights.sum()
        var pick = pool.last()
        for (i in pool.indices) { r -= weights[i]; if (r <= 0) { pick = pool[i]; break } }
        dao.markPicked(pick.id, now)
        return pick
    }

    fun undoCompleteTask(task: TaskEntity) {
        viewModelScope.launch {
            // queueOrder was never touched when the task was completed, so it slots back into its old place.
            dao.restore(task.id)
        }
    }

    fun moveToNextForNow(task: TaskEntity) {
        viewModelScope.launch {
            val maxOrder = dao.getMaxQueueOrder() ?: System.currentTimeMillis()
            dao.update(task.copy(queueOrder = maxOrder + 10L))
        }
    }

    fun reorder(ordered: List<TaskEntity>) {
        viewModelScope.launch {
            dao.setOrders(ordered.map { it.id })
        }
    }

    fun clearArchive() {
        viewModelScope.launch { dao.clearArchive(); dao.purgeOrphanSubtasks() }
    }

    suspend fun exportText(settings: Map<String, String>): String = Backup.export(dao, settings)
    suspend fun importAdd(p: Backup.Parsed) = Backup.importAdd(dao, p)
    suspend fun importReplace(p: Backup.Parsed) = Backup.importReplace(dao, p)
}

class TaskViewModelFactory(private val dao: TaskDao, private val prefs: android.content.SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TaskViewModel(dao, prefs) as T
    }
}

enum class HapticStyle { TICK, CONFIRM, SHATTER }

/** Master switch for vibration, set from Settings. */
object Haptics { var enabled = true }

fun triggerHaptic(context: Context, style: HapticStyle) {
    if (!Haptics.enabled) return
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val effect = when (style) {
            HapticStyle.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticStyle.CONFIRM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            HapticStyle.SHATTER -> VibrationEffect.createWaveform(longArrayOf(0, 25, 45, 35), -1)
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(30)
    }
}

/** The lists you can switch between. Each task belongs to exactly one. */
val PLAYLISTS = listOf("main" to "Main", "morning" to "Morning", "night" to "Night", "cleaning" to "Cleaning")

val OledBlack = Color(0xFF000000)
val OledSurface = Color(0xFF0D0D0D)
val OledBorder = Color(0xFF1F1F1F)
val ElectricYellow = Color(0xFFFFEA00)
val WarmAmberLight = Color(0xFFD99B00)
val OffWhite = Color(0xFFF9F9F8)
val LightSurface = Color(0xFFFFFFFF)

private val OledDarkScheme = darkColorScheme(
    primary = ElectricYellow,
    onPrimary = OledBlack,
    background = OledBlack,
    onBackground = Color.White,
    surface = OledSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = OledBorder
)

private val CleanLightScheme = lightColorScheme(
    primary = WarmAmberLight,
    onPrimary = Color.White,
    background = OffWhite,
    onBackground = Color(0xFF121212),
    surface = LightSurface,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFEFECE6),
    onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFE2E2DF)
)

@Composable
fun MonoFocusTheme(isDark: Boolean, playlist: String = "main", content: @Composable () -> Unit) {
    val scheme = when (playlist) {
        // Morning flips to a light page with dark text; the soft gradient is drawn behind it.
        "morning" -> CleanLightScheme.copy(background = Color(0xFFFFF0A3), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF111111), onSurface = Color(0xFF111111))
        "night", "cleaning" -> OledDarkScheme
        else -> if (isDark) OledDarkScheme else CleanLightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val db = remember { AppDatabase.get(context) }
            val viewModel: TaskViewModel = viewModel(factory = TaskViewModelFactory(db.taskDao(), context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE)))
            val themePrefs = remember { context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE) }
            var isOledDark by remember { mutableStateOf(themePrefs.getBoolean("dark", true)) }

            val playlistKey by viewModel.playlist.collectAsState()
            MonoFocusTheme(isDark = isOledDark, playlist = playlistKey) {
                MonoFocusApp(viewModel = viewModel, isOledDark = isOledDark, onToggleTheme = { isOledDark = !isOledDark; themePrefs.edit().putBoolean("dark", isOledDark).apply() })
            }
        }
    }
}

@Composable
fun LockedDisplayEffect(active: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(active) {
            val activity = view.context as Activity
            activity.setLockedDisplay(active)
            onDispose { activity.setLockedDisplay(false) }
        }
    }
}

@Composable
fun SystemBarsAndWakeEffect(immersiveFullScreen: Boolean, keepScreenOn: Boolean, isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(immersiveFullScreen, keepScreenOn, isDark) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark

            if (immersiveFullScreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }

            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

enum class ScreenState { HOME, FOCUS, ARCHIVE, SETTINGS }

@Composable
fun MonoFocusApp(viewModel: TaskViewModel, isOledDark: Boolean, onToggleTheme: () -> Unit) {
    var screen by rememberSaveable { mutableStateOf(ScreenState.HOME) }
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusTitleY by rememberSaveable { mutableFloatStateOf(420f) }
    var returnId by remember { mutableStateOf<Long?>(null) }
    var diceSession by rememberSaveable { mutableStateOf(false) }
    var rolledTask by remember { mutableStateOf<TaskEntity?>(null) }
    // Finishing a task: yellow wash -> back to the list -> the next task grows into focus as if tapped.
    val appScope = rememberCoroutineScope()
    var winVisible by remember { mutableStateOf(false) }
    var winCenter by remember { mutableStateOf(Offset.Zero) }
    var pendingUndo by remember { mutableStateOf<TaskEntity?>(null) }
    var autoSelectId by remember { mutableStateOf<Long?>(null) }
    var keepScreenOn by rememberSaveable { mutableStateOf(false) }
    // Set when finishing a task carries focus into a new section: the separator title ("" when it has none), shown briefly on the next task.
    var sectionBanner by remember { mutableStateOf<String?>(null) }
    // A meditation/focus timer lives here so it keeps running (and rings) wherever you are in the app.
    var medTimer by remember { mutableStateOf<MedTimer?>(null) }
    val bellCtx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(medTimer?.endsAt, medTimer?.done) {
        val t = medTimer ?: return@LaunchedEffect
        if (t.done) return@LaunchedEffect
        val wait = t.endsAt - android.os.SystemClock.elapsedRealtime()
        if (wait > 0) delay(wait)
        medTimer = t.copy(done = true)
        repeat(3) { i ->
            if (i > 0) delay(3200)
            triggerHaptic(bellCtx, HapticStyle.CONFIRM)
            Sfx.play(Sound.BELL)
        }
    }

    val activeQueue by viewModel.activeQueue.collectAsState()
    val todayList by viewModel.todayList.collectAsState()
    // What next/previous walks through: open tasks plus the separators between them, so focus always pauses at a separator.
    val focusSeq = todayList.filter { it.isSeparator || !it.isCompleted }
    val focusIndex = activeQueue.indexOfFirst { it.id == focusedId }
    // Looked up by id so a task rolled from the cleaning bank can be focused even though it is not in the current list.
    val focusFlow = remember(focusedId) { focusedId?.let { viewModel.taskFlow(it) } ?: kotlinx.coroutines.flow.flowOf<TaskEntity?>(null) }
    val focusById by focusFlow.collectAsState(initial = null)
    val focusTask = (focusById?.takeIf { it.id == focusedId } ?: activeQueue.firstOrNull { it.id == focusedId } ?: rolledTask?.takeIf { it.id == focusedId })
        ?.takeIf { !it.isCompleted && !it.archivedFromList }
    val archivedTasks by viewModel.archivedTasks.collectAsState()
    val subtasksByTask by viewModel.subtasksByTask.collectAsState()
    val editor = remember(viewModel) { EditorActions(viewModel::editTask, viewModel::addSubtask, viewModel::toggleSubtask, viewModel::deleteSubtask, viewModel::setFocusSessions) }

    val appContext = androidx.compose.ui.platform.LocalContext.current
    val playlist by viewModel.playlist.collectAsState()
    val uiPrefs = remember { appContext.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE) }
    var soundOn by remember { mutableStateOf(uiPrefs.getBoolean("sounds", true).also { Sfx.enabled = it }) }
    var hapticsOn by remember { mutableStateOf(uiPrefs.getBoolean("haptics", true).also { Haptics.enabled = it }) }
    var autoNext by remember { mutableStateOf(uiPrefs.getBoolean("auto_next", true)) }
    var stopAtSep by remember { mutableStateOf(uiPrefs.getBoolean("stop_at_separator", true)) }
    var morningMin by remember { mutableIntStateOf(resetMinutes(appContext, "morning")) }
    var nightMin by remember { mutableIntStateOf(resetMinutes(appContext, "night")) }
    var eyeLocks by remember { mutableStateOf(uiPrefs.getBoolean("eye_locks", true)) }
    var medDefault by remember { mutableIntStateOf(uiPrefs.getInt("med_default", 5)) }
    var sleepOn by remember { mutableStateOf(sleepEnabled(appContext)) }
    var sleepMin by remember { mutableIntStateOf(sleepMinutes(appContext)) }
    fun applySleep(on: Boolean) { sleepOn = on; setSleepEnabled(appContext, on); syncSleepNotification(appContext) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) applySleep(true) else Toast.makeText(appContext, "Allow notifications to see the sleep countdown.", Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) { syncSleepNotification(appContext) }
    var importPending by remember { mutableStateOf<Backup.Parsed?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) appScope.launch {
            val ok = try {
                val text = viewModel.exportText(linkedMapOf(
                    "morning_reset_minutes" to morningMin.toString(), "night_reset_minutes" to nightMin.toString(),
                    "auto_next" to autoNext.toString(), "stop_at_separator" to stopAtSep.toString(),
                    "sound" to soundOn.toString(), "haptics" to hapticsOn.toString(), "theme" to (if (isOledDark) "dark" else "light")
                ))
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { appContext.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
                true
            } catch (e: Exception) { false }
            Toast.makeText(appContext, if (ok) "Backup saved." else "Could not save the backup.", Toast.LENGTH_SHORT).show()
        }
    }
    fun applyImportedSettings(m: Map<String, String>) {
        m["morning_reset_minutes"]?.toIntOrNull()?.let { morningMin = it; setResetMinutes(appContext, "morning", it) }
        m["night_reset_minutes"]?.toIntOrNull()?.let { nightMin = it; setResetMinutes(appContext, "night", it) }
        m["auto_next"]?.toBooleanStrictOrNull()?.let { autoNext = it; uiPrefs.edit().putBoolean("auto_next", it).apply() }
        m["stop_at_separator"]?.toBooleanStrictOrNull()?.let { stopAtSep = it; uiPrefs.edit().putBoolean("stop_at_separator", it).apply() }
        m["sound"]?.toBooleanStrictOrNull()?.let { soundOn = it; Sfx.enabled = it; uiPrefs.edit().putBoolean("sounds", it).apply() }
        m["haptics"]?.toBooleanStrictOrNull()?.let { hapticsOn = it; Haptics.enabled = it; uiPrefs.edit().putBoolean("haptics", it).apply() }
        m["theme"]?.let { if ((it == "dark") != isOledDark) onToggleTheme() }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) appScope.launch {
            val text = try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } } } catch (e: Exception) { null }
            val parsed = text?.let { Backup.parse(it) }
            if (parsed == null) Toast.makeText(appContext, "That file is not a MONO backup.", Toast.LENGTH_LONG).show() else importPending = parsed
        }
    }

    // Switching lists: a soft bright wash spreads from the tapped button, the list swaps underneath, then the wash fades out.
    val washReveal = remember { Animatable(0f) }
    val washAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(1f) }
    var bgKey by remember { mutableStateOf(playlist) }
    var washKey by remember { mutableStateOf("main") }
    var washCenter by remember { mutableStateOf(Offset.Zero) }
    var washBusy by remember { mutableStateOf(false) }
    fun switchPlaylist(key: String, center: Offset) {
        if (washBusy || key == playlist) return
        appScope.launch {
            washBusy = true
            // The wash IS that list's page. Going back to Main, it is the page you are leaving that shrinks away.
            washKey = if (key == "main") playlist else key
            washCenter = center
            // The list's content swaps and fades in at the same moment the colour wash moves.
            launch {
                contentAlpha.snapTo(0f)
                contentAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            }
            if (key == "main") {
                // Main is the base underneath; the page you are leaving shrinks away toward the button.
                washReveal.snapTo(1.3f)
                washAlpha.snapTo(1f)
                bgKey = "main"
                viewModel.setPlaylist("main")
                washReveal.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
                washAlpha.snapTo(0f)
            } else {
                washReveal.snapTo(0f)
                washAlpha.snapTo(1f)
                viewModel.setPlaylist(key)
                washReveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                bgKey = key
                delay(160)
                washAlpha.snapTo(0f)
            }
            washBusy = false
        }
    }
    // Settings always wear Main's colours. From Morning/Night the page washes to Main toward that list's button, and washes back out of it.
    var settingsWashCenter by remember { mutableStateOf<Offset?>(null) }
    fun openSettings(center: Offset) {
        if (playlist == "main" || washBusy) { screen = ScreenState.SETTINGS; return }
        settingsWashCenter = center
        appScope.launch {
            washBusy = true
            washKey = playlist; washCenter = center
            launch { contentAlpha.snapTo(0f); contentAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
            washReveal.snapTo(1.3f); washAlpha.snapTo(1f)
            screen = ScreenState.SETTINGS
            washReveal.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            washAlpha.snapTo(0f)
            washBusy = false
        }
    }
    fun closeSettings() {
        val c = settingsWashCenter
        settingsWashCenter = null
        if (playlist == "main" || c == null || washBusy) { screen = ScreenState.HOME; return }
        appScope.launch {
            washBusy = true
            washKey = playlist; washCenter = c
            launch { contentAlpha.snapTo(0f); contentAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
            washReveal.snapTo(0f); washAlpha.snapTo(1f)
            bgKey = "main"
            screen = ScreenState.HOME
            washReveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            bgKey = playlist
            delay(160)
            washAlpha.snapTo(0f)
            washBusy = false
        }
    }
    LaunchedEffect(activeQueue) { updateAllWidgets(appContext) }
    // Morning clears at midnight and Night at 6 am (local time), whether the app was open or not.
    LaunchedEffect(Unit) { while (true) { applyDailyResets(appContext); delay(30_000) } }

    fun rollDice(avoid: Long?) {
        appScope.launch {
            val t = viewModel.rollCleaning(avoid)
            if (t == null) {
                Toast.makeText(appContext, "Add a few tiny cleans first: long-press the dice to open your cleaning list.", Toast.LENGTH_LONG).show()
                return@launch
            }
            triggerHaptic(appContext, HapticStyle.CONFIRM)
            Sfx.play(Sound.TICK)
            rolledTask = t
            diceSession = true
            focusedId = t.id
            returnId = null
            autoSelectId = null
            if (screen != ScreenState.FOCUS) { focusTitleY = 420f; screen = ScreenState.FOCUS }
        }
    }

    fun finishTask(task: TaskEntity, center: Offset) {
        appScope.launch {
            winCenter = center
            winVisible = true
            delay(700) // the yellow now covers the screen
            if (task.playlist == "cleaning") {
                viewModel.completeTask(task) // counted, and stays in the bank
                diceSession = false
                returnId = null
                focusedId = null
                screen = ScreenState.HOME
                delay(1600)
                winVisible = false
                return@launch
            }
            val idx = activeQueue.indexOfFirst { it.id == task.id }
            // The very next thing in the list, even if it is a separator: focus always pauses there.
            // Unless Settings says otherwise: with "stop at separators" off, auto-start skips over them.
            val following = focusSeq.drop(focusSeq.indexOfFirst { it.id == task.id } + 1)
            // Morning and Night always carry on into the next section (with a banner); elsewhere Settings decides.
            val crossSep = !stopAtSep || task.playlist == "morning" || task.playlist == "night"
            val after = if (crossSep) following.firstOrNull { !it.isSeparator } else following.firstOrNull()
            val next = (after ?: activeQueue.getOrNull(idx - 1))?.takeIf { it.id != task.id }
            val enteredSep = if (crossSep && after != null) following.takeWhile { it.id != after.id }.lastOrNull { it.isSeparator } else null
            viewModel.completeTask(task)
            pendingUndo = task
            returnId = null
            sectionBanner = null
            focusedId = if (autoNext && next?.isSeparator != true) next?.id else null
            screen = ScreenState.HOME
            delay(1600)
            winVisible = false
            delay(650) // let the list show for a beat, with the finished task dimmed in place
            if (!autoNext) { /* back on the list; nothing opens by itself */ }
            else if (next?.isSeparator != true) { autoSelectId = next?.id; if (enteredSep != null) sectionBanner = enteredSep.title.trim() }
            delay(20000)
            if (pendingUndo?.id == task.id) pendingUndo = null
        }
    }

    // Keep-awake = lock the phone for real, then show MONO above the lock screen. Turning it off returns to the lock screen.
    val activity = appContext as Activity
    val adminComponent = remember { ComponentName(activity, MonoAdminReceiver::class.java) }
    val dpm = remember { activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    var locking by remember { mutableStateOf(false) }
    fun lockAndKeepAwake() {
        if (locking) return
        appScope.launch {
            locking = true
            keepScreenOn = true      // flags: stay on + draw above the lock screen
            delay(250)
            dpm.lockNow()            // the phone is now genuinely locked
            delay(700)
            wakeScreen(activity)     // bring the screen back; MONO shows over the lock screen
            delay(2500)              // ignore the resume this very wake-up causes
            locking = false
        }
    }
    // Eye on but the phone got unlocked (e.g. you left and came back): lock it again.
    val keyguard = remember { activity.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager }
    val eyeOn by rememberUpdatedState(keepScreenOn)
    val eyeLocksNow by rememberUpdatedState(eyeLocks)
    DisposableEffect(activity) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME && eyeOn && eyeLocksNow && !locking && dpm.isAdminActive(adminComponent) && !keyguard.isKeyguardLocked) lockAndKeepAwake()
        }
        val owner = activity as androidx.activity.ComponentActivity
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // Day and Night lists turn the eye on by themselves when a task is started.
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (dpm.isAdminActive(adminComponent)) lockAndKeepAwake()
        else Toast.makeText(activity, "MONO needs that permission to lock the phone, so the screen was left alone.", Toast.LENGTH_LONG).show()
    }
    val toggleKeepAwake = {
        if (keepScreenOn) keepScreenOn = false
        else if (!eyeLocks) keepScreenOn = true   // only keeps the screen on; the phone is not locked
        else if (dpm.isAdminActive(adminComponent)) lockAndKeepAwake()
        else adminLauncher.launch(
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "MONO uses this only to lock your phone when you turn on \"keep screen on\", so the screen can stay lit without leaving your phone open.")
        )
    }
    // Settings and the archive always wear the Main list's colours, whichever list they were opened from.
    val onSettings = screen == ScreenState.SETTINGS || screen == ScreenState.ARCHIVE
    val shownPlaylist = if (onSettings) "main" else playlist
    fun autoEye() { if ((playlist == "morning" || playlist == "night") && !keepScreenOn) toggleKeepAwake() }
    LockedDisplayEffect(active = keepScreenOn && eyeLocks)
    SystemBarsAndWakeEffect(immersiveFullScreen = (screen == ScreenState.FOCUS), keepScreenOn = keepScreenOn || medTimer?.done == false, isDark = (isOledDark && shownPlaylist != "morning") || shownPlaylist == "night" || shownPlaylist == "cleaning")

    MonoFocusTheme(isDark = isOledDark, playlist = if (diceSession && screen == ScreenState.FOCUS) "cleaning" else shownPlaylist) {
    // The page base follows bgKey (not the live theme), so the old colour holds until the wash has covered it.
    val themeBg = MaterialTheme.colorScheme.background
    var mainBg by remember { mutableStateOf(themeBg) }
    SideEffect { if (onSettings || (playlist == "main" && bgKey == "main")) mainBg = themeBg }
    Surface(modifier = Modifier.fillMaxSize(), color = if (onSettings || bgKey == "main" || (diceSession && screen == ScreenState.FOCUS)) (if (onSettings || bgKey == "main") mainBg else themeBg) else playlistFlat(bgKey)) {
      Box(Modifier.fillMaxSize()) {
        // A dice-rolled cleaning task always sits on the Cleaning gradient, even when rolled from Main.
        PlaylistBackdrop(if (diceSession && screen == ScreenState.FOCUS) "cleaning" else if (onSettings) "main" else bgKey)
        PlaylistWash(reveal = washReveal.value, alpha = washAlpha.value, key = washKey, center = washCenter)
        AnimatedContent(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
            targetState = screen,
            transitionSpec = {
                if (initialState == ScreenState.HOME && targetState == ScreenState.FOCUS) {
                    if (diceSession) fadeIn(tween(300)) togetherWith fadeOut(tween(300)) else fadeIn(tween(1)) togetherWith fadeOut(tween(460), targetAlpha = 0.99f)
                }
                else if (initialState == ScreenState.FOCUS && targetState == ScreenState.HOME) EnterTransition.None togetherWith fadeOut(tween(440, easing = FastOutSlowInEasing))
                else fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "Nav"
        ) { target ->
            when (target) {
                ScreenState.HOME -> HomeScreen(
                    tasks = todayList, subtasksByTask = subtasksByTask, editor = editor,
                    onAddTasks = { viewModel.addTasks(it, "") },
                    onComplete = viewModel::completeTask, onUndoComplete = viewModel::undoCompleteTask, onArchiveTask = viewModel::archiveTask,
                    onReorder = viewModel::reorder,
                    onSelect = { t, y -> autoEye(); if (autoSelectId == null) sectionBanner = null; diceSession = false; focusedId = t.id; focusTitleY = y; returnId = null; autoSelectId = null; screen = ScreenState.FOCUS },
                    autoSelectId = autoSelectId,
                    onOpenSettings = { c -> returnId = null; openSettings(c) },
                    keepScreenOn = keepScreenOn, onToggleKeepScreenOn = { toggleKeepAwake() },
                    onArchiveAll = viewModel::archiveAll, undoBanner = viewModel.undoBanner, onUndo = viewModel::runUndo, onDismissUndo = viewModel::dismissUndo,
                    onInsertAt = viewModel::insertAt, onInsertSeparatorAt = viewModel::insertSeparatorAt, freshIds = viewModel.freshIds, onAddSeparator = viewModel::addSeparator, onDeleteSeparator = viewModel::deleteSeparator, onLabelSeparator = viewModel::setSeparatorLabel,
                    playlist = playlist, onPlaylist = { k, c -> switchPlaylist(k, c) },
                    onRoll = { rollDice(null) },
                    onCleaningToggle = { c -> switchPlaylist(if (playlist == "cleaning") "main" else "cleaning", c) },
                    returnId = returnId, returnY = focusTitleY, onReturnConsumed = { returnId = null }
                )
                ScreenState.FOCUS -> FocusScreen(
                    task = focusTask, position = focusIndex + 1, titleY = focusTitleY, subtasksByTask = subtasksByTask, editor = editor, onFocusSession = viewModel::addFocusSession, remainingCount = if (diceSession) 2 else focusSeq.size, diceMode = diceSession, morphFromList = !diceSession, keepScreenOn = keepScreenOn, sectionBanner = sectionBanner, onBannerDone = { sectionBanner = null },
                    medTimer = medTimer, medDefault = medDefault,
                    onMedStart = { min -> medTimer = MedTimer(focusedId ?: 0L, min * 60, android.os.SystemClock.elapsedRealtime() + min * 60_000L, false) },
                    onMedAdd = { sec -> medTimer = medTimer?.let { m -> if (m.done) m.copy(total = sec, endsAt = android.os.SystemClock.elapsedRealtime() + sec * 1000L, done = false) else m.copy(total = m.total + sec, endsAt = m.endsAt + sec * 1000L) } },
                    onMedClear = { medTimer = null },
                    onToggleKeepScreenOn = { toggleKeepAwake() },
                    onFinish = { t, c -> finishTask(t, c) },
                    undoTask = pendingUndo,
                    onUndo = { t -> viewModel.undoCompleteTask(t); focusedId = t.id; pendingUndo = null },
                    onUndoDismiss = { pendingUndo = null },
                    onMoveToPrev = { t ->
                        pendingUndo = null
                        val idx = focusSeq.indexOfFirst { it.id == t.id }
                        if (focusSeq.size > 1) {
                            val target = focusSeq[(idx - 1 + focusSeq.size) % focusSeq.size]
                            // A separator is where single-task focus ends: go back to the list.
                            if (target.isSeparator) { returnId = t.id; diceSession = false; screen = ScreenState.HOME } else focusedId = target.id
                        }
                    },
                    onMoveToNext = { t ->
                        pendingUndo = null
                        if (diceSession) rollDice(t.id)
                        else {
                            val idx = focusSeq.indexOfFirst { it.id == t.id }
                            if (focusSeq.size > 1) {
                                val target = focusSeq[(idx + 1) % focusSeq.size]
                                if (target.isSeparator) { returnId = t.id; diceSession = false; screen = ScreenState.HOME } else focusedId = target.id
                            }
                        }
                    },
                    onExitFocus = { pendingUndo = null; returnId = if (diceSession) null else focusTask?.id; diceSession = false; screen = ScreenState.HOME }
                )
                ScreenState.ARCHIVE -> ArchiveScreen(
                    archivedTasks = archivedTasks, onRestore = viewModel::undoCompleteTask, onClearArchive = viewModel::clearArchive, onBack = { screen = ScreenState.SETTINGS }
                )
                ScreenState.SETTINGS -> SettingsScreen(
                    archivedCount = archivedTasks.size, onOpenArchive = { screen = ScreenState.ARCHIVE },
                    morningMin = morningMin, nightMin = nightMin,
                    onMorningMin = { morningMin = it; setResetMinutes(appContext, "morning", it) },
                    onNightMin = { nightMin = it; setResetMinutes(appContext, "night", it) },
                    autoNext = autoNext, onAutoNext = { autoNext = it; uiPrefs.edit().putBoolean("auto_next", it).apply() },
                    stopAtSep = stopAtSep, onStopAtSep = { stopAtSep = it; uiPrefs.edit().putBoolean("stop_at_separator", it).apply() },
                    soundOn = soundOn, onSound = { soundOn = it; Sfx.enabled = it; uiPrefs.edit().putBoolean("sounds", it).apply(); if (it) Sfx.play(Sound.TICK) },
                    hapticsOn = hapticsOn, onHaptics = { hapticsOn = it; Haptics.enabled = it; uiPrefs.edit().putBoolean("haptics", it).apply(); if (it) triggerHaptic(appContext, HapticStyle.TICK) },
                    eyeLocks = eyeLocks, onEyeLocks = { eyeLocks = it; uiPrefs.edit().putBoolean("eye_locks", it).apply() },
                    medDefault = medDefault, onMedDefault = { medDefault = it; uiPrefs.edit().putInt("med_default", it).apply() },
                    sleepOn = sleepOn, sleepMin = sleepMin,
                    onSleepOn = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        else applySleep(on)
                    },
                    onSleepMin = { sleepMin = it; setSleepMinutes(appContext, it); syncSleepNotification(appContext) },
                    isDark = isOledDark, onDark = { if (it != isOledDark) onToggleTheme() },
                    onExport = { exportLauncher.launch("mono-backup-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + ".txt") },
                    onImport = { importLauncher.launch(arrayOf("text/plain", "application/octet-stream")) },
                    onBack = { closeSettings() }
                )
            }
        }
        DoneOverlay(visible = winVisible, center = winCenter)
        importPending?.let { p ->
            if (!confirmReplace) AlertDialog(
                onDismissRequest = { importPending = null }, containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Import backup") },
                text = { Text("${p.real} tasks, ${p.separators} separators, ${p.archived} archived, and ${p.settings.size} settings.\n\nAdd them to the end of your lists, or overwrite your lists with them. Settings are restored either way.") },
                confirmButton = {
                    TextButton(onClick = {
                        importPending = null
                        appScope.launch { applyImportedSettings(p.settings); val a = viewModel.importAdd(p); Toast.makeText(appContext, "Added $a items to the end of your lists.", Toast.LENGTH_LONG).show() }
                    }) { Text("Add to the end", color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = { TextButton(onClick = { confirmReplace = true }) { Text("Overwrite lists", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            ) else AlertDialog(
                onDismissRequest = { confirmReplace = false; importPending = null }, containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Overwrite your lists?") },
                text = { Text("This deletes every task in MONO right now and puts the backup in their place. It cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmReplace = false; importPending = null
                        appScope.launch { applyImportedSettings(p.settings); viewModel.importReplace(p); Toast.makeText(appContext, "Lists overwritten from the backup.", Toast.LENGTH_LONG).show() }
                    }) { Text("Overwrite", color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = { TextButton(onClick = { confirmReplace = false; importPending = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            )
        }
      }
    }
    }
}

/** A feathered circular reveal of the list's real background (same base colour, same glows), so it matches the page exactly. */
@Composable
fun PlaylistWash(reveal: Float, alpha: Float, key: String, center: Offset) {
    if (alpha <= 0.005f) return
    Box(
        Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val far = kotlin.math.hypot(maxOf(center.x, size.width - center.x), maxOf(center.y, size.height - center.y))
                val r = (reveal * far * 1.15f).coerceAtLeast(1f)
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint())
                    drawRect(pageBase(key))
                    drawPlaylistGlows(key, 1f)
                    // Soft edge: keep the page where the mask is opaque, fade it out toward the rim.
                    drawRect(
                        brush = Brush.radialGradient(0f to Color.Black, 0.84f to Color.Black, 1f to Color.Transparent, center = center, radius = r),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                    canvas.restore()
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    )
}

/** Yellow wash that grows out of the check button, then a happy face. Lives above all screens so it survives the switch back to the list. */
@Composable
fun DoneOverlay(visible: Boolean, center: Offset) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    AnimatedVisibility(visible = visible, enter = EnterTransition.None, exit = fadeOut(tween(400)), modifier = Modifier.fillMaxSize()) {
        val reveal = remember { Animatable(0f) }
        val face = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            Sfx.play(Sound.WIN)
            reveal.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
            triggerHaptic(context, HapticStyle.TICK)
            face.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        Box(
            modifier = Modifier.fillMaxSize()
                .drawBehind {
                    val far = kotlin.math.hypot(maxOf(center.x, size.width - center.x), maxOf(center.y, size.height - center.y))
                    drawCircle(primary, radius = reveal.value * far, center = center)
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
            contentAlignment = Alignment.Center
        ) {
            HappyFace(
                modifier = Modifier.size(170.dp).graphicsLayer {
                    scaleX = face.value; scaleY = face.value
                    rotationZ = (1f - face.value) * -14f
                    alpha = face.value.coerceIn(0f, 1f)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    tasks: List<TaskEntity>, subtasksByTask: Map<Long, List<SubtaskEntity>>, editor: EditorActions,
    onAddTasks: (String) -> Unit, onComplete: (TaskEntity) -> Unit, onUndoComplete: (TaskEntity) -> Unit,
    onReorder: (List<TaskEntity>) -> Unit, onSelect: (TaskEntity, Float) -> Unit, onOpenSettings: (Offset) -> Unit, onArchiveAll: () -> Unit, onArchiveTask: (TaskEntity) -> Unit,
    onInsertAt: (Long, String, (Long) -> Unit) -> Unit, onInsertSeparatorAt: (Long, String, (Long) -> Unit) -> Unit, freshIds: List<Long>, onAddSeparator: (String) -> Unit, onDeleteSeparator: (TaskEntity) -> Unit, onLabelSeparator: (TaskEntity, String) -> Unit,
    undoBanner: TaskViewModel.UndoBanner? = null, onUndo: () -> Unit = {}, onDismissUndo: () -> Unit = {},
    playlist: String, onPlaylist: (String, Offset) -> Unit, onRoll: () -> Unit, onCleaningToggle: (Offset) -> Unit,
    autoSelectId: Long?, returnId: Long?, returnY: Float, onReturnConsumed: () -> Unit,
    keepScreenOn: Boolean = false, onToggleKeepScreenOn: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var quickId by remember { mutableStateOf<Long?>(null) }
    var dockH by remember { mutableStateOf(0.dp) }
    // The list's bottom room follows the dock (which includes the keyboard). Growing is immediate; shrinking waits a moment and then eases,
    // so closing the keyboard never yanks a list that was scrolled past its new end (rows used to jump down in two sudden steps).
    val heldPad = remember { Animatable(dockH, androidx.compose.ui.unit.Dp.VectorConverter) }
    LaunchedEffect(dockH) { if (dockH >= heldPad.value) heldPad.snapTo(dockH) else { delay(300); heldPad.animateTo(dockH, tween(420, easing = FastOutSlowInEasing)) } }
    var glyphRowCenter by remember { mutableStateOf(Offset.Zero) }
    // When coming back from the focus screen the list starts in the "selected" look and plays the animation in reverse.
    var selectingId by remember { mutableStateOf(returnId) }
    var morphY by remember { mutableFloatStateOf(if (returnId != null) returnY else -1f) }
    val focusX = remember(density) { with(density) { FOCUS_TITLE_X.toPx() } }
    val minY = remember(density) { with(density) { 76.dp.toPx() } }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxY = remember(density, screenHeightDp) { with(density) { (screenHeightDp * 0.55f).dp.toPx() } }
    LaunchedEffect(Unit) { if (returnId != null) { selectingId = null; onReturnConsumed() } }
    val chromeAlpha by animateFloatAsState(if (selectingId != null) 0f else 1f, tween(440, easing = FastOutSlowInEasing), label = "chrome")
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingSeparator by remember { mutableStateOf<TaskEntity?>(null) }

    // Local copy of the list so drag-to-reorder is instant; written back on drop.
    val items = remember { mutableStateListOf<TaskEntity>().apply { addAll(tasks) } }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var dragStartOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(tasks) { if (draggingId == null) { items.clear(); items.addAll(tasks) } }
    // After finishing a task the next one is opened automatically; make sure its row is on screen first.
    // The row is brought to the middle of the visible area (above the dock) first; only then does it start growing into focus.
    var readyId by remember { mutableStateOf<Long?>(null) }
    val imeInsets = WindowInsets.ime
    suspend fun centerRow(i: Int, id: Long, force: Boolean = false) {
        val dockPx = with(density) { dockH.toPx() }   // already includes the keyboard when it is up
        fun info() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
        val viewH = listState.layoutInfo.viewportSize.height.toFloat()
        val topEdge = with(density) { 8.dp.toPx() }
        val bottomEdge = viewH - dockPx
        val target = (topEdge + bottomEdge) / 2f
        val cur = info()
        if (!force && cur != null && cur.offset >= topEdge && cur.offset + cur.size <= bottomEdge) return
        // One long, eased glide for the whole distance (estimated from the rows on screen when the target is off screen),
        // then a tiny correction once the real row is measured. No jumps.
        val delta = if (cur != null) cur.offset + cur.size / 2f - target else {
            val vis = listState.layoutInfo.visibleItemsInfo
            val first = vis.firstOrNull() ?: return
            val avg = vis.sumOf { it.size }.toFloat() / vis.size
            first.offset + (i - first.index) * avg + avg / 2f - target
        }
        val ms = (380 + kotlin.math.abs(delta) / density.density * 0.35f).toInt().coerceAtMost(900)
        listState.animateScrollBy(delta, tween(ms, easing = FastOutSlowInEasing))
        info()?.let { r ->
            val err = r.offset + r.size / 2f - target
            if (kotlin.math.abs(err) > 3f) listState.animateScrollBy(err, tween(200, easing = FastOutSlowInEasing))
        }
        delay(40)
    }
    LaunchedEffect(autoSelectId, items.size) {
        if (autoSelectId == null) { readyId = null; return@LaunchedEffect }
        val i = items.indexOfFirst { it.id == autoSelectId }
        if (i >= 0 && readyId != autoSelectId) { centerRow(i, autoSelectId); readyId = autoSelectId }
    }

    // Tap the empty space above or below any to-do or separator to choose where the next things you add go.
    // TOP_SEAM = above the first row; otherwise "right after the row with this id". It lets go by itself after a while without use.
    var slotId by remember { mutableStateOf<Long?>(null) }
    var slotTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(slotId, slotTick) { if (slotId != null) { delay(12_000); slotId = null } }
    LaunchedEffect(input) { if (slotId != null) slotTick++ }   // typing keeps the spot alive
    LaunchedEffect(playlist, selectingId) { slotId = null }
    LaunchedEffect(items.size) { delay(400); val s = slotId; if (s != null && s != TOP_SEAM && items.none { it.id == s }) slotId = null }
    // Once what was typed is in, the chosen spot lets go and the keyboard tucks away.
    val keyboardCtl = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    fun finishPlacing() { slotId = null; keyboardCtl?.hide(); focusManager.clearFocus() }
    val seamTol = remember(density) { with(density) { 12.dp.toPx() } }
    val gapHalf = remember(density) { with(density) { 7.dp.toPx() } }
    fun it_seam(i: androidx.compose.foundation.lazy.LazyListItemInfo, marked: Long?, half: Float): Float = i.offset + i.size - (if (i.key == marked) half else 0f)
    val slotGesture = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            if (selectingId != null || draggingId != null || items.isEmpty()) return@awaitEachGesture
            val vis = listState.layoutInfo.visibleItemsInfo.filter { it.key != "top-slot" }
            val firstVis = vis.firstOrNull() ?: return@awaitEachGesture
            val lastVis = vis.last()
            // Item offsets are measured from the start of the list content, i.e. after its top padding.
            val y = first.position.y - listState.layoutInfo.beforeContentPadding
            val onRow = vis.any { y >= it.offset && y <= it.offset + it.size }
            val seam: Long? = when {
                firstVis.key == items.first().id && y <= firstVis.offset + seamTol -> TOP_SEAM
                lastVis.key == items.last().id && y >= lastVis.offset + lastVis.size - seamTol -> items.last().id
                else -> {
                    // Where each seam really is: the marked one sits in the middle of its open gap, so the line itself is a tap target.
                    fun seamY(i: androidx.compose.foundation.lazy.LazyListItemInfo) = it_seam(i, slotId, gapHalf)
                    vis.minByOrNull { kotlin.math.abs(seamY(it) - y) }?.takeIf { kotlin.math.abs(seamY(it) - y) <= seamTol }?.key as? Long
                }
            }
            if (seam == null) return@awaitEachGesture
            // Only a quick tap counts; a scroll, drag or hold is left alone.
            var up: androidx.compose.ui.input.pointer.PointerInputChange? = null
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == first.id } ?: break
                    if (c.isConsumed) break
                    if (!c.pressed) { up = c; break }
                    if ((c.position - first.position).getDistance() > viewConfiguration.touchSlop) break
                }
            }
            val lift = up ?: return@awaitEachGesture
            if (onRow) lift.consume()   // the edge of a row is not a tap on that row
            triggerHaptic(context, HapticStyle.TICK)
            // Tapping the marked spot again lets it go.
            if (slotId == seam) slotId = null else { slotId = seam; slotTick++ }
        }
    }

    // Pull the list down from the top to open Settings. A gear fades in as you pull and clicks in when you can let go.
    var pull by remember { mutableFloatStateOf(0f) }
    val pullThreshold = remember(density) { with(density) { 90.dp.toPx() } }
    var pullArmed by remember { mutableStateOf(false) }
    val pullConnection = remember(listState, pullThreshold) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (pull > 0f && available.y < 0f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag) {
                    val next = (pull + available.y).coerceAtLeast(0f)
                    val used = next - pull
                    pull = next
                    pullArmed = pull >= pullThreshold
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag && available.y > 0f && !listState.canScrollBackward && selectingId == null && draggingId == null) {
                    pull = (pull + available.y * 0.5f).coerceAtMost(pullThreshold * 1.5f)
                    val armed = pull >= pullThreshold
                    if (armed && !pullArmed) triggerHaptic(context, HapticStyle.CONFIRM)
                    pullArmed = armed
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pull <= 0f) return androidx.compose.ui.unit.Velocity.Zero
                val open = pull >= pullThreshold
                if (open) { Sfx.play(Sound.TICK); onOpenSettings(glyphRowCenter + Offset(with(density) { 24.dp.toPx() } * (if (playlist == "morning") -1 else 1), 0f)) }
                pullArmed = false
                animate(pull, 0f, animationSpec = tween(240, easing = FastOutSlowInEasing)) { v, _ -> pull = v }
                return available
            }
        }
    }

    // Double-tapping empty space opens the first open task through the same grow-into-focus animation as tapping it.
    fun startFirst() {
        if (selectingId != null || draggingId != null) return
        slotId = null
        val i = items.indexOfFirst { !it.isSeparator && !it.isCompleted }
        if (i < 0) return
        val id = items[i].id
        scope.launch { centerRow(i, id); quickId = id }
    }

    // On Morning / Night / Cleaning, Back returns to Main instead of leaving the app.
    BackHandler(enabled = playlist != "main" && selectingId == null) {
        triggerHaptic(context, HapticStyle.TICK)
        onPlaylist("main", glyphRowCenter + Offset(with(density) { 24.dp.toPx() } * (if (playlist == "morning") -1 else 1), 0f))
    }

    fun submit() {
        if (input.isNotBlank()) {
            triggerHaptic(context, HapticStyle.TICK)
            Sfx.play(Sound.ADD)
            val s = slotId
            if (s != null) onInsertAt(s, input) {} else onAddTasks(input)
            input = ""
            if (s != null) finishPlacing()
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = chromeAlpha }.padding(start = 28.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            if (playlist == "main") {
                Text("MONO", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 4.sp), color = MaterialTheme.colorScheme.primary)
            } else {
                // On a list page the title is the list's name, with a back arrow to Main.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-12).dp)) {
                    IconButton(onClick = {
                        triggerHaptic(context, HapticStyle.TICK)
                        // Shrink back to this list's own button (Morning sits left of centre, Night/Cleaning right).
                        val dx = with(density) { 24.dp.toPx() } *(if (playlist == "morning") -1 else 1)
                        onPlaylist("main", glyphRowCenter + Offset(dx, 0f))
                    }) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back to main list", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(17.dp)) }
                    Text(
                        PLAYLISTS.firstOrNull { it.first == playlist }?.second ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light, letterSpacing = 1.5.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            Row {
                if (keepScreenOn) IconButton(onClick = { triggerHaptic(context, HapticStyle.TICK); onToggleKeepScreenOn() }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.Visibility, contentDescription = "Eye is on. Tap to turn it off", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = { triggerHaptic(context, HapticStyle.CONFIRM); Sfx.play(Sound.SWEEP); onArchiveAll() }, enabled = items.any { !it.isSeparator } && selectingId == null, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.MoveToInbox, contentDescription = "Archive all", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (items.isNotEmpty()) 0.6f else 0.25f), modifier = Modifier.size(19.dp))
                }
            }
        }

        // The list runs underneath the dock, so the empty corners beside the list-button tab show the text and take double-taps too.
        Box(modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) { detectTapGestures(onDoubleTap = { startFirst() }) }) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(pullConnection).then(slotGesture)) {
            // The gear that rises out of the top edge while you pull.
            val reveal = (pull / pullThreshold).coerceIn(0f, 1f)
            if (reveal > 0.02f) Box(
                Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = pull * 0.45f - 6f; alpha = reveal },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.width(132.dp).height(36.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Settings, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (pullArmed) 0.85f else 0.55f),
                        modifier = Modifier.size(17.dp).graphicsLayer { rotationZ = reveal * 120f }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Settings", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (pullArmed) 0.85f else 0.55f))
                }
            }
            if (items.isEmpty()) {
                Text(if (playlist == "cleaning") "Add tiny 2-minute cleans here." else "Nothing to do.", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().graphicsLayer { translationY = pull }, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp + heldPad.value)) {
                item(key = "top-slot") {
                    val first = items.firstOrNull()
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.fillMaxWidth().height(animateDpAsState(if (first != null && !first.isSeparator) 24.dp else 0.dp, tween(200), label = "slotH").value))
                        SlotLine(slotId == TOP_SEAM)
                    }
                }
                items(items, key = { it.id }) { task ->
                    val isDragging = draggingId == task.id
                    val rowModifier = if (isDragging || selectingId == task.id) Modifier.zIndex(1f) else Modifier.animateItemPlacement()
                    val dragTranslation = {
                        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == task.id }
                        if (info == null) dragDelta else dragDelta - (info.offset - dragStartOffset)
                    }
                    val freshIdx = freshIds.indexOf(task.id)
                    val onDragStartRow = {
                        triggerHaptic(context, HapticStyle.TICK)
                        focusManager.clearFocus()
                        draggingId = task.id
                        dragDelta = 0f
                        dragStartOffset = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == task.id }?.offset ?: 0
                    }
                    val onDragRow = { dy: Float ->
                      if (draggingId == task.id) {
                        dragDelta += dy
                        val info = listState.layoutInfo
                        val me = info.visibleItemsInfo.firstOrNull { it.key == task.id }
                        if (me != null) {
                            val center = dragStartOffset + dragDelta + me.size / 2f
                            // Swap only when the dragged edge passes the neighbour's midpoint, so rows of different heights never fight each other.
                            val top = dragStartOffset + dragDelta
                            val bottom = top + me.size
                            val below = info.visibleItemsInfo.firstOrNull { it.index == me.index + 1 }
                            val above = info.visibleItemsInfo.firstOrNull { it.index == me.index - 1 }
                            val target = if (below != null && bottom > below.offset + below.size / 2f) below
                                else if (above != null && top < above.offset + above.size / 2f) above else null
                            if (target != null) {
                                val from = items.indexOfFirst { it.id == task.id }
                                val to = target.index - 1   // row 0 of the lazy list is the top slot
                                if (from >= 0 && to >= 0 && to < items.size && to != from) {
                                    items.add(to, items.removeAt(from))
                                    triggerHaptic(context, HapticStyle.TICK)
                                }
                            }
                            if (center < info.viewportStartOffset + 90f) scope.launch { listState.scrollBy(-24f) }
                            else if (center > info.viewportEndOffset - 90f) scope.launch { listState.scrollBy(24f) }
                        }
                      }
                    }
                    val onDragEndRow = {
                        if (draggingId == task.id) {
                            draggingId = null
                            dragDelta = 0f
                            onReorder(items.toList())
                        }
                    }
                    if (task.isSeparator) {
                        SeparatorRow(
                            task = task, fresh = freshIdx >= 0, freshDelay = freshIdx * 70, isDragging = isDragging, modifier = rowModifier, hidden = selectingId != null,
                            dragTranslation = dragTranslation, onEdit = { editingSeparator = task }, onDelete = { onDeleteSeparator(task) },
                            onDragStart = onDragStartRow, onDrag = onDragRow, onDragEnd = onDragEndRow
                        )
                    } else TaskRow(
                        task = task, fresh = freshIdx >= 0, freshDelay = freshIdx * 70,
                        subtasks = subtasksByTask[task.id].orEmpty(),
                        onOpen = { if (selectingId == null) { triggerHaptic(context, HapticStyle.TICK); editingId = task.id } },
                        isDragging = isDragging,
                        modifier = rowModifier,
                        selection = when (selectingId) { null -> 0; task.id -> 2; else -> 1 },
                        focusX = focusX, minY = minY, maxY = maxY, morphY = morphY,
                        autoSelect = (autoSelectId == task.id && readyId == task.id) || quickId == task.id,
                        onSelect = { y ->
                            if (selectingId == null && draggingId == null && !task.isCompleted) {
                                quickId = null
                                triggerHaptic(context, HapticStyle.CONFIRM)
                                Sfx.play(Sound.TICK)
                                focusManager.clearFocus()
                                morphY = y
                                selectingId = task.id
                                onSelect(task, y)
                            }
                        },
                        dragTranslation = dragTranslation,
                        onCheck = { if (task.isCompleted) onUndoComplete(task) else onComplete(task) },
                        onArchive = { onArchiveTask(task) },
                        onToggleSub = editor.onToggleSub,
                        onDragStart = onDragStartRow, onDrag = onDragRow, onDragEnd = onDragEndRow
                    )
                    SlotLine(slotId == task.id)
                }
            }
        }

        // After archiving: a quiet pill with Undo, gone by itself after a few seconds.
        LaunchedEffect(undoBanner?.id) { if (undoBanner != null) { delay(5000); onDismissUndo() } }
        androidx.compose.animation.AnimatedVisibility(
            visible = undoBanner != null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = dockH + 10.dp),
            enter = fadeIn(tween(200)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it / 2 }
        ) {
            // Just the Undo button, the same one as on the focus screen.
            Row(
                modifier = Modifier.shadow(8.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.UNDO); onUndo() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Undo, contentDescription = "Undo archive", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Undo", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { dockH = with(density) { it.height.toDp() } }.graphicsLayer { alpha = chromeAlpha }.navigationBarsPadding().imePadding().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 8.dp)) {
            // One shape, one outline: the bar with a raised centre tab that holds the list buttons. The tab sits above the text area, never over it.
            val dockDensity = LocalDensity.current
            val dockShape = remember(dockDensity) { DockShape(with(dockDensity) { 116.dp.toPx() }, with(dockDensity) { 48.dp.toPx() }, with(dockDensity) { 22.dp.toPx() }, with(dockDensity) { 22.dp.toPx() }) }
            Column(Modifier.fillMaxWidth().clip(dockShape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, dockShape)) {
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    // Hairline between the list buttons and the text bar, across the tab only.
                    Box(Modifier.align(Alignment.BottomCenter).width(92.dp).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)))
                    Row(
                        Modifier.onGloballyPositioned { glyphRowCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistGlyphs(
                            selected = playlist,
                            onSelect = { k, c -> if (selectingId == null) { triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.TICK); onPlaylist(k, c) } },
                            onRoll = { if (selectingId == null) onRoll() },
                            onCleaningToggle = { c -> if (selectingId == null) { triggerHaptic(context, HapticStyle.CONFIRM); Sfx.play(Sound.TICK); onCleaningToggle(c) } }
                        )
                    }
                }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default, capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(vertical = 8.dp)) {
                            if (input.isEmpty()) Text("Add a task", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            inner()
                        }
                    }
                )
                val canAdd = input.isNotBlank()
                val multiLine = input.contains('\n')   // a separator label has to be a single line
                val btnShape = RoundedCornerShape(14.dp)
                // Adds a divider line; anything typed becomes its label.
                Box(
                    modifier = Modifier.size(40.dp).clip(btnShape).clickable {
                        if (multiLine) {
                            // A separator label is one line; say so instead of silently doing something else. Nothing typed is lost.
                            triggerHaptic(context, HapticStyle.TICK)
                            android.widget.Toast.makeText(context, "A separator can't have a multi-line label", android.widget.Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.ADD)
                        val label = input.trim(); input = ""
                        val s = slotId
                        if (s != null) { onInsertSeparatorAt(s, label) {}; finishPlacing() } else onAddSeparator(label)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.HorizontalRule, contentDescription = "Add a separator", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (multiLine) 0.2f else 0.55f))
                }
                // The + only exists while there is something to add.
                AnimatedVisibility(visible = canAdd, enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.7f), exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f)) {
                    Box(
                        modifier = Modifier.padding(start = 4.dp).size(40.dp).clip(btnShape).background(MaterialTheme.colorScheme.primary).clickable { submit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            }
        }
        }
    }

    items.firstOrNull { it.id == editingId }?.let { t ->
        TaskEditorSheet(task = t, subtasks = subtasksByTask[t.id].orEmpty(), actions = editor, onClose = { editingId = null })
    }

    editingSeparator?.let { sep ->
        SeparatorEditorSheet(
            task = sep,
            onSave = { onLabelSeparator(sep, it) },
            onRemove = { onDeleteSeparator(sep) },
            onClose = { editingSeparator = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeparatorEditorSheet(task: TaskEntity, onSave: (String) -> Unit, onRemove: () -> Unit, onClose: () -> Unit) {
    var v by remember(task.id) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(task.title)) }
    var focused by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val close = { if (!removed) onSave(v.text); onClose() }
    InWindowSheet(onDismiss = close) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            BasicTextField(
                value = v, onValueChange = { v = it }, singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(primary), modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { close() }),
                decorationBox = { inner ->
                    Box {
                        if (v.text.isEmpty()) Text("Separator label", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = muted.copy(alpha = 0.5f))
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).clickable { removed = true; onRemove(); onClose() }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text("Remove separator", style = MaterialTheme.typography.bodyLarge, color = muted)
            }
        }
    }
}

/** A divider line with an optional label. Hold it to move it, tap to rename, swipe left to take it away. */
@Composable
fun SeparatorRow(
    task: TaskEntity, fresh: Boolean, freshDelay: Int, isDragging: Boolean, hidden: Boolean, modifier: Modifier, dragTranslation: () -> Float,
    onEdit: () -> Unit, onDelete: () -> Unit, onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val lightPage = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val outline = if (lightPage) Color(0xFFB4B4BA) else Color.White.copy(alpha = 0.17f)
    val shape = RoundedCornerShape(16.dp)
    // A separator placed by tapping a spot fades in and settles down from just above.
    var appeared by remember(task.id) { mutableStateOf(!fresh) }
    LaunchedEffect(task.id) { if (!appeared) delay(freshDelay.toLong()); appeared = true }
    val rise by animateFloatAsState(if (appeared) 0f else 1f, tween(420, easing = FastOutSlowInEasing), label = "sepRise")
    val risePx = with(LocalDensity.current) { 10.dp.toPx() }
    val alpha by animateFloatAsState(if (hidden || !appeared) 0f else 1f, tween(if (appeared && !hidden && fresh) 450 else 440, easing = FastOutSlowInEasing), label = "sepAlpha")
    val swipe = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 110.dp.toPx() }
    Row(
        modifier = modifier.fillMaxWidth()
            // Hold anywhere on it to pick it up and move it.
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { triggerHaptic(context, HapticStyle.TICK); onDragStart() },
                    onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .pointerInput(task.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (swipe.value < -threshold) {
                                triggerHaptic(context, HapticStyle.CONFIRM)
                                swipe.animateTo(-size.width.toFloat(), tween(260, easing = FastOutSlowInEasing))
                                onDelete()
                            } else swipe.animateTo(0f, tween(200))
                        }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(0f, tween(200)) } },
                    onHorizontalDrag = { change, delta -> change.consume(); scope.launch { swipe.snapTo((swipe.value + delta).coerceAtMost(0f)) } }
                )
            }
            .graphicsLayer { translationX = swipe.value; translationY = (if (isDragging) dragTranslation() else 0f) - rise * risePx; this.alpha = alpha * (1f + swipe.value / (size.width.coerceAtLeast(1f))).coerceIn(0f, 1f) }
            // The lifted card (while moving or sliding away) includes the breathing room above and below the line.
            .then(if (isDragging) Modifier.shadow(12.dp, shape).background(MaterialTheme.colorScheme.surface, shape) else if (swipe.value < -1f) Modifier.background(MaterialTheme.colorScheme.surface, shape) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).height(14.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onEdit() }.padding(start = 20.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.title.isNotBlank()) {
                Text(task.title.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium), color = if (lightPage) Color(0xFF8A8A90) else muted.copy(alpha = 0.45f), modifier = Modifier.widthIn(max = 240.dp))
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f).height(1.dp).background(outline))
        }
    }
}

/**
 * A bottom sheet drawn inside the app's own window instead of a separate dialog window.
 * Android's native Cut/Copy/Paste pop-up is hidden in dialog windows, but works here exactly as it does in the add bar.
 */
@Composable
fun InWindowSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { shown = true }
    val dismiss: () -> Unit = { scope.launch { shown = false; delay(220); onDismiss() } }
    BackHandler(onBack = dismiss)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(shown, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() })
        }
        AnimatedVisibility(
            shown, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it }, exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it }
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(MaterialTheme.colorScheme.surface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                }
                content()
            }
        }
    }
}

/** Pseudo seam id for the empty space above the first to-do. */
const val TOP_SEAM = -1L

/** The yellow line that marks where the next typed to-dos or separator will land: a thin inset line that only fades in and out, never moves. */
@Composable
fun SlotLine(visible: Boolean) {
    // Fade in 110ms, out 80ms (leaving is quicker than arriving). The gap that makes room for it moves at the same pace.
    val ms = if (visible) 110 else 80
    // Yellow on the dark lists; on the light Day list (pastel yellow page) yellow would vanish, so it is the same near-black as the text there.
    val lightPage = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val lineColor = if (lightPage) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(ms, easing = FastOutSlowInEasing), label = "slotLineA")
    val h by animateDpAsState(if (visible) 14.dp else 0.dp, tween(ms, easing = FastOutSlowInEasing), label = "slotLineH")
    if (h > 0.05.dp || visible) Box(Modifier.fillMaxWidth().height(h)) {
        // Pinned to where the gap ends up (its middle), so while the gap opens or closes the line stays put and only its opacity changes.
        Box(
            Modifier.fillMaxWidth().wrapContentHeight(align = Alignment.Top, unbounded = true).padding(top = 6.dp)
                .padding(horizontal = 20.dp).height(1.5.dp).graphicsLayer { alpha = a }
                .clip(CircleShape).background(lineColor)
        )
    }
}

/** The rounded square every check box uses, the same family as the big yellow check button. */
val CheckShape = RoundedCornerShape(7.dp)

/** Left edge of the focus screen's title. Its vertical position is wherever the tapped task's title was on screen. */
val FOCUS_TITLE_X = 28.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: TaskEntity, subtasks: List<SubtaskEntity>, onOpen: () -> Unit, onSelect: (Float) -> Unit, autoSelect: Boolean, selection: Int, focusX: Float, minY: Float, maxY: Float, morphY: Float, isDragging: Boolean, modifier: Modifier, fresh: Boolean = false, freshDelay: Int = 0,
    dragTranslation: () -> Float, onCheck: () -> Unit, onArchive: () -> Unit, onToggleSub: (SubtaskEntity) -> Unit,
    onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val checked = task.isCompleted
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val onBg = MaterialTheme.colorScheme.onBackground
    val shape = RoundedCornerShape(16.dp)
    // 1 = other rows fade away; 2 = the tapped row keeps its title and lets everything else go.
    val rowAlpha by animateFloatAsState(if (selection == 1) 0f else 1f, tween(440, easing = FastOutSlowInEasing), label = "rowAlpha")
    // A to-do placed by tapping a spot fades in and settles down from just above, one after another when several arrive together.
    var appeared by remember(task.id) { mutableStateOf(!fresh) }
    LaunchedEffect(task.id) { if (!appeared) delay(freshDelay.toLong()); appeared = true }
    val appear by animateFloatAsState(if (appeared) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "rowAppear")
    val risePx = with(LocalDensity.current) { 10.dp.toPx() }
    val partsAlpha by animateFloatAsState(if (selection == 2) 0f else 1f, tween(440, easing = FastOutSlowInEasing), label = "partsAlpha")
    val morph by animateFloatAsState(if (selection == 2) 1f else 0f, tween(440, easing = FastOutSlowInEasing), label = "morph")
    var titleOrigin by remember { mutableStateOf(Offset.Zero) }
    var originKnown by remember { mutableStateOf(false) }
    LaunchedEffect(autoSelect, originKnown) { if (autoSelect && originKnown && selection == 0) onSelect(titleOrigin.y.coerceIn(minY, maxY)) }

    // Swipe a row to the left to archive it. Nothing else ever archives for you.
    val swipe = remember { Animatable(0f) }
    val swipeThreshold = with(LocalDensity.current) { 110.dp.toPx() }
    val archivePop = remember { Animatable(0f) }
    val archivePainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Outlined.Inventory2)
    val checkPainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Check)
    var armed by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth()
            .pointerInput(task.id, selection) {
                if (selection != 0) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (swipe.value < -swipeThreshold) {
                                triggerHaptic(context, HapticStyle.CONFIRM); Sfx.play(Sound.SWEEP)
                                val pop = launch { archivePop.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
                                swipe.animateTo(-size.width.toFloat() * 0.55f, tween(360, easing = FastOutSlowInEasing))
                                pop.join()
                                onArchive()
                            } else swipe.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                        }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(0f, tween(200)) } },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        // A little resistance past the threshold, and a soft detent as it arms.
                        val next = (swipe.value + delta * (if (swipe.value < -swipeThreshold) 0.5f else 1f)).coerceAtMost(0f)
                        val nowArmed = next < -swipeThreshold
                        if (nowArmed && !armed) { triggerHaptic(context, HapticStyle.TICK) }
                        armed = nowArmed
                        scope.launch { swipe.snapTo(next) }
                    }
                )
            }
            // Hold anywhere on the row to pick it up and move it.
            .pointerInput(task.id, selection) {
                if (selection != 0) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { triggerHaptic(context, HapticStyle.TICK); onDragStart() },
                    onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .drawBehind {
                // A thin ring fills as you pull. At the threshold it completes, then the glyph settles into a check and lifts away.
                val prog = (-swipe.value / swipeThreshold).coerceIn(0f, 1f)
                val pop = archivePop.value
                if (prog > 0.03f) {
                    val fade = if (pop < 0.75f) 1f else 1f - (pop - 0.75f) / 0.25f
                    val swap = (pop / 0.5f).coerceIn(0f, 1f)
                    val ringR = 19.dp.toPx()
                    val cx = size.width - 46.dp.toPx()
                    val cy = size.height / 2f - 6.dp.toPx() * pop
                    val tint = androidx.compose.ui.graphics.lerp(muted, primary, prog)
                    drawCircle(muted.copy(alpha = 0.16f * fade), radius = ringR, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    drawCircle(primary.copy(alpha = 0.14f * swap * fade), radius = ringR, center = Offset(cx, cy))
                    drawArc(
                        color = tint.copy(alpha = fade), startAngle = -90f, sweepAngle = 360f * prog, useCenter = false,
                        topLeft = Offset(cx - ringR, cy - ringR), size = androidx.compose.ui.geometry.Size(ringR * 2, ringR * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    val px = 17.dp.toPx()
                    translate(cx - px / 2f, cy - px / 2f) {
                        with(archivePainter) { draw(androidx.compose.ui.geometry.Size(px, px), alpha = (prog * (1f - swap) * fade).coerceIn(0f, 1f), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint)) }
                        with(checkPainter) { draw(androidx.compose.ui.geometry.Size(px, px), alpha = (swap * fade).coerceIn(0f, 1f), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(primary)) }
                    }
                }
            }
            .graphicsLayer { translationX = swipe.value; translationY = (if (isDragging) dragTranslation() else 0f) - (1f - appear) * risePx; alpha = appear * rowAlpha * (1f - archivePop.value * 0.85f) * (1f - (-swipe.value / (size.width.coerceAtLeast(1f))).coerceIn(0f, 1f) * 0.5f) }
            .then(if (isDragging) Modifier.shadow(12.dp, shape).background(surface, shape) else Modifier)
            .padding(start = 8.dp),
        // Top-aligned: the check box and drag handle stay level with the title line, however many steps sit below it.
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.graphicsLayer { alpha = partsAlpha }.size(48.dp).clip(CircleShape).clickable(enabled = selection == 0) {
                triggerHaptic(context, if (checked) HapticStyle.TICK else HapticStyle.CONFIRM)
                Sfx.play(if (checked) Sound.UNDO else Sound.CHECK)
                onCheck()
            },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(22.dp).clip(CheckShape)
                    .background(if (checked) muted.copy(alpha = 0.35f) else Color.Transparent)
                    .then(if (checked) Modifier else Modifier.border(1.5.dp, muted.copy(alpha = 0.6f), CheckShape)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Icon(Icons.Default.Check, contentDescription = "Completed. Tap to undo", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(14.dp))
            }
        }
        Column(modifier = Modifier.weight(1f).combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen, onDoubleClick = { onSelect(titleOrigin.y.coerceIn(minY, maxY)) }).padding(start = 6.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 21.sp, fontWeight = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) FontWeight.Normal else FontWeight.Medium, letterSpacing = (-0.25).sp),
                color = if (checked) muted.copy(alpha = 0.45f) else onBg,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier
                    .onGloballyPositioned { titleOrigin = it.positionInRoot(); originKnown = true }
                    .graphicsLayer {
                        // Scale up from the title's own top-left corner and slide left: its height on screen stays the same.
                        val targetY = if (morphY >= 0f) morphY else titleOrigin.y.coerceIn(minY, maxY)
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = (focusX - titleOrigin.x) * morph
                        translationY = (targetY - titleOrigin.y) * morph
                        scaleX = 1f + morph
                        scaleY = 1f + morph
                        if (!originKnown) alpha = 0f
                    }
            )
            val meta = listOfNotNull(
                task.notes.takeIf { it.isNotBlank() },
                null
            ).joinToString("  ·  ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = muted.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.graphicsLayer { alpha = partsAlpha }.padding(top = 2.dp))
            }
            if (subtasks.isNotEmpty()) Column(Modifier.graphicsLayer { alpha = partsAlpha }.padding(top = 9.dp)) {
                subtasks.forEach { sub ->
                    key(sub.id) {
                        // Same as a main task: only the check box toggles; a tap anywhere else falls through to the row (opens the editor).
                        Row(
                            modifier = Modifier.fillMaxWidth().offset(x = (-8).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = selection == 0) {
                                    triggerHaptic(context, HapticStyle.TICK); Sfx.play(if (sub.isDone) Sound.UNDO else Sound.CHECK); onToggleSub(sub)
                                }.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(if (sub.isDone) muted.copy(alpha = 0.35f) else Color.Transparent)
                                        .then(if (sub.isDone) Modifier else Modifier.border(1.3.dp, muted.copy(alpha = 0.6f), RoundedCornerShape(5.dp))),
                                    contentAlignment = Alignment.Center
                                ) { if (sub.isDone) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(11.dp)) }
                            }
                            Spacer(Modifier.width(2.dp))
                            Text(
                                sub.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                color = if (sub.isDone) muted.copy(alpha = 0.45f) else onBg.copy(alpha = 0.8f),
                                textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
            if (task.focusSessions > 0) TimerBadgesFit(task.focusSessions, size = 20.dp, modifier = Modifier.graphicsLayer { alpha = partsAlpha }.padding(top = 6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    task: TaskEntity?, position: Int, titleY: Float, subtasksByTask: Map<Long, List<SubtaskEntity>>, editor: EditorActions, onFocusSession: (Long) -> Unit, remainingCount: Int, diceMode: Boolean, morphFromList: Boolean, keepScreenOn: Boolean, sectionBanner: String?, onBannerDone: () -> Unit, medTimer: MedTimer?, medDefault: Int, onMedStart: (Int) -> Unit, onMedAdd: (Int) -> Unit, onMedClear: () -> Unit, onToggleKeepScreenOn: () -> Unit,
    onFinish: (TaskEntity, Offset) -> Unit, undoTask: TaskEntity?, onUndo: (TaskEntity) -> Unit, onUndoDismiss: () -> Unit, onMoveToNext: (TaskEntity) -> Unit, onMoveToPrev: (TaskEntity) -> Unit,
    onExitFocus: () -> Unit
) {
    // Everything on this screen except the title fades in while the list's text grows into place; the title swaps in when it lands.
    val entryAlpha = remember { Animatable(0f) }
    var titleShown by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch { entryAlpha.animateTo(1f, tween(440, easing = FastOutSlowInEasing)) }
        delay(if (morphFromList) 440 else 120)
        titleShown = true
    }
    val exit = { exiting = true; onExitFocus() }
    val titleAlpha by animateFloatAsState(if (titleShown && !exiting) 1f else 0f, tween(if (exiting) 160 else if (morphFromList) 0 else 300), label = "titleAlpha")
    BackHandler(onBack = exit)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var finishing by remember(task?.id) { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE) }
    var singleStep by remember { mutableStateOf(prefs.getBoolean("single_step", false)) }
    val currentSubs = task?.let { subtasksByTask[it.id] }.orEmpty()
    var showStuck by remember { mutableStateOf(false) }
    var timerEnd by remember(task?.id) { mutableStateOf<Long?>(null) }
    var remainingMs by remember(task?.id) { mutableLongStateOf(0L) }
    var doneCenter by remember { mutableStateOf(Offset.Zero) }
    // A small Undo stays available for a few seconds once the next task has appeared.
    val undoAlpha by animateFloatAsState(if (undoTask != null) 1f else 0f, tween(250), label = "undoAlpha")
    LaunchedEffect(undoTask) { if (undoTask != null) { delay(3000); onUndoDismiss() } }
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Done is a wide pill: a big, easy target without a block of colour in the middle of the screen.
    val doneW = if (landscape) 168.dp else 116.dp
    // The top edge of the screen can hold a camera hole: anything placed there must start below it.
    val cutoutTop = with(LocalDensity.current) { WindowInsets.displayCutout.getTop(this).toDp() }
    val bannerTop = maxOf(22.dp, cutoutTop + 8.dp)
    val doneH = if (landscape) 80.dp else 64.dp
    val titleTopPad = with(LocalDensity.current) { (titleY.toDp() - 56.dp).coerceAtLeast(8.dp) }

    val startTimer = {
        triggerHaptic(context, HapticStyle.TICK)
        Sfx.play(Sound.TIMER_START)
        remainingMs = 120_000L
        timerEnd = android.os.SystemClock.elapsedRealtime() + 120_000L
    }
    val timerRunning = timerEnd != null
    DisposableEffect(timerRunning) {
        val window = (context as? Activity)?.window
        if (timerRunning) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (timerRunning && !keepScreenOn) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(timerEnd) {
        val end = timerEnd ?: return@LaunchedEffect
        val id = task?.id ?: return@LaunchedEffect
        while (true) {
            val left = end - android.os.SystemClock.elapsedRealtime()
            if (left <= 0) {
                triggerHaptic(context, HapticStyle.SHATTER)
                Sfx.play(Sound.TIMER_DONE)
                onFocusSession(id)
                timerEnd = null
                break
            }
            remainingMs = left
            delay(250)
        }
    }
    var dragY by remember { mutableFloatStateOf(0f) }
    var goingBack by remember { mutableStateOf(false) }
    val diceSpin = remember { Animatable(0f) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = entryAlpha.value }.imePadding().padding(horizontal = 12.dp).padding(top = 8.dp)
                .pointerInput(task?.id, remainingCount) {
                    if (task != null && remainingCount > 1) {
                        detectVerticalDragGestures(
                            onDragStart = { dragY = 0f },
                            onVerticalDrag = { _, a -> dragY += a },
                            onDragEnd = {
                                if (dragY < -140f) { triggerHaptic(context, HapticStyle.TICK); goingBack = false; onMoveToNext(task) }
                                else if (dragY > 140f && !diceMode) { triggerHaptic(context, HapticStyle.TICK); goingBack = true; onMoveToPrev(task) }
                            }
                        )
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = exit) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = muted) }
                Spacer(Modifier.weight(1f))
                if (task != null) {
                    Row {
                        if (currentSubs.isNotEmpty()) {
                            IconButton(onClick = {
                                triggerHaptic(context, HapticStyle.TICK)
                                singleStep = !singleStep
                                prefs.edit().putBoolean("single_step", singleStep).apply()
                            }) {
                                Icon(
                                    if (singleStep) Icons.Outlined.LooksOne else Icons.Outlined.Checklist,
                                    contentDescription = if (singleStep) "Showing one step. Tap to show all steps" else "Showing all steps. Tap to show one step at a time",
                                    tint = if (singleStep) primary else muted.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(onClick = { triggerHaptic(context, HapticStyle.TICK); onToggleKeepScreenOn() }) {
                            Icon(if (keepScreenOn) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = "Keep screen on. The phone stays locked", tint = if (keepScreenOn) primary else muted.copy(alpha = 0.6f))
                        }
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            AnimatedContent(
                targetState = task, modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    if (goingBack) (slideInVertically { -it / 4 } + fadeIn(tween(350))).togetherWith(slideOutVertically { it / 4 } + fadeOut(tween(250)))
                    else (slideInVertically { it / 4 } + fadeIn(tween(350))).togetherWith(slideOutVertically { -it / 4 } + fadeOut(tween(250)))
                },
                contentKey = { it?.id }, label = "FocusTask"
            ) { current ->
                // Title sits at a fixed spot (FOCUS_TITLE_X / FOCUS_TITLE_Y from the window's top-left) so the list's text can glide onto it.
                Box(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = if (landscape) 0.dp else titleTopPad), contentAlignment = if (landscape) Alignment.Center else Alignment.TopStart) {
                    if (current == null) {
                        Column {
                            Icon(Icons.Default.Check, contentDescription = null, tint = primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("All done.", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start) {
                            Text(
                                current.title,
                                textAlign = if (landscape) TextAlign.Center else TextAlign.Start,
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = if (landscape) 56.sp else 34.sp, lineHeight = if (landscape) 68.sp else 42.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
                                color = if (finishing) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground,
                                textDecoration = if (finishing) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.graphicsLayer { alpha = titleAlpha }.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showEditor = true }
                            )
                            if (current.notes.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text(current.notes, textAlign = if (landscape) TextAlign.Center else TextAlign.Start, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = muted)
                            }
                            Spacer(Modifier.height(28.dp))
                            val subs = subtasksByTask[current.id].orEmpty()
                            if (landscape) {
                                // Flash-card view: just the big centred text.
                            } else if (singleStep && subs.isNotEmpty()) {
                                val next = subs.firstOrNull { !it.isDone }
                                AnimatedContent(
                                    targetState = next, contentKey = { it?.id }, label = "NextStep",
                                    transitionSpec = { (slideInVertically { it / 3 } + fadeIn(tween(250))).togetherWith(fadeOut(tween(150))) }
                                ) { step ->
                                    if (step == null) {
                                        Text("All steps done", style = MaterialTheme.typography.bodyLarge, color = muted)
                                    } else {
                                        Column {
                                            Text("STEP ${subs.count { it.isDone } + 1} OF ${subs.size}", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = muted.copy(alpha = 0.7f))
                                            Spacer(Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { triggerHaptic(context, HapticStyle.CONFIRM); editor.onToggleSub(step) }.padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(26.dp).clip(CheckShape).border(2.dp, primary, CheckShape))
                                                Spacer(Modifier.width(16.dp))
                                                Text(step.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
                                            }
                                        }
                                    }
                                }
                            } else {
                                SubtaskList(
                                    subtasks = subs,
                                    onToggle = { triggerHaptic(context, HapticStyle.TICK); editor.onToggleSub(it) },
                                    onDelete = null,
                                    onAdd = { editor.onAddSub(current.id, it) },
                                    showAdd = false
                                )
                            }
                            if (wantsMedTimer(current) || medTimer?.taskId == current.id) {
                                Spacer(Modifier.height(24.dp))
                                MeditationTimerPill(medTimer?.takeIf { it.taskId == current.id }, medDefault, onMedStart, onMedAdd, onMedClear)
                            }
                        }
                    }
                }
            }

            if (task != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Helpers on the left and the next arrow on the right share the leftover width, so the pill stays centred.
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (!diceMode) {
                        IconButton(onClick = { triggerHaptic(context, HapticStyle.TICK); showStuck = true }, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = "I'm stuck", tint = muted)
                        }
                        Spacer(Modifier.width(8.dp))
                    } else Spacer(Modifier.width(60.dp))
                    val running = timerEnd != null
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).clickable { if (running) timerEnd = null else startTimer() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (running) {
                            val secs = ((remainingMs + 999) / 1000).toInt()
                            Text("%d:%02d".format(secs / 60, secs % 60), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = primary)
                        } else {
                            Icon(Icons.Outlined.Timer, contentDescription = "Start 2-minute timer", tint = muted)
                        }
                    }
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.size(doneW, doneH).onGloballyPositioned { doneCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }.clip(CircleShape).background(primary)
                            .clickable(enabled = !finishing) {
                                finishing = true
                                triggerHaptic(context, HapticStyle.CONFIRM)
                                onFinish(task, doneCenter)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(if (landscape) 38.dp else 30.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            triggerHaptic(context, HapticStyle.TICK)
                            if (diceMode) scope.launch {
                                diceSpin.snapTo(0f)
                                val tumble = launch { diceSpin.animateTo(360f, tween(480, easing = FastOutSlowInEasing)) }
                                Sfx.play(Sound.TICK)
                                delay(340)
                                onMoveToNext(task)
                                tumble.join()
                            } else { goingBack = false; onMoveToNext(task) }
                        },
                        enabled = remainingCount > 1, modifier = Modifier.size(52.dp)
                    ) {
                        Icon(if (diceMode) Icons.Outlined.Casino else Icons.Outlined.ArrowForward, contentDescription = if (diceMode) "Another one" else "Next task", tint = if (remainingCount > 1) muted else muted.copy(alpha = 0.25f), modifier = Modifier.graphicsLayer { rotationZ = if (diceMode) diceSpin.value else 0f })
                    }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)).clickable { exit() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to list", tint = muted) }
                }
            }
        }

        // Entering a new section: a rule with the separator's title (just the rule when it has none) settles in for a moment.
        var bannerText by remember { mutableStateOf<String?>(null) }
        val bannerAlpha = remember { Animatable(0f) }
        LaunchedEffect(sectionBanner, task?.id) {
            if (sectionBanner == null) return@LaunchedEffect
            bannerText = sectionBanner
            delay(500) // after the next task has slid in
            bannerAlpha.animateTo(1f, tween(450))
            delay(2200)
            bannerAlpha.animateTo(0f, tween(600))
            onBannerDone()
        }
        if (bannerAlpha.value > 0.01f && bannerText != null) {
            // Sits in the empty middle of the top bar, between the back arrow and the eye, but always below a camera cutout. Undo is kept below it.
            val named = bannerText!!.isNotBlank()
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = bannerTop).height(20.dp)
                    .graphicsLayer { alpha = bannerAlpha.value; translationY = (bannerAlpha.value - 1f) * 10f },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(if (named) 16.dp else 36.dp).height(1.dp).background(primary.copy(alpha = 0.55f)))
                if (named) {
                    Text(bannerText!!.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp).widthIn(max = if (landscape) 280.dp else 140.dp),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold), color = primary)
                    Box(Modifier.width(16.dp).height(1.dp).background(primary.copy(alpha = 0.55f)))
                }
            }
        }

        // Undo sits just above the check button, for a few seconds after finishing a task.
        if (undoAlpha > 0.01f) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = maxOf((LocalConfiguration.current.screenHeightDp * 0.10f).dp, 64.dp, bannerTop + 32.dp))) {
                    Row(
                        modifier = Modifier.graphicsLayer { alpha = undoAlpha }.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable(enabled = undoTask != null) {
                                undoTask?.let { triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.UNDO); goingBack = true; onUndo(it) }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Undo, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Undo", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = primary)
                    }
            }
        }

        // One badge in the top-right corner for every 2 minutes of focus put into this task.
        val sessions = task?.focusSessions ?: 0
        if (sessions > 0) TimerBadgesFit(sessions, size = 26.dp, modifier = Modifier.align(Alignment.TopEnd).graphicsLayer { alpha = entryAlpha.value }.padding(top = 60.dp, end = 20.dp).widthIn(max = 230.dp))

    }

    if (showStuck && task != null) {
        ModalBottomSheet(onDismissRequest = { showStuck = false }, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, scrimColor = Color.Black.copy(alpha = 0.7f)) {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 16.dp)) {
                Text("What's in the way?", style = MaterialTheme.typography.titleMedium, color = muted, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                StuckOption(Icons.Outlined.Timer, "Just try 2 minutes") { showStuck = false; startTimer() }
                StuckOption(Icons.Outlined.Checklist, "Break it into steps") { showStuck = false; showEditor = true }
                StuckOption(Icons.Outlined.ArrowBack, "Do something else first") { showStuck = false; exit() }
            }
        }
    }

    if (showEditor && task != null) {
        TaskEditorSheet(task = task, subtasks = subtasksByTask[task.id].orEmpty(), actions = editor, onClose = { showEditor = false })
    }
}

data class MedTimer(val taskId: Long, val total: Int, val endsAt: Long, val done: Boolean)

private val MED_WORDS = Regex("meditat|mindful|focus|breath|pranayama|zazen|contemplat|concentrat|stillness|silence|\\bpray|deep work|\\bzen\\b|\\bsit(ting)? quietly", RegexOption.IGNORE_CASE)
fun wantsMedTimer(t: TaskEntity): Boolean = MED_WORDS.containsMatchIn(t.title) || MED_WORDS.containsMatchIn(t.notes)

private fun clock(sec: Long): String { val s = sec.coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

/** The same little pill as the website's cooking-mode timer: outlined presets, then a running countdown with +time and a cancel. */
@Composable
fun MeditationTimerPill(timer: MedTimer?, defaultMin: Int, onStart: (Int) -> Unit, onAdd: (Int) -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // Day pages: black, but moderately dulled so the task title leads and the timer is a quiet add-on. Dark pages: white, with the accent while running.
    val lightPage = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val base = MaterialTheme.colorScheme.onBackground
    val fg = if (lightPage) base.copy(alpha = 0.62f) else base
    val edgeIdle = base.copy(alpha = if (lightPage) 0.38f else 0.18f)
    // While it runs the timer is the thing in use, so on day pages it is proper black; at rest it is the dulled add-on.
    val inkRun = if (lightPage) base else primary
    val edgeRun = if (lightPage) base else primary
    val chipBg = fg.copy(alpha = 0.14f)
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(timer?.endsAt, timer?.done) { while (timer != null && !timer.done) { now = android.os.SystemClock.elapsedRealtime(); delay(250) } }
    val label = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp, fontFeatureSettings = "tnum")
    if (timer == null) {
        // One capsule: tap to start the default length (set in Settings).
        Row(
            modifier = Modifier.height(52.dp).clip(CircleShape).border(1.5.dp, edgeIdle, CircleShape)
                .clickable { triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.TIMER_START); onStart(defaultMin) }.padding(start = 18.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("$defaultMin min", style = label.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium), color = fg)
        }
    } else {
        val done = timer.done
        // When it ends the capsule just settles from the accent to quiet white and grey over a couple of seconds. No fill, no flashing.
        val edge by animateColorAsState(if (done) edgeIdle else edgeRun, tween(2200), label = "medEdge")
        val ink by animateColorAsState(if (done) fg else inkRun, tween(2200), label = "medInk")
        Row(
            modifier = Modifier.height(48.dp).clip(CircleShape).border(1.5.dp, edge, CircleShape).padding(start = 20.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (done) "Done" else clock((timer.endsAt - now + 999) / 1000), style = label.copy(fontSize = 20.sp), color = ink)
            Spacer(Modifier.width(10.dp))
            listOf(60 to "+1m", 300 to "+5m").forEach { (sec, chipLabel) ->
                Box(
                    Modifier.height(28.dp).clip(CircleShape).background(chipBg)
                        .clickable { triggerHaptic(context, HapticStyle.TICK); onAdd(sec) }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(chipLabel, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = muted) }
                Spacer(Modifier.width(6.dp))
            }
            Box(Modifier.size(36.dp).clip(CircleShape).clickable { triggerHaptic(context, HapticStyle.TICK); onClear() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, contentDescription = if (done) "Dismiss timer" else "Cancel timer", tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun gaussianStops(c: Color, peak: Float): Array<Pair<Float, Color>> =
    Array(11) { i ->
        val t = i / 10f
        t to c.copy(alpha = if (i == 10) 0f else (peak * kotlin.math.exp(-2.4f * t * t)).coerceIn(0f, 1f))
    }

/** Flat page colour of each list. Used by both the live page and the wash, so they are identical. */
private fun playlistFlat(key: String): Color = when (key) {
    "morning" -> Color(0xFFFFF0A3)   // sunny pastel yellow
    "night" -> Color(0xFF17134A)     // deep indigo
    "cleaning" -> Color(0xFF0A3B30)  // deep mint-teal
    else -> Color.Black
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlaylistGlows(key: String, a: Float) {
    if (a <= 0.01f) return
    drawRect(playlistFlat(key).copy(alpha = a))
}

private fun pageBase(key: String): Color = playlistFlat(key)

/** The add bar plus a raised centre tab, as one outline. */
class DockShape(private val tabW: Float, private val tabH: Float, private val barR: Float, private val tabR: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val bar = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, tabH, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(barR)))
        }
        val left = (size.width - tabW) / 2f
        val tab = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(
                left, 0f, left + tabW, tabH + barR,
                topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(tabR), topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(tabR),
                bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero, bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero
            ))
        }
        val out = androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Union, bar, tab)
        return androidx.compose.ui.graphics.Outline.Generic(out)
    }
}

/** Flat colour behind the Morning, Night and Cleaning lists. */
@Composable
fun PlaylistBackdrop(playlist: String) {
    val morning by animateFloatAsState(if (playlist == "morning") 1f else 0f, tween(120), label = "morningGlow")
    val night by animateFloatAsState(if (playlist == "night") 1f else 0f, tween(120), label = "nightGlow")
    val cleaning by animateFloatAsState(if (playlist == "cleaning") 1f else 0f, tween(120), label = "cleaningGlow")
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawPlaylistGlows("morning", morning)
            drawPlaylistGlows("night", night)
            drawPlaylistGlows("cleaning", cleaning)
        }
    )
}

private class PlaylistGlyph(val key: String, val name: String, val off: androidx.compose.ui.graphics.vector.ImageVector, val on: androidx.compose.ui.graphics.vector.ImageVector)

/**
 * Morning and Night, with the cleaning dice hidden off to the right (swipe left to reveal it).
 * All three are the accent colour inside a full ring. Tap the dice to roll; long-press it to open the cleaning list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistGlyphs(selected: String, onSelect: (String, Offset) -> Unit, onRoll: () -> Unit, onCleaningToggle: (Offset) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    val centers = remember { mutableMapOf<String, Offset>() }
    val glyphs = remember {
        listOf(
            PlaylistGlyph("morning", "Morning", Icons.Outlined.WbTwilight, Icons.Filled.WbTwilight),
            PlaylistGlyph("night", "Night", Icons.Outlined.Bedtime, Icons.Filled.Bedtime),
            PlaylistGlyph("cleaning", "Cleaning dice", Icons.Outlined.Casino, Icons.Filled.Casino)
        )
    }
    val ink = Color(0xFF111111)
    val onDayPage = selected == "morning"
    // Resting buttons are the accent yellow (a touch deeper on the light Morning page so they stay visible).
    val rest = if (onDayPage) ink else ElectricYellow
    // Small and close together; exactly two fit the window, the dice waits out of sight to the right.
    Row(
        modifier = Modifier.width(80.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        glyphs.forEach { g ->
            val isOn = selected == g.key
            // Morning page: the active sun turns dark. Night / Cleaning pages: the active button fills with the accent.
            val fill by animateColorAsState(if (isOn) rest.copy(alpha = 0.16f) else Color.Transparent, tween(250), label = "glyphFill")
            val tint by animateColorAsState(rest.copy(alpha = if (isOn) 0.9f else 0.45f), tween(250), label = "glyphTint")
            Box(
                modifier = Modifier.size(32.dp)
                    .onGloballyPositioned { centers[g.key] = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
                    .clip(CircleShape)
                    .background(fill, CircleShape)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = {
                            if (g.key == "cleaning") {
                                if (!spin.isRunning) scope.launch {
                                    // The dice tumbles for a moment before it picks something.
                                    spin.snapTo(0f)
                                    val tumble = launch { spin.animateTo(720f, tween(620, easing = FastOutSlowInEasing)) }
                                    repeat(3) { triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.TICK); delay(140) }
                                    tumble.join()
                                    onRoll()
                                }
                            } else onSelect(if (isOn) "main" else g.key, centers[g.key] ?: Offset.Zero)
                        },
                        onLongClick = { if (g.key == "cleaning") onCleaningToggle(centers[g.key] ?: Offset.Zero) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOn) g.on else g.off,
                    contentDescription = if (g.key == "cleaning") "Random tiny clean. Long-press to open the cleaning list" else if (isOn) "${g.name} list is on. Tap to go back to the main list" else "${g.name} list",
                    tint = tint,
                    modifier = Modifier.size(17.dp).graphicsLayer { if (g.key == "cleaning") rotationZ = spin.value }
                )
            }
        }
    }
}

@Composable
fun HappyFace(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ink = Color(0xFF1A1400)
        val stroke = Stroke(width = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val eye = w * 0.2f
        // happy closed eyes: ^ ^
        for (cx in listOf(w * 0.28f, w * 0.72f)) {
            drawArc(ink, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(cx - eye / 2, h * 0.26f), size = androidx.compose.ui.geometry.Size(eye, eye), style = stroke)
        }
        // rosy cheeks
        for (cx in listOf(w * 0.12f, w * 0.88f)) drawCircle(Color(0xFFFF7043).copy(alpha = 0.5f), radius = w * 0.08f, center = Offset(cx, h * 0.56f))
        // open smile with a tiny tongue
        drawArc(ink, startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(w * 0.3f, h * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.3f))
        drawArc(Color(0xFFFF6F61), startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(w * 0.4f, h * 0.6f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.2f))
    }
}

/** One badge per completed 2-minute timer; collapses to a number only when they would not fit. */
@Composable
fun TimerBadgesFit(count: Int, size: Dp, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier) {
        val gap = 5.dp
        if ((size + gap) * count - gap <= maxWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                repeat(count) {
                    Box(Modifier.size(size).clip(CircleShape).background(primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Timer, contentDescription = "2 minutes of focus", tint = primary, modifier = Modifier.size(size * 0.58f))
                    }
                }
            }
        } else {
            FocusBadge(count)
        }
    }
}

@Composable
fun FocusBadge(count: Int, large: Boolean = false) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.clip(CircleShape).background(primary.copy(alpha = 0.14f)).padding(horizontal = if (large) 12.dp else 8.dp, vertical = if (large) 6.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Timer, contentDescription = null, tint = primary, modifier = Modifier.size(if (large) 16.dp else 13.dp))
        Spacer(Modifier.width(if (large) 6.dp else 4.dp))
        Text(
            if (large) "$count × 2 min" else "$count",
            style = (if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall).copy(fontWeight = FontWeight.SemiBold), color = primary
        )
    }
}

@Composable
fun StuckOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), color = MaterialTheme.colorScheme.onSurface)
    }
}

class EditorActions(
    val onEditTask: (TaskEntity, String, String) -> Unit,
    val onAddSub: (Long, String) -> Unit,
    val onToggleSub: (SubtaskEntity) -> Unit,
    val onDeleteSub: (SubtaskEntity) -> Unit,
    val onSetSessions: (Long, Int) -> Unit
)

@Composable
fun SubtaskList(
    subtasks: List<SubtaskEntity>, onToggle: (SubtaskEntity) -> Unit, onDelete: ((SubtaskEntity) -> Unit)?,
    onAdd: (String) -> Unit, modifier: Modifier = Modifier, showAdd: Boolean = true
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    var draft by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth()) {
        subtasks.forEach { sub ->
            key(sub.id) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onToggle(sub) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CheckShape).background(if (sub.isDone) primary else Color.Transparent)
                            .border(1.5.dp, if (sub.isDone) primary else muted.copy(alpha = 0.6f), CheckShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sub.isDone) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        sub.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                        color = if (sub.isDone) muted.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (onDelete != null) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove step", tint = muted.copy(alpha = 0.6f), modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onDelete(sub) }.padding(6.dp))
                    }
                }
            }
        }
        if (showAdd) Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = muted.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            BasicTextField(
                value = draft, onValueChange = { draft = it }, singleLine = true, modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { if (draft.isNotBlank()) { onAdd(draft); draft = "" } }),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) Text("Add a step", style = MaterialTheme.typography.bodyLarge, color = muted.copy(alpha = 0.6f))
                        inner()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(task: TaskEntity, subtasks: List<SubtaskEntity>, actions: EditorActions, onClose: () -> Unit) {
    var titleV by remember(task.id) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(task.title)) }
    var notesV by remember(task.id) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(task.notes)) }
    val title = titleV.text
    val notes = notesV.text
    var focusedField by remember { mutableIntStateOf(0) } // 1 = title, 2 = notes
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val close = { actions.onEditTask(task, title, notes); onClose() }
    // Enter saves and closes instead of adding a line. Pasted multi-line text just has its breaks turned into spaces.
    fun onEdit(old: androidx.compose.ui.text.input.TextFieldValue, new: androidx.compose.ui.text.input.TextFieldValue, set: (androidx.compose.ui.text.input.TextFieldValue) -> Unit) {
        if (!new.text.contains('\n')) { set(new); return }
        val flat = new.text.replace("\r", "").replace('\n', ' ')
        if (new.text.length == old.text.length + 1 && new.text.replace("\n", "") == old.text) close() else set(new.copy(text = flat))
    }

    InWindowSheet(onDismiss = close) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            BasicTextField(
                value = titleV, onValueChange = { onEdit(titleV, it) { v -> titleV = v } },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(primary), modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedField = 1 },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = notesV, onValueChange = { onEdit(notesV, it) { v -> notesV = v } },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = muted),
                cursorBrush = SolidColor(primary), modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedField = 2 },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { inner ->
                    Box {
                        if (notes.isEmpty()) Text("Add a note", style = MaterialTheme.typography.bodyLarge, color = muted.copy(alpha = 0.5f))
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("2-minute sessions", style = MaterialTheme.typography.labelLarge, color = muted)
                    Spacer(Modifier.height(8.dp))
                    if (task.focusSessions > 0) TimerBadgesFit(task.focusSessions, size = 22.dp)
                    else Text("None yet", style = MaterialTheme.typography.bodyMedium, color = muted.copy(alpha = 0.6f))
                }
                IconButton(onClick = { actions.onSetSessions(task.id, task.focusSessions - 1) }, enabled = task.focusSessions > 0) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Remove a session", tint = if (task.focusSessions > 0) muted else muted.copy(alpha = 0.3f))
                }
                IconButton(onClick = { actions.onSetSessions(task.id, task.focusSessions + 1) }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add a session", tint = primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
            Spacer(Modifier.height(8.dp))
            SubtaskList(
                subtasks = subtasks, onToggle = actions.onToggleSub, onDelete = actions.onDeleteSub,
                onAdd = { actions.onAddSub(task.id, it) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(archivedTasks: List<TaskEntity>, onRestore: (TaskEntity) -> Unit, onClearArchive: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var showOlder by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) }
                Text("ARCHIVE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = muted)
                if (archivedTasks.isNotEmpty()) {
                    IconButton(onClick = onClearArchive) { Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear all", tint = MaterialTheme.colorScheme.primary) }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    ) { padding ->
        if (archivedTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nothing archived yet.", color = muted.copy(alpha = 0.7f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                item(key = "hint") {
                    Text("Use the arrow on the right to send a task back to its list.", style = MaterialTheme.typography.bodySmall, color = muted.copy(alpha = 0.6f), modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
                }
                val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val recent = archivedTasks.filter { it.archivedAt >= weekAgo }
                val older = archivedTasks.filter { it.archivedAt < weekAgo }
                if (recent.isEmpty()) item(key = "norecent") {
                    Text("Nothing archived in the last 7 days.", style = MaterialTheme.typography.bodyMedium, color = muted.copy(alpha = 0.6f), modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
                }
                items(recent, key = { it.id }) { t -> ArchiveRow(t, onRestore) }
                if (older.isNotEmpty()) {
                    item(key = "olderHeader") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp)).clickable { triggerHaptic(context, HapticStyle.TICK); showOlder = !showOlder }.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("OLDER · ${older.size}", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium), color = muted.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
                            Icon(if (showOlder) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = if (showOlder) "Fold older tasks" else "Show older tasks", tint = muted.copy(alpha = 0.7f))
                        }
                    }
                    if (showOlder) items(older, key = { it.id }) { t -> ArchiveRow(t, onRestore) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.ArchiveRow(t: TaskEntity, onRestore: (TaskEntity) -> Unit) {
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.animateItemPlacement().fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(22.dp).clip(CheckShape)
                    .background(if (t.isCompleted) muted.copy(alpha = 0.35f) else Color.Transparent)
                    .then(if (t.isCompleted) Modifier else Modifier.border(1.5.dp, muted.copy(alpha = 0.5f), CheckShape)),
                contentAlignment = Alignment.Center
            ) {
                if (t.isCompleted) Icon(Icons.Default.Check, contentDescription = "Completed", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(14.dp))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp, horizontal = 6.dp)) {
            Text(t.title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, textDecoration = if (t.isCompleted) TextDecoration.LineThrough else TextDecoration.None), color = muted.copy(alpha = 0.55f))
            if (t.notes.isNotBlank()) {
                Text(t.notes, style = MaterialTheme.typography.bodySmall, color = muted.copy(alpha = 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (t.focusSessions > 0) TimerBadgesFit(t.focusSessions, size = 20.dp, modifier = Modifier.padding(top = 6.dp))
        }
        IconButton(onClick = { triggerHaptic(context, HapticStyle.TICK); Sfx.play(Sound.UNDO); onRestore(t) }) {
            Icon(Icons.Outlined.Unarchive, contentDescription = "Send back to the list", tint = muted.copy(alpha = 0.7f), modifier = Modifier.size(21.dp))
        }

    }
}

