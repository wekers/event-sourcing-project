package com.example.eventsourcing.command.infrastructure;

import com.example.eventsourcing.command.domain.AggregateRoot;
import com.example.eventsourcing.command.domain.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStore {
    
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Salva os eventos de um agregado no Event Store
     */
    @Transactional
    public void saveEvents(AggregateRoot aggregate) {
        List<Event> events = aggregate.getUncommittedEvents();
        if (events.isEmpty()) {
            log.debug("No uncommitted events to save for aggregate {}", aggregate.getId());
            return;
        }
        
        List<EventStoreEntity> entities = events.stream()
                .map(event -> {
                    EventStoreEntity entity = new EventStoreEntity();
                    entity.setAggregateId(event.getAggregateId());
                    entity.setAggregateType(event.getAggregateType());
                    entity.setEventType(event.getEventType());
                    entity.setVersion(event.getVersion());
                    entity.setCreatedAt(event.getTimestamp());
                    
                    // Converte o evento para Map<String, Object>
                    Map<String, Object> eventData = objectMapper.convertValue(event, 
                            new TypeReference<Map<String, Object>>() {});
                    entity.setEventData(eventData);
                    
                    // Metadata (se houver)
                    // entity.setEventMetadata(event.getMetadata());
                    
                    return entity;
                })
                .collect(Collectors.toList());
        
        eventStoreRepository.saveAll(entities);
        aggregate.markEventsAsCommitted();
        
        log.debug("Saved {} events for aggregate {}", events.size(), aggregate.getId());
    }
    
    /**
     * Carrega eventos de um agregado a partir de uma versão específica
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsFromVersion(UUID aggregateId, Long version) {
        List<EventStoreEntity> entities = eventStoreRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, version);
        
        return entities.stream()
                .map(entity -> {
                    try {
                        // Reconstitui o evento a partir dos dados e do tipo
                        Class<?> eventClass = Class.forName("com.example.eventsourcing.command.domain.pedido.events." + entity.getEventType());
                        return (Event) objectMapper.convertValue(entity.getEventData(), eventClass);
                    } catch (ClassNotFoundException e) {
                        log.error("Event class not found: {}", entity.getEventType(), e);
                        throw new RuntimeException("Failed to load event", e);
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Obtém a versão atual de um agregado no Event Store
     */
    @Transactional(readOnly = true)
    public Long getCurrentVersion(UUID aggregateId) {
        Long version = eventStoreRepository.findMaxVersionByAggregateId(aggregateId);
        return version != null ? version : 0L;
    }

    @Transactional(readOnly = true)
    public List<Event> getEvents(UUID aggregateId) {
        List<EventStoreEntity> entities =
                eventStoreRepository.findByAggregateIdOrderByVersionAsc(aggregateId);

        return entities.stream()
                .map(entity -> {
                    try {
                        Class<?> eventClass = Class.forName(
                                "com.example.eventsourcing.command.domain.pedido.events." + entity.getEventType()
                        );
                        return (Event) objectMapper.convertValue(entity.getEventData(), eventClass);
                    } catch (ClassNotFoundException e) {
                        log.error("Event class not found: {}", entity.getEventType(), e);
                        throw new RuntimeException("Failed to load event", e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtém todos os aggregateIds distintos no Event Store.
     */
    @Transactional(readOnly = true)
    public List<UUID> getAllAggregateIds() {
        return eventStoreRepository.findDistinctAggregateIds();
    }

    public Optional<String> getAggregateType(UUID aggregateId) {
        return eventStoreRepository.findAggregateType(aggregateId);
    }

    /**
     * Verifica se um aggregateId existe no Event Store
     */
    @Transactional(readOnly = true)
    public boolean existsAggregateId(UUID aggregateId) {
        return eventStoreRepository.existsByAggregateId(aggregateId);
    }

    /**
     * Resolve aggregateId a partir de um outboxId (se existir relação).
     */
    @Transactional(readOnly = true)
    public Optional<UUID> findAggregateIdByOutboxId(UUID outboxId) {
        return eventStoreRepository.findAggregateIdByOutboxId(outboxId);
    }


}


