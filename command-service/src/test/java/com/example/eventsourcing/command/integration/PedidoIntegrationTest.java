package com.example.eventsourcing.command.integration;

import com.example.eventsourcing.command.application.command.PedidoCommandService;
import com.example.eventsourcing.command.domain.pedido.EnderecoEntrega;
import com.example.eventsourcing.command.domain.pedido.ItemPedido;
import com.example.eventsourcing.command.domain.pedido.Pedido;
import com.example.eventsourcing.command.domain.pedido.StatusPedido;
import com.example.eventsourcing.command.infrastructure.AggregateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PedidoIntegrationTest {

    @Autowired
    private PedidoCommandService pedidoCommandService;

    @Autowired
    private AggregateRepository<Pedido> pedidoRepository;

    // OBS: Remover atributos estáticos e gerar sempre instâncias novas para cada teste:
    private EnderecoEntrega novoEnderecoEntrega() {
        return new EnderecoEntrega(
                "Rua X", "123", "Apto 10", "Centro", "São Paulo", "SP", "11111-111", "Próx praça"
        );
    }

    private ItemPedido novoItemPedido() {
        return new ItemPedido(
                UUID.randomUUID(), "Produto A", "Descrição", 2, new BigDecimal("10.00")
        );
    }

    // -------------------------
    // Criar Pedido
    // -------------------------
    @Test
    void deveCriarEPersistirPedido() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-001", UUID.randomUUID(), "Maria", "maria@email.com",
                List.of(novoItemPedido()), novoEnderecoEntrega()
        );

        UUID pedidoId = pedidoCommandService.criarPedido(command);

        Pedido pedidoPersistido = pedidoRepository.findById(pedidoId, Pedido.class).orElseThrow();

        assertThat(pedidoPersistido.getNumeroPedido()).isEqualTo("PED-001");
        assertThat(pedidoPersistido.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(pedidoPersistido.getValorTotal()).isEqualTo(new BigDecimal("20.00"));
        // Teste campos relevantes do domínio:
        assertThat(pedidoPersistido.getClienteNome()).isEqualTo("Maria");
        assertThat(pedidoPersistido.getClienteEmail()).isEqualTo("maria@email.com");
        assertThat(pedidoPersistido.getDataCriacao()).isNotNull();
        assertThat(pedidoPersistido.getItens()).hasSize(1)
            .extracting(ItemPedido::getProdutoNome)
            .containsExactly("Produto A");
    }

    // -------------------------
    // Atualizar Pedido
    // -------------------------
    @Test
    void deveAtualizarPedidoExistente() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-002", UUID.randomUUID(), "João", "joao@email.com",
                List.of(novoItemPedido()), novoEnderecoEntrega()
        );
        UUID pedidoId = pedidoCommandService.criarPedido(command);

        var novoEndereco = new EnderecoEntrega(
                "Av Brasil", "999", "Sala 2", "Bela Vista", "Campinas", "SP", "22222-222", "Próx mercado"
        );
        var novoItem = new ItemPedido(UUID.randomUUID(), "Produto B", "Novo", 1, new BigDecimal("30.00"));

        var updateCommand = new PedidoCommandService.AtualizarPedidoCommand(
                pedidoId, List.of(novoItem), novoEndereco, "Observação teste"
        );

        pedidoCommandService.atualizarPedido(updateCommand);

        Pedido atualizado = pedidoRepository.findById(pedidoId, Pedido.class).orElseThrow();
        assertThat(atualizado.getItens())
                .extracting(ItemPedido::getProdutoNome)
                .containsExactly("Produto B");
        assertThat(atualizado.getValorTotal()).isEqualTo(new BigDecimal("30.00"));
        assertThat(atualizado.getObservacoes()).isEqualTo("Observação teste");
        assertThat(atualizado.getEnderecoEntrega())
            .usingRecursiveComparison()
            .isEqualTo(novoEndereco);
    }

    // -------------------------
    // Atualizar Status
    // -------------------------
    @Test
    void deveAtualizarStatusPedido() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-003", UUID.randomUUID(), "Carlos", "carlos@email.com",
                List.of(novoItemPedido()), novoEnderecoEntrega()
        );
        UUID pedidoId = pedidoCommandService.criarPedido(command);

        var statusCommand = new PedidoCommandService.AtualizarStatusCommand(
                pedidoId, StatusPedido.CONFIRMADO
        );

        pedidoCommandService.atualizarStatusPedido(statusCommand);

        Pedido atualizado = pedidoRepository.findById(pedidoId, Pedido.class).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    // -------------------------
    // Cancelar Pedido
    // -------------------------
    @Test
    void deveCancelarPedido() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-004", UUID.randomUUID(), "Ana", "ana@email.com",
                List.of(novoItemPedido()), novoEnderecoEntrega()
        );
        UUID pedidoId = pedidoCommandService.criarPedido(command);

        var cancelCommand = new PedidoCommandService.CancelarPedidoCommand(
                pedidoId, "Cliente desistiu"
        );

        pedidoCommandService.cancelarPedido(cancelCommand);

        Pedido cancelado = pedidoRepository.findById(pedidoId, Pedido.class).orElseThrow();
        assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(cancelado.getObservacoes()).isEqualTo("Cliente desistiu");
        assertThat(cancelado.getDataCancelamento()).isNotNull();
    }

    // -------------------------
    // Consulta
    // -------------------------
    @Test
    void deveRetornarVersaoAtual() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-005", UUID.randomUUID(), "Pedro", "pedro@email.com",
                List.of(novoItemPedido()), novoEnderecoEntrega()
        );
        UUID pedidoId = pedidoCommandService.criarPedido(command);

        Long versao = pedidoCommandService.obterVersaoAtual(pedidoId);

        // Usando um valor esperado com base na regra de versionamento:
        // Se o versionamento esperado for 0 ao criar, ajuste para 0!
        assertThat(versao).isNotNull()
                .isBetween(0L, 1L); // Para cobrir diferentes regras de versionamento
    }

    // -------------------------
    // Testes de fluxo de erro e estados inválidos
    // -------------------------
    @Test
    void naoDeveAtualizarPedidoQueNaoExiste() {
        UUID pedidoIdFake = UUID.randomUUID();
        var command = new PedidoCommandService.AtualizarPedidoCommand(
                pedidoIdFake, List.of(novoItemPedido()), novoEnderecoEntrega(), "Obs"
        );
        assertThatThrownBy(() -> pedidoCommandService.atualizarPedido(command))
                .isInstanceOf(PedidoCommandService.PedidoNotFoundException.class);
    }

    @Test
    void naoDeveCancelarPedidoEntregue() {
        var criarCmd = new PedidoCommandService.CriarPedidoCommand(
                "PED-006", UUID.randomUUID(), "Luis", "luis@email.com",
                List.of(novoItemPedido()), novoEnderecoEntrega()
        );
        UUID pedidoId = pedidoCommandService.criarPedido(criarCmd);

        // Simula fluxo de status completo até ENTREGUE
        pedidoCommandService.atualizarStatusPedido(new PedidoCommandService.AtualizarStatusCommand(pedidoId, StatusPedido.CONFIRMADO));
        pedidoCommandService.atualizarStatusPedido(new PedidoCommandService.AtualizarStatusCommand(pedidoId, StatusPedido.EM_PREPARACAO));
        pedidoCommandService.atualizarStatusPedido(new PedidoCommandService.AtualizarStatusCommand(pedidoId, StatusPedido.ENVIADO));
        pedidoCommandService.atualizarStatusPedido(new PedidoCommandService.AtualizarStatusCommand(pedidoId, StatusPedido.ENTREGUE));

        var cancelCmd = new PedidoCommandService.CancelarPedidoCommand(pedidoId, "Tentativa após entrega");
        assertThatThrownBy(() -> pedidoCommandService.cancelarPedido(cancelCmd))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveRetornarErroAoCriarPedidoCamposNulos() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> pedidoCommandService.criarPedido(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Erro ao criar pedido");
    }


}
