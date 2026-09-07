package dev.cesarmanzocode.ricemobile.rice.violet

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
import androidx.compose.foundation.shape.CircleShape
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

private val VIOLET_BACKGROUND = Color(0xFF241A38)
private val VIOLET_INK = Color(0xFFEDE6F5)
private val VIOLET_NODE = Color(0xFF3E2E60)

/** Violet Night Home structure: favorites in a deterministic 1-2-2 cluster (contract prompt). */
@Composable
fun VioletHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize().background(VIOLET_BACKGROUND),
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Text(text = "Violet Night", color = VIOLET_INK)
            if (!model.isDefaultHome) {
                TextButton(onClick = actions.requestHomeRole) {
                    Text(text = stringResource(R.string.action_use_as_home))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Cluster(favorites = model.favorites, actions = actions)
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

/** Rows of 1, 2 and 2 favorites, always in the same slots regardless of how many exist. */
@Composable
private fun Cluster(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = VIOLET_INK)
        return
    }
    val rowSizes = listOf(1, 2, 2)
    var index = 0
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        for (rowSize in rowSizes) {
            if (index >= favorites.size) break
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                repeat(rowSize) {
                    if (index < favorites.size) {
                        Node(slot = favorites[index], actions = actions)
                        index++
                    }
                }
            }
        }
    }
}

@Composable
private fun Node(slot: FavoriteSlot, actions: RiceActions) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(VIOLET_NODE)
            .combinedClickable(
                onClick = { slot.app?.let { actions.openApp(it.key) } },
                onLongClick = { actions.showAppMenu(slot.key) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val app = slot.app
        if (app != null) AppIcon(entry = app, size = 44.dp) else Text(text = "?", color = VIOLET_INK)
    }
}
