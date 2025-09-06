package com.example.eventsourcing.command.domain.pedido.events;

import com.example.eventsourcing.command.domain.Event;
import com.example.eventsourcing.command.domain.pedido.Pedido;
import com.example.eventsourcing.command.domain.pedido.StatusPedido;

import java.time.Instant;
import java.util.UUID;

public sealed interface PedidoEvent extends Event
    permits PedidoCriado, PedidoAtualizado, PedidoConfirmado, PedidoEmPreparacao,
            PedidoEnviado, PedidoEntregue, PedidoCancelado {

    UUID aggregateId();
    Instant timestamp();
    Long version();
    StatusPedido status(); // Cada evento sabe o status final

    @Override
    default UUID getAggregateId() {
        return aggregateId();
    }

    @Override
    default Instant getTimestamp() {
        return timestamp();
    }

    @Override
    default Long getVersion() {
        return version();
    }

    @Override
    default String getEventType() {
        //return this.getClass().getName();
        return this.getClass().getSimpleName();
    }

    @Override
    default String getAggregateType() {
        //return "Pedido"; // Tipo fixo para eventos de Pedido
        return com.example.eventsourcing.command.domain.pedido.Pedido.class.getName();
    }

    @Override
    default void apply(Object aggregate) {
        if (aggregate instanceof Pedido pedido) {
            switch (this) {
                case PedidoCriado e -> pedido.apply(e);
                case PedidoAtualizado e -> pedido.apply(e);
                case PedidoConfirmado e -> pedido.apply(e);
                case PedidoEmPreparacao e -> pedido.apply(e);
                case PedidoEnviado e -> pedido.apply(e);
                case PedidoEntregue e -> pedido.apply(e);
                case PedidoCancelado e -> pedido.apply(e);
            }
        }
    }
}


