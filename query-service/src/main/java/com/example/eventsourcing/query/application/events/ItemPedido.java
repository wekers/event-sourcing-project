package com.example.eventsourcing.query.application.events;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPedido(
        UUID produtoId,
        String produtoNome,
        String produtoDescricao,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal valorTotal
) {}
