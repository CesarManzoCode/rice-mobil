package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface

private val MONOCHROME_BACKGROUND = Color(0xFF0A0A0A)
private val MONOCHROME_INK = Color(0xFFF5F5F0)

/**
 * Provisional Monochrome Home (contract §14): the real clock, favorites and full §18.2
 * bloque tipográfico land in S2/S3. S1 only needs a working Home surface that opens the
 * drawer and can request the Home role.
 */
@Composable
fun MonochromeHome(
    isDefaultHome: Boolean,
    onOpenDrawer: () -> Unit,
    onRequestHomeRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeGestureSurface(
        onSwipeUp = onOpenDrawer,
        modifier = modifier.fillMaxSize().background(MONOCHROME_BACKGROUND),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "rice-mobile", color = MONOCHROME_INK)

            if (!isDefaultHome) {
                Button(onClick = onRequestHomeRole) {
                    Text(text = stringResource(R.string.action_use_as_home))
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = onOpenDrawer) {
                    Text(text = stringResource(R.string.action_open_drawer))
                }
            }
        }
    }
}
