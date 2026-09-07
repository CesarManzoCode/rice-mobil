# Sprint de Motion + Interaction + UX — Arctic Glass / rice-mobile

**Branch:** `feat/v1-rices` (mismo PR #5, sin nuevo branch/PR).
**Base de esta pasada:** `dfeaa65` (cierre del sprint de performance).
**Entorno de escritura del código:** sesión remota (Claude Code on the web). Mismo sandbox que los
sprints anteriores: **sin Android SDK/`adb`/emulador** — `./gradlew :app:compileDebugKotlin`
falla al resolver el plugin `com.android.application` por falta de SDK local. Todo lo que sigue
está por tanto **verificado leyendo el código, nunca compilado ni ejecutado en este sandbox.**

## 0. Regla de honestidad de esta pasada

Por instrucción explícita de la tarea: **INTERACTION GATE = PENDING** hasta que el usuario lo
sienta bien en el CUBOT KingKong X con un build real. Cuatro categorías, sin mezclar:

- **IMPLEMENTED** — código escrito, presente en el diff de este sprint.
- **STATICALLY VERIFIED** — releído con atención: tipos correctos, señales conectadas de punta a
  punta (el callback que un widget dispara es el mismo que el host consume), sin ciclo de
  recomposición evidente en el camino caliente. Sigue siendo una lectura, no una medición.
- **PHYSICALLY VERIFIED** — con número o sensación real, en un dispositivo real. *(vacío en esta
  pasada — cero acceso a un dispositivo en este sandbox; ver sección 6.)*
- **PENDING** — no tocado esta pasada, o tocado a medias y señalado como tal.

No se declara "se siente mejor" en ningún punto de este documento sin esa etiqueta.

## 1. Auditoría de interacciones (Fase 1)

Estado del código **antes** de este sprint (base `dfeaa65`). Todas las rutas pasan por
`LauncherHost.kt` (host) + la implementación de cada rice; a menos que se diga lo contrario, la
fila describe el comportamiento en **Arctic Glass**, el laboratorio de este sprint.

### HOME

| Interacción | Estado inicial | Estado final | Feedback actual (antes) | Problema | Transición propuesta |
|---|---|---|---|---|---|
| Swipe up | Home en reposo | Drawer abierto | Ninguno durante el gesto: `HomeGestureSurface` acumulaba el drag y solo al soltar, si superaba un umbral, llamaba `onSwipeUp` → cambio de `screen` → `AnimatedContent` reproducía un tween ya en marcha, desconectado del dedo | Exactamente el síntoma descrito por el usuario: "swipe → delay perceptual → Drawer aparece entero" | **Implementado**: progreso 0f..1f en vivo, drawer sigue el dedo, home recede, scrim progresivo (§2-3) |
| Tap drawer affordance (page dots) | Home en reposo | Drawer abierto | Igual que arriba (mismo `actions.openDrawer`) — corte seco a la animación de ruta | Toque y gesto no compartían código ni sensación | **Implementado**: mismo camino 0→1 que el gesto, spring corto (§5) |
| Tap favorite (dock) | — | App abre | `rememberPressScale` ya existía en `DockSlot` | Ninguno grave; ya cumplía §10 | Sin cambio funcional; migrado a `ricePressable` compartido |
| Long press favorite | — | Menú contextual | `DropdownMenu` de Material, sin posición anclada al item, aparece "de golpe" | Contradice §9 explícitamente ("no Dialog Material genérico"), no aparece cerca del item | **PENDING** — necesita plumbing de posición del item; ver §9 más abajo |
| Tap rice button | Home | Rice Picker | `AnimatedContent` con tween de ruta (motion de Arctic), sin feedback de toque en el botón | El botón en sí (`ArcticRiceButton`) no tenía press-scale propio (heredaba de `ArcticGlassSurface.onClick`, que tampoco lo tenía) | **Implementado**: `ArcticGlassSurface.onClick` ahora usa `ricePressable` — cubre este botón, chips, category tiles, etc. Picker open/select en sí: **PENDING** (fases 15-16) |
| Long press empty area | Home | Rice Picker | Idéntico al tap de arriba (mismo `actions.openPicker`) | Ninguno adicional | Sin cambio esta pasada |

### DRAWER

| Interacción | Estado inicial | Estado final | Feedback actual (antes) | Problema | Transición propuesta |
|---|---|---|---|---|---|
| Back (sistema) | Drawer | Home | `PredictiveBackHandler` único para Drawer+Picker, con un preview genérico (translateY 4%+alpha) del **árbol completo** de `AnimatedContent`, no del Drawer en particular | El preview no era el panel real deslizándose, solo un desvanecido de toda la pantalla | **Implementado**: predictive back de Drawer ahora reescribe directamente el mismo progreso 0f..1f del gesto manual (§31) |
| Swipe down | Drawer | Home | No existía como gesto explícito — solo Back (sistema) o el back-arrow interno de `DrawerSubHeader` | Sin affordance de cierre por gesto | **Implementado**: grabber (handle) arrastrable en la parte superior del panel — ver nota de alcance abajo |
| Tap categoría | Browse | Category | `Crossfade` de 150ms — la "Crossfade usada como martillo" que la tarea pide evitar explícitamente | Sin dirección, sin sensación de "entrar" | **Implementado**: `AnimatedContent` direccional (§10) |
| Tap chip | Browse | Category/AllApps | Mismo `Crossfade` que arriba (los chips navegan por `view`) | Igual que arriba | **Implementado**, mismo mecanismo |
| Tap recent / tap favorite (fila) | — | App abre | `combinedClickable` sin press-scale | Sin feedback de toque | **Implementado**: `ricePressable` |
| Tap "todas las apps" | Browse | AllApps | Mismo `Crossfade` | Igual, además sin distinguir "profundizar" de "cambiar de categoría" | **Implementado** (mismo AnimatedContent direccional; ver nota de alcance §11) |
| Tap app (grid/lista) | — | App abre | `combinedClickable` sin press-scale en `GridCell`/`AllAppsRow` | Sin feedback de toque | **Implementado**: `ricePressable` |
| Long press app | — | Menú contextual | Igual que long-press favorite en Home | Igual | **PENDING** (mismo motivo) |
| Search focus | Campo inerte | Campo enfocado | Sin ningún cambio visual al enfocar | El campo "no se siente vivo" hasta que se escribe | **Implementado**: glow de tinte/borde en foco (§12) |
| Search typing | Browse/Category | Grid de resultados | Sin transición al entrar/salir de resultados; cada tecla reconstruye el grid (correcto) pero el *cambio de vista* (Browse↔Search) era un corte seco | El corte iba y venía en cada búsqueda que se vaciaba/llenaba | **Implementado**: fade+slide corto solo en el cambio Browse↔Search, nunca por tecla (§13) |
| Clear search | Search results | Browse (o Category si `view` no era Browse) | Igual que arriba | Igual | Cubierto por el mismo mecanismo |
| IME action (buscar) | — | Abre único resultado | Sin motion asociado (correcto, es una acción no una transición) | Ninguno | Sin cambio |

### PICKER

| Interacción | Estado inicial | Estado final | Feedback actual (antes) | Problema | Transición propuesta |
|---|---|---|---|---|---|
| Open (long press empty Home) | Home | Picker | `AnimatedContent` con motion de Arctic (fade+slide 28dp) | Razonable ya, pero sin "Home se reduce sutilmente" ni scrim propio (fases 15) | **PENDING** — ver alcance abajo |
| Rice tap | Picker | Nuevo rice, Home | Crossfade fijo de 180ms (contrato §10, valor normativo) sobre **todo el árbol** | Es exactamente el contrato del proyecto (`docs/V1_EXECUTION_CONTRACT.md` §10: "Cambio de rice: crossfade de 180 ms... no conservar los cinco") — no se toca sin instrucción explícita de cambiarlo | **PENDING por diseño**: el valor de 180ms es un contrato fijo del proyecto, no un descuido; una redirección (tile responde con press-scale, ya cubierto) es lo único que se tocó esta pasada |
| Back | Picker | Home (sin cambiar rice) | `PredictiveBackHandler` compartido con Drawer, ahora separado (ver arriba) | El preview era genérico para ambas rutas | **Implementado**: Picker mantiene su propio mecanismo (backProgress + AnimatedContent), ahora sin compartir código con el mecanismo nuevo del Drawer — más simple de razonar, cero regresión |
| Dismiss | — | — | (no existe un "dismiss" fuera de Back en el código actual) | — | Sin cambio |

## 2. Qué se implementó (con verificación estática)

### 2.1 Motion primitives compartidos (`ui/shared/Motion.kt`)

- `MotionTokens`: duraciones (`Instant`/`Fast`/`Standard`/`Emphasis`) y dos `AnimationSpec`
  (`PanelSettleSpring`, `ReducedMotionSettle`) más las constantes del gesto Home↔Drawer (distancia
  de arrastre, umbrales, velocidad de fling). Un objeto pequeño, no un design-system.
- `LocalDrawerDragProgress`: `CompositionLocal<() -> Float>` — una función, no un `Float`, a
  propósito: `LauncherHost` no recompone en cada tick del gesto, así que la única forma correcta de
  que un rice vea el valor continuo es leerlo dentro de su propio `graphicsLayer { }`.
- `Modifier.ricePressable(pressScale, pressMs, onClick, onLongClick?)`: la primitiva que la tarea
  pide explícitamente en la Fase 6. Envuelve `rememberPressScale` (ya existente) + un
  `combinedClickable` sin ripple de Material. Cada rice sigue pasando sus propios números de
  `RiceMotion` — nunca hardcodeados en la primitiva.

**Categoría: STATICALLY VERIFIED.** Releído dos veces: `ricePressable` no introduce un
`MutableInteractionSource` nuevo por recomposición (vive dentro de `composed { remember { ... } }`),
y `LocalDrawerDragProgress` nunca se lee fuera de un bloque `graphicsLayer`/`draw` en el código de
este sprint (ver 2.3).

### 2.2 `HomeGestureSurface` — reporting de arrastre en vivo

Se añadieron `onDragStart`/`onDrag`/`onDragEnd` (todos opcionales, `= {}` por defecto) junto al
`onSwipeUp` que ya existía. Comparten el mismo `draggable`/`rememberDraggableState` que el código
original — no hay un segundo detector de gestos compitiendo por el mismo puntero. Los otros cuatro
rices (Monochrome/Ember/Ivory/Violet) siguen llamando esta función exactamente igual que antes
(`onSwipeUp = actions.openDrawer`, nombrado, sin los parámetros nuevos) — **cero cambio de
comportamiento para ellos**, confirmado por lectura: ningún argumento posicional en sus llamadas
(grep de las 5 llamadas a `HomeGestureSurface`), así que añadir parámetros con default en medio de
la firma no rompe nada.

**Categoría: STATICALLY VERIFIED.**

### 2.3 Home↔Drawer gesture-driven (`LauncherHost.kt`) — el núcleo del sprint

Antes: `screen` cambiaba de golpe (Home→Drawer) y **luego** `AnimatedContent` reproducía una
animación de ruta ya en marcha, sin relación con el dedo. Ahora:

- Un `Animatable` (`drawerSettle`) + un `Float` en vivo (`rawDrawerProgress`, actualizado
  síncronamente durante el arrastre, sin salto a corrutina) representan el mismo panel 0f..1f tanto
  en reposo como a mitad de gesto.
- Mientras se arrastra: Home permanece compuesto y visible detrás (escala 1→0.95, alpha 1→0.9), un
  scrim negro sube de 0 a 0.45 de alpha, y el Drawer (con un grabber encima) se traduce desde fuera
  de pantalla hasta su posición final — todo leído dentro de bloques `graphicsLayer { }`, nunca como
  parámetro de recomposición (ver comentario en el propio archivo sobre por qué `progressProvider`
  es una función).
- Al soltar: se decide abrir/cerrar por umbral de progreso (35%/65%) **o** velocidad de fling
  (1100dp/s), y se anima con un spring corto (`MotionTokens.PanelSettleSpring`) hasta el resultado;
  solo entonces se llama a `viewModel.openDrawer()`/`goHome()` — el estado del ViewModel nunca
  cambia a mitad de una animación en curso.
- El tap del affordance (`actions.openDrawer`) y el "back" (`actions.goHome`, usado hoy solo por el
  back-arrow interno de VioletDrawer) pasan por la **misma** función `settleDrawer(...)` que el
  gesto — nunca un camino separado (contrato §5).
- El predictive back del sistema para el Drawer ahora escribe directamente sobre el mismo
  `rawDrawerProgress` en vez de un preview genérico aparte — cancelar el gesto hace un `spring`
  de vuelta a abierto, nunca un salto.
- `AnimatedContent` deja de ser responsable de la pareja Home↔Drawer: su `key` ya no incluye
  `screen`, solo `(riceId, isPicker)`. Sigue existiendo para el cambio de rice (crossfade de 180ms,
  contrato fijo) y para Picker (motion de ruta sin cambios).

**Bug real encontrado y corregido durante esta misma pasada** (no llegó a commitearse roto): la
primera versión leía `state.screen` en vivo dentro del contenido de `AnimatedContent`; como
`AnimatedContent` mantiene viva la instancia *saliente* del lambda de contenido durante toda la
transición hacia/desde Picker, esa instancia también habría visto `screen == RicePicker` y
renderizado una pantalla vacía en vez del Home/Drawer que se supone se desvanece. Se corrigió con
`lastNonPickerScreen` (congela el último Home/Drawer válido justo cuando `screen` pasa a Picker).
Documentado explícitamente en el propio archivo.

**Categoría: STATICALLY VERIFIED** (releído el flujo completo de estado, incluida la interacción
con `AnimatedContent`/Picker/cambio de rice; sin poder ejecutar, no hay forma de subir esto a
PHYSICALLY VERIFIED en este sandbox).

**Alcance deliberadamente reducido — swipe-down-to-close:** en vez de que *toda el área* del Drawer
responda a un arrastre hacia abajo (lo que competiría con el scroll vertical de sus listas/grids,
exactamente el riesgo que la Fase 21 señala), el cierre por gesto vive en un grabber pequeño
("handle" de 36×4dp) sobre el contenido propio del rice. Es también tocable (cierra igual) — nunca
gesto-only (Fase 30). Esto es una simplificación consciente, no un descuido: resolver prioridad de
gestos entre un drag-to-close de área completa y el scroll de una `LazyColumn`/`LazyVerticalGrid`
sin introducir "scroll pegajoso" habría requerido un `NestedScrollConnection` a medida — fuera del
presupuesto de esta pasada. Queda como **PENDING** si el usuario lo quiere después de probar el
grabber en el dispositivo.

### 2.4 Arctic-specific: dock + wallpaper parallax (§27)

`ArcticHome` lee `LocalDrawerDragProgress` y aplica, solo dentro de `graphicsLayer`:
wallpaper con scale 1→1.05 y `translationY` hacia arriba (14dp máx.); la fila del dock (favoritos +
botón de rice) baja hasta 10dp mientras el Drawer sube. Ningún otro rice lee este `CompositionLocal`
todavía — el valor por defecto (`{ 0f }`) los deja exactamente como estaban (Fase 28: primitivas sin
hardcodear Arctic en la infraestructura).

**Categoría: STATICALLY VERIFIED.**

### 2.5 Drawer: navegación jerárquica direccional + búsqueda (`ArcticDrawer.kt`)

Un único `AnimatedContent` (antes: un `Crossfade` de 150ms) sobre una clave unificada
(`DrawerContentKey`, `Search` o `Hierarchy(view)`):

- Browse↔Category y Browse↔AllApps: slide horizontal con dirección (24dp de entrada, 16dp de
  salida, 220ms) — "profundizar" en vez de intercambio plano. El contenedor se mueve, nunca los
  ítems individuales (contrato §10 explícito: "no animar cada item individual").
- Browse/Category/AllApps ↔ resultados de búsqueda: fade + slide de 6dp, 140ms, **solo quando la
  clave cambia entre buscar/no-buscar** — seguir escribiendo mantiene la misma clave (`Search`), así
  que cada tecla actualiza el grid sin animación (Fase 13, verificado por construcción: la
  `AnimatedContent` no puede disparar una transición si `targetState` no cambia).
- Reduced motion: fade corto sin desplazamiento, en las tres ramas.

**Categoría: STATICALLY VERIFIED**, con una simplificación de alcance frente a la Fase 11: "Todas
las apps" usa el mismo slide horizontal que una categoría en vez de un slide vertical propio —
documentado aquí, no silenciado, por presupuesto de tiempo. Sigue siendo direccional y sigue sin
ser un Crossfade plano.

### 2.6 Search focus glow (§12)

`ArcticSearchField` ahora rastrea foco (`onFocusChanged`) y anima tinte/alpha/borde del
`ArcticGlassSurface` (nuevo parámetro `borderBrush`, con default idéntico al de siempre para todo
lo demás) en 160ms. Reduced motion → `snap()`.

**Categoría: STATICALLY VERIFIED.**

### 2.7 Press feedback ampliado (§6)

`ricePressable` reemplaza `clickable`/`combinedClickable` sin press-scale en: `ArcticGlassSurface`
(cubre de un solo golpe chips, category tiles, "todas las apps" preview, botón de rice picker,
campo de búsqueda), `AppQuickTile`, `DockSlot`, `PageDots` (Home); `AppRowTile`, `GridCell`,
`AllAppsRow` (Drawer); `RiceOption` (Picker). Quedan sin tocar dos enlaces de texto secundarios
("usar como inicio", "ver todo") — afordancias de texto, no ítems de app/chip/categoría, fuera del
foco explícito de la Fase 6.

**Categoría: STATICALLY VERIFIED.**

## 3. Lo que NO se tocó (pendiente, explícito)

- **Long press → menú contextual (Fases 8-9):** sigue siendo un `DropdownMenu` de Material sin
  posición anclada al item. Arreglarlo de verdad requiere que `showAppMenu` lleve consigo la
  posición en pantalla del item pulsado (hoy solo lleva `AppKey`) — un cambio de forma de datos que
  toca `LauncherState`/`LauncherViewModel`/los cinco Drawers/Homes, no solo Arctic. Se decidió no
  hacerlo a medias.
- **Rice picker open (Fase 15) / rice switch como transición propia (Fases 16-18):** el
  crossfade de 180ms al cambiar de rice es un **valor de contrato fijo**
  (`docs/V1_EXECUTION_CONTRACT.md` §10), no un descuido de este sprint — no se ha tocado sin que el
  usuario pida explícitamente revisar ese contrato. La apertura del picker (Home se reduce, scrim,
  stagger corto de miniaturas) no se implementó esta pasada.
- **Wallpaper old/new painter crossfade (Fase 18):** el `WallpaperBackdrop` actual ya cachea 2
  bitmaps máx. y decodifica fuera del hilo principal (sprint de performance), pero no hace un
  crossfade visual entre el wallpaper viejo y el nuevo al cambiar de rice — sigue siendo un corte.
- **Favorite add/remove placement (Fase 20), home module state changes (Fase 19), overscroll
  (Fase 22), IME insets animados (Fase 14), haptics (Fase 29):** no tocados esta pasada.
- **Accesibilidad (Fase 30):** el grabber nuevo es tocable (no gesto-only) y tiene
  `contentDescription`; no se hizo una pasada de TalkBack/focus-order completa sobre el resto de los
  cambios — pendiente de verificación real con TalkBack activado.
- **Los otros cuatro rices:** por instrucción explícita de la tarea, no se les tocó visualmente.
  Se benefician *solo* de lo que es genuinamente compartido: `HomeGestureSurface` (con las nuevas
  señales sin usar, cero cambio de comportamiento) y, más notablemente, el propio mecanismo de
  panel gesto-driven de `LauncherHost` — que es agnóstico de rice por diseño, así que Home→Drawer
  en Monochrome/Ember/Ivory/Violet también pasa a tener arrastre real y spring-settle, aunque sin
  la reacción visual extra (parallax/dock) que solo Arctic implementa. Esto no se ha probado
  visualmente en esos cuatro rices en este sandbox — **PENDING** de una mirada rápida del usuario a
  que nada se vea raro (p.ej. que el panel translúcido del scrim no choque con un fondo ya oscuro).

## 4. Salvaguardas de performance (Fase 25-26) — verificación estática

- Ningún nuevo código de este sprint llama a `PackageManager`, decodifica bitmaps, ordena listas ni
  escribe en DataStore dentro de un `graphicsLayer` o de un callback de arrastre — confirmado
  releyendo cada `graphicsLayer { }`/`onDrag`/`settleDrawer` añadido: son lecturas/escrituras de
  `Float`/`Boolean` en memoria, nada más.
- El progreso continuo del panel nunca es un parámetro de función que fuerce recomposición: se lee
  siempre dentro de un bloque `graphicsLayer` (ver comentarios en `LauncherHost.kt` y
  `LocalDrawerDragProgress`), replicando exactamente el patrón que el código ya usaba para el
  predictive back de Picker.
- No se usa `animateContentSize` en ningún grid/lista; el `AnimatedContent` nuevo del Drawer mueve
  el contenedor completo (`slideInHorizontally`/`slideOutHorizontally` sobre el Box entero), nunca
  ítem por ítem.
- El grabber nuevo no introduce una segunda detección de gestos sobre el área scrolleable del
  Drawer — vive en una franja de 28dp por encima del contenido propio del rice, evitando el
  conflicto de gestos que la Fase 21 señala en vez de intentar resolver prioridad entre dos
  `NestedScrollConnection`.

**Categoría: STATICALLY VERIFIED.** Ningún número de frames — eso solo lo da el dispositivo (§6).

## 5. Reduced motion (Fase 24)

- El settle del panel (`MotionTokens.ReducedMotionSettle`, `tween(120)`) sigue siendo una animación
  corta, no un salto instantáneo — la propia tarea pide "no hacer todo instantáneo si eso empeora
  UX".
- El arrastre en vivo (mientras el dedo está en pantalla) **no se desactiva** con reduced motion:
  no es una animación decorativa, es tracking directo de input — desactivarlo sería peor UX, no
  mejor accesibilidad.
- Las transiciones de categoría/búsqueda caen a un `fadeIn`/`fadeOut` corto sin desplazamiento
  cuando `LocalReducedMotion.current` es `true`.
- El glow de foco del buscador usa `snap()` en vez de `tween(160)` con reduced motion.

**Categoría: STATICALLY VERIFIED** (la lógica `if (reducedMotion) ... else ...` está en cada punto
nuevo; no hay forma de confirmar la sensación real sin el ajuste de sistema activado en un
dispositivo).

## 6. Medición física (Fase 32) — comandos, sin ejecutar aquí

Mismos comandos que el sprint de performance (`docs/evidence/performance-sprint.md` §4), más
específicos para motion:

```bash
# Antes de cada secuencia de la Fase 33, resetea los contadores:
adb shell dumpsys gfxinfo <pkg> reset
# ... ejecuta la secuencia completa de la Fase 33 en el dispositivo ...
adb shell dumpsys gfxinfo <pkg>
```

```bash
# Grabación para inspección visual manual (framerate real, no descripción):
adb shell screenrecord --time-limit 20 /sdcard/interaction-check.mp4
adb pull /sdcard/interaction-check.mp4
```

Interacciones a grabar por separado (no mezclar en un solo clip largo): swipe-up lento cancelado,
swipe-up rápido completado, swipe-down del grabber, tap del affordance, tap de categoría ida y
vuelta, tap "todas las apps" ida y vuelta, búsqueda con clear, back-gesture durante Drawer.

## 7. Secuencia de aceptación de interacción (Fase 33)

Cada paso: qué debe sentirse/verse — sin ejecutar en este sandbox, para que el usuario compare
contra esto en el dispositivo real.

1. **Home idle.** Reloj visible, wallpaper estático, dock con favoritos (o hint vacío).
2. **Swipe up lento hasta 40%, cancelar.** El Drawer debe asomar exactamente hasta donde llegó el
   dedo (ni más ni menos), Home debe verse ligeramente más pequeño/opaco, y al soltar antes del
   35% debe volver a Home con un spring corto — nunca un salto brusco a 0.
3. **Swipe up rápido, completar.** Aunque no llegue al 35% de recorrido, una velocidad alta hacia
   arriba (fling) debe abrir igual.
4. **Swipe down lento, cancelar.** Desde el grabber, arrastrar hacia abajo menos del 35% del
   recorrido y soltar: debe volver a abierto con spring, no cerrarse.
5. **Swipe down rápido, cerrar.** Un fling hacia abajo desde el grabber cierra aunque el recorrido
   sea corto.
6. **Tap drawer affordance.** Debe sentirse **igual de rápido** que completar el gesto — mismo
   spring, ~220-300ms totales, nunca "teatral".
7. **Tap categoría.** El grid nuevo entra desde la derecha mientras el browse sale a la izquierda
   (o viceversa) — nunca un crossfade plano.
8. **Back.** Reversa exacta del paso 7: la categoría sale hacia donde entró, browse vuelve desde el
   lado opuesto.
9. **Tap "Todas".** Misma familia de movimiento que categoría (ver §11 en la sección 2.5 — alcance
   reducido, documentado).
10. **Back.** Reversa del paso 9.
11. **Focus search.** El campo debe iluminarse (tinte/borde) en menos de 200ms perceptibles.
12. **Type "cha".** Cada letra actualiza el grid sin ninguna animación de por medio — solo el
    primer cambio Browse→Search (o viceversa al borrar todo) anima.
13. **Clear.** Mismo fade+slide corto que el paso 11, en reversa.
14. **Tap app.** La app debe abrir "inmediatamente" — sin delay artificial (el código no añade
    ninguno: `AppLauncher.launch` es síncrono y `goHome()` se llama justo después, contrato §3.5,
    sin tocar esta pasada).
15. **Long press app.** **PENDING** — hoy sigue siendo un `DropdownMenu` genérico, sin tratamiento
    especial que verificar aquí.
16. **Add/remove favorite.** Sin animación de colocación (Fase 20 no tocada) — el dock se
    recompone con el nuevo conjunto, sin easing de por medio.
17. **Open picker.** Motion de ruta existente (fade+slide de Arctic), sin el tratamiento nuevo de
    reducción-de-Home/scrim que pide la Fase 15 — **PENDING**.
18. **Select rice.** Crossfade de 180ms (contrato fijo) — el tile de selección sí responde al
    toque (press-scale nuevo), la transición en sí no se tocó.
19. **Back gesture durante Drawer.** El panel debe seguir el gesto del sistema en tiempo real
    (§31); cancelar el back del sistema debe volver a abierto con spring, sin salto.
20. **Rapid sequence x10.** Abrir/cerrar el Drawer diez veces seguidas rápido no debe acumular
    lag creciente ni dejar el panel a medio camino — cada `settleDrawer` sincroniza explícitamente
    su punto de partida (`drawerSettle.snapTo(start)`) antes de animar, así que un gesto que
    interrumpe una animación en curso no debería producir un salto visible.

## 8. GATE FINAL

**INTERACTION GATE = PENDING.**

Este documento no declara éxito por "hay AnimatedContent" o "hay springs" — declara,
específicamente, qué se implementó, qué se verificó leyendo el código, y qué queda exactamente
igual que antes. La pregunta real — "¿cada interacción responde inmediatamente y se siente
físicamente conectada?" — solo la puede responder el usuario en el CUBOT KingKong X con un build
real, con la secuencia de la sección 7.
