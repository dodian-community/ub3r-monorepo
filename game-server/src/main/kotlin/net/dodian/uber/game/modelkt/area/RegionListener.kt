package net.dodian.uber.game.modelkt.area

import net.dodian.uber.game.modelkt.entity.Entity

interface RegionListener {
    fun execute(region: Region, entity: Entity, update: EntityUpdateType)
}