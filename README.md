# 📦 Event Sourcing Project — Query Service (MongoDB Branch)

Este projeto implementa **DDD + Event Sourcing + CQRS + Outbox Pattern (com CDC via Debezium)** em uma arquitetura baseada em microserviços.  
Atualmente, o **Query Service** utiliza **MongoDB** como banco de dados para os **Read Models** (antes era PostgreSQL).
- **Command Service (8080):** Responsável por processar comandos e armazenar eventos no PostgreSQL.
- **Query Service (8081):** Mantém um *read model* no MongoDB e expõe consultas otimizadas.

Eventos são propagados via **Debezium + Kafka**, garantindo consistência entre escrita e leitura.

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

# ATENÇÃO -> IMPORTANTE!!!
use a branch atual **mongodb**, não a branch **main**!
- **main** → versão original com PostgreSQL em ambos os serviços.
- **mongodb** → branch atual, onde o Query Service usa MongoDB.

---

## 📂 Estrutura dos Serviços
- `command-service/` → Processa comandos, aplica regras de negócio e publica eventos.
- `query-service/` → Consome eventos do Kafka e atualiza o MongoDB.
- `docker/` → Arquivos de configuração de inicialização.

---

## 🔧 Tecnologias
- **Spring Boot 3.x**
- **PostgreSQL** (Event Store, Outbox, Snapshots)
- **Flyway** (migração de banco)
- **MongoDB** (Read Model)
- **Kafka + Zookeeper** (plataforma de streaming de eventos)
- **Debezium** (CDC para Outbox → Kafka)
- **Kafka UI** (interface para inspecionar tópicos)
- **Docker Compose**

---
## ▶️ Como Executar

### Primeiramente faça um clone do projeto!

## ⚙️ Perfis de Execução
### ▶️ Rodar infraestrutura +  serviços apps (tudo dockerizados)
```bash
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d --build
```

### ▶️ Rodar apenas a infraestrutura no Docker + apps localmente (Maven)
```bash
docker-compose -f docker-compose.yml up -d

# Command Service
cd command-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Query Service
cd query-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

📌 **Configuração de perfis no `application.yml`:**
```yaml
spring:
  profiles:
    active: local   # Para rodar localmente
    #active: docker # Para rodar em containers
```

---

## 🔗 Acessos Importantes
- **Command Service:** [http://localhost:8080/api/pedidos](http://localhost:8080/api/pedidos)
- **Query Service:** [http://localhost:8081/api/pedidos](http://localhost:8081/api/pedidos)
- **Kafka UI:** [http://localhost:8082](http://localhost:8082)
- **Debezium Connect:** [http://localhost:8083](http://localhost:8083)
- **Postgres:** `localhost:5435` (user: postgres / pass: pass)
- **MongoDB:** `localhost:27018` (user: user / pass: pass)

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
3. **Query Service** consome evento do Kafka → atualiza **MongoDB** (`pedido_read`).
4. Query Service Tenta chamar `Command Service` → `/outbox/{id}/processed` para confirmar **processamento** no **Command Service**.
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

## 🔎 Roteiro de Testes (Postman)

Foram preparados exemplos no **Postman** para interagir com os serviços.

📥 Baixe os arquivos na raiz do projeto:
- [`postman_collection.json`](postman_collection.json)
- [`Event Sourcing.postman_environment.json`](Event%20Sourcing.postman_environment.json)

Após importar no **Postman**, você poderá testar:
- Criar, atualizar, cancelar pedidos (**Command Service**)
- Consultar pedidos por ID, número, cliente, status (**Query Service**)
- Estatísticas de pedidos e valores gastos por cliente

---
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

## 📊 Fluxo Resumido do Sistema
1. O **Command Service** salva eventos no PostgreSQL (tabela `event_outbox`).
2. O **Debezium** captura os eventos e publica no **Kafka**.
3. O **Query Service** consome os eventos e atualiza o MongoDB.
4. As consultas ao sistema são feitas diretamente no **Query Service**.

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

## ✅ Status Atual
- [x] Command Service isolado com PostgreSQL + Debezium
- [x] Query Service com MongoDB como read model
- [x] Kafka UI para monitoramento
- [x] Perfis configurados para rodar **local** ou **docker**
- [x] Exemplos de API disponíveis no Postman

---
## 📌 Notas Importantes

- `PedidoReadModel` está anotado com `@Field(..., targetType = FieldType.DECIMAL128)` para salvar valores como `NumberDecimal` e permitir agregações.
- O branch `mongodb` já está isolado do `command-service` — o `query-service` não depende mais de classes do Command.

---

✍️ **Autor:** Fernando Gilli  
