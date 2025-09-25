package com.example.eventsourcing.command.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/* before run this test - only query-service need up (in local mode) and docker-compose.yml */
@DisplayName("Teste E2E de Integração Simples")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PedidoE2ETest {

    @BeforeAll
    static void ensureCommandServiceIsDown() throws Exception {
        System.out.println("🛑 Parando Command-Service...");
        Runtime.getRuntime().exec("fuser -k 8080/tcp"); // Linux/Mac
        Thread.sleep(2000);
    }

    /*@AfterAll
    static void restartCommandService() throws Exception {
        System.out.println("🚀 Subindo Command-Service novamente...");

        // comando shell: cd no diretório e roda mvn
        String cmd = "cd ~/Spring\\ Boot/event-sourcing-project-latest/meu-sistema-event-sourcing/command-service && mvn spring-boot:run -Dspring-boot.run.profiles=local";

        // executa via bash
        Process p = new ProcessBuilder("bash", "-c", cmd)
                .inheritIO() // joga stdout/stderr para o console
                .start();

        // espera alguns segundos pra subir
        Thread.sleep(15_000); // melhor dar mais tempo que 5s

        System.out.println("✅ Command-Service reiniciado!");
    }*/

    @Test
    void fluxoCompletoPedido() {

        // 1. Criar pedido via Command Service
        String pedidoId = given()
                .port(8080)
                .contentType("application/json")
                .body("""
    {
      "numeroPedido": "PED-E2E",
      "clienteId": "e77a9d70-36e1-4e34-9d75-fbe3a9c86364",
      "clienteNome": "Cliente E2E",
      "clienteEmail": "e2e@email.com",
      "itens": [{
        "produtoId": "bba103f2-69e5-4fd3-bf2d-a6223cf472d3",
        "produtoNome": "Produto E2E",
        "produtoDescricao": "Descrição do Produto E2E",
        "quantidade": 1,
        "precoUnitario": 120.0
      }],
      "enderecoEntrega": {
        "logradouro": "Rua X",
        "numero": "10",
        "bairro": "Centro",
        "cidade": "RJ",
        "estado": "RJ",
        "cep": "22222-000"
      }
    }
    """)
                .when()
                .post("http://localhost:8080/api/pedidos")
                .then()
                .log().all()
                .statusCode(201)
                .body("pedidoId", notNullValue())
                .extract()
                .jsonPath()
                .getString("pedidoId");

        System.out.println("🎯 Pedido criado com ID: " + pedidoId);

        // 2. Consultar no Query Service (com timeout maior)
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            given()
                    .port(8081)
                    .when()
                    .get("http://localhost:8081/api/pedidos/" + pedidoId)
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("id", equalTo(pedidoId))  // ← Note: "id" em vez de "pedidoId"
                    .body("clienteNome", equalTo("Cliente E2E"));
        });

        System.out.println("🎉 Teste E2E concluído com sucesso!");
    }
}