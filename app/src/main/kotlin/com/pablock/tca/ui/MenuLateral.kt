package com.pablock.tca.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablock.tca.EntrenamientoEnCursoPrefs

// Menú principal de la app: hamburguesa -> panel de 1/2 pantalla con acceso directo a
// cualquier apartado, se cierra con la flecha de abajo o tocando la otra mitad.
@Composable
fun MenuLateral(
    abierto: Boolean,
    onCerrar: () -> Unit,
    esPantallaPrincipal: Boolean,
    hayEntrenamientoEnCurso: Boolean,
    onTerminar: () -> Unit,
    onInicio: () -> Unit,
    onNotas: () -> Unit,
    onHerramientas: () -> Unit,
    onEstadisticas: () -> Unit,
    onAjustes: () -> Unit,
) {
    if (!abierto) return

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(2f).fillMaxHeight().background(Color.Black)) {
            // Capa bloqueadora: sin ella, tocar el espacio vacío entre botones (o cualquier
            // margen del menú sin un ItemMenuLateral encima) no consume el toque — un
            // Column con solo .background() no intercepta entrada — y el toque le llegaba
            // a la pantalla de atrás (bug: activaba el modo seleccionar de Notas).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = {}),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
            ) {
                // En Inicio con un entrenamiento a medias, este primer renglón es "Terminar
                // entrenamiento" directo; en cualquier otra pantalla es "Inicio" con un punto
                // que avisa que hay un entrenamiento en curso — ahí sí se puede terminar.
                if (esPantallaPrincipal && hayEntrenamientoEnCurso) {
                    ItemMenuLateral("Terminar entrenamiento", onTerminar)
                } else {
                    ItemMenuLateral("Inicio", onInicio, conPunto = hayEntrenamientoEnCurso && !esPantallaPrincipal)
                }
                ItemMenuLateral("Notas", onNotas)
                ItemMenuLateral("Herramientas", onHerramientas)
                ItemMenuLateral("Estadísticas", onEstadisticas)
                ItemMenuLateral("Ajustes", onAjustes)
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().clickable(onClick = onCerrar).padding(vertical = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar menú", tint = Color.White)
                }
            }
        }
        // Línea gris que marca dónde termina el menú.
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF3A3A3A)),
        )
        // La otra mitad: un toque cierra el menú, sin ningún efecto de click visible.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onCerrar,
                ),
        )
    }
}

@Composable
private fun ItemMenuLateral(texto: String, onClick: () -> Unit, conPunto: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(texto, color = Color.White, fontSize = 18.sp)
        if (conPunto) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(8.dp)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

// Estado compartido "hay un entrenamiento en curso" para pantallas que no tienen su
// propio ViewModel de Principal (Estadísticas, Ajustes) pero sí necesitan mostrar u
// ocultar "Terminar entrenamiento" en el menú.
@Composable
fun hayEntrenamientoEnCursoState(): State<Boolean> {
    val context = LocalContext.current
    val flujo = remember(context) { EntrenamientoEnCursoPrefs.observar(context) }
    return produceState(initialValue = false, flujo) {
        flujo.collect { value = it.entrenamientoId != null }
    }
}
