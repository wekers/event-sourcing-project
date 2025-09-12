package com.example.eventsourcing.query.application.events;

import java.time.Instant;
import java.util.UUID;

public record PedidoEnviado(
        UUID aggregateId,
        Instant timestamp,
        Long version,
        String codigoRastreamento
) {}
