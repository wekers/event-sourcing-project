package com.example.eventsourcing.domain.pedido.events;

import com.example.eventsourcing.domain.pedido.StatusPedido;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PedidoCancelado(
        UUID aggregateId,
        Instant timestamp,
        Long version,
        String motivo
) implements PedidoEvent {

    @Override
    public StatusPedido status() {
        return StatusPedido.CANCELADO;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of();
    }
}


