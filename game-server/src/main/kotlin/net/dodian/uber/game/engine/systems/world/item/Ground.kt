package net.dodian.uber.game.engine.systems.world.item

import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GroundItem

object Ground {
    @JvmField
    val ground_items: CopyOnWriteArrayList<GroundItem> = CopyOnWriteArrayList()

    @JvmField
    val untradeable_items: CopyOnWriteArrayList<GroundItem> = CopyOnWriteArrayList()

    @JvmField
    val tradeable_items: CopyOnWriteArrayList<GroundItem> = CopyOnWriteArrayList()

    @JvmStatic
    fun deleteItem(item: GroundItem) {
        when (item.type) {
            0 -> {
                item.setTaken(true)
                item.visible = false
                item.removeItemDisplay()
            }

            1 -> {
                item.setTaken(true)
                item.removeItemDisplay()
                untradeable_items.remove(item)
            }

            else -> {
                item.setTaken(true)
                item.visible = false
                item.removeItemDisplay()
                tradeable_items.remove(item)
            }
        }
    }

    @JvmStatic
    fun addItem(item: GroundItem) {
        when (item.type) {
            0 -> {
                val existingIndex = ground_items.indexOf(item)
                if (existingIndex < 0) {
                    ground_items.add(item)
                } else {
                    ground_items[existingIndex] = item
                }
                item.itemDisplay()
            }

            1 -> untradeable_items.add(item)
            else -> tradeable_items.add(item)
        }
    }

    @JvmStatic
    fun isTracked(item: GroundItem?): Boolean {
        if (item == null) {
            return false
        }
        return when (item.type) {
            0 -> ground_items.contains(item)
            1 -> untradeable_items.contains(item)
            else -> tradeable_items.contains(item)
        }
    }

    @JvmStatic
    fun canPickup(client: Client?, item: GroundItem?): Boolean {
        if (client == null || item == null || item.isTaken() || client.position.z != item.z) {
            return false
        }
        return when (item.type) {
            0 -> item.isVisible()
            1 -> client.dbId == item.playerId
            else -> item.isVisible() || client.dbId == item.playerId
        }
    }

    @JvmStatic
    fun tryClaimPickup(client: Client?, item: GroundItem?): Boolean {
        if (client == null || item == null) {
            return false
        }
        synchronized(item) {
            if (!isTracked(item) || item.isTaken() || !canPickup(client, item)) {
                return false
            }
            item.setTaken(true)
            return true
        }
    }

    @JvmStatic
    fun releaseClaim(item: GroundItem?) {
        if (item == null) {
            return
        }
        synchronized(item) {
            item.setTaken(false)
        }
    }

    private fun findGroundItem(
        list: CopyOnWriteArrayList<GroundItem>,
        client: Client?,
        id: Int,
        x: Int,
        y: Int,
        z: Int,
    ): GroundItem? {
        if (list.isEmpty()) {
            return null
        }
        for (item in list) {
            if (item.id != id || item.x != x || item.y != y || item.z != z || item.isTaken()) {
                continue
            }
            if (client != null && !canPickup(client, item)) {
                continue
            }
            return item
        }
        return null
    }

    @JvmStatic
    fun findGroundItem(client: Client?, id: Int, x: Int, y: Int, z: Int): GroundItem? {
        if (!Server.itemManager.isTradable(id)) {
            val staticItem = findGroundItem(ground_items, client, id, x, y, z)
            if (staticItem != null) {
                return staticItem
            }
            return findGroundItem(untradeable_items, client, id, x, y, z)
        }

        val staticItem = findGroundItem(ground_items, client, id, x, y, z)
        if (staticItem != null) {
            return staticItem
        }
        return findGroundItem(tradeable_items, client, id, x, y, z)
    }

    @JvmStatic
    fun findGroundItem(id: Int, x: Int, y: Int, z: Int): GroundItem? =
        findGroundItem(null, id, x, y, z)

    @JvmStatic
    fun addGroundItem(pos: Position, id: Int, amount: Int, time: Int) {
        addItem(GroundItem(pos, id, amount, time, true))
    }

    @JvmStatic
    fun addFloorItem(c: Client, id: Int, amount: Int) {
        addItem(GroundItem(c.position, intArrayOf(c.slot, id, amount, 500)))
    }

    @JvmStatic
    fun addFloorItem(c: Client, pos: Position, id: Int, amount: Int, time: Int) {
        addItem(GroundItem(pos, intArrayOf(c.slot, id, amount, time)))
    }

    @JvmStatic
    fun addNpcDropItem(c: Client, n: Npc, id: Int, amount: Int) {
        addItem(GroundItem(n.position, id, amount, c.slot, n.id))
    }
}
