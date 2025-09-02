package com.example.eventsourcing.web;

import com.example.eventsourcing.application.query.PedidoQueryService;
import com.example.eventsourcing.application.readmodel.PedidoCompletoDTO;
import com.example.eventsourcing.application.readmodel.PedidoDTO;
import com.example.eventsourcing.domain.pedido.StatusPedido;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoQueryController {

    private final PedidoQueryService queryService;

    @GetMapping("/{pedidoId}")
    public ResponseEntity<PedidoDTO> buscarPedido(@PathVariable UUID pedidoId) {
        return queryService.findById(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numeroPedido}")
    public ResponseEntity<PedidoDTO> buscarPorNumero(@PathVariable String numeroPedido) {
        return queryService.findByNumeroPedido(numeroPedido)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<Page<PedidoDTO>> buscarPorCliente(
            @PathVariable UUID clienteId,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.findPedidosPorCliente(clienteId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PedidoDTO>> buscarPorStatus(
            @PathVariable StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.findByStatus(status, pageable));
    }

    @GetMapping("/periodo")
    public ResponseEntity<Page<PedidoDTO>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.findByPeriodo(inicio, fim, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PedidoDTO>> buscarPedidos(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.search(clienteId, status, inicio, fim, pageable));
    }

    @GetMapping("/{pedidoId}/completo")
    public ResponseEntity<PedidoCompletoDTO> buscarPedidoCompleto(@PathVariable UUID pedidoId) {
        return queryService.findPedidoCompletoById(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numeroPedido}/completo")
    public ResponseEntity<PedidoCompletoDTO> buscarPedidoCompletoPorNumero(
            @PathVariable String numeroPedido) {
        return queryService.findPedidoCompletoByNumero(numeroPedido)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> listarTodosPedidos(Pageable pageable) {
        return ResponseEntity.ok(queryService.findAll(pageable));
    }

    // Novos endpoints para estatísticas
    @GetMapping("/estatisticas/status/{status}/count")
    public ResponseEntity<Long> countPorStatus(@PathVariable StatusPedido status) {
        return ResponseEntity.ok(queryService.countByStatus(status));
    }

    @GetMapping("/estatisticas/cliente/{clienteId}/count")
    public ResponseEntity<Long> countPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(queryService.countByClienteId(clienteId));
    }

    @GetMapping("/estatisticas/cliente/{clienteId}/total-gasto")
    public ResponseEntity<BigDecimal> getTotalGastoPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(queryService.getTotalGastoPorCliente(clienteId));
    }
}
