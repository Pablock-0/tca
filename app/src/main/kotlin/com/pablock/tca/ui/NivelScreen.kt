package com.pablock.tca.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun NivelScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var z by remember { mutableFloatStateOf(0f) }
    var haySensor by remember { mutableFloatStateOf(1f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            haySensor = 0f
            onDispose {}
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    x = event.values[0]
                    y = event.values[1]
                    z = event.values[2]
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 16.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.clickable(onClick = onVolver).padding(8.dp),
            )
            Text("Nivel", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }

        if (haySensor == 0f) {
            Text("Este teléfono no tiene acelerómetro.", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 24.dp))
        } else {
            val roll = Math.toDegrees(atan2(x.toDouble(), hypot(y.toDouble(), z.toDouble()))).toFloat()
            val pitch = Math.toDegrees(atan2(y.toDouble(), hypot(x.toDouble(), z.toDouble()))).toFloat()

            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                BurbujaNivel(roll = roll, pitch = pitch, modifier = Modifier.fillMaxWidth(0.8f))
            }

            Column(modifier = Modifier.padding(top = 46.dp)) {
                FilaEje("Eje X", roll, "°")
                Spacer(modifier = Modifier.height(22.dp))
                FilaEje("Eje Y", pitch, "°")
            }
        }
    }
}

@Composable
private fun FilaEje(etiqueta: String, valor: Float, unidad: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = Color.White.copy(alpha = 0.8f), fontSize = 48.sp)
        Text("${valor.roundToInt()}$unidad", color = Color.White, fontSize = 48.sp)
    }
}

// Onda triangular de período 180° y amplitud 45: sube de 0 a 45 (0°-45°), baja de
// 45 a 0 (45°-90°, mismo lado), sigue bajando a -45 (90°-135°, lado opuesto) y vuelve
// a subir a 0 (135°-180°) — la burbuja nunca salta de un borde al otro, solo cambia
// de dirección suavemente cada 45°, y nunca sale del círculo externo. Ver prompt.
private fun rebote(valorGrados: Float): Float {
    val r = ((valorGrados % 180f) + 180f) % 180f
    return when {
        r < 45f -> r
        r < 135f -> 90f - r
        else -> r - 180f
    }
}

@Composable
private fun BurbujaNivel(roll: Float, pitch: Float, modifier: Modifier = Modifier) {
    val diferenciaX = rebote(roll)
    val diferenciaY = rebote(pitch)
    val nivelado = kotlin.math.abs(diferenciaX) < 1f && kotlin.math.abs(diferenciaY) < 1f
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radio = min(size.width, size.height) / 2f
        val centro = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0xFF3A3A3A), radius = radio, center = centro, style = Stroke(width = 2f))
        drawCircle(color = Color(0xFF3A3A3A), radius = radio * 0.15f, center = centro, style = Stroke(width = 1.5f))

        // ±45 (el máximo que puede valer la diferencia) mapea al 80% del radio.
        val desplazamiento = radio * 0.8f / 45f
        val burbuja = centro + Offset(diferenciaX * desplazamiento, -diferenciaY * desplazamiento)
        drawCircle(
            color = if (nivelado) Color(0xFF4CAF50) else Color(0xFF008080),
            radius = radio * 0.12f,
            center = burbuja,
        )
    }
}
