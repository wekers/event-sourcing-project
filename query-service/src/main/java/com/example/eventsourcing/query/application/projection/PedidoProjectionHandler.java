package com.example.eventsourcing.query.application.projection;

import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.events.*;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.query.application.readmodel.StatusPedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoProjectionHandler {

    private final PedidoReadModelRepository readModelRepository;

    public void handlePedidoCriado(PedidoCriado evento) {
        List<ItemPedido> itensReadModel = evento.itens().stream()
                .map(item -> new ItemPedido(
                        item.produtoId(),
                        item.produtoNome(),
                        item.produtoDescricao(),
                        item.quantidade(),
                        item.precoUnitario(),
                        item.valorTotal()
                ))
                .collect(Collectors.toList());

        PedidoReadModel readModel = PedidoReadModel.builder()
                .id(evento.aggregateId())
                .numeroPedido(evento.numeroPedido())
                .clienteId(evento.clienteId())
                .clienteNome(evento.clienteNome())
                .clienteEmail(evento.clienteEmail())
                .itens(itensReadModel)
                .enderecoEntrega(evento.enderecoEntrega())
                .valorTotal(evento.valorTotal())
                .status(resolveStatus(evento.status(), StatusPedido.PENDENTE))
                .dataCriacao(evento.timestamp())
                .version(evento.version())
                .build();

        readModelRepository.save(readModel);
        log.debug("✅ Created read model for pedido: {}", evento.aggregateId());
    }

    public void handlePedidoAtualizado(PedidoAtualizado evento) {
        if (evento.aggregateId() == null) {
            log.warn("ID do aggregate não pode ser nulo");
            return;
        }

        readModelRepository.findById(evento.aggregateId()).ifPresentOrElse(pedido -> {
            try {
                log.info("Processando atualização do pedido: {}", evento.aggregateId());

                // Debug logs
                log.debug("Status recebido: {}", evento.status());
                log.debug("Itens recebidos: {}", evento.itens());
                log.debug("Valor total: {}", evento.valorTotal());

                boolean mudancas = false;

                // Atualizar itens se fornecidos e diferentes
                if (evento.itens() != null) {
                    List<ItemPedido> itensReadModel = evento.itens().stream()
                            .map(item -> new ItemPedido(
                                    item.produtoId(),
                                    item.produtoNome(),
                                    item.produtoDescricao(),
                                    item.quantidade(),
                                    item.precoUnitario(),
                                    item.valorTotal()
                            ))
                            .collect(Collectors.toList());

                    if (!itensReadModel.equals(pedido.getItens())) {
                        pedido.setItens(itensReadModel);
                        mudancas = true;
                    }
                }

                // Atualizar endereço se fornecido e diferente
                if (evento.enderecoEntrega() != null &&
                        !evento.enderecoEntrega().equals(pedido.getEnderecoEntrega())) {
                    pedido.setEnderecoEntrega(evento.enderecoEntrega());
                    mudancas = true;
                }

                // Atualizar valor total se fornecido e diferente
                if (evento.valorTotal() != null &&
                        !evento.valorTotal().equals(pedido.getValorTotal())) {
                    pedido.setValorTotal(evento.valorTotal());
                    mudancas = true;
                }

                // Atualizar observações se fornecidas e diferentes
                if (evento.observacoes() != null &&
                        !evento.observacoes().equals(pedido.getObservacoes())) {
                    pedido.setObservacoes(evento.observacoes());
                    mudancas = true;
                }

                // Atualizar status se fornecido e válido
                if (evento.status() != null) {
                    try {
                        StatusPedido novoStatus = StatusPedido.valueOf(evento.status());
                        if (!novoStatus.equals(pedido.getStatus())) {
                            pedido.setStatus(novoStatus);
                            mudancas = true;
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Status inválido '{}' para pedido {}, mantendo status atual: {}",
                                evento.status(), evento.aggregateId(), pedido.getStatus());
                    }
                }

                if (mudancas) {
                    pedido.setVersion(evento.version());
                    pedido.setDataAtualizacao(evento.timestamp());
                    readModelRepository.save(pedido);
                    log.info("✅ Pedido {} atualizado com sucesso", evento.aggregateId());
                } else {
                    log.debug("ℹ️  Nenhuma mudança detectada para pedido {}", evento.aggregateId());
                }

            } catch (Exception e) {
                log.error("❌ Erro crítico ao atualizar pedido {}: {}", evento.aggregateId(), e.getMessage(), e);
                throw new RuntimeException("Falha ao processar atualização do pedido", e);
            }
        }, () -> log.warn("⚠️ Pedido {} não encontrado para atualização", evento.aggregateId()));
    }

    @Transactional
    public void handlePedidoCancelado(PedidoCancelado evento) {
        PedidoReadModel pedido = readModelRepository.findById(evento.aggregateId())
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + evento.aggregateId()));

        pedido.setStatus(StatusPedido.CANCELADO); // 🔑 enum local
        pedido.setDataCancelamento(evento.timestamp());
        pedido.setVersion(evento.version());

        readModelRepository.save(pedido);
        log.debug("❌ Pedido {} cancelado", evento.aggregateId());
    }

    @Transactional
    public void handlePedidoConfirmado(PedidoConfirmado evento) {
        PedidoReadModel pedido = readModelRepository.findById(evento.aggregateId())
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + evento.aggregateId()));

        pedido.setStatus(StatusPedido.CONFIRMADO);
        pedido.setVersion(evento.version());

        readModelRepository.save(pedido);
        log.debug("✅ Pedido {} confirmado", evento.aggregateId());
    }

    @Transactional
    public void handlePedidoEmPreparacao(PedidoEmPreparacao evento) {
        PedidoReadModel pedido = readModelRepository.findById(evento.aggregateId())
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + evento.aggregateId()));

        pedido.setStatus(StatusPedido.EM_PREPARACAO);
        pedido.setVersion(evento.version());

        readModelRepository.save(pedido);
        log.debug("👨‍🍳 Pedido {} em preparação", evento.aggregateId());
    }

    @Transactional
    public void handlePedidoEnviado(PedidoEnviado evento) {
        PedidoReadModel pedido = readModelRepository.findById(evento.aggregateId())
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + evento.aggregateId()));

        pedido.setStatus(StatusPedido.ENVIADO);
        pedido.setVersion(evento.version());

        readModelRepository.save(pedido);
        log.debug("📦 Pedido {} enviado", evento.aggregateId());
    }

    @Transactional
    public void handlePedidoEntregue(PedidoEntregue evento) {
        PedidoReadModel pedido = readModelRepository.findById(evento.aggregateId())
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + evento.aggregateId()));

        pedido.setStatus(StatusPedido.ENTREGUE);
        pedido.setVersion(evento.version());

        readModelRepository.save(pedido);
        log.debug("🏁 Pedido {} entregue", evento.aggregateId());
    }

    private StatusPedido resolveStatus(Object statusObj, StatusPedido fallback) {
        if (statusObj == null) return fallback;
        if (statusObj instanceof StatusPedido s) return s;
        if (statusObj instanceof String s) return StatusPedido.valueOf(s);
        return fallback;
    }
}
