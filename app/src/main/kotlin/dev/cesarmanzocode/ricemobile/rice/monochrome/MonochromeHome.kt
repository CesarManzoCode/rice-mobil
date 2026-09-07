package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.apps.packageNameGuess
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface

private val MONOCHROME_BACKGROUND = Color(0xFF0A0A0A)
private val MONOCHROME_INK = Color(0xFFF5F5F0)
private val MONOCHROME_DIM = Color(0xFF8A8A85)

/**
 * Monochrome Home structure: vertical composition, favorites as a linear list (contract
 * prompt). Swipe up opens the Drawer; long press on free space opens the Rice Picker.
 */
@Composable
fun MonochromeHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize().background(MONOCHROME_BACKGROUND),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = "rice-mobile", color = MONOCHROME_INK)
                if (!model.isDefaultHome) {
                    TextButton(onClick = actions.requestHomeRole) {
                        Text(text = stringResource(R.string.action_use_as_home))
                    }
                }
            }

            FavoritesList(favorites = model.favorites, actions = actions)

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = actions.openPicker) {
                    Text(text = stringResource(R.string.action_open_rice_picker))
                }
                Button(onClick = actions.openDrawer) {
                    Text(text = stringResource(R.string.action_open_drawer))
                }
            }
        }
    }
}

@Composable
private fun FavoritesList(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = MONOCHROME_DIM)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (slot in favorites) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .combinedClickable(
                        onClick = { slot.app?.let { actions.openApp(it.key) } },
                        onLongClick = { actions.showAppMenu(slot.key) },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val app = slot.app
                if (app != null) {
                    AppIcon(entry = app, size = 32.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = app.label, color = MONOCHROME_INK)
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${slot.key.packageNameGuess} · " +
                            stringResource(R.string.favorite_unavailable),
                        color = MONOCHROME_DIM,
                    )
                }
            }
        }
    }
}
