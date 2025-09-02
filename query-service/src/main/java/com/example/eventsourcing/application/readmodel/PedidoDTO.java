package com.example.eventsourcing.application.readmodel;

import com.example.eventsourcing.domain.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PedidoDTO(
        UUID id,
        String numeroPedido,
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
                readModel.getClienteNome(),
                readModel.getClienteEmail(),
                readModel.getStatus(),
                readModel.getValorTotal(),
                readModel.getDataCriacao(),
                readModel.getDataAtualizacao()
        );
    }
}