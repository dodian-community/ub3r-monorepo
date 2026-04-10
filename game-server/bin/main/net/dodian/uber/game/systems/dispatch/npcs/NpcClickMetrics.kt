package net.dodian.uber.game.systems.dispatch.npcs

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

object NpcClickMetrics {
    private val logger = LoggerFactory.getLogger(NpcClickMetrics::class.java)
    private val counters = ConcurrentHashMap<String, LongAdder>()
    private val traceEnabled = flag("npc.click.trace.enabled", defaultValue = true)

    @JvmStatic
    fun increment(key: String) {
        counters.computeIfAbsent(key) { LongAdder() }.increment()
    }

    @JvmStatic
    fun count(key: String): Long = counters[key]?.sum() ?: 0L

    @JvmStatic
    fun clearForTests() {
        counters.clear()
    }

    @JvmStatic
    fun recordDecoded(opcode: Int, option: Int, npcIndex: Int, playerName: String) {
        increment("decode.option.$option")
        increment("decode.opcode.$opcode")
        if (traceEnabled && logger.isTraceEnabled) {
            logger.trace(
                "npc_click stage=decoded opcode={} option={} npcIndex={} player={}",
                opcode,
                option,
                npcIndex,
                playerName,
            )
        }
    }

    @JvmStatic
    fun recordScheduled(opcode: Int, option: Int, npcId: Int, npcIndex: Int, playerName: String) {
        increment("schedule.option.$option")
        if (traceEnabled && logger.isTraceEnabled) {
            logger.trace(
                "npc_click stage=scheduled opcode={} option={} npcId={} npcIndex={} player={}",
                opcode,
                option,
                npcId,
                npcIndex,
                playerName,
            )
        }
    }

    @JvmStatic
    fun recordRejected(reason: String, opcode: Int, option: Int, npcIndex: Int, playerName: String) {
        increment("reject.$reason")
        if (traceEnabled && logger.isDebugEnabled) {
            logger.debug(
                "npc_click stage=rejected reason={} opcode={} option={} npcIndex={} player={}",
                reason,
                opcode,
                option,
                npcIndex,
                playerName,
            )
        }
    }

    @JvmStatic
    fun recordWait(reason: String, option: Int, npcId: Int, npcIndex: Int, playerName: String) {
        increment("wait.$reason")
        if (traceEnabled && logger.isTraceEnabled) {
            logger.trace(
                "npc_click stage=waiting reason={} option={} npcId={} npcIndex={} player={}",
                reason,
                option,
                npcId,
                npcIndex,
                playerName,
            )
        }
    }

    @JvmStatic
    fun recordDispatch(option: Int, npcId: Int, handled: Boolean, handlerName: String?, playerName: String) {
        increment("dispatch.option.$option")
        increment(if (handled) "dispatch.handled" else "dispatch.unhandled")
        if (!handled) {
            increment("fallback.option.$option")
        }
        if (traceEnabled && logger.isTraceEnabled) {
            logger.trace(
                "npc_click stage=dispatch option={} npcId={} handled={} handler={} player={}",
                option,
                npcId,
                handled,
                handlerName ?: "n/a",
                playerName,
            )
        }
    }

    @JvmStatic
    fun recordQueueStale(playerName: String, intentType: String) {
        increment("queue.stale")
        if (traceEnabled && logger.isDebugEnabled) {
            logger.debug("npc_click stage=cancelled reason=stale_intent intentType={} player={}", intentType, playerName)
        }
    }

    private fun flag(propertyName: String, defaultValue: Boolean): Boolean {
        val prop = System.getProperty(propertyName)?.trim()
        if (!prop.isNullOrEmpty()) {
            return prop.equals("true", ignoreCase = true)
        }
        val env = System.getenv(propertyName.uppercase().replace('.', '_'))?.trim()
        if (!env.isNullOrEmpty()) {
            return env.equals("true", ignoreCase = true)
        }
        return defaultValue
    }
}
