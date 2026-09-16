#!/usr/bin/env bash
#
# Lance l'app en local contre le Postgres de docker-compose.yml.
#
# Pourquoi ce script : .env pointe sur Neon, dont le port 5432 est filtré sur
# certains réseaux, et application.properties charge .env automatiquement.
# Les variables exportées ici sont prioritaires sur .env (les variables
# d'environnement gagnent sur les fichiers de configuration en Spring Boot).
#
# Usage : ./run-local.sh            (port 8080)
#         SERVER_PORT=8081 ./run-local.sh
#
set -euo pipefail
cd "$(dirname "$0")"

docker compose up -d postgres
echo "Attente de Postgres..."
for _ in $(seq 1 30); do
  if docker compose exec -T postgres pg_isready -U postgres -d rattrapage >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

# 8080 est souvent déjà pris (autre stack Docker) : SERVER_PORT=8081 ./run-local.sh
export SERVER_PORT="${SERVER_PORT:-8080}"

export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/rattrapage"
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres

# Bucket et bus factices : le contexte démarre, Flyway migre la base locale,
# mais POST /submissions échouera sur l'upload S3 (aucun bucket local).
# Le chemin complet (upload + vignette + email) est couvert par SubmissionIT,
# qui mocke BucketComponent / EventProducer / Mailer.
export AWS_S3_BUCKET=local-bucket
export AWS_EVENTBRIDGE_BUS=local-bus

./gradlew bootRun
