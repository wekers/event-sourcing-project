# Projeto Spring Boot Event Sourcing (Microserviços)

Este projeto demonstra uma arquitetura de **Event Sourcing** e **CQRS**
(Command Query Responsibility Segregation) utilizando **Spring Boot**,
**PostgreSQL**, **Kafka** e **Debezium**.  
A aplicação foi refatorada em dois microserviços principais:

- **Command Service**: responsável por receber comandos, persistir
  eventos no **Event Store**, gerenciar **Snapshots** e publicar eventos via
  **Outbox Pattern**.
- **Query Service**: responsável por consumir eventos do Kafka,
  construir e manter projeções (Read Models) em um banco relacional e
  servir consultas. Também faz ACK de eventos processados de volta ao
  `Command Service`.

---

## 🚀 Arquitetura

```text
                       ┌────────────────────────────┐
                       │   Pedido Command Service   │
                       │                            │
    [REST Controller]─>│ PedidoCommandService       │
                       │            │               │
                       │            v               │
                       │       [EventStore]         │
                       │            │               │
                       │        [Outbox]            │
                       │            │               │
                       └────────────┼───────────────┘
                                    │
                                    ▼
                                [Debezium CDC]
                                    │
                                    ▼
                                 [Kafka]
                                    │
                                    ▼
                       ┌────────────────────────────┐
                       │    Pedido Query Service    │
                       │                            │
                       │ KafkaEventConsumer         │
                       │            │               │
                       │            ▼               │
                       │   [PedidoReadModel DB]     │
                       │                            │
                       │ OutboxAckRetryJob (ACKs)   │
                       └────────────────────────────┘
```

---

## 🛠 Tecnologias Utilizadas

- **Spring Boot 3.2+**
- **PostgreSQL** (Event Store, Snapshots, Outbox, Read Models)
- **Kafka** (plataforma de streaming de eventos)
- **Debezium** (CDC para Outbox → Kafka)
- **Flyway** (migração de banco)
- **Lombok**
- **Jackson**
- **Testcontainers**

---

## 📦 Pré-requisitos

Certifique-se de ter instalado:

- **Java 17+**
- **Maven 3.6+**
- **Docker** e **Docker Compose**
- **Postman** (para testes)

---

## ▶️ Como Executar

### 1. Compilar os Microserviços

```bash
# Command Service
cd command-service
mvn clean package -DskipTests
cd ..

# Query Service
cd query-service
mvn clean package -DskipTests
cd ..
```

### 2. Subir Infraestrutura com Docker

Na raiz do projeto:

```bash
docker-compose up -d --build
```

Isso inicia:
- PostgreSQL (com Event Store, Outbox e Read Models)
- Kafka + Zookeeper
- Debezium
- Kafka UI (http://localhost:8082)

### 3. Registrar Conector Debezium

```bash
curl -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   -d @docker/debezium/register-postgres.json
```

### 4. Endpoints dos Serviços

- **Command Service:** `http://localhost:8080`
- **Query Service:** `http://localhost:8081`

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

### 6. Cancelar Pedido

```http
DELETE http://localhost:8080/api/pedidos/{pedidoId}
```

- Evento `PedidoCancelado`
- Status no read model: `CANCELADO`

---

## 📂 Estrutura do Projeto

```text
event-sourcing-project/
├── command-service/        # Microserviço de Comandos
│   ├── domain/             # Agregados e Eventos
│   ├── application/        # Command Handlers e Services
│   ├── infrastructure/     # EventStore, Outbox, Snapshot
│   └── admin/              # AggregateRebuildService + RebuildController
├── query-service/          # Microserviço de Consultas
│   ├── projection/         # KafkaEventConsumer + PedidoProjectionHandler
│   ├── readmodel/          # PedidoReadModel + Repository
│   ├── outbox/             # OutboxClient + PendingAck + RetryJob
│   └── controller/         # Endpoints de consulta
├── docker/
│   ├── postgres/
│   └── debezium/
├── docker-compose.yml      # Infra: Postgres, Kafka, Debezium, Kafka-UI
├── postman_collection.json # Requisições pré-configuradas
└── README.md
```

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

## 🔮 Próximos Passos

- Implementar **ProjectionRebuildService** no `Query Service` para recriar
  projeções diretamente a partir do **Event Store** ou dos **Snapshots**.
- Melhorar métricas/observabilidade do fluxo de eventos.
- Expandir os testes automatizados com **Testcontainers**.

---

👨‍💻 Desenvolvido por Fernando Gilli
