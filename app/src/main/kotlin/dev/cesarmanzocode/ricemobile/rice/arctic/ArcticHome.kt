package dev.cesarmanzocode.ricemobile.rice.arctic

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

private val ARCTIC_BACKGROUND = Color(0xFFD8E6EE)
private val ARCTIC_INK = Color(0xFF1E2A30)
private val ARCTIC_DOCK = Color(0x99FFFFFF)

/** Arctic Glass Home structure: a floating horizontal dock of favorites near the bottom. */
@Composable
fun ArcticHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize().background(ARCTIC_BACKGROUND),
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Text(text = "Arctic Glass", color = ARCTIC_INK)
            if (!model.isDefaultHome) {
                TextButton(onClick = actions.requestHomeRole) {
                    Text(text = stringResource(R.string.action_use_as_home))
                }
            }
            Box(modifier = Modifier.weight(1f))
            Dock(favorites = model.favorites, actions = actions)
        }
    }
}

@Composable
private fun Dock(favorites: List<FavoriteSlot>, actions: RiceActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(ARCTIC_DOCK)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (favorites.isEmpty()) {
            Text(text = stringResource(R.string.favorites_empty_hint), color = ARCTIC_INK)
        } else {
            for (slot in favorites) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .combinedClickable(
                            onClick = { slot.app?.let { actions.openApp(it.key) } },
                            onLongClick = { actions.showAppMenu(slot.key) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val app = slot.app
                    if (app != null) {
                        AppIcon(entry = app, size = 40.dp)
                    } else {
                        Text(text = "·", color = ARCTIC_INK)
                    }
                }
            }
        }
        TextButton(onClick = actions.openDrawer) { Text(text = stringResource(R.string.action_open_drawer)) }
        TextButton(onClick = actions.openPicker) { Text(text = stringResource(R.string.action_open_rice_picker)) }
    }
}
