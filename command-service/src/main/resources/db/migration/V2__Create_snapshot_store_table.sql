-- Tabela para armazenar snapshots dos agregados
CREATE TABLE snapshot_store (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_data JSONB NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Garantir apenas um snapshot por agregado
    CONSTRAINT uk_snapshot_store_aggregate_id UNIQUE (aggregate_id)
);

-- Índices para otimizar consultas
CREATE INDEX idx_snapshot_store_aggregate_type ON snapshot_store (aggregate_type);
CREATE INDEX idx_snapshot_store_version ON snapshot_store (version);
CREATE INDEX idx_snapshot_store_created_at ON snapshot_store (created_at);

-- Comentários para documentação
COMMENT ON TABLE snapshot_store IS 'Tabela para armazenar snapshots dos agregados para reconstrução rápida';
COMMENT ON COLUMN snapshot_store.aggregate_id IS 'Identificador único do agregado';
COMMENT ON COLUMN snapshot_store.aggregate_type IS 'Tipo do agregado';
COMMENT ON COLUMN snapshot_store.aggregate_data IS 'Estado completo do agregado em formato JSON';
COMMENT ON COLUMN snapshot_store.version IS 'Versão do agregado no momento do snapshot';

