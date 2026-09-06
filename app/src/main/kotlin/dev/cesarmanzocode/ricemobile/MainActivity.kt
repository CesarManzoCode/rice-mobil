package dev.cesarmanzocode.ricemobile

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val roleManager: RoleManager by lazy { getSystemService(RoleManager::class.java) }

    private var isDefaultHome by mutableStateOf(false)

    private val requestHome = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshHomeRole() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiceMobileBootstrap(
                isDefaultHome = isDefaultHome,
                onRequestHome = ::requestDefaultHome,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeRole()
    }

    private fun refreshHomeRole() {
        isDefaultHome = roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun requestDefaultHome() {
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                requestHome.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
        } else {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}

@Composable
private fun RiceMobileBootstrap(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "rice-mobile")
                    if (!isDefaultHome) {
                        Button(onClick = onRequestHome) {
                            Text(text = stringResource(R.string.action_use_as_home))
                        }
                    }
                }
            }
        }
    }
}
