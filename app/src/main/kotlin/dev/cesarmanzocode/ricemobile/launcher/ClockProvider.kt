package dev.cesarmanzocode.ricemobile.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

/**
 * The single source of "now" for every rice's clock (contract §16 task 6, §3.5). Ticks once a
 * minute — never a second — using [java.time], and only while the host is at least STARTED, so
 * it is inert in the background and never becomes a periodic service. Deliberately not part of
 * [LauncherState]: the contract forbids a per-minute value invalidating unrelated composition
 * ("el reloj no puede recomponer toda la lista"), so only the composables that call this recompose.
 */
object ClockProvider {

    @Composable
    fun rememberNow(): State<ZonedDateTime> {
        val lifecycleOwner = LocalLifecycleOwner.current
        return produceState(initialValue = ZonedDateTime.now(), lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val now = ZonedDateTime.now()
                    value = now
                    val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
                    delay(Duration.between(now, nextMinute).toMillis().coerceAtLeast(1_000))
                }
            }
        }
    }
}
