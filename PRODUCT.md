# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Contadores, despachos contables y responsables fiscales/financieros que administran uno o varios RFC (contribuyentes) propios o de clientes, y necesitan detectar comportamientos atípicos en su facturación (CFDI) para anticipar riesgo fiscal frente al SAT.

## Product Purpose

Detectar y dar seguimiento a comportamientos atípicos en CFDI (facturación electrónica mexicana) mediante un motor de ~40 criterios estadísticos y fiscales, para que el usuario identifique, documente y corrija irregularidades antes de que el SAT las cuestione. Éxito = hallazgos accionables con evidencia clara (UUIDs, montos, periodo) y un resumen ejecutivo exportable.

## Positioning

A diferencia de un ERP contable genérico, el producto corre un motor propio de ~40 criterios (percentiles, z-scores mensuales, congruencia de claves de producto/servicio, patrones de nómina, Carta Porte, cancelaciones fuera de plazo, etc.) calibrados al marco de riesgo del SAT mexicano, y produce evidencia lista para auditoría o defensa fiscal (matriz de riesgo, resumen ejecutivo, exportación a PDF/Excel).

## Operating Context

Flujo típico de uso: alta de un RFC → carga masiva de XML de CFDI y metadata del SAT (archivos grandes, procesamiento asíncrono con jobs) → conciliación XML vs metadata → ejecución del motor de análisis → revisión y seguimiento de hallazgos (cambio de estatus, notas) → generación/edición de resumen ejecutivo → exportación a PDF/Excel para el cliente o la autoridad. Los datos son fiscales reales y sensibles de los contribuyentes del usuario; el uso es recurrente (revisión mensual/anual) y multi-RFC (un contador gestiona varios contribuyentes a la vez).

## Capabilities and Constraints

- Backend ya construido (Spring Boot + MySQL) con contrato de API fijo y documentado; el frontend debe seguir consumiéndolo tal cual (JWT, multipart uploads, descargas binarias con `Content-Disposition`, polling de jobs asíncronos).
- Aislamiento multi-tenant estricto por usuario (cada RFC pertenece a un usuario).
- Sin framework ni librería de componentes de UI (decisión explícita del usuario, tras un primer intento con Bootstrap que se descartó): CSS puro + CSS Modules, iconos `lucide-react`. Ver DESIGN.md.
- Sin generación de imágenes disponible en este entorno: cualquier elemento visual se construye con CSS/SVG, no con imágenes generadas.

## Brand Commitments

Nombre del producto: "CFDI Atípicos" / "Detector de Comportamientos Atípicos en CFDI". Sin logotipo formal; la paleta (índigo + grises fríos) y tipografía (Inter) están fijadas en DESIGN.md.

## Evidence on Hand

No hay capturas de clientes reales. El frontend no incluye ninguna función de datos de demostración (se removió deliberadamente); cualquier contenido de ejemplo en el rediseño debe marcarse o inferirse como ilustrativo, nunca presentarse como dato real de un cliente.

## Product Principles

1. Claridad de riesgo antes que estética: cada hallazgo y celda de riesgo debe ser escaneable y accionable en segundos, sin decoración que compita con el dato.
2. Seriedad y confianza: es una herramienta de cumplimiento fiscal; el tono visual debe sentirse profesional y calmado, nunca lúdico o "producto de consumo".
3. Densidad de datos controlada: tablas, mallas y matrices grandes deben mantenerse legibles sin perder contexto ni forzar scroll excesivo.
4. Estado del sistema siempre visible: trabajo asíncrono (importación, análisis) debe comunicar su progreso y resultado sin ambigüedad.

## Accessibility & Inclusion

Sin requisito específico confirmado por el usuario. Dado que la herramienta sostiene decisiones fiscales delicadas, se mantiene como piso: buen contraste de texto, foco de teclado visible y tamaños de toque adecuados en controles interactivos.
