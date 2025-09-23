package com.example.eventsourcing.command.web;

import com.example.eventsourcing.command.application.command.PedidoCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoCommandController.class)
class PedidoCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoCommandService commandService;

    // Facilita debug e repetibilidade
    private static final UUID FIXED_CLIENTE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_ITEM_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID FIXED_PEDIDO_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("Deve criar um pedido com sucesso e validar binding")
    void criarPedido_Sucesso() throws Exception {
        Mockito.when(commandService.criarPedido(any())).thenReturn(FIXED_PEDIDO_ID);

        String payload = """
                {
                  "numeroPedido": "12345",
                  "clienteId": "%s",
                  "clienteNome": "João Silva",
                  "clienteEmail": "joao@email.com",
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto A",
                      "produtoDescricao": "Descrição do Produto A",
                      "quantidade": 2,
                      "precoUnitario": 10.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua X",
                    "numero": "123",
                    "complemento": "Apto 45",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "estado": "SP",
                    "cep": "12345-678",
                    "pontoReferencia": "Próximo à praça"
                  }
                }
                """.formatted(FIXED_CLIENTE_ID, FIXED_ITEM_ID);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").value(FIXED_PEDIDO_ID.toString()));

        // Captura e valida se algum campo veio nulo, além do tipo
        ArgumentCaptor<PedidoCommandService.CriarPedidoCommand> captor = ArgumentCaptor.forClass(PedidoCommandService.CriarPedidoCommand.class);
        verify(commandService).criarPedido(captor.capture());
        var cmd = captor.getValue();
        assert cmd.numeroPedido().equals("12345");
        assert cmd.clienteId().equals(FIXED_CLIENTE_ID);
        assert cmd.itens().get(0).getProdutoId().equals(FIXED_ITEM_ID);
        assert cmd.enderecoEntrega().getBairro().equals("Centro");
    }

    @Test
    @DisplayName("Deve atualizar um pedido existente e validar binding")
    void atualizarPedido_Sucesso() throws Exception {
        doNothing().when(commandService).atualizarPedido(any());

        String payload = """
                {
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto B",
                      "produtoDescricao": "Descrição do Produto B",
                      "quantidade": 1,
                      "precoUnitario": 15.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Av Brasil",
                    "numero": "456",
                    "complemento": "Sala 10",
                    "bairro": "Bela Vista",
                    "cidade": "São Paulo",
                    "estado": "SP",
                    "cep": "11111-111",
                    "pontoReferencia": "Ao lado do shopping"
                  },
                  "observacoes": "Entregar no horário comercial"
                }
                """.formatted(FIXED_ITEM_ID);

        mockMvc.perform(put("/api/pedidos/{pedidoId}", FIXED_PEDIDO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(isEmptyOrNullString()));

        // Validação do binding
        ArgumentCaptor<PedidoCommandService.AtualizarPedidoCommand> captor = ArgumentCaptor.forClass(PedidoCommandService.AtualizarPedidoCommand.class);
        verify(commandService).atualizarPedido(captor.capture());
        var cmd = captor.getValue();
        assert cmd.pedidoId().equals(FIXED_PEDIDO_ID);
        assert cmd.itens().get(0).getProdutoNome().equals("Produto B");
        assert cmd.enderecoEntrega().getBairro().equals("Bela Vista");
        assert cmd.observacoes().equals("Entregar no horário comercial");
    }

    @Test
    @DisplayName("Deve atualizar status de um pedido e validar binding do enum")
    void atualizarStatusPedido_Sucesso() throws Exception {
        doNothing().when(commandService).atualizarStatusPedido(any());

        String payload = """
                {
                  "novoStatus": "CONFIRMADO"
                }
                """;

        mockMvc.perform(patch("/api/pedidos/{pedidoId}/status", FIXED_PEDIDO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(isEmptyOrNullString()));

        ArgumentCaptor<PedidoCommandService.AtualizarStatusCommand> captor = ArgumentCaptor.forClass(PedidoCommandService.AtualizarStatusCommand.class);
        verify(commandService).atualizarStatusPedido(captor.capture());
        var cmd = captor.getValue();
        assert cmd.pedidoId().equals(FIXED_PEDIDO_ID);
        assert cmd.novoStatus().name().equals("CONFIRMADO");
    }

    @Test
    @DisplayName("Deve cancelar um pedido existente e validar binding")
    void cancelarPedido_Sucesso() throws Exception {
        doNothing().when(commandService).cancelarPedido(any());

        String payload = """
                {
                  "motivo": "Cliente desistiu"
                }
                """;

        mockMvc.perform(delete("/api/pedidos/{pedidoId}", FIXED_PEDIDO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(isEmptyOrNullString()));

        ArgumentCaptor<PedidoCommandService.CancelarPedidoCommand> captor = ArgumentCaptor.forClass(PedidoCommandService.CancelarPedidoCommand.class);
        verify(commandService).cancelarPedido(captor.capture());
        var cmd = captor.getValue();
        assert cmd.pedidoId().equals(FIXED_PEDIDO_ID);
        assert cmd.motivo().equals("Cliente desistiu");
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar atualizar pedido inexistente e validar body vazio")
    void atualizarPedido_NotFound() throws Exception {
        doThrow(new PedidoCommandService.PedidoNotFoundException("Pedido não encontrado"))
                .when(commandService).atualizarPedido(any());

        String payload = """
                {
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto C",
                      "produtoDescricao": "Descrição",
                      "quantidade": 1,
                      "precoUnitario": 20.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua Z",
                    "numero": "789",
                    "complemento": "Casa",
                    "bairro": "Jardins",
                    "cidade": "Campinas",
                    "estado": "SP",
                    "cep": "22222-222",
                    "pontoReferencia": "Próximo ao mercado"
                  },
                  "observacoes": "Pedido teste"
                }
                """.formatted(FIXED_ITEM_ID);

        mockMvc.perform(put("/api/pedidos/{pedidoId}", FIXED_PEDIDO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(content().string(isEmptyOrNullString()));
    }

    @Test
    @DisplayName("Deve retornar 400 se o número do pedido for nulo (validação @NotBlank)")
    void criarPedido_CampoObrigatorioInvalido() throws Exception {
        String payload = """
                {
                  "numeroPedido": "",
                  "clienteId": "%s",
                  "clienteNome": "João Silva",
                  "clienteEmail": "joao@email.com",
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto A",
                      "produtoDescricao": "Descrição do Produto A",
                      "quantidade": 2,
                      "precoUnitario": 10.00
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua X",
                    "numero": "123",
                    "complemento": "Apto 45",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "estado": "SP",
                    "cep": "12345-678",
                    "pontoReferencia": "Próximo à praça"
                  }
                }
                """.formatted(FIXED_CLIENTE_ID, FIXED_ITEM_ID);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 ao enviar UUID malformado no pathVar")
    void atualizarPedido_ComUUIDInvalido() throws Exception {
        String payload = """
                {
                  "itens": [],
                  "enderecoEntrega": {
                    "logradouro": "Av Brasil",
                    "numero": "456",
                    "complemento": "Sala 10",
                    "bairro": "Bela Vista",
                    "cidade": "São Paulo",
                    "estado": "SP",
                    "cep": "11111-111",
                    "pontoReferencia": "Ao lado do shopping"
                  },
                  "observacoes": "abc"
                }
                """;

        mockMvc.perform(put("/api/pedidos/{pedidoId}", "abc-nota-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 ao passar enum inválido para novoStatus")
    void atualizarStatusPedido_EnumInvalido() throws Exception {
        String payload = """
                { "novoStatus": "INVALIDO" }
                """;

        mockMvc.perform(patch("/api/pedidos/{pedidoId}/status", FIXED_PEDIDO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 409 ao atualizar pedido com conflito de negócio")
    void atualizarPedido_Conflito() throws Exception {
        doThrow(new IllegalStateException("Pedido em estado inválido"))
                .when(commandService).atualizarPedido(any());

        String payload = """
                {
                  "itens": [
                    {
                      "produtoId": "%s",
                      "produtoNome": "Produto B",
                      "produtoDescricao": "Descrição",
                      "quantidade": 1,
                      "precoUnitario": 99.99
                    }
                  ],
                  "enderecoEntrega": {
                    "logradouro": "Rua XX",
                    "numero": "1",
                    "complemento": "Sala 1",
                    "bairro": "Centro",
                    "cidade": "Campinas",
                    "estado": "SP",
                    "cep": "99999-999",
                    "pontoReferencia": "Perto do mercado"
                  },
                  "observacoes": "Conflito"
                }
                """.formatted(FIXED_ITEM_ID);

        mockMvc.perform(put("/api/pedidos/{pedidoId}", FIXED_PEDIDO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}
