-- Tabela para implementar o Outbox Pattern
CREATE TABLE event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    event_metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    
    -- Status para controle de processamento
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

-- Índices para otimizar consultas
CREATE INDEX idx_event_outbox_status ON event_outbox (status);
CREATE INDEX idx_event_outbox_created_at ON event_outbox (created_at);
CREATE INDEX idx_event_outbox_aggregate_id ON event_outbox (aggregate_id);
CREATE INDEX idx_event_outbox_aggregate_type ON event_outbox (aggregate_type);
CREATE INDEX idx_event_outbox_event_type ON event_outbox (event_type);

-- Índice para otimizar consultas de eventos não processados
CREATE INDEX idx_event_outbox_pending ON event_outbox (created_at) WHERE status = 'PENDING';

-- Comentários para documentação
COMMENT ON TABLE event_outbox IS 'Tabela para implementar o Outbox Pattern - eventos a serem publicados';
COMMENT ON COLUMN event_outbox.aggregate_id IS 'Identificador único do agregado';
COMMENT ON COLUMN event_outbox.aggregate_type IS 'Tipo do agregado para roteamento';
COMMENT ON COLUMN event_outbox.event_type IS 'Tipo do evento para roteamento';
COMMENT ON COLUMN event_outbox.event_data IS 'Dados do evento em formato JSON';
COMMENT ON COLUMN event_outbox.event_metadata IS 'Metadados do evento';
COMMENT ON COLUMN event_outbox.status IS 'Status do processamento do evento';
COMMENT ON COLUMN event_outbox.processed_at IS 'Timestamp de quando o evento foi processado';

