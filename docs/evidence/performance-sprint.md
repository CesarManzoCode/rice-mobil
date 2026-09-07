# Sprint de Performance — Arctic Glass / rice-mobile

**Branch:** `feat/v1-rices` (mismo PR #5, sin nuevo branch/PR).
**Base de esta pasada:** `b762cb6` (última reconstrucción visual de Arctic — Home/Drawer/wallpaper).
**Entorno de escritura del código:** sesión remota (Claude Code on the web). **Sin Android
SDK/`adb`/emulador en este contenedor** — se intentó `./gradlew help` (offline y online) y falló al
resolver el plugin `com.android.application` porque no hay SDK local instalado. Esto es una
limitación dura del sandbox, no del proyecto: significa que **nada de lo que sigue puede incluir un
número de FPS, jank o cold start medido en este entorno**. Todo lo cuantitativo queda para el
usuario en su Arch con el CUBOT KingKong X conectado, con los comandos exactos en la sección 4.

## 0. Regla de honestidad de esta pasada

Por instrucción explícita de la tarea: **PERFORMANCE GATE = PENDING** hasta que el usuario mida en
el dispositivo físico. Nada de lo escrito abajo declara "más fluido" o "menos jank" como hecho: se
separa en tres categorías.

- **MEDIDO** — con número real, de un comando real, en un dispositivo real. *(vacío en esta pasada
  — cero acceso a adb/SDK en este sandbox.)*
- **VERIFICADO ESTÁTICAMENTE** — confirmado leyendo el código: una recomposición evitable, una capa
  de overdraw eliminada, una asignación movida fuera del hot path. Es un hecho sobre el código, no
  sobre el frame time.
- **NO VERIFICADO** — hipótesis razonable o cambio que solo demuestra su valor con el dispositivo.

## 1. Debug vs Release — lo que se pudo confirmar sin compilar

No se pudo generar `app-release.apk` en este sandbox (no hay SDK para invocar el compilador de
Android/R8). Se revisó la configuración de Gradle en su lugar:

- `app/build.gradle.kts`: `release { isMinifyEnabled = true; isShrinkResources = true }`, con
  `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
  Ya estaba así **antes** de esta pasada — no fue necesario tocarlo.
- `debug { applicationIdSuffix = ".debug" }` — sin overrides de `isDebuggable`, por lo que toma el
  default de AGP (`true` en debug, `false` en release). Correcto, no se cambió.
- `proguard-rules.pro` está vacío a propósito ("ninguna librería basada en reflexión necesita regla
  todavía" — comentario ya existente). No hay razón para añadir reglas: el proyecto no usa
  Room/Hilt/Retrofit/Gson.
- Búsqueda de `Log.`/`println(` en `app/src/main/kotlin`: **cero resultados**. No hay logging de
  depuración que limpiar del hot path.
- `lint { abortOnError = true }` ya configurado.

**Conclusión estática:** la config de release ya era correcta antes de este sprint; no había nada
que arreglar en Fase 2. Lo que sí es cierto — y **no verificado aquí, solo señalado** — es que
Compose en `debug` paga overhead real e independiente de este código: sin R8, con la
instrumentación de recomposición de `debug-tooling`/live-literals activa, sin inlining agresivo de
lambdas, y con el runtime de Compose sin las optimizaciones que R8 aplica sobre `androidx.compose`
en release. Ese coste existe en **cualquier** app Compose y no es evidencia de un problema de
diseño de rice-mobile — pero tampoco se puede cuantificar cuánto pesa aquí sin medir ambas builds
en el mismo dispositivo (sección 4).

## 2. Auditoría de recomposición / overdraw — hallazgos y fixes aplicados

Cuatro commits en esta pasada, cada uno acotado a un hallazgo concreto leído en el código (no
superstición):

### 2.1 `perf: mark rice view models as stable for Compose`

**Hallazgo:** `HomeModel`, `DrawerModel` (en `rice/Rice.kt`) y `CategoryGroup` (en
`rice/DrawerHierarchy.kt`) son `data class` con campos `List<...>`. El compilador de Compose trata
`List` como inestable por defecto (es una interfaz mutable), así que cualquier composable que
reciba uno de estos modelos **nunca podía saltarse la recomposición** aunque el contenido fuera
idéntico al del frame anterior — cada emisión de `LauncherViewModel.uiState` (incluida una que solo
cambia `wallpaperStatus` o un `message` de toast) forzaba una re-ejecución completa de
`rice.Home(...)`/`rice.Drawer(...)` y de cada `CategoryTile`.

**Fix:** `@Immutable` en las tres clases. Es honesto, no una mentira al runtime: estas listas
siempre son snapshots recién derivados (`filter`/`map`/`associateBy` en el ViewModel o en
`DrawerHierarchy.categorize`), nunca mutados en el sitio después de construirse. `AppEntry`/`AppKey`
ya eran completamente estables sin anotar (solo `String`/`Long`/enum), así que no se tocaron.

**Categoría:** VERIFICADO ESTÁTICAMENTE (es una propiedad del código, confirmable leyendo las
clases). El *efecto* en frames reales — NO VERIFICADO.

### 2.2 `perf: precompute launcher hierarchy models in the ViewModel`

**Hallazgo doble en `LauncherViewModel`:**

1. `LauncherHost.kt` llamaba `viewModel.favoriteSlots(state)` dentro del propio cuerpo del
   composable — recalculando `apps.associateBy { it.key }` sobre todo el catálogo y volviendo a
   mapear las claves de favoritos **en cada recomposición de Home**, en vez de usar algo ya
   calculado. Compose no puede "cachear" eso por sí solo: es una llamada a función normal, no un
   `remember`.
2. El único `combine()` de nivel superior de `uiState` recalculaba `appsByKey`,
   `favorites.mapNotNull`, y `recentApps.mapNotNull` sobre la lista completa de apps **cada vez que
   cualquiera de sus 5 flows emitía** — incluido el flow de resultados de búsqueda (cada letra
   tecleada) y el de estado del wallpaper, que no tienen relación con el catálogo ni las
   preferencias.

**Fix:** se extrajo un `combine(repository.catalog, preferencesRepository.snapshots)` anidado
(`catalogPrefsModel`) que precalcula `appsByKey`/`favorites`/`favoriteSlots`/`recentApps` una sola
vez por cambio real de catálogo o preferencias. El `combine` externo ahora solo junta ese modelo ya
listo con `transient`/`searchResults`/`wallpaperStatus`, sin repetir el trabajo pesado. Se añadió
`favoriteSlots: List<FavoriteSlot>` a `LauncherState` y `LauncherHost` ahora lee `state.favoriteSlots`
directamente — cero recomputo en el árbol de Compose.

**Categoría:** VERIFICADO ESTÁTICAMENTE (menos trabajo por keystroke es una propiedad estructural
del flujo de datos, no una medición). Cuánto CPU real ahorra por letra tecleada en el CUBOT — NO
VERIFICADO.

### 2.3 `perf: reduce arctic glass overdraw for repeated tiles`

**Hallazgo:** `ArcticGlassSurface` (usado por *todo* módulo de vidrio de Arctic) siempre aplicaba
`Modifier.shadow(elevation = 16.dp, ambientColor = ..., spotColor = ...)`. Para los módulos sueltos
de Home (reloj, glance, dock — 1 instancia cada uno) eso es razonable. Pero `Chip` (fila de
categorías, `LazyRow`, hasta ~8 visibles) y `CategoryTile` (grid de categorías, hasta ~8
simultáneos) son la **misma superficie repetida muchas veces a la vez** en contenido que hace
scroll — exactamente el patrón que la Fase 11 de la tarea pedía auditar ("Modifier.shadow... si
produce coste, sustituye por borde/gradiente/highlight").

**Fix:** se añadió el parámetro `elevated: Boolean = true` a `ArcticGlassSurface`. `Chip` y
`CategoryTile` pasan `elevated = false`: el degradado de relleno + el borde con brillo (que ya
existían) siguen leyéndose como vidrio sin pagar una capa de sombra por cada tile repetido. Ningún
otro sitio de llamada cambió — los módulos de Home, el buscador y el panel de "Todas las apps"
conservan exactamente el mismo look. Además se movieron los `Brush` del borde y del brillo superior
a constantes de módulo (`ARCTIC_BORDER_BRUSH`, `ARCTIC_SHEEN_BRUSH`) y el `Brush` de relleno a
`remember(tint, baseAlpha)`, evitando reasignarlos en cada llamada a la función (Fase 10:
"Brush remembered/cached").

**Categoría:** VERIFICADO ESTÁTICAMENTE (menos capas `RenderNode`/shadow por frame en el grid es un
hecho sobre el árbol de dibujo). Si esto es perceptible en el CUBOT — NO VERIFICADO, es la hipótesis
más fuerte de esta pasada para el jank de scroll/categorías reportado, pero sigue siendo hipótesis
hasta medir.

### 2.4 Zonas auditadas SIN cambios (ya correctas)

Estas zonas fueron leídas específicamente porque la tarea las señalaba como sospechosas, y **no se
tocaron** porque ya cumplían el objetivo — forzar un cambio ahí habría sido "optimización por
superstición":

- **`ClockProvider`** (`launcher/ClockProvider.kt`): usa `produceState` local a quien lo llama, con
  `repeatOnLifecycle(STARTED)`. No vive en `LauncherState`, así que un tick de reloj nunca invalida
  Drawer/catálogo/wallpaper. Ya cumplía la Fase 16 explícitamente.
- **`IconLoader`** (`apps/IconLoader.kt`): `LruCache` acotado (24 MB) por `IconKey` (componente +
  serial + revisión + densidad + tamaño + tratamiento), semáforo de 2 decodificaciones concurrentes,
  todo el decode/rasterizado en `Dispatchers.IO`. Ya cumplía la Fase 7 explícitamente — no hay
  recarga de `PackageManager`/bitmap por recomposición.
- **`WallpaperBackdrop`** (`wallpaper/WallpaperBackdrop.kt`): decode muestreado al tamaño de
  viewport (nunca el asset completo), cache acotada a 2 bitmaps (LRU con reordenado por acceso),
  decode en `Dispatchers.IO`, pinta un color de fallback inmediato mientras decodifica. Ya cumplía
  la Fase 8.
- **`rememberBatterySnapshot`/`rememberNextAlarmSnapshot`** (`system/*.kt`): receptor de broadcast
  vía `callbackFlow` dentro de `repeatOnLifecycle(STARTED)`, sin polling, sin registro duplicado.
  Ya cumplía la Fase 17.
- **`AppSearch`** (`apps/AppSearch.kt`): `normalizedLabel`/`normalizedPackage` ya se calculan **una
  vez** al construir cada `AppEntry` (en `AppsRepository.toEntry`), no por keystroke. `tokensOf`
  normaliza solo la query (una string corta), no las 71 apps. Ya cumplía la Fase 6.
- **`LauncherViewModel.openApp`** (`launcher/LauncherViewModel.kt`): `launcher.launch(key)` se
  ejecuta primero de forma síncrona; `preferencesRepository.recordAppOpened(key)` se dispara con
  `viewModelScope.launch { }` (fire-and-forget) *después*. La apertura de la app nunca espera al
  DataStore. Ya cumplía la Fase 18/19.
- **`ArcticDrawer.BrowseView`**: `categories`/`favoriteApps` ya usan `remember(model.results, ...)`.
  El `Crossfade` entre `Browse`/`Category`/`AllApps` monta **un solo árbol a la vez** (nunca
  Browse+AllApps simultáneos) — ya cumplía la Fase 14.
- **`LazyColumn`/`LazyVerticalGrid`** en `ArcticDrawer.kt`: todas usan `key = { ... }` estable
  (`userSerial:component`), sin animaciones de item costosas. Ya cumplía la Fase 13.

Estas ocho verificaciones son igualmente VERIFICADO ESTÁTICAMENTE — pero la conclusión es "correcto,
no tocar", no un commit.

## 3. Lo que NO se pudo hacer en este sandbox

- No se generó `app-release.apk` (sin SDK de Android para invocar `com.android.application`/R8).
- No se ejecutó `./gradlew test|lint|assembleDebug` (mismo motivo — falla al resolver el plugin de
  Gradle, con o sin red).
- No se capturó ningún número de `dumpsys gfxinfo`, `am start -W`, ni percentiles de jank.
- No se validó Baseline Profile (Fase 22): no tiene sentido evaluarlo sin antes tener una medición
  base de cold start real.

**Por lo tanto: PERFORMANCE GATE = PENDING.** Ninguno de los cuatro commits de este sprint puede
declararse "mejora de rendimiento confirmada" hasta que el usuario los mida en el CUBOT KingKong X.

## 4. Comandos exactos para medir en Arch (BEFORE/AFTER)

Con el dispositivo conectado por `adb` (`adb devices` debe mostrarlo como `device`, no
`unauthorized`). Sustituye `<pkg>` por `dev.cesarmanzocode.ricemobile.debug` (build debug) o
`dev.cesarmanzocode.ricemobile` (build release).

### 4.1 Builds

```bash
# Debug (el que el usuario ya probó)
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release (necesita firma local — ver nota abajo)
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Nota de firma para prueba local:** si `assembleRelease` falla por falta de `signingConfig`, usa
la keystore de debug de AGP únicamente para esta prueba local de rendimiento — **nunca** para
publicar:

```bash
# Solo para medir localmente. No es la firma de producción.
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=$HOME/.android/debug.keystore \
  -Pandroid.injected.signing.store.password=android \
  -Pandroid.injected.signing.key.alias=androiddebugkey \
  -Pandroid.injected.signing.key.password=android
```

### 4.2 Cold start / warm start

```bash
# Cold start: fuerza matar el proceso primero
adb shell am force-stop <pkg>
adb shell am start -W -n <pkg>/dev.cesarmanzocode.ricemobile.MainActivity

# Warm start: el proceso ya vive, solo vuelve a traer la Activity al frente
adb shell input keyevent KEYCODE_HOME
adb shell am start -W -n <pkg>/dev.cesarmanzocode.ricemobile.MainActivity
```

Anota `TotalTime` (lo que ve el usuario) y `WaitTime` de la salida de `am start -W`.

### 4.3 Frames / jank (dumpsys gfxinfo)

```bash
# Resetea los contadores antes de la interacción que quieres medir
adb shell dumpsys gfxinfo <pkg> reset

# ... aquí, en el dispositivo: abre el drawer, haz scroll, cambia de categoría, etc. ...

# Vuelca el resumen (incluye Total frames, Janky frames, percentiles 50/90/95/99)
adb shell dumpsys gfxinfo <pkg>
```

Repite esta secuencia reset→interacción→dump por separado para cada interacción de la sección 4.5,
para no mezclar los números de "abrir drawer" con los de "scroll".

### 4.4 Perfetto / atrace (opcional, si `dumpsys gfxinfo` no basta para diagnosticar dónde se va el frame)

```bash
adb shell atrace --async_start -b 16000 gfx view sched freq idle
# ... interactúa con el launcher ...
adb shell atrace --async_stop > trace.txt
```

O, si el dispositivo soporta Perfetto vía `adb shell perfetto` (API 28+), usa la UI de
[ui.perfetto.dev](https://ui.perfetto.dev) para abrir el trace y mirar el track de la app.

### 4.5 Interacciones concretas a medir (BEFORE = commit `b762cb6`, AFTER = HEAD de este sprint)

Para cada una: `dumpsys gfxinfo <pkg> reset`, ejecuta la interacción ~5 veces seguidas, luego
`dumpsys gfxinfo <pkg>`.

| Interacción | Cómo reproducirla en el dispositivo |
|---|---|
| Drawer open | Desde Home, swipe-up (o tap en los page dots) 5 veces, volviendo a Home entre cada una |
| Drawer scroll | En el Drawer, con "Todas las apps" abierto, scroll de arriba a abajo y viceversa varias veces |
| Category navigation | Tap en 3-4 categorías distintas desde el grid, volviendo a Browse cada vez |
| Search typing | Tap en el buscador, escribe una query de 6-8 caracteres letra a letra |
| Rice switch | Abre el selector de rice (botón ◐), cambia entre 2 rices un par de veces |
| App open | Desde Home o Drawer, abre una app real y vuelve con back/home |

### 4.6 Comparación BEFORE/AFTER

```bash
git log --oneline b762cb6..HEAD -- app/src/main/kotlin
```

Para reproducir el estado BEFORE exacto: `git worktree add /tmp/rice-before b762cb6` en otra
carpeta, compilar esa copia como `app-debug-before.apk`/`app-release-before.apk` con un
`applicationIdSuffix` distinto (o desinstalando entre pruebas) para no pisar la instalación AFTER,
y repetir 4.2–4.5 sobre ambas.

## 5. Riesgos conocidos de los cambios de esta pasada

- `elevated = false` en `Chip`/`CategoryTile` es un cambio visual mínimo (sin sombra propia en esos
  dos tipos de tile) — revisar visualmente en el dispositivo que el contraste borde+degradado siga
  leyéndose como "vidrio" a la luz del día, no solo en el emulador/captura.
- La restructuración de `combine()` en `LauncherViewModel` no cambia ningún contrato observable de
  `uiState` (mismos campos, mismos valores) — es una reorganización interna del cálculo, no un
  cambio de comportamiento. Sin un test de ViewModel existente que lo cubra explícitamente en este
  repo, la garantía es por lectura de código, no por test automatizado.
