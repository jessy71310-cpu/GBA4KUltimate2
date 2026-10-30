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
                        destination.outputStream().use { output -> input.copyTo(output) }
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
                    onExitGame = {
                        fullscreen = false
                        gameFile = null
                    },
                    onPickRom = { romPicker.launch(arrayOf("*/*")) }
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
    onExitGame: () -> Unit,
    onPickRom: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var confirmExit by remember { mutableStateOf(false) }

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
                            Modifier.padding(16.dp),
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

                    Text(
                        if (gameFile != null) "Écran du jeu"
                        else "Choisis un fichier de jeu GBA que tu possèdes.",
                        color = if (gameFile != null) Accent else Color(0xFFB8C5D6)
                    )
                }
            }

            if (gameFile != null) {
                Box(
                    modifier = if (fullscreen) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().height(300.dp).background(Color.Black)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
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
                            GLRetroView(viewContext, data).also {
                                lifecycle.addObserver(it)
                            }
                        },
                        onRelease = { lifecycle.removeObserver(it) }
                    )

                    if (fullscreen) {
                        TextButton(
                            onClick = {
                                menuOpen = true
                                optionsOpen = false
                                selectedOption = null
                            },
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("RETOUR", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    if (menuOpen) {
                        Box(
                            Modifier.fillMaxSize().background(Color(0xB8000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(0.82f).heightIn(max = 430.dp),
                                colors = CardDefaults.cardColors(containerColor = Panel),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "GBA 4K ULTIMATE",
                                        color = Accent,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    if (!optionsOpen) {
                                        Text(
                                            "MENU DU JEU",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Button(
                                            onClick = { menuOpen = false },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("REPRENDRE LE JEU")
                                        }
                                        OutlinedButton(
                                            onClick = { optionsOpen = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("⚙  OPTIONS")
                                        }
                                        OutlinedButton(
                                            onClick = { confirmExit = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFFF9B9B)
                                            )
                                        ) {
                                            Text("QUITTER LE JEU")
                                        }
                                    } else {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = {
                                                optionsOpen = false
                                                selectedOption = null
                                            }) {
                                                Text("← RETOUR")
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                "OPTIONS",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }

                                        val categories = listOf(
                                            "Commandes tactiles" to
                                                "Réglages des boutons à l’écran",
                                            "Manette Bluetooth" to
                                                "Connexion et attribution des boutons",
                                            "Image" to
                                                "Plein écran, étirement et filtres",
                                            "Sauvegardes" to
                                                "Sauvegardes de jeu et états rapides"
                                        )

                                        categories.forEach { (name, description) ->
                                            OutlinedButton(
                                                onClick = { selectedOption = name },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(Modifier.fillMaxWidth()) {
                                                    Text(name)
                                                    Text(
                                                        description,
                                                        color = Color(0xFFB8C5D6),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }

                                        if (selectedOption != null) {
                                            Text(
                                                "$selectedOption : cette rubrique sera configurée dans une prochaine étape.",
                                                color = Accent,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        TextButton(onClick = {
                                            menuOpen = false
                                            optionsOpen = false
                                            selectedOption = null
                                        }) {
                                            Text("REPRENDRE LE JEU")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!fullscreen) {
                if (gameFile != null) {
                    Button(
                        onClick = { onFullscreenChange(true) },
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
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
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Quitter le jeu ?") },
            text = {
                Text("La partie en cours sera fermée et tu reviendras à la bibliothèque.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    menuOpen = false
                    optionsOpen = false
                    onExitGame()
                }) {
                    Text("QUITTER", color = Color(0xFFFF9B9B))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("ANNULER")
                }
            },
            containerColor = Panel,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB8C5D6)
        )
    }
}
