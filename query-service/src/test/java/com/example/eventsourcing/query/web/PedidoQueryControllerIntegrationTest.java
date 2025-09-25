package com.example.eventsourcing.query.web;

import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.query.application.readmodel.StatusPedido;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Testes de Integração - PedidoQueryController")
class PedidoQueryControllerIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoReadModelRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID pedidoId;
    private UUID clienteId;
    private String numeroPedido;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        pedidoId = UUID.randomUUID();
        clienteId = UUID.randomUUID();
        numeroPedido = "PED-" + System.currentTimeMillis();

        PedidoReadModel pedido = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido(numeroPedido)
                .clienteId(clienteId)
                .clienteNome("João Silva")
                .clienteEmail("joao@email.com")
                .status(StatusPedido.PENDENTE)
                .valorTotal(BigDecimal.valueOf(200.00))
                .dataCriacao(Instant.now())
                .dataAtualizacao(Instant.now())
                .version(1L)
                .build();

        repository.save(pedido);
    }

    @Test
    @DisplayName("Deve buscar pedido por ID com sucesso")
    void deveBuscarPedidoPorId() throws Exception {
        mockMvc.perform(get("/api/pedidos/{pedidoId}", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pedidoId.toString())))
                .andExpect(jsonPath("$.numeroPedido", is(numeroPedido)))
                .andExpect(jsonPath("$.clienteId", is(clienteId.toString())))
                .andExpect(jsonPath("$.status", is("PENDENTE")));
    }

    @Test
    @DisplayName("Deve retornar 404 quando pedido não encontrado")
    void deveRetornar404QuandoPedidoNaoEncontrado() throws Exception {
        UUID idInexistente = UUID.randomUUID();

        mockMvc.perform(get("/api/pedidos/{pedidoId}", idInexistente))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve buscar pedido por número com sucesso")
    void deveBuscarPedidoPorNumero() throws Exception {
        mockMvc.perform(get("/api/pedidos/numero/{numeroPedido}", numeroPedido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroPedido", is(numeroPedido)));
    }

    @Test
    @DisplayName("Deve buscar pedidos por cliente com paginação")
    void deveBuscarPedidosPorCliente() throws Exception {
        mockMvc.perform(get("/api/pedidos/cliente/{clienteId}", clienteId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clienteId", is(clienteId.toString())));
    }

    @Test
    @DisplayName("Deve buscar pedidos por status")
    void deveBuscarPedidosPorStatus() throws Exception {
        mockMvc.perform(get("/api/pedidos/status/{status}", "PENDENTE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PENDENTE")));
    }

    @Test
    @DisplayName("Deve buscar pedido completo por ID")
    void deveBuscarPedidoCompletoPorId() throws Exception {
        mockMvc.perform(get("/api/pedidos/{pedidoId}/completo", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pedidoId.toString())))
                .andExpect(jsonPath("$.numeroPedido", is(numeroPedido)));
    }

    @Test
    @DisplayName("Deve contar pedidos por status")
    void deveContarPedidosPorStatus() throws Exception {
        mockMvc.perform(get("/api/pedidos/estatisticas/status/{status}/count", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Deve contar pedidos por cliente")
    void deveContarPedidosPorCliente() throws Exception {
        mockMvc.perform(get("/api/pedidos/estatisticas/cliente/{clienteId}/count", clienteId))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }
}