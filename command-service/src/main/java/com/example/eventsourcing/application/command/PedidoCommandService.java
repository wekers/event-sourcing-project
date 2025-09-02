package com.example.eventsourcing.application.command;

import com.example.eventsourcing.domain.pedido.EnderecoEntrega;
import com.example.eventsourcing.domain.pedido.ItemPedido;
import com.example.eventsourcing.domain.pedido.Pedido;
import com.example.eventsourcing.domain.pedido.StatusPedido;
import com.example.eventsourcing.infrastructure.AggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoCommandService {
    
    private final AggregateRepository<Pedido> pedidoRepository;
    private static final String PEDIDO_NOT_FOUND_MESSAGE = "Pedido não encontrado: ";
    /**
     * Cria um novo pedido
     */
    @Transactional
    public UUID criarPedido(CriarPedidoCommand command) {
        try {
            UUID pedidoId = UUID.randomUUID();
            
            Pedido pedido = new Pedido(
                    pedidoId,
                    command.numeroPedido(),
                    command.clienteId(),
                    command.clienteNome(),
                    command.clienteEmail(),
                    command.itens(),
                    command.enderecoEntrega()
            );
            
            pedidoRepository.save(pedido);
            
            log.info("Pedido criado: {} para cliente: {}", pedidoId, command.clienteId());
            
            return pedidoId;
            
        } catch (Exception e) {
            log.error("Erro ao criar pedido para cliente: {}", command.clienteId(), e);
            throw new IllegalArgumentException("Erro ao criar pedido", e);
        }
    }
    
    /**
     * Atualiza um pedido existente
     */
    @Transactional
    public void atualizarPedido(AtualizarPedidoCommand command) {
        try {
            Pedido pedido = pedidoRepository.findById(command.pedidoId(), Pedido.class)
                    .orElseThrow(() -> new PedidoNotFoundException( PEDIDO_NOT_FOUND_MESSAGE + command.pedidoId()));
            
            pedido.atualizar(command.itens(), command.enderecoEntrega(), command.observacoes());
            
            pedidoRepository.save(pedido);
            
            log.info("Pedido atualizado: {}", command.pedidoId());
            
        } catch (PedidoNotFoundException e) {
            log.warn("Tentativa de atualizar pedido inexistente: {}", command.pedidoId());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("Tentativa de atualizar pedido em estado inválido: {}", command.pedidoId());
            throw e;
        } catch (ConcurrencyException e) {
            log.warn("Conflito de concorrência ao atualizar pedido: {}", command.pedidoId());
            throw new ConcurrencyException("Conflito de concorrência detectado", e);
        } catch (Exception e) {
            log.error("Erro ao atualizar pedido: {}", command.pedidoId(), e);
            throw new IllegalArgumentException("Erro ao atualizar pedido", e);
        }
    }
    
    /**
     * Atualiza o status de um pedido
     */
    @Transactional
    public void atualizarStatusPedido(AtualizarStatusCommand command) {
        try {
            Pedido pedido = pedidoRepository.findById(command.pedidoId(), Pedido.class)
                    .orElseThrow(() -> new PedidoNotFoundException(PEDIDO_NOT_FOUND_MESSAGE + command.pedidoId()));

            pedido.atualizarStatus(command.novoStatus()); // delega pro agregado

            pedidoRepository.save(pedido);
            log.info("Status do pedido {} atualizado para {}.", command.pedidoId(), command.novoStatus());
        } catch (PedidoNotFoundException e) {
            log.warn("Tentativa de atualizar status de pedido inexistente: {}", command.pedidoId());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("Tentativa de atualizar status de pedido em estado inválido: {}", command.pedidoId());
            throw e;
        } catch (ConcurrencyException e) {
            log.warn("Conflito de concorrência ao atualizar status do pedido: {}", command.pedidoId());
            throw new ConcurrencyException("Conflito de concorrência detectado", e);
        } catch (Exception e) {
            log.error("Erro ao atualizar status do pedido: {}", command.pedidoId(), e);
            throw new IllegalArgumentException("Erro ao atualizar status do pedido", e);
        }
    }

    /**
     * Cancela um pedido
     */
    @Transactional
    public void cancelarPedido(CancelarPedidoCommand command) {
        try {
            Pedido pedido = pedidoRepository.findById(command.pedidoId(), Pedido.class)
                    .orElseThrow(() -> new PedidoNotFoundException(PEDIDO_NOT_FOUND_MESSAGE + command.pedidoId()));

            pedido.cancelar(command.motivo());

            pedidoRepository.save(pedido);

            log.info("Pedido cancelado: {} - Motivo: {}", command.pedidoId(), command.motivo());

        } catch (PedidoNotFoundException e) {
            log.warn("Tentativa de cancelar pedido inexistente: {}", command.pedidoId());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("Tentativa de cancelar pedido em estado inválido: {}", command.pedidoId());
            throw e;
        } catch (ConcurrencyException e) {
            log.warn("Conflito de concorrência ao cancelar pedido: {}", command.pedidoId());
            throw new ConcurrencyException("Conflito de concorrência detectado", e);
        } catch (Exception e) {
            log.error("Erro ao cancelar pedido: {}", command.pedidoId(), e);
            throw new IllegalArgumentException("Erro ao cancelar pedido", e);
        }
    }

    
    /**
     * Busca um pedido por ID (para comandos que precisam do estado atual)
     */
    @Transactional(readOnly = true)
    public Pedido buscarPedido(UUID pedidoId) {
        return pedidoRepository.findById(pedidoId, Pedido.class)
                .orElseThrow(() -> new PedidoNotFoundException(PEDIDO_NOT_FOUND_MESSAGE + pedidoId));
    }
    
    /**
     * Verifica se um pedido existe
     */
    @Transactional(readOnly = true)
    public boolean pedidoExiste(UUID pedidoId) {
        return pedidoRepository.exists(pedidoId);
    }
    
    /**
     * Obtém a versão atual de um pedido
     */
    @Transactional(readOnly = true)
    public Long obterVersaoAtual(UUID pedidoId) {
        return pedidoRepository.getCurrentVersion(pedidoId);
    }
    
    // Commands
    public record CriarPedidoCommand(
            String numeroPedido,
            UUID clienteId,
            String clienteNome,
            String clienteEmail,
            List<ItemPedido> itens,
            EnderecoEntrega enderecoEntrega
    ) {}
    
    public record AtualizarPedidoCommand(
            UUID pedidoId,
            List<ItemPedido> itens,
            EnderecoEntrega enderecoEntrega,
            String observacoes
    ) {}
    
    public record AtualizarStatusCommand(
            UUID pedidoId,
            StatusPedido novoStatus
    ) {}

    public record CancelarPedidoCommand(
            UUID pedidoId,
            String motivo
    ) {}
    
    // Exceptions
    public static class PedidoNotFoundException extends RuntimeException {
        public PedidoNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
