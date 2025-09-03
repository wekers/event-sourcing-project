package com.example.eventsourcing.infrastructure;

import com.example.eventsourcing.domain.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher; // ✅ Para eventos de domínio

    /**
     * Publica eventos no outbox
     */
    @Transactional
    public void publishEvents(List<Event> events) {
        try {
            List<OutboxEventEntity> outboxEvents = events.stream()
                    .map(this::toOutboxEventEntity)
                    .toList();

            List<OutboxEventEntity> savedEvents = outboxEventRepository.saveAll(outboxEvents);

            log.debug("Published {} events to outbox", events.size());

            // ✅ Opcional: Publica evento de domínio para notificar sobre novos eventos
            savedEvents.forEach(event -> {
                eventPublisher.publishEvent(new OutboxEventCreatedEvent(event.getId()));
            });

        } catch (Exception e) {
            log.error("Failed to publish events to outbox", e);
            throw new RuntimeException("Failed to publish events to outbox", e);
        }
    }

    /**
     * Processa eventos pendentes (executado pelo Debezium via CDC)
     * Este método é principalmente para monitoramento e limpeza
     */
    @Scheduled(fixedDelay = 30000) // 30 segundos
    @Transactional
    public void monitorOutboxEvents() { // ✅ Renomeado para monitor (não processa)
        try {
            // Busca eventos pendentes para monitoramento
            List<OutboxEventEntity> pendingEvents = outboxEventRepository
                    .findByStatusOrderByCreatedAtAsc(OutboxEventEntity.OutboxStatus.PENDING);

            if (!pendingEvents.isEmpty()) {
                log.info("📊 Monitoring: Found {} pending events in outbox", pendingEvents.size());

                // Alertas para eventos pendentes há muito tempo
                pendingEvents.stream()
                        .filter(event -> event.getCreatedAt()
                                .isBefore(Instant.now().minus(15, ChronoUnit.MINUTES)))
                        .findFirst()
                        .ifPresent(oldEvent -> {
                            log.warn("⚠️ Event {} pending for more than 15 minutes", oldEvent.getId());
                        });
            }

            // Limpa eventos processados antigos (mais de 7 dias)
            Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
            int deletedCount = outboxEventRepository.deleteOldProcessedEvents(
                    OutboxEventEntity.OutboxStatus.PROCESSED, cutoffDate);

            if (deletedCount > 0) {
                log.info("🧹 Cleaned up {} old processed events", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error monitoring outbox events", e);
        }
    }

    /**
     * Busca eventos por status (para fallback mechanism)
     */
    public List<OutboxEventEntity> findEventsByStatus(OutboxEventEntity.OutboxStatus status) {
        return outboxEventRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    /**
     * Atualiza status do evento (para fallback mechanism)
     */
    @Transactional
    public void updateEventStatus(UUID eventId, OutboxEventEntity.OutboxStatus status) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(status);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
            log.debug("Updated event {} status to {}", eventId, status);
        });
    }

    /**
     * Converte Event para OutboxEventEntity
     */
    private OutboxEventEntity toOutboxEventEntity(Event event) {
        try {
            Map<String, Object> eventData = objectMapper.convertValue(event,
                    new TypeReference<Map<String, Object>>() {});

            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setAggregateId(event.getAggregateId());
            entity.setAggregateType(event.getAggregateType());
            entity.setEventType(event.getEventType());
            entity.setEventData(eventData);
            entity.setEventMetadata(event.getMetadata());
            entity.setCreatedAt(event.getTimestamp());
            entity.setStatus(OutboxEventEntity.OutboxStatus.PENDING);

            return entity;

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert event to outbox entity", e);
        }
    }

    // ✅ Evento de domínio para notificar sobre novo evento no outbox
    @Getter
    @AllArgsConstructor
    public static class OutboxEventCreatedEvent {
        private final UUID outboxEventId;
    }
}