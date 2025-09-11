package com.example.eventsourcing.query.application.readmodel;

import org.bson.codecs.pojo.annotations.BsonRepresentation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representa um item do pedido no Read Model (MongoDB).
 */
public record ItemPedido(
        UUID produtoId,
        String produtoNome,
        String produtoDescricao,
        Integer quantidade,

        @BsonRepresentation(org.bson.BsonType.DECIMAL128)
        BigDecimal precoUnitario,

        @BsonRepresentation(org.bson.BsonType.DECIMAL128)
        BigDecimal valorTotal
) {
    // 🔹 Método de fábrica: evita dependência direta do "command-service"
    public static ItemPedido from(
            UUID produtoId,
            String produtoNome,
            String produtoDescricao,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal valorTotal
    ) {
        return new ItemPedido(
                produtoId,
                produtoNome,
                produtoDescricao,
                quantidade,
                precoUnitario,
                valorTotal
        );
    }
}
