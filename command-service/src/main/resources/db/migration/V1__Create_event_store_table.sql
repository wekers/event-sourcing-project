-- Tabela para armazenar eventos (Event Store)
CREATE TABLE event_store (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    event_metadata JSONB,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Índices para performance
    CONSTRAINT uk_event_store_aggregate_version UNIQUE (aggregate_id, version)
);

-- Índices para otimizar consultas
CREATE INDEX idx_event_store_aggregate_id ON event_store (aggregate_id);
CREATE INDEX idx_event_store_aggregate_type ON event_store (aggregate_type);
CREATE INDEX idx_event_store_event_type ON event_store (event_type);
CREATE INDEX idx_event_store_created_at ON event_store (created_at);
CREATE INDEX idx_event_store_aggregate_id_version ON event_store (aggregate_id, version);

-- Comentários para documentação
COMMENT ON TABLE event_store IS 'Tabela append-only para armazenar todos os eventos do sistema';
COMMENT ON COLUMN event_store.aggregate_id IS 'Identificador único do agregado';
COMMENT ON COLUMN event_store.aggregate_type IS 'Tipo do agregado (ex: Pedido, Cliente)';
COMMENT ON COLUMN event_store.event_type IS 'Tipo do evento (ex: PedidoCriado, PedidoAtualizado)';
COMMENT ON COLUMN event_store.event_data IS 'Dados do evento em formato JSON';
COMMENT ON COLUMN event_store.event_metadata IS 'Metadados do evento (usuário, correlação, etc.)';
COMMENT ON COLUMN event_store.version IS 'Versão do agregado para controle de concorrência otimista';

