package dev.cesarmanzocode.ricemobile.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** Real battery state — no permission required, this is the same sticky broadcast every app can
 * read. Never fabricated (Sprint 3 second pass §"CERO DATOS FALSOS"). */
data class BatterySnapshot(val percent: Int, val isCharging: Boolean)

private fun Intent.toBatterySnapshot(): BatterySnapshot? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatterySnapshot(percent = (level * 100f / scale).toInt().coerceIn(0, 100), isCharging = isCharging)
}

/**
 * Native replacement for the weather/media modules the mockups show (§"DATOS PERMITIDOS PARA
 * MÓDULOS"): a rice keeps the mockup's module size/mass/position but fills it with this instead.
 * `registerReceiver(null, filter)` returns the last sticky broadcast synchronously with no
 * listener at all; a live receiver is only registered while the host is at least STARTED, and
 * unregistered on stop — never a background service.
 */
@Composable
fun rememberBatterySnapshot(): State<BatterySnapshot?> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState<BatterySnapshot?>(initialValue = null, context, lifecycleOwner) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(null, filter)?.toBatterySnapshot()?.let { value = it }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            callbackFlow {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(receiverContext: Context, intent: Intent) {
                        intent.toBatterySnapshot()?.let { trySend(it) }
                    }
                }
                context.registerReceiver(receiver, filter)
                awaitClose { context.unregisterReceiver(receiver) }
            }.collect { value = it }
        }
    }
}
