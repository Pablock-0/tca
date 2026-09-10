# TCA (Tiro con arco)

App Android nativa (Kotlin + Jetpack Compose) para registrar entrenamientos de tiro con arco: contador de flechas, diana interactiva con marcaje por toque, estadísticas por entrenamiento, notas, y herramientas de campo (clima, nivel, brújula).

## Stack técnico

- Kotlin + Jetpack Compose (Material3), minSdk 26, target/compileSdk 37.
- AGP 9.3.2 con soporte Kotlin integrado — **incompatible con kapt**. El procesamiento de anotaciones de Room usa KSP (`com.google.devtools.ksp`, versión 2.3.11) en su lugar.
- Room 2.8.4 para persistencia local (entrenamientos, series, flechas, dianas guardadas, notas).
- DataStore Preferences 1.2.1 para Ajustes y para el estado del entrenamiento en curso (sobrevive a que se cierre la app a medio entrenamiento).
- Patrón MVVM: `AndroidViewModel` + `StateFlow`, consumido en Compose con `collectAsState()`.
- Sin librerías de red: la llamada a OpenWeatherMap usa `HttpURLConnection` a mano.
- Todo el almacenamiento es local — no hay backend ni sincronización en la nube.

## Estructura del código

```
app/src/main/kotlin/com/pablock/tca/
├── MainActivity.kt, TcaApplication.kt        — entrypoint
├── PrincipalViewModel.kt, AjustesViewModel.kt,
│   NotasViewModel.kt, EstadisticasListViewModel.kt,
│   ClimaViewModel.kt                          — un ViewModel por pantalla con estado propio
├── TcaRepository.kt                           — toda la lógica de negocio (avanzar contador,
│                                                 guardar series/flechas, estadísticas, notas,
│                                                 dianas guardadas, exportar CSV/TXT)
├── DianaLogic.kt                               — geometría pura de la diana: de un toque (dx,dy)
│                                                 a un puntaje (calcularToque), y su inversa para
│                                                 dibujar puntos ya guardados (posicionEnDiana);
│                                                 conversión azimut ↔ hora de reloj
├── Prefs.kt                                    — EntrenamientoEnCursoPrefs (estado en curso) y
│                                                 AjustesPrefs (toda la configuración)
├── StatsModels.kt                              — data classes de resumen/detalle de estadísticas
├── Clima.kt, ClimaRepository.kt                — llamada a OpenWeatherMap con candado de 40
│                                                 peticiones/día y refresco una vez al día
├── NotificacionEntrenamiento.kt                — recordatorio local (opcional, off por defecto)
├── db/
│   ├── Entidades.kt                            — Entrenamiento, Serie, Flecha, DianaConfig, Nota
│   ├── Daos.kt                                 — DAOs de Room
│   └── TcaDatabase.kt                          — @Database, fallbackToDestructiveMigration
└── ui/
    ├── PrincipalScreen.kt                      — pantalla principal (diana, contador, distancia)
    ├── DianaCanvas.kt                          — Canvas de la diana (bandas de color, líneas,
    │                                             modo Zoom, mapa de calor del resumen)
    ├── DistanciaSelector.kt, RegistroDatosTabla.kt, Botones.kt (BotonBorde/BotonBordeAccion)
    ├── MenuLateral.kt                          — menú lateral propio (no ModalNavigationDrawer)
    ├── EstadisticasListScreen.kt,
    │   EstadisticasResumenScreen.kt,
    │   EstadisticasDetalleScreen.kt            — lista de entrenamientos → resumen (mapa de
    │                                             calor) → detalle serie por serie
    ├── NotasScreen.kt, AjustesScreen.kt
    └── HerramientasScreen.kt, ClimaScreen.kt,
        NivelScreen.kt, BrujulaScreen.kt        — Herramientas: clima, nivel (burbuja con rebote),
                                                   brújula
```

## Decisiones de diseño no evidentes en el código

- **Geometría de la diana**: el radio interactivo (`FACTOR_RADIO_DIANA = 10/11`) deja un margen alrededor del círculo visible donde también se registran toques (P=0 fuera de la diana). El anillo de score 10 real ocupa `t ∈ [0.05, 0.1]` de esa banda (la otra mitad, `[0, 0.05]`, es X) — ver `calcularToque`/`posicionEnDiana` en `DianaLogic.kt`.
- **Modo Zoom**: solo amplía los anillos 6–10/X (5 bandas en vez de 10), reescalando `t` con `T_REAL_MAXIMO_ZOOM`. Un toque fuera del círculo en modo Zoom anota 5 en vez de 0.
- **`resolverDianaCm`** (`TcaRepository.kt`) se resuelve **fresco en cada guardado**, nunca se cachea en `EntrenamientoEnCursoPrefs` — hubo un bug donde el tamaño de diana quedaba pegado al primero elegido y no reflejaba cambios posteriores en Ajustes > Diana default.
- **Doble click de Guardar/Contador**: implementado a mano con `clickable` + comparación de timestamps (`UMBRAL_DOBLE_CLICK_MS = 400`), no con `detectTapGestures(onDoubleTap=...)` de Compose — ese detector descarta el primer toque sin avisar si el segundo no llega a tiempo, causando dobles clicks fallidos de forma intermitente.
- **Botón de Zoom desplazándose en la pantalla principal**: la `Column` que agrupa el contador y el botón de Zoom debe usar `horizontalAlignment = Alignment.End`, no `CenterHorizontally` — con `CenterHorizontally`, el botón de Zoom (más angosto) se centraba bajo el ancho variable del contador ("Iniciar" vs. un número), pareciendo que "se movía" según el texto del contador.
- **Diana usada por serie**: `Serie.dianaCm` (Room) y `DetalleSerie.dianaCm` (`StatsModels.kt`) ya existían desde el bug fix de "Diana default"; `EstadisticasDetalleScreen.kt` lo muestra como "Diana: [cm]" (solo el número, sin unidad) entre Distancia y Flechas.
- **Bug de la "serie fantasma"**: el primer click de un entrenamiento nuevo (antes de que exista la diana) no debe crear una Serie — se guarda solo si ya hay flechas pendientes o no es el primer click (`avanzarContador` en `TcaRepository.kt`).
- **Burbuja de Nivel**: usa una función de "rebote" (onda triangular, `rebote()` en `NivelScreen.kt`), no un salto/snap — un salto instantáneo al cruzar 45° se sintió como un teletransporte y se descartó.
- **Clima**: candado explícito de 40 peticiones/día (límite gratuito de OpenWeatherMap) y refresco una sola vez al día — no hace polling constante.
- **`local.properties`** guarda `OWM_API_KEY` (nunca commiteado; ver `.gitignore` de este repo). Se expone a la app vía `buildConfigField` en `app/build.gradle.kts`.
- **MenuLateral** es un componente propio (no `ModalNavigationDrawer` de Material3): ocupa 2/3 del ancho, con el 1/3 restante como área para cerrar tocando fuera. Detrás de los `ItemMenuLateral` hay una capa bloqueadora (`Box` con `clickable` vacío) del mismo tamaño que el panel — sin ella, tocar el espacio vacío entre botones no consumía el toque (un `Column` con solo `.background()` no intercepta entrada) y el toque le llegaba a la pantalla oculta detrás (bug real: activaba el modo seleccionar de Notas).
- **Línea entre 9 y 10 en la diana**: banda 0 (10) y banda 1 (9) comparten el mismo color de fondo (dorado), así que su línea divisoria usa el mismo negro de alto contraste que la línea 10/X (`COLOR_LINEA_ALTO_CONTRASTE` en `DianaCanvas.kt`) — con el gris normal casi no se distinguía.
- **Eje Z de Nivel**: se quitó. El acelerómetro en reposo solo mide la dirección de la gravedad, que tiene 2 grados de libertad reales; Eje X (roll) y Eje Y (pitch) ya la describen por completo, así que un tercer ángulo derivado de los mismos tres valores no aporta información independiente (solo se movía como combinación de X e Y).
- **Tamaños de texto por pantalla**: varios ajustes de tamaño (Clima al 200% salvo el título, Nivel al 300% en Eje X/Y, Brújula al 200% en el texto de grados) fueron pedidos explícitamente en px/porcentaje y convertidos a sp/dp a mano — no hay una escala global, cada pantalla tiene sus valores fijos en el código.

## Build y pruebas

```
./gradlew :app:assembleDebug
adb install --no-streaming -r app/build/outputs/apk/debug/app-debug.apk
```

`--no-streaming` es necesario en el dispositivo de pruebas (MIUI) para evitar `INSTALL_FAILED_USER_RESTRICTED`. La inyección de toques por ADB (`adb shell input tap/text`) falla con `SecurityException` en ese dispositivo (MIUI no concede `INJECT_EVENTS`), así que las pruebas de interacción táctil se hacen físicamente en el teléfono, no por ADB.

## Próxima versión (sin implementar todavía)

Dos features planeadas para la siguiente ronda de desarrollo:

1. **Detección de flechas por cámara**: en vez de marcar el toque a mano sobre la diana en pantalla, la app tomará una foto de la diana física y detectará automáticamente las coordenadas de las flechas clavadas a partir de la imagen, convirtiéndolas en los mismos datos (P, UR) que hoy genera el toque manual.
2. **Zoom en el resumen de un entrenamiento**: la vista de mapa de calor de `EstadisticasResumenScreen.kt` (que usa `DianaMapaDeCalor` en `DianaCanvas.kt`) ganará una forma de hacer zoom sobre la diana para inspeccionar agrupaciones de flechas con más detalle, similar en espíritu al modo Zoom que ya existe en la pantalla principal pero aplicado a datos históricos ya guardados en vez de al marcaje en vivo.
