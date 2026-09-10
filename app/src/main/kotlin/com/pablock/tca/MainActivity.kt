package com.pablock.tca

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pablock.tca.ui.TcaApp

// Acento aqua/teal para toda la app (selección de texto, botones de diálogos, switches
// encendidos, foco de campos...) en vez del morado por default de Material3.
private val COLOR_ACENTO = Color(0xFF008080)
private val ESQUEMA_COLOR_TCA = darkColorScheme(
    primary = COLOR_ACENTO,
    secondary = COLOR_ACENTO,
    tertiary = COLOR_ACENTO,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pedirNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            MaterialTheme(colorScheme = ESQUEMA_COLOR_TCA) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TcaApp()
                }
            }
        }
    }
}
