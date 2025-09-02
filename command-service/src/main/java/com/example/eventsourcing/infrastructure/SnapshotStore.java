package com.example.eventsourcing.infrastructure;

import com.example.eventsourcing.domain.AggregateRoot;
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
     * Salva um snapshot do agregado
     */
    @Transactional
    public void saveSnapshot(AggregateRoot aggregate) {
        try {
            Map<String, Object> aggregateData = objectMapper.convertValue(aggregate, 
                    new TypeReference<Map<String, Object>>() {});
            
            Optional<SnapshotStoreEntity> existingSnapshot = 
                    snapshotStoreRepository.findByAggregateId(aggregate.getId());
            
            SnapshotStoreEntity entity;
            if (existingSnapshot.isPresent()) {
                entity = existingSnapshot.get();
                entity.setAggregateData(aggregateData);
                entity.setVersion(aggregate.getVersion());
            } else {
                entity = new SnapshotStoreEntity();
                entity.setAggregateId(aggregate.getId());
                entity.setAggregateType(aggregate.getAggregateType());
                entity.setAggregateData(aggregateData);
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
     * Carrega um snapshot do agregado
     */
    @Transactional(readOnly = true)
    public <T extends AggregateRoot> Optional<T> loadSnapshot(UUID aggregateId, Class<T> aggregateClass) {
        try {
            Optional<SnapshotStoreEntity> entity = snapshotStoreRepository.findByAggregateId(aggregateId);
            
            if (entity.isEmpty()) {
                return Optional.empty();
            }
            
            T aggregate = objectMapper.convertValue(entity.get().getAggregateData(), aggregateClass);
            
            log.debug("Loaded snapshot for aggregate {} at version {}", 
                    aggregateId, aggregate.getVersion());
            
            return Optional.of(aggregate);
            
        } catch (Exception e) {
            log.error("Failed to load snapshot for aggregate {}", aggregateId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Remove um snapshot
     */
    @Transactional
    public void deleteSnapshot(UUID aggregateId) {
        snapshotStoreRepository.deleteByAggregateId(aggregateId);
        log.debug("Deleted snapshot for aggregate {}", aggregateId);
    }
    
    /**
     * Verifica se existe snapshot para o agregado
     */
    @Transactional(readOnly = true)
    public boolean hasSnapshot(UUID aggregateId) {
        return snapshotStoreRepository.existsByAggregateId(aggregateId);
    }
}

