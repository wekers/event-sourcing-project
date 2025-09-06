package com.example.eventsourcing.command.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntity, Long> {
    
    /**
     * Busca todos os eventos de um agregado ordenados por versão
     */
    List<EventStoreEntity> findByAggregateIdOrderByVersionAsc(UUID aggregateId);
    
    /**
     * Busca eventos de um agregado a partir de uma versão específica
     */
    List<EventStoreEntity> findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
            UUID aggregateId, Long version);
    
    /**
     * Busca a última versão de um agregado
     */
    @Query("SELECT MAX(e.version) FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId")
    Long findMaxVersionByAggregateId(@Param("aggregateId") UUID aggregateId);


    @Query("SELECT DISTINCT e.aggregateId FROM EventStoreEntity e")
    List<UUID> findDistinctAggregateIds();

    @Query("select e.aggregateType from EventStoreEntity e where e.aggregateId = :aggregateId order by e.version asc limit 1")
    Optional<String> findAggregateType(UUID aggregateId);


    /**
     * Verifica se existe um evento com a versão específica para o agregado
     */
    boolean existsByAggregateIdAndVersion(UUID aggregateId, Long version);
    
    /**
     * Busca eventos por tipo de agregado
     */
    List<EventStoreEntity> findByAggregateTypeOrderByCreatedAtAsc(String aggregateType);
    
    /**
     * Busca eventos por tipo de evento
     */
    List<EventStoreEntity> findByEventTypeOrderByCreatedAtAsc(String eventType);


    boolean existsByAggregateId(UUID aggregateId);

    // 🔑 Mapeia um outboxId -> aggregateId
    @Query("SELECT e.aggregateId FROM EventStoreEntity e WHERE e.id = :outboxId")
    Optional<UUID> findAggregateIdByOutboxId(@Param("outboxId") UUID outboxId);
}

