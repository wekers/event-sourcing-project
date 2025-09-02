package com.example.eventsourcing.domain.pedido.events;

import com.example.eventsourcing.domain.pedido.StatusPedido;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PedidoEmPreparacao(
        UUID aggregateId,
        Instant timestamp,
        Long version
) implements PedidoEvent {

    @Override
    public StatusPedido status() {
        return StatusPedido.EM_PREPARACAO;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of();
    }
}


