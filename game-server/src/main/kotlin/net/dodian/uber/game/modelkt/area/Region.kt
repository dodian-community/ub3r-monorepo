package net.dodian.uber.game.modelkt.area

import net.dodian.uber.game.modelkt.entity.Entity
import net.dodian.uber.game.modelkt.entity.isTransient

class Region {

    fun removeEntity(entity: Entity) {
        val type = entity.type
        if (type.isTransient) {
            error("Tried to remove a transient entity ($entity) from region ($this).")
        }
    }
}