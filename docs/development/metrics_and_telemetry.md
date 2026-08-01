# Metrics, Telemetry & Real-Time Dashboard

Dodian Ub3r includes built-in real-time operational monitoring, Prometheus exporter endpoints, and an interactive HTML5 visual dashboard powered by the Ktor Web API service (port `8081`).

---

## Service Endpoints

When the server starts up with `web_enabled = true` in `Settings.toml`, the following operational monitoring endpoints are available directly without requiring authentication tokens:

| Endpoint | Content Type | Purpose |
| :--- | :--- | :--- |
| **`http://localhost:8081/prometheus`** | `text/plain` | Standard Prometheus text-format metric exporter for Prometheus server scraping. |
| **`http://localhost:8081/metrics`** | `text/plain` | Alias for `/prometheus`. |
| **`http://localhost:8081/dashboard`** | `text/html` | Real-time interactive browser dashboard with live visual charts (tick timings, active players, memory usage, packet throughput). |
| **`http://localhost:8081/metrics/dashboard`** | `text/html` | Alias for `/dashboard`. |
| **`http://localhost:8081/api/hiscores?player=<name>`** | `text/plain` / `application/json` | Public player hiscores lookup endpoint compatible with RuneLite. |

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

## Direct Access Examples

Endpoints can be queried directly via standard HTTP tools or browser:

```bash
# Prometheus Scrape
curl http://localhost:8081/prometheus

# Player Hiscores Lookup
curl "http://localhost:8081/api/hiscores?player=Admin"
```

---

## Prometheus Integration (`prometheus.yml`)

Add Dodian Ub3r to your Prometheus scraper configuration:

```yaml
scrape_configs:
  - job_name: 'dodian_ub3r'
    scrape_interval: 5s
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/prometheus'
```
