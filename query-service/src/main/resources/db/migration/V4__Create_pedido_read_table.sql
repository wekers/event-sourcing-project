-- Tabela para o Read Model de Pedidos (CQRS)
CREATE TABLE pedido_read (
    id UUID PRIMARY KEY,
    numero_pedido VARCHAR(50) NOT NULL UNIQUE,
    cliente_id UUID NOT NULL,
    cliente_nome VARCHAR(255) NOT NULL,
    cliente_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    valor_total DECIMAL(10,2) NOT NULL,
    data_criacao TIMESTAMP WITH TIME ZONE NOT NULL,
    data_atualizacao TIMESTAMP WITH TIME ZONE NOT NULL,
    data_cancelamento TIMESTAMP WITH TIME ZONE,
    observacoes TEXT,
    
    -- Dados dos itens do pedido (desnormalizado para consultas rápidas)
    itens JSONB NOT NULL,
    
    -- Endereço de entrega (desnormalizado)
    endereco_entrega JSONB,
    
    -- Versão para controle de concorrência
    version BIGINT NOT NULL DEFAULT 0
);

-- Índices para otimizar consultas do read model
CREATE INDEX idx_pedido_read_numero_pedido ON pedido_read (numero_pedido);
CREATE INDEX idx_pedido_read_cliente_id ON pedido_read (cliente_id);
CREATE INDEX idx_pedido_read_cliente_email ON pedido_read (cliente_email);
CREATE INDEX idx_pedido_read_status ON pedido_read (status);
CREATE INDEX idx_pedido_read_data_criacao ON pedido_read (data_criacao);
CREATE INDEX idx_pedido_read_data_atualizacao ON pedido_read (data_atualizacao);
CREATE INDEX idx_pedido_read_valor_total ON pedido_read (valor_total);

-- Índices compostos para consultas comuns
CREATE INDEX idx_pedido_read_cliente_status ON pedido_read (cliente_id, status);
CREATE INDEX idx_pedido_read_status_data ON pedido_read (status, data_criacao);

-- Índice GIN para consultas nos dados JSON
CREATE INDEX idx_pedido_read_itens_gin ON pedido_read USING GIN (itens);
CREATE INDEX idx_pedido_read_endereco_gin ON pedido_read USING GIN (endereco_entrega);

-- Comentários para documentação
COMMENT ON TABLE pedido_read IS 'Read Model otimizado para consultas de pedidos (CQRS)';
COMMENT ON COLUMN pedido_read.id IS 'Identificador único do pedido (mesmo do agregado)';
COMMENT ON COLUMN pedido_read.numero_pedido IS 'Número sequencial do pedido para exibição';
COMMENT ON COLUMN pedido_read.itens IS 'Lista de itens do pedido em formato JSON';
COMMENT ON COLUMN pedido_read.endereco_entrega IS 'Endereço de entrega em formato JSON';
COMMENT ON COLUMN pedido_read.version IS 'Versão para controle de concorrência otimista';

