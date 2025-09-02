package com.example.eventsourcing.infrastructure;

import com.example.eventsourcing.domain.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Publica eventos no outbox
     */
    @Transactional
    public void publishEvents(List<Event> events) {
        try {
            List<OutboxEventEntity> outboxEvents = events.stream()
                    .map(this::toOutboxEventEntity)
                    .toList();
            
            outboxEventRepository.saveAll(outboxEvents);
            
            log.debug("Published {} events to outbox", events.size());
            
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
    public void processOutboxEvents() {
        try {
            // Busca eventos pendentes para monitoramento
            List<OutboxEventEntity> pendingEvents = outboxEventRepository
                    .findPendingEvents(OutboxEventEntity.OutboxStatus.PENDING, 
                                     PageRequest.of(0, 100));
            
            if (!pendingEvents.isEmpty()) {
                log.debug("Found {} pending events in outbox", pendingEvents.size());
            }
            
            // Limpa eventos processados antigos (mais de 7 dias)
            Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
            outboxEventRepository.deleteOldProcessedEvents(
                    OutboxEventEntity.OutboxStatus.PROCESSED, cutoffDate);
            
        } catch (Exception e) {
            log.error("Error processing outbox events", e);
        }
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
}

