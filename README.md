# Pizza Orders — demo Ktor (CRUD + RBAC + persistenza + observability)

Progetto didattico per imparare Ktor: una CRUD API per gestire un menu di pizze e i relativi
ordini, con autenticazione JWT, autorizzazione basata sui ruoli (RBAC), persistenza su Postgres,
cache/rate-limit/blacklist su Redis, e observability con Prometheus + Grafana.

## Architettura

Struttura a layer (`config` → `plugins` → `repository` → `service` → `controller`), come
consigliato dalla documentazione ufficiale di Ktor:

```
src/main/kotlin/com/example/com/
├── Application.kt          # bootstrap: ordine di inizializzazione di tutti i pezzi
├── config/                 # connessioni a Postgres (Exposed/R2DBC) e Redis (Lettuce)
├── plugins/                 # JWT, RBAC, DI (Koin), metriche, error handling
├── domain/                  # tabelle Exposed + modelli di dominio (Role, User, Pizza, Order)
├── dto/
│   ├── request/              # cosa può mandare il client (inbound)
│   └── response/              # cosa risponde il server (outbound), con mapper toDomain()/toResponse()
├── repository/               # query Exposed DSL, isolano Postgres dal resto
├── service/                  # logica di business + cache/blacklist/rate-limit su Redis
└── controller/               # routing HTTP
```

**Ruoli**: `ADMIN` gestisce il menu (crea/modifica/elimina pizze) e vede tutti gli ordini;
`USER` consulta il menu e vede/gestisce solo i propri ordini.

**Redis** è usato per tre cose diverse:
1. Cache-aside sul menu pizze (`GET /pizzas`, TTL 60s, invalidata alla scrittura).
2. Blacklist dei JWT: `POST /auth/logout` rende il token inutilizzabile subito, anche se un JWT
   di per sé è stateless.
3. Rate limiting su `POST /auth/login` (max 5 tentativi/60s per IP).

## Come avviarlo

```bash
docker compose up -d      # Postgres, Redis, Prometheus, Grafana
./gradlew run              # avvia l'app su http://localhost:8080
```

Al primo avvio vengono creati due utenti demo (bcrypt-hashed in Postgres):

| username | password  | ruolo |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| alice    | alice123  | USER  |

Per lanciare i test (richiedono Postgres/Redis attivi):

```bash
./gradlew test
```

## Esempio d'uso (curl)

```bash
# Login come alice (USER)
ALICE_TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"alice123"}' | jq -r .token)

# Alice può consultare il menu...
curl -s localhost:8080/pizzas -H "Authorization: Bearer $ALICE_TOKEN"

# ...ma non può crearne una nuova (403 Forbidden)
curl -s -X POST localhost:8080/pizzas -H "Authorization: Bearer $ALICE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Margherita","ingredients":"pomodoro, mozzarella","priceCents":900}'

# Login come admin e creazione pizza (201 Created)
ADMIN_TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

curl -s -X POST localhost:8080/pizzas -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Margherita","ingredients":"pomodoro, mozzarella","priceCents":900}'

# Alice ordina una pizza
curl -s -X POST localhost:8080/orders -H "Authorization: Bearer $ALICE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"pizzaId":1,"quantity":2}'

# Alice vede solo i propri ordini, admin li vede tutti
curl -s localhost:8080/orders -H "Authorization: Bearer $ALICE_TOKEN"
curl -s localhost:8080/orders -H "Authorization: Bearer $ADMIN_TOKEN"

# Logout: il token viene messo in blacklist su Redis
curl -s -X POST localhost:8080/auth/logout -H "Authorization: Bearer $ALICE_TOKEN"
curl -s localhost:8080/pizzas -H "Authorization: Bearer $ALICE_TOKEN"   # ora 401

# Rate limiting: 6+ login falliti di fila da bloccati con 429
for i in $(seq 1 6); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8080/auth/login \
    -H 'Content-Type: application/json' -d '{"username":"alice","password":"wrong"}'
done
```

## Observability

- `GET /metrics-micrometer` — metriche in formato Prometheus (richieste HTTP, JVM, contatori
  custom: `pizza_orders_created_total`, `pizza_cache_requests_total`, `auth_login_rate_limited_total`).
- Prometheus: [http://localhost:9090](http://localhost:9090) — scrape automatico dell'app.
- Grafana: [http://localhost:3000](http://localhost:3000) (login anonimo abilitato come Admin) —
  dashboard "Pizza Orders — Overview" già pronta (request rate, latenza p95, memoria JVM, hit
  ratio della cache, ordini creati, login bloccati dal rate limiter).

## Verificare la cache Redis manualmente

```bash
docker exec -it $(docker compose ps -q redis) redis-cli KEYS 'pizza:*'
```

## Stack tecnico

Ktor 3.5 · Exposed (DSL, R2DBC) · Postgres · Redis (Lettuce, coroutine API) · Koin (DI) ·
JWT (auth0) · Micrometer + Prometheus · Grafana · bcrypt.
