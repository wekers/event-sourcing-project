package com.example.eventsourcing.query.application.readmodel;

import com.example.eventsourcing.command.domain.pedido.StatusPedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido_read")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoReadModel {
    
    @Id
    private UUID id;
    
    @Column(name = "numero_pedido", nullable = false, unique = true)
    private String numeroPedido;
    
    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;
    
    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;
    
    @Column(name = "cliente_email", nullable = false)
    private String clienteEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusPedido status;
    
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    
    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;
    
    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;
    
    @Column(name = "data_cancelamento")
    private Instant dataCancelamento;
    
    @Column(name = "observacoes")
    private String observacoes;
    
    @Column(name = "itens", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ItemPedido> itens;

    @Column(name = "endereco_entrega", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private EnderecoEntrega enderecoEntrega;

    @Version
    @Column(name = "version")
    private Long version = 0L;
}

