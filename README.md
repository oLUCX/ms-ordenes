# ms-ordenes

Microservicio de **Gestión del Ciclo de Vida del Pedido** del caso BodegaNube (asignatura JVY0101).

## Responsabilidad (bounded context)
Crear órdenes a partir de avisos de venta, evitar duplicados por reintentos del webhook externo
(patrón *Idempotent Receiver*), gestionar los estados de la orden y exponer las consultas por
comercio. Ver sección 4.1 del informe de arquitectura.

## Requerimientos que cubre
- **RF-02**: recepción de avisos de venta externos vía webhook.
- **RF-03**: consulta de órdenes disponibles para picking.
- **RF-06**: consulta exclusiva de órdenes propias por comercio (Tenant Isolation).
- **RNF-02**: control de idempotencia para evitar duplicados por reintentos.

## Stack
Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL.

## Endpoints
| Método | Endpoint                        | Descripción                                                        |
|--------|----------------------------------|---------------------------------------------------------------------|
| POST   | `/api/ordenes`                  | Recibe el aviso de venta (simula el evento tras SQS + Lambda).      |
| GET    | `/api/ordenes?comercioId=X`     | Consulta las órdenes propias de un comercio.                       |
| GET    | `/api/ordenes/disponibles-picking` | Lista las órdenes listas para picking (stock ya reservado).     |
| PATCH  | `/api/ordenes/{id}/estado`      | Actualiza el estado de una orden.                                   |

## Cómo correrlo localmente
1. Crear una base PostgreSQL llamada `bodeganube_ordenes`.
2. `mvn spring-boot:run`

## Próximos pasos (fuera del alcance de este esqueleto)
- Invocar a `ms-inventario` para reservar stock al crear la orden, protegido con
  Circuit Breaker + Retry (Resilience4j), tal como se describe en el diagrama de arquitectura.
- Publicar un evento cuando la orden queda `LISTA_PARA_PICKING` en vez de requerir un PATCH manual.
