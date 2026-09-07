# Sprint 3 — Segunda pasada mayor (rediseño visual sobre mockups aprobados)

**Branch:** `feat/v1-rices` (mismo PR #5, sin nuevo branch/PR)
**Base de esta pasada:** `1a46c2c` (Sprint 3 primera pasada, ya con `test`/`lint`/`assembleDebug` validados localmente por el usuario en Arch, según el propio prompt de esta tarea).
**HEAD tras esta pasada:** `0667998`
**Entorno de escritura del código:** sesión remota (Claude Code on the web). Sin Android SDK/`adb`/emulador en este contenedor — idéntica limitación a S1/S2/S3-primera-pasada. `pip install pillow numpy` sí tuvo acceso de red (pypi.org) y se usó exclusivamente para generar los wallpapers offline, nunca en runtime de la app.

**Commits de esta pasada (`1a46c2c..0667998`):**

```
0fd1d1e fix: restore app content in themed drawers
b36d288 feat: add local app hierarchy and recent history
2a9297a feat: rebuild monochrome around approved mockup
9aed81c feat: rebuild ivory around approved mockup
ae790dc feat: rebuild ember around approved mockup
db7fa14 feat: rebuild arctic around approved mockup
4ee703c feat: rebuild violet around approved mockup
78dac7c feat: refine wallpapers icons and rice picker
0667998 feat: crossfade drawer hierarchy navigation
```

## 0. Autoridad visual de esta pasada

El usuario adjuntó cinco mockups aprobados (Monochrome, Arctic Glass, Ember Forge, Ivory Paper,
Violet Night) y los declaró la nueva verdad visual objetivo, con instrucción explícita de
acercarse lo máximo posible sin falsificar datos. Esta pasada reemplaza el estado visual de la
primera pasada de Sprint 3 (ver `docs/evidence/sprint-3.md`), no la sustituye como historial: ese
documento sigue siendo válido para lo que describe (arquitectura de motion/iconos/reloj de la
primera pasada), y este documento cubre exclusivamente lo que cambió en la segunda pasada.

## 1. Bug de drawers (Task 0)

**Síntoma reportado:** Arctic/Ember/Violet mostraban el conteo real de resultados (p. ej. "71
resultados") pero el área de catálogo aparecía vacía o casi vacía.

**Causa real (confirmada por lectura de código, no por captura de pantalla):** el
`decorationBox` del `BasicTextField` de búsqueda envolvía su contenido en
`Box(Modifier.fillMaxSize(), ...)`. Ese campo de búsqueda es el último hijo, no ponderado
(`weight`), de un `Column` cuyo primer hijo es el catálogo con `Modifier.weight(1f)`. Compose mide
los hijos no ponderados de un `Column` **antes** de repartir el espacio restante entre los
ponderados — así que el `Box(fillMaxSize())` del campo de búsqueda recibía como restricción de
altura máxima la altura *completa* aún no repartida del `Column`, no la fracción que le
correspondía a esa fila. El campo terminaba reclamando prácticamente toda la altura disponible,
dejando el `Box(weight(1f))` del grid/lista sin espacio real.

**Fix:** en `ArcticDrawer.kt`, `EmberDrawer.kt` y `VioletDrawer.kt`, el `decorationBox` cambió a
`Box(Modifier.fillMaxWidth(), ...)` — mismo patrón que ya usaban Monochrome/Ivory, que nunca
tuvieron el bug. Commit `fix: restore app content in themed drawers`, separado y pusheado antes de
tocar nada visual, como pedía la tarea.

## 2. Arquitectura de recientes/categorías (Task local hierarchy)

- **`apps/AppCategory.kt`** (nuevo): enum determinista mapeado 1:1 desde
  `ApplicationInfo.category` (Comunicación←SOCIAL, Productividad←PRODUCTIVITY,
  Multimedia←AUDIO/VIDEO/IMAGE, Noticias←NEWS, Mapas y viajes←MAPS, Juegos←GAME); todo lo demás cae
  en Herramientas (si es app de sistema, `FLAG_SYSTEM`) u Otros. 100% offline, sin red, sin IA, sin
  heurística de nombre/paquete. Ninguna app desaparece de "Todas" por su categoría.
- **`apps/AppEntry.kt`** gana `category: AppCategory = AppCategory.Other` (default preserva
  compatibilidad con los tests existentes que construían `AppEntry` sin ese campo).
- **`apps/AppsRepository.kt`** calcula la categoría de cada `AppEntry` a partir de
  `LauncherActivityInfo.applicationInfo` (ya disponible en la estructura enumerada, sin IPC
  adicional).
- **Recientes:** `preferences/RecentAppsCodec.kt` + `RecentAppsRules.kt` (mismo formato de línea
  `serial<TAB>component` que `FavoriteCodec`, extraído a un parser común en
  `AppKeyLineCodec.kt` para no duplicar validación). `recordOpen` mueve la key al frente y
  deduplica; tope `MAX_RECENTS = 12`. `PreferencesRepository.recordAppOpened(key)` se llama desde
  `LauncherViewModel.openApp` en `LaunchResult.Started`, en un `viewModelScope.launch` que no
  bloquea ni retrasa el regreso a Home. **Nunca** `UsageStatsManager`, **nunca** permiso especial.
- **`rice/DrawerHierarchy.kt`** (nuevo): `categorize()` (orden fijo, categorías vacías ausentes) y
  `groupByInitial()` (compartido, reemplaza la copia que sólo tenía Ivory). `DrawerView` (sealed:
  `Browse`/`Category`/`AllApps`) es el estado de navegación interno que los cinco Drawers
  instancian con `remember { mutableStateOf(...) }` — decisión de cada rice, nunca parte de
  `LauncherScreen`/el host.
- **`system/BatteryGlance.kt`** / **`system/NextAlarmGlance.kt`** (nuevos): proveedores de datos
  reales sin permisos nuevos — batería vía el broadcast sticky `ACTION_BATTERY_CHANGED`
  (`registerReceiver(null, filter)` para el valor inicial, receiver real sólo mientras el host está
  `STARTED`), próxima alarma vía `AlarmManager.nextAlarmClock` (la misma información que ya
  muestra el icono de alarma en la barra de estado). Ninguno de los dos es un service ni un
  trabajo periódico en background.
- Tests nuevos: `AppCategoryTest`, `DrawerHierarchyTest`, `RecentAppsRulesTest`,
  `RecentAppsCodecTest` — puros, sin Android real en runtime (las constantes de
  `ApplicationInfo.category` son `int` compile-time, se resuelven en bytecode sin invocar el
  stub de Android).

## 3. Qué datos reales usa cada Home

Ningún Home muestra clima, calendario, canción o batería/alarma inventados. Fuentes reales:

| Dato | Origen | Permiso |
|---|---|---|
| Reloj/fecha | `ClockProvider` (`java.time`, ya existente) | Ninguno |
| Batería/carga | `system/BatteryGlance.kt` | Ninguno (broadcast público) |
| Próxima alarma | `system/NextAlarmGlance.kt` (`AlarmManager.nextAlarmClock`) | Ninguno |
| App más reciente / recientes | `LauncherPreferences.recentApps` (historial local del propio launcher) | Ninguno |
| Favoritos | Ya existente (Sprint 2) | Ninguno |

Cuando un dato no está disponible (sin alarma programada, sin historial aún), el módulo que lo
mostraría se oculta o se reduce — nunca se rellena con un valor de relleno.

## 4. Por rice: qué tan cerca quedó del mockup aprobado

### Monochrome
- **Home:** reloj SansSerif Black en el tercio superior (igual que antes), pero ahora con un
  módulo compacto con borde debajo (batería + próxima alarma reales) donde el mockup tiene su
  bloque tipo reproductor — misma posición/masa, contenido honesto. Favoritos siguen siendo la
  lista lineal indexada del tercio inferior.
- **Drawer:** ya no es una lista plana de 71 apps desde el primer frame. Landing = Recientes (si
  hay historial) + Categorías (fila con conteo real + flecha) + "Todas las apps" con conteo.
  "Todas las apps" abre un índice A–Z real con **rail lateral tocable** que hace scroll a la letra
  (contract mockup: "rail A–Z a la derecha").
- **Distancia al mockup:** estructural y de jerarquía, muy cercana; el rail A–Z interactivo es una
  adición razonable no descrita letra por letra en el mockup pero coherente con "índice + rail" que
  sí se ve en la imagen.

### Arctic Glass
- **Home:** el par de paneles glass del mockup (clima + agenda) se convirtió en un panel
  batería/próxima-alarma y un panel de recientes — misma masa, posición y materialidad glass
  (translúcido + borde + sombra corta), contenido real. Dock/pill/círculo Rice sin cambios.
- **Drawer:** cambio más visible de esta pasada — **search ahora vive arriba** del panel (como en
  el mockup, a diferencia de todos los demás rices), seguido de chips horizontales, Recientes,
  tiles de Categorías (2 columnas), y "Todas las apps" como fila explícita antes del grid completo.
- **Distancia al mockup:** alta — layout, orden de secciones y posición de search coinciden con la
  imagen; los tiles de categoría son una interpretación razonable (Android no ofrece "Internet"
  como categoría real, ver §17).

### Ember Forge
- **Home:** header comprimido sin cambios; se añadió la fila de dos tarjetas pequeñas (batería +
  próxima alarma, mockup: "BATERÍA"/"PRÓXIMO EVENTO") y un bloque ancho "ACCESO RÁPIDO" con
  recientes reales en vez del bloque de música del mockup. Matriz de favoritos y barra inferior
  sin cambios.
- **Drawer:** landing ahora es Acceso rápido (tiles) + Categorías como paneles industriales de 2
  columnas (nombre + conteo, cut corners, borde cobre) + fila secundaria "Todas las apps" — antes
  era directamente el grid denso de 1–2 columnas.
- **Distancia al mockup:** alta en el dashboard de Home (dos cards + bloque ancho es
  estructuralmente el mismo layout); el Drawer sigue el texto del prompt más que la imagen (el
  mockup no muestra el drawer de Ember), y es coherente con el lenguaje industrial ya establecido.

### Ivory Paper
- **Home:** el cambio más importante de esta pasada para este rice. Se añadió el bloque "HOY"
  editorial (batería/próxima alarma/app reciente, cada fila presente sólo si hay dato real) que
  faltaba por completo, y los favoritos pasaron de índice de sólo texto a **iconos editoriales**
  en placas color crema con leyenda serif — el mockup muestra explícitamente iconos en el Home
  (esto reemplaza la decisión "sin iconos en Home" de la primera pasada, que la propia
  especificación de esta tarea pide invertir: "favoritos abajo como iconos editoriales").
- **Drawer:** el índice alfabético con margen (ya bien resuelto en S3-primera-pasada) se conserva
  intacto, pero deja de ser la vista de aterrizaje: ahora hay Recientes + Categorías primero.
- **Distancia al mockup:** alta — el Home ya no se siente "vacío"; el bloque HOY y los iconos
  eran exactamente la brecha que el usuario señaló como "técnicamente correcto pero horrible en
  uso real".

### Violet Night
- **Home:** el hero module de música del mockup se convirtió en un hero real — la app más
  reciente en grande con un botón "abrir", más una línea de batería/próxima alarma — misma
  posición/tamaño/panel violeta que el mockup, nunca datos de reproducción falsos. Cluster 1-2-2 y
  footer sin cambios.
- **Drawer:** mismo sheet real (88% alto, wallpaper visible arriba) pero con chips + Recientes +
  grid de Categorías antes de "Todas las apps", en vez de un grid de 3 columnas desde el primer
  frame. El header del sheet gana una flecha "atrás" al navegar una categoría/el índice completo.
- **Distancia al mockup:** alta en Home (posición/masa del hero calca el mockup); el Drawer sigue
  la especificación de texto (el mockup no muestra el drawer de Violet).

## 5. Wallpapers

Cinco WebP nuevos, generados offline con un script Python (`Pillow`/`numpy`, semilla fija,
sin red en tiempo de ejecución de la app) que vive fuera del repo de producción
(scratchpad de la sesión) — sólo los `.webp` resultantes se comitean. Dirección aplicada, no copia
literal del mockup:

| Rice | Peso | Dirección |
|---|---|---|
| `monochrome.webp` | ~10 KiB | Negro mate con una masa tipo eclipse casi invisible arriba-derecha y ruido de muy baja amplitud. |
| `arctic_glass.webp` | ~24 KiB | Niebla helada con luz superior-derecha y capas de montaña translúcidas. |
| `ember_forge.webp` | ~64 KiB | Carbón con fisuras finas y resplandor cobre inferior-lateral. |
| `ivory_paper.webp` | ~180 KiB | Papel cálido con grano de fibra y sombras botánicas suaves (forma de hoja real, no óvalos). |
| `violet_night.webp` | ~28 KiB | Luna grande con sombreado suave (sin cráteres discretos) y halo atmosférico, niebla de montaña abajo. |

Todos ≤2 MiB (muy por debajo). `assetRevision` subió de `2` a `3` en los cinco `WallpaperSpec` —
el controlador/writer no se tocó, sólo specs/bytes, como exige el contrato de wallpaper.

## 6. Icon treatment

- Monochrome/Ivory conservan el tratamiento monocromo (capa `AdaptiveIconDrawable.monochrome` si
  existe, desaturación si no) de la primera pasada — ya cumplía el mockup.
- **Arctic:** las placas de icono pasan de blanco translúcido genérico a un tinte cian de marca
  muy sutil (`ARCTIC_ICON_PLATE`), acercándose a "halo cyan muy sutil" del contrato de iconos.
- **Violet:** el icono del hero module del Home gana un borde fino con el acento violeta —
  "glow muy controlado" en vez de un blur real.
- **Ember:** sin cambio (ya usaba placas oscuras + cut corners + acento cobre, correcto desde la
  primera pasada).

## 7. Rice picker

Los cinco dibujos estructurales (`RicePicker.kt`) ganaron el módulo nuevo de cada rice: una barra
para Monochrome, dos paneles para Arctic, un par de tarjetas para Ember, un bloque "HOY" para
Ivory, un panel hero para Violet — para que la miniatura siga reflejando geometría real, no cinco
colores.

## 8. Motion

Se añadió un `Crossfade` de 150ms alrededor del cambio interno Browse/Categoría/Todas-las-apps en
los cinco Drawers — antes era un salto instantáneo. No afecta la transición de ruta del host
(`AnimatedContent` con los valores de `RiceMotion` por rice, sin cambios), no afecta la búsqueda
(que sigue mostrando resultados planos sin animación de por medio) y no retrasa abrir una app.

## 9. Build/test/lint disponibles en esta sesión

**Igual que en toda sesión anterior de este proyecto: este contenedor no tiene Android SDK ni
`adb`.** `./gradlew help` falla al resolver el plugin AGP (sin acceso a `dl.google.com`/
`maven.google.com`). No se declara `PASS` para `test`/`lint`/`assembleDebug` aquí — el usuario ya
confirmó en el prompt de esta tarea que los ejecutó localmente en Arch para el estado previo a
esta pasada; debe repetirlos para este HEAD:

```bash
git fetch origin feat/v1-rices
git switch feat/v1-rices
./gradlew test
./gradlew lint
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En su lugar se hizo revisión estática dedicada: balance de llaves por archivo, imports duplicados,
referencias completamente calificadas accidentales (se encontraron y corrigieron dos import lines
rotas por un `sed`/reemplazo de texto demasiado agresivo durante la limpieza de imports de
`MonochromeDrawer.kt`, antes de comitear), y una relectura completa de cada archivo nuevo/tocado
contra las firmas reales de `Rice`/`RiceActions`/`HomeModel`/`DrawerModel`/`AppIcon`/`EmptyState`.
Un hallazgo real que sí se corrigió antes de comitear: `IvoryHome.HoyModule` llamaba
`stringResource(...)` dentro del lambda no-`@Composable` de `buildList { }` — se resolvieron las
cadenas fuera del `buildList` primero.

## 10. Gate físico pendiente (idéntico en forma al de S1/S2/S3-primera-pasada)

**Estado: PENDING — PHYSICAL DEVICE VALIDATION.** Ninguna fila se declara `PASS` sin dispositivo
real. Además de la matriz D01/D03/D04/D05/D09/D10/D11 ya pendiente de la primera pasada, esta
pasada añade comprobaciones específicas:

| Verificación nueva de esta pasada | Estado |
|---|---|
| Bug de drawers realmente resuelto en Arctic/Ember/Violet (catálogo visible con el conteo real) | No ejecutada |
| Historial de recientes persiste tras reiniciar el proceso y se actualiza al abrir apps | No ejecutada |
| Categorías reales del catálogo del usuario (cuántas apps caen en "Otros" en la práctica) | No ejecutada |
| Batería/próxima alarma se actualizan en vivo (enchufar/desenchufar, programar una alarma) | No ejecutada |
| Rail A–Z de Monochrome hace scroll a la letra correcta | No ejecutada |
| Comparación física mockup vs. Home/Drawer real, los diez casos de la tarea | No ejecutada |
| `assembleDebug`/`test`/`lint` en este HEAD exacto | No ejecutada en esta sesión |

## 11. Desviaciones inevitables respecto a los mockups

1. **Categorías:** Android no expone una categoría real "Internet" en `ApplicationInfo.category`
   (sólo GAME/AUDIO/VIDEO/IMAGE/SOCIAL/NEWS/MAPS/PRODUCTIVITY/ACCESSIBILITY). No se inventó un
   detector heurístico de navegadores: la mayoría de apps de terceros que no declaran categoría
   caen en "Otros", exactamente como esta misma tarea anticipó ("no necesitas producir esta lista
   exacta si Android no distingue alguna bien").
2. **Datos del hero/módulos:** los mockups muestran clima, calendario y reproductor de música;
   esta pasada los sustituye siempre por batería/próxima alarma/recientes reales, nunca por una
   imitación de esos datos.
3. **Drawers de Ember/Ivory-hoy/Violet-drawer no aparecen en los mockups adjuntos** (los mockups
   sólo muestran Home+Drawer para Arctic/Monochrome/Violet-ish, e Ivory Home+Drawer, Ember sólo
   Home+Drawer sin foto de referencia visual — se revisó la imagen adjunta real de cada uno antes
   de escribir esto). Donde no había imagen de referencia directa, se siguió el texto detallado de
   la tarea, tal como esa misma tarea instruye ("si imagen y texto discrepan, prioriza el texto").
4. **Sin validación física ni de build en esta sesión** — ver §9/§10, idéntico al patrón de toda
   sesión anterior de este proyecto en este entorno.
5. **AllApps/rail A-Z, chips, Crossfade** son extrapolaciones razonables del texto/mockup, no
   trazables letra por letra a un pixel del mockup — se documentan aquí en vez de presentarlas
   como si vinieran literalmente de la imagen.

## 12. Pendientes concretos antes de cerrar esta pasada

1. Ejecutar en el Arch del usuario `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`
   sobre este HEAD (`0667998`), instalar el APK debug.
2. Confirmar visualmente el fix del bug de drawers en Arctic/Ember/Violet.
3. Comparar físicamente cada Home/Drawer real contra su mockup (los diez casos pedidos).
4. Ejercitar recientes/categorías con el catálogo real del teléfono y confirmar que "Otros" no se
   siente como un cajón de sastre inaceptable (si lo es, es una limitación de datos de Android, no
   de esta implementación — documentado en §11).
5. Repetir D01/D03/D04/D05/D09/D10/D11 (no repetidas automáticamente por este cambio).
