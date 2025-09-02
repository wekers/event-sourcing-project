package com.example.eventsourcing.application.readmodel;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPedido(
        UUID produtoId,
        String produtoNome,
        String produtoDescricao,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal valorTotal) {

    public static ItemPedido from(com.example.eventsourcing.domain.pedido.ItemPedido item) {
        return new ItemPedido(
                item.getProdutoId(),
                item.getProdutoNome(),
                item.getProdutoDescricao(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getValorTotal()
        );
    }
}
