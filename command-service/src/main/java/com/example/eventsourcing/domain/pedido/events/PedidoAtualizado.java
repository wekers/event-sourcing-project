package com.example.eventsourcing.domain.pedido.events;

import com.example.eventsourcing.domain.pedido.EnderecoEntrega;
import com.example.eventsourcing.domain.pedido.ItemPedido;
import com.example.eventsourcing.domain.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PedidoAtualizado(
        UUID aggregateId,
        Instant timestamp,
        Long version,
        List<ItemPedido> itens,
        EnderecoEntrega enderecoEntrega,
        BigDecimal valorTotal,
        String observacoes,
        StatusPedido currentStatus // Adicionado para manter o status atual do agregado
) implements PedidoEvent {

    @Override
    public StatusPedido status() {
        return currentStatus; // Retorna o status que o pedido tinha antes da atualização de dados
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of();
    }
}


