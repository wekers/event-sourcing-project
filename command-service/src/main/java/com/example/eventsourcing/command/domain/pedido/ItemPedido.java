package com.example.eventsourcing.command.domain.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {
    
    private UUID produtoId;
    private String produtoNome;
    private String produtoDescricao;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    
    public BigDecimal getValorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}

