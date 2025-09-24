package com.example.eventsourcing.query.application.readmodel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PedidoDTO(
        UUID id,
        String numeroPedido,
        UUID clienteId,
        String clienteNome,
        String clienteEmail,
        StatusPedido status,
        BigDecimal valorTotal,
        Instant dataCriacao,
        Instant dataAtualizacao
) {
    public static PedidoDTO from(PedidoReadModel readModel) {
        return new PedidoDTO(
                readModel.getId(),
                readModel.getNumeroPedido(),
                readModel.getClienteId(),
                readModel.getClienteNome(),
                readModel.getClienteEmail(),
                readModel.getStatus(),
                readModel.getValorTotal(),
                readModel.getDataCriacao(),
                readModel.getDataAtualizacao()
        );
    }
}