package campalans.m8.cameraappjc

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoCameraScreen()
                }
            }
        }
    }
}

@Composable
fun VideoCameraScreen() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // 1. Launcher per capturar el vídeo
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            capturedVideoUri = videoUri
            capturedVideoUri?.let {
                exoPlayer.setMediaItem(MediaItem.fromUri(it))
                exoPlayer.prepare()
            }
        }
    }

    // 2. Launcher per demanar múltiples permisos (Càmera i Vídeo)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            // Si l'usuari accepta tots, obrim la càmera
            videoUri = createVideoFile(context)
            videoLauncher.launch(videoUri!!)
        }
    }

    // Funció per comprovar i demanar permisos
    fun checkAndLaunchCamera() {
        val permissionsNeeded = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissionsNeeded.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            videoUri = createVideoFile(context)
            videoLauncher.launch(videoUri!!)
        } else {
            permissionsLauncher.launch(permissionsNeeded)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Gravadora amb Permisos", style = MaterialTheme.typography.headlineMedium)

        Button(
            onClick = { checkAndLaunchCamera() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gravar Vídeo (Comprova Permisos)")
        }

        if (capturedVideoUri != null) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply { player = exoPlayer }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Funció auxiliar per crear el fitxer
fun createVideoFile(context: Context): Uri {
    val data = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val file = File(context.getExternalFilesDir(null), "video_$data.mp4")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}