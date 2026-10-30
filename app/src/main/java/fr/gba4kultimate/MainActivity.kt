package fr.gba4kultimate

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import java.io.File

private val Bg = Color(0xFF080D16)
private val Panel = Color(0xFF121D2C)
private val Accent = Color(0xFF70E1C1)

class MainActivity : ComponentActivity() {
    private var gameFile by mutableStateOf<File?>(null)
    private var importError by mutableStateOf<String?>(null)
    private var fullscreen by mutableStateOf(false)

    private val romPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val destination = File(filesDir, "current-game.gba")
                    contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Impossible de lire ce fichier." }
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    require(destination.length() > 0) { "Le fichier est vide." }
                    gameFile = destination
                    importError = null
                } catch (error: Exception) {
                    importError = "Import impossible : ${error.localizedMessage}"
                    gameFile = null
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                background = Bg,
                surface = Panel,
                onBackground = Color.White,
                onSurface = Color.White
            )) {
                EmulatorHome(
                    gameFile = gameFile,
                    importError = importError,
                    lifecycle = lifecycle,
                    fullscreen = fullscreen,
                    onFullscreenChange = { fullscreen = it },
                    onPickRom = {
                        romPicker.launch(arrayOf("*/*"))
                    }
                )
            }
        }
    }

    private fun enableImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }
}

@Composable
private fun EmulatorHome(
    gameFile: File?,
    importError: String?,
    lifecycle: Lifecycle,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onPickRom: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!fullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("GBA 4K", fontSize = 28.sp, color = Color.White)
                            Text("ULTIMATE • ANDROID", color = Accent, fontSize = 12.sp)
                        }
                        Text("mGBA ARM64", color = Accent, fontSize = 11.sp)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Bibliothèque", style = MaterialTheme.typography.titleLarge)
                            Text(
                                gameFile?.name ?: "Aucun jeu sélectionné",
                                color = Color(0xFFB8C5D6)
                            )
                            Button(
                                onClick = onPickRom,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Importer un jeu .gba")
                            }
                            if (importError != null) {
                                Text(importError, color = Color(0xFFFF9B9B))
                            }
                        }
                    }

                    if (gameFile != null) {
                        Text(
                            "Écran du jeu",
                            color = Accent,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            "Choisis un fichier de jeu GBA que tu possèdes.",
                            color = Color(0xFFB8C5D6)
                        )
                    }
                }
            }

            if (gameFile != null) {
                AndroidView(
                    modifier = if (fullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.Black)
                    },
                    factory = { viewContext ->
                        val data = GLRetroViewData(viewContext).apply {
                            coreFilePath = "libmgba_libretro_android.so"
                            gameFilePath = gameFile.absolutePath
                            gameFileBytes = null
                            systemDirectory = viewContext.filesDir.absolutePath
                            savesDirectory = viewContext.filesDir.absolutePath
                            variables = arrayOf()
                            saveRAMState = null
                            shader = ShaderConfig.Default
                            rumbleEventsEnabled = true
                            preferLowLatencyAudio = true
                        }
                        GLRetroView(viewContext, data).also { view ->
                            lifecycle.addObserver(view)
                        }
                    },
                    onRelease = { view ->
                        lifecycle.removeObserver(view)
                    }
                )
            }

            if (!fullscreen) {
                if (gameFile != null) {
                    Button(
                        onClick = { onFullscreenChange(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("Jouer en plein écran")
                    }
                }
                Text(
                    "Les filtres IA et les commandes tactiles restent à développer.",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFFFD28A),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Button(
                        onClick = { onFullscreenChange(false) },
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("Retour")
                    }
                }
            }
        }
    }
}
