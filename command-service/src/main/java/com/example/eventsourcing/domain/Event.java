package com.example.eventsourcing.domain;

import com.example.eventsourcing.domain.pedido.events.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Interface base para todos os eventos do sistema
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class") //aparece o nome da classe no JSON

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,       // Usa apenas o "nome" e não o FQN
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"            // Nome do campo no JSON
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PedidoCriado.class, name = "PedidoCriado"),
        @JsonSubTypes.Type(value = PedidoConfirmado.class, name = "PedidoConfirmado"),
        @JsonSubTypes.Type(value = PedidoAtualizado.class, name = "PedidoAtualizado"),
        @JsonSubTypes.Type(value = PedidoEmPreparacao.class, name = "PedidoEmPreparacao"),
        @JsonSubTypes.Type(value = PedidoEnviado.class, name = "PedidoEnviado"),
        @JsonSubTypes.Type(value = PedidoEntregue.class, name = "PedidoEntregue"),
        @JsonSubTypes.Type(value = PedidoCancelado.class, name = "PedidoCancelado")
})
public interface Event {

    UUID getAggregateId();
    String getAggregateType();
    String getEventType();
    Long getVersion();
    Instant getTimestamp();
    Map<String, Object> getMetadata();

    /**
     * Método que deve ser implementado por cada evento
     * para definir como ele deve ser aplicado ao agregado
     */
    void apply(Object aggregate);

}
