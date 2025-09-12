package com.example.eventsourcing.query.application.events;

public record EnderecoEntrega(
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String pontoReferencia
) {}
