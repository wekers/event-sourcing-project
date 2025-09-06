package com.example.eventsourcing.command.infrastructure;

import com.example.eventsourcing.command.domain.AggregateRoot;
import com.example.eventsourcing.command.domain.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AggregateRepository<T extends AggregateRoot> {
    
    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    private final OutboxService outboxService;
    
    @Value("${app.event-store.snapshot-frequency:10}")
    private int snapshotFrequency;
    
    /**
     * Salva um agregado (eventos + snapshot se necessário + outbox)
     */
    @Transactional
    public void save(T aggregate) {
        if (!aggregate.hasUncommittedEvents()) {
            log.debug("No uncommitted events for aggregate {}", aggregate.getId());
            return;
        }
        
        List<Event> events = aggregate.getUncommittedEvents();
        
        try {
            // 1. Salva eventos no Event Store
            eventStore.saveEvents(aggregate);
            
            // 2. Publica eventos no Outbox para Debezium
            outboxService.publishEvents(events);
            
            // 3. Cria snapshot se necessário
            if (shouldCreateSnapshot(aggregate)) {
                snapshotStore.saveSnapshot(aggregate);
                log.debug("Created snapshot for aggregate {} at version {}", 
                        aggregate.getId(), aggregate.getVersion());
            }
            
            log.debug("Saved aggregate {} with {} events", 
                    aggregate.getId(), events.size());
            
        } catch (Exception e) {
            log.error("Failed to save aggregate {}", aggregate.getId(), e);
            throw e;
        }
    }
    
    /**
     * Carrega um agregado por ID
     */
    @Transactional(readOnly = true)
    public Optional<T> findById(UUID aggregateId, Class<T> aggregateClass) {
        try {
            // 1. Tenta carregar snapshot
            Optional<T> snapshot = snapshotStore.loadSnapshot(aggregateId, aggregateClass);
            
            T aggregate;
            Long fromVersion = 0L;
            
            if (snapshot.isPresent()) {
                aggregate = snapshot.get();
                fromVersion = aggregate.getVersion();
                log.debug("Loaded snapshot for aggregate {} at version {}", 
                        aggregateId, fromVersion);
            } else {
                // Cria nova instância do agregado
                try {
                    aggregate = aggregateClass.getDeclaredConstructor().newInstance();
                    aggregate.setId(aggregateId);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create aggregate instance", e);
                }
            }
            
            // 2. Carrega eventos posteriores ao snapshot
            List<Event> events = eventStore.getEventsFromVersion(aggregateId, fromVersion);
            
            if (events.isEmpty() && snapshot.isEmpty()) {
                return Optional.empty();
            }
            
            // 3. Aplica eventos ao agregado
            if (!events.isEmpty()) {
                aggregate.loadFromHistory(events);
                log.debug("Applied {} events to aggregate {}", events.size(), aggregateId);
            }
            
            return Optional.of(aggregate);
            
        } catch (Exception e) {
            log.error("Failed to load aggregate {}", aggregateId, e);
            throw new RuntimeException("Failed to load aggregate", e);
        }
    }
    
    /**
     * Verifica se existe um agregado
     */
    @Transactional(readOnly = true)
    public boolean exists(UUID aggregateId) {
        Long version = eventStore.getCurrentVersion(aggregateId);
        return version > 0;
    }
    
    /**
     * Obtém a versão atual de um agregado
     */
    @Transactional(readOnly = true)
    public Long getCurrentVersion(UUID aggregateId) {
        return eventStore.getCurrentVersion(aggregateId);
    }
    
    /**
     * Determina se deve criar um snapshot
     */
    private boolean shouldCreateSnapshot(T aggregate) {
        return aggregate.getVersion() % snapshotFrequency == 0;
    }
}

