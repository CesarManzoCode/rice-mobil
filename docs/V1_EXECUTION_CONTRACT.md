# rice-mobile — V1 EXECUTION CONTRACT

**Repositorio:** `CesarManzoCode/rice-mobile`  
**Destino:** `docs/V1_EXECUTION_CONTRACT.md`  
**Fecha de investigación:** 2026-09-06  
**Ejecutor:** Claude Code Sonnet · **Producto:** launcher Android V1.0.0  
**Estado:** especificación documental; no es evidencia de un build ni de pruebas físicas ejecutadas.

## 0. Cómo ejecutar este contrato

Leer §§1–13 y el sprint solicitado; consultar §§18–21 durante su ejecución. Las decisiones aquí fijadas son normativas. Los snippets expresan contratos y algoritmos, no archivos de producción completos: añadir imports y ensamblaje en los archivos indicados. No volver a investigar la arquitectura ni sustituir versiones por `latest`.

- **HECHO VERIFICADO:** comportamiento respaldado por una fuente oficial enlazada junto al punto material.
- **DECISIÓN:** elección del proyecto; no se atribuye a Android ni pretende ser la única arquitectura posible.
- **VALIDAR EN TELÉFONO:** evidencia que esta investigación no puede producir. Registrar pendiente hasta ejecutarla; un build no la sustituye.

No hay checkout accesible en el workspace de preparación; se entrega este documento para copiar al repositorio. No se afirma que el repositorio remoto no exista. No se crea implementación, branch o PR desde esta fase documental.

### Disciplina vinculante durante implementación

Un sprint por branch indicada; partir de `main` que contenga el sprint anterior. Commits pequeños por unidades funcionales; **push después de cada commit coherente** y antes de terminar una sesión. PR al terminar el bloque; título, problema, resultado y evidencia reales. Mantener historial revisable; no hacer trabajo funcional sobre `main`, no reescribir commits publicados ni borrar trabajo ajeno. Si no existe una convención previa, merge por squash del PR conservando su descripción; si el usuario define otra, obedecerla. Las pruebas físicas ya pedidas son gates del producto, no solicitudes nuevas de permiso. Se puede abrir el PR con hardware pendiente, pero no declarar terminado el sprint ni simular evidencia.

No abrir issues para cada tarea; abrirlos para bugs reproducibles que queden pendientes o decisiones que realmente deban rastrearse. Resolver defectos locales con fixes locales. No añadir sprints, frameworks o aprobaciones. Si falta acceso a GitHub, conservar commits y comunicar el bloqueo de push; no declarar trabajo respaldado remotamente.

## 1. PRODUCT CONTRACT

**rice-mobile** es un launcher Android personal: elegir un rice completo transforma composición, tipografía y motion. Comparte catálogo, acciones y preferencias, pero no obliga a compartir geometría.

### V1 exacta

Home predeterminable; catálogo de actividades lanzables del usuario actual; apertura de apps; actualización del catálogo por cambios de paquetes y al volver al foreground; drawer; búsqueda local; 0–5 favoritos ordenados; selector de cinco rices; persistencia; wallpaper estático por rice; iconos tratados visualmente; reloj/fecha internos; gestos mínimos; motion; APK firmada instalable; validación física y 24–48 h de uso.

Rices y sus IDs persistentes: `monochrome`, `arctic_glass`, `ember_forge`, `ivory_paper`, `violet_night`. El inicial es Monochrome. Selección explícita; sin Material Dynamic Color que sustituya sus paletas.

**DECISIÓN de perfiles:** V1 muestra el usuario Android en el que se ejecuta la app (`Process.myUserHandle()`). No agrega work profiles, usuarios secundarios, clones de OEM ni perfiles privados. Investigar perfiles no implica implementar administración multiperfil: §3.3 fija el límite y evita identidades ambiguas. README debe decir “apps del perfil actual”, nunca “todas las apps de todos los perfiles”.

### Exclusiones congeladas

Widgets Android, folders, drag-and-drop, icon packs externos, badges, feeds/noticias, web search, IA, cuentas, cloud, backup/sync, páginas Home adicionales, editor de grid, preferencias extensas, ocultación de apps, Private Space, app locking, live wallpapers, optimización tablet/foldable, landscape como objetivo específico, monetización. Tampoco acceso a notificaciones, AccessibilityService, usage stats, device admin, root, servicios persistentes, integración Quickstep/SystemUI ni Play Store.

### Invariantes

1. Una identidad = componente lanzable + serial de usuario. Nunca label, posición ni sólo package.
2. Catálogo y favoritos iguales entre rices; el layout no almacena reglas de negocio.
3. Máximo cinco favoritos; orden de adición; quitar y volver a añadir coloca al final.
4. Home del sistema cierra drawer/picker/menú/IME y limpia búsqueda. No se relanza automáticamente una app.
5. Ninguna llamada de disco, enumeración de paquetes o decodificación de imágenes dentro de composición o en el hilo principal.
6. Instalar/actualizar un paquete no borra favoritos por un vacío transitorio.
7. Un icono o wallpaper fallido no impide usar Home o abrir otras apps.
8. Long press nunca lanza la app al soltar. Gestos internos no secuestran los bordes del sistema.
9. Un rice se reconoce en escala de grises por estructura, además de su color.
10. APK sin red ni permisos sensibles; estado local. No prometer supervivencia del proceso: reconstruirlo.

## 2. VERIFIED TOOLCHAIN

### 2.1 Versiones fijadas

| Elemento | Pin del contrato | Evidencia / elección |
|---|---|---|
| Android Studio estable actual | Quail 4 · `2026.1.4` | Canal estable oficial; soporta AGP hasta 9.4. [Studio](https://developer.android.com/studio/releases) |
| AGP elegido | `9.3.2` | Rama con parche concreto de lint/JDK 17 y API 37; 9.4.0 ya existe, pero no hace falta adoptarlo. [AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes) |
| Gradle Wrapper | `9.5.0`, distribución `bin` | Versión requerida por AGP 9.3; coincide con rango plenamente compatible de Kotlin 2.4. [Kotlin 2.4](https://kotlinlang.org/docs/whatsnew24.html) |
| JDK para Gradle / bytecode | Java `17` / JVM `17` | AGP exige JDK 17. Pin funcional del major; registrar proveedor y patch reales en evidencia, no inventar una revisión de Arch. |
| Kotlin / plugin Compose compiler | `2.4.10` ambos | Estable publicado; no adoptar 2.4.20 EAP. [Releases Kotlin](https://kotlinlang.org/docs/releases.html) |
| Compose BOM | `2026.08.00` estable | Gestiona módulos Compose; no gestiona Kotlin, Activity o DataStore. [BOM](https://developer.android.com/develop/ui/compose/bom) |
| Activity Compose | `1.13.0` | [AndroidX estables](https://developer.android.com/jetpack/androidx/versions) |
| Lifecycle runtime/viewmodel Compose | `2.11.0` | Misma fuente AndroidX. |
| Core KTX | `1.19.0` | Misma fuente AndroidX. |
| Preferences DataStore | `1.2.1` | Misma fuente AndroidX. |
| Coroutines Android / test | `1.11.0` | [Release oficial](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0) |
| JUnit JVM | `4.13.2` | Pin de infraestructura de test simple; no exige migración a JUnit 5. |
| compileSdk / targetSdk | `37` / `37` | Android 17; sin SDK preview. [Setup oficial](https://developer.android.com/about/versions/17/setup-sdk) |
| minSdk | `29` | Decisión: Android 10+, RoleManager disponible, alcance móvil moderno. |
| SDK Build Tools | `36.0.0` | Default/mínimo documentado de AGP elegido; no tiene que igualar compileSdk. |
| SDK Platform Tools | `37.0.1` | Revisión documentada julio 2026. [Platform Tools](https://developer.android.com/tools/releases/platform-tools) |

**Compatibilidad verificada documentalmente, no compilada aquí.** Kotlin integrado en AGP 9: no aplicar `org.jetbrains.kotlin.android`, no `kapt`, no `android.builtInKotlin=false`. Seleccionar explícitamente KGP en classpath y el compiler plugin a la misma versión; no asumir que el Kotlin embebido de Gradle es el compilador de la app. [Migración](https://developer.android.com/build/migrate-to-built-in-kotlin), [selección de KGP](https://developer.android.com/build/releases/agp-9-0-0-release-notes), [Compose compiler](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler).

No override manual de R8/D8: usar los de AGP. La compatibilidad con bytecode Kotlin se comprueba con la [tabla oficial de Kotlin/AGP/R8](https://developer.android.com/build/kotlin-support) y con `assembleRelease` en S4. No copiar una versión de compiler extension de Kotlin 1.x.

### 2.2 Arch / CLI

Equipo objetivo: Ryzen 5 5600G, **16 GB RAM**, teléfono USB; no instalar Emulator, imágenes de sistema, NDK o CMake. Instalar Studio oficial Quail 4 para Linux y en SDK Manager: Android SDK Platform 37, Build Tools 36.0.0, Platform Tools y Command-line Tools. Instalar un JDK 17 local; seleccionar la misma ruta en Studio → Gradle JDK y en `JAVA_HOME` de la terminal. El runtime del IDE puede ser distinto del JDK del build.

Con SDK instalado en la ruta habitual:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager --licenses
sdkmanager 'platforms;android-37' 'build-tools;36.0.0' 'platform-tools'
java -version
adb version
adb devices -l
```

`sdkmanager platform-tools` instala la revisión disponible: anotar si supera 37.0.1; no altera el APK. `latest` en la ruta de command-line tools es una ruta de instalación, no un selector de dependencias de la app. `local.properties` contiene `sdk.dir` absoluto local y se ignora en Git. Si el teléfono dice `unauthorized`, desbloquearlo y aceptar su clave RSA; si no hay dispositivo, comprobar cable de datos y permisos USB/udev antes de reinstalar toolchain.

### 2.3 Archivos Gradle normativos

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "rice-mobile"
include(":app")
```

`build.gradle.kts` raíz:

```kotlin
buildscript {
    repositories { google(); mavenCentral() }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}
plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
```

`app/build.gradle.kts` (DataStore se añade al llegar a S2):

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "dev.cesarmanzocode.ricemobile"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "dev.cesarmanzocode.ricemobile"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro")
        }
    }
    lint { abortOnError = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Desde Sprint 2:
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
dependencyLocking { lockAllConfigurations() }
```

Sin version catalog adicional: para este tamaño, versiones explícitas en esos dos archivos. El bloque release usa DSL legado aún soportado por AGP 9.3; no mezclarlo con el DSL nuevo de optimización. `proguard-rules.pro` puede estar vacío salvo comentarios: no añadir `-keep class **` para esconder problemas.

`gradle.properties`: `org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8`, `org.gradle.workers.max=4`, `android.useAndroidX=true`, `org.gradle.caching=true`. No activar Jetifier. No codificar `org.gradle.java.home` personal en Git.

Versionar `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` y `.properties`; `gradlew` ejecutable. Distribución `https://services.gradle.org/distributions/gradle-9.5.0-bin.zip`. Generar wrapper con Studio o Gradle oficial temporal 9.5.0, no exigir Gradle global de Arch. Incluir `distributionSha256Sum` tomado del checksum oficial de esa distribución; no inventarlo. [Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

En S1 resolver y versionar lockfiles con `./gradlew :app:dependencies --write-locks`; actualizar sólo al añadir DataStore en S2 y dependencias justificadas. No versiones `+`, SNAPSHOT ni repositorios locales. Reproducible significa checkout + SDK/JDK fijados → build CLI consistente; no se promete igualdad bit a bit de APK firmados en hosts distintos.

## 3. ANDROID PLATFORM CONTRACT

### 3.1 Manifest y Activity

Manifest final mínimo; `SET_WALLPAPER` entra en S2. El filtro de apertura normal separado permite abrir la app antes de elegir Home. El catálogo excluye ambos application IDs propios (`.debug` y release).

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.SET_WALLPAPER" />
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>
    <application
        android:name=".RiceApplication"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.RiceMobile"
        android:supportsRtl="true"
        android:enableOnBackInvokedCallback="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

No permiso `BIND_HOME`, `QUERY_ALL_PACKAGES`, `ACCESS_HIDDEN_PROFILES`, `LOCK_APPS`, almacenamiento, Internet, `RECEIVE_BOOT_COMPLETED` ni foreground service. No `configChanges` masivo, no `noHistory`, no `finish()` al abrir otra app, no task affinity inventada. `singleTask` es una decisión para reutilizar la Activity Home, no autorización para manipular tareas ajenas. [Tareas](https://developer.android.com/guide/components/activities/tasks-and-back-stack).

Tema XML: padre `android:style/Theme.Material.Light.NoActionBar`; `windowActionModeOverlay=true`, `windowLightStatusBar=false`, `windowLightNavigationBar=false`, `windowBackground=#0A0A0A`. Sin window translúcida ni lectura del wallpaper ajeno. Splash del sistema breve; no mantenerlo esperando iconos/catálogo. Desde S2, mostrar una superficie neutra hasta leer preferencias, evitando un frame del rice equivocado.

### 3.2 Home role

`RoleManager` y `ROLE_HOME` existen desde API 29. El usuario concede el rol mediante UI del sistema; no se asigna por manifest. [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager).

```kotlin
val roleManager = getSystemService(RoleManager::class.java)
val requestHome = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { refreshHomeRole() }

fun requestDefaultHome() {
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            requestHome.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
    } else {
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }
}
```

Registrar launcher de Activity Result incondicionalmente antes de STARTED. Botón “Usar como inicio” visible si no tiene rol; no abrir solicitudes al arrancar ni en bucle tras denegación. Capturar `ActivityNotFoundException` en fallback y abrir `Settings.ACTION_SETTINGS`. Reevaluar `isRoleHeld` en `onResume`; no confiar sólo en resultCode. Si pierde rol sigue funcionando como app normal.

### 3.3 Catálogo: perfiles, visibilidad y actividades sintéticas

**HECHOS:** `LauncherApps.getProfiles()` informa perfiles accesibles; `getActivityList(null, user)` enumera actividades, pero puede incluir entradas sintéticas hacia Ajustes. `startMainActivity` lanza un componente para su usuario. No confundir esto con enumerar todos los paquetes instalados. [LauncherApps](https://developer.android.com/reference/android/content/pm/LauncherApps).

**DECISIÓN:** consultar perfiles accesibles, seleccionar únicamente `Process.myUserHandle()`, y obtener su serial con `UserManager.getSerialNumberForUser`. Si no aparece o devuelve serial negativo, reportar error recuperable; no inventar serial 0. No recorrer perfiles adicionales. El serial persistido evita mezclar futuras identidades y no es el UID del paquete. [UserManager](https://developer.android.com/reference/android/os/UserManager).

Filtrar entradas sintéticas mediante intersección de componentes con una consulta real `MAIN + LAUNCHER` del `PackageManager` del usuario actual. Declarar `<queries>` anterior para que esa consulta tenga visibilidad; no usar flags de actividades deshabilitadas. [Visibilidad](https://developer.android.com/training/package-visibility/declaring).

```kotlin
// Ejecutar en Dispatchers.IO. Se omiten transformación y orden por brevedad.
val self = Process.myUserHandle()
check(self in launcherApps.profiles)
val serial = userManager.getSerialNumberForUser(self)
check(serial >= 0)
val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
val resolves = if (Build.VERSION.SDK_INT >= 33) {
    packageManager.queryIntentActivities(main, PackageManager.ResolveInfoFlags.of(0L))
} else {
    @Suppress("DEPRECATION")
    packageManager.queryIntentActivities(main, 0)
}
val actualComponents = resolves.mapNotNull { info ->
    info.activityInfo?.let { ComponentName(it.packageName, it.name) }
}.toSet()
val activities = launcherApps.getActivityList(null, self)
    .filter { it.componentName in actualComponents }
    .filterNot { it.componentName.packageName in ownApplicationIds }
    .distinctBy { it.componentName }
```

Conservar distintas activities/aliases de una app si el sistema las anuncia: no deduplicar por package. Resolver label en IO, fallback al nombre de paquete si queda vacío. Error de un label/icono no aborta el snapshot. No persistir el catálogo; regenerarlo tras inicio. No añadir inventario de APKs ni escaneo de disco.

### 3.4 Callbacks y reconciliación

`AppsRepository` registra una sola `LauncherApps.Callback` antes del primer refresh y la desregistra al cerrar. Activity invoca `start()` idempotente en `onStart`, `stop()` en `onStop`; al volver se hace refresh completo. No depende de recibir eventos mientras el proceso está muerto. Los callbacks sólo invalidan cache y encolan refresh; nunca enumeran dentro de main. [Callback](https://developer.android.com/reference/android/content/pm/LauncherApps.Callback).

```kotlin
private val callback = object : LauncherApps.Callback() {
    override fun onPackageAdded(p: String, u: UserHandle) = changed(p, u)
    override fun onPackageRemoved(p: String, u: UserHandle) = changed(p, u)
    override fun onPackageChanged(p: String, u: UserHandle) = changed(p, u)
    override fun onPackagesAvailable(p: Array<out String>, u: UserHandle, replacing: Boolean) = changedAll(p, u)
    override fun onPackagesUnavailable(p: Array<out String>, u: UserHandle, replacing: Boolean) = changedAll(p, u)
    override fun onPackagesSuspended(p: Array<out String>, u: UserHandle) = changedAll(p, u)
    override fun onPackagesUnsuspended(p: Array<out String>, u: UserHandle) = changedAll(p, u)
}
// En start():
launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
requestRefresh()
// En stop():
launcherApps.unregisterCallback(callback)
```

`changed*` ignora usuarios ajenos; invalida iconos de paquetes afectados. Cola `Channel<Unit>(Channel.CONFLATED)` + un consumidor: snapshot en IO, orden/búsqueda en Default, publicación atómica del resultado. Refrescos seriales, sin que un resultado viejo sobrescriba uno nuevo; eventos durante refresh disparan el siguiente. No debounce que posponga indefinidamente la primera lista. En fallo conservar último snapshot y mostrar “No se pudo actualizar · Reintentar”; en primer fallo mantener Home navegable.

### 3.5 Apertura y lifecycle

Lanzar sólo como consecuencia inmediata de toque/acción IME, mientras Activity está RESUMED. `AppLauncher` posee servicios de plataforma, no ViewModel. Resolver el UserHandle desde el serial al usarlo y comprobar que sigue siendo el actual. `startMainActivity(component,user,null,null)`; no `getLaunchIntentForPackage` que pierda alias/usuario. No retrasar apertura por una animación.

```kotlin
fun launch(key: AppKey): LaunchResult {
    val user = userManager.getUserForSerialNumber(key.userSerial)
        ?: return LaunchResult.Unavailable
    if (user != Process.myUserHandle()) return LaunchResult.Unavailable
    val component = ComponentName.unflattenFromString(key.component)
        ?: return LaunchResult.Unavailable
    return try {
        launcherApps.startMainActivity(component, user, null, null)
        LaunchResult.Started
    } catch (_: ActivityNotFoundException) {
        LaunchResult.Unavailable
    } catch (_: SecurityException) {
        LaunchResult.Denied
    } catch (_: IllegalStateException) {
        LaunchResult.Unavailable
    }
}
```

Al éxito, VM deja `screen=Home`, query vacía y menú cerrado; al fallo se conserva drawer/query, muestra aviso y solicita refresh. Bloquear doble toque mientras se despacha; liberar al retorno a RESUMED. No guardar un “launch pendiente” ni reejecutarlo desde un Flow tras rotación.

`onCreate`: cablear container/VM; estado transitorio nuevo parte en Home. `onNewIntent`: `super`, `setIntent(intent)`; si `ACTION_MAIN` y `CATEGORY_HOME`, llamar `resetToHome()` incluso si se repite. `onStart`: catálogo y reloj activos. `onResume`: rol, formato horario y movimiento reducido. `onStop`: parar receptores/tick y observación de paquetes. Activity Result que vuelve desde diálogo de rol no debe resetear toda la navegación sin un Home intent.

VM sobrevive a recreación de configuración. Tras muerte real se restauran sólo preferencias: Home, sin IME ni menú, siempre. No guardar Bitmaps, intents o catálogo en Bundle. Reloj con `java.time`, zona/locale actuales y `DateFormat.is24HourFormat`; actualizar al entrar y al minuto, cambios de fecha/hora/zona mientras visible; nunca un service/tick por segundo. [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel), [estado Compose](https://developer.android.com/develop/ui/compose/state).

### 3.6 Android 15 / 16 / 17

| Plataforma | Hecho material | Acción V1 |
|---|---|---|
| Android 15 / API 35 | Edge-to-edge obligatorio para target 35+; Private Space requiere rol y permiso específico. | Insets explícitos. No declarar acceso a perfiles ocultos ni presentar apps privadas. [Cambios target 15](https://developer.android.com/about/versions/15/behavior-changes-15), [Private Space](https://developer.android.com/about/versions/15/behavior-changes-all) |
| Android 16 / API 36 | No basarse en opt-out de edge-to-edge; transición a predictive back; restricciones de orientación en pantallas grandes. | APIs AndroidX de back; no overrides `onBackPressed`/KEYCODE_BACK; layout no debe crashear al cambiar tamaño. [Cambios 16](https://developer.android.com/about/versions/16/behavior-changes-16) |
| Android 17 / API 37 | Límites de memoria por dispositivo; MessageQueue sin locks para target 37; cambios de recreación de Activity. | Cache acotada, sin reflexión en internals, estado independiente de recreación. [Todas las apps](https://developer.android.com/about/versions/17/behavior-changes-all), [target 17](https://developer.android.com/about/versions/17/behavior-changes-17), [recreación](https://developer.android.com/blog/posts/android-17-is-here) |

El límite de imágenes de RemoteViews de Android 17 no afecta directamente a esta V1: no hospeda widgets. No introducir JNI ni binarios nativos; los cambios de páginas de memoria no justifican añadir NDK. Ninguna API nueva obliga a incorporar app locking o Private Space. Ser launcher de terceros no da control de Recents ni garantiza animaciones idénticas a Pixel Launcher: probar regreso por Home y gesture navigation del OEM sin usar APIs privadas.

Reboot: el sistema conserva su elección de Home según sus reglas; reconstruir al ser iniciado después del desbloqueo. No se requiere boot receiver para “mantener vivo” el launcher. No declarar Direct Boot support: disponibilidad antes del primer desbloqueo no es parte de V1.

### 3.7 Edge-to-edge y system bars

```kotlin
// Activity.onCreate antes de setContent:
enableEdgeToEdge()
// En el host, al cambiar rice o volver a la ventana:
WindowCompat.getInsetsController(window, window.decorView).apply {
    isAppearanceLightStatusBars = riceUsesLightBackground
    isAppearanceLightNavigationBars = riceUsesLightBackground
}
```

Wallpaper ocupa toda la ventana. Cada rice recibe el tamaño completo y aplica `WindowInsets.safeDrawing` a contenido interactivo, incluyendo cutouts. Drawer aplica `imePadding()` a su estructura para mantener búsqueda sobre teclado, consumiendo insets una sola vez. No envolver todo con padding y volver a añadirlo en cada celda. Scrim local oscuro/claro detrás de barras si el wallpaper compromete legibilidad; mantener contraste de navegación de tres botones por defecto. No ocultar barras ni usar immersive mode. [Insets Compose](https://developer.android.com/develop/ui/compose/system/insets-ui), [edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge).

## 4. REPOSITORY STRUCTURE

Un módulo `:app`; paquete base `dev.cesarmanzocode.ricemobile`. Usar ruta Kotlin convencional `app/src/main/kotlin/dev/cesarmanzocode/ricemobile/`. No mover archivos existentes sólo por estética si ya usan `java/` de forma compatible.

| Archivo/ruta relativa al paquete | Responsabilidad única | Sprint |
|---|---|---|
| `RiceApplication.kt`, `AppContainer.kt` | Instancias con applicationContext, scope de aplicación, repositorio/cache/servicios; DI manual. | 1; extender 2 |
| `MainActivity.kt` | Home intents, role request, lifecycle, apertura inmediata de apps, ventana, host Compose. | 1 |
| `apps/AppEntry.kt` | Identidad y metadatos sin Bitmap/Drawable. | 1 |
| `apps/AppsRepository.kt` | Snapshot y refresh del usuario actual; callbacks. | 1 |
| `apps/AppLauncher.kt` | `startMainActivity` y resultados tipados. | 1 |
| `apps/AppSearch.kt` | Normalización, orden y filtrado puro. | 1 |
| `apps/IconLoader.kt` | Decode/raster/cache IO; invalidación. | 1; tratamientos 3 |
| `launcher/LauncherState.kt`, `LauncherViewModel.kt` | Catálogo/query/Home/Drawer en S1; estado completo y acciones en S2. | 1→2 |
| `launcher/LauncherHost.kt` | Selección de rice, rutas, back, efectos Activity; no diseño de Home. | 1→2 |
| `launcher/ClockProvider.kt` | Minutos, locale, zona, 12/24 h; sólo visible. | 3 |
| `preferences/LauncherPreferences.kt` | Modelo de preferencias y repositorio `PreferencesRepository` con DataStore singleton/transacciones. | 2 |
| `preferences/FavoriteCodec.kt`, `FavoriteRules.kt` | Formato ordenado y límite, recuperación de payload. | 2 |
| `wallpaper/WallpaperController.kt` | Aplicación serial del wallpaper, marker/reintento. | 2 |
| `wallpaper/WallpaperSpec.kt` | Asset ID/revisión y color de fallback. | 2 |
| `rice/Rice.kt`, `RiceId.kt`, `RiceRegistry.kt` | Contrato, IDs y objetos sin estado. | 2 |
| `rice/RicePicker.kt` | Cinco miniaturas estructurales; elegir explícitamente. | 2 |
| `rice/{monochrome,arctic,ember,ivory,violet}/*Rice.kt` | Implementación del contrato y tokens privados. | 2→3 |
| `rice/{...}/*Home.kt`, `*Drawer.kt` | Layout específico, geometría libre. | 1/2→3 |
| `ui/shared/AppIcon.kt`, `SearchField.kt`, `AppActionsMenu.kt` | Primitivas accesibles; no grid/dock universal obligatorio. | 1→3 |
| `ui/shared/Clock.kt`, `EmptyState.kt`, `HomeGestureSurface.kt` | Componentes pequeños y gestos de fondo. | 1→3 |

Fuera del paquete: `app/src/main/AndroidManifest.xml`, `res/values/{strings,themes}.xml`, `res/mipmap-*` para icono propio; `assets/wallpapers/{id}.webp` desde S2; `res/font/` sólo si se añaden fuentes locales justificadas; `app/src/test/kotlin/...` para lógica; `docs/evidence/sprint-N.md`, `docs/screenshots/`, `README.md`, este contrato y archivos Gradle de §2.

**NO crear:** módulos domain/data/core/design-system; interfaces de repositorio sin consumidor alternativo real; use-case por botón; Hilt, Room, Navigation framework para tres estados, DI genérica, plugins de rice descargables, service, backend, downloader de imágenes, WebView. La interfaz `Rice` sí tiene cinco implementaciones reales. Interfaces mínimas para fake de tests sólo donde eviten dependencias de Android, sin jerarquías ceremoniales.

## 5. DATA MODEL

```kotlin
data class AppKey(val userSerial: Long, val component: String)
// component usa ComponentName.flattenToString(), no flattenToShortString variable.
data class AppEntry(
    val key: AppKey,
    val packageName: String,
    val label: String,
    val normalizedLabel: String,
    val normalizedPackage: String,
    val iconRevision: Long,
    val available: Boolean = true,
)
enum class RiceId(val persisted: String) {
    Monochrome("monochrome"), ArcticGlass("arctic_glass"),
    EmberForge("ember_forge"), IvoryPaper("ivory_paper"),
    VioletNight("violet_night")
}
enum class LauncherScreen { Home, Drawer, RicePicker }
data class LauncherPreferences(
    val rice: RiceId = RiceId.Monochrome,
    val favorites: List<AppKey> = emptyList(),
    val appliedWallpaper: String? = null, // id:assetRevision
)
sealed interface CatalogStatus {
    data object Loading : CatalogStatus
    data object Ready : CatalogStatus
    data class Failed(val recoverable: Boolean = true) : CatalogStatus
}
data class LauncherState(
    val preferencesReady: Boolean = false,
    val rice: RiceId = RiceId.Monochrome,
    val screen: LauncherScreen = LauncherScreen.Home,
    val catalogStatus: CatalogStatus = CatalogStatus.Loading,
    val apps: List<AppEntry> = emptyList(),
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
    val favoriteKeys: List<AppKey> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val appMenu: AppKey? = null,
    val isDefaultHome: Boolean = false,
    val preferencesWritable: Boolean = true,
    val wallpaperStatus: WallpaperStatus = WallpaperStatus.Idle,
    val message: UiMessage? = null,
)
```

`WallpaperStatus` = Idle/Applying/Failed; `UiMessage` es `id` + tipo de texto localizado, no Exception. `LaunchResult` = Started/Unavailable/Denied. Estos tipos se definen en sus archivos propietarios, no en un paquete universal de resultados. Listas tratadas como snapshots inmutables; nunca modificar una lista publicada. No añadir `@Immutable` sobre objetos mutables para silenciar recomposición.

### 5.1 Orden y búsqueda

Orden general: `Collator` del locale actual sobre label, desempate por `component`, luego `userSerial`. Normalización para búsqueda: NFD, eliminar marcas Unicode `\p{M}+`, lowercase `Locale.ROOT`, trim y colapsar whitespace. Preservar label original para pintar. Buscar todos los tokens como substrings del label normalizado o package normalizado; query vacía devuelve orden general. Sin fuzzy, servidor, categorías, historial ni índices de base de datos.

Precalcular campos normalizados al snapshot. Query limita entrada a 200 caracteres, no la lista de resultados. Filtrado en `Dispatchers.Default`, sin debounce perceptible; publicar sólo resultado de query y snapshot vigentes (`mapLatest` y cancelación cooperativa cada 64 elementos). Al cambiar query, scroll al inicio; nunca mostrar resultados viejos como si correspondieran al texto nuevo.

### 5.2 Favoritos e instalación/desinstalación

**DECISIÓN:** conservar identidades favoritas temporalmente no disponibles. `favoriteKeys` persiste 0–5 claves; `favorites` contiene las que aparecen en snapshot. Un favorito ausente se muestra como slot “No disponible”, con nombre de paquete derivado del componente y acción quitar, sin toque de lanzamiento. El menú debe permitir liberar ese slot. Así una desinstalación no deja un favorito invisible que consume capacidad.

| Evento | Resultado |
|---|---|
| Update con desaparecido transitorio | Mantener favorito; regresar cuando mismo componente esté disponible. |
| Uninstall | Conservar slot no disponible; quitarlo sólo por acción del usuario. |
| Reinstall mismo componente + usuario | Recuperar slot y orden automáticamente. |
| Reinstall cambia activity/alias | Antiguo slot no disponible; no migrar arbitrariamente al primer componente del paquete. |
| Deshabilitada/suspendida | No borrar favoritos; indicar no disponible o error de apertura y refrescar. |
| Catálogo vacío/loading/error | Nunca limpiar favoritos como efecto lateral. |
| Duplicado en payload | Conservar primera aparición. |
| Añadir sexto | Sin cambios; “Máximo 5 favoritos. Quita uno primero.” |

No se cuenta la app por package: dos aliases distintos pueden ocupar dos slots. No favoritos preseleccionados por heurísticas de marca/OEM. Home con cero muestra invitación “Mantén pulsada una app para fijarla”.

## 6. RICE ENGINE CONTRACT

### 6.1 Interfaz

```kotlin
interface Rice {
    val id: RiceId
    val wallpaper: WallpaperSpec
    val motion: RiceMotion
    val lightSystemBars: Boolean

    @Composable
    fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier)

    @Composable
    fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier)
}
data class FavoriteSlot(val key: AppKey, val app: AppEntry?)
data class HomeModel(val favorites: List<FavoriteSlot>, val isDefaultHome: Boolean)
data class DrawerModel(
    val query: String, val results: List<AppEntry>,
    val favoriteKeys: Set<AppKey>, val status: CatalogStatus,
)
data class RiceActions(
    val openDrawer: () -> Unit,
    val openPicker: () -> Unit,
    val goHome: () -> Unit,
    val updateQuery: (String) -> Unit,
    val openApp: (AppKey) -> Unit,
    val showAppMenu: (AppKey) -> Unit,
    val requestHomeRole: () -> Unit,
    val retryCatalog: () -> Unit,
)
```

`RiceMotion` contiene enter/exit duración, easing, desplazamiento relativo y escala mínima; §10 fija valores. `WallpaperSpec` contiene asset path, revisión, fallback color. `RiceRegistry` es lista fija de cinco objetos, orden Monochrome → Arctic → Ember → Ivory → Violet; `require` IDs únicos. Parse de preferencia desconocida vuelve a Monochrome; no usar `enum.ordinal` ni reflexión.

Host recibe state con `collectAsStateWithLifecycle()`, obtiene rice del registry y renderiza sólo el actual. `HomeModel` no incluye reloj que invalide todo cada minuto: cada Home incorpora `Clock` con Flow visible. Construir callbacks estables con `remember` sobre VM/Activity y modelos sólo cuando cambien sus inputs. `Rice` no recibe VM ni DataStore ni Activity.

### 6.2 Propiedad de cada capa

- **Compartido:** catálogo, búsqueda, favoritos, permisos/rol, identidad, apertura, persistencia, cache de iconos, wallpaper controller, navegación lógica, semántica de menús, reloj y textos.
- **Rice:** jerarquía, colocación de reloj/favoritos, estructura completa de drawer, celda, densidad, tokens, icon treatment, indicadores, motion interno y especificación de transición.
- **Host:** transición entre rutas, overlay global de menú/mensajes, picker, ventana e insets disponibles. No envolver cada rice en un Scaffold que imponga barra/dock/card central idénticos.

Material3 se usa para semántica/control de texto/menús cuando conviene, no para imponer el mismo layout. Primitivas compartidas aceptan Modifier y estilo; ningún `BaseRiceHome` con cincuenta flags. Duplicar una columna de composición sencilla entre rices es preferible a un motor geométrico.

### 6.3 Fuente única y Flows

S1 tiene VM mínimo para catálogo/query/ruta; S2 lo amplía en el mismo archivo. No introducir y desechar un segundo sistema de estado.

```kotlin
// Esquema: cada Flow tiene un único propietario.
val uiState: StateFlow<LauncherState> = combine(
    repository.catalog, preferencesFlow, transientState, searchResults
) { catalog, prefs, transient, results ->
    reduceToLauncherState(catalog, prefs, transient, results)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherState())
```

`transientState` contiene ruta/query/menú/mensaje y estados operativos; preferencias persistidas proceden exclusivamente de DataStore. `searchResults` porta revisión de catálogo y query; reducer descarta resultados desfasados. No volver a derivar filtro por separado dentro de cada rice. `favorites` se deriva del orden de keys y mapa de catálogo.

Acciones de favoritos/rice ejecutan transacciones; no mantener una copia optimista permanente que diverja. Confirmar cambio visible al commit de DataStore; el commit es pequeño y asíncrono. Estado de preferencias no listo no puede emitirse como preferencias vacías ya validadas.

## 7. NAVIGATION / INTERACTION CONTRACT

Navegación con enum, sin back stack de librería. Picker siempre abre desde Home; si entra por un acceso del drawer, hacer Home → Picker y limpiar query. Selector es pantalla/overlay de cinco opciones, no actividad adicional. Menú de app es overlay sobre pantalla vigente.

| Entrada | Acción exacta |
|---|---|
| Swipe up en fondo libre de Home | Abre Drawer una vez; query vacía; IME cerrado. |
| Botón Apps | Misma acción; siempre accesible. |
| Tap campo de búsqueda | Focus y teclado; IME Search abre sólo si hay exactamente un resultado disponible. |
| Long press app / favorito | Menú con “Añadir a favoritos” o “Quitar de favoritos”. No toggle silencioso. |
| Long press espacio vacío | Picker; nunca se activa detrás de app/celda/scroll. |
| Botón Rice | Picker; alternativa accesible al gesto. |
| Tap opción rice | Persistir, volver Home, aplicar wallpaper asíncronamente. |
| Back con menú | Cierra menú. |
| Back con IME | Cierra IME, conserva query y Drawer. |
| Back en Picker | Home, sin cambiar rice si no se eligió. |
| Back en Drawer sin IME | Home, limpiar query/focus/scroll. |
| Back en Home siendo default | No-op; nunca salir a Home anterior. |
| Back en Home sin rol (apertura normal) | Dejar comportamiento normal del sistema. |
| Intent Home desde cualquier estado | Home, limpiar todo transitorio inmediatamente. |

### Gestos y back

Fondo libre con `draggable(Orientation.Vertical)` y detector de long press separado. Abrir al soltar si desplazamiento hacia arriba >=64 dp o si >=24 dp y velocidad hacia arriba >=900 dp/s; ignorar swipe horizontal y cancelar si hijo consumió. Hacer DPI conversion una vez. No captura global sobre LazyColumn/Grid. Dejar franja de navegación/insets al sistema. `combinedClickable` en celdas para tap/long press y semántica, no dos detectores bloqueantes en una misma coroutine. [Gestos Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures).

S1 usa `BackHandler` para salida lógica. S3 añade `PredictiveBackHandler` para Drawer/Picker: recopila progreso para alpha/translation de la superficie; sólo al completar invoca Home, y ante cancelación restaura estado visual. No ejecutar transición de salida completa de nuevo después del gesto predictivo. En Home default, BackHandler no-op; no interceptar back del IME. Los handlers se declaran incondicionalmente con `enabled` calculado. Menú por encima de pantalla. [Predictive back Compose](https://developer.android.com/develop/ui/compose/system/predictive-back).

Nunca prometer controlar la animación de regreso desde otra app: la realiza Android/OEM. No usar overlays, gestos de accesibilidad ni reflexión para sustituirla.

## 8. WALLPAPER CONTRACT

**DECISIÓN:** cada rice contiene imagen propia empaquetada. Home pinta ese mismo asset como fondo interno y, al seleccionar rice, lo aplica al wallpaper de sistema. Así glass y composición no requieren leer wallpapers privados del usuario. No usar `WallpaperManager.getDrawable()`/archivos del wallpaper actual; Android restringe esa lectura. Para escritura existe `SET_WALLPAPER`, flags y comprobación de políticas. [WallpaperManager](https://developer.android.com/reference/android/app/WallpaperManager).

V1 sólo escribe `FLAG_SYSTEM`; no `FLAG_LOCK`, no modificar lock screen explícitamente. Preservar comportamiento del sistema cuando Home/Lock compartían fondo; verificarlo en dispositivo y describirlo con precisión, sin prometer recorte idéntico entre OEMs.

```kotlin
// Dentro de controlador serial, Dispatchers.IO; cerrar stream siempre.
if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) {
    return WallpaperApplyResult.Blocked
}
return try {
    val wallpaperId = context.assets.open(spec.assetPath).use { input ->
        manager.setStream(input, null, false, WallpaperManager.FLAG_SYSTEM)
    }
    if (wallpaperId > 0) WallpaperApplyResult.Applied(wallpaperId)
    else WallpaperApplyResult.Failed
} catch (_: IOException) {
    WallpaperApplyResult.Failed
} catch (_: SecurityException) {
    WallpaperApplyResult.Blocked
}
```

### Consistencia / concurrencia

Preferencia `rice_id` manda. `wallpaper_applied` guarda `id:assetRevision` sólo tras éxito. Un worker serial en scope de aplicación consume el último rice deseado mediante canal conflated. **No** `collectLatest` suponiendo que cancela un Binder `setStream` ya iniciado. Si cambia selección durante escritura, terminarla y aplicar la más reciente; impedir writers paralelos.

Al completar, actualizar marker dentro de `DataStore.edit` sólo si el rice persistido aún corresponde a esa operación. Si el proceso muere entre commit y wallpaper, al siguiente foreground la discrepancia reintenta. Reintentar automáticamente como máximo una vez por entrada a foreground; fallo muestra “No se pudo cambiar el fondo · Reintentar”. No revertir el rice ni bloquear Home.

No escribir wallpaper en cada recomposición/minuto/return Home. Seleccionar rice ya activo no hace escritura si marker coincide; botón Reintentar fuerza la operación. Si el usuario cambia el wallpaper desde Ajustes y no cambia rice, no pelear con el sistema reescribiéndolo cada vez: el fondo interno del rice permanece deliberadamente propio.

### Assets definitivos (S3)

Cinco WebP sRGB, 1440×3200, sin texto ni logotipos, <=2 MiB cada uno. Revisiones `1`; cambiar revisión al cambiar bytes. Imagen interna center-crop; composición principal tolera recorte lateral. Miniaturas decodificadas a <=240 px de ancho. No predecodificar cinco fondos a resolución completa.

Dirección cerrada: Monochrome negro mate con veta orgánica casi invisible; Arctic neblina de hielo translúcida con luz superior derecha; Ember carbón abrasado con resplandor cobre lateral bajo; Ivory papel cálido de fibra fina, sin collage; Violet nube de índigo/violeta difusa con profundidad y centro luminoso suave fuera de texto. No wallpaper de “panel desktop”, polígonos repetidos, texto de terminal ni UI dibujada.

No asset remoto en runtime. En S3 producir imágenes con herramientas disponibles o render offline determinista de gradientes/texturas orgánicas; conservar el resultado en Git. No dejar fondos de shells como definitivos. Registrar procedencia/licencia de assets externos en README. El controlador recibe recursos terminados, no genera arte cada arranque.

## 9. ICON CONTRACT

`LauncherActivityInfo.getIcon(densityDpi)` proporciona Drawable; adaptive puede exponer capa monochrome desde API 33. `getBadgedIcon` existe, pero no se necesita badge de work profile en V1. [LauncherActivityInfo](https://developer.android.com/reference/android/content/pm/LauncherActivityInfo), [AdaptiveIconDrawable](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable).

Pipeline elegido: obtener Drawable en IO → nueva instancia/mutate local → rasterizar a tamaño solicitado con bounds preservando proporción → Bitmap/ImageBitmap cacheado → Compose. Nunca compartir un Drawable mutable entre hilos. Adaptive normal se renderiza como Drawable completo, sin cortar foreground manualmente. Legacy usa fit centrado con margen 12%; no estirar iconos rectangulares.

Para Monochrome/Ivory, si API >=33 y `monochrome != null`, tint de esa capa sobre placa legible; si falta, icono original desaturado, conservando variaciones de luminancia. No tintear todo un bitmap multicolor a una silueta maciza. Arctic/Ember/Violet mantienen marcas reconocibles en color y cambian contenedor/acento, según §18. Iconos a 40–56 dp visuales, targets >=48 dp.

Cache `LruCache<IconKey, Bitmap>` por bytes; key = AppKey + package revision + density + tamaño px + variante mono/normal. La revisión se incrementa al recibir evento del paquete; también limpiar al volver a foreground para no conservar iconos obsoletos de updates perdidos, y al cambiar configuración de densidad. Precargar sólo favoritos y viewport inicial, concurrencia máxima 2. Durante loading usar placeholder del mismo tamaño; error usa inicial del label o símbolo genérico. No bloquear apertura por icono ausente. Evicción no llama `recycle()` sobre Bitmap que la UI aún pueda dibujar.

## 10. MOTION CONTRACT

| Rice | Drawer entrada / salida | Desplazamiento | Press | Característica |
|---|---|---|---|---|
| Monochrome | 140 / 110 ms; `LinearOutSlowInEasing` | 12 dp; fade leve | escala .98, 70 ms | Corte preciso; sin rebote. |
| Arctic | 280 / 220 ms; `FastOutSlowInEasing` | 28 dp + alpha | escala .96, 100 ms | Panel elevado suave. |
| Ember | 170 / 140 ms; `FastOutLinearInEasing` en salida | 18 dp | escala .98 + cambio de borde, 70 ms | Masa compacta; sin overshoot. |
| Ivory | 200 / 160 ms; `FastOutSlowInEasing` | 8 dp + fade | cambio tinta/fondo, 90 ms | Transición editorial contenida. |
| Violet | 340 / 260 ms; `FastOutSlowInEasing` | sheet desde fracción .12 de altura | escala .96, 110 ms | Profundidad sin animación perpetua. |

Host usa `AnimatedContent` o `updateTransition` sobre ruta con specs del rice. Cambio de rice: crossfade de 180 ms con árbol entrante/saliente acotado; no conservar los cinco. Celdas de drawer no entran una por una: latencia de la última app no depende del número de apps. Animar alpha/translation/scale en `graphicsLayer`, no recalcular catálogo o blur por frame. Launch sale inmediatamente; no esperar exit animation.

**Reduced motion:** animaciones estándar respetan escala del sistema; `ValueAnimator.areAnimatorsEnabled()` decide snap cuando están deshabilitadas. Reevaluar al volver a RESUMED; no añadir toggle propio ni leer ajustes con permisos nuevos. Con motion off, acciones y estados finales idénticos; predictive back no depende de un timeout para completar. [ValueAnimator](https://developer.android.com/reference/android/animation/ValueAnimator).

Glass V1 = transparencias + borde + sombra corta + fondo propio suave/preprocesado. No `Modifier.blur` sobre todo el drawer esperando blur real del fondo: también difuminaría contenido. No RenderScript, captura continua de pantalla, backdrop blur global, shaders experimentales o loop atmosférico infinito.

## 11. PERSISTENCE CONTRACT

Una única instancia Preferences DataStore por archivo, creada fuera de composición. DataStore ofrece Flow y actualizaciones transaccionales; no añadir SharedPreferences paralelo para acelerar arranque. [DataStore](https://developer.android.com/topic/libraries/architecture/datastore).

```kotlin
val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
private val RICE = stringPreferencesKey("rice_id")
private val FAVORITES = stringPreferencesKey("favorites_v1")
private val WALLPAPER = stringPreferencesKey("wallpaper_applied")

suspend fun selectRice(id: RiceId) {
    store.edit { it[RICE] = id.persisted }
}
suspend fun toggleFavorite(key: AppKey) {
    store.edit { prefs ->
        val old = decodeFavorites(prefs[FAVORITES])
        val next = when {
            key in old -> old.filterNot { it == key }
            old.size < 5 -> old + key
            else -> old
        }
        prefs[FAVORITES] = encodeFavorites(next)
    }
}
```

El resultado “límite alcanzado” se calcula dentro de la misma transacción y se devuelve al llamador después del commit; no decidir con state viejo. Serializar acciones de preferencia del usuario en orden de llegada; dos toggles rápidos no se pierden. Capturar IOException de lectura/escritura y mostrar fallo; no atrapar CancellationException como error de disco.

**Codec exacto:** String con hasta cinco líneas `serial<TAB>component`, usando componentes completos con `/`, sin tab/newline internos. `FavoriteCodec` es Kotlin/JVM puro; validar serial >=0, un `/` separando package y class no vacíos, rechazar saltos/tab extra, deduplicar conservando primera ocurrencia, `take(5)`. No set de strings: perdería orden. Payload ausente → lista vacía; línea malformada → descartar esa línea; no crashear ni sustituir con favoritos inventados.

Unknown rice → Monochrome al decodificar; normalizar en siguiente escritura explícita. Corrupción del archivo se recupera a defaults con handler y aviso registrado; **IOException no prueba corrupción**, no borrar datos en ese caso. En error de lectura mostrar Home recuperable con defaults sólo en memoria y `preferencesWritable=false`, botón Reintentar; no escribir esos defaults sobre preferencias desconocidas. Al recuperar lectura retomar estado persistido.

DataStore usa scope de aplicación; comandos del VM arrancan en viewModelScope pero esperan commit. Toast/snackbar de éxito sólo después de commit. Muerte antes de confirmación puede perder la acción en curso; no prometer que un tap aún sin confirmar ya es durable. Backup desactivado; debug y release tienen preferencias independientes por application ID.

## 12. TEST STRATEGY

### Unit tests con señal

- `AppSearchTest`: acentos, espacios, mayúsculas, tokens múltiples, package, query vacía, labels iguales y desempate, unicode/emoji, label vacío.
- `FavoriteRulesTest`: sexto rechazado, quitar/añadir, aliases distintos, duplicados, orden, unavailable conservado, snapshot vacío no borra nada.
- `FavoriteCodecTest`: round trip, tabs inválidos, serial negativo, >5, payload parcialmente corrupto.
- `LauncherNavigationTest`: Home intent desde drawer/picker/menú; IME/back precedencia como reglas puras; fallo de launch conserva búsqueda; éxito limpia; no reproducción de launch tras recreación.
- `WallpaperSequencingTest`: fake writer bloqueable; A/B/C rápidos termina C; marcador sólo si rice vigente; muerte simulada con marker viejo → reintento; fallo no marca aplicado.
- Prueba de repositorio con fake source si se extrae frontera mínima: evento durante refresh acaba en snapshot nuevo, start/stop idempotente. No mockear cada método Android.

Usar JUnit y `kotlinx-coroutines-test.runTest`; no sleeps reales. Para reglas puras pasar claves como strings, sin `ComponentName` de android.jar en tests locales. Codec simple evita añadir JSON serializer sólo para cinco claves.

**No unit tests:** valores dp/color, getters, Compose internals, “verify called once” de cada API, cada variante de Layout, snapshots pixel-perfect frágiles. No cobertura porcentual contractual. No suite de emulador obligatoria. Instrumentación adicional sólo para un bug reproducible que no pueda verificarse con los tests anteriores + dispositivo.

### Validación física obligatoria

Home role, lifecycle real, install/uninstall/update, aperturas, IME y gesto predictivo cancelado/completado, reboot, recreación, wallpaper con flags/recorte, UI a fontScale 1.0 y 1.5, TalkBack, navegación 3 botones/gestos si disponibles, iconos adaptive/legacy y APK release firmada. Compilar debug no demuestra que R8 release sea correcto.

## 13. DEVICE VALIDATION PROCEDURE

### 13.1 Preparación y recuperación

Antes de elegir rice-mobile por primera vez, guardar el componente Home anterior; no desinstalar ni deshabilitar el launcher OEM. Comandos desde repo, teléfono desbloqueado y un único dispositivo conectado. Si hay varios, usar `adb -s SERIAL` en todos.

```bash
mkdir -p docs/evidence
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell wm size
adb shell wm density
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > docs/evidence/home-before.txt
./gradlew --version
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.cesarmanzocode.ricemobile.debug/dev.cesarmanzocode.ricemobile.MainActivity
```

En la UI tocar “Usar como inicio”, elegir rice-mobile Debug, después Home físico/gesto. No usar `set-home-activity` para sustituir la prueba del RoleManager. Abrir Ajustes Home cuando haga falta:

```bash
adb shell am start -a android.settings.HOME_SETTINGS
adb shell input keyevent KEYCODE_HOME
adb shell cmd role get-role-holders --user current android.app.role.HOME
```

Si hay crash loop, recuperar mediante Ajustes Home con adb; si no logra abrirse, poner el componente anterior guardado:

```bash
adb shell cmd package set-home-activity --user current 'PAQUETE_ANTERIOR/ACTIVITY_ANTERIOR'
adb shell input keyevent KEYCODE_HOME
```

`PAQUETE_ANTERIOR/ACTIVITY_ANTERIOR` se reemplaza por resultado real guardado, no se inventa Pixel/Samsung. Si el resultado era ResolverActivity, usar Ajustes para elegir una app disponible. No ejecutar `pm clear` como recuperación normal: destruye preferencias.

### 13.2 Logs / capturas

```bash
adb logcat -b crash -d > docs/evidence/crash.txt
adb logcat -d -v threadtime 'RiceMobile:D' 'AndroidRuntime:E' '*:S' > docs/evidence/launcher-log.txt
adb shell dumpsys meminfo dev.cesarmanzocode.ricemobile.debug > docs/evidence/meminfo-debug.txt
adb exec-out screencap -p > docs/evidence/home.png
adb shell screenrecord --time-limit 60 /sdcard/rice-sequence.mp4
adb pull /sdcard/rice-sequence.mp4 docs/evidence/rice-sequence.mp4
adb shell rm /sdcard/rice-sequence.mp4
```

En aplicación registrar tags: `RiceMobile` + operación/revisión/resultado; no volcar catálogo completo en cada evento. Guardar sólo evidencia relevante; revisar logs/capturas antes de commit por datos personales visibles. Fuente de comandos: [adb](https://developer.android.com/tools/adb).

### 13.3 Matriz de pruebas repetible

| ID | Procedimiento | Resultado exigido |
|---|---|---|
| D01 | Install → rol → Home → swipe → buscar → abrir → Home | Aparece Home limpio; app correcta. |
| D02 | Denegar rol, seguir usando; conceder; cambiar a otro Home desde Ajustes | No solicitudes en bucle ni crash; indicador actualizado. |
| D03 | Abrir Drawer, focus búsqueda, Back, Back | Primero IME fuera; después Home/query vacía. |
| D04 | Long press app, cerrar; long press vacío; cancelar picker | No launch accidental ni cambio de rice al cancelar. |
| D05 | Añadir 5, intentar 6, quitar 1, añadir 1; cambiar rice | Límite/order consistentes en los cinco. |
| D06 | Cambiar rice y favoritos; esperar confirmación; matar proceso | Preferencias persisten y arranca en Home. |
| D07 | Instalar/update/uninstall app de prueba | Catálogo e iconos actualizados; favorito ausente sigue removible; reinstall restaura identidad igual. |
| D08 | `adb reboot`, desbloquear, Home | Rice/favoritos correctos, reloj actual, sin dependencia de service. |
| D09 | 5 rices; barras/cutout/IME; navegación botones y gestos disponibles | Sin controles tapados ni gestos robados. |
| D10 | Font grande/TalkBack/motion off | Apps/picker/favoritos accesibles sin depender de long press visual. |
| D11 | Cambiar 10 veces rápidamente de rice y volver desde otra app | Termina con wallpaper del rice vigente; memoria estabiliza. |
| D12 | APK release firmada, repetir D01/D05/D06/D09 + 24–48 h | Uso diario sin defecto que fuerce volver a otro launcher. |

Muerte de proceso (debug), **no confundir con recreación**: mandar app a background con Ajustes, intentar `adb shell am kill dev.cesarmanzocode.ricemobile.debug`, verificar `adb shell pidof dev.cesarmanzocode.ricemobile.debug`. Algunos sistemas retienen el Home. Para muerte inequívoca de debug obtener PID real, usar `adb shell run-as dev.cesarmanzocode.ricemobile.debug kill -9 PID_OBTENIDO`; Home y verificar nuevo PID. `am force-stop` sirve como prueba separada de arranque forzado; no equivale a muerte por memoria y puede afectar al estado stopped. Documentar cuál se hizo.

Prueba de update con APK de prueba del mismo application ID y firma, versión superior; `adb install -r RUTA_APK_PRUEBA`. Uninstall sólo de esa app de prueba: `adb uninstall PACKAGE_PRUEBA`. No desinstalar apps personales para probar ni usar nombre de paquete supuesto.

**Formato de evidencia por sprint:** SHA commit, comandos/resultados, modelo/API/build del teléfono, modo navegación, número de apps, Dxx ejecutadas con observado, pendientes, defectos y fix. `No ejecutada` no es `PASS`. Ausencia de dispositivo no impide avanzar documentación/código, pero no permite cerrar el gate físico.

## 14. SPRINT 1 — LAUNCHER SPINE

**Objetivo:** launcher real que cumpla D01, con Monochrome provisional y build CLI.  
**Branch:** `feat/launcher-spine`.

### Preconditions

Repo accesible o inicializado con `main` documental, sin implementación previa que haya que preservar; leer AGENTS.md si existe. Toolchain §2 y adb disponibles para el gate, teléfono API >=29. Si hay código inicial Compose, adaptar ese módulo; no crear otro proyecto dentro. Si `main` no existe, inicializar README/contrato/gitignore como bootstrap documental y crear branch antes del primer cambio funcional.

### Archivos

Gradle/wrapper/gitignore, manifest/themes/strings/icono propio, Application/Container/Activity, `apps/*`, VM/state/host mínimos, `rice/monochrome/MonochromeHome.kt` y `MonochromeDrawer.kt`, `ui/shared/{AppIcon,SearchField,EmptyState,HomeGestureSurface}.kt`, `AppSearchTest.kt`, evidencia S1. **Todavía no** `Rice.kt`, registry, DataStore ni wallpaper controller.

### Orden de implementación y API

1. Fijar §2, namespace e IDs debug/release. Crear UI Compose mínima. `./gradlew assembleDebug` para aislar toolchain antes de APIs del launcher.
2. Manifest Home/Launcher separados, singleTask, Home request con Activity Result + RoleManager. Edge-to-edge §3.7.
3. AppKey/AppEntry, query real de componentes, `LauncherApps.getActivityList`, intersección y orden §3.3/§5.1. Estado Loading/Ready/Failed. Primer snapshot sin esperar iconos.
4. Callback registrada antes del refresh; cola conflated, start/stop idempotentes. Cache lazy de iconos normal, sin tematización avanzada.
5. VM mínimo: catálogo + query + Home/Drawer. Home provisional reloj puede ser texto de marca; reloj real llega S3. Drawer `LazyColumn` con label/icono y campo búsqueda inferior; retry/error/empty.
6. `AppLauncher.startMainActivity`, fallo tipado; éxito limpia transitorio. Home intents reutilizan Activity correctamente.
7. Swipe del fondo y botón Apps; Back/IME según §7. No favoritos/menú persistente aún.
8. Test búsqueda/orden y navegación mínima, build y D01–D04 aplicables; instalar una app de prueba para confirmar refresh.

### Invariantes / prohibiciones

No bloquear main, no listar por package, no permisos extra, Home repetido no apila Activity. Nada de engine completo, favoritos, DataStore, arte definitivo, blur o cinco layouts. No alterar launcher OEM. No introducir launch effects que se reproduzcan al recrear UI.

### Commits naturales sugeridos

1. `build: bootstrap pinned Android Compose toolchain`
2. `feat: register Home activity and default launcher request`
3. `feat: enumerate and observe launchable activities`
4. `feat: add searchable drawer and app launching`
5. `test: cover search and Home navigation rules`
6. `docs: record launcher spine device validation`

Cada commit coherente se pushea; no posponer respaldo a que el teléfono pase todas las pruebas.

### Comandos / pruebas

```bash
git switch main
git pull --ff-only
git switch -c feat/launcher-spine
./gradlew :app:dependencies --write-locks
./gradlew test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Después de cada unidad: `git add` de rutas propias, `git commit -m 'mensaje concreto'`, `git push -u origin feat/launcher-spine` en el primero y `git push` después. Abrir PR con la interfaz GitHub disponible; no hace falta instalar otro stack para ello. Adjuntar evidencia del gate, no generar capturas falsas.

### Definition of Done / acceptance gate

Build debug pasa; búsqueda con acentos/duplicados funciona; install callback visible; no crash al volver. **Gate literal:** instalar APK → elegir rice-mobile como Home → pulsar Home → aparece launcher → swipe up → apps → buscar una → abrirla → Home → vuelve a Home limpio. PR revisable, commits publicados y evidencia D01 real. Sin dispositivo, marcar gate pendiente y entregar el trabajo ya hecho.

### Fallos previsibles → solución concreta

| Fallo | Solución |
|---|---|
| KGP duplicado / extensión Kotlin ya existe | Quitar plugin `org.jetbrains.kotlin.android`; conservar built-in y compiler plugin según §2. |
| SDK no encontrado / JDK incorrecto | Corregir `local.properties`/JAVA_HOME y Gradle JDK; no cambiar versiones al azar. |
| Launcher no sale como candidato | Verificar actividad exported, MAIN/HOME/DEFAULT en un filtro, APK correcto instalado. |
| Catálogo vacío | Revisar query manifest y usuario actual; registrar conteos LauncherApps/intersección, no pedir QUERY_ALL_PACKAGES. |
| Aparecen entradas de Ajustes en vez de apps | Aplicar intersección con componentes reales MAIN/LAUNCHER. |
| Home vuelve al Drawer | Procesar onNewIntent/Home y limpiar ruta/query; revisar singleTask. |
| App instalada con Home en background no aparece | Refresh en cada onStart; callback no cubre proceso muerto. |
| Swipe bloquea tap de app | Limitar detector a fondo no consumido y usar combinedClickable en celdas. |

## 15. SPRINT 2 — RICE ENGINE + PERSISTENCIA

**Objetivo:** cinco estructuras intercambiables, rice/favoritos durables y wallpaper coordinado.  
**Branch:** `feat/rice-engine`.

### Preconditions / archivos

S1 integrado en main y gate registrado; misma toolchain. Crear `rice/{RiceId,Rice,RiceRegistry,RicePicker}.kt`, los cuatro paquetes nuevos y wrappers Monochrome, `preferences/*`, `wallpaper/*`, cinco assets provisionales locales; ampliar VM/state/host/container; AppActionsMenu; tests codec/favoritos/secuenciación; evidencia S2. Añadir DataStore y permiso wallpaper. No cambiar identidad de aplicación.

### Tareas en orden y API exacta

1. Implementar RiceId y codec con valores persistentes fijos; registry completo. Los cinco shells implementan `Home`/`Drawer` desde el inicio.
2. DataStore singleton, `edit`, Flow de preferencias con ready/error explícito. Favorites rules y serialización ordenada. Tests antes de conectar UI para evitar corrupción silenciosa.
3. Extender VM/LauncherState existente; `combine` y `stateIn`; construir FavoriteSlot. Catálogo vacío no purga datos.
4. RiceActions y host de rutas; Monochrome pasa al contrato. Aplicar sólo una instancia activa, no cinco composiciones ocultas.
5. Menú contextual añadir/quitar, sexto rechazado, unavailable removible. Debe funcionar en drawer y favoritos Home.
6. Picker con cinco miniaturas simples que representen geometría; selección explícita → commit → Home. Long press vacío y botón Rice accesible.
7. Wallpaper controller serial `setStream(...FLAG_SYSTEM)`, marker `id:revision`, fallback y retry. Probar selección rápida con fake y dispositivo.
8. Crear shells estructurales: Monochrome lista, Arctic dock y grid, Ember bloques y drawer de dos columnas, Ivory índice tipográfico, Violet cluster y sheet. Pueden carecer de polish, no de geometría distinta.
9. Muerte de proceso/relanzar y D05/D06/D11. Actualizar lockfile sólo por DataStore. D01 vuelve a pasar como regresión.

### Invariantes / prohibiciones

Un state persistente compartido; orden/límite transaccional; selección nunca resetea favoritos; wallpaper no bloquea Home; errores no crashean ni destruyen datos. Prohibidos Room, backend, navigation library, drag, settings generales y cinco variantes de un Scaffold idéntico.

### Commits sugeridos

1. `feat: add rice contracts and structural shells`
2. `feat: persist selected rice and ordered favorites`
3. `feat: wire rice picker and favorite actions`
4. `feat: apply bundled wallpapers with serialized recovery`
5. `test: cover persistence rules and wallpaper races`
6. `docs: record rice engine acceptance`

### Comandos / pruebas

```bash
git switch main
git pull --ff-only
git switch -c feat/rice-engine
./gradlew :app:dependencies --write-locks
./gradlew test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Push por commit y PR igual que S1. Automatizadas: límite/codec/rice unknown, secuencia wallpaper, state de ausencia temporal. Manuales: D01/D05/D06/D11; wallpaper y lock screen observados, favorito de app desinstalada removible.

### Definition of Done / gate

Elegir rice cambia estructura; elegir otro → esperar commit → matar proceso → se conserva. Añadir favorito → cambiar rice → permanece → reiniciar launcher → mismo favorito y orden. Sexto rechazado sin pérdida. Cambios rápidos acaban en rice/wallpaper vigente. DataStore sin lecturas bloqueantes ni escrituras desde composición. PR y evidencia publicados.

### Fallos previsibles → solución

| Fallo | Solución |
|---|---|
| Favoritos cambian de orden al reiniciar | Usar codec listado, no stringSet ni sorted de package. |
| Se pierden al actualizar app | No podar por snapshot; conservar unavailable conforme §5.2. |
| Sexto favorito tras taps rápidos | Validar límite dentro de `edit`, serializar comandos. |
| Wallpaper viejo termina sobre el nuevo | Un único writer; conflación de deseo, nunca writers concurrentes. |
| DataStore multiple instances | Delegate único con applicationContext; no crear por rice/Activity. |
| Flash de Monochrome antes del rice elegido | UI neutra hasta preferencesReady; no reinterpretar Loading como default guardado. |
| Crash por ID de rice viejo/desconocido | Parse total con fallback; jamás `enum.valueOf` sobre datos sin validar. |

## 16. SPRINT 3 — THE FIVE RICES

**Objetivo:** completar diseño, assets, iconos y motion de los cinco, siguiendo §18.  
**Branch:** `feat/v1-rices`.

### Preconditions / archivos

S2 integrado con persistencia física verificada. Modificar sólo archivos de rices, UI shared pertinente, icon loader, clock, assets, motion/host/picker, recursos de texto y evidencia. Modelos/reglas no se rediseñan. Añadir `ClockProvider.kt`, tipografías usando familias del sistema fijadas abajo; no descargar fuentes en runtime.

### Orden de tareas y APIs

1. Fijar tokens por rice según §18; usar medidas de ventana reales y safeDrawing; Home con 0/1/5/unavailable favoritos. Completar Monochrome primero como patrón de funcionalidad, no como base geométrica.
2. Hacer Ivory con lista editorial y Ember con bloques compactos: validar a simple vista que difieren de Monochrome.
3. Hacer Arctic con dock flotante y panel; Violet con cluster y sheet. Evitar posiciones absolutas a pixeles de un teléfono.
4. Terminar cinco wallpapers y subir assets definitivos con revisión; no cambios funcionales a controller. Picker miniaturas reflejan layout real, no cinco cuadros de colores.
5. IconLoader normal/mono y placado por rice; fallback legacy y labels largos/emoji. Reusar cache acotada.
6. ClockProvider con minuto, cambio hora/zona, 12/24; cada rice pone reloj en su posición y jerarquía.
7. Press/focus/selected; `AnimatedContent`, `graphicsLayer`, `PredictiveBackHandler`; motion off y cancelación. No animación en loop permanente.
8. Ergonomía: targets 48+, bottom search, texto escalado, lista scrollable, botón alternativo a cada gesto. TalkBack recorrido y etiquetas.
9. Grabar cinco rices en secuencia con mismos favoritos. Revisar versiones en escala de grises sin cambiar datos. Corregir el layout si no se reconoce; no intensificar sólo el color.

### Invariantes / prohibiciones

Las cinco implementaciones contienen Home y Drawer propios. Mismo conjunto/orden de apps y favoritos; ningún rice pierde búsqueda/acciones. Nada de carpetas, widgets, packs, nuevas páginas, selector de fuentes o colores. No copiar Waybar/Rofi ni simular terminal de desktop. No introducir lib visual sólo para blur.

### Commits sugeridos

`feat: complete monochrome visual composition`; `feat: complete ivory editorial rice`; `feat: complete ember industrial rice`; `feat: complete arctic glass rice`; `feat: complete violet night rice`; `feat: refine shared icons clock and motion`; `docs: capture five-rice visual acceptance`.

### Comandos / pruebas

```bash
git switch main
git pull --ff-only
git switch -c feat/v1-rices
./gradlew test
./gradlew lint
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No nuevos unit tests para colores/dp; ampliar los existentes sólo si se corrige una regla. Manual D01/D03/D04/D05/D09/D10/D11 en los cinco. Video de §13 y capturas Home/Drawer por rice; mismas condiciones, query vacía, cinco favoritos, fontScale normal; pruebas aparte con 0 y 1 favoritos y fuente grande. Evidencia contiene hallazgos/fixes, no sólo archivos PNG.

### Definition of Done / gate

Monochrome → Arctic → Ember → Ivory → Violet es inequívoco **sin color**. Las diez pantallas Home/Drawer están completas, assets definitivos, sin placeholders de diseño. Todos tienen búsqueda, menú, favoritos, reloj, accesibilidad y motion. `test`, `lint`, build pasan; evidencia visual real y commits/PR publicados.

### Fallos previsibles → solución

| Fallo | Solución |
|---|---|
| Cinco recolores | Sustituir estructura según §18; verificar silueta, posición reloj y formato de drawer. |
| Glass ilegible / lento | Mayor opacidad de superficie, fondo suave estático; retirar blur, no añadir GPU hacks. |
| Cluster de Violet se solapa con 5 favoritos | Layout determinista por filas 1–2–2 con targets medidos, no offsets arbitrarios superpuestos. |
| Reloj actualiza toda la lista | Colectar tiempo dentro de Clock, fuera de LauncherState general. |
| FontScale recorta controles | Alturas mínimas y wrapping; reducir columnas antes que texto o target. |
| Drawer salta dos veces al predictive back | Gesto controla progreso y commit; suprimir animación duplicada postcommit. |
| Fallback de icono parece bloque sólido | Desaturar luminancia, no tint uniforme del bitmap. |

## 17. SPRINT 4 — DAILY DRIVER / V1 RELEASE

**Objetivo:** robustez y publicación local V1.0.0, sin features grandes.  
**Branch:** `release/v1`.

### Preconditions / archivos

S3 integrado; cinco diseños completos. Cambios sólo para defectos/evidencia, versión/firma, README, capturas y release. No añadir paquetes arquitectónicos. Mantener `docs/evidence/sprint-4.md` como registro único de la validación y uso diario.

### Orden de ejecución / APIs

1. Ejecutar matriz D01–D11; registrar modelo/API/build, datos de catálogo y modo navegación. Priorizar crash, launch incorrecto, pérdida de estado, bloqueo de gestos.
2. Probar muerte de proceso, reboot, pérdida de rol, instalación/update/uninstall; confirmar serial/component y cache invalidation. Revisar `onNewIntent`, callback lifecycle y DataStore.
3. Probar labels extra largos, icono legacy/adaptive/fallido, no resultados, 0/5 favoritos, slot ausente, español/locale del teléfono y 12/24.
4. Medir §19 en build release perfilable o release normal con herramientas del sistema. Corregir IO en main, cache sin límite, recomposición por reloj, carga eager y readers duplicados.
5. Revisar insets, keyboard/back/cutout, TalkBack, motion off y texto grande. Ajustes pequeños de layout, no rediseño general.
6. `versionName=1.0.0`, `versionCode=1` para primer release; debug conserva suffix. Firma local de propietario fuera de repo según §20. `assembleRelease` con R8; verificar e instalar esa APK.
7. Repetir core en release; usar **esa versión** 24–48 horas. Registrar inicio/fin y cualquier defecto. Si un fix posterior toca comportamiento crítico, repetir su escenario y continuar uso con la candidata corregida; no contar horas de una versión rota como aprobación de otra.
8. README operativo, 10 capturas, SHA256 de APK, PR final con evidencia. Tras merge según workflow autorizado, tag `v1.0.0` en commit final y push del tag. No subir Play Store.

### Invariantes / prohibiciones

No afirmar 24–48 h si no transcurrieron ni medir velocidad debug como release. No usar backup/export como excusa para ampliar scope. No borrar favoritos al firmar: debug/release son apps distintas y su separación se explica. No commitear passwords, keystore, caches o APKs grandes dentro de Git. No deshabilitar lint/R8 para “pasar”; corregir causa o justificar una regla específica.

### Commits sugeridos

`fix: preserve launcher state across lifecycle transitions`; fixes concretos por bug real; `perf: bound icon and wallpaper allocations` si hay problema; `build: configure signed v1 release`; `docs: add install recovery and rice screenshots`; `chore: finalize v1.0.0 acceptance evidence`.

### Comandos / pruebas

```bash
git switch main
git pull --ff-only
git switch -c release/v1
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

Completar firma §20 antes de último comando. Verificar `app-release.apk` y probar release. Push por commit y PR. Los gates automáticos son exactamente `test`, `lint`, `assembleDebug`, más release firmada requerida para entrega. No añadir suite de benchmarking multimódulo sólo para este sprint.

### Definition of Done / acceptance gates

- Técnico: todos los comandos anteriores pasan; APK firmada verificada, R8 funcional, sin crashes reproducibles en matriz.
- Producto: 24–48 h de uso real de la candidata sin defecto que obligue abandonar rice-mobile. Registrar versión y duración observada.
- Entrega: V1.0.0, README/capturas, SHA256, artefacto local/release y recuperación documentada, commit/PR/tag publicados según alcance autorizado.

Si Sonnet termina código en una sesión menor a 24 h: entregar y publicar el trabajo ya autorizado, indicar **gate de uso diario pendiente**. No inventar temporizadores, testimonios ni un “PASS por expectativa”; no detener el resto de trabajo útil por esperar esas horas.

### Fallos previsibles → solución

| Fallo | Solución |
|---|---|
| Debug funciona, release crashea | Crash log release + mapping R8; regla keep específica sólo si reflexión real la exige. |
| INSTALL_FAILED_UPDATE_INCOMPATIBLE | Verificar application ID y certificado; debug `.debug` evita colisión. No desinstalar silenciosamente datos del usuario. |
| Primera Home tras reboot incorrecta | Verificar rol real, reinicio tras desbloqueo y lectura de prefs; no añadir boot service. |
| Memoria crece en cada rice switch | Soltar fondo anterior, limitar cache, eliminar referencias Activity/Drawable del container. |
| OEM recorta wallpaper distinto | Ajustar composición/asset safe area, mantener background interno coherente; no leer wallpaper privado. |
| 24–48 h revelan bloqueo de navegación | Fix mínimo, repetir D01/D03/D09 y uso de candidata; no añadir features. |

## 18. VISUAL CONTRACT — FIVE RICES

### 18.1 Reglas compartidas de accesibilidad y adaptación

Diseñar primero para ventana útil de 360×800 dp, comprobar 320 dp de ancho y teléfono real. No obligar aspecto fijo. Fondo full bleed; contenido respeta insets. Base de spacing 4 dp; targets mínimo 48×48 dp, labels 13–16 sp normalmente, líneas de texto con contraste mínimo 4.5:1 (3:1 para texto grande/controles). Las celdas aumentan altura con fontScale; nunca reducir target para conservar columnas. [Accesibilidad Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

No texto decorativo que parezca dato real: sin clima, uptime, notificaciones ni conteos inventados. Reloj real sin segundos; fecha local. Inglés técnico no invade UI: strings españoles, nombres propios de rices sin traducir. `start/end` en vez de left/right donde importe RTL. No todo uppercase para labels de apps; sólo títulos cortos.

Acción Apps y Rice cerca del pulgar, a 12–20 dp sobre safe bottom. Búsqueda fija al fondo del drawer, se eleva con IME. Cerrar accesible cerca de esa zona; Back funciona también. Long press tiene equivalente mediante `onLongClickLabel`/custom accessibility action y menú; unavailable también se anuncia. Si label visible ya nombra la app, el icono decorativo lleva `contentDescription=null` para no duplicar lectura.

Las familias iniciales son locales del sistema: `FontFamily.SansSerif`, `Monospace`, `Serif`, cursando pesos explícitos; no descargar Google Fonts. La composición fija el concepto; no depende de que un OEM tenga exactamente el mismo contorno tipográfico.

### 18.2 Monochrome — bloque tipográfico

| Elemento | Especificación |
|---|---|
| Paleta | Fondo `#0A0A0A`; tinta `#F5F5F0`; secundario `#A3A3A0`; borde `#454545`. |
| Home | Columna rígida a start, márgenes 24 dp. Reloj enorme en tercio superior, espacio vacío central, cinco favoritos en **filas lineales verticales** en tercio inferior. No dock ni cards. |
| Reloj/fecha | Hora SansSerif Black 80–104 sp, una línea si cabe; fecha Monospace 13 sp debajo, separada por regla horizontal 1 dp. Formato 12h puede ir AM/PM pequeño aparte. |
| Favoritos | Filas 48–56 dp, índice 01–05 monoespaciado, icono mono 28–32 dp, label 17 sp. Sin slots decorativos vacíos; con cero, instrucción breve. |
| Trigger | Franja inferior “APPS ↑” con target 48; “RICE” al extremo opuesto. Swipe del vacío. |
| Drawer | **Lista única** de filas separadas por líneas; encabezado “APPS” 32 sp y número real de resultados pequeño. Search rectangular inferior con underline gruesa. |
| Celdas | 60 dp mínimo, icono mono 36 dp, label 17 sp, favorita marcada por pequeño signo cuadrado accesible. |
| Superficies/geometría | Negro opaco, esquinas 0–2 dp, sin sombra. Espaciado 8/16/24. |
| Motion | §10: breve, seco; overscroll de lista estándar moderado, no spring decorativo en Home. |
| Wallpaper | Veta casi invisible; el contraste tipográfico manda. |
| Distinción estructural | Una columna de favoritos y drawer de lista con reglas, gran reloj pesado superior. |

### 18.3 Arctic Glass — dock suspendido

| Elemento | Especificación |
|---|---|
| Paleta | Fondo `#071B2A`; tinta `#E8FAFF`; secundario `#ACCAD5`; acento `#78DCEF`; glass azul oscuro 75–88% opaco. |
| Home | Reloj ligero centrado en mitad superior con fecha pill; amplio vacío debajo; **dock horizontal flotante** cerca del pulgar con 1–5 iconos, sobre separado del fondo por sombra corta. |
| Reloj/fecha | SansSerif Light 68–84 sp, fecha 14 sp en cápsula; no masthead ni líneas industriales. |
| Favoritos | Dock radius 28 dp, padding 12; targets 48 dp con iconos 40 dp. En 320 dp usar padding horizontal 8 para cinco targets, no scroll horizontal como requisito. Labels breves debajo dentro del dock, aumentar alto al escalar fuente. |
| Trigger | Tirador/pill “Apps” encima del dock con target 48; Rice circular discreto en fila inferior. |
| Drawer | **Panel redondeado casi completo**, márgenes 12 dp, grid nominal 4 columnas con mínimo 76 dp por celda; se reduce a 3 si falta ancho/fuente grande. Search como cápsula inferior dentro del panel. |
| Celdas | Icono a color 44 dp en placa circular translúcida; label 13 sp, hasta 2 líneas. Alto mínimo 84 dp. |
| Superficies/geometría | Radius 20–28 dp, borde blanco 1 dp a 18% opacidad, sombras 4–8 dp; capas máximas 2. Espaciado 12/20/28. |
| Motion | Suave 280 ms, dock press .96; scroll nativo, nada de blur animado. |
| Wallpaper | Niebla fría bajo superficies; área del reloj oscurecida con scrim suave. |
| Distinción estructural | Reloj centrado, favoritos en dock horizontal y drawer de grid dentro de panel flotante. |

### 18.4 Ember Forge — matriz industrial

| Elemento | Especificación |
|---|---|
| Paleta | Carbón `#171411`; superficie `#24201B`; tinta `#F1E8DC`; cobre `#D99A67`; secundario `#C0ABA0`. |
| Home | Header compacto a start con hora/fecha en dos columnas; bloque central de aire corto; **matriz de favoritos en 2 columnas**, primer favorito puede abarcar ancho completo y siguientes 2×2. Rejilla alineada a bordes. |
| Reloj/fecha | Monospace Bold 44–56 sp; fecha 12–14 sp en columna lateral, regla cobre de 3 dp. |
| Favoritos | Primer bloque 72 dp alto mínimo, restantes 64 dp; icono 36 dp + label 14–16 sp. Si 1 favorito, sólo bloque ancho; si 2–5, completar filas sin celdas falsas accionables. |
| Trigger | Barra baja “TODAS LAS APPS” rectangular, icono flecha y botón Rice cuadrado; targets 48–56 dp. |
| Drawer | **Dos columnas de filas horizontales compactas** con icono+texto; no iconos encima de labels como Arctic. Encabezado bajo y search inferior rectangular con borde cobre. |
| Celdas | Alto mínimo 64 dp, icono 32 dp, label 14 sp dos líneas; a fuente grande o ancho <340 dp colapsar a columna única. |
| Superficies/geometría | Cortes diagonales discretos 6 dp con `CutCornerShape`, no polígonos por todas partes; borde 1 dp, sin glow. Spacing 8/12/16. |
| Motion | Rápido/pesado; desplazamiento corto; pressed invierte borde/fondo. Sin rebote de bloques. |
| Wallpaper | Carbón/cobre lateral que no compite con matriz; scrim oscuro bajo contenido. |
| Distinción estructural | Header comprimido, favoritos en bloques 1+2×2 y drawer denso de dos columnas horizontales. |

### 18.5 Ivory Paper — página editorial

| Elemento | Especificación |
|---|---|
| Paleta | Papel `#F3EBDD`; tinta `#25231E`; secundario `#655F55`; regla `#AAA08D`. Barras con iconos oscuros. |
| Home | Pequeña fecha arriba como dateline; **hora serif a start a media altura**, gran aire superior/lateral; favoritos integrados en índice numerado debajo, alineado como texto editorial, no dock. |
| Reloj/fecha | Serif Normal 64–80 sp; fecha SansSerif 13 sp arriba; regla fina corta en vez de divisor full width. |
| Favoritos | Lista textual 48–56 dp por fila, número pequeño y nombre Serif 19 sp. Sin icono en Home por decisión editorial; drawer sí muestra iconos. No perder acción long press. |
| Trigger | Pie “Biblioteca de apps ↗” subrayado con target 48; Rice como “Edición” a un lado. |
| Drawer | **Índice alfabético agrupado** en LazyColumn: cabecera de letra grande en margen y filas desplazadas a start+32 dp. Grupo `#` para nombres sin inicial alfabética. Mantener orden por Collator y agrupar de forma estable. |
| Search | Pie tipo campo editorial con underline, label “Buscar apps”, fondo papel opaco al abrir IME. Con query activa usar lista de resultados simple sin grandes cabeceras de letra. |
| Celdas | 56 dp mínimo, icono desaturado 28 dp, label Serif 17 sp, regla sólo entre grupos. No separador grueso por cada app. |
| Superficies/geometría | Sin cards/sombras, esquinas 0, padding 28 dp (20 en 320 dp), spacing 12/24/40. |
| Motion | Fade/8 dp discreto, sin escala del texto; overscroll estándar sin dramatizar. |
| Wallpaper | Fibra muy sutil; manchas oscuras nunca detrás del texto. |
| Distinción estructural | Fecha primero, reloj medio, favoritos texto sin iconos y drawer con margen alfabético. |

### 18.6 Violet Night — escena y sheet

| Elemento | Especificación |
|---|---|
| Paleta | Índigo `#100C24`; violeta `#302053`; tinta `#F3EDFF`; secundario `#C3B3D9`; acento `#BC9BFF`. |
| Home | Wallpaper ocupa la mayoría del plano; reloj pequeño/medio en esquina superior end; **cluster de favoritos en mitad inferior**, con una app principal al centro y dos pares escalonados por filas; controles inferiores separados. |
| Reloj/fecha | SansSerif Medium 48–64 sp, alineado end; fecha 13 sp debajo. No llenar todo el ancho. |
| Favoritos | Layout medido 1–2–2: principal icono 56 dp, otros 44 dp; targets >=56, labels 13 sp. Filas no se superponen; pequeños desfases visuales de 8 dp conservan cajas y orden accesible. Con 1–2, centrar composición parcial; no placeholders orbitales. |
| Trigger | Pill baja “Explorar ↑”; Rice como punto/botón etiquetado, target 48. |
| Drawer | **Sheet visual** anclada al fondo de 88% de altura útil, deja wallpaper visible arriba, scrim no modal sobre resto; grid nominal 3 columnas, iconos grandes, esquinas superiores 32 dp. Implementación dentro de ruta, no ModalBottomSheet con back stack propio. |
| Search | Search pill en pie del sheet; al abrir IME, sheet puede ocupar altura útil completa. Botón cerrar 48 dp en pie. |
| Celdas | Icono a color 48–52 dp, placa circular oscura, label 14 sp hasta dos líneas; alto mínimo 92 dp. |
| Superficies/geometría | Sheet opaca 94%, sombras estáticas suaves, acento de halo **pre-renderizado/estático**, spacing 12/20/32. |
| Motion | 340 ms de sheet, alpha/translation; nada de estrellas parpadeando ni parallax continuo. |
| Wallpaper | Protagonista; zona de luz lejos del reloj y scrim bajo cluster. |
| Distinción estructural | Hora a end, cluster 1–2–2 y sheet que revela fondo superior. |

### 18.7 Comprobación estructural obligatoria

Comparar con misma hora y cinco favoritos: Monochrome debe verse como bloque/lista; Arctic como dock/panel; Ember como matriz; Ivory como página/índice; Violet como escena/cluster/sheet. En 0 favoritos deben seguir diferenciándose reloj, espacio negativo y drawer. No aprobar sólo porque las capturas usan wallpapers distintos.

## 19. PERFORMANCE BUDGET

**DECISIÓN: objetivos de ingeniería, no benchmarks medidos.** Tomar baseline del teléfono disponible; registrar hardware/API/refresco/temperatura razonable y tamaño real del catálogo. Si hardware concreto no logra un objetivo, investigar traza y documentar causa; no asumir que el Ryzen del PC es la CPU del teléfono.

| Área | Objetivo razonable | Método / límite |
|---|---|---|
| Cold start | Primera Home interactiva <=1 s mediana; catálogo usable <=1.5 s para ~300 actividades | 5 arranques fríos release, proceso ausente verificado; registrar mediana y máximo, no p95 inventado de cinco datos. |
| Warm Home | Respuesta visible <=150 ms excluyendo animación Android/OEM externa | Video/trace; no esperar refresh de catálogo para pintar snapshot previo. |
| Abrir drawer | Primer cambio visual en siguiente frame útil; preparación <=50 ms; transición <=340 ms | Cache caliente, cada rice; duración artística no equivale a input lag. |
| Search | Filtro <=20 ms para 500 entradas, resultado visible <=50 ms tras input | Test JVM para algoritmo + trace real para input/UI. Sin debounce 300 ms. |
| Catálogo | Soportar 500 actividades normalmente; prueba sintética 1,000 | Lista lazy, sin truncar catálogo; no promesa de benchmark con mil APKs reales. |
| Frames | Buscar 60 fps en pantalla 60 Hz, sin bloqueos >100 ms propios | Perfetto/System Trace; medir release; priorizar ausencia de jank visible reproducible. |
| Recomposición | Reloj sólo afecta Clock; scroll no dispara filtro; escritura query no recompone Home oculto | Layout Inspector/trace para hipótesis concreta, no cuota arbitraria de recomposiciones. |
| Cache de iconos | `min(24 MiB, maxHeap/8)`; hasta 2 loads simultáneos | Size por `allocationByteCount`; cero precarga global. |
| Memoria | Objetivo PSS estable <=150 MiB, pico <=220 MiB en teléfono típico | `dumpsys meminfo`; depende de resolución/OEM. Tras 20 cambios no crecer sostenidamente. Si heap limitado, bajar cache/bitmap primero. |
| Wallpaper | Activo máximo 1440×3200 (~17.6 MiB ARGB); transición máximo 2 activos | Decode downsample al viewport; al terminar soltar anterior, miniaturas separadas. |
| Reposo | Sin frame loop ni trabajo periódico en background | Tick minuto sólo visible; reloj no usa segundos. |
| APK | Objetivo <=25 MiB con cinco assets | Informativo, no razón para borrar diseños o accesibilidad. |

Prácticas de Compose: usar lazy keys por identidad, no trabajo pesado en composición, derivar estado sólo cuando beneficia y diferir lectura de animación a draw/layout cuando procede. No insertar `remember` ciegamente ni marcar todo estable. [Performance Compose](https://developer.android.com/develop/ui/compose/performance/bestpractices).

Medición básica release, después de recuperar estado Home y rol:

```bash
adb shell am force-stop dev.cesarmanzocode.ricemobile
adb shell am start -W -n dev.cesarmanzocode.ricemobile/dev.cesarmanzocode.ricemobile.MainActivity -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell dumpsys meminfo dev.cesarmanzocode.ricemobile
adb shell dumpsys gfxinfo dev.cesarmanzocode.ricemobile framestats
```

`am start -W` mide arranque Activity, no por sí solo catálogo listo. Añadir marcas `Trace.beginSection/endSection` para refresh, filter y primera lista usable; no mantener una traza siempre activa en producción. Si `gfxinfo` no representa bien los frames Compose en ese OEM, usar System Trace de Studio en teléfono, no concluir rendimiento perfecto por falta de datos.

## 20. RELEASE CHECKLIST V1.0.0

### 20.1 Firma y variantes

Debug: `dev.cesarmanzocode.ricemobile.debug`, label `rice-mobile Debug`, certificado debug. Release: `dev.cesarmanzocode.ricemobile`, label `rice-mobile`, certificado propio duradero. Crear overrides de string en `src/debug/res/values/strings.xml`. Preferencias separadas deliberadamente; pasar de debug a release implica seleccionar rol/rice/favoritos una vez, **no migración de datos**. Primera release `versionCode=1`; siguientes releases incrementan, no reutilizan.

Firma con keystore del propietario fuera del repo. Si no existe, crear una vez desde JDK; almacenar copia segura que permita futuras actualizaciones. No usar el certificado debug para publicar V1. [Firma Android](https://developer.android.com/studio/publish/app-signing).

```bash
mkdir -p "$HOME/.local/share/rice-mobile/keys"
keytool -genkeypair -v -keystore "$HOME/.local/share/rice-mobile/keys/release.jks" -alias rice-mobile -keyalg RSA -keysize 3072 -validity 10000
```

Comando interactivo: introducir identidad y contraseñas reales del propietario; no inventar secretos ni imprimirlos. Sonnet puede completar build/debug/documentación mientras falte firma; la APK release firmada requiere esa clave. Configurar `signingConfigs.create("release")` leyendo `RICE_KEYSTORE`, `RICE_STORE_PASSWORD`, `RICE_KEY_ALIAS`, `RICE_KEY_PASSWORD` mediante providers/environment. No escribir secretos al Gradle versionado, ni usar `echo` para registrarlos. Build debug, test y lint deben funcionar sin secretos. Crear signingConfig sólo cuando las cuatro variables existan; si se solicita empaquetar/ensamblar release sin ellas, fallar con mensaje claro antes de entregar artefactos. No hacer `require` incondicional durante configuración de Gradle, pues rompería lint/debug. Nunca degradar silenciosamente a firma debug ni presentar unsigned como entregable.

Después de build:

```bash
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

Guardar fingerprint y SHA256 públicos en evidencia; no passwords. Entregar APK por artefacto de release/local solicitado, no commit binario al árbol. No crear cuenta Play ni App Bundle como gate.

### 20.2 Checklist final

- [ ] Cuatro sprints integrados con branches/PRs/commits publicados; ningún cambio funcional perdido en scratch.
- [ ] `versionName=1.0.0`, application IDs/labels correctos.
- [ ] Gradle Wrapper 9.5.0, checksum y lockfiles en Git; checkout limpio puede compilar.
- [ ] `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug` pasan.
- [ ] `assembleRelease`, `apksigner verify` e instalación release pasan.
- [ ] Manifest fusionado sin permisos extra inesperados.
- [ ] D01–D12 registrados; lo no disponible en hardware se identifica, no se declara probado.
- [ ] 24–48 h sobre candidata real; cero defecto que fuerce abandono.
- [ ] Cinco rices inequívocos, 10 screenshots reales Home/Drawer, video secuencia.
- [ ] Default role, reboot, proceso muerto, install/update/uninstall probados.
- [ ] Búsqueda/favoritos persisten sin corrupción ni purga transitoria.
- [ ] Wallpapers FLAGS/recorte/memoria y fallos tratados.
- [ ] Texto grande, TalkBack, back/IME y reduced motion revisados.
- [ ] README: qué es, requisitos API29+, build/install, elegir/quitar Home, gestos, favoritos, rices, límites del perfil actual, wallpapers, recuperación, debug/release, fuentes de assets y estado de validación por dispositivo.
- [ ] APK + checksum entregados; certificados/keystore tratados conforme §20.1.
- [ ] PR release mergeado según autorización; tag `v1.0.0` apunta al commit final y está pusheado.

### 20.3 Evidencia física que no se puede sustituir

El modelo/API del teléfono aún no se conoce: medir en §13, no pedirlo como bloqueo de esta especificación. Una sola unidad física valida ese dispositivo; no anunciar compatibilidad ensayada en Android 10–17 si sólo se probó una versión. El target 37 es decisión de compilación/comportamiento, no evidencia de haber tenido teléfono Android 17. Los mínimos pendientes obligatorios son Home role y regreso, lifecycle/proceso/reboot, ciclo de paquetes, insets/IME/gestos del OEM, wallpaper real, reconocimiento/lectura de los cinco diseños, rendimiento/memoria y uso diario 24–48 h de release.

## 21. POST-V1 PARKING LOT

No implementar durante estos cuatro sprints: widgets/AppWidgetHost, folders/drag, packs de iconos, badges/listener de notificaciones, feeds/noticias, web search/IA, cuentas/cloud/backup/importación debug-release, páginas Home, editor de grid/colores/fuentes, ocultar/bloquear apps, Private Space, work profiles/clones/multiusuario, live wallpapers, wallpaper picker de galería, gestos configurables, integration Recents/Quickstep, tablet/foldables/landscape dedicado, monetización/Play Store, categorías automáticas, paquetes de rice descargables y efectos gráficos perpetuos.

Mover aquí una idea no autoriza desarrollarla. V1 termina cuando el launcher pequeño cumple sus gates y los cinco rices completos se usan diariamente.
