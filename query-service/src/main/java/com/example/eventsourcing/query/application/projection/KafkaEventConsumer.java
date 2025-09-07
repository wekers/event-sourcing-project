package com.example.eventsourcing.query.application.projection;

import com.example.eventsourcing.command.domain.pedido.events.*;
import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.infrastructure.outbox.OutboxClient;
import com.example.eventsourcing.query.application.infrastructure.outbox.OutboxPendingAck;
import com.example.eventsourcing.query.application.infrastructure.outbox.OutboxPendingAckRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final PedidoProjectionHandler pedidoProjectionHandler;
    private final ObjectMapper objectMapper;
    private final PedidoReadModelRepository readModelRepository;
    private final OutboxClient outboxClient;
    private final OutboxPendingAckRepository pendingAckRepository;

    @KafkaListener(
            topics = "outbox.public.event_outbox",
            groupId = "query-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        UUID outboxEventId = null;
        try {
            if (record.value() == null) {
                log.warn("⚠️ Tombstone recebido, ignorando");
                ack.acknowledge();
                return;
            }

            JsonNode rootNode = objectMapper.readTree(record.value());
            if (!"c".equals(rootNode.get("op").asText())) {
                ack.acknowledge();
                return;
            }

            JsonNode afterNode = rootNode.get("after");
            outboxEventId = UUID.fromString(afterNode.get("id").asText());
            String eventType = afterNode.get("event_type").asText();
            String eventDataRaw = afterNode.get("event_data").asText();
            JsonNode eventData = objectMapper.readTree(eventDataRaw);

            UUID aggregateId = UUID.fromString(eventData.get("aggregateId").asText());
            int eventVersion = eventData.get("version").asInt();

            log.info("🎯 Processing {} v{} for pedido {}, outboxId={}",
                    eventType, eventVersion, aggregateId, outboxEventId);

            // deduplicação
            var existingModel = readModelRepository.findById(aggregateId);
            if (existingModel.isPresent() && existingModel.get().getVersion() >= eventVersion) {
                log.debug("⏭️ Ignorando versão duplicada {}", eventVersion);
                try {
                    outboxClient.markAsProcessed(outboxEventId);
                } catch (Exception ex) {
                    log.warn("⚠️ Command-service offline (dup), salvando pendência {}", outboxEventId);
                    pendingAckRepository.save(new OutboxPendingAck(outboxEventId));
                }
                ack.acknowledge();
                return;
            }

            // processa evento
            processEventByType(eventType, eventData);

            // tenta avisar command-service que foi processado
            try {
                outboxClient.markAsProcessed(outboxEventId);
            } catch (Exception ex) {
                log.warn("⚠️ Command-service offline, salvando pendência {}", outboxEventId);
                pendingAckRepository.save(new OutboxPendingAck(outboxEventId));
            }

            // confirma para Kafka (não vamos reprocessar indefinidamente)
            ack.acknowledge();

        } catch (Exception e) {
            log.error("💥 ERRO ao processar Kafka msg, outboxId={}", outboxEventId, e);
            throw new RuntimeException("Erro no KafkaEventConsumer", e);
        }
    }

    private void processEventByType(String eventType, JsonNode eventData) throws Exception {
        switch (eventType) {
            case "PedidoCriado" -> pedidoProjectionHandler.handlePedidoCriado(
                    objectMapper.convertValue(eventData, PedidoCriado.class));
            case "PedidoAtualizado" -> pedidoProjectionHandler.handlePedidoAtualizado(
                    objectMapper.convertValue(eventData, PedidoAtualizado.class));
            case "PedidoCancelado" -> pedidoProjectionHandler.handlePedidoCancelado(
                    objectMapper.convertValue(eventData, PedidoCancelado.class));
            case "PedidoConfirmado" -> pedidoProjectionHandler.handlePedidoConfirmado(
                    objectMapper.convertValue(eventData, PedidoConfirmado.class));
            case "PedidoEmPreparacao" -> pedidoProjectionHandler.handlePedidoEmPreparacao(
                    objectMapper.convertValue(eventData, PedidoEmPreparacao.class));
            case "PedidoEnviado" -> pedidoProjectionHandler.handlePedidoEnviado(
                    objectMapper.convertValue(eventData, PedidoEnviado.class));
            case "PedidoEntregue" -> pedidoProjectionHandler.handlePedidoEntregue(
                    objectMapper.convertValue(eventData, PedidoEntregue.class));
            default -> log.warn("❓ Unknown event type: {}", eventType);
        }
    }
}
