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
        // A routine HANDLED click is already logged by the object/npc/item audit categories;
        // repeating it here just doubles every skill interaction in the console. Keep the
        // per-click line for problems only - counters above still record everything.
        val routineSuccess = result.outcome.name == "HANDLED" && result.incidentId == null
        if (routineSuccess && !logger.isDebugEnabled) {
            return
        }
        val message = "SKILL DISPATCH | outcome={} | module={} | route={} | binding={} | elapsedMs={} | incident={} | player={} | dbId={} | pos={} | interface={}"
        val args = arrayOf<Any?>(
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
        if (routineSuccess) {
            logger.debug(message, *args)
        } else {
            logger.info(message, *args)
        }
    }

    @JvmStatic
    fun snapshot(): Map<String, Long> = counters.entries
        .sortedWith(compareBy({ it.key.moduleId }, { it.key.route.name }, { it.key.outcome }))
        .associate { (key, value) -> "${key.moduleId}:${key.route.name.lowercase()}:${key.outcome.lowercase()}" to value.sum() }

    internal fun recordOutcome(moduleId: String, route: SkillPolicyRoute, outcome: String) {
        val key = SkillBetaMetricKey(moduleId, route, outcome)
        counters.computeIfAbsent(key) { LongAdder() }.increment()
        OperationalTelemetry.incrementCounter("skill.dispatch.${route.name.lowercase()}.${outcome.lowercase()}")
        if (outcome == "ERROR" || outcome == "QUARANTINED") {
            OperationalTelemetry.incrementCounter("skill.dispatch.failure")
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
