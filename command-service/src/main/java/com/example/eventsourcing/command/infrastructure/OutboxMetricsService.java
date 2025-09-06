package com.example.eventsourcing.command.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxMetricsService {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelay = 60000) // 1 minuto
    public void logOutboxMetrics() {
        Map<String, Long> metrics = new HashMap<>();

        for (OutboxEventEntity.OutboxStatus status : OutboxEventEntity.OutboxStatus.values()) {
            long count = outboxEventRepository.countByStatus(status);
            metrics.put(status.name(), count);
        }

        log.info("📊 Outbox Metrics: {}", metrics);

        // Alertas para eventos pendentes há muito tempo
        long oldPendingCount = outboxEventRepository.countOldPendingEvents(
                Instant.now().minus(5, ChronoUnit.MINUTES));

        if (oldPendingCount > 0) {
            log.warn("⚠️  {} eventos pendentes há mais de 5 minutos", oldPendingCount);
        }
    }
}