package com.example.eventsourcing.command.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/* before run this test - both services command-service and query-service need up (in local mode) */
// PRECISA de AMBOS serviços ONLINE
// Testa fluxo COMPLETO (create + read)
// Valida integração REAL entre serviços

@DisplayName("Teste E2E de Integração Complexo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PedidoE2EIntegrationTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private final String commandServiceUrl = "http://localhost:8080";
    private final String queryServiceUrl = "http://localhost:8081";

    @BeforeAll
    static void checkInfrastructure() {
        System.out.println("🔍 Verificando infraestrutura...");

        checkService("PostgreSQL", "localhost", 5435);
        checkService("Kafka", "localhost", 9092);
        checkService("MongoDB", "localhost", 27018);
        checkService("Command Service", "localhost", 8080);
        checkService("Query Service", "localhost", 8081);
    }

    private static void checkService(String serviceName, String host, int port) {
        try {
            java.net.Socket socket = new java.net.Socket(host, port);
            socket.close();
            System.out.println("✅ " + serviceName + " está rodando em " + host + ":" + port);
        } catch (Exception e) {
            System.err.println("❌ " + serviceName + " NÃO está acessível em " + host + ":" + port);
            throw new RuntimeException(serviceName + " não está disponível");
        }
    }

    @BeforeEach
    void setUp() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // Aguarda serviços estarem prontos
        waitForServices();
    }

    private void waitForServices() {
        System.out.println("⏳ Aguardando serviços estarem completamente prontos...");

        // Aguarda Command Service
        Awaitility.await()
                .atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> isServiceReady(commandServiceUrl + "/actuator/health"));

        // Aguarda Query Service
        Awaitility.await()
                .atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> isServiceReady(queryServiceUrl + "/actuator/health"));
    }

    private boolean isServiceReady(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean ready = response.getStatusCode().is2xxSuccessful();
            System.out.println("Health check " + url + ": " + (ready ? "✅" : "❌"));
            return ready;
        } catch (Exception e) {
            System.out.println("Health check failed for " + url + ": " + e.getMessage());
            return false;
        }
    }

    @Test
    @DisplayName("Fluxo completo E2E: criar pedido e verificar integração entre serviços")
    void fluxoCompletoPedido() throws Exception {
        // Dados de teste
        String clienteId = UUID.randomUUID().toString();
        String produtoId = UUID.randomUUID().toString();
        String numeroPedido = "PED-E2E-" + System.currentTimeMillis();

        System.out.println("🎯 Iniciando teste E2E...");
        System.out.println("📦 Número do pedido: " + numeroPedido);

        // 1. Criar pedido no Command Service
        String payload = """
                {
                  "numeroPedido": "%s",
                  "clienteId": "%s",
                  "clienteNome": "João Silva",
                  "clienteEmail": "joao@email.com",
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto X",
                      "produtoDescricao": "Descrição do Produto X",
                      "quantidade": 2,
                      "precoUnitario": 10.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua A",
                    "numero": "100",
                    "complemento": "Casa",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "estado": "SP",
                    "cep": "12345-678",
                    "pontoReferencia": "Próximo à praça"
                  }
                }
                """.formatted(numeroPedido, clienteId, produtoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        System.out.println("📤 Enviando requisição para: " + commandServiceUrl + "/api/pedidos");

        ResponseEntity<String> response = restTemplate.postForEntity(
                commandServiceUrl + "/api/pedidos",
                request,
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        System.out.println("✅ Resposta do Command Service: " + response.getBody());

        String pedidoId = extractPedidoId(response.getBody());
        assertThat(pedidoId).isNotBlank();
        System.out.println("📝 Pedido criado com ID: " + pedidoId);

        // 2. Esperar Query-Service processar e salvar no MongoDB (AUMENTEI O TIMEOUT)
        System.out.println("⏳ Aguardando processamento pelo Query Service (pode demorar alguns segundos)...");

        Awaitility.await()
                .atMost(Duration.ofSeconds(90))  // Aumentei para 90 segundos
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    System.out.println("🔍 Verificando Query Service para pedido: " + pedidoId);

                    try {
                        ResponseEntity<String> queryResponse = restTemplate.getForEntity(
                                queryServiceUrl + "/api/pedidos/" + pedidoId,
                                String.class
                        );

                        System.out.println("📊 Status da resposta do Query Service: " + queryResponse.getStatusCode());

                        if (queryResponse.getStatusCode().is2xxSuccessful()) {
                            String responseBody = queryResponse.getBody();
                            System.out.println("📄 Resposta do Query Service: " + responseBody);

                            JsonNode json = objectMapper.readTree(responseBody);
                            assertThat(json.has("id")).isTrue();
                            assertThat(json.get("id").asText()).isEqualTo(pedidoId);
                            assertThat(json.has("status")).isTrue();

                            String status = json.get("status").asText();
                            System.out.println("🎯 Status do pedido no Query Service: " + status);

                            assertThat(status).isEqualTo("PENDENTE");
                        } else {
                            System.out.println("⚠️  Query Service retornou: " + queryResponse.getStatusCode());
                            throw new AssertionError("Query Service retornou: " + queryResponse.getStatusCode());
                        }

                    } catch (Exception e) {
                        System.out.println("❌ Erro ao consultar Query Service: " + e.getMessage());
                        throw e;
                    }
                });

        System.out.println("🎉 Fluxo E2E concluído com sucesso!");
    }

    // Método auxiliar para debug do Kafka
    private void checkKafkaTopics() {
        try {
            System.out.println("📋 Verificando tópicos do Kafka...");
            // Você pode adicionar uma chamada para listar tópicos se tiver o kafka-ui ou kafkacat
        } catch (Exception e) {
            System.out.println("⚠️  Não foi possível verificar tópicos do Kafka: " + e.getMessage());
        }
    }

    private String extractPedidoId(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("pedidoId").asText();
        } catch (Exception e) {
            System.out.println("⚠️  Erro ao extrair pedidoId, usando fallback: " + e.getMessage());
            // Fallback para regex
            return json.replaceAll(".*\"pedidoId\":\"([^\"]+)\".*", "$1");
        }
    }
}