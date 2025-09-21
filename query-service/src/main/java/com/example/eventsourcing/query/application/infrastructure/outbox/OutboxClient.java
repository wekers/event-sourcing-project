package com.example.eventsourcing.query.application.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.command-service.base-url}")
    private String commandServiceBaseUrl;
    public void markAsProcessed(UUID eventId) {
        String url = commandServiceBaseUrl + "/api/admin/outbox/" + eventId + "/processed";
        webClientBuilder.build()
                .post()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .block();

        log.debug("✅ Evento {} marcado como PROCESSED no command-service", eventId);
    }
}


/*@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxClient {

    private final WebClient.Builder webClientBuilder;

    private static final String COMMAND_SERVICE_URL = "http://localhost:8080";
    // 🔧 ajuste conforme porta do command-service

    public void markAsProcessed(UUID outboxEventId) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(COMMAND_SERVICE_URL + "/api/admin/outbox/{id}/processed", outboxEventId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.debug("✅ Evento {} marcado como PROCESSED via command-service", outboxEventId);
        } catch (Exception e) {
            log.error("❌ Falha ao chamar command-service para evento {}", outboxEventId, e);
        }
    }
}
*/