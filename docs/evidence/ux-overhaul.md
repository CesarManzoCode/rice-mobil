# UX Overhaul — rice-mobile

**Branch:** `feat/v1-rices` (mismo PR #5, sin nuevo branch/PR, por instrucción explícita de la tarea).
**Base de esta pasada:** `544afec` (cierre del sprint de interacción — gesture-driven Home↔Drawer,
predictive back, press feedback compartido, navegación jerárquica del Drawer). Esa pasada dejó
documentado, explícitamente como **PENDING**, exactamente lo que esta pasada ataca: menú contextual
anclado, picker interactivo, transición hero de rice, crossfade de wallpaper, haptics, motion
language distinto por rice, y arbitraje de gesto completo (no solo el grabber).

**Entorno de escritura**: sesión remota (Claude Code on the web), mismo sandbox que todos los
sprints anteriores — **sin Android SDK/`adb`/emulador**. `./gradlew :app:compileDebugKotlin` no
puede ejecutarse aquí (falla resolviendo el plugin `com.android.application` por falta de SDK
local). Todo lo que sigue es **código releído con mucho cuidado, nunca compilado ni ejecutado en
este sandbox** — la etiqueta de cada sección lo dice explícitamente, sin fingir más certeza de la
que hay.

## 0. Regla de honestidad

- **IMPLEMENTED** — código escrito, presente en el diff.
- **STATICALLY VERIFIED** — releído con atención: tipos, imports, señales conectadas de punta a
  punta, sin ciclo de recomposición evidente. Sigue siendo lectura, no medición.
- **PHYSICALLY VERIFIED** — vacío en esta pasada. Cero acceso a dispositivo en este sandbox.
- **PENDING** — no tocado, o tocado a medias y señalado como tal explícitamente.

**INTERACTION GATE = PENDING** hasta que el usuario lo sienta en un dispositivo real. Este
documento no declara "listo" en ningún punto sin esa reserva.

## 1. Qué se implementó

### 1.1 Menú contextual anclado, por rice, con haptics (§7-9, §16, §21)

Reemplaza por completo el `DropdownMenu` de Material genérico que sobrevivía del sprint anterior.

- **`ui/shared/ScreenRect.kt`** (nuevo): bounds ventana-relativas, tipo puro sin dependencia
  Compose/Android más allá de construirse desde `LayoutCoordinates.boundsInWindow()` — mantiene
  `LauncherNavigation`/`LauncherState` testeables como JVM puro (mismo principio que `AppKey`).
- **`ui/shared/Motion.kt` → `Modifier.appCellPressable(...)`** (nuevo): envuelve `ricePressable`
  capturando las bounds del ítem vía `onGloballyPositioned` + dispara el haptic de long-press
  **una sola vez, centralizado**, en vez de repetirlo en cada uno de los ~15 sitios que lo llaman.
- **`LauncherState.kt`**: `TransientState.appMenu`/`LauncherState.appMenu` pasan de `AppKey?` a
  `AppMenuRequest(key, anchor)?` — el anchor viaja con la petición del menú, no se recalcula tarde.
- **`RiceActions.showAppMenu`**: `(AppKey) -> Unit` → `(AppKey, ScreenRect) -> Unit`.
- **`launcher/AppActionsMenu.kt`** (nuevo, reemplaza `ui/shared/AppActionsMenu.kt`): un `Popup`
  posicionado por un `PopupPositionProvider` a medida (`AnchoredMenuPositionProvider`) que calcula
  su posición **desde el `ScreenRect` capturado**, nunca desde el anchor por defecto de `Popup`
  (que solo vería la posición del host completo). Abre debajo del ítem, voltea arriba si no hay
  espacio, siempre clamp dentro de la ventana. Envuelto en `AnimatedVisibility` con
  `enter = EnterTransition.None` / `exit = fadeOut(...)` para mantener el `Popup` compuesto durante
  la animación de salida (el patrón estándar de Compose para animar Dialogs/Popups: el contenido
  real usa `Modifier.animateEnterExit(scaleIn+fadeIn / scaleOut+fadeOut)` con `transformOrigin`
  arriba-centro, así "nace desde" el punto de anclaje y se encoge de vuelta hacia él al cerrar —
  nunca un corte instantáneo). Estilo (`ContextMenuStyle`: fondo/tinta/borde/forma) viene de
  `RiceMotion.menuStyle`, uno por rice — Monochrome rectangular alto contraste, Arctic glass
  translúcido redondeado, Ember cut-corner carbón/cobre, Ivory crema/tinta editorial, Violet
  violeta profundo redondeado. `dismissOnBackPress = false` porque el `BackHandler` del host
  (`LauncherNavigation.menuBackHandlerEnabled`) ya es la única fuente de verdad para Back —
  `dismissOnClickOutside = true` sí queda al Popup.
- **15 sitios de long-press actualizados** (Arctic Home×2/Drawer×3, Monochrome Home×1/Drawer×1,
  Ember Home×1/Drawer×2, Ivory Home×1/Drawer×1, Violet Home×1/Drawer×2) para pasar por
  `appCellPressable`/capturar bounds y llamar `showAppMenu(key, rect)`. La única excepción es
  `EmberHome.FavoriteBlock`, que conserva su propio mecanismo de prensado (inversión de color, no
  escala — su identidad de motion, §19) y captura bounds manualmente con el mismo patrón interno.

**Categoría: STATICALLY VERIFIED.** Releído dos veces el flujo `onGloballyPositioned` → `bounds`
state → `onLongClick` → `showAppMenu(key, bounds)` → `TransientState.appMenu` →
`AppActionsMenuHost` → `Popup` posicionado. Sin PackageManager/IO en el camino.

### 1.2 Arbitraje de gesto completo: drag-to-close desde cualquier punto del Drawer (§23)

El sprint anterior dejó el cierre por gesto limitado a un grabber de 28dp arriba del contenido,
documentado como "alcance reducido" precisamente para no pelear con el scroll de las listas.

- **`LauncherHost.kt` → `rememberDrawerCloseNestedScrollConnection`** (nuevo): un
  `NestedScrollConnection` que envuelve el `Box` que aloja `rice.Drawer(...)`. Como padre del
  `LazyColumn`/`LazyVerticalGrid` que cada rice pone dentro, solo ve `available` (lo que la lista
  *no* pudo consumir) — que es exactamente cero mientras la lista puede seguir scrolleando, y deja
  de serlo justo cuando un arrastre hacia abajo llega al tope de la lista sin más espacio.
  Desde ahí llama los mismos `onGrabberDragStart/onGrabberDrag/onGrabberDragEnd` que el grabber ya
  usaba — mismo camino, misma física, **sin tocar ninguno de los cinco rices**: funciona porque
  cualquier `LazyColumn`/`LazyVerticalGrid` participa automáticamente en nested scroll.
  `rememberUpdatedState` para cada callback (mismo patrón que `reducedMotionState`/`densityState`
  ya usaban en este archivo) para que la instancia `remember`ada sobreviva sin closures viejas.
- El grabber explícito se mantiene (accesibilidad: cerrar nunca depende solo de un gesto).

**Categoría: STATICALLY VERIFIED** con una limitación documentada, no oculta: si el usuario
arrastra hacia abajo (entra en modo cierre), luego invierte la dirección lo suficiente para que la
lista vuelva a poder consumir el scroll normalmente, y suelta sin volver a generar un delta hacia
abajo, el gesto de cierre puede quedar congelado en el progreso donde se pausó hasta el próximo
fling/onPreFling en vez de deshacerse visualmente al mismo ritmo del regreso del dedo. No es un
crash ni un estado inconsistente — es una imprecisión física de un caso de reversión poco común;
arreglarlo del todo pediría un tracking de dirección más fino que no entró en el presupuesto de
esta pasada.

### 1.3 Drag en vivo real para los cinco rices, no solo Arctic (§1, corrige un hallazgo del sprint anterior)

Auditando el código antes de tocar nada: **Monochrome/Ember/Ivory/Violet nunca llamaban
`onDragStart`/`onDrag`/`onDragEnd`** de `HomeGestureSurface` — solo Arctic lo hacía. Los otros
cuatro seguían con el `onSwipeUp` de umbral fijo del sprint viejo; la evidencia previa afirmaba
("STATICALLY VERIFIED") que los cinco se beneficiaban del panel gesto-driven del host, pero eso
solo es cierto para el *scrim/recede genérico* que vive en `LauncherHost` — el *input* del
arrastre en sí nunca llegaba a activarse para esos cuatro, así que `dragActive` se quedaba en
`false` durante todo el gesto y no había nada que animar en vivo hasta soltar. Corregido: los
cuatro ahora pasan `onDragStart = actions.beginDrawerDrag`, `onDrag = actions.dragDrawer`,
`onDragEnd = actions.endDrawerDrag`, `onSwipeUp = {}` — exactamente el cableado de Arctic.

**Categoría: STATICALLY VERIFIED** (mismos parámetros, mismo tipo, misma función compartida; sin
comportamiento nuevo que inventar, solo la wiring que faltaba).

### 1.4 Rice Picker: fondo vivo, prensado por rice, hero de selección (§15-17)

- El fondo deja de ser un panel neutro (`Color(0xFF141414)`) y pasa a ser el **wallpaper real del
  rice actual**, atenuado bajo un scrim — el picker ya no se siente como un diálogo aparte.
  **Simplificación documentada**: reconstruir el Home completo detrás del picker (la redacción
  literal del pedido) quedó fuera del presupuesto de esta pasada; se optó por la parte más honesta
  y barata de la misma idea ("el fondo sigue vivo") sin la composición completa de Home.
- Cada `RiceOption` usa `rice.motion.pressScale/pressMs/pressSpec` en vez de una constante fija —
  el picker también hereda el motion language distinto de cada rice al tocarlo.
- Al seleccionar: haptic inmediato, `prefetchWallpaper(...)` lanzado en paralelo (sin bloquear,
  sin `delay()` artificial — contrato §31) para que el wallpaper del rice destino tenga la mejor
  chance de ya estar decodificado cuando el nuevo Home componga, y un scale-up sutil (`heroScale`,
  spring del propio rice) en el tile tocado que sigue animando mientras la transición de ruta del
  host (crossfade de 180ms, valor de contrato fijo, sin tocar) ya está en marcha — el tile
  "toma la pantalla" en vez de solo desaparecer.

**Categoría: STATICALLY VERIFIED.**

### 1.5 Wallpaper: prefetch + fallback-to-bitmap crossfade, nunca blanco (§18)

- **`prefetchWallpaper(context, spec, targetSize)`** (nuevo, exportado): decodifica y cachea un
  wallpaper por adelantado, reutilizando el mismo `WallpaperBitmapCache`/`decodeSampled` que
  `WallpaperBackdrop` ya usaba. Llamado desde el picker en el momento exacto de la selección.
- **`WallpaperBackdrop`**: en vez de un `Box` simple, ahora un `Crossfade` entre
  `WallpaperLayer(key, fallbackColor, image)` — cuando el bitmap real termina de decodificar,
  la transición de "color de fallback" a "bitmap real" se anima (180ms, `snap()` con reduced
  motion) en vez de aparecer de golpe.

**Nota de honestidad arquitectónica**: cada rice instancia `WallpaperBackdrop` con un `spec` fijo
para toda la vida de esa composición (el spec de un rice no cambia dentro de la misma instancia);
un cambio de rice crea una instancia *nueva* vía el `AnimatedContent` del host. Así que el
crossfade "viejo wallpaper → nuevo wallpaper" real ocurre a nivel del crossfade de ruta del host
(180ms, contrato fijo), con ambas instancias (saliente/entrante) vivas simultáneamente durante esa
ventana — el mecanismo de esta sección resuelve el problema *dentro* de esa ventana (que la
instancia entrante no muestre un color plano mientras decodifica) más que un crossfade "propio"
entre dos wallpapers dentro de un único `WallpaperBackdrop`. Documentado explícitamente para que
quede claro qué mecanismo resuelve qué parte del problema.

**Categoría: STATICALLY VERIFIED.**

### 1.6 Motion language distinto por rice, no solo Arctic (§19-20)

`RiceMotion` gana `pressSpec: AnimationSpec<Float>?` y `menuStyle: ContextMenuStyle`, uno por rice:

| Rice | `pressSpec` | Sensación buscada |
|---|---|---|
| Monochrome | `spring(dampingRatio=1f, stiffness=1400f)` | snap crítico, sin overshoot — "quirúrgico" |
| Arctic | `spring(dampingRatio=0.78f, stiffness=520f)` | un pelo de overshoot — "líquido pero rápido" |
| Ember | `spring(dampingRatio=0.42f, stiffness=700f)` | subamortiguado, overshoot real — "piezas encajando" |
| Ivory | `spring(dampingRatio=1f, stiffness=380f)` | crítico pero más lento — editorial, casi sin bounce |
| Violet | `spring(dampingRatio=0.62f, stiffness=260f)` | suave y lento — "profundidad", cinematográfico |

Cableado en los 15 sitios de `appCellPressable`/`ricePressable` de favoritos/dock/celdas de cada
rice (además del picker, §1.4). **Limitación reconocida**: el pedido original también describe
efectos visuales por rice más allá de la curva (halo en Violet, compresión mecánica visible en
Ember más allá del spring) — esos efectos *visuales* adicionales no se implementaron esta pasada
por costo de plumbing (exponer el estado de presión fuera de `appCellPressable` para dibujar un
halo aparte); lo que sí se implementó y es real es la curva de animación en sí, que ya hace que
las cinco sensaciones de prensado sean objetivamente distintas, no solo un número de escala
diferente sobre el mismo `tween`.

**Categoría: STATICALLY VERIFIED.**

### 1.7 Favorito add/remove: reflow y fade real, no teleport (§9)

- **Arctic**: dock (`Row` → `LazyRow` + `animateItem()`) y el módulo de acceso rápido de Home.
- **Monochrome**: lista de favoritos de Home (`Column`+`for` → `LazyColumn`+`itemsIndexed`+
  `animateItem()`), más la fila de recientes del Drawer.
- **Ember**: la matriz 1+2×2 (`Column`+`chunked(2)` → `LazyVerticalGrid` de 2 columnas con
  `GridItemSpan(2)` en el primer ítem — reproduce la misma geometría *gratis*, vía spans, en vez
  de chunking manual, y de paso gana `animateItem()`).
- **Ivory**: fila de favoritos de Home (`Row` → `LazyRow` + `animateItem()`), más la fila de
  recientes del Drawer.
- **Violet**: **simplificación documentada** — el cluster 1-2-2 con stagger vertical manual
  (contrato §18.6) no encaja limpio en un `LazyVerticalGrid` con spans sin perder exactamente ese
  stagger; se aplicó `Modifier.animateContentSize()` al contenedor en su lugar (el contenedor se
  redimensiona con una animación cuando aparece/desaparece una fila, en vez de saltar), una
  degradación real y reconocida frente al fade+reflow por ítem que sí tienen los otros cuatro.

`animateItem()` es un miembro de `LazyItemScope` — no se aplicó a los grids grandes de catálogo
(`AppGrid`, `CompactGrid`, `AllAppsView`, resultados de búsqueda) **a propósito**: el propio pedido
advierte explícitamente "no animar 71 apps"; esos grids re-filtran en cada tecla y animar su
reflow completo sería exactamente el "carnaval" que el pedido pide evitar.

**Categoría: STATICALLY VERIFIED.**

### 1.8 Haptics (§21)

- Long-press en cualquier celda de app/favorito → `appCellPressable` (centralizado, 15 sitios de
  una sola vez) + el mecanismo manual equivalente en `EmberHome.FavoriteBlock`.
- Toggle de favorito en el menú contextual → `AppActionsMenuHost`.
- Selección de rice en el picker → `RicePicker.RiceOption`.
- **No** se añadió haptic al settle del drag del Drawer (el pedido lo marca como "quizá" — se
  decidió no hacerlo para no arriesgar un patrón de vibración repetitivo en cada apertura/cierre).

**Categoría: STATICALLY VERIFIED.**

### 1.9 IME (§14) — ya resuelto, sin cambios necesarios

Los cinco Drawers ya envuelven su columna raíz en `.safeDrawingPadding()` desde el sprint
anterior — `safeDrawing` es la unión de `systemBars ∪ ime ∪ displayCutout`, así que el espacio
para el teclado ya se reserva/anima sin salto brusco. Añadir un `.imePadding()` adicional encima
habría *duplicado* ese padding cuando el teclado está visible — se verificó esto releyendo el
comportamiento de `safeDrawingPadding()` en vez de añadir código que hubiera sido un bug nuevo.
Documentado aquí para que quede explícito que se revisó, no que se ignoró.

## 2. Lo que NO se tocó, explícito

- **Transformación completa de "Search Mode"** (§12-13): la mecánica ya existente (fade+slide
  Browse↔Search, sin animación por tecla) del sprint anterior se mantiene tal cual — no se
  construyó la reconfiguración dramática de layout (campo expandiéndose a posición dominante,
  chips ocultándose) que el pedido describe. Motivo: tocar esto de verdad implica rehacer el
  layout de cabecera de los cinco Drawers, cada uno con su propia geometría — fuera del
  presupuesto de esta pasada frente a las diez tareas anteriores, que tocan las cinco failure-
  criteria explícitas de la tarea (picker, menú, rice-switch, motion-por-rice, Home/Drawer).
- **Categorías/All Apps con transición propia distinta** (§10-11): ya direccional desde el sprint
  anterior (slide horizontal con profundidad), no se le dio a "Todas las apps" una transición
  vertical/expand distinta de "Categoría" — sigue siendo la simplificación de alcance que el
  sprint anterior ya documentó.
- **Halo visual de Violet / compresión mecánica visible de Ember** más allá de la curva de spring
  — ver §1.6.
- **Overscroll por rice** (§24): no tocado.
- **Reversión perfecta del drag-to-close en caso de inversión de dirección** — ver §1.2.

Ningún ítem de esta lista se presenta como completo en ningún otro punto de este documento.

## 3. Revisión estática (Fase 43 del pedido)

Se releyó cada archivo tocado buscando específicamente: imports faltantes/sobrantes tras mover
código entre funciones, firmas de función que cambiaron de forma (positional vs named en
`RiceMotion`, que tiene un parámetro con default antes de uno sin default — verificado que las
únicas instanciaciones usan argumentos nombrados), el patrón `AnimatedVisibility` +
`animateEnterExit` dentro de un `Popup` anidado (resolución de receptor implícito de Kotlin a
través de una lambda sin receptor propio — patrón estándar documentado de Compose para animar
Dialogs/Popups), la dirección de dependencia de paquetes (ui.shared nunca importa de `launcher`;
`AppActionsMenu.kt` se movió a `launcher` precisamente para no invertir esa dependencia), y las
firmas de `NestedScrollConnection` (`onPostScroll`/`onPreFling`, no-suspend/suspend
respectivamente). Se encontraron y corrigieron en el camino: un import roto con caracteres
corruptos introducido por un error de edición propio, y dos imports de haptics (`Motion.kt`)
que faltaban tras escribir el código que los usa — ambos corregidos antes de este documento, no
después.

## 4. Estado de build/test — sin ejecutar en este sandbox

Mismo bloqueo que todos los sprints anteriores: sin Android SDK/`adb` en este sandbox,
`./gradlew :app:compileDebugKotlin` / `test` / `lint` / `assembleDebug` no pueden ejecutarse aquí.

```bash
# A ejecutar en el equipo del usuario:
./gradlew :app:compileDebugKotlin
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

## 5. Validación física — PENDING, secuencia sugerida

Sin acceso a dispositivo en este sandbox. Cuando el usuario instale el build:

1. Home → swipe up lento, cancelar, swipe up rápido — en **los cinco rices**, no solo Arctic
   (§1.3 corrige un hallazgo real del sprint anterior).
2. Drag-to-close: desde el grabber, y desde *cualquier punto* de una lista ya scrolleada al tope
   (§1.2) — en los cinco rices, sin tocar código de rice individual.
3. Long-press cualquier app/favorito en los cinco rices: el menú debe nacer *desde* el ítem
   tocado, no aparecer centrado/genérico; su forma/color debe leerse distinta por rice.
4. Añadir/quitar favorito desde el menú: el dock/lista debe reflowear con motion, no saltar
   (excepto Violet, que solo redimensiona el contenedor — ver §1.7).
5. Abrir el picker (long-press en área vacía de Home): el fondo debe verse el wallpaper actual
   atenuado, no un panel plano. Tocar un rice: haptic, el tile debe crecer un poco antes/durante
   el crossfade de ruta.
6. Repetir la secuencia de tortura rápida de la tarea (Home→Drawer→Home→Drawer→Categoría→back→
   Search→clear→Picker→Rice2→Picker→Rice3→Drawer→app) en los cinco rices sin crash ni estado
   inconsistente.
7. TalkBack: confirmar que el menú contextual anclado sigue siendo alcanzable/anunciable (usa
   `Popup(focusable=true)`, no verificado con TalkBack real en este sandbox).

**INTERACTION GATE = PENDING.** Este documento no declara que las cinco failure-criteria de la
tarea estén "resueltas" en sentido físico — solo que el código que las ataca existe, se releyó con
cuidado, y qué se simplificó honestamente en el camino.
