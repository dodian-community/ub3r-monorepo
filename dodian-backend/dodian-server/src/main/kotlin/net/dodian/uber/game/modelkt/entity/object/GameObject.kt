package net.dodian.uber.game.modelkt.entity.`object`

import net.dodian.uber.game.modelkt.entity.Entity
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.EntityUpdateType
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.area.update.GroupableEntity
import net.dodian.uber.game.modelkt.area.update.ObjectUpdateOperation
import net.dodian.uber.game.modelkt.area.update.UpdateOperation
import net.dodian.uber.game.modelkt.entity.player.Player

@Suppress("MemberVisibilityCanBePrivate")
abstract class GameObject(
    val id: Int,
    val type: Int,
    val orientation: Int,
    override var position: Position,
    override val world: World
) : Entity, GroupableEntity {

    val packed: Int get() = id shl 8 or (type and 0x3F) shl 2 or orientation and 0x3

    constructor(packed: Int, position: Position, world: World) : this(
        id = packed ushr 8,
        type = packed shr 2 and 0x3F,
        orientation = packed and 0x3,
        position,
        world
    )

    abstract fun viewableBy(player: Player, world: World)

    override fun toUpdateOperation(region: Region, updateType: EntityUpdateType): UpdateOperation<*> {
        return ObjectUpdateOperation(this, region, updateType)
    }
}