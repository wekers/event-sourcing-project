package com.example.eventsourcing.command.domain.pedido.events;

import com.example.eventsourcing.command.domain.pedido.EnderecoEntrega;
import com.example.eventsourcing.command.domain.pedido.ItemPedido;
import com.example.eventsourcing.command.domain.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public record PedidoCriado(
        UUID aggregateId,
        Instant timestamp,
        Long version,
        String numeroPedido,
        UUID clienteId,
        String clienteNome,
        String clienteEmail,
        List<ItemPedido> itens,
        EnderecoEntrega enderecoEntrega,
        BigDecimal valorTotal
) implements PedidoEvent {

    @Override
    public StatusPedido status() {
        return StatusPedido.PENDENTE;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of();
    }
}


