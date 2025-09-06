package com.example.eventsourcing.command.infrastructure;

import com.example.eventsourcing.command.domain.AggregateRoot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotStore {

    private final SnapshotStoreRepository snapshotStoreRepository;
    private final ObjectMapper objectMapper;

    /**
     * Salva ou atualiza um snapshot (mantém apenas 1 por aggregateId).
     */
    @Transactional
    public void saveSnapshot(AggregateRoot aggregate) {
        try {
            Optional<SnapshotStoreEntity> existing =
                    snapshotStoreRepository.findByAggregateId(aggregate.getId());

            SnapshotStoreEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
                entity.setAggregateData(objectMapper.convertValue(
                        aggregate, new TypeReference<Map<String, Object>>() {}));
                entity.setVersion(aggregate.getVersion());
            } else {
                entity = new SnapshotStoreEntity();
                entity.setAggregateId(aggregate.getId());
                entity.setAggregateType(aggregate.getAggregateType());
                entity.setAggregateData(objectMapper.convertValue(
                        aggregate, new TypeReference<Map<String, Object>>() {}));
                entity.setVersion(aggregate.getVersion());
            }

            snapshotStoreRepository.save(entity);
            log.debug("Saved snapshot for aggregate {} at version {}",
                    aggregate.getId(), aggregate.getVersion());
        } catch (Exception e) {
            log.error("Failed to save snapshot for aggregate {}", aggregate.getId(), e);
            throw new RuntimeException("Failed to save snapshot", e);
        }
    }

    /**
     * Carrega o snapshot (se existir).
     */
    @Transactional(readOnly = true)
    public <T extends AggregateRoot> Optional<T> loadSnapshot(UUID aggregateId, Class<T> aggregateClass) {
        try {
            return snapshotStoreRepository.findByAggregateId(aggregateId)
                    .map(entity -> {
                        T aggregate = objectMapper.convertValue(entity.getAggregateData(), aggregateClass);
                        log.debug("Loaded snapshot for aggregate {} at version {}",
                                aggregateId, aggregate.getVersion());
                        return aggregate;
                    });
        } catch (Exception e) {
            log.error("Failed to load snapshot for aggregate {}", aggregateId, e);
            return Optional.empty();
        }
    }

    /**
     * Remove snapshot de um aggregate.
     */
    @Transactional
    public void deleteSnapshot(UUID aggregateId) {
        snapshotStoreRepository.deleteByAggregateId(aggregateId);
        log.debug("Deleted snapshot for aggregate {}", aggregateId);
    }

    /**
     * Verifica se existe snapshot.
     */
    @Transactional(readOnly = true)
    public boolean hasSnapshot(UUID aggregateId) {
        return snapshotStoreRepository.existsByAggregateId(aggregateId);
    }

    /** Remove todos os snapshots */
    @Transactional
    public void deleteAll() {
        snapshotStoreRepository.deleteAll();
        log.debug("Deleted all snapshots");
    }
}
