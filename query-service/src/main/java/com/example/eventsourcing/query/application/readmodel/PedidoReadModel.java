package com.example.eventsourcing.query.application.readmodel;

import com.example.eventsourcing.query.application.events.EnderecoEntrega;
import com.example.eventsourcing.query.application.events.ItemPedido;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pedido_read")
public class PedidoReadModel {

    @Id
    private UUID id;

    private String numeroPedido;

    @Field("cliente_id") // ← Mapeia para o campo no MongoDB
    private UUID clienteId;
    @Field("cliente_nome")
    private String clienteNome;
    @Field("cliente_email")
    private String clienteEmail;

    private List<ItemPedido> itens;
    private EnderecoEntrega enderecoEntrega;

    public static final String FIELD_VALOR_TOTAL = "valor_total";

    @Field(value = FIELD_VALOR_TOTAL, targetType = FieldType.DECIMAL128) // ← Importante para BigDecimal
    private BigDecimal valorTotal;


    private StatusPedido status;

    @Field("data_criacao")
    private Instant dataCriacao;
    private Instant dataAtualizacao;
    private Instant dataCancelamento;

    private String observacoes;

    private Long version;
}
