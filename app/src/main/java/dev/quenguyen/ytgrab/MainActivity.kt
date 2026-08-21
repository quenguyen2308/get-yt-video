package dev.quenguyen.ytgrab

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.quenguyen.ytgrab.ui.common.SimpleViewModelFactory
import dev.quenguyen.ytgrab.ui.downloads.DownloadsScreen
import dev.quenguyen.ytgrab.ui.downloads.DownloadsViewModel
import dev.quenguyen.ytgrab.ui.home.HomeScreen
import dev.quenguyen.ytgrab.ui.home.HomeViewModel
import dev.quenguyen.ytgrab.ui.selection.SelectionScreen
import dev.quenguyen.ytgrab.ui.selection.SelectionViewModel
import dev.quenguyen.ytgrab.ui.settings.SettingsScreen
import dev.quenguyen.ytgrab.ui.settings.SettingsViewModel
import dev.quenguyen.ytgrab.ui.theme.YtGrabTheme

private object Routes {
    const val HOME = "home"
    const val SELECTION = "selection"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private var sharedUrlState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        sharedUrlState.value = extractSharedUrl(intent)

        val app = application as YtGrabApp

        setContent {
            YtGrabTheme {
                val navController = rememberNavController()
                val sharedUrl by sharedUrlState

                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = SimpleViewModelFactory { HomeViewModel(app.ytDlpRepository) },
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            sharedUrl = sharedUrl,
                            onSharedUrlConsumed = { sharedUrlState.value = null },
                            onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                            onDownloadsClick = { navController.navigate(Routes.DOWNLOADS) },
                            onReady = { navController.navigate(Routes.SELECTION) },
                        )
                    }
                    composable(Routes.SELECTION) {
                        val selectionViewModel: SelectionViewModel = viewModel(
                            factory = SimpleViewModelFactory { SelectionViewModel(app.settingsRepository) },
                        )
                        SelectionScreen(
                            viewModel = selectionViewModel,
                            onBack = { navController.popBackStack() },
                            onConfirmed = {
                                navController.navigate(Routes.DOWNLOADS) {
                                    popUpTo(Routes.HOME)
                                }
                            },
                        )
                    }
                    composable(Routes.DOWNLOADS) {
                        val downloadsViewModel: DownloadsViewModel = viewModel(
                            factory = SimpleViewModelFactory {
                                DownloadsViewModel(app.database.downloadHistoryDao())
                            },
                        )
                        DownloadsScreen(
                            viewModel = downloadsViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        val settingsViewModel: SettingsViewModel = viewModel(
                            factory = SimpleViewModelFactory {
                                SettingsViewModel(app.settingsRepository, app.ytDlpRepository)
                            },
                        )
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedUrl(intent)?.let { sharedUrlState.value = it }
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }
}
