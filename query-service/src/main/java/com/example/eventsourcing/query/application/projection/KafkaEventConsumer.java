package com.example.eventsourcing.query.application.projection;

import com.example.eventsourcing.command.domain.pedido.events.*;
import com.example.eventsourcing.command.infrastructure.OutboxEventEntity;
import com.example.eventsourcing.command.infrastructure.OutboxEventRepository;
import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final PedidoProjectionHandler pedidoProjectionHandler;
    private final ObjectMapper objectMapper;
    private final PedidoReadModelRepository readModelRepository;
    private final OutboxEventRepository outboxEventRepository; // ✅ Adicionar esta injeção

    @KafkaListener(
            topics = "outbox.public.event_outbox",
            groupId = "query-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String messageKey = record.key();
        long offset = record.offset();
        int partition = record.partition();

        log.info("📥 Processing message: topic={}, partition={}, offset={}, key={}",
                record.topic(), partition, offset, messageKey);

        UUID outboxEventId = null;

        try {
            if (record.value() == null) {
                log.warn("⚠️ Received null value (tombstone), skipping");
                ack.acknowledge();
                return;
            }

            JsonNode rootNode = objectMapper.readTree(record.value());

            String op = rootNode.get("op").asText();
            if (!"c".equals(op)) {
                log.debug("Skipping non-insert operation: {}", op);
                ack.acknowledge();
                return;
            }

            JsonNode afterNode = rootNode.get("after");
            if (afterNode == null || afterNode.isNull()) {
                log.warn("⚠️ After node is null, skipping");
                ack.acknowledge();
                return;
            }

            // EXTRAI outboxEventId para marcar como PROCESSED
            outboxEventId = UUID.fromString(afterNode.get("id").asText());
            String eventType = afterNode.get("event_type").asText();
            String eventDataRaw = afterNode.get("event_data").asText();
            JsonNode eventData = objectMapper.readTree(eventDataRaw);

            // Extrai informações para deduplicação
            UUID aggregateId = UUID.fromString(eventData.get("aggregateId").asText());
            int eventVersion = eventData.get("version").asInt();

            log.info("🎯 Processing {} event for pedido: {}, version: {}, outboxEventId: {}",
                    eventType, aggregateId, eventVersion, outboxEventId);

            // DEDUPLICAÇÃO POR VERSION
            Optional<PedidoReadModel> existingModel = readModelRepository.findById(aggregateId);
            if (existingModel.isPresent() && existingModel.get().getVersion() >= eventVersion) {
                log.debug("⏭️ Skipping event version {} - current version is {}",
                        eventVersion, existingModel.get().getVersion());

                // Mesmo sendo duplicata, marca como PROCESSED
                markAsProcessed(outboxEventId);
                ack.acknowledge();
                return;
            }

            // Processa o evento
            processEventByType(eventType, eventData);

            // MARCA COMO PROCESSED após processamento bem-sucedido
            markAsProcessed(outboxEventId);

            log.info("✅ Successfully processed {} event for pedido: {}", eventType, aggregateId);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("💥 ERROR processing message - partition: {}, offset: {}, key: {}, outboxEventId: {}",
                    partition, offset, messageKey, outboxEventId, e);

            // IMPORTANTE: Não faz acknowledge → Spring Kafka fará retry automático
            throw new RuntimeException("Failed to process Kafka message", e);
        }
    }

    // MÉTODO PARA MARCAR COMO PROCESSED
    private void markAsProcessed(UUID outboxEventId) {
        try {
            outboxEventRepository.updateStatus(
                    outboxEventId,
                    OutboxEventEntity.OutboxStatus.PROCESSED,
                    Instant.now()
            );
            log.debug("✅ Evento {} marcado como PROCESSED", outboxEventId);
        } catch (Exception e) {
            log.error("❌ Erro ao marcar evento {} como PROCESSED", outboxEventId, e);
            // Não lança exception para não afetar o processamento principal
        }
    }

    private void processEventByType(String eventType, JsonNode eventData) throws Exception {
        switch (eventType) {
            case "PedidoCriado" -> {
                PedidoCriado event = objectMapper.convertValue(eventData, PedidoCriado.class);
                pedidoProjectionHandler.handlePedidoCriado(event);
            }
            case "PedidoAtualizado" -> {
                PedidoAtualizado event = objectMapper.convertValue(eventData, PedidoAtualizado.class);
                pedidoProjectionHandler.handlePedidoAtualizado(event);
            }
            case "PedidoCancelado" -> {
                PedidoCancelado event = objectMapper.convertValue(eventData, PedidoCancelado.class);
                pedidoProjectionHandler.handlePedidoCancelado(event);
            }
            case "PedidoConfirmado" -> {
                PedidoConfirmado event = objectMapper.convertValue(eventData, PedidoConfirmado.class);
                pedidoProjectionHandler.handlePedidoConfirmado(event);
            }
            case "PedidoEmPreparacao" -> {
                PedidoEmPreparacao event = objectMapper.convertValue(eventData, PedidoEmPreparacao.class);
                pedidoProjectionHandler.handlePedidoEmPreparacao(event);
            }
            case "PedidoEnviado" -> {
                PedidoEnviado event = objectMapper.convertValue(eventData, PedidoEnviado.class);
                pedidoProjectionHandler.handlePedidoEnviado(event);
            }
            case "PedidoEntregue" -> {
                PedidoEntregue event = objectMapper.convertValue(eventData, PedidoEntregue.class);
                pedidoProjectionHandler.handlePedidoEntregue(event);
            }
            default -> log.warn("❓ Unknown event type: {}", eventType);
        }
    }

    // Listener para Dead Letter Topic
    @KafkaListener(
            topics = "outbox.public.event_outbox.DLT",
            groupId = "query-service-group-dlt"
    )
    public void listenDlt(ConsumerRecord<String, String> record) {
        log.error("💀 DLT MESSAGE RECEIVED - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                record.topic(), record.partition(), record.offset(), record.key());

        // Tenta extrair outboxEventId da mensagem DLT para marcar como FAILED
        try {
            JsonNode rootNode = objectMapper.readTree(record.value());
            JsonNode afterNode = rootNode.get("after");
            if (afterNode != null && !afterNode.isNull()) {
                UUID outboxEventId = UUID.fromString(afterNode.get("id").asText());
                markAsFailed(outboxEventId, "DLT: " + record.value());
            }
        } catch (Exception e) {
            log.error("💀 Error processing DLT message", e);
        }
    }

    // MÉTODO PARA MARCAR COMO FAILED (para DLT)
    private void markAsFailed(UUID outboxEventId, String errorMessage) {
        try {
            // Use o método updateStatus se existir
            outboxEventRepository.updateStatus(
                    outboxEventId,
                    OutboxEventEntity.OutboxStatus.FAILED,
                    Instant.now()
            );
            log.error("💀 Evento {} marcado como FAILED: {}", outboxEventId, errorMessage);
        } catch (Exception e) {
            log.error("Erro ao marcar evento {} como FAILED", outboxEventId, e);

            // Fallback: use findById + save se updateStatus não funcionar
            try {
                outboxEventRepository.findById(outboxEventId).ifPresent(outboxEvent -> {
                    outboxEvent.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
                    outboxEvent.setProcessedAt(Instant.now());
                    outboxEventRepository.save(outboxEvent);
                    log.error("Evento {} marcado como FAILED (fallback)", outboxEventId);
                });
            } catch (Exception ex) {
                log.error("Erro total ao marcar evento como FAILED", ex);
            }
        }
    }
}