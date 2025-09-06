package com.example.eventsourcing.command.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SnapshotStoreRepository extends JpaRepository<SnapshotStoreEntity, Long> {
    
    /**
     * Busca snapshot por ID do agregado
     */
    Optional<SnapshotStoreEntity> findByAggregateId(UUID aggregateId);
    
    /**
     * Remove snapshot por ID do agregado
     */
    void deleteByAggregateId(UUID aggregateId);
    
    /**
     * Verifica se existe snapshot para o agregado
     */
    boolean existsByAggregateId(UUID aggregateId);

    void deleteAll();


    Optional<SnapshotStoreEntity> findTopByAggregateIdOrderByVersionDesc(UUID aggregateId);

}

