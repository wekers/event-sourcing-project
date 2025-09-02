package com.example.eventsourcing.application.projection;

import com.example.eventsourcing.application.PedidoReadModelRepository;
import com.example.eventsourcing.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.domain.pedido.events.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final PedidoProjectionHandler pedidoProjectionHandler;
    private final ObjectMapper objectMapper;
    private final PedidoReadModelRepository readModelRepository;

    @KafkaListener(
            topics = "outbox.public.event_outbox",
            groupId = "query-service-group",
            containerFactory = "kafkaListenerContainerFactory" // Usa a factory configurada automaticamente
    )
    @Transactional
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String messageKey = record.key();
        long offset = record.offset();
        int partition = record.partition();

        log.info("📥 Processing message: topic={}, partition={}, offset={}, key={}",
                record.topic(), partition, offset, messageKey);

        try {
            if (record.value() == null) {
                log.warn("⚠️ Received null value (tombstone), skipping");
                ack.acknowledge();
                return;
            }

            JsonNode rootNode = objectMapper.readTree(record.value());

            String op = rootNode.get("op").asText();
            if (!"c".equals(op) && !"u".equals(op)) {
                log.debug("⏭️ Skipping operation: {}", op);
                ack.acknowledge();
                return;
            }

            JsonNode afterNode = rootNode.get("after");
            if (afterNode == null || afterNode.isNull()) {
                log.warn("⚠️ After node is null, skipping");
                ack.acknowledge();
                return;
            }

            String eventType = afterNode.get("event_type").asText();
            String eventDataRaw = afterNode.get("event_data").asText();
            JsonNode eventData = objectMapper.readTree(eventDataRaw);

            // Extrai informações para deduplicação
            UUID aggregateId = UUID.fromString(eventData.get("aggregateId").asText());
            int eventVersion = eventData.get("version").asInt();

            log.info("🎯 Processing {} event for pedido: {}, version: {}",
                    eventType, aggregateId, eventVersion);

            // ⭐⭐ DEDUPLICAÇÃO POR VERSION ⭐⭐
            Optional<PedidoReadModel> existingModel = readModelRepository.findById(aggregateId);
            if (existingModel.isPresent() && existingModel.get().getVersion() >= eventVersion) {
                log.debug("⏭️ Skipping event version {} - current version is {}",
                        eventVersion, existingModel.get().getVersion());
                ack.acknowledge();
                return;
            }

            // Processa o evento
            processEventByType(eventType, eventData);

            log.info("✅ Successfully processed {} event for pedido: {}", eventType, aggregateId);
            ack.acknowledge(); // Confirma o offset apenas se processado com sucesso

        } catch (Exception e) {
            log.error("💥 ERROR processing message - partition: {}, offset: {}, key: {}",
                    partition, offset, messageKey, e);

            // ⭐⭐ IMPORTANTE: Não faz acknowledge → Spring Kafka fará retry automático ⭐⭐
            // Após 3 tentativas (configurado no application.yml), será enviado para DLT automaticamente
            throw new RuntimeException("Failed to process Kafka message", e);
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

    // ⭐⭐ METODO OPICIONAL: Listener para a Dead Letter Topic ⭐⭐
    @KafkaListener(
            topics = "outbox.public.event_outbox.DLT", // Nome automático do DLT
            groupId = "query-service-group-dlt"
    )
    public void listenDlt(ConsumerRecord<String, String> record) {
        log.error("💀 DLT MESSAGE RECEIVED - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                record.topic(), record.partition(), record.offset(), record.key());

        log.error("💀 DLT Message - Key: {}, Error: {}", record.key(), record.value());


        // Aqui você pode:
        // 1. Logar em um sistema de monitoramento
        // 2. Enviar alerta por email/Slack
        // 3. Salvar em um banco de eventos problemáticos
        // 4. Tentar reprocessamento manual

        // ⭐⭐ IMPORTANTE: Não throw exception aqui para não criar loop infinito ⭐⭐
    }
}