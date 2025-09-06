package com.example.eventsourcing.command.domain.pedido.events;

import com.example.eventsourcing.command.domain.pedido.StatusPedido;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PedidoEntregue(
        UUID aggregateId,
        Instant timestamp,
        Long version
) implements PedidoEvent {

    @Override
    public StatusPedido status() {
        return StatusPedido.ENTREGUE;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of();
    }
}


