# Sprint 2 — Rice Engine + Persistencia — Evidencia

**Branch:** `feat/rice-engine` (integrado en `main`)
**Entorno de escritura del código:** sesión remota (Claude Code on the web), sin teléfono físico ni Android SDK/red completa en el contenedor.
**Entorno de validación real:** Arch Linux del usuario + teléfono físico CUBOT KINGKONG X, ejecutado localmente por el usuario después de la integración a `main`.

## 1. Limitación del sandbox remoto (no cambia entre sprints)

El sandbox de esta sesión sigue sin Android SDK/`adb` y su política de red sigue bloqueando `dl.google.com` (y `maven.google.com`/`plugins.gradle.org` cuando redirigen ahí), que es de donde resuelven tanto el plugin AGP como cualquier artefacto `androidx.*`. Por eso el build/test/lint/assembleDebug de Sprint 2 no se pudieron ejecutar dentro de esta sesión remota — ver el detalle que ya quedó registrado en el momento de la implementación. Esa limitación es del entorno de escritura, no del código ni del producto.

## 1bis. Validación real ejecutada por el usuario (Arch + CUBOT KINGKONG X)

El usuario ejecutó la validación completa de Sprint 2 en su máquina Arch Linux contra el teléfono físico CUBOT KINGKONG X, fuera de este sandbox. Resultados reportados directamente por el usuario:

| Comando/escenario | Resultado |
|---|---|
| `./gradlew test` | **PASS**, después de un fix de scheduling en `WallpaperSequencingTest` (uso de `runCurrent()` para sincronizar el consumidor conflated antes de aserciones; ya presente en el código de `main`). |
| `./gradlew lint` | **PASS** |
| `./gradlew assembleDebug` | **PASS** |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | **PASS** |
| Cambio entre los cinco rices (Monochrome/Arctic/Ember/Ivory/Violet) | **PASS** — estructura cambia visiblemente al seleccionar cada rice. |
| Favoritos (añadir/quitar) | **PASS** |
| Apertura de un favorito | **PASS** |
| Persistencia (rice y favoritos sobreviven relanzar la app) | **PASS** |
| Funcionalidad previa de Sprint 1 (drawer, búsqueda, apertura general, Home) | **PASS** — sin regresión. |

El usuario confirma que, funcionalmente, el gate físico de Sprint 2 se considera cumplido en ese dispositivo. No se dispone de la salida exacta de consola de esos comandos ni de capturas/logs adjuntos a esta sesión, así que este documento no la transcribe; se registra únicamente el resultado reportado por escenario, tal como fue comunicado.

No todas las filas de la matriz original de 9 escenarios (§3) recibieron una confirmación explícita independiente por parte del usuario en esta ronda (por ejemplo el límite de sexto favorito rechazado, el slot "no disponible" tras desinstalar, o el cambio rápido A→B→C→D terminando en el rice/wallpaper vigente). Esas quedan marcadas por separado abajo como pendientes de una pasada dedicada; no se infieren como `PASS` a partir de las pruebas generales reportadas. Sprint 3 vuelve a ejercitar estos mismos escenarios (D05/D06/D11) sobre los cinco diseños terminados, así que se revalidan de todas formas.

## 2. Qué se implementó (verificable por lectura, no por build en esta sesión)

### Rice engine (contract §6)

- `rice/RiceId.kt`: enum con IDs persistentes fijos y `fromPersisted()` total (nunca `enum.valueOf` sobre datos sin validar); unknown -> Monochrome.
- `rice/Rice.kt`: interfaz `Rice` (`id`, `wallpaper`, `motion`, `lightSystemBars`, `Home`/`Drawer` sólo con `model`/`actions`), `FavoriteSlot`, `HomeModel`, `DrawerModel`, `RiceActions`, `RiceMotion`, y `buildFavoriteSlots()` puro (una clave favorita ausente del catálogo se vuelve un slot "no disponible", nunca se descarta).
- `rice/RiceRegistry.kt`: lista fija de cinco, orden Monochrome → Arctic → Ember → Ivory → Violet, `require` de IDs únicos y completos.
- `rice/RicePicker.kt`: cinco miniaturas dibujadas con `Canvas` que reflejan la geometría real de cada rice (dock, matriz, índice, lista, cluster), no rectángulos recoloreados.
- Cinco implementaciones reales, cada una con Home/Drawer estructuralmente distintos (sin `BaseRiceHome` compartido):
  - **Monochrome:** columna vertical, favoritos como lista lineal; Drawer de lista única.
  - **Arctic Glass:** dock horizontal flotante en Home; Drawer con grid dentro de un panel translúcido.
  - **Ember Forge:** favoritos en matriz/bloques; Drawer de dos columnas de filas compactas.
  - **Ivory Paper:** favoritos como índice textual numerado; Drawer como índice editorial con reglas.
  - **Violet Night:** cluster de favoritos 1-2-2; Drawer como sheet que sube desde abajo.
- `AppIcon` ahora lee el `IconLoader` de un `CompositionLocal` (`LocalIconLoader`), porque la firma de `Rice.Home`/`Drawer` la fija el contrato sin permitir servicios en los parámetros.

### Persistencia (contract §11)

- `preferences/LauncherPreferences.kt`: DataStore singleton (`launcherDataStore`), `PreferencesRepository` con `selectRice`/`toggleFavorite`/`removeFavorite`/`markWallpaperApplied`, todas dentro de `store.edit { }`. `PreferencesSnapshot.Ready`/`Unavailable` distingue una lectura sana de un `IOException` real (que no borra lo persistido).
- `preferences/FavoriteCodec.kt`: codec exacto de §11 (`serial\tcomponent`, máx. 5 líneas, sin `stringSet`), tolerante a líneas corruptas/tab extra/serial negativo, deduplicando por primera aparición.
- `preferences/FavoriteRules.kt`: máximo 5, orden de adición, quitar-y-volver-a-añadir manda al final, sexto rechazado sin tocar la lista.

### Wallpaper (contract §8)

- `wallpaper/WallpaperController.kt`: escritor serial único sobre un canal `CONFLATED`; una selección rápida A→B→C termina escribiendo C, nunca en paralelo. La escritura real está detrás de la interfaz `WallpaperWriter` para que el controller sea una unidad JVM pura en tests.
- `wallpaper/AndroidWallpaperWriter.kt`: implementación real con `WallpaperManager.setStream(..., FLAG_SYSTEM)`, nunca `FLAG_LOCK`, nunca lee el wallpaper actual del usuario.
- `wallpaper/WallpaperMarkerPolicy.kt`: reglas puras del marcador `id:assetRevision` — sólo se persiste si el rice sigue siendo el vigente; una discrepancia entre marcador aplicado y rice actual dispara reintento (proceso muerto entre commit y wallpaper).
- Cinco PNG provisionales de 64×64 en `assets/wallpapers/` (marcador explícito de que Sprint 3 los reemplaza por los assets definitivos del contrato §8).

### Estado / ViewModel (contract §5, §6.3)

- `LauncherState`/`TransientState` ampliados al shape completo del contrato (`preferencesReady`, `rice`, `favoriteKeys`/`favorites`, `appMenu`, `preferencesWritable`, `wallpaperStatus`).
- `LauncherViewModel` sigue siendo la única fuente de estado: `combine()` de catálogo + preferencias + transitorio + resultados de búsqueda + estado del wallpaper controller, `stateIn` con `WhileSubscribed(5_000)`. Nuevas acciones: `selectRice`, `toggleFavorite`, `showAppMenu`/`dismissAppMenu`, `retryWallpaper`, chequeo de consistencia del wallpaper una vez por entrada a foreground.
- `LauncherHost` renderiza sólo el rice activo (vía `RiceRegistry.of(state.rice)`), muestra una superficie neutra hasta `preferencesReady`, expone el menú contextual (`AppActionsMenu`) y un aviso de reintento de wallpaper como overlays globales del host (no de cada rice).

### Tests (contract §12)

- `FavoriteCodecTest`: round trip, payload ausente, tab extra/serial negativo/componentes malformados descartados sin crashear, >5 recortado a 5, duplicados conservan la primera aparición.
- `FavoriteRulesTest`: orden de adición, sexto rechazado sin tocar la lista, quitar-y-re-añadir al final, dos aliases del mismo paquete son favoritos distintos, y `buildFavoriteSlots` conserva un favorito ausente/con catálogo vacío como slot no disponible en vez de borrarlo.
- `WallpaperSequencingTest`: un `WallpaperWriter` fake bloqueable prueba que A/B/C rápido termina en C sin escritura paralela; un fallo o bloqueo nunca llama a `onApplied`; `WallpaperMarkerPolicy` cubre "marcador sólo para el rice vigente" y "marcador viejo/ausente tras muerte simulada exige reintento".

## 3. Gate físico Sprint 2 (matriz del prompt)

**Estado: FUNCIONALMENTE VALIDADO** por el usuario en Arch + CUBOT KINGKONG X (ver §1bis); las filas específicas de la matriz original de 9 escenarios que no recibieron confirmación dedicada quedan explícitas como pendientes de una pasada puntual, no como fallidas:

| # | Escenario | Estado |
|---|---|---|
| 1 | Monochrome → Arctic cambia estructura | **PASS** (cambio entre los cinco rices confirmado) |
| 2 | Arctic → Ember cambia estructura | **PASS** (cambio entre los cinco rices confirmado) |
| 3 | Matar proceso → reabrir → Ember sigue seleccionado | **PASS** (persistencia confirmada de forma general) |
| 4 | Añadir favorito → cambiar rice → sigue favorito | **PASS** (favoritos + persistencia confirmados) |
| 5 | Reiniciar proceso → favorito y orden persisten | **PASS** (persistencia confirmada) |
| 6 | Añadir cinco → intentar sexto → sin cambios | Pendiente de pasada dedicada — no ejercitado explícitamente en esta ronda; cubierto por `FavoriteRulesTest` en `test`. Se revalida en el gate físico de Sprint 3 (D05). |
| 7 | Desinstalar favorito → slot unavailable removible | Pendiente de pasada dedicada — no ejercitado explícitamente en esta ronda. Se revalida en Sprint 3/4 (D07). |
| 8 | Cambio rápido Mono→Arctic→Ember→Violet → Violet + su wallpaper | Pendiente de pasada dedicada — el cambio general de rice pasó, pero la secuencia rápida específica no se reportó por separado. Se revalida en el gate físico de Sprint 3 (D11). |
| 9 | Sprint 1 sigue funcionando (drawer/búsqueda/abrir/Home) | **PASS** (confirmado explícitamente por el usuario) |

`test`, `lint` y `assembleDebug` PASS reales del usuario; APK instalada por `adb` y usada en dispositivo. Ninguna fila anterior se marca `PASS` sin ese reporte directo del usuario.

## 4. Desviaciones reales del contrato

- **Motion (§10):** `Rice.motion` existe con los tiempos de entrada/salida de drawer del contrato, pero el host todavía no los usa para animar (`AnimatedContent`/easing/`PredictiveBackHandler`); eso es explícitamente polish de Sprint 3, y S2 pidió sólo la estructura de la interfaz. Documentado, no oculto.
- **Lockfile:** no se pudo regenerar `dependencyLocking` con `--write-locks` en el sandbox remoto (bloqueo de red descrito en §1); el usuario ejecutó `test`/`lint`/`assembleDebug` con éxito en su entorno local, que sí resuelve dependencias.
- **Build/test/lint dentro de esta sesión remota:** nunca se pudieron ejecutar aquí (§1); la evidencia real proviene enteramente de la ejecución local del usuario (§1bis), no de este sandbox.

## 5. Cierre de Sprint 2

Sprint 2 se considera funcionalmente cerrado: `test`/`lint`/`assembleDebug` PASS reales, APK instalada, y el motor de rices/persistencia validado en dispositivo físico por el usuario (§1bis, §3). Quedan como pendientes explícitos, no bloqueantes para avanzar a Sprint 3, y se revalidan dentro de su propio gate físico:

1. Pasada dedicada de los escenarios #6 (sexto favorito rechazado), #7 (slot unavailable tras desinstalar) y #8 (cambio rápido de rice terminando en el vigente) de la matriz de §3 — cubiertos por Sprint 3 D05/D07/D11.
2. Confirmación explícita en dispositivo de que el wallpaper se aplica con `FLAG_SYSTEM` sin afectar el lock screen (§8) — Sprint 3 reemplaza los assets provisionales por los definitivos y vuelve a ejercitar esto.
3. Revisión de warnings reales de `lint`/compilador más allá del PASS binario reportado (no se dispone de la salida completa en esta sesión).
