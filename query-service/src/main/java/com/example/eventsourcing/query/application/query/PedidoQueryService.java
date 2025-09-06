package com.example.eventsourcing.query.application.query;

import com.example.eventsourcing.command.domain.pedido.StatusPedido;
import com.example.eventsourcing.query.application.PedidoReadModelRepository;
import com.example.eventsourcing.query.application.readmodel.PedidoCompletoDTO;
import com.example.eventsourcing.query.application.readmodel.PedidoDTO;
import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PedidoQueryService {

    private final PedidoReadModelRepository readModelRepository;

    public Optional<PedidoDTO> findById(UUID pedidoId) {
        log.debug("Buscando pedido por ID: {}", pedidoId);
        return readModelRepository.findById(pedidoId)
                .map(PedidoDTO::from);
    }

    public Optional<PedidoDTO> findByNumeroPedido(String numeroPedido) {
        log.debug("Buscando pedido por número: {}", numeroPedido);
        return readModelRepository.findByNumeroPedido(numeroPedido)
                .map(PedidoDTO::from);
    }

    public Page<PedidoDTO> findPedidosPorCliente(UUID clienteId, Pageable pageable) {
        log.debug("Buscando pedidos do cliente: {}", clienteId);
        return readModelRepository.findByClienteIdOrderByDataCriacaoDesc(clienteId, pageable)
                .map(PedidoDTO::from);
    }

    public Page<PedidoDTO> findByStatus(StatusPedido status, Pageable pageable) {
        log.debug("Buscando pedidos por status: {}", status);
        return readModelRepository.findByStatusOrderByDataCriacaoDesc(status, pageable)
                .map(PedidoDTO::from);
    }

    public Page<PedidoDTO> findByPeriodo(Instant inicio, Instant fim, Pageable pageable) {
        log.debug("Buscando pedidos por período: {} até {}", inicio, fim);
        return readModelRepository.findByDataCriacaoBetween(inicio, fim, pageable)
                .map(PedidoDTO::from);
    }

    public Page<PedidoDTO> search(UUID clienteId, StatusPedido status, Instant inicio, Instant fim, Pageable pageable) {
        log.debug("Buscando pedidos com filtros - cliente: {}, status: {}, inicio: {}, fim: {}",
                clienteId, status, inicio, fim);

        return readModelRepository.search(clienteId, status, inicio, fim, pageable)
                .map(PedidoDTO::from);
    }

    public Page<PedidoDTO> findAll(Pageable pageable) {
        log.debug("Buscando todos os pedidos com paginação");
        return readModelRepository.findAll(pageable)
                .map(PedidoDTO::from);
    }

    public Optional<PedidoCompletoDTO> findPedidoCompletoById(UUID pedidoId) {
        log.debug("Buscando pedido completo por ID: {}", pedidoId);
        return readModelRepository.findById(pedidoId)
                .map(this::toPedidoCompletoDTO);
    }

    public Optional<PedidoCompletoDTO> findPedidoCompletoByNumero(String numeroPedido) {
        log.debug("Buscando pedido completo por número: {}", numeroPedido);
        return readModelRepository.findByNumeroPedido(numeroPedido)
                .map(this::toPedidoCompletoDTO);
    }

    private PedidoCompletoDTO toPedidoCompletoDTO(PedidoReadModel readModel) {
        return PedidoCompletoDTO.builder()
                .id(readModel.getId())
                .numeroPedido(readModel.getNumeroPedido())
                .clienteId(readModel.getClienteId())
                .clienteNome(readModel.getClienteNome())
                .clienteEmail(readModel.getClienteEmail())
                .status(readModel.getStatus())
                .valorTotal(readModel.getValorTotal())
                .dataCriacao(readModel.getDataCriacao())
                .dataAtualizacao(readModel.getDataAtualizacao())
                .dataCancelamento(readModel.getDataCancelamento())
                .observacoes(readModel.getObservacoes())
                .itens(readModel.getItens().stream()
                        .map(item -> PedidoCompletoDTO.ItemPedidoCompletoDTO.builder()
                                .produtoId(item.produtoId())
                                .produtoNome(item.produtoNome())
                                .produtoDescricao(item.produtoDescricao())
                                .quantidade(item.quantidade())
                                .precoUnitario(item.precoUnitario())
                                .valorTotal(item.valorTotal())
                                .build())
                        .collect(Collectors.toList()))
                .enderecoEntrega(readModel.getEnderecoEntrega() != null ?
                        PedidoCompletoDTO.EnderecoEntregaDTO.builder()
                                .logradouro(readModel.getEnderecoEntrega().getLogradouro())
                                .numero(readModel.getEnderecoEntrega().getNumero())
                                .complemento(readModel.getEnderecoEntrega().getComplemento())
                                .bairro(readModel.getEnderecoEntrega().getBairro())
                                .cidade(readModel.getEnderecoEntrega().getCidade())
                                .estado(readModel.getEnderecoEntrega().getEstado())
                                .cep(readModel.getEnderecoEntrega().getCep())
                                .pontoReferencia(readModel.getEnderecoEntrega().getPontoReferencia())
                                .build() : null)
                .version(readModel.getVersion())
                .build();
    }


    // Métodos adicionais para estatísticas
    public long countByStatus(StatusPedido status) {
        return readModelRepository.countByStatus(status);
    }

    public long countByClienteId(UUID clienteId) {
        return readModelRepository.countByClienteId(clienteId);
    }

    public BigDecimal getTotalGastoPorCliente(UUID clienteId) {
        return readModelRepository.sumValorTotalByClienteId(clienteId);
    }
}