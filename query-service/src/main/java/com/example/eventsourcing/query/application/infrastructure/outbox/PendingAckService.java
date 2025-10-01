package com.example.eventsourcing.query.application.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingAckService {

    private final OutboxPendingAckRepository pendingAckRepository;

    public void savePendingAck(UUID outboxEventId) {
        try {
            pendingAckRepository.save(new OutboxPendingAck(outboxEventId));
            log.info("Evento {} salvo em outbox_pending_ack", outboxEventId);
        } catch (Exception e) {
            log.error("Erro ao salvar {} em outbox_pending_ack", outboxEventId, e);
        }
    }
}
