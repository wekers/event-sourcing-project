// docker/mongo-init/pedido_read_indexes.js

print("📌 Criando índices para a coleção pedido_read...");

db = db.getSiblingDB("querydb");

// índice único no numeroPedido
db.pedido_read.createIndex(
    { "numeroPedido": 1 },
    { unique: true, name: "uk_numeroPedido" }
);

// índice para consultas por cliente ordenadas por data
db.pedido_read.createIndex(
    { "clienteId": 1, "dataCriacao": -1 },
    { name: "idx_cliente_data" }
);

// índice para consultas por status ordenadas por data
db.pedido_read.createIndex(
    { "status": 1, "dataCriacao": -1 },
    { name: "idx_status_data" }
);

// índice combinado (cliente + status + data)
db.pedido_read.createIndex(
    { "clienteId": 1, "status": 1, "dataCriacao": -1 },
    { name: "idx_cliente_status_data" }
);

// índice para buscas por email
db.pedido_read.createIndex(
    { "clienteEmail": 1, "dataCriacao": -1 },
    { name: "idx_cliente_email" }
);

// índice para range queries de valor total
db.pedido_read.createIndex(
    { "valorTotal": 1 },
    { name: "idx_valorTotal" }
);

print("✅ Índices criados com sucesso para pedido_read!");
