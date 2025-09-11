package com.example.eventsourcing.query.application.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxAckRetryJob {

    private final OutboxPendingAckRepository pendingAckRepository;
    private final OutboxClient outboxClient;

    //private static final int MAX_RETRIES = 100; // limite de tentativas
    private static final Duration ACK_TTL = Duration.ofDays(7); // expira ACKs muito antigos

    @Scheduled(fixedDelay = 30000) // a cada 30s
    public void retryPendingAcks() {
        var pendencias = pendingAckRepository.findAll();

        if (pendencias.isEmpty()) {
            return; // nada para fazer
        }

        log.debug("🔄 Tentando reenviar {} ACKs pendentes...", pendencias.size());

        List<OutboxPendingAck> processados = new ArrayList<>();
        List<OutboxPendingAck> descartados = new ArrayList<>();

        for (OutboxPendingAck pending : pendencias) {
            try {
                // Verifica expiração
                if (pending.getCreatedAt().isBefore(Instant.now().minus(ACK_TTL))) {
                    log.warn("🗑️ Descartando ACK expirado para {}", pending.getOutboxEventId());
                    descartados.add(pending);
                    continue;
                }

                // Verifica limite de tentativas
              /*  if (pending.getRetryCount() >= MAX_RETRIES) {
                    log.error("❌ ACK excedeu limite de {} tentativas para {}",
                            MAX_RETRIES, pending.getOutboxEventId());
                    descartados.add(pending);
                    continue;
                }*/

                // Reenvia
                outboxClient.markAsProcessed(pending.getOutboxEventId());
                log.info("✅ ACK reenviado com sucesso para {}", pending.getOutboxEventId());
                processados.add(pending);

            } catch (Exception e) {
                // Falhou → incrementa contador
                pending.incrementRetryCount();
                pendingAckRepository.save(pending); // persiste contador
                log.warn("⚠️ Ainda offline? Tentativa {} falhou para {}",
                        pending.getRetryCount(), pending.getOutboxEventId());
            }
        }

        // Deleta os que foram confirmados ou descartados
        if (!processados.isEmpty()) {
            pendingAckRepository.deleteAll(processados);
        }
        if (!descartados.isEmpty()) {
            pendingAckRepository.deleteAll(descartados);
        }
    }
}

