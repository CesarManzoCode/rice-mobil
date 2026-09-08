# Sprint 3 — The Five Rices — Evidencia

**Branch:** `feat/v1-rices`
**Entorno de escritura del código:** sesión remota (Claude Code on the web), sin teléfono físico ni Android SDK/red completa en el contenedor.
**Base:** `main` en `dc2fa5e33aa01e21b0153deab5a1c332946dbec1` (Sprint 2 integrado y validado físicamente por el usuario en Arch + CUBOT KINGKONG X, ver `docs/evidence/sprint-2.md`).

**Commits de este sprint (`dc2fa5e..c27c106`):**

```
c27c106 feat: refine shared icons clock and motion
511720e feat: complete violet night rice
0b2f0c4 feat: complete arctic glass rice
f3e009c feat: complete ember industrial rice
d83161e feat: complete ivory editorial rice
0f696ba feat: complete monochrome visual composition
```

## 1. Limitación del sandbox remoto (idéntica a S1/S2, no del contrato)

Este contenedor sigue sin Android SDK/`adb`, y su política de red sigue bloqueando `dl.google.com`/`maven.google.com`, de donde resuelven el plugin AGP y los artefactos `androidx.*`:

```
$ ./gradlew help
FAILURE: Build failed with an exception.
* Where: Build file '/home/user/rice-mobil/build.gradle.kts' line: 7
* What went wrong:
Plugin [id: 'com.android.application', version: '9.3.2', apply: false] was not found in any of the following sources:
...
BUILD FAILED in 32s
```

Se repitió la comprobación en esta sesión (no sólo se asumió del historial de S1/S2) antes de empezar el trabajo de Sprint 3. **Consecuencia:** `./gradlew test`, `./gradlew lint` y `./gradlew assembleDebug` **no se pudieron ejecutar en esta sesión**. No se declara `PASS` para ninguno aquí. El código se revisó manualmente con la mayor atención posible — incluyendo una pasada de revisión estática dedicada, cruzando cada archivo de rice contra la interfaz `Rice`/`RiceActions`/`HomeModel`/`DrawerModel` y contra las firmas reales de `AppIcon`/`IconLoader`/`EmptyState` — pero eso no sustituye una compilación real. Esa revisión sí encontró y corrigió un error real antes de empujar: faltaba `import androidx.compose.foundation.layout.weight` en cinco archivos (`MonochromeHome.kt`, `MonochromeDrawer.kt`, `EmberDrawer.kt`, `ArcticDrawer.kt`, `VioletDrawer.kt`) que usan `Modifier.weight(1f)` — se corrigió antes del commit correspondiente, no queda pendiente.

**Pendiente explícito para el usuario en su Arch:**

```bash
git fetch origin feat/v1-rices
git switch feat/v1-rices
./gradlew test
./gradlew lint
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 2. Qué se implementó (verificable por lectura, no por build en esta sesión)

### Tokens y dirección visual (§18)

Cada rice fija sus tokens de color exactos del contrato como `Color` privados/internos en su propio archivo de Home (no hay un archivo de tokens compartido ni un tema Material central que los sustituya):

- **Monochrome** `#0A0A0A`/`#F5F5F0`/`#A3A3A0`/`#454545`.
- **Arctic Glass** `#071B2A`/`#E8FAFF`/`#ACCAD5`/`#78DCEF`, glass ~88% opaco.
- **Ember Forge** `#171411`/`#24201B`/`#F1E8DC`/`#D99A67`/`#C0ABA0`.
- **Ivory Paper** `#F3EBDD`/`#25231E`/`#655F55`/`#AAA08D`.
- **Violet Night** `#100C24`/`#302053`/`#F3EDFF`/`#C3B3D9`/`#BC9BFF`.

### Los cinco rices (contract §18.2–§18.6)

- **Monochrome** (`rice/monochrome/`): columna rígida a start con 24dp de margen; reloj SansSerif Black 92sp en el tercio superior con fecha Monospace separada por una regla de 1dp; favoritos como filas lineales indexadas `01…05` con icono monocromo 30dp; franja inferior "APPS ↑"/"RICE" como triggers tipográficos, sin botones Material. Drawer: lista única con encabezado "APPS" 32sp + contador real de resultados, filas de 60dp con regla entre cada una, favorito marcado con un pequeño cuadrado (no estrella), campo de búsqueda como `BasicTextField` con underline gruesa de 2dp — no el `OutlinedTextField` compartido de Sprint 2.
- **Ivory Paper** (`rice/ivory/`): dateline pequeña arriba, hora Serif 72sp a media altura (aire asimétrico arriba/abajo vía pesos de `Spacer`), favoritos como índice numerado sin iconos en Home (decisión editorial explícita del contrato), pie "Biblioteca de apps ↗" subrayado + "Edición" para el picker. Drawer: índice alfabético agrupado (`groupByInitial`, agrupación estable sobre resultados ya ordenados por Collator aguas arriba, grupo `#` para no-letras), cabecera de letra grande en el margen, filas a start+32dp, regla sólo entre grupos.
- **Ember Forge** (`rice/ember/`): header comprimido hora Monospace Bold 50sp + regla cobre vertical de 3dp + fecha en columna lateral; favoritos en un bloque ancho (72dp) + grid 2 columnas (64dp) con `CutCornerShape(6dp)`, press invierte fondo/borde/tinta (sin easing de rebote); barra inferior "TODAS LAS APPS →" + botón cuadrado "R". Drawer: grid de 1–2 columnas (`BoxWithConstraints`, colapsa bajo 340dp de ancho o fontScale >1.3 en vez de encoger el target), celdas icono+label horizontales (nunca icono encima del label), búsqueda como rectángulo con borde cobre.
- **Arctic Glass** (`rice/arctic/`): reloj SansSerif Light 76sp centrado en la mitad superior con fecha en cápsula glass, gran vacío, dock horizontal flotante (`RoundedCornerShape(28dp)`, borde blanco 18% opacidad, sombra corta) con slots por `weight(1f)` que caben sin scroll horizontal en 320dp, pill "Apps ↑" encima del dock y círculo "Rice" discreto en la misma fila. Drawer: panel redondeado casi de borde a borde con grid 4→3 columnas según ancho/fontScale, celdas circulares translúcidas, búsqueda como cápsula dentro del panel. Glass V1 = opacidad + borde + sombra corta, sin `Modifier.blur` ni backdrop real.
- **Violet Night** (`rice/violet/`): wallpaper protagonista, reloj SansSerif Medium 56sp alineado end en la esquina superior, cluster de favoritos determinista 1‑2‑2 (cada fila es su propio `Row` centrado, por eso 1–2 favoritos también centran su composición parcial sin placeholders orbitales), desfase vertical de 8–16dp entre nodos de una misma fila. Drawer: sheet real anclado al 88% de la altura útil dentro de la misma ruta (`LauncherScreen.Drawer`, no `ModalBottomSheet` con back stack propio), esquinas superiores 32dp, scrim no modal sobre el wallpaper visible arriba, grid de 3 columnas, botón cerrar 48dp.

Ningún rice comparte `BaseRiceHome`/`BaseRiceDrawer`; cada uno tiene su propio archivo de Home y Drawer con su propia geometría, y cada uno usa su propio `BasicTextField` de búsqueda en vez del `OutlinedTextField` genérico de Sprint 2 (`ui/shared/SearchField.kt` pasó de ser un componente visual a exponer sólo `SearchImeOptions`/`rememberSearchKeyboardActions`, lógica compartida sin UI compartida).

### Wallpapers definitivos (§8)

Los cinco PNG de 64×64 provisionales de Sprint 2 se sustituyeron por cinco WebP de 1440×3200, generados offline y de forma determinista con un script Python local (`Pillow`/`numpy`, sin red, semillas fijas) que compone ruido coherente multi-octava, glow radial y gradientes verticales según la dirección del contrato — no son color sólido ni un gradiente básico:

| Rice | Peso | Dirección aplicada |
|---|---|---|
| `monochrome.webp` | ~9.7 KiB | Negro mate `#0A0A0A` con veta orgánica de muy baja opacidad (~5%) derivada del gradiente de un campo de ruido — casi invisible, tal como pide el contrato. |
| `arctic_glass.webp` | ~16.6 KiB | Gradiente frío con glow superior derecho y niebla translúcida en bandas. |
| `ember_forge.webp` | ~12.1 KiB | Carbón con grano fino y resplandor cobre lateral bajo. |
| `ivory_paper.webp` | ~12.3 KiB | Papel cálido con textura de fibra fina y viñeta muy sutil, sin manchas oscuras. |
| `violet_night.webp` | ~15.3 KiB | Nube índigo/violeta con centro luminoso difuso lejos del reloj (end) y del cluster (mitad inferior), scrim bajo el cluster. |

Todos <=2 MiB (muy por debajo, ~66 KiB los cinco juntos), `assetRevision` se subió de `1` a `2` en cada `WallpaperSpec` (el contrato exige cambiar la revisión al cambiar los bytes, para que el marcador `id:revision` no crea erróneamente que el wallpaper viejo ya está aplicado). `WallpaperController`/`AndroidWallpaperWriter`/`WallpaperMarkerPolicy` no se tocaron: sólo cambiaron specs/assets/revisión, como pide el contrato. Se añadió `wallpaper/WallpaperBackdrop.kt`, pieza nueva que faltaba desde Sprint 2: decodifica el asset de cada rice con downsampling al viewport (nunca a resolución completa) en `Dispatchers.IO`, con una cache acotada a dos bitmaps activos (contract §19), y lo pinta como fondo interno del Home (y del Drawer donde el diseño lo pide: detrás del panel de Arctic, detrás del sheet de Violet). Esto es aparte del `WallpaperController` que aplica el wallpaper de sistema — arquitectura sin cambios, sólo una superficie de pintado interna que no existía.

### Icon treatment (§9)

`apps/IconLoader.kt` gana un parámetro `IconTreatment` (`Normal`/`Monochrome`) y `IconLoader.Result(bitmap, isAlphaMask)`:

- Si hay una capa `AdaptiveIconDrawable.monochrome` real (API ≥33) y se pidió tratamiento monocromo, se rasteriza como máscara blanca-sobre-transparente (`PorterDuff.SRC_IN`) — sin colorear en el loader. El tinte final (tinta de Monochrome vs. tinta de Ivory, opuestas) se aplica en `AppIcon` con `ColorFilter.tint(...)` en tiempo de composición, así un mismo bitmap cacheado sirve a los dos rices que lo piden.
- Si no hay capa monocromo, se desatura el icono original (`ColorMatrix.setSaturation(0f)`) preservando su variación de luminancia — nunca se convierte en silueta maciza de un solo tono.
- Legacy (no adaptive) usa fit centrado con 12% de margen, sin estirar; adaptive se sigue renderizando completo.
- Arctic/Ember/Violet piden `IconTreatment.Normal` (color real) y en su lugar cambian placa/borde/máscara (`AppIcon` ahora acepta `plateShape`/`plateColor`).

### Reloj y fecha (§16 tarea 6, §3.5)

`launcher/ClockProvider.kt` (nuevo): `produceState` + `repeatOnLifecycle(STARTED)`, tick alineado al siguiente minuto exacto vía `java.time`/`Duration`, nunca por segundo, inerte fuera de STARTED. `ui/shared/Clock.kt` (nuevo): funciones puras de formato (`formatClockTime`/`formatClockAmPm`/`formatClockDate`) más `rememberIs24HourFormat`/`rememberCurrentLocale` que releen `DateFormat.is24HourFormat`/`LocalConfiguration` en cada recomposición por cambio de configuración. No existe un "widget reloj" visual compartido: cada rice llama al mismo provider/formato y compone su propia jerarquía (tamaño, alineación, si hay AM/PM aparte, si hay regla).

### Motion (§10, §16 tarea 7)

`rice/Rice.kt` amplía `RiceMotion` con enter/exit ms+easing, `MotionDisplacement` (`Fixed(dp)` o `HeightFraction(0.12f)` para el sheet de Violet), y escala/duración de press — con los cinco valores exactos del contrato como constantes (`RiceMotion.Monochrome`, `.Arctic`, `.Ember`, `.Ivory`, `.Violet`). `ui/shared/Motion.kt` (nuevo) añade `rememberReducedMotion()` (basado en `ValueAnimator.areAnimatorsEnabled()`, re-evaluado en cada `ON_RESUME`, sin permiso ni ajuste propio) y `rememberPressScale(...)` (mecánica de escala por `interactionSource`, compartida; cada rice decide qué anima con ella — Ember también invierte fondo/borde, Ivory no usa escala y en su lugar cambia tinta/fondo).

`launcher/LauncherHost.kt`: `AnimatedContent` sobre una clave `(riceId, screen)`; si cambia de rice usa crossfade de 180ms fijo (o instantáneo con reduced motion); si sólo cambia de pantalla dentro del mismo rice, usa el `RiceMotion` de ese rice (duración/easing/desplazamiento reales, vía `fadeIn/fadeOut + slideInVertically/slideOutVertically` sobre `graphicsLayer`). Sólo se renderiza el árbol entrante/saliente de esa transición puntual — nunca los cinco rices vivos a la vez. `PredictiveBackHandler` sustituye (no añade) los antiguos `BackHandler` de Drawer/Picker: colecciona el progreso real del gesto en un `Animatable`, aplica alpha/translationY al contenido activo, y en cancelación anima de vuelta a 0 (150ms) en vez de saltar; en commit marca un flag de una sola vez para que la transición de `AnimatedContent` que sigue sea instantánea (`EnterTransition.None`/`ExitTransition.None`), evitando la doble animación que el contrato advierte explícitamente. `LauncherHost` también aplica `isAppearanceLightStatusBars`/`isAppearanceLightNavigationBars` según `Rice.lightSystemBars` cada vez que cambia el rice activo (contract §3.7), pieza que tampoco existía en Sprint 2.

### Rice picker (§16 tarea 4 y "Rice picker")

`rice/RicePicker.kt`: cada miniatura ahora se pinta con el fondo/tinta *real* de ese rice (no un card neutro compartido) y un dibujo de estructura actualizado para reflejar el diseño final — el de Ember pasó de una matriz 3×2 genérica a "un bloque ancho + grid 2×2", el de Ivory ganó una marca de margen grande simulando la letra de grupo. Selección explícita (tap → `onSelect` → persist en el ViewModel → Home → wallpaper async, sin cambios en esa cadena), long press vacío sigue inerte (el picker no tiene gesto de fondo), cancelar con Back no cambia el rice (mismo `viewModel.goHome()` que antes, ahora enrutado por el `PredictiveBackHandler` combinado).

### Ergonomía / accesibilidad (§16 tarea 8, §18.1)

Todos los targets interactivos nuevos son ≥48dp (filas, pills, botones cuadrados/circulares) vía `defaultMinSize`/`heightIn(min=...)`, incluidos los triggers tipográficos de Monochrome que antes no tenían target explícito. Cada celda con long press expone `onLongClickLabel` (añadir/quitar favorito) para TalkBack. Los grids de Ember/Arctic recalculan columnas con `BoxWithConstraints`/`fontScale` en vez de encoger targets o texto. `EmptyState`/búsqueda de cada rice toman color explícito (antes `EmptyState` no recibía color y heredaba el default de Material, ilegible sobre fondos oscuros). Labels largos usan `TextOverflow.Ellipsis` con `maxLines` explícito en todas las celdas/filas nuevas.

## 3. Lo que NO se pudo verificar en esta sesión

- **Compilación real.** Ver §1. Se hizo una revisión estática dedicada (ver más abajo) pero no reemplaza `assembleDebug`.
- **Apariencia física real.** No hay teléfono ni emulador en este entorno; todas las descripciones de composición son a partir del código, no de una captura de pantalla real.
- **D01/D03/D04/D05/D09/D10/D11 y la prueba visual de 10 pantallas en escala de grises.** Ninguna fila de la matriz se declara `PASS`; ver §4.

## 4. Gate físico Sprint 3 (matriz del prompt)

**Estado: PENDING — PHYSICAL DEVICE VALIDATION.** Ninguna fila se declara `PASS` sin dispositivo real.

| ID | Procedimiento | Estado |
|---|---|---|
| D01 | Install → rol → Home → swipe → buscar → abrir → Home | No ejecutada |
| D03 | Abrir Drawer, focus búsqueda, Back, Back | No ejecutada |
| D04 | Long press app, cerrar; long press vacío; cancelar picker | No ejecutada |
| D05 | Añadir 5, intentar 6, quitar 1, añadir 1; cambiar rice | No ejecutada |
| D09 | 5 rices; barras/cutout/IME; navegación botones y gestos disponibles | No ejecutada |
| D10 | Font grande/TalkBack/motion off | No ejecutada |
| D11 | 10 cambios rápidos de rice + volver desde otra app | No ejecutada |
| — | 10 pantallas Home/Drawer × 5 rices, mismos 5 favoritos, condiciones comparables | No ejecutada |
| — | Las mismas 10 pantallas en escala de grises: cada rice debe reconocerse sin color | No ejecutada |
| — | 0 favoritos / 1 favorito / 5 favoritos | No ejecutada |
| — | fontScale grande / TalkBack / motion off / IME / gesture navigation / cutout | No ejecutada |

## 5. Desviaciones reales del contrato

- **Sin build/test/lint reales en esta sesión:** idéntico a S1/S2, ver §1. `test`/`lint`/`assembleDebug` sólo se conocerán reales cuando el usuario los ejecute en Arch.
- **Sin evidencia visual real:** todas las descripciones de §2 son lectura de código, no capturas de pantalla ni video del dispositivo — el contrato es explícito en que el video/capturas son evidencia, no sustituibles por descripción; quedan como pendiente físico.
- **Tratamiento del layer monocromo de iconos:** cuando existe una capa `AdaptiveIconDrawable.monochrome` real, se tiñe siempre a la tinta del rice que la pide (correcto), pero no se probó en dispositivo con un icono que realmente exponga esa capa (API 33+, poco común aún en catálogos reales) — la ruta de desaturación (la que cubre la inmensa mayoría de iconos hoy) sí es la más ejercitada conceptualmente pero tampoco se vio en pantalla real.
- **Predictive back:** implementado con `PredictiveBackHandler`, `Animatable` para el progreso, y un flag de una sola pasada para evitar la doble animación tras commit; sin dispositivo Android 15/16 con gesture navigation no se pudo confirmar el comportamiho real del progreso frame a frame, sólo la lógica.
- **No se generó documentación de accesibilidad TalkBack real** (recorrido de foco, anuncios): se añadieron `onLongClickLabel` y colores explícitos, pero un recorrido real con TalkBack activo está en la matriz pendiente (D10).

## 6. Pendientes concretos antes de mergear Sprint 3

1. Ejecutar en el Arch del usuario: `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`, instalar el APK debug.
2. Correr la matriz D01/D03/D04/D05/D09/D10/D11 de §4 y las 10+10 capturas (color y escala de grises) con los mismos cinco favoritos.
3. Probar 0/1/5 favoritos, fontScale grande, TalkBack, motion off, IME, gesture navigation y cutout en los cinco rices.
4. D11 específicamente: 10 cambios rápidos de rice y confirmar que el wallpaper final coincide con el rice final.
5. Revisar warnings reales de `lint`/compilador una vez que el build corra (no verificado aquí).
6. Confirmar que el Predictive Back gesture (Android 14+/gesture navigation) no duplica la animación de salida tras completar el gesto, en dispositivo real.
