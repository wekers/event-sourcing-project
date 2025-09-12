package com.example.eventsourcing.query.application.readmodel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PedidoCompletoDTO {
    private UUID id;
    private String numeroPedido;
    private UUID clienteId;
    private String clienteNome;
    private String clienteEmail;
    private StatusPedido status;
    private BigDecimal valorTotal;
    private Instant dataCriacao;
    private Instant dataAtualizacao;
    private Instant dataCancelamento;
    private String observacoes;
    private List<ItemPedidoCompletoDTO> itens;
    private EnderecoEntregaDTO enderecoEntrega;
    private Long version;

    @Data
    @Builder
    public static class ItemPedidoCompletoDTO {
        private UUID produtoId;
        private String produtoNome;
        private String produtoDescricao;
        private Integer quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal valorTotal;

        public BigDecimal getValorTotal() {
            if (valorTotal != null) return valorTotal;
            if (precoUnitario != null && quantidade != null) {
                return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
            }
            return BigDecimal.ZERO;
        }
    }

    @Data
    @Builder
    public static class EnderecoEntregaDTO {
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String estado;
        private String cep;
        private String pontoReferencia;
    }
}