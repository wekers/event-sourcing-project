package com.example.eventsourcing.command.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoCompletoPedidoE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fluxo E2E: Criar, atualizar, confirmar, preparar, enviar, entregar e cancelar pedido")
    void fluxoCompletoPedido() throws Exception {
        // ---------- 1. Criar Pedido ----------
        var criarPayload = """
                {
                  "numeroPedido": "PED-123",
                  "clienteId": "%s",
                  "clienteNome": "Cliente Teste",
                  "clienteEmail": "teste@email.com",
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto A",
                      "produtoDescricao": "Descrição A",
                      "quantidade": 1,
                      "precoUnitario": 100.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua X",
                    "numero": "123",
                    "complemento": "Apto 10",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "estado": "SP",
                    "cep": "12345-678",
                    "pontoReferencia": "Próximo à praça"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        String response = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonNode = objectMapper.readTree(response);
        UUID pedidoId = UUID.fromString(jsonNode.get("pedidoId").asText());

        // ---------- 2. Atualizar Pedido ----------
        var atualizarPayload = """
                {
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto B",
                      "produtoDescricao": "Descrição B",
                      "quantidade": 2,
                      "precoUnitario": 50.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua Nova",
                    "numero": "456",
                    "complemento": "Sala 20",
                    "bairro": "Bela Vista",
                    "cidade": "Campinas",
                    "estado": "SP",
                    "cep": "98765-432",
                    "pontoReferencia": "Ao lado do shopping"
                  },
                  "observacoes": "Entregar rápido"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(put("/api/pedidos/{id}", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizarPayload))
                .andExpect(status().isOk());

        // ---------- 3. Confirmar Pedido ----------
        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk());

        // ---------- 4. Iniciar Preparação ----------
        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"EM_PREPARACAO\"}"))
                .andExpect(status().isOk());

        // ---------- 5. Enviar Pedido ----------
        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ENVIADO\"}"))
                .andExpect(status().isOk());

        // ---------- 6. Entregar Pedido ----------
        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ENTREGUE\"}"))
                .andExpect(status().isOk());

        // ---------- 7. Cancelar Pedido (deve falhar, já entregue) ----------
        mockMvc.perform(delete("/api/pedidos/{id}", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Desistência\"}"))
                .andExpect(status().isConflict());
    }
}
