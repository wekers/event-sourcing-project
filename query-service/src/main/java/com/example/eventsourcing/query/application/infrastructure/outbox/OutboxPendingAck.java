package com.example.eventsourcing.query.application.infrastructure.outbox;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxPendingAck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // casa com "IDENTITY" do Flyway
    private Long id;

    @Column(name = "outbox_event_id", nullable = false, columnDefinition = "UUID")
    private UUID outboxEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public OutboxPendingAck(UUID outboxEventId) {
        this.outboxEventId = outboxEventId;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
