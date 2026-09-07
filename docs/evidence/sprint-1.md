# Sprint 1 — Launcher Spine — Evidencia

**Branch:** `feat/launcher-spine`
**SHA evaluado:** `54f81df1fd83439cb14520c2d55f559a418f3a84`
**Entorno de ejecución:** sesión remota (Claude Code on the web), sin teléfono físico ni Android SDK instalado en el contenedor.

## 1. Limitación real de entorno (no del contrato)

Este contenedor no tiene Android SDK, `adb`, `kotlinc` ni un JDK 17 instalado, y la política de red egress bloquea `dl.google.com` (y sus alias `maven.google.com`, `plugins.gradle.org` cuando redirige allí) con `CONNECT tunnel failed, response 403`. Google Maven es la fuente tanto del plugin AGP (`com.android.application`) como de cualquier artefacto `androidx.*`, y también la fuente de descarga de SDK Platform/Build-Tools vía `sdkmanager`. Como consecuencia:

- **Sí funciona:** descarga de la distribución Gradle 9.5.0 real (`services.gradle.org`) y su checksum oficial (vía `services.gradle.org/versions/all`, sin redirección), usados para generar `gradle/wrapper/gradle-wrapper.properties` con `distributionSha256Sum` real, no inventado.
- **No funciona en esta sesión:** resolver el plugin `com.android.application:9.3.2` (bloqueado en el paso `buildscript`/`pluginManagement`, antes de tocar ningún código propio), y por lo tanto tampoco compilar módulo `:app` ni ejecutar `./gradlew test` / `assembleDebug` / `:app:dependencies --write-locks`.

**Qué esperaba el contrato:** que `./gradlew test`, `./gradlew assembleDebug` y el lockfile pasen en S1 (§14, Definition of Done).
**Qué evidencia lo contradice:** el error real de Gradle (reproducido abajo) muestra que la resolución del plugin AGP falla por bloqueo de red del entorno, no por un error de configuración del proyecto.
**Modificación mínima:** ninguna al contrato ni al código; se documenta el bloqueo y se deja el trabajo listo para compilar en un entorno con Android SDK y acceso real a Google Maven (Android Studio Quail 4 según §2.2, o un runner CI con acceso a `dl.google.com`).

No se intentó eludir la política de red (proxy, mirrors no oficiales, deshabilitar verificación TLS): sería contrario a "no inventar evidencia" y a las prácticas de seguridad esperadas.

## 2. Comandos ejecutados y resultado real

```
$ ./gradlew --version
# OK — distribución 9.5.0 descargada y verificada correctamente.

$ ./gradlew assembleDebug
FAILURE: Build failed with an exception.
* Where: Build file '/home/user/rice-mobil/build.gradle.kts' line: 7
* What went wrong:
Plugin [id: 'com.android.application', version: '9.3.2', apply: false] was not found in any of the following sources:
- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Included Builds (No included builds contain this plugin)
- Plugin Repositories (could not resolve plugin artifact 'com.android.application:com.android.application.gradle.plugin:9.3.2')
  Searched in the following repositories:
    Google
    MavenRepo
    Gradle Central Plugin Repository
BUILD FAILED in 1s

$ ./gradlew test
# Falla con el mismo error exacto (resolución de plugin, antes de compilar ningún .kt propio).

$ ./gradlew :app:dependencies --write-locks
# No ejecutado: fallaría en el mismo punto (resolución de plugin) antes de poder generar el lockfile.

$ adb devices -l
bash: adb: command not found
```

Diagnóstico de red (para que quien retome esto en un entorno con SDK no repita la investigación):

```
$ curl -sSI https://dl.google.com/...        -> CONNECT tunnel failed, response 403 (bloqueado)
$ curl -sSI https://maven.google.com/...     -> 301 (redirige a dl.google.com, bloqueado igual)
$ curl -sSI https://plugins.gradle.org/m2/...-> 303 (redirige a dl.google.com, bloqueado igual)
$ curl -sS https://services.gradle.org/versions/all -> 200 OK (usado para el checksum real del wrapper)
```

## 3. Qué se implementó (verificable por lectura, no por build en esta sesión)

- Gradle/wrapper 9.5.0 (checksum real), AGP 9.3.2 + Kotlin 2.4.10 fijados en `build.gradle.kts`/`app/build.gradle.kts` según contrato §2.3, sin `org.jetbrains.kotlin.android`, sin catálogo de versiones adicional.
- `AndroidManifest.xml`: Activity única `singleTask`, filtros `HOME+DEFAULT` y `LAUNCHER` separados, sin `SET_WALLPAPER` (entra en S2), sin permisos no autorizados.
- `RoleManager`/`ROLE_HOME`: solicitud vía `ActivityResultContracts.StartActivityForResult`, registrado incondicionalmente en el constructor de la Activity (antes de `STARTED`), reevaluación en cada `onResume`, fallback a `Settings.ACTION_HOME_SETTINGS` → `ACTION_SETTINGS`.
- `edge-to-edge` habilitado antes de `setContent`.
- `AppKey`/`AppEntry`, `AppSearch` (normalización NFD/lowercase/whitespace, orden por `Collator` + desempate componente/serial, filtro por tokens) puros, sin tipos Android.
- `AppsRepository`: enumeración exclusiva de `Process.myUserHandle()`, intersección real `MAIN+LAUNCHER` vía `PackageManager.queryIntentActivities`, sin `QUERY_ALL_PACKAGES`; callback único registrado antes del primer refresh; cola conflated con consumidor único serie; `start()`/`stop()` idempotentes; conserva el último snapshot en caso de fallo.
- `IconLoader`: `LruCache` acotada (`min(24 MiB, ...)`), máximo 2 decodificaciones concurrentes, sin tematización (eso es S3).
- `AppLauncher`: `LauncherApps.startMainActivity`, nunca `getLaunchIntentForPackage`, resultados tipados (`Started`/`Unavailable`/`Denied`).
- `LauncherViewModel`/`LauncherState`/`LauncherNavigation`: subconjunto S1 del estado completo de §5 (catálogo/query/pantalla), filtro cooperativo con chequeo de cancelación cada 64 elementos, guard de doble-tap liberado en `onResume`, reglas de navegación puras y testeables (Home intent, back/IME, éxito/fallo de apertura).
- `MonochromeHome`/`MonochromeDrawer` provisionales: swipe-up para abrir drawer, lista buscable con estados loading/empty/error, sin favoritos/menú (correcto para S1).
- Tests JVM puros: `AppSearchTest` (acentos, mayúsculas, espacios, unicode/emoji, label vacío, empate, package, query vacía, clamp) y `LauncherNavigationTest` (reset Home incluso repetido, éxito limpia, fallo conserva, precedencia back/IME).

## 4. Gate físico D01–D04

**Estado: PENDING — PHYSICAL DEVICE VALIDATION.**

No hay teléfono ni `adb` disponibles en esta sesión. Ninguna fila de la matriz D01–D12 fue ejecutada; ninguna se declara `PASS`. Pendiente para quien continúe con hardware real:

| ID | Estado |
|---|---|
| D01 | No ejecutada |
| D02 | No ejecutada |
| D03 | No ejecutada |
| D04 | No ejecutada |

No hay modelo/API de teléfono, modo de navegación ni número de apps del catálogo que registrar porque no hubo dispositivo conectado.

## 5. Pendientes que impiden declarar Sprint 1 cerrado

1. Ejecutar `./gradlew :app:dependencies --write-locks`, `./gradlew test`, `./gradlew assembleDebug` en un entorno con Android SDK real (Platform 37, Build-Tools 36.0.0) y acceso de red a Google Maven — ninguno pudo ejecutarse aquí.
2. Instalar el APK debug en un teléfono físico API>=29 y ejecutar el gate literal D01, más D02–D04.
3. Instalar una app de prueba para confirmar el refresh de catálogo en vivo (§14, paso 8).
4. Revisar el resultado real de lint/warnings del compilador una vez que el build corra (no verificado aquí).

Nada de esto bloquea la entrega del código: todo el trabajo de S1 está commiteado y pusheado en `feat/launcher-spine`, listo para que el primer build real confirme o señale errores puntuales de compilación.

## 6. Validación real ejecutada por el usuario (entorno local, no este sandbox)

**Importante:** todo lo siguiente fue ejecutado por el usuario en su propia máquina Arch Linux, con Android SDK/JDK/`adb` reales instalados localmente. Esta sesión de Claude Code (sandbox remoto) no ejecutó ninguno de estos comandos ni tiene acceso al teléfono; se registra aquí lo que el usuario reportó, sin inventar outputs exactos de consola que este entorno no produjo.

**SHA validado:** `27cfd5c` (fix de compilación en `feat/launcher-spine`, cabeza del PR #1 en el momento del merge) o un descendiente válido de esa branch.

**Hardware:** teléfono físico CUBOT KINGKONG X, conectado por USB vía `adb`.

### Comandos ejecutados por el usuario — resultado reportado

| Comando | Resultado |
|---|---|
| `./gradlew test` | PASS |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | PASS |

### Gate físico D01–D04 — reportado PASS

- rice-mobile fue seleccionado como Home vía RoleManager.
- Pulsar Home físico regresa correctamente a rice-mobile.
- Swipe up desde el fondo libre de Home abre el Drawer.
- El catálogo real del dispositivo aparece en el Drawer.
- La búsqueda local filtra correctamente sobre ese catálogo.
- Abrir una app desde el Drawer funciona y, al volver a Home, el launcher queda limpio (query vacía, sin drawer/menú abiertos).

Con esto, D01 (instalar → rol → Home → swipe → buscar → abrir → Home) y las verificaciones asociadas a D02–D04 relevantes para S1 quedan **PASS**, según lo reportado por el usuario sobre hardware real. El aspecto visual de Monochrome en S1 es deliberadamente provisional (contrato §14) y no fue ni debe ser evaluado como diseño final.

### Cierre

Con toolchain, build automatizado y gate físico confirmados sobre hardware real por el usuario, **Sprint 1 (Launcher Spine) queda cerrado**. El PR #1 (`feat/launcher-spine` → `main`) fue mergeado por el usuario (`merged_by: CesarManzoCode`, merge commit `e0a4d8e`) y `main` en `origin` ya contiene el árbol completo de S1. Sprint 2 (`feat/rice-engine`) parte de ese `main`.
