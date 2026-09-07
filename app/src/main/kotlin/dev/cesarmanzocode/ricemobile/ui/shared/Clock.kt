package dev.cesarmanzocode.ricemobile.ui.shared

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.LocalLocale
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Pure formatting shared by every rice's clock (contract §16 task 6, §18.1: "Puede compartir
 * provider/lógica. No debe compartir obligatoriamente composición"). Each rice calls these and
 * lays out the result with its own hierarchy/typography; there is no shared visual clock widget.
 */

/** Re-reads on every recomposition triggered by a configuration change, so a mid-session
 * 12h/24h system toggle is honored while a rice is visible (contract §3.5). */
@Composable
fun rememberIs24HourFormat(): Boolean {
    val context = LocalContext.current
    LocalConfiguration.current
    return DateFormat.is24HourFormat(context)
}

@Composable
fun rememberCurrentLocale(): Locale = LocalLocale.current.platformLocale

/** "14:07" (24h) or "2:07" (12h, no leading zero — contract §18.2 allows AM/PM separately). */
fun formatClockTime(now: ZonedDateTime, is24Hour: Boolean, locale: Locale): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm"
    return DateTimeFormatter.ofPattern(pattern, locale).format(now)
}

/** "AM"/"PM" in the current locale; callers only need this when [formatClockTime] used 12h. */
fun formatClockAmPm(now: ZonedDateTime, locale: Locale): String =
    DateTimeFormatter.ofPattern("a", locale).format(now)

fun formatClockDate(now: ZonedDateTime, locale: Locale, style: FormatStyle = FormatStyle.FULL): String =
    now.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(style).withLocale(locale))
