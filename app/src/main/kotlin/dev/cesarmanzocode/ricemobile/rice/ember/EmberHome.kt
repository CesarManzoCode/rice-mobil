package dev.cesarmanzocode.ricemobile.rice.ember

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface

private val EMBER_BACKGROUND = Color(0xFF241512)
private val EMBER_INK = Color(0xFFF2E4D8)
private val EMBER_BLOCK = Color(0xFF3A241C)
private const val COLUMNS = 3

/** Ember Forge Home structure: favorites as a compact matrix/blocks (contract prompt). */
@Composable
fun EmberHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize().background(EMBER_BACKGROUND),
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Text(text = "Ember Forge", color = EMBER_INK)
            if (!model.isDefaultHome) {
                TextButton(onClick = actions.requestHomeRole) {
                    Text(text = stringResource(R.string.action_use_as_home))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FavoritesMatrix(favorites = model.favorites, actions = actions)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = actions.openPicker) {
                    Text(text = stringResource(R.string.action_open_rice_picker))
                }
                TextButton(onClick = actions.openDrawer) {
                    Text(text = stringResource(R.string.action_open_drawer))
                }
            }
        }
    }
}

@Composable
private fun FavoritesMatrix(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = EMBER_INK)
        return
    }
    val rows = favorites.chunked(COLUMNS)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (slot in row) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EMBER_BLOCK)
                            .combinedClickable(
                                onClick = { slot.app?.let { actions.openApp(it.key) } },
                                onLongClick = { actions.showAppMenu(slot.key) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val app = slot.app
                        if (app != null) AppIcon(entry = app, size = 40.dp) else Text(text = "?", color = EMBER_INK)
                    }
                }
            }
        }
    }
}
