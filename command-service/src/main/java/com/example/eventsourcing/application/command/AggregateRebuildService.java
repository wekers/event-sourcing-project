package com.example.eventsourcing.application.command;

import com.example.eventsourcing.domain.AggregateRoot;
import com.example.eventsourcing.domain.pedido.Pedido;
import com.example.eventsourcing.infrastructure.*;
import com.example.eventsourcing.domain.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregateRebuildService {

    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;

    /**
     * Reidrata um agregado a partir de todos os eventos,
     * ignorando o snapshot.
     */
    @Transactional(readOnly = true)
    public <T extends AggregateRoot> Optional<T> rehydrateAggregate(UUID aggregateId, Class<T> aggregateClass) {
        try {
            List<Event> events = eventStore.getEvents(aggregateId);

            if (events.isEmpty()) {
                log.warn("No events found for aggregate {}", aggregateId);
                return Optional.empty();
            }

            T aggregate = aggregateClass.getDeclaredConstructor().newInstance();
            aggregate.setId(aggregateId);
            aggregate.loadFromHistory(events);

            log.info("Rehydrated aggregate {} with {} events (version {})",
                    aggregateId, events.size(), aggregate.getVersion());

            return Optional.of(aggregate);
        } catch (Exception e) {
            log.error("Failed to rehydrate aggregate {}", aggregateId, e);
            throw new RuntimeException("Failed to rehydrate aggregate", e);
        }
    }

    /**
     * Reconstrói e sobrescreve o snapshot de um agregado.
     */
    @Transactional
    public <T extends AggregateRoot> void rebuildSnapshot(UUID aggregateId, Class<T> aggregateClass) {
        rehydrateAggregate(aggregateId, aggregateClass).ifPresent(aggregate -> {
            snapshotStore.saveSnapshot(aggregate);
            log.info("Rebuilt snapshot for aggregate {} at version {}",
                    aggregateId, aggregate.getVersion());
        });
    }

    /**
     * Reconstrói snapshots de todos os agregados.
     */
    @Transactional
    public <T extends AggregateRoot> void rebuildAllSnapshots(Class<T> aggregateClass) {
        List<UUID> aggregateIds = eventStore.getAllAggregateIds();

        for (UUID aggregateId : aggregateIds) {
            rebuildSnapshot(aggregateId, aggregateClass);
        }

        log.info("Rebuilt snapshots for {} aggregates", aggregateIds.size());
    }

    /**
     * Apaga snapshot de um agregado específico.
     */
    @Transactional
    public void clearSnapshot(UUID aggregateId) {
        snapshotStore.deleteSnapshot(aggregateId);
        log.info("Cleared snapshot for aggregate {}", aggregateId);
    }

    /**
     * Apaga todos os snapshots.
     */
    @Transactional
    public void clearAllSnapshots() {
        snapshotStore.deleteAll();
        log.info("Cleared all snapshots");
    }
}
