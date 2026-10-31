package fr.gba4kultimate

import android.net.Uri
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import java.io.File
import android.view.KeyEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput

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

        val hdr = windowManager.defaultDisplay.hdrCapabilities
        android.util.Log.i(
            "GBA4K_HDR",
            "Types=${hdr.supportedHdrTypes.contentToString()}, " +
                "Luminosité max=${hdr.desiredMaxLuminance}, " +
                "moyenne=${hdr.desiredMaxAverageLuminance}, " +
                "min=${hdr.desiredMinLuminance}"
        )

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
var retroView by remember { mutableStateOf<GLRetroView?>(null) }

    val context = LocalContext.current
    val touchPreferences = remember(context) {
        context.getSharedPreferences("touch_controls", Context.MODE_PRIVATE)
    }
    val displayPreferences = remember(context) {
        context.getSharedPreferences("display_settings", Context.MODE_PRIVATE)
    }
    var hdrEnabled by remember {
        mutableStateOf(displayPreferences.getBoolean("hdr_enabled", false))
    }
    var showTouchControls by remember {
        mutableStateOf(touchPreferences.getBoolean("visible", true))
    }
    var hideTouchWithGamepad by remember {
        mutableStateOf(touchPreferences.getBoolean("hide_with_gamepad", true))
    }
    var hasGamepad by remember { mutableStateOf(false) }
    var touchEditMode by remember { mutableStateOf(false) }
    var touchButtonScale by remember {
        mutableFloatStateOf(touchPreferences.getFloat("button_scale", 1f))
    }
    var touchButtonAlpha by remember {
        mutableFloatStateOf(touchPreferences.getFloat("button_alpha", 0.47f))
    }

    val inputManager = remember(context) {
        context.getSystemService(Context.INPUT_SERVICE) as InputManager
    }
    val refreshGamepadState = {
        hasGamepad = inputManager.inputDeviceIds.any { deviceId ->
            val device = inputManager.getInputDevice(deviceId)
            device != null && (
                (device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (device.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            )
        }
    }

    DisposableEffect(inputManager) {
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                refreshGamepadState()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                refreshGamepadState()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                refreshGamepadState()
            }
        }
        inputManager.registerInputDeviceListener(listener, null)
        refreshGamepadState()
        onDispose {
            inputManager.unregisterInputDeviceListener(listener)
        }
    }
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
                                shader = if (hdrEnabled) ShaderConfig.HDR else ShaderConfig.Default
                                rumbleEventsEnabled = true
                                preferLowLatencyAudio = true
                            }
                            GLRetroView(viewContext, data).also {
    retroView = it
    lifecycle.addObserver(it)
}
                        },
                        onRelease = {
    lifecycle.removeObserver(it)
    if (retroView === it) retroView = null
}
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


                    if (fullscreen && !menuOpen && showTouchControls && !(hideTouchWithGamepad && hasGamepad)) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TouchKey(retroView, "L", KeyEvent.KEYCODE_BUTTON_L1, 48.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                Spacer(Modifier.height(8.dp))
                                Column {
                                    Row {
                                        TouchKey(retroView, "↖", KeyEvent.KEYCODE_DPAD_UP_LEFT, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                        TouchKey(retroView, "↑", KeyEvent.KEYCODE_DPAD_UP, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                        TouchKey(retroView, "↗", KeyEvent.KEYCODE_DPAD_UP_RIGHT, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                    }
                                    Row {
                                        TouchKey(retroView, "←", KeyEvent.KEYCODE_DPAD_LEFT, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                        Spacer(Modifier.size(40.dp))
                                        TouchKey(retroView, "→", KeyEvent.KEYCODE_DPAD_RIGHT, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                    }
                                    Row {
                                        TouchKey(retroView, "↙", KeyEvent.KEYCODE_DPAD_DOWN_LEFT, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                        TouchKey(retroView, "↓", KeyEvent.KEYCODE_DPAD_DOWN, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                        TouchKey(retroView, "↘", KeyEvent.KEYCODE_DPAD_DOWN_RIGHT, 40.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TouchKey(retroView, "R", KeyEvent.KEYCODE_BUTTON_R1, 48.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                Spacer(Modifier.height(12.dp))
                                Row {
                                    TouchKey(retroView, "B", KeyEvent.KEYCODE_BUTTON_A, 58.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                    Spacer(Modifier.width(12.dp))
                                    TouchKey(retroView, "A", KeyEvent.KEYCODE_BUTTON_B, 58.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                }
                                Spacer(Modifier.height(18.dp))
                                Row {
                                    TouchKey(retroView, "SELECT", KeyEvent.KEYCODE_BUTTON_SELECT, 54.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                    Spacer(Modifier.width(10.dp))
                                    TouchKey(retroView, "START", KeyEvent.KEYCODE_BUTTON_START, 54.dp, touchPreferences, touchEditMode, touchButtonScale, touchButtonAlpha)
                                }
                            }
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

                                        if (selectedOption == "Commandes tactiles") {
                                            Column(
                                                Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("Taille des boutons : ${(touchButtonScale * 100).roundToInt()} %", color = Accent)
                                                Slider(
                                                    value = touchButtonScale,
                                                    onValueChange = { touchButtonScale = it },
                                                    onValueChangeFinished = {
                                                        touchPreferences.edit().putFloat("button_scale", touchButtonScale).apply()
                                                    },
                                                    valueRange = 0.65f..1.45f
                                                )
                                                Text("Transparence : ${(touchButtonAlpha * 100).roundToInt()} %", color = Accent)
                                                Slider(
                                                    value = touchButtonAlpha,
                                                    onValueChange = { touchButtonAlpha = it },
                                                    onValueChangeFinished = {
                                                        touchPreferences.edit().putFloat("button_alpha", touchButtonAlpha).apply()
                                                    },
                                                    valueRange = 0.10f..1f
                                                )
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("Déplacer les boutons")
                                                        Text(
                                                            if (touchEditMode) "Fais glisser chaque bouton sur l’écran."
                                                            else "Active pour déplacer les boutons individuellement.",
                                                            color = Color(0xFFB8C5D6),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    Switch(
                                                        checked = touchEditMode,
                                                        onCheckedChange = { touchEditMode = it }
                                                    )
                                                }
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("Afficher les commandes tactiles")
                                                        Text(
                                                            "Afficher ou masquer les boutons à l’écran.",
                                                            color = Color(0xFFB8C5D6),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    Switch(
                                                        checked = showTouchControls,
                                                        onCheckedChange = {
                                                            showTouchControls = it
                                                            touchPreferences.edit()
                                                                .putBoolean("visible", it)
                                                                .apply()
                                                        }
                                                    )
                                                }
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("Masquer avec une manette")
                                                        Text(
                                                            if (hasGamepad) "Manette détectée."
                                                            else "Aucune manette détectée.",
                                                            color = if (hasGamepad) Accent else Color(0xFFB8C5D6),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    Switch(
                                                        checked = hideTouchWithGamepad,
                                                        onCheckedChange = {
                                                            hideTouchWithGamepad = it
                                                            touchPreferences.edit()
                                                                .putBoolean("hide_with_gamepad", it)
                                                                .apply()
                                                        }
                                                    )
                                                }
                                            }
                                        } else if (selectedOption == "Image") {
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("Effets visuels", color = Accent)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("Effet HDR logiciel")
                                                        Text(
                                                            "Contraste et couleurs renforcés",
                                                            color = Color(0xFFB8C5D6),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    Switch(
                                                        checked = hdrEnabled,
                                                        onCheckedChange = { enabled ->
                                                            hdrEnabled = enabled
                                                            displayPreferences.edit()
                                                                .putBoolean("hdr_enabled", enabled)
                                                                .apply()
                                                            retroView?.shader = if (enabled) ShaderConfig.HDR else ShaderConfig.Default
                                                        }
                                                    )
                                                }
                                            }
                                        } else if (selectedOption != null) {
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
                    "Les filtres IA restent à développer.",
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

@Composable
private fun TouchKey(
    retroView: GLRetroView?,
    label: String,
    keyCode: Int,
    buttonSize: androidx.compose.ui.unit.Dp,
    preferences: android.content.SharedPreferences,
    editMode: Boolean,
    scale: Float,
    alpha: Float
) {
    val density = LocalDensity.current
    var position by remember(preferences, label) {
        mutableStateOf(
            Offset(
                preferences.getFloat("pos_${label}_x", 0f),
                preferences.getFloat("pos_${label}_y", 0f)
            )
        )
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    with(density) { position.x.dp.roundToPx() },
                    with(density) { position.y.dp.roundToPx() }
                )
            }
            .size(buttonSize * scale)
            .background(Color(0xFF0B1726).copy(alpha = alpha), CircleShape)
            .border(1.dp, Color.White.copy(alpha = (alpha * 0.75f).coerceIn(0.15f, 1f)), CircleShape)
            .pointerInput(retroView, keyCode, editMode) {
                if (editMode) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = with(density) { dragAmount.x.toDp().value }
                        val dy = with(density) { dragAmount.y.toDp().value }
                        position = Offset(position.x + dx, position.y + dy)
                        preferences.edit()
                            .putFloat("pos_${label}_x", position.x)
                            .putFloat("pos_${label}_y", position.y)
                            .apply()
                    }
                } else {
                    detectTapGestures(onPress = {
                        retroView?.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                        try {
                            tryAwaitRelease()
                        } finally {
                            retroView?.sendKeyEvent(KeyEvent.ACTION_UP, keyCode)
                        }
                    })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (editMode) "$label ⋮" else label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
