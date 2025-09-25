package com.example.eventsourcing.query.service.unit;

import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.events.*;
import com.example.eventsourcing.query.application.projection.PedidoProjectionHandler;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.query.application.readmodel.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - PedidoProjectionHandler")
class PedidoProjectionHandlerUnitTest {

    @Mock
    private PedidoReadModelRepository readModelRepository;

    @InjectMocks
    private PedidoProjectionHandler projectionHandler;

    private UUID pedidoId;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        pedidoId = UUID.randomUUID();
        clienteId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve criar novo pedido quando receber evento PedidoCriado")
    void deveCriarNovoPedido() {
        // Arrange
        Instant agora = Instant.now();
        PedidoCriado evento = new PedidoCriado(
                pedidoId,
                agora,
                1L,
                "PED-123",
                clienteId,
                "João Silva",
                "joao@email.com",
                List.of(), // Evita NullPointerException, não passe null aqui
                null,
                BigDecimal.valueOf(100.00),
                StatusPedido.PENDENTE
        );

        // Não há necessidade de mock para findById, pois handler não o utiliza

        // Act
        projectionHandler.handlePedidoCriado(evento);

        // Assert
        ArgumentCaptor<PedidoReadModel> captor = ArgumentCaptor.forClass(PedidoReadModel.class);
        verify(readModelRepository).save(captor.capture());

        PedidoReadModel salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(pedidoId);
        assertThat(salvo.getNumeroPedido()).isEqualTo("PED-123");
        assertThat(salvo.getClienteId()).isEqualTo(clienteId);
        assertThat(salvo.getClienteNome()).isEqualTo("João Silva");
        assertThat(salvo.getClienteEmail()).isEqualTo("joao@email.com");
        assertThat(salvo.getItens()).isEmpty();
        assertThat(salvo.getEnderecoEntrega()).isNull();
        assertThat(salvo.getValorTotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(salvo.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(salvo.getDataCriacao()).isEqualTo(agora);
        assertThat(salvo.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve atualizar status do pedido existente")
    void deveAtualizarStatusDoPedidoExistente() {
        // Arrange
        PedidoReadModel pedidoExistente = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-123")
                .status(StatusPedido.PENDENTE)
                .version(1L)
                .build();

        PedidoConfirmado evento = new PedidoConfirmado(pedidoId, Instant.now(), 2L);

        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoExistente));

        // Act
        projectionHandler.handlePedidoConfirmado(evento);

        // Assert
        ArgumentCaptor<PedidoReadModel> captor = ArgumentCaptor.forClass(PedidoReadModel.class);
        verify(readModelRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(captor.getValue().getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Deve cancelar pedido existente")
    void deveCancelarPedidoExistente() {
        // Arrange
        PedidoReadModel pedidoExistente = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-123")
                .status(StatusPedido.PENDENTE)
                .version(1L)
                .build();

        PedidoCancelado evento = new PedidoCancelado(pedidoId,  Instant.now(),2L,"Cliente desistiu");

        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoExistente));

        // Act
        projectionHandler.handlePedidoCancelado(evento);

        // Assert
        ArgumentCaptor<PedidoReadModel> captor = ArgumentCaptor.forClass(PedidoReadModel.class);
        verify(readModelRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(captor.getValue().getDataCancelamento()).isEqualTo(evento.timestamp());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException se pedido não encontrado")
    void deveLancarExcecaoSePedidoNaoEncontrado() {
        // Arrange
        PedidoConfirmado evento = new PedidoConfirmado(pedidoId, Instant.now(), 2L);
        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> projectionHandler.handlePedidoConfirmado(evento));
        verify(readModelRepository, never()).save(any(PedidoReadModel.class));
    }

    @Test
    @DisplayName("Deve processar todos os tipos de evento de status")
    void deveProcessarTodosOsTiposDeEventoDeStatus() {
        // Arrange
        PedidoReadModel pedidoExistente = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-123")
                .status(StatusPedido.PENDENTE)
                .version(1L)
                .build();

        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoExistente));

        // Act & Assert para cada tipo de evento
        projectionHandler.handlePedidoConfirmado(new PedidoConfirmado(pedidoId, Instant.now(), 2L));
        assertStatusSalvo(StatusPedido.CONFIRMADO);

        projectionHandler.handlePedidoEmPreparacao(new PedidoEmPreparacao(pedidoId, Instant.now(), 3L));
        assertStatusSalvo(StatusPedido.EM_PREPARACAO);

        projectionHandler.handlePedidoEnviado(new PedidoEnviado(pedidoId, Instant.now(),4L,"TRK123"));
        assertStatusSalvo(StatusPedido.ENVIADO);

        projectionHandler.handlePedidoEntregue(new PedidoEntregue(pedidoId, Instant.now(), 5L));
        assertStatusSalvo(StatusPedido.ENTREGUE);
    }

    private void assertStatusSalvo(StatusPedido statusEsperado) {
        ArgumentCaptor<PedidoReadModel> captor = ArgumentCaptor.forClass(PedidoReadModel.class);
        verify(readModelRepository, atLeastOnce()).save(captor.capture());

        // Verificar o último pedido salvo
        PedidoReadModel ultimoSalvo = captor.getValue();
        assertThat(ultimoSalvo.getStatus()).isEqualTo(statusEsperado);
    }
}