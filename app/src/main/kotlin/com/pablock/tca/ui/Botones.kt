package com.pablock.tca.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Umbral entre dos toques para contarlos como doble click (ver BotonBordeDobleClick).
const val UMBRAL_DOBLE_CLICK_MS = 400L

// Estilo minimalista de los bosquejos: rectángulo con solo borde, sin relleno,
// tema oscuro (ver prompt: "fondos obscuros con líneas y textos claros").
val ColorBordeTca = Color(0xFF3A3A3A)
private val FORMA_BOTON = RoundedCornerShape(6.dp)

@Composable
fun BotonBorde(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 18.dp,
    conBorde: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .then(if (conBorde) Modifier.border(BorderStroke(1.5.dp, ColorBordeTca), FORMA_BOTON) else Modifier)
            .clickable(onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// Contador y Guardar reaccionan a un click o a doble click según Ajustes > Contador >
// Click (default doble) — un solo lugar para no repetir la rama en cada pantalla.
@Composable
fun BotonBordeAccion(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 18.dp,
    doble: Boolean,
    onActivar: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    if (doble) {
        BotonBordeDobleClick(modifier = modifier, padding = padding, onDobleClick = onActivar, content = content)
    } else {
        BotonBorde(modifier = modifier, padding = padding, onClick = onActivar, content = content)
    }
}

// Solo reacciona a doble click/tap. Se implementa a mano sobre clickable (en vez de
// detectTapGestures(onDoubleTap=...)) porque ese detector de Compose descarta el primer
// toque sin avisar si el segundo no llega justo a tiempo — con clickable cada toque
// siempre se registra y aquí solo se decide si cuenta como doble.
@Composable
fun BotonBordeDobleClick(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 18.dp,
    onDobleClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var ultimoClickMs by remember { mutableLongStateOf(0L) }
    Box(
        modifier = modifier
            .border(BorderStroke(1.5.dp, ColorBordeTca), FORMA_BOTON)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                val ahora = System.currentTimeMillis()
                if (ahora - ultimoClickMs <= UMBRAL_DOBLE_CLICK_MS) {
                    ultimoClickMs = 0L
                    onDobleClick()
                } else {
                    ultimoClickMs = ahora
                }
            }
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
