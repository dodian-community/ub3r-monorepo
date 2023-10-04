package net.dodian.uber.game.modelkt.area.update

import net.dodian.uber.game.modelkt.area.EntityUpdateType
import net.dodian.uber.game.modelkt.area.Region

interface GroupableEntity {
    fun toUpdateOperation(region: Region, updateType: EntityUpdateType): UpdateOperation<*>
}