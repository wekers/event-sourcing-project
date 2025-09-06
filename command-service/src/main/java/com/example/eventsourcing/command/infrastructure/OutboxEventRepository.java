package com.example.eventsourcing.command.infrastructure;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Busca eventos por status ordenados por criação
     */
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEventEntity.OutboxStatus status);

    /**
     * Busca eventos pendentes com paginação
     */
    @Query("SELECT o FROM OutboxEventEntity o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(@Param("status") OutboxEventEntity.OutboxStatus status,
                                              Pageable pageable);

    /**
     * Atualiza status do evento
     */
    @Modifying
    @Transactional
    @Query("UPDATE OutboxEventEntity o SET o.status = :status, o.processedAt = :processedAt WHERE o.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") OutboxEventEntity.OutboxStatus status,
                      @Param("processedAt") Instant processedAt);

    /**
     * Remove eventos processados antigos
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEventEntity o WHERE o.status = :status AND o.processedAt < :cutoffDate")
    int deleteOldProcessedEvents(@Param("status") OutboxEventEntity.OutboxStatus status,
                                 @Param("cutoffDate") Instant cutoffDate);

    /**
     * Conta eventos por status
     */
    long countByStatus(OutboxEventEntity.OutboxStatus status);

    /**
     * Busca eventos pendentes há muito tempo
     */
    //@Query("SELECT o FROM OutboxEventEntity o WHERE o.status = 'PENDING' AND o.createdAt < :cutoff")
    //List<OutboxEventEntity> findOldPendingEvents(@Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(o) FROM OutboxEventEntity o WHERE o.status = 'PENDING' AND o.createdAt < :cutoff")
    long countOldPendingEvents(@Param("cutoff") Instant cutoff);
}