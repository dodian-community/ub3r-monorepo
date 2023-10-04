package net.dodian.uber.game.modelkt.entity.`object`

import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.entity.EntityType
import net.dodian.uber.game.modelkt.entity.player.Player

class DynamicGameObject(
    id: Int,
    position: Position,
    orientation: Int,
    type: Int,
    world: World,
    val alwaysVisible: Boolean,
    override val entityType: EntityType = EntityType.DYNAMIC_OBJECT
) : GameObject(id, type, orientation, position, world) {

    override fun viewableBy(player: Player, world: World) {

    }
}