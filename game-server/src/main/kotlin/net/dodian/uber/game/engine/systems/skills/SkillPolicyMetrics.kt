package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.engine.systems.action.PolicyPreset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

enum class SkillPolicyRoute {
    OBJECT,
    NPC,
    ITEM_ON_ITEM,
    ITEM,
    ITEM_ON_OBJECT,
    MAGIC_ON_OBJECT,
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
    private val counters = ConcurrentHashMap<SkillPolicyMetricKey, LongAdder>()

    @JvmStatic
    fun record(
        preset: PolicyPreset,
        route: SkillPolicyRoute,
        result: SkillPolicyResult,
    ) {
        counters.computeIfAbsent(SkillPolicyMetricKey(preset, route, result)) { LongAdder() }.increment()
    }

    @JvmStatic
    fun snapshot(): Map<SkillPolicyMetricKey, Long> =
        counters.mapValues { it.value.sum() }
}
