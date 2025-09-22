# Spring Boot Event Sourcing Project (Microservices)

---
## ✅ UPDATE!
### 👉 Use the `mongodb` branch
---

This project demonstrates an Event Sourcing and CQRS (Command Query Responsibility Segregation) architecture using Spring Boot, PostgreSQL, Kafka, and Debezium. It was refactored to separate responsibilities into two distinct microservices:

*   **Command Service:** Responsible for receiving commands, persisting events in the Event Store, managing Snapshots, and publishing events to Kafka via the Outbox Pattern.
*   **Query Service:** Responsible for consuming events from Kafka, building and maintaining projections (Read Models) in a relational database, and serving queries.

---

## Language
- [Versão em Português do conteúdo do README](README_PT.md) <br/>
- [English version of the README content](README.md)

---

## Architecture

```
                          ┌────────────────────────────┐
                          │   Order Command Service    │
                          │                            │
   [REST Controller] ---> │      OrderService          │
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
                          │     Order Query Service    │
                          │                            │
                          │   Projection Worker        │
                          │            │               │
                          │            v               │
                          │     [order_read SQL]       │
                          └────────────────────────────┘
```

## Technologies Used

*   **Spring Boot 3.2.0:** Framework for developing Java applications.
*   **PostgreSQL:** Relational database for Event Store, Snapshots, Outbox, and Read Models.
*   **Kafka:** Event streaming platform for asynchronous communication between services.
*   **Debezium:** Change Data Capture (CDC) platform for publishing Outbox events to Kafka.
*   **Flyway:** Database migration tool.
*   **Lombok:** Library to reduce boilerplate code.
*   **Jackson:** Library for JSON handling.
*   **Testcontainers:** For integration testing with real infrastructure in containers.

## Prerequisites

Make sure you have the following software installed on your machine:

*   **Java 17+**
*   **Maven 3.6+**
*   **Docker** and **Docker Compose** (or `docker compose`)
*   **Postman** (to test the endpoints)

## How to Run the Project

Follow the steps below to configure, run, and test all functionalities:

### 1. Clone the Repository (if applicable) and Navigate to the Project Directory

### 2. Start the Docker Infrastructure

In the project root folder (`event-sourcing-project`), run Docker Compose to start PostgreSQL, Kafka, Zookeeper, Debezium, and Kafka UI:

```bash
docker-compose up -d --build
```

*   **Note:** The `--build` ensures that the images of your microservices are built based on the `Dockerfile`s.
*   Wait a few minutes until all services are fully ready. You can monitor logs with `docker-compose logs -f`.

### 3. Register the Debezium Connector

After the Docker services are `Up`, register the Debezium connector. This will instruct Debezium to monitor the `event_outbox` table in PostgreSQL and publish changes to Kafka.

```bash
curl -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   -d @docker/debezium/register-postgres.json
```

### 4. Access Management Interfaces (Optional)

*   **Kafka UI:** Go to `http://localhost:8082` to view Kafka topics, including `outbox.public.event_outbox`.

### 5. Test Endpoints with Postman

The microservices will be running on the following ports:

*   **Command Service:** `http://localhost:8080`
*   **Query Service:** `http://localhost:8081`

You can import the `postman_collection.json` file (provided with the project) into Postman to have all pre-configured requests.

#### Test Flow:

1.  **Create Order (Command Service):**
    *   Send a `POST` request to `http://localhost:8080/api/orders` with the order creation JSON body.
    *   The returned `orderId` will automatically be saved in a Postman environment variable (if you use the provided collection).

2.  **Check Event Flow (Kafka UI):**
    *   After creating the order, check the `outbox.public.event_outbox` topic in Kafka UI (`http://localhost:8082`). You should see the `OrderCreated` event message.

3.  **Query Order (Query Service):**
    *   Send a `GET` request to `http://localhost:8081/api/orders/{{orderId}}` (using the Postman environment variable).
    *   You should receive the newly created order data, confirming that the event was processed by the Query Service and the Read Model was updated.

4.  **Update/Cancel Order (Command Service):**
    *   Try the `PUT` and `DELETE` requests in Command Service (`http://localhost:8080/api/orders/{orderId}`) to update or cancel the order.
    *   Check again in Query Service (`http://localhost:8081/api/orders/{orderId}`) to confirm that the Read Model was updated with the new information.

5.  **Update Order Status (Command Service):**
    *   Send a `PATCH` request to `http://localhost:8080/api/orders/{orderId}/status` with the JSON body containing the `newStatus` (e.g.: `{"newStatus": "CONFIRMED"}`).
    *   After each transition, check the order status in Query Service (`http://localhost:8081/api/orders/{orderId}`).

## Snapshot Configuration

The snapshot creation frequency is configured in `command-service/src/main/resources/application.yml`:

```yaml
app:
  event-store:
    snapshot-frequency: 2 # A snapshot is created every 2 events (aggregate version multiple of 2)
```
