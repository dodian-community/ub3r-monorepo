# Metrics, Telemetry & Real-Time Dashboard

Dodian Ub3r includes built-in real-time operational monitoring, Prometheus exporter endpoints, and an interactive HTML5 visual dashboard powered by the Ktor Web API service (port `8081`).

---

## Service Endpoints

When the server starts up with `web_enabled = true` in `Settings.toml`, the following operational monitoring endpoints are available:

| Endpoint | Content Type | Purpose |
| :--- | :--- | :--- |
| **`http://localhost:8081/prometheus`** | `text/plain` | Standard Prometheus text-format metric exporter for Prometheus server scraping. |
| **`http://localhost:8081/metrics`** | `text/plain` | Alias for `/prometheus`. |
| **`http://localhost:8081/dashboard`** | `text/html` | Real-time interactive browser dashboard with live visual charts (tick timings, active players, memory usage, packet throughput). |
| **`http://localhost:8081/metrics/dashboard`** | `text/html` | Alias for `/dashboard`. |

---

## Configuration (`Settings.toml`)

Enable or toggle metrics components inside `game-server/Settings.toml`:

```toml
[network]
web_enabled = true
web_port = 8081

[metrics]
enabled = true
prometheus_endpoint = true
dashboard_enabled = true
```

---

## Authentication & API Access Token (`WEB_API_OPS_TOKEN`)

Operational endpoints and administrative Web API routes require token authentication when accessed externally.

### 1. Setting Your Security Token
Set `WEB_API_OPS_TOKEN` in your `game-server/.env` file:
```env
WEB_API_OPS_TOKEN=your_secure_secret_token_here
```

### 2. Passing the Token in Requests
Include the token in your HTTP requests using the standard `Authorization` header:

```bash
# Curl Example for Prometheus Scrape
curl -H "Authorization: Bearer your_secure_secret_token_here" http://localhost:8081/prometheus

# Curl Example for Operations Endpoints
curl -H "Authorization: your_secure_secret_token_here" http://localhost:8081/ready
```

---

## Prometheus Integration (`prometheus.yml`)

Add Dodian Ub3r to your Prometheus scraper configuration:

```yaml
scrape_configs:
  - job_name: 'dodian_ub3r'
    scrape_interval: 5s
    bearer_token: 'your_secure_secret_token_here'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/prometheus'
```
