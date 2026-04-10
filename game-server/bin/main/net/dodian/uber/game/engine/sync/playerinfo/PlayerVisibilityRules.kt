package net.dodian.uber.game.engine.sync.playerinfo

import net.dodian.uber.game.model.entity.player.Player

object PlayerVisibilityRules {
    @JvmStatic
    fun isVisibleTo(viewer: Player?, other: Player?): Boolean {
        if (viewer == null || other == null || viewer === other || !other.isActive) {
            return false
        }
        if (!viewer.withinDistance(other)) {
            return false
        }
        return !other.invis || viewer.invis
    }

    @JvmStatic
    fun canAddLocal(viewer: Player?, other: Player?): Boolean {
        if (!isVisibleTo(viewer, other)) {
            return false
        }
        return viewer!!.didTeleport() || !viewer.playersUpdating.contains(other)
    }
}
