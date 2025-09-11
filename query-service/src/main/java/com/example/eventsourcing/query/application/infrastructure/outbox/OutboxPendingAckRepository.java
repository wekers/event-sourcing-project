package com.example.eventsourcing.query.application.infrastructure.outbox;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxPendingAckRepository extends MongoRepository<OutboxPendingAck, UUID> {

    Optional<OutboxPendingAck> findByOutboxEventId(UUID outboxEventId);

}
