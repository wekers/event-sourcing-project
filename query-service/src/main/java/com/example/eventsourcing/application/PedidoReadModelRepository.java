package com.example.eventsourcing.application;

import com.example.eventsourcing.application.readmodel.PedidoReadModel;
import com.example.eventsourcing.domain.pedido.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PedidoReadModelRepository extends JpaRepository<PedidoReadModel, UUID> {

    Optional<PedidoReadModel> findByNumeroPedido(String numeroPedido);

    // Consultas por cliente
    List<PedidoReadModel> findByClienteIdOrderByDataCriacaoDesc(UUID clienteId);
    Page<PedidoReadModel> findByClienteIdOrderByDataCriacaoDesc(UUID clienteId, Pageable pageable);
    Page<PedidoReadModel> findByClienteId(UUID clienteId, Pageable pageable);

    // Consultas por status
    List<PedidoReadModel> findByStatusOrderByDataCriacaoDesc(StatusPedido status);
    Page<PedidoReadModel> findByStatusOrderByDataCriacaoDesc(StatusPedido status, Pageable pageable);

    // Consultas combinadas
    List<PedidoReadModel> findByClienteIdAndStatusOrderByDataCriacaoDesc(UUID clienteId, StatusPedido status);
    Page<PedidoReadModel> findByClienteIdAndStatus(UUID clienteId, StatusPedido status, Pageable pageable);

    // Consultas por email
    List<PedidoReadModel> findByClienteEmailOrderByDataCriacaoDesc(String clienteEmail);

    // Consultas temporais
    @Query("SELECT p FROM PedidoReadModel p WHERE p.dataCriacao BETWEEN :inicio AND :fim ORDER BY p.dataCriacao DESC")
    List<PedidoReadModel> findByPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    Page<PedidoReadModel> findByDataCriacaoBetween(Instant inicio, Instant fim, Pageable pageable);

    // Consultas por valor
    List<PedidoReadModel> findByValorTotalGreaterThanEqualOrderByDataCriacaoDesc(BigDecimal valorMinimo);

    @Query("SELECT p FROM PedidoReadModel p WHERE p.valorTotal BETWEEN :valorMin AND :valorMax ORDER BY p.dataCriacao DESC")
    List<PedidoReadModel> findByFaixaValor(@Param("valorMin") BigDecimal valorMin, @Param("valorMax") BigDecimal valorMax);

    // Estatísticas
    long countByStatus(StatusPedido status);
    long countByClienteId(UUID clienteId);

    @Query("SELECT COALESCE(SUM(p.valorTotal), 0) FROM PedidoReadModel p WHERE p.clienteId = :clienteId")
    BigDecimal sumValorTotalByClienteId(@Param("clienteId") UUID clienteId);

    // Consulta para busca flexível
    @Query("SELECT p FROM PedidoReadModel p WHERE " +
            "(:clienteId IS NULL OR p.clienteId = :clienteId) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:inicio IS NULL OR p.dataCriacao >= :inicio) AND " +
            "(:fim IS NULL OR p.dataCriacao <= :fim)")
    Page<PedidoReadModel> search(
            @Param("clienteId") UUID clienteId,
            @Param("status") StatusPedido status,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            Pageable pageable);
}