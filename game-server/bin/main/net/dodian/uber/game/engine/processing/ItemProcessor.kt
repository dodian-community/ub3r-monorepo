package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.systems.world.item.Ground

class ItemProcessor : Runnable {
    override fun run() {
        if (Ground.tradeable_items.isNotEmpty()) {
            for (item in Ground.tradeable_items) {
                item.reduceTime()
                if (!item.isTaken() && !item.isVisible() && item.timeToShow < 1) {
                    item.visible = true
                    item.itemDisplay()
                } else if (item.getDespawnTime() < 1) {
                    Ground.deleteItem(item)
                }
            }
        }

        if (Ground.untradeable_items.isNotEmpty()) {
            for (item in Ground.untradeable_items) {
                item.reduceTime()
                if (item.getDespawnTime() < 1) {
                    Ground.deleteItem(item)
                }
            }
        }

        if (Ground.ground_items.isNotEmpty()) {
            for (item in Ground.ground_items) {
                if (item.isVisible() || !item.isTaken()) {
                    continue
                }
                item.reduceTime()
                if (item.getDespawnTime() < 1) {
                    item.setTaken(false)
                    item.visible = true
                    item.timeToDespawn = item.display
                    Ground.addItem(item)
                }
            }
        }
    }
}
