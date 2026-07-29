package net.dodian.uber.game.model.entity.player

import net.dodian.uber.game.model.item.GameItem

/**
 * Per-session economy UI state.  It is deliberately not persisted: inventory and
 * bank containers remain the durable MySQL-backed player data.
 */
class EconomyRuntimeState {
    val priceCheckerItems = mutableListOf<GameItem>()
    var priceCheckerOpen = false
    var priceCheckerMode = 0
    var priceCheckerSearchedItem: GameItem? = null
}
