package com.pablock.tca.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun BrujulaScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    var haySensores by remember { mutableStateOf(true) }
    var azimut by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acelerometro = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometro = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (acelerometro == null || magnetometro == null) {
            haySensores = false
            onDispose {}
        } else {
            val valoresAcel = FloatArray(3)
            val valoresMagnet = FloatArray(3)
            val matrizRotacion = FloatArray(9)
            val orientacion = FloatArray(3)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, valoresAcel, 0, 3)
                        Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, valoresMagnet, 0, 3)
                    }
                    if (SensorManager.getRotationMatrix(matrizRotacion, null, valoresAcel, valoresMagnet)) {
                        SensorManager.getOrientation(matrizRotacion, orientacion)
                        val grados = Math.toDegrees(orientacion[0].toDouble()).toFloat()
                        azimut = (grados + 360) % 360
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magnetometro, SensorManager.SENSOR_DELAY_UI)
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
            Text("Brújula", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }

        if (!haySensores) {
            Text(
                "Este teléfono no tiene los sensores necesarios (magnetómetro).",
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                EsferaBrujula(azimut = azimut, modifier = Modifier.fillMaxWidth(0.85f))
            }
            Text(
                "${azimut.roundToInt()}° ${direccionCardinal(azimut)}",
                color = Color.White,
                fontSize = 56.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 38.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun direccionCardinal(azimut: Float): String {
    val direcciones = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val indice = ((azimut + 22.5f) / 45f).toInt() % 8
    return direcciones[indice]
}

@Composable
private fun EsferaBrujula(azimut: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radio = min(size.width, size.height) / 2f * 0.9f
        val centro = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0xFF3A3A3A), radius = radio, center = centro, style = Stroke(width = 2f))

        // La esfera gira al revés del azimut para que la marca "N" siempre apunte al norte real.
        for (grado in 0 until 360 step 30) {
            val anguloDibujo = Math.toRadians((grado - azimut - 90).toDouble())
            val externo = centro + Offset((radio * cos(anguloDibujo)).toFloat(), (radio * sin(anguloDibujo)).toFloat())
            val interno = centro + Offset((radio * 0.85f * cos(anguloDibujo)).toFloat(), (radio * 0.85f * sin(anguloDibujo)).toFloat())
            drawLine(Color(0xFF3A3A3A), interno, externo, strokeWidth = 2f)
        }

        val anguloNorte = Math.toRadians((-azimut - 90).toDouble())
        val puntaNorte = centro + Offset((radio * 0.75f * cos(anguloNorte)).toFloat(), (radio * 0.75f * sin(anguloNorte)).toFloat())
        drawLine(Color(0xFF008080), centro, puntaNorte, strokeWidth = 6f)
        drawCircle(color = Color(0xFF008080), radius = 10f, center = puntaNorte)
    }
}
