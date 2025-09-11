package com.example.eventsourcing.query.application.infrastructure.outbox;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "outbox_pending_ack")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxPendingAck {

    @Id
    private String id; // Mongo usa String/ObjectId como chave

    @Indexed(unique = true) // garante que só pode haver 1 por outboxEventId
    private UUID outboxEventId;

    private Instant createdAt = Instant.now();
    private int retryCount = 0;

    public OutboxPendingAck(UUID outboxEventId) {
        this.outboxEventId = outboxEventId;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}