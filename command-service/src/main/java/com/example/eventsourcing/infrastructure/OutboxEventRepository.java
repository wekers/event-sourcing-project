package com.example.eventsourcing.infrastructure;

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
     * Busca eventos pendentes ordenados por data de criação
     */
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEventEntity.OutboxStatus status);
    
    /**
     * Busca eventos pendentes com limite
     */
    @Query("SELECT o FROM OutboxEventEntity o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(@Param("status") OutboxEventEntity.OutboxStatus status, 
                                             org.springframework.data.domain.Pageable pageable);
    
    /**
     * Marca evento como processado
     */
    @Modifying
    @Query("UPDATE OutboxEventEntity o SET o.status = :status, o.processedAt = :processedAt WHERE o.id = :id")
    void updateStatus(@Param("id") UUID id, 
                     @Param("status") OutboxEventEntity.OutboxStatus status, 
                     @Param("processedAt") Instant processedAt);
    
    /**
     * Remove eventos processados antigos
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity o WHERE o.status = :status AND o.processedAt < :cutoffDate")
    void deleteOldProcessedEvents(@Param("status") OutboxEventEntity.OutboxStatus status, 
                                 @Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Conta eventos por status
     */
    long countByStatus(OutboxEventEntity.OutboxStatus status);
}

