# Sprint 2 — Rice Engine + Persistencia — Evidencia

**Branch:** `feat/rice-engine`
**Entorno de ejecución:** sesión remota (Claude Code on the web), sin teléfono físico ni Android SDK/red completa en el contenedor.

## 1. Limitación real de entorno (idéntica a Sprint 1, no del contrato)

Este sandbox sigue sin Android SDK/`adb` y su política de red sigue bloqueando `dl.google.com` (y `maven.google.com`/`plugins.gradle.org` cuando redirigen ahí) con `403`, que es de donde resuelven tanto el plugin AGP como cualquier artefacto `androidx.*`. Se repitió la comprobación en esta sesión:

```
$ ./gradlew --version
# OK — Gradle 9.5.0 se descarga y arranca correctamente (services.gradle.org no está bloqueado).

$ ./gradlew test
FAILURE: Build failed with an exception.
* Where: Build file '/home/user/rice-mobil/build.gradle.kts' line: 7
* What went wrong:
Plugin [id: 'com.android.application', version: '9.3.2', apply: false] was not found in any of the following sources:
...
BUILD FAILED in 23s
```

**Consecuencia:** `./gradlew :app:dependencies --write-locks`, `./gradlew test`, `./gradlew lint` y `./gradlew assembleDebug` **no se pudieron ejecutar en esta sesión**, igual que en Sprint 1. No se declara `PASS` para ninguno de ellos aquí. El código se revisó manualmente con la mayor atención posible (balance de llaves/paréntesis por archivo, imports cruzados, firmas de contrato) pero eso no sustituye una compilación real.

**Pendiente explícito para el usuario en su Arch:**

```bash
git fetch origin feat/rice-engine
git switch feat/rice-engine
./gradlew :app:dependencies --write-locks
./gradlew test
./gradlew lint
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

El lockfile de Gradle (`dependencyLocking`) tampoco pudo regenerarse aquí por el mismo bloqueo de red; queda pendiente el primer `--write-locks` real con DataStore ya en las dependencias (§2.3 del contrato).

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

**Estado: PENDING — PHYSICAL DEVICE VALIDATION**, igual que Sprint 1 hasta que el usuario lo ejecute en su Arch/CUBOT KINGKONG X:

| # | Escenario | Estado |
|---|---|---|
| 1 | Monochrome → Arctic cambia estructura | No ejecutada |
| 2 | Arctic → Ember cambia estructura | No ejecutada |
| 3 | Matar proceso → reabrir → Ember sigue seleccionado | No ejecutada |
| 4 | Añadir favorito → cambiar rice → sigue favorito | No ejecutada |
| 5 | Reiniciar proceso → favorito y orden persisten | No ejecutada |
| 6 | Añadir cinco → intentar sexto → sin cambios | No ejecutada |
| 7 | Desinstalar favorito → slot unavailable removible | No ejecutada |
| 8 | Cambio rápido Mono→Arctic→Ember→Violet → Violet + su wallpaper | No ejecutada |
| 9 | Sprint 1 sigue funcionando (drawer/búsqueda/abrir/Home) | No ejecutada |

Ninguna fila se declara `PASS` sin dispositivo real.

## 4. Desviaciones reales del contrato

- **Motion (§10):** `Rice.motion` existe con los tiempos de entrada/salida de drawer del contrato, pero el host todavía no los usa para animar (`AnimatedContent`/easing/`PredictiveBackHandler`); eso es explícitamente polish de Sprint 3, y S2 pidió sólo la estructura de la interfaz. Documentado, no oculto.
- **Lockfile:** no se pudo regenerar `dependencyLocking` con `--write-locks` en este sandbox (mismo bloqueo de red que Sprint 1); pendiente en el entorno del usuario.
- **Sin build/test/lint reales ejecutados aquí:** ver §1. Todo lo demás sigue el contrato §15 en el orden indicado, sin ampliar scope (nada de Room, Navigation Compose, Hilt, widgets, folders, drag, icon packs, settings generales, ni trabajo de Sprint 3).

## 5. Pendientes concretos antes de mergear Sprint 2

1. Ejecutar en el Arch del usuario: `./gradlew :app:dependencies --write-locks`, `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`.
2. Instalar el APK debug e correr la matriz física de 9 escenarios de este documento (equivalente a D05/D06/D11 + regresión D01 del contrato).
3. Confirmar en dispositivo real que wallpaper se aplica (`FLAG_SYSTEM`) y que el lock screen no se ve afectado, según §8.
4. Revisar warnings reales de `lint`/compilador una vez que el build corra (no verificado aquí).
