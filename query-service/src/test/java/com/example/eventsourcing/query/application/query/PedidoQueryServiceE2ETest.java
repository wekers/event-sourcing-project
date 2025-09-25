package com.example.eventsourcing.query.application.query;

import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.query.application.readmodel.StatusPedido;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Testes Funcionais E2E - Query Service")
class PedidoQueryServiceE2ETest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PedidoReadModelRepository repository;

    @Autowired
    private ObjectMapper objectMapper; // Adicione isso

    private String baseUrl;
    private UUID pedidoId;
    private UUID clienteId;
    private String numeroPedido;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/pedidos";
        repository.deleteAll();

        pedidoId = UUID.randomUUID();
        clienteId = UUID.randomUUID();
        numeroPedido = "PED-E2E-" + System.currentTimeMillis();

        // Criar dados de teste (mesmo código)
        PedidoReadModel pedido1 = PedidoReadModel.builder()
                .id(pedidoId)
                .numeroPedido(numeroPedido)
                .clienteId(clienteId)
                .clienteNome("Cliente E2E")
                .clienteEmail("cliente@email.com")
                .status(StatusPedido.PENDENTE)
                .valorTotal(BigDecimal.valueOf(150.00))
                .dataCriacao(Instant.now())
                .version(1L)
                .build();

        PedidoReadModel pedido2 = PedidoReadModel.builder()
                .id(UUID.randomUUID())
                .numeroPedido("PED-E2E-2")
                .clienteId(clienteId)
                .clienteNome("Cliente E2E")
                .clienteEmail("cliente@email.com")
                .status(StatusPedido.CONFIRMADO)
                .valorTotal(BigDecimal.valueOf(250.00))
                .dataCriacao(Instant.now())
                .version(1L)
                .build();

        repository.save(pedido1);
        repository.save(pedido2);
    }

    // Método auxiliar para extrair totalElements do JSON
    private int extractTotalElements(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        return root.path("totalElements").asInt();
    }

    @Test
    @DisplayName("Fluxo completo: Buscar pedido por ID")
    void fluxoBuscarPedidoPorId() {
        ResponseEntity<PedidoReadModel> response = restTemplate.getForEntity(
                baseUrl + "/{pedidoId}", PedidoReadModel.class, pedidoId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(pedidoId);
        assertThat(response.getBody().getNumeroPedido()).isEqualTo(numeroPedido);
    }

    @Test
    @DisplayName("Fluxo completo: Buscar pedido por número")
    void fluxoBuscarPedidoPorNumero() {
        ResponseEntity<PedidoReadModel> response = restTemplate.getForEntity(
                baseUrl + "/numero/{numeroPedido}", PedidoReadModel.class, numeroPedido);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNumeroPedido()).isEqualTo(numeroPedido);
    }

    @Test
    @DisplayName("Fluxo completo: Listar pedidos por cliente")
    void fluxoListarPedidosPorCliente() throws Exception {
        // Use String.class e parse manualmente
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/cliente/{clienteId}?page=0&size=10", String.class, clienteId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        int totalElements = extractTotalElements(response.getBody());
        assertThat(totalElements).isEqualTo(2);
    }


    @Test
    @DisplayName("Fluxo completo: Buscar pedidos por status")
    void fluxoBuscarPedidosPorStatus() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/status/{status}?page=0&size=10", String.class, "PENDENTE");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        int totalElements = extractTotalElements(response.getBody());
        assertThat(totalElements).isEqualTo(1);
    }

    @Test
    @DisplayName("Fluxo completo: Buscar pedido completo")
    void fluxoBuscarPedidoCompleto() {
        ResponseEntity<PedidoReadModel> response = restTemplate.getForEntity(
                baseUrl + "/{pedidoId}/completo", PedidoReadModel.class, pedidoId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(pedidoId);
    }

    @Test
    @DisplayName("Fluxo completo: Estatísticas por status")
    void fluxoEstatisticasPorStatus() {
        ResponseEntity<Long> response = restTemplate.getForEntity(
                baseUrl + "/estatisticas/status/{status}/count", Long.class, "PENDENTE");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Fluxo completo: Estatísticas por cliente")
    void fluxoEstatisticasPorCliente() {
        ResponseEntity<Long> response = restTemplate.getForEntity(
                baseUrl + "/estatisticas/cliente/{clienteId}/count", Long.class, clienteId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Fluxo completo: Total gasto por cliente")
    void fluxoTotalGastoPorCliente() {
        ResponseEntity<Double> response = restTemplate.getForEntity(
                baseUrl + "/estatisticas/cliente/{clienteId}/total-gasto", Double.class, clienteId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(400.00);
    }

    @Test
    @DisplayName("Fluxo completo: Listar todos os pedidos")
    void fluxoListarTodosPedidos() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "?page=0&size=10", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        int totalElements = extractTotalElements(response.getBody());
        assertThat(totalElements).isEqualTo(2);
    }
}