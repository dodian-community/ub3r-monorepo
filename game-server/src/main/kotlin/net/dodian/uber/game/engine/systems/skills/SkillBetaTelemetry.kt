package net.dodian.uber.game.engine.systems.skills

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import net.dodian.uber.game.api.content.ContentBooleanResult
import net.dodian.uber.game.api.plugin.ContentRouteCatalog
import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

/**
 * Low-cardinality, player-safe telemetry for live skill-plugin beta testing.
 *
 * This records only routes that were actually claimed by a skill plugin. Generic object/NPC
 * audit logs remain responsible for unknown client interactions, avoiding an unbounded metric
 * cardinality from arbitrary IDs sent by clients.
 */
object SkillBetaTelemetry {
    private val logger = LoggerFactory.getLogger(SkillBetaTelemetry::class.java)
    private val counters = ConcurrentHashMap<SkillBetaMetricKey, LongAdder>()
    private val modulesByBinding = ConcurrentHashMap<String, String>()

    @JvmStatic
    fun record(
        client: Client,
        bindingKey: String,
        route: SkillPolicyRoute,
        result: ContentBooleanResult,
        elapsedNanos: Long,
    ) {
        // Module discovery is frozen before live traffic. Cache this finite route lookup so normal
        // skill actions do not rebuild the diagnostic catalog on every click.
        val moduleId = modulesByBinding.computeIfAbsent(bindingKey) {
            ContentRouteCatalog.moduleForBinding(it) ?: "unknown"
        }
        recordOutcome(moduleId, route, result.outcome.name)
        logger.info(
            "SKILL BETA | outcome={} | module={} | route={} | binding={} | elapsedMs={} | incident={} | player={} | dbId={} | pos={} | interface={}",
            result.outcome,
            moduleId,
            route,
            bindingKey,
            String.format(java.util.Locale.ROOT, "%.3f", elapsedNanos.coerceAtLeast(0L) / 1_000_000.0),
            result.incidentId ?: "-",
            client.playerName,
            client.dbId,
            client.position,
            client.activeInterfaceId,
        )
    }

    @JvmStatic
    fun snapshot(): Map<String, Long> = counters.entries
        .sortedWith(compareBy({ it.key.moduleId }, { it.key.route.name }, { it.key.outcome }))
        .associate { (key, value) -> "${key.moduleId}:${key.route.name.lowercase()}:${key.outcome.lowercase()}" to value.sum() }

    internal fun recordOutcome(moduleId: String, route: SkillPolicyRoute, outcome: String) {
        val key = SkillBetaMetricKey(moduleId, route, outcome)
        counters.computeIfAbsent(key) { LongAdder() }.increment()
        OperationalTelemetry.incrementCounter("skill.beta.${route.name.lowercase()}.${outcome.lowercase()}")
        if (outcome == "ERROR" || outcome == "QUARANTINED") {
            OperationalTelemetry.incrementCounter("skill.beta.failure")
        }
    }

    internal fun resetForTests() {
        counters.clear()
        modulesByBinding.clear()
    }
}

data class SkillBetaMetricKey(
    val moduleId: String,
    val route: SkillPolicyRoute,
    val outcome: String,
)
