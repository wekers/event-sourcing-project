# Projeto Spring Boot Event Sourcing (Microserviços)

---
## ✅ UPDATE !
###  👉  Utilize a banch `mongodb`
---

Este projeto demonstra uma arquitetura de Event Sourcing e CQRS (Command Query Responsibility Segregation) utilizando Spring Boot, PostgreSQL, Kafka e Debezium. Foi refatorado para separar as responsabilidades em dois microserviços distintos:

*   **Command Service:** Responsável por receber comandos, persistir eventos no Event Store, gerenciar Snapshots e publicar eventos no Kafka via Outbox Pattern.
*   **Query Service:** Responsável por consumir eventos do Kafka, construir e manter projeções (Read Models) em um banco de dados relacional e servir consultas.

---

## Language
- [Versão em Português do conteúdo do README](README_PT.md) <br/>
- [English version of the README content](README.md)

---

## Arquitetura

```
                          ┌────────────────────────────┐
                          │   Pedido Command Service   │
                          │                            │
   [REST Controller] ---> │      PedidoService         │
                          │            │               │
                          │            v               │
                          │       [EventStore]         │
                          │            │               │
                          │         (snapshot)         │
                          └────────────┼───────────────┘
                                       │
                                       v
                                   [Kafka]
                                       │
                                       v
                          ┌────────────────────────────┐
                          │    Pedido Query Service    │
                          │                            │
                          │   Projection Worker        │
                          │            │               │
                          │            v               │
                          │     [pedido_read SQL]      │
                          └────────────────────────────┘
```

## Tecnologias Utilizadas

*   **Spring Boot 3.2.0:** Framework para desenvolvimento de aplicações Java.
*   **PostgreSQL:** Banco de dados relacional para Event Store, Snapshots, Outbox e Read Models.
*   **Kafka:** Plataforma de streaming de eventos para comunicação assíncrona entre os serviços.
*   **Debezium:** Plataforma de Change Data Capture (CDC) para publicar eventos do Outbox para o Kafka.
*   **Flyway:** Ferramenta de migração de banco de dados.
*   **Lombok:** Biblioteca para reduzir código boilerplate.
*   **Jackson:** Biblioteca para manipulação de JSON.
*   **Testcontainers:** Para testes de integração com infraestrutura real em contêineres.

## Pré-requisitos

Certifique-se de ter os seguintes softwares instalados em sua máquina:

*   **Java 17+**
*   **Maven 3.6+**
*   **Docker** e **Docker Compose** (ou `docker compose`)
*   **Postman** (para testar os endpoints)

## Como Executar o Projeto

Siga os passos abaixo para configurar, executar e testar todas as funcionalidades:

### 1. Clonar o Repositório (se aplicável) e Navegar para o Diretório do Projeto

### 2. Iniciar a Infraestrutura Docker

Na pasta raiz do projeto (`event-sourcing-project`), execute o Docker Compose para subir o PostgreSQL, Kafka, Zookeeper, Debezium e Kafka UI:

```bash
docker-compose up -d --build
```

*   **Observação:** O `--build` garante que as imagens dos seus microserviços sejam construídas com base nos `Dockerfile`s.
*   Aguarde alguns minutos para que todos os serviços estejam completamente prontos. Você pode monitorar os logs com `docker-compose logs -f`.

### 3. Registrar o Conector Debezium

Após os serviços Docker estarem `Up`, registre o conector do Debezium. Isso instruirá o Debezium a monitorar a tabela `event_outbox` no PostgreSQL e publicar as mudanças no Kafka.

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @docker/debezium/register-postgres.json
```

### 4. Acessar as Interfaces de Gerenciamento (Opcional)

*   **Kafka UI:** Acesse `http://localhost:8082` para visualizar os tópicos do Kafka, incluindo `outbox.public.event_outbox`.

### 5. Testar os Endpoints com Postman

Os microserviços estarão rodando nas seguintes portas:

*   **Command Service:** `http://localhost:8080`
*   **Query Service:** `http://localhost:8081`

Você pode importar o arquivo `postman_collection.json` (fornecido junto com o projeto) no Postman para ter todas as requisições pré-configuradas.

#### Fluxo de Teste:

1.  **Criar Pedido (Command Service):**
    *   Envie uma requisição `POST` para `http://localhost:8080/api/pedidos` com o corpo JSON de criação de pedido.
    *   O `pedidoId` retornado será automaticamente salvo em uma variável de ambiente do Postman (se você usar a coleção fornecida).

2.  **Verificar Fluxo de Eventos (Kafka UI):**
    *   Após criar o pedido, verifique o tópico `outbox.public.event_outbox` no Kafka UI (`http://localhost:8082`). Você deverá ver a mensagem do evento `PedidoCriado`.

3.  **Consultar Pedido (Query Service):**
    *   Envie uma requisição `GET` para `http://localhost:8081/api/pedidos/{{pedidoId}}` (usando a variável de ambiente do Postman).
    *   Você deverá receber os dados do pedido recém-criado, confirmando que o evento foi processado pelo Query Service e o Read Model foi atualizado.

4.  **Atualizar/Cancelar Pedido (Command Service):**
    *   Tente as requisições `PUT` e `DELETE` no Command Service (`http://localhost:8080/api/pedidos/{pedidoId}`) para atualizar ou cancelar o pedido.
    *   Verifique novamente o Query Service (`http://localhost:8081/api/pedidos/{pedidoId}`) para confirmar que o Read Model foi atualizado com as novas informações.

5.  **Atualizar Status do Pedido (Command Service):**
    *   Envie uma requisição `PATCH` para `http://localhost:8080/api/pedidos/{pedidoId}/status` com o corpo JSON contendo o `novoStatus` (ex: `{"novoStatus": "CONFIRMADO"}`).
    *   Após cada transição, verifique o status do pedido no Query Service (`http://localhost:8081/api/pedidos/{pedidoId}`).

## Configuração de Snapshot

A frequência de criação de snapshots é configurada no `command-service/src/main/resources/application.yml`:

```yaml
app:
  event-store:
    snapshot-frequency: 2 # Um snapshot é criado a cada 2 eventos (versão do agregado múltipla de 2)
```
