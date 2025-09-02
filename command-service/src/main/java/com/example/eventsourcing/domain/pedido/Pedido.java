package com.example.eventsourcing.domain.pedido;

import com.example.eventsourcing.domain.AggregateRoot;
import com.example.eventsourcing.domain.Event;
import com.example.eventsourcing.domain.pedido.events.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class Pedido extends AggregateRoot {

    private String numeroPedido;
    private UUID clienteId;
    private String clienteNome;
    private String clienteEmail;
    private StatusPedido status;
    private BigDecimal valorTotal;
    private Instant dataCriacao;
    private Instant dataAtualizacao;
    private Instant dataCancelamento;
    private String observacoes;
    private List<ItemPedido> itens = new ArrayList<>();
    private EnderecoEntrega enderecoEntrega;

    // Construtor para criação de novo pedido
    public Pedido(UUID id, String numeroPedido, UUID clienteId, String clienteNome,
                  String clienteEmail, List<ItemPedido> itens, EnderecoEntrega enderecoEntrega) {

        super(id);

        BigDecimal valorTotal = itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PedidoCriado evento = new PedidoCriado(
                id,
                Instant.now(),
                this.version + 1,
                numeroPedido,
                clienteId,
                clienteNome,
                clienteEmail,
                itens,
                enderecoEntrega,
                valorTotal
        );

        applyNewEvent(evento);
    }

    // ---- Métodos de comportamento ----

    public void atualizar(List<ItemPedido> novosItens, EnderecoEntrega novoEndereco, String observacoes) {
        if (this.status == StatusPedido.CANCELADO || this.status == StatusPedido.ENTREGUE || this.status != StatusPedido.PENDENTE ) {
            throw new IllegalStateException("Não é possível atualizar um pedido que está cancelado ou entregue");
        }


        BigDecimal novoValorTotal = novosItens.stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PedidoAtualizado evento = new PedidoAtualizado(
                this.id, Instant.now(), this.version + 1,
                novosItens, novoEndereco, novoValorTotal, observacoes, this.status
        );

        applyNewEvent(evento);
    }

    public void cancelar(String motivo) {
        if (this.status == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Pedido já está cancelado");
        }
        if (this.status == StatusPedido.ENTREGUE) {
            throw new IllegalStateException("Não é possível cancelar um pedido já entregue");
        }

        PedidoCancelado evento = new PedidoCancelado(this.id, Instant.now(), this.version + 1, motivo);
        applyNewEvent(evento);
    }

    public void confirmar() {
        if (this.status != StatusPedido.PENDENTE) {
            throw new IllegalStateException("Não é possível confirmar um pedido que não está PENDENTE");
        }
        PedidoConfirmado evento = new PedidoConfirmado(this.id, Instant.now(), this.version + 1);
        applyNewEvent(evento);
    }

    public void iniciarPreparacao() {
        if (this.status != StatusPedido.CONFIRMADO) {
            throw new IllegalStateException("Não é possível iniciar preparação de um pedido que não está CONFIRMADO");
        }
        PedidoEmPreparacao evento = new PedidoEmPreparacao(this.id, Instant.now(), this.version + 1);
        applyNewEvent(evento);
    }

    public void enviar() {
        if (this.status != StatusPedido.EM_PREPARACAO) {
            throw new IllegalStateException("Não é possível enviar um pedido que não está EM_PREPARACAO");
        }
        PedidoEnviado evento = new PedidoEnviado(this.id, Instant.now(), this.version + 1);
        applyNewEvent(evento);
    }

    public void entregar() {
        if (this.status != StatusPedido.ENVIADO) {
            throw new IllegalStateException("Não é possível entregar um pedido que não está ENVIADO");
        }
        PedidoEntregue evento = new PedidoEntregue(this.id, Instant.now(), this.version + 1);
        applyNewEvent(evento);
    }

    public void atualizarStatus(StatusPedido novoStatus) {
        switch (novoStatus) {
            case PENDENTE -> throw new IllegalStateException("Não é possível definir o status para PENDENTE diretamente.");
            case CONFIRMADO -> confirmar();
            case EM_PREPARACAO -> iniciarPreparacao();
            case ENVIADO -> enviar();
            case ENTREGUE -> entregar();
            case CANCELADO -> cancelar("Cancelado via atualização de status");
            default -> throw new IllegalArgumentException("Status inválido: " + novoStatus);
        }
    }

    // ---- Aplicação de eventos ----
    public void apply(PedidoCriado evento) {
        this.numeroPedido = evento.numeroPedido();
        this.clienteId = evento.clienteId();
        this.clienteNome = evento.clienteNome();
        this.clienteEmail = evento.clienteEmail();
        this.status = evento.status();
        this.valorTotal = evento.valorTotal();
        this.dataCriacao = evento.timestamp();
        this.dataAtualizacao = evento.timestamp();
        this.itens = new ArrayList<>(evento.itens());
        this.enderecoEntrega = evento.enderecoEntrega();
    }

    public void apply(PedidoAtualizado evento) {
        this.itens = new ArrayList<>(evento.itens());
        this.enderecoEntrega = evento.enderecoEntrega();
        this.valorTotal = evento.valorTotal();
        this.observacoes = evento.observacoes();
        this.dataAtualizacao = evento.timestamp();
    }

    public void apply(PedidoCancelado evento) {
        this.status = evento.status();
        this.dataCancelamento = evento.timestamp();
        this.dataAtualizacao = evento.timestamp();
        this.observacoes = evento.motivo();
    }

    public void apply(PedidoConfirmado evento) {
        this.status = evento.status();
        this.dataAtualizacao = evento.timestamp();
    }

    public void apply(PedidoEmPreparacao evento) {
        this.status = evento.status();
        this.dataAtualizacao = evento.timestamp();
    }

    public void apply(PedidoEnviado evento) {
        this.status = evento.status();
        this.dataAtualizacao = evento.timestamp();
    }

    public void apply(PedidoEntregue evento) {
        this.status = evento.status();
        this.dataAtualizacao = evento.timestamp();
    }

    // ---- Rebuild (histórico) ----
    @Override
    public void loadFromHistory(List<Event> events) {
        for (Event event : events) {
            if (event instanceof PedidoEvent pedidoEvent) {
                applyHistoricalEvent(pedidoEvent); // não coloca em uncommitted
                this.status = pedidoEvent.status();
            } else {
                throw new IllegalArgumentException("Evento inválido no histórico para Pedido: "
                        + event.getClass().getSimpleName());
            }
        }
    }
}
