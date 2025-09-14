#!/bin/sh
echo "⏳ Waiting for Kafka Connect..."
sleep 5
curl -X POST http://debezium-connect:8083/connectors \
  -H "Content-Type: application/json" \
  -d @/kafka/config/register-postgres.json \
|| echo "⚠️ Conector já existe ou falhou ao registrar"
