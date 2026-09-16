# poja-starter-template

API de soumission de fichiers avec vignette 256x256 (`doc/api.yml`).

## Développement local

```bash
./run-local.sh                     # Postgres Docker + app sur http://localhost:8080
SERVER_PORT=8081 ./run-local.sh    # si 8080 est déjà pris par un autre stack Docker
```

Le script démarre le Postgres de `docker-compose.yml` puis exporte les variables de
connexion locales : elles sont prioritaires sur `.env`, qui pointe sur Neon (dont le
port 5432 est filtré sur certains réseaux). Flyway crée le schéma au démarrage.

- `GET /submissions` fonctionne en local, `POST /submissions` insère la ligne mais
  échoue sur l'upload S3 : il n'y a pas de bucket en local.
- Le chemin complet (upload + vignette + email) est couvert par `SubmissionIT`, qui
  mocke `BucketComponent`, `EventProducer` et `Mailer`.
- Tests : `./gradlew test` (Testcontainers démarre son propre Postgres).

## Configuration

`application.properties` ne contient que des placeholders `${ENV_VAR}` et charge `.env`
en local (`spring.config.import`, fichier gitignoré). En Lambda, Poja injecte les
variables d'environnement, qui ont la priorité ; aucun secret n'est versionné.
