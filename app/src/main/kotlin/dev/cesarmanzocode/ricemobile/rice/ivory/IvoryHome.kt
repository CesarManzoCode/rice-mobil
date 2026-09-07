package dev.cesarmanzocode.ricemobile.rice.ivory

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.packageNameGuess
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface

private val IVORY_BACKGROUND = Color(0xFFF3ECDF)
private val IVORY_INK = Color(0xFF241E14)
private val IVORY_RULE = Color(0xFFBDB09A)

/** Ivory Paper Home structure: favorites as a textual index/list (contract prompt). */
@Composable
fun IvoryHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize().background(IVORY_BACKGROUND),
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Text(text = "Ivory Paper", color = IVORY_INK)
            HorizontalDivider(color = IVORY_RULE, modifier = Modifier.padding(vertical = 12.dp))
            if (!model.isDefaultHome) {
                TextButton(onClick = actions.requestHomeRole) {
                    Text(text = stringResource(R.string.action_use_as_home))
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Index(favorites = model.favorites, actions = actions)
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
private fun Index(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = IVORY_INK)
        return
    }
    Column {
        for ((i, slot) in favorites.withIndex()) {
            val app = slot.app
            val label = app?.label ?: "${slot.key.packageNameGuess} · ${stringResource(R.string.favorite_unavailable)}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { app?.let { actions.openApp(it.key) } },
                        onLongClick = { actions.showAppMenu(slot.key) },
                    )
                    .padding(vertical = 10.dp),
            ) {
                Text(text = "%02d".format(i + 1), color = IVORY_RULE)
                Text(text = "  $label", color = IVORY_INK)
            }
            HorizontalDivider(color = IVORY_RULE)
        }
    }
}
