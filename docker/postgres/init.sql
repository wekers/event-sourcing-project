-- Configurações para Debezium
ALTER SYSTEM SET wal_level = logical;
ALTER SYSTEM SET max_replication_slots = 4;
ALTER SYSTEM SET max_wal_senders = 4;

-- Criar usuário para replicação (se necessário)
-- CREATE USER debezium WITH REPLICATION PASSWORD 'dbz';

-- Garantir que o banco de dados existe
SELECT 'CREATE DATABASE eventstore' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'eventstore');



-- Criar slot de replicação para Debezium (se não existir)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'debezium_slot') THEN
        PERFORM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');
    END IF;
END;
$$;

