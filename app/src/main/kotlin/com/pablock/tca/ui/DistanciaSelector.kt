package com.pablock.tca.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DISTANCIAS_DEFAULT = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 70.0, 90.0)

fun formatDistancia(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

@Composable
fun DistanciaSelector(
    distanciaActual: Double,
    distanciasGuardadas: List<Double>,
    onSeleccionar: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    var mostrarDialogoOtra by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        BotonBorde(onClick = { expandido = true }, padding = 31.dp) {
            Text(formatDistancia(distanciaActual), fontSize = 41.sp)
        }

        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            val opciones = (DISTANCIAS_DEFAULT + distanciasGuardadas).distinct().sorted()
            for (d in opciones) {
                DropdownMenuItem(text = { Text(formatDistancia(d)) }, onClick = { expandido = false; onSeleccionar(d) })
            }
            DropdownMenuItem(text = { Text("otra") }, onClick = { expandido = false; mostrarDialogoOtra = true })
        }
    }

    if (mostrarDialogoOtra) {
        var texto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarDialogoOtra = false },
            title = { Text("Distancia") },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Metros") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    texto.toDoubleOrNull()?.let(onSeleccionar)
                    mostrarDialogoOtra = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoOtra = false }) { Text("Cancelar") }
            },
        )
    }
}
