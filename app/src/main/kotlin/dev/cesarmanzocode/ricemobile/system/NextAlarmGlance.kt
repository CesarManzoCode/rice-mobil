package dev.cesarmanzocode.ricemobile.system

import android.app.AlarmManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** The next alarm clock set by any app via `setAlarmClock` — the exact information the status
 * bar's alarm icon already reflects. No permission, no Notification/Usage access, no calendar
 * read; `null` simply means no app has scheduled one (Sprint 3 second pass §"CERO DATOS FALSOS":
 * a rice must hide/shrink its module rather than invent a time). */
data class NextAlarmSnapshot(val triggerAtMillis: Long)

private const val RECHECK_INTERVAL_MS = 60_000L

@Composable
fun rememberNextAlarmSnapshot(): State<NextAlarmSnapshot?> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState<NextAlarmSnapshot?>(initialValue = null, context, lifecycleOwner) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = alarmManager?.nextAlarmClock?.let { NextAlarmSnapshot(it.triggerTime) }
                delay(RECHECK_INTERVAL_MS)
            }
        }
    }
}
