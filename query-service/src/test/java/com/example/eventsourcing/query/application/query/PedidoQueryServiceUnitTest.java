package com.example.eventsourcing.query.application.query;

import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.events.EnderecoEntrega;
import com.example.eventsourcing.query.application.events.ItemPedido;
import com.example.eventsourcing.query.application.readmodel.PedidoCompletoDTO;
import com.example.eventsourcing.query.application.readmodel.PedidoDTO;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.query.application.readmodel.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - PedidoQueryService")
class PedidoQueryServiceUnitTest {

    @Mock
    private PedidoReadModelRepository readModelRepository;

    @InjectMocks
    private PedidoQueryService pedidoQueryService;

    private UUID pedidoId;
    private UUID clienteId;
    private PedidoReadModel pedidoReadModel;

    @BeforeEach
    void setUp() {
        pedidoId = UUID.randomUUID();
        clienteId = UUID.randomUUID();

        // Criar dados de teste de forma mais simples
        pedidoReadModel = criarPedidoReadModelCompleto();
    }

    private PedidoReadModel criarPedidoReadModelCompleto() {
        List<ItemPedido> itens = List.of(
                new ItemPedido(UUID.randomUUID(), "Produto A", "Descrição A", 2,
                        BigDecimal.valueOf(50.00), BigDecimal.valueOf(100.00))
        );

        EnderecoEntrega endereco = new EnderecoEntrega("Rua Teste", "123", null,
                "Centro", "São Paulo", "SP", "12345-678", null);

        return PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-123")
                .clienteId(clienteId)
                .clienteNome("João Silva")
                .clienteEmail("joao@email.com")
                .itens(itens)
                .enderecoEntrega(endereco)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(StatusPedido.PENDENTE)
                .dataCriacao(Instant.now())
                .dataAtualizacao(Instant.now())
                .version(1L)
                .build();
    }

    // TESTES BÁSICOS DE BUSCA
    @Test
    @DisplayName("Deve buscar pedido por ID")
    void deveBuscarPedidoPorId() {
        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoReadModel));

        Optional<PedidoDTO> result = pedidoQueryService.findById(pedidoId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(pedidoId);
    }

    @Test
    @DisplayName("Deve retornar vazio quando pedido não existe")
    void deveRetornarVazioQuandoPedidoNaoExiste() {
        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.empty());

        Optional<PedidoDTO> result = pedidoQueryService.findById(pedidoId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar pedido por número")
    void deveBuscarPedidoPorNumero() {
        when(readModelRepository.findByNumeroPedido("PED-123")).thenReturn(Optional.of(pedidoReadModel));

        Optional<PedidoDTO> result = pedidoQueryService.findByNumeroPedido("PED-123");

        assertThat(result).isPresent();
        assertThat(result.get().numeroPedido()).isEqualTo("PED-123");
    }

    // TESTES DE PEDIDO COMPLETO
    @Test
    @DisplayName("Deve buscar pedido completo por ID")
    void deveBuscarPedidoCompletoPorId() {
        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoReadModel));

        Optional<PedidoCompletoDTO> result = pedidoQueryService.findPedidoCompletoById(pedidoId);

        assertThat(result).isPresent();
        PedidoCompletoDTO dto = result.get();

        assertThat(dto.getId()).isEqualTo(pedidoId);
        assertThat(dto.getNumeroPedido()).isEqualTo("PED-123");
        assertThat(dto.getItens()).hasSize(1);
        assertThat(dto.getEnderecoEntrega()).isNotNull();
    }

    @Test
    @DisplayName("Deve buscar pedido completo por número")
    void deveBuscarPedidoCompletoPorNumero() {
        when(readModelRepository.findByNumeroPedido("PED-123")).thenReturn(Optional.of(pedidoReadModel));

        Optional<PedidoCompletoDTO> result = pedidoQueryService.findPedidoCompletoByNumero("PED-123");

        assertThat(result).isPresent();
        assertThat(result.get().getNumeroPedido()).isEqualTo("PED-123");
    }

    // TESTES DE LISTAGEM COM PAGINAÇÃO
    @Test
    @DisplayName("Deve listar pedidos por cliente")
    void deveListarPedidosPorCliente() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PedidoReadModel> page = new PageImpl<>(List.of(pedidoReadModel), pageable, 1);

        when(readModelRepository.findByClienteIdOrderByDataCriacaoDesc(clienteId, pageable)).thenReturn(page);

        Page<PedidoDTO> result = pedidoQueryService.findPedidosPorCliente(clienteId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).clienteId()).isEqualTo(clienteId);
    }

    @Test
    @DisplayName("Deve listar pedidos por status")
    void deveListarPedidosPorStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PedidoReadModel> page = new PageImpl<>(List.of(pedidoReadModel), pageable, 1);

        when(readModelRepository.findByStatusOrderByDataCriacaoDesc("PENDENTE", pageable)).thenReturn(page);

        Page<PedidoDTO> result = pedidoQueryService.findByStatus(StatusPedido.PENDENTE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(StatusPedido.PENDENTE);
    }

    // TESTES DE ESTATÍSTICAS
    @Test
    @DisplayName("Deve contar pedidos por status")
    void deveContarPedidosPorStatus() {
        when(readModelRepository.countByStatus("PENDENTE")).thenReturn(5L);

        long result = pedidoQueryService.countByStatus(StatusPedido.PENDENTE);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("Deve contar pedidos por cliente")
    void deveContarPedidosPorCliente() {
        when(readModelRepository.countByClienteId(clienteId)).thenReturn(3L);

        long result = pedidoQueryService.countByClienteId(clienteId);

        assertThat(result).isEqualTo(3L);
    }

    // TESTES DE CONVERSÃO
    @Test
    @DisplayName("Deve converter PedidoDTO corretamente")
    void deveConverterPedidoDTOCorretamente() {
        PedidoDTO dto = PedidoDTO.from(pedidoReadModel);

        assertThat(dto.id()).isEqualTo(pedidoReadModel.getId());
        assertThat(dto.numeroPedido()).isEqualTo(pedidoReadModel.getNumeroPedido());
        assertThat(dto.clienteId()).isEqualTo(pedidoReadModel.getClienteId());
        assertThat(dto.status()).isEqualTo(pedidoReadModel.getStatus());
    }

    @Test
    @DisplayName("Deve lidar com pedido sem itens")
    void deveLidarComPedidoSemItens() {
        // Criar pedido sem itens
        PedidoReadModel pedidoSemItens = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-456")
                .clienteId(clienteId)
                .clienteNome("Maria Silva")
                .status(StatusPedido.PENDENTE)
                .valorTotal(BigDecimal.ZERO)
                .dataCriacao(Instant.now())
                .itens(null) // Sem itens
                .version(1L)
                .build();

        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoSemItens));

        Optional<PedidoCompletoDTO> result = pedidoQueryService.findPedidoCompletoById(pedidoId);

        assertThat(result).isPresent();
        // Agora deve retornar lista vazia em vez de null
        assertThat(result.get().getItens()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Deve lidar com pedido sem endereço")
    void deveLidarComPedidoSemEndereco() {
        // Criar pedido sem endereço
        PedidoReadModel pedidoSemEndereco = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-789")
                .clienteId(clienteId)
                .clienteNome("Carlos Silva")
                .status(StatusPedido.PENDENTE)
                .valorTotal(BigDecimal.valueOf(50.00))
                .dataCriacao(Instant.now())
                .itens(List.of(new ItemPedido(UUID.randomUUID(), "Produto", "Desc", 1,
                        BigDecimal.valueOf(50.00), BigDecimal.valueOf(50.00))))
                .enderecoEntrega(null) // Sem endereço
                .version(1L)
                .build();

        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoSemEndereco));

        Optional<PedidoCompletoDTO> result = pedidoQueryService.findPedidoCompletoById(pedidoId);

        assertThat(result).isPresent();
        assertThat(result.get().getEnderecoEntrega()).isNull();
        // Itens devem estar presentes (não vazios)
        assertThat(result.get().getItens()).isNotEmpty().hasSize(1);
    }


    @Test
    @DisplayName("Deve lidar com pedido vazio (sem itens e sem endereço)")
    void deveLidarComPedidoVazio() {
        // Criar pedido completamente vazio
        PedidoReadModel pedidoVazio = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido("PED-000")
                .clienteId(clienteId)
                .clienteNome("Pedro Silva")
                .status(StatusPedido.PENDENTE)
                .valorTotal(BigDecimal.ZERO)
                .dataCriacao(Instant.now())
                .itens(null) // Sem itens
                .enderecoEntrega(null) // Sem endereço
                .version(1L)
                .build();

        when(readModelRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoVazio));

        Optional<PedidoCompletoDTO> result = pedidoQueryService.findPedidoCompletoById(pedidoId);

        assertThat(result).isPresent();
        // Itens deve ser lista vazia (não null)
        assertThat(result.get().getItens()).isNotNull().isEmpty();
        // Endereço pode ser null
        assertThat(result.get().getEnderecoEntrega()).isNull();
    }

}