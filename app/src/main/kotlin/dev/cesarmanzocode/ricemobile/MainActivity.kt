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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.launcher.LauncherHost
import dev.cesarmanzocode.ricemobile.launcher.LauncherViewModel
import dev.cesarmanzocode.ricemobile.launcher.LauncherViewModelFactory

class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as RiceApplication).container

    private val roleManager: RoleManager by lazy { getSystemService(RoleManager::class.java) }

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModelFactory(
            repository = container.appsRepository,
            launcher = container.appLauncher,
            preferencesRepository = container.preferencesRepository,
            wallpaperController = container.wallpaperController,
        )
    }

    private val requestHome = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshHomeRole() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleHomeIntent(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LauncherHost(
                        viewModel = viewModel,
                        iconLoader = container.iconLoader,
                        onRequestHomeRole = ::requestDefaultHome,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHomeIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        container.appsRepository.start()
    }

    override fun onStop() {
        container.appsRepository.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshHomeRole()
        viewModel.onActivityResumed()
    }

    /** Contract §3.5: a Home intent resets navigation even if it repeats. */
    private fun handleHomeIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.resetToHome()
        }
    }

    private fun refreshHomeRole() {
        val isDefaultHome = roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        viewModel.updateHomeRoleStatus(isDefaultHome)
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
