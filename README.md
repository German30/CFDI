# Detector de Comportamientos Atípicos en CFDI

API REST para cargar CFDI y metadata del SAT de uno o varios RFC, detectar operaciones atípicas, construir la malla mensual y la matriz de riesgo, y generar el resumen ejecutivo. Marca: Cultura Fiscal MX · Mario Beltrán.

El código está en [cfid/](cfid/). El índice que relaciona cada apartado de la especificación con su implementación está en [cfid/docs/INDICE-ESPECIFICACION.md](cfid/docs/INDICE-ESPECIFICACION.md).

## Requisitos

- JDK 21. Si `java -version` muestra otra versión, define `JAVA_HOME` antes de compilar.
- Maven 3.9 o el wrapper incluido.
- MySQL 8 en `localhost:3306`. La base `cfdi_atipicos` se crea sola.

Las pruebas usan H2 en memoria y no necesitan MySQL.

## Ejecución

```bash
cd cfid
mvn spring-boot:run     # API en http://localhost:8080
mvn test                # 19 pruebas
```

Prueba rápida con datos ficticios:

```bash
A=http://localhost:8080/api
T=$(curl -s -X POST $A/auth/registro -H 'Content-Type: application/json' \
  -d '{"nombre":"Demo","email":"demo@ejemplo.mx","password":"ClaveSegura123"}' | jq -r .accessToken)
curl -X POST "$A/demo?ejercicio=2024&operacionesBase=600" -H "Authorization: Bearer $T"
curl "$A/consolidado" -H "Authorization: Bearer $T"
```

## Configuración

Todo valor de `application.properties` se puede sustituir con una variable de entorno. Fuera de un equipo de desarrollo, define al menos estas:

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexión a MySQL |
| `APP_JWT_SECRET` | Clave de firma de los tokens |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Administrador inicial |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos para el frontend |
| `APP_RESPALDO_INTERVALO_MS`, `APP_RESPALDO_MAX_VERSIONES` | Respaldo automático (60,000 ms y 10 versiones) |
| `APP_RESPALDO_DIRECTORIO` | Opcional: carpeta donde también se escribe `RFC_ejercicio.json` |

## API

Todas las rutas requieren `Authorization: Bearer <accessToken>`, salvo `login`, `registro` y `refresh`. Cada usuario solo ve sus contribuyentes; un recurso ajeno responde 404.

`{id}` es el contribuyente. `ejercicio` es opcional en las rutas de análisis: si se omite, se usa el del contribuyente.

| Método y ruta | Función |
|---|---|
| `POST /api/auth/registro`, `/login`, `/refresh` · `GET /api/auth/me` | Autenticación |
| `GET, POST /api/contribuyentes` · `GET, PUT, DELETE /api/contribuyentes/{id}` | Alta, edición y baja de RFC |
| `GET /api/consolidado` | Comparativo entre RFC |
| `POST /api/contribuyentes/{id}/importaciones/xml` | Carga XML o .zip (campo `archivos`); responde 202 con el trabajo |
| `POST /api/contribuyentes/{id}/importaciones/metadata` | Carga metadata .txt o .zip (campo `archivos`) |
| `GET /api/contribuyentes/{id}/importaciones[/{jobId}[/errores]]` | Historial, avance y bitácora |
| `GET /api/contribuyentes/{id}/conciliacion` | Tablero XML contra metadata |
| `GET /api/contribuyentes/{id}/malla[/detalle]` | Malla y CFDI de una celda; filtros `tipo`, `direccion`, `estatus`, `contraparte`, `claveProdServ`, `mes` |
| `GET /api/contribuyentes/{id}/criterios` · `PUT .../criterios/{codigo}` | Catálogo con fundamento, activación y umbrales |
| `POST, GET /api/lista-69b` | Importa el .csv del art. 69-B (campo `archivo`) |
| `POST /api/contribuyentes/{id}/analisis` | Ejecuta el análisis |
| `GET /api/contribuyentes/{id}/hallazgos[/{hid}]` · `PATCH .../hallazgos/{hid}` | Hallazgos y estatus de seguimiento |
| `GET /api/contribuyentes/{id}/matriz` · `GET, PUT .../parametros-riesgo` | Matriz 5 × 5 y ponderaciones |
| `POST, GET, PUT /api/contribuyentes/{id}/resumen` | Genera, consulta y edita el resumen |
| `GET /api/contribuyentes/{id}/exportar/pdf`, `/excel` | Exportaciones |
| `GET, POST /api/contribuyentes/{id}/respaldos` | Último respaldo, versiones y respaldo manual |
| `GET .../respaldos/descarga`, `.../respaldos/{rid}/descarga` | Descarga del JSON |
| `POST .../respaldos/restauracion`, `.../respaldos/{rid}/restauracion` | Restauración validada |
| `POST, DELETE /api/datos/eliminacion` | Borrado total: pide un token y lo confirma con la frase `ELIMINAR TODO` |
| `POST /api/demo` | Carga contribuyentes ficticios con casos atípicos |

Flujo habitual: dar de alta el RFC, cargar XML y metadata, consultar el avance, ejecutar `analisis`, revisar malla y hallazgos, generar `resumen` y exportar.
