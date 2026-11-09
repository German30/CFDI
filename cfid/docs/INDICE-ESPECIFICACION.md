# Índice de la especificación

Relaciona cada apartado de `README-ERP-UPDATE.txt` con su implementación. Las rutas de código son relativas a `src/main/java/mx/culturafiscal/atipicos/`.

## Decisión de arquitectura

La especificación describe un HTML autocontenido, sin servidor, donde ningún dato sale del equipo. Este proyecto es un backend Spring Boot con MySQL y JWT, por lo que la especificación se tomó como requisitos funcionales. **No hay interfaz gráfica en este repositorio**: la API expone los datos para un frontend separado.

| Requisito original | Implementación en el backend |
|---|---|
| Procesamiento local, sin servidor | Servidor propio. Los datos sí salen del navegador hacia el servidor |
| IndexedDB | MySQL |
| Web Workers con barra de progreso | Pool `importExecutor` y consulta del avance del trabajo |
| JSZip, SheetJS, Chart.js | `java.util.zip`, Apache POI y OpenPDF; las gráficas corresponden al frontend |
| Archivo elegido con File System Access API | `app.respaldo.directorio`, en el servidor |
| Pestaña «Ayuda» | [README](../../README.md) y `GET .../criterios`, con el fundamento de cada criterio |
| Sin usuarios | Cuentas con JWT y aislamiento de datos por usuario |

## Cobertura por apartado

Estado: **Completo** (implementado y probado), **Parcial** (ver nota) o **Pendiente** (requiere frontend).

| § | Tema | Estado | Código | Nota |
|---|---|---|---|---|
| 1 | Requisitos técnicos | Parcial | Ver tabla anterior | Diseño responsivo y temas corresponden al frontend |
| 2 | Multi-RFC | Completo | `contribuyente/`, `consolidado/` | |
| 3 | Carga y clasificación | Completo | `cfdi/CfdiXmlParser`, `importacion/` | También lee CFDI 3.3. Protegido contra XXE y bombas zip |
| 4 | Metadata y conciliación | Completo | `metadata/`, `conciliacion/` | Incisos a) a f). La última carga de metadata prevalece |
| 5 | Malla mensual | Parcial | `malla/` | La API entrega `z` y `atipicidad` (0 a 3) por celda; dibujar el mapa de calor corresponde al frontend |
| 6.1 | Monto | Completo | `criterio/motor/DetectoresEstadisticos` | M01 a M05 |
| 6.2 | Número de operaciones | Completo | `criterio/motor/DetectoresEstadisticos` | F01 a F06 |
| 6.3 | Clave de producto o servicio | Completo | `criterio/motor/DetectoresClaves` | C01 a C05. C01 requiere declarar `clavesCongruentes` |
| 6.4 | Adicionales | Completo | `criterio/motor/DetectoresFiscales`, `DetectoresNominaTraslado` | A01 a A17, X01, N01 a N04, T01, T02 |
| 7 | Matriz de riesgo | Completo | `hallazgo/` | Ponderaciones editables; el estatus se conserva al reanalizar |
| 8 | Resumen ejecutivo | Completo | `resumen/`, `exportacion/` | Editable; PDF carta y Excel con las 5 hojas |
| 9 | Respaldo | Completo | `respaldo/`, `datos/` | Ver «Criterios de implementación» |
| 10 | Identidad visual | Parcial | `exportacion/` | Colores aplicados. El PDF usa Helvetica: Ysabeau SC no está incluida |
| 11 | Datos de prueba | Completo | `demo/` | 3 contribuyentes ficticios |
| 12 | Validación | Completo | `src/test/` | Ver «Validación» |

## Criterios de implementación

Decisiones que interpretan la especificación y conviene conocer al revisar resultados:

- **Importe en la malla:** Total × TipoCambio para I, E y N; monto de los pagos para P; cero para T. Los cancelados se muestran aparte.
- **Cancelados sin XML:** si solo constan en metadata, se incluyen en la malla usando el monto de metadata, que no se convierte a MXN.
- **Desviación estándar:** muestral (n−1), sobre los 12 meses. Un ejercicio en curso cuenta los meses sin operaciones como cero.
- **Plazo del art. 29-A CFF:** fin de marzo del año siguiente para RFC de 12 caracteres y fin de abril para RFC de 13.
- **M04:** exige superar 1.5 veces el P95, porque cerca del 5% de los CFDI rebasa el P95 simple por definición.
- **A04 (Benford):** el umbral es el mayor entre 0.015 y 2 veces la MAD esperada para el tamaño de la muestra.
- **A15 y A17:** comparan contra el comportamiento interno del contribuyente, no contra el DOF ni contra la LIVA.
- **T01:** no distingue las excepciones a la Carta Porte previstas en la RMF.
- **Respaldo automático:** revisa cada 60 s, pero solo crea versión si hubo cambios; así las copias idénticas no desplazan a las 10 versiones útiles.
- **Restauración:** sustituye los datos del RFC en una sola transacción y exige que el respaldo sea del mismo RFC.

## Validación

Resultado de `mvn test`: 19 pruebas, 0 fallas.

| Verificación del § 12 | Resultado |
|---|---|
| Carga de al menos 5,000 XML sin bloqueo | 7,944 XML: la petición respondió en menos de 40 ms y el proceso tardó unos 3.5 s en H2 |
| Totales de la malla contra la suma de los CFDI | Coinciden celda por celda con un cálculo independiente del generador |
| Cruce de cancelados | Conteos exactos, incluidos fuera de plazo, fuera de ejercicio y con pago |
| Respaldo y restauración | La malla queda idéntica; rechaza archivos inválidos, de otra versión o de otro RFC |
| Los 41 criterios | Cada uno detecta su caso sembrado |

También se ejecutó el .jar contra MySQL 8 con una base temporal: la carga de demostración de unos 8,000 XML con su análisis tardó 26 s; el análisis solo, 3 s; y la restauración, 21 s.

**Falsos positivos.** En 30 ejercicios sintéticos sin anomalías, ningún criterio fiscal o documental se disparó. Las pruebas estadísticas sí, como se espera de sus umbrales:

| Criterio | Casos en 30 ejercicios | Tasa aproximada |
|---|---|---|
| M01 Puntaje Z | 25 | 3.5% de los meses |
| M02 Rango intercuartílico | 23 | 3.2% de los meses |
| M03 Variación mensual | 27 | 4.1% de los meses |
| F04 Picos diarios | 2 | Ocasional |

## Pendiente de validar

- **Archivos reales del SAT:** todas las pruebas usan XML y metadata generados por este proyecto. Falta probar con descargas reales.
- **Fundamentos legales:** los artículos citados en el catálogo y el cálculo del plazo del art. 29-A deben revisarse por un especialista fiscal antes de usarse con clientes.
- **Temporizador del respaldo:** se probó la lógica llamando al método directamente, no esperando el ciclo de 60 s.
