package com.example.monofocus

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager

/** Lets MONO lock the phone (and nothing else) so the screen can stay on without leaving the phone open. */
class MonoAdminReceiver : DeviceAdminReceiver()

/** While on, this activity is drawn above the lock screen. Turning it off hands the screen back to the keyguard. */
fun Activity.setLockedDisplay(on: Boolean) {
    if (Build.VERSION.SDK_INT >= 27) {
        setShowWhenLocked(on)
        setTurnScreenOn(on)
    } else {
        @Suppress("DEPRECATION")
        val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        if (on) window.addFlags(flags) else window.clearFlags(flags)
    }
}

@Suppress("DEPRECATION")
fun wakeScreen(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "mono:wake").acquire(3000)
}
