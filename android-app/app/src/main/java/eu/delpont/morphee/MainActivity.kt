package eu.delpont.morphee

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import eu.delpont.morphee.ui.AppUi
import eu.delpont.morphee.ui.theme.MorpheeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.scanLibrary()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorpheeTheme {
                AppUi(viewModel)
            }
        }
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, audioPermission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed += audioPermission
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isEmpty()) {
            viewModel.scanLibrary()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
