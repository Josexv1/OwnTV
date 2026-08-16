# Cinematic Movies — especificación

> Archivo local, gitignored. Sacado del código de la implementación anterior
> (`main:features/movies/MoviesScreen.kt`, 1.915 líneas), no de memoria.
>
> Se reconstruye **desde cero sobre upstream**, no se porta. Upstream no tiene
> `MediaListRow`, `CategoryHeader` ni `MediaContextMenu`: se usan sus componentes
> (`FocusableSurface`, `OwnTVButton`, `dialogPanel`).

---

## Qué es

Una segunda presentación para Películas. La clásica son tres paneles
(categorías | lista | vista previa). La cinemática es un **carril + lista a ancho
completo**, y al abrir un título una **ficha a sangre**, estilo Prime Video.

Se elige en Ajustes → "Movies layout": `Classic panels` (por defecto) o `Cinematic`.
El ajuste es nuestro; upstream no lo tiene. Clave DataStore: `movies_layout_mode`.

---

## Fases

| Fase | Contenido | Estado |
|---|---|---|
| 1 | La pantalla: hero a sangre, póster, textos, acciones | ⏳ |
| 2 | Carril de películas similares | hecho (fc3bd5a) |
| 3 | Pills de descubrimiento: géneros y reparto | pendiente |
| 4 | Normalización de nombres | pendiente |

---

## Fase 1 — la pantalla

**Fondo.** Backdrop de TMDB a sangre (`w1280`), fijo. Si no hay, el del proveedor.
Degradado de dos ejes para que el texto se lea encima.

**Sin scroll vertical.** El hero es de altura fija. Atrás lo maneja el `BackHandler`
del padre. Nada de página larga.

**Columna izquierda.** Póster con tamaño fijo. Precedencia según el modo de metadata:
en `TMDB_ONLY` gana TMDB, si no gana el del proveedor.

**Columna derecha**, con la misma altura que el póster para que las acciones queden
ancladas abajo y el plot flexione en medio:

1. **Título como texto, siempre.** Nunca el logo de TMDB — suelen ser marcas
   gráficas (la A de Avengers), no el título. El nombre del proveedor normalizado
   gana, para conservar títulos localizados ("17 otra vez" y no "17 Again"); TMDB
   es el respaldo si el del proveedor está vacío o el modo es TMDB-only.
2. **Línea meta:** año · nota · duración · calidad.
3. **Chips de género** (fase 3: clicables).
4. **Línea de subtítulos:** solo idiomas de OpenSubtitles ya descargados. NO el
   "idioma original" de TMDB — no es lo que trae el archivo y confundía.
5. **Aviso de reanudación** si hay progreso.
6. **Sinopsis**, en el hueco flexible: 3 líneas (2 si el carril de similares está
   levantado), con elipsis. Nunca debe comprimir las acciones de abajo.
7. **Reparto** (fase 3: nombres subrayados y clicables). Se oculta cuando el carril
   de similares sube, para dejar sitio vertical.
8. **Fila de acciones**, iconos compactos anclados abajo: Reanudar/Reproducir,
   Tráiler, Favorito, Descargar. Con tooltip cada uno.

**Foco.** Abajo desde *cualquier* acción entra en el carril de similares por el
primer póster (fase 2) — no por el póster que caiga bajo esa columna.

---

## Fase 2 — carril de similares

- Asoma por debajo del hero; al enfocarlo **sube y tapa la parte baja** (Prime-like).
- **Scroll infinito**, no 12 pósters: recomendaciones de TMDB y, al agotarse, su lista
  `similar`. Obliga a `LazyRow`, con lo que vuelve el problema del `FocusRequester` del
  ítem 0 al descartarse. Se resuelve rebobinando la fila al perder el foco: así el ítem 0
  siempre está compuesto mientras el carril no tiene el foco.
- Pósters algo más pequeños mientras asoma, para que el hero siga dominando.
- Arriba desde cualquier póster vuelve a la acción principal (Reproducir/Reanudar).
- Pulsación larga sobre un póster: busca ese título en todas las listas y carpetas
  (otras copias, otros idiomas).

## Fase 3 — descubrimiento

Tres overlays sobre la misma rejilla paginada:

| Overlay | Se abre desde | Vacío |
|---|---|---|
| Género | chip de género | "No matching movies in your library yet" |
| Reparto | nombre subrayado | "No matching movies in your library for this cast member yet" |
| Todas las copias | pulsación larga en similar | "No copies found across your playlists" |

Cruzan datos de TMDB con el catálogo local: solo se muestra lo que se puede reproducir.

## Fase 4 — normalización de nombres

`TitleNormalizer` limpia "NF -", "4K", "(2009)" y demás ruido del proveedor para
generar el título mostrado y la consulta de búsqueda.

Ojo, bug conocido de la implementación anterior: se comía paréntesis en cualquier
posición, así que "(Un)lucky Sisters" se mostraba como "lucky Sisters". Al rehacerlo,
quitar solo lo del final (año/calidad), no los paréntesis iniciales que son parte del
título.
