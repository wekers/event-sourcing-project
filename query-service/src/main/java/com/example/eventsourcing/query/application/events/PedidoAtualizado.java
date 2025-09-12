package com.example.eventsourcing.query.application.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoAtualizado(
        UUID aggregateId,
        Instant timestamp,
        Long version,
        List<ItemPedido> itens,
        EnderecoEntrega enderecoEntrega,  // 👈 novo
        BigDecimal valorTotal,
        String observacoes,               // 👈 novo
        String status

) {}
