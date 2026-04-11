package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.engine.systems.action.PolicyPreset
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

enum class SkillPolicyRoute {
    OBJECT,
    NPC,
    ITEM_ON_ITEM,
    ITEM,
    ITEM_ON_OBJECT,
    BUTTON,
    ACTION_CYCLE,
}

enum class SkillPolicyResult {
    HANDLED,
    CANCELLED,
    SETTLE_WAIT,
    POLICY_REJECT,
    ROUTE_BYPASS_REJECT,
}

data class SkillPolicyMetricKey(
    val preset: PolicyPreset,
    val route: SkillPolicyRoute,
    val result: SkillPolicyResult,
)

object SkillPolicyMetrics {
    private val logger = LoggerFactory.getLogger(SkillPolicyMetrics::class.java)
    private val counters = ConcurrentHashMap<SkillPolicyMetricKey, LongAdder>()
    private val totalEvents = AtomicLong(0L)
    private const val LOG_EVERY_EVENTS = 250L

    @JvmStatic
    fun record(
        preset: PolicyPreset,
        route: SkillPolicyRoute,
        result: SkillPolicyResult,
    ) {
        counters.computeIfAbsent(SkillPolicyMetricKey(preset, route, result)) { LongAdder() }.increment()
        val current = totalEvents.incrementAndGet()
        if (current % LOG_EVERY_EVENTS == 0L) {
            val snapshot = counters.entries
                .sortedByDescending { it.value.sum() }
                .take(12)
                .joinToString { "${it.key.preset}/${it.key.route}/${it.key.result}=${it.value.sum()}" }
            logger.info("Skill policy metrics totalEvents={} {}", current, snapshot)
        }
    }

    @JvmStatic
    fun snapshot(): Map<SkillPolicyMetricKey, Long> =
        counters.mapValues { it.value.sum() }
}
