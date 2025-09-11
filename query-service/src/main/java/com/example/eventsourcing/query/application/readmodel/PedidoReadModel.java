package com.example.eventsourcing.query.application.readmodel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collection = "pedido_read")
public class PedidoReadModel {

    @Id
    private UUID id; // usa UUID diretamente como _id
    @Indexed(unique = true)
    private String numeroPedido;
    private UUID clienteId;
    private String clienteNome;
    private String clienteEmail;
    private String status;
    private BigDecimal valorTotal;
    private Instant dataCriacao;
    private Instant dataAtualizacao;
    private Instant dataCancelamento;
    private String observacoes;
    private List<ItemPedido> itens;
    private EnderecoEntrega enderecoEntrega;
    private Long version;

}