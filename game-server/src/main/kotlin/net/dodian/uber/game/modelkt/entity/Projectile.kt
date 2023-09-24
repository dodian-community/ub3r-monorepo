package net.dodian.uber.game.modelkt.entity

import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.EntityUpdateType
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.area.update.GroupableEntity
import net.dodian.uber.game.modelkt.area.update.ProjectileUpdateOperation
import net.dodian.uber.game.modelkt.area.update.UpdateOperation

data class Projectile(
    val delay: Int,
    val source: Position,
    val destination: Position,
    val startHeight: Int,
    val endHeight: Int,
    val graphic: Int,
    val lifetime: Int,
    val pitch: Int,
    val target: Int,
    val offset: Int,
    override val world: World,
    override val entityType: EntityType = EntityType.PROJECTILE,
    override var position: Position = source
) : Entity, GroupableEntity {

    override fun toUpdateOperation(region: Region, updateType: EntityUpdateType) =
        ProjectileUpdateOperation(region, updateType, this)
}