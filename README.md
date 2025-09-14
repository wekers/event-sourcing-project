# 📦 Event Sourcing Project — Query Service (MongoDB Branch)

Este projeto implementa **DDD + Event Sourcing + CQRS + Outbox Pattern (com CDC via Debezium)** em uma arquitetura baseada em microserviços.  
Atualmente, o **Query Service** utiliza **MongoDB** como banco de dados para os **Read Models** (antes era PostgreSQL).

---

## ⚙️ Arquitetura

### 1. **Command Service**
- Persiste os eventos no **Event Store** (PostgreSQL).
- Registra os eventos na tabela **Outbox** (`event_outbox`).
- Gera snapshots dos agregados em `snapshot_store`.
- Expõe o endpoint `/outbox/{id}/processed` para confirmar o processamento dos eventos no Query Service.

### 2. **Debezium**
- Monitora a tabela **Outbox** (`event_outbox`) no PostgreSQL.
- Publica mudanças no tópico Kafka `outbox.public.event_outbox`.

### 3. **Query Service (MongoDB)**
- Consome eventos do Kafka via `KafkaEventConsumer`.
- Projeta os dados em `pedido_read` no MongoDB.
- Confirma o processamento dos eventos chamando o Command Service (`/outbox/{id}/processed`).
- Se o Command Service estiver **offline**, salva o evento em `outbox_pending_ack`.
- O `OutboxAckRetryJob` reenvia os ACKs pendentes a cada 10s quando o Command Service voltar.

### 4. **Snapshots**
- `AggregateRebuildService` (no Command Service) permite reidratar agregados a partir do **Event Store** ou de **Snapshots**.

### 5. **Consultas**
- O **Query Service** expõe endpoints REST que consultam diretamente o MongoDB.
- Exemplo de read models:  
  - `pedido_read` → visão otimizada de pedidos.
  - Consultas agregadas (estatísticas de clientes, total gasto, status de pedidos, etc).

---

## 🗄️ Estrutura do Banco de Dados

### PostgreSQL (Command Service)
- `event_store` → eventos de domínio (append-only).
- `event_outbox` → eventos pendentes de publicação (Outbox Pattern).
- `snapshot_store` → snapshots de agregados.

### MongoDB (Query Service)
- `pedido_read` → Read Model de pedidos (otimizado para queries).
- `outbox_pending_ack` → ACKs pendentes quando o Command Service está offline.

---

## 📂 Estrutura de Branches

- **main** → versão original com PostgreSQL em ambos os serviços.
- **mongodb** → branch atual, onde o Query Service usa MongoDB.

---

## 🐳 Docker Compose

### Subir infraestrutura:
```bash
docker-compose up -d --buld
```

### Inclui:
- PostgreSQL (command-service)
- Kafka + Zookeeper
- Debezium (CDC)
- MongoDB + Mongo Express
- Kafka UI (http://localhost:8082)
---

### Obs: Após inicializar os serviços, Registrar Conector Debezium

```bash
curl -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   -d @docker/debezium/register-postgres.json
```

## 📜 Scripts de Inicialização

### Índices MongoDB
Arquivo: `docker/mongo-init/pedido_read_indexes.js`

```js
db = db.getSiblingDB("pedido_read_db");

db.pedido_read.createIndex({ cliente_id: 1 });
db.pedido_read.createIndex({ status: 1 });
db.pedido_read.createIndex({ data_criacao: -1 });
db.pedido_read.createIndex({ numero_pedido: 1 }, { unique: true });
db.pedido_read.createIndex({ cliente_email: 1 });
db.pedido_read.createIndex({ valor_total: 1 });
```

---

## 📡 Endpoints Principais

### Query Service
- `GET /api/pedidos/{id}/completo` → Pedido detalhado.
- `GET /api/pedidos/estatisticas/cliente/{clienteId}/total-gasto` → Total gasto por cliente.
- `GET /api/pedidos?clienteId=...&status=...` → Filtros dinâmicos.

### Command Service
- `POST /api/pedidos` → Criação de pedidos.
- `PUT /api/pedidos/{id}` → Atualização.
- `POST /outbox/{id}/processed` → Confirmação de evento processado.

---

## 🔄 Fluxo Completo

1. **Command Service** grava evento no **Event Store** e no **Outbox**.
2. **Debezium** detecta mudanças no `event_outbox` e publica no **Kafka**.
3. **Query Service** consome evento → atualiza **MongoDB** (`pedido_read`).
4. Query Service confirma processamento no Command Service.
   - Se offline → salva no `outbox_pending_ack`.
   - `OutboxAckRetryJob` reprocessa periodicamente até sucesso.
5. Consultas são feitas diretamente no **MongoDB** via Query Service.
6. `AggregateRebuildService` e `SnapshotStore` garantem reidratação eficiente de agregados.

---

## 📊 Tecnologias

- **Spring Boot 3.x**
- **Kafka**
- **PostgreSQL** (Command Service)
- **MongoDB** (Query Service)
- **Debezium** (CDC / Outbox Pattern)
- **Docker Compose**
- **Lombok / JPA / Spring Data MongoDB**

---

## 🚀 Como rodar

### Subir infraestrutura
```bash
docker-compose -f docker-compose.yml up -d
```

### Subir Command Service
```bash
./mvnw spring-boot:run -pl command-service
```

### Subir Query Service (MongoDB branch)
```bash
./mvnw spring-boot:run -pl query-service -Dspring-boot.run.profiles=mongodb
```
---

## 🔎 Roteiro de Testes (Postman)

### 1. Criar Pedido (Command)

```http
POST http://localhost:8080/api/pedidos
```

- Gera evento `PedidoCriado`
- `outbox_event.status = PENDING`

### 2. Debezium → Kafka

- Evento publicado em `outbox.public.event_outbox`
- `outbox_event.status = PUBLISHED`

### 3. Query Service

```http
GET http://localhost:8081/api/pedidos/{pedidoId}
```

- Deve retornar o pedido criado no **read model**.

### 4. Atualizar Pedido

```http
PUT http://localhost:8080/api/pedidos/{pedidoId}
```

- Gera evento `PedidoAtualizado`
- Query Service reflete as mudanças

### 5. Alterar Status

```http
PATCH http://localhost:8080/api/pedidos/{pedidoId}/status
```

Payload:

```json
{ "novoStatus": "CONFIRMADO" }
```

- Read model atualizado com novo status


- Status devem seguir a ordem:
 - Status final: ENTREGUE
   - Linha do tempo:
	 - 2025-08-24T17:40:22Z - PENDENTE
	 - 2025-08-24T17:40:22Z - CONFIRMADO
     - 2025-08-24T17:40:22Z - EM_PREPARACAO
	 - 2025-08-24T17:40:22Z - ENVIADO
	 - 2025-08-24T17:40:22Z - ENTREGUE
ex.: não pode voltar de ENTREGUE para EM_PREPARACAO
ou ex.: de CONFIRMADO para ENVIADO direto

### 6. Cancelar Pedido

```http
DELETE http://localhost:8080/api/pedidos/{pedidoId}
```
- **Payload**:
```json
{ "motivo": "Desistência" }
```

- Evento `PedidoCancelado`
- Status no read model: `CANCELADO`

---
## ✅ Fluxo Completo

1. **Command Service**
   - Grava evento no **Event Store**
   - Persiste no **Outbox**
2. **Debezium**
   - Detecta mudança no Outbox
   - Publica no **Kafka**
3. **Query Service**
   - Consome evento do Kafka
   - Atualiza o **Read Model**
   - Tenta chamar `Command Service` → `/outbox/{id}/processed`
   - Se offline → salva em `outbox_pending_ack`
   - `OutboxAckRetryJob` reenvia quando voltar
4. **Snapshots**
   - `AggregateRebuildService` permite reidratar agregados a partir do Event Store
   - `SnapshotStore` guarda estado consolidado
5. **Consultas**
   - Read Models são consultados via `Query Service`


---

## 📌 Notas Importantes

- `PedidoReadModel` está anotado com `@Field(..., targetType = FieldType.DECIMAL128)` para salvar valores como `NumberDecimal` e permitir agregações.
- Sempre usar `cliente_id` e `valor_total` nos pipelines de agregação MongoDB.
- O branch `mongodb` já está isolado do `command-service` — o `query-service` não depende mais de classes do Command.

---

✍️ **Autor:** Fernando Gilli  
