package net.dodian.uber.game.modelkt.entity

import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.Position

class Npc(
    override var position: Position,
    override val world: World,
    override val entityType: EntityType = EntityType.NPC,
) : Mob()