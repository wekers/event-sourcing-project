package com.example.eventsourcing.web;

import com.example.eventsourcing.application.command.PedidoCommandService;
import com.example.eventsourcing.domain.pedido.EnderecoEntrega;
import com.example.eventsourcing.domain.pedido.ItemPedido;
import com.example.eventsourcing.domain.pedido.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoCommandController {
    
    private final PedidoCommandService commandService;
    
    @PostMapping
    public ResponseEntity<CriarPedidoResponse> criarPedido(@Valid @RequestBody CriarPedidoRequest request) {
        try {
            var command = new PedidoCommandService.CriarPedidoCommand(
                    request.numeroPedido,
                    request.clienteId,
                    request.clienteNome,
                    request.clienteEmail,
                    request.itens,
                    request.enderecoEntrega
            );
            
            UUID pedidoId = commandService.criarPedido(command);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CriarPedidoResponse(pedidoId));
                    
        } catch (Exception e) {
            log.error("Erro ao criar pedido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{pedidoId}")
    public ResponseEntity<Void> atualizarPedido(@PathVariable UUID pedidoId,
                                               @Valid @RequestBody AtualizarPedidoRequest request) {
        try {
            var command = new PedidoCommandService.AtualizarPedidoCommand(
                    pedidoId,
                    request.itens,
                    request.enderecoEntrega,
                    request.observacoes
            );
            
            commandService.atualizarPedido(command);
            
            return ResponseEntity.ok().build();
            
        } catch (PedidoCommandService.PedidoNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (PedidoCommandService.ConcurrencyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Erro ao atualizar pedido: {}", pedidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{pedidoId}/status")
    public ResponseEntity<Void> atualizarStatusPedido(@PathVariable UUID pedidoId,
                                                     @Valid @RequestBody AtualizarStatusRequest request) {
        try {
            var command = new PedidoCommandService.AtualizarStatusCommand(pedidoId, request.novoStatus);
            commandService.atualizarStatusPedido(command);
            return ResponseEntity.ok().build();
        } catch (PedidoCommandService.PedidoNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (PedidoCommandService.ConcurrencyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Erro ao atualizar status do pedido: {}", pedidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{pedidoId}")
    public ResponseEntity<Void> cancelarPedido(@PathVariable UUID pedidoId,
                                               @Valid @RequestBody CancelarPedidoRequest request) {
        try {
            var command = new PedidoCommandService.CancelarPedidoCommand(pedidoId, request.motivo);
            commandService.cancelarPedido(command);
            return ResponseEntity.ok().build();

        } catch (PedidoCommandService.PedidoNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | PedidoCommandService.ConcurrencyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Erro ao cancelar pedido: {}", pedidoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    
    // DTOs
    
    public record CriarPedidoRequest(
            @NotBlank String numeroPedido,
            @NotNull UUID clienteId,
            @NotBlank String clienteNome,
            @NotBlank String clienteEmail,
            @NotEmpty List<ItemPedido> itens,
            @NotNull EnderecoEntrega enderecoEntrega
    ) {}
    
    public record CriarPedidoResponse(UUID pedidoId) {}
    
    public record AtualizarPedidoRequest(
            @NotEmpty List<ItemPedido> itens,
            @NotNull EnderecoEntrega enderecoEntrega,
            String observacoes
    ) {}
    
    public record AtualizarStatusRequest(
            @NotNull StatusPedido novoStatus
    ) {}

    public record CancelarPedidoRequest(
            @NotBlank String motivo
    ) {}
}
