package com.pablock.tca.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private enum class Pantalla {
    PRINCIPAL, ESTADISTICAS_LISTA, ESTADISTICAS_RESUMEN, ESTADISTICAS_DETALLE, NOTAS, AJUSTES,
    HERRAMIENTAS, CLIMA, NIVEL, BRUJULA,
}

@Composable
fun TcaApp() {
    var pantalla by remember { mutableStateOf(Pantalla.PRINCIPAL) }
    var entrenamientoSeleccionado by remember { mutableStateOf<Long?>(null) }

    when (pantalla) {
        Pantalla.PRINCIPAL -> PrincipalScreen(
            onEstadisticas = { pantalla = Pantalla.ESTADISTICAS_LISTA },
            onNotas = { pantalla = Pantalla.NOTAS },
            onAjustes = { pantalla = Pantalla.AJUSTES },
            onHerramientas = { pantalla = Pantalla.HERRAMIENTAS },
        )
        Pantalla.ESTADISTICAS_LISTA -> EstadisticasListScreen(
            onSeleccionar = { id ->
                entrenamientoSeleccionado = id
                pantalla = Pantalla.ESTADISTICAS_RESUMEN
            },
            onIrAInicio = { pantalla = Pantalla.PRINCIPAL },
            onNotas = { pantalla = Pantalla.NOTAS },
            onHerramientas = { pantalla = Pantalla.HERRAMIENTAS },
            onAjustes = { pantalla = Pantalla.AJUSTES },
        )
        Pantalla.ESTADISTICAS_RESUMEN -> EstadisticasResumenScreen(
            entrenamientoId = entrenamientoSeleccionado ?: return,
            onVerDetalle = { pantalla = Pantalla.ESTADISTICAS_DETALLE },
            onVolver = { pantalla = Pantalla.ESTADISTICAS_LISTA },
        )
        Pantalla.ESTADISTICAS_DETALLE -> EstadisticasDetalleScreen(
            entrenamientoId = entrenamientoSeleccionado ?: return,
            onVolver = { pantalla = Pantalla.ESTADISTICAS_RESUMEN },
        )
        Pantalla.NOTAS -> NotasScreen(
            onIrAInicio = { pantalla = Pantalla.PRINCIPAL },
            onIrAEstadisticas = { pantalla = Pantalla.ESTADISTICAS_LISTA },
            onIrAAjustes = { pantalla = Pantalla.AJUSTES },
            onHerramientas = { pantalla = Pantalla.HERRAMIENTAS },
        )
        Pantalla.AJUSTES -> AjustesScreen(
            onVolver = { pantalla = Pantalla.PRINCIPAL },
            onNotas = { pantalla = Pantalla.NOTAS },
            onHerramientas = { pantalla = Pantalla.HERRAMIENTAS },
            onEstadisticas = { pantalla = Pantalla.ESTADISTICAS_LISTA },
        )
        Pantalla.HERRAMIENTAS -> HerramientasScreen(
            onIrAClima = { pantalla = Pantalla.CLIMA },
            onIrANivel = { pantalla = Pantalla.NIVEL },
            onIrABrujula = { pantalla = Pantalla.BRUJULA },
            onIrAInicio = { pantalla = Pantalla.PRINCIPAL },
            onNotas = { pantalla = Pantalla.NOTAS },
            onEstadisticas = { pantalla = Pantalla.ESTADISTICAS_LISTA },
            onAjustes = { pantalla = Pantalla.AJUSTES },
        )
        Pantalla.CLIMA -> ClimaScreen(onVolver = { pantalla = Pantalla.HERRAMIENTAS })
        Pantalla.NIVEL -> NivelScreen(onVolver = { pantalla = Pantalla.HERRAMIENTAS })
        Pantalla.BRUJULA -> BrujulaScreen(onVolver = { pantalla = Pantalla.HERRAMIENTAS })
    }
}
