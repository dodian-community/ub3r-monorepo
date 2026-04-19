package net.dodian.uber.game.engine.systems.interaction.objects

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.persistence.audit.ConsoleAuditLog
import net.dodian.uber.game.engine.systems.cache.CacheCollisionAuditStore
import net.dodian.uber.game.engine.systems.interaction.ClipProbeService
import net.dodian.uber.game.engine.systems.interaction.ObjectInteractionContext
import org.slf4j.LoggerFactory
import kotlin.math.abs

object ObjectClickLoggingService {
    private val ignoredUnhandled = emptySet<Int>()
    private val logger = LoggerFactory.getLogger(ObjectClickLoggingService::class.java)

    @JvmStatic
    fun log(
        context: ObjectInteractionContext,
        resolution: ObjectContentRegistry.ObjectResolution?,
        handled: Boolean,
        handlerSource: String? = null,
    ) {
        if (!handled) {
            logUnhandledDetails(context)
        }
        if (handled || context.objectId !in ignoredUnhandled) {
            ConsoleAuditLog.objectInteraction(context, resolution, handled, handlerSource)
        }
    }

    private fun logUnhandledDetails(context: ObjectInteractionContext) {
        if (!logger.isWarnEnabled) return
        val definition = context.obj ?: GameObjectData.forId(context.objectId)
        val candidates = ObjectContentRegistry.resolveCandidates(context.objectId, context.position)
        val probe = ClipProbeService.probeTile(context.position.x, context.position.y, context.position.z)
        val matchingOverlaps = probe.objectMatches.filter { it.objectId == context.objectId }
        val nearbySameIdAnchors =
            CacheCollisionAuditStore
                .objectsForTile(context.position.x, context.position.y)
                .asSequence()
                .filter { it.plane == context.position.z && it.objectId == context.objectId }
                .map { obj ->
                    val dx = abs(obj.x - context.position.x)
                    val dy = abs(obj.y - context.position.y)
                    Triple(dx + dy, obj.x, obj.y)
                }.filter { it.first <= 3 }
                .sortedBy { it.first }
                .take(3)
                .joinToString(";") { "d=${it.first}@${it.second},${it.third}" }
                .ifBlank { "none" }
        val overlapSummary = ClipProbeService.formatOverlapSummary(probe, 3)
        logger.warn(
            "OBJECT UNHANDLED DETAIL | type={} option={} objectId={} pos={},{},{} " +
                "| defName={} size={}x{} blockWalk={} blockRange={} solid={} walkable={} actions={} " +
                "| contentCandidates={} matchingOverlaps={} nearbySameIdAnchors={} fullBlocked={} flags=0x{} [{}] overlaps={}",
            context.type,
            context.option ?: -1,
            context.objectId,
            context.position.x,
            context.position.y,
            context.position.z,
            definition.name,
            definition.sizeX,
            definition.sizeY,
            definition.blockWalk(),
            definition.blockRange(),
            definition.isSolid(),
            definition.isWalkable(),
            definition.hasActions(),
            candidates.size,
            matchingOverlaps.size,
            nearbySameIdAnchors,
            probe.fullMobBlocked,
            probe.rawFlags.toString(16),
            ClipProbeService.formatFlags(probe.rawFlags),
            overlapSummary,
        )
    }
}
