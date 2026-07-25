package net.dodian.uber.game.engine.metrics

import net.dodian.uber.game.engine.config.serverEnv

object MetricsDashboardHtml {
    fun renderHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dodian Server (${serverEnv}) - Metrics Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        :root {
            --bg-primary: #0a0c10;
            --bg-card: rgba(22, 27, 34, 0.75);
            --border-card: rgba(255, 255, 255, 0.1);
            --accent-green: #238636;
            --accent-cyan: #38bdf8;
            --accent-purple: #a855f7;
            --accent-amber: #f59e0b;
            --accent-red: #ef4444;
            --text-primary: #f0f6fc;
            --text-secondary: #8b949e;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg-primary);
            color: var(--text-primary);
            padding: 24px;
            min-height: 100vh;
        }

        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
            padding-bottom: 16px;
            border-bottom: 1px solid var(--border-card);
        }

        .title-group h1 {
            font-size: 24px;
            font-weight: 700;
            background: linear-gradient(135deg, #38bdf8 0%, #a855f7 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .title-group p {
            color: var(--text-secondary);
            font-size: 14px;
            margin-top: 4px;
        }

        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 12px;
            border-radius: 9999px;
            background: rgba(35, 134, 54, 0.2);
            border: 1px solid rgba(35, 134, 54, 0.4);
            color: #4ade80;
            font-size: 13px;
            font-weight: 600;
        }

        .pulse-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #4ade80;
            box-shadow: 0 0 8px #4ade80;
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(74, 222, 128, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(74, 222, 128, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(74, 222, 128, 0); }
        }

        .grid-stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
        }

        .card {
            background: var(--bg-card);
            backdrop-filter: blur(12px);
            border: 1px solid var(--border-card);
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
        }

        .card-label {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-secondary);
            margin-bottom: 8px;
        }

        .card-value {
            font-size: 28px;
            font-weight: 700;
            color: var(--text-primary);
        }

        .card-subtext {
            font-size: 12px;
            color: var(--text-secondary);
            margin-top: 4px;
        }

        .grid-charts {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 20px;
        }

        @media (max-width: 1024px) {
            .grid-charts { grid-template-columns: 1fr; }
        }

        .chart-card {
            display: flex;
            flex-direction: column;
            height: 340px;
        }

        .chart-header {
            font-size: 16px;
            font-weight: 600;
            margin-bottom: 12px;
            color: var(--text-primary);
        }

        .chart-container {
            position: relative;
            flex: 1;
            width: 100%;
            height: 100%;
        }

        .endpoint-bar {
            margin-top: 24px;
            padding: 12px 16px;
            background: rgba(255, 255, 255, 0.03);
            border-radius: 8px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 13px;
            color: var(--text-secondary);
        }

        .endpoint-bar a {
            color: var(--accent-cyan);
            text-decoration: none;
        }
    </style>
</head>
<body>
    <header>
        <div class="title-group">
            <h1>Dodian Server (${serverEnv}) - Telemetry Dashboard</h1>
            <p>Real-time game tick cycles, active entity counts, JVM memory & network bandwidth</p>
        </div>
        <div class="status-badge">
            <div class="pulse-dot"></div>
            <span>LIVE METRICS</span>
        </div>
    </header>

    <div class="grid-stats">
        <div class="card">
            <div class="card-label">Active Players</div>
            <div class="card-value" id="stat-players">0</div>
            <div class="card-subtext">Online RS317 Sessions</div>
        </div>
        <div class="card">
            <div class="card-label">Active NPCs</div>
            <div class="card-value" id="stat-npcs">0</div>
            <div class="card-subtext">Spawned Entities</div>
        </div>
        <div class="card">
            <div class="card-label">Tick Processing</div>
            <div class="card-value" id="stat-tick">0.00 ms</div>
            <div class="card-subtext" id="stat-tick-sub">600ms Budget</div>
        </div>
        <div class="card">
            <div class="card-label">JVM Memory</div>
            <div class="card-value" id="stat-memory">0 MB</div>
            <div class="card-subtext" id="stat-memory-sub">Heap Allocated</div>
        </div>
        <div class="card">
            <div class="card-label">Network Traffic</div>
            <div class="card-value" id="stat-network">0.0 KB/s</div>
            <div class="card-subtext" id="stat-network-sub">In / Out Rate</div>
        </div>
    </div>

    <div class="grid-charts">
        <div class="card chart-card">
            <div class="chart-header">Active Entities (Players & NPCs)</div>
            <div class="chart-container"><canvas id="entitiesChart"></canvas></div>
        </div>
        <div class="card chart-card">
            <div class="chart-header">Tick Processing Time (ms)</div>
            <div class="chart-container"><canvas id="tickChart"></canvas></div>
        </div>
        <div class="card chart-card">
            <div class="chart-header">JVM Memory Usage (MB)</div>
            <div class="chart-container"><canvas id="memoryChart"></canvas></div>
        </div>
        <div class="card chart-card">
            <div class="chart-header">Network Bandwidth (KB/s)</div>
            <div class="chart-container"><canvas id="networkChart"></canvas></div>
        </div>
    </div>

    <div class="endpoint-bar">
        <span>Prometheus Exporter Endpoint: <a href="/prometheus" target="_blank">/prometheus</a></span>
        <span>Raw Metrics History API: <a href="/api/metrics/history" target="_blank">/api/metrics/history</a></span>
    </div>

    <script>
        const chartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 300 },
            scales: {
                x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8b949e', maxTicksLimit: 10 } },
                y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8b949e' }, beginAtZero: true }
            },
            plugins: {
                legend: { labels: { color: '#f0f6fc', font: { family: 'inherit', size: 12 } } }
            }
        };

        const entitiesCtx = document.getElementById('entitiesChart').getContext('2d');
        const entitiesChart = new Chart(entitiesCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    { label: 'Players', data: [], borderColor: '#38bdf8', backgroundColor: 'rgba(56,189,248,0.1)', fill: true, tension: 0.3 },
                    { label: 'NPCs', data: [], borderColor: '#a855f7', backgroundColor: 'rgba(168,85,247,0.1)', fill: true, tension: 0.3 }
                ]
            },
            options: chartOptions
        });

        const tickCtx = document.getElementById('tickChart').getContext('2d');
        const tickChart = new Chart(tickCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    { label: 'Total Cycle (ms)', data: [], borderColor: '#238636', backgroundColor: 'rgba(35,134,54,0.15)', fill: true, tension: 0.3 },
                    { label: 'Player Processing', data: [], borderColor: '#38bdf8', borderWidth: 1.5, tension: 0.3 },
                    { label: 'NPC Processing', data: [], borderColor: '#a855f7', borderWidth: 1.5, tension: 0.3 },
                    { label: 'World Maintenance', data: [], borderColor: '#f59e0b', borderWidth: 1.5, tension: 0.3 }
                ]
            },
            options: chartOptions
        });

        const memoryCtx = document.getElementById('memoryChart').getContext('2d');
        const memoryChart = new Chart(memoryCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    { label: 'Used Memory (MB)', data: [], borderColor: '#f59e0b', backgroundColor: 'rgba(245,158,11,0.15)', fill: true, tension: 0.3 },
                    { label: 'Max Allocated (MB)', data: [], borderColor: 'rgba(255,255,255,0.2)', borderDash: [5, 5], fill: false }
                ]
            },
            options: chartOptions
        });

        const networkCtx = document.getElementById('networkChart').getContext('2d');
        const networkChart = new Chart(networkCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    { label: 'Inbound (KB/s)', data: [], borderColor: '#38bdf8', tension: 0.3 },
                    { label: 'Outbound (KB/s)', data: [], borderColor: '#ef4444', tension: 0.3 }
                ]
            },
            options: chartOptions
        });

        async function fetchMetrics() {
            try {
                const res = await fetch('/api/metrics/history');
                if (!res.ok) return;
                const history = await res.json();
                if (!history || history.length === 0) return;

                const latest = history[history.length - 1];
                document.getElementById('stat-players').innerText = latest.players;
                document.getElementById('stat-npcs').innerText = latest.npcs;
                document.getElementById('stat-tick').innerText = latest.cycleTimeMs.toFixed(2) + ' ms';
                document.getElementById('stat-memory').innerText = Math.round(latest.heapMemoryMb) + ' MB';
                document.getElementById('stat-memory-sub').innerText = 'Max: ' + Math.round(latest.maxMemoryMb) + ' MB';
                document.getElementById('stat-network').innerText = (latest.bandwidthInKb + latest.bandwidthOutKb).toFixed(1) + ' KB/s';
                document.getElementById('stat-network-sub').innerText = 'In: ' + latest.bandwidthInKb.toFixed(1) + ' | Out: ' + latest.bandwidthOutKb.toFixed(1) + ' KB/s';

                const labels = history.map(item => {
                    const d = new Date(item.timestamp);
                    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
                });

                entitiesChart.data.labels = labels;
                entitiesChart.data.datasets[0].data = history.map(i => i.players);
                entitiesChart.data.datasets[1].data = history.map(i => i.npcs);
                entitiesChart.update('none');

                tickChart.data.labels = labels;
                tickChart.data.datasets[0].data = history.map(i => i.cycleTimeMs);
                tickChart.data.datasets[1].data = history.map(i => i.playerCycleMs);
                tickChart.data.datasets[2].data = history.map(i => i.npcCycleMs);
                tickChart.data.datasets[3].data = history.map(i => i.worldCycleMs || 0);
                tickChart.update('none');

                memoryChart.data.labels = labels;
                memoryChart.data.datasets[0].data = history.map(i => i.heapMemoryMb);
                memoryChart.data.datasets[1].data = history.map(i => i.maxMemoryMb);
                memoryChart.update('none');

                networkChart.data.labels = labels;
                networkChart.data.datasets[0].data = history.map(i => i.bandwidthInKb);
                networkChart.data.datasets[1].data = history.map(i => i.bandwidthOutKb);
                networkChart.update('none');

            } catch (err) {
                console.error("Failed to update dashboard metrics:", err);
            }
        }

        fetchMetrics();
        setInterval(fetchMetrics, 3000);
    </script>
</body>
</html>
        """.trimIndent()
    }
}
