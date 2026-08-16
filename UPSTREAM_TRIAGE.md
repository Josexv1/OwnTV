# Triaje de upstream — OwnTV / SteadfastTV → Josexv1/OwnTV

> Archivo **local, gitignored**. Es la libreta de trabajo para decidir qué commits de los dos
> repos de arriba entran en nuestro fork. Actualízalo conforme vayas aplicando cosas.
>
> Generado: 2026-08-15 · Base local: `2e648f1`

---

## 1. Topología real

```
                      cdade9a  "feat(settings): support hidden third browse panels"
                         │      ← ancestro común de TODO
         ┌───────────────┴────────────────┐
         │                                │
   ahXN0O/OwnTV                    flashingcursor/SteadfastTV
   (upstream, original)            (el fork que nos descargamos)
         │  23 commits                    │  102 commits
         │  → 9dcda6b (v4.2.1)            │  → f379cb4
         │                                │
         │                          NOSOTROS: f379cb4 + 2e648f1  = 103 commits
         │                          + WIP sin commitear (metadata/TMDB)
         │
   Josexv1/OwnTV main = efe2d76  ← solo era un espejo viejo de upstream, sin trabajo propio
```

Datos duros:

| Medida | Valor |
|---|---|
| Commits de upstream que NOS FALTAN | **23** |
| De esos, ya aplicados por equivalencia (`git cherry`) | **0** |
| Commits propios de SteadfastTV (ya son nuestra base) | 102 |
| Commits nuestros encima | 1 (`2e648f1`) + WIP |
| Ficheros que hemos tocado desde `cdade9a` | 181 |

**Qué es SteadfastTV**: no es un fork de features, es un **rediseño visual completo** de OwnTV —
tipografía Figtree, paleta charcoal neutra, foco por anillo blanco (no relleno de acento),
componentes de browse compartidos (`MediaListRow`, `CategoryHeader`, `MediaContextMenu`),
onboarding reconstruido sobre `SetupScaffold`. Por eso choca justo con los commits de
"appearance" de upstream y casi nada con los de player/red/metadata.

---

## 2. Remotos (ya configurados)

```bash
origin      https://github.com/Josexv1/OwnTV            # fetch + push  ← el nuestro
steadfast   https://github.com/flashingcursor/SteadfastTV   # fetch only (push deshabilitado)
upstream    https://github.com/ahXN00/OwnTV                 # fetch only (push deshabilitado)
```

Refrescar y ver qué hay nuevo:

```bash
git fetch --all --prune
git log --oneline --no-merges HEAD..upstream/main    # lo que falta del original
git log --oneline --no-merges HEAD..steadfast/main   # lo que falta del fork descargado (hoy: 0)
```

---

## 2b. ESTADO: qué se ha aplicado ya (2026-08-15)

15 commits nuevos sobre `2e648f1`. Todo compila, `testStandardDebugUnitTest` y
`lintStandardDebug` en verde, y el APK release firmado pasa el gate del Streamer 4K.

### ✅ Aplicados

| Commit | Cómo entró |
|---|---|
| `f1b1f1c` Retry-After en cambio de canal | cherry-pick limpio |
| `58c8402` foco PiP en la Guía | cherry-pick (solo chocó el changelog) |
| `e1d6416` artwork + fiabilidad DNS | cherry-pick |
| `b3a9fbf` respeta el motor elegido | cherry-pick |
| `2ea168f` split de los Workers | cherry-pick totalmente limpio |
| `c6afd38` estado del test DNS | cherry-pick (dependía de `b3a9fbf`) |
| `c990182` auditoría de playback | adaptado: `SubtitleRepository` y `modalScrim` |
| `710e3b5` Stalker device ID | **renumerado a v30** (colisión de migración) |
| `1076280` toolchain | portado a mano, sin los screenshots ni el player |
| `39f96af` Now Trending | **renumerado a v31+v32**, conserva nuestro scorer |
| `6dd0baa` horario de Trending | cherry-pick limpio |
| `34c65c8` cupo metadata + fotos de reparto | adaptado: `cast` → `List<CastMember>` |
| `cd83d57` surround + decoder real | **port parcial**, solo los ficheros de player |

Más tres commits nuestros: el WIP de metadata, la adaptación del scrim, y el arreglo
de plurales que hacía fallar el lint.

### ❌ NO aplicados y por qué

| Commit | Motivo |
|---|---|
| `8bf43f1` zoom/volumen por ítem | **Bloqueado.** Upstream partió `PlayerHud.kt` en cuatro ficheros (`PlayerHudChrome/Controls/Dialogs`). Nuestro fork tiene el HUD reescrito entero (scrim propio, timeline unificado, badges neutros). No es un conflicto, es un refactor: hay que portar la feature a mano. |
| `91ad324`, `5ba3c65`, `f9a3f3d` | Los tres ROJOS de apariencia. Sin decidir (ver §5). |
| `efe2d76`, `29445d1` | Gates de i18n; romperían el build hasta hacer una pasada completa. |
| `9dcda6b`, `1afdc59`, `26e6aea` | Bookkeeping de upstream. |

### ✅ Migraciones de Room: numeración ALINEADA con upstream

Hubo una divergencia temporal (nuestro v29 era `spokenLanguagesJson`, el suyo Stalker),
pero al eliminar ese campo la numeración volvió a coincidir exactamente:

| Versión | Contenido |
|---|---|
| v29 | identidad Stalker (serial / deviceId / deviceId2 / signature) |
| v30 | snapshots de Now Trending |
| v31 | metadata de título indexada + estado persistente de Trending |
| (v32) | `playback_prefs` — **no aplicado**, llega con `8bf43f1` |

Ya no hay renumeración que mantener: los próximos commits de upstream que traigan
migraciones encajan con su número original.

**Por qué se quitó `spokenLanguages`**: se parseaba de TMDB, se guardaba en
`metadata_cache` y no se mostraba en ningún sitio. Son idiomas de producción/doblaje,
no las pistas de audio del archivo IPTV, y presentarlos como tal confundía. La línea de
subtítulos sí se queda: esa lista subtítulos de OpenSubtitles realmente descargados.

Room solo exporta el esquema de la versión que compila, así que en el árbol solo hay
`31.json`. Upstream tampoco publica nada más allá de `9.json`, y el test de migraciones
arranca de `2.json`/`3.json`/`7.json`, así que no falta nada.

### ⚠️ Si tenías instalada una build de la numeración vieja

Una BD escrita por las builds intermedias quedó en **v32** con la columna
`spokenLanguagesJson`, o sea *por delante* de esta build (v31). Room no baja de versión.
Arreglo sin perder datos, sobre una copia sacada con `run-as`:

```sql
ALTER TABLE metadata_cache DROP COLUMN spokenLanguagesJson;
PRAGMA user_version = 31;
UPDATE room_master_table SET identity_hash = '<identityHash de 31.json>' WHERE id = 42;
```

Las tres cosas hacen falta: cambiar solo `user_version` deja el hash viejo en
`room_master_table` y Room aborta con "cannot verify the data integrity".

Devolver el fichero al sandbox: `adb push` a `/data/local/tmp`, `chmod 666`, y luego
`run-as tv.own.owntv sh -c 'cat /data/local/tmp/owntv.db > databases/owntv.db'`
borrando antes `-wal` y `-shm`. Hazlo desde PowerShell, no desde Git Bash: MSYS traduce
`/data/...` a `C:/Program Files/Git/data/...` y el comando falla en silencio.

Probado así en el emulador: 194.728 películas intactas, `integrity_check` ok, la app
abre en v31 sin migración.

---

## 3. Los 23 commits de upstream, triados

`overlap` = ficheros de *código* que ese commit toca y que nosotros también hemos modificado
desde `cdade9a` (excluye `extras/`, `CHANGELOG*`, `README`, `tools/i18n/`, `worker/`).
Es el proxy honesto del coste de conflicto.

### 🟢 VERDE — traer ya, riesgo bajo

| SHA | Qué hace | files / overlap | Nota |
|---|---|---|---|
| `f1b1f1c` | Espera el `Retry-After` del proveedor en vez de fallar el canal (HTTP 429 al cambiar de canal) | 10 / 2 | **El de más valor real.** Bug de playback puro. Overlap solo en `PlayerHud.kt` + un string. |
| `58c8402` | Restaura el foco del PiP en la Guía y limpia avisos obsoletos | 4 / 2 | Fix pequeño y limpio. |
| `e1d6416` | Mejora la carga de artwork y la fiabilidad del test de DNS | 6 / 1 | Overlap solo en `SettingsViewModel.kt`. |
| `1076280` | Refresco de toolchain: Kotlin 2.3.21, Compose BOM 2026.06.01, KSP 2.3.11, media3 1.11.0, Gradle 9.7 | 30 / 4 | **Aplicar a mano**, no cherry-pick: el commit arrastra screenshots y docs. Ver §4. |
| `568e38f` | Baseline profile generado (`baseline-prof.txt` + `startup-prof.txt`) | 2 / 0 | Sin conflicto, pero está grabado contra la UI de upstream. **Mejor regenerar el nuestro**: `./gradlew :app:generateBaselineProfile` (necesita emulador API 33+). |

### 🟡 ÁMBAR — valioso, pero hay que aplicarlo a mano

| SHA | Qué hace | files / overlap | Nota |
|---|---|---|---|
| `cd83d57` | Mantiene el surround a través de huecos de timing y reporta el decoder real | 66 / 39 | La lógica está en `AudioOutputPolicy` / player; **el overlap es casi todo traducciones**. Valor alto si usas audio passthrough. |
| `b3a9fbf` | Respeta el motor elegido para pelis y series en cada apertura | 61 / 51 | Igual: overlap inflado por `strings_*.xml` de 20 idiomas. La lógica es corta. |
| `c990182` | Cierra la auditoría de playback: identidad, recuperación y estados terminales | 51 / 29 | Robustez de reproducción. Ídem, mayoría traducciones. |
| `710e3b5` | Stalker: identificación avanzada de dispositivo (device ID, serial, MAC) | 32 / 10 | **Feature real** para portales Stalker que exigen device ID. ⚠️ Trae migración de Room (`OwnTVDatabase` +24). Coordinar con nuestro schema 29. |
| `8bf43f1` | Zoom y volumen por ítem, ajustes de paso | 105 / 60 | Feature grande. Traducciones + tests inflan la cuenta. |
| `6dd0baa` | Trending: baja la lista de TMDB por horario, no tras cada sync | 17 / 4 | **Depende de `39f96af`.** Si no traes Trending, este no aplica. |
| `39f96af` | "Now Trending" en Home, emparejado con el proveedor | 165 / 77 | Feature gordo + 3 idiomas nuevos (tr, zh-CN, zh-TW). No tenemos `core/trending/` en absoluto. Conflicto duro en `HomeScreen.kt` (nuestro Home está rediseñado entero). |
| `34c65c8` | Metadata: cupo por instalación, fotos de reparto, control de acceso al Worker | 51 / 35 | ⚠️ **Choca de frente con tu WIP sin commitear** (`MetadataMatchScorer.kt`, `TmdbProvider.kt`, `MetadataRepository.kt`). Dejar para el final, después de commitear lo tuyo. |
| `c6afd38` | Simplifica el estado de éxito del test DNS | 27 / 26 | Cosmético. Depende de `e1d6416`. 26 de 27 ficheros son traducciones. |
| `2ea168f` | Separa los Cloudflare Workers en copia pública self-host + privada | 10 / 0 | Mueve `worker/` → `extras/worker/`. Solo importa si te auto-hospedas los Workers. |

### 🔴 ROJO — chocan de frente con el rediseño de SteadfastTV

| SHA | Qué hace | files / overlap | Por qué choca |
|---|---|---|---|
| `91ad324` | Personalización global de tipografía (selector de fuentes) | 58 / 33 | Nuestro fork estandarizó **Figtree** y borró Lora a propósito (`a96fdda`, `e7924e1`, `9611561`). Este commit reintroduce el selector y las fuentes OFL. |
| `5ba3c65` | Rehace el material "Liquid Glass" y el renderizado del foco | 85 / 53 | Nuestro fork sustituyó el foco por **anillo blanco sin acento** (`ceb1f92`, `150dedb`). Direcciones opuestas. |
| `f9a3f3d` | Upgrade completo de UI y glass | 84 / 40 | Continuación del anterior. |

### ⚪ SKIP — bookkeeping de upstream

| SHA | Qué | Por qué no |
|---|---|---|
| `9dcda6b` | Fecha v4.2.1 en el changelog | Versionado de upstream, no nuestro. |
| `1afdc59`, `26e6aea` | Ediciones del README (badge de Weblate) | Nuestro README es de nuestro proyecto. |
| `efe2d76` | **Falla el build** si hay texto hardcodeado sin traducir | Añade un gate en `app/build.gradle.kts`. Nuestros 102 commits de rediseño metieron mucha UI nueva: esto rompería el build hasta hacer una pasada de i18n completa. Diferir. |
| `29445d1` | Clasifica el inventario de literales y corre el ratchet en main | Mismo motivo; además nuestro fork borró strings (`80299fe`), así que el baseline no cuadra. |

---

## 4. Orden recomendado de aplicación

Uno por uno, compilando entre medias. **No hagas `git merge upstream/main`**: son 181 ficheros
divergentes, el merge es inmanejable.

```bash
# 0. Primero: commitea tu WIP de metadata. 34c65c8 lo pisa.
git add -A && git commit -m "..."

# 1. Verdes de bajo riesgo, en este orden
git cherry-pick f1b1f1c    # Retry-After  ← el más valioso
git cherry-pick 58c8402    # foco PiP en la Guía
git cherry-pick e1d6416    # artwork + DNS

# 2. Toolchain: A MANO, no cherry-pick.
#    Editar gradle/libs.versions.toml:
#      kotlin      2.3.10 -> 2.3.21
#      ksp         2.3.9  -> 2.3.11
#      composeBom  2026.05.00 -> 2026.06.01
#      media3      1.10.1 -> 1.11.0
#    Editar gradle/wrapper/gradle-wrapper.properties: gradle-9.5.0 -> gradle-9.7.0
#    (agp se queda en 9.3.1, ya coincide con upstream)
./gradlew :app:assembleStandardRelease

# 3. Fixes de player (esperar conflictos SOLO en strings_*.xml de 20 idiomas)
git cherry-pick cd83d57    # surround
git cherry-pick b3a9fbf    # motor elegido
git cherry-pick c990182    # auditoría de playback

# 4. Features, si los quieres
git cherry-pick 710e3b5    # Stalker device ID  ← ojo con la migración de Room
git cherry-pick 8bf43f1    # zoom/volumen por ítem

# 5. Trending (paquete, o nada)
git cherry-pick 39f96af 6dd0baa

# 6. Metadata, lo último
git cherry-pick 34c65c8
```

Truco para los conflictos de traducción: en `values-*/strings_*.xml` casi siempre puedes
quedarte con la versión de upstream (`git checkout --theirs`), porque nosotros no hemos
traducido nada, solo hemos borrado claves huérfanas.

---

## 5. La decisión estratégica que hay pendiente

Hoy estamos **103 commits divergidos** de un upstream que sigue muy activo (v4.2.1, 325 estrellas,
último push hoy). Toda esa divergencia es de **rediseño visual**, no de funcionalidad. Eso
significa que cada release de upstream nos va a costar una sesión de cherry-picks como esta.

Dos caminos honestos:

**A. Seguir como estamos** (lo que asume este documento). Nos quedamos con el rediseño de
SteadfastTV como identidad del proyecto y vamos importando de upstream solo player/red/metadata.
Coste: recurrente, pero acotado — los ROJOS son solo 3 commits de 23.

**B. Rebasar sobre upstream y reaplicar solo el rediseño que de verdad quieras.** Si el rediseño
no te aporta lo suficiente, esta es la opción barata a largo plazo: te pones al día con upstream
de golpe y dejas de pagar el peaje. Coste: una reconstrucción grande ahora.

Sin decisión tomada. Por defecto vamos con **A**.

---

## 6. Estado del trabajo local sin commitear

WIP en el árbol al generar este documento (no lo pierdas):

```
M  core/database/OwnTVDatabase.kt, dao/MetadataDao.kt, dao/MovieDao.kt, entity/MetadataEntities.kt
M  core/metadata/MetadataProvider.kt, MetadataRepository.kt, TitleNormalizer.kt, TmdbProvider.kt
M  di/DatabaseModule.kt
M  features/home/HomeScreen.kt, movies/MovieViewModel.kt, movies/MoviesScreen.kt
M  features/shell/components/MediaDetailsScreen.kt
M  res/values/strings_content.xml
?? app/schemas/.../29.json
?? core/metadata/MetadataMatchScorer.kt  (+ su test)
```

Colisiona con `34c65c8` y `6dd0baa`. Commitear antes de tocar esos dos.

### ⚠️ Trampa: el emulador está en schema 29, HEAD está en 28

El WIP sube la base de datos de Room a `version = 29`
(`metadata_cache.spokenLanguagesJson`). El AVD `Television_1080p` ya tiene esa versión escrita en
disco, con la playlist IPTV real dentro.

Instalar encima **cualquier** APK construido desde `HEAD` (schema 28) revienta al arrancar:

```
IllegalStateException: A migration from 29 to 28 was required but not found
```

Room no baja de versión. No es corrupción — los datos siguen ahí — pero la app no abre hasta que
instalas otra vez un APK de schema ≥ 29.

**Nunca resolverlo con `adb uninstall` ni "borrar datos": eso sí se lleva la playlist por delante.**
La salida correcta es reinstalar desde el árbol de trabajo:

```bash
./gradlew :app:assembleX86_64Debug          # x86_64 porque el AVD es x86_64, no arm
adb install -r app/build/outputs/apk/x86_64/debug/app-x86_64-debug.apk
adb shell am start -n tv.own.owntv/.MainActivity
```

Lo mismo aplica al hacer cherry-pick de `710e3b5` (Stalker device ID), que trae su propia
migración de Room: hay que reconciliarla con la 29 antes de probar en el emulador.

---

## 7. CI y firma (estado actual)

- `.github/workflows/android.yml` construye APK dev en cada push a `main` y publica Release en tags `v*`.
- Lleva un gate **Verify Google TV Streamer 4K compatibility** que falla el build si el APK no
  llevaría arm64-v8a, minSdk > 34, targetSdk < 23, una feature requerida que el aparato no tiene,
  sin actividad LEANBACK_LAUNCHER, o firma sin esquema v2/v3.
- Secretos configurados en `Josexv1/OwnTV`: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- **Keystore real: `F:\Developing\OwnTV-signing\owntv-release.jks`, contraseña en `password.txt` al lado.**
  Fuera del repo a propósito. Si se pierde, no se pueden publicar actualizaciones sobre las releases
  ya instaladas — hacer copia.

Sacar una release:

```bash
git tag v4.3.0 && git push origin v4.3.0
```

El APK arm (`OwnTV-v4.3.0.apk` + `OwnTV.apk` de nombre fijo) es el que va al Streamer 4K.
