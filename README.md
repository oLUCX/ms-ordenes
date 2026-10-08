# ms-ordenes

Microservicio de **gestión del ciclo de vida del pedido** del caso BodegaNube (JVY0101). Recibe los avisos
de venta, evita duplicados por reintentos y controla los estados de cada orden hasta que se despacha.

| | |
|---|---|
| Puerto | `8082` |
| Base de datos | PostgreSQL `bodeganube_ordenes` (una base por microservicio) |
| Stack | Java 17, Spring Boot 3.2, Spring Data JPA, Bean Validation, Maven Wrapper |

## Requerimientos que cubre
- **RF-02**: recepción de avisos de venta externos (webhook que llega vía Lambda y SQS).
- **RF-03**: consulta de las órdenes disponibles para picking.
- **RF-06**: cada comercio consulta solo sus propias órdenes (Tenant Isolation).
- **RNF-02**: idempotencia. Un reintento del mismo aviso no crea una segunda orden (*Idempotent Receiver*).

## Ciclo de vida de una orden
```
PENDIENTE_STOCK ──► LISTA_PARA_PICKING ──► EN_PICKING ──► DESPACHADA
       └──────────► RECHAZADA_SIN_STOCK
```
Las transiciones permitidas están en `EstadoOrden.puedeCambiarA`. Cualquier otra responde 409.
Los ítems solo se pueden editar en `PENDIENTE_STOCK`, y una orden `EN_PICKING` o `DESPACHADA` no se elimina.

## Arquitectura en capas
```
src/main/java/com/bodeganube/ordenes
├── controller/   OrdenController             recibe HTTP, valida con @Valid y delega al service
├── service/      OrdenService                idempotencia, reglas de estado y transacciones
├── repository/   OrdenRepository             Spring Data JPA, con @EntityGraph para evitar N+1
├── model/        Orden, OrdenItem, EstadoOrden  entidades JPA y estados
├── dto/          *Request / *Response        contrato JSON de la API, separado de las entidades
└── exception/    GlobalExceptionHandler      todos los errores con el mismo formato JSON
```
La API responde siempre con DTOs: devolver la entidad directamente hacía que Jackson entrara en un ciclo
infinito (`Orden → items → orden → ...`).

## Modelo de datos
Relación **uno a muchos** entre `ordenes` y `orden_items`. Hibernate crea las tablas al iniciar.

| Tabla | Columna | Restricciones |
|---|---|---|
| ordenes | id | PK, autoincremental |
| ordenes | external_order_id | NOT NULL, UNIQUE: clave de idempotencia |
| ordenes | comercio_id | NOT NULL |
| ordenes | estado | NOT NULL (enum como texto) |
| ordenes | tracking_number, fecha_creacion | fecha NOT NULL |
| orden_items | id | PK, autoincremental |
| orden_items | orden_id | NOT NULL, **FK → ordenes.id** |
| orden_items | producto_id, cantidad | NOT NULL. producto_id es el SKU de ms-inventario |

En código: `Orden` tiene `@OneToMany(mappedBy = "orden", cascade = ALL, orphanRemoval = true)` y
`OrdenItem` tiene `@ManyToOne(fetch = LAZY, optional = false)` con `@JoinColumn(name = "orden_id")`.
Los ítems se guardan y se borran junto con su orden.

## Endpoints
Base: `http://localhost:8082` (o `http://localhost:8080` a través de ms-gateway).

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| POST | `/api/ordenes` | Recibe un aviso de venta | 201 con `Location`; 200 si es un reintento; 400 |
| GET | `/api/ordenes` | Lista todas; `?comercioId=X` filtra por comercio | 200 |
| GET | `/api/ordenes/{id}` | Obtiene una orden con sus ítems | 200, 404 |
| GET | `/api/ordenes/disponibles-picking` | Órdenes en `LISTA_PARA_PICKING` | 200 |
| PUT | `/api/ordenes/{id}` | Reemplaza los ítems: `{"items": [...]}` | 200, 400, 404, 409 |
| PATCH | `/api/ordenes/{id}/estado` | Cambia el estado: `{"estado": "LISTA_PARA_PICKING"}` | 200, 400, 404, 409 |
| DELETE | `/api/ordenes/{id}` | Elimina la orden | 204, 404, 409 si ya está en bodega |

Ejemplo de aviso de venta:
```json
{
  "externalOrderId": "shopify-evt-001",
  "comercioId": "comercio-123",
  "items": [{ "productoId": "SKU-1", "cantidad": 2 }]
}
```

Todas las respuestas de error tienen el mismo formato:
```json
{
  "timestamp": "2026-10-08T18:30:12.448",
  "status": 409,
  "error": "Conflict",
  "mensaje": "No se puede pasar una orden de EN_PICKING a PENDIENTE_STOCK",
  "ruta": "/api/ordenes/1/estado"
}
```
En los errores de validación (400) se agrega `detalles`, con el mensaje de cada campo inválido.

## Levantar el servicio desde cero

### 1. Requisitos
- JDK 17 o superior (`java -version`).
- PostgreSQL 14 o superior en `localhost:5432` (usuario y contraseña `postgres` por defecto).
- Git. **No hace falta instalar Maven**: el repositorio trae el Maven Wrapper (`mvnw` y `mvnw.cmd`).

### 2. Clonar el repositorio
```bash
git clone https://github.com/oLUCX/ms-ordenes.git
cd ms-ordenes
```

### 3. Crear la base de datos
Con psql:
```bash
psql -U postgres -c "CREATE DATABASE bodeganube_ordenes;"
```
O en pgAdmin: clic derecho en *Databases* → *Create* → *Database...* → `bodeganube_ordenes`.
Las tablas las crea la aplicación al iniciar.

Si no tienes PostgreSQL instalado, puedes usar Docker:
```bash
docker run -d --name bodeganube-db -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16
docker exec bodeganube-db psql -U postgres -c "CREATE DATABASE bodeganube_ordenes;"
```

### 4. Compilar, probar y empaquetar
```bash
.\mvnw.cmd clean package    # Windows (PowerShell)
./mvnw clean package        # Linux, macOS o Git Bash
```
Descarga las dependencias, compila, corre las pruebas automatizadas y genera `target/ms-ordenes.jar`.

### 5. Ejecutar
```bash
java -jar target/ms-ordenes.jar
```
También se puede levantar sin empaquetar con `.\mvnw.cmd spring-boot:run`. Queda escuchando en
`http://localhost:8082`.

### 6. Probar con Postman
Importa `postman/ms-ordenes.postman_collection.json` (*Import* → archivo) y usa *Run collection*.
Ejecuta en orden 16 peticiones (aviso de venta, reintento idempotente, consultas, edición, cambios de
estado y eliminación, más los casos de error 400, 404 y 409) y cada una verifica su código HTTP. Se puede
repetir: el externalOrderId cambia en cada ejecución.

Para ver los datos en la base:
```sql
SELECT o.id, o.external_order_id, o.estado, i.producto_id, i.cantidad
FROM ordenes o JOIN orden_items i ON i.orden_id = o.id;
```

## Configuración
Todo tiene un valor por defecto para desarrollo local y se puede cambiar con variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `PORT` | `8082` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/bodeganube_ordenes` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |

## Pruebas automatizadas
`.\mvnw.cmd test` corre 15 pruebas que no necesitan base de datos:
- `OrdenServiceTest` (JUnit 5 + Mockito): idempotencia, transiciones de estado y reglas de edición y borrado.
- `OrdenControllerTest` (MockMvc): 201 frente a 200 en reintentos, validaciones, 404, 409 y formato de error.

## Ramas
`main` tiene la versión entregada, `develop` integra el trabajo en curso y cada cambio entra desde una
rama `feature/...` con un Pull Request hacia `develop`.

## Próximos pasos
- Reservar stock en ms-inventario al recibir la orden, con Circuit Breaker y Retry (Resilience4j).
- Publicar un evento en una cola (SQS) cuando la orden queda `LISTA_PARA_PICKING`, en vez de llamar
  directo a ms-picking-despacho (feedback de la Evaluación 1).
- Si no hay stock, notificar el rechazo al comercio con un webhook de retorno.
