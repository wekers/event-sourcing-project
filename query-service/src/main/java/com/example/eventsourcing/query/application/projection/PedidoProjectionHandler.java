package com.example.eventsourcing.query.application.projection;

import com.example.eventsourcing.command.domain.Event;
import com.example.eventsourcing.command.domain.pedido.events.*;
import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.readmodel.EnderecoEntrega;
import com.example.eventsourcing.query.application.readmodel.ItemPedido;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoProjectionHandler {

    private final PedidoReadModelRepository readModelRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handlePedidoCriado(PedidoCriado evento) {
        try {
            PedidoReadModel readModel = new PedidoReadModel();
            readModel.setId(evento.aggregateId());
            readModel.setNumeroPedido(evento.numeroPedido());
            readModel.setClienteId(evento.clienteId());
            readModel.setClienteNome(evento.clienteNome());
            readModel.setClienteEmail(evento.clienteEmail());
            readModel.setStatus(evento.status()); // Usa o status do evento
            readModel.setValorTotal(evento.valorTotal());
            readModel.setDataCriacao(evento.timestamp());
            readModel.setDataAtualizacao(evento.timestamp());
            List<ItemPedido> itensReadModel = evento.itens().stream()
                    .map(itemEvent -> ItemPedido.from(itemEvent))
                    .collect(Collectors.toList());

            readModel.setItens(itensReadModel);
            // Converter o endereço
            readModel.setEnderecoEntrega(convertEndereco(evento.enderecoEntrega()));            readModel.setVersion(evento.version());

            readModelRepository.save(readModel);

            log.debug("Created read model for pedido: {}", evento.aggregateId());

        } catch (Exception e) {
            log.error("Error handling PedidoCriado event", e);
            throw e;
        }
    }

    // Método auxiliar para converter EnderecoEntrega
    private EnderecoEntrega convertEndereco(
            com.example.eventsourcing.command.domain.pedido.EnderecoEntrega domainEndereco) {

        if (domainEndereco == null) {
            return null;
        }

        return new EnderecoEntrega(
                domainEndereco.getLogradouro(),
                domainEndereco.getNumero(),
                domainEndereco.getComplemento(),
                domainEndereco.getBairro(),
                domainEndereco.getCidade(),
                domainEndereco.getEstado(),
                domainEndereco.getCep(),
                domainEndereco.getPontoReferencia()
        );
    }

    @Transactional
    public void handlePedidoAtualizado(PedidoAtualizado evento) {
        try {
            Optional<PedidoReadModel> readModelOpt = readModelRepository.findById(evento.aggregateId());

            if (readModelOpt.isPresent()) {
                PedidoReadModel readModel = readModelOpt.get();
                // Converter os itens
                List<ItemPedido> itensReadModel = evento.itens().stream()
                        .map(itemEvent -> ItemPedido.from(itemEvent))
                        .collect(Collectors.toList());

                readModel.setItens(itensReadModel);

                // Converter o endereço
                readModel.setEnderecoEntrega(convertEndereco(evento.enderecoEntrega()));
                readModel.setValorTotal(evento.valorTotal());
                readModel.setObservacoes(evento.observacoes());
                readModel.setDataAtualizacao(evento.timestamp());
                readModel.setVersion(evento.version());
                readModel.setStatus(evento.currentStatus()); // Mantém o status atual do agregado

                readModelRepository.save(readModel);

                log.debug("Updated read model for pedido: {}", evento.aggregateId());
            } else {
                log.warn("Read model not found for pedido: {}", evento.aggregateId());
            }

        } catch (Exception e) {
            log.error("Error handling PedidoAtualizado event", e);
            throw e;
        }
    }

    @Transactional
    public void handlePedidoCancelado(PedidoCancelado evento) {
        try {
            Optional<PedidoReadModel> readModelOpt = readModelRepository.findById(evento.aggregateId());

            if (readModelOpt.isPresent()) {
                PedidoReadModel readModel = readModelOpt.get();
                readModel.setStatus(evento.status()); // Usa o status do evento
                readModel.setDataCancelamento(evento.timestamp());
                readModel.setDataAtualizacao(evento.timestamp());
                readModel.setObservacoes(evento.motivo());
                readModel.setVersion(evento.version());

                readModelRepository.save(readModel);

                log.debug("Cancelled read model for pedido: {}", evento.aggregateId());
            } else {
                log.warn("Read model not found for pedido: {}", evento.aggregateId());
            }

        } catch (Exception e) {
            log.error("Error handling PedidoCancelado event", e);
            throw e;
        }
    }

    @Transactional
    public void handlePedidoConfirmado(PedidoConfirmado evento) {
        try {
            Optional<PedidoReadModel> readModelOpt = readModelRepository.findById(evento.aggregateId());

            if (readModelOpt.isPresent()) {
                PedidoReadModel readModel = readModelOpt.get();
                readModel.setStatus(evento.status());
                readModel.setDataAtualizacao(evento.timestamp());
                readModel.setVersion(evento.version());

                readModelRepository.save(readModel);

                log.debug("Confirmed read model for pedido: {}", evento.aggregateId());
            } else {
                log.warn("Read model not found for pedido: {}", evento.aggregateId());
            }

        } catch (Exception e) {
            log.error("Error handling PedidoConfirmado event", e);
            throw e;
        }
    }

    @Transactional
    public void handlePedidoEmPreparacao(PedidoEmPreparacao evento) {
        try {
            Optional<PedidoReadModel> readModelOpt = readModelRepository.findById(evento.aggregateId());

            if (readModelOpt.isPresent()) {
                PedidoReadModel readModel = readModelOpt.get();
                readModel.setStatus(evento.status());
                readModel.setDataAtualizacao(evento.timestamp());
                readModel.setVersion(evento.version());

                readModelRepository.save(readModel);

                log.debug("In preparation read model for pedido: {}", evento.aggregateId());
            } else {
                log.warn("Read model not found for pedido: {}", evento.aggregateId());
            }

        } catch (Exception e) {
            log.error("Error handling PedidoEmPreparacao event", e);
            throw e;
        }
    }

    @Transactional
    public void handlePedidoEnviado(PedidoEnviado evento) {
        try {
            Optional<PedidoReadModel> readModelOpt = readModelRepository.findById(evento.aggregateId());

            if (readModelOpt.isPresent()) {
                PedidoReadModel readModel = readModelOpt.get();
                readModel.setStatus(evento.status());
                readModel.setDataAtualizacao(evento.timestamp());
                readModel.setVersion(evento.version());

                readModelRepository.save(readModel);

                log.debug("Sent read model for pedido: {}", evento.aggregateId());
            } else {
                log.warn("Read model not found for pedido: {}", evento.aggregateId());
            }

        } catch (Exception e) {
            log.error("Error handling PedidoEnviado event", e);
            throw e;
        }
    }

    @Transactional
    public void handlePedidoEntregue(PedidoEntregue evento) {
        try {
            Optional<PedidoReadModel> readModelOpt = readModelRepository.findById(evento.aggregateId());

            if (readModelOpt.isPresent()) {
                PedidoReadModel readModel = readModelOpt.get();
                readModel.setStatus(evento.status());
                readModel.setDataAtualizacao(evento.timestamp());
                readModel.setVersion(evento.version());

                readModelRepository.save(readModel);

                log.debug("Delivered read model for pedido: {}", evento.aggregateId());
            } else {
                log.warn("Read model not found for pedido: {}", evento.aggregateId());
            }

        } catch (Exception e) {
            log.error("Error handling PedidoEntregue event", e);
            throw e;
        }
    }

    // ======================
    //  MÉTODOS DE REBUILD
    // ======================

    /**
     * Limpa todas as projeções.
     */
    @Transactional
    public void clear() {
        log.info("Limpando projeções de pedidos...");
        readModelRepository.deleteAll();
    }

    /**
     * Aplica um evento genérico, delegando para o método correto.
     */
    @Transactional
    public void onEvent(Event event) {
        if (event instanceof PedidoCriado e) {
            handlePedidoCriado(e);
        } else if (event instanceof PedidoAtualizado e) {
            handlePedidoAtualizado(e);
        } else if (event instanceof PedidoCancelado e) {
            handlePedidoCancelado(e);
        } else if (event instanceof PedidoConfirmado e) {
            handlePedidoConfirmado(e);
        } else if (event instanceof PedidoEmPreparacao e) {
            handlePedidoEmPreparacao(e);
        } else if (event instanceof PedidoEnviado e) {
            handlePedidoEnviado(e);
        } else if (event instanceof PedidoEntregue e) {
            handlePedidoEntregue(e);
        } else {
            log.warn("Evento ignorado na projeção: {}", event.getClass().getSimpleName());
        }
    }

}


