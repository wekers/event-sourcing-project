package com.example.eventsourcing.command.domain.pedido;

import com.example.eventsourcing.command.domain.pedido.events.PedidoCancelado;
import com.example.eventsourcing.command.domain.pedido.events.PedidoConfirmado;
import com.example.eventsourcing.command.domain.pedido.events.PedidoCriado;
import com.example.eventsourcing.command.domain.pedido.events.PedidoEmPreparacao;
import com.example.eventsourcing.command.domain.pedido.events.PedidoEnviado;
import com.example.eventsourcing.command.domain.pedido.events.PedidoEntregue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PedidoTest {

    // IDs e valores fixos para testes determinísticos
    private static final UUID FIXED_PRODUTO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIXED_CLIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private EnderecoEntrega criarEnderecoPadrao() {
        return new EnderecoEntrega(
                "Rua X", "123", "Apto 10", "Centro", "São Paulo", "SP", "11111-111", "Próx à praça"
        );
    }

    private ItemPedido criarItemPadrao() {
        return new ItemPedido(FIXED_PRODUTO_ID, "Produto A", "Descrição", 2, new BigDecimal(10));
    }

    private Pedido criarPedidoBasico() {
        return new Pedido(
                UUID.randomUUID(),
                "12345",
                FIXED_CLIENTE_ID,
                "João",
                "joao@email.com",
                List.of(criarItemPadrao()),
                criarEnderecoPadrao()
        );
    }

    @Test
    void naoDevePermitirModificarListaDeItensExternamente() {
        Pedido pedido = criarPedidoBasico();
        List<ItemPedido> itens = pedido.getItens();
        assertThatThrownBy(() -> itens.add(criarItemPadrao()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // -----------------------
    // Criação
    // -----------------------
    @Test
    void deveCriarPedidoComStatusPendente() {
        Instant inicio = Instant.now();
        Pedido pedido = criarPedidoBasico();
        Instant fim = Instant.now();

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo(new BigDecimal("20.00"));        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getDataCriacao()).isNotNull();
        assertThat(pedido.getDataCriacao()).isBetween(inicio, fim);
    }

    // -----------------------
    // Atualização
    // -----------------------
    @Test
    void deveAtualizarPedidoComSucesso() {
        Pedido pedido = criarPedidoBasico();

        EnderecoEntrega novoEndereco = new EnderecoEntrega(
                "Av Brasil", "999", "Sala 2", "Bela Vista", "Campinas", "SP", "22222-222", "Próx mercado"
        );
        ItemPedido novoItem = new ItemPedido(FIXED_PRODUTO_ID, "Produto B", "Novo", 1, new BigDecimal("30.00"));

        pedido.atualizar(List.of(novoItem), novoEndereco, "Observações de teste");

        assertThat(pedido.getItens())
                .hasSize(1)
                .allSatisfy(i -> {
                    assertThat(i.getProdutoNome()).isEqualTo("Produto B");
                    assertThat(i.getQuantidade()).isEqualTo(1);
                    assertThat(i.getPrecoUnitario()).isEqualTo(new BigDecimal("30.00"));
                });
        assertThat(pedido.getValorTotal()).isEqualTo(new BigDecimal("30.00"));
        assertThat(pedido.getObservacoes()).isEqualTo("Observações de teste");
        assertThat(pedido.getEnderecoEntrega())
                .usingRecursiveComparison()
                .isEqualTo(novoEndereco);
    }

    @Test
    void devePermitirAtualizarObservacoesParaNulo() {
        Pedido pedido = criarPedidoBasico();
        EnderecoEntrega novoEndereco = criarEnderecoPadrao();
        ItemPedido novoItem = criarItemPadrao();

        pedido.atualizar(List.of(novoItem), novoEndereco, null);

        assertThat(pedido.getObservacoes()).isNull();
    }

    @Test
    void devePermitirAtualizarObservacoesParaVazio() {
        Pedido pedido = criarPedidoBasico();
        EnderecoEntrega novoEndereco = criarEnderecoPadrao();
        ItemPedido novoItem = criarItemPadrao();

        pedido.atualizar(List.of(novoItem), novoEndereco, "");

        assertThat(pedido.getObservacoes()).isEmpty();
    }

    @Test
    void naoDeveAtualizarPedidoCancelado() {
        Pedido pedido = criarPedidoBasico();
        pedido.cancelar("Cliente desistiu");

        assertThatThrownBy(() ->
                pedido.atualizar(List.of(criarItemPadrao()), criarEnderecoPadrao(), "Tentativa")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveAtualizarPedidoEntregue() {
        Pedido pedido = criarPedidoBasico();
        pedido.confirmar();
        pedido.iniciarPreparacao();
        pedido.enviar();
        pedido.entregar();

        assertThatThrownBy(() ->
                pedido.atualizar(List.of(criarItemPadrao()), criarEnderecoPadrao(), "Tentativa")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveAtualizarPedidoForaDoStatusPendente() {
        Pedido pedido = criarPedidoBasico();
        pedido.confirmar();

        assertThatThrownBy(() ->
                pedido.atualizar(List.of(criarItemPadrao()), criarEnderecoPadrao(), "Tentativa")
        ).isInstanceOf(IllegalStateException.class);
    }

    // -----------------------
    // Cancelamento
    // -----------------------
    @Test
    void deveCancelarPedidoEmPreparacaoOuEnviado() {
        // Pode cancelar enquanto não estiver ENTREGUE ou CANCELADO
        for (StatusPedido status : List.of(StatusPedido.CONFIRMADO, StatusPedido.EM_PREPARACAO, StatusPedido.ENVIADO)) {
            Pedido pedido = criarPedidoBasico();
            if (status == StatusPedido.CONFIRMADO) pedido.confirmar();
            if (status == StatusPedido.EM_PREPARACAO) { pedido.confirmar(); pedido.iniciarPreparacao();}
            if (status == StatusPedido.ENVIADO) { pedido.confirmar(); pedido.iniciarPreparacao(); pedido.enviar(); }
            pedido.cancelar("Teste multi-status");
            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(pedido.getObservacoes()).isEqualTo("Teste multi-status");
        }
    }

    @Test
    void deveCancelarPedidoPendente() {
        Pedido pedido = criarPedidoBasico();
        pedido.cancelar("Cliente desistiu");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getObservacoes()).isEqualTo("Cliente desistiu");
        assertThat(pedido.getDataCancelamento()).isNotNull();
        assertThat(pedido.getDataCancelamento()).isAfter(pedido.getDataCriacao());
    }

    @Test
    void naoDeveCancelarPedidoJaCancelado() {
        Pedido pedido = criarPedidoBasico();
        pedido.cancelar("Primeiro cancelamento");

        assertThatThrownBy(() -> pedido.cancelar("Segundo cancelamento"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveCancelarPedidoEntregue() {
        Pedido pedido = criarPedidoBasico();
        pedido.confirmar();
        pedido.iniciarPreparacao();
        pedido.enviar();
        pedido.entregar();

        assertThatThrownBy(() -> pedido.cancelar("Cliente desistiu"))
                .isInstanceOf(IllegalStateException.class);
    }

    // -----------------------
    // Fluxo de Status
    // -----------------------
    @Test
    void deveSeguirFluxoCompletoDeStatus() {
        Pedido pedido = criarPedidoBasico();

        pedido.confirmar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);

        pedido.iniciarPreparacao();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_PREPARACAO);

        pedido.enviar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENVIADO);

        pedido.entregar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    void naoDeveConfirmarPedidoJaConfirmado() {
        Pedido pedido = criarPedidoBasico();
        pedido.confirmar();

        assertThatThrownBy(pedido::confirmar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveIniciarPreparacaoDePedidoPendente() {
        Pedido pedido = criarPedidoBasico();

        assertThatThrownBy(pedido::iniciarPreparacao)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveEnviarPedidoQueNaoEstaEmPreparacao() {
        Pedido pedido = criarPedidoBasico();

        assertThatThrownBy(pedido::enviar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoDeveEntregarPedidoQueNaoFoiEnviado() {
        Pedido pedido = criarPedidoBasico();

        assertThatThrownBy(pedido::entregar)
                .isInstanceOf(IllegalStateException.class);
    }

    // -----------------------
    // Atualizar Status via atualizarStatus()
    // -----------------------
    @Test
    void atualizarStatusDeveChamarMetodosCorretos() {
        Pedido pedido = criarPedidoBasico();

        pedido.atualizarStatus(StatusPedido.CONFIRMADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);

        pedido.atualizarStatus(StatusPedido.EM_PREPARACAO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_PREPARACAO);

        pedido.atualizarStatus(StatusPedido.ENVIADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENVIADO);

        pedido.atualizarStatus(StatusPedido.ENTREGUE);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    void naoDevePermitirDefinirStatusPendenteDiretamente() {
        Pedido pedido = criarPedidoBasico();

        assertThatThrownBy(() -> pedido.atualizarStatus(StatusPedido.PENDENTE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveCancelarViaAtualizarStatus() {
        Pedido pedido = criarPedidoBasico();
        pedido.atualizarStatus(StatusPedido.CANCELADO);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getObservacoes()).contains("Cancelado via atualização de status");
    }

    @Test
    void naoDeveAtualizarStatusDePedidoJaCancelado() {
        Pedido pedido = criarPedidoBasico();
        pedido.cancelar("Motivo qualquer");
        assertThatThrownBy(() -> pedido.atualizarStatus(StatusPedido.CONFIRMADO))
            .isInstanceOf(IllegalStateException.class);
    }

    // Testes para status inválidos na atualização via atualizarStatus
    @Test
    void naoDeveAtualizarStatusParaStatusInvalido() {
        Pedido pedido = criarPedidoBasico();
        // Simulando um status não tratado
        assertThatThrownBy(() -> pedido.atualizarStatus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------
    // Rebuild via Histórico
    // -----------------------
    @Test
    void deveReconstruirPedidoDoHistorico() {
        ItemPedido item = criarItemPadrao();
        EnderecoEntrega endereco = criarEnderecoPadrao();

        UUID aggregateId = UUID.randomUUID();

        PedidoCriado eventoCriado = new PedidoCriado(
                aggregateId, Instant.now(), 1L, "12345",
                FIXED_CLIENTE_ID, "João", "joao@email.com",
                List.of(item), endereco, BigDecimal.valueOf(20.00)
        );
        PedidoConfirmado eventoConfirmado = new PedidoConfirmado(eventoCriado.aggregateId(), Instant.now(), 2L);

        Pedido pedido = new Pedido();
        pedido.loadFromHistory(List.of(eventoCriado, eventoConfirmado));

        assertThat(pedido.getNumeroPedido()).isEqualTo("12345");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getItens().getFirst().getProdutoNome()).isEqualTo("Produto A");
        assertThat(pedido.getEnderecoEntrega())
                .usingRecursiveComparison()
                .isEqualTo(endereco);
        assertThat(pedido.getValorTotal()).isEqualTo(BigDecimal.valueOf(20.00));
    }

    @Test
    void deveReconstruirHistoricoComTodosOsEventos() {
        ItemPedido item = criarItemPadrao();
        EnderecoEntrega endereco = criarEnderecoPadrao();

        UUID aggregateId = UUID.randomUUID();

        List<PedidoCriado> eventos = new ArrayList<>();
        PedidoCriado eventoCriado = new PedidoCriado(
                aggregateId, Instant.now(), 1L, "12345",
                FIXED_CLIENTE_ID, "João", "joao@email.com",
                List.of(item), endereco, BigDecimal.valueOf(20.00));

        PedidoConfirmado eventoConfirmado = new PedidoConfirmado(aggregateId, Instant.now(), 2L);
        PedidoEmPreparacao eventoPrep = new PedidoEmPreparacao(aggregateId, Instant.now(), 3L);
        PedidoEnviado eventoEnviado = new PedidoEnviado(aggregateId, Instant.now(), 4L);
        PedidoEntregue eventoEntregue = new PedidoEntregue(aggregateId, Instant.now(), 5L);
        PedidoCancelado eventoCancelado = new PedidoCancelado(aggregateId, Instant.now(), 6L, "Motivo X");

        Pedido pedido = new Pedido();
        pedido.loadFromHistory(List.of(
                eventoCriado, 
                eventoConfirmado,
                eventoPrep, 
                eventoEnviado, 
                eventoEntregue, 
                eventoCancelado
        ));

        assertThat(pedido.getNumeroPedido()).isEqualTo("12345");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getDataCancelamento()).isNotNull();
        assertThat(pedido.getObservacoes()).isEqualTo("Motivo X");
    }

    @Test
    void pedidoVazioDeveTerEstadoInicialNulo() {
        Pedido pedido = new Pedido();
        assertThat(pedido.getNumeroPedido()).isNull();
        assertThat(pedido.getClienteId()).isNull();
        assertThat(pedido.getStatus()).isNull();
        assertThat(pedido.getItens()).isEmpty();
        assertThat(pedido.getEnderecoEntrega()).isNull();
    }
}
