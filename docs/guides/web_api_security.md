# Web API security

The public game API exposes server status, lightweight health, and compatible
highscore routes. Readiness, content manifests, Prometheus output, metric
history, and dashboards require `WEB_API_OPS_TOKEN`; they remain unavailable
when that variable is blank.

Configure exact browser origins and trusted reverse proxies in
`game-server/Settings.toml`:

```toml
[web_security]
allowed_origins = ["https://example.com"]
trusted_proxy_cidrs = ["127.0.0.1/32", "10.0.0.0/8"]
```

Forwarded client-address headers are ignored unless the direct peer is in the
trusted-proxy list. API and login throttles are bounded and in memory, so their
state clears whenever the game server restarts.

These controls protect application resources from abusive clients. They cannot
absorb a volumetric DDoS attack; production deployments must also enforce
connection and traffic limits at a firewall, hosting provider, or reverse
proxy.
