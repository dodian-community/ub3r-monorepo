package net.dodian.uber.game.modelkt.entity

import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.Position

interface Entity {
    val entityType: EntityType
    val world: World

    var position: Position
}