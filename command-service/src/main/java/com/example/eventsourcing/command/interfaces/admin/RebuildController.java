package com.example.eventsourcing.command.interfaces.admin;

import com.example.eventsourcing.command.infrastructure.EventStore;
import com.example.eventsourcing.command.infrastructure.rebuild.AggregateRebuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/rebuild")
@RequiredArgsConstructor
@Slf4j
public class RebuildController {

    private final AggregateRebuildService aggregateRebuildService;
    private final EventStore eventStore;

    /**
     * Reidrata um agregado específico a partir do Event Store.
     * Pode receber tanto o aggregateId (event_store) quanto o outboxId (event_outbox).
     */
    @PostMapping("/aggregate/{id}")
    public ResponseEntity<String> rebuildAggregate(@PathVariable UUID id) {
        try {
            UUID aggregateId = resolveAggregateId(id);
            aggregateRebuildService.rebuildAggregate(aggregateId);
            return ResponseEntity.ok("✅ Aggregate " + aggregateId + " reidratado com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao reidratar aggregate {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("❌ Falha ao reidratar aggregate " + id);
        }
    }

    /**
     * Reprocessa todos os eventos de todos os agregados (limpa snapshots e reconstrói).
     */
    @PostMapping("/aggregates")
    public ResponseEntity<String> rebuildAllAggregates() {
        try {
            aggregateRebuildService.rebuildAllAggregates();
            return ResponseEntity.ok("✅ Todos os agregados foram reidratados com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao reidratar todos os agregados", e);
            return ResponseEntity.internalServerError()
                    .body("❌ Falha ao reidratar todos os agregados");
        }
    }

    /**
     * Resolve o aggregateId real, mesmo que o usuário passe um outboxId por engano.
     */
    private UUID resolveAggregateId(UUID id) {
        // 1. Tenta buscar como se fosse aggregateId direto
        if (eventStore.existsAggregateId(id)) {
            return id;
        }
        // 2. Se não achou, tenta resolver pelo outboxId
        return eventStore.findAggregateIdByOutboxId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhum aggregate encontrado para id=" + id));
    }
}
