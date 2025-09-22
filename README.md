# 📦 Event Sourcing Project (Microservices)— Query Service on (MongoDB Branch)

This project implements **DDD + Event Sourcing + CQRS + Outbox Pattern (with CDC via Debezium)** in a microservices-based architecture.  
Currently, the **Query Service** uses **MongoDB** as the database for **Read Models** (previously it was PostgreSQL).
- **Command Service (8080):** Responsible for processing commands and storing events in PostgreSQL.
- **Query Service (8081):** Maintains a *read model* in MongoDB and exposes optimized queries.

Events are propagated via **Debezium + Kafka**, ensuring consistency between write and read.

---

## Language
- [Versão em Português do conteúdo do README](README_PT.md) <br/>
- [English version of the README content](README.md)

---

## ⚙️ Architecture

### 1. **Command Service**
- Persists events in the **Event Store** (PostgreSQL).
- Registers events in the **Outbox** table (`event_outbox`).
- Generates snapshots of aggregates in `snapshot_store`.
- Exposes the endpoint `/outbox/{id}/processed` to confirm event processing in the Query Service.

### 2. **Debezium**
- Monitors the **Outbox** table (`event_outbox`) in PostgreSQL.
- Publishes changes to the Kafka topic `outbox.public.event_outbox`.

### 3. **Query Service (MongoDB)**
- Consumes events from Kafka via `KafkaEventConsumer`.
- Projects data into `pedido_read` in MongoDB.
- Confirms event processing by calling the Command Service (`/outbox/{id}/processed`).
- If the Command Service is **offline**, stores the event in `outbox_pending_ack`.
- The `OutboxAckRetryJob` resends pending ACKs every 10s once the Command Service is back online.

### 4. **Snapshots**
- `AggregateRebuildService` (in the Command Service) allows rehydrating aggregates from the **Event Store** or from **Snapshots**.

### 5. **Queries**
- The **Query Service** exposes REST endpoints that query MongoDB directly.
- Example of read models:  
  - `pedido_read` → optimized view of orders.
  - Aggregated queries (customer statistics, total spent, order status, etc).

---

## 🗄️ Database Structure

### PostgreSQL (Command Service)
- `event_store` → domain events (append-only).
- `event_outbox` → events pending publication (Outbox Pattern).
- `snapshot_store` → aggregate snapshots.

### MongoDB (Query Service)
- `pedido_read` → Read Model of orders (optimized for queries).
- `outbox_pending_ack` → Pending ACKs when the Command Service is offline.

---

## 📂 Branch Structure

# ATTENTION -> IMPORTANT!!!
Use the current branch **mongodb**, not the **main** branch!
- **main** → original version with PostgreSQL in both services.
- **mongodb** → current branch, where the Query Service uses MongoDB.

---

## 📂 Service Structure
- `command-service/` → Processes commands, applies business rules, and publishes events.
- `query-service/` → Consumes events from Kafka and updates MongoDB.
- `docker/` → Startup configuration files.

---

## 🔧 Technologies
- **Spring Boot 3.x**
- **PostgreSQL** (Event Store, Outbox, Snapshots)
- **Flyway** (database migration)
- **MongoDB** (Read Model)
- **Kafka + Zookeeper** (event streaming platform)
- **Debezium** (CDC for Outbox → Kafka)
- **Kafka UI** (interface to inspect topics)
- **Docker Compose**

---
## ▶️ How to Run

### First, clone the project!

## ⚙️ Execution Profiles
### ▶️ Run infrastructure + app services (all dockerized)
```bash
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d --build
```

### ▶️ Run only infrastructure in Docker + apps locally (Maven)
```bash
docker-compose -f docker-compose.yml up -d

# Command Service
cd command-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Query Service
cd query-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

📌 **Profile configuration in `application.yml`:**
```yaml
spring:
  profiles:
    active: local   # To run locally
    #active: docker # To run in containers
```

---

## 🔗 Important Access Points
- **Command Service:** [http://localhost:8080/api/pedidos](http://localhost:8080/api/pedidos)
- **Query Service:** [http://localhost:8081/api/pedidos](http://localhost:8081/api/pedidos)
- **Kafka UI:** [http://localhost:8082](http://localhost:8082)
- **Debezium Connect:** [http://localhost:8083](http://localhost:8083)
- **Postgres:** `localhost:5435` (user: postgres / pass: pass)
- **MongoDB:** `localhost:27018` (user: user / pass: pass)

---

## 📡 Main Endpoints

### Query Service
- `GET /api/pedidos/{id}/completo` → Detailed order.
- `GET /api/pedidos/estatisticas/cliente/{clienteId}/total-gasto` → Total spent by customer.
- `GET /api/pedidos?clienteId=...&status=...` → Dynamic filters.

### Command Service
- `POST /api/pedidos` → Create orders.
- `PUT /api/pedidos/{id}` → Update.
- `POST /outbox/{id}/processed` → Confirmation of processed event.

---

## 🔄 Complete Flow

1. **Command Service** records event in **Event Store** and in **Outbox**.
2. **Debezium** detects changes in `event_outbox` and publishes to **Kafka**.
3. **Query Service** consumes event from Kafka → updates **MongoDB** (`pedido_read`).
4. Query Service tries to call `Command Service` → `/outbox/{id}/processed` to confirm **processing** in **Command Service**.
   - If offline → saves in `outbox_pending_ack`.
   - `OutboxAckRetryJob` reprocesses periodically until successful.
5. Queries are made directly in **MongoDB** via Query Service.
6. `AggregateRebuildService` and `SnapshotStore` ensure efficient rehydration of aggregates.

---

## 📊 Technologies

- **Spring Boot 3.x**
- **Kafka**
- **PostgreSQL** (Command Service)
- **MongoDB** (Query Service)
- **Debezium** (CDC / Outbox Pattern)
- **Docker Compose**
- **Lombok / JPA / Spring Data MongoDB**

---

## 🔎 Test Scenarios (Postman)

Examples were prepared in **Postman** to interact with the services.

📥 Download the files at the project root:
- [`postman_collection.json`](postman_collection.json)
- [`Event Sourcing.postman_environment.json`](https://github.com/wekers/event-sourcing-project/blob/mongodb/Even%20Sourcing.postman_environment.json)

After importing into **Postman**, you will be able to test:
- Create, update, cancel orders (**Command Service**)
- Query orders by ID, number, customer, status (**Query Service**)
- Order statistics and total amounts spent per customer

---
### 1. Create Order (Command)


```http
POST http://localhost:8080/api/pedidos
```

- Generates `PedidoCriado` event
- `outbox_event.status = PENDING`

### 2. Debezium → Kafka

- Event published in `outbox.public.event_outbox`
- `outbox_event.status = PUBLISHED`

### 3. Query Service

```http
GET http://localhost:8081/api/pedidos/{pedidoId}
```

- Should return the order created in the **read model**.

### 4. Update Order

```http
PUT http://localhost:8080/api/pedidos/{pedidoId}
```

- Generates `PedidoAtualizado` event
- Query Service reflects the changes

### 5. Change Status

```http
PATCH http://localhost:8080/api/pedidos/{pedidoId}/status
```

Payload:

```json
{ "novoStatus": "CONFIRMADO" }
```

- Read model updated with new status

- Status must follow the order:
 - Final status: ENTREGUE (DELIVERED)
   - Timeline:
     - 2025-08-24T17:40:22Z - PENDENTE (PENDING)
     - 2025-08-24T17:40:22Z - CONFIRMADO (CONFIRMED)
     - 2025-08-24T17:40:22Z - EM_PREPARACAO (IN_PREPARATION)
     - 2025-08-24T17:40:22Z - ENVIADO (SENT)
     - 2025-08-24T17:40:22Z - ENTREGUE (DELIVERED)
ex.: it cannot go back from DELIVERED to IN_PREPARATION  
or ex.: from CONFIRMED directly to SENT

### 6. Cancel Order

```http
DELETE http://localhost:8080/api/pedidos/{pedidoId}
```
- **Payload**:
```json
{ "motivo": "Desistência" }
```

- `PedidoCancelado` event
- Status in read model: `CANCELADO` (CANCELLED)

---

## 📊 System Flow Summary
1. The **Command Service** saves events in PostgreSQL (`event_outbox` table).
2. **Debezium** captures the events and publishes to **Kafka**.
3. The **Query Service** consumes events and updates MongoDB.
4. System queries are made directly to the **Query Service**.

---

## ✅ Complete Flow

1. **Command Service**
   - Records event in **Event Store**
   - Persists in **Outbox**
2. **Debezium**
   - Detects change in Outbox
   - Publishes to **Kafka**
3. **Query Service**
   - Consumes event from Kafka
   - Updates the **Read Model**
   - Tries to call `Command Service` → `/outbox/{id}/processed`
   - If offline → stores in `outbox_pending_ack`
   - `OutboxAckRetryJob` resends when back online
4. **Snapshots**
   - `AggregateRebuildService` allows rehydrating aggregates from Event Store
   - `SnapshotStore` stores consolidated state
5. **Queries**
   - Read Models are queried via `Query Service`

---

## ✅ Current Status
- [x] Command Service isolated with PostgreSQL + Debezium
- [x] Query Service with MongoDB as read model
- [x] Kafka UI for monitoring
- [x] Profiles configured to run **local** or **docker**
- [x] API examples available in Postman

---
## 📌 Important Notes

- `PedidoReadModel` is annotated with `@Field(..., targetType = FieldType.DECIMAL128)` to save values as `NumberDecimal` and allow aggregations.
- The `mongodb` branch is already isolated from `command-service` — the `query-service` no longer depends on Command classes.

---

✍️ **Author:** Fernando Gilli  
