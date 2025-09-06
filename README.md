# Gateway RPC Proxy

A fast Vert.x JSON‑RPC 2.0 proxy for Ethereum with access logging, Prometheus metrics, and optional TLS (HTTPS).  
It forwards single **and** batch JSON‑RPC requests to a configured upstream and returns responses **as‑is**.

---

## Table of contents
- [Requirements](#requirements)
- [Quick start (Docker)](#quick-start-docker)
- [Build from source](#build-from-source)
- [Configuration](#configuration)
- [Endpoints](#endpoints)
- [TLS (HTTPS)](#tls-https)
- [Verification (curl)](#verification-curl)
- [Behavior & limits](#behavior--limits)
- [Design notes](#design-notes)
- [Future ideas](#future-ideas)
- [License](#license)

---

## Requirements
- Java 21+
- Maven 3.8+
- Docker (optional, for containerized run)

---

## Quick start (Docker)

> Defaults enable **TLS on port 8443**. For plain HTTP, set `tls.enabled=false` and change `http.port` if needed, then rebuild.

Build:
```bash
docker build --no-cache -t gateway-rpc-proxy:dev .
```

Run (HTTPS, with PKCS#12 mounted):
```bash
docker run --rm -p 8443:8443 -v "$PWD/server.p12:/server.p12" gateway-rpc-proxy:dev
```
Expected:
```
... HTTP server started on https://localhost:8443
```

Run (HTTP only):
```bash
# set tls.enabled=false in application.properties and rebuild first
docker run --rm -p 8080:8080 gateway-rpc-proxy:dev
```

---

## Build from source
```bash
mvn clean package -DskipTests
java -jar target/app.jar
```

---

## Configuration

All values live in `src/main/resources/application.properties`.

| Property                               | Default                         | Description |
|----------------------------------------|---------------------------------|-------------|
| `http.port`                            | `8443`                          | HTTP/HTTPS port |
| `tls.enabled`                          | `true`                          | Enable HTTPS |
| `tls.pkcs12.path`                      | `/server.p12`                   | Path to PKCS#12 inside the container |
| `tls.pkcs12.password`                  | `changeit`                      | PKCS#12 password (demo only) |
| `target.rpc.url`                       | `https://cloudflare-eth.com`    | Upstream Ethereum JSON‑RPC endpoint |
| `request.timeout.ms`                   | `10000`                         | Upstream request timeout |
| `max.body.bytes`                       | `10485760` (10 MiB)             | Max incoming body size |
| `circuit.breaker.enabled`              | `true`                          | Enable Vert.x circuit breaker |
| `circuit.breaker.failures.threshold`   | `5`                             | Failures before OPEN |
| `circuit.breaker.reset.timeout.ms`     | `30000`                         | Time before HALF_OPEN probe |

> Any public endpoint will work (e.g. `https://eth.llamarpc.com`) as well as providers requiring API keys.

---

## Endpoints

- `POST /rpc` — JSON‑RPC 2.0 proxy (single or batch).
- `GET  /metrics` — **compact JSON** with per‑method call counters (for billing). Example:
  ```json
  {"calls":{"eth_chainId":3,"eth_blockNumber":5}}
  ```
- `GET  /prometheus` — full Prometheus metrics (Vert.x + JVM + custom counters).
- `GET  /health` — liveness probe (`OK`).

Observability is powered by **vertx‑micrometer‑metrics** with the Prometheus backend.

---

## TLS (HTTPS)

Generate PKCS#12 (self‑signed) and mount it into the container.

**Windows (PowerShell):**
```powershell
keytool -genkeypair -alias gateway -storetype PKCS12 -keyalg RSA `
  -keystore server.p12 -storepass changeit -validity 3650 `
  -dname "CN=localhost, OU=Dev, O=Gateway, L=City, S=State, C=US"
```

**macOS/Linux (bash):**
```bash
keytool -genkeypair -alias gateway -storetype PKCS12 -keyalg RSA \
  -keystore server.p12 -storepass changeit -validity 3650 \
  -dname "CN=localhost, OU=Dev, O=Gateway, L=City, S=State, C=US"
```

`application.properties`:
```properties
tls.enabled=true
http.port=8443
tls.pkcs12.path=/server.p12
tls.pkcs12.password=changeit
```

---

## Verification (curl)

Health:
```bash
curl -k https://localhost:8443/health
```

Metrics (compact JSON):
```bash
curl -ks https://localhost:8443/metrics | jq .
```

Prometheus:
```bash
curl -ks https://localhost:8443/prometheus | head
```

JSON‑RPC (single):
```bash
curl -ks -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
  https://localhost:8443/rpc | jq .
```

JSON‑RPC (batch):
```bash
curl -ks -H "Content-Type: application/json" \
  -d '[{"jsonrpc":"2.0","method":"eth_chainId","params":[],"id":1}, {"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":2}]' \
  https://localhost:8443/rpc | jq .
```

---

## Behavior & limits
- Upstream responses are **not transformed**; the proxy returns the upstream payload (including JSON‑RPC errors with HTTP 200).
- Empty body → HTTP `400` with JSON‑RPC code `-32600`.
- Body larger than `max.body.bytes` → HTTP `413` (handled by Vert.x `BodyHandler`).
- Access logs include method, path, status, duration, bytes, and client IP (prefers `X-Forwarded-For`).
- Circuit breaker: Vert.x `vertx-circuit-breaker` (OPEN on consecutive failures, HALF_OPEN after reset timeout).
- Per‑method counters are exported via Micrometer and exposed at `/metrics` (JSON) and `/prometheus` (Prometheus).

---

## Design notes
- **Stack:** Vert.x Web + WebClient (async, non‑blocking), Java 21.
- **Observability:** Vert.x Micrometer + Prometheus; custom JSON endpoint for billing.
- **Packages:** `http` (server/routing/logs), `rpc` (forwarding & validation), `metrics` (names/handlers), `config` (properties loader).
- **Docker:** multi‑stage build (Maven → JRE), runs the fat‑jar.

---

## Future ideas
- Rate limiting (per‑IP / per‑method).
- Retry policy with backoff/jitter.
- Externalized configuration (mount overrides).
- Structured logs (JSON) with MDC correlation id.
- Integration tests with Testcontainers.

---

## License
MIT (replace if needed).

### .gitignore
```
target/
*.p12
*.jks
```
