package com.example.eventsourcing.query.application.infrastructure.outbox;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxPendingAck {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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
