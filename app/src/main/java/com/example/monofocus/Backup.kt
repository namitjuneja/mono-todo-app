package com.example.monofocus

/**
 * Export and import of everything MONO holds, as one plain, readable .txt file:
 * settings, every list (with its steps and separators) and the archive.
 */
object Backup {
    class Parsed(val tasks: List<TaskEntity>, val steps: List<Pair<Int, SubtaskEntity>>, val settings: Map<String, String>) {
        val separators get() = tasks.count { it.isSeparator }
        val real get() = tasks.count { !it.isSeparator && !it.archivedFromList }
        val archived get() = tasks.count { !it.isSeparator && it.archivedFromList }
    }

    private val lists = listOf("main", "morning", "night", "cleaning")

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n")
    private fun unesc(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { sb.append(if (s[i + 1] == 'n') '\n' else s[i + 1]); i += 2 } else { sb.append(c); i++ }
        }
        return sb.toString()
    }

    suspend fun export(dao: TaskDao, settings: Map<String, String>): String {
        val tasks = dao.allTasksOnce()
        val subs = dao.allSubtasksOnce().groupBy { it.taskId }
        val out = StringBuilder()
        out.append("MONO BACKUP 1\n")
        out.append("exported: ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())).append("\n\n")
        out.append("[settings]\n")
        settings.forEach { (k, v) -> out.append(k).append(" = ").append(v).append("\n") }

        fun writeTask(t: TaskEntity, withArchivedAt: Boolean) {
            if (t.isSeparator) {
                out.append(if (t.title.isBlank()) "---" else "--- ${esc(t.title)} ---").append("\n")
                if (t.duration.isNotBlank()) out.append("  time: ").append(esc(t.duration)).append("\n")
                return
            }
            out.append(if (t.isCompleted) "[x] " else "[ ] ").append(esc(t.title)).append("\n")
            if (t.duration.isNotBlank()) out.append("  time: ").append(esc(t.duration)).append("\n")
            if (t.notes.isNotBlank()) out.append("  note: ").append(esc(t.notes)).append("\n")
            if (t.focusSessions > 0) out.append("  timers: ").append(t.focusSessions).append("\n")
            if (t.doneCount > 0) out.append("  done: ").append(t.doneCount).append("\n")
            if (withArchivedAt && t.archivedAt > 0) out.append("  archived: ").append(t.archivedAt).append("\n")
            subs[t.id].orEmpty().sortedBy { it.position }.forEach { out.append("  - ").append(if (it.isDone) "[x] " else "[ ] ").append(esc(it.title)).append("\n") }
        }

        for (list in lists) {
            val live = tasks.filter { it.playlist == list && !it.archivedFromList }.sortedBy { it.queueOrder }
            if (live.isEmpty()) continue
            out.append("\n[list ").append(list).append("]\n")
            live.forEach { writeTask(it, false) }
        }
        for (list in lists) {
            val old = tasks.filter { it.playlist == list && it.archivedFromList && !it.isSeparator }.sortedBy { it.archivedAt }
            if (old.isEmpty()) continue
            out.append("\n[archive ").append(list).append("]\n")
            old.forEach { writeTask(it, true) }
        }
        return out.toString()
    }

    /** Null when the text is not a MONO backup. */
    fun parse(text: String): Parsed? {
        val lines = text.replace("\r", "").split("\n")
        if (lines.firstOrNull()?.trim()?.startsWith("MONO BACKUP") != true) return null
        val tasks = ArrayList<TaskEntity>()
        val steps = ArrayList<Pair<Int, SubtaskEntity>>()
        val settings = LinkedHashMap<String, String>()
        var section = ""
        var list = "main"
        var archive = false
        var order = 0L
        fun last() = tasks.lastIndex
        for (raw in lines.drop(1)) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue
            if (line.startsWith("[settings]")) { section = "settings"; continue }
            val header = Regex("""^\[(list|archive) (\w+)]$""").matchEntire(line)
            if (header != null) { section = "tasks"; archive = header.groupValues[1] == "archive"; list = header.groupValues[2]; continue }
            when (section) {
                "settings" -> { val i = line.indexOf('='); if (i > 0) settings[line.substring(0, i).trim()] = line.substring(i + 1).trim() }
                "tasks" -> {
                    order += 10
                    when {
                        line.startsWith("---") -> {
                            val label = unesc(line.removePrefix("---").removeSuffix("---").trim())
                            tasks.add(TaskEntity(title = label, queueOrder = order, playlist = list, isSeparator = true))
                        }
                        line.startsWith("[ ] ") || line.startsWith("[x] ") -> {
                            val done = line.startsWith("[x] ")
                            tasks.add(TaskEntity(
                                title = unesc(line.substring(4)), queueOrder = order, playlist = list, isCompleted = done,
                                completedAt = if (done) System.currentTimeMillis() else null, archivedFromList = archive,
                                archivedAt = if (archive) System.currentTimeMillis() else 0L
                            ))
                        }
                        line.startsWith("  - [") && tasks.isNotEmpty() -> {
                            order -= 10
                            val body = line.removePrefix("  - ")
                            steps.add(last() to SubtaskEntity(taskId = 0, title = unesc(body.drop(4)), isDone = body.startsWith("[x]"), position = steps.size.toLong()))
                        }
                        line.startsWith("  ") && tasks.isNotEmpty() -> {
                            order -= 10
                            val t = tasks[last()]
                            val body = line.trim()
                            when {
                                body.startsWith("time:") -> tasks[last()] = t.copy(duration = unesc(body.removePrefix("time:").trim()))
                                body.startsWith("note:") -> tasks[last()] = t.copy(notes = unesc(body.removePrefix("note:").trim()))
                                body.startsWith("timers:") -> tasks[last()] = t.copy(focusSessions = body.removePrefix("timers:").trim().toIntOrNull() ?: 0)
                                body.startsWith("done:") -> tasks[last()] = t.copy(doneCount = body.removePrefix("done:").trim().toIntOrNull() ?: 0)
                                body.startsWith("archived:") -> tasks[last()] = t.copy(archivedAt = body.removePrefix("archived:").trim().toLongOrNull() ?: t.archivedAt)
                            }
                        }
                        else -> order -= 10
                    }
                }
            }
        }
        return if (tasks.isEmpty() && settings.isEmpty()) null else Parsed(tasks, steps, settings)
    }

    private suspend fun insertAll(dao: TaskDao, p: Parsed, base: Long, only: Set<Int>? = null) {
        p.tasks.forEachIndexed { i, t ->
            if (only != null && i !in only) return@forEachIndexed
            val newId = dao.insert(t.copy(queueOrder = base + t.queueOrder))
            p.steps.filter { it.first == i }.forEach { dao.insertSubtask(it.second.copy(taskId = newId)) }
        }
    }

    /** Adds everything from the file to the end of the matching lists. Nothing is skipped or merged. Returns how many items were added. */
    suspend fun importAdd(dao: TaskDao, p: Parsed): Int {
        val base = (dao.getMaxQueueOrder() ?: 0L) + 10L
        insertAll(dao, p, base)
        return p.tasks.size
    }

    suspend fun importReplace(dao: TaskDao, p: Parsed) {
        dao.deleteAllSubtasks()
        dao.deleteAllTasks()
        insertAll(dao, p, 0L)
    }
}
