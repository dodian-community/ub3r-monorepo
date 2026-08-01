package net.dodian.uber.game.combat

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.netty.listener.out.Projectile
import net.dodian.uber.game.netty.listener.out.SendMessage

object SpotAnimNames {
    private val nameToId: Map<String, Int>
    private val defs: Map<String, ProjectileDef>

    init {
        val loaded = TomlProjectileLoader.load()
        nameToId = loaded.associate { it.name to it.id }
        defs = loaded.associate { it.name to it.def }
    }

    fun getId(name: String): Int {
        return nameToId[name] ?: -1
    }

    fun getDef(name: String): ProjectileDef? {
        return defs[name]
    }
}

object ShootProjectile {
    const val DEFAULT_HEIGHT = 40
    const val DEFAULT_DELAY = 51
    /** Opcode 117's serialized trajectory-curve byte. */
    const val DEFAULT_SLOPE = 16
}

data class ProjectileDef(
    val startHeight: Int = 43,
    val endHeight: Int = 31,
    val delay: Int = 51,
    val slope: Int = 16
)

fun Position.shoot(
    gfxName: String,
    target: Entity,
    delay: Int? = null,
    startHeight: Int? = null,
    endHeight: Int? = null,
    slope: Int? = null
): Int {
    return projectile(
        caster = null,
        gfxName = gfxName,
        sourceTile = this,
        targetTile = target.position,
        target = target,
        delay = delay,
        startHeight = startHeight,
        endHeight = endHeight,
        slope = slope
    )
}

fun Position.shoot(
    gfxName: String,
    targetTile: Position,
    delay: Int? = null,
    startHeight: Int? = null,
    endHeight: Int? = null,
    slope: Int? = null
): Int {
    return projectile(
        caster = null,
        gfxName = gfxName,
        sourceTile = this,
        targetTile = targetTile,
        target = null,
        delay = delay,
        startHeight = startHeight,
        endHeight = endHeight,
        slope = slope
    )
}

fun Entity.shoot(
    gfxName: String,
    target: Entity,
    delay: Int? = null,
    startHeight: Int? = null,
    endHeight: Int? = null,
    slope: Int? = null
): Int {
    val defaultDef = SpotAnimNames.getDef(gfxName)
    return projectile(
        caster = this,
        gfxName = gfxName,
        sourceTile = this.position,
        targetTile = target.position,
        target = target,
        delay = delay,
        startHeight = startHeight ?: defaultDef?.startHeight ?: 43,
        endHeight = endHeight ?: defaultDef?.endHeight ?: 31,
        slope = slope
    )
}

fun Entity.shoot(
    gfxName: String,
    targetTile: Position,
    delay: Int? = null,
    startHeight: Int? = null,
    endHeight: Int? = null,
    slope: Int? = null
): Int {
    val defaultDef = SpotAnimNames.getDef(gfxName)
    return projectile(
        caster = this,
        gfxName = gfxName,
        sourceTile = this.position,
        targetTile = targetTile,
        target = null,
        delay = delay,
        startHeight = startHeight ?: defaultDef?.startHeight ?: 43,
        endHeight = endHeight ?: defaultDef?.endHeight ?: 31,
        slope = slope
    )
}

private fun projectile(
    caster: Entity?,
    gfxName: String,
    sourceTile: Position,
    targetTile: Position,
    target: Entity?,
    delay: Int?,
    startHeight: Int?,
    endHeight: Int?,
    slope: Int?
): Int {
    val gfxId = SpotAnimNames.getId(gfxName)
    if (gfxId == -1) return -1

    val defaultDef = SpotAnimNames.getDef(gfxName)

    val dx = kotlin.math.abs(sourceTile.x - targetTile.x)
    val dy = kotlin.math.abs(sourceTile.y - targetTile.y)
    val distance = kotlin.math.max(dx, dy)

    val startDelay = delay ?: defaultDef?.delay ?: ShootProjectile.DEFAULT_DELAY
    val projSlope = slope ?: defaultDef?.slope ?: ShootProjectile.DEFAULT_SLOPE

    val speed = 50 + (distance * 5)
    val totalTime = startDelay + speed

    val targetIndex = if (target != null) {
        val isNpc = target is Npc
        if (isNpc) target.slot + 1 else -(target.slot + 1)
    } else {
        0
    }

    if (net.dodian.uber.game.engine.config.gameWorldId == 2) {
        val msg = "[W2-PROJECTILE] gfx=$gfxName, source=(${sourceTile.x},${sourceTile.y},${sourceTile.z}), target=(${targetTile.x},${targetTile.y},${targetTile.z}), dist=$distance, delay=$startDelay, speed=$speed, slope=$projSlope, targetIndex=$targetIndex"
        println(msg)
        if (caster is Client) {
            caster.send(SendMessage(msg))
        }
    }

    // Pass the actual caster position (mutable, will be read during serialization after movement finalize)
    // or the static source tile if no caster entity exists.
    val sourceObject = caster?.position ?: sourceTile
    val targetObject = target ?: targetTile

    sendProjectile(
        source = sourceObject,
        targetObject = targetObject,
        gfxMoving = gfxId,
        startHeight = startHeight ?: defaultDef?.startHeight ?: ShootProjectile.DEFAULT_HEIGHT,
        endHeight = endHeight ?: defaultDef?.endHeight ?: ShootProjectile.DEFAULT_HEIGHT,
        targetIndex = targetIndex,
        delay = startDelay,
        speed = speed,
        slope = projSlope
    )

    return totalTime
}

private fun sendProjectile(
    source: Position,
    targetObject: Any,
    gfxMoving: Int,
    startHeight: Int,
    endHeight: Int,
    targetIndex: Int,
    delay: Int,
    speed: Int,
    slope: Int
) {
    val projectile = Projectile(source, targetObject, speed, gfxMoving, startHeight, endHeight, targetIndex, delay, slope, 64)
    for (p in PlayerRegistry.players) {
        if (p != null) {
            val viewer = p as Client
            if (viewer.isActive && !viewer.disconnected && viewer.dbId > 0
                && viewer.position != null
                && viewer.position.z == source.z
                && viewer.position.withinDistance(source, 64)) {
                viewer.send(projectile)
            }
        }
    }
}
