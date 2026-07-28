package net.dodian.uber.game.api.content

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.plugin.ContentRouteCatalog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

enum class ContentHandlerOutcome {
    HANDLED,
    REJECTED,
    ERROR,
    QUARANTINED,
}

data class ContentBooleanResult(
    val handled: Boolean,
    val outcome: ContentHandlerOutcome,
    val incidentId: String? = null,
)

object ContentErrorPolicy {
    private val logger: Logger = LoggerFactory.getLogger(ContentErrorPolicy::class.java)
    private val incidentSequence = AtomicLong()

    @JvmStatic
    fun runBoolean(
        player: Client,
        scope: String,
        bindingKey: String = scope,
        defaultValue: Boolean = false,
        action: () -> Boolean,
    ): Boolean = runBooleanResult(player, scope, bindingKey, defaultValue, action).handled

    /**
     * Executes a player-facing content handler without allowing it to destabilize the game loop.
     * The outcome is intentionally explicit so dispatchers can emit useful beta telemetry instead
     * of treating a rejected interaction, an exception, and a quarantined route as the same false.
     */
    @JvmStatic
    fun runBooleanResult(
        player: Client,
        scope: String,
        bindingKey: String = scope,
        defaultValue: Boolean = false,
        action: () -> Boolean,
    ): ContentBooleanResult {
        if (!ContentFaultCircuitBreaker.allows(bindingKey)) {
            val incidentId = nextIncidentId()
            logger.warn("Content handler skipped because binding is quarantined incident={} scope={} binding={} slot={} name={} pos={} interface={} recent={}", incidentId, scope, bindingKey, player.slot, player.playerName, player.position, player.activeInterfaceId, player.describeRecentInboundPackets())
            player.sendMessage("That interaction is temporarily unavailable. Reference: $incidentId")
            return ContentBooleanResult(defaultValue, ContentHandlerOutcome.QUARANTINED, incidentId)
        }
        return try {
            val handled = action()
            ContentBooleanResult(
                handled = handled,
                outcome = if (handled) ContentHandlerOutcome.HANDLED else ContentHandlerOutcome.REJECTED,
            )
        } catch (throwable: Throwable) {
            val incidentId = nextIncidentId()
            ContentFaultCircuitBreaker.recordFailure(bindingKey, ContentRouteCatalog.moduleForBinding(bindingKey))
            logger.error(
                "Content handler failure incident={} scope={} binding={} slot={} name={} dbId={} pos={} interface={} recent={}",
                incidentId,
                scope,
                bindingKey,
                player.slot,
                player.playerName,
                player.dbId,
                player.position,
                player.activeInterfaceId,
                player.describeRecentInboundPackets(),
                throwable,
            )
            player.sendMessage("That interaction could not be completed. Reference: $incidentId")
            ContentBooleanResult(defaultValue, ContentHandlerOutcome.ERROR, incidentId)
        }
    }

    @JvmStatic
    fun <T> runNullable(
        player: Client,
        scope: String,
        bindingKey: String = scope,
        action: () -> T?,
    ): T? {
        if (!ContentFaultCircuitBreaker.allows(bindingKey)) {
            return null
        }
        return try {
            action()
        } catch (throwable: Throwable) {
            ContentFaultCircuitBreaker.recordFailure(bindingKey, ContentRouteCatalog.moduleForBinding(bindingKey))
            logger.error(
                "Content handler failure scope={} binding={} slot={} name={} dbId={} pos={} interface={} recent={}",
                scope,
                bindingKey,
                player.slot,
                player.playerName,
                player.dbId,
                player.position,
                player.activeInterfaceId,
                player.describeRecentInboundPackets(),
                throwable,
            )
            null
        }
    }

    private fun nextIncidentId(): String = "CT-${System.currentTimeMillis().toString(36)}-${incidentSequence.incrementAndGet().toString(36)}"
}
