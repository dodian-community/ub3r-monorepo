package net.dodian.uber.game.content.objects.services

import net.dodian.cache.region.Region
import net.dodian.uber.game.model.Position
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object ObjectClipService {
    private val logger = LoggerFactory.getLogger(ObjectClipService::class.java)

    data class AppliedClip(
        val position: Position,
        val type: Int,
        val direction: Int,
        val solid: Boolean,
    )

    private val appliedClips = ConcurrentHashMap<String, AppliedClip>()

    fun apply(position: Position, type: Int, direction: Int, solid: Boolean) {
        try {
            Region.addClippingForVariableObject(position.x, position.y, position.z, type, direction, solid)
        } catch (e: Exception) {
            logger.debug("Clipping apply failed at {} (region cache not ready yet)", position, e)
        }
        appliedClips[key(position)] = AppliedClip(position.copy(), type, direction, solid)
    }

    fun remove(position: Position, type: Int, direction: Int, solid: Boolean) {
        try {
            Region.removeClippingForVariableObject(position.x, position.y, position.z, type, direction, solid)
        } catch (e: Exception) {
            logger.debug("Clipping remove failed at {} (region cache not ready yet)", position, e)
        }
        appliedClips.remove(key(position))
    }

    internal fun getAppliedForTests(position: Position): AppliedClip? = appliedClips[key(position)]

    internal fun clearForTests() {
        appliedClips.clear()
    }

    private fun key(position: Position): String = "${position.x}:${position.y}:${position.z}"
}
