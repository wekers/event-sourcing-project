package com.example.eventsourcing.query.application;

import com.example.eventsourcing.query.application.readmodel.PedidoReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PedidoReadModelRepository extends MongoRepository<PedidoReadModel, UUID> {

    Optional<PedidoReadModel> findByNumeroPedido(String numeroPedido);

    // Consultas por cliente
    List<PedidoReadModel> findByClienteIdOrderByDataCriacaoDesc(UUID clienteId);
    Page<PedidoReadModel> findByClienteIdOrderByDataCriacaoDesc(UUID clienteId, Pageable pageable);
    Page<PedidoReadModel> findByClienteId(UUID clienteId, Pageable pageable);

    // Consultas por status
    List<PedidoReadModel> findByStatusOrderByDataCriacaoDesc(String status);
    Page<PedidoReadModel> findByStatusOrderByDataCriacaoDesc(String status, Pageable pageable);

    // Consultas combinadas
    List<PedidoReadModel> findByClienteIdAndStatusOrderByDataCriacaoDesc(UUID clienteId, String status);
    Page<PedidoReadModel> findByClienteIdAndStatus(UUID clienteId, String status, Pageable pageable);

    // Consultas por email
    List<PedidoReadModel> findByClienteEmailOrderByDataCriacaoDesc(String clienteEmail);

    // Consultas temporais
    List<PedidoReadModel> findByDataCriacaoBetweenOrderByDataCriacaoDesc(Instant inicio, Instant fim);
    Page<PedidoReadModel> findByDataCriacaoBetween(Instant inicio, Instant fim, Pageable pageable);

    // Consultas por valor
    List<PedidoReadModel> findByValorTotalGreaterThanEqualOrderByDataCriacaoDesc(BigDecimal valorMinimo);

    // Estatísticas
    long countByStatus(String status);
    long countByClienteId(UUID clienteId);

    // Soma total por cliente (Mongo Aggregation)
    @Aggregation(pipeline = {
            "{ '$match': { 'clienteId': ?0 } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$valorTotal' } } }"
    })
    Double sumValorTotalByClienteId(UUID clienteId);

    // Busca flexível → só com query dinâmica (precisa de implementação custom via MongoTemplate)
}
