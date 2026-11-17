package com.example.monofocus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun formatMinutes(context: android.content.Context, minutes: Int): String {
    val c = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, minutes / 60); set(java.util.Calendar.MINUTE, minutes % 60) }
    return android.text.format.DateFormat.getTimeFormat(context).format(c.time)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 12.dp).heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}

/** The same rounded-square check box the lists use. */
@Composable
private fun QuietCheck(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier.offset(x = 9.dp).size(40.dp).clip(CircleShape).clickable(enabled = enabled) { onChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        // Accent outline always; the tick itself is the accent when it is on.
        Box(
            Modifier.size(22.dp).clip(CheckShape)
                .border(1.5.dp, primary, CheckShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = primary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun TimePill(text: String, onClick: () -> Unit) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp)).padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinutes: Int, title: String, onDone: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = android.text.format.DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = {
            val primary = MaterialTheme.colorScheme.primary
            val cs = MaterialTheme.colorScheme
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = cs.surfaceVariant, clockDialSelectedContentColor = cs.onPrimary, clockDialUnselectedContentColor = cs.onSurface,
                    selectorColor = primary,
                    periodSelectorBorderColor = cs.outline, periodSelectorSelectedContainerColor = primary.copy(alpha = 0.18f), periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = primary, periodSelectorUnselectedContentColor = cs.onSurfaceVariant,
                    timeSelectorSelectedContainerColor = primary.copy(alpha = 0.18f), timeSelectorUnselectedContainerColor = cs.surfaceVariant,
                    timeSelectorSelectedContentColor = primary, timeSelectorUnselectedContentColor = cs.onSurface
                )
            )
        },
        confirmButton = { TextButton(onClick = { onDone(state.hour * 60 + state.minute) }) { Text("Set", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
fun SettingsScreen(
    archivedCount: Int, onOpenArchive: () -> Unit,
    morningMin: Int, nightMin: Int, onMorningMin: (Int) -> Unit, onNightMin: (Int) -> Unit,
    autoNext: Boolean, onAutoNext: (Boolean) -> Unit, stopAtSep: Boolean, onStopAtSep: (Boolean) -> Unit,
    autoKeyboard: Boolean, onAutoKeyboard: (Boolean) -> Unit,
    soundOn: Boolean, onSound: (Boolean) -> Unit, hapticsOn: Boolean, onHaptics: (Boolean) -> Unit,
    eyeLocks: Boolean, onEyeLocks: (Boolean) -> Unit,
    medDefault: Int, onMedDefault: (Int) -> Unit,
    sleepOn: Boolean, sleepMin: Int, onSleepOn: (Boolean) -> Unit, onSleepMin: (Int) -> Unit,
    isDark: Boolean, onDark: (Boolean) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var editing by remember { mutableStateOf<String?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(17.dp)) }
            Text("Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light, letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 32.dp)) {
            SectionLabel("Archive")
            SettingRow("Archived notes", if (archivedCount == 0) "Nothing archived" else "$archivedCount ${if (archivedCount == 1) "task" else "tasks"}", onClick = onOpenArchive) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = muted.copy(alpha = 0.7f))
            }

            SectionLabel("Lists")
            SettingRow("Day tasks reset", "Morning unchecks itself at this time") { TimePill(formatMinutes(context, morningMin)) { editing = "morning" } }
            SettingRow("Night tasks reset", "Night unchecks itself at this time") { TimePill(formatMinutes(context, nightMin)) { editing = "night" } }
            SettingRow("Start the next task", "Open it automatically after you finish one", onClick = { onAutoNext(!autoNext) }) { QuietCheck(autoNext, onAutoNext) }
            Box(Modifier.alpha(if (autoNext) 1f else 0.4f)) {
                SettingRow("Stop at separators", "Return to the list at a separator instead of skipping it", onClick = if (autoNext) ({ onStopAtSep(!stopAtSep) }) else null) { QuietCheck(stopAtSep, onStopAtSep, enabled = autoNext) }
            }

            SettingRow("Keyboard on tapped spot", "Open the keyboard when you tap a gap to choose where to add", onClick = { onAutoKeyboard(!autoKeyboard) }) { QuietCheck(autoKeyboard, onAutoKeyboard) }

            SectionLabel("Eye")
            SettingRow("Lock the phone", "Off: the eye only keeps the screen on, without locking the phone", onClick = { onEyeLocks(!eyeLocks) }) { QuietCheck(eyeLocks, onEyeLocks) }

            SectionLabel("Meditation")
            SettingRow("Timer length", "Default for the timer on meditate and focus tasks") {
                val steps = listOf(2, 3, 5, 10, 15, 20, 30, 45, 60)
                val i = steps.indexOf(medDefault).let { if (it >= 0) it else steps.indexOfLast { s -> s <= medDefault }.coerceAtLeast(0) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (i > 0) onMedDefault(steps[i - 1]) }, enabled = i > 0) { Icon(Icons.Outlined.Remove, contentDescription = "Shorter", tint = if (i > 0) MaterialTheme.colorScheme.primary else muted.copy(alpha = 0.3f)) }
                    Text("$medDefault min", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.widthIn(min = 64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { if (i < steps.lastIndex) onMedDefault(steps[i + 1]) }, enabled = i < steps.lastIndex) { Icon(Icons.Outlined.Add, contentDescription = "Longer", tint = if (i < steps.lastIndex) MaterialTheme.colorScheme.primary else muted.copy(alpha = 0.3f)) }
                }
            }

            SectionLabel("Sleep")
            SettingRow("Sleep countdown", "A quiet notification, on the lock screen too, showing the time left until bed", onClick = { onSleepOn(!sleepOn) }) { QuietCheck(sleepOn, onSleepOn) }
            Box(Modifier.alpha(if (sleepOn) 1f else 0.4f)) {
                SettingRow("Sleep time", "When the countdown reaches zero") { TimePill(formatMinutes(context, sleepMin)) { if (sleepOn) editing = "sleep" } }
            }

            SectionLabel("Feedback")
            SettingRow("Sound", onClick = { onSound(!soundOn) }) { QuietCheck(soundOn, onSound) }
            SettingRow("Haptics", onClick = { onHaptics(!hapticsOn) }) { QuietCheck(hapticsOn, onHaptics) }

            SectionLabel("Appearance")
            SettingRow("Theme") { Segmented(listOf("Dark", "Light"), if (isDark) 0 else 1) { onDark(it == 0) } }

            SectionLabel("Data")
            SettingRow("Export backup", "Save lists, steps and settings to a text file", onClick = onExport)
            SettingRow("Import backup", "Add to the end of your lists, or overwrite them", onClick = onImport)
        }
    }

    editing?.let { which ->
        TimeDialog(
            initialMinutes = when (which) { "morning" -> morningMin; "sleep" -> sleepMin; else -> nightMin },
            title = when (which) { "morning" -> "Day tasks reset at"; "sleep" -> "Sleep time"; else -> "Night tasks reset at" },
            onDone = { m -> when (which) { "morning" -> onMorningMin(m); "sleep" -> onSleepMin(m); else -> onNightMin(m) }; editing = null },
            onDismiss = { editing = null }
        )
    }
}
