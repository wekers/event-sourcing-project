package com.example.eventsourcing.command.infrastructure.rebuild;

import com.example.eventsourcing.command.domain.AggregateRoot;
import com.example.eventsourcing.command.domain.Event;
import com.example.eventsourcing.command.infrastructure.EventStore;
import com.example.eventsourcing.command.infrastructure.SnapshotStore;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public void rebuildAggregate(UUID aggregateId) {
        try {
            List<Event> events = eventStore.getEvents(aggregateId);
            if (events == null || events.isEmpty()) {
                log.warn("Nenhum evento encontrado para aggregate {}", aggregateId);
                return;
            }

            Optional<String> optAggregateType = eventStore.getAggregateType(aggregateId);
            if (optAggregateType.isEmpty()) {
                throw new IllegalArgumentException("Aggregate type não encontrado: " + aggregateId);
            }

            String aggregateType = optAggregateType.get();
            log.debug("aggregateType (raw) from DB = {}", aggregateType);

            // resolve a classe do aggregate de forma tolerante
            Class<?> aggregateClass = resolveAggregateClass(aggregateType);
            log.debug("Resolved aggregate class = {}", aggregateClass.getName());

            AggregateRoot aggregate = (AggregateRoot) aggregateClass.getDeclaredConstructor().newInstance();
            aggregate.setId(aggregateId);

            // aplica eventos (EventStore já converte eventos para objetos do domínio)
            aggregate.loadFromHistory(events);

            log.info("Aggregate {} reidratado até a versão {}", aggregateId, aggregate.getVersion());

            // salva snapshot atualizado
            snapshotStore.saveSnapshot(aggregate);
            log.info("Snapshot atualizado para aggregate {}", aggregateId);

        } catch (Exception e) {
            log.error("Erro ao reconstruir aggregate {}", aggregateId, e);
            throw new RuntimeException("Falha ao reconstruir aggregate " + aggregateId, e);
        }
    }

    /**
     * Tenta resolver a classe do aggregate de maneira flexível:
     * - se aggregateType já for a classe do aggregate -> usa ela;
     * - se aggregateType for a classe de um event -> deriva o package base e constrói a FQN do aggregate;
     * - se aggregateType for simples (ex: "Pedido") -> tenta pacotes padrão.
     */
    private Class<?> resolveAggregateClass(String aggregateType) throws ClassNotFoundException {
        // 1) tenta carregar diretamente (se for FQN correto)
        try {
            Class<?> c = Class.forName(aggregateType);
            // se já for aggregate root
            if (AggregateRoot.class.isAssignableFrom(c)) {
                return c;
            }
            // se for Event (ex.: com....events.PedidoCriado), derivamos o aggregate
            if (Event.class.isAssignableFrom(c)) {
                String pkg = c.getPackage().getName(); // ex: com.example...pedido.events
                String basePkg = pkg;
                if (basePkg.endsWith(".events")) {
                    basePkg = basePkg.substring(0, basePkg.length() - ".events".length()); // ...pedido
                } else {
                    // comportamento defensivo: remove último segmento de package
                    int idx = basePkg.lastIndexOf('.');
                    if (idx > 0) basePkg = basePkg.substring(0, idx);
                }
                String lastSegment = basePkg.substring(basePkg.lastIndexOf('.') + 1); // "pedido"
                String aggregateSimple = Character.toUpperCase(lastSegment.charAt(0)) + lastSegment.substring(1); // "Pedido"
                String aggregateFqn = basePkg + "." + aggregateSimple;
                log.debug("Derived aggregate FQN from event: {}", aggregateFqn);
                return Class.forName(aggregateFqn);
            }
            // caso carregue alguma classe que não é aggregate nem event, continuar tentativa abaixo
        } catch (ClassNotFoundException ex) {
            // ignora e tenta heurísticas abaixo
            log.debug("Class.forName('{}') falhou, tentando heurísticas...", aggregateType);
        }

        // 2) se aggregateType for apenas simples ("Pedido" ou "PedidoCriado") -> heurística para pacote padrão
        if (!aggregateType.contains(".")) {
            // tenta pacote padrão de aggregates (ajuste se seu package for diferente)
            String candidate = "com.example.eventsourcing.command.domain." + aggregateType.toLowerCase() + "." + capitalize(aggregateType);
            log.debug("Trying candidate aggregate FQN: {}", candidate);
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException e) {
                // ignora e tentará outro candidato
            }
            // outro candidato genérico
            String candidate2 = "com.example.eventsourcing.command.domain." + capitalize(aggregateType);
            log.debug("Trying candidate aggregate FQN: {}", candidate2);
            try {
                return Class.forName(candidate2);
            } catch (ClassNotFoundException e) {
                // ignora
            }
        }

        // 3) se chegou aqui não conseguiu resolver -> lança
        throw new ClassNotFoundException("Não foi possível resolver classe de aggregate a partir de: " + aggregateType);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Reidrata todos os agregados existentes (limpa snapshots e recria).
     */
    @Transactional
    public void rebuildAllAggregates() {
        try {
            List<UUID> aggregateIds = eventStore.getAllAggregateIds();

            log.info("Iniciando reidratação de {} agregados", aggregateIds.size());

            // apaga todos snapshots antes de reconstruir
            snapshotStore.deleteAll();

            for (UUID id : aggregateIds) {
                rebuildAggregate(id);
            }

            log.info("✅ Reidratação completa de todos os agregados");

        } catch (Exception e) {
            log.error("Erro ao reconstruir todos os agregados", e);
            throw new RuntimeException("Falha ao reconstruir todos os agregados", e);
        }
    }
}
