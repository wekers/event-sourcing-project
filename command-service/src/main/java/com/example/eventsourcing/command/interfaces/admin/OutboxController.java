package com.example.eventsourcing.command.interfaces.admin;


import com.example.eventsourcing.command.infrastructure.OutboxEventEntity;
import com.example.eventsourcing.command.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/outbox")
@RequiredArgsConstructor
@Slf4j
public class OutboxController {

    private final OutboxEventRepository outboxEventRepository;

    @PostMapping("/{id}/processed")
    public ResponseEntity<String> markAsProcessed(@PathVariable UUID id) {
        return outboxEventRepository.findById(id).map(event -> {
            event.setStatus(OutboxEventEntity.OutboxStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);

            log.info("✅ Outbox event {} marcado como PROCESSED", id);
            return ResponseEntity.ok("Evento marcado como PROCESSED: " + id);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}