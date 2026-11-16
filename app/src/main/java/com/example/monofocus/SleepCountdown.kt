package com.example.monofocus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

// Highest importance an app can ask for, so it ranks above everything else in the list. It stays silent: no sound or vibration.
private const val CHANNEL = "sleep_countdown_v3"
private const val NOTIF_ID = 4201
private val AppSurface = 0xFF0D0D0D.toInt()   // the app's OLED surface
private val AppText = 0xFFF2F2F2.toInt()
private val AppMuted = 0xFF8E8E93.toInt()
private val AppAccent = 0xFFFFEA00.toInt()     // the app's electric yellow

fun sleepEnabled(context: Context): Boolean = context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE).getBoolean("sleep_on", false)
fun sleepMinutes(context: Context): Int = context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE).getInt("sleep_min", 23 * 60)
fun setSleepEnabled(context: Context, on: Boolean) { context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE).edit().putBoolean("sleep_on", on).apply() }
fun setSleepMinutes(context: Context, m: Int) { context.getSharedPreferences("mono_prefs", Context.MODE_PRIVATE).edit().putInt("sleep_min", m).apply() }

/** Starts, refreshes or stops the countdown notification to match the saved setting. */
fun syncSleepNotification(context: Context) {
    if (sleepEnabled(context)) {
        try { androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, SleepCountdownService::class.java)) } catch (_: Exception) {}
    } else context.stopService(Intent(context, SleepCountdownService::class.java))
}

private fun clockText(context: Context, minutes: Int): String {
    val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60) }
    return android.text.format.DateFormat.getTimeFormat(context).format(c.time)
}

private fun buildSleepNotification(context: Context): Notification {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
        nm.deleteNotificationChannel("sleep_countdown"); nm.deleteNotificationChannel("sleep_countdown_v2")
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Sleep countdown", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); enableLights(false)
            description = "Time left until your sleep time"
            setShowBadge(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }
    val sleepMin = sleepMinutes(context)
    val now = System.currentTimeMillis()
    val next = Calendar.getInstance().apply {
        timeInMillis = now; set(Calendar.HOUR_OF_DAY, sleepMin / 60); set(Calendar.MINUTE, sleepMin % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val sinceMin = ((now - (next - 24L * 60 * 60 * 1000)) / 60_000L).toInt()
    val leftMinNow = if (sinceMin < 180) 0 else ((next - now + 59_999L) / 60_000L).toInt()
    // Just the time left, with a bed glyph beside it. Once bedtime arrives it simply says so.
    val title = if (sinceMin < 180) "Bedtime" else fmtSpan(((next - now + 59_999L) / 60_000L).toInt())
    val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    // Numerals warm up gradually through the last three hours.
    val ink = warmInk(leftMinNow)
    fun views(layout: Int) = RemoteViews(context.packageName, layout).apply {
        setTextViewText(R.id.sleep_time, title); setTextColor(R.id.sleep_time, ink)
        setInt(R.id.sleep_glyph, "setColorFilter", AppMuted)   // the glyph stays a quiet grey; only the numerals warm up
    }
    return NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_m)
        .setContentTitle(title)
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setCustomContentView(views(R.layout.notif_sleep_collapsed))
        .setCustomBigContentView(views(R.layout.notif_sleep_expanded))
        .setColor(AppSurface).setColorized(true)
        .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false).setSilent(true)
        // Re-stamped every minute, so among equals it stays the newest and therefore the topmost.
        .setWhen(now).setSortKey("0")
        .setCategory(NotificationCompat.CATEGORY_STATUS).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(open).build()
}

/** The app's own off-white all evening. In the last 3 hours it drifts toward the app's yellow accent: a third of the way at 2h, two thirds at 1h, full accent at bedtime. */
private fun warmInk(minutesLeft: Int): Int {
    val t = (1f - minutesLeft.coerceIn(0, 180) / 180f)   // 0 at 3h left, 1 at bedtime
    return ColorUtils.blendARGB(AppText, AppAccent, t)
}

/** "3h 20m", "3h", "45m". */
private fun fmtSpan(totalMin: Int): String {
    val h = totalMin / 60; val m = totalMin % 60
    return if (h == 0) "${m}m" else if (m == 0) "${h}h" else "${h}h ${m}m"
}

/** Keeps one quiet notification up, redrawn at the top of every minute and whenever the screen turns on. */
class SleepCountdownService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { refresh() }
    }

    private fun refresh() {
        try { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildSleepNotification(this)) } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!sleepEnabled(this)) { stopSelf(); return START_NOT_STICKY }
        ServiceCompat.startForeground(this, NOTIF_ID, buildSleepNotification(this), if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(60_000L - System.currentTimeMillis() % 60_000L + 300L)
                refresh()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenOn, IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_USER_PRESENT) })
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenOn) } catch (_: Exception) {}
        scope.cancel()
        super.onDestroy()
    }
}

/** Brings the countdown back after a reboot or an app update. */
class SleepBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { if (sleepEnabled(context)) syncSleepNotification(context) }
}
