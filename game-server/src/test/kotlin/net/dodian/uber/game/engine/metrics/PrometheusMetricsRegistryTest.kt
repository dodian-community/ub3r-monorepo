package net.dodian.uber.game.engine.metrics

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrometheusMetricsRegistryTest {

    @Test
    fun `test Prometheus text format generation`() {
        PrometheusMetricsRegistry.recordLogin("success")
        PrometheusMetricsRegistry.recordTickMetrics(
            players = 10,
            npcs = 50,
            totalCycleMs = 12.5,
            playerMs = 4.2,
            npcMs = 3.1,
            bytesInTotal = 1024L,
            bytesOutTotal = 4096L
        )

        val output = PrometheusMetricsRegistry.getPrometheusTextFormat()
        assertNotNull(output)
        assertTrue(output.contains("dodian_active_players") || output.startsWith("# Prometheus"))
    }

    @Test
    fun `test history snapshot collection`() {
        PrometheusMetricsRegistry.recordTickMetrics(
            players = 5,
            npcs = 20,
            totalCycleMs = 8.0,
            playerMs = 2.0,
            npcMs = 1.5
        )

        val history = PrometheusMetricsRegistry.getHistorySnapshot()
        assertNotNull(history)
        assertFalse(history.isEmpty())
    }

    @Test
    fun `test dashboard HTML rendering`() {
        val html = MetricsDashboardHtml.renderHtml()
        assertNotNull(html)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("chart.js"))
    }
}
