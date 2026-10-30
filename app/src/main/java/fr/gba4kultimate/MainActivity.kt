package fr.gba4kultimate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri

private val Bg = Color(0xFF080D16)
private val Panel = Color(0xFF121D2C)
private val Accent = Color(0xFF70E1C1)

class MainActivity : ComponentActivity() {
    private var selectedRom by mutableStateOf<Uri?>(null)
    private val romPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedRom = uri
        uri?.let { contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent, background = Bg, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) {
                PrototypeHome(
                    selectedRom = selectedRom,
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
private fun PrototypeHome(selectedRom: Uri?, onPickRom: () -> Unit) {
    var graphicsMode by remember { mutableStateOf("AI Enhance") }
    var smoothing by remember { mutableFloatStateOf(0.75f) }
    var sharpness by remember { mutableFloatStateOf(0.35f) }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("GBA 4K", fontSize = 30.sp, color = Color.White)
                    Text("ULTIMATE  •  ANDROID", color = Accent, fontSize = 12.sp)
                }
                Text("PROTOTYPE", color = Accent, fontSize = 11.sp)
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bibliothèque", style = MaterialTheme.typography.titleLarge)
                    Text(
                        selectedRom?.lastPathSegment ?: "Aucun jeu sélectionné",
                        color = Color(0xFFB8C5D6)
                    )
                    Button(onClick = onPickRom, modifier = Modifier.fillMaxWidth()) {
                        Text("Importer un jeu .gba")
                    }
                    Text(
                        "La sélection de fichier est prête. Le lancement du jeu nécessite encore le cœur mGBA natif.",
                        color = Color(0xFFFFD28A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Moteur graphique", style = MaterialTheme.typography.titleLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("AI Enhance", "HD Remaster").forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = graphicsMode == mode,
                                onClick = { graphicsMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(index, 2)
                            ) { Text(if (mode == "AI Enhance") "IA" else "Remaster") }
                        }
                    }
                    Text("Lissage  •  ${(smoothing * 100).toInt()}%")
                    Slider(value = smoothing, onValueChange = { smoothing = it })
                    Text("Netteté  •  ${(sharpness * 100).toInt()}%")
                    Slider(value = sharpness, onValueChange = { sharpness = it })
                    Text(
                        "Mode choisi : $graphicsMode. Ces commandes sont l'interface de réglage ; les filtres GPU et l'upscaler IA restent à intégrer.",
                        color = Color(0xFFB8C5D6),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profil Galaxy S25+", style = MaterialTheme.typography.titleMedium)
                    Text("Plein écran immersif • paysage • cible QHD+ • rendu haute résolution adaptatif",
                        color = Color(0xFFB8C5D6))
                    Text("Le 4K désigne une résolution de rendu intermédiaire éventuelle : l'écran du S25+ est QHD+.",
                        color = Color(0xFFB8C5D6), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Prototype d'interface — ne lance pas encore les ROM.", color = Color(0xFFFFD28A))
        }
    }
}
