---
name: CFDI Atípicos
description: Dashboard de detección de riesgo fiscal en CFDI, en CSS puro (sin frameworks de UI)
colors:
  primary: "#4338ca"
  secondary: "#64748b"
  success: "#15803d"
  warning: "#b45309"
  orange: "#c2410c"
  danger: "#b91c1c"
  info: "#0f6a7c"
  gray-50: "#f8fafc"
  gray-100: "#f1f5f9"
  gray-200: "#e2e8f0"
  gray-300: "#cbd5e1"
  gray-400: "#94a3b8"
  gray-500: "#64748b"
  gray-600: "#475569"
  gray-700: "#334155"
  gray-800: "#1e293b"
  gray-900: "#0f172a"
  white: "#ffffff"
  primary-hover: "#3730a3"
  danger-hover: "#991b1b"
  alert-danger-text: "#7f1d1d"
  alert-warning-text: "#78350f"
  alert-info-text: "#164e5a"
  alert-success-text: "#14532d"
typography:
  display:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "2.5rem"
    fontWeight: 600
  title:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1.375rem"
    fontWeight: 600
  subtitle:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
  subtitle-sm:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 600
  display-sm:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 600
  display-xs:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 600
  body:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.5
  control:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
  small:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
  label:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
  heading:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontWeight: 600
  mono:
    fontFamily: "ui-monospace, SFMono-Regular, Consolas, Liberation Mono, monospace"
rounded:
  sm: "8px"
  md: "10px"
  lg: "12px"
  pill: "999px"
spacing:
  sm: "8px"
  md: "16px"
  lg: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#ffffff"
    rounded: "{rounded.sm}"
  badge-nivel-critico:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.danger}"
    rounded: "{rounded.pill}"
---

# Design System: CFDI Atípicos

## Overview

**Creative North Star: "The Quiet Ledger"**

CFDI Atípicos es una herramienta de auditoría fiscal, no un producto de consumo: contadores y responsables financieros la usan para encontrar comportamientos atípicos en facturación electrónica antes de que el SAT los cuestione. El sistema visual se comporta como un libro mayor bien lleva­do — grises fríos neutros, un único acento (índigo) reservado para acción e identidad, y superficies planas con borde fino en vez de sombra pesada. El color nunca decora; cuando aparece saturado (verde/ámbar/naranja/rojo) es siempre una señal de riesgo real, nunca un adorno de marca. Se rechazó explícitamente: iconografía gigante centrada como contenido de página, tarjetas idénticas repetidas como estructura, bordes de color grueso como adorno de "alerta", y cualquier sombra con aspecto de neón o vidrio.

**Key Characteristics:**
- Grises fríos ("slate") como estructura; índigo como único acento de marca.
- Riesgo codificado en tinte suave (`-subtle`/`-emphasis`), nunca en relleno sólido saturado.
- Superficies planas: borde de 1px en reposo, sombra solo en overlays (modal, dropdown, drawer móvil).
- Datos numéricos en `tabular-nums`; identificadores (UUID, RFC, código de criterio) en monoespaciada.
- Un solo tema claro (sin modo oscuro, por decisión explícita del usuario).
- **Sin framework de UI.** CSS puro: `src/styles/tokens.css` (variables) + `src/styles/global.css` (reset, utilidades, componentes) para todo lo compartido; CSS Modules (`*.module.css`) solo donde un componente necesita estilos verdaderamente propios (Modal, AppLayout). Cero Bootstrap, cero Tailwind, cero librería de componentes. Iconos: `lucide-react` (SVG, un solo trazo/peso).

## Colors

Paleta restringida: neutros fríos para toda la estructura, un acento de marca, y cuatro semánticos de riesgo que se mantienen reconocibles en cualquier redondeo futuro del sistema.

### Primary
- **Índigo editorial** (`#4338ca`): único acento de marca. Botones primarios, enlaces, ítem de navegación activo, foco de formularios, iniciales de avatar. The One Accent Rule: el índigo es el único color no semántico del sistema; si un elemento necesita destacar y no es una acción ni una identidad, se resuelve con peso tipográfico o espaciado, nunca con un color nuevo.

### Neutral
- **Slate 50–900** (`#f8fafc` → `#0f172a`): fondo de página (`gray-50`), superficies de tarjeta (blanco), bordes (`gray-200`), texto secundario (`gray-500`/`600`), texto de cuerpo (`gray-700`), encabezados (`gray-900`).

### Semánticos de riesgo
- **BAJO — verde** (`#15803d`): hallazgo de bajo riesgo, estatus "corregido".
- **MODERADO — ámbar** (`#b45309`): hallazgo de riesgo moderado.
- **ALTO — naranja** (`#c2410c`): hallazgo de riesgo alto. Color de tema personalizado agregado al mapa de Bootstrap (no existe por defecto).
- **CRÍTICO — rojo** (`#b91c1c`): hallazgo crítico, eliminación de datos, acciones irreversibles.

### Named Rules
**The Tinted Signal Rule.** Todo indicador de riesgo o estado (badge de nivel, estatus, estado de importación) usa el par de clases propias `bg-{color}-subtle` / `text-{color}-emphasis` definidas en `global.css`, nunca un relleno sólido `bg-{color}`. La saturación completa se reserva para acciones destructivas explícitas (botón de eliminar, confirmación de borrado total).

## Typography

**Body & Heading Font:** Inter (con `system-ui` como respaldo) — autohospedada vía `@fontsource/inter`, pesos 400/500/600/700.
**Mono Font:** pila `ui-monospace` del sistema, usada solo para identificadores reales (UUID, RFC, código de criterio como `M04`), nunca como adorno "técnico".

**Character:** Inter es la voz de trabajo de un dashboard Operate: neutra, muy legible en tamaños pequeños, sin personalidad propia que compita con los datos fiscales que son el verdadero contenido.

### Hierarchy
- **Heading** (600, escala de Bootstrap `h3`–`h6`): títulos de página y de tarjeta.
- **Body** (400, 15px / 0.9375rem, 1.5): texto general; densidad intencional de dashboard, no la base de 16px por defecto.
- **Label** (400–500, `.small`/13px): etiquetas de campo, texto secundario, metadatos de tabla.
- **Numeric** (`font-variant-numeric: tabular-nums`): toda celda de tabla con cifras (montos, operaciones, porcentajes) para que las columnas alineen visualmente.

## Layout

Shell de aplicación de dos columnas: sidebar fijo de 248px (oculto bajo 992px, reemplazado por un drawer deslizable controlado con `useState` en `AppLayout.jsx`, no por JS de un framework) + topbar delgado de 56px + contenido en `main` con padding 1–1.5rem. Las páginas internas usan un mini-sistema de rejilla propio (`.row` + `.col-6`/`.col-md-*`/`.col-lg-*` + `.g-2`/`.g-3`, flexbox con `flex-basis` porcentual) para tarjetas de estadística y paneles; las tablas de datos ocupan el ancho completo de su tarjeta contenedora con `table-responsive`. Sin contenedor centrado de ancho fijo: el dashboard usa el viewport completo, a diferencia de una página de marketing.

## Elevation & Depth

**The Flat-By-Default Rule.** Las superficies de contenido (tarjetas, filas de tabla, sidebar) son planas en reposo: un borde de 1px (`gray-200`), sin `box-shadow`. La sombra se reserva para elementos que genuinamente flotan sobre el contenido — modal, dropdown, offcanvas — con una sombra suave de doble capa (difusa + de contacto), nunca un halo de color ni un offset duro.

### Shadow Vocabulary
- **lg** (`var(--shadow-lg)` = `0 10px 25px -5px rgba(15,23,42,.12), 0 6px 10px -6px rgba(15,23,42,.08)`): modal, dropdown-menu, drawer móvil. Es la única sombra del sistema.

### Named Rules
**The One Entrance Rule.** Todo overlay (modal, dropdown, drawer móvil) entra con la misma curva — `cubic-bezier(0.16, 1, 0.3, 1)` (exponencial, ease-out), 120–220ms, fade + leve `translateY`/`scale` u opcionalmente `slide` para el drawer. Nunca una curva o duración distinta por componente. El contenido de página hace fade+rise sutil (`content-fade-in`, 200ms) solo al cambiar de sección de primer nivel, nunca al cambiar de pestaña dentro de un mismo contribuyente (eso remontaría `ContribuyenteLayout` y repetiría su fetch). Botones dan feedback táctil con `transform: scale(0.97)` al presionar. Todo lo anterior se desactiva automáticamente bajo `prefers-reduced-motion: reduce`.

## Shapes

Radio consistente de 10px en tarjetas y contenedores grandes, 8px en botones/inputs/badges-pill, radio completo (pill) en badges de estado y avatares. Bordes de 1px en `gray-200` para toda separación estructural; nunca un borde de color de más de 1px sobre tarjetas, filas o alertas (reservado exclusivamente para la codificación de riesgo en celdas de malla/matriz, que es contenido, no decoración de contenedor).

## Components

Todos los componentes de abajo viven como clases planas en `src/styles/global.css` (mismos nombres de clase familiares — `btn`, `card`, `badge`, `table`, `form-control`, `nav-pills`…, pero CSS propio desde cero, sin import de ningún framework). Los únicos componentes con CSS Module dedicado son los que tienen estructura verdaderamente propia: `Modal.module.css` (modal + confirmación) y `AppLayout.module.css` (sidebar/drawer/topbar shell).

### Buttons
- **Shape:** radio 8px, peso de fuente 500.
- **Primary:** fondo índigo `#4338ca`, texto blanco.
- **Outline / Secondary:** borde `gray-300`, texto `gray-700`; usado para toda acción no primaria (descargar, editar, cancelar).
- **Hover / Focus:** anillo de foco índigo translúcido (`var(--primary-ring)`), vía `box-shadow` en `:focus`.

### Badges
- **Style:** `rounded-pill`, par `bg-{color}-subtle`/`text-{color}-emphasis`.
- **Identificadores** (código de criterio, UUID corto): no son un badge semántico — chip con borde neutro, texto `text-secondary`, tipografía monoespaciada.

### Cards / Containers
- **Corner Style:** 10px (`var(--radius-md)`).
- **Background:** blanco sobre fondo de página `gray-50`.
- **Shadow Strategy:** ninguna (ver Elevation & Depth); el borde de 1px es la única separación visual.
- **Internal Padding:** `.card-body` = 1.25rem; paneles de estadística con padding propio y divisores internos (`border-start` 1px entre columnas de un mismo panel) en vez de tarjetas repetidas separadas.

### Inputs / Fields
- **Style:** borde `gray-300`, fondo blanco, radio 8px.
- **Focus:** borde + anillo índigo translúcido.

### Modals & Drawer
- Controlados 100% con estado de React (`useState` + `useEffect` para Escape/click-outside), sin dependencia de ningún framework de UI para abrir/cerrar. `Modal`/`ConfirmModal` comparten `Modal.module.css`; el drawer móvil vive en `AppLayout.module.css`.

### Navigation
- **Sidebar:** fondo blanco (no oscuro), ítem activo = `bg-primary-subtle` + texto índigo + peso 600, ítem inactivo = texto gris secundario. Sin filled-pill sólido ni barra lateral de color en el ítem activo.
- **Topbar:** blanco, borde inferior 1px, avatar de iniciales (círculo índigo-sutil) en vez de un ícono de persona genérico. El menú de usuario es un dropdown propio (`useState` + click-outside), no un componente de framework.
- **Tabs internas** (dentro de un contribuyente): `.nav-pills`, activo = fondo índigo sólido (contraste suficiente al ser un control de navegación secundario, no una tarjeta de contenido).

### Icons
- `lucide-react` exclusivamente: SVG, 14–20px según contexto, un solo trazo/peso. Nunca Unicode/emoji como ícono.

## Do's and Don'ts

### Do:
- **Do** usar los pares `bg-{color}-subtle`/`text-{color}-emphasis` ya definidos en `global.css` para cualquier badge o indicador de estado nuevo, en vez de inventar un nuevo par de colores.
- **Do** mantener los cuatro semánticos de riesgo (verde/ámbar/naranja/rojo) reconocibles como BAJO/MODERADO/ALTO/CRÍTICO en cualquier componente nuevo que muestre nivel de riesgo.
- **Do** aplicar `tabular-nums` a cualquier celda de tabla nueva que muestre cifras (ya es la regla global en `global.css`, se hereda automáticamente).
- **Do** usar monoespaciada solo para identificadores reales (UUID, RFC, código), nunca como estética "técnica" decorativa.
- **Do** añadir estilos nuevos y reutilizables a `global.css` como clases planas (mismo espíritu que las existentes); reservar un `*.module.css` solo para un componente con estructura genuinamente única.

### Don't:
- **Don't** reintroducir Bootstrap, Tailwind, o cualquier librería de componentes de UI — el sistema es deliberadamente CSS puro + CSS Modules.
- **Don't** usar `border-start`/`border-end` de más de 1px como decoración de "alerta" en tarjetas, filas o callouts — el color de riesgo vive en el badge o en la celda de datos, no en un borde de contenedor.
- **Don't** construir una página nueva como una fila de tarjetas idénticas de ícono + título + texto + botón; esa plantilla ya se reemplazó en Exportación y Lista 69-B por un patrón de lista/panel.
- **Don't** introducir una segunda familia tipográfica o un segundo acento de marca: Inter + índigo es la única voz del sistema.
- **Don't** agregar modo oscuro sin que el usuario lo pida explícitamente (decisión consciente: un solo tema claro, pulido).
- **Don't** usar un ícono de otra librería o un emoji; todo ícono nuevo sale de `lucide-react`.
