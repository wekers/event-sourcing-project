package com.example.eventsourcing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxStatusUpdater {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "outbox.public.event_outbox", groupId = "command-service-outbox-updater-group")
    @Transactional
    public void listen(ConsumerRecord<String, String> record) {
        log.info("Received message from Kafka topic {} for outbox status update: partition {}, offset {}, key {}, value {}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

        try {

            JsonNode rootNode = objectMapper.readTree(record.value());
            JsonNode opNode = rootNode.get("op");

            JsonNode afterNode = null;
            if (opNode != null && "c".equals(opNode.asText())) {
                afterNode = rootNode.get("after");
            }

            if (afterNode != null) {
                UUID outboxEventId = UUID.fromString(afterNode.get("id").asText());

                outboxEventRepository.findById(outboxEventId).ifPresent(outboxEvent -> {
                    outboxEvent.setStatus(OutboxEventEntity.OutboxStatus.PROCESSED);
                    outboxEvent.setProcessedAt(Instant.now());
                    outboxEventRepository.save(outboxEvent);
                    log.info("Outbox event {} status updated to PROCESSED.", outboxEventId);
                });
            } else {
                log.warn("Payload 'after' node is null for Kafka message: {}", record.value());
            }

        } catch (Exception e) {
            log.error("Error processing Kafka message for outbox status update: {}", record.value(), e);
            // Dependendo da estratégia de tratamento de erros, pode-se lançar uma exceção
            // para que a mensagem seja reprocessada ou movida para uma DLQ do Kafka.
        }


    }
}


