# TCA (Tiro con arco)

App Android nativa (Kotlin + Jetpack Compose) para registrar entrenamientos de tiro con arco: contador de flechas, diana interactiva con marcaje por toque, estadísticas por entrenamiento, notas, y herramientas de campo (clima, nivel, brújula).

## 📲 Instalar la app en tu teléfono (sin saber de programación)

Solo necesitas un teléfono **Android**. Hazlo **desde el teléfono**, no desde la computadora:

1. Abre este enlace y espera a que baje el archivo:
   ### 👉 [Descargar TCA](https://github.com/Pablock-0/tca/releases/latest/download/tca.apk)
2. Cuando termine, toca el archivo descargado (**`tca.apk`**). Aparece en la barra de notificaciones o en tu carpeta **Descargas**.
3. La primera vez, Android dirá que no puede instalar apps de este origen. Toca **Ajustes** en ese aviso y activa **Permitir de esta fuente**; luego regresa.
4. Toca **Instalar**. Si sale una advertencia de *Play Protect*, toca **Más detalles → Instalar de todas formas**.
5. Abre **TCA** desde tus aplicaciones. Listo.

> Es una app para instalar por tu cuenta (no está en Google Play). Todos tus datos se quedan **solo en tu teléfono**; lo único que usa internet es la herramienta de Clima. Para desinstalarla, mantén presionado el ícono y elige *Desinstalar*.

---

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
- **Bug de "Dígitos en contador" sin efecto**: `PrincipalViewModel.contadorExtensionDigitos` es un `StateFlow` con `SharingStarted.WhileSubscribed`; como nada lo colectaba con `collectAsState()` en `PrincipalScreen.kt`, el Flow de Ajustes nunca se suscribía y `.value` se quedaba pegado en el default (2) sin importar el cambio en Ajustes. Se arregló colectándolo en la pantalla y pasándolo como parámetro a `textoContador(contador, digitos)` en vez de leerlo internamente. Rango cambiado de 1-6 a 1-4 (máximo 9999 antes de reiniciar a 0000).
- **Bug de Guardar sin efecto en "Click de contador: Simple"**: `MitadBarraInferior` tenía `onClick`/`onDobleClick` separados; la barra de Guardar solo pasaba `onDobleClick`, así que con `doble=false` (Simple) el `clickable` de la rama simple llamaba a un `onClick` que nunca se pasó. Se unificó a un solo callback `onAccion` — "simple" vs. "doble" ahora solo cambia cuántos toques hacen falta para dispararlo, nunca si se dispara; el comportamiento default (doble click) queda idéntico al de antes.
- **Glitch al renombrar notas (incluida Nota 0)**: el `OutlinedTextField` del diálogo "Editar notas" usaba `value = nota.titulo` directo, sin estado local — cada letra escribía en Room, Room reemitía la lista completa por Flow, y esa vuelta asíncrona pisaba a medio tecleo lo que el campo mostraba, con el cursor reapareciendo al inicio del texto. Se arregló con un `remember(nota.id) { mutableStateOf(nota.titulo) }` local por nota (mismo patrón que ya usaba `EditarDianasDialog` para las dianas), que absorbe el tecleo y solo dispara el guardado sin esperar a que vuelva por el Flow.
- **Teclado numérico en Modalidad registro = Datos**: los campos P y UR de `RegistroDatosTabla.kt` usan `KeyboardType.Number`. Efecto secundario: ya no se puede escribir "X" (puntaje perfecto) directo desde ese teclado — solo dígitos — pese a que `parsearFilasDatos` lo sigue aceptando si se escribe (ej. cambiando de teclado a mano).
- **Clima se actualiza al entrar, no solo con el botón de refrescar**: `ClimaViewModel.init` ahora llama a `actualizar(forzar = true)` siempre que hay ciudad configurada (antes solo si no se había actualizado ese día). El candado de `CLIMA_PETICIONES_MAXIMO_DIA`/día sigue aplicando dentro de `actualizar()` — `forzar` solo salta el chequeo de "ya se actualizó hoy", no el límite duro.

## Build y pruebas

```
./gradlew :app:assembleDebug
adb install --no-streaming -r app/build/outputs/apk/debug/app-debug.apk
```

`--no-streaming` puede ser necesario en algunos dispositivos con restricciones del fabricante para evitar `INSTALL_FAILED_USER_RESTRICTED`. En esos mismos dispositivos la inyección de toques por ADB (`adb shell input tap/text`) puede fallar con `SecurityException` (el sistema no concede `INJECT_EVENTS`), así que las pruebas de interacción táctil se hacen físicamente en el teléfono, no por ADB.

### Publicar una versión descargable

El enlace "Descargar TCA" de arriba apunta a `releases/latest/download/tca.apk`. Para actualizarlo tras un cambio:

```
./gradlew :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk tca.apk
gh release create vX.Y -R Pablock-0/tca -t "TCA vX.Y" -n "Cambios de esta versión" tca.apk
```

El APK va firmado con la llave de debug (no hay `signingConfig` de release); para instalar de forma manual entre teléfonos es suficiente. `tca.apk` está en `.gitignore`, no se versiona — vive solo como asset del release.

## Próxima versión (sin implementar todavía)

Dos features planeadas para la siguiente ronda de desarrollo:

1. **Detección de flechas por cámara**: en vez de marcar el toque a mano sobre la diana en pantalla, la app tomará una foto de la diana física y detectará automáticamente las coordenadas de las flechas clavadas a partir de la imagen, convirtiéndolas en los mismos datos (P, UR) que hoy genera el toque manual.
2. **Zoom en el resumen de un entrenamiento**: la vista de mapa de calor de `EstadisticasResumenScreen.kt` (que usa `DianaMapaDeCalor` en `DianaCanvas.kt`) ganará una forma de hacer zoom sobre la diana para inspeccionar agrupaciones de flechas con más detalle, similar en espíritu al modo Zoom que ya existe en la pantalla principal pero aplicado a datos históricos ya guardados en vez de al marcaje en vivo.
3. **Diana de silueta de atacante**: además de la diana convencional (círculos concéntricos), la app permitirá elegir un segundo tipo de diana con forma de silueta humana ("de atacante"), como alternativa de entrenamiento. Implica una segunda geometría de puntaje en `DianaLogic.kt` (o una `calcularToque`/`posicionEnDiana` parametrizada por tipo de diana) y un segundo dibujo en `DianaCanvas.kt`, además de guardar el tipo elegido junto al resto de la configuración de diana (`DianaConfig`/Ajustes).

## Licencia

Doble licencia, aplicable a todo el contenido presente y futuro del repo:

- **Código**: [MIT License](LICENSE) — libre para cualquier uso, incluido comercial, conservando el aviso de copyright.
- **Documentación y explicaciones** (este README y los comentarios descriptivos del código): [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) — libre para cualquier uso, incluido comercial, dando crédito a Pablock-0.

```
(c) Pablock-0
Código: MIT License
Documentación/explicaciones: CC BY 4.0
```
