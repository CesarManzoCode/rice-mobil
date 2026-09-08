package dev.cesarmanzocode.ricemobile.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared loading/empty/error surface for the catalog (contract §14 acceptance). Deliberately
 * minimal — just text and an optional 48dp action — so each rice supplies its own colors/type
 * instead of inheriting Material defaults that would read wrong on a dark or paper background.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    accentColor: Color = textColor,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = textColor)
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, color = accentColor)
            }
        }
    }
}
