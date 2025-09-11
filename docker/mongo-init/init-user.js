db = db.getSiblingDB('pedido_read_db');

db.createUser({
    user: "user",
    pwd: "pass",
    roles: [
        { role: "readWrite", db: "pedido_read_db" }
    ]
});
