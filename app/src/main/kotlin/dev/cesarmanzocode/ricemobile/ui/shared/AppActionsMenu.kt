package dev.cesarmanzocode.ricemobile.ui.shared

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppKey

/**
 * Long-press context menu for an app or favorite slot (contract §7): "Add to favorites" or
 * "Remove from favorites", never a silent toggle. Rendered once by the host as a global
 * overlay above the current screen (contract §6.2), not duplicated per rice.
 */
@Composable
fun AppActionsMenu(
    key: AppKey,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: (AppKey) -> Unit,
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                    ),
                )
            },
            onClick = {
                onToggleFavorite(key)
                onDismiss()
            },
        )
    }
}
