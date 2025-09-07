package com.example.eventsourcing.query.application.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxPendingAckRepository extends JpaRepository<OutboxPendingAck, UUID> {
}
