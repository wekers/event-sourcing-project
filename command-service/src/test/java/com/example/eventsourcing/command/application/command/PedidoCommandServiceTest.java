package com.example.eventsourcing.command.application.command;

import com.example.eventsourcing.command.domain.pedido.EnderecoEntrega;
import com.example.eventsourcing.command.domain.pedido.ItemPedido;
import com.example.eventsourcing.command.domain.pedido.Pedido;
import com.example.eventsourcing.command.domain.pedido.StatusPedido;
import com.example.eventsourcing.command.infrastructure.AggregateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoCommandServiceTest {

    @Mock
    private AggregateRepository<Pedido> pedidoRepository;

    @InjectMocks
    private PedidoCommandService pedidoCommandService;

    private static final UUID FIXED_CLIENTE_ID = UUID.fromString("ba766320-32fe-45f9-87ed-5bfe0b1a0001");
    private static final UUID FIXED_ITEM_ID = UUID.fromString("90649df2-92a2-4895-bb86-0f2fff0c0002");
    private static final UUID FIXED_PEDIDO_ID = UUID.fromString("368e7aca-bc4b-4d07-9fc7-4e48d0200003");

    @Test
    void deveCriarPedidoComSucesso() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-123",
                FIXED_CLIENTE_ID,
                "Cliente Teste",
                "teste@email.com",
                List.of(
                        new ItemPedido(
                                FIXED_ITEM_ID,
                                "Produto A",
                                "Desc A",
                                2,
                                BigDecimal.valueOf(100)
                        )
                ),
                new EnderecoEntrega(
                        "Rua X",
                        "123",
                        null,           // complemento
                        "Centro",       // bairro
                        "SP",           // cidade
                        "SP",           // estado
                        "01000-000",    // cep
                        null            // pontoReferencia
                )
        );

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        UUID pedidoId = pedidoCommandService.criarPedido(command);

        assertNotNull(pedidoId);
        verify(pedidoRepository, times(1)).save(captor.capture());

        Pedido persistido = captor.getValue();
        assertAll("Validação de campos persistidos",
                () -> assertEquals("Cliente Teste", persistido.getClienteNome()),
                () -> assertEquals("PED-123", persistido.getNumeroPedido()),
                () -> assertEquals(FIXED_CLIENTE_ID, persistido.getClienteId()),
                () -> assertEquals("teste@email.com", persistido.getClienteEmail()),
                () -> assertEquals(1, persistido.getItens().size()),
                () -> assertEquals("Produto A", persistido.getItens().get(0).getProdutoNome()),
                () -> assertEquals("Centro", persistido.getEnderecoEntrega().getBairro()),
                () -> assertEquals("SP", persistido.getEnderecoEntrega().getEstado()),
                () -> assertEquals("01000-000", persistido.getEnderecoEntrega().getCep()),
                () -> assertNotNull(persistido.getDataCriacao()),
                () -> assertEquals(0, persistido.getValorTotal().compareTo(new BigDecimal("200.00"))),
                () -> assertEquals(StatusPedido.PENDENTE, persistido.getStatus())
        );
    }

    @Test
    void deveAtualizarPedidoExistenteComDominioReal() {
        // Cria um pedido real (não mock!) para forçar regra de negócio real
        Pedido pedido = new Pedido(
                FIXED_PEDIDO_ID,
                "PED-123",
                FIXED_CLIENTE_ID,
                "Nome Antigo",
                "antigo@email.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "Antigo", "Desc", 3, BigDecimal.valueOf(75))),
                new EnderecoEntrega("Av Velha", "9", null, "Velho", "SP", "SP", "99999-999", null)
        );

        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class))).thenReturn(Optional.of(pedido));

        var novosItens = List.of(new ItemPedido(FIXED_ITEM_ID, "Produto B", "Desc B", 1, BigDecimal.valueOf(50)));
        var novoEndereco = new EnderecoEntrega("Rua Y", "456", null, "Bairro Novo", "RJ", "RJ", "20000-000", null);
        var observacoes = "Observação";

        var command = new PedidoCommandService.AtualizarPedidoCommand(
                FIXED_PEDIDO_ID, novosItens, novoEndereco, observacoes
        );

        pedidoCommandService.atualizarPedido(command);

        verify(pedidoRepository, times(1)).save(pedido);

        assertAll(
                () -> assertEquals(novosItens.size(), pedido.getItens().size()),
                () -> assertEquals("Produto B", pedido.getItens().get(0).getProdutoNome()),
                () -> assertEquals(novoEndereco, pedido.getEnderecoEntrega()),
                () -> assertEquals(0, pedido.getValorTotal().compareTo(new BigDecimal("50.00"))),
                () -> assertEquals(observacoes, pedido.getObservacoes())
        );
    }

    @Test
    void deveLancarExcecaoQuandoAtualizarPedidoEmEstadoInvalido() {
        // Usando domínio real para pegar a mensagem certa
        Pedido pedido = new Pedido(
                FIXED_PEDIDO_ID,
                "PED-CANCELADO",
                FIXED_CLIENTE_ID,
                "Cliente Que Cancellou",
                "email@email.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "Produto", "Desc", 1, BigDecimal.valueOf(10))),
                new EnderecoEntrega("Rua", "1", null, "Centro", "SP", "SP", "11111-111", null)
        );
        pedido.cancelar("Cliente desistiu");

        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class))).thenReturn(Optional.of(pedido));

        var command = new PedidoCommandService.AtualizarPedidoCommand(
                FIXED_PEDIDO_ID,
                List.of(new ItemPedido(FIXED_ITEM_ID, "Produto Teste", "Desc", 1, BigDecimal.valueOf(20))),
                new EnderecoEntrega("Rua Teste", "10", null, "Centro", "SP", "SP", "11111-000", null),
                "Obs qualquer"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> pedidoCommandService.atualizarPedido(command));
        assertTrue(ex.getMessage().contains("cancelado"));
    }

    @Test
    void deveAtualizarStatusDoPedidoComFluxoCompleto() {
        // Cria fluxo real: pendente -> confirmado -> em preparação
        Pedido pedido = new Pedido(
                FIXED_PEDIDO_ID,
                "PED-456",
                FIXED_CLIENTE_ID,
                "Cliente Teste",
                "cliente@email.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "Produto", "Desc", 1, BigDecimal.valueOf(10))),
                new EnderecoEntrega("Rua", "1", null, "Centro", "SP", "SP", "11111-111", null)
        );
        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class))).thenReturn(Optional.of(pedido));

        var cmdConfirma = new PedidoCommandService.AtualizarStatusCommand(FIXED_PEDIDO_ID, StatusPedido.CONFIRMADO);
        pedidoCommandService.atualizarStatusPedido(cmdConfirma);
        assertEquals(StatusPedido.CONFIRMADO, pedido.getStatus());

        var cmdPrepara = new PedidoCommandService.AtualizarStatusCommand(FIXED_PEDIDO_ID, StatusPedido.EM_PREPARACAO);
        pedidoCommandService.atualizarStatusPedido(cmdPrepara);
        assertEquals(StatusPedido.EM_PREPARACAO, pedido.getStatus());

        verify(pedidoRepository, times(2)).save(pedido);
    }

    @Test
    void deveLancarExcecaoQuandoAtualizarStatusEmEstadoInvalido() {
        // Arrange: Pedido já cancelado
        Pedido pedido = new Pedido(
                FIXED_PEDIDO_ID,
                "PED",
                FIXED_CLIENTE_ID,
                "Cliente",
                "email@email.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "Produto", "Desc", 1, BigDecimal.valueOf(10))),
                new EnderecoEntrega("Rua", "1", null, "Centro", "São Paulo", "SP", "11111-111", null)
        );
        pedido.cancelar("Desistiu");

        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class)))
                .thenReturn(Optional.of(pedido));

        var command = new PedidoCommandService.AtualizarStatusCommand(
                FIXED_PEDIDO_ID, StatusPedido.EM_PREPARACAO
        );

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pedidoCommandService.atualizarStatusPedido(command));

        assertEquals("Não é possível iniciar preparação de um pedido que não está CONFIRMADO", ex.getMessage());
    }


    @Test
    void deveCancelarPedido() {
        Pedido pedido = new Pedido(
                FIXED_PEDIDO_ID,
                "PED",
                FIXED_CLIENTE_ID,
                "Cliente Teste",
                "cliente@email.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "Produto", "Desc", 2, BigDecimal.valueOf(101))),
                new EnderecoEntrega("Rua", "10", null, "Bairro", "SP", "SP", "55555-555", null)
        );
        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class))).thenReturn(Optional.of(pedido));

        var command = new PedidoCommandService.CancelarPedidoCommand(FIXED_PEDIDO_ID, "Cliente desistiu apresentação");

        pedidoCommandService.cancelarPedido(command);
        verify(pedidoRepository).save(pedido);

        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertEquals("Cliente desistiu apresentação", pedido.getObservacoes());
        assertNotNull(pedido.getDataCancelamento());
    }

    @Test
    void deveLancarExcecaoQuandoCancelarPedidoEmEstadoInvalido() {
        Pedido pedido = new Pedido(
                FIXED_PEDIDO_ID,
                "PED",
                FIXED_CLIENTE_ID,
                "Cliente",
                "email@email.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "Produto", "Desc", 1, BigDecimal.valueOf(10))),
                new EnderecoEntrega("Rua", "1", null, "Centro", "SP", "SP", "11111-111", null)
        );
        pedido.cancelar("Primeira vez");

        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class))).thenReturn(Optional.of(pedido));

        var command = new PedidoCommandService.CancelarPedidoCommand(FIXED_PEDIDO_ID, "Tentar cancelar de novo");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                pedidoCommandService.cancelarPedido(command));
        assertTrue(ex.getMessage().toLowerCase().contains("já está cancelado"));
    }

    @Test
    void deveLancarExcecaoPedidoNotFoundQuandoAtualizarPedido() {
        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class))).thenReturn(Optional.empty());

        var command = new PedidoCommandService.AtualizarPedidoCommand(
                FIXED_PEDIDO_ID, List.of(
                        new ItemPedido(FIXED_ITEM_ID, "Produto X", "Desc", 1, BigDecimal.valueOf(5))
                ),
                new EnderecoEntrega("R", "1", null, "C", "SP", "SP", "11111-111", null),
                "Obs"
        );

        Exception ex = assertThrows(PedidoCommandService.PedidoNotFoundException.class,
                () -> pedidoCommandService.atualizarPedido(command));
        assertTrue(ex.getMessage().contains(FIXED_PEDIDO_ID.toString()));
    }

    @Test
    void devePropagarConcurrencyExceptionAoAtualizarStatus() {
        when(pedidoRepository.findById(eq(FIXED_PEDIDO_ID), eq(Pedido.class)))
                .thenThrow(new PedidoCommandService.ConcurrencyException("Conflito!", new RuntimeException()));

        var command = new PedidoCommandService.AtualizarStatusCommand(FIXED_PEDIDO_ID, StatusPedido.CONFIRMADO);

        assertThrows(PedidoCommandService.ConcurrencyException.class,
                () -> pedidoCommandService.atualizarStatusPedido(command));
    }

    @Test
    void naoDeveCriarPedidoComCamposObrigatoriosNulos() {
        var command = new PedidoCommandService.CriarPedidoCommand(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> pedidoCommandService.criarPedido(command));
    }

    @Test
    void deveEncapsularErrosNaoTratadosEmIllegalArgumentException() {
        // Arrange
        doThrow(new RuntimeException("Problema inesperado"))
                .when(pedidoRepository)
                .save(any());


        var command = new PedidoCommandService.CriarPedidoCommand(
                "PED-X",
                FIXED_CLIENTE_ID,
                "Cliente X",
                "email@teste.com",
                List.of(new ItemPedido(FIXED_ITEM_ID, "ProdutoX", "DescX", 1, BigDecimal.valueOf(10))),
                new EnderecoEntrega("Rua", "1", null, "Centro", "SP", "SP", "11111-000", null)
        );

        // Act + Assert
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> pedidoCommandService.criarPedido(command)
        );

        assertEquals("Erro ao criar pedido", thrown.getMessage());
        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("Problema inesperado", thrown.getCause().getMessage());
    }

    @Test
    void deveRetornarVersaoAtual() {
        // Arrange
        UUID pedidoId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        long versaoEsperada = 1L;

        // Mock direto do repositório (sem criar pedido real)
        when(pedidoRepository.getCurrentVersion(pedidoId)).thenReturn(versaoEsperada);

        // Act
        Long versaoObtida = pedidoCommandService.obterVersaoAtual(pedidoId);

        // Assert
        assertThat(versaoObtida).isEqualTo(versaoEsperada);
        verify(pedidoRepository).getCurrentVersion(pedidoId);
    }

}
