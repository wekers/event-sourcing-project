package com.example.eventsourcing.query.application.events;

import com.example.eventsourcing.query.application.readmodel.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        BigDecimal valorTotal,
        StatusPedido status
) {}
